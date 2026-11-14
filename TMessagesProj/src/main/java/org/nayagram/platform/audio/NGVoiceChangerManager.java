package org.nayagram.platform.audio;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * NGVoiceChangerManager - Manages real-time voice changer effects (Normal, Chipmunk, Deep Voice,
 * Robot, Helium, Cave, Telephone) for voice notes and audio recordings in NayaGram.
 */
public final class NGVoiceChangerManager {

    private static final String PREF_NAME = "nayagram_voice_changer_prefs";
    private static final String KEY_EFFECT_MODE = "voice_effect_mode";

    public static final int MODE_NORMAL = 0;
    public static final int MODE_CHIPMUNK = 1;
    public static final int MODE_DEEP = 2;
    public static final int MODE_ROBOT = 3;
    public static final int MODE_HELIUM = 4;
    public static final int MODE_CAVE = 5;
    public static final int MODE_TELEPHONE = 6;

    private NGVoiceChangerManager() {}

    private static SharedPreferences getPrefs(Context context) {
        if (context == null) {
            context = org.telegram.messenger.ApplicationLoader.applicationContext;
        }
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static int getSelectedMode(Context context) {
        try {
            return getPrefs(context).getInt(KEY_EFFECT_MODE, MODE_NORMAL);
        } catch (Throwable ignored) {
            return MODE_NORMAL;
        }
    }

    public static void setSelectedMode(Context context, int mode) {
        try {
            getPrefs(context).edit().putInt(KEY_EFFECT_MODE, mode).apply();
        } catch (Throwable ignored) {}
    }

    public static String getModeTitle(int mode) {
        switch (mode) {
            case MODE_CHIPMUNK: return "🐿️ Chipmunk (High Pitch)";
            case MODE_DEEP: return "🎙️ Deep Voice (Bass Boost)";
            case MODE_ROBOT: return "🤖 Robot (Vocoder)";
            case MODE_HELIUM: return "🎈 Helium";
            case MODE_CAVE: return "🏔️ Cave (Echo)";
            case MODE_TELEPHONE: return "☎️ Telephone (Bandpass)";
            default: return "🗣️ Normal Voice";
        }
    }
}
