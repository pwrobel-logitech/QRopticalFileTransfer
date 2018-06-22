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
        deviceWidth = deviceDisplay.x;
    }

    private double margin_factor = 0.3;


    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b){
        final int count = getChildCount();

        int maxw = 0;
        int maxh = 0;
        for (int i = 0; i < count; i++){
            View child = getChildAt(i);
            if (maxw < child.getMeasuredWidth())
                maxw = child.getMeasuredWidth();
            if (maxh < child.getMeasuredHeight())
                maxh = child.getMeasuredHeight();
        }


        int tab_offx = (int)(maxw * margin_factor);
        int tab_offy = (int)(maxh * margin_factor);

        if (deviceWidth > 2 * (1.0+margin_factor)*maxw) {

            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);
                if (i % 2 == 0)
                    child.layout(tab_offx/2, tab_offy/2 + (int)((i/2)*(1.0+margin_factor)*maxh),
                            (int)((1.0+margin_factor)*maxw), (int)((1.0+margin_factor)*(i/2+1)*maxh));
                else
                    child.layout((int)((1.0+margin_factor)*maxw+tab_offx/2), tab_offy/2+(int)(((i-1)/2)*(1.0+margin_factor)*maxh),
                            (int)(2*(1.0+margin_factor)*maxw), (int)(((i+1)/2)*(1.0+margin_factor)*maxh));

            }
        } else {
            for (int i = 0; i < count; i++) {
                View child = getChildAt(i);

                child.layout(tab_offx/2, tab_offy/2+(int)((i)*(1.0+margin_factor)*maxh),
                        tab_offx/2+(int)((1.0+margin_factor)*maxw), (int)((i+1)*(1.0+margin_factor)*maxh));


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

        if (deviceWidth > 2 * (1.0+margin_factor)*maxw){
            int ih = count/2;
            if (count % 2 == 1)
                ih = count / 2 + 1;
            setMeasuredDimension((int)(2*(1.0+margin_factor)*maxw), (int)(ih * (1.0+margin_factor)*maxh));
        } else {
            setMeasuredDimension((int)((1.0+margin_factor)*maxw), (int)((1.0+margin_factor)*count * maxh));
        }


    }

}
