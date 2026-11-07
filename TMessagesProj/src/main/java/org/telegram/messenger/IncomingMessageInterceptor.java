package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import org.nayagram.platform.NayaConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;

/**
 * Sends the NayaGram Smart Auto-Reply to new incoming private messages.
 * Rules: switch must be on, private 1:1 chats only, never bots or self,
 * never service messages, never replies to another auto-reply, only fresh
 * messages (not old backlog), and at most one reply per chat per cooldown.
 */
public class IncomingMessageInterceptor {
    private static final String TAG = "IncomingMessageInterceptor";
    private static final String PREFS_NAME = "nayagram_auto_reply_state";

    public static void processIncomingMessage(MessageObject messageObject, int accountId) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                if (messageObject == null || messageObject.messageOwner == null) {
                    return;
                }
                NayaConfig config = NayaConfig.getInstance();
                if (!config.isSmartAutoReply()) {
                    return;
                }
                if (messageObject.isOutOwner() || messageObject.messageOwner.action != null) {
                    return;
                }
                long dialogId = messageObject.getDialogId();
                if (dialogId <= 0) {
                    return; // private user chats only: groups, channels and secret chats are skipped
                }
                String incomingText = messageObject.messageOwner.message;
                if (incomingText == null || incomingText.trim().isEmpty()) {
                    return;
                }

                AutoReplyManager autoReplyManager = AutoReplyManager.getInstance();
                AutoReplyManager.AutoReplyRule matchedRule = autoReplyManager.findMatchingRule(incomingText);
                String replyText = matchedRule != null ? matchedRule.response : config.getAutoReplyText();

                if (replyText == null || replyText.trim().isEmpty() || incomingText.equals(replyText)) {
                    return; // never answer another auto-reply
                }
                int now = ConnectionsManager.getInstance(accountId).getCurrentTime();
                // Reject old messages (> 300 seconds)
                if (now - messageObject.messageOwner.date > 300) {
                    return;
                }
                TLRPC.User user = MessagesController.getInstance(accountId).getUser(dialogId);
                if (user == null || user.bot || user.self) {
                    return;
                }
                if (!PerChatAutoReplySettings.canAutoReplyForChat(dialogId)) {
                    return;
                }
                Context context = ApplicationLoader.applicationContext;
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String key = "last_" + accountId + "_" + dialogId;
                long nowMs = System.currentTimeMillis();
                long cooldownMs = config.getAutoReplyCooldownHours() * 3600_000L;
                if (nowMs - prefs.getLong(key, 0L) < cooldownMs) {
                    return;
                }
                prefs.edit().putLong(key, nowMs).apply();

                CharSequence[] messageArr = new CharSequence[]{replyText};
                ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(accountId).getEntities(messageArr, true);
                final String cleanText = messageArr[0] != null ? messageArr[0].toString() : replyText;

                AndroidUtilities.runOnUIThread(() ->
                        SendMessagesHelper.getInstance(accountId).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(
                                        cleanText,
                                        dialogId,
                                        null,
                                        null,
                                        null,
                                        true,
                                        entities,
                                        null,
                                        null,
                                        true,
                                        0,
                                        0,
                                        null,
                                        false
                                )
                        )
                );
                FileLog.d(TAG + ": auto-reply queued for dialog " + dialogId);
            } catch (Exception e) {
                FileLog.e(TAG + ": auto-reply failed", e);
            }
        });
    }
}
