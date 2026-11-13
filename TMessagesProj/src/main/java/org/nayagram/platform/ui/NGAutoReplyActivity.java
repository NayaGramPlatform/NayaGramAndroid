package org.nayagram.platform.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.StrikethroughSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AutoReplyManager;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.QuoteSpan;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.TypefaceSpan;

/**
 * NGAutoReplyActivity - Clean, 100% English Auto-Answer configuration screen.
 * Features:
 * - Vivid Emerald/Cyan ON & Crisp Slate OFF switches with active status pill
 * - Multi-color formatting toolbar (Bold, Italic, Mono, Spoiler, Quote, Strike, Underline, Link, Default Style, Clear)
 * - Live rich-styled EditText composer with auto-save indicator
 * - High-contrast, crisp readable descriptions and helper notes in both Light & Dark themes
 * - Well-positioned Reset Sent List action button at the bottom
 */
public class NGAutoReplyActivity extends BaseFragment {

    private Switch autoAnswerSwitch;
    private TextView statusBadgeView;
    private Switch privateOnlySwitch;
    private TextView privateStatusPill;
    private EditTextBoldCursor messageEditText;
    private TextView charCountView;
    private final AutoReplyManager manager = AutoReplyManager.getInstance();

    /**
     * Custom ResourcesProvider for Switch so Track & Thumb colors resolve to vivid,
     * high-contrast ARGB colors via Theme.getColor(key, resourcesProvider).
     */
    private static Theme.ResourcesProvider createVividSwitchProvider(final int checkedTrackColor, final boolean isDark) {
        return new Theme.ResourcesProvider() {
            @Override
            public int getColor(int key) {
                if (key == Theme.key_switchTrackChecked) {
                    return checkedTrackColor;
                }
                if (key == Theme.key_switchTrack) {
                    return isDark ? 0xFF475569 : 0xFF94A3B8;
                }
                if (key == Theme.key_windowBackgroundWhite) {
                    return 0xFFFFFFFF;
                }
                return Theme.getColor(key);
            }
        };
    }

    @Override
    public View createView(Context context) {
        final boolean isDark = Theme.isCurrentThemeDark();

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Auto answer");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    saveCurrentText();
                    finishFragment();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(isDark ? 0xFF0F172A : 0xFFF1F5F9);
        fragmentView = root;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(32));
        scrollView.addView(content, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // =========================================================================
        // 1. MASTER AUTO-ANSWER TOGGLE CARD (WITH VIVID STATUS BADGE & SWITCH)
        // =========================================================================
        LinearLayout masterCard = new LinearLayout(context);
        masterCard.setOrientation(LinearLayout.VERTICAL);
        masterCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(15), AndroidUtilities.dp(16), AndroidUtilities.dp(15));
        GradientDrawable masterCardBg = new GradientDrawable();
        masterCardBg.setCornerRadius(AndroidUtilities.dp(16));
        masterCardBg.setColor(isDark ? 0xFF1E293B : 0xFFFFFFFF);
        masterCardBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);
        masterCard.setBackground(masterCardBg);

        LinearLayout toggleRow = new LinearLayout(context);
        toggleRow.setOrientation(LinearLayout.HORIZONTAL);
        toggleRow.setGravity(Gravity.CENTER_VERTICAL);

        // Left Robot Icon Badge
        FrameLayout iconBadge = new FrameLayout(context);
        GradientDrawable iconBadgeBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF10B981, 0xFF0284C7}
        );
        iconBadgeBg.setCornerRadius(AndroidUtilities.dp(12));
        iconBadge.setBackground(iconBadgeBg);

        ImageView robotIcon = new ImageView(context);
        robotIcon.setImageResource(R.drawable.ic_naya_robot);
        robotIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        iconBadge.addView(robotIcon, LayoutHelper.createFrame(22, 22, Gravity.CENTER));
        toggleRow.addView(iconBadge, LayoutHelper.createLinear(42, 42, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        // Middle Title + Status Pill
        LinearLayout titleCol = new LinearLayout(context);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        LinearLayout titleBadgeRow = new LinearLayout(context);
        titleBadgeRow.setOrientation(LinearLayout.HORIZONTAL);
        titleBadgeRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView toggleTitle = new TextView(context);
        toggleTitle.setText("Auto answer");
        toggleTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        toggleTitle.setTypeface(AndroidUtilities.bold());
        toggleTitle.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        titleBadgeRow.addView(toggleTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        statusBadgeView = new TextView(context);
        statusBadgeView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        statusBadgeView.setTypeface(AndroidUtilities.bold());
        statusBadgeView.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(2));
        titleBadgeRow.addView(statusBadgeView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        titleCol.addView(titleBadgeRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView toggleSub = new TextView(context);
        toggleSub.setText("Automatically sends your reply once when someone messages you");
        toggleSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        toggleSub.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        toggleSub.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        titleCol.addView(toggleSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));

        toggleRow.addView(titleCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 10, 0));

        // Right Vivid Switch (Emerald Green #10B981 when ON, Slate when OFF)
        autoAnswerSwitch = new Switch(context, createVividSwitchProvider(0xFF10B981, isDark));
        autoAnswerSwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        autoAnswerSwitch.setDrawIconType(1);
        boolean initialEnabled = manager.isAutoReplyEnabled();
        autoAnswerSwitch.setChecked(initialEnabled, false);
        updateMasterStatusBadge(initialEnabled, isDark);
        toggleRow.addView(autoAnswerSwitch, LayoutHelper.createLinear(46, 28, Gravity.CENTER_VERTICAL));

        toggleRow.setOnClickListener(v -> {
            boolean next = !autoAnswerSwitch.isChecked();
            autoAnswerSwitch.setChecked(next, true);
            manager.setAutoReplyEnabled(next);
            updateMasterStatusBadge(next, isDark);
        });

        masterCard.addView(toggleRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        content.addView(masterCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // =========================================================================
        // 2. MESSAGE COMPOSER & STYLING TOOLBAR CARD
        // =========================================================================
        LinearLayout editorCard = new LinearLayout(context);
        editorCard.setOrientation(LinearLayout.VERTICAL);
        editorCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(15), AndroidUtilities.dp(16), AndroidUtilities.dp(15));
        GradientDrawable editorCardBg = new GradientDrawable();
        editorCardBg.setCornerRadius(AndroidUtilities.dp(16));
        editorCardBg.setColor(isDark ? 0xFF1E293B : 0xFFFFFFFF);
        editorCardBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);
        editorCard.setBackground(editorCardBg);

        // Header Row: "MESSAGE TEXT" + Character Counter
        LinearLayout editorHeaderRow = new LinearLayout(context);
        editorHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        editorHeaderRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView msgHeader = new TextView(context);
        msgHeader.setText("AUTO-ANSWER MESSAGE");
        msgHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        msgHeader.setTypeface(AndroidUtilities.bold());
        msgHeader.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        editorHeaderRow.addView(msgHeader, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        charCountView = new TextView(context);
        charCountView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        charCountView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        charCountView.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        editorHeaderRow.addView(charCountView, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        editorCard.addView(editorHeaderRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView styleHint = new TextView(context);
        styleHint.setText("Highlight text below and tap any style button to format:");
        styleHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        styleHint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        styleHint.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        editorCard.addView(styleHint, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Rich Text Styling Bar (Color-coded chips)
        HorizontalScrollView styleScroll = new HorizontalScrollView(context);
        styleScroll.setHorizontalScrollBarEnabled(false);

        LinearLayout styleRow = new LinearLayout(context);
        styleRow.setOrientation(LinearLayout.HORIZONTAL);
        styleRow.setGravity(Gravity.CENTER_VERTICAL);
        styleScroll.addView(styleRow, LayoutHelper.createFrame(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        styleRow.addView(createStyleChip(context, "B  Bold", Typeface.BOLD, 0xFF2563EB, isDark, v -> applySelectionStyle(STYLE_ACTION_BOLD)));
        styleRow.addView(createStyleChip(context, "I  Italic", Typeface.ITALIC, 0xFF7C3AED, isDark, v -> applySelectionStyle(STYLE_ACTION_ITALIC)));
        styleRow.addView(createStyleChip(context, "</> Mono", Typeface.NORMAL, 0xFF0D9488, isDark, v -> applySelectionStyle(STYLE_ACTION_MONO)));
        styleRow.addView(createStyleChip(context, "✦ Spoiler", Typeface.BOLD, 0xFF9333EA, isDark, v -> applySelectionStyle(STYLE_ACTION_SPOILER)));
        styleRow.addView(createStyleChip(context, "❝ Quote", Typeface.BOLD, 0xFF059669, isDark, v -> applySelectionStyle(STYLE_ACTION_QUOTE)));
        styleRow.addView(createStyleChip(context, "S̶ Strike", Typeface.NORMAL, 0xFFE11D48, isDark, v -> applySelectionStyle(STYLE_ACTION_STRIKE)));
        styleRow.addView(createStyleChip(context, "U̲ Under", Typeface.NORMAL, 0xFF0284C7, isDark, v -> applySelectionStyle(STYLE_ACTION_UNDERLINE)));
        styleRow.addView(createStyleChip(context, "🔗 Link", Typeface.BOLD, 0xFF0EA5E9, isDark, v -> showCreateLinkDialog(context)));
        styleRow.addView(createStyleChip(context, "Aa Text Style", Typeface.BOLD, 0xFF6366F1, isDark, v -> NGTextStyleHelper.showDialog(context)));
        styleRow.addView(createStyleChip(context, "✕ Clear", Typeface.BOLD, 0xFF64748B, isDark, v -> applySelectionStyle(STYLE_ACTION_CLEAR)));

        editorCard.addView(styleScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Message Edit Field Box
        FrameLayout inputBox = new FrameLayout(context);
        GradientDrawable inputBoxBg = new GradientDrawable();
        inputBoxBg.setCornerRadius(AndroidUtilities.dp(12));
        inputBoxBg.setColor(isDark ? 0xFF0F172A : 0xFFF8FAFC);
        inputBoxBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFF94A3B8);
        inputBox.setBackground(inputBoxBg);

        messageEditText = new EditTextBoldCursor(context);
        messageEditText.setBackground(null);
        messageEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        messageEditText.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        messageEditText.setHintTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        messageEditText.setHint("Enter your auto-answer message here...");
        messageEditText.setMinLines(4);
        messageEditText.setMaxLines(8);
        messageEditText.setGravity(Gravity.TOP | Gravity.START);
        messageEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        messageEditText.setImeOptions(EditorInfo.IME_FLAG_NO_EXTRACT_UI);
        messageEditText.setCursorColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        messageEditText.setCursorSize(AndroidUtilities.dp(20));
        messageEditText.setCursorWidth(1.5f);
        int pad = AndroidUtilities.dp(12);
        messageEditText.setPadding(pad, pad, pad, pad);

        String savedMsg = manager.getCustomReplyMessage();
        messageEditText.setText(savedMsg);
        messageEditText.setSelection(messageEditText.length());
        updateCharCounter(savedMsg != null ? savedMsg.length() : 0);

        messageEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String val = s.toString();
                manager.setCustomReplyMessage(val);
                updateCharCounter(val.length());
            }
        });

        inputBox.addView(messageEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        editorCard.addView(inputBox, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Crisp Auto-Save & Markdown Note below the input box
        LinearLayout saveNoteRow = new LinearLayout(context);
        saveNoteRow.setOrientation(LinearLayout.HORIZONTAL);
        saveNoteRow.setGravity(Gravity.CENTER_VERTICAL);
        saveNoteRow.setPadding(AndroidUtilities.dp(2), AndroidUtilities.dp(10), AndroidUtilities.dp(2), 0);

        TextView saveBadge = new TextView(context);
        saveBadge.setText("✓ Auto-saved");
        saveBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        saveBadge.setTypeface(AndroidUtilities.bold());
        saveBadge.setTextColor(isDark ? 0xFF34D399 : 0xFF059669);
        saveNoteRow.addView(saveBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        TextView markdownHint = new TextView(context);
        markdownHint.setText("• Supports Bold, Italic, Mono, Spoiler, Quote & Links");
        markdownHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        markdownHint.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        markdownHint.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        saveNoteRow.addView(markdownHint, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));

        editorCard.addView(saveNoteRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        content.addView(editorCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // =========================================================================
        // 3. DELIVERY OPTIONS & RESET ACTION CARD
        // =========================================================================
        LinearLayout optionsCard = new LinearLayout(context);
        optionsCard.setOrientation(LinearLayout.VERTICAL);
        optionsCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        GradientDrawable optionsCardBg = new GradientDrawable();
        optionsCardBg.setCornerRadius(AndroidUtilities.dp(16));
        optionsCardBg.setColor(isDark ? 0xFF1E293B : 0xFFFFFFFF);
        optionsCardBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);
        optionsCard.setBackground(optionsCardBg);

        TextView optionsHeader = new TextView(context);
        optionsHeader.setText("DELIVERY SETTINGS");
        optionsHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        optionsHeader.setTypeface(AndroidUtilities.bold());
        optionsHeader.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        optionsCard.addView(optionsHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Private Chats Only Row
        LinearLayout privateRow = new LinearLayout(context);
        privateRow.setOrientation(LinearLayout.HORIZONTAL);
        privateRow.setGravity(Gravity.CENTER_VERTICAL);
        privateRow.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(10));

        LinearLayout privateTextCol = new LinearLayout(context);
        privateTextCol.setOrientation(LinearLayout.VERTICAL);

        LinearLayout privateTitleRow = new LinearLayout(context);
        privateTitleRow.setOrientation(LinearLayout.HORIZONTAL);
        privateTitleRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView privateTitle = new TextView(context);
        privateTitle.setText("Private chats only");
        privateTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        privateTitle.setTypeface(AndroidUtilities.bold());
        privateTitle.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        privateTitleRow.addView(privateTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));

        privateStatusPill = new TextView(context);
        privateStatusPill.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11f);
        privateStatusPill.setTypeface(AndroidUtilities.bold());
        privateStatusPill.setPadding(AndroidUtilities.dp(7), AndroidUtilities.dp(2), AndroidUtilities.dp(7), AndroidUtilities.dp(2));
        privateTitleRow.addView(privateStatusPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        privateTextCol.addView(privateTitleRow, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView privateSub = new TextView(context);
        privateSub.setText("Only reply in 1-on-1 direct messages (never in groups or channels)");
        privateSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        privateSub.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        privateSub.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        privateTextCol.addView(privateSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 3, 0, 0));

        privateRow.addView(privateTextCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 10, 0));

        privateOnlySwitch = new Switch(context, createVividSwitchProvider(0xFF0284C7, isDark));
        privateOnlySwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        privateOnlySwitch.setDrawIconType(1);
        boolean initPrivateOnly = manager.isPrivateOnly();
        privateOnlySwitch.setChecked(initPrivateOnly, false);
        updatePrivateStatusBadge(initPrivateOnly, isDark);
        privateRow.addView(privateOnlySwitch, LayoutHelper.createLinear(46, 28, Gravity.CENTER_VERTICAL));

        privateRow.setOnClickListener(v -> {
            boolean next = !privateOnlySwitch.isChecked();
            privateOnlySwitch.setChecked(next, true);
            manager.setPrivateOnly(next);
            updatePrivateStatusBadge(next, isDark);
        });
        optionsCard.addView(privateRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Divider
        View divider = new View(context);
        divider.setBackgroundColor(isDark ? 0xFF334155 : 0xFFE2E8F0);
        optionsCard.addView(divider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 4, 0, 12));

        // Reset Sent List Action Row (Positioned cleanly as an interactive pill button)
        LinearLayout resetRow = new LinearLayout(context);
        resetRow.setOrientation(LinearLayout.HORIZONTAL);
        resetRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout resetInfoCol = new LinearLayout(context);
        resetInfoCol.setOrientation(LinearLayout.VERTICAL);

        TextView resetTitle = new TextView(context);
        resetTitle.setText("Reply history memory");
        resetTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        resetTitle.setTypeface(AndroidUtilities.bold());
        resetTitle.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        resetInfoCol.addView(resetTitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));

        TextView resetSub = new TextView(context);
        resetSub.setText("Clear the list of users who already received your auto-answer");
        resetSub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        resetSub.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        resetSub.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        resetInfoCol.addView(resetSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        resetRow.addView(resetInfoCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 10, 0));

        TextView resetBtn = new TextView(context);
        resetBtn.setText("↻ Reset list");
        resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        resetBtn.setTypeface(AndroidUtilities.bold());
        resetBtn.setTextColor(Color.WHITE);
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
        GradientDrawable resetBtnBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0284C7, 0xFF2563EB}
        );
        resetBtnBg.setCornerRadius(AndroidUtilities.dp(10));
        resetBtn.setBackground(resetBtnBg);
        resetBtn.setOnClickListener(v -> {
            manager.clearRepliedPeers();
            Toast.makeText(context, "Sent list reset — Auto answer will reply again", Toast.LENGTH_SHORT).show();
        });
        resetRow.addView(resetBtn, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL));

        optionsCard.addView(resetRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        content.addView(optionsCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // =========================================================================
        // 4. HIGH-CONTRAST INFO FOOTER CARD (CRISP READABLE SMALL TEXT)
        // =========================================================================
        LinearLayout infoCard = new LinearLayout(context);
        infoCard.setOrientation(LinearLayout.VERTICAL);
        infoCard.setPadding(AndroidUtilities.dp(15), AndroidUtilities.dp(13), AndroidUtilities.dp(15), AndroidUtilities.dp(13));
        GradientDrawable infoCardBg = new GradientDrawable();
        infoCardBg.setCornerRadius(AndroidUtilities.dp(14));
        infoCardBg.setColor(isDark ? 0xFF172554 : 0xFFEFF6FF);
        infoCardBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF1D4ED8 : 0xFF93C5FD);
        infoCard.setBackground(infoCardBg);

        TextView infoTitle = new TextView(context);
        infoTitle.setText("ℹ️ How Auto answer works");
        infoTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13.5f);
        infoTitle.setTypeface(AndroidUtilities.bold());
        infoTitle.setTextColor(isDark ? 0xFF60A5FA : 0xFF1D4ED8);
        infoCard.addView(infoTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView footerInfo = new TextView(context);
        footerInfo.setText("When Auto answer is ON, NayaGram sends your message once to each person who writes to you. Tap \"Reset list\" above anytime to clear the memory and reply to them again.");
        footerInfo.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        footerInfo.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        footerInfo.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        footerInfo.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        infoCard.addView(footerInfo, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        content.addView(infoCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return fragmentView;
    }

    private void updateMasterStatusBadge(boolean enabled, boolean isDark) {
        if (statusBadgeView == null) return;
        statusBadgeView.setText(enabled ? "● ON" : "○ OFF");
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setCornerRadius(AndroidUtilities.dp(10));
        if (enabled) {
            statusBadgeView.setTextColor(isDark ? 0xFFECFDF5 : 0xFF065F46);
            pillBg.setColor(isDark ? 0xFF059669 : 0xFFD1FAE5);
            pillBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF34D399 : 0xFF10B981);
        } else {
            statusBadgeView.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF334155);
            pillBg.setColor(isDark ? 0xFF334155 : 0xFFE2E8F0);
            pillBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF475569 : 0xFF94A3B8);
        }
        statusBadgeView.setBackground(pillBg);
    }

    private void updatePrivateStatusBadge(boolean enabled, boolean isDark) {
        if (privateStatusPill == null) return;
        privateStatusPill.setText(enabled ? "ON" : "OFF");
        GradientDrawable pillBg = new GradientDrawable();
        pillBg.setCornerRadius(AndroidUtilities.dp(8));
        if (enabled) {
            privateStatusPill.setTextColor(isDark ? 0xFFE0F2FE : 0xFF075985);
            pillBg.setColor(isDark ? 0xFF0284C7 : 0xFFE0F2FE);
            pillBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF38BDF8 : 0xFF0284C7);
        } else {
            privateStatusPill.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF475569);
            pillBg.setColor(isDark ? 0xFF334155 : 0xFFE2E8F0);
            pillBg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF475569 : 0xFFCBD5E1);
        }
        privateStatusPill.setBackground(pillBg);
    }

    private void updateCharCounter(int length) {
        if (charCountView != null) {
            charCountView.setText(length + " chars");
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCurrentText();
    }

    private void saveCurrentText() {
        if (messageEditText != null && messageEditText.getText() != null) {
            manager.setCustomReplyMessage(messageEditText.getText().toString());
        }
    }

    private static final int STYLE_ACTION_BOLD = 1;
    private static final int STYLE_ACTION_ITALIC = 2;
    private static final int STYLE_ACTION_MONO = 3;
    private static final int STYLE_ACTION_SPOILER = 4;
    private static final int STYLE_ACTION_QUOTE = 5;
    private static final int STYLE_ACTION_STRIKE = 6;
    private static final int STYLE_ACTION_UNDERLINE = 7;
    private static final int STYLE_ACTION_CLEAR = 8;

    private View createStyleChip(Context context, String label, int typefaceStyle, int accentColor, boolean isDark, View.OnClickListener listener) {
        TextView chip = new TextView(context);
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        chip.setTypeface(label.contains("Mono") ? Typeface.MONOSPACE : Typeface.defaultFromStyle(typefaceStyle), typefaceStyle);
        chip.setTextColor(isDark ? Color.WHITE : accentColor);
        chip.setGravity(Gravity.CENTER);
        chip.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(7), AndroidUtilities.dp(12), AndroidUtilities.dp(7));

        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(10));
        bg.setColor(isDark ? (accentColor & 0x38FFFFFF) : (accentColor & 0x16FFFFFF));
        bg.setStroke(AndroidUtilities.dp(1.2f), isDark ? (accentColor & 0xAAFFFFFF) : (accentColor & 0x66FFFFFF));
        chip.setBackground(bg);
        chip.setOnClickListener(listener);

        LinearLayout.LayoutParams lp = LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT);
        lp.rightMargin = AndroidUtilities.dp(7);
        chip.setLayoutParams(lp);
        return chip;
    }

    private void applySelectionStyle(int action) {
        if (messageEditText == null || messageEditText.getText() == null) return;
        Editable editable = messageEditText.getText();
        int start = Math.max(0, messageEditText.getSelectionStart());
        int end = Math.max(0, messageEditText.getSelectionEnd());
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        if (start == end) {
            if (editable.length() == 0) {
                Toast.makeText(getParentActivity(), "Type your message first, then select text to style", Toast.LENGTH_SHORT).show();
                return;
            }
            start = 0;
            end = editable.length();
        }

        String selected = editable.subSequence(start, end).toString();
        String replacement = selected;

        switch (action) {
            case STYLE_ACTION_BOLD:
                replacement = wrapOrUnwrap(selected, "**", "**");
                break;
            case STYLE_ACTION_ITALIC:
                replacement = wrapOrUnwrap(selected, "_", "_");
                break;
            case STYLE_ACTION_MONO:
                replacement = wrapOrUnwrap(selected, "`", "`");
                break;
            case STYLE_ACTION_SPOILER:
                replacement = wrapOrUnwrap(selected, "||", "||");
                break;
            case STYLE_ACTION_STRIKE:
                replacement = wrapOrUnwrap(selected, "~~", "~~");
                break;
            case STYLE_ACTION_UNDERLINE:
                replacement = wrapOrUnwrap(selected, "__", "__");
                break;
            case STYLE_ACTION_QUOTE:
                if (selected.startsWith("> ")) {
                    replacement = selected.substring(2);
                } else {
                    replacement = "> " + selected.replace("\n", "\n> ");
                }
                break;
            case STYLE_ACTION_CLEAR:
                replacement = selected
                        .replace("**", "")
                        .replace("__", "")
                        .replace("~~", "")
                        .replace("||", "")
                        .replace("`", "")
                        .replaceFirst("^>\\s*", "");
                break;
        }

        editable.replace(start, end, replacement);
        int newEnd = Math.min(editable.length(), start + replacement.length());
        messageEditText.setSelection(start, newEnd);
        manager.setCustomReplyMessage(editable.toString());
    }

    private String wrapOrUnwrap(String text, String prefix, String suffix) {
        if (text.startsWith(prefix) && text.endsWith(suffix) && text.length() >= prefix.length() + suffix.length()) {
            return text.substring(prefix.length(), text.length() - suffix.length());
        }
        return prefix + text + suffix;
    }

    private void showCreateLinkDialog(Context context) {
        if (context == null || messageEditText == null) return;
        Editable editable = messageEditText.getText();
        final int start = Math.max(0, Math.min(messageEditText.getSelectionStart(), messageEditText.getSelectionEnd()));
        final int end = Math.max(0, Math.max(messageEditText.getSelectionStart(), messageEditText.getSelectionEnd()));
        String selectedText = (editable != null && end > start) ? editable.subSequence(start, end).toString() : "";

        boolean isDark = Theme.isCurrentThemeDark();
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(4));

        EditText textInput = new EditText(context);
        textInput.setHint("Text (e.g. Visit Website)");
        textInput.setText(selectedText);
        textInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        textInput.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        textInput.setHintTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        box.addView(textInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        EditText urlInput = new EditText(context);
        urlInput.setHint("URL (https://...)");
        urlInput.setText("https://");
        urlInput.setSelection(urlInput.getText().length());
        urlInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        urlInput.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        urlInput.setHintTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        urlInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        box.addView(urlInput, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Create Link");
        b.setView(box);
        b.setPositiveButton("Insert", (d, w) -> {
            String label = textInput.getText().toString().trim();
            String url = urlInput.getText().toString().trim();
            if (TextUtils.isEmpty(url) || "https://".equals(url)) return;
            if (TextUtils.isEmpty(label)) label = url;
            String linkMarkdown = "[" + label + "](" + url + ")";
            Editable ed = messageEditText.getText();
            if (ed != null) {
                int s = Math.min(start, ed.length());
                int e = Math.min(end, ed.length());
                ed.replace(s, e, linkMarkdown);
                manager.setCustomReplyMessage(ed.toString());
            }
        });
        b.setNegativeButton(LocaleController.getString("Cancel", R.string.Cancel), null);
        showDialog(b.create());
    }
}
