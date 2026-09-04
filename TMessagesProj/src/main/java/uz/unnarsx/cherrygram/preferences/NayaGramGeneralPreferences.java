package uz.unnarsx.cherrygram.preferences;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;

/**
 * NayaGram General Preferences - B Feature
 * Private Repo - TeleGram + NayaGram Dui Bondhu 😁
 */
public class NayaGramGeneralPreferences {
    
    private static final String PREFS_NAME = "nayagram_general";
    private static final String KEY_NO_ADS = "no_sponsored_ads";
    private static final String KEY_PREMIUM_FREE = "premium_free";

    // Check if No Ads is ON
    public static boolean isNoAdsEnabled() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_NO_ADS, true);
    }

    // Set No Ads ON/OFF
    public static void setNoAdsEnabled(boolean enabled) {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_NO_ADS, enabled).apply();
    }

    // Check Premium Free
    public static boolean isPremiumFreeEnabled() {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_PREMIUM_FREE, true);
    }

    public static void setPremiumFreeEnabled(boolean enabled) {
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_PREMIUM_FREE, enabled).apply();
    }
    
    // B Feature - Sponsored Message Block Logic
    public static boolean shouldBlockSponsored() {
        return isNoAdsEnabled();
    }
}
