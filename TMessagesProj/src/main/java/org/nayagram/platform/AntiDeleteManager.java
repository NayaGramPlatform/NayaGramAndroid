package org.nayagram.platform;

import android.content.Context;
import android.content.SharedPreferences;

import org.nayagram.platform.messenger.ApplicationLoader;

/**
 * AntiDeleteManager - Handles anti-delete functionality
 * Saves deleted messages locally so they can be recovered
 * 
 * Features:
 * - Enable/Disable anti-delete via settings
 * - Save deleted message content before removal
 * - Display "This message was deleted" with recovery option
 */
public class AntiDeleteManager {
    
    private static final String PREF_NAME = "nayagram_anti_delete_prefs";
    private static final String KEY_ANTI_DELETE_ENABLED = "anti_delete_enabled";
    private static final String KEY_SAVE_DELETED_COUNT = "save_deleted_count";
    
    private static AntiDeleteManager instance;
    private SharedPreferences preferences;
    private Context context;
    
    private AntiDeleteManager(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    /**
     * Get singleton instance of AntiDeleteManager
     */
    public static AntiDeleteManager getInstance(Context context) {
        if (instance == null) {
            synchronized (AntiDeleteManager.class) {
                if (instance == null) {
                    instance = new AntiDeleteManager(context);
                }
            }
        }
        return instance;
    }
    
    /**
     * Get singleton instance - requires ApplicationLoader context
     */
    public static AntiDeleteManager getInstance() {
        return getInstance(ApplicationLoader.applicationContext);
    }
    
    /**
     * Enable or disable anti-delete feature
     */
    public void setAntiDeleteEnabled(boolean enabled) {
        preferences.edit()
                .putBoolean(KEY_ANTI_DELETE_ENABLED, enabled)
                .apply();
    }
    
    /**
     * Check if anti-delete is enabled
     */
    public boolean isAntiDeleteEnabled() {
        return preferences.getBoolean(KEY_ANTI_DELETE_ENABLED, false);
    }
    
    /**
     * Save a deleted message to local database
     * This is called when a message is deleted
     * 
     * @param messageId The ID of the deleted message
     * @param chatId The chat ID where message was deleted
     * @param messageText The original message text
     * @param senderId The ID of message sender
     * @param timestamp When the message was sent
     * @return true if saved successfully
     */
    public boolean saveDeletedMessage(long messageId, long chatId, String messageText, 
                                     long senderId, long timestamp) {
        if (!isAntiDeleteEnabled()) {
            return false;
        }
        
        try {
            // Save to local database via MessagesStorage
            // This will be implemented with database schema updates
            MessageDatabase.getInstance().saveDeletedMessage(
                    messageId, 
                    chatId, 
                    messageText, 
                    senderId, 
                    timestamp
            );
            
            // Increment deleted message counter
            int currentCount = preferences.getInt(KEY_SAVE_DELETED_COUNT, 0);
            preferences.edit()
                    .putInt(KEY_SAVE_DELETED_COUNT, currentCount + 1)
                    .apply();
            
            return true;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Get saved deleted messages for a specific chat
     * 
     * @param chatId The chat ID
     * @return List of saved deleted messages
     */
    public java.util.List<DeletedMessage> getSavedDeletedMessages(long chatId) {
        if (!isAntiDeleteEnabled()) {
            return new java.util.ArrayList<>();
        }
        
        try {
            return MessageDatabase.getInstance().getDeletedMessages(chatId);
        } catch (Exception e) {
            FileLog.e(e);
            return new java.util.ArrayList<>();
        }
    }
    
    /**
     * Restore a deleted message (make it visible again in chat)
     * 
     * @param messageId The ID of the deleted message
     * @return true if restoration was successful
     */
    public boolean restoreDeletedMessage(long messageId) {
        if (!isAntiDeleteEnabled()) {
            return false;
        }
        
        try {
            return MessageDatabase.getInstance().restoreDeletedMessage(messageId);
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Delete a saved deleted message permanently
     * 
     * @param messageId The ID to permanently delete
     * @return true if deletion was successful
     */
    public boolean permanentlyDeleteMessage(long messageId) {
        try {
            return MessageDatabase.getInstance().permanentlyDeleteMessage(messageId);
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Get count of saved deleted messages
     */
    public int getSavedDeletedCount() {
        return preferences.getInt(KEY_SAVE_DELETED_COUNT, 0);
    }
    
    /**
     * Clear all saved deleted messages
     */
    public void clearAllDeletedMessages() {
        try {
            MessageDatabase.getInstance().clearAllDeletedMessages();
            preferences.edit()
                    .putInt(KEY_SAVE_DELETED_COUNT, 0)
                    .apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
    
    /**
     * Data model for deleted messages
     */
    public static class DeletedMessage {
        public long messageId;
        public long chatId;
        public String messageText;
        public long senderId;
        public long originalTimestamp;
        public long deletedTimestamp;
        public boolean isRestored;
        
        public DeletedMessage(long messageId, long chatId, String messageText, 
                            long senderId, long originalTimestamp, long deletedTimestamp) {
            this.messageId = messageId;
            this.chatId = chatId;
            this.messageText = messageText;
            this.senderId = senderId;
            this.originalTimestamp = originalTimestamp;
            this.deletedTimestamp = deletedTimestamp;
            this.isRestored = false;
        }
    }
}
