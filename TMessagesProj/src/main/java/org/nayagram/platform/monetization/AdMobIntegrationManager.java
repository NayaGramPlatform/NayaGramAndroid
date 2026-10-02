package org.nayagram.platform.monetization;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.ui.ActionBar.Theme;

/**
 * AdMobIntegrationManager - Future Monetization & Clean Native Ads Architecture.
 * 
 * Strategy & Google Play Policy Compliance:
 * 1. DISABLED BY DEFAULT: No ads shown during initial 3-5 months to build trust and organic user growth.
 * 2. ZERO USER ANNOYANCE: Never displays interstitial popups or blocking overlays.
 * 3. NO ADS IN 1-ON-1 PRIVATE CHATS: Ads only appear in public channels or discovery screens.
 * 4. CLEAR SPONSORED DISCLOSURE: Complies with Google Play Developer Policy for transparency.
 * 5. REMOTE ACTIVATION READY: Can be toggled on smoothly without recompiling core chat logic.
 */
public final class AdMobIntegrationManager {

    private static final String PREFS_NAME = "nayagram_monetization_prefs";
    private static final String KEY_ADS_ENABLED = "admob_ads_enabled";
    private static final String KEY_ACTIVATION_TIMESTAMP = "admob_activation_timestamp";
    private static final String KEY_BANNER_AD_UNIT_ID = "admob_banner_unit_id";
    private static final String KEY_NATIVE_AD_UNIT_ID = "admob_native_unit_id";

    // Standard AdMob Test Ad Unit IDs (Google Official Test IDs)
    public static final String DEFAULT_TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111";
    public static final String DEFAULT_TEST_NATIVE_ID = "ca-app-pub-3940256099942544/2247696110";

    private static volatile AdMobIntegrationManager sInstance;
    private final SharedPreferences preferences;

    private AdMobIntegrationManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static AdMobIntegrationManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (AdMobIntegrationManager.class) {
                if (sInstance == null) {
                    sInstance = new AdMobIntegrationManager(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Check if ads are active. By default, returns FALSE for the initial 3-5 months.
     */
    public boolean isAdsActive() {
        return preferences.getBoolean(KEY_ADS_ENABLED, false);
    }

    /**
     * Remotely or locally enable ads when ready after 3-5 months.
     */
    public void setAdsActive(boolean active) {
        preferences.edit()
                .putBoolean(KEY_ADS_ENABLED, active)
                .putLong(KEY_ACTIVATION_TIMESTAMP, System.currentTimeMillis())
                .apply();
    }

    public void configureAdUnitIds(String bannerUnitId, String nativeUnitId) {
        preferences.edit()
                .putString(KEY_BANNER_AD_UNIT_ID, bannerUnitId)
                .putString(KEY_NATIVE_AD_UNIT_ID, nativeUnitId)
                .apply();
    }

    public String getBannerUnitId() {
        return preferences.getString(KEY_BANNER_AD_UNIT_ID, DEFAULT_TEST_BANNER_ID);
    }

    public String getNativeUnitId() {
        return preferences.getString(KEY_NATIVE_AD_UNIT_ID, DEFAULT_TEST_NATIVE_ID);
    }

    /**
     * Checks if ads are allowed in the given chat context.
     * Policy: Absolutely NEVER allow ads in 1-on-1 private conversations or secret chats!
     * Only permissible in broad public channels or discovery screens.
     */
    public boolean canShowAdInChat(boolean isPublicChannel, boolean is1on1PrivateChat) {
        if (!isAdsActive()) {
            return false;
        }
        if (is1on1PrivateChat) {
            return false; // Zero intrusion in personal chats
        }
        return isPublicChannel;
    }

    /**
     * Build an ultra-clean, elegant native sponsored container that matches Telegram design system.
     * When ads are disabled (current state), returns a GONE view with zero layout overhead.
     */
    public View createSponsoredCardView(Context context, String title, String body, String ctaText, final Runnable onCtaClick) {
        FrameLayout container = new FrameLayout(context);
        container.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        if (!isAdsActive()) {
            container.setVisibility(View.GONE);
            return container;
        }

        container.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(6), AndroidUtilities.dp(14), AndroidUtilities.dp(6));

        LinearLayout card = new LinearLayout(context);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(AndroidUtilities.dp(14), AndroidUtilities.dp(12), AndroidUtilities.dp(14), AndroidUtilities.dp(12));

        // Elegant Card Background
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        cardBg.setCornerRadius(AndroidUtilities.dp(14));
        cardBg.setStroke(AndroidUtilities.dp(1), Theme.getColor(Theme.key_divider));
        card.setBackground(cardBg);

        // Header Row: "Sponsored" Tag + Title + Close Button
        LinearLayout headerRow = new LinearLayout(context);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        TextView badge = new TextView(context);
        badge.setText("SPONSORED");
        badge.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 9);
        badge.setTextColor(Color.WHITE);
        badge.setTypeface(AndroidUtilities.bold());
        badge.setPadding(AndroidUtilities.dp(6), AndroidUtilities.dp(2), AndroidUtilities.dp(6), AndroidUtilities.dp(2));
        GradientDrawable badgeBg = new GradientDrawable();
        badgeBg.setColor(0xFF24A1DE);
        badgeBg.setCornerRadius(AndroidUtilities.dp(4));
        badge.setBackground(badgeBg);

        TextView titleView = new TextView(context);
        titleView.setText(title != null ? title : "Sponsored Recommendation");
        titleView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 14);
        titleView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        titleView.setTypeface(AndroidUtilities.bold());
        LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
        titleParams.leftMargin = AndroidUtilities.dp(8);
        titleView.setLayoutParams(titleParams);

        headerRow.addView(badge);
        headerRow.addView(titleView);

        // Body Text
        TextView bodyView = new TextView(context);
        bodyView.setText(body != null ? body : "");
        bodyView.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        bodyView.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteGrayText));
        LinearLayout.LayoutParams bodyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        bodyParams.topMargin = AndroidUtilities.dp(6);
        bodyView.setLayoutParams(bodyParams);

        // CTA Button
        TextView ctaButton = new TextView(context);
        ctaButton.setText(ctaText != null ? ctaText : "Learn More");
        ctaButton.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 12);
        ctaButton.setTextColor(Color.WHITE);
        ctaButton.setTypeface(AndroidUtilities.bold());
        ctaButton.setGravity(Gravity.CENTER);
        ctaButton.setPadding(AndroidUtilities.dp(16), AndroidUtilities.dp(8), AndroidUtilities.dp(16), AndroidUtilities.dp(8));
        GradientDrawable ctaBg = new GradientDrawable();
        ctaBg.setColor(0xFF24A1DE);
        ctaBg.setCornerRadius(AndroidUtilities.dp(8));
        ctaButton.setBackground(ctaBg);

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = AndroidUtilities.dp(10);
        btnParams.gravity = Gravity.END;
        ctaButton.setLayoutParams(btnParams);

        if (onCtaClick != null) {
            ctaButton.setOnClickListener(v -> onCtaClick.run());
        }

        card.addView(headerRow);
        card.addView(bodyView);
        card.addView(ctaButton);

        container.addView(card);
        return container;
    }
}
