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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.LocaleController;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextCaption;

import java.util.ArrayList;
import java.util.List;

/**
 * NGTextStyleHelper - Persistent Text Formatting & Auto-Styler.
 * Features:
 * - Direct style picker across 1-on-1 chats, groups, channels, and bots.
 * - Radiant checkmark badge (উজ্জ্বল রঙের টিক চিহ্ন ✓) for the active selection.
 * - Prominent, beautifully styled Desktop/PC keyboard shortcuts guide card.
 * - Automatically applies chosen text style to outgoing messages.
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

    public static final String[] STYLE_DESCS = new String[] {
        "Standard readable plain text",
        "Hidden covered text revealed on tap",
        "Bold emphasized text",
        "Slanted italic font style",
        "Fixed-width monospace coding font",
        "Strikethrough crossed line format",
        "Underlined bottom accent format",
        "Prominent blockquote format"
    };

    public static final int[] STYLE_COLORS = new int[] {
        0xFF0EA5E9, // Regular: Sky blue
        0xFFF59E0B, // Spoiler: Amber
        0xFF2563EB, // Bold: Royal blue
        0xFF9333EA, // Italic: Purple
        0xFF0D9488, // Mono: Teal
        0xFF64748B, // Strikethrough: Slate
        0xFF0284C7, // Underline: Ocean
        0xFF4F46E5  // Quote: Indigo
    };

    public static final String[] STYLE_ICONS = new String[] {
        "Aa", "✦", "B", "I", "</>", "S̶", "U̲", "❝"
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
     */
    public static void applyStyle(EditTextCaption editField) {
        if (editField == null || editField.length() == 0) return;
        int style = getSelectedStyle();
        if (style == STYLE_NONE || style == STYLE_REGULAR) return;

        try {
            String str = editField.getText() != null ? editField.getText().toString().trim() : "";
            int[] emojiOnly = new int[1];
            org.telegram.messenger.Emoji.replaceEmoji(str, null, false, emojiOnly);
            if (emojiOnly[0] > 0 || org.telegram.messenger.Emoji.isValidEmoji(str)) {
                return;
            }
        } catch (Exception ignored) {}

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
     * Displays the modernized "Text style" selection dialog across all chats, groups, channels, and bots.
     * Features vibrant radiant checkmark badges (উজ্জ্বল রঙের টিক চিহ্ন ✓) and an enlarged Desktop/PC shortcuts cheatsheet.
     */
    public static void showDialog(Context context) {
        if (context == null) return;

        final boolean isDark = Theme.isCurrentThemeDark();
        final int current = getSelectedStyle();
        final int[] checkedIndex = new int[] { 0 };
        for (int i = 0; i < RADIO_STYLES.length; i++) {
            if (RADIO_STYLES[i] == current) {
                checkedIndex[0] = i;
                break;
            }
        }

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(12));

        // Subtitle guide
        TextView subTitle = new TextView(context);
        subTitle.setText("Choose default text style for messages & replies");
        subTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subTitle.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        subTitle.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(10));
        root.addView(subTitle);

        final LinearLayout itemsList = new LinearLayout(context);
        itemsList.setOrientation(LinearLayout.VERTICAL);

        final List<CharSequence> labels = buildStyledLabels();
        final View[] rowViews = new View[RADIO_STYLES.length];
        final TextView[] tickViews = new TextView[RADIO_STYLES.length];

        for (int i = 0; i < RADIO_STYLES.length; i++) {
            final int index = i;
            final int styleColor = STYLE_COLORS[i];

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

            LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rowParams.bottomMargin = AndroidUtilities.dp(8);
            row.setLayoutParams(rowParams);

            // Left icon badge
            TextView iconBadge = new TextView(context);
            iconBadge.setText(STYLE_ICONS[i]);
            iconBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            iconBadge.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            iconBadge.setTextColor(Color.WHITE);
            iconBadge.setGravity(Gravity.CENTER);

            GradientDrawable iconBg = new GradientDrawable();
            iconBg.setShape(GradientDrawable.OVAL);
            iconBg.setColor(styleColor);
            iconBadge.setBackground(iconBg);

            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(34), AndroidUtilities.dp(34));
            iconParams.rightMargin = AndroidUtilities.dp(12);
            row.addView(iconBadge, iconParams);

            // Middle text container
            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView titleView = new TextView(context);
            titleView.setText(labels.get(i));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            textCol.addView(titleView);

            TextView descView = new TextView(context);
            descView.setText(STYLE_DESCS[i]);
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            descView.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
            descView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            textCol.addView(descView);

            row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            // Right selection badge (উজ্জ্বল রঙের টিক চিহ্ন ✓)
            TextView tickBadge = new TextView(context);
            tickBadge.setGravity(Gravity.CENTER);
            tickViews[i] = tickBadge;

            LinearLayout.LayoutParams tickParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(28), AndroidUtilities.dp(28));
            tickParams.leftMargin = AndroidUtilities.dp(8);
            row.addView(tickBadge, tickParams);

            rowViews[i] = row;

            row.setOnClickListener(v -> {
                checkedIndex[0] = index;
                setSelectedStyle(RADIO_STYLES[index]);
                updateRowStates(rowViews, tickViews, checkedIndex[0], isDark);
                Toast.makeText(context, "Style active: " + STYLE_NAMES[index] + " ✓", Toast.LENGTH_SHORT).show();
            });

            itemsList.addView(row);
        }

        updateRowStates(rowViews, tickViews, checkedIndex[0], isDark);

        ScrollView scroll = new ScrollView(context);
        scroll.setVerticalScrollBarEnabled(true);
        scroll.addView(itemsList);
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, AndroidUtilities.dp(230)));

        // Prominent Desktop / PC Keyboard Shortcuts Section
        View keyboardGuideCard = buildKeyboardShortcutCard(context);
        LinearLayout.LayoutParams guideParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        guideParams.topMargin = AndroidUtilities.dp(12);
        root.addView(keyboardGuideCard, guideParams);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Text style");
        builder.setView(root);
        builder.setPositiveButton("Select", (d, w) -> {
            int chosenIdx = checkedIndex[0];
            if (chosenIdx >= 0 && chosenIdx < RADIO_STYLES.length) {
                int chosen = RADIO_STYLES[chosenIdx];
                setSelectedStyle(chosen);
                Toast.makeText(context, "Text style set to: " + STYLE_NAMES[chosenIdx] + " ✓", Toast.LENGTH_SHORT).show();
            }
            d.dismiss();
        });
        builder.setNegativeButton("Close", (d, w) -> d.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    /**
     * Updates visual state: selected row receives glowing border and vibrant checkmark (✓).
     */
    private static void updateRowStates(View[] rowViews, TextView[] tickViews, int selectedIndex, boolean isDark) {
        for (int i = 0; i < rowViews.length; i++) {
            boolean isSelected = (i == selectedIndex);
            View row = rowViews[i];
            TextView tick = tickViews[i];

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(AndroidUtilities.dp(14));

            if (isSelected) {
                // Vibrant glowing accent background + cyan border
                rowBg.setColor(isDark ? 0x3300D4FF : 0x180284C7);
                rowBg.setStroke(AndroidUtilities.dp(1.8f), isDark ? 0xFF00D4FF : 0xFF0284C7);

                // Radiant emerald checkmark badge (উজ্জ্বল সবুজ টিক চিহ্ন ✓)
                GradientDrawable tickBg = new GradientDrawable();
                tickBg.setShape(GradientDrawable.OVAL);
                tickBg.setColor(0xFF10B981); // Emerald Green
                tick.setBackground(tickBg);
                tick.setText("✓");
                tick.setTextColor(Color.WHITE);
                tick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
                tick.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            } else {
                rowBg.setColor(isDark ? 0xFF1E293B : 0xFFF8FAFC);
                rowBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFE2E8F0);

                // Empty ring
                GradientDrawable ringBg = new GradientDrawable();
                ringBg.setShape(GradientDrawable.OVAL);
                ringBg.setColor(Color.TRANSPARENT);
                ringBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF475569 : 0xFFCBD5E1);
                tick.setBackground(ringBg);
                tick.setText("");
            }
            row.setBackground(rowBg);
        }
    }

    /**
     * Builds an enlarged, beautifully styled Desktop / PC Keyboard Shortcuts cheatsheet card.
     */
    private static View buildKeyboardShortcutCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        boolean isDark = Theme.isCurrentThemeDark();
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isDark ? 0xFF0F172A : 0xFFF1F5F9);
        gd.setCornerRadius(AndroidUtilities.dp(14));
        gd.setStroke(AndroidUtilities.dp(1.5f), isDark ? 0xFF38BDF8 : 0xFF0284C7);
        card.setBackground(gd);

        // Header Title
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("💻 Desktop / PC Keyboard Shortcuts");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        titleRow.addView(title);

        card.addView(titleRow);

        TextView subtitle = new TextView(context);
        subtitle.setText("Quick formatting keys for Telegram Desktop, Web & Mac:");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        subtitle.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        subtitle.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(8));
        card.addView(subtitle);

        String[][] shortcuts = new String[][]{
            {"Ctrl + B", "Bold (বোল্ড)"},
            {"Ctrl + I", "Italic (ইটালিক)"},
            {"Ctrl + U", "Underline (নিচে দাগ)"},
            {"Ctrl + Shift + M", "Monospace (মনোস্পেস)"},
            {"Ctrl + Shift + X", "Strikethrough (মাঝখান কাটা)"},
            {"Ctrl + Shift + P", "Spoiler (স্পয়লার)"},
            {"Ctrl + Shift + >", "Quote (কোট / উদ্ধৃতি)"}
        };

        for (String[] pair : shortcuts) {
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(0, AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3));

            // Key cap badge
            TextView keyBadge = new TextView(context);
            keyBadge.setText(pair[0]);
            keyBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            keyBadge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            keyBadge.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0369A1);
            keyBadge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));

            GradientDrawable keyBg = new GradientDrawable();
            keyBg.setColor(isDark ? 0xFF1E293B : 0xFFE2E8F0);
            keyBg.setCornerRadius(AndroidUtilities.dp(6));
            keyBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF334155 : 0xFFCBD5E1);
            keyBadge.setBackground(keyBg);
            item.addView(keyBadge);

            // Arrow
            TextView arrow = new TextView(context);
            arrow.setText(" › ");
            arrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            arrow.setTextColor(isDark ? 0xFF00D4FF : 0xFF0284C7);
            item.addView(arrow);

            // Action name
            TextView action = new TextView(context);
            action.setText(pair[1]);
            action.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            action.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
            item.addView(action);

            card.addView(item);
        }

        return card;
    }

    private static List<CharSequence> buildStyledLabels() {
        List<CharSequence> list = new ArrayList<>();
        boolean isBn = false;
        try {
            if (LocaleController.getInstance() != null && LocaleController.getInstance().getCurrentLocale() != null) {
                isBn = "bn".equalsIgnoreCase(LocaleController.getInstance().getCurrentLocale().getLanguage());
            }
        } catch (Throwable ignored) {}

        // Regular
        SpannableStringBuilder regular = new SpannableStringBuilder("Regular" + (isBn ? " (সাধারণ)" : ""));
        regular.setSpan(new ForegroundColorSpan(0xFF0EA5E9), 0, regular.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(regular);

        // Spoiler
        SpannableStringBuilder spoiler = new SpannableStringBuilder("Spoiler" + (isBn ? " (স্পয়লার)" : ""));
        spoiler.setSpan(new ForegroundColorSpan(0xFFD97706), 0, spoiler.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(spoiler);

        // Bold
        SpannableStringBuilder bold = new SpannableStringBuilder("Bold" + (isBn ? " (বোল্ড)" : ""));
        bold.setSpan(new StyleSpan(Typeface.BOLD), 0, bold.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        bold.setSpan(new ForegroundColorSpan(0xFF1D4ED8), 0, bold.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(bold);

        // Italic
        SpannableStringBuilder italic = new SpannableStringBuilder("Italic" + (isBn ? " (ইটালিক)" : ""));
        italic.setSpan(new StyleSpan(Typeface.ITALIC), 0, italic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        italic.setSpan(new ForegroundColorSpan(0xFF7E22CE), 0, italic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(italic);

        // Mono
        SpannableStringBuilder mono = new SpannableStringBuilder("Mono" + (isBn ? " (মনোস্পেস)" : ""));
        mono.setSpan(new TypefaceSpan("monospace"), 0, mono.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mono.setSpan(new ForegroundColorSpan(0xFF0D9488), 0, mono.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(mono);

        // Strikethrough
        SpannableStringBuilder strike = new SpannableStringBuilder("Strikethrough" + (isBn ? " (মাঝখান কাটা)" : ""));
        strike.setSpan(new StrikethroughSpan(), 0, strike.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        strike.setSpan(new ForegroundColorSpan(0xFF475569), 0, strike.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(strike);

        // Underline
        SpannableStringBuilder underline = new SpannableStringBuilder("Underline" + (isBn ? " (নিচে দাগ)" : ""));
        underline.setSpan(new UnderlineSpan(), 0, underline.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        underline.setSpan(new ForegroundColorSpan(0xFF0284C7), 0, underline.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(underline);

        // Quote
        SpannableStringBuilder quote = new SpannableStringBuilder("Quote" + (isBn ? " (কোট)" : ""));
        quote.setSpan(new ForegroundColorSpan(0xFF4338CA), 0, quote.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(quote);

        return list;
    }
}
