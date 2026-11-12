package org.nayagram.platform.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffColorFilter;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewOutlineProvider;
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
import org.telegram.messenger.browser.Browser;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;

/**
 * NayaAboutActivity - Clean Novagram-style About screen.
 * Seamless gray backdrop & toolbar, crisp white rounded cards,
 * dual version chips, official channels, leadership, support & policy compliance.
 */
public class NayaAboutActivity extends BaseFragment {

    private static final String TELEGRAM_BASE = "13.1.4";
    private static final String SUPPORT_EMAIL = "support.nayagram@gmail.com";
    private static final String PRIVACY_URL = "https://nayagramplatform.github.io/PrivacyArticleCenter/";
    private static final String SUPER_ADMIN_HANDLE = "NGSupreme";
    private static final String ADMIN_2_HANDLE = "NG_HuzayfaSheikh";
    private static final String TG_CHANNEL = "NayaGramPro";
    private static final String X_HANDLE = "NayaGramPro";
    private static final String YT_HANDLE = "NayaGramPro";
    private static final String FB_HANDLE = "NayaGramPro";

    @Override
    public View createView(Context context) {
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setAllowOverlayTitle(true);
        actionBar.setTitle("About");
        actionBar.setCastShadows(false);
        actionBar.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));
        actionBar.setItemsColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText), false);
        actionBar.setItemsBackgroundColor(Theme.getColor(Theme.key_actionBarWhiteSelector), false);
        actionBar.setTitleColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setVerticalScrollBarEnabled(false);
        scroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundGray));

        LinearLayout column = new LinearLayout(context) {
            @Override
            protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
                int avail = MeasureSpec.getSize(widthMeasureSpec);
                int max = dp(560);
                int side = avail > max ? (avail - max) / 2 + dp(12) : dp(12);
                if (getPaddingLeft() != side || getPaddingRight() != side) {
                    setPadding(side, dp(10), side, dp(24));
                }
                super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            }
        };
        column.setOrientation(LinearLayout.VERTICAL);
        column.setPadding(dp(12), dp(10), dp(12), dp(24));

        // 1. Hero Card (Novagram AboutScreen.kt aesthetic)
        column.addView(buildHeroCard(context));

        // 2. Our Channels Card
        column.addView(buildChannelsCard(context), spacedCard());

        // 3. Support & Leadership Card
        column.addView(buildSupportCard(context), spacedCard());

        // 4. Platform & Privacy Card
        column.addView(buildPlatformCard(context), spacedCard());

        // Footer version & legal line
        TextView footer = new TextView(context);
        int year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
        footer.setText("NayaGram v" + getAppVersion() + " • Build " + getInternalBuildCode()
                + "\n© " + year + " NayaGram Platform · Not affiliated with Telegram FZ-LLC");
        footer.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        footer.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        footer.setGravity(Gravity.CENTER);
        footer.setLineSpacing(dp(3), 1f);
        footer.setPadding(dp(8), dp(18), dp(8), dp(8));
        column.addView(footer, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));

        scroll.addView(column, LayoutHelper.createScroll(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP));
        fragmentView = scroll;
        return fragmentView;
    }

    private View buildHeroCard(Context context) {
        LinearLayout card = card(context);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(dp(16), dp(22), dp(16), dp(20));

        ImageView logo = new ImageView(context);
        logo.setImageResource(R.drawable.nayagram_intro_logo);
        logo.setScaleType(ImageView.ScaleType.FIT_CENTER);
        GradientDrawable logoBg = new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{0xFF0A84FF, 0xFF3B82F6}
        );
        logoBg.setCornerRadius(dp(22));
        logo.setBackground(logoBg);
        logo.setPadding(dp(12), dp(12), dp(12), dp(12));
        if (Build.VERSION.SDK_INT >= 21) {
            logo.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(22));
                }
            });
            logo.setClipToOutline(true);
        }
        card.addView(logo, LayoutHelper.createLinear(92, 92, Gravity.CENTER_HORIZONTAL));

        TextView title = new TextView(context);
        title.setText("NayaGram");
        title.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        title.setTypeface(AndroidUtilities.bold());
        title.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 22);
        title.setGravity(Gravity.CENTER);
        card.addView(title, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 12, 0, 0));

        TextView subtitle = new TextView(context);
        subtitle.setText("Secure · Modern · Independent");
        subtitle.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        subtitle.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setMaxLines(1);
        subtitle.setEllipsize(TextUtils.TruncateAt.END);
        card.addView(subtitle, LayoutHelper.createLinear(LayoutHelper.WRAP_CONTENT, LayoutHelper.WRAP_CONTENT, Gravity.CENTER_HORIZONTAL, 0, 2, 0, 0));

        LinearLayout chips = new LinearLayout(context);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.setGravity(Gravity.CENTER);
        chips.addView(versionChip(context, "NayaGram", getAppVersion()), chipParams(true));
        chips.addView(versionChip(context, "Telegram", TELEGRAM_BASE), chipParams(false));
        card.addView(chips, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 18, 0, 0));

        return card;
    }

    private View versionChip(Context context, String label, String value) {
        LinearLayout box = new LinearLayout(context);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        box.setPadding(dp(14), dp(10), dp(14), dp(10));
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(14));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundGray));
        box.setBackground(bg);

        TextView labelView = new TextView(context);
        labelView.setText(label);
        labelView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        labelView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        labelView.setGravity(Gravity.CENTER);
        labelView.setMaxLines(1);
        labelView.setEllipsize(TextUtils.TruncateAt.END);
        box.addView(labelView);

        TextView valView = new TextView(context);
        valView.setText(value);
        valView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        valView.setTypeface(AndroidUtilities.bold());
        valView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        valView.setGravity(Gravity.CENTER);
        valView.setMaxLines(1);
        valView.setEllipsize(TextUtils.TruncateAt.END);
        valView.setPadding(0, dp(2), 0, 0);
        box.addView(valView);

        return box;
    }

    private LinearLayout.LayoutParams chipParams(boolean first) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (first) lp.setMarginEnd(dp(5));
        else lp.setMarginStart(dp(5));
        return lp;
    }

    private View buildChannelsCard(Context context) {
        LinearLayout card = card(context);
        card.addView(header(context, "Our channels"));
        card.addView(row(context, R.drawable.ic_social_telegram, 0xFF1BA4ED, "Official Channel", "@" + TG_CHANNEL, v -> openUrl("https://t.me/" + TG_CHANNEL)));
        card.addView(divider(context));
        card.addView(row(context, R.drawable.ic_social_youtube, 0xFFE53935, "YouTube", "@" + YT_HANDLE, v -> openUrl("https://youtube.com/@" + YT_HANDLE)));
        card.addView(divider(context));
        card.addView(row(context, R.drawable.ic_social_x, 0xFF334155, "Twitter / X", "@" + X_HANDLE, v -> openUrl("https://x.com/" + X_HANDLE)));
        card.addView(divider(context));
        card.addView(row(context, R.drawable.ic_social_facebook, 0xFF1877F2, "Facebook", "@" + FB_HANDLE, v -> openUrl("https://facebook.com/" + FB_HANDLE)));
        return card;
    }

    private View buildSupportCard(Context context) {
        LinearLayout card = card(context);
        card.addView(header(context, "Support & Leadership"));
        card.addView(row(context, R.drawable.msg_admins, 0xFF7C3AED, "Founder & Super Admin", "@" + SUPER_ADMIN_HANDLE, v -> openSuperAdmin()));
        card.addView(divider(context));
        card.addView(row(context, R.drawable.msg_admins, 0xFF0D9488, "Co-Founder & Admin", "@" + ADMIN_2_HANDLE + " · Huzayfa Sheikh", v -> openAdmin2()));
        card.addView(divider(context));
        card.addView(row(context, R.drawable.msg_msgbubble, 0xFF2CBE8C, "Contact support", SUPPORT_EMAIL, v -> openEmail()));
        if (BuildVars.PLAYSTORE_APP_URL != null && !BuildVars.PLAYSTORE_APP_URL.isEmpty()) {
            card.addView(divider(context));
            card.addView(row(context, R.drawable.msg_premium_liststar, 0xFFF59E0B, "Rate the app", "Leave a review on Google Play", v -> openPlayStoreRating()));
        }
        return card;
    }

    private View buildPlatformCard(Context context) {
        LinearLayout card = card(context);
        card.addView(header(context, "Privacy & Platform"));
        card.addView(row(context, R.drawable.settings_policy, 0xFF0284C7, "Privacy Policy", "How NayaGram protects your data", v -> openUrl(PRIVACY_URL)));
        card.addView(divider(context));
        card.addView(row(context, R.drawable.outline_shield_check, 0xFF10B981, "Independent Telegram client", "Built with care in Bangladesh · Compliant with Play & API terms", v -> openUrl(PRIVACY_URL)));
        card.addView(divider(context));
        card.addView(row(context, R.drawable.msg_secret, 0xFF6366F1, "Delete my account", "Settings → Privacy → Delete my account", v -> {
            if (getParentActivity() != null) {
                BulletinFactory.of(this).createSimpleBulletin(R.raw.info, "Open Settings → Privacy and Security → Delete my account").show();
            }
        }));
        return card;
    }

    private LinearLayout card(Context context) {
        LinearLayout c = new LinearLayout(context);
        c.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setCornerRadius(dp(16));
        bg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        c.setBackground(bg);
        c.setClipToOutline(true);
        return c;
    }

    private LinearLayout.LayoutParams spacedCard() {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(12);
        return lp;
    }

    private TextView header(Context context, String title) {
        TextView tv = new TextView(context);
        tv.setText(title);
        tv.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueHeader));
        tv.setTypeface(AndroidUtilities.bold());
        tv.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 15);
        tv.setPadding(dp(16), dp(14), dp(16), dp(8));
        tv.setGravity(LocaleController.isRTL ? Gravity.RIGHT : Gravity.LEFT);
        return tv;
    }

    private View row(Context context, int iconRes, int iconColor, String title, String subtitle, View.OnClickListener onClick) {
        boolean rtl = LocaleController.isRTL;
        LinearLayout rowView = new LinearLayout(context);
        rowView.setOrientation(LinearLayout.HORIZONTAL);
        rowView.setGravity(Gravity.CENTER_VERTICAL);
        if (rtl) rowView.setPadding(dp(14), dp(12), dp(16), dp(12));
        else rowView.setPadding(dp(16), dp(12), dp(14), dp(12));

        TypedValue tv = new TypedValue();
        if (context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, tv, true)) {
            rowView.setBackgroundResource(tv.resourceId);
        }
        rowView.setClickable(true);
        rowView.setOnClickListener(onClick);

        ImageView icon = new ImageView(context);
        try {
            icon.setImageResource(iconRes);
        } catch (Exception ignored) {}
        icon.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        icon.setColorFilter(new PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN));
        GradientDrawable iconBg = new GradientDrawable();
        iconBg.setShape(GradientDrawable.RECTANGLE);
        iconBg.setCornerRadius(dp(9));
        iconBg.setColor(iconColor);
        icon.setBackground(iconBg);
        icon.setPadding(dp(6), dp(6), dp(6), dp(6));

        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(32), dp(32));
        if (rtl) iconParams.leftMargin = dp(14);
        else iconParams.rightMargin = dp(14);

        LinearLayout texts = new LinearLayout(context);
        texts.setOrientation(LinearLayout.VERTICAL);

        TextView titleView = new TextView(context);
        titleView.setText(title);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        titleView.setMaxLines(1);
        titleView.setEllipsize(TextUtils.TruncateAt.END);
        titleView.setGravity(rtl ? Gravity.RIGHT : Gravity.LEFT);
        texts.addView(titleView);

        if (!TextUtils.isEmpty(subtitle)) {
            TextView subView = new TextView(context);
            subView.setText(subtitle);
            subView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
            subView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 13);
            subView.setMaxLines(2);
            subView.setEllipsize(TextUtils.TruncateAt.END);
            subView.setPadding(0, dp(1), 0, 0);
            subView.setGravity(rtl ? Gravity.RIGHT : Gravity.LEFT);
            texts.addView(subView);
        }

        LinearLayout.LayoutParams textsParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        if (rtl) textsParams.leftMargin = dp(8);
        else textsParams.rightMargin = dp(8);

        ImageView chevron = new ImageView(context);
        try {
            chevron.setImageResource(R.drawable.msg_arrowright);
        } catch (Exception ignored) {}
        chevron.setColorFilter(new PorterDuffColorFilter(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText), PorterDuff.Mode.SRC_IN));
        chevron.setAlpha(0.6f);
        if (rtl) chevron.setScaleX(-1f);
        LinearLayout.LayoutParams chevronParams = new LinearLayout.LayoutParams(dp(18), dp(18));

        if (rtl) {
            rowView.addView(chevron, chevronParams);
            rowView.addView(texts, textsParams);
            rowView.addView(icon, iconParams);
        } else {
            rowView.addView(icon, iconParams);
            rowView.addView(texts, textsParams);
            rowView.addView(chevron, chevronParams);
        }
        return rowView;
    }

    private View divider(Context context) {
        LinearLayout holder = new LinearLayout(context);
        View v = new View(context);
        v.setBackgroundColor(Theme.getColor(Theme.key_divider));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1)));
        if (LocaleController.isRTL) lp.rightMargin = dp(62);
        else lp.leftMargin = dp(62);
        holder.addView(v, lp);
        return holder;
    }

    private int dp(int value) {
        return AndroidUtilities.dp(value);
    }

    private String getInternalBuildCode() {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager()
                    .getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return String.valueOf(pInfo.versionCode / 10);
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "4087";
    }

    private String getAppVersion() {
        try {
            PackageInfo pInfo = ApplicationLoader.applicationContext.getPackageManager()
                    .getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            if (pInfo.versionName != null && !pInfo.versionName.isEmpty()) return pInfo.versionName;
        } catch (Exception e) {
            FileLog.e(e);
        }
        return "4.26.69 NG";
    }

    private void openSuperAdmin() {
        try {
            org.telegram.messenger.MessagesController.getInstance(currentAccount).openByUserName(SUPER_ADMIN_HANDLE, this, 1);
        } catch (Exception e) {
            openUrl("https://t.me/" + SUPER_ADMIN_HANDLE);
        }
    }

    private void openAdmin2() {
        try {
            org.telegram.messenger.MessagesController.getInstance(currentAccount).openByUserName(ADMIN_2_HANDLE, this, 1);
        } catch (Exception e) {
            openUrl("https://t.me/" + ADMIN_2_HANDLE);
        }
    }

    private void openPlayStoreRating() {
        if (getParentActivity() == null) return;
        Context context = getParentActivity();
        String packageName = context.getPackageName();
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=" + packageName));
            intent.setPackage("com.android.vending");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(intent);
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e3) {
                Browser.openUrl(context, "https://play.google.com/store/apps/details?id=" + packageName, true);
            }
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
