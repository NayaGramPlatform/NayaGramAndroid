package org.nayagram.platform;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;

/**
 * NayaConfig - Global configuration & preferences for NayaGram client features.
 * Houses state for all 17 exclusive NayaGram Messenger features.
 */
public class NayaConfig {
    private static final String PREF_NAME = "nayagram_config_prefs";

    // The 17 Exclusive Features Keys
    private static final String KEY_GHOST_MODE = "feature_01_ghost_mode";
    private static final String KEY_MESSAGE_SCHEDULER = "feature_02_msg_scheduler";
    private static final String KEY_SMART_AUTO_REPLY = "feature_03_auto_reply";
    private static final String KEY_AUTO_REPLY_TEXT = "feature_03_auto_reply_text";
    private static final String KEY_STORY_SAVER = "feature_04_story_saver";
    private static final String KEY_ANONYMOUS_STORIES = "feature_05_anonymous_stories";
    private static final String KEY_ANTI_DELETE = "feature_06_anti_delete";
    private static final String KEY_FORWARD_NO_QUOTE = "feature_07_forward_without_quote";
    private static final String KEY_VOICE_TRANSCRIPTION = "feature_08_voice_transcribe";
    private static final String KEY_SMART_CHAT_FOLDERS = "feature_09_smart_chat_folders";
    private static final String KEY_CONFIRM_ACTIONS = "feature_10_confirm_actions";
    private static final String KEY_SHOW_ID_DC = "feature_11_show_id_and_dc";
    private static final String KEY_MODULAR_CONFIG = "feature_12_modular_config";
    private static final String KEY_FOCUS_MODE = "feature_13_focus_mode";
    private static final String KEY_STORAGE_DOCTOR = "feature_14_storage_doctor";
    private static final String KEY_BATTERY_SAVER = "feature_15_battery_saver";
    private static final String KEY_BIOMETRIC_LOCKER = "feature_16_biometric_locker";
    private static final String KEY_INSTANT_TRANSLATOR = "feature_17_instant_translator";

    private static NayaConfig instance;
    private final SharedPreferences preferences;

    private NayaConfig(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized NayaConfig getInstance() {
        if (instance == null) {
            instance = new NayaConfig(ApplicationLoader.applicationContext);
        }
        return instance;
    }

    // 1. Ghost Mode
    public boolean isGhostMode() { return preferences.getBoolean(KEY_GHOST_MODE, false); }
    public void setGhostMode(boolean e) { preferences.edit().putBoolean(KEY_GHOST_MODE, e).apply(); }

    // 2. Message Scheduler
    public boolean isMessageScheduler() { return preferences.getBoolean(KEY_MESSAGE_SCHEDULER, true); }
    public void setMessageScheduler(boolean e) { preferences.edit().putBoolean(KEY_MESSAGE_SCHEDULER, e).apply(); }

    // 3. Smart Auto-Reply
    public boolean isSmartAutoReply() { return preferences.getBoolean(KEY_SMART_AUTO_REPLY, false); }
    public void setSmartAutoReply(boolean e) { preferences.edit().putBoolean(KEY_SMART_AUTO_REPLY, e).apply(); }
    public String getAutoReplyText() { return preferences.getString(KEY_AUTO_REPLY_TEXT, "Hello! I am currently busy. I will get back to you soon. (Sent via NayaGram Auto-Reply)"); }
    public void setAutoReplyText(String t) { preferences.edit().putString(KEY_AUTO_REPLY_TEXT, t).apply(); }

    // 4. Story Saver
    public boolean isStorySaverEnabled() { return preferences.getBoolean(KEY_STORY_SAVER, true); }
    public void setStorySaverEnabled(boolean e) { preferences.edit().putBoolean(KEY_STORY_SAVER, e).apply(); }

    // 5. Anonymous Story Viewer
    public boolean isAnonymousStories() { return preferences.getBoolean(KEY_ANONYMOUS_STORIES, false); }
    public void setAnonymousStories(boolean e) { preferences.edit().putBoolean(KEY_ANONYMOUS_STORIES, e).apply(); }

    // 6. Anti-Delete Recovery
    public boolean isAntiDeleteEnabled() { return preferences.getBoolean(KEY_ANTI_DELETE, true); }
    public void setAntiDeleteEnabled(boolean e) { preferences.edit().putBoolean(KEY_ANTI_DELETE, e).apply(); }

    // 7. Forward Without Quote
    public boolean isForwardWithoutQuote() { return preferences.getBoolean(KEY_FORWARD_NO_QUOTE, false); }
    public void setForwardWithoutQuote(boolean e) { preferences.edit().putBoolean(KEY_FORWARD_NO_QUOTE, e).apply(); }

    // 8. Voice Transcription
    public boolean isVoiceTranscriptionEnabled() { return preferences.getBoolean(KEY_VOICE_TRANSCRIPTION, true); }
    public void setVoiceTranscriptionEnabled(boolean e) { preferences.edit().putBoolean(KEY_VOICE_TRANSCRIPTION, e).apply(); }

    // 9. Smart Chat Folders
    public boolean isSmartFoldersEnabled() { return preferences.getBoolean(KEY_SMART_CHAT_FOLDERS, true); }
    public void setSmartFoldersEnabled(boolean e) { preferences.edit().putBoolean(KEY_SMART_CHAT_FOLDERS, e).apply(); }

    // 10. Call & Voice Protection
    public boolean isConfirmActions() { return preferences.getBoolean(KEY_CONFIRM_ACTIONS, true); }
    public void setConfirmActions(boolean e) { preferences.edit().putBoolean(KEY_CONFIRM_ACTIONS, e).apply(); }

    // 11. User ID & DC Display
    public boolean isShowIdAndDc() { return preferences.getBoolean(KEY_SHOW_ID_DC, true); }
    public void setShowIdAndDc(boolean e) { preferences.edit().putBoolean(KEY_SHOW_ID_DC, e).apply(); }

    // 12. Modular NayaConfig
    public boolean isModularConfigActive() { return preferences.getBoolean(KEY_MODULAR_CONFIG, true); }
    public void setModularConfigActive(boolean e) { preferences.edit().putBoolean(KEY_MODULAR_CONFIG, e).apply(); }

    // 13. Digital Wellbeing & Focus Mode
    public boolean isFocusModeEnabled() { return preferences.getBoolean(KEY_FOCUS_MODE, false); }
    public void setFocusModeEnabled(boolean e) { preferences.edit().putBoolean(KEY_FOCUS_MODE, e).apply(); }

    // 14. Smart Storage Doctor
    public boolean isStorageDoctorEnabled() { return preferences.getBoolean(KEY_STORAGE_DOCTOR, true); }
    public void setStorageDoctorEnabled(boolean e) { preferences.edit().putBoolean(KEY_STORAGE_DOCTOR, e).apply(); }

    // 15. Ultra Battery & Low-Data Saver
    public boolean isBatterySaverEnabled() { return preferences.getBoolean(KEY_BATTERY_SAVER, true); }
    public void setBatterySaverEnabled(boolean e) { preferences.edit().putBoolean(KEY_BATTERY_SAVER, e).apply(); }

    // 16. Biometric Chat Locker
    public boolean isBiometricChatLockerEnabled() { return preferences.getBoolean(KEY_BIOMETRIC_LOCKER, false); }
    public void setBiometricChatLockerEnabled(boolean e) { preferences.edit().putBoolean(KEY_BIOMETRIC_LOCKER, e).apply(); }

    // 17. In-Chat Instant Translator
    public boolean isInstantTranslatorEnabled() { return preferences.getBoolean(KEY_INSTANT_TRANSLATOR, true); }
    public void setInstantTranslatorEnabled(boolean e) { preferences.edit().putBoolean(KEY_INSTANT_TRANSLATOR, e).apply(); }

    // Reset all features to default values
    public void resetToDefaults() {
        preferences.edit()
            .putBoolean(KEY_GHOST_MODE, false)
            .putBoolean(KEY_MESSAGE_SCHEDULER, true)
            .putBoolean(KEY_SMART_AUTO_REPLY, false)
            .putBoolean(KEY_STORY_SAVER, true)
            .putBoolean(KEY_ANONYMOUS_STORIES, false)
            .putBoolean(KEY_ANTI_DELETE, true)
            .putBoolean(KEY_FORWARD_NO_QUOTE, false)
            .putBoolean(KEY_VOICE_TRANSCRIPTION, true)
            .putBoolean(KEY_SMART_CHAT_FOLDERS, true)
            .putBoolean(KEY_CONFIRM_ACTIONS, true)
            .putBoolean(KEY_SHOW_ID_DC, true)
            .putBoolean(KEY_MODULAR_CONFIG, true)
            .putBoolean(KEY_FOCUS_MODE, false)
            .putBoolean(KEY_STORAGE_DOCTOR, true)
            .putBoolean(KEY_BATTERY_SAVER, true)
            .putBoolean(KEY_BIOMETRIC_LOCKER, false)
            .putBoolean(KEY_INSTANT_TRANSLATOR, true)
            .apply();
    }
}
