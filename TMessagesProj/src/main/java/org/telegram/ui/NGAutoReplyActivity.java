package org.telegram.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
import android.text.InputType;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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
 * - Pure Novagram aesthetic: grouped settings section + spacious full-editor card.
 * - Top native formatting & link creator toolbar (Bold, Italic, Mono, Spoiler, Quote, Strike, Underline, Link, Styles, Clear).
 * - Zero markdown symbol clutter, zero 15+ keyword rule lists.
 * - Auto-saves text and native Telegram formatting entities as you type.
 */
public class NGAutoReplyActivity extends BaseFragment {

    private static final int MENU_DONE = 1;
    private static final int MENU_RESET_HISTORY = 2;

    private Switch autoAnswerSwitch;
    private Switch privateOnlySwitch;
    private EditTextCaption autoAnswerEditText;

    @Override
    public View createView(Context context) {
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
        column.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(8), AndroidUtilities.dp(12), AndroidUtilities.dp(20));
        scrollView.addView(column, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // 1. Settings section card (Novagram style)
        LinearLayout settingsCard = createSectionCard(context);
        TextView sectionHeader = new TextView(context);
        sectionHeader.setText("Auto answer");
        sectionHeader.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        sectionHeader.setTypeface(AndroidUtilities.bold());
        sectionHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        sectionHeader.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(6));
        settingsCard.addView(sectionHeader);

        AutoReplyManager mgr = AutoReplyManager.getInstance();

        autoAnswerSwitch = new Switch(context);
        autoAnswerSwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        autoAnswerSwitch.setChecked(mgr.isAutoReplyEnabled(), false);
        settingsCard.addView(createSwitchRow(
                context,
                "Auto answer",
                "Automatically reply once to the first message",
                autoAnswerSwitch,
                v -> {
                    boolean next = !autoAnswerSwitch.isChecked();
                    autoAnswerSwitch.setChecked(next, true);
                    AutoReplyManager.getInstance().setAutoReplyEnabled(next);
                }
        ));

        settingsCard.addView(createHairlineDivider(context));

        privateOnlySwitch = new Switch(context);
        privateOnlySwitch.setColors(Theme.key_switchTrack, Theme.key_switchTrackChecked, Theme.key_windowBackgroundWhite, Theme.key_windowBackgroundWhite);
        privateOnlySwitch.setChecked(mgr.isPrivateOnly(), false);
        settingsCard.addView(createSwitchRow(
                context,
                "Private chats only",
                "Skip groups and channels",
                privateOnlySwitch,
                v -> {
                    boolean next = !privateOnlySwitch.isChecked();
                    privateOnlySwitch.setChecked(next, true);
                    AutoReplyManager.getInstance().setPrivateOnly(next);
                }
        ));

        column.addView(settingsCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // 2. Reply Message Editor Card with Top Native Formatting & Link Bar
        LinearLayout editorCard = createSectionCard(context);

        LinearLayout editorHeaderRow = new LinearLayout(context);
        editorHeaderRow.setOrientation(LinearLayout.HORIZONTAL);
        editorHeaderRow.setGravity(Gravity.CENTER_VERTICAL);
        editorHeaderRow.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(12), AndroidUtilities.dp(8));

        TextView editorHeader = new TextView(context);
        editorHeader.setText("Reply message");
        editorHeader.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        editorHeader.setTypeface(AndroidUtilities.bold());
        editorHeader.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        editorHeaderRow.addView(editorHeader, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        TextView resetBtn = new TextView(context);
        resetBtn.setText("Reset sent list");
        resetBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        resetBtn.setTypeface(AndroidUtilities.bold());
        resetBtn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        resetBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(6), AndroidUtilities.dp(4));
        resetBtn.setOnClickListener(v -> {
            AutoReplyManager.getInstance().clearAnsweredDialogs();
            BulletinFactory.of(NGAutoReplyActivity.this)
                    .createSimpleBulletin(R.raw.chats_infotip, "Answered chats list cleared")
                    .show();
        });
        editorHeaderRow.addView(resetBtn);
        editorCard.addView(editorHeaderRow);

        // Top Formatting & Link Creator Toolbar
        editorCard.addView(buildFormattingToolbar(context));

        View topDivider = new View(context);
        topDivider.setBackgroundColor(Theme.getColor(Theme.key_divider));
        editorCard.addView(topDivider, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1));

        // Spacious full-height EditTextCaption (Novagram AutoAnswerMenu style)
        autoAnswerEditText = new EditTextCaption(context, null);
        autoAnswerEditText.setBackground(null);
        autoAnswerEditText.setGravity(Gravity.TOP | Gravity.START);
        autoAnswerEditText.setMinLines(8);
        autoAnswerEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        autoAnswerEditText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        autoAnswerEditText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        autoAnswerEditText.setHint("Write your auto-answer reply here...\nSelect words and tap the style buttons above to format or add a link.");
        autoAnswerEditText.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        autoAnswerEditText.setSingleLine(false);
        autoAnswerEditText.setPadding(
                AndroidUtilities.dp(16),
                AndroidUtilities.dp(14),
                AndroidUtilities.dp(16),
                AndroidUtilities.dp(16)
        );

        String savedText = mgr.getAutoAnswerText();
        String savedEntities = mgr.getAutoAnswerEntitiesJson();
        autoAnswerEditText.setText(savedText);
        if (savedEntities != null && !savedEntities.isEmpty()) {
            AutoReplyManager.restoreSpansFromJson(autoAnswerEditText.getText(), savedEntities);
        } else if (savedText != null && (savedText.contains("**") || savedText.contains("__") || savedText.contains("||") || savedText.contains("`") || savedText.contains("["))) {
            convertLegacyMarkdownToSpans(autoAnswerEditText.getText());
        }
        autoAnswerEditText.setSelection(autoAnswerEditText.getText().length());

        autoAnswerEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {
                String plain = s != null ? s.toString() : "";
                String json = AutoReplyManager.serializeSpansToJson(s);
                AutoReplyManager.getInstance().setAutoAnswerText(plain, json);
            }
        });

        editorCard.addView(autoAnswerEditText, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        ));

        TextView footerHint = new TextView(context);
        footerHint.setText("Changes save automatically with native formatting & links.");
        footerHint.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        footerHint.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        footerHint.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        editorCard.addView(footerHint);

        LinearLayout.LayoutParams editorCardLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
        );
        column.addView(editorCard, editorCardLp);

        return root;
    }

    private LinearLayout createSectionCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        card.setBackground(bg);
        card.setClipToOutline(true);
        return card;
    }

    private View createSwitchRow(Context context, String title, String subtitle, Switch sw, View.OnClickListener onClick) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        android.util.TypedValue tv = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            row.setBackgroundResource(tv.resourceId);
        }
        row.setClickable(true);
        row.setOnClickListener(onClick);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        texts.addView(titleView);

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subView = new TextView(context);
            subView.setText(subtitle);
            subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subView.setPadding(0, AndroidUtilities.dp(2), 0, 0);
            texts.addView(subView);
        }

        row.addView(texts, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        sw.setClickable(false);
        row.addView(sw, LayoutHelper.createLinear(37, 20, Gravity.CENTER_VERTICAL, 12, 0, 0, 0));
        return row;
    }

    private View createHairlineDivider(Context context) {
        LinearLayout holder = new LinearLayout(context);
        View line = new View(context);
        line.setBackgroundColor(Theme.getColor(Theme.key_divider));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1);
        lp.leftMargin = AndroidUtilities.dp(16);
        holder.addView(line, lp);
        return holder;
    }

    private View buildFormattingToolbar(Context context) {
        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        hsv.setOverScrollMode(View.OVER_SCROLL_NEVER);
        hsv.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(2), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        hsv.addView(row);

        row.addView(createFormatChip(context, "B Bold", v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedBold(); })));
        row.addView(createFormatChip(context, "I Italic", v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedItalic(); })));
        row.addView(createFormatChip(context, "</> Mono", v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedMono(); })));
        row.addView(createFormatChip(context, "✦ Spoiler", v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedSpoiler(); })));
        row.addView(createFormatChip(context, "❝ Quote", v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedQuote(); })));
        row.addView(createFormatChip(context, "S̶ Strike", v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedStrike(); })));
        row.addView(createFormatChip(context, "U̲ Under", v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedUnderline(); })));
        row.addView(createFormatChip(context, "🔗 Link", v -> {
            if (autoAnswerEditText == null) return;
            if (autoAnswerEditText.hasSelection()) {
                autoAnswerEditText.makeSelectedUrl(() -> saveCurrentEditor());
            } else {
                showInsertLinkDialogNative(context, autoAnswerEditText);
            }
        }));
        row.addView(createFormatChip(context, "🎨 Style", v -> org.nayagram.platform.textstyle.NGTextStyleHelper.showDialog(context)));
        row.addView(createFormatChip(context, "Clear", v -> applyFormat(et -> { selectWordOrAll(et); et.makeSelectedRegular(); })));

        return hsv;
    }

    private interface FormatAction {
        void run(EditTextCaption et);
    }

    private void applyFormat(FormatAction action) {
        if (autoAnswerEditText == null) return;
        action.run(autoAnswerEditText);
        saveCurrentEditor();
    }

    private TextView createFormatChip(Context context, String label, View.OnClickListener onClick) {
        boolean isDark = Theme.isCurrentThemeDark();
        TextView btn = new TextView(context);
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12.5f);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(AndroidUtilities.dp(11), AndroidUtilities.dp(6), AndroidUtilities.dp(11), AndroidUtilities.dp(6));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        gd.setCornerRadius(AndroidUtilities.dp(10));
        gd.setStroke(AndroidUtilities.dp(1), isDark ? 0x33FFFFFF : 0x1A000000);
        btn.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = AndroidUtilities.dp(6);
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
        if (start != end) return;
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

    private void showInsertLinkDialogNative(Context context, EditTextCaption targetEditText) {
        if (context == null || targetEditText == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle(LocaleController.getString(R.string.CreateLink));

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(10), AndroidUtilities.dp(20), AndroidUtilities.dp(10));

        TextView titleLabel = new TextView(context);
        titleLabel.setText("Text");
        titleLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        titleLabel.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        layout.addView(titleLabel);

        EditText titleInput = new EditText(context);
        titleInput.setHint("e.g. Contact Support");
        titleInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        titleInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(titleInput);

        TextView urlLabel = new TextView(context);
        urlLabel.setText(LocaleController.getString(R.string.URL));
        urlLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        urlLabel.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        urlLabel.setPadding(0, AndroidUtilities.dp(12), 0, 0);
        layout.addView(urlLabel);

        EditText urlInput = new EditText(context);
        urlInput.setHint("https://...");
        urlInput.setText("https://");
        urlInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        urlInput.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        layout.addView(urlInput);

        b.setView(layout);
        b.setPositiveButton(LocaleController.getString(R.string.OK), (d, w) -> {
            String title = titleInput.getText() != null ? titleInput.getText().toString().trim() : "";
            String url = urlInput.getText() != null ? urlInput.getText().toString().trim() : "";
            if (title.isEmpty()) title = "Link";
            if (url.isEmpty()) url = "https://";

            int start = Math.max(0, targetEditText.getSelectionStart());
            Editable editable = targetEditText.getText();
            if (editable != null) {
                editable.insert(start, title);
                editable.setSpan(new URLSpanReplacement(url), start, start + title.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                targetEditText.setSelection(start + title.length());
                saveCurrentEditor();
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
                        editable.setSpan(new URLSpanReplacement(entity.url), entity.offset, entity.offset + entity.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }
}
