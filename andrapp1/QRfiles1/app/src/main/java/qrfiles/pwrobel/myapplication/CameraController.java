package qrfiles.pwrobel.myapplication;

import android.view.SurfaceHolder;

/**
 * Created by pwrobel on 06.05.17.
 */

public interface CameraController {

    public void initCamAsync(int surfacew, int surfaceh);

    public void closeCamAsync();

    public void setCallbackBufferSizeAsync(int size);

    public void callAutoFocusAsync();

    public int getCamPreviewWidth();

    public int getCamPreviewHeight();

    public boolean isCameraInitialized();

    public double getCurrentSuccRatio();

    public double getCurrentNoiseRatio(); //for RS chunk total noise level

    public double getCurrentProgressRatio(); //normalized progress of the current download

}
