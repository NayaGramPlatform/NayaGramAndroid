package org.nayagram.platform.ui;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.LeadingMarginSpan;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.R;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.QuoteSpan;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.TypefaceSpan;

import java.util.ArrayList;

/**
 * NGTextStyleHelper - NayaGram Text Style Manager.
 * Allows users to pick a default outgoing text style (Regular, Bold, Italic, Mono,
 * Strike, Underline, Spoiler, Quote) applied automatically when sending messages,
 * plus a compact scrollable modal with high-contrast, crisp small labels and
 * Desktop & Web keyboard shortcuts.
 */
public class NGTextStyleHelper {

    private static final String PREFS_NAME = "nayagram_text_style_prefs";
    private static final String KEY_SELECTED_STYLE = "selected_text_style";

    public static final int STYLE_REGULAR = 0;
    public static final int STYLE_BOLD = 1;
    public static final int STYLE_ITALIC = 2;
    public static final int STYLE_MONO = 3;
    public static final int STYLE_STRIKE = 4;
    public static final int STYLE_UNDERLINE = 5;
    public static final int STYLE_SPOILER = 6;
    public static final int STYLE_QUOTE = 7;

    private static SharedPreferences getPrefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static int getSelectedStyle() {
        try {
            return getPrefs().getInt(KEY_SELECTED_STYLE, STYLE_REGULAR);
        } catch (Throwable e) {
            return STYLE_REGULAR;
        }
    }

    public static void setSelectedStyle(int style) {
        try {
            getPrefs().edit().putInt(KEY_SELECTED_STYLE, style).apply();
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    public static String getSelectedStyleName() {
        return getStyleName(getSelectedStyle());
    }

    public static String getStyleName(int style) {
        switch (style) {
            case STYLE_BOLD:
                return "Bold";
            case STYLE_ITALIC:
                return "Italic";
            case STYLE_MONO:
                return "Monospace";
            case STYLE_STRIKE:
                return "Strikethrough";
            case STYLE_UNDERLINE:
                return "Underline";
            case STYLE_SPOILER:
                return "Spoiler";
            case STYLE_QUOTE:
                return "Quote";
            case STYLE_REGULAR:
            default:
                return "Regular";
        }
    }

    public static String getStyleDescription(int style) {
        switch (style) {
            case STYLE_BOLD:
                return "Strong heavy text  •  Ctrl + B";
            case STYLE_ITALIC:
                return "Slanted emphasis text  •  Ctrl + I";
            case STYLE_MONO:
                return "Code & fixed-width font  •  Ctrl + Shift + M";
            case STYLE_STRIKE:
                return "Crossed-out line text  •  Ctrl + Shift + X";
            case STYLE_UNDERLINE:
                return "Underlined emphasis text  •  Ctrl + U";
            case STYLE_SPOILER:
                return "Hidden shimmer until tapped  •  Ctrl + Shift + P";
            case STYLE_QUOTE:
                return "Highlighted quote block  •  Ctrl + Shift + .";
            case STYLE_REGULAR:
            default:
                return "Standard clean message text  •  Ctrl + Shift + N";
        }
    }

    public static int getStyleBadgeColor(int style) {
        switch (style) {
            case STYLE_BOLD:
                return 0xFF2563EB; // Royal Blue
            case STYLE_ITALIC:
                return 0xFF7C3AED; // Vivid Violet
            case STYLE_MONO:
                return 0xFF0D9488; // Teal Cyan
            case STYLE_STRIKE:
                return 0xFFE11D48; // Rose Red
            case STYLE_UNDERLINE:
                return 0xFF0284C7; // Sky Blue
            case STYLE_SPOILER:
                return 0xFF9333EA; // Purple
            case STYLE_QUOTE:
                return 0xFF059669; // Emerald
            case STYLE_REGULAR:
            default:
                return 0xFF475569; // Crisp Slate
        }
    }

    public static String getStyleBadgeSymbol(int style) {
        switch (style) {
            case STYLE_BOLD:
                return "B";
            case STYLE_ITALIC:
                return "I";
            case STYLE_MONO:
                return "</>";
            case STYLE_STRIKE:
                return "S̶";
            case STYLE_UNDERLINE:
                return "U̲";
            case STYLE_SPOILER:
                return "✦";
            case STYLE_QUOTE:
                return "❝";
            case STYLE_REGULAR:
            default:
                return "Aa";
        }
    }

    /**
     * Applies the user's selected default text style to the outgoing message caption/text
     * right before Telegram extracts entities, or directly onto the outgoing entities list.
     */
    public static void applySelectedStyleToOutgoingEditText(EditTextCaption editText) {
        if (editText == null) return;
        int style = getSelectedStyle();
        if (style == STYLE_REGULAR) return;

        try {
            Editable editable = editText.getText();
            if (editable == null || editable.length() == 0) return;
            String raw = editable.toString();
            if (TextUtils.isEmpty(raw.trim())) return;

            int len = editable.length();
            TextStyleSpan[] existingStyles = editable.getSpans(0, len, TextStyleSpan.class);
            if (existingStyles != null && existingStyles.length > 0) {
                return;
            }
            TypefaceSpan[] existingTypeface = editable.getSpans(0, len, TypefaceSpan.class);
            if (existingTypeface != null && existingTypeface.length > 0) {
                return;
            }

            switch (style) {
                case STYLE_BOLD:
                    editable.setSpan(new TypefaceSpan(AndroidUtilities.bold()), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case STYLE_ITALIC:
                    editable.setSpan(new TypefaceSpan(AndroidUtilities.getTypeface("fonts/ritalic.ttf")), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                case STYLE_MONO: {
                    TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                    run.flags |= TextStyleSpan.FLAG_STYLE_MONO;
                    editable.setSpan(new TextStyleSpan(run), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
                case STYLE_STRIKE: {
                    TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                    run.flags |= TextStyleSpan.FLAG_STYLE_STRIKE;
                    editable.setSpan(new TextStyleSpan(run), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
                case STYLE_UNDERLINE: {
                    TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                    run.flags |= TextStyleSpan.FLAG_STYLE_UNDERLINE;
                    editable.setSpan(new TextStyleSpan(run), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
                case STYLE_SPOILER: {
                    TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
                    run.flags |= TextStyleSpan.FLAG_STYLE_SPOILER;
                    editable.setSpan(new TextStyleSpan(run), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    break;
                }
                case STYLE_QUOTE: {
                    QuoteSpan.putQuoteToEditable(editable, 0, len, false);
                    break;
                }
            }
        } catch (Throwable e) {
            FileLog.e(e);
        }
    }

    /**
     * Fallback entity-level application: if a plain text message is sent via SendMessagesHelper,
     * ensures the chosen default style entity is attached when no other formatting entities exist.
     */
    public static ArrayList<TLRPC.MessageEntity> ensureOutgoingEntitiesStyled(String message, ArrayList<TLRPC.MessageEntity> entities) {
        int style = getSelectedStyle();
        if (style == STYLE_REGULAR || TextUtils.isEmpty(message)) {
            return entities;
        }
        if (message.startsWith("/")) {
            return entities;
        }
        if (entities != null) {
            for (int i = 0; i < entities.size(); i++) {
                TLRPC.MessageEntity e = entities.get(i);
                if (e instanceof TLRPC.TL_messageEntityBold ||
                    e instanceof TLRPC.TL_messageEntityItalic ||
                    e instanceof TLRPC.TL_messageEntityCode ||
                    e instanceof TLRPC.TL_messageEntityPre ||
                    e instanceof TLRPC.TL_messageEntityStrike ||
                    e instanceof TLRPC.TL_messageEntityUnderline ||
                    e instanceof TLRPC.TL_messageEntitySpoiler ||
                    e instanceof TLRPC.TL_messageEntityBlockquote) {
                    return entities;
                }
            }
        }

        TLRPC.MessageEntity entity = createEntityForStyle(style, 0, message.length());
        if (entity != null) {
            if (entities == null) {
                entities = new ArrayList<>();
            }
            entities.add(entity);
        }
        return entities;
    }

    public static TLRPC.MessageEntity createEntityForStyle(int style, int offset, int length) {
        if (length <= 0) return null;
        TLRPC.MessageEntity entity = null;
        switch (style) {
            case STYLE_BOLD:
                entity = new TLRPC.TL_messageEntityBold();
                break;
            case STYLE_ITALIC:
                entity = new TLRPC.TL_messageEntityItalic();
                break;
            case STYLE_MONO:
                entity = new TLRPC.TL_messageEntityCode();
                break;
            case STYLE_STRIKE:
                entity = new TLRPC.TL_messageEntityStrike();
                break;
            case STYLE_UNDERLINE:
                entity = new TLRPC.TL_messageEntityUnderline();
                break;
            case STYLE_SPOILER:
                entity = new TLRPC.TL_messageEntitySpoiler();
                break;
            case STYLE_QUOTE:
                entity = new TLRPC.TL_messageEntityBlockquote();
                break;
            default:
                return null;
        }
        entity.offset = offset;
        entity.length = length;
        return entity;
    }

    /**
     * Shows the NayaGram "Text style" modal dialog with a compact scrollable viewport
     * and crisp, high-contrast small labels.
     */
    public static void showDialog(Context context) {
        showDialog(context, null);
    }

    public static void showDialog(Context context, Runnable onStyleChanged) {
        if (context == null) return;

        final boolean isDark = Theme.isCurrentThemeDark();
        final int currentStyle = getSelectedStyle();
        final Dialog[] dialogRef = new Dialog[1];

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(16), AndroidUtilities.dp(18), AndroidUtilities.dp(8));

        // Top Header Row with Icon Badge + Title + Active Pill
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView headerIcon = new TextView(context);
        headerIcon.setText("Aa");
        headerIcon.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        headerIcon.setTypeface(AndroidUtilities.bold());
        headerIcon.setTextColor(Color.WHITE);
        headerIcon.setGravity(Gravity.CENTER);
        GradientDrawable headerIconBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0EA5E9, 0xFF6366F1}
        );
        headerIconBg.setCornerRadius(AndroidUtilities.dp(10));
        headerIcon.setBackground(headerIconBg);
        headerRow.addView(headerIcon, LayoutHelper.createLinear(34, 34, 0, 0, 10, 0));

        TextView titleView = new TextView(context);
        titleView.setText("Text style");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 20);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        headerRow.addView(titleView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        TextView activePill = new TextView(context);
        activePill.setText(getStyleName(currentStyle));
        activePill.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        activePill.setTypeface(AndroidUtilities.bold());
        int activeBadgeColor = getStyleBadgeColor(currentStyle);
        activePill.setTextColor(isDark ? Color.WHITE : activeBadgeColor);
        activePill.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        GradientDrawable activePillBg = new GradientDrawable();
        activePillBg.setCornerRadius(AndroidUtilities.dp(12));
        activePillBg.setColor(isDark ? (activeBadgeColor & 0x45FFFFFF) : (activeBadgeColor & 0x1AFFFFFF));
        activePillBg.setStroke(AndroidUtilities.dp(1), isDark ? (activeBadgeColor & 0x99FFFFFF) : (activeBadgeColor & 0x55FFFFFF));
        activePill.setBackground(activePillBg);
        headerRow.addView(activePill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        root.addView(headerRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView subTitle = new TextView(context);
        subTitle.setText("Select default style for outgoing messages (scroll for more):");
        subTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        subTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        subTitle.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        root.addView(subTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Compact bounded ScrollView so users can scroll comfortably within a neat compact area
        ScrollView scrollView = new ScrollView(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int maxScrollHeight = Math.min(
                        AndroidUtilities.dp(310),
                        (int) (AndroidUtilities.displaySize.y * 0.42f)
                );
                int cappedHeightSpec = MeasureSpec.makeMeasureSpec(maxScrollHeight, MeasureSpec.AT_MOST);
                super.onMeasure(widthMeasureSpec, cappedHeightSpec);
            }
        };
        scrollView.setVerticalScrollBarEnabled(true);
        scrollView.setScrollbarFadingEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_IF_CONTENT_SCROLLS);

        LinearLayout optionsContainer = new LinearLayout(context);
        optionsContainer.setOrientation(LinearLayout.VERTICAL);
        optionsContainer.setPadding(0, AndroidUtilities.dp(2), AndroidUtilities.dp(4), AndroidUtilities.dp(4));
        scrollView.addView(optionsContainer, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(scrollView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        final int[] styles = new int[]{
            STYLE_REGULAR,
            STYLE_BOLD,
            STYLE_ITALIC,
            STYLE_MONO,
            STYLE_STRIKE,
            STYLE_UNDERLINE,
            STYLE_SPOILER,
            STYLE_QUOTE
        };

        for (int i = 0; i < styles.length; i++) {
            final int styleValue = styles[i];
            final boolean isSelected = (styleValue == currentStyle);
            final int badgeColor = getStyleBadgeColor(styleValue);

            LinearLayout row = new LinearLayout(context);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(9), AndroidUtilities.dp(12), AndroidUtilities.dp(9));

            GradientDrawable rowBg = new GradientDrawable();
            rowBg.setCornerRadius(AndroidUtilities.dp(12));
            if (isSelected) {
                rowBg.setColor(isDark ? 0xFF1E293B : 0xFFEFF6FF);
                rowBg.setStroke(AndroidUtilities.dp(1.8f), isDark ? 0xFF38BDF8 : 0xFF2563EB);
            } else {
                rowBg.setColor(isDark ? 0x18FFFFFF : 0xFFF8FAFC);
                rowBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x28FFFFFF : 0xFFCBD5E1);
            }
            row.setBackground(rowBg);

            // Colored Left Symbol Badge
            TextView symbolBadge = new TextView(context);
            symbolBadge.setText(getStyleBadgeSymbol(styleValue));
            symbolBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, styleValue == STYLE_MONO ? 11.5f : 14f);
            symbolBadge.setTypeface(styleValue == STYLE_MONO ? Typeface.MONOSPACE : AndroidUtilities.bold());
            symbolBadge.setTextColor(Color.WHITE);
            symbolBadge.setGravity(Gravity.CENTER);
            GradientDrawable badgeDrawable = new GradientDrawable();
            badgeDrawable.setCornerRadius(AndroidUtilities.dp(10));
            badgeDrawable.setColor(badgeColor);
            symbolBadge.setBackground(badgeDrawable);
            row.addView(symbolBadge, LayoutHelper.createLinear(34, 34, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

            // Middle Column: Styled Title + Crisp High-Contrast Subtitle
            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);

            TextView label = new TextView(context);
            label.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
            label.setTextColor(isSelected
                    ? (isDark ? 0xFF38BDF8 : 0xFF1D4ED8)
                    : (isDark ? 0xFFF8FAFC : 0xFF0F172A));
            label.setText(buildStyledPreviewLabel(styleValue, getStyleName(styleValue), isDark));
            textCol.addView(label, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            TextView descView = new TextView(context);
            descView.setText(getStyleDescription(styleValue));
            descView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
            descView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            descView.setTextColor(isSelected
                    ? (isDark ? 0xFFBAE6FD : 0xFF1E40AF)
                    : (isDark ? 0xFFCBD5E1 : 0xFF334155));
            textCol.addView(descView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

            row.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            // Right Checkmark Indicator
            TextView checkBadge = new TextView(context);
            checkBadge.setGravity(Gravity.CENTER);
            checkBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            checkBadge.setTypeface(AndroidUtilities.bold());
            GradientDrawable checkBg = new GradientDrawable();
            checkBg.setShape(GradientDrawable.OVAL);
            if (isSelected) {
                checkBadge.setText("✓");
                checkBadge.setTextColor(Color.WHITE);
                checkBg.setColor(0xFF10B981); // Vivid Emerald Green
            } else {
                checkBadge.setText("");
                checkBg.setColor(Color.TRANSPARENT);
                checkBg.setStroke(AndroidUtilities.dp(1.5f), isDark ? 0x44FFFFFF : 0xFF94A3B8);
            }
            checkBadge.setBackground(checkBg);
            row.addView(checkBadge, LayoutHelper.createLinear(24, 24, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

            row.setOnClickListener(v -> {
                setSelectedStyle(styleValue);
                if (dialogRef[0] != null) {
                    try {
                        dialogRef[0].dismiss();
                    } catch (Throwable ignored) {}
                }
                if (onStyleChanged != null) {
                    try {
                        onStyleChanged.run();
                    } catch (Throwable ignored) {}
                }
                Toast.makeText(context, "Text style: " + getStyleName(styleValue), Toast.LENGTH_SHORT).show();
            });

            LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT);
            lp.bottomMargin = AndroidUtilities.dp(6);
            optionsContainer.addView(row, lp);
        }

        // Desktop & Web Keyboard Shortcuts Reference Card inside the ScrollView
        optionsContainer.addView(
                createShortcutsCard(context, isDark),
                LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 2)
        );

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setView(root);
        builder.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), (d, w) -> d.dismiss());
        Dialog dialog = builder.create();
        dialogRef[0] = dialog;
        dialog.show();
    }

    private static View createShortcutsCard(Context context, boolean isDark) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(12);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(12));
        bg.setColor(isDark ? 0xFF1E293B : 0xFFF1F5F9);
        bg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);
        card.setBackground(bg);

        TextView header = new TextView(context);
        header.setText("⌨️ Desktop & Web Keyboard Shortcuts");
        header.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        header.setTypeface(AndroidUtilities.bold());
        header.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        card.addView(header, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView subtitle = new TextView(context);
        subtitle.setText("Select text in the message field and press:");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        subtitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        subtitle.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        card.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 8));

        String[][] shortcuts = new String[][]{
            {"Bold", "Ctrl + B"},
            {"Italic", "Ctrl + I"},
            {"Underline", "Ctrl + U"},
            {"Strikethrough", "Ctrl + Shift + X"},
            {"Monospace", "Ctrl + Shift + M"},
            {"Spoiler", "Ctrl + Shift + P"},
            {"Quote Block", "Ctrl + Shift + ."},
            {"Add Link", "Ctrl + K"},
            {"Clear Formatting", "Ctrl + Shift + N"}
        };

        for (int i = 0; i < shortcuts.length; i++) {
            LinearLayout sRow = new LinearLayout(context);
            sRow.setOrientation(LinearLayout.HORIZONTAL);
            sRow.setGravity(Gravity.CENTER_VERTICAL);
            sRow.setPadding(0, AndroidUtilities.dp(3), 0, AndroidUtilities.dp(3));

            TextView nameView = new TextView(context);
            nameView.setText(shortcuts[i][0]);
            nameView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
            nameView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
            nameView.setTextColor(isDark ? 0xFFF1F5F9 : 0xFF0F172A);
            sRow.addView(nameView, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

            TextView keyCap = new TextView(context);
            keyCap.setText(shortcuts[i][1]);
            keyCap.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
            keyCap.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
            keyCap.setTextColor(isDark ? 0xFF38BDF8 : 0xFF1D4ED8);
            keyCap.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
            GradientDrawable keyBg = new GradientDrawable();
            keyBg.setCornerRadius(AndroidUtilities.dp(6));
            keyBg.setColor(isDark ? 0xFF0F172A : 0xFFFFFFFF);
            keyBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF475569 : 0xFF94A3B8);
            keyCap.setBackground(keyBg);
            sRow.addView(keyCap, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

            card.addView(sRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }

        return card;
    }

    private static CharSequence buildStyledPreviewLabel(int style, String text, boolean isDark) {
        SpannableStringBuilder ssb = new SpannableStringBuilder(text);
        int len = ssb.length();
        switch (style) {
            case STYLE_BOLD:
                ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case STYLE_ITALIC:
                ssb.setSpan(new StyleSpan(Typeface.ITALIC), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case STYLE_MONO:
                ssb.setSpan(new android.text.style.TypefaceSpan("monospace"), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case STYLE_STRIKE:
                ssb.setSpan(new StrikethroughSpan(), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case STYLE_UNDERLINE:
                ssb.setSpan(new UnderlineSpan(), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            case STYLE_SPOILER: {
                ssb = new SpannableStringBuilder(text + "  •••");
                ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            }
            case STYLE_QUOTE: {
                ssb = new SpannableStringBuilder("❝ " + text);
                ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, ssb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
            }
            case STYLE_REGULAR:
            default:
                ssb.setSpan(new StyleSpan(Typeface.BOLD), 0, len, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                break;
        }
        return ssb;
    }
}
