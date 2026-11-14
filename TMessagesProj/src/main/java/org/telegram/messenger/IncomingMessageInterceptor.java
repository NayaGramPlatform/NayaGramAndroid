package org.telegram.messenger;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;

import org.nayagram.platform.NayaConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;

/**
 * IncomingMessageInterceptor - Clean Auto Answer dispatcher.
 * - Replies to incoming messages with native Telegram formatting & links.
 * - Works immediately while offline/backgrounded (via FCM Push or background MTProto updates) by using a
 *   reference-counted background job latch and WakeLock until Telegram's server acknowledges the reply.
 * - Strictly preserves the user's offline / last-seen timestamp: never calls setAppPaused(false, false) while
 *   offline or on a secondary account, marks the sendMessage request as background=true, and immediately sends
 *   TL_account.updateStatus(offline=true) as soon as the server processes the auto-reply.
 */
public class IncomingMessageInterceptor {
    private static final String TAG = "IncomingMessageInterceptor";
    private static final Pattern SINGLE_UNDERSCORE_PATTERN = Pattern.compile("(?<!_)_([^\\s_][^_]*?[^\\s_]|[^\\s_])_(?!_)");

    private static final AtomicBoolean autoReplyDispatchOnUiThread = new AtomicBoolean(false);
    private static final ConcurrentHashMap<Integer, Long> keepOfflineUntilByAccount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Integer, Long> lastDispatchedDialogByAccount = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<Long, JobHandle> jobHandlesByDialog = new ConcurrentHashMap<>();

    private static final Object backgroundLock = new Object();
    private static int pendingBackgroundJobs = 0;
    private static CountDownLatch activeBackgroundLatch = null;
    private static PowerManager.WakeLock activeWakeLock = null;

    public static boolean isAutoReplyDispatchActive() {
        return autoReplyDispatchOnUiThread.get();
    }

    public static boolean isKeepOfflineActive(int accountId) {
        Long until = keepOfflineUntilByAccount.get(accountId);
        return until != null && SystemClock.elapsedRealtime() < until;
    }

    public static void markKeepOfflineWindow(int accountId, long durationMs) {
        keepOfflineUntilByAccount.put(accountId, SystemClock.elapsedRealtime() + durationMs);
    }

    /**
     * Called by PushListenerController so FirebaseMessagingService stays alive until
     * all triggered background auto-reply jobs finish transmitting over MTProto.
     */
    public static void awaitBackgroundAutoReply(long timeoutMs) {
        CountDownLatch latch;
        synchronized (backgroundLock) {
            latch = activeBackgroundLatch;
        }
        if (latch == null) {
            return;
        }
        try {
            latch.await(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (Throwable ignored) {}
    }

    public static void onAutoReplyServerResponse(final int accountId, final boolean hasError) {
        onAutoReplyServerResponse(accountId, 0L, hasError);
    }

    /**
     * Called by SendMessagesHelper when a background/auto-reply TL_messages_sendMessage
     * finishes (or exits early/errors). Releases its tracked background job token cleanly.
     */
    public static void onAutoReplyServerResponse(final int accountId, final long dialogId, final boolean hasError) {
        JobHandle trackedHandle = dialogId != 0L ? jobHandlesByDialog.remove(dialogId) : null;
        try {
            if (hasError) {
                long targetDialog = dialogId != 0 ? dialogId : 0L;
                if (targetDialog == 0L) {
                    Long removed = lastDispatchedDialogByAccount.remove(accountId);
                    if (removed != null) {
                        targetDialog = removed;
                    }
                } else {
                    lastDispatchedDialogByAccount.remove(accountId, targetDialog);
                }
                if (targetDialog != 0L) {
                    AutoReplyManager.getInstance().removeAnsweredDialog(targetDialog);
                }
            } else if (dialogId != 0L) {
                lastDispatchedDialogByAccount.remove(accountId, dialogId);
            }
            boolean shouldStayOffline = ApplicationLoader.mainInterfacePaused
                    || !ApplicationLoader.isScreenOn
                    || accountId != UserConfig.selectedAccount
                    || isKeepOfflineActive(accountId)
                    || NayaConfig.getInstance().isStealthOfflinePresenceEnabled()
                    || NayaConfig.getInstance().isAutoReplyStealthMode();

            if (shouldStayOffline) {
                markKeepOfflineWindow(accountId, 20_000L);
                sendOfflineStatusNow(accountId, () -> {
                    if (trackedHandle != null) {
                        trackedHandle.release();
                    }
                });
                AndroidUtilities.runOnUIThread(() -> sendOfflineStatusNow(accountId, null), 1500);
            } else if (trackedHandle != null) {
                trackedHandle.release();
            }
        } catch (Throwable t) {
            if (trackedHandle != null) {
                trackedHandle.release();
            }
        }
    }

    private static final class JobHandle {
        private final AtomicBoolean released = new AtomicBoolean(false);
        private final boolean trackBackground;
        private final long dialogId;

        JobHandle(boolean trackBackground, long dialogId) {
            this.trackBackground = trackBackground;
            this.dialogId = dialogId;
            if (trackBackground) {
                acquireBackgroundJob();
                // Safety guard so a dead socket or aborted send can never hold WakeLock indefinitely
                Utilities.globalQueue.postRunnable(this::release, 12_000L);
            }
        }

        void release() {
            if (dialogId != 0L) {
                jobHandlesByDialog.remove(dialogId, this);
            }
            if (trackBackground && released.compareAndSet(false, true)) {
                releaseBackgroundJob();
            }
        }
    }

    private static void acquireBackgroundJob() {
        synchronized (backgroundLock) {
            if (pendingBackgroundJobs == 0 || activeBackgroundLatch == null || activeBackgroundLatch.getCount() == 0) {
                activeBackgroundLatch = new CountDownLatch(1);
            }
            pendingBackgroundJobs++;
            try {
                if (ApplicationLoader.applicationContext != null) {
                    PowerManager pm = (PowerManager) ApplicationLoader.applicationContext.getSystemService(Context.POWER_SERVICE);
                    if (pm != null) {
                        if (activeWakeLock == null) {
                            activeWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NayaGram:AutoReplyWakeLock");
                            activeWakeLock.setReferenceCounted(false);
                        }
                        if (!activeWakeLock.isHeld()) {
                            activeWakeLock.acquire(15_000L);
                        }
                    }
                }
            } catch (Throwable ignored) {}
        }
    }

    private static void releaseBackgroundJob() {
        synchronized (backgroundLock) {
            if (pendingBackgroundJobs > 0) {
                pendingBackgroundJobs--;
            }
            if (pendingBackgroundJobs == 0) {
                try {
                    if (activeBackgroundLatch != null) {
                        activeBackgroundLatch.countDown();
                        activeBackgroundLatch = null;
                    }
                } catch (Throwable ignored) {}
                try {
                    if (activeWakeLock != null && activeWakeLock.isHeld()) {
                        activeWakeLock.release();
                    }
                } catch (Throwable ignored) {}
            }
        }
    }

    public static void processIncomingMessage(MessageObject messageObject, int accountId) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        final AutoReplyManager autoReplyManager = AutoReplyManager.getInstance();
        final NayaConfig config = NayaConfig.getInstance();
        if (!config.isSmartAutoReply() && !autoReplyManager.isAutoReplyEnabled()) {
            return;
        }
        if (messageObject.isOutOwner() || messageObject.isOut() || messageObject.messageOwner.action != null) {
            return;
        }
        if (messageObject.isReactionPush || messageObject.isStoryPush || messageObject.isStoryReactionPush
                || messageObject.isStoryMentionPush || messageObject.isLiveStoryPush || messageObject.isStoryPushHidden
                || messageObject.isEdited()) {
            return;
        }

        final long dialogId = messageObject.getDialogId();
        final int messageId = messageObject.getId();
        if (dialogId == 0 || (autoReplyManager.isPrivateOnly() && dialogId <= 0)) {
            return;
        }
        if (autoReplyManager.shouldSkipDuplicate(dialogId, messageId)) {
            return;
        }

        final boolean isOfflineOrBackground = ApplicationLoader.mainInterfacePaused
                || !ApplicationLoader.isScreenOn
                || accountId != UserConfig.selectedAccount;

        final JobHandle jobHandle = new JobHandle(isOfflineOrBackground, dialogId);

        Utilities.globalQueue.postRunnable(() -> {
            boolean handedOffToSend = false;
            try {
                if (autoReplyManager.shouldSkipDuplicate(dialogId, messageId)) {
                    return;
                }

                String replyText = autoReplyManager.getAutoAnswerText();
                if (replyText == null || replyText.trim().isEmpty()) {
                    replyText = config.getAutoReplyText();
                }
                if (replyText == null || replyText.trim().isEmpty()) {
                    return;
                }

                String incomingText = messageObject.messageOwner.message;
                if (incomingText != null && incomingText.trim().equalsIgnoreCase(replyText.trim())) {
                    return;
                }

                // Check message freshness (2 hours tolerance for delayed push delivery)
                int now = ConnectionsManager.getInstance(accountId).getCurrentTime();
                if (now <= 0) {
                    now = (int) (System.currentTimeMillis() / 1000L);
                }
                if (messageObject.messageOwner.date > 0 && Math.abs(now - messageObject.messageOwner.date) > 7200) {
                    return;
                }

                if (!PerChatAutoReplySettings.canAutoReplyForChat(dialogId)) {
                    return;
                }

                if (MessagesController.getInstance(accountId).getSendPaidMessagesStars(dialogId) > 0) {
                    return;
                }

                // Ensure peer (User or Chat) is fully loaded so SendMessagesHelper has valid access_hash
                if (dialogId > 0) {
                    if (dialogId == UserConfig.getInstance(accountId).getClientUserId()) {
                        return;
                    }
                    TLRPC.User user = MessagesController.getInstance(accountId).getUser(dialogId);
                    if (user == null || user.min || (user.access_hash == 0 && !user.self && user.fromMessageDialogId == 0)) {
                        try {
                            TLRPC.User dbUser = MessagesStorage.getInstance(accountId).getUserSync(dialogId);
                            if (dbUser != null && !dbUser.min && (dbUser.access_hash != 0 || dbUser.self)) {
                                MessagesController.getInstance(accountId).putUser(dbUser, false);
                                user = dbUser;
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (user == null || user.min || (user.access_hash == 0 && !user.self && user.fromMessageDialogId == 0)) {
                        // Peer not in local DB yet (e.g. brand-new contact via FCM push). Wake network & fetch difference;
                        // do NOT mark dialog answered yet so getDifference() will trigger auto-reply with valid access_hash!
                        ConnectionsManager.onInternalPushReceived(accountId);
                        ConnectionsManager.getInstance(accountId).keepNetworkAliveInBackground();
                        ConnectionsManager.getInstance(accountId).resumeNetworkMaybe();
                        ConnectionsManager.getInstance(accountId).checkConnection();
                        AndroidUtilities.runOnUIThread(() -> {
                            try {
                                MessagesController.getInstance(accountId).getDifference();
                            } catch (Throwable ignored) {}
                        });
                        // Keep the background handle alive briefly while getDifference fetches the message + user,
                        // then release this specific handle after 6.5s so PushListenerController never leaks locks.
                        handedOffToSend = true;
                        Utilities.globalQueue.postRunnable(jobHandle::release, 6_500L);
                        return;
                    }
                    if (user.bot || user.self) {
                        return;
                    }
                } else {
                    TLRPC.Chat chat = MessagesController.getInstance(accountId).getChat(-dialogId);
                    if (chat == null || chat.min) {
                        try {
                            TLRPC.Chat dbChat = MessagesStorage.getInstance(accountId).getChatSync(-dialogId);
                            if (dbChat != null && !dbChat.min) {
                                MessagesController.getInstance(accountId).putChat(dbChat, false);
                                chat = dbChat;
                            }
                        } catch (Throwable ignored) {}
                    }
                    if (chat == null || chat.min) {
                        ConnectionsManager.onInternalPushReceived(accountId);
                        ConnectionsManager.getInstance(accountId).keepNetworkAliveInBackground();
                        ConnectionsManager.getInstance(accountId).resumeNetworkMaybe();
                        ConnectionsManager.getInstance(accountId).checkConnection();
                        AndroidUtilities.runOnUIThread(() -> {
                            try {
                                MessagesController.getInstance(accountId).getDifference();
                            } catch (Throwable ignored) {}
                        });
                        handedOffToSend = true;
                        Utilities.globalQueue.postRunnable(jobHandle::release, 6_500L);
                        return;
                    }
                    if (ChatObject.isChannel(chat) && !chat.megagroup && !ChatObject.canPost(chat)) {
                        return;
                    }
                }

                // Synchronously check and mark message + cooldown window to prevent duplicate sends from push + socket
                synchronized (autoReplyManager) {
                    if (autoReplyManager.shouldSkipDuplicate(dialogId, messageId)) {
                        return;
                    }
                    autoReplyManager.markMessageAnswered(dialogId, messageId);
                }
                lastDispatchedDialogByAccount.put(accountId, dialogId);

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
                final boolean preserveOffline = ApplicationLoader.mainInterfacePaused
                        || !ApplicationLoader.isScreenOn
                        || accountId != UserConfig.selectedAccount
                        || config.isStealthOfflinePresenceEnabled()
                        || config.isAutoReplyStealthMode();

                if (preserveOffline) {
                    markKeepOfflineWindow(accountId, 25_000L);
                }

                if (isOfflineOrBackground) {
                    JobHandle previous = jobHandlesByDialog.put(dialogId, jobHandle);
                    if (previous != null && previous != jobHandle) {
                        previous.release();
                    }
                }
                handedOffToSend = true;
                AndroidUtilities.runOnUIThread(() -> {
                    boolean sentDispatched = false;
                    try {
                        ConnectionsManager cm = ConnectionsManager.getInstance(accountId);
                        ConnectionsManager.onInternalPushReceived(accountId);
                        cm.keepNetworkAliveInBackground();
                        cm.resumeNetworkMaybe();
                        cm.checkConnection();
                        autoReplyManager.ensureBackgroundKeepAlive();

                        HashMap<String, String> sendExtraParams = new HashMap<>(2);
                        sendExtraParams.put("ng_auto_reply", "1");

                        autoReplyDispatchOnUiThread.set(true);
                        try {
                            SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                                    finalCleanText, dialogId, null, null, null, true, finalEntities, null, sendExtraParams, false, 0, 0, null, false
                            );
                            SendMessagesHelper.getInstance(accountId).sendMessage(params);
                            sentDispatched = true;
                        } finally {
                            autoReplyDispatchOnUiThread.set(false);
                        }

                        if (preserveOffline) {
                            sendOfflineStatusNow(accountId, null);
                        }
                    } catch (Throwable t) {
                        FileLog.e(TAG + ": UI dispatch error", t);
                        if (!sentDispatched) {
                            jobHandle.release();
                        }
                    }
                });
                FileLog.d(TAG + ": auto-answer dispatched to dialog " + dialogId + " (preserveOffline=" + preserveOffline + ")");
            } catch (Exception e) {
                FileLog.e(TAG + ": auto-answer failed", e);
            } finally {
                if (!handedOffToSend) {
                    jobHandle.release();
                }
            }
        });
    }

    private static void sendOfflineStatusNow(final int accountId, final Runnable onComplete) {
        try {
            ConnectionsManager cm = ConnectionsManager.getInstance(accountId);
            cm.keepNetworkAliveInBackground();
            org.telegram.tgnet.tl.TL_account.updateStatus req = new org.telegram.tgnet.tl.TL_account.updateStatus();
            req.offline = true;
            cm.sendRequest(req, (response, error) -> {
                try {
                    ConnectionsManager.getInstance(accountId).keepNetworkAliveInBackground();
                } catch (Throwable ignored) {}
                if (BuildVars.LOGS_ENABLED) {
                    FileLog.d(TAG + ": stealth presence offline: " + (error == null ? "OK" : error.text));
                }
                if (onComplete != null) {
                    onComplete.run();
                }
            }, ConnectionsManager.RequestFlagCanCompress | ConnectionsManager.RequestFlagInvokeAfter);
        } catch (Throwable t) {
            FileLog.e(TAG + ": failed to send stealth offline status", t);
            if (onComplete != null) {
                onComplete.run();
            }
        }
    }
}
