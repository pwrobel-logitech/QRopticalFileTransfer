package pl.pwrobel.opticalfiletransfer;

import java.io.File;
import java.text.DecimalFormat;

/**
 * Created by coco on 6/7/15.
 */
public class FileUtil {


    public static String getExtension(File file) {
        if (file == null) {
            return null;
        }

        int dot = file.getName().lastIndexOf(".");
        if (dot >= 0) {
            return file.getName().substring(dot);
        } else {
            // No extension.
            return "";
        }
    }

    public static String getReadableFileSize(long size) {
        final double BYTES_IN_KILOBYTES = 1024;
        final DecimalFormat dec = new DecimalFormat("###.#");
        final String BYTES = " B";
        final String KILOBYTES = " KB";
        final String MEGABYTES = " MB";
        final String GIGABYTES = " GB";
        double fileSize = size;
        String suffix = BYTES;

        if (size >= BYTES_IN_KILOBYTES) {
            suffix = KILOBYTES;
            fileSize = size / BYTES_IN_KILOBYTES;
            if (fileSize >= BYTES_IN_KILOBYTES) {
                fileSize = fileSize / BYTES_IN_KILOBYTES;
                suffix = MEGABYTES;
                if (fileSize >= BYTES_IN_KILOBYTES) {
                    fileSize = fileSize / BYTES_IN_KILOBYTES;
                    suffix = GIGABYTES;
                }
            }
        }
        return String.valueOf(dec.format(fileSize) + suffix);
    }

}
