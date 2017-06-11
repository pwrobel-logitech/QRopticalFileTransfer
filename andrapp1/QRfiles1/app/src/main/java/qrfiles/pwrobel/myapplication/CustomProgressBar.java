package qrfiles.pwrobel.myapplication;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import java.util.IllegalFormatException;

/**
 * Created by pwrobel on 10.06.17.
 */

public class CustomProgressBar extends SurfaceView implements SurfaceHolder.Callback, View.OnTouchListener {


    private final Paint rectanglePaint = new Paint();

    boolean is_operational = false;

    public CustomProgressBar(Context context, AttributeSet attrs){
        super(context, attrs);
        this.setZOrderOnTop(true);
        this.getHolder().addCallback(this);
    }

    public CustomProgressBar(Context context) {
        super(context);
        this.setZOrderOnTop(true);
        this.getHolder().addCallback(this);
        //this.setZOrderOnTop(true);
        //this.invalidate();
    }

    /*
    @Override
    protected void onDraw(Canvas canvas) {

        Log.i("PRBAR", "onDraw called on the custom progress bar");
        rectanglePaint.setColor(Color.CYAN);
        canvas.drawRect(new Rect(0,0,100,200), rectanglePaint);
        super.onDraw(canvas);
    }
*/

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //synchronized (holder) {
            Log.i("PRBAR", "progressbar surface created");
            this.is_operational = true;
        //}

    }

    @Override
    public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        //synchronized (holder) {
            Log.i("PRBAR", "progressbar surface changed");
            this.is_operational = true;
        //}
    }

    @Override
    public synchronized void surfaceDestroyed(SurfaceHolder holder) {
        //synchronized (holder){//
            Log.i("PRBAR", "progressbar surface destroyed");
            this.is_operational = false;
        //}
    }

    public synchronized void drawMe(long progr) {
        synchronized (this){
        boolean operational = false;

        SurfaceHolder shold;
        synchronized (this.getHolder()) {
            shold = this.getHolder();


            operational = this.is_operational;
        }

        if (!operational) {
            Log.i("PRBAR", "not oprational, returning");
            return;
        }


        Canvas c = null;

        boolean exception_got_when_locking = false;
        boolean locked = true;

        synchronized (shold) {

            Surface surface = shold.getSurface();
            if (surface != null && surface.isValid() && this.is_operational)
            {

            try {

                ///https://stackoverflow.com/questions/26987728/java-lang-illegalargumentexceptionat-android-view-surface-unlockcanvasandpostn
                ///https://stackoverflow.com/questions/13535912/illegalargumentexception-when-switching-to-settings-activity-in-live-wallpaper
                try {
                    if (this.is_operational)
                        c = shold.lockCanvas();
                }catch (IllegalArgumentException e){
                    locked = false;
                    Log.i("PRBAR", "lock canvas has thrown");
                }
                if (c != null ) {
//////////////////////////////////////////////////
                    this.setZOrderOnTop(true);
                    int w = this.getWidth();
                    int h = this.getHeight();

                    //Log.i("PRBAR", "drawMe w" + w + " h "+h);
                    //c.drawColor(Color.argb(100, 50, 50, 50));
                    //transparancy and blending is causing inermittient crashes in unlockCanvasAndPost
                    //this.getHolder().setFormat(PixelFormat.TRANSPARENT);
                    int epsilon = 4;
                    //c.drawColor(Color.CYAN);
                    c.drawColor(0, PorterDuff.Mode.CLEAR);
                    rectanglePaint.setColor(Color.BLUE);
                    c.drawColor(Color.DKGRAY);

                    c.drawRect(new Rect(epsilon, epsilon, (int) (w * progr / 1000.0f) - epsilon, h - epsilon), rectanglePaint);
                    //c.drawColor(Color.TRANSPARENT);
                    //c.drawARGB(127, 180, 180, 180);
                    //this.getHolder().setFormat(PixelFormat.TRANSPARENT);
                    //c.drawCircle(w/2, h/2, h/2, rectanglePaint);
//////////////////////////////////////////////////
                }

            } catch (Exception e) {
                Log.i("PRBAR", "Got exception when locking and drawing surf");
                //exception_got_when_locking = true;///
            } finally {
                if (c != null && shold != null /*&& !exception_got_when_locking*/ && locked) {
                    try {
                        if (this.is_operational)
                            shold.unlockCanvasAndPost(c);
                    } catch (IllegalFormatException e) {
                        Log.i("PRBAR", "failed unlockCanvasAndPost");
                    }
                }
            }
        }
    }
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        Log.i("PRBAR", "on touch in the progressbar");
        return false;
    }
}
