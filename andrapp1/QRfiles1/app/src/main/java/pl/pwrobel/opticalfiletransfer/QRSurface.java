package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.provider.Settings;
import android.util.AttributeSet;
import android.util.Log;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileReader;
import java.nio.ByteBuffer;
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
        //Log.i("QRSurf", "constructor 1");
        mContext = context;
        this.init_qrsurf();
    }

    public QRSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        //Log.i("QRSurf", "constructor 2");
        mContext = context;
        this.init_qrsurf();
    }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        //Log.i("QRSurf", "surface created");

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
        //Log.i("QRSurf", "surface changed w"+width + " h "+height);
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
        if (!should_display){
            GLES20.glClearColor(1.0f, 1.0f, 1.0f, 0.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
            GLES20.glViewport(0, 0, this.surfw, this.surfh);

            synchronized (this) {
                this.is_frame_drawing = false;
                this.notifyAll();
            }

            return;
        }

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


        //Log.i("QRSurf", "ondrawframe");
        boolean shoulddisplay = true;
        synchronized (this) {
            this.is_frame_drawing = false;
            this.notifyAll();
        }
    }

    private boolean waiting_for_qrmanager_thread_to_finish = false;
    public void destroy_all_resources(){

        synchronized (this){
            //clear_ui();
        }

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


    private int pending_fps = 17;
    private int pending_errlevel = 50;
    private int pending_qrsize = 585;
    private int pending_header_timeout = 6;
    private int pending_suggested_N = 511;
    public synchronized void reset_producer(int fps, int err, int qrsize, int headertimeout, int suggested_N){

        this.pending_fps = fps;
        this.pending_errlevel = err;
        this.pending_qrsize = qrsize;
        this.pending_header_timeout = headertimeout;
        this.pending_suggested_N = suggested_N;

        if (this.waiting_to_add_files)
            return;



        //this.update_descriptions_in_views();


        synchronized (this){
            force_encoder_reset = true;
        }
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
                    setup_initial_brightness();
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
                    QRSurface.this.nframe_data_last_produced = -1;
                    QRSurface.this.total_number_of_dataframes_produced_by_the_encoder = 0;
                }
            }
        });
        qrsufr_manager_thread.start();
    };


    private boolean waiting_to_add_files = true;
    public void add_new_files_to_send(ArrayList<String> filespath){
        synchronized (this){
            this.index_of_currently_processed_file=-1;
            destroy_current_encoder();
            files_to_send.clear();
            this.index_of_currently_processed_file++;
            for (int i = 0; i < filespath.size(); i++){
                files_to_send.add(i, filespath.get(i));
            }
            //files_to_send.add(1, filespath.get(0));
            Log.i("qrsurf", "index : " + this.index_of_currently_processed_file+
                    ", file : " + files_to_send.get(this.index_of_currently_processed_file));
            init_and_set_external_file_info(
                    files_to_send.get(this.index_of_currently_processed_file), "",
                    this.pending_qrsize, this.pending_errlevel / 100.0, pending_suggested_N);
            this.setFPS(this.pending_fps);
            this.header_time_timeout_ns = this.pending_header_timeout * 1.0e9;
            this.continuous_status_display_update_is_over = false;
            this.header_time_start_ns = System.nanoTime();
            waiting_to_add_files = false;
            this.should_display_anything = true;
            this.is_header_generating = true;
            this.nframe_last_produced = -1;
            this.nframe_data_last_produced = -1;
            this.total_number_of_dataframes_produced_by_the_encoder = 0;
            this.time_ns_last_upload_progressbar_done = System.nanoTime();
            this.time_ns_header_initialized = System.nanoTime();

            this.update_descriptions_in_views();
        }
    }

    private boolean force_encoder_reset = false;
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

        if (this.waiting_to_add_files)
           return;

        double framewaitns = 1e9 / this.fps;
        double current_ns = System.nanoTime();

        if (current_ns - this.last_ns_time_frame_requested_for_display > framewaitns){
            last_ns_time_frame_requested_for_display = current_ns;
            //Log.i("qrsurf", "manager thread wants new frame");
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
                            //Log.i("PPP", "header stooops, TOT frames : "+ this.total_number_of_dataframes_produced_by_the_encoder);
                            this.description_status_1 = this.getStringResourceByName("upload_file_desc_string");
                        }
                    }

                    this.produce_new_qrdata_to_surf_buffer();

                    boolean forced_reset = false;
                    synchronized (this){
                        forced_reset = this.force_encoder_reset;
                    }
                    if (forced_reset){
                        synchronized (this){
                            this.force_encoder_reset = false;
                            this.waiting_to_add_files = true;


                            this.index_of_currently_processed_file=-1;

                            files_to_send.clear();

        //files_to_send.add(1, filespath.get(0));
        //Log.i("qrsurf", "index : " + this.index_of_currently_processed_file+
        //        ", file : " + files_to_send.get(this.index_of_currently_processed_file));
        //init_and_set_external_file_info(files_to_send.get(this.index_of_currently_processed_file), "", 580, 0.5);
                            this.continuous_status_display_update_is_over = false;
                            this.header_time_start_ns = System.nanoTime();
                            waiting_to_add_files = true;
                            this.should_display_anything = false;
                            this.is_header_generating = true;
                            this.nframe_last_produced = -1;
                            this.nframe_data_last_produced = -1;
                            this.total_number_of_dataframes_produced_by_the_encoder = 0;
                            this.time_ns_last_upload_progressbar_done = System.nanoTime();
                            this.time_ns_header_initialized = System.nanoTime();



                            destroy_current_encoder();
                            for (int i = 0; i < this.surface_buffer.surfdata.capacity(); i++){
                                this.surface_buffer.surfdata.put(i, (byte)0xff);
                            }
                        }
                        this.end_transmission();
                    }

                    QRSurface.this.requestRender();



                    synchronized (this){
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



    }

    private TextView encoder_status_textfield = null;
    private TextView encoder_status_textfield2 = null;
    public synchronized void setCustomTextViewStatus(TextView tv, TextView tv2){
        this.encoder_status_textfield = tv;
        this.encoder_status_textfield2 = tv2;

        this.clear_ui();
    }

    private void clear_ui(){
        Activity a = (Activity) this.getContext();
        if (a != null)
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if(encoder_status_textfield != null) {
                        encoder_status_textfield.setText(getStringResourceByName("ask_for_new_file_selection_to_upload"));
                        encoder_status_textfield.requestLayout();
                    }
                    if(encoder_status_textfield2 != null){
                        encoder_status_textfield2.setText(" ");
                        encoder_status_textfield2.setBackgroundResource(R.mipmap.down_arrow_icon);
                        ViewGroup.LayoutParams params = encoder_status_textfield2.getLayoutParams();
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        encoder_status_textfield2.setLayoutParams(params);
                        encoder_status_textfield2.requestLayout();
                    }
                }
            });
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
    private double time_ns_header_initialized = System.nanoTime();
    private double time_ns_interval_upload_updated = 1.0e8;
    private boolean is_header_generating = false;
    private int nframe_last_produced = -1;
    private int nframe_data_last_produced = -1;
    private int total_number_of_dataframes_produced_by_the_encoder = 0;
    private double header_time_start_ns = 0.0;
    private double header_time_timeout_ns = 7e9;//7s

    private String timeoutbar_descr_string = "";
    private String description_status_1 = "";
    private String file_trimmed_text = "";
    //returns status, 1=end
    private boolean continuous_status_display_update_is_over = true;
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
        if (!this.is_header_generating)
            this.nframe_data_last_produced++;
        this.surface_buffer.current_width = this.surface_buffer.produced_width_buffer.asIntBuffer().get(0);
        this.surface_buffer.current_height = this.surface_buffer.current_width;

        this.surface_buffer.current_qrbuffer_size_width = (int)upper_power_of_two((long)this.surface_buffer.current_width);
        this.surface_buffer.current_qrbuffer_size_height = this.surface_buffer.current_qrbuffer_size_width;

        //Log.i("qrsurf", "got buff size : "+this.surface_buffer.current_height + " pow2 : "+this.surface_buffer.current_qrbuffer_size_width);

        double currt = System.nanoTime();
        if (this.total_number_of_dataframes_produced_by_the_encoder > 0)
        if (currt - this.time_ns_last_upload_progressbar_done > this.time_ns_interval_upload_updated
                || this.nframe_last_produced == this.total_number_of_dataframes_produced_by_the_encoder-1){
            //Log.i("qrsurf", "will draw progressbarsurf");
            final double progr = ((double) this.nframe_data_last_produced) / ((double)this.total_number_of_dataframes_produced_by_the_encoder-1);
            Activity a = (Activity) this.getContext();
            if (a != null && !this.continuous_status_display_update_is_over)
                a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                        //QRSurface.this.encoder_progressbar.setFileName(files_to_send.get(QRSurface.this.index_of_currently_processed_file));
                    QRSurface.this.encoder_progressbar.drawMe((long)(1000 * progr), CustomProgressBar.progressBarType.PROGRESS, true);
                    if(QRSurface.this.encoder_status_textfield != null)
                        QRSurface.this.encoder_status_textfield.setText(QRSurface.this.description_status_1);
                    if(QRSurface.this.encoder_status_textfield2 != null){
                        QRSurface.this.encoder_status_textfield2.setText(QRSurface.this.file_trimmed_text);
                        encoder_status_textfield2.setBackgroundResource(0);
                        ViewGroup.LayoutParams params = encoder_status_textfield2.getLayoutParams();
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        encoder_status_textfield2.setLayoutParams(params);
                    }
                }
            });

            this.time_ns_last_upload_progressbar_done = currt;
        }

        currt = System.nanoTime();
        if (total_number_of_dataframes_produced_by_the_encoder <= 0 &&
                currt - this.time_ns_last_upload_progressbar_done > this.time_ns_interval_upload_updated)
        {
            /// do the header progressbar update
            Activity a = (Activity) this.getContext();
            double pr = (currt - this.time_ns_header_initialized) / this.header_time_timeout_ns;
            if (pr > 1.0)
                pr = 1.0;
            final double progr = pr;
            if (a != null)
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        QRSurface.this.encoder_progressbar.setFileName(timeoutbar_descr_string+" ");
                        QRSurface.this.encoder_progressbar.setTimeLimitForDrawingTimeout(QRSurface.this.header_time_timeout_ns/1.0e9);
                        QRSurface.this.encoder_progressbar.drawMe((long)(1000 * progr), CustomProgressBar.progressBarType.TIMEOUT, true);
                        if(QRSurface.this.encoder_status_textfield != null)
                            QRSurface.this.encoder_status_textfield.setText(QRSurface.this.description_status_1);
                        if(QRSurface.this.encoder_status_textfield2 != null){
                            QRSurface.this.encoder_status_textfield2.setText(QRSurface.this.file_trimmed_text);
                            encoder_status_textfield2.setBackgroundResource(0);
                            ViewGroup.LayoutParams params = encoder_status_textfield2.getLayoutParams();
                            params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                            encoder_status_textfield2.setLayoutParams(params);
                            encoder_status_textfield2.requestLayout();
                        }
                    }
                });
            this.time_ns_last_upload_progressbar_done = currt;
        }



        if (stat == 1){
                final Activity a = (Activity) this.getContext();
                if (a != null)
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(QRSurface.this.encoder_status_textfield != null)
                                QRSurface.this.encoder_status_textfield.setText(getStringResourceByName("upload_file_finalizing_msg"));
                        }
                    });

                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                destroy_current_encoder();
                for (int i = 0; i < this.surface_buffer.surfdata.capacity(); i++){
                    this.surface_buffer.surfdata.put(i, (byte)0xff);
                }

                //synchronized (this){
                    //this.should_display_anything = false;
                 //   this.is_frame_drawing = true;
               // }

                synchronized (this){
                    this.should_display_anything = false;
                    this.is_frame_drawing = true;
                    QRSurface.this.requestRender();
                    while (this.is_frame_drawing) {
                        try {
                            this.wait(1000);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    //this.should_display_anything = true;
                }
                //synchronized (this){
                //    this.should_display_anything = true;
                //}
                //this.waiting_to_add_files = false;
                //this.should_display_anything = false;
                //Log.i("PPP", "frame producer ended, destroying..");



                this.index_of_currently_processed_file++;
                if (this.index_of_currently_processed_file < this.files_to_send.size()){
                    init_and_set_external_file_info(
                            files_to_send.get(this.index_of_currently_processed_file), "",
                            this.pending_qrsize, this.pending_errlevel / 100.0, pending_suggested_N);
                    this.setFPS(this.pending_fps);
                    this.header_time_timeout_ns = this.pending_header_timeout * 1.0e9;
                    this.continuous_status_display_update_is_over = false;
                    this.is_header_generating = true;
                    this.header_time_start_ns = System.nanoTime();
                    this.should_display_anything = true;
                    this.time_ns_last_upload_progressbar_done = System.nanoTime();
                    this.time_ns_header_initialized = System.nanoTime();

                    this.update_descriptions_in_views();
                }else{
                    this.end_transmission();
                    waiting_to_add_files = true;
                }

        }

        return status;
    }

    private void end_transmission(){
        final Activity a = (Activity) this.getContext();
        if (a != null)
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    set_brightness_back_to_auto();
                }
            });
        continuous_status_display_update_is_over = true;


        if (a != null){
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    set_brightness_back_to_auto();
                    if (encoder_progressbar != null) {
                        encoder_progressbar.setVisibility(INVISIBLE);
                        encoder_progressbar.requestLayout();
                    }
                    if(encoder_status_textfield != null) {
                        encoder_status_textfield.setText(getStringResourceByName("ask_for_new_file_selection_to_upload"));
                        encoder_status_textfield.requestLayout();
                    }
                    if(encoder_status_textfield2 != null){
                        encoder_status_textfield2.setText(" ");
                        //encoder_status_textfield2.setBackgroundColor(Color.argb(0x7f, 0xff, 0xff, 0xff));
                        //encoder_status_textfield2.getBackground().setAlpha(100);
                        encoder_status_textfield2.setBackgroundResource(R.mipmap.down_arrow_icon);
                        ViewGroup.LayoutParams params = encoder_status_textfield2.getLayoutParams();
                        params.width = ViewGroup.LayoutParams.WRAP_CONTENT;
                        encoder_status_textfield2.setLayoutParams(params);
                        encoder_status_textfield2.requestLayout();
                    }

                }
            });
        }
    }

    private int lastbrihtnesslevel = 127;
    private boolean automax_already_set = false;

    public void setup_initial_brightness(){
        if (automax_already_set)
            return;
        Activity aa = (Activity) getContext();
        if (aa != null){
            Settings.System.putInt(aa.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            int currbrightnessValue = Settings.System.getInt(
                    aa.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    0
            );
            if (currbrightnessValue >= 0)
                this.lastbrihtnesslevel = currbrightnessValue;

        }
        //Log.i("QQQQQQ", "set initial brightness "+ this.lastbrihtnesslevel);
    }

    public void set_brightness_manual_max(){
        if (automax_already_set)
            return;
        Activity aa = (Activity) getContext();
        if (aa != null){



            Settings.System.putInt(aa.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);

            int currbrightnessValue = Settings.System.getInt(
                    aa.getContentResolver(),
                    Settings.System.SCREEN_BRIGHTNESS,
                    0
            );

            if (currbrightnessValue >= 0)
                this.lastbrihtnesslevel = currbrightnessValue;

            android.provider.Settings.System.putInt(getContext().getContentResolver(),
                android.provider.Settings.System.SCREEN_BRIGHTNESS, 255);
            this.automax_already_set = true;
        }
        //Log.i("QQQQQ", "Set bright max "+this.automax_already_set+ "  act "+aa);
    }

    public void set_brightness_back_to_auto(){
        Activity aa = (Activity) getContext();
        //Log.i("QQQQQQQ", "BR to auto val "+this.lastbrihtnesslevel + " activ "+aa);
        if (aa != null){
            Settings.System.putInt(aa.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            android.provider.Settings.System.putInt(getContext().getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS, this.lastbrihtnesslevel);
            this.automax_already_set = false;
        }
    }

    private void update_descriptions_in_views(){
        final Activity a = (Activity) this.getContext();
        if (a != null)
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    //Log.i("BR", "Setting the brightness to full");
/*
                    WindowManager.LayoutParams lp = a.getWindow().getAttributes();
                    lp.screenBrightness =  BRIGHTNESS_OVERRIDE_FULL;
                    a.getWindow().setAttributes(lp);
*/

                    set_brightness_manual_max();

                    if (encoder_progressbar != null) {
                        encoder_progressbar.setVisibility(VISIBLE);
                        encoder_progressbar.requestLayout();
                    }
                }
            });
        this.timeoutbar_descr_string = this.getStringResourceByName("start_sequence_string");
        this.description_status_1 = this.getStringResourceByName("start_sequence_upload_file_desc");
        this.file_trimmed_text = trimFileNameText(files_to_send.get(QRSurface.this.index_of_currently_processed_file));
    }

    public synchronized void reset_settins(int fps, int errlev, int qrsize){

    }

    private String getStringResourceByName(String aString) {
        Activity a = (Activity) this.getContext();
        if (a == null)
            return null;
        String packageName = a.getPackageName();
        int resId = a.getResources()
                .getIdentifier(aString, "string", packageName);
        if (resId == 0) {
            return aString;
        } else {
            return a.getString(resId);
        }
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

    static String trimFileNameText(String filepath){
        int index = filepath.lastIndexOf("/");
        return filepath.substring(index + 1);
    }

    public static boolean checkFileCanRead(File file){
        if (!file.exists())
            return false;
        if (!file.canRead())
            return false;
        try {
            FileReader fileReader = new FileReader(file.getAbsolutePath());
            fileReader.read();
            fileReader.close();
        } catch (Exception e) {
            Log.i("ERR", "Exception when checked file can read with message:"+e.getMessage());
            return false;
        }
        return true;
    }

    //native part

    public static native int init_and_set_external_file_info(String filename, String filepath,
                                                             int suggested_qr_payload_length,
                                                             double suggested_err_fraction,
                                                             double suggested_N);


    //returns status, if status=1, the frame sequence is done
    //ByteBuffer must have already allocated enough memory in advance (might have more than necessary)
    // this function populates the buffer
    // produced_width is the 4-byte buffer for holding the obtained width
    public static native int produce_next_qr_grayscale_image_to_mem(ByteBuffer produced_image, ByteBuffer produced_width);

    public static native int tell_no_more_generating_header();

    public static native int tell_how_much_frames_will_be_generated();

    public static native int destroy_current_encoder();

}
