package qrfiles.pwrobel.myapplication;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
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
import java.nio.IntBuffer;
import java.security.acl.LastOwnerException;
import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * Created by pwrobel on 18.06.17.
 */

public class QRSurface extends GLSurfaceView implements
        GLSurfaceView.Renderer {

    public class SurfBuff {
        //surface buffer in java
        int max_width_unscaled = 512;
        int current_width; //must be lower than the max value above. Must be power of 2
        int current_height;
        int current_qrbuffer_size_width;//contains actual width of the image data
        int current_qrbuffer_size_height;
        //which part of the buffer on x and y the actual, non 2^n size buffer occpies
        //=current_qrbuffer_size_width/current_width
        double curr_texture_sample_fraction = 1.0;
        ByteBuffer surfdata = ByteBuffer.allocateDirect(max_width_unscaled * max_width_unscaled);
        //holds returned width
        ByteBuffer produced_width_buffer = null;
        SurfBuff(){
            surfdata = ByteBuffer.allocateDirect(max_width_unscaled * max_width_unscaled);
            for (int i = 0; i < surfdata.capacity(); i++){
                surfdata.put(i, (byte)0xff);
            }
            produced_width_buffer = ByteBuffer.allocateDirect(8); //needs only 4, actually
            produced_width_buffer.order( java.nio.ByteOrder.LITTLE_ENDIAN ); //is big endian by default, but c++ assumes little endian
        }
    };
    SurfBuff surface_buffer = new SurfBuff();


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


    int last_surf_buff_w = 0, last_surf_buff_h = 0;
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

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        GLES20.glEnable( GLES20.GL_TEXTURE_2D );

        //GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextureHandles[0]);
        //GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        //GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);

        //GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGB, 2, 2, 0,
        //       GLES20.GL_RGB, GLES20.GL_UNSIGNED_BYTE, surfdata);
        //SurfaceTexture oldSurfaceTexture = mSurfaceTexture;
        //mSurfaceTexture = new SurfaceTexture(mTextureHandle);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle);

        int uTextureloc = mOffscreenShader.getHandle("tex");
        GLES20.glUniform1i(uTextureloc, 0);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, this.surface_buffer.current_qrbuffer_size_width, this.surface_buffer.current_qrbuffer_size_height, 0,
                GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, surface_buffer.surfdata);
        this.last_surf_buff_w = this.surface_buffer.current_qrbuffer_size_width;
        this.last_surf_buff_h = this.surface_buffer.current_qrbuffer_size_height;

        float rot = 90.0f;
        Matrix.setRotateM(mOrientationM, 0, rot, 0f, 0f, 1f);

    }




    private Shader mOffscreenShader = null;
    private float[] mTransformM = new float[16];
    private float[] mOrientationM = new float[16];
    private SurfaceTexture mSurfaceTexture = null;

    private boolean is_frame_drawing = false;

    @Override
    public void onDrawFrame(GL10 gl) {

        boolean should_display = true;
        synchronized (this){
            should_display = this.should_display_anything;
        }
        if (!should_display)
            return;

        GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);




        //for (int q = 0 ; q<32;q++){
        //    surface_buffer.surfdata.put(q, (byte)0xFF);
        //}
        //surface_buffer.surfdata.put(2, (byte)0x00);
        //surface_buffer.surfdata.put(2, (byte)0x70);

        surface_buffer.surfdata.position(0);


        //GLUtils.texImage2D()

        //mSurfaceTexture.updateTexImage();
        //mSurfaceTexture.getTransformMatrix(mTransformM);

        GLES20.glViewport(0, 0, this.surfw, this.surfh);


        mOffscreenShader.useProgram();

        //int uTransformM = mOffscreenShader.getHandle("uTransformM");
        int uOrientationM = mOffscreenShader.getHandle("uOrientationM");
        int utexratio = mOffscreenShader.getHandle("texratio");

        //int uTextureloc = mOffscreenShader.getHandle("tex");

        //GLES20.glUniformMatrix4fv(uTransformM, 1, false, mTransformM, 0);
        GLES20.glUniformMatrix4fv(uOrientationM, 1, false, mOrientationM, 0);
        //GLES20.glUniform2fv(uRatioV, 1, mRatio, 0);
        //GLES20.glUniform2fv(usizes, 1, sizeprev, 0);
        double texratio = ((double)this.surface_buffer.current_width) / ((double)this.surface_buffer.current_qrbuffer_size_width);
        GLES20.glUniform1f(utexratio, (float)texratio);
        //GLES20.glUniform1f(usuccratio, (float)curr_succ_ratio_got_from_camworker);

       // GLES20.glGenTextures ( 1, textureId, 0 );
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureHandle);

        //GLES20.glUniform1i(uTextureloc, 0);

        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);

        if (this.last_surf_buff_w == this.surface_buffer.current_qrbuffer_size_width &&
                this.last_surf_buff_h == this.surface_buffer.current_qrbuffer_size_height)
            GLES20.glTexSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0,
                    this.surface_buffer.current_qrbuffer_size_width, this.surface_buffer.current_qrbuffer_size_height,
                           GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, surface_buffer.surfdata);
        else {
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_LUMINANCE, this.surface_buffer.current_qrbuffer_size_width, this.surface_buffer.current_qrbuffer_size_height, 0,
                    GLES20.GL_LUMINANCE, GLES20.GL_UNSIGNED_BYTE, surface_buffer.surfdata);
            this.last_surf_buff_w = this.surface_buffer.current_qrbuffer_size_width;
            this.last_surf_buff_h = this.surface_buffer.current_qrbuffer_size_height;
        }
        //render quad
        int aPosition = mOffscreenShader.getHandle("aPosition");
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_BYTE, false, 0, mvertexs);
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        /////////////////////////////////////////////////////////


        Log.i("QRSurf", "ondrawframe");
        boolean shoulddisplay = true;
        synchronized (this) {
            this.is_frame_drawing = false;
            this.notifyAll();
        }
    }

    private boolean waiting_for_qrmanager_thread_to_finish = false;
    public void destroy_all_resources(){
        synchronized (this){
            should_display_anything = false;
            qrsurf_manager_thread_running = false;
        }



        try {
            synchronized (this) {
                this.is_frame_drawing = false;
                this.notifyAll();
            }
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



    private boolean should_display_anything = false;
    private double fps = 5;
    private double last_ns_time_frame_requested_for_display = 0.0;
    private boolean qrsurf_manager_thread_running = false;
    private Thread qrsufr_manager_thread = null;
    public void init_qrsurf_thread(){
        //this.should_display_anything = true;
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
                synchronized (QRSurface.this) {
                    QRSurface.this.nframe_last_produced = -1;
                    QRSurface.this.total_number_of_dataframes_produced_by_the_encoder = 0;
                }
            }
        });
        qrsufr_manager_thread.start();
    };


    private boolean waiting_to_add_files = true;
    public void add_new_files_to_send(ArrayList<String> filespath){
        synchronized (this){
            destroy_current_encoder();
            files_to_send.clear();
            this.index_of_currently_processed_file++;
            for (int i = 0; i < filespath.size(); i++){
                files_to_send.add(i, filespath.get(i));
            }
            //files_to_send.add(1, filespath.get(0));
            Log.i("qrsurf", "index : " + this.index_of_currently_processed_file+
                    ", file : " + files_to_send.get(this.index_of_currently_processed_file));
            init_and_set_external_file_info(files_to_send.get(this.index_of_currently_processed_file), "", 580, 0.5);
            this.header_time_start_ns = System.nanoTime();
            waiting_to_add_files = false;
            this.should_display_anything = true;
            this.is_header_generating = true;
        }
    }

    private int index_of_currently_processed_file = -1;
    private List<String> files_to_send = new ArrayList<String>();
    private void qrsurf_manager_thread_mainfunc(){
        try {
            int sleeptime = 1;
            synchronized (this){
                if (!should_display_anything || this.waiting_to_add_files)
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
                synchronized (this){
                    this.is_frame_drawing = true;

                    if (this.is_header_generating) {
                        double currtime = System.nanoTime();
                        if (currtime - this.header_time_start_ns > this.header_time_timeout_ns) {
                            this.is_header_generating = false;
                            tell_no_more_generating_header();
                            this.total_number_of_dataframes_produced_by_the_encoder
                                = tell_how_much_frames_will_be_generated();
                            Log.i("PPP", "header stooops, TOT frames : "+ this.total_number_of_dataframes_produced_by_the_encoder);
                        }
                    }

                    this.produce_new_qrdata_to_surf_buffer();
                    QRSurface.this.requestRender();

                    while (this.is_frame_drawing) {
                        try {
                            this.wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }



    }


    private CustomProgressBar encoder_progressbar = null;
    public synchronized void setCustomProgressBar(CustomProgressBar progbar){
        this.encoder_progressbar = progbar;
    }

    public void set_header_display_timeout(double header_timeout_s){
        synchronized (this) {
            this.header_time_timeout_ns = 1.0e9 * header_timeout_s;
        }
    }

    private double time_ns_last_upload_progressbar_done = System.nanoTime();
    private double time_ns_interval_upload_updated = 1.0e8;
    private boolean is_header_generating = false;
    private int nframe_last_produced = -1;
    private int total_number_of_dataframes_produced_by_the_encoder = 0;
    private double header_time_start_ns = 0.0;
    private double header_time_timeout_ns = 7e9;//7s
    //returns status, 1=end
    private int produce_new_qrdata_to_surf_buffer(){
        int status = 0;
        int nfiles = 0;

        nfiles = this.files_to_send.size();

        if (nfiles == 0)
            return -1;

        surface_buffer.surfdata.position(0);
        int stat =
            produce_next_qr_grayscale_image_to_mem(this.surface_buffer.surfdata,
                                                   this.surface_buffer.produced_width_buffer);
        this.nframe_last_produced++;
        this.surface_buffer.current_width = this.surface_buffer.produced_width_buffer.asIntBuffer().get(0);
        this.surface_buffer.current_height = this.surface_buffer.current_width;

        this.surface_buffer.current_qrbuffer_size_width = (int)upper_power_of_two((long)this.surface_buffer.current_width);
        this.surface_buffer.current_qrbuffer_size_height = this.surface_buffer.current_qrbuffer_size_width;

        Log.i("qrsurf", "got buff size : "+this.surface_buffer.current_height + " pow2 : "+this.surface_buffer.current_qrbuffer_size_width);

        if (stat == 1){


                try {
                    Thread.sleep(2800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                destroy_current_encoder();
                for (int i = 0; i < this.surface_buffer.surfdata.capacity(); i++){
                    this.surface_buffer.surfdata.put(i, (byte)0xff);
                }
                //this.waiting_to_add_files = false;
                //this.should_display_anything = false;
                Log.i("PPP", "frame producer ended, destroying..");



                this.index_of_currently_processed_file++;
                if (this.index_of_currently_processed_file < this.files_to_send.size()){
                    init_and_set_external_file_info(files_to_send.get(this.index_of_currently_processed_file), "", 580, 0.5);
                    this.is_header_generating = true;
                    this.header_time_start_ns = System.nanoTime();
                    this.should_display_anything = true;
                }

        }

        return status;
    }


    static long upper_power_of_two(long v)
    {
        v--;
        v |= v >> 1;
        v |= v >> 2;
        v |= v >> 4;
        v |= v >> 8;
        v |= v >> 16;
        v++;
        return v;
    }

    //native part

    public static native int init_and_set_external_file_info(String filename, String filepath,
                                                             int suggested_qr_payload_length,
                                                             double suggested_err_fraction //NOTIMPL
                                                              );


    //returns status, if status=1, the frame sequence is done
    //ByteBuffer must have already allocated enough memory in advance (might have more than necessary)
    // this function populates the buffer
    // produced_width is the 4-byte buffer for holding the obtained width
    public static native int produce_next_qr_grayscale_image_to_mem(ByteBuffer produced_image, ByteBuffer produced_width);

    public static native int tell_no_more_generating_header();

    public static native int tell_how_much_frames_will_be_generated();

    public static native int destroy_current_encoder();

}
