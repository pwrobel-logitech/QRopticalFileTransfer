package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Created by pwrobel on 05.07.17.
 */

public class SizeExceededDialog extends DialogFragment {

    private SizeExceededDialogDismisser dismisserListener = null;
    public void setDismisserListener(SizeExceededDialogDismisser dismisserListener){
        this.dismisserListener = dismisserListener;
    }

    /*
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        super.onCreateDialog(savedInstanceState);
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.size_exceed_dialog_msg1)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {

                    }
                });

        // Create the AlertDialog object and return it
        Dialog d = builder.create();
        d.getWindow().setBackgroundDrawableResource(R.color.greyC);
        return d;
    }
*/
    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (this.dismisserListener != null)
            this.dismisserListener.onSetSizeExceededDialogGone();
    }

    private TextView prolink = null;
    private TextView oktext = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.size_exceeded, container, false);

        prolink = (TextView) v.findViewById(R.id.textViewSE1b);
        prolink.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final Activity act = (Activity) SizeExceededDialog.this.getActivity();
                if (act != null){
                    Qrfiles.openProVersionOnPlayStore(act);
                    SizeExceededDialog.this.dismiss();
                }
            }
        });

        oktext = (TextView) v.findViewById(R.id.textViewokSE);
        oktext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SizeExceededDialog.this.dismiss();
            }
        });

        return v;
    }
}
