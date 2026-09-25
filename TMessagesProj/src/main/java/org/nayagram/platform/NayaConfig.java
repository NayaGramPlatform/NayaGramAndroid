package org.nayagram.platform;

import android.content.Context;
import android.content.SharedPreferences;
import org.telegram.messenger.ApplicationLoader;

/**
 * NayaConfig - Global configuration & preferences for NayaGram client features
 * Inspired by Nekogram, Cherrygram, and Novagram.
 */
public class NayaConfig {

    private static final String PREF_NAME = "nayagram_config_prefs";
    private static final String KEY_FORWARD_NO_QUOTE = "forward_without_quote";
    private static final String KEY_CONFIRM_ACTIONS = "confirm_actions_send_calls";
    private static final String KEY_SHOW_ID_DC = "show_id_and_dc";
    private static final String KEY_ANONYMOUS_STORIES = "anonymous_stories";
    private static final String KEY_STORY_SAVER = "story_saver_enabled";

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

    // 07: Forward without quote (Direct Share / hide sender name)
    public boolean isForwardWithoutQuote() {
        return preferences.getBoolean(KEY_FORWARD_NO_QUOTE, false);
    }

    public void setForwardWithoutQuote(boolean enabled) {
        preferences.edit().putBoolean(KEY_FORWARD_NO_QUOTE, enabled).apply();
    }

    // 08: Confirm actions (Calls & Voice/Video notes to prevent accidental sends)
    public boolean isConfirmActions() {
        return preferences.getBoolean(KEY_CONFIRM_ACTIONS, true);
    }

    public void setConfirmActions(boolean enabled) {
        preferences.edit().putBoolean(KEY_CONFIRM_ACTIONS, enabled).apply();
    }

    // 09: Show Telegram ID & DC (Datacenter) in profile & chat info
    public boolean isShowIdAndDc() {
        return preferences.getBoolean(KEY_SHOW_ID_DC, true);
    }

    public void setShowIdAndDc(boolean enabled) {
        preferences.edit().putBoolean(KEY_SHOW_ID_DC, enabled).apply();
    }

    // 10: Anonymous story viewing & story downloader
    public boolean isAnonymousStories() {
        return preferences.getBoolean(KEY_ANONYMOUS_STORIES, false);
    }

    public void setAnonymousStories(boolean enabled) {
        preferences.edit().putBoolean(KEY_ANONYMOUS_STORIES, enabled).apply();
    }

    public boolean isStorySaverEnabled() {
        return preferences.getBoolean(KEY_STORY_SAVER, true);
    }

    public void setStorySaverEnabled(boolean enabled) {
        preferences.edit().putBoolean(KEY_STORY_SAVER, enabled).apply();
    }

    // Reset all NayaConfig features to default values
    public void resetToDefaults() {
        preferences.edit()
            .putBoolean(KEY_FORWARD_NO_QUOTE, false)
            .putBoolean(KEY_CONFIRM_ACTIONS, true)
            .putBoolean(KEY_SHOW_ID_DC, true)
            .putBoolean(KEY_ANONYMOUS_STORIES, false)
            .putBoolean(KEY_STORY_SAVER, true)
            .apply();
    }
}
