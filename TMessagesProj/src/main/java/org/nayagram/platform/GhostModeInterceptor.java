package org.nayagram.platform;

import org.telegram.tgnet.TLRPC;
import org.telegram.messenger.FileLog;

/**
 * GhostModeInterceptor - Intercepts and modifies messages/status updates for Ghost Mode
 * Handles hiding:
 * - Typing indicators
 * - Online status
 * - Forward tags on messages
 * - Read receipts
 */
public class GhostModeInterceptor {
    
    private static GhostModeInterceptor instance;
    private GhostModeManager ghostModeManager;
    
    private GhostModeInterceptor() {
        ghostModeManager = GhostModeManager.getInstance();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized GhostModeInterceptor getInstance() {
        if (instance == null) {
            instance = new GhostModeInterceptor();
        }
        return instance;
    }
    
    /**
     * Intercept typing indicator before sending
     * If ghost mode enabled, typing status is not sent
     * 
     * @param chatId The chat ID
     * @param typingStatus The typing status (typing, voice recording, etc)
     * @return true if typing indicator should be sent, false if should be blocked
     */
    public boolean shouldSendTypingIndicator(long chatId, int typingStatus) {
        if (!ghostModeManager.isGhostModeEnabled()) {
            return true;
        }
        
        // Check if hide typing is enabled
        if (ghostModeManager.isHideTypingStatus()) {
            FileLog.d("GhostMode: Blocked typing indicator for chat " + chatId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Intercept online status updates
     * If ghost mode enabled, user appears offline
     * 
     * @return The status to send (online or offline)
     */
    public boolean shouldSendOnlineStatus() {
        if (!ghostModeManager.isGhostModeEnabled()) {
            return true;
        }
        
        if (ghostModeManager.isHideOnlineStatus()) {
            FileLog.d("GhostMode: Hiding online status");
            return false;
        }
        
        return true;
    }
    
    /**
     * Intercept message before sending to hide forward tag
     * Removes "Forwarded from" label if ghost mode enabled
     * 
     * @param message The message to modify
     * @return Modified message with forward tag hidden if needed
     */
    public TLRPC.Message processMessageForForwardTag(TLRPC.Message message) {
        if (message == null) {
            return message;
        }
        
        if (!ghostModeManager.isGhostModeEnabled()) {
            return message;
        }
        
        if (!ghostModeManager.isHideForwardTag()) {
            return message;
        }
        
        // Remove forward information
        if (message.fwd_from != null) {
            message.fwd_from = null;
            FileLog.d("GhostMode: Removed forward tag from message");
        }
        
        return message;
    }
    
    /**
     * Intercept read receipt before sending
     * If ghost mode enabled, read receipts are not sent
     * 
     * @param chatId The chat ID
     * @param messageId The message ID being read
     * @return true if read receipt should be sent, false if should be blocked
     */
    public boolean shouldSendReadReceipt(long chatId, int messageId) {
        if (!ghostModeManager.isGhostModeEnabled()) {
            return true;
        }
        
        if (ghostModeManager.isHideReadReceipts()) {
            FileLog.d("GhostMode: Blocked read receipt for message " + messageId);
            return false;
        }
        
        return true;
    }
    
    /**
     * Intercept online status update before sending
     * Modifies user status to show as offline if ghost mode enabled
     * 
     * @param userStatus Current user status
     * @return Modified user status
     */
    public TLRPC.UserStatus processUserStatus(TLRPC.UserStatus userStatus) {
        if (userStatus == null || !ghostModeManager.isGhostModeEnabled()) {
            return userStatus;
        }
        
        if (!ghostModeManager.isHideOnlineStatus()) {
            return userStatus;
        }
        
        // Set status to offline
        TLRPC.TL_userStatusOffline offlineStatus = new TLRPC.TL_userStatusOffline();
        offlineStatus.expires = (int) (System.currentTimeMillis() / 1000) - 300; // 5 minutes ago
        
        FileLog.d("GhostMode: Changed user status to offline");
        return offlineStatus;
    }
    
    /**
     * Check if typing indicator should be shown for a specific chat
     * Allows selective ghost mode per chat
     * 
     * @param chatId The chat ID
     * @return true if typing should be hidden, false if should be shown
     */
    public boolean shouldHideTypingForChat(long chatId) {
        if (!ghostModeManager.isGhostModeEnabled()) {
            return false;
        }
        
        // Check if ghost mode is enabled for this specific chat
        if (ghostModeManager.isGhostModeEnabledForChat(chatId)) {
            return true;
        }
        
        return ghostModeManager.isHideTypingStatus();
    }
    
    /**
     * Check if online status should be hidden for a specific chat
     * 
     * @param chatId The chat ID
     * @return true if online status should be hidden, false if should be shown
     */
    public boolean shouldHideOnlineStatusForChat(long chatId) {
        if (!ghostModeManager.isGhostModeEnabled()) {
            return false;
        }
        
        if (ghostModeManager.isGhostModeEnabledForChat(chatId)) {
            return true;
        }
        
        return ghostModeManager.isHideOnlineStatus();
    }
    
    /**
     * Validate if ghost mode can be toggled
     * Returns false if there are active calls or other conflicting states
     * 
     * @return true if ghost mode can be safely enabled
     */
    public boolean canEnableGhostMode() {
        // TODO: Add checks for active calls, group editing, etc.
        return true;
    }
    
    /**
     * Get summary of what is hidden in ghost mode
     * 
     * @return Array of hidden features
     */
    public String[] getHiddenFeatures() {
        GhostModeManager.GhostModeStatus status = ghostModeManager.getStatus();
        int count = status.getEnabledCount();
        String[] features = new String[count];
        int index = 0;
        
        if (status.hideTyping) {
            features[index++] = "Typing Status";
        }
        if (status.hideOnline) {
            features[index++] = "Online Status";
        }
        if (status.hideForwardTag) {
            features[index++] = "Forward Tag";
        }
        if (status.hideReadReceipts) {
            features[index++] = "Read Receipts";
        }
        
        return features;
    }
}
