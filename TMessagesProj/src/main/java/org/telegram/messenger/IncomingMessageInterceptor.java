package org.telegram.messenger;

/**
 * IncomingMessageInterceptor - Handles auto-reply logic for incoming messages
 * Integrates AutoReplyManager with the message receive flow
 */
public class IncomingMessageInterceptor {
    private static final String TAG = "IncomingMessageInterceptor";

    /**
     * Process incoming message for auto-reply
     * Call this when receiving a new incoming message
     * 
     * @param messageText The incoming message text
     * @param fromUserId The sender user ID
     * @param dialogId The chat/dialog ID
     * @param accountId The account ID
     * @return true if auto-reply was sent, false otherwise
     */
    public static boolean processIncomingMessage(String messageText, int fromUserId, 
                                                  long dialogId, int accountId) {
        try {
            // Check if auto-reply is globally enabled
            AutoReplyManager autoReplyManager = AutoReplyManager.getInstance();
            if (!autoReplyManager.isAutoReplyEnabled()) {
                return false;
            }

            // Check if auto-reply is disabled for this specific chat
            if (!PerChatAutoReplySettings.canAutoReplyForChat(dialogId)) {
                FileLog.d(TAG + ": Auto-reply disabled for chat " + dialogId);
                return false;
            }

            // Check if message is from user themselves or system
            if (!OwnMessageFilter.canAutoReply(fromUserId, accountId)) {
                FileLog.d(TAG + ": Skipping auto-reply for own/system message from user " + fromUserId);
                return false;
            }

            // Check if we already replied to this message
            DuplicateReplyPrevention dupeChecker = DuplicateReplyPrevention.getInstance();
            long messageHashId = hashMessageId(messageText, fromUserId, dialogId);
            if (dupeChecker.hasAlreadyReplied(messageHashId)) {
                FileLog.d(TAG + ": Already replied to this message");
                return false;
            }

            // Find matching auto-reply rule
            AutoReplyManager.AutoReplyRule matchingRule = autoReplyManager.findMatchingRule(messageText);
            if (matchingRule == null) {
                return false;
            }

            // Send auto-reply
            String replyText = matchingRule.response;
            if (replyText == null || replyText.isEmpty()) {
                return false;
            }

            try {
                SendMessagesHelper sendHelper = SendMessagesHelper.getInstance(accountId);
                SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                        replyText, dialogId, null, null, null, true, null, null, 
                        null, true, 0, 0, null, false);
                sendHelper.sendMessage(params);

                // Mark as replied to prevent duplicate
                dupeChecker.markAsReplied(messageHashId);
                FileLog.d(TAG + ": Auto-reply sent for message from user " + fromUserId + " in chat " + dialogId);
                return true;
            } catch (Exception e) {
                FileLog.e(TAG + ": Error sending auto-reply", e);
                return false;
            }

        } catch (Exception e) {
            FileLog.e(TAG + ": Error processing incoming message for auto-reply", e);
            return false;
        }
    }

    /**
     * Create a unique hash for a message to track duplicates
     * Combines message text, sender, and dialog to create unique ID
     */
    private static long hashMessageId(String messageText, int fromUserId, long dialogId) {
        try {
            String combined = messageText + "|" + fromUserId + "|" + dialogId;
            return combined.hashCode() & 0xFFFFFFFFL;
        } catch (Exception e) {
            return (messageText.hashCode() ^ fromUserId ^ (int)(dialogId & 0xFFFFFFFF)) & 0xFFFFFFFFL;
        }
    }
}
