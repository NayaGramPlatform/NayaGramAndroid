package org.telegram.ui;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
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
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
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
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.EditTextCaption;
import org.telegram.ui.Components.URLSpanReplacement;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.List;

/**
 * NGAutoReplyActivity - Clean Novagram & Graph Messenger style Auto-Reply.
 * - Simple master switch (Auto-Reply On/Off).
 * - Full-page auto-answer editor with real-time auto-saving (like Novagram AutoAnswerMenu.kt).
 * - Private chats only toggle.
 * - Custom keyword rules list with instant Add/Delete (Graph Messenger style).
 * - ZERO forced 16 preset rules, ZERO fake stealth ghost cards, ZERO broken markdown tags.
 */
public class NGAutoReplyActivity extends BaseFragment {

    private Switch masterSwitch;
    private TextView masterStatusText;
    private EditTextCaption autoAnswerEditText;
    private LinearLayout rulesContainer;
    private TextView emptyRulesText;

    private static final int COLOR_PRIMARY_CYAN = 0xFF0284C7;
    private static final int COLOR_EMERALD = 0xFF10B981;
    private static final int COLOR_CARD_DARK = 0xFF1E293B;
    private static final int COLOR_BORDER_DARK = 0xFF334155;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("Auto-Reply");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                }
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int padH = AndroidUtilities.dp(16);
        int padV = AndroidUtilities.dp(16);
        root.setPadding(padH, padV, padH, AndroidUtilities.dp(40));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Master Auto-Reply Switch Card (Novagram Style)
        root.addView(createMasterSwitchCard(context));

        // 2. Main Auto-Answer Message Card (Novagram AutoAnswerMenu Style)
        root.addView(createAutoAnswerMessageCard(context));

        // 3. Target Scope Settings (Private Chats Only)
        root.addView(createScopeCard(context));

        // 4. Custom Keyword Rules Section (Graph Messenger Style)
        root.addView(createKeywordRulesHeader(context));

        rulesContainer = new LinearLayout(context);
        rulesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(rulesContainer);

        emptyRulesText = new TextView(context);
        emptyRulesText.setText("No custom keyword rules added yet.\nTap \"+ Add Rule\" above to trigger replies on specific words.");
        emptyRulesText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        emptyRulesText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        emptyRulesText.setGravity(Gravity.CENTER);
        emptyRulesText.setPadding(0, AndroidUtilities.dp(20), 0, AndroidUtilities.dp(20));
        root.addView(emptyRulesText);

        refreshRulesList(context);

        return fragmentView;
    }

    private View createMasterSwitchCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);
        bg.setColors(new int[]{0xFF0284C7, 0xFF0EA5E9});
        bg.setCornerRadius(AndroidUtilities.dp(18));
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(14);
        card.setLayoutParams(lp);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(context);
        title.setText("Auto-Reply (স্বয়ংক্রিয় উত্তর)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 17);
        title.setTextColor(Color.WHITE);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        textCol.addView(title);

        masterStatusText = new TextView(context);
        boolean active = AutoReplyManager.getInstance().isAutoReplyEnabled();
        masterStatusText.setText(active ? "🟢 Active & Ready to reply" : "⚪ Disabled (Auto-replies paused)");
        masterStatusText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        masterStatusText.setTextColor(0xEEFFFFFF);
        masterStatusText.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        textCol.addView(masterStatusText);

        row.addView(textCol);

        masterSwitch = new Switch(context);
        masterSwitch.setChecked(active);
        masterSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            AutoReplyManager.getInstance().setAutoReplyEnabled(isChecked);
            masterStatusText.setText(isChecked ? "🟢 Active & Ready to reply" : "⚪ Disabled (Auto-replies paused)");
            BulletinFactory.of(this).createSimpleBulletin(
                isChecked ? R.raw.done : R.raw.chats_infotip,
                "Auto-Reply: " + (isChecked ? "Enabled" : "Disabled")
            ).show();
        });
        row.addView(masterSwitch);
        card.addView(row);

        TextView sub = new TextView(context);
        sub.setText("Automatically sends your pre-written reply when contacts message you.");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        sub.setTextColor(0xCCFFFFFF);
        sub.setPadding(0, AndroidUtilities.dp(10), 0, 0);
        card.addView(sub);

        return card;
    }

    private View createAutoAnswerMessageCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        boolean isDark = Theme.isCurrentThemeDark();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isDark ? COLOR_CARD_DARK : Color.WHITE);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setStroke(AndroidUtilities.dp(1), isDark ? COLOR_BORDER_DARK : 0xFFE2E8F0);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(14);
        card.setLayoutParams(lp);

        TextView title = new TextView(context);
        title.setText("💬 Auto-Reply Text (মূল মেসেজ)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(isDark ? Color.WHITE : 0xFF0F172A);
        card.addView(title);

        TextView subtitle = new TextView(context);
        subtitle.setText("This message is sent automatically when someone messages you.");
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        subtitle.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        subtitle.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(10));
        card.addView(subtitle);

        // Rich Text Formatting & Link Toolbar
        card.addView(buildFormattingToolbar(context, () -> autoAnswerEditText));

        // Edit box (Novagram AutoAnswerMenu style with Telegram native EditTextCaption - 100% live formatting)
        autoAnswerEditText = new EditTextCaption(context, null);
        autoAnswerEditText.setGravity(Gravity.TOP | Gravity.START);
        autoAnswerEditText.setMinLines(4);
        autoAnswerEditText.setMaxLines(8);
        autoAnswerEditText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        autoAnswerEditText.setTextColor(isDark ? Color.WHITE : 0xFF0F172A);
        autoAnswerEditText.setHintTextColor(isDark ? 0xFF64748B : 0xFF94A3B8);
        autoAnswerEditText.setHint("Write your auto-reply text here...\ne.g. Hello! I am currently busy, will get back to you soon.");
        autoAnswerEditText.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);

        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(isDark ? 0xFF0F172A : 0xFFF8FAFC);
        inputBg.setCornerRadius(AndroidUtilities.dp(12));
        inputBg.setStroke(AndroidUtilities.dp(1.2f), isDark ? 0xFF334155 : 0xFFCBD5E1);
        autoAnswerEditText.setBackground(inputBg);
        autoAnswerEditText.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        String savedText = AutoReplyManager.getInstance().getAutoAnswerText();
        String savedEntities = AutoReplyManager.getInstance().getAutoAnswerEntitiesJson();
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

        card.addView(autoAnswerEditText);

        // Clean save confirmation info
        TextView note = new TextView(context);
        note.setText("✓ Auto-saves automatically as you type.");
        note.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        note.setTextColor(COLOR_EMERALD);
        note.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(6), 0, 0);
        card.addView(note);

        return card;
    }

    private View createScopeCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));

        boolean isDark = Theme.isCurrentThemeDark();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isDark ? COLOR_CARD_DARK : Color.WHITE);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setStroke(AndroidUtilities.dp(1), isDark ? COLOR_BORDER_DARK : 0xFFE2E8F0);
        card.setBackground(bg);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.bottomMargin = AndroidUtilities.dp(14);
        card.setLayoutParams(lp);

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(context);
        title.setText("Private Chats Only (শুধুমাত্র ইনবক্স)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(isDark ? Color.WHITE : 0xFF0F172A);
        textCol.addView(title);

        TextView sub = new TextView(context);
        sub.setText("Do not send auto-replies in groups or channels.");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        sub.setTextColor(isDark ? 0xFF94A3B8 : 0xFF64748B);
        sub.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        textCol.addView(sub);

        card.addView(textCol);

        Switch scopeSwitch = new Switch(context);
        scopeSwitch.setChecked(AutoReplyManager.getInstance().isPrivateOnly());
        scopeSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            AutoReplyManager.getInstance().setPrivateOnly(isChecked);
        });
        card.addView(scopeSwitch);

        return card;
    }

    private View createKeywordRulesHeader(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(4), AndroidUtilities.dp(6), AndroidUtilities.dp(4), AndroidUtilities.dp(10));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(context);
        title.setText("Keyword Rules (Graph Messenger Style)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textCol.addView(title);

        TextView sub = new TextView(context);
        sub.setText("Optional rules triggered by specific keywords.");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        textCol.addView(sub);

        row.addView(textCol);

        // + Add Rule Button
        TextView addBtn = new TextView(context);
        addBtn.setText("+ Add Rule");
        addBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        addBtn.setTypeface(Typeface.DEFAULT_BOLD);
        addBtn.setTextColor(Color.WHITE);
        addBtn.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));

        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(COLOR_PRIMARY_CYAN);
        btnBg.setCornerRadius(AndroidUtilities.dp(12));
        addBtn.setBackground(btnBg);
        addBtn.setOnClickListener(v -> showAddRuleDialog(context));

        row.addView(addBtn);

        return row;
    }

    private void refreshRulesList(Context context) {
        if (rulesContainer == null) return;
        rulesContainer.removeAllViews();

        List<AutoReplyManager.AutoReplyRule> rules = AutoReplyManager.getInstance().getRules();
        if (rules.isEmpty()) {
            if (emptyRulesText != null) emptyRulesText.setVisibility(View.VISIBLE);
            return;
        }
        if (emptyRulesText != null) emptyRulesText.setVisibility(View.GONE);

        boolean isDark = Theme.isCurrentThemeDark();

        for (int i = 0; i < rules.size(); i++) {
            final int index = i;
            AutoReplyManager.AutoReplyRule rule = rules.get(i);

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.VERTICAL);
            card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(isDark ? COLOR_CARD_DARK : Color.WHITE);
            cardBg.setCornerRadius(AndroidUtilities.dp(14));
            cardBg.setStroke(AndroidUtilities.dp(1), isDark ? COLOR_BORDER_DARK : 0xFFE2E8F0);
            card.setBackground(cardBg);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = AndroidUtilities.dp(10);
            card.setLayoutParams(lp);

            // Top Row: Keyword Badge + Switch + Delete
            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView keywordBadge = new TextView(context);
            keywordBadge.setText("🔑 " + rule.keyword);
            keywordBadge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            keywordBadge.setTypeface(Typeface.DEFAULT_BOLD);
            keywordBadge.setTextColor(COLOR_PRIMARY_CYAN);
            topRow.addView(keywordBadge, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            Switch ruleSwitch = new Switch(context);
            ruleSwitch.setChecked(rule.enabled);
            ruleSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                AutoReplyManager.getInstance().setRuleEnabled(index, isChecked);
            });
            topRow.addView(ruleSwitch);

            TextView deleteBtn = new TextView(context);
            deleteBtn.setText("🗑️");
            deleteBtn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
            deleteBtn.setPadding(AndroidUtilities.dp(10), 0, 0, 0);
            deleteBtn.setOnClickListener(v -> {
                AutoReplyManager.getInstance().deleteRule(index);
                refreshRulesList(context);
                Toast.makeText(context, "Rule deleted", Toast.LENGTH_SHORT).show();
            });
            topRow.addView(deleteBtn);

            card.addView(topRow);

            // Response Text
            TextView replyText = new TextView(context);
            replyText.setText(rule.response);
            replyText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            replyText.setTextColor(isDark ? 0xFFCBD5E1 : 0xFF334155);
            replyText.setPadding(0, AndroidUtilities.dp(6), 0, 0);
            card.addView(replyText);

            rulesContainer.addView(card);
        }
    }

    private void showAddRuleDialog(Context context) {
        AlertDialog.Builder b = new AlertDialog.Builder(context);
        b.setTitle("Add Keyword Rule");

        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(12), AndroidUtilities.dp(20), AndroidUtilities.dp(12));

        final EditText keywordInput = new EditText(context);
        keywordInput.setHint("Keyword to match (e.g. price, address)");
        layout.addView(keywordInput);

        final EditTextCaption replyInput = new EditTextCaption(context, null);
        replyInput.setHint("Automated reply text");
        replyInput.setMinLines(2);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = AndroidUtilities.dp(12);
        lp.bottomMargin = AndroidUtilities.dp(4);
        layout.addView(replyInput, lp);
        layout.addView(buildFormattingToolbar(context, () -> replyInput));

        b.setView(layout);

        b.setPositiveButton("Save", (dialog, which) -> {
            String kw = keywordInput.getText().toString().trim();
            String rep = replyInput.getText().toString().trim();
            String repJson = AutoReplyManager.serializeSpansToJson(replyInput.getText());
            if (TextUtils.isEmpty(kw) || TextUtils.isEmpty(rep)) {
                Toast.makeText(context, "Keyword and reply cannot be empty", Toast.LENGTH_SHORT).show();
                return;
            }
            AutoReplyManager.getInstance().addRule(new AutoReplyManager.AutoReplyRule(kw, kw, rep, repJson));
            refreshRulesList(context);
            Toast.makeText(context, "Rule added successfully", Toast.LENGTH_SHORT).show();
        });

        b.setNegativeButton("Cancel", null);
        b.show();
    }

    private View buildFormattingToolbar(Context context, java.util.concurrent.Callable<EditTextCaption> targetGetter) {
        android.widget.HorizontalScrollView hsv = new android.widget.HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout.LayoutParams hsvLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        hsvLp.bottomMargin = AndroidUtilities.dp(8);
        hsv.setLayoutParams(hsvLp);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        hsv.addView(row);

        boolean isDark = Theme.isCurrentThemeDark();

        // 1. Bold Button (1-Click Telegram Native Bold - Zero Asterisks)
        TextView boldBtn = createToolbarButton(context, "B Bold", isDark);
        boldBtn.setOnClickListener(v -> {
            try {
                EditTextCaption et = targetGetter.call();
                if (et != null) {
                    selectWordOrAll(et);
                    et.makeSelectedBold();
                }
            } catch (Exception ignored) {}
        });
        row.addView(boldBtn);

        // 2. Italic Button (1-Click Telegram Native Italic - Zero Underscores)
        TextView italicBtn = createToolbarButton(context, "I Italic", isDark);
        italicBtn.setOnClickListener(v -> {
            try {
                EditTextCaption et = targetGetter.call();
                if (et != null) {
                    selectWordOrAll(et);
                    et.makeSelectedItalic();
                }
            } catch (Exception ignored) {}
        });
        row.addView(italicBtn);

        // 3. Monospace Button (1-Click Telegram Native Code - Zero Backticks)
        TextView monoBtn = createToolbarButton(context, "</> Mono", isDark);
        monoBtn.setOnClickListener(v -> {
            try {
                EditTextCaption et = targetGetter.call();
                if (et != null) {
                    selectWordOrAll(et);
                    et.makeSelectedMono();
                }
            } catch (Exception ignored) {}
        });
        row.addView(monoBtn);

        // 4. Spoiler Button (1-Click Telegram Native Animated Spoiler - Zero Vertical Bars)
        TextView spoilerBtn = createToolbarButton(context, "✦ Spoiler", isDark);
        spoilerBtn.setOnClickListener(v -> {
            try {
                EditTextCaption et = targetGetter.call();
                if (et != null) {
                    selectWordOrAll(et);
                    et.makeSelectedSpoiler();
                }
            } catch (Exception ignored) {}
        });
        row.addView(spoilerBtn);

        // 5. Quote Button (1-Click Telegram Native Quote - Zero Angle Brackets)
        TextView quoteBtn = createToolbarButton(context, "❝ Quote", isDark);
        quoteBtn.setOnClickListener(v -> {
            try {
                EditTextCaption et = targetGetter.call();
                if (et != null) {
                    selectWordOrAll(et);
                    et.makeSelectedQuote();
                }
            } catch (Exception ignored) {}
        });
        row.addView(quoteBtn);

        // 6. Hyperlink Button (1-Click Telegram Native Link - Zero Brackets)
        TextView linkBtn = createToolbarButton(context, "🔗 Link", isDark);
        linkBtn.setOnClickListener(v -> {
            try {
                EditTextCaption et = targetGetter.call();
                if (et != null) {
                    if (et.hasSelection()) {
                        et.makeSelectedUrl();
                    } else {
                        showInsertLinkDialogNative(context, et);
                    }
                }
            } catch (Exception ignored) {}
        });
        row.addView(linkBtn);

        // 7. Text Styles Preset Button (International Telegram Palette)
        TextView styleBtn = createToolbarButton(context, "🎨 Styles", isDark);
        styleBtn.setOnClickListener(v -> {
            org.nayagram.platform.textstyle.NGTextStyleHelper.showDialog(context);
        });
        row.addView(styleBtn);

        return hsv;
    }

    private TextView createToolbarButton(Context context, String label, boolean isDark) {
        TextView btn = new TextView(context);
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        btn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        btn.setTextColor(isDark ? 0xFF38BDF8 : 0xFF0284C7);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));

        GradientDrawable gd = new GradientDrawable();
        gd.setColor(isDark ? 0xFF1E293B : 0xFFF1F5F9);
        gd.setCornerRadius(AndroidUtilities.dp(8));
        gd.setStroke(AndroidUtilities.dp(1), isDark ? 0xFF334155 : 0xFFCBD5E1);
        btn.setBackground(gd);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = AndroidUtilities.dp(6);
        btn.setLayoutParams(lp);
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
        titleLabel.setText("Link Text:");
        titleLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        layout.addView(titleLabel);

        EditText titleInput = new EditText(context);
        titleInput.setHint("e.g. My Website / Support Channel");
        titleInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        layout.addView(titleInput);

        TextView urlLabel = new TextView(context);
        urlLabel.setText(LocaleController.getString(R.string.URL) + ":");
        urlLabel.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        urlLabel.setPadding(0, AndroidUtilities.dp(10), 0, 0);
        layout.addView(urlLabel);

        EditText urlInput = new EditText(context);
        urlInput.setHint("https://...");
        urlInput.setText("https://");
        urlInput.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
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
            }
        });
        b.setNegativeButton(LocaleController.getString(R.string.Cancel), null);
        b.show();
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
