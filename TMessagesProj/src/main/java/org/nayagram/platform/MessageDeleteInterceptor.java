package org.nayagram.platform;

import org.nayagram.platform.messenger.SendMessagesHelper;
import org.nayagram.platform.tgnet.TLRPC;

/**
 * MessageDeleteInterceptor - Intercepts message deletion events
 * Saves deleted messages before they are permanently removed
 */
public class MessageDeleteInterceptor {
    
    private static MessageDeleteInterceptor instance;
    private AntiDeleteManager antiDeleteManager;
    
    private MessageDeleteInterceptor() {
        antiDeleteManager = AntiDeleteManager.getInstance();
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized MessageDeleteInterceptor getInstance() {
        if (instance == null) {
            instance = new MessageDeleteInterceptor();
        }
        return instance;
    }
    
    /**
     * Called when a message is about to be deleted
     * Saves the message if anti-delete is enabled
     *
     * @param messageId The message ID being deleted
     * @param chatId The chat ID containing the message
     * @param messageObject The message object containing content
     * @return true if message was saved, false otherwise
     */
    public boolean onMessageDeleting(int messageId, long chatId, TLRPC.Message messageObject) {
        if (!antiDeleteManager.isAntiDeleteEnabled() || messageObject == null) {
            return false;
        }
        
        try {
            String messageText = extractMessageText(messageObject);
            long senderId = messageObject.from_id != null ? messageObject.from_id.user_id : 0;
            long timestamp = messageObject.date;
            
            // Save to database
            boolean saved = antiDeleteManager.saveDeletedMessage(
                    messageId,
                    chatId,
                    messageText,
                    senderId,
                    timestamp
            );
            
            if (saved) {
                FileLog.d("Anti-Delete: Message " + messageId + " saved before deletion");
            }
            
            return saved;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Called when a message is deleted for everyone
     * Saves the message content before it's removed from the chat
     *
     * @param messageId The message ID
     * @param chatId The chat ID
     * @param messageObject The message object
     * @return true if message was saved
     */
    public boolean onMessageDeletedForEveryone(int messageId, long chatId, TLRPC.Message messageObject) {
        if (!antiDeleteManager.isAntiDeleteEnabled() || messageObject == null) {
            return false;
        }
        
        try {
            String messageText = extractMessageText(messageObject);
            long senderId = messageObject.from_id != null ? messageObject.from_id.user_id : 0;
            long timestamp = messageObject.date;
            
            // Save with media if present
            String mediaPath = extractMediaPath(messageObject);
            
            boolean saved;
            if (mediaPath != null) {
                saved = antiDeleteManager.saveDeletedMessageWithMedia(
                        messageId,
                        chatId,
                        messageText,
                        senderId,
                        timestamp,
                        mediaPath
                );
            } else {
                saved = antiDeleteManager.saveDeletedMessage(
                        messageId,
                        chatId,
                        messageText,
                        senderId,
                        timestamp
                );
            }
            
            if (saved) {
                FileLog.d("Anti-Delete: Message " + messageId + " deleted for everyone but saved locally");
            }
            
            return saved;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Extract message text from message object
     *
     * @param message The TLRPC message object
     * @return The message text or description
     */
    private String extractMessageText(TLRPC.Message message) {
        if (message.message != null && !message.message.isEmpty()) {
            return message.message;
        }
        
        // Handle media captions
        if (message.caption != null && !message.caption.isEmpty()) {
            return message.caption;
        }
        
        // Handle media type messages
        if (message.media != null) {
            if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
                return "[Photo]";
            } else if (message.media instanceof TLRPC.TL_messageMediaVideo) {
                return "[Video]";
            } else if (message.media instanceof TLRPC.TL_messageMediaDocument) {
                return "[File]";
            } else if (message.media instanceof TLRPC.TL_messageMediaAudio) {
                return "[Audio]";
            } else if (message.media instanceof TLRPC.TL_messageMediaVenue) {
                return "[Location]";
            } else if (message.media instanceof TLRPC.TL_messageMediaContact) {
                return "[Contact]";
            }
        }
        
        return "[Message]";
    }
    
    /**
     * Extract media path from message if available
     *
     * @param message The TLRPC message object
     * @return The media file path or null
     */
    private String extractMediaPath(TLRPC.Message message) {
        if (message.media instanceof TLRPC.TL_messageMediaDocument) {
            TLRPC.Document doc = ((TLRPC.TL_messageMediaDocument) message.media).document;
            if (doc instanceof TLRPC.TL_document) {
                // Try to get the cached file path
                return FileLoader.getPathToAttach(doc, true).toString();
            }
        } else if (message.media instanceof TLRPC.TL_messageMediaPhoto) {
            TLRPC.Photo photo = ((TLRPC.TL_messageMediaPhoto) message.media).photo;
            if (photo instanceof TLRPC.TL_photo) {
                return FileLoader.getPathToAttach(photo.sizes.get(0), true).toString();
            }
        }
        return null;
    }
    
    /**
     * Check if a message was deleted but saved by anti-delete
     *
     * @param messageId The message ID
     * @return true if message was saved by anti-delete
     */
    public boolean wasMessageSaved(long messageId) {
        if (!antiDeleteManager.isAntiDeleteEnabled()) {
            return false;
        }
        return MessageDatabase.getInstance().getDeletedMessage(messageId) != null;
    }
    
    /**
     * Restore a deleted message to chat
     *
     * @param messageId The message ID to restore
     * @return true if restoration was successful
     */
    public boolean restoreDeletedMessage(long messageId) {
        return antiDeleteManager.restoreDeletedMessage(messageId);
    }
}
