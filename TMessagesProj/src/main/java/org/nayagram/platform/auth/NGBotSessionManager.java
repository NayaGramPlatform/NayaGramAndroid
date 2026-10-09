package org.nayagram.platform.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;

import java.nio.charset.StandardCharsets;

/**
 * NGBotSessionManager - Secure Bot Token & Session Storage with automatic redaction
 * and complete logout cleanup.
 *
 * Ensures:
 * 1. Bot tokens are never printed in plaintext to logs or console.
 * 2. Tokens are stored in private SharedPreferences with obfuscation.
 * 3. Logging out of an account completely wipes token and session state.
 */
public class NGBotSessionManager {

    private static final String PREF_PREFIX = "ng_bot_session_";
    private static final String KEY_IS_BOT = "is_bot";
    private static final String KEY_TOKEN_OBFUSCATED = "enc_token";
    private static final String KEY_BOT_ID = "bot_id";
    private static final String KEY_BOT_USERNAME = "bot_username";
    private static final String KEY_BOT_FIRST_NAME = "bot_first_name";
    private static final String KEY_LOGIN_TIME = "login_time";

    // Salt mask for token obfuscation in storage
    private static final byte[] MASK_KEY = new byte[]{0x4E, 0x61, 0x79, 0x61, 0x47, 0x72, 0x61, 0x6D}; // "NayaGram"

    /**
     * Redacts a bot token for safe logging.
     * E.g., "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ123456789" -> "123456789:AB***89"
     */
    public static String maskToken(String token) {
        if (token == null) {
            return "[null]";
        }
        token = token.trim();
        if (token.isEmpty()) {
            return "[empty]";
        }
        int colonIdx = token.indexOf(':');
        if (colonIdx > 0 && colonIdx < token.length() - 1) {
            String botId = token.substring(0, colonIdx);
            String secret = token.substring(colonIdx + 1);
            if (secret.length() <= 4) {
                return botId + ":****";
            }
            return botId + ":" + secret.substring(0, 2) + "***" + secret.substring(secret.length() - 2);
        }
        if (token.length() <= 4) {
            return "****";
        }
        return token.substring(0, 2) + "***" + token.substring(token.length() - 2);
    }

    private static SharedPreferences getPrefs(int currentAccount) {
        if (ApplicationLoader.applicationContext == null) {
            return null;
        }
        return ApplicationLoader.applicationContext.getSharedPreferences(
                PREF_PREFIX + currentAccount,
                Context.MODE_PRIVATE
        );
    }

    /**
     * Stores bot authentication data securely.
     */
    public static void saveBotSession(int currentAccount, String token, TLRPC.User botUser) {
        SharedPreferences prefs = getPrefs(currentAccount);
        if (prefs == null || token == null) {
            return;
        }

        try {
            String obfuscated = obfuscate(token);
            SharedPreferences.Editor editor = prefs.edit()
                    .putBoolean(KEY_IS_BOT, true)
                    .putString(KEY_TOKEN_OBFUSCATED, obfuscated)
                    .putLong(KEY_LOGIN_TIME, System.currentTimeMillis());

            if (botUser != null) {
                editor.putLong(KEY_BOT_ID, botUser.id);
                editor.putString(KEY_BOT_USERNAME, botUser.username != null ? botUser.username : "");
                editor.putString(KEY_BOT_FIRST_NAME, botUser.first_name != null ? botUser.first_name : "");
            }

            editor.apply();
            FileLog.d("NGBotSessionManager: Bot session saved securely for account " + currentAccount
                    + ", token " + maskToken(token));
        } catch (Exception e) {
            FileLog.e("NGBotSessionManager: Error saving bot session", e);
        }
    }

    /**
     * Checks if current account was authenticated using a Bot Token.
     */
    public static boolean isBotAccount(int currentAccount) {
        SharedPreferences prefs = getPrefs(currentAccount);
        return prefs != null && prefs.getBoolean(KEY_IS_BOT, false);
    }

    /**
     * Retrieves the de-obfuscated bot token for session operations.
     */
    public static String getBotToken(int currentAccount) {
        SharedPreferences prefs = getPrefs(currentAccount);
        if (prefs == null) {
            return null;
        }
        String obfuscated = prefs.getString(KEY_TOKEN_OBFUSCATED, null);
        if (obfuscated == null) {
            return null;
        }
        return deobfuscate(obfuscated);
    }

    /**
     * Returns masked bot token for display in settings.
     */
    public static String getMaskedBotToken(int currentAccount) {
        String token = getBotToken(currentAccount);
        return maskToken(token);
    }

    /**
     * Wipes all stored bot token & session data upon logout.
     */
    public static void cleanupAccount(int currentAccount) {
        SharedPreferences prefs = getPrefs(currentAccount);
        if (prefs != null) {
            prefs.edit().clear().apply();
            FileLog.d("NGBotSessionManager: Cleaned up bot session for account " + currentAccount);
        }
    }

    private static String obfuscate(String input) {
        byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
        byte[] result = new byte[bytes.length];
        for (int i = 0; i < bytes.length; i++) {
            result[i] = (byte) (bytes[i] ^ MASK_KEY[i % MASK_KEY.length]);
        }
        return Base64.encodeToString(result, Base64.NO_WRAP);
    }

    private static String deobfuscate(String input) {
        try {
            byte[] bytes = Base64.decode(input, Base64.NO_WRAP);
            byte[] result = new byte[bytes.length];
            for (int i = 0; i < bytes.length; i++) {
                result[i] = (byte) (bytes[i] ^ MASK_KEY[i % MASK_KEY.length]);
            }
            return new String(result, StandardCharsets.UTF_8);
        } catch (Exception e) {
            FileLog.e("NGBotSessionManager: Deobfuscation error", e);
            return null;
        }
    }
}
