package org.telegram.messenger;

import android.content.SharedPreferences;

public class NayoGramFeatures {
    private static final String PREF_NAME = "NayoGramCustomFeatures";
    
    public static boolean isBlockApkZipEnabled() {
        SharedPreferences preferences = MessagesController.getMainSettings(UserConfig.selectedAccount);
        return preferences.getBoolean("block_apk_zip", false);
    }

    public static void setBlockApkZipEnabled(boolean enabled) {
        SharedPreferences.Editor editor = MessagesController.getMainSettings(UserConfig.selectedAccount).edit();
        editor.putBoolean("block_apk_zip", enabled).apply();
    }

    public static boolean isGhostModeEnabled() {
        SharedPreferences preferences = MessagesController.getMainSettings(UserConfig.selectedAccount);
        return preferences.getBoolean("ghost_mode", false);
    }

    public static void setGhostModeEnabled(boolean enabled) {
        SharedPreferences.Editor editor = MessagesController.getMainSettings(UserConfig.selectedAccount).edit();
        editor.putBoolean("ghost_mode", enabled).apply();
    }

    public static boolean isAntiDeleteEnabled() {
        SharedPreferences preferences = MessagesController.getMainSettings(UserConfig.selectedAccount);
        return preferences.getBoolean("anti_delete", true);
    }

    public static void setAntiDeleteEnabled(boolean enabled) {
        SharedPreferences.Editor editor = MessagesController.getMainSettings(UserConfig.selectedAccount).edit();
        editor.putBoolean("anti_delete", enabled).apply();
    }

    public static boolean isAppLockEnabled() {
        SharedPreferences preferences = MessagesController.getMainSettings(UserConfig.selectedAccount);
        return preferences.getBoolean("app_lock", false);
    }

    public static void setAppLockEnabled(boolean enabled) {
        SharedPreferences.Editor editor = MessagesController.getMainSettings(UserConfig.selectedAccount).edit();
        editor.putBoolean("app_lock", enabled).apply();
    }

    // Check if a file should be blocked based on extension (.apk, .zip, .jar, .exe)
    public static boolean shouldBlockFile(String fileName) {
        if (!isBlockApkZipEnabled() || fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        return lower.endsWith(".apk") || lower.endsWith(".zip") || lower.endsWith(".jar") || lower.endsWith(".exe") || lower.endsWith(".rar") || lower.endsWith(".iso");
    }
}
