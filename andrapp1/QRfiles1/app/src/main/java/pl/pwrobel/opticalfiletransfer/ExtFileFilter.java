package pl.pwrobel.opticalfiletransfer;

import java.io.File;
import java.io.FileFilter;

/**
 * Created by coco on 6/7/15.
 */
public class ExtFileFilter implements FileFilter {
    boolean m_allowHidden;
    boolean m_onlyDirectory;
    String[] m_ext;

    public ExtFileFilter() {
        this(false, false);
    }

    public ExtFileFilter(String... ext_list) {
        this(false, false, ext_list);
    }

    public ExtFileFilter(boolean dirOnly, boolean hidden, String... ext_list) {
        m_allowHidden = hidden;
        m_onlyDirectory = dirOnly;
        m_ext = ext_list;
    }

    @Override
    public boolean accept(File pathname) {
        if (!m_allowHidden) {
            if (pathname.isHidden())
                return false;
        }

        if (m_onlyDirectory) {
            if (!pathname.isDirectory())
                return false;
        }

        return true;

        /*
        if (m_ext == null)
            return true;

        if (pathname.isDirectory())
            return true;


        String full = FileUtil.getExtension(pathname);
        if (full.length() == 0)
            return true;
        String ext = FileUtil.getExtension(pathname).substring(1);
        for (String e : m_ext) {
            if (ext.equalsIgnoreCase(e))
                return true;
        }
        return false;*/
    }

}
