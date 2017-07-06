package pl.pwrobel.opticalfiletransfer;

/**
 * Created by pwrobel on 06.05.17.
 */

public interface CameraController {



    public void initCamAsync(int surfacew, int surfaceh, String dumpfoldername);

    public void closeCamAsync();

    public void setCallbackBufferSizeAsync(int size);

    public void callAutoFocusAsync();

    public int getCamPreviewWidth();

    public int getCamPreviewHeight();

    public boolean isCameraInitialized();

    public double getCurrentSuccRatio();

    public double getCurrentNoiseRatio(); //for RS chunk total noise level

    public double getCurrentProgressRatio(); //normalized progress of the current download

    public boolean shouldDrawProgressBars();

    //deprecated
    public String getFileNameCapturedFromHeader(); // for 8 dots '........' there is no header detected yet

    public class DisplayStatusInfo{
        enum StatusDisplayType{
            TYPE_NOTE,
            TYPE_ERR, //errors are red..
            TYPE_DONE //but these are green
        }
        public String displaytext;
        public String displaytext2; //for the eventual second line
        public StatusDisplayType displayTextType;
        public String additional_err_text;
        boolean should_draw_status;
    }

    public DisplayStatusInfo getDisplayStatusText();

}
