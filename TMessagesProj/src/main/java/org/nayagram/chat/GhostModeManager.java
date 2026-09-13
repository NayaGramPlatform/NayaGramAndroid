package org.nayagram.chat;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * GhostModeManager - Manages Ghost Mode and hide features
 * Hides typing status, online status, forward tags, and read receipts
 * 
 * Features:
 * - Hide typing status (no one sees you typing)
 * - Hide online status (show as offline)
 * - Hide forward tag from forwarded messages
 * - Control read receipts visibility
 */
public class GhostModeManager {
    
    private static final String PREF_NAME = "nayagram_ghost_mode_prefs";
    private static final String KEY_GHOST_MODE_ENABLED = "ghost_mode_enabled";
    private static final String KEY_HIDE_TYPING = "hide_typing_status";
    private static final String KEY_HIDE_ONLINE = "hide_online_status";
    private static final String KEY_HIDE_FORWARD_TAG = "hide_forward_tag";
    private static final String KEY_HIDE_READ_RECEIPTS = "hide_read_receipts";
    private static final String KEY_SELECTIVE_GHOST = "selective_ghost_chats";
    
    private static GhostModeManager instance;
    private SharedPreferences preferences;
    private Context context;
    
    private GhostModeManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get singleton instance of GhostModeManager
     */
    public static GhostModeManager getInstance(Context context) {
        if (instance == null) {
            synchronized (GhostModeManager.class) {
                if (instance == null) {
                    instance = new GhostModeManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Get singleton instance - requires ApplicationLoader context
     */
    public static GhostModeManager getInstance() {
        return getInstance(ApplicationLoader.applicationContext);
    }
    
    /**
     * Enable or disable ghost mode (master toggle)
     */
    public void setGhostModeEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_GHOST_MODE_ENABLED, enabled)
                .apply();
    }
    
    /**
     * Check if ghost mode is enabled
     */
    public boolean isGhostModeEnabled() {
        return preferences.getBoolean(KEY_GHOST_MODE_ENABLED, false);
    }
    
    /**
     * Enable or disable hiding typing status
     */
    public void setHideTypingStatus(boolean hide) {
        preferences.edit()
                .putBoolean(KEY_HIDE_TYPING, hide)
                .apply();
    }
    
    /**
     * Check if typing status should be hidden
     */
    public boolean isHideTypingStatus() {
        return isGhostModeEnabled() && preferences.getBoolean(KEY_HIDE_TYPING, false);
    }
    
    /**
     * Enable or disable hiding online status
     */
    public void setHideOnlineStatus(boolean hide) {
        preferences.edit()
                .putBoolean(KEY_HIDE_ONLINE, hide)
                .apply();
    }
    
    /**
     * Check if online status should be hidden
     */
    public boolean isHideOnlineStatus() {
        return isGhostModeEnabled() && preferences.getBoolean(KEY_HIDE_ONLINE, false);
    }
    
    /**
     * Enable or disable hiding forward tags
     */
    public void setHideForwardTag(boolean hide) {
        preferences.edit()
                .putBoolean(KEY_HIDE_FORWARD_TAG, hide)
                .apply();
    }
    
    /**
     * Check if forward tags should be hidden
     */
    public boolean isHideForwardTag() {
        return isGhostModeEnabled() && preferences.getBoolean(KEY_HIDE_FORWARD_TAG, false);
    }
    
    /**
     * Enable or disable hiding read receipts
     */
    public void setHideReadReceipts(boolean hide) {
        preferences.edit()
                .putBoolean(KEY_HIDE_READ_RECEIPTS, hide)
                .apply();
    }
    
    /**
     * Check if read receipts should be hidden
     * When enabled, messages appear unread even when read
     */
    public boolean isHideReadReceipts() {
        return isGhostModeEnabled() && preferences.getBoolean(KEY_HIDE_READ_RECEIPTS, false);
    }
    
    /**
     * Check if ghost mode is enabled for specific chat
     * This allows selective ghost mode per chat
     */
    public boolean isGhostModeEnabledForChat(long chatId) {
        String selectiveGhosts = preferences.getString(KEY_SELECTIVE_GHOST, "");
        return selectiveGhosts.contains("," + chatId + ",");
    }
    
    /**
     * Enable ghost mode for specific chat
     */
    public void enableGhostModeForChat(long chatId) {
        String selectiveGhosts = preferences.getString(KEY_SELECTIVE_GHOST, "");
        if (!selectiveGhosts.contains("," + chatId + ",")) {
            selectiveGhosts += chatId + ",";
            preferences.edit()
                    .putString(KEY_SELECTIVE_GHOST, selectiveGhosts)
                    .apply();
        }
    }
    
    /**
     * Disable ghost mode for specific chat
     */
    public void disableGhostModeForChat(long chatId) {
        String selectiveGhosts = preferences.getString(KEY_SELECTIVE_GHOST, "");
        selectiveGhosts = selectiveGhosts.replace("," + chatId + ",", ",");
        preferences.edit()
                .putString(KEY_SELECTIVE_GHOST, selectiveGhosts)
                .apply();
    }
    
    /**
     * Get all features status as a summary
     */
    public GhostModeStatus getStatus() {
        return new GhostModeStatus(
                isGhostModeEnabled(),
                isHideTypingStatus(),
                isHideOnlineStatus(),
                isHideForwardTag(),
                isHideReadReceipts()
        );
    }
    
    /**
     * Reset all ghost mode settings to default
     */
    public void resetAllSettings() {
        preferences.edit()
                .putBoolean(KEY_GHOST_MODE_ENABLED, false)
                .putBoolean(KEY_HIDE_TYPING, false)
                .putBoolean(KEY_HIDE_ONLINE, false)
                .putBoolean(KEY_HIDE_FORWARD_TAG, false)
                .putBoolean(KEY_HIDE_READ_RECEIPTS, false)
                .putString(KEY_SELECTIVE_GHOST, "")
                .apply();
    }
    
    /**
     * Data model for Ghost Mode status
     */
    public static class GhostModeStatus {
        public boolean ghostModeEnabled;
        public boolean hideTyping;
        public boolean hideOnline;
        public boolean hideForwardTag;
        public boolean hideReadReceipts;
        
        public GhostModeStatus(boolean ghostModeEnabled, boolean hideTyping,
                             boolean hideOnline, boolean hideForwardTag,
                             boolean hideReadReceipts) {
            this.ghostModeEnabled = ghostModeEnabled;
            this.hideTyping = hideTyping;
            this.hideOnline = hideOnline;
            this.hideForwardTag = hideForwardTag;
            this.hideReadReceipts = hideReadReceipts;
        }
        
        public int getEnabledCount() {
            int count = 0;
            if (hideTyping) count++;
            if (hideOnline) count++;
            if (hideForwardTag) count++;
            if (hideReadReceipts) count++;
            return count;
        }
    }
}
