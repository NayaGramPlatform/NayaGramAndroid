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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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
 * Complete UI/UX studio for managing 16+ keyword rules, master switch,
 * search filter, categories, and Telegram native Text Style dialog.
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

        // 5. Container for Dynamic Rule Cards
        rulesContainer = new LinearLayout(context);
        rulesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(rulesContainer);

        // 6. Action Buttons (+ Add Rule, Restore Defaults)
        root.addView(createBottomActions(context));

        refreshRulesList(context);

        return fragmentView;
    }

    private View createMasterCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = AndroidUtilities.dp(16);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        bg.setColors(new int[]{0xFF0284C7, 0xFF0369A1});
        bg.setCornerRadius(AndroidUtilities.dp(16));
        card.setBackground(bg);

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

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

        LinearLayout btnRow = new LinearLayout(context);
        btnRow.setOrientation(LinearLayout.HORIZONTAL);
        btnRow.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(4));

        TextView btnEdit = new TextView(context);
        btnEdit.setText("✏️ Edit Away Message");
        btnEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        btnEdit.setTextColor(0xFF0284C7);
        btnEdit.setTypeface(AndroidUtilities.bold());
        btnEdit.setOnClickListener(v -> showEditDefaultMessageDialog());
        btnRow.addView(btnEdit);

        card.addView(btnRow);

        LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        cardParams.bottomMargin = AndroidUtilities.dp(16);
        card.setLayoutParams(cardParams);
        return card;
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
        cardParams.bottomMargin = AndroidUtilities.dp(16);
        card.setLayoutParams(cardParams);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(context);
        title.setText("Stealth Ghost Presence");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        textLayout.addView(title);

        TextView sub = new TextView(context);
        sub.setText("Keep Offline: Auto-replies will not show you online");
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
        desc.setText("Ensures Telegram preserves your original last-seen time and does not reveal you as online when automated replies are sent.");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dParams.topMargin = AndroidUtilities.dp(6);
        desc.setLayoutParams(dParams);
        card.addView(desc);

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
        addCategoryChip(chips, "👋 Greetings", "GREET", context);
        addCategoryChip(chips, "💼 Business & Inquiries", "BIZ", context);
        addCategoryChip(chips, "🤝 Support & FAQ", "SUPPORT", context);

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
        layout.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(10));

        TextView tvTitle = new TextView(context);
        tvTitle.setText(title);
        tvTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
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

    private void refreshRulesList(Context context) {
        if (rulesContainer == null) return;
        rulesContainer.removeAllViews();

        List<AutoReplyManager.AutoReplyRule> rules = AutoReplyManager.getInstance().getRules();
        int count = 0;

        for (int i = 0; i < rules.size(); i++) {
            final AutoReplyManager.AutoReplyRule rule = rules.get(i);
            final int index = i;

            if (!matchesFilters(rule)) {
                continue;
            }
            count++;

            LinearLayout card = new LinearLayout(context);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            int pad = AndroidUtilities.dp(14);
            card.setPadding(pad, pad, pad, pad);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cardBg.setCornerRadius(AndroidUtilities.dp(12));
            card.setBackground(cardBg);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            // Top row: Keyword chip + Rule name
            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            TextView chip = new TextView(context);
            chip.setText("[" + rule.keyword + "]");
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setTextColor(0xFF0284C7);
            chip.setTypeface(AndroidUtilities.bold());
            chip.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(0x1A0284C7);
            chipBg.setCornerRadius(AndroidUtilities.dp(6));
            chip.setBackground(chipBg);
            topRow.addView(chip);

            TextView name = new TextView(context);
            name.setText("  " + rule.name);
            name.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            name.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            name.setTypeface(AndroidUtilities.bold());
            name.setSingleLine(true);
            name.setEllipsize(TextUtils.TruncateAt.END);
            topRow.addView(name);

            textCol.addView(topRow);

            // Response Preview
            TextView resp = new TextView(context);
            resp.setText(rule.response);
            resp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            resp.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            resp.setPadding(0, AndroidUtilities.dp(4), 0, 0);
            resp.setMaxLines(2);
            resp.setEllipsize(TextUtils.TruncateAt.END);
            textCol.addView(resp);

            card.addView(textCol);

            // Toggle switch
            Switch sw = new Switch(context);
            sw.setChecked(rule.enabled);
            sw.setOnCheckedChangeListener((btn, isChecked) -> {
                rule.enabled = isChecked;
                AutoReplyManager.getInstance().updateRule(index, rule);
            });
            card.addView(sw);

            // Tap card to edit
            card.setOnClickListener(v -> showAddEditRuleDialog(index, rule));

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
            lp.bottomMargin = AndroidUtilities.dp(10);
            card.setLayoutParams(lp);

            rulesContainer.addView(card);
        }

        if (count == 0) {
            TextView empty = new TextView(context);
            empty.setText("No rules found matching criteria.");
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, AndroidUtilities.dp(24), 0, AndroidUtilities.dp(24));
            rulesContainer.addView(empty);
        }
    }

    private boolean matchesFilters(AutoReplyManager.AutoReplyRule rule) {
        if (rule == null) return false;
        if (!currentSearchQuery.isEmpty()) {
            boolean nameMatch = rule.name != null && rule.name.toLowerCase().contains(currentSearchQuery);
            boolean kwMatch = rule.keyword != null && rule.keyword.toLowerCase().contains(currentSearchQuery);
            boolean respMatch = rule.response != null && rule.response.toLowerCase().contains(currentSearchQuery);
            if (!nameMatch && !kwMatch && !respMatch) {
                return false;
            }
        }
        if ("ALL".equals(currentCategoryFilter)) {
            return true;
        }
        String kw = rule.keyword != null ? rule.keyword.toLowerCase() : "";
        if ("GREET".equals(currentCategoryFilter)) {
            return kw.equals("hi") || kw.equals("hello") || kw.equals("salam") || kw.equals("bye");
        } else if ("BIZ".equals(currentCategoryFilter)) {
            return kw.equals("price") || kw.equals("catalog") || kw.equals("order") || kw.equals("hours") || kw.equals("service");
        } else if ("SUPPORT".equals(currentCategoryFilter)) {
            return kw.equals("help") || kw.equals("location") || kw.equals("contact") || kw.equals("meeting") || kw.equals("thanks") || kw.equals("ok") || kw.equals("feedback");
        }
        return true;
    }

    private View createBottomActions(Context context) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(16));

        TextView btnAdd = new TextView(context);
        btnAdd.setText("+ Add Custom Rule");
        btnAdd.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnAdd.setTextColor(0xFFFFFFFF);
        btnAdd.setTypeface(AndroidUtilities.bold());
        btnAdd.setGravity(Gravity.CENTER);
        btnAdd.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        GradientDrawable addBg = new GradientDrawable();
        addBg.setColor(0xFF0284C7);
        addBg.setCornerRadius(AndroidUtilities.dp(10));
        btnAdd.setBackground(addBg);
        btnAdd.setOnClickListener(v -> showAddEditRuleDialog(-1, null));
        LinearLayout.LayoutParams addParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        addParams.rightMargin = AndroidUtilities.dp(8);
        btnAdd.setLayoutParams(addParams);
        row.addView(btnAdd);

        TextView btnRestore = new TextView(context);
        btnRestore.setText("🔄 Restore 16 Rules");
        btnRestore.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnRestore.setTextColor(0xFF0284C7);
        btnRestore.setTypeface(AndroidUtilities.bold());
        btnRestore.setGravity(Gravity.CENTER);
        btnRestore.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable resBg = new GradientDrawable();
        resBg.setColor(0x1A0284C7);
        resBg.setCornerRadius(AndroidUtilities.dp(10));
        btnRestore.setBackground(resBg);
        btnRestore.setOnClickListener(v -> {
            AutoReplyManager.getInstance().seedDefaultRules();
            refreshRulesList(context);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Default 16 rules restored!").show();
        });
        row.addView(btnRestore);

        return row;
    }

    private void showEditDefaultMessageDialog() {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Edit Default Away Message");

        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        final EditText et = new EditText(getParentActivity());
        et.setText(NayaConfig.getInstance().getAutoReplyText());
        et.setMinLines(3);
        et.setGravity(Gravity.TOP);
        layout.addView(et);

        TextView btnStyle = new TextView(getParentActivity());
        btnStyle.setText("🎨 Text Style (Telegram Formatting)");
        btnStyle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnStyle.setTextColor(0xFF0284C7);
        btnStyle.setTypeface(AndroidUtilities.bold());
        btnStyle.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(4));
        btnStyle.setOnClickListener(v -> showTextStyleDialog(et));
        layout.addView(btnStyle);

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
        b.setTitle(existing != null ? "Edit Rule" : "Add Custom Rule");

        LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        layout.setPadding(pad, pad, pad, pad);

        final EditText etName = new EditText(getParentActivity());
        etName.setHint("Rule Name (e.g. Price Query, Office Hours)");
        if (existing != null) etName.setText(existing.name);
        layout.addView(etName);

        final EditText etKw = new EditText(getParentActivity());
        etKw.setHint("Keyword to match (e.g. price, catalog, help, info)");
        if (existing != null) etKw.setText(existing.keyword);
        layout.addView(etKw);

        final EditText etResp = new EditText(getParentActivity());
        etResp.setHint("Auto-Reply Response Message");
        if (existing != null) etResp.setText(existing.response);
        etResp.setMinLines(3);
        layout.addView(etResp);

        TextView btnStyle = new TextView(getParentActivity());
        btnStyle.setText("🎨 Text Style (Telegram Formatting)");
        btnStyle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        btnStyle.setTextColor(0xFF0284C7);
        btnStyle.setTypeface(AndroidUtilities.bold());
        btnStyle.setPadding(0, AndroidUtilities.dp(10), 0, AndroidUtilities.dp(4));
        btnStyle.setOnClickListener(v -> showTextStyleDialog(etResp));
        layout.addView(btnStyle);

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

    /**
     * Native Telegram Text Style chooser dialog:
     * Regular, Spoiler, Bold, Italic, Mono, Strikethrough, Underline, Quote, Link
     * Complete with PC keyboard shortcut guide.
     */
    private void showTextStyleDialog(final EditText target) {
        if (getParentActivity() == null || target == null) return;

        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());

        LinearLayout root = new LinearLayout(getParentActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        root.setPadding(pad, pad, pad, AndroidUtilities.dp(14));

        TextView title = new TextView(getParentActivity());
        title.setText("Text style");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 19);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        root.addView(title);

        TextView sub = new TextView(getParentActivity());
        sub.setText("Select any style");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sub.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(12));
        root.addView(sub);

        final String[] styles = new String[]{
                "Regular",
                "Spoiler",
                "Bold",
                "Italic",
                "Mono",
                "Strikethrough",
                "Underline",
                "Quote",
                "Link"
        };

        final int[] selectedIndex = new int[]{2}; // Default Bold

        final RadioGroup radioGroup = new RadioGroup(getParentActivity());
        radioGroup.setOrientation(RadioGroup.VERTICAL);

        for (int i = 0; i < styles.length; i++) {
            RadioButton rb = new RadioButton(getParentActivity());
            rb.setText(styles[i]);
            rb.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
            rb.setId(i + 100);
            if (i == 2) {
                rb.setChecked(true);
            }
            if ("Bold".equals(styles[i])) {
                rb.setTextColor(0xFF0284C7);
                rb.setTypeface(AndroidUtilities.bold());
            } else if ("Italic".equals(styles[i])) {
                rb.setTextColor(0xFF7C3AED);
            } else if ("Spoiler".equals(styles[i])) {
                rb.setTextColor(0xFFD97706);
            } else if ("Mono".equals(styles[i])) {
                rb.setTextColor(0xFF0D9488);
            } else {
                rb.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
            }
            radioGroup.addView(rb);
        }

        radioGroup.setOnCheckedChangeListener((group, checkedId) -> selectedIndex[0] = checkedId - 100);
        root.addView(radioGroup);

        // Desktop / PC Keyboard Shortcuts Info Box
        LinearLayout guideBox = new LinearLayout(getParentActivity());
        guideBox.setOrientation(LinearLayout.VERTICAL);
        guideBox.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        GradientDrawable guideBg = new GradientDrawable();
        guideBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        guideBg.setCornerRadius(AndroidUtilities.dp(10));
        guideBox.setBackground(guideBg);

        LinearLayout.LayoutParams gParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gParams.topMargin = AndroidUtilities.dp(12);
        guideBox.setLayoutParams(gParams);

        TextView gTitle = new TextView(getParentActivity());
        gTitle.setText("💻 Desktop / PC Keyboard Shortcuts:");
        gTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        gTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        gTitle.setTypeface(AndroidUtilities.bold());
        guideBox.addView(gTitle);

        TextView gList = new TextView(getParentActivity());
        gList.setText("• Ctrl + B › Bold\n• Ctrl + I › Italic\n• Ctrl + U › Underline\n• Ctrl + Shift + M › Monospace\n• Ctrl + Shift + X › Strikethrough\n• Ctrl + Shift + P › Spoiler\n• Ctrl + Shift + > › Quote");
        gList.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        gList.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        gList.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        guideBox.addView(gList);

        root.addView(guideBox);

        b.setView(root);
        b.setPositiveButton("Select", (d, w) -> {
            int sel = selectedIndex[0];
            if (sel >= 0 && sel < styles.length) {
                applyStyleToEditText(target, styles[sel]);
            }
        });
        b.setNegativeButton("Close", null);
        showDialog(b.create());
    }

    private void applyStyleToEditText(EditText et, String style) {
        if (et == null) return;
        int start = Math.max(et.getSelectionStart(), 0);
        int end = Math.max(et.getSelectionEnd(), 0);
        if (start > end) {
            int tmp = start;
            start = end;
            end = tmp;
        }
        String text = et.getText().toString();
        String selected = text.substring(start, end);
        String replacement;

        if ("Regular".equals(style)) {
            // Strip markdown symbols
            replacement = selected.replaceAll("[*_~`>\\[\\]()]", "");
        } else if ("Bold".equals(style)) {
            replacement = "**" + (selected.isEmpty() ? "bold text" : selected) + "**";
        } else if ("Italic".equals(style)) {
            replacement = "__" + (selected.isEmpty() ? "italic text" : selected) + "__";
        } else if ("Mono".equals(style)) {
            replacement = "`" + (selected.isEmpty() ? "code" : selected) + "`";
        } else if ("Spoiler".equals(style)) {
            replacement = "||" + (selected.isEmpty() ? "spoiler" : selected) + "||";
        } else if ("Strikethrough".equals(style)) {
            replacement = "~~" + (selected.isEmpty() ? "strikethrough text" : selected) + "~~";
        } else if ("Underline".equals(style)) {
            replacement = "--" + (selected.isEmpty() ? "underlined text" : selected) + "--";
        } else if ("Quote".equals(style)) {
            replacement = "> " + (selected.isEmpty() ? "quote text" : selected);
        } else if ("Link".equals(style)) {
            replacement = "[" + (selected.isEmpty() ? "Title" : selected) + "](https://example.com)";
        } else {
            replacement = selected;
        }

        et.getText().replace(start, end, replacement);
        et.setSelection(start + replacement.length());
    }
}
