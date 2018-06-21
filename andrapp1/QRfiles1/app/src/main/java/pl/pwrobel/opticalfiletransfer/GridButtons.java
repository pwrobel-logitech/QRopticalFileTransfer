package pl.pwrobel.opticalfiletransfer;

import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.RadioGroup;

/**
 * Created by pwrobel on 22.06.18.
 */

public class GridButtons extends RadioGroup {

    public GridButtons(Context context) {
        super(context);
        init(context);
    }

    public GridButtons(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }


    private void init(Context context) {
        final Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        Point deviceDisplay = new Point();
        display.getSize(deviceDisplay);
        deviceWidth = 1;//deviceDisplay.x;
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b){
        final int count = getChildCount();

        int maxw = 0;
        for (int i = 0; i < count; i++){
            View child = getChildAt(i);
            if (maxw < child.getMeasuredWidth())
                maxw = child.getMeasuredWidth();
        }

        if (deviceWidth > 2 * maxw) {

            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (i % 2 == 0)
                    child.layout(0, (i/2)*child.getMeasuredHeight(),
                            maxw, (i/2+1)*child.getMeasuredHeight());
                else
                    child.layout(maxw, ((i-1)/2)*child.getMeasuredHeight(),
                            2*maxw, ((i+1)/2)*child.getMeasuredHeight());

            }
        } else {
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);

                child.layout(0, (i)*child.getMeasuredHeight(),
                        maxw, (i+1)*child.getMeasuredHeight());


            }
        }
    }

    int deviceWidth;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        //uper.onMeasure(widthMeasureSpec, widthMeasureSpec);
        final int count = getChildCount();

        for(int i=0; i<count;i++) {
            View v = getChildAt(i);
            v.measure(widthMeasureSpec/2, v.getMeasuredHeight());
        }

        int maxw = 0;
        int maxh = 0;
        for (int i = 0; i < count; i++){
            View child = getChildAt(i);
            if (maxw < child.getMeasuredWidth())
                maxw = child.getMeasuredWidth();
            if (maxh < child.getMeasuredHeight())
                maxh = child.getMeasuredHeight();
        }

        if (deviceWidth > 2 * maxw){
            int ih = count/2;
            if (count % 2 == 1)
                ih = count / 2 + 1;
            setMeasuredDimension(2*maxw, ih * maxh);
        } else {
            setMeasuredDimension(maxw, count * maxh);
        }


    }

}
