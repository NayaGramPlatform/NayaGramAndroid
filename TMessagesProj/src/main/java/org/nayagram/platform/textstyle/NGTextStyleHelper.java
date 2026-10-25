package org.nayagram.platform.textstyle;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
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
 * - Includes PC/Desktop keyboard shortcut guide.
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
     * Displays the "Text style" selection dialog matching Novagram's UI,
     * with vibrant labels, instant selection persistence, and PC shortcuts.
     */
    public static void showDialog(Context context) {
        if (context == null) return;

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));

        int current = getSelectedStyle();
        int checkedIndex = 0;
        for (int i = 0; i < RADIO_STYLES.length; i++) {
            if (RADIO_STYLES[i] == current) {
                checkedIndex = i;
                break;
            }
        }

        RadioGroup radioGroup = new RadioGroup(context);
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        List<CharSequence> labels = buildStyledLabels();
        for (int i = 0; i < labels.size(); i++) {
            final int index = i;
            RadioButton rb = new RadioButton(context);
            rb.setText(labels.get(i));
            rb.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
            rb.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), 0, AndroidUtilities.dp(10));
            rb.setId(1000 + i);
            if (i == checkedIndex) {
                rb.setChecked(true);
            }
            rb.setOnClickListener(v -> {
                setSelectedStyle(RADIO_STYLES[index]);
                Toast.makeText(context, "Saved: " + STYLE_NAMES[index], Toast.LENGTH_SHORT).show();
            });
            radioGroup.addView(rb, new RadioGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        ScrollView scroll = new ScrollView(context);
        scroll.addView(radioGroup);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(220)));

        // Add Computer / Keyboard Shortcuts section
        View keyboardGuideCard = buildKeyboardShortcutCard(context);
        LinearLayout.LayoutParams guideParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        guideParams.topMargin = AndroidUtilities.dp(14);
        root.addView(keyboardGuideCard, guideParams);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Text style");
        builder.setMessage("Select any style");
        builder.setView(root);
        builder.setPositiveButton("Select", (d, w) -> {
            int selectedId = radioGroup.getCheckedRadioButtonId();
            int idx = selectedId - 1000;
            if (idx >= 0 && idx < RADIO_STYLES.length) {
                int chosen = RADIO_STYLES[idx];
                setSelectedStyle(chosen);
                Toast.makeText(context, "Text style set to: " + STYLE_NAMES[idx], Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("Close", (d, w) -> d.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private static View buildKeyboardShortcutCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        boolean isDark = Theme.isCurrentThemeDark();
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isDark ? 0xFF1E293B : 0xFFF1F5F9);
        gd.setCornerRadius(AndroidUtilities.dp(12));
        gd.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF334155 : 0xFFCBD5E1);
        card.setBackground(gd);

        TextView title = new TextView(context);
        title.setText("💻 PC/Desktop Keyboard Shortcuts:");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        card.addView(title);

        String[] shortcuts = new String[]{
            "Ctrl + B : Bold",
            "Ctrl + I : Italic",
            "Ctrl + U : Underline",
            "Ctrl + Shift + M : Monospace",
            "Ctrl + Shift + X : Strikethrough",
            "Ctrl + Shift + P : Spoiler",
            "Ctrl + Shift + > : Quote"
        };

        for (String s : shortcuts) {
            TextView tv = new TextView(context);
            tv.setText("• " + s);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            tv.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
            tv.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            card.addView(tv);
        }

        return card;
    }

    private static List<CharSequence> buildStyledLabels() {
        List<CharSequence> list = new ArrayList<>();

        // Regular
        SpannableStringBuilder regular = new SpannableStringBuilder("Regular (সাধারণ)");
        regular.setSpan(new ForegroundColorSpan(0xFF0EA5E9), 0, regular.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(regular);

        // Spoiler
        SpannableStringBuilder spoiler = new SpannableStringBuilder("Spoiler (স্পয়লার)");
        spoiler.setSpan(new ForegroundColorSpan(0xFFF59E0B), 0, spoiler.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(spoiler);

        // Bold
        SpannableStringBuilder bold = new SpannableStringBuilder("Bold (বোল্ড)");
        bold.setSpan(new StyleSpan(Typeface.BOLD), 0, bold.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        bold.setSpan(new ForegroundColorSpan(0xFFEF4444), 0, bold.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(bold);

        // Italic
        SpannableStringBuilder italic = new SpannableStringBuilder("Italic (ইটালিক)");
        italic.setSpan(new StyleSpan(Typeface.ITALIC), 0, italic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        italic.setSpan(new ForegroundColorSpan(0xFFA855F7), 0, italic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(italic);

        // Mono
        SpannableStringBuilder mono = new SpannableStringBuilder("Mono (মনোস্পেস)");
        mono.setSpan(new TypefaceSpan("monospace"), 0, mono.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mono.setSpan(new ForegroundColorSpan(0xFF10B981), 0, mono.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(mono);

        // Strikethrough
        SpannableStringBuilder strike = new SpannableStringBuilder("Strikethrough (মাঝখান কাটা)");
        strike.setSpan(new StrikethroughSpan(), 0, strike.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        strike.setSpan(new ForegroundColorSpan(0xFFEC4899), 0, strike.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(strike);

        // Underline
        SpannableStringBuilder underline = new SpannableStringBuilder("Underline (নিচে দাগ)");
        underline.setSpan(new UnderlineSpan(), 0, underline.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        underline.setSpan(new ForegroundColorSpan(0xFF3B82F6), 0, underline.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(underline);

        // Quote
        SpannableStringBuilder quote = new SpannableStringBuilder("Quote (কোট)");
        quote.setSpan(new ForegroundColorSpan(0xFF6366F1), 0, quote.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(quote);

        return list;
    }
}
