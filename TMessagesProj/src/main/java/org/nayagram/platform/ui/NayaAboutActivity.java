package org.nayagram.platform.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
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
 * Premium About page — large logo, soft cards, policy-safe disclosure.
 */
public class NayaAboutActivity extends BaseFragment {

    private static final String SUPPORT_EMAIL = "support.nayagram@gmail.com";
    private static final String PRIVACY_URL = "https://nayagramplatform.github.io/PrivacyArticleCenter/";
    private static final String SUPER_ADMIN_HANDLE = "NG_HuzayfaSheikh";
    private static final String TG_CHANNEL = "NayaGramPro";
    private static final String X_HANDLE = "NayaGramPro";
    private static final String YT_HANDLE = "NayaGramPro";
    private static final String FB_HANDLE = "NayaGramPro";

    private static final int C_CYAN = 0xFF00D4FF;
    private static final int C_BLUE = 0xFF0284C7;
    private static final int C_BLUE2 = 0xFF4C6FFF;
    private static final int C_PURPLE = 0xFF7C3AED;
    private static final int C_TEAL = 0xFF00BFA5;
    private static final int C_GREEN = 0xFF10B981;
    private static final int C_PINK = 0xFFEC407A;
    private static final int C_RED = 0xFFE53935;
    private static final int C_TG = 0xFF2AABEE;

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("About");
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        fragmentView = new FrameLayout(context);
        fragmentView.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        ((FrameLayout) fragmentView).addView(scroll, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(6), AndroidUtilities.dp(16), AndroidUtilities.dp(40));
        scroll.addView(root, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // ════════ HERO ════════
        FrameLayout heroFrame = new FrameLayout(context);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0A84FF, 0xFF5B6CFF, 0xFF9B59FF}
        );
        heroBg.setCornerRadius(AndroidUtilities.dp(24));
        heroFrame.setBackground(heroBg);
        if (Build.VERSION.SDK_INT >= 21) heroFrame.setElevation(AndroidUtilities.dp(8));

        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(AndroidUtilities.dp(22), AndroidUtilities.dp(36), AndroidUtilities.dp(22), AndroidUtilities.dp(28));
        heroFrame.addView(hero, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Large logo with soft glow
        FrameLayout logoOuter = new FrameLayout(context);
        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setColors(new int[]{0x66FFFFFF, 0x00FFFFFF});
        logoOuter.setBackground(glow);

        FrameLayout logoInner = new FrameLayout(context);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(0x2EFFFFFF);
        ring.setStroke(AndroidUtilities.dp(3), 0x88FFFFFF);
        logoInner.setBackground(ring);

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.nayagram_intro_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logoInner.addView(logo, LayoutHelper.createFrame(126, 126, Gravity.CENTER));
        logoOuter.addView(logoInner, LayoutHelper.createFrame(138, 138, Gravity.CENTER));
        hero.addView(logoOuter, LayoutHelper.createLinear(154, 154, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 16));

        TextView name = new TextView(context);
        name.setText("NayaGram");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 28);
        name.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        name.setTextColor(Color.WHITE);
        name.setGravity(Gravity.CENTER);
        name.setLetterSpacing(0.03f);
        hero.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView tag = new TextView(context);
        tag.setText("Secure  ·  Modern  ·  Independent");
        tag.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
        tag.setTextColor(0xE6FFFFFF);
        tag.setGravity(Gravity.CENTER);
        hero.addView(tag, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // Version row — Novagram style dual cards
        LinearLayout verRow = new LinearLayout(context);
        verRow.setOrientation(LinearLayout.HORIZONTAL);
        verRow.addView(makeVerCard(context, "NayaGram", getAppVersion(), "App version"), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 8, 0));
        verRow.addView(makeVerCard(context, "Telegram", "12.x", "Core engine"), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 8, 0, 0, 0));
        hero.addView(verRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(heroFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ════════ CHIPS ════════
        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chips = new LinearLayout(context);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(AndroidUtilities.dp(2), 0, AndroidUtilities.dp(2), 0);
        chips.addView(chip(context, R.drawable.msg_secret, C_TEAL, "E2E Secret"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        chips.addView(chip(context, R.drawable.outline_shield_check, C_BLUE, "Privacy First"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        chips.addView(chip(context, R.drawable.msg_folders, C_PURPLE, "Smart Tools"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        chips.addView(chip(context, R.drawable.ic_naya_fingerprint, C_PINK, "Biometric"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        chips.addView(chip(context, R.drawable.outline_ai_translate2, C_GREEN, "Translate"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT));
        chipsScroll.addView(chips);
        root.addView(chipsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ════════ DISCLOSURE ════════
        LinearLayout notice = whiteCard(context);
        notice.setOrientation(LinearLayout.HORIZONTAL);
        notice.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));
        notice.setGravity(Gravity.CENTER_VERTICAL);
        // soft blue stroke
        GradientDrawable nBg = new GradientDrawable();
        nBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        nBg.setCornerRadius(AndroidUtilities.dp(16));
        nBg.setStroke(AndroidUtilities.dp(1), 0x330D80ED);
        notice.setBackground(nBg);

        notice.addView(iconBadge(context, R.drawable.outline_shield_check, C_BLUE, 40), LayoutHelper.createLinear(40, 40, Gravity.TOP, 0, 0, 12, 0));
        LinearLayout nCol = new LinearLayout(context);
        nCol.setOrientation(LinearLayout.VERTICAL);
        nCol.addView(bold(context, "Independent messaging client", 14, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)));
        TextView nBody = normal(context, "NayaGram is not the official Telegram app. It uses the Telegram API under applicable terms. Telegram® belongs to Telegram FZ-LLC.", 12, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        nCol.addView(nBody, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        notice.addView(nCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        root.addView(notice, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ════════ WHY ════════
        root.addView(section(context, "Why NayaGram"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        LinearLayout pillars = new LinearLayout(context);
        pillars.setOrientation(LinearLayout.HORIZONTAL);
        pillars.addView(pillar(context, R.drawable.msg_secret, C_TEAL, "Encrypted", "Secret chats stay end-to-end on your device"), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 0, 0, 6, 0));
        pillars.addView(pillar(context, R.drawable.msg_policy, C_PURPLE, "Honest", "Clear labels. No ghost or anti-delete tricks."), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 3, 0, 3, 0));
        pillars.addView(pillar(context, R.drawable.ic_lock_header, C_GREEN, "Compliant", "Designed for Play Store & API rules"), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 6, 0, 0, 0));
        root.addView(pillars, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ════════ PRIVACY ════════
        root.addView(section(context, "Privacy & control"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        LinearLayout privacy = whiteCard(context);
        privacy.addView(row(context, R.drawable.settings_policy, C_BLUE, "Privacy Policy", "What we collect and why", v -> openUrl(PRIVACY_URL)));
        privacy.addView(div(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        privacy.addView(row(context, R.drawable.outline_shield_check, C_TEAL, "Delete my account", "Settings → Privacy → Delete my account", v -> {
            if (getParentActivity() != null)
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "Open Settings → Privacy and Security → Delete my account").show();
        }));
        privacy.addView(div(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        privacy.addView(row(context, R.drawable.msg_policy, C_PURPLE, "No chat selling", "We do not sell messages or contacts", v -> openUrl(PRIVACY_URL)));
        root.addView(privacy, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ════════ SUPER ADMIN ════════
        root.addView(section(context, "Administration"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout adminBtnCard = new LinearLayout(context);
        adminBtnCard.setOrientation(LinearLayout.HORIZONTAL);
        adminBtnCard.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable adminBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{0xFF0284C7, 0xFF7C3AED}
        );
        adminBg.setCornerRadius(AndroidUtilities.dp(18));
        adminBg.setStroke(AndroidUtilities.dp(1.5f), C_CYAN);
        adminBtnCard.setBackground(adminBg);
        adminBtnCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        if (Build.VERSION.SDK_INT >= 21) adminBtnCard.setElevation(AndroidUtilities.dp(4));
        adminBtnCard.setOnClickListener(v -> openSuperAdmin());

        FrameLayout adminIconWrap = new FrameLayout(context);
        GradientDrawable adminIconBg = new GradientDrawable();
        adminIconBg.setShape(GradientDrawable.OVAL);
        adminIconBg.setColor(0x33FFFFFF);
        adminIconBg.setStroke(AndroidUtilities.dp(1), 0x80FFFFFF);
        adminIconWrap.setBackground(adminIconBg);
        ImageView adminIcon = new ImageView(context);
        adminIcon.setImageResource(R.drawable.msg_admins);
        adminIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        adminIconWrap.addView(adminIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));
        adminBtnCard.addView(adminIconWrap, LayoutHelper.createLinear(44, 44, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout adminCol = new LinearLayout(context);
        adminCol.setOrientation(LinearLayout.VERTICAL);
        TextView adminRoleBadge = bold(context, "👑 SUPER ADMIN", 11, C_CYAN);
        adminCol.addView(adminRoleBadge);
        TextView adminHandleView = bold(context, "@" + SUPER_ADMIN_HANDLE, 16, Color.WHITE);
        adminCol.addView(adminHandleView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        TextView adminSubView = normal(context, "Official Super Admin · Tap to message directly", 12, 0xE6FFFFFF);
        adminCol.addView(adminSubView, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        adminBtnCard.addView(adminCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        TextView adminActionPill = bold(context, "Chat →", 13, Color.WHITE);
        GradientDrawable adminPillBg = new GradientDrawable();
        adminPillBg.setColor(0x33FFFFFF);
        adminPillBg.setCornerRadius(AndroidUtilities.dp(14));
        adminPillBg.setStroke(AndroidUtilities.dp(1), C_CYAN);
        adminActionPill.setBackground(adminPillBg);
        adminActionPill.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
        adminBtnCard.addView(adminActionPill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
        root.addView(adminBtnCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ════════ COMMUNITY ════════
        root.addView(section(context, "Community & updates"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        LinearLayout community = whiteCard(context);
        community.addView(row(context, R.drawable.msg_admins, C_PURPLE, "Super Admin", "@" + SUPER_ADMIN_HANDLE, v -> openSuperAdmin()));
        community.addView(div(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        community.addView(row(context, R.drawable.ic_social_telegram, C_TG, "Telegram", "@" + TG_CHANNEL, v -> openUrl("https://t.me/" + TG_CHANNEL)));
        community.addView(div(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        community.addView(row(context, R.drawable.ic_social_x, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), "Twitter / X", "@" + X_HANDLE, v -> openUrl("https://x.com/" + X_HANDLE)));
        community.addView(div(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        community.addView(row(context, R.drawable.ic_social_youtube, C_RED, "YouTube", "@" + YT_HANDLE, v -> openUrl("https://youtube.com/@" + YT_HANDLE)));
        community.addView(div(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        community.addView(row(context, R.drawable.ic_social_facebook, 0xFF1877F2, "Facebook", "@" + FB_HANDLE, v -> openUrl("https://facebook.com/" + FB_HANDLE)));
        root.addView(community, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ════════ RATING & SUPPORT CTA ════════
        root.addView(section(context, "Support & Rating"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        if (BuildVars.PLAYSTORE_APP_URL != null && !BuildVars.PLAYSTORE_APP_URL.isEmpty()) {
            LinearLayout rateBtnCard = new LinearLayout(context);
            rateBtnCard.setOrientation(LinearLayout.HORIZONTAL);
            rateBtnCard.setGravity(Gravity.CENTER_VERTICAL);
            GradientDrawable rateBg = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{0xFF00B4D8, C_PURPLE, C_GREEN}
            );
            rateBg.setCornerRadius(AndroidUtilities.dp(18));
            rateBg.setStroke(AndroidUtilities.dp(1.5f), C_CYAN);
            rateBtnCard.setBackground(rateBg);
            rateBtnCard.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
            if (Build.VERSION.SDK_INT >= 21) rateBtnCard.setElevation(AndroidUtilities.dp(4));
            rateBtnCard.setOnClickListener(v -> openUrl(BuildVars.PLAYSTORE_APP_URL));

            FrameLayout rateIconWrap = new FrameLayout(context);
            GradientDrawable rateIconBg = new GradientDrawable();
            rateIconBg.setShape(GradientDrawable.OVAL);
            rateIconBg.setColor(0x33FFFFFF);
            rateIconBg.setStroke(AndroidUtilities.dp(1), 0x80FFFFFF);
            rateIconWrap.setBackground(rateIconBg);
            ImageView rateIcon = new ImageView(context);
            rateIcon.setImageResource(R.drawable.msg_premium_liststar);
            rateIcon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
            rateIconWrap.addView(rateIcon, LayoutHelper.createFrame(24, 24, Gravity.CENTER));
            rateBtnCard.addView(rateIconWrap, LayoutHelper.createLinear(44, 44, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

            LinearLayout rateCol = new LinearLayout(context);
            rateCol.setOrientation(LinearLayout.VERTICAL);
            TextView rateStars = bold(context, "★ ★ ★ ★ ★   OFFICIAL RATING", 11, C_CYAN);
            rateCol.addView(rateStars);
            TextView rateTitle = bold(context, "Rate NayaGram on Google Play", 16, Color.WHITE);
            rateCol.addView(rateTitle, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
            TextView rateSub = normal(context, "Share your honest 5-star review & support our team", 12, 0xE6FFFFFF);
            rateCol.addView(rateSub, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
            rateBtnCard.addView(rateCol, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

            TextView ratePill = bold(context, "Rate →", 13, Color.WHITE);
            GradientDrawable ratePillBg = new GradientDrawable();
            ratePillBg.setColor(0x33FFFFFF);
            ratePillBg.setCornerRadius(AndroidUtilities.dp(14));
            ratePillBg.setStroke(AndroidUtilities.dp(1), C_CYAN);
            ratePill.setBackground(ratePillBg);
            ratePill.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(6), AndroidUtilities.dp(12), AndroidUtilities.dp(6));
            rateBtnCard.addView(ratePill, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_VERTICAL, 8, 0, 0, 0));
            root.addView(rateBtnCard, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        }

        TextView cta = new TextView(context);
        cta.setText("✉   Contact support");
        cta.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        cta.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        cta.setTextColor(Color.WHITE);
        cta.setGravity(Gravity.CENTER);
        GradientDrawable ctaBg = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{C_BLUE, C_PURPLE});
        ctaBg.setCornerRadius(AndroidUtilities.dp(16));
        cta.setBackground(ctaBg);
        cta.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(15), AndroidUtilities.dp(16), AndroidUtilities.dp(15));
        if (Build.VERSION.SDK_INT >= 21) cta.setElevation(AndroidUtilities.dp(4));
        cta.setOnClickListener(v -> openEmail());
        root.addView(cta, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        LinearLayout support = whiteCard(context);
        support.addView(row(context, R.drawable.msg_msgbubble, C_GREEN, "Email", SUPPORT_EMAIL, v -> openEmail()));
        if (BuildVars.PLAYSTORE_APP_URL != null && !BuildVars.PLAYSTORE_APP_URL.isEmpty()) {
            support.addView(div(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
            support.addView(row(context, R.drawable.msg_premium_liststar, C_PURPLE, "Rate on Google Play", "Share honest feedback", v -> openUrl(BuildVars.PLAYSTORE_APP_URL)));
        }
        root.addView(support, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // ════════ PLATFORM ════════
        root.addView(section(context, "Platform"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));
        LinearLayout platform = whiteCard(context);
        platform.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));
        LinearLayout pHead = new LinearLayout(context);
        pHead.setOrientation(LinearLayout.HORIZONTAL);
        pHead.setGravity(Gravity.CENTER_VERTICAL);
        pHead.addView(iconBadge(context, R.drawable.ic_naya_info, C_BLUE, 40), LayoutHelper.createLinear(40, 40, 0, 0, 12, 0));
        LinearLayout pTitles = new LinearLayout(context);
        pTitles.setOrientation(LinearLayout.VERTICAL);
        pTitles.addView(bold(context, "NayaGram Platform", 16, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)));
        pTitles.addView(normal(context, "Built with care in Bangladesh 🇧🇩", 12, C_BLUE), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        pHead.addView(pTitles, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        platform.addView(pHead, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));
        platform.addView(normal(context, "Features are labeled clearly and designed to respect Telegram API requirements and Google Play policies. Future ads, if any, will be disclosed in the Privacy Policy and inside the app.", 13, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2)));
        root.addView(platform, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 22));

        TextView footer = new TextView(context);
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        footer.setText("NayaGram v" + getAppVersion() + " • Build " + getInternalBuildCode() + "\n© " + year + " NayaGram Platform\nNot affiliated with Telegram FZ-LLC\nAll rights reserved");
        footer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        footer.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(AndroidUtilities.dp(3), 1f);
        root.addView(footer);

        return fragmentView;
    }

    private LinearLayout makeVerCard(Context context, String brand, String value, String hint) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x28FFFFFF);
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setStroke(AndroidUtilities.dp(1), 0x44FFFFFF);
        card.setBackground(bg);
        card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12));

        TextView b = new TextView(context);
        b.setText(brand);
        b.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        b.setTextColor(0xB3FFFFFF);
        b.setGravity(Gravity.CENTER);
        card.addView(b);

        TextView v = new TextView(context);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
        v.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        card.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));

        TextView h = new TextView(context);
        h.setText(hint);
        h.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        h.setTextColor(0x99FFFFFF);
        h.setGravity(Gravity.CENTER);
        card.addView(h, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        return card;
    }

    private LinearLayout whiteCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        boolean dark = Theme.isCurrentThemeDark();
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setStroke(AndroidUtilities.dp(1), dark ? 0x4400D4FF : 0xFFBAE6FD);
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(AndroidUtilities.dp(2));
            card.setClipToOutline(true);
        }
        return card;
    }

    private LinearLayout section(Context context, String title) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View bar = new View(context);
        GradientDrawable b = new GradientDrawable();
        b.setColor(C_BLUE);
        b.setCornerRadius(AndroidUtilities.dp(3));
        bar.setBackground(b);
        row.addView(bar, LayoutHelper.createLinear(4, 16, 0, 0, 8, 0));
        row.addView(bold(context, title, 14, Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader)));
        return row;
    }

    private LinearLayout chip(Context context, int iconRes, int color, String label) {
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(22));
        chip.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) chip.setElevation(AndroidUtilities.dp(2));
        chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));
        chip.addView(iconBadge(context, iconRes, color, 28), LayoutHelper.createLinear(28, 28, 0, 0, 8, 0));
        chip.addView(bold(context, label, 12, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)));
        return chip;
    }

    private LinearLayout pillar(Context context, int iconRes, int color, String title, String body) {
        LinearLayout card = whiteCard(context);
        card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14));
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        View accent = new View(context);
        GradientDrawable a = new GradientDrawable();
        a.setColor(color);
        a.setCornerRadius(AndroidUtilities.dp(2));
        accent.setBackground(a);
        card.addView(accent, LayoutHelper.createLinear(28, 3, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));
        card.addView(iconBadge(context, iconRes, color, 36), LayoutHelper.createLinear(36, 36, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));
        TextView t = bold(context, title, 12, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        t.setGravity(Gravity.CENTER);
        card.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));
        TextView b = normal(context, body, 10, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        b.setGravity(Gravity.CENTER);
        card.addView(b, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return card;
    }

    private LinearLayout row(Context context, int iconRes, int color, String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(13), AndroidUtilities.dp(14), AndroidUtilities.dp(13));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        row.setOnClickListener(click);
        row.addView(iconBadge(context, iconRes, color, 36), LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));
        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        boolean dark = Theme.isCurrentThemeDark();
        int titleColor = dark ? Theme.getColor(Theme.key_windowBackgroundWhiteBlackText) : color;
        col.addView(bold(context, title, 15, titleColor));
        col.addView(normal(context, subtitle, 12, dark ? 0xFFCBD5E1 : Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        row.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));
        ImageView chevron = new ImageView(context);
        try {
            chevron.setImageResource(R.drawable.msg_arrowright);
            chevron.setColorFilter(new PorterDuffColorFilter(0xFFB0B8C4, PorterDuff.Mode.SRC_IN));
        } catch (Exception ignored) {}
        row.addView(chevron, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));
        return row;
    }

    private FrameLayout iconBadge(Context context, int iconRes, int color, int sizeDp) {
        FrameLayout wrap = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(AndroidUtilities.dp(sizeDp * 0.28f));
        bg.setColor(Color.argb(28, Color.red(color), Color.green(color), Color.blue(color)));
        wrap.setBackground(bg);
        ImageView icon = new ImageView(context);
        try {
            icon.setImageResource(iconRes);
            icon.setColorFilter(new PorterDuffColorFilter(color, PorterDuff.Mode.SRC_IN));
        } catch (Exception ignored) {}
        int iconSize = Math.max(16, sizeDp - 14);
        wrap.addView(icon, LayoutHelper.createFrame(iconSize, iconSize, Gravity.CENTER));
        return wrap;
    }

    private View div(Context context) {
        View d = new View(context);
        d.setBackgroundColor(Theme.getColor(Theme.key_divider));
        return d;
    }

    private TextView bold(Context context, String text, int sp, int color) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sp);
        t.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        t.setTextColor(color);
        return t;
    }

    private TextView normal(Context context, String text, int sp, int color) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sp);
        t.setTextColor(color);
        t.setLineSpacing(AndroidUtilities.dp(2), 1f);
        return t;
    }

    private String getInternalBuildCode() {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager()
                    .getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return String.valueOf(pInfo.versionCode / 10);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "4018";
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager()
                    .getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            if (pInfo.versionName != null && !pInfo.versionName.isEmpty()) return pInfo.versionName;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "2.1.4 NG";
    }

    private void openSuperAdmin() {
        try {
            org.telegram.messenger.MessagesController.getInstance(currentAccount).openByUserName(SUPER_ADMIN_HANDLE, this, 1);
        } catch (Exception e) {
            openUrl("https://t.me/" + SUPER_ADMIN_HANDLE);
        }
    }

    private void openUrl(String url) {
        try {
            if (getParentActivity() != null) Browser.openUrl(getParentActivity(), url);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    private void openEmail() {
        try {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:" + SUPPORT_EMAIL));
            intent.putExtra(Intent.EXTRA_SUBJECT, "NayaGram Support");
            if (getParentActivity() != null)
                getParentActivity().startActivity(Intent.createChooser(intent, "Contact support"));
        } catch (Exception e) {
            if (getParentActivity() != null)
                BulletinFactory.of(this).createSimpleBulletin(R.raw.error, "No email app found").show();
            FileLog.e(e);
        }
    }
}
