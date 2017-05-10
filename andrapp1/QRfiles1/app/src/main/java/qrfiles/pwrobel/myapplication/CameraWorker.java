package qrfiles.pwrobel.myapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;

import android.os.Process;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RunnableFuture;

/**
 * Created by pwrobel on 29.04.17.
 */

public class CameraWorker extends HandlerThread implements CameraController, Camera.PreviewCallback{

    public volatile Handler handler;
    public Camera camera;
    int camwidth, camheight;
    public CameraPreviewSurface camsurf;
    Context context;
    byte[] callbackbuffer;
    byte[] greyscalebuffer;

    public void setContext(Context cont){
        context = cont;
        Activity a = (Activity) cont;
        camsurf = (CameraPreviewSurface) a.findViewById(R.id.glsurfaceView1);
        callbackbuffer = null;
        camsurf.setCameraController(this);
         //notify initialization done
        synchronized (context){
            context.notify();
        }
        //context.notifyAll();
        initialize_decoder();
        set_decoded_file_path("/mnt/sdcard/out");

        camsurf.setOnTouchListener(camsurf);
    }

    public CameraWorker(String name) {
        super(name, Process.THREAD_PRIORITY_URGENT_DISPLAY);
        context = null;
        camsurf = null;
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Log.i("thr", "executed on thread id: " + android.os.Process.myTid());
        //Log.i("info", "Got byte arrQ of size "+data.length+" bytes");

        applyGrayScale(greyscalebuffer, data, camwidth, camheight);

        final byte[] dat = data;
        Activity a = (Activity) context;


        Camera.Parameters parameters = camera.getParameters();
        final int width = parameters.getPreviewSize().width;
        final int height = parameters.getPreviewSize().height;



        final int status = send_next_grayscale_buffer_to_decoder(greyscalebuffer, width, height);
        if(false)
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toolbar t = (Toolbar)((Activity)context).findViewById(R.id.toolbar);
                t.setTitle("Status "+status);
            }
        });

        //Log.i("STATUS QR frame", "status is : " + status);

/*
        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

        byte[] bytes = out.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
*/

        if(false){

            Runnable r = new Runnable() {
                @Override
                public void run() {
                    Activity act = (Activity) CameraWorker.this.context;
                    ImageView f = (ImageView) act.findViewById(R.id.imv1);

                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inMutable = true;
                    //for (int q = 0; q < greyscalebuffer.length; q++)
                    //  greyscalebuffer[q] = (byte)0x7f;
                    //Bitmap bmp = BitmapFactory.decodeByteArray(dat, 0, dat.length, options);
                    int[] pixels = new int[width * height];
                    for (int i = 0; i < width; i++)
                        for (int j = 0; j < height; j++) {
                            byte p = greyscalebuffer[j * width + i];
                            pixels[j * width + i] = 0xff000000 | ((p << 16) & 0xff0000) | ((p << 8) & 0xff00) | (p & 0xff);
                        }
                    Bitmap bmp = Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888);


                    f.setImageBitmap(bmp);
                    synchronized (this) {
                        this.notify();
                    }
                }
            };

            synchronized (r) {
                a.runOnUiThread(r);
                try {
                    r.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

        }


        camera.addCallbackBuffer(callbackbuffer);
    }


    public synchronized void waitUntilReady() {
        handler = new Handler(getLooper());
    }

    public void initCamAsync(){
        handler.post(new Runnable() {
            @Override
            public void run() {

                if(camera != null){
                    camera.stopPreview();
                    camera.release();
                }

                camera = Camera.open();

                camera.cancelAutoFocus();

                Camera.Parameters param = camera.getParameters();
                List<Camera.Size> psize = param.getSupportedPreviewSizes();

                int camwidth = psize.get(7).width;
                int camheight = psize.get(7).height;
                CameraWorker.this.camwidth = camwidth;
                CameraWorker.this.camheight = camheight;

                callbackbuffer = new byte[camheight*camwidth*4 * 2];
                greyscalebuffer = new byte[camheight*camwidth];

                try{
                    param.setPreviewSize(camwidth, camheight);
                }catch (Exception e){
                    Log.e("camworker", "Error setting the camera preview size");
                }

                SurfaceTexture surfaceTexture = camsurf.getSurfaceTexture();
                try {
                    camera.setPreviewTexture(surfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                param.setRecordingHint(true);
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_MACRO);

                int nareas = param.getMaxNumFocusAreas();
                if (nareas > 0) {
                    ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>(1);
                    focusAreas.add(new Camera.Area(new Rect(-500, -500, 500, 500), 1000));
                    param.setFocusAreas(focusAreas);
                }

                camera.setParameters(param);



                camera.addCallbackBuffer(callbackbuffer);
                camera.setPreviewCallbackWithBuffer(CameraWorker.this);
                camera.startPreview();

            }
        });
    }

    @Override
    public void closeCamAsync() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if(camera != null){
                    camera.stopPreview();
                    camera.release();
                    camera = null;
                }
                if(camsurf != null){
                    camsurf.getSurfaceTexture().release();
                }
            }
        });
    };

    @Override
    public void setCallbackBufferSizeAsync(final int size) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                callbackbuffer = new byte[size];
            }
        });
    }

    @Override
    public void callAutoFocusAsync() {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (camera != null)
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera) {
                            Log.i("Focus", "camera autofocused, success = " + success);
                        }
                    });
            }
        });
    }

    ;


    /// data as NV21 input, pixels as 8bit greyscale output
    public static void applyGrayScale(byte [] pixels, byte [] data, int width, int height) {
        applygrayscalenative(pixels, data, width, height);
/*
        byte p;
        int size = width*height;
        for(int i = 0; i < size; i++) {
            p = (byte)(data[i] & 0xFF);
            pixels[i] = p;
        }
*/
    }


    /////////native part
    public static native void applygrayscalenative(byte [] pixels, byte [] data, int width, int height);

    //native decoder/encoder lib part

    public static native int initialize_decoder();
    public static native int set_decoded_file_path(String path);
    public static native int send_next_grayscale_buffer_to_decoder(
        byte[] grayscale_qr_data,
        int image_width,
        int image_height);

    public static native int tell_decoder_no_more_qr();
    public static native int deinitialize_decoder();

}
