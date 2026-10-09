package org.nayagram.platform.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
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
import org.telegram.messenger.R;
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

/**
 * NayaAboutActivity — polished, policy-aligned About page.
 *
 * Design goals:
 * - Clear third-party client disclosure (not official Telegram)
 * - No exaggerated stats or misleading claims
 * - Transparent privacy / support / community links
 * - Attractive card UI consistent with modern messenger settings
 */
public class NayaAboutActivity extends BaseFragment {

    private static final String SUPPORT_EMAIL = "support.nayagram@gmail.com";
    private static final String PRIVACY_URL = "https://nayagramplatform.github.io/PrivacyArticleCenter/";
    private static final String TG_CHANNEL = "NayaGramPro";
    private static final String X_HANDLE = "NayaGramPro";
    private static final String YT_HANDLE = "NayaGramPro";
    private static final String FB_HANDLE = "NayaGramPro";

    // Brand blues / accents (safe, professional)
    private static final int C_PRIMARY = 0xFF0D80ED;
    private static final int C_PRIMARY_DARK = 0xFF0A5FBE;
    private static final int C_TEAL = 0xFF00BFA5;
    private static final int C_PURPLE = 0xFF7C4DFF;
    private static final int C_ORANGE = 0xFFFF8A00;
    private static final int C_GREEN = 0xFF43A047;
    private static final int C_RED = 0xFFE53935;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("About");
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_actionBarDefault));
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
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(10), AndroidUtilities.dp(16), AndroidUtilities.dp(32));
        scrollView.addView(root, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // ═══════════════════════════════════════
        // HERO — gradient header + logo + name
        // ═══════════════════════════════════════
        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{C_PRIMARY, C_PRIMARY_DARK, 0xFF1A237E}
        );
        heroBg.setCornerRadius(AndroidUtilities.dp(18));
        hero.setBackground(heroBg);
        hero.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(28), AndroidUtilities.dp(20), AndroidUtilities.dp(22));

        // Logo circle
        FrameLayout logoWrap = new FrameLayout(context);
        GradientDrawable logoCircle = new GradientDrawable();
        logoCircle.setShape(GradientDrawable.OVAL);
        logoCircle.setColor(0x33FFFFFF);
        logoWrap.setBackground(logoCircle);
        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.nayagram_intro_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logoWrap.addView(logo, LayoutHelper.createFrame(56, 56, Gravity.CENTER));
        hero.addView(logoWrap, LayoutHelper.createLinear(80, 80, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        TextView name = new TextView(context);
        name.setText("NayaGram");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 24);
        name.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        name.setTextColor(Color.WHITE);
        name.setGravity(Gravity.CENTER);
        hero.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView tagline = new TextView(context);
        tagline.setText("Privacy-focused messaging client");
        tagline.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tagline.setTextColor(0xCCFFFFFF);
        tagline.setGravity(Gravity.CENTER);
        hero.addView(tagline, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // Version chips on dark hero
        LinearLayout versionRow = new LinearLayout(context);
        versionRow.setOrientation(LinearLayout.HORIZONTAL);
        versionRow.setGravity(Gravity.CENTER);
        versionRow.addView(makeHeroChip(context, "App", getAppVersion()), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 6, 0));
        versionRow.addView(makeHeroChip(context, "Based on", "Telegram 12.x"), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 6, 0, 0, 0));
        hero.addView(versionRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(hero, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // ═══════════════════════════════════════
        // POLICY NOTICE — transparent disclosure
        // ═══════════════════════════════════════
        LinearLayout notice = makeCard(context);
        notice.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        notice.setOrientation(LinearLayout.HORIZONTAL);
        notice.setGravity(Gravity.CENTER_VERTICAL);

        FrameLayout noticeIcon = makeIconBadge(context, R.drawable.outline_shield_check, C_PRIMARY);
        notice.addView(noticeIcon, LayoutHelper.createLinear(40, 40, Gravity.TOP, 0, 0, 12, 0));

        LinearLayout noticeText = new LinearLayout(context);
        noticeText.setOrientation(LinearLayout.VERTICAL);

        TextView noticeTitle = new TextView(context);
        noticeTitle.setText("Independent client");
        noticeTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        noticeTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        noticeTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        noticeText.addView(noticeTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        TextView noticeBody = new TextView(context);
        noticeBody.setText("NayaGram is not the official Telegram app. It is an independent messaging client that uses the Telegram API and follows applicable platform rules. Telegram branding and services belong to Telegram FZ-LLC.");
        noticeBody.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        noticeBody.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        noticeBody.setLineSpacing(AndroidUtilities.dp(2), 1f);
        noticeText.addView(noticeBody, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));

        notice.addView(noticeText, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        root.addView(notice, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // ═══════════════════════════════════════
        // COMMITMENTS — 3 policy-safe pillars
        // ═══════════════════════════════════════
        root.addView(makeSectionTitle(context, "Our commitments"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 4, 0, 8));

        LinearLayout pillars = new LinearLayout(context);
        pillars.setOrientation(LinearLayout.HORIZONTAL);
        pillars.addView(makePillar(context, R.drawable.msg_secret, C_TEAL, "Encryption", "Secret chats stay end-to-end encrypted on device"), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 0, 0, 6, 0));
        pillars.addView(makePillar(context, R.drawable.msg_policy, C_PURPLE, "Transparency", "Clear privacy policy & honest feature labels"), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 3, 0, 3, 0));
        pillars.addView(makePillar(context, R.drawable.ic_lock_header, C_GREEN, "Compliance", "Built to respect Play Store & API rules"), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 6, 0, 0, 0));
        root.addView(pillars, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // ═══════════════════════════════════════
        // PRIVACY & DATA
        // ═══════════════════════════════════════
        root.addView(makeSectionTitle(context, "Privacy & data"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 4, 0, 8));

        LinearLayout privacy = makeCard(context);
        privacy.addView(makeLinkRow(context, R.drawable.settings_policy, C_PRIMARY, "Privacy Policy", "What we collect, store, and why", v -> openUrl(PRIVACY_URL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        privacy.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        privacy.addView(makeLinkRow(context, R.drawable.outline_shield_check, C_TEAL, "Account control", "Delete account via Telegram Settings → Privacy", v -> {
            if (getParentActivity() != null) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "Open Settings → Privacy and Security → Delete my account").show();
            }
        }), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        privacy.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        privacy.addView(makeLinkRow(context, R.drawable.msg_policy, C_PURPLE, "No hidden trackers", "We do not sell your chats or contact list", v -> openUrl(PRIVACY_URL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(privacy, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // ═══════════════════════════════════════
        // COMMUNITY
        // ═══════════════════════════════════════
        root.addView(makeSectionTitle(context, "Community & updates"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 4, 0, 8));

        LinearLayout community = makeCard(context);
        community.addView(makeLinkRow(context, R.drawable.msg_channel, 0xFF2AABEE, "Telegram channel", "@" + TG_CHANNEL, v -> openUrl("https://t.me/" + TG_CHANNEL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        community.addView(makeLinkRow(context, R.drawable.msg_link, 0xFF1DA1F2, "Twitter / X", "@" + X_HANDLE, v -> openUrl("https://x.com/" + X_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        community.addView(makeLinkRow(context, R.drawable.msg_link, C_RED, "YouTube", "@" + YT_HANDLE, v -> openUrl("https://youtube.com/@" + YT_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
        community.addView(makeLinkRow(context, R.drawable.msg_link, 0xFF1877F2, "Facebook", "@" + FB_HANDLE, v -> openUrl("https://facebook.com/" + FB_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(community, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // ═══════════════════════════════════════
        // SUPPORT
        // ═══════════════════════════════════════
        root.addView(makeSectionTitle(context, "Support"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 4, 0, 8));

        LinearLayout support = makeCard(context);
        support.addView(makeLinkRow(context, R.drawable.msg_msgbubble, C_GREEN, "Contact support", SUPPORT_EMAIL, v -> openEmail()), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        if (BuildVars.PLAYSTORE_APP_URL != null && !BuildVars.PLAYSTORE_APP_URL.isEmpty()) {
            support.addView(makeDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 56, 0, 0, 0));
            support.addView(makeLinkRow(context, R.drawable.msg_rate_up, C_ORANGE, "Rate on Google Play", "Share honest feedback", v -> openUrl(BuildVars.PLAYSTORE_APP_URL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        root.addView(support, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 14));

        // ═══════════════════════════════════════
        // PLATFORM CARD
        // ═══════════════════════════════════════
        root.addView(makeSectionTitle(context, "Platform"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 4, 4, 0, 8));

        LinearLayout platform = makeCard(context);
        platform.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        TextView platformTitle = new TextView(context);
        platformTitle.setText("NayaGram Platform");
        platformTitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        platformTitle.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        platformTitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        platform.addView(platformTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        TextView platformMeta = new TextView(context);
        platformMeta.setText("Engineered with care in Bangladesh");
        platformMeta.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        platformMeta.setTextColor(C_PRIMARY);
        platform.addView(platformMeta, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        TextView platformBody = new TextView(context);
        platformBody.setText("We focus on clarity, security, and features that do not violate Telegram API terms or Google Play policies. Optional tools are labeled honestly. Ads, if enabled later, will be disclosed in the Privacy Policy and in-app.");
        platformBody.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        platformBody.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        platformBody.setLineSpacing(AndroidUtilities.dp(2), 1f);
        platform.addView(platformBody, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(platform, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // Footer
        TextView footer = new TextView(context);
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        footer.setText("© " + year + " NayaGram Platform\nNot affiliated with Telegram FZ-LLC\nAll rights reserved");
        footer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        footer.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(AndroidUtilities.dp(3), 1f);
        root.addView(footer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        return fragmentView;
    }

    // ─── UI helpers ───────────────────────────────────────────

    private LinearLayout makeCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(14));
        card.setBackground(bg);
        if (android.os.Build.VERSION.SDK_INT >= 21) {
            card.setElevation(AndroidUtilities.dp(1));
            card.setClipToOutline(true);
        }
        return card;
    }

    private TextView makeSectionTitle(Context context, String text) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        t.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        t.setPadding(AndroidUtilities.dp(4), 0, 0, 0);
        return t;
    }

    private LinearLayout makeHeroChip(Context context, String label, String value) {
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x33FFFFFF);
        bg.setCornerRadius(AndroidUtilities.dp(12));
        chip.setBackground(bg);
        chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10), AndroidUtilities.dp(10));

        TextView l = new TextView(context);
        l.setText(label);
        l.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        l.setTextColor(0xB3FFFFFF);
        l.setGravity(Gravity.CENTER);
        chip.addView(l);

        TextView v = new TextView(context);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        v.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        chip.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        return chip;
    }

    private LinearLayout makePillar(Context context, int iconRes, int color, String title, String body) {
        LinearLayout card = makeCard(context);
        card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12));
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        FrameLayout icon = makeIconBadge(context, iconRes, color);
        card.addView(icon, LayoutHelper.createLinear(36, 36, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        t.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        t.setGravity(Gravity.CENTER);
        card.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView b = new TextView(context);
        b.setText(body);
        b.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        b.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        b.setGravity(Gravity.CENTER);
        b.setLineSpacing(AndroidUtilities.dp(1), 1f);
        card.addView(b, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return card;
    }

    private FrameLayout makeIconBadge(Context context, int iconRes, int color) {
        FrameLayout wrap = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(11));
        bg.setColor(color);
        wrap.setBackground(bg);
        ImageView icon = new ImageView(context);
        try {
            icon.setImageResource(iconRes);
            icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        } catch (Exception ignored) {}
        wrap.addView(icon, LayoutHelper.createFrame(20, 20, Gravity.CENTER));
        return wrap;
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

        FrameLayout iconWrap = makeIconBadge(context, iconRes, iconColor);
        row.addView(iconWrap, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout textCol = new LinearLayout(context);
        textCol.setOrientation(LinearLayout.VERTICAL);

        TextView t = new TextView(context);
        t.setText(title);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        t.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        textCol.addView(t);

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
