package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * PerChatAutoReplySettings - Handles chat-level auto-reply preferences
 */
public class PerChatAutoReplySettings {
    private static final String TAG = "PerChatAutoReplySettings";
    private static final String PREFS_NAME = "nayagram_per_chat_autoreply";
    private static final String KEY_PREFIX_DISABLED = "disabled_chat_";

    public static boolean canAutoReplyForChat(long dialogId) {
        try {
            if (ApplicationLoader.applicationContext == null) {
                return true;
            }
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            return !prefs.getBoolean(KEY_PREFIX_DISABLED + dialogId, false);
        } catch (Exception e) {
            FileLog.e(TAG + ": Error checking per-chat auto-reply settings", e);
            return true;
        }
    }

    public static void setAutoReplyForChat(long dialogId, boolean enabled) {
        try {
            if (ApplicationLoader.applicationContext == null) {
                return;
            }
            SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_PREFIX_DISABLED + dialogId, !enabled).apply();
            FileLog.d(TAG + ": Auto-reply for chat " + dialogId + " set to " + enabled);
        } catch (Exception e) {
            FileLog.e(TAG + ": Error saving per-chat auto-reply setting", e);
        }
    }
}
