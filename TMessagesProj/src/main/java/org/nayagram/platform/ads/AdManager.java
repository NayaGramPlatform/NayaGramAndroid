package org.nayagram.platform.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import org.nayagram.platform.monetization.INGMonetizationProvider;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.NayaGramAdsConfig;
import org.telegram.messenger.UserConfig;

/**
 * Central gate for all NayaGram ad placements.
 *
 * Ads are opt-in at release level, never shown over chats/calls, and
 * interstitials are capped to one per 20 minutes after meaningful usage.
 */
public final class AdManager {
    private static final String TAG = "NayaGramMonetization";
    private static final String PREFS_NAME = "nayagram_ads";
    private static final String KEY_LAST_INTERSTITIAL_WALL_TIME = "last_interstitial_wall_time";

    public static final long DISPLAY_INTERVAL_MINUTES = 20L;
    public static final long DISPLAY_INTERVAL_MS = NayaGramAdsConfig.INTERSTITIAL_COOLDOWN_MS;
    public static final int MINIMUM_ACTIONS_BEFORE_AD = NayaGramAdsConfig.ACTIONS_BEFORE_INTERSTITIAL;

    public static volatile INGMonetizationProvider activeProvider;

    private static final long APP_SESSION_STARTED_WALL_TIME = System.currentTimeMillis();
    private static long lastAdShownWallTime = -1L;
    private static int actionCounter;

    private AdManager() { }

    public static boolean isEnabled() {
        return NayaGramAdsConfig.ENABLE_ADS;
    }

    public static void setEnabled(boolean enabled) {
        NayaGramAdsConfig.ENABLE_ADS = enabled;
    }

    private static boolean isPremiumAccount() {
        int account = UserConfig.selectedAccount;
        return account >= 0 && account < UserConfig.MAX_ACCOUNT_COUNT
                && UserConfig.getInstance(account).isPremium();
    }

    private static synchronized long lastShownWallTime() {
        if (lastAdShownWallTime < 0L) {
            try {
                Context context = ApplicationLoader.applicationContext;
                lastAdShownWallTime = context == null ? System.currentTimeMillis()
                        : context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .getLong(KEY_LAST_INTERSTITIAL_WALL_TIME, 0L);
            } catch (RuntimeException error) {
                Log.w(TAG, "Could not read persisted ad cooldown; failing closed.", error);
                lastAdShownWallTime = System.currentTimeMillis();
            }
        }
        return lastAdShownWallTime;
    }

    public static synchronized void recordUserAction() {
        if (isEnabled() && !isPremiumAccount()) {
            actionCounter = Math.min(actionCounter + 1, MINIMUM_ACTIONS_BEFORE_AD);
        }
    }

    public static synchronized boolean canShowInterstitial() {
        if (!isEnabled() || isPremiumAccount()) return false;

        INGMonetizationProvider provider = activeProvider;
        if (provider == null || !provider.isMonetizationEnabled()) return false;
        if (actionCounter < MINIMUM_ACTIONS_BEFORE_AD) return false;

        long now = System.currentTimeMillis();
        long lastShown = lastShownWallTime();
        if (lastShown == 0L && now - APP_SESSION_STARTED_WALL_TIME < DISPLAY_INTERVAL_MS) {
            return false;
        }
        if (now < lastShown) return false;
        return now - lastShown >= DISPLAY_INTERVAL_MS;
    }

    public static synchronized void onAdDisplayed() {
        long now = System.currentTimeMillis();
        lastAdShownWallTime = now;
        actionCounter = 0;
        try {
            Context context = ApplicationLoader.applicationContext;
            if (context != null) {
                context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                        .putLong(KEY_LAST_INTERSTITIAL_WALL_TIME, now).apply();
            }
        } catch (RuntimeException error) {
            Log.w(TAG, "Could not persist ad cooldown.", error);
        }
        Log.i(TAG, "Interstitial impression recorded; 20-minute cooldown started.");
    }

    public static boolean showInterstitialIfReady(Activity activity) {
        if (activity == null || activity.isFinishing()
                || (android.os.Build.VERSION.SDK_INT >= 17 && activity.isDestroyed())) {
            return false;
        }
        if (!canShowInterstitial()) return false;

        INGMonetizationProvider provider = activeProvider;
        if (provider == null || !provider.isMonetizationEnabled()) return false;
        try {
            return provider.showInterstitialIfAvailable(activity, AdManager::onAdDisplayed);
        } catch (RuntimeException error) {
            Log.e(TAG, "Interstitial display failed; continuing without an ad.", error);
            return false;
        }
    }

    public static void registerProvider(INGMonetizationProvider provider) {
        activeProvider = provider;
        Log.i(TAG, "Monetization provider registered: "
                + (provider == null ? "none" : provider.getClass().getSimpleName()));
    }

    public static synchronized void resetTimer() {
        lastAdShownWallTime = System.currentTimeMillis();
        actionCounter = 0;
    }
}
