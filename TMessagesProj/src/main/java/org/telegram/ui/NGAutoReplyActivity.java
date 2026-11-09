package org.telegram.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.res.ColorStateList;
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
 * High-end, spacious and professional UI/UX for managing auto-reply rules.
 * Generous breathing room, vibrant colors (Emerald Green, Radiant Cyan, Royal Purple),
 * zero clutter, and native Telegram Text Style modal with PC shortcuts.
 */
public class NGAutoReplyActivity extends BaseFragment {

    private LinearLayout rulesContainer;
    private TextView masterStatusText;
    private Switch masterSwitch;
    private TextView defaultMessagePreview;
    private String currentSearchQuery = "";
    private String currentCategoryFilter = "ALL";

    // Vibrant Modern Color Palette
    private static final int COLOR_CYAN_PRIMARY = 0xFF0284C7;
    private static final int COLOR_CYAN_LIGHT = 0xFF00E5FF;
    private static final int COLOR_GREEN_PRIMARY = 0xFF059669;
    private static final int COLOR_GREEN_LIGHT = 0xFF00E676;
    private static final int COLOR_PURPLE_PRIMARY = 0xFF7C3AED;
    private static final int COLOR_PURPLE_LIGHT = 0xFFA855F7;
    private static final int COLOR_AMBER_PRIMARY = 0xFFD97706;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setTitle("Smart Auto-Reply Studio");
        actionBar.setSubtitle("16 Clean Rules & Instant Replies");
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
        int padH = AndroidUtilities.dp(18);
        int padV = AndroidUtilities.dp(20);
        root.setPadding(padH, padV, padH, AndroidUtilities.dp(48));
        scrollView.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Master Toggle Card (Spacious Radiant Cyan to Emerald Gradient)
        root.addView(createMasterCard(context));

        // 2. Default Away Message Card (Spacious white card with Emerald pill button)
        root.addView(createDefaultMessageCard(context));

        // 2.5 Stealth Ghost Presence Card (Spacious Royal Purple accent)
        root.addView(createStealthPresenceCard(context));

        // 3. Search Bar & Category Filter Chips (Airy, smooth pill chips)
        root.addView(createSearchAndFilterSection(context));

        // 4. Keyword Rules Section Header
        root.addView(createSectionHeader(context, "Instant Keyword Rules", "Matches incoming queries and replies immediately without cooldown"));

        // 5. Container for Dynamic Rule Cards
        rulesContainer = new LinearLayout(context);
        rulesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(rulesContainer);

        // 6. Action Buttons (+ Add Rule, Reset to Clean 16 Rules)
        root.addView(createBottomActions(context));

        refreshRulesList(context);

        return fragmentView;
    }

    private void applySwitchTint(Switch sw, int checkedThumb, int checkedTrack, int uncheckedThumb, int uncheckedTrack) {
        if (sw == null) return;
        int[][] states = new int[][]{
                new int[]{android.R.attr.state_checked},
                new int[]{-android.R.attr.state_checked}
        };
        int[] thumbColors = new int[]{checkedThumb, uncheckedThumb};
        int[] trackColors = new int[]{checkedTrack, uncheckedTrack};
        sw.setThumbTintList(new ColorStateList(states, thumbColors));
        sw.setTrackTintList(new ColorStateList(states, trackColors));
    }

    private View createMasterCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.HORIZONTAL);
        card.setGravity(Gravity.CENTER_VERTICAL);
        int pad = AndroidUtilities.dp(20);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        bg.setColors(new int[]{0xFF0284C7, 0xFF059669}); // Cyan to Emerald Green
        bg.setCornerRadius(AndroidUtilities.dp(18));
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
        masterStatusText.setText(isEnabled ? "Active: Automatically replying to 1:1 private chats" : "Disabled: No auto-replies will be sent");
        masterStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        masterStatusText.setTextColor(0xFFE0F2FE);
        masterStatusText.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        textLayout.addView(masterStatusText);

        card.addView(textLayout);

        masterSwitch = new Switch(context);
        masterSwitch.setChecked(isEnabled);
        applySwitchTint(masterSwitch, COLOR_CYAN_LIGHT, 0x9900E5FF, 0xFFE2E8F0, 0x66CBD5E1);
        masterSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            NayaConfig.getInstance().setSmartAutoReply(isChecked);
            AutoReplyManager.getInstance().setAutoReplyEnabled(isChecked);
            masterStatusText.setText(isChecked ? "Active: Automatically replying to 1:1 private chats" : "Disabled: No auto-replies will be sent");
            BulletinFactory.of(this).createSimpleBulletin(
                    isChecked ? R.raw.done : R.raw.chats_infotip,
                    "Smart Auto-Reply: " + (isChecked ? "Enabled" : "Disabled")
            ).show();
        });
        card.addView(masterSwitch);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = AndroidUtilities.dp(16);
        card.setLayoutParams(lp);

        return card;
    }

    private View createDefaultMessageCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(18));
        bg.setStroke(AndroidUtilities.dp(1.5f), 0x260284C7);
        card.setBackground(bg);

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(context);
        title.setText("Default Away Message");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        textLayout.addView(title);

        TextView sub = new TextView(context);
        sub.setText("Sent once per hour when no specific keyword rule matches");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sub.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        textLayout.addView(sub);

        topRow.addView(textLayout);
        card.addView(topRow);

        // Preview Box with comfortable padding
        LinearLayout previewBox = new LinearLayout(context);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        previewBox.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable pBg = new GradientDrawable();
        pBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        pBg.setCornerRadius(AndroidUtilities.dp(12));
        previewBox.setBackground(pBg);

        LinearLayout.LayoutParams pParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        pParams.topMargin = AndroidUtilities.dp(12);
        previewBox.setLayoutParams(pParams);

        defaultMessagePreview = new TextView(context);
        defaultMessagePreview.setText(NayaConfig.getInstance().getAutoReplyText());
        defaultMessagePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        defaultMessagePreview.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        defaultMessagePreview.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        previewBox.addView(defaultMessagePreview);

        card.addView(previewBox);

        // Vibrant Emerald Action Pill Button
        LinearLayout editBtn = new LinearLayout(context);
        editBtn.setOrientation(LinearLayout.HORIZONTAL);
        editBtn.setGravity(Gravity.CENTER);
        editBtn.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(12), AndroidUtilities.dp(18), AndroidUtilities.dp(12));
        GradientDrawable btnBg = new GradientDrawable();
        btnBg.setColor(COLOR_GREEN_PRIMARY);
        btnBg.setCornerRadius(AndroidUtilities.dp(12));
        editBtn.setBackground(btnBg);

        TextView editLabel = new TextView(context);
        editLabel.setText("✏️ Edit Away Message & Formatting");
        editLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        editLabel.setTextColor(0xFFFFFFFF);
        editLabel.setTypeface(AndroidUtilities.bold());
        editBtn.addView(editLabel);

        LinearLayout.LayoutParams bParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bParams.topMargin = AndroidUtilities.dp(14);
        editBtn.setLayoutParams(bParams);
        editBtn.setOnClickListener(v -> showEditAwayMessageDialog());

        card.addView(editBtn);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = AndroidUtilities.dp(16);
        card.setLayoutParams(lp);

        return card;
    }

    private View createStealthPresenceCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(18));
        bg.setStroke(AndroidUtilities.dp(1.5f), 0x337C3AED); // Royal Purple Border
        card.setBackground(bg);

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
        sub.setTextColor(COLOR_PURPLE_PRIMARY);
        sub.setPadding(0, AndroidUtilities.dp(2), 0, 0);
        textLayout.addView(sub);

        row.addView(textLayout);

        Switch stealthSwitch = new Switch(context);
        stealthSwitch.setChecked(NayaConfig.getInstance().isAutoReplyStealthMode());
        applySwitchTint(stealthSwitch, COLOR_PURPLE_LIGHT, 0x99A855F7, 0xFFE2E8F0, 0x66CBD5E1);
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
        desc.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        LinearLayout.LayoutParams dParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dParams.topMargin = AndroidUtilities.dp(8);
        desc.setLayoutParams(dParams);
        card.addView(desc);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.bottomMargin = AndroidUtilities.dp(16);
        card.setLayoutParams(lp);

        return card;
    }

    private View createSearchAndFilterSection(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, AndroidUtilities.dp(16));

        // Search Input with clean rounded border
        EditText etSearch = new EditText(context);
        etSearch.setHint("🔍 Search rule by keyword or name...");
        etSearch.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        etSearch.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        GradientDrawable searchBg = new GradientDrawable();
        searchBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        searchBg.setCornerRadius(AndroidUtilities.dp(14));
        searchBg.setStroke(AndroidUtilities.dp(1.5f), 0x260284C7);
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

        // Filter Chips with vibrant colors and generous touch targets
        HorizontalScrollView hsv = new HorizontalScrollView(context);
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout chips = new LinearLayout(context);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(0, AndroidUtilities.dp(10), 0, 0);

        addCategoryChip(chips, "All (16)", "ALL", COLOR_CYAN_PRIMARY, context);
        addCategoryChip(chips, "👋 Greetings (4)", "GREET", COLOR_GREEN_PRIMARY, context);
        addCategoryChip(chips, "💼 Business & Inquiries (5)", "BIZ", COLOR_PURPLE_PRIMARY, context);
        addCategoryChip(chips, "🤝 Support & FAQ (7)", "SUPPORT", COLOR_AMBER_PRIMARY, context);

        hsv.addView(chips);
        layout.addView(hsv);

        return layout;
    }

    private void addCategoryChip(LinearLayout parent, String label, final String cat, int activeColor, final Context context) {
        TextView chip = new TextView(parent.getContext());
        chip.setText(label);
        chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        boolean isSelected = currentCategoryFilter.equals(cat);
        chip.setTextColor(isSelected ? 0xFFFFFFFF : activeColor);
        chip.setTypeface(isSelected ? AndroidUtilities.bold() : null);
        chip.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(isSelected ? activeColor : 0x1A000000 | (activeColor & 0x00FFFFFF));
        bg.setCornerRadius(AndroidUtilities.dp(14));
        chip.setBackground(bg);
        chip.setOnClickListener(v -> {
            currentCategoryFilter = cat;
            refreshRulesList(context);
        });

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        params.rightMargin = AndroidUtilities.dp(10);
        parent.addView(chip, params);
    }

    private View createSectionHeader(Context context, String title, String subtitle) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, AndroidUtilities.dp(6), 0, AndroidUtilities.dp(12));

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
        tvSub.setPadding(0, AndroidUtilities.dp(2), 0, 0);
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
            int pad = AndroidUtilities.dp(16);
            card.setPadding(pad, pad, pad, pad);

            GradientDrawable cardBg = new GradientDrawable();
            cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
            cardBg.setCornerRadius(AndroidUtilities.dp(16));
            cardBg.setStroke(AndroidUtilities.dp(1), 0x1A000000);
            card.setBackground(cardBg);

            LinearLayout textCol = new LinearLayout(context);
            textCol.setOrientation(LinearLayout.VERTICAL);
            textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

            // Top row: Keyword chip + Rule name
            LinearLayout topRow = new LinearLayout(context);
            topRow.setOrientation(LinearLayout.HORIZONTAL);
            topRow.setGravity(Gravity.CENTER_VERTICAL);

            int chipColor = getRuleChipColor(rule.keyword);

            TextView chip = new TextView(context);
            chip.setText("[" + rule.keyword + "]");
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
            chip.setTextColor(chipColor);
            chip.setTypeface(AndroidUtilities.bold());
            chip.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(3), AndroidUtilities.dp(8), AndroidUtilities.dp(3));
            GradientDrawable chipBg = new GradientDrawable();
            chipBg.setColor(0x1F000000 | (chipColor & 0x00FFFFFF));
            chipBg.setCornerRadius(AndroidUtilities.dp(8));
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

            // Response Preview with airy line spacing
            TextView resp = new TextView(context);
            resp.setText(rule.response);
            resp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            resp.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            resp.setPadding(0, AndroidUtilities.dp(6), 0, 0);
            resp.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
            resp.setMaxLines(2);
            resp.setEllipsize(TextUtils.TruncateAt.END);
            textCol.addView(resp);

            card.addView(textCol);

            // Vibrant Emerald Green Toggle switch
            Switch sw = new Switch(context);
            sw.setChecked(rule.enabled);
            applySwitchTint(sw, COLOR_GREEN_LIGHT, 0x9900E676, 0xFFE2E8F0, 0x66CBD5E1);
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
            lp.bottomMargin = AndroidUtilities.dp(12);
            card.setLayoutParams(lp);

            rulesContainer.addView(card);
        }

        if (count == 0) {
            TextView empty = new TextView(context);
            empty.setText("No rules found matching criteria.");
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, AndroidUtilities.dp(28), 0, AndroidUtilities.dp(28));
            rulesContainer.addView(empty);
        }
    }

    private int getRuleChipColor(String keyword) {
        if (keyword == null) return COLOR_CYAN_PRIMARY;
        String kw = keyword.toLowerCase();
        if (kw.equals("hello") || kw.equals("hi") || kw.equals("salam") || kw.equals("bye")) {
            return COLOR_GREEN_PRIMARY;
        } else if (kw.equals("hours") || kw.equals("price") || kw.equals("catalog") || kw.equals("order") || kw.equals("service")) {
            return COLOR_PURPLE_PRIMARY;
        } else if (kw.equals("help") || kw.equals("location") || kw.equals("contact") || kw.equals("meeting") || kw.equals("thanks") || kw.equals("ok") || kw.equals("feedback")) {
            return COLOR_AMBER_PRIMARY;
        }
        return COLOR_CYAN_PRIMARY;
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
            return kw.equals("hello") || kw.equals("hi") || kw.equals("salam") || kw.equals("bye");
        } else if ("BIZ".equals(currentCategoryFilter)) {
            return kw.equals("hours") || kw.equals("price") || kw.equals("catalog") || kw.equals("order") || kw.equals("service");
        } else if ("SUPPORT".equals(currentCategoryFilter)) {
            return kw.equals("help") || kw.equals("location") || kw.equals("contact") || kw.equals("meeting") || kw.equals("thanks") || kw.equals("ok") || kw.equals("feedback");
        }
        return true;
    }

    private View createBottomActions(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, AndroidUtilities.dp(16), 0, AndroidUtilities.dp(16));

        // + Add Custom Rule Button (Cyan-to-Emerald Gradient)
        LinearLayout addBtn = new LinearLayout(context);
        addBtn.setOrientation(LinearLayout.HORIZONTAL);
        addBtn.setGravity(Gravity.CENTER);
        addBtn.setPadding(AndroidUtilities.dp(18), AndroidUtilities.dp(14), AndroidUtilities.dp(18), AndroidUtilities.dp(14));
        GradientDrawable addBg = new GradientDrawable();
        addBg.setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        addBg.setColors(new int[]{COLOR_CYAN_PRIMARY, COLOR_GREEN_PRIMARY});
        addBg.setCornerRadius(AndroidUtilities.dp(14));
        addBtn.setBackground(addBg);

        TextView addText = new TextView(context);
        addText.setText("+ Add Custom Keyword Rule");
        addText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        addText.setTextColor(0xFFFFFFFF);
        addText.setTypeface(AndroidUtilities.bold());
        addBtn.addView(addText);
        addBtn.setOnClickListener(v -> showAddEditRuleDialog(-1, null));
        layout.addView(addBtn);

        // Reset to 16 Clean Rules Pill Button
        LinearLayout resetBtn = new LinearLayout(context);
        resetBtn.setOrientation(LinearLayout.HORIZONTAL);
        resetBtn.setGravity(Gravity.CENTER);
        resetBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(12));
        GradientDrawable rBg = new GradientDrawable();
        rBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        rBg.setCornerRadius(AndroidUtilities.dp(14));
        rBg.setStroke(AndroidUtilities.dp(1.5f), COLOR_PURPLE_PRIMARY);
        resetBtn.setBackground(rBg);

        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.topMargin = AndroidUtilities.dp(12);
        resetBtn.setLayoutParams(rLp);

        TextView resetText = new TextView(context);
        resetText.setText("🔄 Reset & Purge Legacy Rules (Clean 16 Universal Rules)");
        resetText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        resetText.setTextColor(COLOR_PURPLE_PRIMARY);
        resetText.setTypeface(AndroidUtilities.bold());
        resetBtn.addView(resetText);

        resetBtn.setOnClickListener(v -> {
            AutoReplyManager.getInstance().seedDefaultRules();
            refreshRulesList(context);
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Reset to 16 Clean Rules!").show();
        });
        layout.addView(resetBtn);

        return layout;
    }

    private void showEditAwayMessageDialog() {
        if (getParentActivity() == null) return;

        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Default Away Message");

        LinearLayout root = new LinearLayout(getParentActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        root.setPadding(pad, pad, pad, pad);

        final EditText etMessage = new EditText(getParentActivity());
        etMessage.setText(NayaConfig.getInstance().getAutoReplyText());
        etMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etMessage.setMinLines(3);
        etMessage.setMaxLines(7);
        etMessage.setGravity(Gravity.TOP | Gravity.START);
        etMessage.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        bg.setCornerRadius(AndroidUtilities.dp(12));
        etMessage.setBackground(bg);
        root.addView(etMessage);

        // Formatting toolbar button
        TextView formatBtn = new TextView(getParentActivity());
        formatBtn.setText("✨ Insert Text Style (Bold, Italic, Spoiler, Quote...)");
        formatBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        formatBtn.setTextColor(COLOR_CYAN_PRIMARY);
        formatBtn.setTypeface(AndroidUtilities.bold());
        formatBtn.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(8));
        formatBtn.setOnClickListener(v -> showTextStyleDialog(etMessage));
        root.addView(formatBtn);

        b.setView(root);
        b.setPositiveButton("Save", (d, w) -> {
            String text = etMessage.getText().toString().trim();
            if (!text.isEmpty()) {
                NayaConfig.getInstance().setAutoReplyText(text);
                if (defaultMessagePreview != null) {
                    defaultMessagePreview.setText(text);
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
        b.setTitle(existing == null ? "Add Keyword Rule" : "Edit Keyword Rule");

        LinearLayout root = new LinearLayout(getParentActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView tvName = new TextView(getParentActivity());
        tvName.setText("Rule Display Name");
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvName.setTextColor(COLOR_CYAN_PRIMARY);
        tvName.setTypeface(AndroidUtilities.bold());
        root.addView(tvName);

        final EditText etName = new EditText(getParentActivity());
        etName.setHint("e.g., Pricing Inquiry");
        etName.setText(existing != null ? existing.name : "");
        etName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etName.setSingleLine(true);
        etName.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        GradientDrawable bgName = new GradientDrawable();
        bgName.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        bgName.setCornerRadius(AndroidUtilities.dp(10));
        etName.setBackground(bgName);
        root.addView(etName);

        TextView tvKw = new TextView(getParentActivity());
        tvKw.setText("Trigger Keyword");
        tvKw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvKw.setTextColor(COLOR_PURPLE_PRIMARY);
        tvKw.setTypeface(AndroidUtilities.bold());
        tvKw.setPadding(0, AndroidUtilities.dp(14), 0, 0);
        root.addView(tvKw);

        final EditText etKw = new EditText(getParentActivity());
        etKw.setHint("e.g., price (case-insensitive)");
        etKw.setText(existing != null ? existing.keyword : "");
        etKw.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etKw.setSingleLine(true);
        etKw.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        GradientDrawable bgKw = new GradientDrawable();
        bgKw.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        bgKw.setCornerRadius(AndroidUtilities.dp(10));
        etKw.setBackground(bgKw);
        root.addView(etKw);

        TextView tvResp = new TextView(getParentActivity());
        tvResp.setText("Auto-Reply Message");
        tvResp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvResp.setTextColor(COLOR_GREEN_PRIMARY);
        tvResp.setTypeface(AndroidUtilities.bold());
        tvResp.setPadding(0, AndroidUtilities.dp(14), 0, 0);
        root.addView(tvResp);

        final EditText etResp = new EditText(getParentActivity());
        etResp.setHint("Write the response text...");
        etResp.setText(existing != null ? existing.response : "");
        etResp.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etResp.setMinLines(3);
        etResp.setMaxLines(6);
        etResp.setGravity(Gravity.TOP | Gravity.START);
        etResp.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));
        GradientDrawable bgResp = new GradientDrawable();
        bgResp.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        bgResp.setCornerRadius(AndroidUtilities.dp(10));
        etResp.setBackground(bgResp);
        root.addView(etResp);

        TextView formatBtn = new TextView(getParentActivity());
        formatBtn.setText("✨ Insert Text Style (Bold, Italic, Spoiler, Quote...)");
        formatBtn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        formatBtn.setTextColor(COLOR_CYAN_PRIMARY);
        formatBtn.setTypeface(AndroidUtilities.bold());
        formatBtn.setPadding(0, AndroidUtilities.dp(10), 0, 0);
        formatBtn.setOnClickListener(v -> showTextStyleDialog(etResp));
        root.addView(formatBtn);

        b.setView(root);
        b.setPositiveButton("Save", (d, w) -> {
            String name = etName.getText().toString().trim();
            String kw = etKw.getText().toString().trim();
            String resp = etResp.getText().toString().trim();
            if (!name.isEmpty() && !kw.isEmpty() && !resp.isEmpty()) {
                if (existing != null && index >= 0) {
                    existing.name = name;
                    existing.keyword = kw;
                    existing.response = resp;
                    AutoReplyManager.getInstance().updateRule(index, existing);
                } else {
                    AutoReplyManager.getInstance().addRule(new AutoReplyManager.AutoReplyRule(name, kw, resp));
                }
                refreshRulesList(getParentActivity());
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Rule saved!").show();
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
        int pad = AndroidUtilities.dp(22);
        root.setPadding(pad, pad, pad, AndroidUtilities.dp(16));

        TextView title = new TextView(getParentActivity());
        title.setText("Text style");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        root.addView(title);

        TextView sub = new TextView(getParentActivity());
        sub.setText("Select any style to format message text");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sub.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(14));
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
            rb.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(6), AndroidUtilities.dp(8), AndroidUtilities.dp(6));
            rb.setId(i + 100);
            if (i == 2) {
                rb.setChecked(true);
            }
            if ("Bold".equals(styles[i])) {
                rb.setTextColor(COLOR_CYAN_PRIMARY);
                rb.setTypeface(AndroidUtilities.bold());
            } else if ("Italic".equals(styles[i])) {
                rb.setTextColor(COLOR_PURPLE_PRIMARY);
            } else if ("Spoiler".equals(styles[i])) {
                rb.setTextColor(COLOR_AMBER_PRIMARY);
            } else if ("Mono".equals(styles[i])) {
                rb.setTextColor(COLOR_GREEN_PRIMARY);
            } else if ("Strikethrough".equals(styles[i])) {
                rb.setTextColor(0xFFE11D48); // Coral red
            } else if ("Underline".equals(styles[i])) {
                rb.setTextColor(0xFF4F46E5); // Indigo
            } else if ("Quote".equals(styles[i])) {
                rb.setTextColor(COLOR_CYAN_PRIMARY);
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
        guideBox.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        GradientDrawable guideBg = new GradientDrawable();
        guideBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        guideBg.setCornerRadius(AndroidUtilities.dp(14));
        guideBg.setStroke(AndroidUtilities.dp(1), 0x260284C7);
        guideBox.setBackground(guideBg);

        LinearLayout.LayoutParams gParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gParams.topMargin = AndroidUtilities.dp(14);
        guideBox.setLayoutParams(gParams);

        TextView gTitle = new TextView(getParentActivity());
        gTitle.setText("💻 Desktop / PC Keyboard Shortcuts:");
        gTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        gTitle.setTextColor(COLOR_CYAN_PRIMARY);
        gTitle.setTypeface(AndroidUtilities.bold());
        guideBox.addView(gTitle);

        TextView gList = new TextView(getParentActivity());
        gList.setText("• [Ctrl + B] › Bold\n• [Ctrl + I] › Italic\n• [Ctrl + U] › Underline\n• [Ctrl + Shift + M] › Monospace\n• [Ctrl + Shift + X] › Strikethrough\n• [Ctrl + Shift + P] › Spoiler\n• [Ctrl + Shift + >] › Quote");
        gList.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        gList.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        gList.setPadding(0, AndroidUtilities.dp(6), 0, 0);
        gList.setLineSpacing(AndroidUtilities.dp(4), 1.0f);
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
