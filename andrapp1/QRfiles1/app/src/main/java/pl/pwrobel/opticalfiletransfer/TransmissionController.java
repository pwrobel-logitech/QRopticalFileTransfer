package pl.pwrobel.opticalfiletransfer;

/**
 * Created by pwrobel on 26.06.17.
 */

//setting window communicates activity the new setting through this interface

public interface TransmissionController {

    public void onNewTransmissionSettings(int fps, int errlevpercent, int qrsize, int currstartuptime, String newdumppath);

}
