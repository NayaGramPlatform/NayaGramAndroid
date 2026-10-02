package org.nayagram.platform.features;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.Cursor;
import android.content.ContentValues;

import java.util.ArrayList;
import java.util.List;

/**
 * SmartChatFolderDatabase - SQLite storage for chat folder organization
 * Handles user-defined folders, folder-to-chat mappings, and folder preferences
 */
public class SmartChatFolderDatabase extends SQLiteOpenHelper {
    
    private static final String DATABASE_NAME = "smart_chat_folders.db";
    private static final int DATABASE_VERSION = 1;
    
    // Table names
    private static final String TABLE_FOLDERS = "chat_folders";
    private static final String TABLE_FOLDER_MAPPINGS = "folder_mappings";
    private static final String TABLE_FOLDER_FILTERS = "folder_filters";
    
    // Folder columns
    private static final String COL_FOLDER_ID = "folder_id";
    private static final String COL_FOLDER_NAME = "folder_name";
    private static final String COL_FOLDER_COLOR = "folder_color";
    private static final String COL_FOLDER_ICON = "folder_icon";
    private static final String COL_SORT_ORDER = "sort_order";
    private static final String COL_CREATED_AT = "created_at";
    private static final String COL_IS_CUSTOM = "is_custom";
    
    // Folder mapping columns
    private static final String COL_CHAT_ID = "chat_id";
    private static final String COL_IS_PINNED = "is_pinned";
    private static final String COL_ADDED_AT = "added_at";
    
    // Folder filter columns
    private static final String COL_FILTER_TYPE = "filter_type";  // "unread", "muted", "archived"
    private static final String COL_FILTER_ENABLED = "filter_enabled";
    
    private static SmartChatFolderDatabase instance;
    
    public static synchronized SmartChatFolderDatabase getInstance(Context context) {
        if (instance == null) {
            instance = new SmartChatFolderDatabase(context);
        }
        return instance;
    }
    
    private SmartChatFolderDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create folders table
        String createFoldersTable = "CREATE TABLE " + TABLE_FOLDERS + " (" +
                COL_FOLDER_ID + " INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_FOLDER_NAME + " TEXT NOT NULL," +
                COL_FOLDER_COLOR + " TEXT NOT NULL," +
                COL_FOLDER_ICON + " INTEGER NOT NULL," +
                COL_SORT_ORDER + " INTEGER DEFAULT 0," +
                COL_CREATED_AT + " INTEGER," +
                COL_IS_CUSTOM + " INTEGER DEFAULT 1" +
                ")";
        db.execSQL(createFoldersTable);
        
        // Create folder mappings table (many-to-many: chats to folders)
        String createMappingsTable = "CREATE TABLE " + TABLE_FOLDER_MAPPINGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                COL_FOLDER_ID + " INTEGER NOT NULL," +
                COL_CHAT_ID + " INTEGER NOT NULL," +
                COL_IS_PINNED + " INTEGER DEFAULT 0," +
                COL_ADDED_AT + " INTEGER," +
                "UNIQUE(" + COL_FOLDER_ID + ", " + COL_CHAT_ID + ")" +
                ")";
        db.execSQL(createMappingsTable);
        
        // Create filters table
        String createFiltersTable = "CREATE TABLE " + TABLE_FOLDER_FILTERS + " (" +
                COL_FOLDER_ID + " INTEGER PRIMARY KEY," +
                COL_FILTER_TYPE + " TEXT NOT NULL," +
                COL_FILTER_ENABLED + " INTEGER DEFAULT 0" +
                ")";
        db.execSQL(createFiltersTable);
        
        // Create indexes
        db.execSQL("CREATE INDEX idx_folder_name ON " + TABLE_FOLDERS + 
                "(" + COL_FOLDER_NAME + ")");
        db.execSQL("CREATE INDEX idx_chat_id ON " + TABLE_FOLDER_MAPPINGS + 
                "(" + COL_CHAT_ID + ")");
        db.execSQL("CREATE INDEX idx_folder_id ON " + TABLE_FOLDER_MAPPINGS + 
                "(" + COL_FOLDER_ID + ")");
        
        // Insert default folders
        insertDefaultFolders(db);
    }
    
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Handle future schema updates
    }
    
    /**
     * Insert default system folders
     */
    private void insertDefaultFolders(SQLiteDatabase db) {
        ContentValues[] defaultFolders = {
            createFolderValues("Family", "FF4CAF50", 1, 0, false),      // Green
            createFolderValues("Work", "FF2196F3", 2, 1, false),        // Blue
            createFolderValues("Friends", "FF9C27B0", 3, 2, false),     // Purple
            createFolderValues("Business", "FF00BCD4", 4, 3, false),    // Cyan
            createFolderValues("VIP", "FFFFB300", 5, 4, false)          // Amber
        };
        
        for (ContentValues values : defaultFolders) {
            db.insert(TABLE_FOLDERS, null, values);
        }
    }
    
    /**
     * Helper to create folder ContentValues
     */
    private ContentValues createFolderValues(String name, String color, 
                                            int icon, int order, boolean isCustom) {
        ContentValues values = new ContentValues();
        values.put(COL_FOLDER_NAME, name);
        values.put(COL_FOLDER_COLOR, color);
        values.put(COL_FOLDER_ICON, icon);
        values.put(COL_SORT_ORDER, order);
        values.put(COL_CREATED_AT, System.currentTimeMillis() / 1000);
        values.put(COL_IS_CUSTOM, isCustom ? 1 : 0);
        return values;
    }
    
    /**
     * Create new custom folder
     */
    public long createFolder(String folderName, String folderColor, int icon) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        int maxOrder = 0;
        Cursor cursor = db.rawQuery("SELECT MAX(" + COL_SORT_ORDER + ") FROM " + 
                TABLE_FOLDERS, null);
        if (cursor.moveToFirst()) {
            maxOrder = cursor.getInt(0) + 1;
        }
        cursor.close();
        
        ContentValues values = createFolderValues(folderName, folderColor, icon, maxOrder, true);
        long folderId = db.insert(TABLE_FOLDERS, null, values);
        
        db.close();
        return folderId;
    }
    
    /**
     * Add chat to folder
     */
    public boolean addChatToFolder(long folderId, long chatId) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COL_FOLDER_ID, folderId);
        values.put(COL_CHAT_ID, chatId);
        values.put(COL_ADDED_AT, System.currentTimeMillis() / 1000);
        
        long result = db.insert(TABLE_FOLDER_MAPPINGS, null, values);
        db.close();
        
        return result != -1;
    }
    
    /**
     * Remove chat from folder
     */
    public boolean removeChatFromFolder(long folderId, long chatId) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        int result = db.delete(TABLE_FOLDER_MAPPINGS, 
                COL_FOLDER_ID + " = ? AND " + COL_CHAT_ID + " = ?",
                new String[]{String.valueOf(folderId), String.valueOf(chatId)});
        
        db.close();
        return result > 0;
    }
    
    /**
     * Get all chats in folder
     */
    public List<Long> getChatsInFolder(long folderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        List<Long> chatIds = new ArrayList<>();
        
        String query = "SELECT " + COL_CHAT_ID + " FROM " + TABLE_FOLDER_MAPPINGS +
                " WHERE " + COL_FOLDER_ID + " = ? ORDER BY " + COL_ADDED_AT + " DESC";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(folderId)});
        
        if (cursor.moveToFirst()) {
            do {
                chatIds.add(cursor.getLong(0));
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return chatIds;
    }
    
    /**
     * Get all folders
     */
    public List<ChatFolder> getAllFolders() {
        SQLiteDatabase db = this.getReadableDatabase();
        List<ChatFolder> folders = new ArrayList<>();
        
        String query = "SELECT " + COL_FOLDER_ID + ", " + COL_FOLDER_NAME + ", " +
                COL_FOLDER_COLOR + ", " + COL_FOLDER_ICON + ", " + COL_SORT_ORDER +
                " FROM " + TABLE_FOLDERS + " ORDER BY " + COL_SORT_ORDER;
        
        Cursor cursor = db.rawQuery(query, null);
        
        if (cursor.moveToFirst()) {
            do {
                ChatFolder folder = new ChatFolder(
                    cursor.getLong(0),           // folder_id
                    cursor.getString(1),         // folder_name
                    cursor.getString(2),         // folder_color
                    cursor.getInt(3),            // folder_icon
                    cursor.getInt(4)             // sort_order
                );
                
                // Get chat count
                int chatCount = getChatCountInFolder(folder.folderId);
                folder.chatCount = chatCount;
                
                folders.add(folder);
            } while (cursor.moveToNext());
        }
        
        cursor.close();
        db.close();
        
        return folders;
    }
    
    /**
     * Get chat count in folder
     */
    private int getChatCountInFolder(long folderId) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        String query = "SELECT COUNT(*) FROM " + TABLE_FOLDER_MAPPINGS +
                " WHERE " + COL_FOLDER_ID + " = ?";
        
        Cursor cursor = db.rawQuery(query, new String[]{String.valueOf(folderId)});
        int count = 0;
        
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        
        cursor.close();
        db.close();
        
        return count;
    }
    
    /**
     * Pin/unpin chat in folder
     */
    public boolean setPinned(long folderId, long chatId, boolean pinned) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COL_IS_PINNED, pinned ? 1 : 0);
        
        int result = db.update(TABLE_FOLDER_MAPPINGS, values,
                COL_FOLDER_ID + " = ? AND " + COL_CHAT_ID + " = ?",
                new String[]{String.valueOf(folderId), String.valueOf(chatId)});
        
        db.close();
        return result > 0;
    }
    
    /**
     * Update folder details
     */
    public boolean updateFolder(long folderId, String name, String color, int icon) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COL_FOLDER_NAME, name);
        values.put(COL_FOLDER_COLOR, color);
        values.put(COL_FOLDER_ICON, icon);
        
        int result = db.update(TABLE_FOLDERS, values,
                COL_FOLDER_ID + " = ?",
                new String[]{String.valueOf(folderId)});
        
        db.close();
        return result > 0;
    }
    
    /**
     * Delete custom folder
     */
    public boolean deleteFolder(long folderId) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        // Delete folder mappings
        db.delete(TABLE_FOLDER_MAPPINGS, COL_FOLDER_ID + " = ?",
                new String[]{String.valueOf(folderId)});
        
        // Delete folder
        int result = db.delete(TABLE_FOLDERS, COL_FOLDER_ID + " = ?",
                new String[]{String.valueOf(folderId)});
        
        db.close();
        return result > 0;
    }
    
    /**
     * Set folder filter
     */
    public void setFolderFilter(long folderId, String filterType, boolean enabled) {
        SQLiteDatabase db = this.getWritableDatabase();
        
        ContentValues values = new ContentValues();
        values.put(COL_FOLDER_ID, folderId);
        values.put(COL_FILTER_TYPE, filterType);
        values.put(COL_FILTER_ENABLED, enabled ? 1 : 0);
        
        db.insertWithOnConflict(TABLE_FOLDER_FILTERS, null, values,
                SQLiteDatabase.CONFLICT_REPLACE);
        
        db.close();
    }
    
    /**
     * Get folder filter status
     */
    public boolean isFolderFilterEnabled(long folderId, String filterType) {
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_FOLDER_FILTERS, new String[]{COL_FILTER_ENABLED},
                COL_FOLDER_ID + " = ? AND " + COL_FILTER_TYPE + " = ?",
                new String[]{String.valueOf(folderId), filterType},
                null, null, null);
        
        boolean enabled = false;
        if (cursor.moveToFirst()) {
            enabled = cursor.getInt(0) == 1;
        }
        
        cursor.close();
        db.close();
        
        return enabled;
    }
    
    /**
     * ChatFolder data class
     */
    public static class ChatFolder {
        public long folderId;
        public String folderName;
        public String folderColor;      // Hex color: FF4CAF50
        public int folderIcon;          // Icon ID
        public int sortOrder;
        public int chatCount;           // Number of chats in folder
        
        public ChatFolder(long folderId, String folderName, String folderColor,
                         int folderIcon, int sortOrder) {
            this.folderId = folderId;
            this.folderName = folderName;
            this.folderColor = folderColor;
            this.folderIcon = folderIcon;
            this.sortOrder = sortOrder;
            this.chatCount = 0;
        }
        
        @Override
        public String toString() {
            return "ChatFolder{" +
                    "name='" + folderName + '\'' +
                    ", color='" + folderColor + '\'' +
                    ", chats=" + chatCount +
                    '}';
        }
    }
}
