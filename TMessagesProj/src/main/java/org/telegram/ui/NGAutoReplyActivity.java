package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AutoReplyManager;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.R;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.Switch;
import org.telegram.ui.Components.URLSpanReplacement;

import java.util.ArrayList;

/**
 * NGAutoReplyActivity - Clean Novagram-style Auto Answer screen.
 * - Pure English Novagram aesthetic with vivid ON/OFF switches and status badges.
 * - Formatting & Link toolbar docked BELOW the text editor so Android's floating
 *   Cut/Copy/Paste selection popup never covers the formatting buttons.
 * - Rock-solid Hyperlink creation, visual blue+underline rendering, and instant JSON entity persistence.
 * - High-contrast, crisp small helper texts in both Light and Dark themes.
 */
public class NGAutoReplyActivity extends BaseFragment {

    private static final int MENU_DONE = 1;
    private static final int MENU_RESET_HISTORY = 2;

    private Switch autoAnswerSwitch;
    private TextView autoAnswerStatusBadge;
    private Switch privateOnlySwitch;
    private TextView privateOnlyStatusBadge;
    private EditTextCaption autoAnswerEditText;
    private boolean suppressTextWatcher = false;

    @Override
    public View createView(Context context) {
        final boolean isDark = Theme.isCurrentThemeDark();

        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Auto answer");
        actionBar.setCastShadows(false);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false);
        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));

        actionBar.createMenu().addItem(MENU_DONE, R.drawable.ic_ab_done);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    saveCurrentEditor();
                    finishFragment();
                } else if (id == MENU_DONE) {
                    saveCurrentEditor();
                    AndroidUtilities.hideKeyboard(autoAnswerEditText);
                    BulletinFactory.of(NGAutoReplyActivity.this)
                            .createSimpleBulletin(R.raw.done, "Auto answer saved")
                            .show();
                    finishFragment();
                } else if (id == MENU_RESET_HISTORY) {
                    AutoReplyManager.getInstance().clearAnsweredDialogs();
                    BulletinFactory.of(NGAutoReplyActivity.this)
                            .createSimpleBulletin(R.raw.chats_infotip, "Reply history reset")
                            .show();
                }
            }
        });

        FrameLayout root = new FrameLayout(context);
        root.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        fragmentView = root;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        root.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout column = new LinearLayout(context);
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(24));
        scrollView.addView(column, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Settings section card (Novagram style with vivid switches)
        LinearLayout settingsCard = createSectionCard(context, isDark);

        TextView sectionHeader = new TextView(context);
        sectionHeader.setText("AUTO ANSWER SETTINGS");
        sectionHeader.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        sectionHeader.setTypeface(AndroidUtilities.bold());
        sectionHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        sectionHeader.setLetterSpacing(0.05f);
        sectionHeader.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(4));
        settingsCard.addView(sectionHeader);

        AutoReplyManager mgr = AutoReplyManager.getInstance();

        boolean initialEnabled = mgr.isAutoReplyEnabled();
        autoAnswerSwitch = new Switch(context, createVividSwitchProvider(0xFF10B981, isDark));
        autoAnswerSwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        autoAnswerSwitch.setDrawIconType(1);
        autoAnswerSwitch.setChecked(initialEnabled, false);
        autoAnswerStatusBadge = createStatusPillBadge(context, initialEnabled, 0xFF10B981, isDark);

        settingsCard.addView(createSwitchRow(
                context,
                R.drawable.ic_naya_robot,
                0xFF10B981,
                "Enable Auto Answer",
                "Automatically reply to incoming messages (instant background & foreground)",
                autoAnswerStatusBadge,
                autoAnswerSwitch,
                isDark,
                v -> {
                    boolean next = !autoAnswerSwitch.isChecked();
                    autoAnswerSwitch.setChecked(next, true);
                    updateStatusPillBadge(autoAnswerStatusBadge, next, 0xFF10B981, isDark);
                    AutoReplyManager.getInstance().setAutoReplyEnabled(next);
                }
        ));

        settingsCard.addView(createHairlineDivider(context, isDark));

        boolean initialPrivate = mgr.isPrivateOnly();
        privateOnlySwitch = new Switch(context, createVividSwitchProvider(0xFF0284C7, isDark));
        privateOnlySwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        privateOnlySwitch.setDrawIconType(1);
        privateOnlySwitch.setChecked(initialPrivate, false);
        privateOnlyStatusBadge = createStatusPillBadge(context, initialPrivate, 0xFF0284C7, isDark);

        settingsCard.addView(createSwitchRow(
                context,
                R.drawable.msg_openprofile,
                0xFF0284C7,
                "Private chats only",
                "Skip groups, supergroups, and broadcast channels",
                privateOnlyStatusBadge,
                privateOnlySwitch,
                isDark,
                v -> {
                    boolean next = !privateOnlySwitch.isChecked();
                    privateOnlySwitch.setChecked(next, true);
                    updateStatusPillBadge(privateOnlyStatusBadge, next, 0xFF0284C7, isDark);
                    AutoReplyManager.getInstance().setPrivateOnly(next);
                }
        ));

        column.addView(settingsCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // 2. Reply Message Editor Card
        LinearLayout editorCard = createSectionCard(context, isDark);

        LinearLayout editorHeaderRow = new LinearLayout(context);
        editorHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        editorHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        editorHeaderRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(10));

        LinearLayout headerTitleCol = new LinearLayout(context);
        headerTitleCol.setOrientation(LinearLayout.VERTICAL);

        TextView editorHeader = new TextView(context);
        editorHeader.setText("REPLY MESSAGE");
        editorHeader.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        editorHeader.setTypeface(AndroidUtilities.bold());
        editorHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        editorHeader.setLetterSpacing(0.05f);
        headerTitleCol.addView(editorHeader);

        TextView editorSubHeader = new TextView(context);
        editorSubHeader.setText("Select text and use the formatting bar below the box");
        editorSubHeader.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
        editorSubHeader.setTypeface(AndroidUtilities.bold());
        editorSubHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12f);
        editorSubHeader.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        headerTitleCol.addView(editorSubHeader);

        editorHeaderRow.addView(headerTitleCol, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView resetBtn = new TextView(context);
        resetBtn.setText("↻ Reset sent list");
        resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        resetBtn.setTypeface(AndroidUtilities.bold());
        resetBtn.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        resetBtn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(isDark ? 0x260284C7 : 0x140284C7);
        resetBg.setCornerRadius(AndroidUtilities.dp(10));
        resetBg.setStroke(AndroidUtilities.dp(1), isDark ? 0x6638BDF8 : 0x4D0284C7);
        resetBtn.setBackground(resetBg);
        resetBtn.setOnClickListener(v -> {
            AutoReplyManager.getInstance().clearAnsweredDialogs();
            BulletinFactory.of(NGAutoReplyActivity.this)
                    .createSimpleBulletin(R.raw.chats_infotip, "Answered chats list cleared")
                    .show();
        });
        editorHeaderRow.addView(resetBtn);
        editorCard.addView(editorHeaderRow);

        View topDivider = new View(context);
        topDivider.setBackgroundColor(isDark ? 0xFF334155 : 0xFFE2E8F0);
        editorCard.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

        // Spacious EditTextCaption with extra top padding so floating selection popup never feels cramped
        FrameLayout inputBoxWrap = new FrameLayout(context);
        inputBoxWrap.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(isDark ? 0xFF0F172A : 0xFFF8FAFC);
        inputBg.setCornerRadius(AndroidUtilities.dp(14));
        inputBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);

        autoAnswerEditText = new EditTextCaption(context, null);
        autoAnswerEditText.setBackground(inputBg);
        autoAnswerEditText.setGravity(Gravity.TOP | Gravity.START);
        autoAnswerEditText.setMinLines(6);
        autoAnswerEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        autoAnswerEditText.setTextColor(isDark ? 0xFFF8FAFC : 0xFF0F172A);
        autoAnswerEditText.setHintTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        autoAnswerEditText.setLinkTextColor(0xFF0EA5E9);
        autoAnswerEditText.setHint("Write your auto-answer message here...\ne.g. Hello! I am currently offline and will get back to you soon.");
        autoAnswerEditText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        autoAnswerEditText.setSingleLine(false);
        autoAnswerEditText.setPadding(
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(14)
        );

        String savedText = mgr.getAutoAnswerText();
        String savedEntities = mgr.getAutoAnswerEntitiesJson();
        suppressTextWatcher = true;
        autoAnswerEditText.setText(savedText);
        if (savedEntities != null && !savedEntities.isEmpty()) {
            AutoReplyManager.restoreSpansFromJson(autoAnswerEditText.getText(), savedEntities);
        } else if (savedText != null && (savedText.contains("**") || savedText.contains("__") || savedText.contains("||") || savedText.contains("`") || savedText.contains("["))) {
            convertLegacyMarkdownToSpans(autoAnswerEditText.getText());
        }
        suppressTextWatcher = false;
        autoAnswerEditText.setSelection(autoAnswerEditText.getText() != null ? autoAnswerEditText.getText().length() : 0);

        autoAnswerEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {
                if (suppressTextWatcher) return;
                saveCurrentEditor();
            }
        });

        inputBoxWrap.addView(autoAnswerEditText, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        editorCard.addView(inputBoxWrap, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Formatting & Link Toolbar docked BELOW the text editor so Android's floating Cut/Copy/Paste popup NEVER covers it!
        TextView toolbarLabel = new TextView(context);
        toolbarLabel.setText("TEXT FORMATTING & HYPERLINK TOOLS");
        toolbarLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        toolbarLabel.setTypeface(AndroidUtilities.bold());
        toolbarLabel.setTextColor(isDark ? 0xFF94A3B8 : 0xFF475569);
        toolbarLabel.setLetterSpacing(0.04f);
        toolbarLabel.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(2), AndroidUtilities.dp(16), AndroidUtilities.dp(4));
        editorCard.addView(toolbarLabel);

        editorCard.addView(buildFormattingToolbar(context, isDark));

        View bottomDivider = new View(context);
        bottomDivider.setBackgroundColor(isDark ? 0xFF334155 : 0xFFE2E8F0);
        editorCard.addView(bottomDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 0, 4, 0, 0));

        // High-contrast footer row with Save Now button
        LinearLayout footerRow = new LinearLayout(context);
        footerRow.setOrientation(LinearLayout.HORIZONTAL);
        footerRow.setGravity(Gravity.CENTER_VERTICAL);
        footerRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        TextView footerHint = new TextView(context);
        footerHint.setText("✓ Auto-saves text, styles & clickable links automatically");
        footerHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        footerHint.setTypeface(AndroidUtilities.bold());
        footerHint.setTextColor(isDark ? 0xFF34D399 : 0xFF059669);
        footerRow.addView(footerHint, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView saveNowBtn = new TextView(context);
        saveNowBtn.setText("Save");
        saveNowBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        saveNowBtn.setTypeface(AndroidUtilities.bold());
        saveNowBtn.setTextColor(Color.WHITE);
        saveNowBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(7), AndroidUtilities.dp(16), AndroidUtilities.dp(7));
        GradientDrawable saveBg = new GradientDrawable();
        saveBg.setColor(0xFF0284C7);
        saveBg.setCornerRadius(AndroidUtilities.dp(10));
        saveNowBtn.setBackground(saveBg);
        saveNowBtn.setOnClickListener(v -> {
            saveCurrentEditor();
            AndroidUtilities.hideKeyboard(autoAnswerEditText);
            BulletinFactory.of(NGAutoReplyActivity.this)
                    .createSimpleBulletin(R.raw.done, "Auto answer message & links saved")
                    .show();
        });
        footerRow.addView(saveNowBtn);

        editorCard.addView(footerRow);
        column.addView(editorCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // Crisp high-contrast bottom info note
        TextView bottomInfoNote = new TextView(context);
        bottomInfoNote.setText("Tip: Auto answer works silently in the background while keeping your last-seen & offline status private. Toggling Auto Answer off or tapping 'Reset sent list' clears the answered history.");
        bottomInfoNote.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        bottomInfoNote.setTypeface(AndroidUtilities.bold());
        bottomInfoNote.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        bottomInfoNote.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        bottomInfoNote.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(12));
        column.addView(bottomInfoNote, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return root;
    }

    private Theme.ResourcesProvider createVividSwitchProvider(final int checkedTrackColor, final boolean isDark) {
        return new Theme.ResourcesProvider() {
            @Override
            public int getColor(int key) {
                if (key == Theme.key_switchTrackChecked) {
                    return checkedTrackColor;
                } else if (key == Theme.key_switchTrack) {
                    return isDark ? 0xFF475569 : 0xFFCBD5E1;
                } else if (key == Theme.key_windowBackgroundWhite) {
                    return Color.WHITE;
                }
                return Theme.getColor(key);
            }
        };
    }

    private TextView createStatusPillBadge(Context context, boolean checked, int activeColor, boolean isDark) {
        TextView badge = new TextView(context);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11.5f);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setGravity(Gravity.CENTER);
        badge.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
        updateStatusPillBadge(badge, checked, activeColor, isDark);
        return badge;
    }

    private void updateStatusPillBadge(TextView badge, boolean checked, int activeColor, boolean isDark) {
        if (badge == null) return;
        badge.setText(checked ? "● ON" : "○ OFF");
        badge.setTextColor(checked ? activeColor : (isDark ? 0xFFCBD5E1 : 0xFF475569));
        GradientDrawable gd = new GradientDrawable();
        gd.setCornerRadius(AndroidUtilities.dp(8));
        gd.setColor(checked ? ((activeColor & 0x00FFFFFF) | (isDark ? 0x33000000 : 0x1A000000)) : (isDark ? 0xFF334155 : 0xFFF1F5F9));
        gd.setStroke(AndroidUtilities.dp(1), checked ? activeColor : (isDark ? 0xFF475569 : 0xFFCBD5E1));
        badge.setBackground(gd);
    }

    private LinearLayout createSectionCard(Context context, boolean isDark) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF334155 : 0xFFE2E8F0);
        card.setBackground(bg);
        card.setClipToOutline(true);
        return card;
    }

    private View createSwitchRow(Context context, int iconRes, int iconColor, String title, String subtitle, TextView statusBadge, Switch sw, boolean isDark, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            row.setBackgroundResource(tv.resourceId);
        }
        row.setClickable(true);
        row.setOnClickListener(onClick);

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(AndroidUtilities.dp(10));
        iconBg.setColor(iconColor);
        iconWrap.setBackground(iconBg);

        ImageView iconView = new ImageView(context);
        iconView.setImageResource(iconRes);
        iconView.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        iconView.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        iconWrap.addView(iconView, LayoutHelper.createFrame(20, 20, Gravity.CENTER));
        row.addView(iconWrap, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTypeface(AndroidUtilities.bold());
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        texts.addView(titleView);

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subView = new TextView(context);
            subView.setText(subtitle);
            subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subView.setTypeface(AndroidUtilities.bold());
            subView.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
            subView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            texts.addView(subView);
        }

        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        if (statusBadge != null) {
            row.addView(statusBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 4, 0));
        }
        sw.setClickable(false);
        row.addView(sw, LayoutHelper.createLinear(37, 20, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));
        return row;
    }

    private View createHairlineDivider(Context context, boolean isDark) {
        LinearLayout holder = new LinearLayout(context);
        View line = new View(context);
        line.setBackgroundColor(isDark ? 0xFF334155 : 0xFFE2E8F0);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = AndroidUtilities.dp(64);
        holder.addView(line, lp);
        return holder;
    }

    private View buildFormattingToolbar(Context context, boolean isDark) {
        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hsv.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(4), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        hsv.addView(row);

        row.addView(createFormatChip(context, "B Bold", 0xFF1D4ED8, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedBold(); })));
        row.addView(createFormatChip(context, "I Italic", 0xFF6366F1, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedItalic(); })));
        row.addView(createFormatChip(context, "</> Mono", 0xFF0284C7, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedMono(); })));
        row.addView(createFormatChip(context, "✦ Spoiler", 0xFF8B5CF6, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedSpoiler(); })));
        row.addView(createFormatChip(context, "❝ Quote", 0xFF4F46E5, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedQuote(); })));
        row.addView(createFormatChip(context, "S̶ Strike", 0xFF0EA5E9, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedStrike(); })));
        row.addView(createFormatChip(context, "U̲ Under", 0xFF06B6D4, isDark, v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedUnderline(); })));
        row.addView(createFormatChip(context, "🔗 Link", 0xFF10B981, isDark, v -> {
            if (autoAnswerEditText == null) return;
            showCreateOrEditLinkDialog(context, autoAnswerEditText);
        }));
        row.addView(createFormatChip(context, "Aa Style", 0xFFEC4899, isDark, v -> org.nayagram.platform.textstyle.NGTextStyleHelper.showDialog(context)));
        row.addView(createFormatChip(context, "✕ Clear", 0xFF64748B, isDark, v -> applyFormat(et -> {
            selectWordOrAll(et);
            et.makeSelectedRegular();
            Editable ed = et.getText();
            if (ed != null) {
                int s = Math.max(0, et.getSelectionStart());
                int e = Math.max(s, et.getSelectionEnd());
                URLSpanReplacement[] urls = ed.getSpans(s, e, URLSpanReplacement.class);
                if (urls != null) {
                    for (URLSpanReplacement u : urls) ed.removeSpan(u);
                }
            }
        })));

        return hsv;
    }

    private interface FormatAction {
        void run(EditTextCaption et);
    }

    private void applyFormat(FormatAction action) {
        if (autoAnswerEditText == null) return;
        suppressTextWatcher = true;
        try {
            action.run(autoAnswerEditText);
        } finally {
            suppressTextWatcher = false;
        }
        autoAnswerEditText.invalidate();
        saveCurrentEditor();
    }

    private TextView createFormatChip(Context context, String label, int accentColor, boolean isDark, View.OnClickListener onClick) {
        TextView btn = new TextView(context);
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(isDark ? Color.WHITE : accentColor);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(7), AndroidUtilities.dp(12), AndroidUtilities.dp(7));
        // Prevent taking focus away from autoAnswerEditText so text selection stays visible!
        btn.setFocusable(false);
        btn.setFocusableInTouchMode(false);

        GradientDrawable gd = new GradientDrawable();
        gd.setColor((accentColor & 0x00FFFFFF) | (isDark ? 0x33000000 : 0x16000000));
        gd.setCornerRadius(AndroidUtilities.dp(10));
        gd.setStroke(AndroidUtilities.dp(1.2f), (accentColor & 0x00FFFFFF) | (isDark ? 0x99000000 : 0x55000000));
        btn.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = AndroidUtilities.dp(7);
        btn.setLayoutParams(lp);
        btn.setOnClickListener(onClick);
        return btn;
    }

    private void selectWordOrAll(EditTextCaption et) {
        if (et == null) return;
        Editable text = et.getText();
        if (text == null || text.length() == 0) return;
        int start = et.getSelectionStart();
        int end = et.getSelectionEnd();
        if (start != end && start >= 0 && end >= 0) return;
        int len = text.length();
        int wStart = Math.min(Math.max(0, start), len);
        int wEnd = wStart;
        while (wStart > 0 && !Character.isWhitespace(text.charAt(wStart - 1))) {
            wStart--;
        }
        while (wEnd < len && !Character.isWhitespace(text.charAt(wEnd))) {
            wEnd++;
        }
        if (wStart < wEnd) {
            et.setSelection(wStart, wEnd);
        } else {
            et.setSelection(0, len);
        }
    }

    /**
     * Unified, rock-solid Link Creator & Editor dialog.
     * Captures selection range immediately before dialog open, supports both selected text and new link insertion,
     * applies URLSpanReplacement + visual blue underline span, and persists entities immediately to AutoReplyManager.
     */
    private void showCreateOrEditLinkDialog(Context context, EditTextCaption targetEditText) {
        if (context == null || targetEditText == null) return;
        final boolean isDark = Theme.isCurrentThemeDark();
        final Editable editable = targetEditText.getText();
        final int rawStart = targetEditText.getSelectionStart();
        final int rawEnd = targetEditText.getSelectionEnd();
        final int selStart = editable != null ? Math.max(0, Math.min(Math.min(rawStart, rawEnd), editable.length())) : 0;
        final int selEnd = editable != null ? Math.max(selStart, Math.min(Math.max(rawStart, rawEnd), editable.length())) : 0;

        String selectedText = (editable != null && selEnd > selStart)
                ? editable.subSequence(selStart, selEnd).toString()
                : "";
        String existingUrl = "https://";
        if (editable != null && selEnd > selStart) {
            URLSpanReplacement[] existingSpans = editable.getSpans(selStart, selEnd, URLSpanReplacement.class);
            if (existingSpans != null && existingSpans.length > 0 && !TextUtils.isEmpty(existingSpans[0].getURL())) {
                existingUrl = existingSpans[0].getURL();
            }
        }

        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(LocaleController.getString(R.string.CreateLink));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        TextView titleLabel = new TextView(context);
        titleLabel.setText("Display Text");
        titleLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        titleLabel.setTypeface(AndroidUtilities.bold());
        titleLabel.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        layout.addView(titleLabel);

        EditText titleInput = new EditText(context);
        titleInput.setHint("e.g. Visit Website / Contact Support");
        titleInput.setText(selectedText);
        titleInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        titleInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleInput.setSingleLine(true);
        layout.addView(titleInput);

        TextView urlLabel = new TextView(context);
        urlLabel.setText("URL (Hyperlink)");
        urlLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13f);
        urlLabel.setTypeface(AndroidUtilities.bold());
        urlLabel.setTextColor(isDark ? 0xFFE2E8F0 : 0xFF1E293B);
        urlLabel.setPadding(0, AndroidUtilities.dp(12), 0, 0);
        layout.addView(urlLabel);

        EditText urlInput = new EditText(context);
        urlInput.setHint("https://t.me/...");
        urlInput.setText(existingUrl);
        urlInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15.5f);
        urlInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        urlInput.setSingleLine(true);
        urlInput.setSelection(urlInput.getText().length());
        layout.addView(urlInput);

        b.setView(layout);
        b.setPositiveButton(LocaleController.getString(R.string.OK), (d, w) -> {
            String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
            String url = urlInput.getText() != null ? urlInput.getText().toString().trim() : "";
            if (url.isEmpty() || url.equalsIgnoreCase("https://") || url.equalsIgnoreCase("http://")) {
                BulletinFactory.of(NGAutoReplyActivity.this)
                        .createSimpleBulletin(R.raw.error, "Please enter a valid URL")
                        .show();
                return;
            }
            if (!url.contains("://") && !url.startsWith("mailto:") && !url.startsWith("tg:")) {
                url = "https://" + url;
            }
            if (title.isEmpty()) {
                title = url;
            }

            Editable targetEditable = targetEditText.getText();
            if (targetEditable != null) {
                suppressTextWatcher = true;
                try {
                    int safeStart = Math.max(0, Math.min(selStart, targetEditable.length()));
                    int safeEnd = Math.max(safeStart, Math.min(selEnd, targetEditable.length()));
                    if (safeEnd > safeStart) {
                        targetEditable.replace(safeStart, safeEnd, title);
                    } else {
                        targetEditable.insert(safeStart, title);
                    }
                    int linkEnd = safeStart + title.length();
                    AutoReplyManager.applyVisualLinkSpan(targetEditable, safeStart, linkEnd, url);
                    targetEditText.setSelection(linkEnd);
                } finally {
                    suppressTextWatcher = false;
                }
                targetEditText.invalidate();
                saveCurrentEditor();
                BulletinFactory.of(NGAutoReplyActivity.this)
                        .createSimpleBulletin(R.raw.done, "Hyperlink saved: " + title)
                        .show();
            }
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        showDialog(b.create());
    }

    private void saveCurrentEditor() {
        if (autoAnswerEditText == null) return;
        Editable s = autoAnswerEditText.getText();
        String plain = s != null ? s.toString() : "";
        String json = AutoReplyManager.serializeSpansToJson(s);
        AutoReplyManager.getInstance().setAutoAnswerText(plain, json);
    }

    @Override
    public int getNavigationBarColor() {
        return Theme.getColor(Theme.key_windowBackgroundGray);
    }

    @Override
    public void onPause() {
        super.onPause();
        saveCurrentEditor();
    }

    public static void convertLegacyMarkdownToSpans(Editable editable) {
        if (editable == null || editable.length() == 0) return;
        try {
            CharSequence[] arr = new CharSequence[]{editable.toString()};
            ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(UserConfig.selectedAccount).getEntities(arr, true);
            if (entities != null && !entities.isEmpty() && arr[0] != null) {
                editable.replace(0, editable.length(), arr[0]);
                MediaDataController.addTextStyleRuns(entities, arr[0], editable);
                for (TLRPC.MessageEntity entity : entities) {
                    if (entity instanceof TLRPC.TL_messageEntityTextUrl) {
                        AutoReplyManager.applyVisualLinkSpan(editable, entity.offset, entity.offset + entity.length, entity.url);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
