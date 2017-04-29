package qrfiles.pwrobel.myapplication;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.os.Process;

/**
 * Created by pwrobel on 29.04.17.
 */

public class CameraWorker extends HandlerThread {

    public volatile Handler handler;

    public CameraWorker(String name) {
        super(name, Process.THREAD_PRIORITY_URGENT_DISPLAY);
    }


    /*
    @Override
    public synchronized void run() {
        super.run();
    }
*/
    /*
    @Override
    protected synchronized void onLooperPrepared() {
        handler = new Handler(getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                // process incoming messages here
                // this will run in non-ui/background thread
            }
        };
    }
    */

    public synchronized void waitUntilReady() {
        handler = new Handler(getLooper());
    }
}
