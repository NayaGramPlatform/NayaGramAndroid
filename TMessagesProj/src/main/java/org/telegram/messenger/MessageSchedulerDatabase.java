package org.telegram.messenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** SQLite storage for scheduled messages. */
public class MessageSchedulerDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "message_scheduler.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_SCHEDULED_MESSAGES = "scheduled_messages";
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DIALOG_ID = "dialog_id";
    private static final String COLUMN_MESSAGE_TEXT = "message_text";
    private static final String COLUMN_SCHEDULE_TIME = "schedule_time";
    private static final String COLUMN_ACCOUNT_ID = "account_id";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_SENT_AT = "sent_at";
    private static MessageSchedulerDatabase instance;

    private MessageSchedulerDatabase(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized MessageSchedulerDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new MessageSchedulerDatabase(context);
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_SCHEDULED_MESSAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_DIALOG_ID + " INTEGER NOT NULL," +
                COLUMN_MESSAGE_TEXT + " TEXT NOT NULL," +
                COLUMN_SCHEDULE_TIME + " INTEGER NOT NULL," +
                COLUMN_ACCOUNT_ID + " INTEGER NOT NULL," +
                COLUMN_STATUS + " INTEGER DEFAULT 0," +
                COLUMN_CREATED_AT + " INTEGER NOT NULL," +
                COLUMN_SENT_AT + " INTEGER DEFAULT 0)");
        db.execSQL("CREATE INDEX idx_account_status ON " + TABLE_SCHEDULED_MESSAGES +
                "(" + COLUMN_ACCOUNT_ID + "," + COLUMN_STATUS + ")");
        db.execSQL("CREATE INDEX idx_schedule_time ON " + TABLE_SCHEDULED_MESSAGES +
                "(" + COLUMN_SCHEDULE_TIME + ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Version 1 schema is already sufficient for scheduler updates.
    }

    public long insertScheduledMessage(MessageScheduler.ScheduledMessage message) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_DIALOG_ID, message.dialogId);
        values.put(COLUMN_MESSAGE_TEXT, message.messageText);
        values.put(COLUMN_SCHEDULE_TIME, message.scheduleTime);
        values.put(COLUMN_ACCOUNT_ID, message.accountId);
        values.put(COLUMN_STATUS, message.status);
        values.put(COLUMN_CREATED_AT, message.createdAt);
        values.put(COLUMN_SENT_AT, message.sentAt);
        long id = getWritableDatabase().insertOrThrow(TABLE_SCHEDULED_MESSAGES, null, values);
        message.id = id;
        return id;
    }

    public void updateScheduledMessage(MessageScheduler.ScheduledMessage message) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_MESSAGE_TEXT, message.messageText);
        values.put(COLUMN_SCHEDULE_TIME, message.scheduleTime);
        values.put(COLUMN_STATUS, message.status);
        values.put(COLUMN_SENT_AT, message.sentAt);
        getWritableDatabase().update(TABLE_SCHEDULED_MESSAGES, values, COLUMN_ID + " = ?",
                new String[]{String.valueOf(message.id)});
    }

    public MessageScheduler.ScheduledMessage getScheduledMessage(long messageId) {
        try (Cursor cursor = getReadableDatabase().query(TABLE_SCHEDULED_MESSAGES, null,
                COLUMN_ID + " = ?", new String[]{String.valueOf(messageId)}, null, null, null)) {
            return cursor.moveToFirst() ? cursorToMessage(cursor) : null;
        }
    }

    public List<MessageScheduler.ScheduledMessage> getPendingMessages(int accountId) {
        List<MessageScheduler.ScheduledMessage> messages = new ArrayList<>();
        try (Cursor cursor = getReadableDatabase().query(TABLE_SCHEDULED_MESSAGES, null,
                COLUMN_ACCOUNT_ID + " = ? AND " + COLUMN_STATUS + " = ?",
                new String[]{String.valueOf(accountId), String.valueOf(MessageScheduler.ScheduledMessage.STATUS_PENDING)},
                null, null, COLUMN_SCHEDULE_TIME + " ASC")) {
            while (cursor.moveToNext()) {
                messages.add(cursorToMessage(cursor));
            }
        }
        return messages;
    }

    public void deleteScheduledMessage(long messageId) {
        getWritableDatabase().delete(TABLE_SCHEDULED_MESSAGES, COLUMN_ID + " = ?",
                new String[]{String.valueOf(messageId)});
    }

    public int getScheduledMessagesCount(int accountId) {
        try (Cursor cursor = getReadableDatabase().rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_SCHEDULED_MESSAGES + " WHERE " +
                        COLUMN_ACCOUNT_ID + " = ? AND " + COLUMN_STATUS + " = ?",
                new String[]{String.valueOf(accountId), String.valueOf(MessageScheduler.ScheduledMessage.STATUS_PENDING)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    private MessageScheduler.ScheduledMessage cursorToMessage(Cursor cursor) {
        MessageScheduler.ScheduledMessage message = new MessageScheduler.ScheduledMessage();
        message.id = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_ID));
        message.dialogId = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_DIALOG_ID));
        message.messageText = cursor.getString(cursor.getColumnIndexOrThrow(COLUMN_MESSAGE_TEXT));
        message.scheduleTime = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SCHEDULE_TIME));
        message.accountId = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_ACCOUNT_ID));
        message.status = cursor.getInt(cursor.getColumnIndexOrThrow(COLUMN_STATUS));
        message.createdAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_CREATED_AT));
        message.sentAt = cursor.getLong(cursor.getColumnIndexOrThrow(COLUMN_SENT_AT));
        return message;
    }

    public void clearOldMessages() {
        long cutoff = System.currentTimeMillis() / 1000 - 30L * 24 * 60 * 60;
        getWritableDatabase().delete(TABLE_SCHEDULED_MESSAGES, COLUMN_CREATED_AT + " < ?",
                new String[]{String.valueOf(cutoff)});
    }
}
