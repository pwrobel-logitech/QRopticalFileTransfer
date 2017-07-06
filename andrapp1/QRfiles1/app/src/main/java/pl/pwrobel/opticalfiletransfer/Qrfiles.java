package pl.pwrobel.opticalfiletransfer;

import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.WindowManager;
import android.webkit.MimeTypeMap;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
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

public class Qrfiles extends AppCompatActivity implements TransmissionController{


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
    FloatingActionButton folderfilebutton = null;

    View detector_view = null;
    View qrsender_view = null;
    View main_layout = null;

    QRSurface qrsurf = null;

    TextView encoder_status_textfield;
    TextView encoder_status_textfield2;
    CustomProgressBar progressBar_encoder;

    private PopupMenu myMainMenu = null;
    private FloatingActionButton fab = null;

    boolean got_upload_request_from_intent = false;
    private String upload_requested_path_by_system = null;

/*
    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putInt("FPS", this.currFPSvalue);
        savedInstanceState.putInt("Errlevel", this.currErrorvalue);
        savedInstanceState.putInt("Qrsize", this.currQrSizevalue);
        savedInstanceState.putString("filedumppath", this.currDumpPath);

        Log.i("REST", "saved fps "+this.currFPSvalue);
        super.onSaveInstanceState(savedInstanceState);
    }
*//*
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.



        this.currFPSvalue = savedInstanceState.getInt("FPS");
        this.currErrorvalue = savedInstanceState.getInt("Errlevel");
        this.currQrSizevalue = savedInstanceState.getInt("Qrsize");
        this.currDumpPath = savedInstanceState.getString("filedumppath");
        Log.i("REST", "restored fps "+this.currFPSvalue);
    }
*/

    private SharedPreferences preferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        this.preferences = this.getPreferences(Context.MODE_PRIVATE);

        Log.i("REST", "oncreate called");

        File yourAppDir = new File(Environment.getExternalStorageDirectory()+"");
        default_search_for_upload_homedir = yourAppDir.getPath();

        Intent intent = getIntent();

        //check if opening this activity is not a request for file upload
        this.got_upload_request_from_intent = false;
        if (intent != null){
            String datastring = intent.getDataString();
            if (datastring != null){
                if (datastring.startsWith("file:///")){
                    String textafter = datastring.substring(7);
                    if (textafter.length() > 0){
                        this.got_upload_request_from_intent = true;
                        this.upload_requested_path_by_system = textafter;
                    }
                }
            }
        }


        Log.i("intent", "file : "+this.upload_requested_path_by_system);

    }


    private void setupMainMenu(){
        this.myMainMenu = new PopupMenu(this, this.fab);;
        this.myMainMenu.inflate(R.menu.mainmenu);

        this.myMainMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId())
                {
                    case R.id.menu_settings:
                        Log.i("MENU", "settings selected");
                        showSettingDialog();
                        return true;
                    case R.id.menu_help:
                        Log.i("MENU", "help selected");
                        showHelpDialog();
                        return true;
                    case R.id.menu_about:
                        Log.i("MENU", "about selected");
                        showAboutDialog();
                        return true;
                    case R.id.menu_privacypolicy:
                        Log.i("MENU", "privacy policy selected");
                        showPrivacyPolicyDialog();
                        return true;
                }
                return false;
            }
        });

        this.myMainMenu.show();
    }



    //private int mSettingsStackLevel = 0;
    private NumberPicker numberPickerFPS = null;
    void showSettingDialog() {

        //mSettingsStackLevel++;

        // DialogFragment.show() will take care of adding the fragment
        // in a transaction.  We also want to remove any currently showing
        // dialog, so make our own transaction and take care of that here.
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        Fragment prev = getFragmentManager().findFragmentByTag("dialog");
        if (prev != null) {
            ft.remove(prev);
        }
        ft.addToBackStack(null);

        // Create and show the dialog.
        SettingsFragment newFragment = SettingsFragment.newInstance();
        newFragment.set_default_setup_settings(this.currFPSvalue, this.currErrorvalue,
                this.currQrSizevalue, this.currStartSeqTime, this.currDumpPath);
        newFragment.setTransmissionContorller(this);
        newFragment.show(ft, "dialog");

    }

    void showHelpDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // Create and show the dialog.
        HelpFragment newFragment = HelpFragment.newInstance();
        newFragment.show(ft, "dialog");
    }

    void showAboutDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // Create and show the dialog.
        AboutFragment newFragment = AboutFragment.newInstance();
        newFragment.show(ft, "dialog");
    }

    void showPrivacyPolicyDialog() {
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // Create and show the dialog.
        PrivacyPolicyFragment newFragment = PrivacyPolicyFragment.newInstance();
        newFragment.show(ft, "dialog");
    }

    static private int clamp(int val, int min, int max){
        if (min > max)
            return min;
        return Math.max(min, Math.min(max, val));
    }

    private void readAndSanitizePrefs(){
        this.preferences = this.getPreferences(Context.MODE_PRIVATE);

        int fps = this.preferences.getInt("FPS", 17);
        int errlev = this.preferences.getInt("Errlevel", 50);
        int qrsize = this.preferences.getInt("Qrsize", 585);
        int timeout = this.preferences.getInt("timeout", 6);
        int suggN = this.preferences.getInt("suggested_N", 511);
        String fdumppath = this.preferences.getString("filedumppath", null);

        this.currFPSvalue = Qrfiles.clamp(fps, 5, 60);
        this.currErrorvalue = Qrfiles.clamp(errlev, 20, 80);
        this.currQrSizevalue = Qrfiles.clamp(qrsize, 95, 1205);
        this.currStartSeqTime = Qrfiles.clamp(timeout, 3, 10);
        this.currsuggested_N = Qrfiles.clamp(suggN, 255, 1023);
        this.currDumpPath = fdumppath;

    }


    boolean is_in_decoder_view = true;
    boolean is_in_qr_sender_view = false;
    boolean is_in_filechooser_view = false;
    @Override
    public void onResume(){
        Log.i("ACTINFO", "Activity resumed");


        this.readAndSanitizePrefs();


        //setContentView(R.layout.activity_qrfiles);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        if (this.fileselection_dialog_in_sender != null)
            this.fileselection_dialog_in_sender.closeit();

        if (!this.got_upload_request_from_intent){
            this.is_in_qr_sender_view = false;
            this.switch_to_detector_view();
            this.initall();
        }else{
            this.is_in_qr_sender_view = true;
            this.is_in_decoder_view = false;
            setContentView(R.layout.activity_qrfiles);
            this.detector_view = (View) findViewById(R.id.detector_layout);
            this.qrsender_view = (View) findViewById(R.id.qrsender_layout);
            this.main_layout = (View) findViewById(R.id.main_layout);


            this.switch_to_qrsender_view();

        }




        super.onResume();
    }


    private boolean file_selection_window_requested = false;
    void set_activity_button_listeners(){
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                //        .setAction("Action", null).show();
                Qrfiles.this.setupMainMenu();
            }
        });

        uparrowbutton = (FloatingActionButton) findViewById(R.id.arrowup);
        uparrowbutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean is_switching_views = false;
                synchronized (Qrfiles.this){
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
                            Qrfiles.this.notifyAll();
                        }
                    }
                }, 1100);

                Qrfiles.this.main_layout.setVisibility(View.GONE);
                Qrfiles.this.uparrowbutton.setOnClickListener(null);
                uparrowbutton.setClickable(false);
                uparrowbutton.requestLayout();
                Log.i("clickable", "setting clickable to false");
                if (Qrfiles.this.is_in_decoder_view) {
                    Qrfiles.this.switch_to_qrsender_view();
                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.icdown3);
                    Qrfiles.this.uparrowbutton.setImageBitmap(bmp);
                    Qrfiles.this.uparrowbutton.requestLayout();
                    Qrfiles.this.is_in_decoder_view = false;
                    Qrfiles.this.is_in_qr_sender_view = true;
                    //Qrfiles.this.is_switching_views = false;
                    return;
                }
                if (!Qrfiles.this.is_in_decoder_view) {
                    Qrfiles.this.switch_to_detector_view();
                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.icup3);
                    Qrfiles.this.uparrowbutton.setImageBitmap(bmp);
                    Qrfiles.this.uparrowbutton.requestLayout();
                    Qrfiles.this.is_in_decoder_view = true;
                    Qrfiles.this.is_in_qr_sender_view = false;
                    //Qrfiles.this.is_switching_views = false;
                    return;
                }
            }
        });

        this.folderfilebutton = (FloatingActionButton) findViewById(R.id.folderfilebutton);

        this.folderfilebutton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean is_switching_views = false;
                synchronized (Qrfiles.this){
                    is_switching_views = Qrfiles.this.is_switching_views;
                }
                if (is_switching_views)
                    return;
                if (file_selection_window_requested)
                    return;
                file_selection_window_requested = true;
                Qrfiles.this.invoke_file_window(is_in_qr_sender_view);
                new Timer().schedule(new TimerTask()
                { //unblock file button after certain interval - hack
                    @Override
                    public void run()
                    {
                        synchronized (Qrfiles.this){
                            Qrfiles.this.file_selection_window_requested = false;
                            Qrfiles.this.notifyAll();
                        }
                    }
                }, 1000);
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

        synchronized (this){
            while (this.file_selection_window_requested || this.is_switching_views){
                try {
                    this.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        this.is_in_qr_sender_view = false;
        this.is_in_decoder_view = true;

        this.preferences = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putInt("FPS", this.currFPSvalue);
        editor.putInt("Errlevel", this.currErrorvalue);
        editor.putInt("Qrsize", this.currQrSizevalue);
        editor.putInt("timeout", this.currStartSeqTime);
        editor.putInt("suggested_N", this.currsuggested_N);
        editor.putString("filedumppath", this.currDumpPath);
        editor.commit();


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

        if (this.fileselection_dialog_in_sender != null)
            this.fileselection_dialog_in_sender.closeit();

        Log.i("QTHRM", "about to destroy qrsurf resources");

        if(this.qrsurf != null){
            this.qrsurf.destroy_all_resources();//actually, only deinits qrsurf manager thread
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Qrfiles.this.qrsurf.set_brightness_back_to_auto();
                }
            });
        }
        this.qrsurf = null;

        Log.i("QTHRM", "destroyed qrsurf resources");
        //setContentView(R.layout.activity_qrfiles);



        setContentView(R.layout.activity_qrfiles);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        set_activity_button_listeners();
        this.folderfilebutton.setImageResource(R.mipmap.fold3lens);


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

        Runtime.getRuntime().gc();

    }


    AdView adView;
    String default_search_for_upload_homedir = null;
    String chosen_file_path;
    ChooserDialog fileselection_dialog_in_sender;
    private void switch_to_qrsender_view(){

        if (this.fileselection_dialog_in_sender != null)
            this.fileselection_dialog_in_sender.closeit();

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

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

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        this.qrsurf = (QRSurface) findViewById(R.id.qrsurf);
        this.qrsurf.setFPS(16.0);
        this.qrsurf.set_header_display_timeout(6.0);
        this.qrsurf.setZOrderOnTop(true);
        this.qrsurf.init_qrsurf_thread();//starts thread

        Qrfiles.this.set_activity_button_listeners();
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.icdown3);
        Qrfiles.this.uparrowbutton.setImageBitmap(bmp);
                uparrowbutton.setClickable(true);
                uparrowbutton.requestLayout();
                Log.i("clickable", "setting clickable to true2");
                Log.i("clickable", "executed on the thread, id: " + android.os.Process.myTid());

        //    }
        //});

        this.folderfilebutton.setImageResource(R.mipmap.fold3nolens);

        this.main_layout.setVisibility(View.VISIBLE);
        this.detector_view.setVisibility(View.GONE);
        this.qrsender_view.setVisibility(View.VISIBLE);
        this.main_layout.requestLayout();
        this.detector_view.requestLayout();
        this.qrsender_view.requestLayout();



        this.progressBar_encoder = (CustomProgressBar) this.findViewById(R.id.encoder_progressbar);
        this.qrsurf.setCustomProgressBar(progressBar_encoder);
        this.qrsurf.reset_producer(currFPSvalue, currErrorvalue, currQrSizevalue, currStartSeqTime, currsuggested_N);

        this.encoder_status_textfield = (TextView) this.findViewById(R.id.encoder_status_textfield);
        this.encoder_status_textfield2 = (TextView) this.findViewById(R.id.encoder_status_textfield2);
        this.qrsurf.setCustomTextViewStatus(this.encoder_status_textfield, this.encoder_status_textfield2);

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
        


            if(!this.got_upload_request_from_intent) {
                this.invoke_file_window(true);
            } else {
                this.got_upload_request_from_intent = false;
                List<String> files = new ArrayList<String>();
                files.clear();
                files.add(0, this.upload_requested_path_by_system);
                Qrfiles.this.qrsurf.add_new_files_to_send((ArrayList<String>) files);
            }



            Runtime.getRuntime().gc();

    }

    private void invoke_file_window(boolean is_file_upload_selection_mode){
        if(is_file_upload_selection_mode){
            if (default_search_for_upload_homedir != null)
                this.fileselection_dialog_in_sender = new ChooserDialog().with(this)
                        .withFilter(false, false)
                        .withStartFile(default_search_for_upload_homedir)
                        .withDateFormat("HH:mm")
                        .withResources(R.string.title_choose_filetosend, R.string.title_choose, R.string.dialog_cancel)
                        .withChosenListener(new ChooserDialog.Result() {
                        @Override
                        public void onChoosePath(String path, File pathFile) {
                                chosen_file_path = path;
                             //Toast.makeText(Qrfiles.this, "FILE: " + chosen_file_path, Toast.LENGTH_SHORT).show();
                             //_tv.setText(_path);
                             List<String> files = new ArrayList<String>();
                             files.clear();
                             files.add(0, chosen_file_path);
                             Qrfiles.this.qrsurf.add_new_files_to_send((ArrayList<String>) files);
                            }
                    })
                        .build()
                        .show();
        }else{

            if (default_search_for_upload_homedir != null)
                this.fileselection_dialog_in_sender = new ChooserDialog().with(this)
                        .withFilter(false, false)
                        .withStartFile(default_search_for_upload_homedir)
                        .withDateFormat("HH:mm")
                        .withResources(R.string.title_filebrowse, R.string.title_choose, R.string.dialog_cancel)
                        .withChosenListener(new ChooserDialog.Result() {
                            @Override
                            public void onChoosePath(String path, File pathFile) {

                                //Toast.makeText(Qrfiles.this, "FILE: " + path, Toast.LENGTH_SHORT).show();


                                MimeTypeMap myMime = MimeTypeMap.getSingleton();
                                Intent newIntent = new Intent(Intent.ACTION_VIEW);
                                String mimeType = myMime.getMimeTypeFromExtension(fileExt(path));
                                if (mimeType == null)
                                    mimeType = "*/*";
                                newIntent.setDataAndType(Uri.fromFile(pathFile),mimeType);
                                newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                try {
                                    startActivity(newIntent);
                                } catch (ActivityNotFoundException e) {
                                    Toast.makeText(Qrfiles.this, "No handler for this type of file.", Toast.LENGTH_LONG).show();
                                }
                                //LaunchOpenFileIntent(path);

                            }
                        })
                        .build()
                        .show();

        }
    }

    int currFPSvalue = 17;
    int currErrorvalue = 50;
    int currQrSizevalue = 585;
    int currStartSeqTime = 6;
    int currsuggested_N = 511;
    String currDumpPath = null;
    @Override
    public void onNewTransmissionSettings(int fps, int errlevpercent, int qrsize, int startseqtime, String newdumppath) {
        Log.i("Settings", "got new settings from settingsmenu, fps : "+fps+", err : "+errlevpercent
                + ", qrsize : "+qrsize+", sseq : "+startseqtime+", dumppath : "+newdumppath);
        this.currFPSvalue = fps;
        this.currErrorvalue = errlevpercent;
        this.currQrSizevalue = qrsize;
        this.currStartSeqTime = startseqtime;
        this.currDumpPath = newdumppath;

        if(Qrfiles.this.qrsurf != null){
            Qrfiles.this.qrsurf.reset_producer(currFPSvalue, currErrorvalue, currQrSizevalue, currStartSeqTime, currsuggested_N);//actually, only deinits qrsurf manager thread
            Qrfiles.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Qrfiles.this.qrsurf.set_brightness_back_to_auto();
                }
            });
        }

    }

    private String fileExt(String url) {
        if (url.indexOf("?") > -1) {
            url = url.substring(0, url.indexOf("?"));
        }
        if (url.lastIndexOf(".") == -1) {
            return null;
        } else {
            String ext = url.substring(url.lastIndexOf(".") + 1);
            if (ext.indexOf("%") > -1) {
                ext = ext.substring(0, ext.indexOf("%"));
            }
            if (ext.indexOf("/") > -1) {
                ext = ext.substring(0, ext.indexOf("/"));
            }
            return ext.toLowerCase();

        }
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


        if(this.qrsurf != null){
            this.qrsurf.destroy_all_resources();
            this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Qrfiles.this.qrsurf.set_brightness_back_to_auto();
                }
            });
        }
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

        Runtime.getRuntime().gc();
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
        System.loadLibrary("qrdecoder_wrapper");
        System.loadLibrary("RSencoder");
        System.loadLibrary("RSdecoder");
        System.loadLibrary("RSencAPI");
        System.loadLibrary("RSdecAPI");
        System.loadLibrary("native-lib");
    }


}
