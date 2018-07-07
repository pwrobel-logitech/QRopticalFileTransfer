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

public class AboutFragment extends DialogFragment {

    public native String QRarch();

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



    private TextView texok3 = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.about_fragment, container, false);

        texok3 = (TextView) v.findViewById(R.id.texok3);
        texok3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });


        final TextView textViewabout3b = (TextView) v.findViewById(R.id.textViewabout3b);
        textViewabout3b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity a = getActivity();
                if (a != null)
                    a.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (a != null)
                                Qrfiles.openProVersionOnPlayStore(a);
                        }
                    });
            }
        });


        final TextView textViewabout6 = (TextView) v.findViewById(R.id.textViewabout6);
        final Activity a = this.getActivity();

        TextView textViewabout1 = (TextView) v.findViewById(R.id.textViewabout1);

        final TextView textViewabout7 = (TextView) v.findViewById(R.id.textViewabout7);
        final TextView textViewabout8 = (TextView) v.findViewById(R.id.textViewabout8);
        final TextView textViewabout9 = (TextView) v.findViewById(R.id.textViewabout9);

        final TextView textViewabout10 = (TextView) v.findViewById(R.id.textViewabout10);
        final TextView textViewabout11 = (TextView) v.findViewById(R.id.textViewabout11);

        final TextView textViewabout13 = (TextView) v.findViewById(R.id.textViewabout13);
        final TextView textViewabout14 = (TextView) v.findViewById(R.id.textViewabout14);

        final TextView textViewabout14a = (TextView) v.findViewById(R.id.textViewabout14a);

        final TextView textViewabout12L = (TextView) v.findViewById(R.id.textViewabout12L);
        textViewabout12L.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AboutFragmentLGPL ab = new AboutFragmentLGPL();
                android.app.FragmentManager fm = getActivity().getFragmentManager();
                ab.show(fm, "dialog");
            }
        });


        if (a != null) {
            HelpFragment.internaladdLinks(textViewabout6, a.getString(R.string.linkab6str1),
                    a.getString(R.string.linkab6val1));
            HelpFragment.internaladdLinks(textViewabout6, a.getString(R.string.linkab6str2),
                    a.getString(R.string.linkab6val2));

            HelpFragment.internaladdLinks(textViewabout1, a.getString(R.string.linkifyab2str),
                    a.getString(R.string.linkifyab2val));
            HelpFragment.internaladdLinks(textViewabout1, a.getString(R.string.linkifyab1str1),
                    a.getString(R.string.link_win));
            HelpFragment.internaladdLinks(textViewabout1, a.getString(R.string.linkifyab1str2),
                    a.getString(R.string.link_lin));

            HelpFragment.internaladdLinks(textViewabout7, a.getString(R.string.linkab7str1),
                    a.getString(R.string.linkab7val1));
            HelpFragment.internaladdLinks(textViewabout7, a.getString(R.string.linkab7str2),
                    a.getString(R.string.linkab7val2));

            HelpFragment.internaladdLinks(textViewabout8, a.getString(R.string.linkab8str1),
                    a.getString(R.string.linkab8val1));
            HelpFragment.internaladdLinks(textViewabout8, a.getString(R.string.linkab8str2),
                    a.getString(R.string.linkab8val2));

            HelpFragment.internaladdLinks(textViewabout9, a.getString(R.string.linkab9str1),
                    a.getString(R.string.linkab9val1));
            HelpFragment.internaladdLinks(textViewabout9, a.getString(R.string.linkab9str2),
                    a.getString(R.string.linkab9val2));

            HelpFragment.internaladdLinks(textViewabout10, a.getString(R.string.linkab10str1),
                    a.getString(R.string.linkab10val1));
            HelpFragment.internaladdLinks(textViewabout10, a.getString(R.string.linkab10str2),
                    a.getString(R.string.linkab10val2));

            HelpFragment.internaladdLinks(textViewabout11, a.getString(R.string.linkab11str1),
                    a.getString(R.string.linkab11val1));
            HelpFragment.internaladdLinks(textViewabout11, a.getString(R.string.linkab11str2),
                    a.getString(R.string.linkab11val2));

            /*
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
                    */
        }


        final AdView adView3 = (AdView) v.findViewById(R.id.adView);
        adView3.setVisibility(View.GONE);
        adView3.requestLayout();

        //a = this.getActivity();
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

        final TextView textViewabout17 = (TextView) v.findViewById(R.id.textViewabout17);
        textViewabout17.append(" "+QRarch());

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return v;
    }

}
