package qrfiles.pwrobel.myapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;

import android.os.Process;
import android.util.Log;

import java.io.IOException;
import java.util.List;

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

    }

    public CameraWorker(String name) {
        super(name, Process.THREAD_PRIORITY_URGENT_DISPLAY);
        context = null;
        camsurf = null;
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        Log.i("thr", "executed on thread id: " + android.os.Process.myTid());
        Log.i("info", "Got byte arrQ of size "+data.length+" bytes");

        applyGrayScale(greyscalebuffer, data, camwidth, camheight);

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

                Camera.Parameters param = camera.getParameters();
                List<Camera.Size> psize = param.getSupportedPreviewSizes();

                int camwidth = psize.get(7).width;
                int camheight = psize.get(7).height;
                CameraWorker.this.camwidth = camwidth;
                CameraWorker.this.camheight = camheight;

                try{
                    param.setPreviewSize(camwidth, camheight);
                }catch (Exception e){
                    Log.e("camsize", "Error setting the camera preview size");
                }

                callbackbuffer = new byte[camheight*camwidth*4 * 2];
                greyscalebuffer = new byte[camheight*camwidth];


                SurfaceTexture surfaceTexture = camsurf.getSurfaceTexture();
                try {
                    camera.setPreviewTexture(surfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                final Camera.Parameters params = camera.getParameters();
                params.setRecordingHint(true);
                params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                camera.setParameters(params);
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
    };


    /// data as NV21 input, pixels as 8bit greyscale output
    public static void applyGrayScale(byte [] pixels, byte [] data, int width, int height) {
        byte p;
        int size = width*height;
        for(int i = 0; i < size; i++) {
            p = (byte)(data[i] & 0xFF);
            pixels[i] = p;
        }
    }
}
