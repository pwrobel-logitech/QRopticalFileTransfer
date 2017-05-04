package qrfiles.pwrobel.myapplication;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Handler;
import android.os.HandlerThread;

import android.os.Process;

import java.io.IOException;

/**
 * Created by pwrobel on 29.04.17.
 */

public class CameraWorker extends HandlerThread {

    public volatile Handler handler;
    public Camera camera;
    public CameraPreviewSurface camsurf;
    Context context;
    byte[] callbackbuffer;

    public void setContext(Context cont){
        context = cont;
        camsurf = new CameraPreviewSurface(context);
        callbackbuffer = new byte[2048*2048*4];
    }

    public CameraWorker(String name) {
        super(name, Process.THREAD_PRIORITY_URGENT_DISPLAY);
        context = null;
        camsurf = null;
    }


    public synchronized void waitUntilReady() {
        handler = new Handler(getLooper());
    }

    public void initAsync(){
        handler.post(new Runnable() {
            @Override
            public void run() {

                camera = Camera.open();

                SurfaceTexture surfaceTexture = new SurfaceTexture(10);
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
                camera.setPreviewCallbackWithBuffer(camsurf);
                camera.startPreview();

            }
        });
    };
}
