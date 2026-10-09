package org.nayagram.platform.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
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
 * NayaAboutActivity — premium visual About page.
 * Policy-safe: independent client disclosure, no fake stats, clear privacy links.
 */
public class NayaAboutActivity extends BaseFragment {

    private static final String SUPPORT_EMAIL = "support.nayagram@gmail.com";
    private static final String PRIVACY_URL = "https://nayagramplatform.github.io/PrivacyArticleCenter/";
    private static final String TG_CHANNEL = "NayaGramPro";
    private static final String X_HANDLE = "NayaGramPro";
    private static final String YT_HANDLE = "NayaGramPro";
    private static final String FB_HANDLE = "NayaGramPro";

    private static final int C_BLUE = 0xFF0D80ED;
    private static final int C_BLUE_DARK = 0xFF0652C5;
    private static final int C_INDIGO = 0xFF3949AB;
    private static final int C_TEAL = 0xFF00BFA5;
    private static final int C_PURPLE = 0xFF7C4DFF;
    private static final int C_PINK = 0xFFEC407A;
    private static final int C_ORANGE = 0xFFFF8A00;
    private static final int C_GREEN = 0xFF43A047;
    private static final int C_CYAN = 0xFF00ACC1;
    private static final int C_RED = 0xFFE53935;

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

        ScrollView scrollView = new ScrollView(context);
        scrollView.setFillViewport(true);
        scrollView.setVerticalScrollBarEnabled(false);
        scrollView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        ((FrameLayout) fragmentView).addView(scrollView, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.MATCH_PARENT));

        LinearLayout root = new LinearLayout(context);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(36));
        scrollView.addView(root, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));

        // ════════════ HERO ════════════
        FrameLayout heroFrame = new FrameLayout(context);
        GradientDrawable heroBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0D80ED, 0xFF5B6CFF, 0xFF7C4DFF}
        );
        heroBg.setCornerRadius(AndroidUtilities.dp(22));
        heroFrame.setBackground(heroBg);
        if (Build.VERSION.SDK_INT >= 21) {
            heroFrame.setElevation(AndroidUtilities.dp(6));
        }

        LinearLayout hero = new LinearLayout(context);
        hero.setOrientation(LinearLayout.VERTICAL);
        hero.setGravity(Gravity.CENTER_HORIZONTAL);
        hero.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(34), AndroidUtilities.dp(20), AndroidUtilities.dp(26));
        heroFrame.addView(hero, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        // Soft glow circle behind logo
        FrameLayout logoOuter = new FrameLayout(context);
        GradientDrawable glow = new GradientDrawable();
        glow.setShape(GradientDrawable.OVAL);
        glow.setColors(new int[]{0x55FFFFFF, 0x00FFFFFF});
        logoOuter.setBackground(glow);

        FrameLayout logoInner = new FrameLayout(context);
        GradientDrawable logoCircle = new GradientDrawable();
        logoCircle.setShape(GradientDrawable.OVAL);
        logoCircle.setColor(0x33FFFFFF);
        logoCircle.setStroke(AndroidUtilities.dp(2), 0x66FFFFFF);
        logoInner.setBackground(logoCircle);

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.nayagram_intro_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        logoInner.addView(logo, LayoutHelper.createFrame(78, 78, Gravity.CENTER));
        logoOuter.addView(logoInner, LayoutHelper.createFrame(112, 112, Gravity.CENTER));
        hero.addView(logoOuter, LayoutHelper.createLinear(128, 128, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 14));

        TextView name = new TextView(context);
        name.setText("NayaGram");
        name.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 26);
        name.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        name.setTextColor(Color.WHITE);
        name.setGravity(Gravity.CENTER);
        name.setLetterSpacing(0.02f);
        hero.addView(name, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 2));

        TextView tagline = new TextView(context);
        tagline.setText("✦  Secure · Modern · Independent  ✦");
        tagline.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        tagline.setTextColor(0xDDFFFFFF);
        tagline.setGravity(Gravity.CENTER);
        hero.addView(tagline, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 18));

        // Glass version pills
        LinearLayout versionRow = new LinearLayout(context);
        versionRow.setOrientation(LinearLayout.HORIZONTAL);
        versionRow.setGravity(Gravity.CENTER);
        versionRow.addView(makeGlassPill(context, "VERSION", getAppVersion()), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 0, 0, 8, 0));
        versionRow.addView(makeGlassPill(context, "CORE", "Telegram 12.x"), LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, 8, 0, 0, 0));
        hero.addView(versionRow, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        root.addView(heroFrame, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // ════════════ FEATURE CHIPS (horizontal) ════════════
        HorizontalScrollView chipsScroll = new HorizontalScrollView(context);
        chipsScroll.setHorizontalScrollBarEnabled(false);
        chipsScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        LinearLayout chips = new LinearLayout(context);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setPadding(AndroidUtilities.dp(2), 0, AndroidUtilities.dp(2), 0);
        chips.addView(makeFeatureChip(context, R.drawable.msg_secret, C_TEAL, "E2E Secret"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        chips.addView(makeFeatureChip(context, R.drawable.outline_shield_check, C_BLUE, "Privacy First"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        chips.addView(makeFeatureChip(context, R.drawable.msg_folders, C_PURPLE, "Smart Tools"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        chips.addView(makeFeatureChip(context, R.drawable.ic_naya_fingerprint, C_PINK, "Biometric"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 8, 0));
        chips.addView(makeFeatureChip(context, R.drawable.outline_ai_translate2, C_ORANGE, "Translate"), LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 0));
        chipsScroll.addView(chips);
        root.addView(chipsScroll, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // ════════════ DISCLOSURE ════════════
        LinearLayout notice = makeSoftCard(context, 0x140D80ED);
        notice.setOrientation(LinearLayout.HORIZONTAL);
        notice.setGravity(Gravity.CENTER_VERTICAL);
        notice.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14), AndroidUtilities.dp(14));

        FrameLayout nIcon = makeRoundIcon(context, R.drawable.outline_shield_check, C_BLUE, 40);
        notice.addView(nIcon, LayoutHelper.createLinear(40, 40, Gravity.TOP, 0, 0, 12, 0));

        LinearLayout nText = new LinearLayout(context);
        nText.setOrientation(LinearLayout.VERTICAL);
        TextView nTitle = boldText(context, "Independent messaging client", 14, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        TextView nBody = normalText(context,
                "NayaGram is not the official Telegram app. It uses the Telegram API under applicable terms. Telegram® belongs to Telegram FZ-LLC.",
                12, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        nText.addView(nTitle);
        nText.addView(nBody, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 4, 0, 0));
        notice.addView(nText, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        root.addView(notice, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // ════════════ COMMITMENTS ════════════
        root.addView(makeSectionHeader(context, "Why NayaGram"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout pillars = new LinearLayout(context);
        pillars.setOrientation(LinearLayout.HORIZONTAL);
        pillars.addView(makePillarCard(context, R.drawable.msg_secret, C_TEAL, "Encrypted", "Secret chats stay end-to-end on your device"), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 0, 0, 6, 0));
        pillars.addView(makePillarCard(context, R.drawable.msg_policy, C_PURPLE, "Honest", "Clear labels. No ghost or anti-delete tricks."), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 3, 0, 3, 0));
        pillars.addView(makePillarCard(context, R.drawable.ic_lock_header, C_GREEN, "Compliant", "Designed for Play Store & API rules"), LayoutHelper.createLinear(0, LayoutHelper.MATCH_PARENT, 1f, 6, 0, 0, 0));
        root.addView(pillars, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // ════════════ PRIVACY ════════════
        root.addView(makeSectionHeader(context, "Privacy & control"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout privacy = makeWhiteCard(context);
        privacy.addView(makePrettyRow(context, R.drawable.settings_policy, C_BLUE, "Privacy Policy", "How data is handled — read in full", v -> openUrl(PRIVACY_URL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        privacy.addView(thinDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        privacy.addView(makePrettyRow(context, R.drawable.outline_shield_check, C_TEAL, "Delete my account", "Settings → Privacy → Delete my account", v -> {
            if (getParentActivity() != null) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "Open Settings → Privacy and Security → Delete my account").show();
            }
        }), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        privacy.addView(thinDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        privacy.addView(makePrettyRow(context, R.drawable.msg_policy, C_PURPLE, "No chat selling", "We do not sell messages or contacts", v -> openUrl(PRIVACY_URL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(privacy, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // ════════════ COMMUNITY ════════════
        root.addView(makeSectionHeader(context, "Community & updates"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout community = makeWhiteCard(context);
        community.addView(makePrettyRow(context, R.drawable.msg_channel, 0xFF2AABEE, "Telegram", "@" + TG_CHANNEL, v -> openUrl("https://t.me/" + TG_CHANNEL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(thinDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        community.addView(makePrettyRow(context, R.drawable.msg_link, 0xFF1DA1F2, "Twitter / X", "@" + X_HANDLE, v -> openUrl("https://x.com/" + X_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(thinDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        community.addView(makePrettyRow(context, R.drawable.msg_link, C_RED, "YouTube", "@" + YT_HANDLE, v -> openUrl("https://youtube.com/@" + YT_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        community.addView(thinDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
        community.addView(makePrettyRow(context, R.drawable.msg_link, 0xFF1877F2, "Facebook", "@" + FB_HANDLE, v -> openUrl("https://facebook.com/" + FB_HANDLE)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        root.addView(community, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // ════════════ SUPPORT CTA ════════════
        root.addView(makeSectionHeader(context, "Support"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        // Big primary CTA
        TextView cta = new TextView(context);
        cta.setText("✉  Contact support");
        cta.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        cta.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        cta.setTextColor(Color.WHITE);
        cta.setGravity(Gravity.CENTER);
        GradientDrawable ctaBg = new GradientDrawable(
                GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{C_BLUE, C_PURPLE}
        );
        ctaBg.setCornerRadius(AndroidUtilities.dp(14));
        cta.setBackground(ctaBg);
        cta.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(14), AndroidUtilities.dp(16), AndroidUtilities.dp(14));
        if (Build.VERSION.SDK_INT >= 21) cta.setElevation(AndroidUtilities.dp(3));
        cta.setOnClickListener(v -> openEmail());
        root.addView(cta, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout support = makeWhiteCard(context);
        support.addView(makePrettyRow(context, R.drawable.msg_msgbubble, C_GREEN, "Email", SUPPORT_EMAIL, v -> openEmail()), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        if (BuildVars.PLAYSTORE_APP_URL != null && !BuildVars.PLAYSTORE_APP_URL.isEmpty()) {
            support.addView(thinDivider(context), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, 1, 58, 0, 0, 0));
            support.addView(makePrettyRow(context, R.drawable.msg_premium_liststar, C_ORANGE, "Rate on Google Play", "Your honest review helps a lot", v -> openUrl(BuildVars.PLAYSTORE_APP_URL)), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        }
        root.addView(support, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 16));

        // ════════════ PLATFORM ════════════
        root.addView(makeSectionHeader(context, "Platform"), LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 10));

        LinearLayout platform = makeWhiteCard(context);
        platform.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16), AndroidUtilities.dp(16));

        LinearLayout platHeader = new LinearLayout(context);
        platHeader.setOrientation(LinearLayout.HORIZONTAL);
        platHeader.setGravity(Gravity.CENTER_VERTICAL);
        FrameLayout platIcon = makeRoundIcon(context, R.drawable.ic_naya_info, C_BLUE, 36);
        platHeader.addView(platIcon, LayoutHelper.createLinear(36, 36, 0, 0, 10, 0));
        LinearLayout platTitles = new LinearLayout(context);
        platTitles.setOrientation(LinearLayout.VERTICAL);
        platTitles.addView(boldText(context, "NayaGram Platform", 15, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)));
        TextView meta = normalText(context, "Built with care in Bangladesh 🇧🇩", 12, C_BLUE);
        platTitles.addView(meta, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        platHeader.addView(platTitles, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f));
        platform.addView(platHeader, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 12));

        TextView body = normalText(context,
                "We ship features that are labeled clearly and designed to respect Telegram API requirements and Google Play policies. Future ads, if any, will be disclosed in the Privacy Policy and inside the app.",
                13, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        platform.addView(body);
        root.addView(platform, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 20));

        // Footer
        TextView footer = new TextView(context);
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        footer.setText("© " + year + " NayaGram Platform\nNot affiliated with Telegram FZ-LLC\nAll rights reserved");
        footer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 11);
        footer.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText3));
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(AndroidUtilities.dp(3), 1f);
        root.addView(footer);

        return fragmentView;
    }

    // ── helpers ──────────────────────────────────────────────

    private LinearLayout makeWhiteCard(Context context) {
        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) {
            card.setElevation(AndroidUtilities.dp(2));
            card.setClipToOutline(true);
        }
        return card;
    }

    private LinearLayout makeSoftCard(Context context, int tint) {
        LinearLayout card = new LinearLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(16));
        bg.setStroke(AndroidUtilities.dp(1), tint);
        card.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) card.setElevation(AndroidUtilities.dp(1));
        return card;
    }

    private LinearLayout makeSectionHeader(Context context, String title) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        View bar = new View(context);
        GradientDrawable barBg = new GradientDrawable();
        barBg.setColor(C_BLUE);
        barBg.setCornerRadius(AndroidUtilities.dp(3));
        bar.setBackground(barBg);
        row.addView(bar, LayoutHelper.createLinear(4, 16, 0, 0, 8, 0));

        TextView t = boldText(context, title, 14, Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        row.addView(t);
        return row;
    }

    private LinearLayout makeGlassPill(Context context, String label, String value) {
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.VERTICAL);
        chip.setGravity(Gravity.CENTER);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x28FFFFFF);
        bg.setCornerRadius(AndroidUtilities.dp(14));
        bg.setStroke(AndroidUtilities.dp(1), 0x40FFFFFF);
        chip.setBackground(bg);
        chip.setPadding(AndroidUtilities.dp(12), AndroidUtilities.dp(10), AndroidUtilities.dp(12), AndroidUtilities.dp(10));

        TextView l = new TextView(context);
        l.setText(label);
        l.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 10);
        l.setTextColor(0xB3FFFFFF);
        l.setGravity(Gravity.CENTER);
        l.setLetterSpacing(0.08f);
        chip.addView(l);

        TextView v = new TextView(context);
        v.setText(value);
        v.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        v.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        v.setTextColor(Color.WHITE);
        v.setGravity(Gravity.CENTER);
        chip.addView(v, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        return chip;
    }

    private LinearLayout makeFeatureChip(Context context, int iconRes, int color, String label) {
        LinearLayout chip = new LinearLayout(context);
        chip.setOrientation(LinearLayout.HORIZONTAL);
        chip.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        bg.setCornerRadius(AndroidUtilities.dp(20));
        chip.setBackground(bg);
        if (Build.VERSION.SDK_INT >= 21) chip.setElevation(AndroidUtilities.dp(2));
        chip.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(8), AndroidUtilities.dp(14), AndroidUtilities.dp(8));

        FrameLayout icon = makeRoundIcon(context, iconRes, color, 28);
        chip.addView(icon, LayoutHelper.createLinear(28, 28, 0, 0, 8, 0));

        TextView t = boldText(context, label, 12, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        chip.addView(t);
        return chip;
    }

    private LinearLayout makePillarCard(Context context, int iconRes, int color, String title, String body) {
        LinearLayout card = makeWhiteCard(context);
        card.setPadding(AndroidUtilities.dp(10), AndroidUtilities.dp(14), AndroidUtilities.dp(10), AndroidUtilities.dp(14));
        card.setGravity(Gravity.CENTER_HORIZONTAL);

        // Top accent line
        View accent = new View(context);
        GradientDrawable aBg = new GradientDrawable();
        aBg.setColor(color);
        aBg.setCornerRadius(AndroidUtilities.dp(2));
        accent.setBackground(aBg);
        card.addView(accent, LayoutHelper.createLinear(24, 3, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 10));

        FrameLayout icon = makeRoundIcon(context, iconRes, color, 36);
        card.addView(icon, LayoutHelper.createLinear(36, 36, Gravity.CENTER_HORIZONTAL, 0, 0, 0, 8));

        TextView t = boldText(context, title, 12, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        t.setGravity(Gravity.CENTER);
        card.addView(t, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 0, 0, 4));

        TextView b = normalText(context, body, 10, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        b.setGravity(Gravity.CENTER);
        card.addView(b, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return card;
    }

    private LinearLayout makePrettyRow(Context context, int iconRes, int color, String title, String subtitle, View.OnClickListener click) {
        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(13), AndroidUtilities.dp(14), AndroidUtilities.dp(13));
        row.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector), 2));
        row.setOnClickListener(click);

        FrameLayout icon = makeRoundIcon(context, iconRes, color, 36);
        row.addView(icon, LayoutHelper.createLinear(36, 36, Gravity.CENTER_VERTICAL, 0, 0, 12, 0));

        LinearLayout col = new LinearLayout(context);
        col.setOrientation(LinearLayout.VERTICAL);
        col.addView(boldText(context, title, 15, Theme.getColor(Theme.key_windowBackgroundWhiteBlackText)));
        TextView s = normalText(context, subtitle, 12, Theme.getColor(Theme.key_windowBackgroundWhiteGrayText2));
        col.addView(s, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 2, 0, 0));
        row.addView(col, LayoutHelper.createLinear(0, LayoutHelper.WRAP_CONTENT, 1f, Gravity.CENTER_VERTICAL));

        ImageView chevron = new ImageView(context);
        try {
            chevron.setImageResource(R.drawable.msg_arrowright);
            chevron.setColorFilter(new PorterDuffColorFilter(0xFFB0B8C4, PorterDuff.Mode.SRC_IN));
        } catch (Exception ignored) {}
        row.addView(chevron, LayoutHelper.createLinear(16, 16, Gravity.CENTER_VERTICAL, 6, 0, 0, 0));
        return row;
    }

    private FrameLayout makeRoundIcon(Context context, int iconRes, int color, int sizeDp) {
        FrameLayout wrap = new FrameLayout(context);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(AndroidUtilities.dp(sizeDp * 0.28f));
        // Soft tinted background + solid icon color
        int soft = Color.argb(28, Color.red(color), Color.green(color), Color.blue(color));
        bg.setColor(soft);
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

    private View thinDivider(Context context) {
        View d = new View(context);
        d.setBackgroundColor(Theme.getColor(Theme.key_divider));
        return d;
    }

    private TextView boldText(Context context, String text, int sp, int color) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sp);
        t.setTypeface(AndroidUtilities.getTypeface("fonts/rmedium.ttf"));
        t.setTextColor(color);
        return t;
    }

    private TextView normalText(Context context, String text, int sp, int color) {
        TextView t = new TextView(context);
        t.setText(text);
        t.setTextSize(TypedValue.COMPLEX_UNIT_DIP, sp);
        t.setTextColor(color);
        t.setLineSpacing(AndroidUtilities.dp(2), 1f);
        return t;
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
