package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Created by pwrobel on 25.06.17.
 */

public class AboutFragmentLGPL extends DialogFragment {

    static AboutFragment newInstance() {
        AboutFragment f = new AboutFragment();
        return f;
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
        setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
        //setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
    }



    private TextView texok4 = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.aboutlgpl_fragment, container, false);

        texok4 = (TextView) v.findViewById(R.id.texok4);
        texok4.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });



        final TextView textViewabout6 = (TextView) v.findViewById(R.id.textViewabout6);
        final Activity a = this.getActivity();

        TextView textViewabout1 = (TextView) v.findViewById(R.id.textViewabout1);



        final TextView textViewabout13 = (TextView) v.findViewById(R.id.textViewabout13);
        final TextView textViewabout14 = (TextView) v.findViewById(R.id.textViewabout14);

        final TextView textViewabout14a = (TextView) v.findViewById(R.id.textViewabout14a);

        if (a != null) {

            HelpFragment.internaladdLinks(textViewabout13, a.getString(R.string.linkab13str1),
                    a.getString(R.string.linkab13val1));
            HelpFragment.internaladdLinks(textViewabout13, a.getString(R.string.linkab13str2),
                    a.getString(R.string.linkab13val2));

            HelpFragment.internaladdLinks(textViewabout14, a.getString(R.string.linkab14str1),
                    a.getString(R.string.linkab14val1));
            HelpFragment.internaladdLinks(textViewabout14, a.getString(R.string.linkab14str2),
                    a.getString(R.string.linkab14val2));

            HelpFragment.internaladdLinks(textViewabout14a, a.getString(R.string.linkab14astr1),
                    a.getString(R.string.linkab14aval1));
        }







        //a = this.getActivity();
        /*
        final AdView adView3 = (AdView) v.findViewById(R.id.adView);
        adView3.setVisibility(View.GONE);
        adView3.requestLayout();
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

                            adView3.loadAd(adRequest);
                            adView3.setAdListener(new AdListener() {
                                @Override
                                public void onAdClosed() {
                                    super.onAdClosed();
                                }

                                public void onAdFailedToLoad(int var1) {
                                    super.onAdFailedToLoad(var1);
                                    adView3.setVisibility(View.GONE);
                                    adView3.requestLayout();
                                }

                                public void onAdLoaded() {
                                    super.onAdLoaded();
                                    adView3.setVisibility(View.VISIBLE);
                                    adView3.requestLayout();
                                }
                            });
                        }
                    });
            }
        }, 1000);
        */
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return v;
    }

}
