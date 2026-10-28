package org.nayagram.platform;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.provider.Settings;
import org.json.JSONObject;
import org.json.JSONException;
import java.util.Map;
import org.telegram.messenger.ApplicationLoader;

/**
 * NayaConfig - Global configuration & preferences for NayaGram client features.
 * Houses state for all exclusive NayaGram Messenger features.
 *
 * Policy notes (keep in sync with Tools/test_voip_fgs_android14.py):
 * - Call & Voice Protection (feature_10 / confirm-actions) is permanently removed.
 *   It caused VoIP signaling timeouts and "NayaGram keeps stopping".
 * - Instant Translator (feature_17) remains disabled until a translation engine is implemented.
 * - Telegram Premium follows the real account. Free pin limit stays 10.
 * - Analytics, Smart Storage Doctor, scheduler, auto-reply, story saver,
 *   anonymous stories, forward-without-quote and voice transcription are removed from the UI.
 */
public class NayaConfig {
    public boolean isVerifiedBadgeEnabled = false;

    private static final String PREF_NAME = "nayagram_config_prefs";

    /** Experimental rows (Ghost Mode, APK Block) stay in code but are hidden. */
    public static final boolean SHOW_EXPERIMENTAL = false;

    // The Exclusive Features Keys
    private static final String KEY_MESSAGE_SCHEDULER = "feature_02_msg_scheduler";
    private static final String KEY_SMART_AUTO_REPLY = "feature_03_auto_reply";
    private static final String KEY_AUTO_REPLY_TEXT = "feature_03_auto_reply_text";
    private static final String KEY_STORY_SAVER = "feature_04_story_saver";
    private static final String KEY_ANONYMOUS_STORIES = "feature_05_anonymous_stories";
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
    private static final String KEY_APK_BLOCK = "feature_apk_block";
    private static final String KEY_AUTO_ADD_TEXT_ENABLED = "feature_auto_add_text_enabled";
    private static final String KEY_AUTO_ADD_TEXT_VALUE = "feature_auto_add_text_value";
    private static final String KEY_ONE_TIME_VOICE_VIDEO = "feature_one_time_voice_video";
    private static final String KEY_HIDE_PHONE_NUMBER = "feature_hide_phone_number";
    private static final String KEY_VERIFIED_BADGE = "feature_verified_badge";
    private static final String KEY_APP_THEME_MODE = "feature_app_theme_mode";

    public static final int THEME_MODE_LIGHT = 0;
    public static final int THEME_MODE_DARK_SLATE = 1;
    public static final int THEME_MODE_NEON_AURORA = 2;


    private static NayaConfig instance;
    private final SharedPreferences preferences;

    private NayaConfig(Context context) {
        this.preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.isVerifiedBadgeEnabled = this.preferences != null ? this.preferences.getBoolean(KEY_VERIFIED_BADGE, false) : true;
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
    public boolean isMessageScheduler() { return false; }
    public void setMessageScheduler(boolean e) { }

    // 3. Smart Auto-Reply
    public boolean isSmartAutoReply() { return false; }
    public void setSmartAutoReply(boolean e) { }
    public String getAutoReplyText() { return preferences.getString(KEY_AUTO_REPLY_TEXT, "Hello! I am currently busy. I will get back to you soon. (Sent via NayaGram Auto-Reply)"); }
    public void setAutoReplyText(String t) { preferences.edit().putString(KEY_AUTO_REPLY_TEXT, t).apply(); }

    // 4. Story Saver
    /** Removed for policy compliance: downloads other users' stories. Always off. */
    public boolean isStorySaverEnabled() { return false; }
    public void setStorySaverEnabled(boolean e) { /* removed for policy compliance */ }

    // 5. Anonymous Story Viewer
    /** Removed for policy compliance: hides story views from view receipts. Always off. */
    public boolean isAnonymousStories() { return false; }
    public void setAnonymousStories(boolean e) { /* removed for policy compliance */ }

    // 6. Anti-Delete Recovery

    // 18. Adaptive Network & Data Saver
    /** Mirrors Telegram's native stream-media preference so the toggle changes real download behavior. */
    public boolean isDataSaverEnabled() { return isSaveMobileData(); }
    public void setDataSaverEnabled(boolean enabled) {
        preferences.edit().putBoolean("feature_data_saver", enabled).apply();
        setSaveMobileData(enabled);
    }

    // 19. WebRTC Acoustic Noise Suppression
    public boolean isNoiseSuppressionEnabled() { return preferences.getBoolean("feature_noise_suppression", false); }
    public void setNoiseSuppressionEnabled(boolean e) { preferences.edit().putBoolean("feature_noise_suppression", e).apply(); }

    // 20. Smart Storage & Cache Auto-Limiter
    public boolean isAutoCacheGuardEnabled() { return preferences.getBoolean("feature_auto_cache_guard", true); }
    public void setAutoCacheGuardEnabled(boolean e) { preferences.edit().putBoolean("feature_auto_cache_guard", e).apply(); }

    // 9. Smart Chat Folders (Disabled by default for Play Store compliance)
    public boolean isSmartFoldersEnabled() { return preferences.getBoolean(KEY_SMART_CHAT_FOLDERS, false); }
    public void setSmartFoldersEnabled(boolean e) { preferences.edit().putBoolean(KEY_SMART_CHAT_FOLDERS, e).apply(); }

    // 10. Call & Voice Protection — permanently removed (do not re-add isConfirmActions)

    // 11. User ID & DC Display
    public boolean isShowIdAndDc() { return preferences.getBoolean(KEY_SHOW_ID_DC, true); }
    public void setShowIdAndDc(boolean e) { preferences.edit().putBoolean(KEY_SHOW_ID_DC, e).apply(); }

    // 12. Modular NayaConfig
    public boolean isModularConfigActive() { return preferences.getBoolean(KEY_MODULAR_CONFIG, false); }
    public boolean isModularConfig() { return isModularConfigActive(); }
    public void setModularConfig(boolean e) { setModularConfigActive(e); }
    public void setModularConfigActive(boolean e) { preferences.edit().putBoolean(KEY_MODULAR_CONFIG, e).apply(); }

    // 13. Digital Wellbeing & Focus Mode
    public boolean isFocusModeEnabled() { return preferences.getBoolean(KEY_FOCUS_MODE, false); }
    public void setFocusModeEnabled(boolean e) { preferences.edit().putBoolean(KEY_FOCUS_MODE, e).apply(); }

    // 14. Smart Storage Doctor
    public boolean isStorageDoctorEnabled() { return false; }
    public void setStorageDoctorEnabled(boolean e) { }

    // 15. Ultra Battery & Low-Data Saver
    public boolean isBatterySaverEnabled() { return preferences.getBoolean(KEY_BATTERY_SAVER, false); }
    public void setBatterySaverEnabled(boolean e) { preferences.edit().putBoolean(KEY_BATTERY_SAVER, e).apply(); }

    // 16. Biometric Chat Locker
    public boolean isBiometricChatLockerEnabled() { return preferences.getBoolean(KEY_BIOMETRIC_LOCKER, false); }
    public void setBiometricChatLockerEnabled(boolean e) { preferences.edit().putBoolean(KEY_BIOMETRIC_LOCKER, e).apply(); }

    // 17. In-Chat Instant Translator (language meaning / translation — kept)
    public boolean isInstantTranslatorEnabled() { return preferences.getBoolean(KEY_INSTANT_TRANSLATOR, false); }
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

    public boolean isReduceMotion() {
        return preferences.getBoolean("feature_reduce_motion", false);
    }

    public void setReduceMotion(boolean enabled) {
        preferences.edit().putBoolean("feature_reduce_motion", enabled).apply();
        try {
            org.telegram.messenger.SharedConfig.setAnimationsEnabled(!enabled);
            org.telegram.messenger.MessagesController.getGlobalMainSettings().edit().putBoolean("view_animations", !enabled).apply();
        } catch (Throwable ignored) {
        }
    }

    public boolean isChatListDetails() {
        return org.telegram.messenger.SharedConfig.useThreeLinesLayout;
    }

    public void setChatListDetails(boolean enabled) {
        org.telegram.messenger.SharedConfig.setUseThreeLinesLayout(enabled);
    }

    public boolean isPauseGifAutoplay() {
        return !org.telegram.messenger.LiteMode.isEnabled(org.telegram.messenger.LiteMode.FLAG_AUTOPLAY_GIFS);
    }

    public void setPauseGifAutoplay(boolean pause) {
        boolean playing = org.telegram.messenger.LiteMode.isEnabled(org.telegram.messenger.LiteMode.FLAG_AUTOPLAY_GIFS);
        if (playing == pause) {
            org.telegram.messenger.SharedConfig.toggleAutoplayGifs();
        }
    }

    public boolean isSaveMobileData() {
        return !org.telegram.messenger.SharedConfig.streamMedia;
    }

    public void setSaveMobileData(boolean save) {
        if (org.telegram.messenger.SharedConfig.streamMedia == save) {
            org.telegram.messenger.SharedConfig.toggleStreamMedia();
        }
    }

    // APK & ZIP File Security Shield
    public boolean isApkBlockEnabled() { return preferences.getBoolean(KEY_APK_BLOCK, false); }
    public void setApkBlockEnabled(boolean e) { preferences.edit().putBoolean(KEY_APK_BLOCK, e).apply(); }

    public boolean isAutoAddTextEnabled() { return preferences.getBoolean(KEY_AUTO_ADD_TEXT_ENABLED, false); }
    public void setAutoAddTextEnabled(boolean e) { preferences.edit().putBoolean(KEY_AUTO_ADD_TEXT_ENABLED, e).apply(); }
    public String getAutoAddText() { return preferences.getString(KEY_AUTO_ADD_TEXT_VALUE, "@NGSupreme"); }
    public void setAutoAddText(String t) { preferences.edit().putString(KEY_AUTO_ADD_TEXT_VALUE, t != null ? t : "@NGSupreme").apply(); }
    public boolean isOneTimeVoiceVideoEnabled() { return preferences.getBoolean(KEY_ONE_TIME_VOICE_VIDEO, false); }
    public void setOneTimeVoiceVideoEnabled(boolean e) { preferences.edit().putBoolean(KEY_ONE_TIME_VOICE_VIDEO, e).apply(); }
    public boolean isHidePhoneNumberEnabled() { return preferences.getBoolean(KEY_HIDE_PHONE_NUMBER, false); }
    public void setHidePhoneNumberEnabled(boolean e) { preferences.edit().putBoolean(KEY_HIDE_PHONE_NUMBER, e).apply(); }

    public int getAppThemeMode() { return preferences.getInt(KEY_APP_THEME_MODE, THEME_MODE_DARK_SLATE); }
    public void setAppThemeMode(int mode) { preferences.edit().putInt(KEY_APP_THEME_MODE, mode).apply(); }
    
    private static final String KEY_CHANNEL_VERIFIED_BADGE = "key_channel_verified_badge";
    public boolean isChannelVerifiedBadgeEnabled = false;

    public boolean isChannelVerifiedBadgeEnabled() {
        return preferences != null ? preferences.getBoolean(KEY_CHANNEL_VERIFIED_BADGE, false) : false;
    }

    public void setChannelVerifiedBadgeEnabled(boolean enabled) {
        this.isChannelVerifiedBadgeEnabled = enabled;
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_CHANNEL_VERIFIED_BADGE, enabled).apply();
        }
    }

    public boolean isVerifiedBadgeEnabled() {
        return preferences != null ? preferences.getBoolean(KEY_VERIFIED_BADGE, false) : isVerifiedBadgeEnabled;
    }

    public void setVerifiedBadgeEnabled(boolean enabled) {
        this.isVerifiedBadgeEnabled = enabled;
        if (preferences != null) {
            preferences.edit().putBoolean(KEY_VERIFIED_BADGE, enabled).apply();
        }
    }


    public static String formatPhoneNumberPrivacy(String phone, boolean isHidden) {
        if (!isHidden || phone == null || phone.isEmpty()) {
            return phone != null ? phone : "";
        }
        String clean = phone.trim();
        int spaceIdx = clean.indexOf(' ');
        if (spaceIdx > 1 && spaceIdx <= 5 && clean.length() > spaceIdx + 4) {
            String country = clean.substring(0, spaceIdx);
            String last4 = clean.substring(clean.length() - 4);
            return country + " ••• ••• " + last4;
        } else if (clean.length() > 7) {
            String start = clean.startsWith("+") ? clean.substring(0, Math.min(4, clean.length())) : clean.substring(0, 3);
            String end = clean.substring(clean.length() - 4);
            return start + " ••• ••• " + end;
        } else {
            return "••••••••••••";
        }
    }


    /**
     * Export only NayaGram feature preference primitives. User-written auto-reply text,
     * account identifiers, chats, and credentials are intentionally excluded.
     */
    public String exportSettingsJson() {
        JSONObject root = new JSONObject();
        JSONObject settings = new JSONObject();
        try {
            root.put("format", "nayagram-settings");
            root.put("schemaVersion", 1);
            for (Map.Entry<String, ?> entry : preferences.getAll().entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                if (!key.startsWith("feature_") || KEY_AUTO_REPLY_TEXT.equals(key)) {
                    continue;
                }
                if (value instanceof Boolean || value instanceof Integer || value instanceof Long) {
                    settings.put(key, value);
                }
            }
            root.put("settings", settings);
            return root.toString(2);
        } catch (JSONException ignored) {
            return "{\"format\":\"nayagram-settings\",\"schemaVersion\":1,\"settings\":{}}";
        }
    }

    // Reset all features to default values
    public void resetToDefaults() {
        // Apply native-backed defaults before clearing the saved previous-font snapshot.
        setSaveMobileData(false);
        setInternationalClock(false);
        setReadableTextEnabled(false);
        setReduceMotion(false);
        preferences.edit()
            .putBoolean(KEY_MESSAGE_SCHEDULER, false)
            .putBoolean(KEY_SMART_AUTO_REPLY, false)
            .putBoolean(KEY_STORY_SAVER, false)
            .putBoolean(KEY_ANONYMOUS_STORIES, false)
            .putBoolean(KEY_SMART_CHAT_FOLDERS, false)
            .remove(LEGACY_KEY_CONFIRM_ACTIONS)
            .remove("enable_call_protection")
            .remove("enable_voice_protection")
            .remove("call_voice_protection")
            .remove("confirm_call_actions")
            .remove("feature_10_call_voice_protection")
            .putBoolean(KEY_SHOW_ID_DC, true)
            .putBoolean(KEY_MODULAR_CONFIG, false)
            .putBoolean(KEY_FOCUS_MODE, false)
            .putBoolean(KEY_STORAGE_DOCTOR, true)
            .putBoolean(KEY_BATTERY_SAVER, false)
            .putBoolean(KEY_BIOMETRIC_LOCKER, false)
            .putBoolean(KEY_INSTANT_TRANSLATOR, false)
            .putBoolean(KEY_INTL_CLOCK, false)
            .putBoolean(KEY_LINK_GUARD, true)
            .putBoolean(KEY_READABLE_TEXT, false)
            .putBoolean("feature_data_saver", false)
            .putBoolean("feature_noise_suppression", false)
            .putBoolean("feature_auto_cache_guard", false)
            .putBoolean("feature_reduce_motion", false)
            .remove(KEY_READABLE_TEXT_PREV)
            .putBoolean(KEY_AUTO_ADD_TEXT_ENABLED, false)
            .putString(KEY_AUTO_ADD_TEXT_VALUE, "@NGSupreme")
            .putBoolean(KEY_ONE_TIME_VOICE_VIDEO, false)
            .apply();
    }
}
