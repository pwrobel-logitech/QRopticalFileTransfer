package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

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

    private String dumpfoldername = "Downloads";
    public synchronized void setDumpFolderName(String dumpfoldname){
        this.dumpfoldername = dumpfoldname;
    }

    boolean surface_and_camera_prepared = false;
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
        camcontroller.initCamAsync(width, height, dumpfoldername);

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

        synchronized (this){

            this.surface_and_camera_prepared = true;
            this.notifyAll();
        }

        Log.i("click", "detector fully inited");
        Log.i("clickable", "executed on the thread in cam surf, id: " + android.os.Process.myTid());
    }

    private boolean isblur = true;
    public synchronized void set_is_blur(boolean isbl){
        this.isblur = isbl;
    }
    @Override
    public void onDrawFrame(GL10 gl) {

        boolean should_quit = false;
        synchronized (this){
            should_quit = this.is_drawer_deinitialized;
        }
        if (should_quit)
            return;

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
        int uisblur = mOffscreenShader.getHandle("is_blur");

        GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
        GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
        GLES20.glUniform2fv(uRatioV, 1, mRatio, 0);
        GLES20.glUniform2fv(usizes, 1, sizeprev, 0);
        GLES20.glUniform1f(urpevratio, m_prev_yx_ratio);
        GLES20.glUniform1f(usuccratio, (float)curr_succ_ratio_got_from_camworker);

        synchronized (this) {
            if (this.isblur)
                GLES20.glUniform1f(uisblur, (float) 0.99f);
            else
                GLES20.glUniform1f(uisblur, (float) 0.01f);
        }

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandle);

        //render quad
        int aPosition = mOffscreenShader.getHandle("aPosition");
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0, mFullQuadVertices);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        /////////////////////////////////////////////////////////


        synchronized (this) {
            if (this.nframe_drawn % 3 == 1 &&
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

            if (this.nframe_drawn % 5 == 1 &&
                    (System.nanoTime() - this.timeinitprogressbar > 200000000L)) {
                final boolean should_draw_something_on_progressbar = this.camcontroller.shouldDrawProgressBars();
                if (should_draw_something_on_progressbar == false){
                    draw_notification_status_if_needed();
                }else{
                    Activity a = (Activity) this.getContext();
                    if (a != null){
                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                decoder_status_textView1.setVisibility(INVISIBLE);
                                decoder_status_textView2.setVisibility(INVISIBLE);
                                decoder_status_textView1.requestLayout();
                                decoder_status_textView2.requestLayout();
                            }
                        });
                    }
                }
            }
        }
        this.nframe_drawn++;

        this.make_text_status_blink_switch();

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

    private TextView decoder_status_textView1;
    private TextView decoder_status_textView2;
    boolean is_blink_phase_on = false; //for the status text, it blinks on and off - this indicates which phase of cycle is set
    double ms_phase_on = 900.0; // approximate miliseconds spend in the on and off phase
    double ms_phase_off = 300.0;
    double last_time_phase_switch = System.nanoTime()/1.0e6; //based on the current time, this value and is_blink_phase_on
    //the switch is done on the last bool value - controlling the visibility of the text
    public void make_text_status_blink_switch(){

            double begin = System.nanoTime()/1.0e6;
            double diff = begin - this.last_time_phase_switch;
            if ((is_blink_phase_on && diff > ms_phase_on) || (!is_blink_phase_on && diff > ms_phase_off)){
                synchronized (this) {
                    this.is_blink_phase_on = !this.is_blink_phase_on;
                }
                this.last_time_phase_switch = begin;
                //Log.i("Blink", "off/on");
            }
    }

    public void setCustomDecoderStatusTextView(TextView tv1, TextView tv2){
        this.decoder_status_textView1 = tv1;
        this.decoder_status_textView2 = tv2;
    }


    private void drawProgressBars(){

        double nr = camcontroller.getCurrentNoiseRatio();
        double pr = camcontroller.getCurrentProgressRatio();

        final boolean should_draw_something_on_progressbar = this.camcontroller.shouldDrawProgressBars();
        Activity a = (Activity) getContext();
        a.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (should_draw_something_on_progressbar){
                    if (pr1drawer != null)
                        pr1drawer.setVisibility(VISIBLE);
                    if (pr2drawer != null)
                        pr2drawer.setVisibility(VISIBLE);
                }
                else {
                    if (pr1drawer != null)
                        pr1drawer.setVisibility(INVISIBLE);
                    if (pr2drawer != null)
                        pr2drawer.setVisibility(INVISIBLE);
                }
            }
        });
        //Log.i("PRBAR", "should draw something " + should_draw_something_on_progressbar);

        //Log.i("NRR", "ratio " + nr);
        if(pr1drawer != null ) {
            pr1drawer.drawMe((int)(1000.0*pr), CustomProgressBar.progressBarType.PROGRESS, should_draw_something_on_progressbar);
        }
        if(pr2drawer != null ) {//
            final CameraController.DisplayStatusInfo status = this.camcontroller.getDisplayStatusText();
            this.errbar_additional_text = status.additional_err_text;
            if (CameraPreviewSurface.this.errbar_additional_text != null)
                pr2drawer.setFileName(errbar_additional_text);
            pr2drawer.drawMe((int)(1000.0*nr), CustomProgressBar.progressBarType.NOISE, should_draw_something_on_progressbar);
        }
    }

    private String errbar_additional_text = null;
    private synchronized void draw_notification_status_if_needed(){
        final CameraController.DisplayStatusInfo status = this.camcontroller.getDisplayStatusText();
        this.errbar_additional_text = status.additional_err_text;
        if (this.decoder_status_textView1 != null && this.decoder_status_textView2 != null){
            Activity a = (Activity) getContext();
            final boolean should_draw_something_on_progressbar = this.camcontroller.shouldDrawProgressBars();
            final boolean blink_text_phase = CameraPreviewSurface.this.is_blink_phase_on;
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    char intensity = 0xff;
                    char backintensity = 0x3f;
                    if (status.displayTextType == CameraController.DisplayStatusInfo.StatusDisplayType.TYPE_NOTE){
                        CameraPreviewSurface.this.decoder_status_textView1.setTextColor(Color.rgb(intensity,intensity,backintensity));
                        CameraPreviewSurface.this.decoder_status_textView2.setTextColor(Color.rgb(intensity,intensity,backintensity));
                    }
                    else if (status.displayTextType == CameraController.DisplayStatusInfo.StatusDisplayType.TYPE_ERR){
                        CameraPreviewSurface.this.decoder_status_textView1.setTextColor(Color.rgb(intensity,backintensity,backintensity));
                        CameraPreviewSurface.this.decoder_status_textView2.setTextColor(Color.rgb(intensity,backintensity,backintensity));
                    }
                    else if (status.displayTextType == CameraController.DisplayStatusInfo.StatusDisplayType.TYPE_DONE){
                        CameraPreviewSurface.this.decoder_status_textView1.setTextColor(Color.rgb(backintensity,intensity,backintensity));
                        CameraPreviewSurface.this.decoder_status_textView2.setTextColor(Color.rgb(backintensity,intensity,backintensity));
                    }
                    //decoder_status_textView1.setBackgroundColor(Color.argb(0x7f,0x0,0x0,0x0));
                    //decoder_status_textView2.setBackgroundColor(Color.argb(0x7f,0x0,0x0,0x0));
                    CameraPreviewSurface.this.decoder_status_textView1.setTextSize(19);
                    CameraPreviewSurface.this.decoder_status_textView1.setText(status.displaytext);
                    CameraPreviewSurface.this.decoder_status_textView2.setTextSize(19);
                    CameraPreviewSurface.this.decoder_status_textView2.setText(status.displaytext2);
                    //Log.i("BLK2", "stat "+blink_text_phase + " should "+should_draw_something_on_progressbar);
                    if((should_draw_something_on_progressbar == false) && blink_text_phase){ //uncomment that to get the blink behaviour
                        //if (decoder_status_textView1.getVisibility() == INVISIBLE){
                            decoder_status_textView1.setVisibility(VISIBLE);
                        //}
                        //if (decoder_status_textView2.getVisibility() == INVISIBLE)
                            decoder_status_textView2.setVisibility(VISIBLE);
                            decoder_status_textView1.requestLayout();
                            decoder_status_textView2.requestLayout();
                    }else{
                        //if (decoder_status_textView1.getVisibility() == VISIBLE)
                            decoder_status_textView1.setVisibility(GONE);

                        //if (decoder_status_textView2.getVisibility() == VISIBLE)
                            decoder_status_textView2.setVisibility(GONE);
                            decoder_status_textView1.requestLayout();
                            decoder_status_textView2.requestLayout();
                    }
                }
            });
        }
    }

    boolean is_drawer_deinitialized = false;
    public synchronized void deinitialize_resources(){
        is_drawer_deinitialized = true;

        if (pr1drawer != null) {
            pr1drawer.setVisibility(GONE);
            pr1drawer.requestLayout();
        }
        if (pr2drawer != null){
            pr2drawer.setVisibility(GONE);
            pr2drawer.requestLayout();
        }

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
