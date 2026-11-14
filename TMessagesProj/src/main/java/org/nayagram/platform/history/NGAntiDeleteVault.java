package org.nayagram.platform.history;

import android.text.TextUtils;

import androidx.collection.LongSparseArray;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.SQLite.SQLiteDatabase;
import org.telegram.SQLite.SQLitePreparedStatement;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.DialogObject;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessageCustomParamsHelper;
import org.telegram.messenger.MessageObject;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.MessagesStorage;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;

/**
 * Separate per-user persistent SQLite vault for NayaGram Anti-Delete messages, dialog previews,
 * users, and chats. Stored directly under ApplicationLoader.getFilesDirFixed() keyed by Telegram
 * ownerUserId so logout / cache4.db deletion / account slot re-login never wipes saved deleted
 * messages.
 */
public final class NGAntiDeleteVault {

    private static final String VAULT_FILE_PREFIX = "nayagram_antidelete_vault_";
    private static final Object VAULT_LOCK = new Object();

    private static long openOwnerUserId = 0L;
    private static SQLiteDatabase openVaultDb = null;
    private static final HashMap<Long, HashSet<Long>> dialogsWithMessagesByOwner = new HashMap<>();

    private NGAntiDeleteVault() {
    }

    private static File getVaultFile(long ownerUserId) {
        return new File(ApplicationLoader.getFilesDirFixed(), VAULT_FILE_PREFIX + ownerUserId + ".db");
    }

    public static boolean hasVaultForUser(long ownerUserId) {
        if (ownerUserId == 0 || ApplicationLoader.getFilesDirFixed() == null) {
            return false;
        }
        return getVaultFile(ownerUserId).exists();
    }

    private static void closeVaultLocked() {
        if (openVaultDb != null) {
            try {
                openVaultDb.close();
            } catch (Throwable t) {
                FileLog.e(t);
            }
            openVaultDb = null;
            openOwnerUserId = 0L;
        }
    }

    private static SQLiteDatabase openVaultLocked(long ownerUserId, boolean createIfMissing) {
        if (ownerUserId == 0 || ApplicationLoader.getFilesDirFixed() == null) {
            return null;
        }
        File vaultFile = getVaultFile(ownerUserId);
        if (!createIfMissing && !vaultFile.exists()) {
            return null;
        }
        if (openVaultDb != null && openOwnerUserId == ownerUserId) {
            return openVaultDb;
        }
        closeVaultLocked();
        try {
            SQLiteDatabase db = new SQLiteDatabase(vaultFile.getPath());
            db.executeFast("PRAGMA temp_store = MEMORY").stepThis().dispose();
            db.executeFast("PRAGMA journal_mode = WAL").stepThis().dispose();
            db.executeFast("PRAGMA journal_size_limit = 5242880").stepThis().dispose();
            ensureSchemaLocked(db);
            openVaultDb = db;
            openOwnerUserId = ownerUserId;
            return db;
        } catch (Throwable t) {
            FileLog.e(t);
            closeVaultLocked();
            return null;
        }
    }

    private static void ensureSchemaLocked(SQLiteDatabase db) throws Exception {
        db.executeFast("CREATE TABLE IF NOT EXISTS saved_messages("
                + "mid INTEGER, uid INTEGER, topic_id INTEGER, read_state INTEGER, send_state INTEGER, "
                + "date INTEGER, data BLOB, out INTEGER, ttl INTEGER, media INTEGER, replydata BLOB, "
                + "imp INTEGER, mention INTEGER, forwards INTEGER, replies_data BLOB, thread_reply_id INTEGER, "
                + "is_channel INTEGER, reply_to_message_id INTEGER, custom_params BLOB, group_id INTEGER, "
                + "reply_to_story_id INTEGER, PRIMARY KEY(mid, uid, topic_id))").stepThis().dispose();
        db.executeFast("CREATE INDEX IF NOT EXISTS idx_saved_messages_uid_date ON saved_messages(uid, date DESC, mid DESC)").stepThis().dispose();
        db.executeFast("CREATE INDEX IF NOT EXISTS idx_saved_messages_uid_topic ON saved_messages(uid, topic_id, mid DESC)").stepThis().dispose();

        db.executeFast("CREATE TABLE IF NOT EXISTS saved_dialogs("
                + "did INTEGER PRIMARY KEY, date INTEGER, unread_count INTEGER, last_mid INTEGER, "
                + "inbox_max INTEGER, outbox_max INTEGER, last_mid_i INTEGER, unread_count_i INTEGER, "
                + "pts INTEGER, date_i INTEGER, pinned INTEGER, flags INTEGER, folder_id INTEGER, "
                + "data BLOB, unread_reactions INTEGER, last_mid_group INTEGER, ttl_period INTEGER, "
                + "unread_poll_votes INTEGER)").stepThis().dispose();
        db.executeFast("CREATE INDEX IF NOT EXISTS idx_saved_dialogs_folder ON saved_dialogs(folder_id, date DESC)").stepThis().dispose();

        db.executeFast("CREATE TABLE IF NOT EXISTS saved_users(uid INTEGER PRIMARY KEY, name TEXT, status INTEGER, data BLOB)").stepThis().dispose();
        db.executeFast("CREATE TABLE IF NOT EXISTS saved_chats(uid INTEGER PRIMARY KEY, name TEXT, data BLOB)").stepThis().dispose();
    }

    private static HashSet<Long> getDialogsWithMessagesLocked(long ownerUserId) {
        HashSet<Long> set = dialogsWithMessagesByOwner.get(ownerUserId);
        if (set != null) {
            return set;
        }
        set = new HashSet<>();
        SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, false);
        if (vaultDb != null) {
            SQLiteCursor cursor = null;
            try {
                cursor = vaultDb.queryFinalized("SELECT DISTINCT uid FROM saved_messages");
                while (cursor.next()) {
                    set.add(cursor.longValue(0));
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        }
        dialogsWithMessagesByOwner.put(ownerUserId, set);
        return set;
    }

    private static boolean hasMessagesForDialog(long ownerUserId, long dialogId) {
        if (ownerUserId == 0 || dialogId == 0) {
            return false;
        }
        synchronized (VAULT_LOCK) {
            if (!hasVaultForUser(ownerUserId)) {
                return false;
            }
            return getDialogsWithMessagesLocked(ownerUserId).contains(dialogId);
        }
    }

    private static void collectDialogPeer(long dialogId, ArrayList<Long> userIds, ArrayList<Long> chatIds) {
        if (DialogObject.isUserDialog(dialogId)) {
            if (!userIds.contains(dialogId)) {
                userIds.add(dialogId);
            }
        } else if (DialogObject.isChatDialog(dialogId)) {
            long chatId = -dialogId;
            if (!chatIds.contains(chatId)) {
                chatIds.add(chatId);
            }
        }
    }

    private static NativeByteBuffer readMessageDataAndCollectPeers(
            SQLiteCursor src, int col, long ownerUserId, ArrayList<Long> userIds, ArrayList<Long> chatIds, int[] outDate) {
        try {
            if (src.isNull(col)) {
                return null;
            }
            NativeByteBuffer buf = src.byteBufferValue(col);
            if (buf == null) {
                return null;
            }
            TLRPC.Message msg = TLRPC.Message.TLdeserialize(buf, buf.readInt32(false), false);
            if (msg != null) {
                msg.readAttachPath(buf, ownerUserId);
                MessagesStorage.addUsersAndChatsFromMessage(msg, userIds, chatIds, null);
                if (outDate != null && outDate.length > 0 && msg.date > 0) {
                    outDate[0] = msg.date;
                }
            }
            buf.rewind();
            return buf;
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        }
    }

    private static NativeByteBuffer buildAntiDeletedCustomParams(SQLiteCursor src, int col) {
        TLRPC.TL_message tmp = new TLRPC.TL_message();
        NativeByteBuffer raw = null;
        try {
            if (!src.isNull(col)) {
                raw = src.byteBufferValue(col);
                if (raw != null) {
                    MessageCustomParamsHelper.readLocalParams(tmp, raw);
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        } finally {
            if (raw != null) {
                raw.reuse();
            }
        }
        tmp.isAntiDeleted = true;
        return MessageCustomParamsHelper.writeLocalParams(tmp);
    }

    private static boolean isAntiDeletedParamsColumn(SQLiteCursor src, int col) {
        NativeByteBuffer raw = null;
        try {
            if (src.isNull(col)) {
                return false;
            }
            raw = src.byteBufferValue(col);
            if (raw == null) {
                return false;
            }
            TLRPC.TL_message tmp = new TLRPC.TL_message();
            MessageCustomParamsHelper.readLocalParams(tmp, raw);
            return tmp.isAntiDeleted;
        } catch (Throwable t) {
            FileLog.e(t);
            return false;
        } finally {
            if (raw != null) {
                raw.reuse();
            }
        }
    }

    private static void bindBlobFromBufferOrNull(SQLitePreparedStatement st, int idx, SQLiteCursor src, int srcCol, ArrayList<NativeByteBuffer> toReuse) throws Exception {
        if (src.isNull(srcCol)) {
            st.bindNull(idx);
            return;
        }
        NativeByteBuffer buf = src.byteBufferValue(srcCol);
        if (buf == null) {
            st.bindNull(idx);
            return;
        }
        buf.rewind();
        st.bindByteBuffer(idx, buf);
        toReuse.add(buf);
    }

    private static void reuseAll(ArrayList<NativeByteBuffer> list) {
        for (int i = 0; i < list.size(); i++) {
            NativeByteBuffer b = list.get(i);
            if (b != null) {
                b.reuse();
            }
        }
        list.clear();
    }

    private static void upsertSavedMessageFromV2RowLocked(
            SQLitePreparedStatement st, SQLiteCursor cur, long ownerUserId, ArrayList<Long> userIds, ArrayList<Long> chatIds) throws Exception {
        ArrayList<NativeByteBuffer> toReuse = new ArrayList<>(5);
        try {
            int mid = cur.intValue(0);
            long uid = cur.longValue(1);
            collectDialogPeer(uid, userIds, chatIds);

            st.requery();
            st.bindInteger(1, mid);
            st.bindLong(2, uid);
            st.bindLong(3, 0L); // topic_id = 0 for messages_v2
            st.bindInteger(4, cur.intValue(2)); // read_state
            st.bindInteger(5, cur.intValue(3)); // send_state
            st.bindInteger(6, cur.intValue(4)); // date

            NativeByteBuffer dataBuf = readMessageDataAndCollectPeers(cur, 5, ownerUserId, userIds, chatIds, null);
            if (dataBuf != null) {
                st.bindByteBuffer(7, dataBuf);
                toReuse.add(dataBuf);
            } else {
                st.bindNull(7);
            }

            st.bindInteger(8, cur.intValue(6)); // out
            st.bindInteger(9, cur.intValue(7)); // ttl
            st.bindInteger(10, cur.intValue(8)); // media

            bindBlobFromBufferOrNull(st, 11, cur, 9, toReuse); // replydata
            st.bindInteger(12, cur.intValue(10)); // imp
            st.bindInteger(13, cur.intValue(11)); // mention
            st.bindInteger(14, cur.intValue(12)); // forwards
            bindBlobFromBufferOrNull(st, 15, cur, 13, toReuse); // replies_data
            st.bindInteger(16, cur.intValue(14)); // thread_reply_id
            st.bindInteger(17, cur.intValue(15)); // is_channel
            st.bindInteger(18, cur.intValue(16)); // reply_to_message_id

            NativeByteBuffer customParamsBuf = buildAntiDeletedCustomParams(cur, 17);
            if (customParamsBuf != null) {
                st.bindByteBuffer(19, customParamsBuf);
                toReuse.add(customParamsBuf);
            } else {
                st.bindNull(19);
            }

            if (cur.isNull(18)) {
                st.bindNull(20);
            } else {
                st.bindLong(20, cur.longValue(18)); // group_id
            }
            st.bindInteger(21, cur.intValue(19)); // reply_to_story_id
            st.step();
        } finally {
            reuseAll(toReuse);
        }
    }

    private static void upsertSavedMessageFromTopicRowLocked(
            SQLitePreparedStatement st, SQLiteCursor cur, long ownerUserId, ArrayList<Long> userIds, ArrayList<Long> chatIds) throws Exception {
        ArrayList<NativeByteBuffer> toReuse = new ArrayList<>(5);
        try {
            int mid = cur.intValue(0);
            long uid = cur.longValue(1);
            collectDialogPeer(uid, userIds, chatIds);

            st.requery();
            st.bindInteger(1, mid);
            st.bindLong(2, uid);
            st.bindLong(3, cur.longValue(2)); // topic_id
            st.bindInteger(4, cur.intValue(3)); // read_state
            st.bindInteger(5, cur.intValue(4)); // send_state
            st.bindInteger(6, cur.intValue(5)); // date

            NativeByteBuffer dataBuf = readMessageDataAndCollectPeers(cur, 6, ownerUserId, userIds, chatIds, null);
            if (dataBuf != null) {
                st.bindByteBuffer(7, dataBuf);
                toReuse.add(dataBuf);
            } else {
                st.bindNull(7);
            }

            st.bindInteger(8, cur.intValue(7)); // out
            st.bindInteger(9, cur.intValue(8)); // ttl
            st.bindInteger(10, cur.intValue(9)); // media

            bindBlobFromBufferOrNull(st, 11, cur, 10, toReuse); // replydata
            st.bindInteger(12, cur.intValue(11)); // imp
            st.bindInteger(13, cur.intValue(12)); // mention
            st.bindInteger(14, cur.intValue(13)); // forwards
            bindBlobFromBufferOrNull(st, 15, cur, 14, toReuse); // replies_data
            st.bindInteger(16, cur.intValue(15)); // thread_reply_id
            st.bindInteger(17, cur.intValue(16)); // is_channel
            st.bindInteger(18, cur.intValue(17)); // reply_to_message_id

            NativeByteBuffer customParamsBuf = buildAntiDeletedCustomParams(cur, 18);
            if (customParamsBuf != null) {
                st.bindByteBuffer(19, customParamsBuf);
                toReuse.add(customParamsBuf);
            } else {
                st.bindNull(19);
            }

            st.bindNull(20); // group_id (messages_topics doesn't have group_id)
            st.bindInteger(21, cur.intValue(19)); // reply_to_story_id
            st.step();
        } finally {
            reuseAll(toReuse);
        }
    }

    private static void saveDialogsFromMainDbLocked(SQLiteDatabase mainDb, SQLiteDatabase vaultDb, Collection<Long> dialogIds) {
        if (dialogIds == null || dialogIds.isEmpty()) {
            return;
        }
        String joined = TextUtils.join(",", dialogIds);
        if (TextUtils.isEmpty(joined)) {
            return;
        }
        SQLiteCursor dCur = null;
        SQLitePreparedStatement st = null;
        ArrayList<NativeByteBuffer> toReuse = new ArrayList<>();
        HashSet<Long> foundDialogs = new HashSet<>();
        try {
            dCur = mainDb.queryFinalized(String.format(Locale.US,
                    "SELECT did, date, unread_count, last_mid, inbox_max, outbox_max, last_mid_i, unread_count_i, "
                            + "pts, date_i, pinned, flags, folder_id, data, unread_reactions, last_mid_group, ttl_period, unread_poll_votes "
                            + "FROM dialogs WHERE did IN(%s)", joined));
            st = vaultDb.executeFast("REPLACE INTO saved_dialogs VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            while (dCur.next()) {
                long did = dCur.longValue(0);
                foundDialogs.add(did);

                int date = dCur.intValue(1);
                int lastMid = dCur.intValue(3);

                // Ensure last_mid / date reflect any newer deleted message already in saved_messages.
                SQLiteCursor maxCur = null;
                try {
                    maxCur = vaultDb.queryFinalized(String.format(Locale.US,
                            "SELECT mid, date FROM saved_messages WHERE uid = %d AND topic_id = 0 AND mid > 0 ORDER BY mid DESC LIMIT 1", did));
                    if (maxCur.next()) {
                        int vMid = maxCur.intValue(0);
                        int vDate = maxCur.intValue(1);
                        if (vMid > lastMid) {
                            lastMid = vMid;
                        }
                        if (vDate > date) {
                            date = vDate;
                        }
                    }
                } finally {
                    if (maxCur != null) {
                        maxCur.dispose();
                    }
                }

                st.requery();
                st.bindLong(1, did);
                st.bindInteger(2, date);
                st.bindInteger(3, 0); // unread_count = 0 (avoid phantom badges after logout)
                st.bindInteger(4, lastMid);
                st.bindInteger(5, dCur.intValue(4)); // inbox_max
                st.bindInteger(6, dCur.intValue(5)); // outbox_max
                st.bindLong(7, dCur.longValue(6)); // last_mid_i
                st.bindInteger(8, 0); // unread_count_i
                st.bindInteger(9, dCur.intValue(8)); // pts
                st.bindInteger(10, dCur.intValue(9)); // date_i
                st.bindInteger(11, dCur.intValue(10)); // pinned
                st.bindInteger(12, dCur.intValue(11)); // flags
                st.bindInteger(13, dCur.intValue(12)); // folder_id
                bindBlobFromBufferOrNull(st, 14, dCur, 13, toReuse);
                st.bindInteger(15, 0); // unread_reactions
                st.bindNull(16); // last_mid_group NULL so LEFT JOIN messages_v2 on last_mid always matches
                st.bindInteger(17, dCur.intValue(16)); // ttl_period
                st.bindInteger(18, 0); // unread_poll_votes
                st.step();
                reuseAll(toReuse);
            }
        } catch (Throwable t) {
            FileLog.e(t);
        } finally {
            reuseAll(toReuse);
            if (st != null) {
                st.dispose();
            }
            if (dCur != null) {
                dCur.dispose();
            }
        }

        // Synthesize minimal saved_dialogs entry if mainDb had no dialog row at all
        for (Long didObj : dialogIds) {
            if (didObj == null || foundDialogs.contains(didObj)) {
                continue;
            }
            long did = didObj;
            SQLiteCursor maxCur = null;
            SQLitePreparedStatement synthSt = null;
            try {
                maxCur = vaultDb.queryFinalized(String.format(Locale.US,
                        "SELECT mid, date FROM saved_messages WHERE uid = %d AND mid > 0 ORDER BY mid DESC LIMIT 1", did));
                if (maxCur.next()) {
                    int maxMid = maxCur.intValue(0);
                    int maxDate = maxCur.intValue(1);
                    synthSt = vaultDb.executeFast("INSERT OR IGNORE INTO saved_dialogs VALUES(?, ?, 0, ?, ?, 0, ?, 0, 0, ?, 0, 0, 0, NULL, 0, NULL, 0, 0)");
                    synthSt.bindLong(1, did);
                    synthSt.bindInteger(2, maxDate);
                    synthSt.bindInteger(3, maxMid);
                    synthSt.bindInteger(4, maxMid);
                    synthSt.bindInteger(5, maxMid);
                    synthSt.bindInteger(6, maxDate);
                    synthSt.step();
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (synthSt != null) {
                    synthSt.dispose();
                }
                if (maxCur != null) {
                    maxCur.dispose();
                }
            }
        }
    }

    private static void saveUsersAndChatsFromMainDbLocked(
            SQLiteDatabase mainDb, SQLiteDatabase vaultDb, ArrayList<Long> userIds, ArrayList<Long> chatIds) {
        ArrayList<NativeByteBuffer> toReuse = new ArrayList<>();
        if (userIds != null && !userIds.isEmpty()) {
            SQLiteCursor cur = null;
            SQLitePreparedStatement st = null;
            try {
                cur = mainDb.queryFinalized(String.format(Locale.US,
                        "SELECT uid, name, status, data FROM users WHERE uid IN(%s)", TextUtils.join(",", userIds)));
                st = vaultDb.executeFast("REPLACE INTO saved_users VALUES(?, ?, ?, ?)");
                while (cur.next()) {
                    st.requery();
                    st.bindLong(1, cur.longValue(0));
                    String name = cur.stringValue(1);
                    if (name != null) {
                        st.bindString(2, name);
                    } else {
                        st.bindNull(2);
                    }
                    st.bindInteger(3, cur.intValue(2));
                    bindBlobFromBufferOrNull(st, 4, cur, 3, toReuse);
                    st.step();
                    reuseAll(toReuse);
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                reuseAll(toReuse);
                if (st != null) {
                    st.dispose();
                }
                if (cur != null) {
                    cur.dispose();
                }
            }
        }
        if (chatIds != null && !chatIds.isEmpty()) {
            SQLiteCursor cur = null;
            SQLitePreparedStatement st = null;
            try {
                cur = mainDb.queryFinalized(String.format(Locale.US,
                        "SELECT uid, name, data FROM chats WHERE uid IN(%s)", TextUtils.join(",", chatIds)));
                st = vaultDb.executeFast("REPLACE INTO saved_chats VALUES(?, ?, ?)");
                while (cur.next()) {
                    st.requery();
                    st.bindLong(1, cur.longValue(0));
                    String name = cur.stringValue(1);
                    if (name != null) {
                        st.bindString(2, name);
                    } else {
                        st.bindNull(2);
                    }
                    bindBlobFromBufferOrNull(st, 3, cur, 2, toReuse);
                    st.step();
                    reuseAll(toReuse);
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                reuseAll(toReuse);
                if (st != null) {
                    st.dispose();
                }
                if (cur != null) {
                    cur.dispose();
                }
            }
        }
    }

    /**
     * Live hook: saves a single message (v2 and/or topic row), its referenced peers, and dialog entry
     * into the user's persistent vault as soon as it is protected by Anti-Delete.
     */
    public static void saveFromMainDb(long ownerUserId, SQLiteDatabase mainDb, long dialogId, int mid) {
        if (ownerUserId == 0 || mainDb == null || dialogId == 0 || mid == 0 || DialogObject.isEncryptedDialog(dialogId)) {
            return;
        }
        synchronized (VAULT_LOCK) {
            SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, true);
            if (vaultDb == null) {
                return;
            }
            ArrayList<Long> userIds = new ArrayList<>();
            ArrayList<Long> chatIds = new ArrayList<>();
            userIds.add(ownerUserId);
            collectDialogPeer(dialogId, userIds, chatIds);

            boolean savedAny = false;
            SQLiteCursor cur = null;
            SQLitePreparedStatement st = null;
            try {
                vaultDb.beginTransaction();
                st = vaultDb.executeFast("REPLACE INTO saved_messages VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

                cur = mainDb.queryFinalized(String.format(Locale.US,
                        "SELECT mid, uid, read_state, send_state, date, data, out, ttl, media, replydata, imp, mention, forwards, replies_data, thread_reply_id, is_channel, reply_to_message_id, custom_params, group_id, reply_to_story_id "
                                + "FROM messages_v2 WHERE mid = %d AND uid = %d", mid, dialogId));
                while (cur.next()) {
                    upsertSavedMessageFromV2RowLocked(st, cur, ownerUserId, userIds, chatIds);
                    savedAny = true;
                }
                cur.dispose();
                cur = null;

                cur = mainDb.queryFinalized(String.format(Locale.US,
                        "SELECT mid, uid, topic_id, read_state, send_state, date, data, out, ttl, media, replydata, imp, mention, forwards, replies_data, thread_reply_id, is_channel, reply_to_message_id, custom_params, reply_to_story_id "
                                + "FROM messages_topics WHERE mid = %d AND uid = %d", mid, dialogId));
                while (cur.next()) {
                    upsertSavedMessageFromTopicRowLocked(st, cur, ownerUserId, userIds, chatIds);
                    savedAny = true;
                }
                cur.dispose();
                cur = null;

                st.dispose();
                st = null;

                if (savedAny) {
                    saveDialogsFromMainDbLocked(mainDb, vaultDb, Collections.singletonList(dialogId));
                    saveUsersAndChatsFromMainDbLocked(mainDb, vaultDb, userIds, chatIds);
                    getDialogsWithMessagesLocked(ownerUserId).add(dialogId);
                }
                vaultDb.commitTransaction();
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (st != null) {
                    st.dispose();
                }
                if (cur != null) {
                    cur.dispose();
                }
                try {
                    vaultDb.commitTransaction();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * Pre-logout safety snapshot: scans cache4.db for all messages carrying isAntiDeleted = true
     * and persists any missing ones (plus their dialogs, users, and chats) before cache4.db is deleted.
     */
    public static void backupAllBeforeLogout(long ownerUserId, SQLiteDatabase mainDb) {
        if (ownerUserId == 0 || mainDb == null) {
            return;
        }
        synchronized (VAULT_LOCK) {
            SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, true);
            if (vaultDb == null) {
                return;
            }
            ArrayList<Long> userIds = new ArrayList<>();
            ArrayList<Long> chatIds = new ArrayList<>();
            userIds.add(ownerUserId);
            HashSet<Long> dialogIds = new HashSet<>();

            SQLiteCursor cur = null;
            SQLitePreparedStatement st = null;
            try {
                vaultDb.beginTransaction();
                st = vaultDb.executeFast("REPLACE INTO saved_messages VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

                cur = mainDb.queryFinalized(
                        "SELECT mid, uid, read_state, send_state, date, data, out, ttl, media, replydata, imp, mention, forwards, replies_data, thread_reply_id, is_channel, reply_to_message_id, custom_params, group_id, reply_to_story_id "
                                + "FROM messages_v2 WHERE custom_params IS NOT NULL");
                while (cur.next()) {
                    long uid = cur.longValue(1);
                    if (DialogObject.isEncryptedDialog(uid)) {
                        continue;
                    }
                    if (!isAntiDeletedParamsColumn(cur, 17)) {
                        continue;
                    }
                    upsertSavedMessageFromV2RowLocked(st, cur, ownerUserId, userIds, chatIds);
                    dialogIds.add(uid);
                }
                cur.dispose();
                cur = null;

                cur = mainDb.queryFinalized(
                        "SELECT mid, uid, topic_id, read_state, send_state, date, data, out, ttl, media, replydata, imp, mention, forwards, replies_data, thread_reply_id, is_channel, reply_to_message_id, custom_params, reply_to_story_id "
                                + "FROM messages_topics WHERE custom_params IS NOT NULL");
                while (cur.next()) {
                    long uid = cur.longValue(1);
                    if (DialogObject.isEncryptedDialog(uid)) {
                        continue;
                    }
                    if (!isAntiDeletedParamsColumn(cur, 18)) {
                        continue;
                    }
                    upsertSavedMessageFromTopicRowLocked(st, cur, ownerUserId, userIds, chatIds);
                    dialogIds.add(uid);
                }
                cur.dispose();
                cur = null;

                st.dispose();
                st = null;

                if (!dialogIds.isEmpty()) {
                    saveDialogsFromMainDbLocked(mainDb, vaultDb, dialogIds);
                    saveUsersAndChatsFromMainDbLocked(mainDb, vaultDb, userIds, chatIds);
                    getDialogsWithMessagesLocked(ownerUserId).addAll(dialogIds);
                }
                vaultDb.commitTransaction();
                closeVaultLocked();
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (st != null) {
                    st.dispose();
                }
                if (cur != null) {
                    cur.dispose();
                }
                try {
                    if (openVaultDb != null) {
                        openVaultDb.commitTransaction();
                    }
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * Imports all persisted Anti-Delete users, chats, messages, and dialogs from the user's vault
     * into mainDb (cache4.db). Safe and idempotent across logins / DB opens.
     */
    public static boolean restoreIntoMainDb(long ownerUserId, SQLiteDatabase mainDb) {
        if (ownerUserId == 0 || mainDb == null) {
            return false;
        }
        synchronized (VAULT_LOCK) {
            SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, false);
            if (vaultDb == null) {
                return false;
            }
            ArrayList<NativeByteBuffer> toReuse = new ArrayList<>();
            SQLiteCursor cur = null;
            SQLitePreparedStatement st = null;
            boolean restoredAny = false;
            try {
                mainDb.beginTransaction();

                // 1. Users
                cur = vaultDb.queryFinalized("SELECT uid, name, status, data FROM saved_users");
                st = mainDb.executeFast("INSERT OR IGNORE INTO users VALUES(?, ?, ?, ?)");
                while (cur.next()) {
                    st.requery();
                    st.bindLong(1, cur.longValue(0));
                    String name = cur.stringValue(1);
                    if (name != null) {
                        st.bindString(2, name);
                    } else {
                        st.bindNull(2);
                    }
                    st.bindInteger(3, cur.intValue(2));
                    bindBlobFromBufferOrNull(st, 4, cur, 3, toReuse);
                    st.step();
                    reuseAll(toReuse);
                }
                st.dispose();
                st = null;
                cur.dispose();
                cur = null;

                // 2. Chats
                cur = vaultDb.queryFinalized("SELECT uid, name, data FROM saved_chats");
                st = mainDb.executeFast("INSERT OR IGNORE INTO chats VALUES(?, ?, ?)");
                while (cur.next()) {
                    st.requery();
                    st.bindLong(1, cur.longValue(0));
                    String name = cur.stringValue(1);
                    if (name != null) {
                        st.bindString(2, name);
                    } else {
                        st.bindNull(2);
                    }
                    bindBlobFromBufferOrNull(st, 3, cur, 2, toReuse);
                    st.step();
                    reuseAll(toReuse);
                }
                st.dispose();
                st = null;
                cur.dispose();
                cur = null;

                // 3. Messages -> messages_v2 (topic_id == 0) and messages_topics (topic_id != 0)
                SQLitePreparedStatement stV2 = mainDb.executeFast(
                        "INSERT OR IGNORE INTO messages_v2 VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                SQLitePreparedStatement stTopics = mainDb.executeFast(
                        "INSERT OR IGNORE INTO messages_topics VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
                try {
                    cur = vaultDb.queryFinalized(
                            "SELECT mid, uid, topic_id, read_state, send_state, date, data, out, ttl, media, replydata, imp, mention, forwards, replies_data, thread_reply_id, is_channel, reply_to_message_id, custom_params, group_id, reply_to_story_id "
                                    + "FROM saved_messages");
                    while (cur.next()) {
                        long topicId = cur.longValue(2);
                        if (topicId == 0) {
                            stV2.requery();
                            stV2.bindInteger(1, cur.intValue(0));
                            stV2.bindLong(2, cur.longValue(1));
                            stV2.bindInteger(3, cur.intValue(3));
                            stV2.bindInteger(4, cur.intValue(4));
                            stV2.bindInteger(5, cur.intValue(5));
                            bindBlobFromBufferOrNull(stV2, 6, cur, 6, toReuse);
                            stV2.bindInteger(7, cur.intValue(7));
                            stV2.bindInteger(8, cur.intValue(8));
                            stV2.bindInteger(9, cur.intValue(9));
                            bindBlobFromBufferOrNull(stV2, 10, cur, 10, toReuse);
                            stV2.bindInteger(11, cur.intValue(11));
                            stV2.bindInteger(12, cur.intValue(12));
                            stV2.bindInteger(13, cur.intValue(13));
                            bindBlobFromBufferOrNull(stV2, 14, cur, 14, toReuse);
                            stV2.bindInteger(15, cur.intValue(15));
                            stV2.bindInteger(16, cur.intValue(16));
                            stV2.bindInteger(17, cur.intValue(17));
                            NativeByteBuffer cp = buildAntiDeletedCustomParams(cur, 18);
                            if (cp != null) {
                                stV2.bindByteBuffer(18, cp);
                                toReuse.add(cp);
                            } else {
                                stV2.bindNull(18);
                            }
                            if (cur.isNull(19)) {
                                stV2.bindNull(19);
                            } else {
                                stV2.bindLong(19, cur.longValue(19));
                            }
                            stV2.bindInteger(20, cur.intValue(20));
                            stV2.step();
                            reuseAll(toReuse);
                        } else {
                            stTopics.requery();
                            stTopics.bindInteger(1, cur.intValue(0));
                            stTopics.bindLong(2, cur.longValue(1));
                            stTopics.bindLong(3, topicId);
                            stTopics.bindInteger(4, cur.intValue(3));
                            stTopics.bindInteger(5, cur.intValue(4));
                            stTopics.bindInteger(6, cur.intValue(5));
                            bindBlobFromBufferOrNull(stTopics, 7, cur, 6, toReuse);
                            stTopics.bindInteger(8, cur.intValue(7));
                            stTopics.bindInteger(9, cur.intValue(8));
                            stTopics.bindInteger(10, cur.intValue(9));
                            bindBlobFromBufferOrNull(stTopics, 11, cur, 10, toReuse);
                            stTopics.bindInteger(12, cur.intValue(11));
                            stTopics.bindInteger(13, cur.intValue(12));
                            stTopics.bindInteger(14, cur.intValue(13));
                            bindBlobFromBufferOrNull(stTopics, 15, cur, 14, toReuse);
                            stTopics.bindInteger(16, cur.intValue(15));
                            stTopics.bindInteger(17, cur.intValue(16));
                            stTopics.bindInteger(18, cur.intValue(17));
                            NativeByteBuffer cp = buildAntiDeletedCustomParams(cur, 18);
                            if (cp != null) {
                                stTopics.bindByteBuffer(19, cp);
                                toReuse.add(cp);
                            } else {
                                stTopics.bindNull(19);
                            }
                            stTopics.bindInteger(20, cur.intValue(20));
                            stTopics.step();
                            reuseAll(toReuse);
                        }
                        restoredAny = true;
                    }
                    cur.dispose();
                    cur = null;
                } finally {
                    stV2.dispose();
                    stTopics.dispose();
                }

                // 4. Reconcile dialogs table in mainDb
                reconcileDialogsAfterServerPutLocked(mainDb, vaultDb);

                mainDb.commitTransaction();
                return restoredAny;
            } catch (Throwable t) {
                FileLog.e(t);
                return false;
            } finally {
                reuseAll(toReuse);
                if (st != null) {
                    st.dispose();
                }
                if (cur != null) {
                    cur.dispose();
                }
                try {
                    mainDb.commitTransaction();
                } catch (Throwable ignore) {
                }
            }
        }
    }

    /**
     * Reconciles cache4.db dialogs after putDialogsInternal writes server dialogs, ensuring:
     * 1) Cleared/deleted dialogs only present in Vault reappear in dialogs list.
     * 2) Existing dialogs whose latest saved deleted message exceeds server last_mid keep last_mid & date updated.
     */
    public static void reconcileDialogsAfterServerPut(long ownerUserId, SQLiteDatabase mainDb) {
        if (ownerUserId == 0 || mainDb == null) {
            return;
        }
        synchronized (VAULT_LOCK) {
            SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, false);
            if (vaultDb == null) {
                return;
            }
            reconcileDialogsAfterServerPutLocked(mainDb, vaultDb);
        }
    }

    private static void reconcileDialogsAfterServerPutLocked(SQLiteDatabase mainDb, SQLiteDatabase vaultDb) {
        SQLiteCursor cur = null;
        SQLitePreparedStatement insertDialogSt = null;
        SQLitePreparedStatement updateLastSt = null;
        ArrayList<NativeByteBuffer> toReuse = new ArrayList<>();
        try {
            insertDialogSt = mainDb.executeFast(
                    "INSERT OR IGNORE INTO dialogs VALUES(?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
            updateLastSt = mainDb.executeFast(
                    "UPDATE dialogs SET last_mid = ?, date = ?, last_mid_group = NULL WHERE did = ?");

            cur = vaultDb.queryFinalized(
                    "SELECT did, date, unread_count, last_mid, inbox_max, outbox_max, last_mid_i, unread_count_i, "
                            + "pts, date_i, pinned, flags, folder_id, data, unread_reactions, last_mid_group, ttl_period, unread_poll_votes "
                            + "FROM saved_dialogs");
            while (cur.next()) {
                long did = cur.longValue(0);
                int maxSavedMid = 0;
                int maxSavedDate = 0;
                SQLiteCursor msgCur = null;
                try {
                    msgCur = vaultDb.queryFinalized(String.format(Locale.US,
                            "SELECT mid, date FROM saved_messages WHERE uid = %d AND topic_id = 0 AND mid > 0 ORDER BY mid DESC LIMIT 1", did));
                    if (msgCur.next()) {
                        maxSavedMid = msgCur.intValue(0);
                        maxSavedDate = msgCur.intValue(1);
                    }
                } finally {
                    if (msgCur != null) {
                        msgCur.dispose();
                    }
                }
                if (maxSavedMid == 0) {
                    // Only restore dialog if there is actually at least one saved message (including topic messages)
                    SQLiteCursor anyCur = null;
                    try {
                        anyCur = vaultDb.queryFinalized(String.format(Locale.US,
                                "SELECT mid, date FROM saved_messages WHERE uid = %d ORDER BY date DESC LIMIT 1", did));
                        if (anyCur.next()) {
                            maxSavedMid = anyCur.intValue(0);
                            maxSavedDate = anyCur.intValue(1);
                        }
                    } finally {
                        if (anyCur != null) {
                            anyCur.dispose();
                        }
                    }
                }
                if (maxSavedMid == 0) {
                    continue;
                }

                int vaultDate = Math.max(cur.intValue(1), maxSavedDate);
                int vaultLastMid = Math.max(cur.intValue(3), maxSavedMid);

                int mainLastMid = -1;
                int mainDate = 0;
                boolean existsInMain = false;
                SQLiteCursor mainCur = null;
                try {
                    mainCur = mainDb.queryFinalized("SELECT last_mid, date FROM dialogs WHERE did = " + did);
                    if (mainCur.next()) {
                        existsInMain = true;
                        mainLastMid = mainCur.intValue(0);
                        mainDate = mainCur.intValue(1);
                    }
                } finally {
                    if (mainCur != null) {
                        mainCur.dispose();
                    }
                }

                if (!existsInMain) {
                    insertDialogSt.requery();
                    insertDialogSt.bindLong(1, did);
                    insertDialogSt.bindInteger(2, vaultDate);
                    insertDialogSt.bindInteger(3, 0);
                    insertDialogSt.bindInteger(4, vaultLastMid);
                    insertDialogSt.bindInteger(5, cur.intValue(4));
                    insertDialogSt.bindInteger(6, cur.intValue(5));
                    insertDialogSt.bindLong(7, cur.longValue(6));
                    insertDialogSt.bindInteger(8, 0);
                    insertDialogSt.bindInteger(9, cur.intValue(8));
                    insertDialogSt.bindInteger(10, cur.intValue(9));
                    insertDialogSt.bindInteger(11, cur.intValue(10));
                    insertDialogSt.bindInteger(12, cur.intValue(11));
                    insertDialogSt.bindInteger(13, cur.intValue(12));
                    bindBlobFromBufferOrNull(insertDialogSt, 14, cur, 13, toReuse);
                    insertDialogSt.bindInteger(15, 0);
                    insertDialogSt.bindNull(16);
                    insertDialogSt.bindInteger(17, cur.intValue(16));
                    insertDialogSt.bindInteger(18, 0);
                    insertDialogSt.step();
                    reuseAll(toReuse);
                } else if (vaultLastMid > mainLastMid) {
                    updateLastSt.requery();
                    updateLastSt.bindInteger(1, vaultLastMid);
                    updateLastSt.bindInteger(2, Math.max(mainDate, vaultDate));
                    updateLastSt.bindLong(3, did);
                    updateLastSt.step();
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        } finally {
            reuseAll(toReuse);
            if (insertDialogSt != null) {
                insertDialogSt.dispose();
            }
            if (updateLastSt != null) {
                updateLastSt.dispose();
            }
            if (cur != null) {
                cur.dispose();
            }
        }
    }

    /**
     * Permanently deletes user-removed messages from the Vault. Never invoked on logout or cache clean.
     */
    public static void deleteMessages(long ownerUserId, long dialogId, Collection<Integer> mids) {
        if (ownerUserId == 0 || dialogId == 0 || mids == null || mids.isEmpty()) {
            return;
        }
        synchronized (VAULT_LOCK) {
            if (!hasVaultForUser(ownerUserId)) {
                return;
            }
            SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, false);
            if (vaultDb == null) {
                return;
            }
            String idsStr = TextUtils.join(",", mids);
            if (TextUtils.isEmpty(idsStr)) {
                return;
            }
            SQLiteCursor countCur = null;
            try {
                vaultDb.executeFast(String.format(Locale.US,
                        "DELETE FROM saved_messages WHERE uid = %d AND mid IN(%s)", dialogId, idsStr)).stepThis().dispose();

                int remaining = 0;
                countCur = vaultDb.queryFinalized(String.format(Locale.US,
                        "SELECT COUNT(*) FROM saved_messages WHERE uid = %d", dialogId));
                if (countCur.next()) {
                    remaining = countCur.intValue(0);
                }
                countCur.dispose();
                countCur = null;

                if (remaining == 0) {
                    vaultDb.executeFast(String.format(Locale.US, "DELETE FROM saved_dialogs WHERE did = %d", dialogId)).stepThis().dispose();
                    getDialogsWithMessagesLocked(ownerUserId).remove(dialogId);
                } else {
                    SQLiteCursor maxCur = null;
                    SQLitePreparedStatement upSt = null;
                    try {
                        maxCur = vaultDb.queryFinalized(String.format(Locale.US,
                                "SELECT mid, date FROM saved_messages WHERE uid = %d AND mid > 0 ORDER BY mid DESC LIMIT 1", dialogId));
                        if (maxCur.next()) {
                            upSt = vaultDb.executeFast("UPDATE saved_dialogs SET last_mid = ?, date = MAX(date, ?) WHERE did = ?");
                            upSt.bindInteger(1, maxCur.intValue(0));
                            upSt.bindInteger(2, maxCur.intValue(1));
                            upSt.bindLong(3, dialogId);
                            upSt.step();
                        }
                    } finally {
                        if (upSt != null) {
                            upSt.dispose();
                        }
                        if (maxCur != null) {
                            maxCur.dispose();
                        }
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (countCur != null) {
                    countCur.dispose();
                }
            }
        }
    }

    /**
     * Permanently deletes a cleared/deleted dialog from the Vault. Only invoked on real deleteDialog(messagesOnly == 0 || 1).
     */
    public static void deleteDialog(long ownerUserId, long dialogId) {
        if (ownerUserId == 0 || dialogId == 0) {
            return;
        }
        synchronized (VAULT_LOCK) {
            if (!hasVaultForUser(ownerUserId)) {
                return;
            }
            SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, false);
            if (vaultDb == null) {
                return;
            }
            try {
                vaultDb.executeFast("DELETE FROM saved_messages WHERE uid = " + dialogId).stepThis().dispose();
                vaultDb.executeFast("DELETE FROM saved_dialogs WHERE did = " + dialogId).stepThis().dispose();
                getDialogsWithMessagesLocked(ownerUserId).remove(dialogId);
            } catch (Throwable t) {
                FileLog.e(t);
            }
        }
    }

    private static TLRPC.Message readSavedMessageRow(SQLiteCursor cur, long ownerUserId, long dialogId) {
        NativeByteBuffer data = null;
        try {
            data = cur.byteBufferValue(0);
            if (data == null) {
                return null;
            }
            TLRPC.Message message = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
            if (message == null) {
                return null;
            }
            message.send_state = cur.intValue(1);
            message.id = cur.intValue(2);
            if (message.id > 0 && message.send_state != 0 && message.send_state != 3) {
                message.send_state = 0;
            }
            if (dialogId == ownerUserId) {
                message.out = true;
            }
            message.readAttachPath(data, ownerUserId);
            data.reuse();
            data = null;

            MessageObject.setUnreadFlags(message, cur.intValue(3));
            message.date = cur.intValue(4);
            message.dialog_id = dialogId;

            if (!cur.isNull(5)) {
                NativeByteBuffer replyData = cur.byteBufferValue(5);
                if (replyData != null) {
                    if (message.reply_to != null && (message.reply_to.reply_to_msg_id != 0 || message.reply_to.reply_to_random_id != 0)) {
                        message.replyMessage = TLRPC.Message.TLdeserialize(replyData, replyData.readInt32(false), false);
                        if (message.replyMessage != null) {
                            message.replyMessage.readAttachPath(replyData, ownerUserId);
                        }
                    }
                    replyData.reuse();
                }
            }
            if (!DialogObject.isEncryptedDialog(dialogId) && message.ttl == 0) {
                message.ttl = cur.intValue(6);
            }
            if (cur.intValue(7) != 0) {
                message.mentioned = true;
            }
            int imp = cur.intValue(8);
            if ((imp & 1) != 0) {
                message.stickerVerified = 0;
            } else if ((imp & 2) != 0) {
                message.stickerVerified = 2;
            }
            if ((message.flags & TLRPC.MESSAGE_FLAG_HAS_VIEWS) != 0) {
                message.views = cur.intValue(9);
                message.forwards = cur.intValue(10);
            }
            if (!cur.isNull(11)) {
                NativeByteBuffer repliesData = cur.byteBufferValue(11);
                if (repliesData != null) {
                    TLRPC.MessageReplies replies = TLRPC.MessageReplies.TLdeserialize(repliesData, repliesData.readInt32(false), false);
                    if (replies != null) {
                        message.replies = replies;
                    }
                    repliesData.reuse();
                }
            }
            if (!cur.isNull(12)) {
                NativeByteBuffer customParams = cur.byteBufferValue(12);
                if (customParams != null) {
                    MessageCustomParamsHelper.readLocalParams(message, customParams);
                    customParams.reuse();
                }
            }
            message.isAntiDeleted = true;
            return message;
        } catch (Throwable t) {
            FileLog.e(t);
            return null;
        } finally {
            if (data != null) {
                data.reuse();
            }
        }
    }

    private static void loadMissingPeersLocked(
            SQLiteDatabase vaultDb,
            ArrayList<Long> userIds,
            ArrayList<Long> chatIds,
            LongSparseArray<TLRPC.User> usersDict,
            LongSparseArray<TLRPC.Chat> chatsDict,
            ArrayList<TLRPC.User> outUsers,
            ArrayList<TLRPC.Chat> outChats) {
        if (!userIds.isEmpty()) {
            ArrayList<Long> missingUsers = new ArrayList<>();
            for (int i = 0; i < userIds.size(); i++) {
                long uid = userIds.get(i);
                if (uid != 0 && (usersDict == null || usersDict.get(uid) == null) && !missingUsers.contains(uid)) {
                    missingUsers.add(uid);
                }
            }
            if (!missingUsers.isEmpty()) {
                SQLiteCursor uCur = null;
                try {
                    uCur = vaultDb.queryFinalized(String.format(Locale.US,
                            "SELECT data FROM saved_users WHERE uid IN(%s)", TextUtils.join(",", missingUsers)));
                    while (uCur.next()) {
                        NativeByteBuffer uBuf = uCur.byteBufferValue(0);
                        if (uBuf != null) {
                            TLRPC.User u = TLRPC.User.TLdeserialize(uBuf, uBuf.readInt32(false), false);
                            uBuf.reuse();
                            if (u != null) {
                                if (usersDict != null) {
                                    usersDict.put(u.id, u);
                                }
                                if (outUsers != null) {
                                    outUsers.add(u);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    FileLog.e(t);
                } finally {
                    if (uCur != null) {
                        uCur.dispose();
                    }
                }
            }
        }

        if (!chatIds.isEmpty()) {
            ArrayList<Long> missingChats = new ArrayList<>();
            for (int i = 0; i < chatIds.size(); i++) {
                long cid = chatIds.get(i);
                if (cid != 0 && (chatsDict == null || chatsDict.get(cid) == null) && !missingChats.contains(cid)) {
                    missingChats.add(cid);
                }
            }
            if (!missingChats.isEmpty()) {
                SQLiteCursor cCur = null;
                try {
                    cCur = vaultDb.queryFinalized(String.format(Locale.US,
                            "SELECT data FROM saved_chats WHERE uid IN(%s)", TextUtils.join(",", missingChats)));
                    while (cCur.next()) {
                        NativeByteBuffer cBuf = cCur.byteBufferValue(0);
                        if (cBuf != null) {
                            TLRPC.Chat c = TLRPC.Chat.TLdeserialize(cBuf, cBuf.readInt32(false), false);
                            cBuf.reuse();
                            if (c != null) {
                                if (chatsDict != null) {
                                    chatsDict.put(c.id, c);
                                }
                                if (outChats != null) {
                                    outChats.add(c);
                                }
                            }
                        }
                    }
                } catch (Throwable t) {
                    FileLog.e(t);
                } finally {
                    if (cCur != null) {
                        cCur.dispose();
                    }
                }
            }
        }
    }

    /**
     * Merges persisted Anti-Delete messages falling within a server history page [minMid..maxMid]
     * into serverMessages during MessagesController.processLoadedMessages(!isCache), so deleted
     * messages immediately appear chronologically even on the first load after fresh login.
     */
    public static boolean mergeServerWindowMessages(
            long ownerUserId,
            long dialogId,
            long topicId,
            int loadType,
            int maxIdParam,
            int requestedCount,
            TLRPC.messages_Messages messagesRes,
            LongSparseArray<TLRPC.User> usersDict,
            LongSparseArray<TLRPC.Chat> chatsDict,
            MessagesController controller) {
        if (ownerUserId == 0 || dialogId == 0 || messagesRes == null || DialogObject.isEncryptedDialog(dialogId)) {
            return false;
        }
        if (!hasMessagesForDialog(ownerUserId, dialogId)) {
            return false;
        }
        if (loadType != MessagesController.LOAD_BACKWARD
                && loadType != MessagesController.LOAD_FORWARD
                && loadType != MessagesController.LOAD_FROM_UNREAD
                && loadType != MessagesController.LOAD_AROUND_MESSAGE
                && loadType != MessagesController.LOAD_AROUND_DATE) {
            return false;
        }

        int serverSize = messagesRes.messages.size();
        int minWin;
        int maxWin;
        int limit = 100;
        if (serverSize == 0) {
            if (loadType == MessagesController.LOAD_BACKWARD) {
                minWin = 1;
                maxWin = maxIdParam > 0 ? maxIdParam - 1 : Integer.MAX_VALUE;
                limit = Math.max(30, requestedCount);
            } else {
                return false;
            }
        } else {
            int minServerId = Integer.MAX_VALUE;
            int maxServerId = Integer.MIN_VALUE;
            for (int i = 0; i < serverSize; i++) {
                TLRPC.Message m = messagesRes.messages.get(i);
                if (m != null && m.id > 0) {
                    if (m.id < minServerId) minServerId = m.id;
                    if (m.id > maxServerId) maxServerId = m.id;
                }
            }
            if (minServerId == Integer.MAX_VALUE) {
                return false;
            }
            if (loadType == MessagesController.LOAD_BACKWARD) {
                maxWin = maxIdParam > 0 ? Math.max(maxIdParam - 1, maxServerId) : Integer.MAX_VALUE;
                minWin = serverSize < requestedCount ? 1 : minServerId;
            } else if (loadType == MessagesController.LOAD_FORWARD) {
                minWin = maxIdParam > 0 ? Math.min(maxIdParam + 1, minServerId) : minServerId;
                maxWin = serverSize < requestedCount ? Integer.MAX_VALUE : maxServerId;
            } else {
                minWin = minServerId;
                maxWin = maxServerId;
            }
        }

        if (minWin > maxWin) {
            return false;
        }

        HashSet<Integer> existingIds = new HashSet<>(serverSize + 4);
        for (int i = 0; i < serverSize; i++) {
            TLRPC.Message m = messagesRes.messages.get(i);
            if (m != null) {
                existingIds.add(m.id);
            }
        }

        ArrayList<TLRPC.Message> added = new ArrayList<>();
        ArrayList<Long> userIds = new ArrayList<>();
        ArrayList<Long> chatIds = new ArrayList<>();

        synchronized (VAULT_LOCK) {
            SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, false);
            if (vaultDb == null) {
                return false;
            }
            SQLiteCursor cur = null;
            try {
                cur = vaultDb.queryFinalized(String.format(Locale.US,
                        "SELECT data, send_state, mid, read_state, date, replydata, ttl, mention, imp, media, forwards, replies_data, custom_params "
                                + "FROM saved_messages WHERE uid = %d AND topic_id = %d AND mid >= %d AND mid <= %d ORDER BY mid DESC LIMIT %d",
                        dialogId, topicId, minWin, maxWin, limit));
                while (cur.next()) {
                    int mid = cur.intValue(2);
                    if (existingIds.contains(mid)) {
                        continue;
                    }
                    if (controller != null && controller.isLocallyDeleted(dialogId, mid)) {
                        continue;
                    }
                    TLRPC.Message msg = readSavedMessageRow(cur, ownerUserId, dialogId);
                    if (msg == null) {
                        continue;
                    }
                    existingIds.add(mid);
                    added.add(msg);
                    MessagesStorage.addUsersAndChatsFromMessage(msg, userIds, chatIds, null);
                    if (msg.replyMessage != null) {
                        MessagesStorage.addUsersAndChatsFromMessage(msg.replyMessage, userIds, chatIds, null);
                    }
                }
                cur.dispose();
                cur = null;

                if (!added.isEmpty()) {
                    loadMissingPeersLocked(vaultDb, userIds, chatIds, usersDict, chatsDict, messagesRes.users, messagesRes.chats);
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (cur != null) {
                    cur.dispose();
                }
            }
        }

        if (added.isEmpty()) {
            return false;
        }
        messagesRes.messages.addAll(added);
        Collections.sort(messagesRes.messages, (lhs, rhs) -> Integer.compare(rhs.id, lhs.id));
        return true;
    }

    /**
     * Merges missing/cleared dialogs and promotes any deleted latest preview message on fresh
     * server dialog loads (MessagesController.processLoadedDialogs) after pagination cursors are saved.
     */
    public static void mergeSavedDialogsForFolder(
            long ownerUserId,
            int folderId,
            TLRPC.messages_Dialogs dialogsRes,
            LongSparseArray<TLRPC.User> usersDict,
            LongSparseArray<TLRPC.Chat> chatsDict,
            LongSparseArray<ArrayList<MessageObject>> newDialogMessages,
            int currentAccount) {
        if (ownerUserId == 0 || dialogsRes == null || (folderId != 0 && folderId != 1)) {
            return;
        }
        if (!NayaConfig.getInstance().isPreventMessageDeletionEnabled() || !hasVaultForUser(ownerUserId)) {
            return;
        }

        LongSparseArray<TLRPC.Dialog> byDid = new LongSparseArray<>(dialogsRes.dialogs.size());
        for (int i = 0; i < dialogsRes.dialogs.size(); i++) {
            TLRPC.Dialog d = dialogsRes.dialogs.get(i);
            if (d != null) {
                DialogObject.initDialog(d);
                if (d.id != 0) {
                    byDid.put(d.id, d);
                }
            }
        }

        synchronized (VAULT_LOCK) {
            SQLiteDatabase vaultDb = openVaultLocked(ownerUserId, false);
            if (vaultDb == null) {
                return;
            }
            SQLiteCursor cur = null;
            ArrayList<Long> userIds = new ArrayList<>();
            ArrayList<Long> chatIds = new ArrayList<>();
            ArrayList<TLRPC.Message> addedMessages = new ArrayList<>();
            try {
                cur = vaultDb.queryFinalized(String.format(Locale.US,
                        "SELECT did, date, last_mid, inbox_max, outbox_max, pts, pinned, flags, folder_id, ttl_period "
                                + "FROM saved_dialogs WHERE folder_id = %d", folderId));
                while (cur.next()) {
                    long did = cur.longValue(0);
                    if (did == 0 || DialogObject.isEncryptedDialog(did) || DialogObject.isFolderDialogId(did)) {
                        continue;
                    }
                    SQLiteCursor msgCur = null;
                    TLRPC.Message latestMsg = null;
                    try {
                        msgCur = vaultDb.queryFinalized(String.format(Locale.US,
                                "SELECT data, send_state, mid, read_state, date, replydata, ttl, mention, imp, media, forwards, replies_data, custom_params "
                                        + "FROM saved_messages WHERE uid = %d AND topic_id = 0 AND mid > 0 ORDER BY mid DESC LIMIT 1", did));
                        if (msgCur.next()) {
                            latestMsg = readSavedMessageRow(msgCur, ownerUserId, did);
                        }
                    } finally {
                        if (msgCur != null) {
                            msgCur.dispose();
                        }
                    }
                    if (latestMsg == null) {
                        continue;
                    }

                    TLRPC.Dialog existing = byDid.get(did);
                    if (existing == null) {
                        TLRPC.TL_dialog d = new TLRPC.TL_dialog();
                        d.id = did;
                        d.peer = MessagesController.getInstance(currentAccount).getPeer(did);
                        d.last_message_date = Math.max(cur.intValue(1), latestMsg.date);
                        d.top_message = Math.max(cur.intValue(2), latestMsg.id);
                        d.read_inbox_max_id = cur.intValue(3);
                        d.read_outbox_max_id = cur.intValue(4);
                        d.pts = cur.intValue(5);
                        d.pinnedNum = cur.intValue(6);
                        d.pinned = d.pinnedNum != 0;
                        int dFlags = cur.intValue(7);
                        d.unread_mark = (dFlags & 1) != 0;
                        d.view_forum_as_messages = (dFlags & 64) != 0;
                        d.folder_id = cur.intValue(8);
                        d.ttl_period = cur.intValue(9);
                        d.unread_count = 0;

                        dialogsRes.dialogs.add(d);
                        dialogsRes.messages.add(latestMsg);
                        addedMessages.add(latestMsg);
                        byDid.put(did, d);
                        collectDialogPeer(did, userIds, chatIds);
                        MessagesStorage.addUsersAndChatsFromMessage(latestMsg, userIds, chatIds, null);
                    } else if (latestMsg.id > existing.top_message) {
                        existing.top_message = latestMsg.id;
                        existing.last_message_date = Math.max(existing.last_message_date, latestMsg.date);
                        dialogsRes.messages.add(latestMsg);
                        addedMessages.add(latestMsg);
                        collectDialogPeer(did, userIds, chatIds);
                        MessagesStorage.addUsersAndChatsFromMessage(latestMsg, userIds, chatIds, null);
                    }
                }
                cur.dispose();
                cur = null;

                if (!addedMessages.isEmpty()) {
                    loadMissingPeersLocked(vaultDb, userIds, chatIds, usersDict, chatsDict, dialogsRes.users, dialogsRes.chats);
                    for (int i = 0; i < addedMessages.size(); i++) {
                        TLRPC.Message msg = addedMessages.get(i);
                        ArrayList<MessageObject> arr = new ArrayList<>(1);
                        arr.add(new MessageObject(currentAccount, msg, usersDict, chatsDict, false, false));
                        newDialogMessages.put(msg.dialog_id, arr);
                    }
                }
            } catch (Throwable t) {
                FileLog.e(t);
            } finally {
                if (cur != null) {
                    cur.dispose();
                }
            }
        }
    }
}
