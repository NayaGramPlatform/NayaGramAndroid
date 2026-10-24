package org.nayagram.platform.textstyle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextCaption;

import java.util.ArrayList;
import java.util.List;

/**
 * NGTextStyleHelper - Persistent Text Formatting & Auto-Styler.
 * Matches Novagram's Text Style feature:
 * - Allows users to pick a default persistent font/entity style (Regular, Spoiler, Bold, Italic, Mono, Strikethrough, Underline, Quote).
 * - Ticking any style automatically formats outgoing messages without having to select text and reformat each time.
 */
public final class NGTextStyleHelper {

    public static final int STYLE_NONE = -1;
    public static final int STYLE_REGULAR = 54;
    public static final int STYLE_SPOILER = 57;
    public static final int STYLE_BOLD = 50;
    public static final int STYLE_ITALIC = 51;
    public static final int STYLE_MONO = 52;
    public static final int STYLE_STRIKE = 55;
    public static final int STYLE_UNDERLINE = 56;
    public static final int STYLE_QUOTE = 58;

    private static final String PREF_NAME = "nayagram_text_style_prefs";
    private static final String KEY_STYLE = "selected_text_style";

    public static final int[] RADIO_STYLES = new int[] {
        STYLE_REGULAR, STYLE_SPOILER, STYLE_BOLD, STYLE_ITALIC,
        STYLE_MONO, STYLE_STRIKE, STYLE_UNDERLINE, STYLE_QUOTE
    };

    public static final String[] STYLE_NAMES = new String[] {
        "Regular", "Spoiler", "Bold", "Italic",
        "Mono", "Strikethrough", "Underline", "Quote"
    };

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getSelectedStyle() {
        try {
            return getPrefs().getInt(KEY_STYLE, STYLE_NONE);
        } catch (Exception e) {
            return STYLE_NONE;
        }
    }

    public static void setSelectedStyle(int style) {
        try {
            getPrefs().edit().putInt(KEY_STYLE, style).apply();
        } catch (Exception ignored) {}
    }

    public static String getSelectedStyleName() {
        int current = getSelectedStyle();
        for (int i = 0; i < RADIO_STYLES.length; i++) {
            if (RADIO_STYLES[i] == current) {
                return STYLE_NAMES[i];
            }
        }
        return "Regular";
    }

    /**
     * Applies the user's chosen text style to the entire message field before sending.
     * Operates once per send — O(1) zero-overhead formatting.
     */
    public static void applyStyle(EditTextCaption editField) {
        if (editField == null || editField.length() == 0) return;
        int style = getSelectedStyle();
        if (style == STYLE_NONE || style == STYLE_REGULAR) return;

        try {
            int length = editField.length();
            editField.setSelectionOverride(0, length);
            editField.makeSelectedRegular();
            editField.setSelectionOverride(0, length);

            switch (style) {
                case STYLE_BOLD:
                    editField.makeSelectedBold();
                    break;
                case STYLE_ITALIC:
                    editField.makeSelectedItalic();
                    break;
                case STYLE_SPOILER:
                    editField.makeSelectedSpoiler();
                    break;
                case STYLE_QUOTE:
                    editField.makeSelectedQuote();
                    break;
                case STYLE_MONO:
                    editField.makeSelectedMono();
                    break;
                case STYLE_STRIKE:
                    editField.makeSelectedStrike();
                    break;
                case STYLE_UNDERLINE:
                    editField.makeSelectedUnderline();
                    break;
            }
        } catch (Exception ignored) {}
    }

    /**
     * Displays the "Text style" selection dialog matching Novagram's UI.
     */
    public static void showDialog(Context context) {
        if (context == null) return;

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(24), AndroidUtilities.dp(8), AndroidUtilities.dp(24), AndroidUtilities.dp(8));

        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        int current = getSelectedStyle();
        int checkedIndex = 0;
        for (int i = 0; i < RADIO_STYLES.length; i++) {
            if (RADIO_STYLES[i] == current) {
                checkedIndex = i;
                break;
            }
        }

        List<CharSequence> labels = buildStyledLabels();
        for (int i = 0; i < labels.size(); i++) {
            RadioButton rb = new RadioButton(context);
            rb.setText(labels.get(i));
            rb.setTextSize(16);
            rb.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
            rb.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
            rb.setId(i);
            rb.setChecked(i == checkedIndex);
            radioGroup.addView(rb, new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ScrollView scroll = new ScrollView(context);
        scroll.addView(radioGroup);
        layout.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Text style");
        builder.setMessage("Select any style");
        builder.setView(layout);
        builder.setPositiveButton("Select", (d, w) -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            if (selectedId >= 0 && selectedId < RADIO_STYLES.length) {
                int chosen = RADIO_STYLES[selectedId];
                setSelectedStyle(chosen);
                Toast.makeText(context, "Text style set to: " + STYLE_NAMES[selectedId], Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Close", (d, w) -> d.dismiss());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static List<CharSequence> buildStyledLabels() {
        List<CharSequence> list = new ArrayList<>();
        list.add("Regular");
        list.add("Spoiler");

        SpannableStringBuilder bold = new SpannableStringBuilder("Bold");
        bold.setSpan(new StyleSpan(Typeface.BOLD), 0, bold.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(bold);

        SpannableStringBuilder italic = new SpannableStringBuilder("Italic");
        italic.setSpan(new StyleSpan(Typeface.ITALIC), 0, italic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(italic);

        SpannableStringBuilder mono = new SpannableStringBuilder("Mono");
        mono.setSpan(new TypefaceSpan("monospace"), 0, mono.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(mono);

        SpannableStringBuilder strike = new SpannableStringBuilder("Strikethrough");
        strike.setSpan(new android.text.style.StrikethroughSpan(), 0, strike.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(strike);

        SpannableStringBuilder underline = new SpannableStringBuilder("Underline");
        underline.setSpan(new android.text.style.UnderlineSpan(), 0, underline.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(underline);

        list.add("Quote");
        return list;
    }
}
