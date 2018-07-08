package pl.pwrobel.opticalfiletransfer;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.method.MovementMethod;
import android.text.style.URLSpan;
import android.text.util.Linkify;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by pwrobel on 25.06.17.
 */

public class HelpFragment extends DialogFragment {

    static HelpFragment newInstance() {
        HelpFragment f = new HelpFragment();
        return f;
    }

    @Override
    public void show(FragmentManager manager, String tag) {
        try {
            FragmentTransaction ft = manager.beginTransaction();
            ft.add(this, tag).addToBackStack(null);
            ft.commitAllowingStateLoss();
        } catch (IllegalStateException e) {
            Log.e("IllegalStateException", "Exception", e);
        }

    }

    private HelpDialogDismisser dismisser = null;
    public void setHelpDialogDismisser(HelpDialogDismisser dis){
        dismisser = dis;
    }

    boolean defaultchecked = false;
    public void setStartDismissStatus(boolean checked){
        this.defaultchecked = checked;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null) {
            dialog.setTitle("ABCCC");

        }
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
        //setStyle(STYLE_NORMAL, R.style.SettingFragmentDialog);
    }

    public static final String[] STRINGE = new String[0];
    private static final boolean addLinksspannable(Spannable spannable, Pattern pattern,
                                         String defaultScheme, String[] schemes,
                                         Linkify.MatchFilter matchFilter, Linkify.TransformFilter transformFilter) {
        final String[] schemesCopy;
        if (defaultScheme == null) defaultScheme = "";
        if (schemes == null || schemes.length < 1) {
            schemes = STRINGE;
        }

        schemesCopy = new String[schemes.length + 1];
        schemesCopy[0] = defaultScheme;
        for (int index = 0; index < schemes.length; index++) {
            String scheme = schemes[index];
            schemesCopy[index + 1] = (scheme == null) ? "" : scheme;//scheme.toLowerCase(Locale.ROOT);
        }

        boolean hasMatches = false;
        Matcher m = pattern.matcher(spannable);

        while (m.find()) {
            int start = m.start();
            int end = m.end();
            boolean allowed = true;

            if (matchFilter != null) {
                allowed = matchFilter.acceptMatch(spannable, start, end);
            }

            if (allowed) {
                String url = makeUrl(m.group(0), schemesCopy, m, transformFilter);

                applyLink(url, start, end, spannable);
                hasMatches = true;
            }
        }

        return hasMatches;
    }


    private static final void applyLink(String url, int start, int end, Spannable text) {
        URLSpan span = new URLSpan(url);

        text.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    private static final String makeUrl(String url, String[] prefixes,
                                        Matcher matcher, Linkify.TransformFilter filter) {
        if (filter != null) {
            url = filter.transformUrl(matcher, url);
        }

        boolean hasPrefix = false;

        for (int i = 0; i < prefixes.length; i++) {
            if (url.regionMatches(true, 0, prefixes[i], 0, prefixes[i].length())) {
                hasPrefix = true;

                // Fix capitalization if necessary
                if (!url.regionMatches(false, 0, prefixes[i], 0, prefixes[i].length())) {
                    url = prefixes[i] + url.substring(prefixes[i].length());
                }

                break;
            }
        }

        if (!hasPrefix && prefixes.length > 0) {
            url = prefixes[0] + url;
        }

        return url;
    }

    private static final void addLinkMovementMethod(TextView t) {
        MovementMethod m = t.getMovementMethod();

        if ((m == null) || !(m instanceof LinkMovementMethod)) {
            if (t.getLinksClickable()) {
                t.setMovementMethod(LinkMovementMethod.getInstance());
            }
        }
    }


    private static final void addLinksC0(TextView text, Pattern pattern,
                                      String defaultScheme, String[] schemes,
                                      Linkify.MatchFilter matchFilter, Linkify.TransformFilter transformFilter) {
        SpannableString spannable = SpannableString.valueOf(text.getText());

        boolean linksAdded = addLinksspannable(spannable, pattern, defaultScheme, schemes, matchFilter,
                transformFilter);
        if (linksAdded) {
            text.setText(spannable);
            addLinkMovementMethod(text);
        }
    }

    private static final void addLinksC(TextView text, Pattern pattern,
                                      String scheme, Linkify.MatchFilter matchFilter,
                                      Linkify.TransformFilter transformFilter) {
        addLinksC0(text, pattern, scheme, null, matchFilter, transformFilter);
    }

    public static void internaladdLinks(TextView textView, String linkThis, String toThis) {
        Pattern pattern = Pattern.compile(linkThis);
        String scheme = toThis;
        addLinksC(textView, pattern, scheme, new android.text.util.Linkify.MatchFilter() {
            @Override
            public boolean acceptMatch(CharSequence s, int start, int end) {
                return true;
            }
        }, new android.text.util.Linkify.TransformFilter() {

            @Override
            public String transformUrl(Matcher match, String url) {
                return "";
            }
        });
    }

    private boolean is_dismiss_from_ok_button = false;
    @Override
    public void onDismiss(final DialogInterface dialog) {
        super.onDismiss(dialog);
        if (this.dismisser != null){
            //if (!is_dismiss_from_ok_button)
                this.dismisser.onSetHelpWindowGone();
            is_dismiss_from_ok_button = false;
        }
    }

    CheckBox dismissbox = null;
    TextView OKtext = null;
    TextView textViewhelpd1 = null;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.help_fragment, container, false);
        this.dismissbox = (CheckBox) v.findViewById(R.id.checkBoxdismiss);
        this.OKtext = (TextView) v.findViewById(R.id.textViewok1);
        this.OKtext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((Activity)dismisser).runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        is_dismiss_from_ok_button = true;
                        HelpFragment.this.dismiss();
                    }
                });
            }
        });

        this.textViewhelpd1 = (TextView) v.findViewById(R.id.textViewhelpd1);
        internaladdLinks(this.textViewhelpd1, "Windows", ((Activity)dismisser).getString(R.string.link_win));
        internaladdLinks(this.textViewhelpd1, "Linux", ((Activity)dismisser).getString(R.string.link_lin));
        internaladdLinks(this.textViewhelpd1, ((Activity)dismisser).getString(R.string.lnkweb2), ((Activity)dismisser).getString(R.string.linkifyab2val));

        this.dismissbox.setChecked(this.defaultchecked);
        this.dismissbox.requestLayout();
        this.dismissbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (dismisser != null)
                    dismisser.onSetDismissedStatus(isChecked);
                if (isChecked){
                    new Timer().schedule(new TimerTask()
                    {
                        @Override
                        public void run()
                        {
                            ((Activity)dismisser).runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    HelpFragment.this.dismisser.onSetHelpWindowGone();
                                    is_dismiss_from_ok_button = false;
                                    HelpFragment.this.dismiss();
                                }
                            });
                        }
                    }, 700);

                }
            }
        });
        getDialog().requestWindowFeature(Window.FEATURE_NO_TITLE);
        getDialog().getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        return v;
    }

}
