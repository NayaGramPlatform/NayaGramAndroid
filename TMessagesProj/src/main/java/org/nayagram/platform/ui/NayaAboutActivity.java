package org.nayagram.platform.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.messenger.browser.Browser;

/**
 * NayaAboutActivity — clean, policy-safe About page for NayaGram.
 * Inspired by modern messenger About screens; no misleading claims.
 */
public class NayaAboutActivity extends BaseFragment {

    private static final String SUPPORT_EMAIL = "support.nayagram@gmail.com";
    private static final String PRIVACY_URL = "https://nayagramplatform.github.io/PrivacyArticleCenter/";
    private static final String TG_CHANNEL = "NayaGramPro";
    private static final String X_HANDLE = "NayaGramPro";
    private static final String YT_HANDLE = "NayaGramPro";
    private static final String FB_HANDLE = "NayaGramPro";

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("About");
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
        scrollView.setVerticalScrollBarEnabled(false);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(12), AndroidUtilities.dp(16), AndroidUtilities.dp(28));
        scrollView.addView(root, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // ===== Hero card: logo + name + tagline + versions =====
        LinearLayout hero = makeCard(context);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(24), AndroidUtilities.dp(20), AndroidUtilities.dp(20));

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.nayagram_intro_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        hero.addView(logo, LayoutHelper.createLinear(72, 72, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 12));

        TextView name = new TextView(context);
        name.setText("NayaGram");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        name.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        name.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        name.setGravity(Gravity.CENTER);
        hero.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView tagline = new TextView(context);
        tagline.setText("Secure messenger");
        tagline.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        tagline.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        tagline.setGravity(Gravity.CENTER);
        hero.addView(tagline, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Version row
        LinearLayout versionRow = new LinearLayout(context);
        versionRow.setOrientation(LinearLayout.HORIZONTAL);
        versionRow.setGravity(Gravity.CENTER);

        versionRow.addView(makeVersionChip(context, "NayaGram", getAppVersion()), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER, 0, 0, 6, 0));
        versionRow.addView(makeVersionChip(context, "Telegram", "12.10.x"), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER, 6, 0, 0, 0));
        hero.addView(versionRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(hero, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // ===== Community & Channels =====
        root.addView(makeSectionTitle(context, "Community & Channels"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 8, 0, 8));

        LinearLayout community = makeCard(context);
        community.addView(makeLinkRow(context, R.drawable.msg_channel, 0xFF2AABEE, "Telegram", "@" + TG_CHANNEL, v -> openUrl("https://t.me/" + TG_CHANNEL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        community.addView(makeLinkRow(context, R.drawable.msg_link, 0xFF1DA1F2, "Twitter / X", "@" + X_HANDLE, v -> openUrl("https://x.com/" + X_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        community.addView(makeLinkRow(context, R.drawable.msg_link, 0xFFFF0000, "YouTube", "@" + YT_HANDLE, v -> openUrl("https://youtube.com/@" + YT_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        community.addView(makeLinkRow(context, R.drawable.msg_link, 0xFF1877F2, "Facebook", "@" + FB_HANDLE, v -> openUrl("https://facebook.com/" + FB_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(community, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // ===== Support =====
        root.addView(makeSectionTitle(context, "Support"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 8, 0, 8));

        LinearLayout support = makeCard(context);
        support.addView(makeLinkRow(context, R.drawable.msg_msgbubble, 0xFF43A047, "Contact support", "Message our team by email", v -> openEmail()), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        support.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        support.addView(makeLinkRow(context, R.drawable.msg_policy, 0xFF5C6BC0, "Privacy Policy", "How we handle your data", v -> openUrl(PRIVACY_URL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        if (BuildVars.PLAYSTORE_APP_URL != null && !BuildVars.PLAYSTORE_APP_URL.isEmpty()) {
            support.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
            support.addView(makeLinkRow(context, R.drawable.msg_rate_down, 0xFFFB8C00, "Rate the app", "Leave a review on Google Play", v -> openUrl(BuildVars.PLAYSTORE_APP_URL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        root.addView(support, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        // ===== Platform =====
        root.addView(makeSectionTitle(context, "Platform"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 8, 0, 8));

        LinearLayout platform = makeCard(context);
        platform.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView platformTitle = new TextView(context);
        platformTitle.setText("NayaGram Platform");
        platformTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        platformTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        platformTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        platform.addView(platformTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView platformMeta = new TextView(context);
        platformMeta.setText("Built with care in Bangladesh");
        platformMeta.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        platformMeta.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        platform.addView(platformMeta, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        TextView platformBody = new TextView(context);
        platformBody.setText("NayaGram is a privacy-focused messaging client based on Telegram. We aim for clear policies, secure communication, and transparent features that respect Google Play and Telegram platform rules.");
        platformBody.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        platformBody.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        platformBody.setLineSpacing(AndroidUtilities.dp(2), 1f);
        platform.addView(platformBody, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        platform.addView(makeLinkRow(context, R.drawable.msg_link, 0xFF0D80ED, "Email", SUPPORT_EMAIL, v -> openEmail()), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(platform, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Footer
        TextView footer = new TextView(context);
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        footer.setText("© " + year + " NayaGram Platform\nAll rights reserved");
        footer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        footer.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(AndroidUtilities.dp(2), 1f);
        root.addView(footer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 8, 0, 0));

        return fragmentView;
    }

    private LinearLayout makeCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(14));
        card.setBackground(bg);
        card.setClipToOutline(true);
        return card;
    }

    private TextView makeSectionTitle(Context context, String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        t.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        return t;
    }

    private LinearLayout makeVersionChip(Context context, String label, String value) {
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        bg.setCornerRadius(AndroidUtilities.dp(10));
        chip.setBackground(bg);
        chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));

        TextView l = new TextView(context);
        l.setText(label);
        l.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        l.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        l.setGravity(Gravity.CENTER);
        chip.addView(l, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView v = new TextView(context);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        v.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        v.setTextColor(0xFF0D80ED);
        v.setGravity(Gravity.CENTER);
        chip.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        return chip;
    }

    private View makeDivider(Context context) {
        View d = new View(context);
        d.setBackgroundColor(Theme.getColor(Theme.key_divider));
        return d;
    }

    private LinearLayout makeLinkRow(Context context, int iconRes, int iconColor, String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        row.setOnClickListener(click);

        FrameLayout iconWrap = new FrameLayout(context);
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(AndroidUtilities.dp(10));
        iconBg.setColor(iconColor);
        iconWrap.setBackground(iconBg);

        ImageView icon = new ImageView(context);
        try {
            icon.setImageResource(iconRes);
            icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        } catch (Exception ignored) {}
        iconWrap.addView(icon, LayoutHelper.createFrame(20, 20, Gravity.CENTER));
        row.addView(iconWrap, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        t.setTypeface(Typeface.DEFAULT);
        textCol.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView s = new TextView(context);
        s.setText(subtitle);
        s.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        s.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        textCol.addView(s, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 1, 0, 0));

        row.addView(textCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        ImageView chevron = new ImageView(context);
        try {
            chevron.setImageResource(R.drawable.msg_arrowright);
            chevron.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3), PorterDuff.Mode.SRC_IN));
        } catch (Exception ignored) {}
        row.addView(chevron, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));

        return row;
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager()
                    .getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            if (pInfo.versionName != null && !pInfo.versionName.isEmpty()) {
                return pInfo.versionName;
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "1.0.0";
    }

    private void openUrl(String url) {
        try {
            if (getParentActivity() != null) {
                Browser.openUrl(getParentActivity(), url);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void openEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, "NayaGram Support");
            if (getParentActivity() != null) {
                getParentActivity().startActivity(Intent.createChooser(intent, "Contact support"));
            }
        } catch (Exception e) {
            if (getParentActivity() != null) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "No email app found").show();
            }
            FileLog.e(e);
        }
    }
}
