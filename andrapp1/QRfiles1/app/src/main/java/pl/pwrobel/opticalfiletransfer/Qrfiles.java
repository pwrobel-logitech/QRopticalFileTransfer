package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.hardware.Camera;
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

public class Qrfiles extends AppCompatActivity implements TransmissionController, HelpDialogDismisser, PreviewSizeController{

    // important for premium / free differentiation
    public static int limit_max_received_file_size = 5*1024*1024; // ~5MB

    public static int smearCustom(int hashCode) {
        hashCode ^= (hashCode >>> 20) ^ (hashCode >>> 12);
        return 37+hashCode ^ (hashCode >>> 7) ^ (hashCode >>> 4);
    }

    public static void openProVersionOnPlayStore(Activity act){
        final Activity a = act;
        if (a != null){
            //a.runOnUiThread(new Runnable() {
            //    @Override
            //    public void run() {
                    String appPackageName = a.getPackageName(); // getPackageName() from Context or Activity object
                    if (!appPackageName.contains("pro")); //must end with pro - the free version cannot contain the "pro" string !
                    {
                        appPackageName = appPackageName + "pro";
                    }
                    Intent googleplayint = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + appPackageName)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        //if(((Qrfiles)act).camworker != null)
                        //    ((Qrfiles)act).camworker.closeCamAsync();

                        a.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + appPackageName)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        try {
                            Thread.sleep(350);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    } catch (android.content.ActivityNotFoundException anfe) {
                        a.startActivity(googleplayint);
                        try {
                            Thread.sleep(350);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                //}
           // });
        }
    }

    public static void watchYoutubeVideo(Activity act, String id){
        /*String link = id;
        Intent webIntent2 = new Intent(Intent.ACTION_VIEW,
                Uri.parse(link)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (act != null)
            act.startActivity(webIntent2);
        try {
            Thread.sleep(350);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }  //retired - restore YT link

        return;*/

        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + id)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + id)).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            if (act != null)
                act.startActivity(webIntent);
            try {
                Thread.sleep(350);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        } catch (ActivityNotFoundException ex) {
            if (act != null)
                act.startActivity(appIntent);
            try {
                Thread.sleep(350);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

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

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {

    }
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

    void FinishActivity(){
        Intent homeIntent2 = new Intent(Intent.ACTION_MAIN);
        homeIntent2.addCategory( Intent.CATEGORY_HOME );
        homeIntent2.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        this.startActivity(homeIntent2);
    }

    /* Checks if external storage is available for read and write */
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    boolean HandleIsSDMountedProperly(){
        if (!this.isExternalStorageWritable()){
            for (int i = 0; i<2; i++){
                Toast d = Toast.makeText(this, this.getString(R.string.norwstoragedetected), Toast.LENGTH_LONG);
                TextView v = (TextView) d.getView().findViewById(android.R.id.message);
                v.setTextColor(Color.RED);
                d.show();
            }
            return false;
        }
        return true;
    }

    private SharedPreferences preferences;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(!this.HandleIsSDMountedProperly())
            FinishActivity();
        //sanitize size
        int smear = Qrfiles.smearCustom(Qrfiles.limit_max_received_file_size);
        if (smear != 5612912) //5612912
            FinishActivity();

        this.preferences = this.getPreferences(Context.MODE_PRIVATE);

        //Log.i("REST", "oncreate called");


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
                        Uri uri = Uri.parse(textafter);
                        textafter = uri.getPath(); // fix %2b %20 etc issue
                        this.upload_requested_path_by_system = textafter;
                    }
                }
            }
        }


        //Log.i("intent", "file : "+this.upload_requested_path_by_system);

    }


    private void setupMainMenu(){
        this.myMainMenu = new PopupMenu(this, this.fab);;
        this.myMainMenu.inflate(R.menu.mainmenu);
        Menu popupMenu = this.myMainMenu.getMenu();
        MenuItem optionupgr = (MenuItem)popupMenu.findItem(R.id.menu_proversion);
        boolean ispro = false;
        ispro = getResources().getBoolean(R.bool.is_pro_version);
        if (ispro)
            optionupgr.setVisible(false);

        this.myMainMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener(){
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId())
                {
                    case R.id.menu_settings:
                        //Log.i("MENU", "settings selected");
                        showSettingDialog();
                        return true;
                    case R.id.menu_help:
                        //Log.i("MENU", "help selected");
                        if (!Qrfiles.this.helpdialogshown)
                            showHelpDialog();
                        return true;
                    case R.id.menu_videolink:
                        Qrfiles.watchYoutubeVideo(Qrfiles.this, Qrfiles.this.getString(R.string.video_id));
                        return true;
                    case R.id.menu_about:
                        //Log.i("MENU", "about selected");
                        showAboutDialog();
                        return true;
                    case R.id.menu_privacypolicy:
                        //Log.i("MENU", "privacy policy selected");
                        showPrivacyPolicyDialog();
                        return true;
                    case R.id.menu_proversion:
                        //Log.i("MENU", "pro update selected.");
                        openProVersionOnPlayStore(Qrfiles.this);
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
        if(isDestroyed())
            return;
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
                this.currQrSizevalue, this.currStartSeqTime, this.currDumpPath, this.pref_is_blurshader);
        newFragment.setTransmissionContorller(this);
        newFragment.setUserPreviewContorller(this);
        newFragment.show(ft, "dialog");

    }

    private boolean helpdialogshown = false;
    void showHelpDialog() {
        if(isDestroyed())
            return;
        FragmentTransaction ft = getFragmentManager().beginTransaction();
        // Create and show the dialog.
        HelpFragment newFragment = HelpFragment.newInstance();
        newFragment.setHelpDialogDismisser(this);
        newFragment.setStartDismissStatus(this.pref_is_dismiss_help);
        newFragment.show(ft, "dialog");
        this.helpdialogshown = true;
    }

    void showAboutDialog() {
        if(isDestroyed())
            return;
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

    private boolean user_pref_prev_index_loaded = false;
    private void readAndSanitizePrefs(){
        this.preferences = this.getPreferences(Context.MODE_PRIVATE);

        int fps = this.preferences.getInt("FPS", 17);
        int errlev = this.preferences.getInt("Errlevel", 50);
        int qrsize = this.preferences.getInt("Qrsize", 585);
        int timeout = this.preferences.getInt("timeout", 6);
        int suggN = this.preferences.getInt("suggested_N", 511);
        String fdumppath = this.preferences.getString("filedumppath", "Download");
        this.pref_is_dismiss_help = this.preferences.getBoolean("is_dismiss_help", false);
        this.pref_is_blurshader = this.preferences.getBoolean("is_blurshader", false);
        this.custom_prev_checked = this.preferences.getBoolean("is_customprevsizecheck", false);

        int tmpindx = this.preferences.getInt("prev_index", -1);
        Log.i("QQQVVV1", "t "+ tmpindx);
        if (tmpindx != -1){
            this.user_selected_camera_index = tmpindx;
            this.user_pref_prev_index_loaded = true;
            this.last_userindex_dispatched_to_camera = tmpindx;
        } else {
            this.user_pref_prev_index_loaded = false;
        }



        this.slider_prev_percent = this.preferences.getInt("slider_prev_percent", 666);
        this.slider_prev_percent = Qrfiles.clamp(this.slider_prev_percent, 0, 1000);

        this.currFPSvalue = Qrfiles.clamp(fps, 5, 60);
        this.currErrorvalue = Qrfiles.clamp(errlev, 20, 80);
        this.currQrSizevalue = Qrfiles.clamp(qrsize, 95, 1205);
        this.currStartSeqTime = Qrfiles.clamp(timeout, 3, 10);
        this.currsuggested_N = Qrfiles.clamp(suggN, 255, 1023);
        if (this.currsuggested_N != 255 || this.currsuggested_N != 511 || this.currsuggested_N != 1023)
            this.currsuggested_N = 511;
        this.currDumpPath = fdumppath;

    }


    boolean is_in_decoder_view = true;
    boolean is_in_qr_sender_view = false;
    boolean is_in_filechooser_view = false;
    @Override
    public void onResume(){
        //Log.i("ACTINFO", "Activity resumed");
        if(!this.HandleIsSDMountedProperly()){
            FinishActivity();
            super.onResume();
            return;
        }


        this.readAndSanitizePrefs();
        //sanitize size
        int smear = Qrfiles.smearCustom(Qrfiles.limit_max_received_file_size);
        if (smear != 5612912)
            FinishActivity();

        File yourAppDir = new File(CameraWorker.create_dump_directory_if_not_present(currDumpPath));
        default_search_for_upload_homedir = yourAppDir.getPath();

        if(yourAppDir.exists() && !yourAppDir.isDirectory()){
            currDumpPath+="2";
            yourAppDir = new File(CameraWorker.create_dump_directory_if_not_present(currDumpPath));
            default_search_for_upload_homedir = yourAppDir.getPath();
        }

        if(yourAppDir.exists() && !yourAppDir.isDirectory()){
            currDumpPath+="3";
            yourAppDir = new File(CameraWorker.create_dump_directory_if_not_present(currDumpPath));
            default_search_for_upload_homedir = yourAppDir.getPath();
        }

        if(yourAppDir.exists() && !yourAppDir.isDirectory()){
            currDumpPath+="8";
            yourAppDir = new File(CameraWorker.create_dump_directory_if_not_present(currDumpPath));
            default_search_for_upload_homedir = yourAppDir.getPath();
        }



        //setContentView(R.layout.activity_qrfiles);
        //Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        if (this.fileselection_dialog_in_sender != null)
            this.fileselection_dialog_in_sender.closeit();

        if (!this.got_upload_request_from_intent){
            this.is_in_qr_sender_view = false;
            this.switch_to_detector_view(false);
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
                //Log.i("clickable", "setting clickable to false");
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
                    Qrfiles.this.switch_to_detector_view(false);
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
        camSurf.setDumpFolderName(this.currDumpPath);
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

        //Log.i("ACTINFO", "Activity paused");
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
        last_encountered_uploader_path = null;

        this.preferences = this.getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = this.preferences.edit();
        editor.putInt("FPS", this.currFPSvalue);
        editor.putInt("Errlevel", this.currErrorvalue);
        editor.putInt("Qrsize", this.currQrSizevalue);
        editor.putInt("timeout", this.currStartSeqTime);
        editor.putInt("suggested_N", this.currsuggested_N);
        editor.putString("filedumppath", this.currDumpPath);
        editor.putBoolean("is_dismiss_help", this.pref_is_dismiss_help);
        editor.putBoolean("is_blurshader", this.pref_is_blurshader);
        editor.putBoolean("is_customprevsizecheck", this.custom_prev_checked);
        editor.putInt("prev_index", this.user_selected_camera_index);
        editor.putInt("slider_prev_percent", this.slider_prev_percent);
        editor.commit();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_qrfiles, menu);
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
    private void switch_to_detector_view(boolean mute_switch_toast){

        if (this.fileselection_dialog_in_sender != null)
            this.fileselection_dialog_in_sender.closeit();

        //Log.i("QTHRM", "about to destroy qrsurf resources");

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

        //Log.i("QTHRM", "destroyed qrsurf resources");
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

                //Log.i("clickable", "setting clickable to true1");
        //Log.i("clickable", "executed on the thread, id: " + android.os.Process.myTid());


        this.main_layout.setVisibility(View.VISIBLE);
        this.main_layout.requestLayout();

        if (!this.pref_is_dismiss_help && (!this.helpdialogshown)){
            this.helpdialogshown = true;
            new Timer().schedule(new TimerTask()
            {
                @Override
                public void run()
                {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showHelpDialog();
                        }
                    });
                }
            }, 1500);

        }

        if (mute_switch_toast == false)
            Toast.makeText(this, (String)this.getString(R.string.download_mode_switch_text),
                   Toast.LENGTH_SHORT).show();

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

        if (this.qrsurf != null)
            this.qrsurf.setup_initial_brightness();

        this.qrsurf.setFPS(16.0);
        this.qrsurf.set_header_display_timeout(6.0);
        this.qrsurf.setZOrderOnTop(true);
        this.qrsurf.init_qrsurf_thread();//starts thread

        Qrfiles.this.set_activity_button_listeners();
        Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.icdown3);
        Qrfiles.this.uparrowbutton.setImageBitmap(bmp);
                uparrowbutton.setClickable(true);
                uparrowbutton.requestLayout();
                //Log.i("clickable", "setting clickable to true2");
                //Log.i("clickable", "executed on the thread, id: " + android.os.Process.myTid());

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

        adView = (AdView) this.findViewById(R.id.adViewUpl);

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
                                .addTestDevice("B8E41997327A892A4FD057B12AD2C843")
                                .addTestDevice("974226D4036724BE09FB80898BA1B4BE")
                                .addTestDevice("565FCDDC3CCE41C9B34DE6365FCF267C")
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

                boolean canread = QRSurface.checkFileCanRead(new File(this.upload_requested_path_by_system));
                if (!canread){
                    final String fname = this.upload_requested_path_by_system;
                    final Activity a = Qrfiles.this;
                    if (a != null){
                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                String p1 = a.getString(R.string.fileread_failed_descr1);
                                String p2 = a.getString(R.string.fileread_failed_descr2);
                                Toast d = Toast.makeText(a, p1+" "+fname+" "+p2, Toast.LENGTH_LONG);
                                d.show();
                            }
                        });
                    }
                    return;
                }

                files.add(0, this.upload_requested_path_by_system);
                Qrfiles.this.qrsurf.add_new_files_to_send((ArrayList<String>) files);
            }


        Toast.makeText(this, (String)this.getString(R.string.upload_mode_switch_text),
                Toast.LENGTH_SHORT).show();

            Runtime.getRuntime().gc();

    }

    private File last_encountered_uploader_path = null;
    private void invoke_file_window(boolean is_file_upload_selection_mode){
        if(is_file_upload_selection_mode){

            File yourAppDir = null;
            if (Environment.getExternalStorageState() != null){
                yourAppDir = Environment.getExternalStorageDirectory();
            }else {
                yourAppDir = Environment.getDataDirectory();
            }

            File whichpath = new File(yourAppDir.getAbsolutePath());
            if (last_encountered_uploader_path != null)
                whichpath = last_encountered_uploader_path;

            if (default_search_for_upload_homedir != null)
                this.fileselection_dialog_in_sender = new ChooserDialog().withCustomParentDir(null).with(this)
                        .withFilter(false, false)
                        .withStartFile(whichpath.getAbsolutePath())
                        .withDateFormat("HH:mm")
                        .withResources(R.string.title_choose_filetosend, R.string.title_choose, R.string.dialog_cancel)
                        .withChosenListener(new ChooserDialog.Result() {
                        @Override
                        public void onChoosePath(String path, File pathFile) {
                            boolean canread = QRSurface.checkFileCanRead(new File(path));
                            last_encountered_uploader_path = new File(path);
                            if (!canread){
                                final String fname = path;
                                final Activity a = Qrfiles.this;
                                if (a != null){
                                    a.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            String p1 = a.getString(R.string.fileread_failed_descr1);
                                            String p2 = a.getString(R.string.fileread_failed_descr2);
                                            Toast d = Toast.makeText(a, p1+" "+fname+" "+p2, Toast.LENGTH_LONG);
                                            d.show();
                                        }
                                    });
                                }
                                return;
                            }

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
                this.fileselection_dialog_in_sender = new ChooserDialog().withCustomParentDir(null).with(this)
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

    boolean pref_is_dismiss_help = false;
    boolean pref_is_blurshader = false;
    boolean pref_is_prevcheck = false;
    int pref_user_prev_index = 0;

    int currFPSvalue = 17;
    int currErrorvalue = 50;
    int currQrSizevalue = 585;
    int currStartSeqTime = 6;
    int currsuggested_N = 511;
    String currDumpPath = null;
    @Override
    public void onNewTransmissionSettings(int fps, int errlevpercent, int qrsize, int startseqtime, String newdumppath, boolean is_blur) {
        //Log.i("Settings", "got new settings from settingsmenu, fps : "+fps+", err : "+errlevpercent
        //        + ", qrsize : "+qrsize+", sseq : "+startseqtime+", dumppath : "+newdumppath);
        this.currFPSvalue = fps;
        this.currErrorvalue = errlevpercent;
        this.currQrSizevalue = qrsize;
        this.currStartSeqTime = startseqtime;

        this.pref_is_blurshader = is_blur;
        if (this.camworker != null)
            this.camworker.set_is_blur(this.pref_is_blurshader);




        if (!newdumppath.equals(this.currDumpPath)){
            this.currDumpPath = newdumppath;
            File yourAppDir = new File(CameraWorker.create_dump_directory_if_not_present(currDumpPath));
            default_search_for_upload_homedir = yourAppDir.getPath();
            if (this.camworker != null)
                this.camworker.setNewDumpPath(yourAppDir.getAbsolutePath());
            //this.destroyall();
            //this.initall();
        }


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
        //Log.i("UIThr", "executed on the UI thread, id: " + android.os.Process.myTid());
        this.camworker = new CameraWorker("CameraDetectorThread");

        this.camworker.does_not_know_optimal_index_yet = this.does_not_know_optimal_index_yet;

        this.camworker.user_selected_camera_index = this.user_selected_camera_index;

        if (this.user_selected_camera_index > -1)
            this.camworker.does_not_know_optimal_index_yet = false;
        if(this.camworker != null)
            this.camworker.set_prevsquare_size_percent(slider_prev_percent);
        this.camworker.setContext(this);
        this.camworker.start();
        this.camworker.waitUntilReady();


        //this.camworker.waitUntilReady();
        this.camworker.handler.post(new Runnable() {
            @Override
            public void run() {
                //Log.i("CamThr", "executed on the camera thread, id: " + android.os.Process.myTid());
            }
        });
        //this.camworker.initAsync();

        if(this.camworker != null)
            this.camworker.set_is_blur(this.pref_is_blurshader);
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

        this.last_userindex_dispatched_to_camera = this.user_selected_camera_index;
        if (this.camworker != null){
            this.last_userindex_dispatched_to_camera = this.camworker.user_selected_camera_index;
            if (this.camworker.user_selected_camera_index == -1 && this.camworker.automatically_deducted_camera_preview_index > -1)
                this.last_userindex_dispatched_to_camera = this.camworker.automatically_deducted_camera_preview_index;
        }


        new Timer().schedule(new TimerTask()
        { //unblock button after certain interval - hack
            @Override
            public void run()
            {
                if (camworker != null){
                    List<Camera.Size> s = getPreviewSizes(); // makes to load preview size on the init as well
                    if (s != null)
                        if(s.size() > 0){
                            does_not_know_optimal_index_yet = false;
                            camworker.does_not_know_optimal_index_yet = false;
                            automatically_deducted_camera_preview_index = camworker.automatically_deducted_camera_preview_index;
                        }
                }
            }
        }, 500);
        new Timer().schedule(new TimerTask()
        { //unblock button after certain interval - hack
            @Override
            public void run()
            {
                if (camworker != null){
                    List<Camera.Size> s = getPreviewSizes(); // makes to load preview size on the init as well
                    if (s != null)
                        if(s.size() > 0){
                            does_not_know_optimal_index_yet = false;
                            camworker.does_not_know_optimal_index_yet = false;
                            automatically_deducted_camera_preview_index = camworker.automatically_deducted_camera_preview_index;
                        }
                }
            }
        }, 1100);

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

    @Override
    public void onSetDismissedStatus(boolean status) {
        this.pref_is_dismiss_help = status;
    }

    @Override
    public void onSetHelpWindowGone() {
        this.helpdialogshown = false;
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


    List<Camera.Size> previev_list = null;
    @Override
    public List<Camera.Size> getPreviewSizes() {
        if (this.camworker != null){
            this.previev_list = this.camworker.previev_list;
            return this.camworker.previev_list;
        }
        else{
            return this.previev_list;
        }
    }

    int user_selected_camera_index = -1;
    int automatically_deducted_camera_preview_index = -1;
    boolean does_not_know_optimal_index_yet = true;
    int last_userindex_dispatched_to_camera = -1;
    @Override
    public void setUserPreviewIndex(int index) {
        if (this.last_userindex_dispatched_to_camera == -1){
            this.last_userindex_dispatched_to_camera = this.automatically_deducted_camera_preview_index;
        }

        if (this.camworker != null)
            if (this.camworker.automatically_deducted_camera_preview_index != -1)
                this.automatically_deducted_camera_preview_index = this.camworker.automatically_deducted_camera_preview_index;

        this.does_not_know_optimal_index_yet = false;
        this.user_selected_camera_index = index;
        if (this.camworker != null)
            this.camworker.user_selected_camera_index = index;
        //if (this.camworker != null)
        //    Log.i("UserPreviewSelect", "User selected " + index + " : " + this.camworker.previev_list.get(index).width + "x" + this.camworker.previev_list.get(index).height);
        if (is_in_decoder_view){
            if (this.last_userindex_dispatched_to_camera != index){
                destroyall();Log.i("PPPPPPPPPPQQWE","wtf dangernn, camw useri "+this.user_selected_camera_index + " last "+this.last_userindex_dispatched_to_camera);
                switch_to_detector_view(true);
            }
        }
        this.last_userindex_dispatched_to_camera = index;
    }

    @Override
    public int getCurrUserPreviewIndex() {
        if (this.last_userindex_dispatched_to_camera == -1){
            this.last_userindex_dispatched_to_camera = this.automatically_deducted_camera_preview_index;
        }
        if (this.camworker != null)
            if (this.camworker.automatically_deducted_camera_preview_index != -1){
                this.automatically_deducted_camera_preview_index = this.camworker.automatically_deducted_camera_preview_index;

            }
        return user_selected_camera_index;
    }

    @Override
    public int getProposedDefaultOptimalPrevievIndex() {
        if (this.camworker != null) {
            if (this.camworker.automatically_deducted_camera_preview_index != -1){
                this.automatically_deducted_camera_preview_index = this.camworker.automatically_deducted_camera_preview_index;

            }
            if (this.last_userindex_dispatched_to_camera == -1){
                this.last_userindex_dispatched_to_camera = this.automatically_deducted_camera_preview_index;
            }
            return this.automatically_deducted_camera_preview_index;
        }
        else{
            if (this.last_userindex_dispatched_to_camera == -1){
                this.last_userindex_dispatched_to_camera = this.automatically_deducted_camera_preview_index;
            }
            return this.automatically_deducted_camera_preview_index;
        }
    }

    public int getStartUpIndexToConstructList(){
        if (this.does_not_know_optimal_index_yet == true && !user_pref_prev_index_loaded) {
            if (this.last_userindex_dispatched_to_camera == -1){
                this.last_userindex_dispatched_to_camera = this.automatically_deducted_camera_preview_index;
            }
            if (this.camworker != null){

                if (this.camworker.automatically_deducted_camera_preview_index != -1)
                    this.automatically_deducted_camera_preview_index = this.camworker.automatically_deducted_camera_preview_index;
                return this.automatically_deducted_camera_preview_index;
            }
            else{
                if (this.last_userindex_dispatched_to_camera == -1){
                    this.last_userindex_dispatched_to_camera = this.automatically_deducted_camera_preview_index;
                }

                return user_selected_camera_index;
            }
        }

        if (this.camworker != null){
            if (this.camworker.automatically_deducted_camera_preview_index != -1)
                this.automatically_deducted_camera_preview_index = this.camworker.automatically_deducted_camera_preview_index;

        }
        if (this.last_userindex_dispatched_to_camera == -1){
            this.last_userindex_dispatched_to_camera = this.automatically_deducted_camera_preview_index;
        }

        return user_selected_camera_index;
    }

    public int getCalculatedOptimalIndex(){

        if (this.camworker != null) {
            if (this.camworker.automatically_deducted_camera_preview_index != -1)
                this.automatically_deducted_camera_preview_index = this.camworker.automatically_deducted_camera_preview_index;

        }
        if (this.last_userindex_dispatched_to_camera == -1){
            this.last_userindex_dispatched_to_camera = this.automatically_deducted_camera_preview_index;
        }

        return this.automatically_deducted_camera_preview_index;
    };

    boolean custom_prev_checked = false;
    public boolean getCheckedCustomPrev(){
        return custom_prev_checked;
    };

    public void setCheckedCustomPrev(boolean new_user_check){
        this.custom_prev_checked = new_user_check;
    };


    public int slider_prev_percent = 666; //promile

    public void setUserAlignerSquarePrev(int perc){
        this.slider_prev_percent = perc;//Log.i("QQQKOP", "perd "+perc);
        if(this.camworker != null)
            this.camworker.set_prevsquare_size_percent(perc);
    };

    public int getUserAlignerSquarePrev(){
        return this.slider_prev_percent;
    };
}
