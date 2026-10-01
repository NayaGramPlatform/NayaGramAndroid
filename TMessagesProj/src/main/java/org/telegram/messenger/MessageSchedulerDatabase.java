package org.telegram.messenger;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * MessageSchedulerDatabase - SQLite database for storing scheduled messages
 */
public class MessageSchedulerDatabase extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "message_scheduler.db";
    private static final int DATABASE_VERSION = 1;
    
    private static final String TABLE_SCHEDULED_MESSAGES = "scheduled_messages";
    
    // Columns for scheduled messages table
    private static final String COLUMN_ID = "id";
    private static final String COLUMN_DIALOG_ID = "dialog_id";
    private static final String COLUMN_MESSAGE_TEXT = "message_text";
    private static final String COLUMN_SCHEDULE_TIME = "schedule_time";
    private static final String COLUMN_ACCOUNT_ID = "account_id";
    private static final String COLUMN_STATUS = "status";
    private static final String COLUMN_CREATED_AT = "created_at";
    private static final String COLUMN_SENT_AT = "sent_at";
    
    private static MessageSchedulerDatabase instance;
    private static final String TAG = "MessageSchedulerDatabase";
    
    private MessageSchedulerDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized MessageSchedulerDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new MessageSchedulerDatabase(context);
        }
        return instance;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create scheduled messages table
        String createTableSQL = "CREATE TABLE " + TABLE_SCHEDULED_MESSAGES + " (" +
                COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COLUMN_DIALOG_ID + " INTEGER NOT NULL," +
                COLUMN_MESSAGE_TEXT + " TEXT NOT NULL," +
                COLUMN_SCHEDULE_TIME + " INTEGER NOT NULL," +
                COLUMN_ACCOUNT_ID + " INTEGER NOT NULL," +
                COLUMN_STATUS + " INTEGER DEFAULT 0," +
                COLUMN_CREATED_AT + " INTEGER NOT NULL," +
                COLUMN_SENT_AT + " INTEGER DEFAULT 0" +
                ")";
        
        db.execSQL(createTableSQL);
        
        // Create index for faster queries
        db.execSQL("CREATE INDEX idx_account_status ON " + TABLE_SCHEDULED_MESSAGES + 
                "(" + COLUMN_ACCOUNT_ID + "," + COLUMN_STATUS + ")");
        db.execSQL("CREATE INDEX idx_schedule_time ON " + TABLE_SCHEDULED_MESSAGES + 
                "(" + COLUMN_SCHEDULE_TIME + ")");
        
        FileLog.d(TAG + ": Database tables created");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle database upgrades
        if (oldVersion < newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_SCHEDULED_MESSAGES);
            onCreate(db);
        }
    }
    
    /**
     * Insert scheduled message
     */
    public long insertScheduledMessage(MessageScheduler.ScheduledMessage message) {
        SQLiteDatabase db = getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_DIALOG_ID, message.dialogId);
        values.put(COLUMN_MESSAGE_TEXT, message.messageText);
        values.put(COLUMN_SCHEDULE_TIME, message.scheduleTime);
        values.put(COLUMN_ACCOUNT_ID, message.accountId);
        values.put(COLUMN_STATUS, message.status);
        values.put(COLUMN_CREATED_AT, message.createdAt);
        values.put(COLUMN_SENT_AT, message.sentAt);
        
        long id = db.insert(TABLE_SCHEDULED_MESSAGES, null, values);
        FileLog.d(TAG + ": Inserted scheduled message with id " + id);
        return id;
    }
    
    /**
     * Update scheduled message
     */
    public void updateScheduledMessage(MessageScheduler.ScheduledMessage message) {
        SQLiteDatabase db = getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COLUMN_STATUS, message.status);
        values.put(COLUMN_SENT_AT, message.sentAt);
        
        int rows = db.update(
                TABLE_SCHEDULED_MESSAGES,
                values,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(message.id)}
        );
        
        FileLog.d(TAG + ": Updated " + rows + " scheduled message(s)");
    }
    
    /**
     * Get scheduled message by ID
     */
    public MessageScheduler.ScheduledMessage getScheduledMessage(long messageId) {
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.query(
                TABLE_SCHEDULED_MESSAGES,
                null,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(messageId)},
                null,
                null,
                null
        );
        
        MessageScheduler.ScheduledMessage message = null;
        if (cursor.moveToFirst()) {
            message = cursorToMessage(cursor);
        }
        cursor.close();
        
        return message;
    }
    
    /**
     * Get all pending messages for account
     */
    public List<MessageScheduler.ScheduledMessage> getPendingMessages(int accountId) {
        SQLiteDatabase db = getReadableDatabase();
        List<MessageScheduler.ScheduledMessage> messages = new ArrayList<>();
        
        Cursor cursor = db.query(
                TABLE_SCHEDULED_MESSAGES,
                null,
                COLUMN_ACCOUNT_ID + " = ? AND " + COLUMN_STATUS + " = ?",
                new String[]{String.valueOf(accountId), String.valueOf(MessageScheduler.ScheduledMessage.STATUS_PENDING)},
                null,
                null,
                COLUMN_SCHEDULE_TIME + " ASC"
        );
        
        while (cursor.moveToNext()) {
            messages.add(cursorToMessage(cursor));
        }
        cursor.close();
        
        return messages;
    }
    
    /**
     * Delete scheduled message
     */
    public void deleteScheduledMessage(long messageId) {
        SQLiteDatabase db = getWritableDatabase();
        
        int rows = db.delete(
                TABLE_SCHEDULED_MESSAGES,
                COLUMN_ID + " = ?",
                new String[]{String.valueOf(messageId)}
        );
        
        FileLog.d(TAG + ": Deleted " + rows + " scheduled message(s)");
    }
    
    /**
     * Get scheduled messages count
     */
    public int getScheduledMessagesCount(int accountId) {
        SQLiteDatabase db = getReadableDatabase();
        
        Cursor cursor = db.rawQuery(
                "SELECT COUNT(*) FROM " + TABLE_SCHEDULED_MESSAGES +
                " WHERE " + COLUMN_ACCOUNT_ID + " = ? AND " + COLUMN_STATUS + " = ?",
                new String[]{String.valueOf(accountId), String.valueOf(MessageScheduler.ScheduledMessage.STATUS_PENDING)}
        );
        
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        
        return count;
    }
    
    /**
     * Convert cursor to scheduled message object
     */
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
    
    /**
     * Clear old scheduled messages (older than 30 days)
     */
    public void clearOldMessages() {
        SQLiteDatabase db = getWritableDatabase();
        long thirtyDaysAgo = (System.currentTimeMillis() / 1000) - (30 * 24 * 60 * 60);
        
        int rows = db.delete(
                TABLE_SCHEDULED_MESSAGES,
                COLUMN_CREATED_AT + " < ?",
                new String[]{String.valueOf(thirtyDaysAgo)}
        );
        
        FileLog.d(TAG + ": Cleared " + rows + " old scheduled message(s)");
    }
}
