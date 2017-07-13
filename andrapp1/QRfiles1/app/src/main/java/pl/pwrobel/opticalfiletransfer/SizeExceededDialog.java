package pl.pwrobel.opticalfiletransfer;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Created by pwrobel on 05.07.17.
 */

public class SizeExceededDialog extends DialogFragment {

    private SizeExceededDialogDismisser dismisserListener = null;
    public void setDismisserListener(SizeExceededDialogDismisser dismisserListener){
        this.dismisserListener = dismisserListener;
    }

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

    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (this.dismisserListener != null)
            this.dismisserListener.onSetSizeExceededDialogGone();
    }

}
