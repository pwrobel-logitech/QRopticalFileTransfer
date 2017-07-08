package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private static void internaladdLinks(TextView textView, String linkThis, String toThis) {
        Pattern pattern = Pattern.compile(linkThis);
        String scheme = toThis;
        android.text.util.Linkify.addLinks(textView, pattern, scheme, new android.text.util.Linkify.MatchFilter() {
            @Override
            public boolean acceptMatch(CharSequence s, int start, int end) {
                return true;
            }
        }, new android.text.util.Linkify.TransformFilter() {

            @Override
            public String transformUrl(Matcher match, String url) {
                return "";
            }
        });
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
    TextView textViewhelpd1 = null;
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

        this.textViewhelpd1 = (TextView) v.findViewById(R.id.textViewhelpd1);
        internaladdLinks(this.textViewhelpd1, "Windows", ((Activity)dismisser).getString(R.string.link_win));
        internaladdLinks(this.textViewhelpd1, "Linux", ((Activity)dismisser).getString(R.string.link_lin));

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
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return v;
    }

}
