package org.telegram.messenger;

/**
 * NayaGram Official Ads Configuration & Controller
 * ------------------------------------------------
 * Completely clean English configuration to prevent any charset encoding issues.
 * Ads are disabled by default for the first 3-5 months (ENABLE_ADS = false).
 * When enabled in the future, peaceful 12-minute cooldown ensures seamless UX.
 */
public class NayaGramAdsConfig {

    // =========================================================================
    // 1. MASTER SWITCH
    // -------------------------------------------------------------------------
    // Set to 'false' to keep app 100% ad-free during the launch period.
    // Set to 'true' in the future whenever you want to activate ads:
    // =========================================================================
    public static boolean ENABLE_ADS = false;

    // =========================================================================
    // 2. GOOGLE ADMOB IDENTIFIERS
    // -------------------------------------------------------------------------
    // Configured with Google official test unit IDs to avoid account flags.
    // Replace with your production AdMob IDs when ready to launch ads:
    // =========================================================================
    public static String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    public static String BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111";
    public static String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712";
    public static String NATIVE_AD_UNIT_ID = "ca-app-pub-3940256099942544/2247696110";

    // =========================================================================
    // 3. USER-FRIENDLY TIMING & FREQUENCY CONTROL
    // =========================================================================

    // Full screen interstitial ads appear at most once every 12 minutes (720 seconds)
    public static final long INTERSTITIAL_COOLDOWN_MS = 12 * 60 * 1000L;

    // User must perform at least 10 actions before eligible for an interstitial ad
    public static final int ACTIONS_BEFORE_INTERSTITIAL = 10;

    // No banner ads inside active chat screens to preserve speed and cleanliness
    public static final boolean SHOW_BANNER_INSIDE_CHATS = false;

    // Banners enabled only on top of dialogs list or inside navigation drawer
    public static final boolean SHOW_BANNER_IN_DIALOGS = true;

    // -------------------------------------------------------------------------
    // State Tracking Variables
    // -------------------------------------------------------------------------
    private static long lastInterstitialShownTime = 0;
    private static int actionCounter = 0;

    /**
     * Checks if it is appropriate to display an interstitial ad.
     */
    public static boolean shouldShowInterstitial() {
        if (!ENABLE_ADS) {
            return false;
        }

        actionCounter++;
        if (actionCounter < ACTIONS_BEFORE_INTERSTITIAL) {
            return false;
        }

        long now = System.currentTimeMillis();
        if (now - lastInterstitialShownTime >= INTERSTITIAL_COOLDOWN_MS) {
            lastInterstitialShownTime = now;
            actionCounter = 0;
            return true;
        }

        return false;
    }

    /**
     * Checks if banner ads are allowed in dialogs/home view.
     */
    public static boolean shouldShowBanner() {
        return ENABLE_ADS && SHOW_BANNER_IN_DIALOGS;
    }
}
