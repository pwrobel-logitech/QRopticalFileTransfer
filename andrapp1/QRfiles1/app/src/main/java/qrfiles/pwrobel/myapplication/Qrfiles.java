package qrfiles.pwrobel.myapplication;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                if (Qrfiles.this.is_in_decoder_view) {
                    Qrfiles.this.switch_to_qrsender_view();
                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.down_arrow_icon);
                    Qrfiles.this.uparrowbutton.setImageBitmap(bmp);
                    Qrfiles.this.uparrowbutton.requestLayout();
                    Qrfiles.this.is_in_decoder_view = false;
                    Qrfiles.this.is_in_qr_sender_view = true;
                    return;
                }
                if (!Qrfiles.this.is_in_decoder_view) {
                    Qrfiles.this.switch_to_detector_view();
                    Bitmap bmp = BitmapFactory.decodeResource(getResources(), R.mipmap.up_arrow_icon);
                    Qrfiles.this.uparrowbutton.setImageBitmap(bmp);
                    Qrfiles.this.uparrowbutton.requestLayout();
                    Qrfiles.this.is_in_decoder_view = true;
                    Qrfiles.this.is_in_qr_sender_view = false;
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


    private synchronized void switch_to_detector_view(){

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


        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        // Example of a call to a native method
        //TextView tv = (TextView) findViewById(R.id.sample_text);
        //tv.setText(stringFromJNI());


        this.detector_view.setVisibility(View.VISIBLE);
        this.qrsender_view.setVisibility(View.GONE);
        this.detector_view.requestLayout();
        this.qrsender_view.requestLayout();
        this.initall();
    }

    private synchronized void switch_to_qrsender_view(){
        this.destroyall();  //destroy camera and detector stuff
        this.detector_view.setVisibility(View.GONE);
        this.qrsender_view.setVisibility(View.VISIBLE);
        this.detector_view.requestLayout();
        this.qrsender_view.requestLayout();
        //set_decoder_elements();
    }


    private synchronized void initall(){
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


    }

    private synchronized void destroyall(){




        if(this.camworker != null)
            this.camworker.closeCamAsync();
        if(this.camSurf != null)
            this.camSurf.deinitialize_resources();

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
        System.loadLibrary("RSdecAPI");
        System.loadLibrary("native-lib");
    }



}
