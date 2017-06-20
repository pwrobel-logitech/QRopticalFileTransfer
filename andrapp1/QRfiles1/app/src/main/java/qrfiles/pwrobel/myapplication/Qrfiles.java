package qrfiles.pwrobel.myapplication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Environment;
import android.os.Process;
import android.util.Log;
import android.view.WindowManager;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class Qrfiles extends Activity {


    //used to hold main camera thread with the higher priority
    CameraWorker camworker;
    CustomProgressBar progressBar1_decoder;
    CustomProgressBar progressBar2_decoder;
    CameraPreviewSurface camSurf;

    //for decoder status text view
    TextView decoder_status_textview1;
    TextView decoder_status_textview2;

    //
    FloatingActionButton uparrowbutton = null;

    View detector_view = null;
    View qrsender_view = null;
    View main_layout = null;

    QRSurface qrsurf = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        File yourAppDir = new File(Environment.getExternalStorageDirectory()+"");
        default_search_for_upload_homedir = yourAppDir.getPath();
    }



    boolean is_in_decoder_view = true;
    boolean is_in_qr_sender_view = false;
    boolean is_in_filechooser_view = false;
    @Override
    public void onResume(){
        Log.i("ACTINFO", "Activity resumed");


        //setContentView(R.layout.activity_qrfiles);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        this.switch_to_detector_view();


        this.initall();

        super.onResume();
    }

    void set_activity_button_listeners(){
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        uparrowbutton = (FloatingActionButton) findViewById(R.id.arrowup);
        uparrowbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean is_switching_views = false;
                synchronized (this){
                    is_switching_views = Qrfiles.this.is_switching_views;
                }
                if (is_switching_views)
                    return;

                Qrfiles.this.is_switching_views = true;
                new Timer().schedule(new TimerTask()
                { //unblock button after certain interval - hack
                    @Override
                    public void run()
                    {
                        synchronized (Qrfiles.this){
                            Qrfiles.this.is_switching_views = false;
                        }
                    }
                }, 1000);

                Qrfiles.this.main_layout.setVisibility(View.GONE);
                Qrfiles.this.uparrowbutton.setOnClickListener(null);
                uparrowbutton.setClickable(false);
                uparrowbutton.requestLayout();
                Log.i("clickable", "setting clickable to false");
                if (Qrfiles.this.is_in_decoder_view) {
                    Qrfiles.this.switch_to_qrsender_view();
                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.down_arrow_icon);
                    Qrfiles.this.uparrowbutton.setImageBitmap(bmp);
                    Qrfiles.this.uparrowbutton.requestLayout();
                    Qrfiles.this.is_in_decoder_view = false;
                    Qrfiles.this.is_in_qr_sender_view = true;
                    //Qrfiles.this.is_switching_views = false;
                    return;
                }
                if (!Qrfiles.this.is_in_decoder_view) {
                    Qrfiles.this.switch_to_detector_view();
                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.up_arrow_icon);
                    Qrfiles.this.uparrowbutton.setImageBitmap(bmp);
                    Qrfiles.this.uparrowbutton.requestLayout();
                    Qrfiles.this.is_in_decoder_view = true;
                    Qrfiles.this.is_in_qr_sender_view = false;
                    //Qrfiles.this.is_switching_views = false;
                    return;
                }
            }
        });
    }

    void set_decoder_elements(){
        camSurf = (CameraPreviewSurface) findViewById(R.id.glsurfaceView1);
        progressBar1_decoder = (CustomProgressBar) findViewById(R.id.surfaceView2);
        progressBar2_decoder = (CustomProgressBar) findViewById(R.id.surfaceView3);

        this.camSurf.setCustomDecoderProgressBarsDrawers(progressBar1_decoder, progressBar2_decoder);

        this.decoder_status_textview1 = (TextView) findViewById(R.id.statustext1);
        this.decoder_status_textview2 = (TextView) findViewById(R.id.statustext2);

        this.decoder_status_textview1.setVisibility(View.VISIBLE);
        this.decoder_status_textview2.setVisibility(View.VISIBLE);

        this.camSurf.setCustomDecoderStatusTextView(this.decoder_status_textview1, this.decoder_status_textview2);
    }

    @Override
    public void onPause(){
        super.onPause();
        Log.i("ACTINFO", "Activity paused");
        this.destroyall();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_qrfiles, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private boolean is_switching_views = false;
    private void switch_to_detector_view(){


        if (this.qrsurf != null)
            this.qrsurf.destroy_all_resources(); //actually, only deinits qrsurf manager thread
        this.qrsurf = null;

        //setContentView(R.layout.activity_qrfiles);



        setContentView(R.layout.activity_qrfiles);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        set_activity_button_listeners();


        this.set_decoder_elements();

        this.detector_view = (View) findViewById(R.id.detector_layout);
        this.qrsender_view = (View) findViewById(R.id.qrsender_layout);
        this.main_layout = (View) findViewById(R.id.main_layout);


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());





        this.detector_view.setVisibility(View.VISIBLE);
        this.qrsender_view.setVisibility(View.GONE);
        this.detector_view.requestLayout();
        this.qrsender_view.requestLayout();
        this.initall();




        //after initialization done - allow the button to be reclickable again


                /*
                Log.i("NNN", "preparing to wait for surface");
                synchronized (Qrfiles.this.camSurf){
                    while (!Qrfiles.this.camSurf.surface_and_camera_prepared){
                        try {
                            Log.i("NNN", "dothewait "+Qrfiles.this.camSurf.surface_and_camera_prepared);
                            Qrfiles.this.camSurf.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }*/

                //if (this.camSurf != null)
                //synchronized (this.camSurf){
                    //while (!(this.camSurf.surface_and_camera_prepared)){

                    //}
                //}
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        uparrowbutton.setClickable(true);
                uparrowbutton.requestLayout();

                Log.i("clickable", "setting clickable to true1");
        Log.i("clickable", "executed on the thread, id: " + android.os.Process.myTid());


        this.main_layout.setVisibility(View.VISIBLE);
        this.main_layout.requestLayout();



    }


    AdView adView;
    String default_search_for_upload_homedir = null;
    String chosen_file_path;
    ChooserDialog fileselection_dialog_in_sender;
    private void switch_to_qrsender_view(){

        this.destroyall();  //destroy camera and detector stuff

        //set_decoder_elements();

        //after deinitialization done - allow the button to be reclickable again
        //this.runOnUiThread(new Runnable() {
        //    @Override
        //    public void run() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        this.qrsurf = (QRSurface) findViewById(R.id.qrsurf);
        this.qrsurf.setFPS(15.0);
        this.qrsurf.set_header_display_timeout(7.0);
        this.qrsurf.setZOrderOnTop(true);
        this.qrsurf.init_qrsurf_thread();//starts thread

                uparrowbutton.setClickable(true);
                uparrowbutton.requestLayout();
                Log.i("clickable", "setting clickable to true2");
                Log.i("clickable", "executed on the thread, id: " + android.os.Process.myTid());
                Qrfiles.this.set_activity_button_listeners();
        //    }
        //});

        this.main_layout.setVisibility(View.VISIBLE);
        this.detector_view.setVisibility(View.GONE);
        this.qrsender_view.setVisibility(View.VISIBLE);
        this.main_layout.requestLayout();
        this.detector_view.requestLayout();
        this.qrsender_view.requestLayout();




        adView = (AdView) this.findViewById(R.id.adView);

        new Timer().schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                Qrfiles.this.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        AdRequest adRequest = new AdRequest.Builder()
                                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                                .addTestDevice("motorola_3g-001")
                                .build();

                        adView.loadAd(adRequest);
                    }
                });
            }
        }, 1000);
        



        if (default_search_for_upload_homedir != null)
            this.fileselection_dialog_in_sender = new ChooserDialog().with(this)
                .withFilter(false, false)
                .withStartFile(default_search_for_upload_homedir)
                .withDateFormat("HH:mm")
                .withResources(R.string.title_choose_folder, R.string.title_choose, R.string.dialog_cancel)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        chosen_file_path = path;
                        Toast.makeText(Qrfiles.this, "FILE: " + chosen_file_path, Toast.LENGTH_SHORT).show();
                        //_tv.setText(_path);
                        List<String> files = new ArrayList<String>();
                        files.clear();
                        files.add(0, chosen_file_path);
                        Qrfiles.this.qrsurf.add_new_files_to_send((ArrayList<String>) files);
                    }
                })
                .build()
                .show();





    }


    private void initall(){
        Log.i("UIThr", "executed on the UI thread, id: " + android.os.Process.myTid());
        this.camworker = new CameraWorker("CameraDetectorThread");
        this.camworker.setContext(this);
        this.camworker.start();
        this.camworker.waitUntilReady();


        //this.camworker.waitUntilReady();
        this.camworker.handler.post(new Runnable() {
            @Override
            public void run() {
                Log.i("CamThr", "executed on the camera thread, id: " + android.os.Process.myTid());
            }
        });
        //this.camworker.initAsync();

        /*
        synchronized (this.camSurf){
            while (!this.camSurf.surface_and_camera_prepared){
                try {
                    this.camSurf.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }*/

        this.main_layout.setVisibility(View.VISIBLE);
        this.main_layout.requestLayout();

    }

    private void destroyall(){




        if(this.camworker != null)
            this.camworker.closeCamAsync();
        if(this.camSurf != null)
            this.camSurf.deinitialize_resources();


        if(this.qrsurf != null)
            this.qrsurf.destroy_all_resources();
        this.qrsurf = null;

        /*
        try {
            this.camworker.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        this.camworker.quit();
        this.camworker.interrupt();
*/

        this.camSurf = null;
        this.camworker = null;

        System.gc();
    }

    /**
     * A native method that is implemented by the 'native-lib' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();

    // Used to load the 'native-lib' library on application startup.
    static {
        System.loadLibrary("fec");
        System.loadLibrary("qrencoder_wrapper");
        System.loadLibrary("RSencoder");
        System.loadLibrary("RSdecoder");
        System.loadLibrary("RSencAPI");
        System.loadLibrary("RSdecAPI");
        System.loadLibrary("native-lib");
    }



}
