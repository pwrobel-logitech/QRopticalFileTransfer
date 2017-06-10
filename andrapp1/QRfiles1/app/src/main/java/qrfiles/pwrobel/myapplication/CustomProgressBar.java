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
    }

    public CustomProgressBar(Context context) {
        super(context);
        this.setZOrderOnTop(true);
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
        synchronized (holder) {
            Log.i("PRBAR", "progressbar surface created");
            this.is_operational = true;
        }
    }

    @Override
    public synchronized void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        synchronized (holder) {
            Log.i("PRBAR", "progressbar surface changed");
            this.is_operational = true;
        }
    }

    @Override
    public synchronized void surfaceDestroyed(SurfaceHolder holder) {
        synchronized (holder){
            this.is_operational = false;
        }
    }

    public void drawMe(){

        //if (this.is_operational == false)
        //    return;

        Canvas c = null;

        try {
            c = this.getHolder().lockCanvas();
            if (c != null){
                synchronized (this.getHolder()) {
//////////////////////////////////////////////////
                    this.setZOrderOnTop(true);
                    int w = this.getWidth();
                    int h = this.getHeight();
                    Log.i("PRBAR", "drawMe w" + w + " h "+h);
                    //c.drawColor(Color.argb(100, 50, 50, 50));
                    this.getHolder().setFormat(PixelFormat.TRANSPARENT);
                    c.drawCircle(w/2, h/2, h/2, rectanglePaint);
//////////////////////////////////////////////////
                }
            }
        }
        finally {
            if (c != null) {
                try {
                    this.getHolder().unlockCanvasAndPost(c);
                }catch (IllegalFormatException e){
                    Log.i("PRBAR", "failed unlockCanvasAndPost");
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
