package qrfiles.pwrobel.myapplication;

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

public class Qrfiles extends AppCompatActivity {


    //used to hold main camera thread with the higher priority
    CameraWorker camworker;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrfiles);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        this.initall();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    // Example of a call to a native method
    //TextView tv = (TextView) findViewById(R.id.sample_text);
    //tv.setText(stringFromJNI());
    }

    @Override
    public void onPause(){
        super.onPause();
        this.camworker.closeCamAsync();
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