package org.telegram.messenger;

import org.nayagram.platform.NayaConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.regex.Pattern;

/**
 * IncomingMessageInterceptor - Clean Novagram-style Auto Answer dispatcher.
 * - Replies once per dialog ("Automatically reply once to the first message") with native Telegram formatting.
 * - Respects Private Chats Only setting, skips bots/self/service/outgoing/stale messages.
 * - Zero keyword rule clutter or markdown symbol leaks.
 */
public class IncomingMessageInterceptor {
    private static final String TAG = "IncomingMessageInterceptor";
    private static final Pattern SINGLE_UNDERSCORE_PATTERN = Pattern.compile("(?<!_)_([^\\s_][^_]*?[^\\s_]|[^\\s_])_(?!_)");

    public static void processIncomingMessage(MessageObject messageObject, int accountId) {
        Utilities.globalQueue.postRunnable(() -> {
            try {
                if (messageObject == null || messageObject.messageOwner == null) {
                    return;
                }
                NayaConfig config = NayaConfig.getInstance();
                AutoReplyManager autoReplyManager = AutoReplyManager.getInstance();
                if (!config.isSmartAutoReply() && !autoReplyManager.isAutoReplyEnabled()) {
                    return;
                }
                if (messageObject.isOutOwner() || messageObject.messageOwner.action != null) {
                    return;
                }

                long dialogId = messageObject.getDialogId();
                if (autoReplyManager.isPrivateOnly() && dialogId <= 0) {
                    return;
                }
                if (dialogId == 0) {
                    return;
                }

                // Skip broadcast channels where we cannot post
                if (dialogId < 0) {
                    TLRPC.Chat chat = MessagesController.getInstance(accountId).getChat(-dialogId);
                    if (chat != null && ChatObject.isChannel(chat) && !chat.megagroup && !ChatObject.canPost(chat)) {
                        return;
                    }
                }

                String incomingText = messageObject.messageOwner.message;
                if (incomingText == null || incomingText.trim().isEmpty()) {
                    return;
                }

                String replyText = autoReplyManager.getAutoAnswerText();
                if (replyText == null || replyText.trim().isEmpty()) {
                    replyText = config.getAutoReplyText();
                }
                if (replyText == null || replyText.trim().isEmpty() || incomingText.trim().equalsIgnoreCase(replyText.trim())) {
                    return;
                }

                // Check message freshness (1 hour tolerance for background delivery)
                int now = ConnectionsManager.getInstance(accountId).getCurrentTime();
                if (now <= 0) {
                    now = (int) (System.currentTimeMillis() / 1000L);
                }
                if (messageObject.messageOwner.date > 0 && Math.abs(now - messageObject.messageOwner.date) > 3600) {
                    return;
                }

                // Skip bots or self in private chats
                if (dialogId > 0) {
                    TLRPC.User user = MessagesController.getInstance(accountId).getUser(dialogId);
                    if (user == null) {
                        try {
                            user = MessagesStorage.getInstance(accountId).getUserSync(dialogId);
                            if (user != null) {
                                MessagesController.getInstance(accountId).putUser(user, true);
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (user != null && (user.bot || user.self)) {
                        return;
                    }
                }

                if (!PerChatAutoReplySettings.canAutoReplyForChat(dialogId)) {
                    return;
                }

                // Novagram rule: automatically reply once per dialog until toggled off or reset
                if (autoReplyManager.hasAnsweredDialog(dialogId)) {
                    return;
                }
                autoReplyManager.markAnsweredDialog(dialogId);

                ArrayList<TLRPC.MessageEntity> entities = null;
                String cleanText = replyText;
                String entJson = autoReplyManager.getAutoAnswerEntitiesJson();
                if (entJson != null && !entJson.isEmpty()) {
                    entities = AutoReplyManager.parseEntitiesFromJson(entJson);
                }
                if (entities == null || entities.isEmpty()) {
                    String preprocessed = SINGLE_UNDERSCORE_PATTERN.matcher(replyText).replaceAll("__$1__");
                    CharSequence[] messageArr = new CharSequence[]{preprocessed};
                    entities = MediaDataController.getInstance(accountId).getEntities(messageArr, true);
                    if (entities == null) {
                        entities = new ArrayList<>();
                    }
                    cleanText = messageArr[0] != null ? messageArr[0].toString() : replyText;
                }

                try {
                    int activeStyle = org.nayagram.platform.textstyle.NGTextStyleHelper.getSelectedStyle();
                    if (activeStyle != org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_NONE
                            && activeStyle != org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_REGULAR
                            && cleanText.length() > 0) {
                        TLRPC.MessageEntity entity = null;
                        switch (activeStyle) {
                            case org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_BOLD:
                                entity = new TLRPC.TL_messageEntityBold();
                                break;
                            case org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_ITALIC:
                                entity = new TLRPC.TL_messageEntityItalic();
                                break;
                            case org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_SPOILER:
                                entity = new TLRPC.TL_messageEntitySpoiler();
                                break;
                            case org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_MONO:
                                entity = new TLRPC.TL_messageEntityCode();
                                break;
                            case org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_STRIKE:
                                entity = new TLRPC.TL_messageEntityStrike();
                                break;
                            case org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_UNDERLINE:
                                entity = new TLRPC.TL_messageEntityUnderline();
                                break;
                            case org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_QUOTE:
                                entity = new TLRPC.TL_messageEntityBlockquote();
                                break;
                        }
                        if (entity != null) {
                            entity.offset = 0;
                            entity.length = cleanText.length();
                            entities.add(entity);
                        }
                    }
                } catch (Throwable ignored) {}

                final ArrayList<TLRPC.MessageEntity> finalEntities = entities;
                final String finalCleanText = cleanText;

                AndroidUtilities.runOnUIThread(() -> {
                    ConnectionsManager cm = ConnectionsManager.getInstance(accountId);
                    try {
                        cm.setAppPaused(false, false);
                        cm.resumeNetworkMaybe();
                        cm.checkConnection();
                    } catch (Throwable ignored) {}

                    SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                            finalCleanText, dialogId, null, null, null, true, finalEntities, null, null, true, 0, 0, null, false
                    );
                    SendMessagesHelper.getInstance(accountId).sendMessage(params);

                    if (NayaConfig.getInstance().isAutoReplyStealthMode()) {
                        enforceStealthOfflinePresence(accountId);
                    }

                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            if (ApplicationLoader.mainInterfacePaused) {
                                cm.setAppPaused(true, false);
                            }
                        } catch (Throwable ignored) {}
                    }, 5000);
                });
                FileLog.d(TAG + ": auto-answer dispatched to dialog " + dialogId);
            } catch (Exception e) {
                FileLog.e(TAG + ": auto-answer failed", e);
            }
        });
    }

    private static void enforceStealthOfflinePresence(final int accountId) {
        sendOfflineStatus(accountId);
        AndroidUtilities.runOnUIThread(() -> sendOfflineStatus(accountId), 1200);
        AndroidUtilities.runOnUIThread(() -> sendOfflineStatus(accountId), 3500);
    }

    private static void sendOfflineStatus(int accountId) {
        try {
            org.telegram.tgnet.tl.TL_account.updateStatus req = new org.telegram.tgnet.tl.TL_account.updateStatus();
            req.offline = true;
            ConnectionsManager.getInstance(accountId).sendRequest(req, (response, error) -> {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d(TAG + ": stealth presence offline: " + (error == null ? "OK" : error.text));
                }
            });
        } catch (Throwable t) {
            FileLog.e(TAG + ": failed to send stealth offline status", t);
        }
    }
}
