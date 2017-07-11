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

        final AdView adView3 = (AdView) v.findViewById(R.id.adView);
        adView3.setVisibility(View.GONE);
        adView3.requestLayout();

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
                                    .addTestDevice("motorola_3g-001")
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

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return v;
    }

}
