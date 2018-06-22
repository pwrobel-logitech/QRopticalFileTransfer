package pl.pwrobel.opticalfiletransfer;

import android.hardware.Camera;

import java.util.List;

/**
 * Created by pwrobel on 19.06.18.
 */

public interface PreviewSizeController {

    public List<Camera.Size> getPreviewSizes();

    void setUserPreviewIndex(int index);

    int getCurrUserPreviewIndex();

    int getStartUpIndexToConstructList();

    int getProposedDefaultOptimalPrevievIndex();

    //-1 if not known
    int getCalculatedOptimalIndex();

    boolean getCheckedCustomPrev();

    void setCheckedCustomPrev(boolean new_user_check);

    // square aligner
    void setUserAlignerSquarePrev(int perc);
    int getUserAlignerSquarePrev();
}
