package qrfiles.pwrobel.myapplication;

import android.content.Context;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;

import java.nio.ByteBuffer;
import java.security.acl.LastOwnerException;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by pwrobel on 18.06.17.
 */

public class QRSurface extends GLSurfaceView implements
        GLSurfaceView.Renderer {

    Context mContext; //interface for the main activity
    public QRSurface(Context context) {
        super(context);
        Log.i("QRSurf", "constructor 1");
        mContext = context;
        this.init_qrsurf();
    }

    public QRSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        Log.i("QRSurf", "constructor 2");
        mContext = context;
        this.init_qrsurf();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i("QRSurf", "surface created");
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i("QRSurf", "surface changed");
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        Log.i("QRSurf", "ondrawframe");
    }


    public void setFPS(double fps){
        this.fps = fps;
    }

    private void init_qrsurf(){
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        init_qrsurf_thread();
    }


    private double fps = 1;
    private double last_ns_time_frame_requested_for_display = 0.0;
    private boolean qrsurf_manager_thread_running = false;
    private Thread qrsufr_manager_thread = null;
    private void init_qrsurf_thread(){
        qrsufr_manager_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (QRSurface.this){
                    qrsurf_manager_thread_running = true;
                }
                while (true){
                    boolean shouldend = false;
                    synchronized (QRSurface.this){
                        shouldend = !qrsurf_manager_thread_running;
                    }
                    if(shouldend)
                        break;
                    qrsurf_manager_thread_mainfunc();
                }
            }
        });
        qrsufr_manager_thread.start();
    };

    private void qrsurf_manager_thread_mainfunc(){
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        double framewaitns = 1e9 / this.fps;
        double current_ns = System.nanoTime();

        if (current_ns - this.last_ns_time_frame_requested_for_display > framewaitns){
            last_ns_time_frame_requested_for_display = current_ns;
            Log.i("qrsurf", "manager thread wants new frame");
        }



    }

}
