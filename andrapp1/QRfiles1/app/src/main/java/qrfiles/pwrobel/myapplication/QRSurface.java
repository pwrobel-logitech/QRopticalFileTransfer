package qrfiles.pwrobel.myapplication;

import android.content.Context;
import android.graphics.SurfaceTexture;
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

        try {
            mOffscreenShader.setProgram(R.raw.qrvsh, R.raw.qrfsh, mContext);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }


    private int mTextureHandle;
    private int surfw = 0, surfh = 0;
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i("QRSurf", "surface changed w"+width + " h "+height);
        this.surfw = width;
        this.surfh = height;

        int[] mTextureHandles = new int[1];
        GLES20.glGenTextures(1, mTextureHandles, 0);
        mTextureHandle = mTextureHandles[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandles[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
        mSurfaceTexture = new SurfaceTexture(mTextureHandle);

        float rot = 0.0f;
        Matrix.setRotateM(mOrientationM, 0, rot, 0f, 0f, 1f);

    }

    private Shader mOffscreenShader = null;
    private float[] mTransformM = new float[16];
    private float[] mOrientationM = new float[16];
    private SurfaceTexture mSurfaceTexture = null;
    @Override
    public void onDrawFrame(GL10 gl) {

        GLES20.glClearColor(0.0f, 1.0f, 1.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTransformM);

        GLES20.glViewport(0, 0, this.surfw, this.surfh);

        mOffscreenShader.useProgram();

        int uTransformM = mOffscreenShader.getHandle("uTransformM");
        int uOrientationM = mOffscreenShader.getHandle("uOrientationM");

        GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
        GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
        //GLES20.glUniform2fv(uRatioV, 1, mRatio, 0);
        //GLES20.glUniform2fv(usizes, 1, sizeprev, 0);
        //GLES20.glUniform1f(urpevratio, m_prev_yx_ratio);
        //GLES20.glUniform1f(usuccratio, (float)curr_succ_ratio_got_from_camworker);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandle);

        //render quad
        int aPosition = mOffscreenShader.getHandle("aPosition");
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0, mvertexs);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        /////////////////////////////////////////////////////////


        Log.i("QRSurf", "ondrawframe");
    }

    private boolean waiting_for_qrmanager_thread_to_finish = false;
    public void destroy_all_resources(){
        synchronized (this){
            should_display_anything = false;
            qrsurf_manager_thread_running = false;
        }



        try {
            this.qrsufr_manager_thread.join();
        } catch (InterruptedException e) {
            Log.i("QRSurfThr", "Failed to join thread");
            e.printStackTrace();
        }
        this.qrsufr_manager_thread = null;
    }


    public void setFPS(double fps){
        this.fps = fps;
    }

    ByteBuffer mvertexs;
    private void init_qrsurf(){

        final byte FULL_QUAD_COORDS[] = {-1, 1, -1, -1, 1, 1, 1, -1};
        mvertexs = ByteBuffer.allocateDirect(4 * 2);
        mvertexs.put(FULL_QUAD_COORDS).position(0);

        mOffscreenShader = new Shader();

        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
        //init_qrsurf_thread();
    }



    private boolean should_display_anything = true;
    private double fps = 1;
    private double last_ns_time_frame_requested_for_display = 0.0;
    private boolean qrsurf_manager_thread_running = false;
    private Thread qrsufr_manager_thread = null;
    public void init_qrsurf_thread(){
        this.should_display_anything = true;
        qrsufr_manager_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                synchronized (QRSurface.this){
                    qrsurf_manager_thread_running = true;
                    waiting_for_qrmanager_thread_to_finish = false;
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
            int sleeptime = 1;
            synchronized (this){
                if (!should_display_anything)
                    sleeptime = 200;
            }
            Thread.sleep(sleeptime);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        double framewaitns = 1e9 / this.fps;
        double current_ns = System.nanoTime();

        if (current_ns - this.last_ns_time_frame_requested_for_display > framewaitns){
            last_ns_time_frame_requested_for_display = current_ns;
            Log.i("qrsurf", "manager thread wants new frame");
            if (should_display_anything){
                QRSurface.this.requestRender();
            }
        }



    }

}
