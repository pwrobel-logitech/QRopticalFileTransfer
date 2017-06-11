package qrfiles.pwrobel.myapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
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

import java.nio.ByteBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by pwrobel on 04.05.17.
 */

public class CameraPreviewSurface extends GLSurfaceView implements
        GLSurfaceView.Renderer,
        SurfaceTexture.OnFrameAvailableListener,

        View.OnTouchListener
{
    private long nframe_drawn = 0;

    private float[] mTransformM = new float[16];
    private float[] mOrientationM = new float[16];
    private float[] mRatio = new float[2];
    private float[] sizeprev = new float[2];
    private float m_prev_yx_ratio = 1.0f;
    private ByteBuffer mFullQuadVertices;
    private int mTextureHandle;
    private Shader mOffscreenShader = null;
    private SurfaceTexture mSurfaceTexture = null;

    public SurfaceTexture getSurfaceTexture(){
        return mSurfaceTexture;
    }

    private Context mContext;
    private CameraController camcontroller;

    private double curr_succ_ratio_got_from_camworker = 0.0;

    public CameraPreviewSurface(Context context, CameraController cc) {
        super(context);
        mContext = context;
        camcontroller = cc;
        init();
    }

    public CameraPreviewSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        init();
    }

    public void setCameraController(CameraController cc){
        this.camcontroller = cc;
    }

    public void init(){
        this.setZOrderOnTop(false);
        mRatio[0] = 1;
        mRatio[1] = 1;

        mOffscreenShader = new Shader();


        final byte FULL_QUAD_COORDS[] = {-1, 1, -1, -1, 1, 1, 1, -1};
        mFullQuadVertices = ByteBuffer.allocateDirect(4 * 2);
        mFullQuadVertices.put(FULL_QUAD_COORDS).position(0);

        setPreserveEGLContextOnPause(true);
        setEGLContextClientVersion(2);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }





    //new frame delivered to the surface - redraw by calling requestRender();
    @Override
    public void onFrameAvailable(SurfaceTexture surfaceTexture) {
        //Log.i("thr", "executed on thread id: " + android.os.Process.myTid());
        //Log.i("frame", "frame from the camera has been send to the texture");
        requestRender();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        Log.i("thr", "executed on thread id: " + android.os.Process.myTid());
        Log.i("draw", "surface created");

        try {
            mOffscreenShader.setProgram(R.raw.vsh, R.raw.fsh, mContext);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        Log.i("thr", "executed on thread id: " + android.os.Process.myTid());
        Log.i("draw", "surface changed, size w "+ this.getWidth()+ " h "+this.getHeight() + "argw "+width+" argh "+height);

        int[] mTextureHandles = new int[1];
        GLES20.glGenTextures(1, mTextureHandles, 0);
        mTextureHandle = mTextureHandles[0];

        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandles[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);


        //set up surfacetexture------------------
        SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
        mSurfaceTexture = new SurfaceTexture(mTextureHandle);
        mSurfaceTexture.setOnFrameAvailableListener(this);
        if(oldSurfaceTexture != null){
            oldSurfaceTexture.release();
        }




        synchronized (mContext) {

            while (camcontroller == null)
                try {
                    mContext.wait(); //wait for the camcontext to be available for sure
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
        camcontroller.initCamAsync(width, height);

        synchronized (camcontroller) {
            while (!camcontroller.isCameraInitialized()){
                try {
                    camcontroller.wait(); //wait for the initialization of the camera to complete
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(0, info);

        float rot_angle = 0.0f;

        rot_angle = info.orientation;

        int camera_width = camcontroller.getCamPreviewWidth();
        int camera_height = camcontroller.getCamPreviewHeight();

        float a=0,b=0; //rotated parameters
        if(info.orientation % 180 == 0){
            a = camera_height;
            b = camera_width;
        }else{
            a = camera_width;
            b = camera_height;
        }

        if( ((float)a)/((float)b) < ((float)height)/((float)width) ){
            //camera preview more square than surface to present
            mRatio[0] = ((((float)b)*((float)height))/(((float)a)*((float)width))); //multiply by q>1
            mRatio[1] = 1.0f;
        }else{
            mRatio[0] = 1.0f;
            mRatio[1] = (((float)a)/((float)b))/(((float)height)/((float)width)); //multiply by the same(inv) q < 1
        }


        m_prev_yx_ratio = ((float)a)/((float)b);
        sizeprev[0] = b;
        sizeprev[1] = a;

        Matrix.setRotateM(mOrientationM, 0, rot_angle, 0f, 0f, 1f);

        /*try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }*/

        this.timeinitprogressbar = System.nanoTime();
        requestRender();
    }

    @Override
    public void onDrawFrame(GL10 gl) {


        curr_succ_ratio_got_from_camworker = camcontroller.getCurrentSuccRatio();

        //Log.i("thr", "executed on thread id: " + android.os.Process.myTid());
        //Log.i("draw", "frame has been requested to be drawn");

        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        mSurfaceTexture.updateTexImage();
        mSurfaceTexture.getTransformMatrix(mTransformM);


        int sw = this.getWidth();
        int sh = this.getHeight();

        GLES20.glViewport(0, 0, sw, sh);

        mOffscreenShader.useProgram();

        int uTransformM = mOffscreenShader.getHandle("uTransformM");
        int uOrientationM = mOffscreenShader.getHandle("uOrientationM");
        int uRatioV = mOffscreenShader.getHandle("ratios");
        int urpevratio = mOffscreenShader.getHandle("prev_yx_ratio");
        int usuccratio = mOffscreenShader.getHandle("succratio");
        int usizes = mOffscreenShader.getHandle("sizeprev");

        GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
        GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
        GLES20.glUniform2fv(uRatioV, 1, mRatio, 0);
        GLES20.glUniform2fv(usizes, 1, sizeprev, 0);
        GLES20.glUniform1f(urpevratio, m_prev_yx_ratio);
        GLES20.glUniform1f(usuccratio, (float)curr_succ_ratio_got_from_camworker);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandle);

        //render quad
        int aPosition = mOffscreenShader.getHandle("aPosition");
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0, mFullQuadVertices);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        /////////////////////////////////////////////////////////


        synchronized (this) {
            if (this.nframe_drawn % 3 == 2 &&
                    (System.nanoTime() - this.timeinitprogressbar > 200000000L)) {
                //Activity a = (Activity) this.getContext();
                //a.runOnUiThread(new Runnable() {
                //    @Override
                //    public void run() {
                        drawProgressBars();
                //    }
                //});
                //this.drawProgressBars();
            }
        }
        this.nframe_drawn++;
    }


    CustomProgressBar pr1drawer = null; //progress
    CustomProgressBar pr2drawer = null; //errors
    long progress_err_num = 400;
    long progress_progress_num = 690;
    long timeinitprogressbar = 0;
    boolean can_draw_progressbars_for_thefirsttime = false;
    public void setCustomDecoderProgressBarsDrawers(CustomProgressBar pr1drawer, CustomProgressBar pr2drawer){
        this.pr1drawer = pr1drawer;
        this.pr2drawer = pr2drawer;
    }

    private void drawProgressBars(){
        if(pr1drawer != null) {
            pr1drawer.drawMe(this.progress_progress_num);
        }
        if(pr2drawer != null) {//
            pr2drawer.drawMe(this.progress_err_num);
        }
    }

    public synchronized void deinitialize_resources(){
        this.pr1drawer = null;
        this.pr2drawer = null;
    };

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i("Touch", "surface touched");
        if (camcontroller != null)
            camcontroller.callAutoFocusAsync();
        return false;
    }
}
