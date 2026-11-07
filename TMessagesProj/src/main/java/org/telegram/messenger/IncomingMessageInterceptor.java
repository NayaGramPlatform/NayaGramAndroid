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
 * Rules: switch must be on, private 1:1 chats only, never bots or self,
 * never service messages, never replies to another auto-reply, only fresh
 * messages (not old backlog), and at most one reply per chat per cooldown.
 */
public class IncomingMessageInterceptor {
    private static final String TAG = "IncomingMessageInterceptor";
    private static final String PREFS_NAME = "nayagram_auto_reply_state";

    private static final Pattern URL_PATTERN = Pattern.compile("\[([^\]]+)\]\(([^)]+)\)");
    private static final Pattern SINGLE_UNDERSCORE_PATTERN = Pattern.compile("(?<!_)_([^\s_][^_]*?[^\s_]|[^\s_])_(?!_)");

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

                // Step 1: Pre-process single underscore _italic_ into __italic__ for MediaDataController
                String preprocessed = SINGLE_UNDERSCORE_PATTERN.matcher(replyText).replaceAll("__$1__");

                // Step 2: Use MediaDataController to parse standard Markdown (**bold**, __italic__, `mono`, ||spoiler||)
                CharSequence[] messageArr = new CharSequence[]{preprocessed};
                ArrayList<TLRPC.MessageEntity> entities = MediaDataController.getInstance(accountId).getEntities(messageArr, true);
                if (entities == null) {
                    entities = new ArrayList<>();
                }
                String currentText = messageArr[0] != null ? messageArr[0].toString() : preprocessed;

                // Step 3: Parse quote lines starting with "> " -> TL_messageEntityBlockquote
                String[] lines = currentText.split("
", -1);
                StringBuilder newTextBuilder = new StringBuilder();
                int lineStart = 0;
                for (int i = 0; i < lines.length; i++) {
                    String line = lines[i];
                    if (line.startsWith("> ")) {
                        String quoteContent = line.substring(2);
                        newTextBuilder.append(quoteContent);
                        TLRPC.TL_messageEntityBlockquote quoteEntity = new TLRPC.TL_messageEntityBlockquote();
                        quoteEntity.offset = lineStart;
                        quoteEntity.length = quoteContent.length();
                        entities.add(quoteEntity);
                        for (int e = 0; e < entities.size() - 1; e++) {
                            TLRPC.MessageEntity existing = entities.get(e);
                            if (existing.offset >= lineStart + 2) {
                                existing.offset -= 2;
                            }
                        }
                        lineStart += quoteContent.length();
                    } else {
                        newTextBuilder.append(line);
                        lineStart += line.length();
                    }
                    if (i < lines.length - 1) {
                        newTextBuilder.append("
");
                        lineStart += 1;
                    }
                }
                currentText = newTextBuilder.toString();

                // Step 4: Parse [label](url) -> TL_messageEntityTextUrl
                Matcher urlMatcher = URL_PATTERN.matcher(currentText);
                while (urlMatcher.find()) {
                    String label = urlMatcher.group(1);
                    String url = urlMatcher.group(2);
                    int start = urlMatcher.start();
                    int end = urlMatcher.end();
                    int fullLen = end - start;
                    int removed = fullLen - label.length();

                    currentText = currentText.substring(0, start) + label + currentText.substring(end);

                    for (int e = 0; e < entities.size(); e++) {
                        TLRPC.MessageEntity existing = entities.get(e);
                        if (existing.offset >= end) {
                            existing.offset -= removed;
                        }
                    }

                    TLRPC.TL_messageEntityTextUrl urlEntity = new TLRPC.TL_messageEntityTextUrl();
                    urlEntity.offset = start;
                    urlEntity.length = label.length();
                    urlEntity.url = url;
                    entities.add(urlEntity);

                    urlMatcher = URL_PATTERN.matcher(currentText);
                }

                final String cleanText = currentText;
                final ArrayList<TLRPC.MessageEntity> finalEntities = entities;

                AndroidUtilities.runOnUIThread(() ->
                        SendMessagesHelper.getInstance(accountId).sendMessage(
                                SendMessagesHelper.SendMessageParams.of(
                                        cleanText,
                                        dialogId,
                                        null,
                                        null,
                                        null,
                                        true,
                                        finalEntities,
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
