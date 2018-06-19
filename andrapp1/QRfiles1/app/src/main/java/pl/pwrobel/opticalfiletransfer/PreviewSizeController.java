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
}
