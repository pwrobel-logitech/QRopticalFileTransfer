package qrfiles.pwrobel.myapplication;

import android.app.Dialog;
import android.app.DialogFragment;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.TextView;

/**
 * Created by pwrobel on 25.06.17.
 */

public class SettingsFragment extends DialogFragment {

    int mNum;

    /**
     * Create a new instance of MyDialogFragment, providing "num"
     * as an argument.
     */
    static SettingsFragment newInstance() {
        SettingsFragment f = new SettingsFragment();

        // Supply num input as an argument.
        //Bundle args = new Bundle();
        //args.putInt("num", num);
        //f.setArguments(args);



        return f;
    }


    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            //dialog.getWindow().setLayout(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
            //dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            dialog.setTitle("ABCCC");



        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //mNum = getArguments().getInt("num");

        //if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
        //setStyle(STYLE_NO_TITLE, android.R.style.Theme_Black_NoTitleBar_Fullscreen);

       // } else {
         //   setStyle(STYLE_NO_TITLE, android.R.style.Theme_DeviceDefault_Light_NoActionBar);
       // }



        // Pick a style based on the num.
        //int style = DialogFragment.STYLE_NORMAL, theme = 0;
        //this.getDialog().getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
        //        WindowManager.LayoutParams.FLAG_FULLSCREEN);
        /*
        switch ((mNum-1)%6) {
            case 1: style = DialogFragment.STYLE_NO_TITLE; break;
            case 2: style = DialogFragment.STYLE_NO_FRAME; break;
            case 3: style = DialogFragment.STYLE_NO_INPUT; break;
            case 4: style = DialogFragment.STYLE_NORMAL; break;
            case 5: style = DialogFragment.STYLE_NORMAL; break;
            case 6: style = DialogFragment.STYLE_NO_TITLE; break;
            case 7: style = DialogFragment.STYLE_NO_FRAME; break;
            case 8: style = DialogFragment.STYLE_NORMAL; break;
        }
        switch ((mNum-1)%6) {
            case 4: theme = android.R.style.Theme_Holo; break;
            case 5: theme = android.R.style.Theme_Holo_Light_Dialog; break;
            case 6: theme = android.R.style.Theme_Holo_Light; break;
            case 7: theme = android.R.style.Theme_Holo_Light_Panel; break;
            case 8: theme = android.R.style.Theme_Holo_Light; break;
        }*/
        //setStyle(style, theme);
    }

    int currFPSvalue = 17;
    int currErrorvalue = 50;
    int currQrSizevalue = 585;
    int currStartSeqTime = 7;
    HorizontalNumberPicker numberPickerFPS = null;
    HorizontalNumberPicker numberPickerError = null;
    HorizontalNumberPicker numberPickerQrsize = null;
    HorizontalNumberPicker numberPickerStartSeqTime = null;
    private int picker_obtainedFPS = -1;
    private int picker_obtainedError = -1;
    private int picker_obtainedQrsize = -1;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_fragment, container, false);
        //View tv = v.findViewById(R.id.text);
        //((TextView)tv).setText("Dialog #" + mNum + ": using style ");


        numberPickerFPS = (HorizontalNumberPicker) v.findViewById(R.id.numberPickerFPS);
        numberPickerFPS.setMinValue(5);
        numberPickerFPS.setMaxValue(60);
        numberPickerFPS.setValue(currFPSvalue);
        numberPickerFPS.setOnLongPressUpdateInterval(200);
        numberPickerFPS.setListener(new HorizontalNumberPickerListener() {
            @Override
            public void onHorizontalNumberPickerChanged(HorizontalNumberPicker horizontalNumberPicker, int value) {
                Log.i("Settings", "got new FPS : "+value);
                currFPSvalue = value;
                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
            }
        });

        numberPickerError = (HorizontalNumberPicker) v.findViewById(R.id.numberPickerError);
        numberPickerError.setMinValue(15);
        numberPickerError.setMaxValue(85);
        numberPickerError.setValue(currErrorvalue);
        numberPickerError.setStepSize(5);
        numberPickerError.setOnLongPressUpdateInterval(300);
        numberPickerError.setListener(new HorizontalNumberPickerListener() {
            @Override
            public void onHorizontalNumberPickerChanged(HorizontalNumberPicker horizontalNumberPicker, int value) {
                Log.i("Settings", "got new errval : "+value);
                currErrorvalue = value;
                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
            }
        });

        numberPickerQrsize = (HorizontalNumberPicker) v.findViewById(R.id.numberPickerQrsize);
        numberPickerQrsize.setMinValue(90);
        numberPickerQrsize.setMaxValue(1500);
        numberPickerQrsize.setValue(currQrSizevalue);
        numberPickerQrsize.setStepSize(10);
        numberPickerQrsize.setOnLongPressUpdateInterval(200);
        numberPickerQrsize.setListener(new HorizontalNumberPickerListener() {
            @Override
            public void onHorizontalNumberPickerChanged(HorizontalNumberPicker horizontalNumberPicker, int value) {
                Log.i("Settings", "got new QRsize : "+value);
                currQrSizevalue = value;
                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
            }
        });

        numberPickerStartSeqTime = (HorizontalNumberPicker) v.findViewById(R.id.numberPickerStartSeqTime);
        numberPickerStartSeqTime.setMinValue(3);
        numberPickerStartSeqTime.setMaxValue(30);
        numberPickerStartSeqTime.setValue(currStartSeqTime);
        numberPickerStartSeqTime.setStepSize(1);
        numberPickerStartSeqTime.setOnLongPressUpdateInterval(200);
        numberPickerStartSeqTime.setListener(new HorizontalNumberPickerListener() {
            @Override
            public void onHorizontalNumberPickerChanged(HorizontalNumberPicker horizontalNumberPicker, int value) {
                Log.i("Settings", "got new start time : "+value);
                currStartSeqTime = value;
                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
            }
        });


        return v;
    }


    private TransmissionController transmissionController = null;
    public void setTransmissionContorller(TransmissionController tc){
        this.transmissionController = tc;
    }

    private void request_resetting_encoder_because_of_new_settings(){
        if (this.transmissionController != null){
            this.transmissionController.onNewTransmissionSettings(this.currFPSvalue, this.currErrorvalue,
                    this.currQrSizevalue, this.currStartSeqTime, null);
        }
    }

}
