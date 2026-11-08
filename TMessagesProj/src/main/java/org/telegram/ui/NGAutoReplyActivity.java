package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.Editable;
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
import android.widget.Switch;
import android.widget.TextView;

import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.AutoReplyManager;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * NayaGram Smart Auto-Reply Activity:
 * Complete UI/UX studio for managing 15+ keyword rules, master switch,
 * search filter, categories, and Markdown away message editor.
 */
public class NGAutoReplyActivity extends BaseFragment {

    private LinearLayout rulesContainer;
    private TextView masterStatusText;
    private Switch masterSwitch;
    private TextView defaultMessagePreview;
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "ALL";

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Smart Auto-Reply Studio");
        actionBar.setSubtitle("15+ Rules & Instant Replies");
        actionBar.setAllowOverlayTitle(true);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) {
                    finishFragment();
                } else if (id == 1) {
                    showAddEditRuleDialog(-1, null);
                }
            }
        });
        actionBar.createMenu().addItem(1, R.drawable.msg_add);

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        FrameLayout frameLayout = (FrameLayout) fragmentView;

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);
        frameLayout.addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(16);
        root.setPadding(pad, pad, pad, AndroidUtilities.dp(36));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Master Toggle Card
        root.addView(createMasterCard(context));

        // 2. Default Away Message Card
        root.addView(createDefaultMessageCard(context));

        // 2.5 Stealth Ghost Presence Card (Keep offline & preserve last-seen)
        root.addView(createStealthPresenceCard(context));

        // 3. Search Bar & Category Filter Chips
        root.addView(createSearchAndFilterSection(context));

        // 4. Keyword Rules Section Header
        root.addView(createSectionHeader(context, "Instant Keyword Rules", "Matches queries and replies immediately without cooldown"));

        // 5. Container for Rules List
        rulesContainer = new LinearLayout(context);
        rulesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(rulesContainer, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 6. Actions Card (Add Rule / Restore Defaults)
        root.addView(createActionsCard(context));

        refreshRulesList(context);

        return fragmentView;
    }

    private View createMasterCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = AndroidUtilities.dp(16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0284C7, 0xFF0369A1}
        );
        bg.setCornerRadius(AndroidUtilities.dp(16));
        card.setBackground(bg);

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams tlParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        textLayout.setLayoutParams(tlParams);

        TextView title = new TextView(context);
        title.setText("Smart Auto-Reply Master Switch");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(AndroidUtilities.bold());
        textLayout.addView(title);

        masterStatusText = new TextView(context);
        boolean isEnabled = NayaConfig.getInstance().isSmartAutoReply() || AutoReplyManager.getInstance().isAutoReplyEnabled();
        masterStatusText.setText(isEnabled ? "Active: Automatically replying to 1:1 chats" : "Disabled: No auto-replies will be sent");
        masterStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        masterStatusText.setTextColor(0xFFE0F2FE);
        masterStatusText.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        textLayout.addView(masterStatusText);

        card.addView(textLayout);

        masterSwitch = new Switch(context);
        masterSwitch.setChecked(isEnabled);
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NayaConfig.getInstance().setSmartAutoReply(isChecked);
            AutoReplyManager.getInstance().setAutoReplyEnabled(isChecked);
            masterStatusText.setText(isChecked ? "Active: Automatically replying to 1:1 chats" : "Disabled: No auto-replies will be sent");
            BulletinFactory.of(this).createSimpleBulletin(
                    isChecked ? R.raw.done : R.raw.chats_infotip,
                    "Smart Auto-Reply: " + (isChecked ? "Enabled" : "Disabled")
            ).show();
        });
        card.addView(masterSwitch);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = AndroidUtilities.dp(16);
        card.setLayoutParams(cardParams);
        return card;
    }

    private View createDefaultMessageCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        card.setBackground(bg);

        TextView header = new TextView(context);
        header.setText("Default Away Message");
        header.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        header.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        header.setTypeface(AndroidUtilities.bold());
        card.addView(header);

        TextView sub = new TextView(context);
        sub.setText("Sent once per hour when no specific keyword rule matches");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sub.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(10));
        card.addView(sub);

        defaultMessagePreview = new TextView(context);
        defaultMessagePreview.setText(NayaConfig.getInstance().getAutoReplyText());
        defaultMessagePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        defaultMessagePreview.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        defaultMessagePreview.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        GradientDrawable previewBg = new GradientDrawable();
        previewBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        previewBg.setCornerRadius(AndroidUtilities.dp(10));
        defaultMessagePreview.setBackground(previewBg);
        card.addView(defaultMessagePreview);

        TextView btnEdit = new TextView(context);
        btnEdit.setText("✏️ Edit Away Message & Formatting");
        btnEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnEdit.setTextColor(0xFF0284C7);
        btnEdit.setTypeface(AndroidUtilities.bold());
        btnEdit.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(4));
        btnEdit.setOnClickListener(v -> showEditDefaultMessageDialog());
        card.addView(btnEdit);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = AndroidUtilities.dp(18);
        card.setLayoutParams(cardParams);
        return card;
    }

    private View createSearchAndFilterSection(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, AndroidUtilities.dp(14));

        // Search Input
        EditText etSearch = new EditText(context);
        etSearch.setHint("🔍 Search rule by keyword or name...");
        etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etSearch.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        searchBg.setCornerRadius(AndroidUtilities.dp(12));
        searchBg.setStroke(AndroidUtilities.dp(1), 0x26000000);
        etSearch.setBackground(searchBg);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentSearchQuery = s != null ? s.toString().trim().toLowerCase() : "";
                refreshRulesList(context);
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        layout.addView(etSearch);

        // Filter Chips
        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(context);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, AndroidUtilities.dp(8), 0, 0);

        addCategoryChip(chips, "All Rules", "ALL", context);
        addCategoryChip(chips, "🛒 Pricing & Orders", "SHOP", context);
        addCategoryChip(chips, "👋 Greetings", "GREET", context);
        addCategoryChip(chips, "💳 Payments (bKash/Nagad)", "PAY", context);

        hsv.addView(chips);
        layout.addView(hsv);

        return layout;
    }

    private void addCategoryChip(LinearLayout parent, String label, final String cat, final Context context) {
        TextView chip = new TextView(parent.getContext());
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        boolean isSelected = currentCategoryFilter.equals(cat);
        chip.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF0284C7);
        chip.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isSelected ? 0xFF0284C7 : 0x1A0284C7);
        bg.setCornerRadius(AndroidUtilities.dp(10));
        chip.setBackground(bg);
        chip.setOnClickListener(v -> {
            currentCategoryFilter = cat;
            refreshRulesList(context);
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.rightMargin = AndroidUtilities.dp(8);
        parent.addView(chip, params);
    }

    private View createSectionHeader(Context context, String title, String subtitle) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(AndroidUtilities.dp(4), 0, AndroidUtilities.dp(4), AndroidUtilities.dp(8));

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        tvTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        tvTitle.setTypeface(AndroidUtilities.bold());
        layout.addView(tvTitle);

        TextView tvSub = new TextView(context);
        tvSub.setText(subtitle);
        tvSub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        layout.addView(tvSub);

        return layout;
    }

    private View createActionsCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(0, AndroidUtilities.dp(16), 0, 0);

        TextView btnAdd = new TextView(context);
        btnAdd.setText("+ Add Custom Rule");
        btnAdd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnAdd.setTextColor(0xFFFFFFFF);
        btnAdd.setTypeface(AndroidUtilities.bold());
        btnAdd.setGravity(Gravity.CENTER);
        btnAdd.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(0xFF10B981);
        addBg.setCornerRadius(AndroidUtilities.dp(12));
        btnAdd.setBackground(addBg);
        btnAdd.setOnClickListener(v -> showAddEditRuleDialog(-1, null));
        card.addView(btnAdd);

        View space = new View(context);
        card.addView(space, new LinearLayout.LayoutParams(AndroidUtilities.dp(12), 1));

        TextView btnReset = new TextView(context);
        btnReset.setText("🔄 Restore 15 Rules");
        btnReset.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnReset.setTextColor(0xFF0284C7);
        btnReset.setTypeface(AndroidUtilities.bold());
        btnReset.setGravity(Gravity.CENTER);
        btnReset.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        GradientDrawable resetBg = new GradientDrawable();
        resetBg.setColor(0x1A0284C7);
        resetBg.setCornerRadius(AndroidUtilities.dp(12));
        btnReset.setBackground(resetBg);
        btnReset.setOnClickListener(v -> {
            AutoReplyManager.getInstance().seedDefaultRules();
            refreshRulesList(context);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Default 15 rules restored!").show();
        });
        card.addView(btnReset);

        return card;
    }

    private void refreshRulesList(Context context) {
        if (rulesContainer == null) {
            return;
        }
        rulesContainer.removeAllViews();
        List<AutoReplyManager.AutoReplyRule> allRules = AutoReplyManager.getInstance().getRules();

        for (int i = 0; i < allRules.size(); i++) {
            final int index = i;
            final AutoReplyManager.AutoReplyRule rule = allRules.get(i);

            // Search filter
            if (!currentSearchQuery.isEmpty()) {
                String kw = rule.keyword != null ? rule.keyword.toLowerCase() : "";
                String nm = rule.name != null ? rule.name.toLowerCase() : "";
                if (!kw.contains(currentSearchQuery) && !nm.contains(currentSearchQuery)) {
                    continue;
                }
            }

            // Category filter
            if ("SHOP".equals(currentCategoryFilter)) {
                String kw = rule.keyword != null ? rule.keyword.toLowerCase() : "";
                if (!kw.contains("price") && !kw.contains("dam") && !kw.contains("order") && !kw.contains("delivery") && !kw.contains("location") && !kw.contains("address")) {
                    continue;
                }
            } else if ("GREET".equals(currentCategoryFilter)) {
                String kw = rule.keyword != null ? rule.keyword.toLowerCase() : "";
                if (!kw.contains("hi") && !kw.contains("hello") && !kw.contains("assalamu") && !kw.contains("salam") && !kw.contains("bye") && !kw.contains("thanks") && !kw.contains("ok")) {
                    continue;
                }
            } else if ("PAY".equals(currentCategoryFilter)) {
                String kw = rule.keyword != null ? rule.keyword.toLowerCase() : "";
                if (!kw.contains("bkash") && !kw.contains("nagad")) {
                    continue;
                }
            }

            LinearLayout ruleCard = new LinearLayout(context);
            ruleCard.setOrientation(LinearLayout.HORIZONTAL);
            ruleCard.setGravity(Gravity.CENTER_VERTICAL);
            int pad = AndroidUtilities.dp(12);
            ruleCard.setPadding(pad, pad, pad, pad);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cardBg.setCornerRadius(AndroidUtilities.dp(14));
            ruleCard.setBackground(cardBg);

            LinearLayout leftInfo = new LinearLayout(context);
            leftInfo.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams liParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            leftInfo.setLayoutParams(liParams);

            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView kwPill = new TextView(context);
            kwPill.setText("[" + rule.keyword + "]");
            kwPill.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            kwPill.setTextColor(0xFF0284C7);
            kwPill.setTypeface(AndroidUtilities.bold());
            kwPill.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(2), AndroidUtilities.dp(8), AndroidUtilities.dp(2));
            GradientDrawable pillBg = new GradientDrawable();
            pillBg.setColor(0x1F0284C7);
            pillBg.setCornerRadius(AndroidUtilities.dp(8));
            kwPill.setBackground(pillBg);
            topRow.addView(kwPill);

            TextView nameText = new TextView(context);
            nameText.setText(rule.name);
            nameText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            nameText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            nameText.setTypeface(AndroidUtilities.bold());
            nameText.setPadding(AndroidUtilities.dp(8), 0, 0, 0);
            nameText.setSingleLine(true);
            nameText.setEllipsize(TextUtils.TruncateAt.END);
            topRow.addView(nameText);

            leftInfo.addView(topRow);

            TextView respText = new TextView(context);
            respText.setText(rule.response);
            respText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            respText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            respText.setPadding(0, AndroidUtilities.dp(4), 0, 0);
            respText.setMaxLines(2);
            respText.setEllipsize(TextUtils.TruncateAt.END);
            leftInfo.addView(respText);

            ruleCard.addView(leftInfo);

            // Rule Switch
            Switch ruleSwitch = new Switch(context);
            ruleSwitch.setChecked(rule.enabled);
            ruleSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
                AutoReplyManager.getInstance().setRuleEnabled(index, isChecked);
            });
            ruleCard.addView(ruleSwitch);

            // Tap card to edit rule
            ruleCard.setOnClickListener(v -> showAddEditRuleDialog(index, rule));

            LinearLayout.LayoutParams rParams = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            rParams.bottomMargin = AndroidUtilities.dp(8);
            rulesContainer.addView(ruleCard, rParams);
        }
    }

    private void showEditDefaultMessageDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Default Away Message");

        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(16);
        layout.setPadding(pad, pad, pad, pad);

        final EditText et = new EditText(getParentActivity());
        et.setText(NayaConfig.getInstance().getAutoReplyText());
        et.setSelection(et.getText().length());
        et.setMinLines(3);
        layout.addView(et);

        // Markdown helper chips
        HorizontalScrollView hsv = new HorizontalScrollView(getParentActivity());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(getParentActivity());
        chips.setOrientation(LinearLayout.HORIZONTAL);

        addFormatChip(chips, "**Bold**", () -> insertFormatting(et, "**", "**"));
        addFormatChip(chips, "_Italic_", () -> insertFormatting(et, "_", "_"));
        addFormatChip(chips, "`Mono`", () -> insertFormatting(et, "`", "`"));
        addFormatChip(chips, "||Spoiler||", () -> insertFormatting(et, "||", "||"));
        addFormatChip(chips, "[Link](url)", () -> insertFormatting(et, "[Title](", "https://url)"));
        addFormatChip(chips, "> Quote", () -> insertFormatting(et, "> ", ""));

        hsv.addView(chips);
        layout.addView(hsv);

        b.setView(layout);
        b.setPositiveButton("Save", (d, w) -> {
            String val = et.getText().toString().trim();
            if (!val.isEmpty()) {
                NayaConfig.getInstance().setAutoReplyText(val);
                if (defaultMessagePreview != null) {
                    defaultMessagePreview.setText(val);
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Away message updated!").show();
            }
        });
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
    }

    private void showAddEditRuleDialog(final int index, final AutoReplyManager.AutoReplyRule existing) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle(existing == null ? "Add Custom Rule" : "Edit Keyword Rule");

        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(16);
        layout.setPadding(pad, pad, pad, pad);

        final EditText etName = new EditText(getParentActivity());
        etName.setHint("Rule Name (e.g. Price Query, bKash Info)");
        if (existing != null) etName.setText(existing.name);
        layout.addView(etName);

        final EditText etKw = new EditText(getParentActivity());
        etKw.setHint("Keyword to match (e.g. price, dam, bkash, hi)");
        if (existing != null) etKw.setText(existing.keyword);
        layout.addView(etKw);

        final EditText etResp = new EditText(getParentActivity());
        etResp.setHint("Auto-Reply Response Message");
        if (existing != null) etResp.setText(existing.response);
        etResp.setMinLines(3);
        layout.addView(etResp);

        // Markdown helper chips for rule response
        HorizontalScrollView hsv = new HorizontalScrollView(getParentActivity());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(getParentActivity());
        chips.setOrientation(LinearLayout.HORIZONTAL);

        addFormatChip(chips, "**Bold**", () -> insertFormatting(etResp, "**", "**"));
        addFormatChip(chips, "_Italic_", () -> insertFormatting(etResp, "_", "_"));
        addFormatChip(chips, "`Mono`", () -> insertFormatting(etResp, "`", "`"));
        addFormatChip(chips, "||Spoiler||", () -> insertFormatting(etResp, "||", "||"));
        addFormatChip(chips, "[Link](url)", () -> insertFormatting(etResp, "[Title](", "https://url)"));
        addFormatChip(chips, "> Quote", () -> insertFormatting(etResp, "> ", ""));

        hsv.addView(chips);
        layout.addView(hsv);

        b.setView(layout);
        b.setPositiveButton("Save", (d, w) -> {
            String name = etName.getText().toString().trim();
            String kw = etKw.getText().toString().trim();
            String resp = etResp.getText().toString().trim();
            if (!name.isEmpty() && !kw.isEmpty() && !resp.isEmpty()) {
                AutoReplyManager.AutoReplyRule newRule = new AutoReplyManager.AutoReplyRule(name, kw, resp);
                if (existing != null && index >= 0) {
                    AutoReplyManager.getInstance().updateRule(index, newRule);
                } else {
                    AutoReplyManager.getInstance().addRule(newRule);
                }
                refreshRulesList(getParentActivity());
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Rule saved successfully!").show();
            }
        });

        if (existing != null && index >= 0) {
            b.setNeutralButton("Delete", (d, w) -> {
                AutoReplyManager.getInstance().deleteRule(index);
                refreshRulesList(getParentActivity());
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Rule deleted!").show();
            });
        }
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
    }

    private void addFormatChip(LinearLayout parent, String label, Runnable action) {
        TextView chip = new TextView(parent.getContext());
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        chip.setTextColor(0xFF0284C7);
        chip.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x1A0284C7);
        bg.setCornerRadius(AndroidUtilities.dp(8));
        chip.setBackground(bg);
        chip.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.rightMargin = AndroidUtilities.dp(6);
        params.topMargin = AndroidUtilities.dp(8);
        parent.addView(chip, params);
    }

    private void insertFormatting(EditText et, String prefix, String suffix) {
        int start = Math.max(et.getSelectionStart(), 0);
        int end = Math.max(et.getSelectionEnd(), 0);
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        String text = et.getText().toString();
        String selected = text.substring(start, end);
        String replacement = prefix + selected + suffix;
        et.getText().replace(start, end, replacement);
        et.setSelection(start + prefix.length() + selected.length());
    }

    private View createStealthPresenceCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        card.setBackground(bg);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.topMargin = AndroidUtilities.dp(12);
        card.setLayoutParams(cardParams);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(context);
        title.setText("Stealth Ghost Presence (অদৃশ্য উপস্থিতি)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        textLayout.addView(title);

        TextView sub = new TextView(context);
        sub.setText("Keep Offline: অটো-রিপ্লাই গেলেও উপস্থিত দেখাবে না");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setTextColor(0xFF0284C7);
        textLayout.addView(sub);

        row.addView(textLayout);

        Switch stealthSwitch = new Switch(context);
        stealthSwitch.setChecked(NayaConfig.getInstance().isAutoReplyStealthMode());
        stealthSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            NayaConfig.getInstance().setAutoReplyStealthMode(isChecked);
            if (getParentActivity() != null) {
                BulletinFactory.of(this).createSimpleBulletin(
                        R.drawable.msg_fave,
                        isChecked ? "Stealth Mode Active: Presence kept offline" : "Stealth Mode Disabled"
                ).show();
            }
        });
        row.addView(stealthSwitch);
        card.addView(row);

        TextView desc = new TextView(context);
        desc.setText("সিস্টেমের স্বয়ংক্রিয় রিপ্লাই যাওয়ার পর টেলিগ্রাম যাতে আপনাকে অনলাইন বা লাস্ট সিন পরিবর্তন না দেখায়, তার জন্য সার্ভার স্ট্যাটাস সুরক্ষিতভাবে অফলাইনে বজায় রাখা হয়।");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dParams.topMargin = AndroidUtilities.dp(8);
        card.addView(desc, dParams);

        return card;
    }

}