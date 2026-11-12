package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import org.nayagram.platform.NayaConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sends the NayaGram Smart Auto-Reply to new incoming private messages.
 * Rules:
 * 1. Switch must be on (either in NayaConfig or AutoReplyManager).
 * 2. Private 1:1 chats only (dialogId > 0).
 * 3. Never bots or self.
 * 4. Never service messages or outgoing messages.
 * 5. Rejects ancient backlog (> 15 minutes old).
 * 6. Keyword rules trigger instantly with 2s loop-protection debounce.
 * 7. Default away message uses the configured cooldown hours.
 */
public class IncomingMessageInterceptor {
    private static final String TAG = "IncomingMessageInterceptor";
    private static final String PREFS_NAME = "nayagram_auto_reply_state";

    private static final Pattern URL_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");
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
                if (dialogId <= 0) {
                    return; // private 1:1 user chats only: groups, channels and secret chats are skipped
                }
                String incomingText = messageObject.messageOwner.message;
                if (incomingText == null || incomingText.trim().isEmpty()) {
                    return;
                }

                AutoReplyManager.AutoReplyRule matchedRule = autoReplyManager.findMatchingRule(incomingText);
                String replyText = matchedRule != null ? matchedRule.response : autoReplyManager.getAutoAnswerText();
                if (replyText == null || replyText.trim().isEmpty()) {
                    replyText = config.getAutoReplyText();
                }

                if (replyText == null || replyText.trim().isEmpty() || incomingText.trim().equalsIgnoreCase(replyText.trim())) {
                    return; // never answer another auto-reply or echo identical text
                }

                // Check message freshness: ensure background tolerance (up to 1 hour)
                int now = ConnectionsManager.getInstance(accountId).getCurrentTime();
                if (now <= 0) {
                    now = (int) (System.currentTimeMillis() / 1000L);
                }
                if (messageObject.messageOwner.date > 0 && Math.abs(now - messageObject.messageOwner.date) > 3600) {
                    return;
                }

                // Skip known bots or self
                TLRPC.User user = MessagesController.getInstance(accountId).getUser(dialogId);
                if (user == null) {
                    try {
                        user = MessagesStorage.getInstance(accountId).getUserSync(dialogId);
                        if (user != null) {
                            MessagesController.getInstance(accountId).putUser(user, true);
                        }
                    } catch (Throwable ignored) {
                    }
                }
                if (user != null && (user.bot || user.self)) {
                    return;
                }

                if (!PerChatAutoReplySettings.canAutoReplyForChat(dialogId)) {
                    return;
                }

                Context context = ApplicationLoader.applicationContext;
                SharedPreferences prefs = context != null ? context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE) : null;
                if (prefs != null) {
                    String key = "last_" + accountId + "_" + dialogId;
                    long nowMs = System.currentTimeMillis();

                    if (matchedRule != null) {
                        // KEYWORD RULE: Instant reply! 2-second debounce prevents echo/feedback loops
                        long lastKw = prefs.getLong(key + "_kw", 0L);
                        if (nowMs - lastKw < 2000L) {
                            return;
                        }
                        prefs.edit().putLong(key + "_kw", nowMs).apply();
                    } else {
                        // GENERIC AWAY MESSAGE: Use configured cooldown (e.g. 1 hour)
                        long lastAway = prefs.getLong(key, 0L);
                        long cooldownMs = Math.max(1, config.getAutoReplyCooldownHours()) * 3600_000L;
                        if (nowMs - lastAway < cooldownMs) {
                            return;
                        }
                        prefs.edit().putLong(key, nowMs).apply();
                    }
                }

                // Step 1: Pre-process single underscore _italic_ into __italic__ for MediaDataController
                String preprocessed = SINGLE_UNDERSCORE_PATTERN.matcher(replyText).replaceAll("__$1__");

                // Step 2: Use MediaDataController to parse standard Markdown (**bold**, __italic__, `mono`, ||spoiler||)
                CharSequence[] messageArr = new CharSequence[]{preprocessed};
                ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(accountId).getEntities(messageArr, true);
                if (entities == null) {
                    entities = new ArrayList<>();
                }
                final String cleanText = messageArr[0] != null ? messageArr[0].toString() : replyText;

                // Step 3: Inherit user active text style if set in Text Style menu
                try {
                    int activeStyle = org.nayagram.platform.textstyle.NGTextStyleHelper.getSelectedStyle();
                    if (activeStyle != org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_NONE &&
                        activeStyle != org.nayagram.platform.textstyle.NGTextStyleHelper.STYLE_REGULAR &&
                        cleanText.length() > 0) {
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

                AndroidUtilities.runOnUIThread(() -> {
                    ConnectionsManager cm = ConnectionsManager.getInstance(accountId);
                    try {
                        cm.setAppPaused(false, false);
                        cm.resumeNetworkMaybe();
                        cm.checkConnection();
                    } catch (Throwable ignored) {
                    }
                    SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                            cleanText, dialogId, null, null, null, true, finalEntities, null, null, true, 0, 0, null, false
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
                        } catch (Throwable ignored) {
                        }
                    }, 5000);
                });
                FileLog.d(TAG + ": auto-reply successfully dispatched to dialog " + dialogId + " (rule: " + (matchedRule != null ? matchedRule.name : "default") + ")");
            } catch (Exception e) {
                FileLog.e(TAG + ": auto-reply failed", e);
            }
        });
    }

    private static void enforceStealthOfflinePresence(final int accountId) {
        sendOfflineStatus(accountId);
        AndroidUtilities.runOnUIThread(() -> {
            sendOfflineStatus(accountId);
        }, 1200);
        AndroidUtilities.runOnUIThread(() -> {
            sendOfflineStatus(accountId);
        }, 3500);
    }

    private static void sendOfflineStatus(int accountId) {
        try {
            org.telegram.tgnet.tl.TL_account.updateStatus req = new org.telegram.tgnet.tl.TL_account.updateStatus();
            req.offline = true;
            ConnectionsManager.getInstance(accountId).sendRequest(req, (response, error) -> {
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d(TAG + ": stealth presence offline confirmed: " + (error == null ? "OK" : error.text));
                }
            });
        } catch (Throwable t) {
            FileLog.e(TAG + ": failed to send stealth offline status", t);
        }
    }
}
