package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.TextView;

/**
 * Created by pwrobel on 25.06.17.
 */

public class PrivacyPolicyFragment extends DialogFragment {

    static PrivacyPolicyFragment newInstance() {
        PrivacyPolicyFragment f = new PrivacyPolicyFragment();
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
            dialog.setTitle("ABCCC");

        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
        //setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
    }



    private TextView textViewok2priv = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.privacypolicy_fragment, container, false);

        TextView tv1 = (TextView) v.findViewById(R.id.textViewprpol1);
        Activity a = this.getActivity();
        if (a != null) {
            HelpFragment.internaladdLinks(tv1, a.getString(R.string.linkifystr1), a.getString(R.string.linkifyval1));
            HelpFragment.internaladdLinks(tv1, a.getString(R.string.linkifystr2), a.getString(R.string.linkifyval2));
        }

        textViewok2priv = (TextView) v.findViewById(R.id.textViewok2priv);
        textViewok2priv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });

        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return v;
    }

}
