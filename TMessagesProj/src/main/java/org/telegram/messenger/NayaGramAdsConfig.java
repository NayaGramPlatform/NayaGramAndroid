package org.telegram.messenger;

/**
 * Central NayaGram advertising policy.
 *
 * Ads intentionally remain disabled for launch. Before enabling, replace the
 * Google test IDs with the IDs issued for the production NayaGram AdMob app.
 */
public final class NayaGramAdsConfig {

    private NayaGramAdsConfig() { }

    /** Master switch: keep false until production AdMob setup and consent are verified. */
    public static volatile boolean ENABLE_ADS = false;

    /** Official Google test IDs. Never use these as production monetization IDs. */
    public static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static final String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741";
    public static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    public static final String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";

    /** Conservative frequency cap: no more than one interstitial per 20 minutes. */
    public static final long INTERSTITIAL_COOLDOWN_MS = 20 * 60 * 1000L;

    /** Require meaningful app usage before an interstitial becomes eligible. */
    public static final int ACTIONS_BEFORE_INTERSTITIAL = 15;

    /** Never put banners or full-screen ads inside active chats or calls. */
    public static final boolean SHOW_BANNER_INSIDE_CHATS = false;
    public static final boolean SHOW_BANNER_IN_DIALOGS = true;

    public static boolean shouldShowBanner() {
        return ENABLE_ADS && SHOW_BANNER_IN_DIALOGS;
    }
}
