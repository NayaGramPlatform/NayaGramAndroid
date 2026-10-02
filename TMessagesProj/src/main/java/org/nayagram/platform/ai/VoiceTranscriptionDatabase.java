package org.nayagram.platform.ai;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

/**
 * VoiceTranscriptionDatabase - Local SQLite storage for transcriptions
 * Privacy-first design: All data stored locally, never synced to cloud
 */
public class VoiceTranscriptionDatabase extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "voice_transcriptions.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_TRANSCRIPTIONS = "transcriptions";
    
    // Table columns
    private static final String COL_MESSAGE_ID = "message_id";
    private static final String COL_DIALOG_ID = "dialog_id";
    private static final String COL_TEXT = "transcription_text";
    private static final String COL_LANGUAGE = "language_code";
    private static final String COL_CONFIDENCE = "confidence_score";
    private static final String COL_PROCESSING_TIME = "processing_time_ms";
    private static final String COL_TIMESTAMP = "timestamp";
    
    private static VoiceTranscriptionDatabase instance;
    
    public static synchronized VoiceTranscriptionDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new VoiceTranscriptionDatabase(context);
        }
        return instance;
    }
    
    private VoiceTranscriptionDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTableSQL = "CREATE TABLE " + TABLE_TRANSCRIPTIONS + " (" +
                COL_MESSAGE_ID + " INTEGER PRIMARY KEY," +
                COL_DIALOG_ID + " INTEGER NOT NULL," +
                COL_TEXT + " TEXT NOT NULL," +
                COL_LANGUAGE + " TEXT," +
                COL_CONFIDENCE + " REAL," +
                COL_PROCESSING_TIME + " INTEGER," +
                COL_TIMESTAMP + " INTEGER" +
                ")";
        
        db.execSQL(createTableSQL);
        db.execSQL("CREATE INDEX idx_dialog_id ON " + TABLE_TRANSCRIPTIONS + 
                "(" + COL_DIALOG_ID + ")");
        db.execSQL("CREATE INDEX idx_text ON " + TABLE_TRANSCRIPTIONS + 
                "(" + COL_TEXT + ")");
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle future database schema updates
    }
    
    /**
     * Save transcription to local database
     */
    public void saveTranscription(VoiceTranscriptionManager.TranscriptionResult result) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COL_MESSAGE_ID, result.messageId);
        values.put(COL_DIALOG_ID, result.dialogId);
        values.put(COL_TEXT, result.text);
        values.put(COL_LANGUAGE, result.languageCode);
        values.put(COL_CONFIDENCE, result.confidence);
        values.put(COL_PROCESSING_TIME, result.processingTime);
        values.put(COL_TIMESTAMP, result.timestamp);
        
        db.insert(TABLE_TRANSCRIPTIONS, null, values);
        db.close();
    }
    
    /**
     * Search transcriptions by text
     */
    public List<VoiceTranscriptionManager.TranscriptionResult> searchTranscriptions(
            String query, long dialogId) {
        
        SQLiteDatabase db = this.getReadableDatabase();
        List<VoiceTranscriptionManager.TranscriptionResult> results = new ArrayList<>();
        
        String selection = COL_DIALOG_ID + " = ? AND " + COL_TEXT + " LIKE ?";
        String[] selectionArgs = {String.valueOf(dialogId), "%" + query + "%"};
        
        Cursor cursor = db.query(TABLE_TRANSCRIPTIONS, null, selection, 
                selectionArgs, null, null, COL_TIMESTAMP + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                VoiceTranscriptionManager.TranscriptionResult result = 
                        new VoiceTranscriptionManager.TranscriptionResult(
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_MESSAGE_ID)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_DIALOG_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT)),
                    cursor.getString(cursor.getColumnIndexOrThrow(COL_LANGUAGE)),
                    cursor.getFloat(cursor.getColumnIndexOrThrow(COL_CONFIDENCE)),
                    cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROCESSING_TIME))
                );
                results.add(result);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return results;
    }
    
    /**
     * Get transcription for specific message
     */
    public VoiceTranscriptionManager.TranscriptionResult getTranscription(long messageId) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_TRANSCRIPTIONS, null, 
                COL_MESSAGE_ID + " = ?", 
                new String[]{String.valueOf(messageId)}, 
                null, null, null);
        
        if (cursor.moveToFirst()) {
            VoiceTranscriptionManager.TranscriptionResult result = 
                    new VoiceTranscriptionManager.TranscriptionResult(
                messageId,
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_DIALOG_ID)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_TEXT)),
                cursor.getString(cursor.getColumnIndexOrThrow(COL_LANGUAGE)),
                cursor.getFloat(cursor.getColumnIndexOrThrow(COL_CONFIDENCE)),
                cursor.getLong(cursor.getColumnIndexOrThrow(COL_PROCESSING_TIME))
            );
            cursor.close();
            db.close();
            return result;
        }
        
        cursor.close();
        db.close();
        return null;
    }
    
    /**
     * Delete transcription
     */
    public void deleteTranscription(long messageId) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSCRIPTIONS, COL_MESSAGE_ID + " = ?", 
                new String[]{String.valueOf(messageId)});
        db.close();
    }
    
    /**
     * Clear all transcriptions
     */
    public void clearAll() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_TRANSCRIPTIONS, null, null);
        db.close();
    }
}
