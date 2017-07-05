package qrfiles.pwrobel.myapplication;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;

import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
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


    private TextView textviewUploadSpeedInfo = null;
    private void setEstimatedUploadSpeedInfo(){
        double speed = 0;
        speed = currFPSvalue * (1.0 - ((double)currErrorvalue)/100.0) * (((double)currQrSizevalue-4.0)/1024.0);
        String currtext = getResources().getString(R.string.resulting_upload_speed) +
                String.format( " %.2f KB/s", speed);
        this.textviewUploadSpeedInfo.setText(currtext);
        this.textviewUploadSpeedInfo.requestLayout();

    }


    int currFPSvalue = 17;
    int currErrorvalue = 50;
    int currQrSizevalue = 585;
    int currStartSeqTime = 6;
    String currFileDumpPath = null;
    HorizontalNumberPicker numberPickerFPS = null;
    HorizontalNumberPicker numberPickerError = null;
    HorizontalNumberPicker numberPickerQrsize = null;
    HorizontalNumberPicker numberPickerStartSeqTime = null;
    Button restore_default_settings_button = null;
    private int picker_obtainedFPS = -1;
    private int picker_obtainedError = -1;
    private int picker_obtainedQrsize = -1;
    FloatingActionButton floatingActionButtonq1 = null;
    FloatingActionButton floatingActionButtonq2 = null;
    FloatingActionButton floatingActionButtonq3 = null;
    FloatingActionButton floatingActionButtonq4 = null;
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
        numberPickerFPS.setOnLongPressUpdateInterval(120);

        final ColorStateList def_blackcol_fps = numberPickerFPS.getTextValueView().getTextColors();

        int value0 = currFPSvalue;
        if (value0 <= 20){
            numberPickerFPS.getTextValueView().setTextColor(def_blackcol_fps);
        }
        if (value0 == 21) {
            numberPickerFPS.getTextValueView().setTextColor(Color.rgb(0x7f, 0x00, 0x00));
        } else if (value0 == 22) {
            numberPickerFPS.getTextValueView().setTextColor(Color.rgb(0x9f, 0x00, 0x00));
        } else if (value0 >= 23){
            numberPickerFPS.getTextValueView().setTextColor(Color.rgb(0xaf, 0x00, 0x00));
        }


        numberPickerFPS.setListener(new HorizontalNumberPickerListener() {
            @Override
            public void onHorizontalNumberPickerChanged(HorizontalNumberPicker horizontalNumberPicker, int value) {
                Log.i("Settings", "got new FPS : "+value);
                currFPSvalue = value;



                if (value <= 20){
                    horizontalNumberPicker.getTextValueView().setTextColor(def_blackcol_fps);
                }
                if (value == 21) {
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0x7f, 0x00, 0x00));
                } else if (value == 22) {
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0x9f, 0x00, 0x00));
                } else if (value >= 23){
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0xaf, 0x00, 0x00));
                }


                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
                SettingsFragment.this.setEstimatedUploadSpeedInfo();
            }
        });



        numberPickerError = (HorizontalNumberPicker) v.findViewById(R.id.numberPickerError);
        numberPickerError.setMinValue(20);
        numberPickerError.setMaxValue(80);
        numberPickerError.setValue(currErrorvalue);
        numberPickerError.setStepSize(5);
        numberPickerError.setOnLongPressUpdateInterval(120);

        final ColorStateList def_blackcol_err = numberPickerError.getTextValueView().getTextColors();

        int value01 = currErrorvalue;
        if (value01 > 40){
            numberPickerError.getTextValueView().setTextColor(def_blackcol_fps);
        }
        if (value01 >= 35 && value01 <= 40) {
            numberPickerError.getTextValueView().setTextColor(Color.rgb(0x7f, 0x00, 0x00));
        } else if (value01 >= 30 && value01 <= 35) {
            numberPickerError.getTextValueView().setTextColor(Color.rgb(0x9f, 0x00, 0x00));
        } else if (value01 >= 25 && value01 <= 30){
            numberPickerError.getTextValueView().setTextColor(Color.rgb(0xaf, 0x00, 0x00));
        }

        numberPickerError.setListener(new HorizontalNumberPickerListener() {
            @Override
            public void onHorizontalNumberPickerChanged(HorizontalNumberPicker horizontalNumberPicker, int value) {
                Log.i("Settings", "got new errval : "+value);
                currErrorvalue = value;

                if (value > 40){
                    horizontalNumberPicker.getTextValueView().setTextColor(def_blackcol_err);
                }
                if (value >= 35 && value <= 40) {
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0x7f, 0x00, 0x00));
                } else if (value >= 30 && value <= 35) {
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0x9f, 0x00, 0x00));
                } else if (value >= 25 && value <= 30){
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0xaf, 0x00, 0x00));
                }


                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
                SettingsFragment.this.setEstimatedUploadSpeedInfo();
            }
        });



        numberPickerQrsize = (HorizontalNumberPicker) v.findViewById(R.id.numberPickerQrsize);
        numberPickerQrsize.setMinValue(95);
        numberPickerQrsize.setMaxValue(1205);
        numberPickerQrsize.setValue(currQrSizevalue);
        numberPickerQrsize.setStepSize(10);
        numberPickerQrsize.setOnLongPressUpdateInterval(120);
        final ColorStateList def_blackcol = numberPickerQrsize.getTextValueView().getTextColors();

        int value = currQrSizevalue;
        if (value <= 585){
            numberPickerQrsize.getTextValueView().setTextColor(def_blackcol);
        }
        if (value > 585 && value < 605) {
            numberPickerQrsize.getTextValueView().setTextColor(Color.rgb(0x7f, 0x00, 0x00));
        } else if (value >= 605 && value < 625) {
            numberPickerQrsize.getTextValueView().setTextColor(Color.rgb(0x9f, 0x00, 0x00));
        } else if (value >= 625){
            numberPickerQrsize.getTextValueView().setTextColor(Color.rgb(0xaf, 0x00, 0x00));
        }

        numberPickerQrsize.setListener(new HorizontalNumberPickerListener() {
            @Override
            public void onHorizontalNumberPickerChanged(HorizontalNumberPicker horizontalNumberPicker, int value) {
                Log.i("Settings", "got new QRsize : "+value);
                currQrSizevalue = value;
                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
                if (value <= 585){
                    horizontalNumberPicker.getTextValueView().setTextColor(def_blackcol);
                }
                if (value > 585 && value < 605) {
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0x7f, 0x00, 0x00));
                } else if (value >= 605 && value < 625) {
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0x9f, 0x00, 0x00));
                } else if (value >= 625){
                    horizontalNumberPicker.getTextValueView().setTextColor(Color.rgb(0xaf, 0x00, 0x00));
                }
                SettingsFragment.this.setEstimatedUploadSpeedInfo();
            }
        });

        numberPickerStartSeqTime = (HorizontalNumberPicker) v.findViewById(R.id.numberPickerStartSeqTime);
        numberPickerStartSeqTime.setMinValue(3);
        numberPickerStartSeqTime.setMaxValue(10);
        numberPickerStartSeqTime.setValue(currStartSeqTime);
        numberPickerStartSeqTime.setStepSize(1);
        numberPickerStartSeqTime.setOnLongPressUpdateInterval(100);
        numberPickerStartSeqTime.setListener(new HorizontalNumberPickerListener() {
            @Override
            public void onHorizontalNumberPickerChanged(HorizontalNumberPicker horizontalNumberPicker, int value) {
                Log.i("Settings", "got new start time : "+value);
                currStartSeqTime = value;

                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
                SettingsFragment.this.setEstimatedUploadSpeedInfo();
            }
        });


        this.restore_default_settings_button = (Button) v.findViewById(R.id.button_default_settings1);
        this.restore_default_settings_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SettingsFragment.this.numberPickerFPS.getTextValueView().setTextColor(def_blackcol_fps);
                SettingsFragment.this.numberPickerError.getTextValueView().setTextColor(def_blackcol_err);
                SettingsFragment.this.numberPickerQrsize.getTextValueView().setTextColor(def_blackcol);

                numberPickerFPS.setValue(17);
                numberPickerError.setValue(50);
                numberPickerQrsize.setValue(585);
                numberPickerStartSeqTime.setValue(6);

                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
                SettingsFragment.this.setEstimatedUploadSpeedInfo();

            }
        });

        this.textviewUploadSpeedInfo = (TextView) v.findViewById(R.id.textResultinguplspeed);
        this.setEstimatedUploadSpeedInfo();

        floatingActionButtonq1 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq1);
        floatingActionButtonq2 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq2);
        floatingActionButtonq3 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq3);
        floatingActionButtonq4 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq4);

        floatingActionButtonq1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.FragmentManager fm = getActivity().getFragmentManager();
                DialogQ1 q1 = new DialogQ1();
                q1.show(fm, "dialog");
            }
        });

        floatingActionButtonq2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.FragmentManager fm = getActivity().getFragmentManager();
                DialogQ2 q2 = new DialogQ2();
                q2.show(fm, "dialog");
            }
        });

        floatingActionButtonq3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.FragmentManager fm = getActivity().getFragmentManager();
                DialogQ3 q3 = new DialogQ3();
                q3.show(fm, "dialog");
            }
        });

        floatingActionButtonq4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.FragmentManager fm = getActivity().getFragmentManager();
                DialogQ4 q4 = new DialogQ4();
                q4.show(fm, "dialog");
            }
        });

        return v;
    }

    public void set_default_setup_settings(int fps, int err, int qrsize, int headertimeout, String dumppath){
        this.currFPSvalue = fps;
        this.currErrorvalue = err;
        this.currQrSizevalue = qrsize;
        this.currStartSeqTime = headertimeout;
        this.currFileDumpPath = dumppath;
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
