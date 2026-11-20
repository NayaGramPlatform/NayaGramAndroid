package org.telegram.messenger;

import android.content.Context;
import android.os.PowerManager;
import android.os.SystemClock;
import android.text.TextUtils;

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
 * IncomingMessageInterceptor - Private 1-on-1 Inbox Auto Answer dispatcher.
 * - Strictly restricted to direct 1-on-1 private human chats ONLY (groups, supergroups, channels,
 *   discussion threads, bots, support, and official service accounts are permanently blocked).
 * - Supports rich formatted text, hyperlinks, and attached media (Photo, Video, Voice Note, Audio, File).
 * - Works reliably online and offline/backgrounded (via FCM Push or background MTProto updates) using
 *   a reference-counted background job latch and WakeLock until Telegram acknowledges the reply.
 * - Preserves the user's offline / last-seen timestamp via background=true and TL_account.updateStatus(offline=true).
 */
public class IncomingMessageInterceptor {

    private static final String TAG = "IncomingMessageInterceptor";
    private static final Pattern SINGLE_UNDERSCORE_PATTERN = Pattern.compile("(?<!_)_([^\\s_][^_]*?[^\\s_]|[^\\s_])_(?!_)");
    private static final int TELEGRAM_MAX_CAPTION_LENGTH = 1024;

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
     * Called by SendMessagesHelper when a background/auto-reply TL_messages_sendMessage or
     * TL_messages_sendMedia finishes (or exits early/errors). Releases its tracked background job token cleanly.
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

        JobHandle(boolean trackBackground, long dialogId, long safetyTimeoutMs) {
            this.trackBackground = trackBackground;
            this.dialogId = dialogId;
            if (trackBackground) {
                acquireBackgroundJob(safetyTimeoutMs);
                // Safety guard so a dead socket or aborted send can never hold WakeLock indefinitely
                Utilities.globalQueue.postRunnable(this::release, safetyTimeoutMs);
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

    private static void acquireBackgroundJob(long timeoutMs) {
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
                            activeWakeLock.acquire(Math.max(15_000L, timeoutMs));
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

    /**
     * Triggered directly from PushListenerController when an FCM push arrives while offline/closed.
     * Guarantees the push process holds a WakeLock and JobHandle until the reply is dispatched & ACKed.
     */
    public static void processIncomingPushMessage(MessageObject messageObject, int accountId) {
        processIncomingMessageInternal(messageObject, accountId, true);
    }

    /**
     * Triggered from PushListenerController when a private 1-on-1 push arrives without readable text
     * (e.g. locked/hidden push preview or muted message) so MTProto wakes up and fetches difference
     * while holding a background JobHandle.
     */
    public static void triggerOfflineDifferenceSync(int accountId, long dialogId) {
        try {
            final AutoReplyManager autoReplyManager = AutoReplyManager.getInstance();
            final NayaConfig config = NayaConfig.getInstance();
            if (!config.isSmartAutoReply() && !autoReplyManager.isAutoReplyEnabled()) {
                return;
            }
            if (!AutoReplyManager.isEligiblePrivateUserDialog(dialogId, accountId)) {
                return;
            }
            if (autoReplyManager.hasAnsweredDialog(dialogId)) {
                return;
            }
            final long timeoutMs = autoReplyManager.hasValidMedia() ? 38_000L : 20_000L;
            final JobHandle jobHandle = new JobHandle(true, dialogId, timeoutMs);
            JobHandle prev = jobHandlesByDialog.put(dialogId, jobHandle);
            if (prev != null && prev != jobHandle) {
                prev.release();
            }
            markKeepOfflineWindow(accountId, timeoutMs);
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    ConnectionsManager cm = ConnectionsManager.getInstance(accountId);
                    ConnectionsManager.onInternalPushReceived(accountId);
                    cm.keepNetworkAliveInBackground();
                    cm.resumeNetworkMaybe();
                    cm.checkConnection();
                    autoReplyManager.ensureBackgroundKeepAlive();
                    MessagesController.getInstance(accountId).getDifference();
                } catch (Throwable t) {
                    FileLog.e(TAG + ": offline difference sync failed", t);
                    jobHandle.release();
                }
            });
        } catch (Throwable t) {
            FileLog.e(TAG + ": triggerOfflineDifferenceSync error", t);
        }
    }

    public static void processIncomingMessage(MessageObject messageObject, int accountId) {
        processIncomingMessageInternal(messageObject, accountId, false);
    }

    private static void processIncomingMessageInternal(MessageObject messageObject, int accountId, boolean fromFcmPush) {
        if (messageObject == null || messageObject.messageOwner == null) {
            return;
        }
        final AutoReplyManager autoReplyManager = AutoReplyManager.getInstance();
        final NayaConfig config = NayaConfig.getInstance();
        if (!config.isSmartAutoReply() && !autoReplyManager.isAutoReplyEnabled()) {
            return;
        }
        final TLRPC.Message owner = messageObject.messageOwner;
        if (messageObject.isOutOwner() || messageObject.isOut() || owner.action != null
                || owner.post || owner.from_scheduled) {
            return;
        }
        if (messageObject.isReactionPush || messageObject.isStoryPush || messageObject.isStoryReactionPush
                || messageObject.isStoryMentionPush || messageObject.isLiveStoryPush || messageObject.isStoryPushHidden
                || messageObject.isEdited()) {
            return;
        }

        // HARD 1-ON-1 PRIVATE HUMAN INBOX CHECK:
        // Reject all groups, supergroups, broadcast channels, discussion threads, bots & system accounts.
        if (owner.peer_id != null && !(owner.peer_id instanceof TLRPC.TL_peerUser)) {
            return;
        }
        if (owner.from_id != null && !(owner.from_id instanceof TLRPC.TL_peerUser)) {
            return;
        }
        final long dialogId = messageObject.getDialogId();
        final int messageId = messageObject.getId();
        if (!AutoReplyManager.isEligiblePrivateUserDialog(dialogId, accountId)) {
            return;
        }
        if (autoReplyManager.shouldSkipDuplicate(dialogId, messageId)) {
            return;
        }

        final boolean hasValidMediaAtCheck = autoReplyManager.hasValidMedia();
        final boolean isOfflineOrBackground = fromFcmPush
                || ApplicationLoader.mainInterfacePaused
                || !ApplicationLoader.isScreenOn
                || accountId != UserConfig.selectedAccount;
        final long safetyTimeoutMs = hasValidMediaAtCheck ? 42_000L : 20_000L;
        final JobHandle jobHandle = new JobHandle(isOfflineOrBackground, dialogId, safetyTimeoutMs);

        Utilities.globalQueue.postRunnable(() -> {
            boolean handedOffToSend = false;
            try {
                if (!AutoReplyManager.isEligiblePrivateUserDialog(dialogId, accountId)) {
                    return;
                }
                if (autoReplyManager.shouldSkipDuplicate(dialogId, messageId)) {
                    return;
                }

                final boolean hasMedia = autoReplyManager.hasValidMedia();
                final int mediaType = hasMedia ? autoReplyManager.getMediaType() : AutoReplyManager.MEDIA_TYPE_NONE;
                final String mediaPath = hasMedia ? autoReplyManager.getMediaPath() : "";
                final String mediaMime = hasMedia ? autoReplyManager.getMediaMime() : "";

                String replyText = hasMedia
                        ? autoReplyManager.getRawAutoAnswerText()
                        : autoReplyManager.getAutoAnswerText();
                if (!hasMedia && (replyText == null || replyText.trim().isEmpty())) {
                    replyText = config.getAutoReplyText();
                }
                final boolean hasText = replyText != null && !replyText.trim().isEmpty();
                if (!hasText && !hasMedia) {
                    return;
                }

                String incomingText = owner.message;
                if (!hasMedia && hasText && incomingText != null && incomingText.trim().equalsIgnoreCase(replyText.trim())) {
                    return;
                }

                // Check message freshness (6 hours tolerance for offline/reconnect & delayed push delivery)
                int now = ConnectionsManager.getInstance(accountId).getCurrentTime();
                if (now <= 0) {
                    now = (int) (System.currentTimeMillis() / 1000L);
                }
                if (owner.date > 0 && Math.abs(now - owner.date) > 21600) {
                    return;
                }
                if (!PerChatAutoReplySettings.canAutoReplyForChat(dialogId)) {
                    return;
                }
                if (MessagesController.getInstance(accountId).getSendPaidMessagesStars(dialogId) > 0) {
                    return;
                }

                // Resolve private 1-on-1 human user peer so SendMessagesHelper has a valid access_hash
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
                    // Brand-new private human contact via FCM push: wake network & fetch difference;
                    // do NOT mark dialog answered yet so getDifference() triggers auto-reply with valid access_hash.
                    ConnectionsManager.onInternalPushReceived(accountId);
                    ConnectionsManager.getInstance(accountId).keepNetworkAliveInBackground();
                    ConnectionsManager.getInstance(accountId).resumeNetworkMaybe();
                    ConnectionsManager.getInstance(accountId).checkConnection();
                    autoReplyManager.ensureBackgroundKeepAlive();
                    AndroidUtilities.runOnUIThread(() -> {
                        try {
                            MessagesController.getInstance(accountId).getDifference();
                        } catch (Throwable ignored) {}
                    });
                    handedOffToSend = true;
                    Utilities.globalQueue.postRunnable(jobHandle::release, 14_000L);
                    return;
                }
                if (user.bot || user.self || user.support || user.deleted) {
                    return;
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
                String cleanText = hasText ? replyText : "";
                if (hasText) {
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
                }

                final ArrayList<TLRPC.MessageEntity> finalEntities = entities;
                final String finalCleanText = cleanText;
                final boolean preserveOffline = fromFcmPush
                        || ApplicationLoader.mainInterfacePaused
                        || !ApplicationLoader.isScreenOn
                        || accountId != UserConfig.selectedAccount
                        || config.isStealthOfflinePresenceEnabled()
                        || config.isAutoReplyStealthMode();

                if (preserveOffline) {
                    markKeepOfflineWindow(accountId, hasMedia ? 45_000L : 25_000L);
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
                            if (hasMedia && !TextUtils.isEmpty(mediaPath)) {
                                dispatchMediaAutoReply(
                                        accountId,
                                        dialogId,
                                        mediaType,
                                        mediaPath,
                                        mediaMime,
                                        finalCleanText,
                                        finalEntities,
                                        sendExtraParams
                                );
                            } else {
                                SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                                        finalCleanText, dialogId, null, null, null, true, finalEntities, null, sendExtraParams, false, 0, 0, null, false
                                );
                                SendMessagesHelper.getInstance(accountId).sendMessage(params);
                            }
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
                FileLog.d(TAG + ": private inbox auto-answer dispatched to user " + dialogId
                        + " (mediaType=" + mediaType + ", preserveOffline=" + preserveOffline + ")");
            } catch (Exception e) {
                FileLog.e(TAG + ": auto-answer failed", e);
            } finally {
                if (!handedOffToSend) {
                    jobHandle.release();
                }
            }
        });
    }

    private static void dispatchMediaAutoReply(
            int accountId,
            long dialogId,
            int mediaType,
            String mediaPath,
            String mediaMime,
            String text,
            ArrayList<TLRPC.MessageEntity> entities,
            HashMap<String, String> extraParams
    ) {
        AccountInstance accountInstance = AccountInstance.getInstance(accountId);
        boolean hasText = !TextUtils.isEmpty(text) && !text.trim().isEmpty();
        boolean longText = hasText && text.length() > TELEGRAM_MAX_CAPTION_LENGTH;
        String caption = (hasText && !longText) ? text : "";
        ArrayList<TLRPC.MessageEntity> captionEntities = caption.isEmpty() ? null : entities;

        if (mediaType == AutoReplyManager.MEDIA_TYPE_PHOTO || mediaType == AutoReplyManager.MEDIA_TYPE_VIDEO) {
            SendMessagesHelper.SendingMediaInfo info = new SendMessagesHelper.SendingMediaInfo();
            info.path = mediaPath;
            info.isVideo = (mediaType == AutoReplyManager.MEDIA_TYPE_VIDEO);
            info.caption = caption.isEmpty() ? null : caption;
            info.entities = captionEntities;
            info.params = new HashMap<>(extraParams);
            ArrayList<SendMessagesHelper.SendingMediaInfo> mediaList = new ArrayList<>(1);
            mediaList.add(info);
            SendMessagesHelper.prepareSendingMedia(
                    accountInstance,
                    mediaList,
                    dialogId,
                    null,
                    null,
                    null,
                    null,
                    false,
                    false,
                    null,
                    false,
                    0,
                    0,
                    0,
                    false,
                    null,
                    null,
                    0,
                    false,
                    0,
                    0,
                    null
            );
        } else {
            // Voice notes (.ogg Opus auto-detected as native Telegram voice message),
            // audio tracks (.m4a/.mp3), and files/documents
            ArrayList<String> paths = new ArrayList<>(1);
            paths.add(mediaPath);
            ArrayList<String> originalPaths = new ArrayList<>(1);
            originalPaths.add(mediaPath);
            SendMessagesHelper.prepareSendingDocuments(
                    accountInstance,
                    paths,
                    originalPaths,
                    null,
                    caption.isEmpty() ? null : caption,
                    captionEntities,
                    TextUtils.isEmpty(mediaMime) ? null : mediaMime,
                    dialogId,
                    null,
                    null,
                    null,
                    null,
                    null,
                    false,
                    0,
                    0,
                    null,
                    null,
                    0,
                    false,
                    0,
                    0,
                    null
            );
        }

        // If message text exceeds Telegram's 1024-char caption limit, also send full formatted text right after
        if (longText) {
            SendMessagesHelper.SendMessageParams textParams = SendMessagesHelper.SendMessageParams.of(
                    text, dialogId, null, null, null, true, entities, null, extraParams, false, 0, 0, null, false
            );
            SendMessagesHelper.getInstance(accountId).sendMessage(textParams);
        }
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
