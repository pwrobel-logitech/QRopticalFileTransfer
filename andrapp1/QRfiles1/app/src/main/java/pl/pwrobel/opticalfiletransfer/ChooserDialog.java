package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Environment;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;


import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Created by coco on 6/7/15.
 */
public class ChooserDialog implements AdapterView.OnItemClickListener, DialogInterface.OnClickListener {

    public interface Result {
        void onChoosePath(String dir, File dirFile);
    }

    public ChooserDialog() {

    }

    public ChooserDialog with(Context cxt) {
        this._context = cxt;
        //custom_parent_dir = null;
        return this;
    }

    public ChooserDialog withFilter(FileFilter ff) {
        withFilter(false, false, (String[]) null);
        this._fileFilter = ff;
        //custom_parent_dir = null;
        return this;
    }

    public ChooserDialog withFilter(boolean dirOnly, boolean allowHidden, FileFilter ff) {
        withFilter(dirOnly, allowHidden, (String[]) null);
        this._fileFilter = ff;
        //custom_parent_dir = null;
        return this;
    }

    public ChooserDialog withFilter(boolean allowHidden, String... suffixes) {
        //custom_parent_dir = null;
        return withFilter(false, allowHidden, suffixes);
    }

    public ChooserDialog withFilter(boolean dirOnly, boolean allowHidden, String... suffixes) {
        this._dirOnly = dirOnly;
        //custom_parent_dir = null;
        if (suffixes == null)
            this._fileFilter = dirOnly ? filterDirectoriesOnly : filterFiles;
        else
            this._fileFilter = new ExtFileFilter(_dirOnly, allowHidden, suffixes);
        return this;
    }

    public ChooserDialog withFilterRegex(boolean dirOnly, boolean allowHidden, String pattern, int flags) {
        this._dirOnly = dirOnly;
        //custom_parent_dir = null;
        this._fileFilter = new RegexFileFilter(_dirOnly, allowHidden, pattern, flags);
        return this;
    }

    public ChooserDialog withFilterRegex(boolean dirOnly, boolean allowHidden, String pattern) {
        this._dirOnly = dirOnly;
        //custom_parent_dir = null;
        this._fileFilter = new RegexFileFilter(_dirOnly, allowHidden, pattern, Pattern.CASE_INSENSITIVE);
        return this;
    }

    public ChooserDialog withStartFile(String startFile) {
        if (startFile != null)
            _currentDir = new File(startFile);
        else
            _currentDir = Environment.getExternalStorageDirectory();

        if(!_currentDir.isDirectory())
            _currentDir = _currentDir.getParentFile();
        //custom_parent_dir = null;
        return this;
    }

    public ChooserDialog withChosenListener(Result r) {
        //custom_parent_dir = null;
        this._result = r;
        return this;
    }

    public ChooserDialog withResources(int titleRes, int okRes, int cancelRes) {
        //custom_parent_dir = null;
        this._titleRes = titleRes;
        this._okRes = okRes;
        this._cancelRes = cancelRes;
        return this;
    }

    public ChooserDialog withDateFormat() {
        //custom_parent_dir = null;
        return this.withDateFormat("yyyy/MM/dd HH:mm:ss");
    }

    public ChooserDialog withDateFormat(String format) {
        //custom_parent_dir = null;
        this._dateFormat = format;
        return this;
    }

    private TextView filepath = null;
    View vtitle = null;
    public ChooserDialog build() {
        if (_titleRes == 0 || _okRes == 0 || _cancelRes == 0)
            throw new RuntimeException("withResources() should be called at first.");

        DirAdapter adapter = refreshDirs();

        Activity a = (Activity)this._context;

        android.support.v7.app.AlertDialog.Builder builder = new android.support.v7.app.AlertDialog.Builder(_context,  R.style.MyDialogTheme);
        //AlertDialog.Builder builder = new AlertDialog.Builder(_context , AlertDialog.THEME_HOLO_LIGHT);




        LayoutInflater inflater = (LayoutInflater)a.getBaseContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.vtitle = inflater.inflate(R.layout.file_chooser_title, null);

        //builder.setTitle(R.string.dlg_choose dir_title);
        TextView title = (TextView) this.vtitle.findViewById(R.id.textViewf1);
        this.filepath = (TextView) this.vtitle.findViewById(R.id.textViewf2);
        try {
            if (this.filepath != null)
                this.filepath.setText(_currentDir.getCanonicalPath());
        } catch (IOException e) {
            Log.e("Err", "Canonical path request for filechooser failed");
            e.printStackTrace();
        }
        // You Can Customise your Title here

        title.setText(a.getString(this._titleRes));
        //title.setBackgroundColor(Color.rgb(0xaa, 0xaa, 0xaa));
        //title.setPadding(10, 27, 10, 10);
        title.setGravity(Gravity.CENTER);
        title.setTextColor(Color.rgb(0x33,0x33,0x33));
        //title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        //builder.setTitle(_titleRes);

        builder.setCustomTitle(this.vtitle);



        builder.setAdapter(adapter, this);

        if (_dirOnly) {
            builder.setPositiveButton(_okRes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    if (_result != null) {
                        if (_dirOnly)
                            _result.onChoosePath(_currentDir.getAbsolutePath(), _currentDir);
                    }
                    dialog.dismiss();
                }
            });
        }

        builder.setNegativeButton(_cancelRes, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                last_encountered_path = _currentDir;
                dialog.cancel();
            }
        });

        _alertDialog = builder.create();
        _list = _alertDialog.getListView();
        _list.setOnItemClickListener(this);
        return this;
    }

    public File last_encountered_path = null;
    public ChooserDialog show() {
        //if (_result == null)
        //    throw new RuntimeException("no chosenListener defined. use withChosenListener() at first.");


        if (_alertDialog == null || _list == null)
            throw new RuntimeException("call build() before show().");
        _alertDialog.show();

        /*
        final Button nButton = _alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        LinearLayout.LayoutParams nButtonLL = (LinearLayout.LayoutParams) nButton.getLayoutParams();
        nButtonLL.gravity = Gravity.CENTER;
        nButton.setBackgroundColor(Color.GRAY);
        nButton.setLayoutParams(nButtonLL);
*/
        return this;
    }


    private void listDirs() {
        _entries.clear();

        // Get files
        File[] files = _currentDir.listFiles(_fileFilter);

        // Add the ".." entry
        boolean adddd = false;
        if (_currentDir.getParent() != null)
            adddd = true;
        if ((this.custom_parent_dir != null &&
                (_currentDir.getAbsolutePath().equals(custom_parent_dir))))
            adddd = false;
        if (adddd)
            _entries.add(new File(".."));

        if (files != null) {
            for (File file : files) {
                _entries.add(file);
            }
        }

        Collections.sort(_entries, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            }
        });
    }

    //if not-null - then it acts as the restricting parent dir;
    private String custom_parent_dir = null;

    public ChooserDialog withCustomParentDir(String custom_parent_dir){
        this.custom_parent_dir = custom_parent_dir;
        return this;
    }

    private void listDirs2() {
        _entries.clear();

        // Get files
        File[] files = _currentDir.listFiles();

        // Add the ".." entry
        boolean adddd = false;
        if (_currentDir.getParent() != null)
            adddd = true;
        if ((this.custom_parent_dir != null &&
                (_currentDir.getAbsolutePath().equals(custom_parent_dir))))
            adddd = false;
        if (adddd)
            _entries.add(new File(".."));

        if (files != null) {
            for (File file : files) {
                if (!file.isDirectory())
                    continue;

                _entries.add(file);
            }
        }

        Collections.sort(_entries, new Comparator<File>() {
            public int compare(File f1, File f2) {
                return f1.getName().toLowerCase().compareTo(f2.getName().toLowerCase());
            }
        });
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View list, int pos, long id) {
        if (pos < 0 || pos >= _entries.size())
            return;

        File file = _entries.get(pos);
        if (file.getName().equals(".."))
            _currentDir = _currentDir.getParentFile();
        else
            _currentDir = file;

        if (!file.isDirectory()) {
            if (!_dirOnly) {
                if (_result != null) {
                    _result.onChoosePath(file.getAbsolutePath(), file);
                    _alertDialog.dismiss();
                    return;
                }
            }
        }

        refreshDirs();
        if (this.vtitle != null){
            this.filepath = (TextView) this.vtitle.findViewById(R.id.textViewf2);
            try {
                if (this.filepath != null){
                    this.filepath.setText(_currentDir.getCanonicalPath());
                    this.filepath.requestLayout();
                }
            } catch (IOException e) {
                Log.e("Err", "Canonical path request for filechooser failed");
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onClick(DialogInterface dialog, int which) {
        //
    }

    DirAdapter refreshDirs() {
        listDirs();
        DirAdapter adapter = new DirAdapter(_context, _entries, R.layout.li_row_textview, this._dateFormat);
        if (_list != null)
            _list.setAdapter(adapter);
        return adapter;
    }

    public void closeit(){
        _alertDialog.dismiss();
    }

    private List<File> _entries = new ArrayList<File>();
    private File _currentDir;
    private Context _context;
    private android.support.v7.app.AlertDialog _alertDialog;
    private ListView _list;
    private Result _result = null;
    private boolean _dirOnly;
    private FileFilter _fileFilter;
    private int _titleRes = R.string.choose_file, _okRes = R.string.title_choose, _cancelRes = R.string.dialog_cancel;
    private String _dateFormat;

    static FileFilter filterDirectoriesOnly = new FileFilter() {
        public boolean accept(File file) {
            return file.isDirectory();
        }
    };

    static FileFilter filterFiles = new FileFilter() {
        public boolean accept(File file) {
            return !file.isHidden();
        }
    };


}
