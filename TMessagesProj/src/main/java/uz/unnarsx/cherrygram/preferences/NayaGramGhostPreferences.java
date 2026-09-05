package uz.unnarsx.cherrygram.preferences;

import org.telegram.messenger.ApplicationLoader;
import android.content.SharedPreferences;

public class NayaGramGhostPreferences {
    private static final SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("nayagram_ghost", 0);

    public static boolean isHideOnline() { return prefs.getBoolean("hideOnline", false); }
    public static void setHideOnline(boolean v) { prefs.edit().putBoolean("hideOnline", v).apply(); }

    public static boolean isHideTyping() { return prefs.getBoolean("hideTyping", false); }
    public static void setHideTyping(boolean v) { prefs.edit().putBoolean("hideTyping", v).apply(); }

    public static boolean isHideRead() { return prefs.getBoolean("hideRead", false); }
    public static void setHideRead(boolean v) { prefs.edit().putBoolean("hideRead", v).apply(); }
  }
