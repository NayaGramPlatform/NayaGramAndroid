package uz.unnarsx.cherrygram.preferences;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;

public class NayaGramGhostPreferences {

    private static final String PREFS_NAME = "nayagram_ghost";
    private static final String KEY_HIDE_ONLINE = "hideOnline";
    private static final String KEY_HIDE_TYPING = "hideTyping";
    private static final String KEY_HIDE_READ = "hideRead";

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static boolean isHideOnline() {
        return prefs().getBoolean(KEY_HIDE_ONLINE, false);
    }

    public static void setHideOnline(boolean value) {
        prefs().edit().putBoolean(KEY_HIDE_ONLINE, value).apply();
    }

    public static boolean isHideTyping() {
        return prefs().getBoolean(KEY_HIDE_TYPING, false);
    }

    public static void setHideTyping(boolean value) {
        prefs().edit().putBoolean(KEY_HIDE_TYPING, value).apply();
    }

    public static boolean isHideRead() {
        return prefs().getBoolean(KEY_HIDE_READ, false);
    }

    public static void setHideRead(boolean value) {
        prefs().edit().putBoolean(KEY_HIDE_READ, value).apply();
    }
}
