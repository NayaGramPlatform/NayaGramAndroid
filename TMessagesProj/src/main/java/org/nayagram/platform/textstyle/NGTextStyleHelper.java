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
 * - Radiant emerald checkmark badge (উজ্জ্বল সবুজ টিক চিহ্ন ✓) for the active selection.
 * - Spacious, elegant international Desktop & Web keyboard shortcuts panel.
 * - Luxurious Telegram royal blue, cyan, sapphire & electric indigo color scheme (zero yellow, red, or dull gray).
 * - Automatically applies chosen text style to outgoing messages and auto-replies.
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
        "Monospace", "Strikethrough", "Underline", "Quote"
    };

    public static final String[] STYLE_DESCS = new String[] {
        "Standard readable text format",
        "Tap-to-reveal hidden spoiler text",
        "Emphasized bold headline weight",
        "Smooth slanted cursive italic font",
        "Clean fixed-width code font",
        "Horizontal strike line format",
        "Crisp bottom underline accent",
        "Prominent blockquote citation"
    };

    // Cohesive, premium Telegram royal blue, cyan, sapphire & indigo palette (NO yellow, NO red, NO dull gray)
    public static final int[] STYLE_COLORS = new int[] {
        0xFF0EA5E9, // Regular: Sky Blue
        0xFF2563EB, // Spoiler: Royal Sapphire
        0xFF1D4ED8, // Bold: Telegram Deep Blue
        0xFF6366F1, // Italic: Electric Indigo
        0xFF0284C7, // Monospace: Ocean Cobalt
        0xFF38BDF8, // Strikethrough: Ice Blue
        0xFF00D4FF, // Underline: Vivid Cyan
        0xFF4F46E5  // Quote: Imperial Blue
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
            android.text.Editable editable = editField.getText();
            if (editable == null || editable.length() == 0) return;
            int length = editable.length();

            org.telegram.ui.Components.TextStyleSpan.TextStyleRun run = new org.telegram.ui.Components.TextStyleSpan.TextStyleRun();
            switch (style) {
                case STYLE_BOLD:
                    run.flags |= org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_BOLD;
                    break;
                case STYLE_ITALIC:
                    run.flags |= org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_ITALIC;
                    break;
                case STYLE_SPOILER:
                    run.flags |= org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_SPOILER;
                    break;
                case STYLE_MONO:
                    run.flags |= org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_MONO;
                    break;
                case STYLE_STRIKE:
                    run.flags |= org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_STRIKE;
                    break;
                case STYLE_UNDERLINE:
                    run.flags |= org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_UNDERLINE;
                    break;
                case STYLE_QUOTE:
                    editField.setSelectionOverride(0, length);
                    editField.makeSelectedQuote();
                    return;
            }
            if (run.flags != 0) {
                org.telegram.messenger.MediaDataController.addStyleToText(new org.telegram.ui.Components.TextStyleSpan(run), 0, length, editable, true);
                if ((run.flags & org.telegram.ui.Components.TextStyleSpan.FLAG_STYLE_SPOILER) != 0) {
                    editField.invalidate();
                }
            }
        } catch (Exception ignored) {}
    }

    /**
     * Displays the modernized "Text style" selection dialog across all chats, groups, channels, and bots.
     * Features spacious international design, vibrant checkmarks, and clean keyboard shortcuts.
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

        ScrollView mainScrollView = new ScrollView(context);
        mainScrollView.setVerticalScrollBarEnabled(false);

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        mainScrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Subtitle guide - clean international English
        TextView subTitle = new TextView(context);
        subTitle.setText("Choose default text style for outgoing messages and replies");
        subTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subTitle.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        subTitle.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(12));
        root.addView(subTitle);

        final LinearLayout itemsList = new LinearLayout(context);
        itemsList.setOrientation(LinearLayout.VERTICAL);
        root.addView(itemsList);

        final List<CharSequence> labels = buildStyledLabels();
        final View[] rowViews = new View[RADIO_STYLES.length];
        final TextView[] tickViews = new TextView[RADIO_STYLES.length];

        for (int i = 0; i < RADIO_STYLES.length; i++) {
            final int index = i;
            final int styleColor = STYLE_COLORS[i];

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(11), AndroidUtilities.dp(14), AndroidUtilities.dp(11));

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

            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(36), AndroidUtilities.dp(36));
            iconParams.rightMargin = AndroidUtilities.dp(14);
            row.addView(iconBadge, iconParams);

            // Middle text container
            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView titleView = new TextView(context);
            titleView.setText(labels.get(i));
            titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            titleView.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            textCol.addView(titleView);

            TextView descView = new TextView(context);
            descView.setText(STYLE_DESCS[i]);
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            descView.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
            descView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            textCol.addView(descView);

            row.addView(textCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            // Right selection badge (উজ্জ্বল সবুজ টিক চিহ্ন ✓)
            TextView tickBadge = new TextView(context);
            tickBadge.setGravity(Gravity.CENTER);
            tickViews[i] = tickBadge;

            LinearLayout.LayoutParams tickParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(26), AndroidUtilities.dp(26));
            tickParams.leftMargin = AndroidUtilities.dp(10);
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

        // Prominent Desktop / PC Keyboard Shortcuts Section (Spacious, airy & international)
        View keyboardGuideCard = buildKeyboardShortcutCard(context);
        LinearLayout.LayoutParams guideParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        guideParams.topMargin = AndroidUtilities.dp(14);
        guideParams.bottomMargin = AndroidUtilities.dp(6);
        root.addView(keyboardGuideCard, guideParams);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Text style");
        builder.setView(mainScrollView);
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
     * Updates visual state: selected row receives glowing border and vibrant emerald checkmark (✓).
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
                rowBg.setColor(isDark ? 0x2E0284C7 : 0x140284C7);
                rowBg.setStroke(AndroidUtilities.dp(1.8f), isDark ? 0xFF00D4FF : 0xFF0284C7);

                // Radiant emerald checkmark badge (উজ্জ্বল সবুজ টিক চিহ্ন ✓)
                GradientDrawable tickBg = new GradientDrawable();
                tickBg.setShape(GradientDrawable.OVAL);
                tickBg.setColor(0xFF10B981); // Emerald Green
                tick.setBackground(tickBg);
                tick.setText("✓");
                tick.setTextColor(Color.WHITE);
                tick.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
                tick.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            } else {
                rowBg.setColor(isDark ? 0xFF1E293B : 0xFFF8FAFC);
                rowBg.setStroke(AndroidUtilities.dp(1f), isDark ? 0xFF334155 : 0xFFE2E8F0);

                // Subtle empty circle
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
     * Builds an airy, beautifully styled Desktop & Web Keyboard Shortcuts card.
     * Clean international English without dense parenthetical text.
     */
    private static View buildKeyboardShortcutCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

        boolean isDark = Theme.isCurrentThemeDark();
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isDark ? 0xFF0F172A : 0xFFF1F5F9);
        gd.setCornerRadius(AndroidUtilities.dp(16));
        gd.setStroke(AndroidUtilities.dp(1.5f), isDark ? 0xFF0284C7 : 0xFF38BDF8);
        card.setBackground(gd);

        // Header Title
        LinearLayout titleRow = new LinearLayout(context);
        titleRow.setOrientation(LinearLayout.HORIZONTAL);
        titleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(context);
        title.setText("💻 Desktop & Web Shortcuts");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        title.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        titleRow.addView(title);
        card.addView(titleRow);

        TextView subtitle = new TextView(context);
        subtitle.setText("Quick formatting keys for Windows, macOS & Telegram Web:");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        subtitle.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        subtitle.setPadding(0, AndroidUtilities.dp(3), 0, AndroidUtilities.dp(10));
        card.addView(subtitle);

        String[][] shortcuts = new String[][]{
            {"Ctrl + B", "Bold"},
            {"Ctrl + I", "Italic"},
            {"Ctrl + U", "Underline"},
            {"Ctrl + Shift + M", "Monospace"},
            {"Ctrl + Shift + X", "Strikethrough"},
            {"Ctrl + Shift + P", "Spoiler"},
            {"Ctrl + Shift + >", "Quote"}
        };

        for (String[] pair : shortcuts) {
            LinearLayout item = new LinearLayout(context);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(Gravity.CENTER_VERTICAL);
            item.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4));

            // Tactile keycap badge
            TextView keyBadge = new TextView(context);
            keyBadge.setText(pair[0]);
            keyBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
            keyBadge.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            keyBadge.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
            keyBadge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));

            GradientDrawable keyBg = new GradientDrawable();
            keyBg.setColor(isDark ? 0xFF1E293B : 0xFFE2E8F0);
            keyBg.setCornerRadius(AndroidUtilities.dp(6));
            keyBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF334155 : 0xFFCBD5E1);
            keyBadge.setBackground(keyBg);
            item.addView(keyBadge);

            // Clean bullet separator
            TextView arrow = new TextView(context);
            arrow.setText("  ›  ");
            arrow.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
            arrow.setTextColor(isDark ? 0xFF00D4FF : 0xFF0284C7);
            item.addView(arrow);

            // Action name - clean international English
            TextView action = new TextView(context);
            action.setText(pair[1]);
            action.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            action.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
            action.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
            item.addView(action);

            card.addView(item);
        }

        return card;
    }

    private static List<CharSequence> buildStyledLabels() {
        List<CharSequence> list = new ArrayList<>();

        // 1. Regular
        SpannableStringBuilder regular = new SpannableStringBuilder("Regular");
        regular.setSpan(new ForegroundColorSpan(0xFF0EA5E9), 0, regular.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(regular);

        // 2. Spoiler
        SpannableStringBuilder spoiler = new SpannableStringBuilder("Spoiler");
        spoiler.setSpan(new ForegroundColorSpan(0xFF2563EB), 0, spoiler.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(spoiler);

        // 3. Bold
        SpannableStringBuilder bold = new SpannableStringBuilder("Bold");
        bold.setSpan(new StyleSpan(Typeface.BOLD), 0, bold.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        bold.setSpan(new ForegroundColorSpan(0xFF1D4ED8), 0, bold.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(bold);

        // 4. Italic
        SpannableStringBuilder italic = new SpannableStringBuilder("Italic");
        italic.setSpan(new StyleSpan(Typeface.ITALIC), 0, italic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        italic.setSpan(new ForegroundColorSpan(0xFF6366F1), 0, italic.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(italic);

        // 5. Monospace
        SpannableStringBuilder mono = new SpannableStringBuilder("Monospace");
        mono.setSpan(new TypefaceSpan("monospace"), 0, mono.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        mono.setSpan(new ForegroundColorSpan(0xFF0284C7), 0, mono.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(mono);

        // 6. Strikethrough
        SpannableStringBuilder strike = new SpannableStringBuilder("Strikethrough");
        strike.setSpan(new StrikethroughSpan(), 0, strike.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        strike.setSpan(new ForegroundColorSpan(0xFF38BDF8), 0, strike.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(strike);

        // 7. Underline
        SpannableStringBuilder underline = new SpannableStringBuilder("Underline");
        underline.setSpan(new UnderlineSpan(), 0, underline.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        underline.setSpan(new ForegroundColorSpan(0xFF00D4FF), 0, underline.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(underline);

        // 8. Quote
        SpannableStringBuilder quote = new SpannableStringBuilder("Quote");
        quote.setSpan(new ForegroundColorSpan(0xFF4F46E5), 0, quote.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        list.add(quote);

        return list;
    }
}
