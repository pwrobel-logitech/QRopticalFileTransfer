package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;

import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
    public void show(FragmentManager manager, String tag) {
        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag).addToBackStack(null);
            ft.commitAllowingStateLoss();
        } catch (IllegalStateException e) {
            Log.e("IllegalStateException", "Exception", e);
        }

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
    Button save_webapp = null;
    private int picker_obtainedFPS = -1;
    private int picker_obtainedError = -1;
    private int picker_obtainedQrsize = -1;
    FloatingActionButton floatingActionButtonq1 = null;
    FloatingActionButton floatingActionButtonq2 = null;
    FloatingActionButton floatingActionButtonq3 = null;
    FloatingActionButton floatingActionButtonq4 = null;
    FloatingActionButton floatingActionButtonq5 = null;
    FloatingActionButton floatingActionButtonq6 = null;
    FloatingActionButton folderselect = null;
    TextView dumpfolderameTextView = null;
    CheckBox checkBoxblur = null;
    CheckBox checkBoxallowprevsize = null;
    boolean pref_is_blurshader = true;
    boolean pref_is_customprevcheck = false;
    ScrollView scrollView = null;
    SeekBar seekBar1 = null; // square size preview

    // for size preview
    GridButtons rgp = null;
    int radiobuttprevnum = -1;

    // Copy an InputStream to a File.
//
    private void copyInputStreamToFile(InputStream in, File file) {
        OutputStream out = null;

        try {
            out = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while((len=in.read(buf))>0){
                out.write(buf,0,len);
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast toast = Toast.makeText(getActivity(), "Failed to store the file!", Toast.LENGTH_SHORT);
                    TextView v = (TextView) toast.getView().findViewById(android.R.id.message);
                    v.setTextColor(Color.RED);
                    toast.show();
                }
            });
        }
        finally {
            // Ensure that the InputStreams are closed even if there's an exception.
            try {
                if ( out != null ) {
                    out.close();
                }

                // If you want to close the "in" InputStream yourself then remove this
                // from here but ensure that you close it yourself eventually.
                in.close();
            }
            catch ( IOException e ) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.settings_fragment, container, false);
        //View tv = v.findViewById(R.id.text);
        //((TextView)tv).setText("Dialog #" + mNum + ": using style ");

        boolean ispro = false;
        ispro = getResources().getBoolean(R.bool.is_pro_version);
        scrollView = (ScrollView) v.findViewById(R.id.scrollv);

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


        save_webapp = (Button) v.findViewById(R.id.button_savewebapplocally);
        save_webapp.setEnabled(true);
        save_webapp.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                save_webapp.setEnabled(false);
                String str = getActivity().getString(R.string.miscsave) + " "+getfullpath(currFileDumpPath)+File.separator+"webuploaderjs.html";

                InputStream is = getResources().openRawResource(R.raw.webuploaderjs);
                File out = new File(getfullpath(currFileDumpPath)+File.separator+"webuploaderjs.html");
                copyInputStreamToFile(is, out);
                Toast.makeText(getActivity(), str, Toast.LENGTH_LONG).show();

                new Timer().schedule(new TimerTask()
                {
                    @Override
                    public void run()
                    {
                        Activity a = getActivity();
                        if (a == null)
                            return;
                        a.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                save_webapp.setEnabled(true);
                            }
                        });

                    }
                },1500);
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
                currFileDumpPath = "Download";
                pref_is_blurshader = false;
                checkBoxblur.setChecked(pref_is_blurshader);
                checkBoxblur.requestLayout();

                if (dumpfolderameTextView != null){

                    dumpfolderameTextView.setText(getfullpath(currFileDumpPath));
                    dumpfolderameTextView.requestLayout();
                }


                SettingsFragment.this.request_resetting_encoder_because_of_new_settings();
                SettingsFragment.this.setEstimatedUploadSpeedInfo();

                if (userPreviewSizeController != null && SettingsFragment.this.rgp != null){


                    //block for 2s async
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (rgp == null)
                                return;
                            restore_default_settings_button.setEnabled(false);
                            int count = rgp.getChildCount();
                            for (int i=0;i<count;i++) {
                                RadioButton o = (RadioButton) rgp.getChildAt(i);
                                o.setEnabled(false);
                                o.setClickable(false);
                                o.setFocusable(false);
                            }
                        }
                    });



                    int def = SettingsFragment.this.userPreviewSizeController.getProposedDefaultOptimalPrevievIndex();
                    //if (def == 0) {
                    //    Log.i("XXXCXXXXQQQ", "wooo");
                    //}
                    //RadioButton rb=(RadioButton)v.findViewById(def);
                    int count = rgp.getChildCount();
                    RadioButton rbtocheck = null;
                    for (int i=0;i<count;i++) {
                        RadioButton o = (RadioButton) rgp.getChildAt(i);
                        int ri = o.getId();
                        if (ri == def){
                            rbtocheck = o;
                            o.setChecked(true);
                        }
                        else
                            o.setChecked(false);
                    }
                    if(userPreviewSizeController != null)
                        if (rbtocheck != null)
                            userPreviewSizeController.setUserPreviewIndex(rbtocheck.getId());



                    new Timer().schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            if (getActivity() != null)
                                getActivity().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        if (rgp == null)
                                            return;
                                        restore_default_settings_button.setEnabled(true);
                                        int count = rgp.getChildCount();
                                        for (int i=0;i<count;i++) {
                                            RadioButton o = (RadioButton) rgp.getChildAt(i);
                                            o.setEnabled(true);
                                            o.setClickable(true);
                                            o.setFocusable(true);
                                        }
                                    }
                                });
                        }
                    }, 1200);
                }

                checkBoxallowprevsize.setChecked(false);
                rgp.setVisibility(View.GONE);
                if(userPreviewSizeController != null){
                    userPreviewSizeController.setCheckedCustomPrev(false);
                    userPreviewSizeController.setUserPreviewIndex(userPreviewSizeController.getCalculatedOptimalIndex());
                    userPreviewSizeController.setUserAlignerSquarePrev(666);
                    if (seekBar1 != null)
                        seekBar1.setProgress(666);
                }
            }
        });

        this.textviewUploadSpeedInfo = (TextView) v.findViewById(R.id.textResultinguplspeed);
        this.setEstimatedUploadSpeedInfo();

        floatingActionButtonq1 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq1);
        floatingActionButtonq2 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq2);
        floatingActionButtonq3 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq3);
        floatingActionButtonq4 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq4);
        floatingActionButtonq5 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq5);
        floatingActionButtonq6 = (FloatingActionButton) v.findViewById(R.id.floatingActionButtonq6);

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

        floatingActionButtonq5.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.FragmentManager fm = getActivity().getFragmentManager();
                DialogQ5 q5 = new DialogQ5();
                q5.show(fm, "dialog");
            }
        });

        floatingActionButtonq6.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                android.app.FragmentManager fm = getActivity().getFragmentManager();
                DialogQ6 q6 = new DialogQ6();
                q6.show(fm, "dialog");
            }
        });

        dumpfolderameTextView = (TextView) v.findViewById(R.id.textViewnamedumpfolder);
        File yourAppDir = null;
        if (Environment.getExternalStorageState() != null){
            yourAppDir = Environment.getExternalStorageDirectory();
        }else {
            yourAppDir = Environment.getDataDirectory();
        }


        checkBoxallowprevsize = (CheckBox) v.findViewById(R.id.checkBoxallowpreviewsize);
        if (this.userPreviewSizeController != null)
            checkBoxallowprevsize.setChecked(this.userPreviewSizeController.getCheckedCustomPrev());
        checkBoxallowprevsize.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (userPreviewSizeController != null){
                    userPreviewSizeController.setCheckedCustomPrev(isChecked);
                }
                if (isChecked == false) {
                    rgp.setVisibility(View.GONE);
                    if(userPreviewSizeController != null){
                        userPreviewSizeController.setUserPreviewIndex(userPreviewSizeController.getCalculatedOptimalIndex());
                        int count = rgp.getChildCount();
                        for (int i=0;i<count;i++) {
                            RadioButton o = (RadioButton) rgp.getChildAt(i);
                            if (i == userPreviewSizeController.getCalculatedOptimalIndex())
                                o.setChecked(true);
                            else
                                o.setChecked(false);
                        }
                    }

                }else{
                    rgp.setVisibility(View.VISIBLE);
                    rgp.requestFocus();
                    if (scrollView != null){
                        scrollView.post(new Runnable() {
                            @Override
                            public void run() {
                                scrollView.fullScroll(ScrollView.FOCUS_DOWN);
                            }
                        });
                    }
                }
            }
        });
        //checkBoxblur.setOnCheckedChangeListener(

        rgp= (GridButtons) v.findViewById(R.id.radiogroup);
        RadioGroup.LayoutParams rprms;
        final View vv = v;

        if (this.userPreviewSizeController != null && this.userPreviewSizeController.getPreviewSizes() != null) {

            int defindex = this.userPreviewSizeController.getStartUpIndexToConstructList(); //this.userPreviewSizeController.getProposedDefaultOptimalPrevievIndex();
            if (defindex == -1){
                if (this.userPreviewSizeController.getProposedDefaultOptimalPrevievIndex() > -1){
                    defindex = this.userPreviewSizeController.getProposedDefaultOptimalPrevievIndex();
                }else{
                    defindex = 0;
                }
            }

            int nprv = this.userPreviewSizeController.getPreviewSizes().size();
            radiobuttprevnum = nprv;
            for(int i=0;i<nprv;i++){
                RadioButton radioButton = new RadioButton(getActivity());
                radioButton.setText(this.userPreviewSizeController.getPreviewSizes().get(i).width + "x"+
                        this.userPreviewSizeController.getPreviewSizes().get(i).height);
                radioButton.setId(i);
                if (i == defindex)
                    radioButton.setChecked(true);
                else
                    radioButton.setChecked(false);

                rprms= new RadioGroup.LayoutParams(AppBarLayout.LayoutParams.WRAP_CONTENT, AppBarLayout.LayoutParams.WRAP_CONTENT);
                rgp.addView(radioButton, rprms);
            }

            if(this.userPreviewSizeController.getCheckedCustomPrev() == false){
                rgp.setVisibility(View.GONE);
            }else
                rgp.setVisibility(View.VISIBLE);

            rgp.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
            {
                public void onCheckedChanged(RadioGroup group, int checkedId) {
                    // checkedId is the RadioButton selected
                    RadioButton rb=(RadioButton)vv.findViewById(checkedId);
                    getActivity().runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if (rgp == null)
                                return;
                            restore_default_settings_button.setEnabled(false);
                            int count = rgp.getChildCount();
                            for (int i=0;i<count;i++) {
                                RadioButton o = (RadioButton) rgp.getChildAt(i);
                                o.setEnabled(false);
                                o.setClickable(false);
                                o.setFocusable(false);
                            }
                        }
                    });
                    new Timer().schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            if (getActivity() != null)
                                getActivity().runOnUiThread(new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        if (rgp == null)
                                            return;
                                        restore_default_settings_button.setEnabled(true);
                                        int count = rgp.getChildCount();
                                        for (int i=0;i<count;i++) {
                                            RadioButton o = (RadioButton) rgp.getChildAt(i);
                                            o.setEnabled(true);
                                            o.setClickable(true);
                                            o.setFocusable(true);
                                        }
                                    }
                                });
                        }
                    }, 1200);

                    if(userPreviewSizeController != null){
                        if (rb.getId() > -1)
                            userPreviewSizeController.setUserPreviewIndex(rb.getId());
                    }
                }
            });
        }

        if (this.userPreviewSizeController != null){
            if (userPreviewSizeController.getPreviewSizes() != null){
                if(userPreviewSizeController.getPreviewSizes().size() == 0){
                    checkBoxallowprevsize.setChecked(false);
                    checkBoxallowprevsize.setEnabled(false);
                }else{
                    checkBoxallowprevsize.setEnabled(true);
                }
            }else{
                checkBoxallowprevsize.setChecked(false);
                checkBoxallowprevsize.setEnabled(false);
            }
        }else{
            checkBoxallowprevsize.setChecked(false);
            checkBoxallowprevsize.setEnabled(false);
        }

        seekBar1 = (SeekBar)v.findViewById(R.id.seekBar1);
        if (seekBar1 != null) {
            seekBar1.setMax(1000);
            if(userPreviewSizeController != null)
                seekBar1.setProgress(userPreviewSizeController.getUserAlignerSquarePrev());
            seekBar1.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if(userPreviewSizeController != null)
                        userPreviewSizeController.setUserAlignerSquarePrev(progress);
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }


        dumpfolderameTextView.setText(getfullpath(currFileDumpPath));
        dumpfolderameTextView.requestLayout();

        checkBoxblur = (CheckBox) v.findViewById(R.id.checkBoxblur);
        checkBoxblur.setChecked(this.pref_is_blurshader);
        checkBoxblur.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                SettingsFragment.this.pref_is_blurshader = isChecked;
                request_resetting_encoder_because_of_new_settings();
            }
        });

        folderselect = (FloatingActionButton) v.findViewById(R.id.folderselect);
        folderselect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File yourAppDir = null;
                if (Environment.getExternalStorageState() != null){
                    yourAppDir = Environment.getExternalStorageDirectory();
                }else {
                    yourAppDir = Environment.getDataDirectory();
                }
                ChooserDialog d = new ChooserDialog().withCustomParentDir(yourAppDir.getAbsolutePath()).with(getActivity())
                        .withFilter(true, false)
                        .withStartFile(yourAppDir.getAbsolutePath())
                        .withDateFormat("HH:mm")
                        .withResources(R.string.ask_folder_choose, R.string.title_choose, R.string.dialog_cancel)
                        .withChosenListener(new ChooserDialog.Result() {

                            @Override
                            public void onChoosePath(String dir, File dirFile) {
                                Log.i("Folder", "Selected folder "+dir);
                                File yourAppDir = null;
                                if (Environment.getExternalStorageState() != null){
                                    yourAppDir = Environment.getExternalStorageDirectory();
                                }else {
                                    yourAppDir = Environment.getDataDirectory();
                                }
                                currFileDumpPath = dir;
                                String lastfold = dir.replace(yourAppDir.getAbsolutePath(), "");
                                if (lastfold.startsWith(File.separator) && lastfold.length() > 1)
                                    lastfold = lastfold.substring(1);
                                currFileDumpPath = lastfold;
                                dumpfolderameTextView.setText(dir);
                                dumpfolderameTextView.requestLayout();
                                request_resetting_encoder_because_of_new_settings();
                            }
                        })
                        .build()
                        .show();
            }
        });


        if (!ispro)
            folderselect.setVisibility(View.GONE);

        View blurview = v.findViewById(R.id.optionalview1);
        //if (!ispro)
        //    blurview.setVisibility(View.GONE);

        final AdView adView2 = (AdView) v.findViewById(R.id.adView);
        adView2.setVisibility(View.GONE);
        adView2.requestLayout();

        final Activity a = this.getActivity();
        new Timer().schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                if (a != null)
                    a.runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        AdRequest adRequest = new AdRequest.Builder()
                                .addTestDevice(AdRequest.DEVICE_ID_EMULATOR)
                                .addTestDevice("B8E41997327A892A4FD057B12AD2C843")
                                .addTestDevice("974226D4036724BE09FB80898BA1B4BE")
                                .addTestDevice("565FCDDC3CCE41C9B34DE6365FCF267C")
                                .build();

                        adView2.loadAd(adRequest);
                        adView2.setAdListener(new AdListener() {
                            @Override
                            public void onAdClosed() {
                                super.onAdClosed();
                            }

                            public void onAdFailedToLoad(int var1) {
                                super.onAdFailedToLoad(var1);
                                adView2.setVisibility(View.GONE);
                                adView2.requestLayout();
                            }

                            public void onAdLoaded() {
                                super.onAdLoaded();
                                adView2.setVisibility(View.VISIBLE);
                                adView2.requestLayout();
                            }
                        });
                    }
                });
            }
        }, 1000);

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return v;
    }

    @NonNull
    private String getfullpath(String folder){
        File yourAppDir = null;
        if (Environment.getExternalStorageState() != null){
            yourAppDir = Environment.getExternalStorageDirectory();
        }else {
            yourAppDir = Environment.getDataDirectory();
        }

        String fpath = yourAppDir.getAbsolutePath() + File.separator + folder;
        return fpath;
    }

    public void set_default_setup_settings(int fps, int err, int qrsize, int headertimeout, String dumppath, boolean blurcheck){
        this.currFPSvalue = fps;
        this.currErrorvalue = err;
        this.currQrSizevalue = qrsize;
        this.currStartSeqTime = headertimeout;
        this.currFileDumpPath = dumppath; // only last folder name of the path, actually
        this.pref_is_blurshader = blurcheck;

        if (dumpfolderameTextView != null){
            Activity a = getActivity();
            if (a != null){
                a.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (dumpfolderameTextView != null){

                            dumpfolderameTextView.setText(getfullpath(currFileDumpPath));
                            dumpfolderameTextView.requestLayout();
                        }
                    }
                });
            }
        }
    }

    private TransmissionController transmissionController = null;
    public void setTransmissionContorller(TransmissionController tc){
        this.transmissionController = tc;
    }

    private PreviewSizeController userPreviewSizeController = null;
    public void setUserPreviewContorller(PreviewSizeController uc){
        this.userPreviewSizeController = uc;
    }

    private void request_resetting_encoder_because_of_new_settings(){
        if (this.transmissionController != null){
            this.transmissionController.onNewTransmissionSettings(this.currFPSvalue, this.currErrorvalue,
                    this.currQrSizevalue, this.currStartSeqTime, this.currFileDumpPath, this.pref_is_blurshader);
        }
    }

}
