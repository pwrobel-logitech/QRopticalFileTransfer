package pl.pwrobel.opticalfiletransfer;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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



    public enum progressBarType {NOISE, PROGRESS, TIMEOUT};

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

    private double time_limit_for_timeout = 0;
    public synchronized void setTimeLimitForDrawingTimeout(double sec){
        this.time_limit_for_timeout = sec;
    }

    private String filename = "";
    public synchronized void setFileName(String fname){
        if (fname != null)
            this.filename = fname;
    }

    private Rect textrect = new Rect();
    public synchronized void drawMe(long progr, progressBarType type, boolean shoulddraw) {
        if (shoulddraw == false){
            return;
        }
        synchronized (this){

            if (progr > 1000)
                progr = 1000;
            if (progr < 0)
                progr = 0;
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

                    int w = this.getWidth();
                    int h = this.getHeight();



                    //Log.i("PRBAR", "drawMe w" + w + " h "+h);
                    //c.drawColor(Color.argb(100, 50, 50, 50));
                    //transparancy and blending is causing inermittient crashes in unlockCanvasAndPost
                    //this.getHolder().setFormat(PixelFormat.TRANSPARENT);
                    int epsilon = 4;
                    //c.drawColor(Color.CYAN);
                    if (shoulddraw) {
                        //this.setZOrderOnTop(true);
                        c.drawColor(0, PorterDuff.Mode.CLEAR);


                        //set color
                        int redratio = (int) ((progr / 1000.0) * 255);
                        if (redratio > 255)
                            redratio = 255;
                        if (redratio < 0)
                            redratio = 0;
                        int greenratio = 255 - redratio;
                        ;
                        ///

                        String add_text = "";
                        if (type == progressBarType.NOISE) {

                            rectanglePaint.setColor(Color.rgb(redratio, greenratio, 0));
                        } else if (type == progressBarType.PROGRESS) {
                            rectanglePaint.setColor(Color.rgb(0x55, 0x55, 0xff));
                        } else if (type == progressBarType.TIMEOUT) {
                            rectanglePaint.setColor(Color.rgb(0xff, 0x88, 0x55));
                        }

                        c.drawColor(Color.rgb(0x7f, 0x7f, 0x7f));

                        if (type != progressBarType.TIMEOUT) {
                            c.drawRect(new Rect(epsilon, epsilon, (int) (w * (progr / 1000.0f)) - epsilon, h - epsilon), rectanglePaint);
                        }else{
                            c.drawRect(new Rect((int)(w - w * (progr / 1000.0f))+epsilon, epsilon, w - epsilon, h - epsilon), rectanglePaint);
                        }

                        if (type == progressBarType.PROGRESS) {
                            rectanglePaint.setColor(Color.BLACK);
                            rectanglePaint.setTextSize(32);
                            String stringtodraw = progr/10.0f+" %";
                            rectanglePaint.getTextBounds(stringtodraw, 0, stringtodraw.length(), this.textrect);
                            c.drawText(stringtodraw, (int)(w/2.0-this.textrect.width()/2.0),
                                    (int)(h/2.0+this.textrect.height()/2.0), rectanglePaint);
                        }

                        if (type == progressBarType.TIMEOUT){
                            double timeval = ((this.time_limit_for_timeout-(progr/1000.0)*this.time_limit_for_timeout));
                            rectanglePaint.setColor(Color.BLACK);
                            rectanglePaint.setTextSize(32);
                            String stringtodraw = this.filename+String.format( "%.2f", timeval)+"s";
                            Log.i("STRQ", stringtodraw);
                            rectanglePaint.getTextBounds(stringtodraw, 0, stringtodraw.length(), this.textrect);
                            c.drawText(stringtodraw, (int)(w/2.0-this.textrect.width()/2.0),
                                       (int)(h/2.0+this.textrect.height()/2.0), rectanglePaint);
                            //-20+w/2.0f, 0.68f*h
                        }

                        if (type == progressBarType.NOISE){

                            add_text = this.filename;
                            rectanglePaint.setColor(Color.BLACK);
                            rectanglePaint.setTextSize(32);


                            rectanglePaint.getTextBounds(add_text, 0, add_text.length(), this.textrect);
                            c.drawText(add_text, (int)(w/2.0-this.textrect.width()/2.0),
                                    (int)(h/2.0+this.textrect.height()/2.0), rectanglePaint);
                        }


                        //c.drawColor(Color.TRANSPARENT);
                        //c.drawARGB(127, 180, 180, 180);
                        //this.getHolder().setFormat(PixelFormat.TRANSPARENT);
                        //c.drawCircle(w/2, h/2, h/2, rectanglePaint);
                    }else{
                        //this.setZOrderOnTop(false);
                        //this.getHolder().setFormat(PixelFormat.TRANSPARENT);
                        c.drawColor(0, PorterDuff.Mode.CLEAR);
                    }
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
