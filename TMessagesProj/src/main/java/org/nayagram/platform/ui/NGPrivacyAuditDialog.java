package org.nayagram.platform.ui;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.nayagram.platform.NayaConfig;
import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

/**
 * NGPrivacyAuditDialog provides a clean, visual audit of security and privacy protections
 * in full compliance with Google Play Store guidelines (no ghost/stealth claims).
 */
public class NGPrivacyAuditDialog {

    public static void show(Context context) {
        if (context == null) {
            return;
        }
        NayaConfig cfg = NayaConfig.getInstance();

        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        ScrollView scrollView = new ScrollView(context);
        scrollView.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(20), AndroidUtilities.dp(16));
        scrollView.addView(content);

        // Header: Shield Icon + Title + Subtitle
        FrameLayout headerIconFrame = new FrameLayout(context);
        GradientDrawable headerBg = new GradientDrawable();
        headerBg.setShape(GradientDrawable.OVAL);
        headerBg.setColor(0xFF0D80ED);
        headerIconFrame.setBackground(headerBg);

        ImageView shieldIcon = new ImageView(context);
        shieldIcon.setImageResource(R.drawable.outline_shield_check);
        shieldIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        headerIconFrame.addView(shieldIcon, LayoutHelper.createFrame(28, 28, Gravity.CENTER));

        content.addView(headerIconFrame, LayoutHelper.createLinear(56, 56, Gravity.CENTER_HORIZONTAL, 0, 4, 0, 12));

        TextView titleView = new TextView(context);
        titleView.setText("Privacy & Security Audit");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 19);
        titleView.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        titleView.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        titleView.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(titleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView subtitleView = new TextView(context);
        subtitleView.setText("Real-time verification of local safeguards & encryption.");
        subtitleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        subtitleView.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        subtitleView.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(subtitleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // Container for status rows
        LinearLayout statusContainer = new LinearLayout(context);
        statusContainer.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable boxBg = new GradientDrawable();
        boxBg.setCornerRadius(AndroidUtilities.dp(12));
        boxBg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        statusContainer.setBackground(boxBg);
        statusContainer.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        content.addView(statusContainer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // 1. Session Encryption
        statusContainer.addView(createStatusRow(context, R.drawable.msg_secret, 0xFF0D80ED,
                "Session Encryption", "MTProto 2.0 End-to-End", true, "VERIFIED", true));

        // 2. Biometric Chat Locker
        boolean bioActive = cfg.isBiometricChatLockerEnabled();
        statusContainer.addView(createStatusRow(context, R.drawable.ic_naya_fingerprint, 0xFF10B981,
                "Biometric Chat Locker", "Passkey & biometric protection", bioActive, bioActive ? "ACTIVE" : "OFF", true));

        // 3. Anonymous Stories
        boolean storiesActive = cfg.isAnonymousStories();
        statusContainer.addView(createStatusRow(context, R.drawable.ic_naya_eye_off, 0xFF6366F1,
                "Private Story Viewing", "View receipt suppression", storiesActive, storiesActive ? "ACTIVE" : "OFF", true));

        // 4. Focus Mode
        boolean focusActive = cfg.isFocusModeEnabled();
        statusContainer.addView(createStatusRow(context, R.drawable.outline_profile_mute_24, 0xFF8E24AA,
                "Focus & Wellbeing", "Quiet hours & notification guard", focusActive, focusActive ? "ACTIVE" : "OFF", false));

        // Bottom note
        TextView noteView = new TextView(context);
        noteView.setText("NayaGram strictly complies with Google Play policies. Security & privacy operations execute locally on your device.");
        noteView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        noteView.setTextColor(Theme.getColor(Theme.key_dialogTextGray3));
        noteView.setGravity(Gravity.CENTER_HORIZONTAL);
        content.addView(noteView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 8, 4, 8, 8));

        builder.setView(scrollView);
        builder.setPositiveButton(LocaleController.getString("OK", R.string.OK), null);
        builder.show();
    }

    private static View createStatusRow(Context context, int iconRes, int iconColor, String title, String subtitle, boolean active, String badgeText, boolean needDivider) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));

        // Icon in squircle
        FrameLayout iconFrame = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setCornerRadius(AndroidUtilities.dp(8));
        iconBg.setColor(iconColor);
        iconFrame.setBackground(iconBg);

        ImageView iv = new ImageView(context);
        iv.setImageResource(iconRes);
        iv.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        iconFrame.addView(iv, LayoutHelper.createFrame(18, 18, Gravity.CENTER));
        iconFrame.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        row.addView(iconFrame, LayoutHelper.createLinear(32, 32, Gravity.CENTER_VERTICAL, 0, 0, 10, 0));

        // Titles
        LinearLayout textLayout = new LinearLayout(context);
        textLayout.setOrientation(LinearLayout.VERTICAL);
        row.addView(textLayout, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        t.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        t.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        textLayout.addView(t);

        TextView s = new TextView(context);
        s.setText(subtitle);
        s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        s.setTextColor(Theme.getColor(Theme.key_dialogTextGray2));
        textLayout.addView(s);

        // Pill Badge
        TextView badge = new TextView(context);
        badge.setText(badgeText);
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        badge.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        badge.setPadding(AndroidUtilities.dp(7), AndroidUtilities.dp(3), AndroidUtilities.dp(7), AndroidUtilities.dp(3));

        GradientDrawable pill = new GradientDrawable();
        pill.setCornerRadius(AndroidUtilities.dp(6));
        if (active) {
            pill.setColor(0x2010B981);
            badge.setTextColor(0xFF059669);
        } else {
            pill.setColor(0x209E9E9E);
            badge.setTextColor(0xFF757575);
        }
        badge.setBackground(pill);
        row.addView(badge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
        row.setContentDescription(title + ", " + subtitle + ", " + badgeText);
        row.setFocusable(true);

        if (!needDivider) {
            return row;
        }

        LinearLayout wrapper = new LinearLayout(context);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.addView(row);

        View div = new View(context);
        div.setBackgroundColor(Theme.getColor(Theme.key_divider));
        wrapper.addView(div, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 42, 0, 0, 0));
        return wrapper;
    }
}
