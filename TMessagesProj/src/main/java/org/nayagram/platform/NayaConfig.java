package org.nayagram.platform;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import org.telegram.messenger.ApplicationLoader;

/**
 * NayaConfig - Global configuration & preferences for NayaGram client features.
 * Houses state for all exclusive NayaGram Messenger features.
 *
 * Policy notes (keep in sync with Tools/test_voip_fgs_android14.py):
 * - Call & Voice Protection (feature_10 / confirm-actions) is permanently removed.
 *   It caused VoIP signaling timeouts and "NayaGram keeps stopping".
 * - Instant Translator (feature_17) stays enabled by default.
 * - Client premium gates are unlocked in UserConfig; free pin limit is 10.
 */
public class NayaConfig {
    private static final String PREF_NAME = "nayagram_config_prefs";

    /** Experimental rows (Ghost Mode, APK Block) stay in code but are hidden. */
    public static final boolean SHOW_EXPERIMENTAL = false;

    // The Exclusive Features Keys
    private static final String KEY_MESSAGE_SCHEDULER = "feature_02_msg_scheduler";
    private static final String KEY_SMART_AUTO_REPLY = "feature_03_auto_reply";
    private static final String KEY_AUTO_REPLY_TEXT = "feature_03_auto_reply_text";
    private static final String KEY_STORY_SAVER = "feature_04_story_saver";
    private static final String KEY_ANONYMOUS_STORIES = "feature_05_anonymous_stories";
    private static final String KEY_FORWARD_NO_QUOTE = "feature_07_forward_without_quote";
    private static final String KEY_VOICE_TRANSCRIPTION = "feature_08_voice_transcribe";
    private static final String KEY_SMART_CHAT_FOLDERS = "feature_09_smart_chat_folders";
    /** Legacy only: Call & Voice Protection / confirm-actions. Never reintroduce as active API. */
    private static final String LEGACY_KEY_CONFIRM_ACTIONS = "feature_10_confirm_actions";
    private static final String KEY_SHOW_ID_DC = "feature_11_show_id_and_dc";
    private static final String KEY_MODULAR_CONFIG = "feature_12_modular_config";
    private static final String KEY_FOCUS_MODE = "feature_13_focus_mode";
    private static final String KEY_STORAGE_DOCTOR = "feature_14_storage_doctor";
    private static final String KEY_BATTERY_SAVER = "feature_15_battery_saver";
    private static final String KEY_BIOMETRIC_LOCKER = "feature_16_biometric_locker";
    private static final String KEY_INSTANT_TRANSLATOR = "feature_17_instant_translator";
    private static final String KEY_INTL_CLOCK = "feature_18_intl_clock_24";
    private static final String KEY_LINK_GUARD = "feature_19_link_guard";
    private static final String KEY_READABLE_TEXT = "feature_20_readable_text";
    private static final String KEY_READABLE_TEXT_PREV = "feature_20_readable_text_prev";

    private static NayaConfig instance;
    private final SharedPreferences preferences;

    private NayaConfig(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        migrateLegacyPreferences();
    }

    /**
     * One-time cleanup: drop unused confirm-actions key after Call & Voice Protection removal.
     * Safe to call repeatedly; no public isConfirmActions() remains.
     */
    private void migrateLegacyPreferences() {
        if (preferences == null) {
            return;
        }
        SharedPreferences.Editor editor = preferences.edit();
        boolean changed = false;
        if (preferences.contains(LEGACY_KEY_CONFIRM_ACTIONS)) {
            editor.remove(LEGACY_KEY_CONFIRM_ACTIONS);
            changed = true;
        }
        // Older aliases that may have been written by experimental builds
        for (String obsolete : new String[]{
                "enable_call_protection",
                "enable_voice_protection",
                "call_voice_protection",
                "confirm_call_actions",
                "feature_10_call_voice_protection"
        }) {
            if (preferences.contains(obsolete)) {
                editor.remove(obsolete);
                changed = true;
            }
        }
        if (changed) {
            editor.apply();
        }
    }

    public static synchronized NayaConfig getInstance() {
        if (instance == null) {
            instance = new NayaConfig(ApplicationLoader.applicationContext);
        }
        return instance;
    }

    /**
     * Floating NG bubble is off unless SYSTEM_ALERT_WINDOW / draw-overlays is granted.
     * Never attach a TYPE_APPLICATION_OVERLAY window without that permission.
     */
    public static boolean allowFloatingOverlay(Context context) {
        if (context == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT < 23) {
            return false;
        }
        try {
            return Settings.canDrawOverlays(context);
        } catch (Throwable ignored) {
            return false;
        }
    }

    // 1. Ghost Mode

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

    // 7. Forward Without Quote
    public boolean isForwardWithoutQuote() { return preferences.getBoolean(KEY_FORWARD_NO_QUOTE, false); }
    public void setForwardWithoutQuote(boolean e) { preferences.edit().putBoolean(KEY_FORWARD_NO_QUOTE, e).apply(); }

    // 8. Voice Transcription
    public boolean isVoiceTranscriptionEnabled() { return preferences.getBoolean(KEY_VOICE_TRANSCRIPTION, true); }
    public void setVoiceTranscriptionEnabled(boolean e) { preferences.edit().putBoolean(KEY_VOICE_TRANSCRIPTION, e).apply(); }

    // 9. Smart Chat Folders (Disabled by default for Play Store compliance)
    public boolean isSmartFoldersEnabled() { return preferences.getBoolean(KEY_SMART_CHAT_FOLDERS, false); }
    public void setSmartFoldersEnabled(boolean e) { preferences.edit().putBoolean(KEY_SMART_CHAT_FOLDERS, e).apply(); }

    // 10. Call & Voice Protection — permanently removed (do not re-add isConfirmActions)

    // 11. User ID & DC Display
    public boolean isShowIdAndDc() { return preferences.getBoolean(KEY_SHOW_ID_DC, true); }
    public void setShowIdAndDc(boolean e) { preferences.edit().putBoolean(KEY_SHOW_ID_DC, e).apply(); }

    // 12. Modular NayaConfig
    public boolean isModularConfigActive() { return preferences.getBoolean(KEY_MODULAR_CONFIG, true); }
    public boolean isModularConfig() { return isModularConfigActive(); }
    public void setModularConfig(boolean e) { setModularConfigActive(e); }
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

    // 17. In-Chat Instant Translator (language meaning / translation — kept)
    public boolean isInstantTranslatorEnabled() { return preferences.getBoolean(KEY_INSTANT_TRANSLATOR, true); }
    public void setInstantTranslatorEnabled(boolean e) { preferences.edit().putBoolean(KEY_INSTANT_TRANSLATOR, e).apply(); }

    /** Forces 24-hour times for people who travel or work across countries. Local display only. */
    public boolean isInternationalClock() { return preferences.getBoolean(KEY_INTL_CLOCK, false); }
    public void setInternationalClock(boolean enabled) {
        preferences.edit().putBoolean(KEY_INTL_CLOCK, enabled).apply();
        applyInternationalClock(enabled);
    }

    public static void applyInternationalClock(boolean enabled) {
        try {
            if (enabled) {
                org.telegram.messenger.LocaleController.is24HourFormat = true;
            } else if (ApplicationLoader.applicationContext != null) {
                org.telegram.messenger.LocaleController.is24HourFormat =
                        android.text.format.DateFormat.is24HourFormat(ApplicationLoader.applicationContext);
            }
            org.telegram.messenger.LocaleController.getInstance().recreateFormatters();
        } catch (Throwable ignored) {
        }
    }

    /** Asks before opening http(s) links that are not Telegram. Does not block Telegram links. */
    public boolean isLinkGuardEnabled() { return preferences.getBoolean(KEY_LINK_GUARD, true); }
    public void setLinkGuardEnabled(boolean e) { preferences.edit().putBoolean(KEY_LINK_GUARD, e).apply(); }

    /** Slightly larger chat text. Uses Telegram's own font-size setting. */
    public boolean isReadableTextEnabled() { return preferences.getBoolean(KEY_READABLE_TEXT, false); }
    public void setReadableTextEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_READABLE_TEXT, enabled).apply();
        applyReadableText(enabled);
    }

    public void applyReadableText(boolean enabled) {
        try {
            int current = org.telegram.messenger.SharedConfig.fontSize;
            int target;
            if (enabled) {
                if (!preferences.contains(KEY_READABLE_TEXT_PREV)) {
                    preferences.edit().putInt(KEY_READABLE_TEXT_PREV, current).apply();
                }
                target = Math.max(current, 18);
            } else {
                target = preferences.getInt(KEY_READABLE_TEXT_PREV, current);
            }
            if (target < 12) target = 12;
            if (target > 30) target = 30;
            if (target == current) return;
            org.telegram.messenger.SharedConfig.fontSize = target;
            org.telegram.messenger.SharedConfig.fontSizeIsDefault = false;
            android.content.SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("mainconfig", Context.MODE_PRIVATE);
            prefs.edit().putInt("fons_size", target).apply();
            org.telegram.ui.ActionBar.Theme.createCommonMessageResources();
        } catch (Throwable ignored) {
        }
    }

    // Reset all features to default values
    public void resetToDefaults() {
        preferences.edit()
            .putBoolean(KEY_MESSAGE_SCHEDULER, true)
            .putBoolean(KEY_SMART_AUTO_REPLY, false)
            .putBoolean(KEY_STORY_SAVER, true)
            .putBoolean(KEY_ANONYMOUS_STORIES, false)
            .putBoolean(KEY_FORWARD_NO_QUOTE, false)
            .putBoolean(KEY_VOICE_TRANSCRIPTION, true)
            .putBoolean(KEY_SMART_CHAT_FOLDERS, false)
            .remove(LEGACY_KEY_CONFIRM_ACTIONS)
            .remove("enable_call_protection")
            .remove("enable_voice_protection")
            .remove("call_voice_protection")
            .remove("confirm_call_actions")
            .remove("feature_10_call_voice_protection")
            .putBoolean(KEY_SHOW_ID_DC, true)
            .putBoolean(KEY_MODULAR_CONFIG, true)
            .putBoolean(KEY_FOCUS_MODE, false)
            .putBoolean(KEY_STORAGE_DOCTOR, true)
            .putBoolean(KEY_BATTERY_SAVER, true)
            .putBoolean(KEY_BIOMETRIC_LOCKER, false)
            .putBoolean(KEY_INSTANT_TRANSLATOR, true)
            .putBoolean(KEY_INTL_CLOCK, false)
            .putBoolean(KEY_LINK_GUARD, true)
            .putBoolean(KEY_READABLE_TEXT, false)
            .remove(KEY_READABLE_TEXT_PREV)
            .apply();
    }
}
