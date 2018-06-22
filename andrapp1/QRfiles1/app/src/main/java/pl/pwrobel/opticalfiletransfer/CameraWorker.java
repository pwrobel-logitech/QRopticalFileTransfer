package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;

import android.os.Process;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URLConnection;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Created by pwrobel on 29.04.17.
 */

public class CameraWorker extends HandlerThread implements CameraController, Camera.PreviewCallback,
        SizeExceededDialogDismisser{


    public volatile Handler handler;
    public Camera camera;
    private int camwidth, camheight;
    public CameraPreviewSurface camsurf;
    Context context;
    byte[] callbackbuffer;
    //byte[] greyscalebuffer;
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


    //after this time of has passed from the last received frame, the decoder is restarted
    private double last_time_some_frame_received = System.nanoTime();
    private boolean death_clock_ticking = false;
    private double ns_death_timeout = 1.0e10;

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

        this.str_encourage_new_transmission = this.getStringResourceByName("draw_blink_encourage_new_transmission");
        this.str_started_header_detection = this.getStringResourceByName("draw_blink_started_header_detection");

        this.str_missed_to_detect_header = this.getStringResourceByName("draw_blink_warning_header_not_detected");

        this.str_detected_file1 = this.getStringResourceByName("draw_blink_detected_file1");
        this.str_detected_file2 = this.getStringResourceByName("draw_blink_detected_file2");

        this.str_failed_header_detection = this.getStringResourceByName("draw_blink_failed_header_detection");
        this.str_failed_data_detection = this.getStringResourceByName("draw_blink_failed_data_detection");

        this.str_pending_async_text = this.getStringResourceByName("draw_blink_pending_async");
        this.str_succeeded_data_detection = this.getStringResourceByName("draw_blink_succeeded_data_detection");

        camsurf.setOnTouchListener(camsurf);
    }

    public CameraWorker(String name) {
        super(name, Process.THREAD_PRIORITY_URGENT_DISPLAY);
        context = null;
        camsurf = null;
        lastframeswithtime = new ArrayDeque<Pair<Integer, Long>>(0);
    }


    private int last_header_frame_delivered = 0;

    private void update_header_statistic(int nheader_delivered){
        if (nheader_delivered != this.last_header_frame_delivered)
            this.lastframeswithtime.addLast(new Pair<Integer, Long>(nheader_delivered, System.nanoTime()));
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

        this.last_header_frame_delivered = nheader_delivered;
    }


    private boolean is_residual = false;
    private int last_chunk_number = 0;
    private int current_chunk_number = 0;
    private int nchunks = 0;
    public synchronized void update_decoder_statistic(int newfrnum){
        if (RSn == 0)
            return;
        int nmainchunks = total_frame_number / RSn;
        this.nchunks = nmainchunks;
        if (total_frame_number % RSn != 0)
            this.nchunks++;


        is_residual = (newfrnum >= (total_frame_number - RSn_res));


        int curr_chunk = newfrnum / RSn;
        this.current_chunk_number = curr_chunk;

        if(curr_chunk != this.last_chunk_number){
            for(int i = 0; i < RSn; i++){
                succesfull_positions_in_prev_chunk[i] = succesfull_positions_in_current_chunk[i];
                succesfull_positions_in_current_chunk[i] = false;
            }
        }


        int curr_RSn = RSn;
        if (is_residual)
            curr_RSn = RSn_res;

        int nfn = newfrnum;
        if (is_residual && RSn > 0)
            nfn -= (nfn / RSn)*RSn;

        this.succesfull_positions_in_current_chunk[nfn % curr_RSn] = true;


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
        this.last_chunk_number = curr_chunk;

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
        //Log.i("SUC", "success ratio : " + success_ratio_in_smallpos + " fps "+this.estimated_max_framerate);
    }

    private boolean size_exceeded_dialog_shown = false;
    @Override
    public void onSetSizeExceededDialogGone() {
        synchronized (this){
            size_exceeded_dialog_shown = false;
        }
    }


    private byte[] auxillary_greyscale_buff = null;
    private int ncores = 1;
    private int nth_every_pixel = 1; // used to scale


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //synchronized (this){
        //    if(!camera_initialized)
        //        return;
        //}
        //Log.i("thr", "executed on thread id: " + android.os.Process.myTid());
        //Log.i("info", "Got byte arrQ of size "+data.length+" bytes");

        //applyGrayScale(greyscalebuffer, data, camwidth, camheight);

        //final byte[] dat = data;
        Activity a = (Activity) context;

        synchronized (CameraWorker.this){
            if (folder_reset_pending){
                folder_reset_pending = false;
                reset_decoder();
                if (camera != null){
                    try {
                        camera.addCallbackBuffer(callbackbuffer);
                    }catch (Exception e){
                        Log.e("camworker", "addcallbackBuffer failure in onPreviewFrame");
                    }
                }
            }
        }

        //greyscalebuffer = data;

        if (auxillary_greyscale_buff == null)
            auxillary_greyscale_buff = new byte[data.length];
        if (auxillary_greyscale_buff.length != data.length)
            auxillary_greyscale_buff = new byte[data.length];
        if (Math.max(camwidth, camheight) > 1900.0){
            nth_every_pixel = 2;
        }else{
            nth_every_pixel = 1;
        }
        final int status = send_next_grayscale_buffer_to_decoder(data, camwidth, camheight, ncores, auxillary_greyscale_buff, nth_every_pixel);

        //greyscalebuffer = null;
        int ntot = get_total_frames_of_data_that_will_be_produced();
        int lf = get_last_number_of_frame_detected();
        int hfn = get_last_number_of_header_frame_detected();

        double currt = System.nanoTime();

        if (this.death_clock_ticking && (currt - this.last_time_some_frame_received > this.ns_death_timeout)){
            //Log.i("RST ERR", "No frames for the past " + this.ns_death_timeout+" nanosec - resetting decoder");
            this.death_clock_ticking = false;
            this.reset_decoder();
            this.last_time_some_frame_received = System.nanoTime();
            if (camera != null){
                try {
                    camera.addCallbackBuffer(callbackbuffer);
                }catch (Exception e){
                    Log.e("camworker", "addcallbackBuffer failure in onPreviewFrame");
                }
            }
            return;
        }

        if (status == 6 || status == 2) { //some frame has been detected, restart decoder death clock
            last_time_some_frame_received = System.nanoTime();
            death_clock_ticking = true;
        }

        if (status == 4) { //got some async error
            synchronized (this){
                this.got_chunkRS_decode_error = true;
                this.last_filename_detected_from_header = get_last_recognized_file_name_str();
            }
            //Log.i("RST ERR", "Got unrecoverable chunk - error");
            this.reset_decoder();
            synchronized (this) {
                this.error_time_arrival_ms = System.nanoTime() / 1.0e6;
                this.should_deliver_error_info_for_certain_time = true;
            }
            if (camera != null){
                try {
                    camera.addCallbackBuffer(callbackbuffer);
                }catch (Exception e){
                    Log.e("camworker", "addcallbackBuffer failure in onPreviewFrame");
                }
            }
            return;
        }

        if (status == 10){ //got header in the middle of data detection
            //Log.i("RST HDR", "Got header in the middle of data detection");
            this.reset_decoder();
            if (camera != null){
                try {
                    camera.addCallbackBuffer(callbackbuffer);
                }catch (Exception e){
                    Log.e("camworker", "addcallbackBuffer failure in onPreviewFrame");
                }
            }
            return;
        }

        if (status == 5){
            synchronized (this){
                 this.got_dataframe_before_header_detection = true;
                 this.last_time_the_first_dataframe_arrived_without_headerframe = System.nanoTime()/1.0e6;
            }
        }

        if (/*status == 6 ||*/ status == 2){
            synchronized (this){
                this.is_header_detected = true;
                this.got_dataframe_before_header_detection = false;
                // size restriction that the receiver can pick up
                this.filesize_carried_in_the_detected_header = get_last_recognized_file_size();
                if (this.filesize_carried_in_the_detected_header >=0 &&
                        this.filesize_carried_in_the_detected_header > Qrfiles.limit_max_received_file_size){
                    final Activity ac = (Activity) context;
                    if (ac != null){
                        boolean should_show_dialog = false;
                        synchronized (CameraWorker.this){
                            should_show_dialog = !size_exceeded_dialog_shown;
                        }
                        if (should_show_dialog)
                            ac.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (ac != null){
                                        android.app.FragmentManager fm = ac.getFragmentManager();
                                        SizeExceededDialog d = new SizeExceededDialog();
                                        d.setDismisserListener(CameraWorker.this);
                                        synchronized (CameraWorker.this){
                                            size_exceeded_dialog_shown = true;
                                        }
                                        d.show(fm, "dialog");
                                    }
                                }});
                        this.reset_decoder();
                        if (camera != null){
                            try {
                                camera.addCallbackBuffer(callbackbuffer);
                            }catch (Exception e){
                                Log.e("camworker", "addcallbackBuffer failure in onPreviewFrame");
                            }
                        }
                        return;
                    }
                }
            }
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
                this.lastframeswithtime.clear();
            }
            if (lf > -1) {
                if (status > 0)
                    this.update_decoder_statistic(lf);
                if (this.estimated_max_framerate > 1e-10)
                    this.estimate_success_ratio_at_current_time();
                synchronized (this) {
                    this.should_draw_progressbars = true;
                    this.got_dataframe_before_header_detection = false;
                }
            }
        }


        //Log.i("QQ2FPS", " lf "+ lf + " , hfn "+hfn+", ntot "+ntot);
        if ((ntot == -1) && (hfn >= 0)) {
            synchronized (this){
                this.did_any_header_frame_arrived = true;
            }
            this.update_header_statistic(hfn);
            if (this.estimated_max_framerate > 1e-10){
                //Log.i("QQFPS", " FPS = "+this.estimated_max_framerate);
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

        if(status > 0)
            //Log.i("APIINFO", "Totalframes : " + get_total_frames_of_data_that_will_be_produced()+
            //        " lashHdrProducedN "+get_last_number_of_header_frame_detected() +
            //        " lastDatFrProducedN "+get_last_number_of_frame_detected() +
            //        " RSM(" + get_main_RSN() +","+get_main_RSK()+");RSR("+get_residual_RSN()+","+get_residual_RSK()+") "+
            //        " laststatus "+status);

        if(ntot > 0 && lf > 0)
            if(lf >= ntot - 1){
                this.reset_decoder();
                if (camera != null){
                    try {
                        camera.addCallbackBuffer(callbackbuffer);
                    }catch (Exception e){
                        Log.e("camworker", "addcallbackBuffer failure in onPreviewFrame");
                    }
                }
                return;
            }
                //if(!triggered_autoestimated_end){

              //      tiggered_lastframedetectedbase_end = true;
            //}



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

        if (camera != null){
            try {
                camera.addCallbackBuffer(callbackbuffer);
            }catch (Exception e){
                Log.e("camworker", "addcallbackBuffer failure in onPreviewFrame");
            }
        }

    }

    /*
    private static boolean isFileWritable(File file) {
        try {
            RandomAccessFile raf = new RandomAccessFile(file, "rw");
            FileChannel fc=raf.getChannel();
            FileLock fl=fc.lock();
            fl.release();
            raf.write(0x47);
            fc.close();
            raf.close();
            return true;
        }
        catch(Exception ex) {
            return false;
        }

    }
    */

    private void testForFolderWritablility(String fullfolderpath){
        File fp = new File(fullfolderpath);
        String state = Environment.getExternalStorageState();
        boolean writable = ((!Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)));
        if (!fp.canWrite())
            writable = false;
        if (!writable){
            final Activity a = (Activity) CameraWorker.this.context;
            if (a != null){
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        for (int i = 0; i < 2; i++){
                            Toast d = Toast.makeText(a,
                                    a.getString(R.string.failed_writability_test1) + " '" +
                                            CameraWorker.this.filedump_directory_fullpath + "'" +
                                            a.getString(R.string.failed_writability_test2)
                                    , Toast.LENGTH_LONG);
                            TextView v = (TextView) d.getView().findViewById(android.R.id.message);
                            v.setTextColor(Color.RED);
                            d.show();
                        }
                    }
                });
            }
        }
        //f.delete();
    }

    public synchronized void waitUntilReady() {
        handler = new Handler(getLooper());
    }

    String newdumppath = null;
    boolean folder_reset_pending = false;
    @Override
    public void setNewDumpPath(final String newpath) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (!CameraWorker.this.filedump_directory_name.equals(newpath)){
                    synchronized (CameraWorker.this){
                        folder_reset_pending = true;
                        filedump_directory_fullpath = newpath;
                    }
                }
            }
        });
    }

    private int last_initCamAsync_w;
    private int last_initCamAsync_h;
    private String last_initCamAsync_foldername;
    public boolean does_not_know_optimal_index_yet = false;

    public void initCamAsync(final int surfacew, final int surfaceh, final String foldername){
        this.last_initCamAsync_foldername = foldername;
        this.last_initCamAsync_w = surfacew;
        this.last_initCamAsync_h = surfaceh;
        handler.post(new Runnable() {
            @Override
            public void run() {
                newdumppath = foldername;
                int sw = surfacew;
                int sh = surfaceh;

                if(camera != null){
                    camera.stopPreview();
                    camera.release();
                }

                try {
                    Thread.sleep(10);
                    camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
                }catch (Exception e){
                    final Activity a = (Activity) CameraWorker.this.context;
                    //Intent homeIntent = new Intent(Intent.ACTION_MAIN);
                    //homeIntent.addCategory( Intent.CATEGORY_HOME );
                    //homeIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    //a.startActivity(homeIntent);
                    Log.e("Camera", "Failed to connect to camera service");

                    if (a != null)
                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (a != null){
                                    Toast d = Toast.makeText(a, a.getString(R.string.failed_init_camera), Toast.LENGTH_LONG);
                                    TextView v = (TextView) d.getView().findViewById(android.R.id.message);
                                    v.setTextColor(Color.RED);
                                    d.show();
                                }
                            }
                        });
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e1) {
                        e1.printStackTrace();
                    }
                    Intent homeIntent2 = new Intent(Intent.ACTION_MAIN);
                    homeIntent2.addCategory( Intent.CATEGORY_HOME );
                    homeIntent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    a.startActivity(homeIntent2);

                    synchronized (CameraWorker.this) {
                        camera_initialized = true;
                        CameraWorker.this.notifyAll();
                    }
                    return;
                }

                if (camera != null)
                    camera.cancelAutoFocus();

                Camera.Parameters param = null;
                if (camera != null)
                    param = camera.getParameters();

                List<Camera.Size> psize = param.getSupportedPreviewSizes();

                int []fpsrange = new int [2];
                param.getPreviewFpsRange(fpsrange);
                param.setPreviewFpsRange(fpsrange[0], fpsrange[1]);

                param.setPreviewFormat(ImageFormat.NV21);

                param.set("vrmode", 1);
                param.set("fast-fps-mode", 1);

                android.hardware.Camera.CameraInfo info =
                        new android.hardware.Camera.CameraInfo();
                android.hardware.Camera.getCameraInfo(0, info);

                previev_list = psize;
                int bestsizeindex = CameraWorker.this.select_best_preview_size_index(psize, sw, sh, info.orientation);
                automatically_deducted_camera_preview_index = bestsizeindex;
                if (does_not_know_optimal_index_yet == true) {
                    user_selected_camera_index = automatically_deducted_camera_preview_index;
                }
                bestsizeindex = user_selected_camera_index;

                int camwidth = psize.get(bestsizeindex).width;
                int camheight = psize.get(bestsizeindex).height;
                CameraWorker.this.camwidth = camwidth;
                CameraWorker.this.camheight = camheight;

                Log.i("camworker", "QWER Preview w "+camwidth + " h " + camheight);

                //YUV-NV21 needs only that much bytes
                callbackbuffer = new byte[(int)(camheight*camwidth*1.5) + 4];
                //greyscalebuffer = new byte[camheight*camwidth];

                try{
                    param.setPreviewSize(camwidth, camheight);
                }catch (Exception e){
                    Log.e("camworker", "Error setting the camera preview size");
                }

                SurfaceTexture surfaceTexture = camsurf.getSurfaceTexture();
                try {
                    if(camera != null)
                        camera.setPreviewTexture(surfaceTexture);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //param.setRecordingHint(true);
                if (param != null) {
                    List<String> focusModes = param.getSupportedFocusModes();
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
                        try {
                            param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        } catch (Exception e){
                            Log.e("camworker", "Error setting the FOCUS_MODE_CONTINUOUS_PICTURE initCamAsync.");
                        }
                    }
                }
                //int nareas = param.getMaxNumFocusAreas();
                //if (nareas > 0) {
                    //ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>(1);
                    //focusAreas.add(new Camera.Area(new Rect(-100, -100, 100, 100), 1000));
                    //param.setFocusAreas(focusAreas);
                    //param.setMeteringAreas(focusAreas);
                //}


                try{
                    if(camera != null)
                        camera.setParameters(param);
                }catch (Exception e){
                    Log.e("camworker", "Error setting the parameters in initCamAsync.");
                }


                try {
                    camera.addCallbackBuffer(callbackbuffer);
                }catch (Exception e){
                    Log.e("camworker", "addCAllbackbuffer exception");
                }

                try {
                    camera.setPreviewCallbackWithBuffer(CameraWorker.this);
                }catch (Exception e){
                    Log.e("camworker", "setPreviewCallbackWithBuffer exception");
                }

                try {
                    camera.startPreview();
                } catch (Exception e){
                    Log.e("camworker", "startpreview exception");
                }

                final Camera.Parameters param2 = param;
                final Camera cam2 = camera;
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        //param.setRecordingHint(true);
                        if (param2 != null) {
                            List<String> focusModes = param2.getSupportedFocusModes();
                            if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
                                try {
                                    param2.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                } catch (Exception e){
                                    Log.e("camworker", "Error setting the FOCUS_MODE_CONTINUOUS_PICTURE initCamAsync.");
                                }
                            } else if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                                try {
                                    param2.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                                } catch (Exception e){
                                    Log.e("camworker", "Error setting the FOCUS_MODE_AUTO initCamAsync.");
                                }
                            }
                        }
                        if (cam2 != null)
                            try {
                                cam2.setParameters(param2);
                            } catch (Exception e){
                                Log.e("camworker", "Error setting params afer 500ms delay");
                            }
                    }
                }, 500);

                CameraWorker.this.filedump_directory_name = foldername;
                CameraWorker.this.filedump_directory_fullpath =
                        create_dump_directory_if_not_present(CameraWorker.this.filedump_directory_name);
                initialize_decoder();
                testForFolderWritablility(filedump_directory_fullpath);
                set_decoded_file_path(CameraWorker.this.filedump_directory_fullpath);

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
        synchronized (this){
            while (!this.camera_initialized){
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    boolean is_waiting_for_deinit_to_complete = false;
    @Override
    public void closeCamAsync() {
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (camera != null){
                    camera.addCallbackBuffer(null);
                    camera.setPreviewCallbackWithBuffer(null);
                    camera.cancelAutoFocus();
                    camera.setAutoFocusMoveCallback(null);
                    camera.autoFocus(null);
                }

                if(camera != null){
                    camera.stopPreview();
                    camera.addCallbackBuffer(null);
                    try {
                        camera.setPreviewDisplay(null);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    camera.release();
                    camera = null;
                    System.gc();
                }
                if(camsurf != null){
                    SurfaceTexture st = camsurf.getSurfaceTexture();
                    if (st != null)
                        st.release();
                }
                tell_decoder_no_more_qr();
                deinitialize_decoder();

                CameraWorker.this.succesfull_positions_in_prev_chunk = null;
                CameraWorker.this.succesfull_positions_in_current_chunk = null;
                CameraWorker.this.succesfull_last_smallpos = null;

                biggest_frame_number = -1;
                total_frame_number = -1;

                CameraWorker.this.RS_info_set = false;

                CameraWorker.this.callbackbuffer = null;
                //CameraWorker.this.greyscalebuffer = null;
                System.gc();
                synchronized (CameraWorker.this){
                    while(is_waiting_for_deinit_to_complete) {
                    try {
                        CameraWorker.this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    }
                    CameraWorker.this.camera_initialized = false;
                }

            }
        });

        synchronized (CameraWorker.this){
            is_waiting_for_deinit_to_complete = false;
            CameraWorker.this.notifyAll();
        }

        ///this.interrupt();

        //this.quit();
        //this.interrupt();

        //handler.getLooper().quit();
        if(this != null)
            synchronized (this){
                this.interrupt();
            }

        //handler = null;
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
                Camera.Parameters param = null;
                if (camera != null)
                    param = camera.getParameters();
                if (param != null) {
                    List<String> focusModes = param.getSupportedFocusModes();
                    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                        try {
                            param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                        } catch (Exception e){
                            Log.e("camworker", "Error setting the FOCUS_MODE_CONTINUOUS_PICTURE initCamAsync.");
                        }
                    }
                }

                try{
                    if(camera != null)
                        camera.setParameters(param);
                }catch (Exception e){
                    Log.e("camworker", "Error setting the parameters in initCamAsync.");
                }

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (camera != null)
                            camera.autoFocus(new Camera.AutoFocusCallback() {
                                @Override
                                public void onAutoFocus(boolean success, Camera camera2) {
                                    if (camera == null)
                                        return;
                                    Log.i("Focus", "camera autofocused, success = " + success);

                                    //if(camera != null)
                                    //    camera.cancelAutoFocus();

                                    Camera.Parameters param = camera.getParameters();
                                    //param.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);
                                    //camera.setParameters(param);

                                    //camera.autoFocus(null);
                                    //Log.i("Focus", "fixed");


                                    //param.setRecordingHint(true);





                                    //param.setRecordingHint(true);
                                    if (param != null) {
                                        List<String> focusModes = param.getSupportedFocusModes();
                                        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)){
                                            try {
                                                param.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                                            } catch (Exception e){
                                                Log.e("camworker", "Error setting the FOCUS_MODE_CONTINUOUS_PICTURE initCamAsync.");
                                            }
                                        }
                                    }
                                    //int nareas = param.getMaxNumFocusAreas();
                                    //if (nareas > 0) {
                                    //ArrayList<Camera.Area> focusAreas = new ArrayList<Camera.Area>(1);
                                    //focusAreas.add(new Camera.Area(new Rect(-100, -100, 100, 100), 1000));
                                    //param.setFocusAreas(focusAreas);
                                    //param.setMeteringAreas(focusAreas);
                                    //}


                                    try{
                                        if(camera != null)
                                            camera.setParameters(param);
                                    }catch (Exception e){
                                        Log.e("camworker", "Error setting the parameters in initCamAsync.");
                                    }



                            /*
                            if (param != null) {
                                List<String> focusModes = param.getSupportedFocusModes();
                                if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)){
                                    try {
                                        param.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                                    } catch (Exception e){
                                        Log.e("camworker", "Error setting the FOCUS_MODE_CONTINUOUS_PICTURE initCamAsync.");
                                    }
                                }
                            }

                            try{
                                if(camera != null)
                                    camera.setParameters(param);
                            }catch (Exception e){
                                Log.e("camworker", "Error setting the parameters in initCamAsync.");
                            }
                            */


                                }
                            });
                    }
                },150);


                //camera.cancelAutoFocus();
                //camera.autoFocus(null);
            }
        });
    }

    ;
    public int automatically_deducted_camera_preview_index = -1;
    public int user_selected_camera_index;
    public List<Camera.Size> previev_list;
    public boolean isUserPreviewSizeSet = false;

    public int index_maximum_preview_sufrace = -1; // not found

    public int select_best_preview_size_index(List<Camera.Size> psize, int sw, int sh, int orientation){
        int magicbig = 640;  // threshold for choosing previev size

        Display display = ((Activity)context).getWindowManager().getDefaultDisplay();
        Point size0 = new Point();
        display.getSize(size0);
        int scrwidth = size0.x;
        int scrheight = size0.y;
        int biggerscr = scrwidth;
        if (scrheight > scrwidth)
            biggerscr = scrheight;
        // heurestic - the bigger the phone display is, prefer bigger preview size
        if (biggerscr >= 1024)
            magicbig = 720;
        //if (biggerscr > 1280)
        //    magicbig = 800;
        //if (biggerscr >= 1920)
        //    magicbig = 900;

        /*double maxsurface = 0.0;
        int index_maxsurface = -1;
        int wmaxs=0, hmaxs=0;
        for (int i = 0; i < psize.size(); i++) {
            Camera.Size size = psize.get(i);
            if (size.width * size.height > maxsurface) {
                maxsurface = size.width * size.height;
                index_maxsurface = i;
                wmaxs = psize.get(i).width;
                hmaxs = psize.get(i).height;
            }
        }*/


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


        int found_sorted_index = -1;
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

            //Log.i("CamPrevSize", "size w "+ size.width + "; size h "+size.height);
            int bigger_size, smaller_size;
            if (size.width > size.height){
                bigger_size = size.width;
                smaller_size = size.height;
            } else {
                bigger_size = size.height;
                smaller_size = size.width;
            }
            if (bigger_size >= magicbig){
                found_sorted_index = i;
                break;
            }
        }

        //backup mode - no matching portraits mode between preview sizes and surface proportion found
        if (found_sorted_index == -1)
            for (int i = 0; i < l.size(); i++) {
                Camera.Size size = l.get(i);
                //Log.i("CamPrevSize", "size w "+ size.width + "; size h "+size.height);
                int bigger_size, smaller_size;
                if (size.width > size.height){
                    bigger_size = size.width;
                    smaller_size = size.height;
                } else {
                    bigger_size = size.height;
                    smaller_size = size.width;
                }
                if (bigger_size >= magicbig){
                    found_sorted_index = i;
                    break;
                }
            }

        //last resort
        if(found_sorted_index == -1)//have not found
            found_sorted_index = 0;//l.size() - 1;

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



    //use to remember when the detector has been reseted.
    // for certain interval, the old status will be still delivered, to give decoder a chance
    //to deliver the end status for some time
    long time_detector_reseted = 0;
    double delivered_NoiseRatio = 0.0;
    double delivered_totalprogress = 0.0;
    boolean should_deliver_pending_info_for_drawer = false;
    long pending_delivered_info_timeout = 500000000L; //1s/2
    @Override
    public synchronized double getCurrentNoiseRatio() {
        if(System.nanoTime() - this.time_detector_reseted > pending_delivered_info_timeout){
            this.should_deliver_pending_info_for_drawer = false;
        }
        if (should_deliver_pending_info_for_drawer){
            return this.delivered_NoiseRatio;
        }
        int RSn_curr = RSn;
        if (is_residual)
            RSn_curr = RSn_res;
        int RSk_curr = RSk;
        if (is_residual)
            RSk_curr = RSk_res;
        double nr = 0;
        int succn = 0;
        int nfn = this.last_frame_number_arrived_so_far;
        if (is_residual && RSn > 0)
            nfn -= (nfn / RSn)*RSn;
        if (RSn_curr > 0)
        {
            for (int i = 0; i < nfn % RSn_curr; i++){
                if (succesfull_positions_in_current_chunk[i]==false)
                    succn++;
            }
            if (this.last_frame_number_arrived_so_far < 2)
                nr = 0;
            else{
                if(!is_residual)
                    nr = ((double)succn) / ((double)(this.last_frame_number_arrived_so_far % RSn_curr));
                else
                    nr = ((double)succn) /
                            ((double)((-(this.last_frame_number_arrived_so_far / RSn)*RSn+this.last_frame_number_arrived_so_far) % RSn_res));
                //Log.i("QQQQ", "succn "+succn + ", nr "+nr + ", RSn_curr "+RSn_curr + ", lasfrarrsofar "+ this.last_frame_number_arrived_so_far);
            }
        }
        if (RSn_curr == 0 || RSk_curr == 0)
            nr = 0;
        else
            nr /= ((double) (RSn_curr-RSk_curr)) / ((double) RSn_curr); // relative to the maximum allowed RS error level
        //if (nr>0)
        //    Log.i("FFF", "nr "+nr);
        return nr;
    }

    @Override
    public synchronized double getCurrentProgressRatio() {
        if(System.nanoTime() - this.time_detector_reseted > pending_delivered_info_timeout){
            this.should_deliver_pending_info_for_drawer = false;
        }
        if (should_deliver_pending_info_for_drawer){
            return this.delivered_totalprogress;
        }
        double pr = 0;
        if (this.total_frame_number > 1)
            pr = ((double)this.last_frame_number_arrived_so_far)/((double)this.total_frame_number-1);
        //if (pr > 0)
        //    Log.i("FFF", "pr "+pr);
        return pr;
    }

    public boolean should_draw_progressbars = false;
    @Override
    public synchronized boolean shouldDrawProgressBars() {
        return this.should_draw_progressbars;
    }

    public synchronized boolean should_draw_async_info(){
        return this.should_deliver_pending_async_for_certain_time;
    }

    String filename_detected_from_header;
    boolean is_header_detected = false;
    int filesize_carried_in_the_detected_header = -1;
    @Override  //deprecated
    public synchronized String getFileNameCapturedFromHeader() {
        if (this.is_header_detected){
            return "FAKENAME.txt"; // TODO - capture the real file name from the detected header
        }
        return "........"; //this means the header is still not recognized
    }

    private double timeout_file_successfuly_saved_ms = 2000.0;
    private boolean should_deliver_info_file_successfully_saved_for_certain_time = false;
    private boolean should_deliver_pending_async_for_certain_time = false;
    private double last_time_the_file_succ_saved = System.nanoTime()/1.0e6;

    private boolean got_dataframe_before_header_detection = false;
    private double timeout_no_header_detected_ms = 1500.0;
    private double last_time_the_first_dataframe_arrived_without_headerframe = System.nanoTime()/1.0e6;

    private boolean should_deliver_error_info_for_certain_time = false;
    private double error_message_time_duration_ms = 2500.0;
    private double error_time_arrival_ms = System.nanoTime()/1.0e6; //take it when get the error message
    private boolean got_chunkRS_decode_error = false;
    private String last_filename_detected_from_header = "";
    private boolean did_any_header_frame_arrived = false;
    DisplayStatusInfo status = new DisplayStatusInfo();
    @Override
    public synchronized DisplayStatusInfo getDisplayStatusText() {

        status.displayTextType = DisplayStatusInfo.StatusDisplayType.TYPE_NOTE;
        status.additional_err_text = this.getChunkInfoString() + " "+
                ((int)(this.current_chunk_number+1))+"/"+this.nchunks;

        double currms = System.nanoTime() / 1.0e6;

        if (this.detector_is_quiet_time_for_notdisplay_nostartseq &&
                currms*1.0e6 - this.detector_native_reset_ended_ns
                        > this.detector_native_reset_ended_ns_quiet_interval_for_not_display_nostartseq)
            this.detector_is_quiet_time_for_notdisplay_nostartseq = false;

        if (this.should_deliver_pending_async_for_certain_time) {
            status.displayTextType = DisplayStatusInfo.StatusDisplayType.TYPE_NOTE;
            status.displaytext2 = "";
            status.displaytext = this.str_pending_async_text;
            status.should_draw_status = true;
            return status;
        }

        if (this.should_deliver_error_info_for_certain_time){
            if (currms - this.error_time_arrival_ms > this.error_message_time_duration_ms){
                this.error_time_arrival_ms = currms;
                this.should_deliver_error_info_for_certain_time = false;
                this.last_filename_detected_from_header = "";
                //Log.i("TTT", "stopping error send");
            }else{
                //Log.i("TTT", "ordering draw err");
                status.displayTextType = DisplayStatusInfo.StatusDisplayType.TYPE_ERR;
                status.displaytext2 = "";
                status.displaytext2 = this.last_filename_detected_from_header;
                status.displaytext = this.str_failed_data_detection;

                status.should_draw_status = true;
            }
            return status;
        }

        if (this.should_deliver_info_file_successfully_saved_for_certain_time) {
            double currtime = System.nanoTime() / 1.0e6;
            if (currtime - last_time_the_file_succ_saved > this.timeout_file_successfuly_saved_ms){
                this.should_deliver_info_file_successfully_saved_for_certain_time = false;
                this.last_time_the_file_succ_saved = currtime;
                this.last_filename_detected_from_header = "";
            }else{
                status.displayTextType = DisplayStatusInfo.StatusDisplayType.TYPE_DONE;
                status.displaytext2 = "";
                status.displaytext2 = this.last_filename_detected_from_header;
                status.displaytext = this.str_succeeded_data_detection;
                status.should_draw_status = true;
            }
            return status;
        }

        if (!this.did_any_header_frame_arrived){
            //Log.i("TTT", "encour start transmm");
            status.displaytext = this.str_encourage_new_transmission;
            status.displaytext2 = "";
            if (this.got_dataframe_before_header_detection){
                status.displaytext2 = this.str_missed_to_detect_header;
                double currtime = System.nanoTime()/1.0e6;

                if (this.detector_is_quiet_time_for_notdisplay_nostartseq){
                    status.should_draw_status = true;
                    status.displaytext = this.str_encourage_new_transmission;
                    status.displaytext2 = "";
                    return status;
                }
                if (currtime - this.last_time_the_first_dataframe_arrived_without_headerframe >
                        this.timeout_no_header_detected_ms){
                    this.got_dataframe_before_header_detection = false;
                    this.last_time_the_first_dataframe_arrived_without_headerframe = currtime;
                }
            }
            status.should_draw_status = true;
            return status;
        }
        if (this.did_any_header_frame_arrived && (!this.is_header_detected)){
            if (!got_dataframe_before_header_detection){
                status.should_draw_status = true;
                status.displaytext = this.str_started_header_detection;
                status.displaytext2 = "";
                return status;
            }else{
                status.should_draw_status = true;
                status.displaytext = this.str_encourage_new_transmission;
                status.displaytext2 = this.str_missed_to_detect_header;;
                return status;
            }
        }
        if (this.did_any_header_frame_arrived && this.is_header_detected){
            status.should_draw_status = true;
            status.displaytext = this.str_detected_file1+" "+get_last_recognized_file_name_str();
            status.displaytext2 = this.str_detected_file2;
            return status;
        }
        return status;
    }

    public synchronized String getChunkInfoString(){
        return this.getStringResourceByName("draw_progress_chunk_name");
    }

    public void set_is_blur(boolean is_blur){
        if (this.camsurf != null)
            this.camsurf.set_is_blur(is_blur);
    }

    public void set_prevsquare_size_percent(int val){
        if (this.camsurf != null)
            this.camsurf.set_prevsquare_size_percent(val);
    }
    /// data as NV21 input, pixels as 8bit greyscale output
    //public static void applyGrayScale(byte [] pixels, byte [] data, int width, int height) {
    //    applygrayscalenative(pixels, data, width, height);
/*
        byte p;
        int size = width*height;
        for(int i = 0; i < size; i++) {
            p = (byte)(data[i] & 0xFF);
            pixels[i] = p;
        }
*/
    //}

    private boolean notify_gallery_about_new_file_if_mimetype_justifies_that(){
        boolean galleryupdated = false;
        final String fpath = filedump_directory_fullpath+File.separator+
                last_filename_detected_from_header;
        final String mimeType = URLConnection.guessContentTypeFromName(fpath);
        galleryupdated = (mimeType != null && (mimeType.startsWith("image") || mimeType.startsWith("video")));
        if (galleryupdated){
            final Activity a = (Activity) context;
            if (a != null){
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            a.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + fpath)));
                        }catch (Exception e){
                            Log.e("camworker", "error sending broadcast while updating gallery..");
                        }
                    }
                });
            }
        }
        return galleryupdated;
    }

    private void deliver_filesaved_toast(){
        final Activity a = (Activity) context;
        if (a != null){
            a.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast d = Toast.makeText(a,
                            a.getString(R.string.textfilesaved_toast1)+" "+filedump_directory_fullpath+File.separator+
                                    last_filename_detected_from_header+" "+ a.getString(R.string.textfilesaved_toast2)
                            , Toast.LENGTH_LONG);
                    d.show();
                }
            });
        }
    }

    public void file_status_delivered(String filename){ //either failed or succeeded
        Log.i("FILESTATUS", "got file status, success : "+this.file_detected_and_finally_saved_successfully +
        " Filename : " + filename);
    }

    private double detector_native_reset_ended_ns = System.nanoTime();
    private double detector_native_reset_ended_ns_quiet_interval_for_not_display_nostartseq = 4e9;
    boolean detector_is_quiet_time_for_notdisplay_nostartseq = false;
    public void reset_decoder(){
        //Log.i("RST", "reset of the decoder is detected");

        synchronized (this){
            this.time_detector_reseted = System.nanoTime();
            this.should_deliver_pending_info_for_drawer = false;
            this.delivered_NoiseRatio = this.getCurrentNoiseRatio();
            this.delivered_totalprogress = this.getCurrentProgressRatio();
            this.should_deliver_pending_info_for_drawer = true;
            this.last_filename_detected_from_header = get_last_recognized_file_name_str();
            this.should_draw_progressbars = false;
            this.should_deliver_pending_async_for_certain_time = true;
        }

        this.estimated_max_framerate = 0;
        this.lastframeswithtime.clear();
        last_frame_number_arrived_so_far = -1;
        tell_decoder_no_more_qr();
        int stat = deinitialize_decoder();
        detector_native_reset_ended_ns = System.nanoTime();
        detector_is_quiet_time_for_notdisplay_nostartseq = true;
        {
            synchronized (this){
                this.should_deliver_pending_async_for_certain_time = false;
            }
            for (int i = 0; i < RSn; i++) {
                succesfull_positions_in_prev_chunk[i] = succesfull_positions_in_current_chunk[i];
                succesfull_positions_in_current_chunk[i] = false;
            }


            this.last_frame_number_arrived_so_far = 0;
            this.total_frame_number = 0;
            this.RSn = 0;
            this.RSk = 0;
            this.RSn_res = 0;
            this.RSk_res = 0;

            this.biggest_frame_number = -1;
            this.last_frame_number = 0;
            this.total_frame_number = -1;

            //Log.i("RST", "stat value "+stat);
            this.file_detection_ended = true;
            if(stat == 3 || stat == 4){

                if (this.is_header_detected) {
                    synchronized (this) {
                        this.got_chunkRS_decode_error = true;
                    }
                    //Log.i("RST ERR", "Got unrecoverable chunk at the end - error");
                    synchronized (this) {
                        this.error_time_arrival_ms = System.nanoTime() / 1.0e6;
                        this.should_deliver_error_info_for_certain_time = true;
                    }
                }else{
                    //Log.i("RST ERR", "Death 8s time passed, restarted, no data obtained");
                }
                this.file_detected_and_finally_saved_successfully = false;
            }
            else if (stat == 7){
                this.file_detected_and_finally_saved_successfully = true;
                should_deliver_info_file_successfully_saved_for_certain_time = true;
                this.last_time_the_file_succ_saved = System.nanoTime()/1.0e6;
                this.deliver_filesaved_toast();
                this.notify_gallery_about_new_file_if_mimetype_justifies_that();
            }

        }

        this.RS_info_set = false;
        this.last_header_frame_delivered = 0;

        this.file_status_delivered(this.last_received_file_name);
        initialize_decoder();
        testForFolderWritablility(filedump_directory_fullpath);
        set_decoded_file_path(this.filedump_directory_fullpath);
        this.file_detection_ended = false;
        synchronized (this){
            this.should_draw_progressbars = false;
            this.is_header_detected = false;
            this.filesize_carried_in_the_detected_header = -1;
        }
        this.is_residual = false;
        this.did_any_header_frame_arrived = false;
        this.got_chunkRS_decode_error = false;
        this.got_dataframe_before_header_detection = false;

        System.gc();
    }

    private String filedump_directory_fullpath = null;
    private String filedump_directory_name = "Download";
    public static String create_dump_directory_if_not_present(String dirname){
        File yourAppDir = null;
        if (Environment.getExternalStorageState() != null){
             yourAppDir = new File(Environment.getExternalStorageDirectory()+File.separator+dirname);
        }else {
            yourAppDir = new File(Environment.getDataDirectory()
                        + File.separator+dirname);
        }

        if(!yourAppDir.exists() && !yourAppDir.isDirectory())
        {
            // create empty directory
            if (yourAppDir.mkdirs())
            {
                Log.i("CreateDir","App dir created");
            }
            else
            {
                Log.w("CreateDir","Unable to create app dir!");
            }
        }
        else
        {
            Log.i("CreateDir","App dir already exists");
        }
        return yourAppDir.getPath();
    }



    private String str_encourage_new_transmission;
    private String str_started_header_detection;
    private String str_missed_to_detect_header;
    private String str_detected_file1;
    private String str_detected_file2;
    private String str_failed_header_detection;
    private String str_failed_data_detection;
    private String str_succeeded_data_detection;
    private String str_pending_async_text;
    private String getStringResourceByName(String aString) {
        Activity a = (Activity) this.context;
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

    /////////native part
    //public static native void applygrayscalenative(byte [] pixels, byte [] data, int width, int height);

    //native decoder/encoder lib part

    public static native int initialize_decoder();
    public static native int set_decoded_file_path(String path);
    public static native int send_next_grayscale_buffer_to_decoder(
        byte[] grayscale_qr_data,
        int image_width,
        int image_height,
        int ncores,
        byte []auxilarry,
        int nth_every_pix);

    public static native int tell_decoder_no_more_qr();

    public static native int get_total_frames_of_data_that_will_be_produced();
    public static native int get_last_number_of_frame_detected();
    public static native int get_last_number_of_header_frame_detected();

    public static native String get_last_recognized_file_name_str();
    public static native int get_last_recognized_file_size();

    public static native int get_main_RSN();
    public static native int get_main_RSK();
    public static native int get_residual_RSN();
    public static native int get_residual_RSK();

    public static native int deinitialize_decoder();

}
