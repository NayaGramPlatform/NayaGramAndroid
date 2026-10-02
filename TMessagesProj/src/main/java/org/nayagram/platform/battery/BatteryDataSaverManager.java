package org.nayagram.platform.battery;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.os.BatteryManager;
import android.os.Build;

/**
 * BatteryDataSaverManager - Smart Network & Ultra Battery Optimizer for NayaGram.
 * 100% Google Play Store Compliant: Dynamically optimizes background media preloading,
 * video autoplay, and animation frames when battery is low (< 20%) or on metered cellular data.
 */
public final class BatteryDataSaverManager {

    private static final String PREFS_NAME = "nayagram_battery_saver_prefs";
    private static final String KEY_AUTO_BATTERY_SAVER = "auto_battery_saver";
    private static final String KEY_AUTO_DATA_SAVER = "auto_data_saver";
    private static final String KEY_MANUAL_ECO_MODE = "manual_eco_mode";

    private static volatile BatteryDataSaverManager sInstance;
    private final SharedPreferences preferences;
    private final Context context;

    private BatteryDataSaverManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static BatteryDataSaverManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (BatteryDataSaverManager.class) {
                if (sInstance == null) {
                    sInstance = new BatteryDataSaverManager(context);
                }
            }
        }
        return sInstance;
    }

    public boolean isEcoModeActive() {
        if (preferences.getBoolean(KEY_MANUAL_ECO_MODE, false)) {
            return true;
        }
        if (preferences.getBoolean(KEY_AUTO_BATTERY_SAVER, true)) {
            return isLowBattery();
        }
        return false;
    }

    public boolean isDataSaverActive() {
        if (preferences.getBoolean(KEY_AUTO_DATA_SAVER, true)) {
            return isMeteredCellular();
        }
        return false;
    }

    public boolean shouldSuppressVideoAutoplay() {
        return isEcoModeActive() || isDataSaverActive();
    }

    public boolean shouldSuppressAnimatedStickers() {
        return isEcoModeActive();
    }

    public int getBatteryPercentage() {
        try {
            Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (batteryIntent == null) return 100;
            int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level == -1 || scale == -1) return 100;
            return (int) (((float) level / (float) scale) * 100.0f);
        } catch (Exception e) {
            return 100;
        }
    }

    public boolean isLowBattery() {
        return getBatteryPercentage() <= 20;
    }

    public boolean isMeteredCellular() {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (cm == null) return false;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                android.net.Network activeNetwork = cm.getActiveNetwork();
                if (activeNetwork == null) return false;
                NetworkCapabilities capabilities = cm.getNetworkCapabilities(activeNetwork);
                if (capabilities == null) return false;
                return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
                        || capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR);
            } else {
                android.net.NetworkInfo activeInfo = cm.getActiveNetworkInfo();
                return activeInfo != null && activeInfo.getType() == ConnectivityManager.TYPE_MOBILE;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void setAutoBatterySaver(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_BATTERY_SAVER, enabled).apply();
    }

    public void setAutoDataSaver(boolean enabled) {
        preferences.edit().putBoolean(KEY_AUTO_DATA_SAVER, enabled).apply();
    }

    public void setManualEcoMode(boolean enabled) {
        preferences.edit().putBoolean(KEY_MANUAL_ECO_MODE, enabled).apply();
    }
}
