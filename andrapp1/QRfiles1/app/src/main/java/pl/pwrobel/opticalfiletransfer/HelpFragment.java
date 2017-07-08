package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by pwrobel on 25.06.17.
 */

public class HelpFragment extends DialogFragment {

    static HelpFragment newInstance() {
        HelpFragment f = new HelpFragment();
        return f;
    }


    private HelpDialogDismisser dismisser = null;
    public void setHelpDialogDismisser(HelpDialogDismisser dis){
        dismisser = dis;
    }

    boolean defaultchecked = false;
    public void setStartDismissStatus(boolean checked){
        this.defaultchecked = checked;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setTitle("ABCCC");

        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
    }


    private boolean is_dismiss_from_ok_button = false;
    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (this.dismisser != null){
            //if (!is_dismiss_from_ok_button)
                this.dismisser.onSetHelpWindowGone();
            is_dismiss_from_ok_button = false;
        }
    }

    CheckBox dismissbox = null;
    TextView OKtext = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.help_fragment, container, false);
        this.dismissbox = (CheckBox) v.findViewById(R.id.checkBoxdismiss);
        this.OKtext = (TextView) v.findViewById(R.id.textViewok1);
        this.OKtext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Activity)dismisser).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        is_dismiss_from_ok_button = true;
                        HelpFragment.this.dismiss();
                    }
                });
            }
        });
        this.dismissbox.setChecked(this.defaultchecked);
        this.dismissbox.requestLayout();
        this.dismissbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (dismisser != null)
                    dismisser.onSetDismissedStatus(isChecked);
                if (isChecked){
                    new Timer().schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            ((Activity)dismisser).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    HelpFragment.this.dismisser.onSetHelpWindowGone();
                                    is_dismiss_from_ok_button = false;
                                    HelpFragment.this.dismiss();
                                }
                            });
                        }
                    }, 700);

                }
            }
        });
        return v;
    }

}
