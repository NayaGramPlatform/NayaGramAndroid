package org.telegram.ui;

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
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.BottomSheet;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * NayaGram Smart Auto-Reply Activity:
 * High-end, spacious and professional UI/UX for managing auto-reply rules.
 * Features:
 * - Master Switch card with status indicator
 * - Default Away Message card with live preview & formatting
 * - Stealth Ghost Presence card (keeps last seen intact)
 * - Clean search bar for rules
 * - Dynamic Keyword Rules list with single-tap switch toggle & edit dialog
 * - Top-level Shutter Icon Button (🎨 Text Styles) above the input box
 * - 1-Tap Quick Formatting Bar directly above message editor
 * - Native sliding BottomSheet shutter for all rich text styles with colored checkmark (টিক চিহ্ন)
 * - Zero clutter: 16 legacy preset rules and category chips completely removed!
 */
public class NGAutoReplyActivity extends BaseFragment {

    private LinearLayout rulesContainer;
    private TextView masterStatusText;
    private Switch masterSwitch;
    private TextView defaultMessagePreview;
    private String currentSearchQuery = "";

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
        actionBar.setSubtitle("Instant Keyword Replies");
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

        // 1. Master Toggle Card
        root.addView(createMasterCard(context));

        // 2. Default Away Message Card
        root.addView(createDefaultMessageCard(context));

        // 2.5 Stealth Ghost Presence Card
        root.addView(createStealthPresenceCard(context));

        // 3. Search Bar for Rules
        root.addView(createSearchSection(context));

        // 4. Keyword Rules Section Header
        root.addView(createSectionHeader(context, "Instant Keyword Rules", "Matches incoming messages by keyword and replies immediately"));

        // 5. Container for Dynamic Rule Cards
        rulesContainer = new LinearLayout(context);
        rulesContainer.setOrientation(LinearLayout.VERTICAL);
        root.addView(rulesContainer);

        // 6. Action Button (+ Add Custom Keyword Rule)
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
        card.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(22);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setOrientation(GradientDrawable.Orientation.TL_BR);
        bg.setColors(new int[]{0xFF0284C7, 0xFF059669});
        bg.setCornerRadius(AndroidUtilities.dp(20));
        card.setBackground(bg);

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        textLayout.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        TextView title = new TextView(context);
        title.setText("Smart Auto-Reply Master Switch");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTextColor(0xFFFFFFFF);
        title.setTypeface(AndroidUtilities.bold());
        textLayout.addView(title);

        masterStatusText = new TextView(context);
        masterStatusText.setText(AutoReplyManager.getInstance().isAutoReplyEnabled() ? "🟢 Currently Active & Listening" : "⚪ Disabled (Auto-replies paused)");
        masterStatusText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        masterStatusText.setTextColor(0xEEFFFFFF);
        masterStatusText.setPadding(0, AndroidUtilities.dp(4), 0, 0);
        textLayout.addView(masterStatusText);

        row.addView(textLayout);

        masterSwitch = new Switch(context);
        masterSwitch.setChecked(AutoReplyManager.getInstance().isAutoReplyEnabled());
        applySwitchTint(masterSwitch, 0xFFFFFFFF, 0x9900E5FF, 0xFFE2E8F0, 0x66CBD5E1);
        masterSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            AutoReplyManager.getInstance().setAutoReplyEnabled(isChecked);
            NayaConfig.getInstance().setSmartAutoReply(isChecked);
            masterStatusText.setText(isChecked ? "🟢 Currently Active & Listening" : "⚪ Disabled (Auto-replies paused)");
            if (getParentActivity() != null) {
                BulletinFactory.of(this).createSimpleBulletin(
                        isChecked ? R.raw.done : R.raw.chats_infotip,
                        "Smart Auto-Reply: " + (isChecked ? "Enabled" : "Disabled")
                ).show();
            }
        });
        row.addView(masterSwitch);
        card.addView(row);

        TextView info = new TextView(context);
        info.setText("Automates replies for 1-on-1 private chats without disturbing channels or groups. Responses trigger instantly upon incoming messages.");
        info.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        info.setTextColor(0xDDFFFFFF);
        info.setLineSpacing(AndroidUtilities.dp(3), 1.0f);
        LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        infoParams.topMargin = AndroidUtilities.dp(14);
        info.setLayoutParams(infoParams);
        card.addView(info);

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
        int pad = AndroidUtilities.dp(20);
        card.setPadding(pad, pad, pad, pad);

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(18));
        bg.setStroke(AndroidUtilities.dp(1.5f), 0x330284C7);
        card.setBackground(bg);

        LinearLayout header = new LinearLayout(context);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        ImageView icon = new ImageView(context);
        icon.setImageResource(R.drawable.msg_edit);
        icon.setColorFilter(COLOR_CYAN_PRIMARY);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(AndroidUtilities.dp(22), AndroidUtilities.dp(22));
        iconParams.rightMargin = AndroidUtilities.dp(10);
        header.addView(icon, iconParams);

        TextView title = new TextView(context);
        title.setText("Default Away Message (অটো-রিপ্লাই মেসেজ)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        header.addView(title);

        card.addView(header);

        TextView desc = new TextView(context);
        desc.setText("Sent automatically when someone messages you and no specific keyword matches.");
        desc.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        desc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        desc.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(12));
        card.addView(desc);

        // Preview Box
        LinearLayout previewBox = new LinearLayout(context);
        previewBox.setOrientation(LinearLayout.VERTICAL);
        previewBox.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        GradientDrawable pbBg = new GradientDrawable();
        pbBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        pbBg.setCornerRadius(AndroidUtilities.dp(12));
        previewBox.setBackground(pbBg);

        defaultMessagePreview = new TextView(context);
        defaultMessagePreview.setText(NayaConfig.getInstance().getAutoReplyText());
        defaultMessagePreview.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        defaultMessagePreview.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        defaultMessagePreview.setLineSpacing(AndroidUtilities.dp(2), 1.0f);
        previewBox.addView(defaultMessagePreview);
        card.addView(previewBox);

        // Cooldown row
        LinearLayout cdRow = new LinearLayout(context);
        cdRow.setOrientation(LinearLayout.HORIZONTAL);
        cdRow.setGravity(Gravity.CENTER_VERTICAL);
        cdRow.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(14));

        TextView cdLabel = new TextView(context);
        cdLabel.setText("⏱️ Per-Chat Cooldown: ");
        cdLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cdLabel.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        cdRow.addView(cdLabel);

        int hours = NayaConfig.getInstance().getAutoReplyCooldownHours();
        final TextView cdVal = new TextView(context);
        cdVal.setText(hours + " Hour" + (hours > 1 ? "s" : ""));
        cdVal.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        cdVal.setTextColor(COLOR_CYAN_PRIMARY);
        cdVal.setTypeface(AndroidUtilities.bold());
        cdVal.setPadding(AndroidUtilities.dp(8), AndroidUtilities.dp(4), AndroidUtilities.dp(8), AndroidUtilities.dp(4));
        GradientDrawable cdValBg = new GradientDrawable();
        cdValBg.setColor(0x1F0284C7);
        cdValBg.setCornerRadius(AndroidUtilities.dp(8));
        cdVal.setBackground(cdValBg);
        cdVal.setOnClickListener(v -> showCooldownPickerDialog(cdVal));
        cdRow.addView(cdVal);
        card.addView(cdRow);

        // Edit Button (Emerald Green Pill)
        LinearLayout editBtn = new LinearLayout(context);
        editBtn.setOrientation(LinearLayout.HORIZONTAL);
        editBtn.setGravity(Gravity.CENTER);
        editBtn.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(10));
        GradientDrawable editBg = new GradientDrawable();
        editBg.setColor(COLOR_GREEN_PRIMARY);
        editBg.setCornerRadius(AndroidUtilities.dp(12));
        editBtn.setBackground(editBg);

        TextView editText = new TextView(context);
        editText.setText("✏️ Edit Away Message & Formatting");
        editText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        editText.setTextColor(0xFFFFFFFF);
        editText.setTypeface(AndroidUtilities.bold());
        editBtn.addView(editText);
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
        bg.setStroke(AndroidUtilities.dp(1.5f), 0x337C3AED);
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

    private View createSearchSection(Context context) {
        LinearLayout layout = new LinearLayout(context);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(0, 0, 0, AndroidUtilities.dp(12));

        EditText etSearch = new EditText(context);
        etSearch.setHint("🔍 Search rules by keyword or name...");
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
        return layout;
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

            int chipColor = COLOR_CYAN_PRIMARY;

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

            // Response Preview
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

            // Emerald Green Toggle switch
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
            empty.setText("No custom keyword rules yet. Tap \"+ Add Custom Keyword Rule\" below to create your first rule!");
            empty.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
            empty.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, AndroidUtilities.dp(28), 0, AndroidUtilities.dp(28));
            rulesContainer.addView(empty);
        }
    }

    private boolean matchesFilters(AutoReplyManager.AutoReplyRule rule) {
        if (rule == null) return false;
        if (!currentSearchQuery.isEmpty()) {
            boolean nameMatch = rule.name != null && rule.name.toLowerCase().contains(currentSearchQuery);
            boolean kwMatch = rule.keyword != null && rule.keyword.toLowerCase().contains(currentSearchQuery);
            boolean respMatch = rule.response != null && rule.response.toLowerCase().contains(currentSearchQuery);
            return nameMatch || kwMatch || respMatch;
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

        // Top Header Row with prominent Shutter Icon Button
        root.addView(createEditorHeaderRow("Message Content", () -> showTextStyleDialog(etMessage)));

        // 1-Tap Quick Formatting Bar
        root.addView(createQuickFormattingBar(etMessage));

        root.addView(etMessage);

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

        // Top Header Row with Shutter Icon Button (🎨 Text Styles)
        root.addView(createEditorHeaderRow("Auto-Reply Message", () -> showTextStyleDialog(etResp)));

        // 1-Tap Quick Formatting Bar
        root.addView(createQuickFormattingBar(etResp));

        root.addView(etResp);

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
     * Editor Header Row: shows section title and a prominent 🎨 Text Style icon button.
     */
    private View createEditorHeaderRow(String titleText, Runnable onShutterClick) {
        LinearLayout row = new LinearLayout(getParentActivity());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(14), 0, AndroidUtilities.dp(4));

        TextView tv = new TextView(getParentActivity());
        tv.setText(titleText);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13);
        tv.setTextColor(COLOR_GREEN_PRIMARY);
        tv.setTypeface(AndroidUtilities.bold());
        LinearLayout.LayoutParams tLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(tv, tLp);

        // Top Icon Button for opening shutter
        LinearLayout shutterBtn = new LinearLayout(getParentActivity());
        shutterBtn.setOrientation(LinearLayout.HORIZONTAL);
        shutterBtn.setGravity(Gravity.CENTER_VERTICAL);
        shutterBtn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(4), AndroidUtilities.dp(10), AndroidUtilities.dp(4));
        GradientDrawable sBg = new GradientDrawable();
        sBg.setColor(0x1F0284C7);
        sBg.setCornerRadius(AndroidUtilities.dp(10));
        sBg.setStroke(AndroidUtilities.dp(1), 0x330284C7);
        shutterBtn.setBackground(sBg);

        TextView sText = new TextView(getParentActivity());
        sText.setText("🎨 Text Styles (শাটার)");
        sText.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sText.setTextColor(COLOR_CYAN_PRIMARY);
        sText.setTypeface(AndroidUtilities.bold());
        shutterBtn.addView(sText);

        shutterBtn.setOnClickListener(v -> onShutterClick.run());
        row.addView(shutterBtn);

        return row;
    }

    /**
     * Quick Horizontal 1-Tap Formatting Toolbar placed directly above EditText.
     */
    private View createQuickFormattingBar(final EditText target) {
        HorizontalScrollView hsv = new HorizontalScrollView(getParentActivity());
        hsv.setHorizontalScrollBarEnabled(false);
        LinearLayout bar = new LinearLayout(getParentActivity());
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(8));

        addQuickFormatButton(bar, "𝗕 Bold", () -> applyStyleToEditText(target, "Bold"), COLOR_CYAN_PRIMARY);
        addQuickFormatButton(bar, "𝘐 Italic", () -> applyStyleToEditText(target, "Italic"), COLOR_PURPLE_PRIMARY);
        addQuickFormatButton(bar, "🔒 Spoiler", () -> applyStyleToEditText(target, "Spoiler"), COLOR_AMBER_PRIMARY);
        addQuickFormatButton(bar, "𝚌𝚘𝚍𝚎", () -> applyStyleToEditText(target, "Mono"), COLOR_GREEN_PRIMARY);
        addQuickFormatButton(bar, "❝ Quote", () -> applyStyleToEditText(target, "Quote"), 0xFF0284C7);
        addQuickFormatButton(bar, "🔗 Link", () -> showCreateLinkDialog(target), 0xFF4F46E5);
        addQuickFormatButton(bar, "🎨 All (শাটার)", () -> showTextStyleDialog(target), 0xFFE11D48);

        hsv.addView(bar);
        return hsv;
    }

    private void addQuickFormatButton(LinearLayout parent, String label, final Runnable action, int tintColor) {
        TextView btn = new TextView(parent.getContext());
        btn.setText(label);
        btn.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        btn.setTextColor(tintColor);
        btn.setTypeface(AndroidUtilities.bold());
        btn.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(6), AndroidUtilities.dp(10), AndroidUtilities.dp(6));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x1F000000 | (tintColor & 0x00FFFFFF));
        bg.setCornerRadius(AndroidUtilities.dp(10));
        btn.setBackground(bg);
        btn.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.rightMargin = AndroidUtilities.dp(6);
        parent.addView(btn, lp);
    }

    private void showCreateLinkDialog(final EditText target) {
        if (getParentActivity() == null || target == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        LinearLayout root = new LinearLayout(getParentActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(20);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(getParentActivity());
        title.setText("🔗 Create Link / Hyperlink");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        title.setTextColor(COLOR_CYAN_PRIMARY);
        title.setTypeface(AndroidUtilities.bold());
        root.addView(title);

        TextView sub = new TextView(getParentActivity());
        sub.setText("Enter URL and display title to create a Telegram formatted link");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        sub.setPadding(0, AndroidUtilities.dp(4), 0, AndroidUtilities.dp(14));
        root.addView(sub);

        int start = Math.max(target.getSelectionStart(), 0);
        int end = Math.max(target.getSelectionEnd(), 0);
        if (start > end) { int tmp = start; start = end; end = tmp; }
        String existingSelection = target.getText().toString().substring(start, end);

        final EditText etLabel = new EditText(getParentActivity());
        etLabel.setHint("Link Display Title (e.g. Visit Channel)");
        if (!TextUtils.isEmpty(existingSelection)) {
            etLabel.setText(existingSelection);
        }
        etLabel.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etLabel.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        GradientDrawable labelBg = new GradientDrawable();
        labelBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        labelBg.setCornerRadius(AndroidUtilities.dp(12));
        labelBg.setStroke(AndroidUtilities.dp(1), 0x260284C7);
        etLabel.setBackground(labelBg);
        root.addView(etLabel);

        final EditText etUrl = new EditText(getParentActivity());
        etUrl.setHint("URL (e.g. https://t.me/NayaGramOfficial)");
        etUrl.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        etUrl.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        GradientDrawable urlBg = new GradientDrawable();
        urlBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        urlBg.setCornerRadius(AndroidUtilities.dp(12));
        urlBg.setStroke(AndroidUtilities.dp(1), 0x260284C7);
        etUrl.setBackground(urlBg);
        LinearLayout.LayoutParams urlLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        urlLp.topMargin = AndroidUtilities.dp(10);
        etUrl.setLayoutParams(urlLp);
        root.addView(etUrl);

        final int fStart = start;
        final int fEnd = end;

        b.setView(root);
        b.setPositiveButton("Insert Link", (d, w) -> {
            String label = etLabel.getText().toString().trim();
            String url = etUrl.getText().toString().trim();
            if (label.isEmpty()) label = "Link";
            if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("tg://")) {
                url = "https://" + url;
            }
            String formatted = "[" + label + "](" + url + ")";
            target.getText().replace(fStart, fEnd, formatted);
            target.setSelection(fStart + formatted.length());
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Link inserted!").show();
        });
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
    }

    /**
     * Native Telegram Sliding BottomSheet Shutter for rich text formatting.
     * Features animated tick marks (টিক চিহ্ন ✓) in radiant colors indicating active selection.
     */
    private void showTextStyleDialog(final EditText target) {
        if (getParentActivity() == null || target == null) return;

        BottomSheet.Builder builder = new BottomSheet.Builder(getParentActivity());

        LinearLayout root = new LinearLayout(getParentActivity());
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = AndroidUtilities.dp(18);
        root.setPadding(pad, AndroidUtilities.dp(12), pad, AndroidUtilities.dp(24));

        // Shutter Drag Handle
        View handle = new View(getParentActivity());
        GradientDrawable handleBg = new GradientDrawable();
        handleBg.setColor(0x33888888);
        handleBg.setCornerRadius(AndroidUtilities.dp(3));
        handle.setBackground(handleBg);
        LinearLayout.LayoutParams hLp = new LinearLayout.LayoutParams(AndroidUtilities.dp(40), AndroidUtilities.dp(5));
        hLp.gravity = Gravity.CENTER_HORIZONTAL;
        hLp.bottomMargin = AndroidUtilities.dp(14);
        root.addView(handle, hLp);

        TextView title = new TextView(getParentActivity());
        title.setText("🎨 Text Style & Formatting (লেখা স্টাইল)");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 17);
        title.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        title.setTypeface(AndroidUtilities.bold());
        root.addView(title);

        TextView sub = new TextView(getParentActivity());
        sub.setText("Tap any style to format selected text or insert formatting tags");
        sub.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        sub.setTextColor(Theme.getColor(Theme.key_dialogTextGray));
        sub.setPadding(0, AndroidUtilities.dp(2), 0, AndroidUtilities.dp(14));
        root.addView(sub);

        BottomSheet sheet = builder.create();

        // Detect current style from selection or text
        int start = Math.max(target.getSelectionStart(), 0);
        int end = Math.max(target.getSelectionEnd(), 0);
        if (start > end) { int tmp = start; start = end; end = tmp; }
        String sel = target.getText().toString().substring(start, end);
        String active = "Bold";
        if (sel.startsWith("**") && sel.endsWith("**")) active = "Bold";
        else if (sel.startsWith("__") && sel.endsWith("__")) active = "Italic";
        else if (sel.startsWith("||") && sel.endsWith("||")) active = "Spoiler";
        else if (sel.startsWith("`") && sel.endsWith("`")) active = "Mono";
        else if (sel.startsWith("> ")) active = "Quote";
        else if (sel.startsWith("~~") && sel.endsWith("~~")) active = "Strikethrough";
        else if (sel.startsWith("--") && sel.endsWith("--")) active = "Underline";
        else if (sel.startsWith("[") && sel.contains("](")) active = "Link";

        // Style list cards with colorful checkmark badge (টিক চিহ্ন)
        addStyleItem(root, "Bold (বোল্ড)", "**bold text**", COLOR_CYAN_PRIMARY, "Bold".equals(active), () -> {
            applyStyleToEditText(target, "Bold");
            sheet.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "✓ Bold style applied!").show();
        });
        addStyleItem(root, "Italic (ইটালিক)", "__italic text__", COLOR_PURPLE_PRIMARY, "Italic".equals(active), () -> {
            applyStyleToEditText(target, "Italic");
            sheet.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "✓ Italic style applied!").show();
        });
        addStyleItem(root, "Spoiler (স্পয়লার)", "||hidden text||", COLOR_AMBER_PRIMARY, "Spoiler".equals(active), () -> {
            applyStyleToEditText(target, "Spoiler");
            sheet.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "✓ Spoiler style applied!").show();
        });
        addStyleItem(root, "Monospace (কোড)", "`code snippet`", COLOR_GREEN_PRIMARY, "Mono".equals(active), () -> {
            applyStyleToEditText(target, "Mono");
            sheet.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "✓ Monospace style applied!").show();
        });
        addStyleItem(root, "Quote (উদ্ধৃতি)", "> quoted line", COLOR_CYAN_PRIMARY, "Quote".equals(active), () -> {
            applyStyleToEditText(target, "Quote");
            sheet.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "✓ Quote style applied!").show();
        });
        addStyleItem(root, "Strikethrough (কাটা দাগ)", "~~strikethrough~~", 0xFFE11D48, "Strikethrough".equals(active), () -> {
            applyStyleToEditText(target, "Strikethrough");
            sheet.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "✓ Strikethrough style applied!").show();
        });
        addStyleItem(root, "Underline (আন্ডারলাইন)", "--underlined text--", 0xFF4F46E5, "Underline".equals(active), () -> {
            applyStyleToEditText(target, "Underline");
            sheet.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "✓ Underline style applied!").show();
        });
        addStyleItem(root, "Create Link (লিংক তৈরি)", "[Title](https://...)", COLOR_CYAN_PRIMARY, "Link".equals(active), () -> {
            sheet.dismiss();
            showCreateLinkDialog(target);
        });
        addStyleItem(root, "Clear Style (সাধারণ লেখা)", "Remove formatting", 0xFF64748B, false, () -> {
            applyStyleToEditText(target, "Regular");
            sheet.dismiss();
            BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Formatting cleared").show();
        });

        // Desktop / PC Keyboard Shortcuts Info Box
        LinearLayout guideBox = new LinearLayout(getParentActivity());
        guideBox.setOrientation(LinearLayout.VERTICAL);
        guideBox.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10));
        GradientDrawable guideBg = new GradientDrawable();
        guideBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        guideBg.setCornerRadius(AndroidUtilities.dp(12));
        guideBox.setBackground(guideBg);

        LinearLayout.LayoutParams gParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        gParams.topMargin = AndroidUtilities.dp(12);
        guideBox.setLayoutParams(gParams);

        TextView gTitle = new TextView(getParentActivity());
        gTitle.setText("💻 Desktop / PC Shortcuts: Ctrl+B (Bold) • Ctrl+I (Italic) • Ctrl+Shift+P (Spoiler)");
        gTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 11);
        gTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        guideBox.addView(gTitle);

        root.addView(guideBox);

        ScrollView sv = new ScrollView(getParentActivity());
        sv.addView(root);
        builder.setCustomView(sv);

        showDialog(sheet);
    }

    private void addStyleItem(LinearLayout parent, String name, String sample, int color, boolean isSelected, Runnable onClick) {
        LinearLayout row = new LinearLayout(parent.getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(12));
        bg.setStroke(AndroidUtilities.dp(isSelected ? 2 : 1), isSelected ? color : 0x1A000000);
        row.setBackground(bg);

        TextView tvName = new TextView(parent.getContext());
        tvName.setText(name);
        tvName.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15);
        tvName.setTextColor(color);
        tvName.setTypeface(AndroidUtilities.bold());
        LinearLayout.LayoutParams nLp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        row.addView(tvName, nLp);

        TextView tvSample = new TextView(parent.getContext());
        tvSample.setText(sample);
        tvSample.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
        tvSample.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        tvSample.setPadding(0, 0, AndroidUtilities.dp(10), 0);
        row.addView(tvSample);

        // Checkmark badge (টিক চিহ্ন)
        TextView checkBadge = new TextView(parent.getContext());
        checkBadge.setText(isSelected ? "✓" : "○");
        checkBadge.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
        checkBadge.setTextColor(isSelected ? 0xFFFFFFFF : 0xFF94A3B8);
        checkBadge.setTypeface(AndroidUtilities.bold());
        checkBadge.setGravity(Gravity.CENTER);
        int badgeSize = AndroidUtilities.dp(24);
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setShape(GradientDrawable.OVAL);
        badgeBg.setColor(isSelected ? color : 0x1A94A3B8);
        checkBadge.setBackground(badgeBg);
        row.addView(checkBadge, new LinearLayout.LayoutParams(badgeSize, badgeSize));

        row.setOnClickListener(v -> onClick.run());

        LinearLayout.LayoutParams rLp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        rLp.bottomMargin = AndroidUtilities.dp(8);
        parent.addView(row, rLp);
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

        if ("Create Link".equals(style) || "Link".equals(style)) {
            showCreateLinkDialog(et);
            return;
        } else if ("Regular".equals(style)) {
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
        } else {
            replacement = selected;
        }

        et.getText().replace(start, end, replacement);
        et.setSelection(start + replacement.length());
    }

    private void showCooldownPickerDialog(final TextView targetView) {
        if (getParentActivity() == null) return;
        AlertDialog.Builder b = new AlertDialog.Builder(getParentActivity());
        b.setTitle("Cooldown Hours");

        final String[] options = new String[]{"1 Hour", "2 Hours", "4 Hours", "8 Hours", "12 Hours", "24 Hours (1 Day)", "48 Hours (2 Days)"};
        final int[] values = new int[]{1, 2, 4, 8, 12, 24, 48};

        b.setItems(options, (dialog, which) -> {
            if (which >= 0 && which < values.length) {
                int chosen = values[which];
                NayaConfig.getInstance().setAutoReplyCooldownHours(chosen);
                if (targetView != null) {
                    targetView.setText(chosen + " Hour" + (chosen > 1 ? "s" : ""));
                }
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, "Cooldown set to " + chosen + " hours").show();
            }
        });
        b.setNegativeButton("Cancel", null);
        showDialog(b.create());
    }
}
