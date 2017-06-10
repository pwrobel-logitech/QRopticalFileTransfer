package qrfiles.pwrobel.myapplication;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
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
import android.util.Pair;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.RunnableFuture;

/**
 * Created by pwrobel on 29.04.17.
 */

public class CameraWorker extends HandlerThread implements CameraController, Camera.PreviewCallback{

    public volatile Handler handler;
    public Camera camera;
    private int camwidth, camheight;
    public CameraPreviewSurface camsurf;
    Context context;
    byte[] callbackbuffer;
    byte[] greyscalebuffer;
    boolean camera_initialized = false;


    //below, fields regarding the estimation of the moment after we know for sure the detector has ended
    //even if the few last frames are missing
    //we estimate based on the moment of first dataframe that arrived and the last dataframe so far
    //we know how many frame to expect to, so simply interpolate the time the last frame is supposed
    //to arrive (with some small delay added)
    boolean is_first_frame_arrived = false;
    int first_frame_num_arrived;
    long time_first_frame_arrived;
    boolean is_last_frame_so_far_arrived = false;
    int last_frame_number_arrived_so_far = -1; // must be greater than the first frame number
    long time_last_frame_number_arrived_so_far; //only used when first and last frame so far arrived
    double current_estimated_moment_of_end;
    static long time_overhead = 300000000; // 180ms in nanosecs as a time unit. From System.nanoTime()
    boolean triggered_autoestimated_end = false;
    boolean tiggered_lastframedetectedbase_end = false;
    //end of the fields for the estimation of the end of the detection


    //start of the fields responsible for the proper detector progress visualization
    static int MAX_CHUNK_LENGTH = 4096 * 4;
    static int MAX_LAST_FR_LEN = 25; // for generating current preview color
    boolean [] succesfull_last_smallpos;
    //static double seconds_window_for_estimation_succ_ratio = 2.0; //2s
    double success_ratio_in_smallpos = 0;
    boolean [] succesfull_positions_in_current_chunk;
    boolean [] succesfull_positions_in_prev_chunk;
    int biggest_frame_number = -1;
    int last_frame_number = 0;
    int total_frame_number = -1;
    int RSn, RSk, RSn_res, RSk_res;
    boolean RS_info_set = false;
    ///
    Deque<Pair<Integer, Long>> lastframeswithtime; //queue of the frame number and the recorded time
    double estimated_max_framerate = 0; //what is the current decoding speed
    //end of the decoder visualization fields

    ////contains file transfer status - whether hash checking etc. produced good file or not
    boolean file_detection_ended = false;
    boolean file_detected_and_finally_saved_successfully = false;
    String last_received_file_name = "";
    ///


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
        lastframeswithtime = new ArrayDeque<Pair<Integer, Long>>(0);
    }


    public void update_decoder_statistic(int newfrnum){
        boolean is_residual = false;
        int nmainchunks = total_frame_number / RSn;

        is_residual = (newfrnum >= (total_frame_number - RSn_res));

        if(newfrnum % RSn == 0){
            for(int i = 0; i < RSn; i++)
                succesfull_positions_in_current_chunk[i] = false;
        }

        if (newfrnum != last_frame_number)
            this.lastframeswithtime.addLast(new Pair<Integer, Long>(newfrnum, System.nanoTime()));
        if (this.lastframeswithtime.size() > MAX_LAST_FR_LEN)
            this.lastframeswithtime.removeFirst();


        //now, estiate the framerate
        double framerate = 0;
        if (this.lastframeswithtime.size() > 1 &&
                (this.lastframeswithtime.getLast().first - this.lastframeswithtime.getFirst().first > 5)){
            framerate = ((double)(this.lastframeswithtime.getLast().first - this.lastframeswithtime.getFirst().first))
                    / ((double)(this.lastframeswithtime.getLast().second - this.lastframeswithtime.getFirst().second));
            framerate *= 1e9;

            estimated_max_framerate = framerate;
        }

        if (framerate < 1e-10) { //if still close to 0 (not calculated) - dimnish the frame window
            if (this.lastframeswithtime.size() > 1 &&
                    (this.lastframeswithtime.getLast().first - this.lastframeswithtime.getFirst().first > 2)){
                framerate = ((double)(this.lastframeswithtime.getLast().first - this.lastframeswithtime.getFirst().first))
                        / ((double)(this.lastframeswithtime.getLast().second - this.lastframeswithtime.getFirst().second));
                framerate *= 1e9;

                estimated_max_framerate = framerate;
            }
        }

/*
        int smallpos = newfrnum;
        if (newfrnum >= MAX_LAST_FR_LEN)
            smallpos = MAX_LAST_FR_LEN - 1;

        int smallposold = this.last_frame_number;
        if (smallposold >= MAX_LAST_FR_LEN)
            smallposold = MAX_LAST_FR_LEN - 1;

        int diff = newfrnum - this.last_frame_number;
        if (diff >= MAX_LAST_FR_LEN){
            for(int j = 0; j < MAX_LAST_FR_LEN-1; j++)
                this.succesfull_last_smallpos[j]=false;
        }

        if(smallpos < MAX_LAST_FR_LEN - 1){
            for (int i = last_frame_number+1; i < smallpos; i++)
                this.succesfull_last_smallpos[i] = false;
            this.succesfull_last_smallpos[smallpos] = true;
        }else if (diff < MAX_LAST_FR_LEN){
            for(int i = 0; i <MAX_LAST_FR_LEN - diff; i++){
                int np = i + diff;
                if(np > 0 && np < MAX_LAST_FR_LEN)
                    this.succesfull_last_smallpos[i] = this.succesfull_last_smallpos[np];
            }
            for(int j = smallposold-diff+1; j < smallpos; j++)
                this.succesfull_last_smallpos[j]=false;
            this.succesfull_last_smallpos[MAX_LAST_FR_LEN-1] = true;
        }



*/
/*
        int sum = 0;
        //for(Iterator itr = this.lastframeswithtime.iterator(); itr.hasNext();)  {
        //    Pair<Integer, Long> p = (Pair<Integer, Long>) itr.next();
        //}


        if(this.lastframeswithtime.size() > 1)
            success_ratio_in_smallpos = ((double)this.lastframeswithtime.size()) /
                ((double)(this.lastframeswithtime.getLast().first-this.lastframeswithtime.getFirst().first+1));

        Log.i("SmallSuccRatio", "Succ coeff : "+success_ratio_in_smallpos + " Framerate "+this.estimated_max_framerate);
*/
        this.last_frame_number = newfrnum;

    }

    public void estimate_success_ratio_at_current_time(){
        double succratio = 0;

        double currtime = (double)System.nanoTime() * 1e-9;
        double seconds_window_for_estimation_succ_ratio = MAX_LAST_FR_LEN / this.estimated_max_framerate;

        //find all the frames that are not further back in time than the current, 2s probation window
        int numfr_in_timewindow = 0;
        for(Iterator itr = this.lastframeswithtime.iterator(); itr.hasNext();)  {
            Pair<Integer, Long> p = (Pair<Integer, Long>) itr.next();
            if ((currtime - (double)(p.second) * 1e-9) < seconds_window_for_estimation_succ_ratio){
                numfr_in_timewindow++;
            }
        }
        double num_expected_in_timewindow_based_on_curr_framerate =
                seconds_window_for_estimation_succ_ratio * this.estimated_max_framerate;

        succratio = ((double) numfr_in_timewindow) / num_expected_in_timewindow_based_on_curr_framerate;

        if (succratio > 1.0)
            succratio = 1.0;

        synchronized (this){
            success_ratio_in_smallpos = succratio;
        }
        Log.i("SUC", "success ratio : " + success_ratio_in_smallpos + " fps "+this.estimated_max_framerate);
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Log.i("thr", "executed on thread id: " + android.os.Process.myTid());
        //Log.i("info", "Got byte arrQ of size "+data.length+" bytes");

        applyGrayScale(greyscalebuffer, data, camwidth, camheight);

        final byte[] dat = data;
        Activity a = (Activity) context;


        final int status = send_next_grayscale_buffer_to_decoder(greyscalebuffer, camwidth, camheight);

        int ntot = get_total_frames_of_data_that_will_be_produced();
        int lf = get_last_number_of_frame_detected();
        int hfn = get_last_number_of_header_frame_detected();

        if (status == 10){ //got header in the middle of data detection
            Log.i("RST HDR", "Got header in the middle of data detection");
            this.reset_decoder();
        }

        if (ntot > 0) {
            this.total_frame_number = ntot;
            this.last_frame_number_arrived_so_far = lf;

            int rsNM = get_main_RSN();
            int rsKM = get_main_RSK();
            int rsNR = get_residual_RSN();
            int rsKR = get_residual_RSK();
            if (rsNM > 0 && rsKM > 0 && rsNR > 0 && rsKR > 0 && (!this.RS_info_set)){
                this.RSn = rsNM;
                this.RSk = rsKM;
                this.RSn_res = rsNR;
                this.RSk_res = rsKR;
                this.success_ratio_in_smallpos = ((double) RSk) / ((double) RSn);
                this.RS_info_set = true;
            }
            if (lf > -1) {
                if (status > 0)
                    this.update_decoder_statistic(lf);
                if (this.estimated_max_framerate > 1e-10)
                    this.estimate_success_ratio_at_current_time();
            }
        }


/*
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
*/

        if(ntot > 0 && lf > 0)
            if(lf >= ntot - 1)
                if(!triggered_autoestimated_end){
                    this.reset_decoder();
                    tiggered_lastframedetectedbase_end = true;
            }

        if(status > 0)
            Log.i("APIINFO", "Totalframes : " + get_total_frames_of_data_that_will_be_produced()+
                  " lashHdrProducedN "+get_last_number_of_header_frame_detected() +
                  " lastDatFrProducedN "+get_last_number_of_frame_detected() +
                    " RSM(" + get_main_RSN() +","+get_main_RSK()+");RSR("+get_residual_RSN()+","+get_residual_RSK()+") "+
                    " laststatus "+status);

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

    public void initCamAsync(final int surfacew, final int surfaceh){
        handler.post(new Runnable() {
            @Override
            public void run() {

                int sw = surfacew;
                int sh = surfaceh;

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

                android.hardware.Camera.CameraInfo info =
                        new android.hardware.Camera.CameraInfo();
                android.hardware.Camera.getCameraInfo(0, info);

                int bestsizeindex = CameraWorker.this.select_best_preview_size_index(psize, sw, sh, info.orientation);

                int camwidth = psize.get(bestsizeindex).width;
                int camheight = psize.get(bestsizeindex).height;
                CameraWorker.this.camwidth = camwidth;
                CameraWorker.this.camheight = camheight;

                Log.i("camworker", "Preview w "+camwidth + " h " + camheight);

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

                CameraWorker.this.succesfull_positions_in_prev_chunk = new boolean[MAX_CHUNK_LENGTH];
                CameraWorker.this.succesfull_positions_in_current_chunk = new boolean[MAX_CHUNK_LENGTH];
                CameraWorker.this.succesfull_last_smallpos = new boolean[MAX_LAST_FR_LEN];

                for (int i = 0; i<MAX_CHUNK_LENGTH; i++){
                    CameraWorker.this.succesfull_positions_in_current_chunk[i] = false;
                    CameraWorker.this.succesfull_positions_in_prev_chunk[i] = false;
                }

                for (int i = 0; i<MAX_LAST_FR_LEN; i++)
                    CameraWorker.this.succesfull_last_smallpos[i] = false;

                System.gc();

                synchronized (CameraWorker.this) {
                    camera_initialized = true;
                    CameraWorker.this.notifyAll();
                }
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

                CameraWorker.this.succesfull_positions_in_prev_chunk = null;
                CameraWorker.this.succesfull_positions_in_current_chunk = null;
                CameraWorker.this.succesfull_last_smallpos = null;

                biggest_frame_number = -1;
                total_frame_number = -1;

                CameraWorker.this.RS_info_set = false;

                System.gc();

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

    public int select_best_preview_size_index(List<Camera.Size> psize, int sw, int sh, int orientation){
        boolean is_surface_portrait = (sh > sw);
        int bestindex = 0;
        List<Camera.Size> l = new ArrayList<Camera.Size>(psize);

        Collections.sort(l, new Comparator<Camera.Size>() {
            @Override
            public int compare(Camera.Size o1, Camera.Size o2) {
                int bo1, bo2;
                if (o1.width > o1.height){
                    bo1 = o1.width;
                }else{
                    bo1 = o1.height;
                }
                if (o2.width > o2.height){
                    bo2 = o2.width;
                }else{
                    bo2 = o2.height;
                }
                if (bo1 > bo2){
                    return 1;
                }else{
                    return -1;
                }

                //return 0;
            }
        });


        int found_sorted_index = 0;
        for (int i = 0; i < l.size(); i++) {
            Camera.Size size = l.get(i);

            float a=0,b=0; //rotated parameters - a = height, b=width in the surface frame reference
            if(orientation % 180 == 0){
                a = size.height;
                b = size.width;
            }else{
                a = size.width;
                b = size.height;
            }

            boolean is_prev_size_portrait = (a > b);
            if (is_surface_portrait != is_prev_size_portrait)
                continue;

            Log.i("CamPrevSize", "size w "+ size.width + "; size h "+size.height);
            int bigger_size, smaller_size;
            if (size.width > size.height){
                bigger_size = size.width;
                smaller_size = size.height;
            } else {
                bigger_size = size.height;
                smaller_size = size.width;
            }
            if (bigger_size >= 640){
                found_sorted_index = i;
                break;
            }
        }

        //backup mode - no matching portraits mode between preview sizes and surface proportion found
        if (found_sorted_index == 0)
            for (int i = 0; i < l.size(); i++) {
                Camera.Size size = l.get(i);
                Log.i("CamPrevSize", "size w "+ size.width + "; size h "+size.height);
                int bigger_size, smaller_size;
                if (size.width > size.height){
                    bigger_size = size.width;
                    smaller_size = size.height;
                } else {
                    bigger_size = size.height;
                    smaller_size = size.width;
                }
                if (bigger_size >= 640){
                    found_sorted_index = i;
                    break;
                }
            }

        //last resort
        if(found_sorted_index == 0)//have not found
            found_sorted_index = l.size() - 1;

        //find the selected index in the unsorted array
        Camera.Size founds = l.get(found_sorted_index);
        int index_found_in_unsorted = 0;
        for (int j = 0; j < psize.size(); j++){
            Camera.Size size = psize.get(j);
            if(size.width == founds.width && size.height == founds.height){
                index_found_in_unsorted = j;
                break;
            }
        }
        bestindex = index_found_in_unsorted;
        return bestindex;
    }

    public synchronized int getCamPreviewWidth(){
        return camwidth;
    };

    public synchronized int getCamPreviewHeight(){
        return camheight;
    }

    @Override
    public boolean isCameraInitialized() {
        return camera_initialized;
    }

    @Override
    public synchronized double getCurrentSuccRatio() {
        double succ = 0.5;
        if (this.estimated_max_framerate > 1e-9)
            succ = this.success_ratio_in_smallpos;
        return succ;
    }

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

    public void file_status_delivered(String filename){ //either failed or succeeded
        Log.i("FILESTATUS", "got file status, success : "+this.file_detected_and_finally_saved_successfully +
        " Filename : " + filename);
    }

    public void reset_decoder(){
        Log.i("RST", "reset of the decoder is detected");
        this.estimated_max_framerate = 0;
        this.lastframeswithtime.clear();
        last_frame_number_arrived_so_far = -1;
        tell_decoder_no_more_qr();
        int stat = deinitialize_decoder();
        Log.i("RST", "stat value "+stat);
        this.file_detection_ended = true;
        if(stat == 3)
            this.file_detected_and_finally_saved_successfully = false;
        else if (stat == 7)
            this.file_detected_and_finally_saved_successfully = true;
        this.file_status_delivered(this.last_received_file_name);
        initialize_decoder();
        set_decoded_file_path("/mnt/sdcard/out");
        this.file_detection_ended = false;
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

    public static native int get_main_RSN();
    public static native int get_main_RSK();
    public static native int get_residual_RSN();
    public static native int get_residual_RSK();

    public static native int deinitialize_decoder();

}
