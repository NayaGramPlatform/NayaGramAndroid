package org.nayagram.platform;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.content.Context;

import java.util.ArrayList;
import java.util.List;

/**
 * MessageDatabase - Local database for storing deleted messages
 * Extends existing database to store anti-delete data
 */
public class MessageDatabase extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "nayagram_messages.db";
    private static final int DATABASE_VERSION = 2;
    
    private static final String TABLE_DELETED_MESSAGES = "deleted_messages";
    private static final String TABLE_ANTI_DELETE_SETTINGS = "anti_delete_settings";
    
    // Deleted Messages Table Columns
    private static final String COL_MESSAGE_ID = "message_id";
    private static final String COL_CHAT_ID = "chat_id";
    private static final String COL_MESSAGE_TEXT = "message_text";
    private static final String COL_SENDER_ID = "sender_id";
    private static final String COL_ORIGINAL_TIMESTAMP = "original_timestamp";
    private static final String COL_DELETED_TIMESTAMP = "deleted_timestamp";
    private static final String COL_IS_RESTORED = "is_restored";
    private static final String COL_MEDIA_PATH = "media_path";
    
    // Anti-Delete Settings Table Columns
    private static final String COL_SETTING_KEY = "setting_key";
    private static final String COL_SETTING_VALUE = "setting_value";
    
    private static MessageDatabase instance;
    private SQLiteDatabase db;
    
    private MessageDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized MessageDatabase getInstance() {
        if (instance == null) {
            instance = new MessageDatabase(ApplicationLoader.applicationContext);
        }
        return instance;
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create deleted messages table
        String createDeletedMessagesTable = "CREATE TABLE IF NOT EXISTS " + TABLE_DELETED_MESSAGES + " (" +
                COL_MESSAGE_ID + " INTEGER PRIMARY KEY," +
                COL_CHAT_ID + " INTEGER NOT NULL," +
                COL_MESSAGE_TEXT + " TEXT," +
                COL_SENDER_ID + " INTEGER," +
                COL_ORIGINAL_TIMESTAMP + " INTEGER," +
                COL_DELETED_TIMESTAMP + " INTEGER," +
                COL_IS_RESTORED + " INTEGER DEFAULT 0," +
                COL_MEDIA_PATH + " TEXT," +
                "UNIQUE(" + COL_MESSAGE_ID + ", " + COL_CHAT_ID + "))";
        
        db.execSQL(createDeletedMessagesTable);
        
        // Create anti-delete settings table
        String createSettingsTable = "CREATE TABLE IF NOT EXISTS " + TABLE_ANTI_DELETE_SETTINGS + " (" +
                "id INTEGER PRIMARY KEY," +
                COL_SETTING_KEY + " TEXT UNIQUE," +
                COL_SETTING_VALUE + " TEXT)";
        
        db.execSQL(createSettingsTable);
        
        // Create index for faster queries
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_chat_id ON " + TABLE_DELETED_MESSAGES + 
                "(" + COL_CHAT_ID + ")");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_deleted_timestamp ON " + TABLE_DELETED_MESSAGES + 
                "(" + COL_DELETED_TIMESTAMP + ")");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Add new columns for media support in version 2
            try {
                db.execSQL("ALTER TABLE " + TABLE_DELETED_MESSAGES + 
                        " ADD COLUMN " + COL_MEDIA_PATH + " TEXT");
            } catch (Exception e) {
                FileLog.e(e);
            }
        }
    }
    
    /**
     * Save a deleted message to database
     */
    public boolean saveDeletedMessage(long messageId, long chatId, String messageText,
                                     long senderId, long timestamp) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_MESSAGE_ID, messageId);
            values.put(COL_CHAT_ID, chatId);
            values.put(COL_MESSAGE_TEXT, messageText);
            values.put(COL_SENDER_ID, senderId);
            values.put(COL_ORIGINAL_TIMESTAMP, timestamp);
            values.put(COL_DELETED_TIMESTAMP, System.currentTimeMillis() / 1000);
            values.put(COL_IS_RESTORED, 0);
            
            long result = db.insertWithOnConflict(TABLE_DELETED_MESSAGES, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            return result != -1;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Save deleted message with media path
     */
    public boolean saveDeletedMessageWithMedia(long messageId, long chatId, String messageText,
                                              long senderId, long timestamp, String mediaPath) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_MESSAGE_ID, messageId);
            values.put(COL_CHAT_ID, chatId);
            values.put(COL_MESSAGE_TEXT, messageText);
            values.put(COL_SENDER_ID, senderId);
            values.put(COL_ORIGINAL_TIMESTAMP, timestamp);
            values.put(COL_DELETED_TIMESTAMP, System.currentTimeMillis() / 1000);
            values.put(COL_IS_RESTORED, 0);
            values.put(COL_MEDIA_PATH, mediaPath);
            
            long result = db.insertWithOnConflict(TABLE_DELETED_MESSAGES, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
            return result != -1;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Get all deleted messages for a specific chat
     */
    public List<AntiDeleteManager.DeletedMessage> getDeletedMessages(long chatId) {
        List<AntiDeleteManager.DeletedMessage> messages = new ArrayList<>();
        try {
            SQLiteDatabase db = getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_DELETED_MESSAGES +
                    " WHERE " + COL_CHAT_ID + " = ? AND " + COL_IS_RESTORED + " = 0" +
                    " ORDER BY " + COL_DELETED_TIMESTAMP + " DESC";
            
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(chatId)});
            
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    long messageId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_MESSAGE_ID));
                    String messageText = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_TEXT));
                    long senderId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SENDER_ID));
                    long originalTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ORIGINAL_TIMESTAMP));
                    long deletedTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DELETED_TIMESTAMP));
                    
                    AntiDeleteManager.DeletedMessage msg = new AntiDeleteManager.DeletedMessage(
                            messageId, chatId, messageText, senderId, originalTime, deletedTime
                    );
                    messages.add(msg);
                }
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return messages;
    }
    
    /**
     * Get a specific deleted message
     */
    public AntiDeleteManager.DeletedMessage getDeletedMessage(long messageId) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            String query = "SELECT * FROM " + TABLE_DELETED_MESSAGES +
                    " WHERE " + COL_MESSAGE_ID + " = ?";
            
            Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(messageId)});
            
            if (cursor != null && cursor.moveToFirst()) {
                long chatId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_CHAT_ID));
                String messageText = cursor.getString(cursor.getColumnIndexOrThrow(COL_MESSAGE_TEXT));
                long senderId = cursor.getLong(cursor.getColumnIndexOrThrow(COL_SENDER_ID));
                long originalTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_ORIGINAL_TIMESTAMP));
                long deletedTime = cursor.getLong(cursor.getColumnIndexOrThrow(COL_DELETED_TIMESTAMP));
                
                cursor.close();
                
                return new AntiDeleteManager.DeletedMessage(
                        messageId, chatId, messageText, senderId, originalTime, deletedTime
                );
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return null;
    }
    
    /**
     * Restore a deleted message
     */
    public boolean restoreDeletedMessage(long messageId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_IS_RESTORED, 1);
            
            int result = db.update(TABLE_DELETED_MESSAGES, values,
                    COL_MESSAGE_ID + " = ?", new String[]{String.valueOf(messageId)});
            return result > 0;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Permanently delete a message
     */
    public boolean permanentlyDeleteMessage(long messageId) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            int result = db.delete(TABLE_DELETED_MESSAGES,
                    COL_MESSAGE_ID + " = ?", new String[]{String.valueOf(messageId)});
            return result > 0;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }
    
    /**
     * Clear all deleted messages
     */
    public void clearAllDeletedMessages() {
        try {
            SQLiteDatabase db = getWritableDatabase();
            db.delete(TABLE_DELETED_MESSAGES, null, null);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
    
    /**
     * Clear deleted messages older than specified days
     */
    public void clearOldDeletedMessages(int days) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            long cutoffTime = (System.currentTimeMillis() / 1000) - (days * 24 * 60 * 60);
            db.delete(TABLE_DELETED_MESSAGES,
                    COL_DELETED_TIMESTAMP + " < ?",
                    new String[]{String.valueOf(cutoffTime)});
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
    
    /**
     * Get count of deleted messages
     */
    public int getDeletedMessageCount() {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.rawQuery(
                    "SELECT COUNT(*) FROM " + TABLE_DELETED_MESSAGES +
                    " WHERE " + COL_IS_RESTORED + " = 0", null);
            
            if (cursor != null && cursor.moveToFirst()) {
                int count = cursor.getInt(0);
                cursor.close();
                return count;
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return 0;
    }
    
    /**
     * Save anti-delete setting
     */
    public void saveSetting(String key, String value) {
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COL_SETTING_KEY, key);
            values.put(COL_SETTING_VALUE, value);
            
            db.insertWithOnConflict(TABLE_ANTI_DELETE_SETTINGS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
    
    /**
     * Get anti-delete setting
     */
    public String getSetting(String key, String defaultValue) {
        try {
            SQLiteDatabase db = getReadableDatabase();
            Cursor cursor = db.query(TABLE_ANTI_DELETE_SETTINGS,
                    new String[]{COL_SETTING_VALUE},
                    COL_SETTING_KEY + " = ?",
                    new String[]{key},
                    null, null, null);
            
            if (cursor != null && cursor.moveToFirst()) {
                String value = cursor.getString(0);
                cursor.close();
                return value;
            }
            if (cursor != null) {
                cursor.close();
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return defaultValue;
    }
    
    /**
     * Close database connection
     */
    public void closeDatabase() {
        if (db != null && db.isOpen()) {
            db.close();
        }
    }
}
