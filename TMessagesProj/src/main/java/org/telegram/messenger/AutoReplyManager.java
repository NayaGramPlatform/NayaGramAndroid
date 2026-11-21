package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.PowerManager;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.Spannable;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.URLSpan;
import android.text.style.UnderlineSpan;
import android.webkit.MimeTypeMap;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.nayagram.platform.NayaConfig;
import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;
import org.telegram.ui.Components.QuoteSpan;
import org.telegram.ui.Components.TextStyleSpan;
import org.telegram.ui.Components.URLSpanReplacement;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * AutoReplyManager - Private 1-on-1 Inbox Auto Answer engine with rich formatting & media support.
 * - Strictly restricted to 1-on-1 private human chats ONLY (groups, supergroups, channels, bots,
 *   and system accounts are permanently excluded).
 * - Supports text, formatted entities, clickable links, and persistent offline-ready media
 *   (Photo, Video, Voice Note, Audio, File/Document).
 * - 4.5s anti-duplicate / anti-loop guard so FCM push + MTProto socket never double-send.
 * - Background MTProto socket keep-alive heartbeat so Auto-Reply works offline/minimized.
 */
public class AutoReplyManager {

    private static AutoReplyManager instance;

    private static final String PREFS_NAME = "auto_reply_prefs";
    private static final String KEY_ENABLED = "auto_reply_enabled";
    private static final String KEY_TEXT = "auto_answer_text";
    private static final String KEY_ENTITIES = "auto_answer_entities";
    private static final String KEY_PRIVATE_ONLY = "auto_reply_private_only";
    private static final String KEY_ANSWERED_DIALOGS = "answered_dialogs";
    private static final String KEY_LAST_ANSWERED_MSG_IDS = "last_answered_msg_ids";
    private static final String KEY_MEDIA_TYPE = "auto_answer_media_type";
    private static final String KEY_MEDIA_PATH = "auto_answer_media_path";
    private static final String KEY_MEDIA_NAME = "auto_answer_media_name";
    private static final String KEY_MEDIA_MIME = "auto_answer_media_mime";
    private static final String KEY_MEDIA_DURATION = "auto_answer_media_duration";

    public static final int MEDIA_TYPE_NONE = 0;
    public static final int MEDIA_TYPE_PHOTO = 1;
    public static final int MEDIA_TYPE_VIDEO = 2;
    public static final int MEDIA_TYPE_VOICE = 3;
    public static final int MEDIA_TYPE_AUDIO = 4;
    public static final int MEDIA_TYPE_DOCUMENT = 5;

    private static final int MAX_PERSISTED_MSG_IDS = 100;
    private static final long DEDUP_COOLDOWN_MS = 4500L;
    private static final long HEARTBEAT_INTERVAL_MS = 15_000L;
    private static final long MAX_MEDIA_COPY_BYTES = 100L * 1024L * 1024L;

    // Official Telegram system, service, anonymous group & channel-bot user IDs
    private static final long TELEGRAM_SERVICE_ID = 777000L;
    private static final long TELEGRAM_SUPPORT_ID = 42777L;
    private static final long TELEGRAM_REPLIES_ID = 1271266957L;
    private static final long TELEGRAM_ANON_BOT_ID = 1087968824L;
    private static final long TELEGRAM_CHANNEL_BOT_ID = 136817688L;
    private static final long TELEGRAM_VERIFY_ID = 489000L;
    private static final long TELEGRAM_SPAM_SERVICE_ID = 333000L;
    private static final long TELEGRAM_NOTIF_ID = 429000L;

    private static final String DEFAULT_AUTO_ANSWER_TEXT = "আসসালামু আলাইকুম! 🥰\nআমি এখন ব্যস্ত আছি, একটু পরে রিপ্লাই দিচ্ছি ইনশাআল্লাহ।";

    private final SharedPreferences preferences;
    private boolean autoReplyEnabled;
    private String autoAnswerText = "";
    private String autoAnswerEntitiesJson = null;
    private int mediaType = MEDIA_TYPE_NONE;
    private String mediaPath = "";
    private String mediaName = "";
    private String mediaMime = "";
    private int mediaDuration = 0;
    private final Set<Long> answeredDialogs = new HashSet<>();
    private final Map<Long, Long> lastReplyTimestampByDialog = new HashMap<>();
    private final Map<Long, Integer> lastAnsweredMsgIdByDialog = new HashMap<>();
    private PowerManager.WakeLock backgroundWakeLock;
    private WifiManager.WifiLock backgroundWifiLock;
    private boolean heartbeatScheduled = false;

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (AutoReplyManager.this) {
                heartbeatScheduled = false;
            }
            if (!isAutoReplyEnabled()) {
                releaseBackgroundLocks();
                return;
            }
            try {
                boolean isBackgrounded = ApplicationLoader.mainInterfacePaused || !ApplicationLoader.isScreenOn;
                if (isBackgrounded) {
                    acquireBackgroundLocks();
                } else {
                    releaseBackgroundLocks();
                }
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    try {
                        if (UserConfig.getInstance(a).isClientActivated()) {
                            ConnectionsManager cm = ConnectionsManager.getInstance(a);
                            cm.keepNetworkAliveInBackground();
                            cm.checkConnection();
                        }
                    } catch (Throwable ignored) {}
                }
            } catch (Throwable ignored) {}
            scheduleNextHeartbeat();
        }
    };

    private AutoReplyManager() {
        preferences = ApplicationLoader.applicationContext.getSharedPreferences(
                PREFS_NAME, Context.MODE_PRIVATE);
        load();
        if (isAutoReplyEnabled()) {
            AndroidUtilities.runOnUIThread(() -> {
                ensureBackgroundKeepAlive();
                AutoReplySyncWorker.schedule(ApplicationLoader.applicationContext);
                ApplicationLoader.startPushService();
            }, 1500);
        }
    }

    public static synchronized AutoReplyManager getInstance() {
        if (instance == null) {
            instance = new AutoReplyManager();
        }
        return instance;
    }

    /**
     * Hard guardrail: Auto-Reply is ONLY allowed for direct 1-on-1 human user dialogs.
     * Groups, supergroups, broadcast channels, secret chats, self, and Telegram system/bot IDs
     * are permanently rejected.
     */
    public static boolean isEligiblePrivateUserDialog(long dialogId, int accountId) {
        if (dialogId <= 0L || !DialogObject.isUserDialog(dialogId)) {
            return false;
        }
        if (dialogId == TELEGRAM_SERVICE_ID
                || dialogId == TELEGRAM_SUPPORT_ID
                || dialogId == TELEGRAM_REPLIES_ID
                || dialogId == TELEGRAM_ANON_BOT_ID
                || dialogId == TELEGRAM_CHANNEL_BOT_ID
                || dialogId == TELEGRAM_VERIFY_ID
                || dialogId == TELEGRAM_SPAM_SERVICE_ID
                || dialogId == TELEGRAM_NOTIF_ID) {
            return false;
        }
        try {
            long selfId = UserConfig.getInstance(accountId).getClientUserId();
            if (selfId != 0L && dialogId == selfId) {
                return false;
            }
        } catch (Throwable ignored) {}
        return true;
    }

    public synchronized void ensureBackgroundKeepAlive() {
        if (!isAutoReplyEnabled()) {
            releaseBackgroundLocks();
            return;
        }
        if (ApplicationLoader.mainInterfacePaused || !ApplicationLoader.isScreenOn) {
            acquireBackgroundLocks();
        }
        scheduleNextHeartbeat();
    }

    private synchronized void scheduleNextHeartbeat() {
        if (heartbeatScheduled || !isAutoReplyEnabled()) {
            return;
        }
        heartbeatScheduled = true;
        Utilities.globalQueue.postRunnable(heartbeatRunnable, HEARTBEAT_INTERVAL_MS);
    }

    private synchronized void acquireBackgroundLocks() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) return;
            if (backgroundWakeLock == null) {
                PowerManager pm = (PowerManager) ctx.getSystemService(Context.POWER_SERVICE);
                if (pm != null) {
                    backgroundWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NayaGram:AutoReplyKeepAlive");
                    backgroundWakeLock.setReferenceCounted(false);
                }
            }
            if (backgroundWakeLock != null && !backgroundWakeLock.isHeld()) {
                backgroundWakeLock.acquire(20_000L);
            }
            if (backgroundWifiLock == null) {
                WifiManager wm = (WifiManager) ctx.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                if (wm != null) {
                    backgroundWifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "NayaGram:AutoReplyWifiLock");
                    backgroundWifiLock.setReferenceCounted(false);
                }
            }
            if (backgroundWifiLock != null && !backgroundWifiLock.isHeld()) {
                backgroundWifiLock.acquire();
            }
        } catch (Throwable ignored) {}
    }

    private synchronized void releaseBackgroundLocks() {
        try {
            if (backgroundWakeLock != null && backgroundWakeLock.isHeld()) {
                backgroundWakeLock.release();
            }
        } catch (Throwable ignored) {}
        try {
            if (backgroundWifiLock != null && backgroundWifiLock.isHeld()) {
                backgroundWifiLock.release();
            }
        } catch (Throwable ignored) {}
    }

    public synchronized void setAutoReplyEnabled(boolean enabled) {
        autoReplyEnabled = enabled;
        answeredDialogs.clear();
        lastReplyTimestampByDialog.clear();
        lastAnsweredMsgIdByDialog.clear();
        preferences.edit()
                .putBoolean(KEY_ENABLED, enabled)
                .putBoolean(KEY_PRIVATE_ONLY, true)
                .remove(KEY_ANSWERED_DIALOGS)
                .remove(KEY_LAST_ANSWERED_MSG_IDS)
                .apply();
        try {
            NayaConfig.getInstance().setSmartAutoReply(enabled);
        } catch (Throwable ignored) {}
        if (enabled) {
            ensureBackgroundKeepAlive();
            try {
                AutoReplySyncWorker.schedule(ApplicationLoader.applicationContext);
                ApplicationLoader.startPushService();
            } catch (Throwable ignored) {}
        } else {
            releaseBackgroundLocks();
            try {
                AutoReplySyncWorker.cancel(ApplicationLoader.applicationContext);
                ApplicationLoader.startPushService();
            } catch (Throwable ignored) {}
        }
    }

    public synchronized boolean isAutoReplyEnabled() {
        try {
            return autoReplyEnabled || NayaConfig.getInstance().isSmartAutoReply();
        } catch (Throwable e) {
            return autoReplyEnabled;
        }
    }

    public synchronized String getAutoAnswerText() {
        if (autoAnswerText != null && !autoAnswerText.trim().isEmpty()) {
            return autoAnswerText;
        }
        if (hasValidMedia()) {
            return "";
        }
        try {
            String fallback = NayaConfig.getInstance().getAutoReplyText();
            if (fallback != null && !fallback.trim().isEmpty()) {
                return fallback;
            }
        } catch (Throwable ignored) {}
        return DEFAULT_AUTO_ANSWER_TEXT;
    }

    public synchronized String getRawAutoAnswerText() {
        return autoAnswerText != null ? autoAnswerText : "";
    }

    public synchronized String getCustomReplyMessage() {
        return getAutoAnswerText();
    }

    public synchronized void setCustomReplyMessage(String text) {
        setAutoAnswerText(text, autoAnswerEntitiesJson);
    }

    public synchronized void setAutoAnswerText(String text) {
        setAutoAnswerText(text, null);
    }

    public synchronized void setAutoAnswerText(String text, String entitiesJson) {
        autoAnswerText = text != null ? text : "";
        autoAnswerEntitiesJson = entitiesJson;
        preferences.edit()
                .putString(KEY_TEXT, autoAnswerText)
                .putString(KEY_ENTITIES, autoAnswerEntitiesJson)
                .apply();
        try {
            NayaConfig.getInstance().setAutoReplyText(autoAnswerText);
        } catch (Throwable ignored) {}
    }

    public synchronized String getAutoAnswerEntitiesJson() {
        return autoAnswerEntitiesJson;
    }

    /**
     * Always true: NayaGram Auto-Reply is permanently locked to 1-on-1 private human chats only.
     */
    public synchronized boolean isPrivateOnly() {
        return true;
    }

    public synchronized void setPrivateOnly(boolean val) {
        preferences.edit().putBoolean(KEY_PRIVATE_ONLY, true).apply();
    }

    public synchronized int getMediaType() {
        return hasValidMedia() ? mediaType : MEDIA_TYPE_NONE;
    }

    public synchronized String getMediaPath() {
        return hasValidMedia() ? mediaPath : "";
    }

    public synchronized String getMediaName() {
        return mediaName != null ? mediaName : "";
    }

    public synchronized String getMediaMime() {
        return mediaMime != null ? mediaMime : "";
    }

    public synchronized int getMediaDuration() {
        return Math.max(0, mediaDuration);
    }

    public synchronized boolean hasValidMedia() {
        if (mediaType == MEDIA_TYPE_NONE || TextUtils.isEmpty(mediaPath)) {
            return false;
        }
        try {
            File f = new File(mediaPath);
            return f.exists() && f.isFile() && f.length() > 0;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String getMediaTypeLabel(int type) {
        switch (type) {
            case MEDIA_TYPE_PHOTO: return "Photo";
            case MEDIA_TYPE_VIDEO: return "Video";
            case MEDIA_TYPE_VOICE: return "Voice Note";
            case MEDIA_TYPE_AUDIO: return "Audio";
            case MEDIA_TYPE_DOCUMENT: return "File";
            default: return "";
        }
    }

    public synchronized void setAutoAnswerMedia(int type, String path, String name, String mime, int durationSec) {
        String oldPath = this.mediaPath;
        this.mediaType = type;
        this.mediaPath = path != null ? path : "";
        this.mediaName = name != null ? name : "";
        this.mediaMime = mime != null ? mime : "";
        this.mediaDuration = Math.max(0, durationSec);
        preferences.edit()
                .putInt(KEY_MEDIA_TYPE, this.mediaType)
                .putString(KEY_MEDIA_PATH, this.mediaPath)
                .putString(KEY_MEDIA_NAME, this.mediaName)
                .putString(KEY_MEDIA_MIME, this.mediaMime)
                .putInt(KEY_MEDIA_DURATION, this.mediaDuration)
                .apply();
        if (!TextUtils.isEmpty(oldPath) && !oldPath.equals(this.mediaPath)) {
            deleteManagedMediaFileQuietly(oldPath);
        }
    }

    public synchronized void clearAutoAnswerMedia() {
        String oldPath = this.mediaPath;
        this.mediaType = MEDIA_TYPE_NONE;
        this.mediaPath = "";
        this.mediaName = "";
        this.mediaMime = "";
        this.mediaDuration = 0;
        preferences.edit()
                .remove(KEY_MEDIA_TYPE)
                .remove(KEY_MEDIA_PATH)
                .remove(KEY_MEDIA_NAME)
                .remove(KEY_MEDIA_MIME)
                .remove(KEY_MEDIA_DURATION)
                .apply();
        if (!TextUtils.isEmpty(oldPath)) {
            deleteManagedMediaFileQuietly(oldPath);
        }
    }

    private void deleteManagedMediaFileQuietly(String path) {
        try {
            File f = new File(path);
            File dir = getMediaStorageDir(ApplicationLoader.applicationContext);
            if (dir != null && f.exists() && f.getAbsolutePath().startsWith(dir.getAbsolutePath())) {
                //noinspection ResultOfMethodCallIgnored
                f.delete();
            }
        } catch (Throwable ignored) {}
    }

    /**
     * Returns a persistent external app-owned directory outside /data/data/<pkg>
     * so SendMessagesHelper / AndroidUtilities.isInternalUri() never rejects it
     * and offline background auto-replies can always read the attached media.
     */
    public static File getMediaStorageDir(Context context) {
        try {
            File filesDir = FileLoader.getDirectory(FileLoader.MEDIA_DIR_FILES);
            if (filesDir != null) {
                File dir = new File(filesDir, "ng_auto_reply");
                if (dir.exists() || dir.mkdirs()) {
                    return dir;
                }
            }
        } catch (Throwable ignored) {}
        try {
            Context ctx = context != null ? context : ApplicationLoader.applicationContext;
            if (ctx != null) {
                File ext = ctx.getExternalFilesDir("ng_auto_reply");
                if (ext != null && (ext.exists() || ext.mkdirs())) {
                    return ext;
                }
            }
        } catch (Throwable ignored) {}
        return null;
    }

    public static class CopiedMediaResult {
        public final String path;
        public final String displayName;
        public final String mimeType;
        public final long sizeBytes;

        public CopiedMediaResult(String path, String displayName, String mimeType, long sizeBytes) {
            this.path = path;
            this.displayName = displayName;
            this.mimeType = mimeType;
            this.sizeBytes = sizeBytes;
        }
    }

    /**
     * Copies a user-picked content:// Uri into persistent external app storage so it stays
     * accessible offline, in background FCM wakeups, and across phone restarts.
     */
    public static CopiedMediaResult copyUriToMediaStorage(Context context, Uri uri, String fallbackExt) {
        if (context == null || uri == null) return null;
        File dir = getMediaStorageDir(context);
        if (dir == null) return null;

        String displayName = null;
        String mime = null;
        try {
            mime = context.getContentResolver().getType(uri);
        } catch (Throwable ignored) {}

        try (Cursor cursor = context.getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) {
                    displayName = cursor.getString(idx);
                }
            }
        } catch (Throwable ignored) {}

        String ext = "";
        if (!TextUtils.isEmpty(displayName)) {
            int dot = displayName.lastIndexOf('.');
            if (dot >= 0 && dot + 1 < displayName.length()) {
                ext = displayName.substring(dot + 1).replaceAll("[^a-zA-Z0-9]", "").toLowerCase(Locale.ROOT);
            }
        }
        if (ext.isEmpty() && !TextUtils.isEmpty(mime)) {
            String fromMime = MimeTypeMap.getSingleton().getExtensionFromMimeType(mime);
            if (!TextUtils.isEmpty(fromMime)) {
                ext = fromMime.toLowerCase(Locale.ROOT);
            }
        }
        if (ext.isEmpty()) {
            ext = !TextUtils.isEmpty(fallbackExt) ? fallbackExt : "bin";
        }
        if (TextUtils.isEmpty(displayName)) {
            displayName = "auto_reply_" + System.currentTimeMillis() + "." + ext;
        }

        String safeBase = displayName.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (safeBase.length() > 64) {
            safeBase = safeBase.substring(safeBase.length() - 64);
        }
        if (!safeBase.toLowerCase(Locale.ROOT).endsWith("." + ext)) {
            safeBase = safeBase + "." + ext;
        }
        File outFile = new File(dir, "ar_" + System.currentTimeMillis() + "_" + safeBase);

        long total = 0;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             FileOutputStream out = new FileOutputStream(outFile)) {
            if (in == null) return null;
            byte[] buf = new byte[16384];
            int n;
            while ((n = in.read(buf)) != -1) {
                total += n;
                if (total > MAX_MEDIA_COPY_BYTES) {
                    //noinspection ResultOfMethodCallIgnored
                    outFile.delete();
                    return null;
                }
                out.write(buf, 0, n);
            }
            out.flush();
        } catch (Throwable t) {
            FileLog.e("AutoReplyManager: failed to copy media uri", t);
            try {
                //noinspection ResultOfMethodCallIgnored
                outFile.delete();
            } catch (Throwable ignored) {}
            return null;
        }
        if (total <= 0) {
            try {
                //noinspection ResultOfMethodCallIgnored
                outFile.delete();
            } catch (Throwable ignored) {}
            return null;
        }
        if (TextUtils.isEmpty(mime)) {
            String guessed = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
            mime = guessed != null ? guessed : "application/octet-stream";
        }
        return new CopiedMediaResult(outFile.getAbsolutePath(), displayName, mime, total);
    }

    public synchronized boolean shouldSkipDuplicate(long dialogId, int messageId) {
        // Novagram-style clean behavior: reply ONCE per private chat until you open or manually reply to it.
        if (answeredDialogs.contains(dialogId)) {
            return true;
        }
        if (messageId != 0) {
            Integer lastMsgId = lastAnsweredMsgIdByDialog.get(dialogId);
            if (lastMsgId != null && lastMsgId == messageId) {
                return true;
            }
        }
        Long lastTs = lastReplyTimestampByDialog.get(dialogId);
        if (lastTs != null && (System.currentTimeMillis() - lastTs) < DEDUP_COOLDOWN_MS) {
            return true;
        }
        return false;
    }

    public synchronized void markMessageAnswered(long dialogId, int messageId) {
        if (answeredDialogs.add(dialogId)) {
            saveAnsweredDialogsLocked();
        }
        lastReplyTimestampByDialog.put(dialogId, System.currentTimeMillis());
        if (messageId != 0) {
            lastAnsweredMsgIdByDialog.put(dialogId, messageId);
            saveLastAnsweredMsgIdsLocked();
        }
    }

    public synchronized boolean hasAnsweredDialog(long dialogId) {
        return answeredDialogs.contains(dialogId) || shouldSkipDuplicate(dialogId, 0);
    }

    public synchronized void markAnsweredDialog(long dialogId) {
        markMessageAnswered(dialogId, 0);
    }

    public synchronized void removeAnsweredDialog(long dialogId) {
        boolean removedDialog = answeredDialogs.remove(dialogId);
        lastReplyTimestampByDialog.remove(dialogId);
        if (removedDialog) {
            saveAnsweredDialogsLocked();
        }
    }

    private void saveAnsweredDialogsLocked() {
        try {
            StringBuilder sb = new StringBuilder();
            for (Long id : answeredDialogs) {
                if (id == null) continue;
                if (sb.length() > 0) sb.append(',');
                sb.append(id);
            }
            preferences.edit().putString(KEY_ANSWERED_DIALOGS, sb.toString()).apply();
        } catch (Throwable ignored) {}
    }

    public synchronized void clearAnsweredDialogs() {
        answeredDialogs.clear();
        lastReplyTimestampByDialog.clear();
        lastAnsweredMsgIdByDialog.clear();
        preferences.edit().remove(KEY_ANSWERED_DIALOGS).remove(KEY_LAST_ANSWERED_MSG_IDS).apply();
    }

    public synchronized boolean shouldSendAutoReply(long dialogId, int accountId) {
        if (!isAutoReplyEnabled()) return false;
        if (!isEligiblePrivateUserDialog(dialogId, accountId)) {
            return false;
        }
        return !shouldSkipDuplicate(dialogId, 0);
    }

    public synchronized String getAutoReplyResponse(String unused) {
        return getAutoAnswerText();
    }

    private void saveLastAnsweredMsgIdsLocked() {
        try {
            if (lastAnsweredMsgIdByDialog.size() > MAX_PERSISTED_MSG_IDS) {
                int toRemove = lastAnsweredMsgIdByDialog.size() - MAX_PERSISTED_MSG_IDS;
                java.util.Iterator<Long> it = lastAnsweredMsgIdByDialog.keySet().iterator();
                while (it.hasNext() && toRemove > 0) {
                    it.next();
                    it.remove();
                    toRemove--;
                }
            }
            StringBuilder sb = new StringBuilder();
            for (Map.Entry<Long, Integer> entry : lastAnsweredMsgIdByDialog.entrySet()) {
                if (sb.length() > 0) sb.append(',');
                sb.append(entry.getKey()).append(':').append(entry.getValue());
            }
            preferences.edit().putString(KEY_LAST_ANSWERED_MSG_IDS, sb.toString()).apply();
        } catch (Throwable ignored) {}
    }

    private synchronized void load() {
        autoReplyEnabled = preferences.getBoolean(KEY_ENABLED, false);
        autoAnswerText = preferences.getString(KEY_TEXT, "");
        autoAnswerEntitiesJson = preferences.getString(KEY_ENTITIES, null);
        mediaType = preferences.getInt(KEY_MEDIA_TYPE, MEDIA_TYPE_NONE);
        mediaPath = preferences.getString(KEY_MEDIA_PATH, "");
        mediaName = preferences.getString(KEY_MEDIA_NAME, "");
        mediaMime = preferences.getString(KEY_MEDIA_MIME, "");
        mediaDuration = preferences.getInt(KEY_MEDIA_DURATION, 0);
        // Permanently lock persisted preference to private 1-on-1 chats only
        if (!preferences.getBoolean(KEY_PRIVATE_ONLY, true)) {
            preferences.edit().putBoolean(KEY_PRIVATE_ONLY, true).apply();
        }
        answeredDialogs.clear();
        lastReplyTimestampByDialog.clear();
        lastAnsweredMsgIdByDialog.clear();
        try {
            String savedDialogs = preferences.getString(KEY_ANSWERED_DIALOGS, "");
            if (savedDialogs != null && !savedDialogs.isEmpty()) {
                for (String token : savedDialogs.split(",")) {
                    String trimmed = token.trim();
                    if (!trimmed.isEmpty()) {
                        answeredDialogs.add(Long.parseLong(trimmed));
                    }
                }
            }
        } catch (Throwable ignored) {}
        try {
            String raw = preferences.getString(KEY_LAST_ANSWERED_MSG_IDS, "");
            if (raw != null && !raw.isEmpty()) {
                for (String pair : raw.split(",")) {
                    int idx = pair.indexOf(':');
                    if (idx > 0 && idx + 1 < pair.length()) {
                        long did = Long.parseLong(pair.substring(0, idx));
                        int mid = Integer.parseInt(pair.substring(idx + 1));
                        lastAnsweredMsgIdByDialog.put(did, mid);
                    }
                }
            }
        } catch (Throwable ignored) {}
    }

    public static class EntityData {
        public int type; // 1=Bold, 2=Italic, 3=Mono, 4=Spoiler, 5=Strike, 6=Underline, 7=Quote, 8=Url
        public int offset;
        public int length;
        public String url;

        public EntityData() {}

        public EntityData(int type, int offset, int length, String url) {
            this.type = type;
            this.offset = offset;
            this.length = length;
            this.url = url;
        }
    }

    private static void addUrlEntityIfAbsent(List<EntityData> list, int start, int length, String url) {
        if (start < 0 || length <= 0 || TextUtils.isEmpty(url)) return;
        for (EntityData existing : list) {
            if (existing.type == 8 && existing.offset == start && existing.length == length) {
                existing.url = url;
                return;
            }
        }
        list.add(new EntityData(8, start, length, url));
    }

    public static String serializeSpansToJson(CharSequence text) {
        if (!(text instanceof Spannable)) return null;
        Spannable s = (Spannable) text;
        List<EntityData> list = new ArrayList<>();
        TextStyleSpan[] spans = s.getSpans(0, s.length(), TextStyleSpan.class);
        if (spans != null) {
            for (TextStyleSpan span : spans) {
                int start = s.getSpanStart(span);
                int end = s.getSpanEnd(span);
                if (start < 0 || end <= start) continue;
                TextStyleSpan.TextStyleRun run = span.getTextStyleRun();
                if (run == null) continue;
                int f = run.flags;
                if ((f & TextStyleSpan.FLAG_STYLE_BOLD) != 0) list.add(new EntityData(1, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_ITALIC) != 0) list.add(new EntityData(2, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_MONO) != 0) list.add(new EntityData(3, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_SPOILER) != 0) list.add(new EntityData(4, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_STRIKE) != 0) list.add(new EntityData(5, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_UNDERLINE) != 0) list.add(new EntityData(6, start, end - start, null));
                if ((f & TextStyleSpan.FLAG_STYLE_URL) != 0 && run.urlEntity != null && !TextUtils.isEmpty(run.urlEntity.url)) {
                    addUrlEntityIfAbsent(list, start, end - start, run.urlEntity.url);
                }
            }
        }
        QuoteSpan.QuoteStyleSpan[] quotes = s.getSpans(0, s.length(), QuoteSpan.QuoteStyleSpan.class);
        if (quotes != null) {
            for (QuoteSpan.QuoteStyleSpan q : quotes) {
                int start = s.getSpanStart(q);
                int end = s.getSpanEnd(q);
                if (start >= 0 && end > start) list.add(new EntityData(7, start, end - start, null));
            }
        }
        URLSpanReplacement[] urls = s.getSpans(0, s.length(), URLSpanReplacement.class);
        if (urls != null) {
            for (URLSpanReplacement u : urls) {
                int start = s.getSpanStart(u);
                int end = s.getSpanEnd(u);
                if (start >= 0 && end > start && !TextUtils.isEmpty(u.getURL())) {
                    addUrlEntityIfAbsent(list, start, end - start, u.getURL());
                }
            }
        }
        URLSpan[] stdUrls = s.getSpans(0, s.length(), URLSpan.class);
        if (stdUrls != null) {
            for (URLSpan u : stdUrls) {
                if (u instanceof URLSpanReplacement) continue;
                int start = s.getSpanStart(u);
                int end = s.getSpanEnd(u);
                if (start >= 0 && end > start && !TextUtils.isEmpty(u.getURL())) {
                    addUrlEntityIfAbsent(list, start, end - start, u.getURL());
                }
            }
        }
        if (list.isEmpty()) return null;
        try {
            return new Gson().toJson(list);
        } catch (Exception e) {
            return null;
        }
    }

    public static ArrayList<TLRPC.MessageEntity> parseEntitiesFromJson(String json) {
        ArrayList<TLRPC.MessageEntity> entities = new ArrayList<>();
        if (json == null || json.isEmpty()) return entities;
        try {
            Type type = new TypeToken<List<EntityData>>() {}.getType();
            List<EntityData> list = new Gson().fromJson(json, type);
            if (list == null) return entities;
            for (EntityData e : list) {
                if (e == null || e.length <= 0) continue;
                TLRPC.MessageEntity entity = null;
                if (e.type == 1) entity = new TLRPC.TL_messageEntityBold();
                else if (e.type == 2) entity = new TLRPC.TL_messageEntityItalic();
                else if (e.type == 3) entity = new TLRPC.TL_messageEntityCode();
                else if (e.type == 4) entity = new TLRPC.TL_messageEntitySpoiler();
                else if (e.type == 5) entity = new TLRPC.TL_messageEntityStrike();
                else if (e.type == 6) entity = new TLRPC.TL_messageEntityUnderline();
                else if (e.type == 7) entity = new TLRPC.TL_messageEntityBlockquote();
                else if (e.type == 8 && e.url != null && !e.url.isEmpty()) {
                    TLRPC.TL_messageEntityTextUrl urlEntity = new TLRPC.TL_messageEntityTextUrl();
                    urlEntity.url = e.url;
                    entity = urlEntity;
                }
                if (entity != null) {
                    entity.offset = e.offset;
                    entity.length = e.length;
                    entities.add(entity);
                }
            }
        } catch (Exception ignored) {}
        return entities;
    }

    public static void applyVisualLinkSpan(Spannable s, int start, int end, String url) {
        if (s == null || start < 0 || end <= start || end > s.length() || TextUtils.isEmpty(url)) return;
        URLSpanReplacement[] oldUrls = s.getSpans(start, end, URLSpanReplacement.class);
        if (oldUrls != null) {
            for (URLSpanReplacement old : oldUrls) {
                s.removeSpan(old);
            }
        }
        s.setSpan(new URLSpanReplacement(url), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new ForegroundColorSpan(0xFF0EA5E9), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        s.setSpan(new UnderlineSpan(), start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
    }

    public static void restoreSpansFromJson(Spannable s, String json) {
        if (s == null || json == null || json.isEmpty()) return;
        ArrayList<TLRPC.MessageEntity> entities = parseEntitiesFromJson(json);
        if (!entities.isEmpty()) {
            MediaDataController.addTextStyleRuns(entities, s, s);
            int len = s.length();
            for (TLRPC.MessageEntity e : entities) {
                int start = Math.max(0, Math.min(e.offset, len));
                int end = Math.max(start, Math.min(e.offset + e.length, len));
                if (end <= start) continue;
                if (e instanceof TLRPC.TL_messageEntityBlockquote) {
                    if (s instanceof Editable) {
                        QuoteSpan.putQuoteToEditable((Editable) s, start, end, false);
                    } else {
                        QuoteSpan.putQuote(s, start, end, false);
                    }
                } else if (e instanceof TLRPC.TL_messageEntityTextUrl) {
                    applyVisualLinkSpan(s, start, end, e.url);
                }
            }
        }
    }
}
