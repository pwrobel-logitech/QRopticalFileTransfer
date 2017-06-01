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


    //below, fields regarding the estimation of the moment after we know for sure the detector has ended
    //even if the few last frames are missing
    //we estimate based on the moment of first dataframe that arrived and the last dataframe so far
    //we know how many frame to expect to, so simply interpolate the time the last frame is supposed
    //to arrive (with some small delay added)
    boolean is_first_frame_arrived = false;
    int first_frame_num_arrived;
    long time_first_frame_arrived;
    boolean is_last_frame_so_far_arrived = false;
    int last_frame_number_arrived_so_far; // must be greater than the first frame number
    long time_last_frame_number_arrived_so_far; //only used when first and last frame so far arrived
    double current_estimated_moment_of_end;
    static long time_overhead = 300000000; // 180ms in nanosecs as a time unit. From System.nanoTime()
    boolean triggered_autoestimated_end = false;
    boolean tiggered_lastframedetectedbase_end = false;
    //end of the fields for the estimation of the end of the detection

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

        int ntot = get_total_frames_of_data_that_will_be_produced();
        int lf = get_last_number_of_frame_detected();

        if(status > 0 && ntot != -1 && lf != -1){ //correctly recognized - not header frame
            if(!is_first_frame_arrived){
                first_frame_num_arrived = lf;
                time_first_frame_arrived = System.nanoTime();
                Log.i("QQQ", "First frame arrived : "+lf);
                is_first_frame_arrived = true;
            }

            if(is_first_frame_arrived && lf > first_frame_num_arrived + 4 && lf > last_frame_number_arrived_so_far){
                is_last_frame_so_far_arrived = true;
                time_last_frame_number_arrived_so_far = System.nanoTime();
                last_frame_number_arrived_so_far = lf;
                Log.i("QQQ", "Last frame arrived so far: "+lf);
            }
        }

        if(is_first_frame_arrived && is_last_frame_so_far_arrived && ntot!=-1 && (!triggered_autoestimated_end)){
            //do the execution of end, if the interpolated time exceeed the limit
            double est_time = time_first_frame_arrived +
                    (time_last_frame_number_arrived_so_far - time_first_frame_arrived) *
                    (ntot/(last_frame_number_arrived_so_far - first_frame_num_arrived));
            long lest_time = (long) est_time;
            if (System.nanoTime() > lest_time + time_overhead){
                Log.i("QQQ", "Triggering the end of detection");
                //if(!tiggered_lastframedetectedbase_end) //turn off time based autodetection, since it's buggy anyway
                //    tell_decoder_no_more_qr();
                //triggered_autoestimated_end = true;
            }
        }


        if(ntot > 0 && lf > 0)
            if(lf >= ntot - 1)
                if(!triggered_autoestimated_end){
                    tell_decoder_no_more_qr();
                    tiggered_lastframedetectedbase_end = true;
            }

        if(status > 0)
            Log.i("APIINFO", "Totalframes : " + get_total_frames_of_data_that_will_be_produced()+
                  " lashHdrProducedN "+get_last_number_of_header_frame_detected() +
                  " lastDatFrProducedN "+get_last_number_of_frame_detected() + " laststatus "+status);

        /*if(false)
        ((Activity)context).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toolbar t = (Toolbar)((Activity)context).findViewById(R.id.toolbar);
                t.setTitle("Status "+status);
            }
        });*/

        //Log.i("STATUS QR frame", "status is : " + status);

/*
        YuvImage yuv = new YuvImage(data, parameters.getPreviewFormat(), width, height, null);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        yuv.compressToJpeg(new Rect(0, 0, width, height), 50, out);

        byte[] bytes = out.toByteArray();
        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
*/
/*
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

        }*/


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


                int []fpsrange = new int [2];
                param.getPreviewFpsRange(fpsrange);
                param.setPreviewFpsRange(fpsrange[0], fpsrange[1]);

                param.set("vrmode", 1);
                param.set("fast-fps-mode", 1);

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

                //param.setRecordingHint(true);
                param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

                //int nareas = param.getMaxNumFocusAreas();
                //if (nareas > 0) {
                    //ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>(1);
                    //focusAreas.add(new Camera.Area(new Rect(-100, -100, 100, 100), 1000));
                    //param.setFocusAreas(focusAreas);
                    //param.setMeteringAreas(focusAreas);
                //}

                camera.setParameters(param);



                camera.addCallbackBuffer(callbackbuffer);
                camera.setPreviewCallbackWithBuffer(CameraWorker.this);
                camera.startPreview();

                initialize_decoder();
                set_decoded_file_path("/mnt/sdcard/out");
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
                tell_decoder_no_more_qr();
                deinitialize_decoder();
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
                //tell_decoder_no_more_qr();
                //if (camera != null)


                    //try{
                    //    camera.cancelAutoFocus();
                    //}catch (Exception e){}
                //Camera.Parameters param = camera.getParameters();

                //param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                //camera.setParameters(param);
                //Log.i("Focus", "auto");
                    camera.autoFocus(new Camera.AutoFocusCallback() {
                        @Override
                        public void onAutoFocus(boolean success, Camera camera2) {
                            Log.i("Focus", "camera autofocused, success = " + success);
                            //Camera.Parameters param = camera.getParameters();
                            //param.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                            //camera.setParameters(param);
                            camera.cancelAutoFocus();
                            //camera.autoFocus(null);
                            //Log.i("Focus", "fixed");

                        }
                    });
                //camera.cancelAutoFocus();
                //camera.autoFocus(null);
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

    public static native int get_total_frames_of_data_that_will_be_produced();
    public static native int get_last_number_of_frame_detected();
    public static native int get_last_number_of_header_frame_detected();


    public static native int deinitialize_decoder();

}
