package org.nayagram.platform.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;

public class NGAdminActivity extends BaseFragment {

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("🛡️ NG Admin & Leadership");
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

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(24));

        // Header Banner Card
        LinearLayout banner = new LinearLayout(context);
        banner.setOrientation(LinearLayout.VERTICAL);
        banner.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(18), AndroidUtilities.dp(20), AndroidUtilities.dp(18));
        banner.setBackground(createGradientBg(0xFF0284C7, 0xFF7C3AED, 18));

        TextView bTitle = new TextView(context);
        bTitle.setText("🛡️ Official Leadership & Support");
        bTitle.setTextSize(18);
        bTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        bTitle.setTextColor(Color.WHITE);
        banner.addView(bTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView bDesc = new TextView(context);
        bDesc.setText("Direct access to official NayaGram founders, platform administrators, and support channels.");
        bDesc.setTextSize(13);
        bDesc.setTextColor(0xFFE2E8F0);
        banner.addView(bDesc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        content.addView(banner, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Section Title
        TextView sectionTitle = new TextView(context);
        sectionTitle.setText("ADMINISTRATORS & FOUNDERS");
        sectionTitle.setTextSize(12);
        sectionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        sectionTitle.setTextColor(0xFF0284C7);
        content.addView(sectionTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 0, 0, 8));

        // Founder Card
        content.addView(createAdminCard(
            context,
            "👑",
            "Super Admin (Founder & Owner)",
            "@NGSupreme",
            "Full platform owner & lead architect of NayaGram.",
            0xFFF59E0B,
            v -> MessagesController.getInstance(currentAccount).openByUserName("NGSupreme", NGAdminActivity.this, 1)
        ), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Co-Founder Card
        content.addView(createAdminCard(
            context,
            "🛡️",
            "Admin (Co-Founder & Admin)",
            "@NG_HuzayfaSheikh",
            "Co-founder, platform administration & community supervisor.",
            0xFF0284C7,
            v -> MessagesController.getInstance(currentAccount).openByUserName("NG_HuzayfaSheikh", NGAdminActivity.this, 1)
        ), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Official Channel Card
        content.addView(createAdminCard(
            context,
            "📢",
            "Official Support & Updates Channel",
            "@NayaGramPro",
            "Official updates, feature announcements & releases.",
            0xFF10B981,
            v -> MessagesController.getInstance(currentAccount).openByUserName("NayaGramPro", NGAdminActivity.this, 1)
        ), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Support Email Card
        LinearLayout emailCard = new LinearLayout(context);
        emailCard.setOrientation(LinearLayout.VERTICAL);
        emailCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        boolean dark = Theme.isCurrentThemeDark();
        emailCard.setBackground(createCardBg(dark ? 0xFF1E293B : 0xFFFFFFFF, dark ? 0xFF334155 : 0xFFE2E8F0, 16));

        TextView eTitle = new TextView(context);
        eTitle.setText("✉️ Direct Email Inquiry");
        eTitle.setTextSize(15);
        eTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        emailCard.addView(eTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView eDesc = new TextView(context);
        eDesc.setText("For platform inquiries or feedback, send an email to support.nayagram@gmail.com.");
        eDesc.setTextSize(12);
        eDesc.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        emailCard.addView(eDesc, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        TextView eBtn = new TextView(context);
        eBtn.setText("Send Email to Support");
        eBtn.setTextSize(14);
        eBtn.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        eBtn.setTextColor(Color.WHITE);
        eBtn.setGravity(Gravity.CENTER);
        eBtn.setPadding(0, AndroidUtilities.dp(12), 0, AndroidUtilities.dp(12));
        eBtn.setBackground(createRoundBg(0xFF0284C7, 12));
        eBtn.setOnClickListener(v -> {
            try {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO);
                emailIntent.setData(Uri.parse("mailto:support.nayagram@gmail.com"));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "NayaGram Inquiry");
                getParentActivity().startActivity(emailIntent);
            } catch (Exception ignored) {}
        });
        emailCard.addView(eBtn, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        content.addView(emailCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scrollView.addView(content, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        return fragmentView;
    }

    private View createAdminCard(Context context, String emoji, String role, String username, String desc, int accentColor, View.OnClickListener clickListener) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        boolean dark = Theme.isCurrentThemeDark();
        card.setBackground(createCardBg(dark ? 0xFF1E293B : 0xFFFFFFFF, accentColor, 16));

        LinearLayout topRow = new LinearLayout(context);
        topRow.setOrientation(LinearLayout.HORIZONTAL);
        topRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView emojiBadge = new TextView(context);
        emojiBadge.setText(emoji);
        emojiBadge.setTextSize(20);
        topRow.addView(emojiBadge, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 10, 0));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView roleText = new TextView(context);
        roleText.setText(role);
        roleText.setTextSize(15);
        roleText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        roleText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textCol.addView(roleText);

        TextView userText = new TextView(context);
        userText.setText(username);
        userText.setTextSize(13);
        userText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        userText.setTextColor(accentColor);
        textCol.addView(userText);

        topRow.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        ImageView chevron = new ImageView(context);
        chevron.setImageResource(R.drawable.msg_arrowright);
        chevron.setColorFilter(new android.graphics.PorterDuffColorFilter(accentColor, android.graphics.PorterDuff.Mode.SRC_IN));
        topRow.addView(chevron, LayoutHelper.createLinear(18, 18, Gravity.CENTER_VERTICAL));

        card.addView(topRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 6));

        TextView descText = new TextView(context);
        descText.setText(desc);
        descText.setTextSize(12);
        descText.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        card.addView(descText, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        card.setOnClickListener(clickListener);
        return card;
    }

    private GradientDrawable createGradientBg(int startColor, int endColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{startColor, endColor});
        gd.setCornerRadius(AndroidUtilities.dp(radiusDp));
        return gd;
    }

    private GradientDrawable createCardBg(int bgColor, int borderColor, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(bgColor);
        gd.setCornerRadius(AndroidUtilities.dp(radiusDp));
        gd.setStroke(AndroidUtilities.dp(1), borderColor);
        return gd;
    }

    private GradientDrawable createRoundBg(int color, int radiusDp) {
        GradientDrawable gd = new GradientDrawable();
        gd.setColor(color);
        gd.setCornerRadius(AndroidUtilities.dp(radiusDp));
        return gd;
    }
}
