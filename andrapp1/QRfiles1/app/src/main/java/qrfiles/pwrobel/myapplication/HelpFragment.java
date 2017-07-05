package qrfiles.pwrobel.myapplication;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by pwrobel on 25.06.17.
 */

public class HelpFragment extends DialogFragment {

    static HelpFragment newInstance() {
        HelpFragment f = new HelpFragment();
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
        //setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
    }



    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.help_fragment, container, false);
        return v;
    }

}
