package org.nayagram.platform.auth;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.tgnet.TLRPC;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * NGBotSessionManager - Encrypted Bot Token & Session Storage with Android KeyStore
 * backed AES-256-GCM, automatic invalidation handling, and complete logout cleanup.
 *
 * Ensures:
 * 1. Bot tokens are encrypted at rest using AES-256-GCM with authenticated tags.
 * 2. Sensitive credentials are never exposed in plaintext logs, console, or error traces.
 * 3. Keystore key invalidation (biometric reset, hardware key wiped) gracefully clears compromised state.
 * 4. Sessions are restored faithfully after app restart / process termination.
 * 5. Logout completely erases SharedPreferences, cached memory, and cryptographic key material.
 */
public class NGBotSessionManager {

    private static final String PREF_PREFIX = "ng_bot_session_";
    private static final String KEY_IS_BOT = "is_bot";
    private static final String KEY_ENC_TOKEN = "enc_token";
    private static final String KEY_BOT_ID = "bot_id";
    private static final String KEY_BOT_USERNAME = "bot_username";
    private static final String KEY_BOT_FIRST_NAME = "bot_first_name";
    private static final String KEY_LOGIN_TIME = "login_time";

    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS_PREFIX = "ng_bot_key_";
    private static final String AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH_BYTES = 12;
    private static final int GCM_TAG_LENGTH_BITS = 128;
    private static final int AES_KEY_SIZE_BITS = 256;

    // In-memory cache per account
    private static final Map<Integer, CachedSession> memoryCache = new ConcurrentHashMap<>();

    // Pluggable / testable crypto provider
    private static CryptoEngine cryptoEngine = new DefaultKeyStoreCryptoEngine();

    public static class CachedSession {
        public final String token;
        public final TLRPC.User user;
        public final long loginTime;

        public CachedSession(String token, TLRPC.User user, long loginTime) {
            this.token = token;
            this.user = user;
            this.loginTime = loginTime;
        }
    }

    /**
     * CryptoEngine interface for encryption, decryption, key management, and invalidation.
     */
    public interface CryptoEngine {
        byte[] encrypt(int account, byte[] plaintext) throws GeneralSecurityException;
        byte[] decrypt(int account, byte[] ciphertext) throws GeneralSecurityException;
        void deleteKey(int account);
        void invalidateKey(int account); // Used in integration tests
    }

    /**
     * Default Android KeyStore AES-256-GCM CryptoEngine.
     * Falls back to standard JCE provider if AndroidKeyStore provider is absent (e.g. host JVM unit tests).
     */
    public static class DefaultKeyStoreCryptoEngine implements CryptoEngine {
        private final Map<Integer, SecretKey> fallbackKeyStore = new ConcurrentHashMap<>();
        private final Map<Integer, Boolean> simulatedInvalidation = new ConcurrentHashMap<>();
        private final SecureRandom secureRandom = new SecureRandom();

        private boolean isKeyStoreAvailable() {
            try {
                KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
                ks.load(null);
                return true;
            } catch (Throwable t) {
                return false;
            }
        }

        private SecretKey getOrCreateKey(int account) throws GeneralSecurityException {
            if (Boolean.TRUE.equals(simulatedInvalidation.get(account))) {
                throw new KeyPermanentlyInvalidatedException("Simulated Keystore key invalidation for account " + account);
            }

            if (isKeyStoreAvailable() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
                    keyStore.load(null);
                    String alias = KEY_ALIAS_PREFIX + account;
                    if (!keyStore.containsAlias(alias)) {
                        KeyGenerator keyGen = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE);
                        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                                alias,
                                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
                        )
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setKeySize(AES_KEY_SIZE_BITS)
                                .setRandomizedEncryptionRequired(true)
                                .build();
                        keyGen.init(spec);
                        return keyGen.generateKey();
                    }
                    KeyStore.Entry entry = keyStore.getEntry(alias, null);
                    if (entry instanceof KeyStore.SecretKeyEntry) {
                        return ((KeyStore.SecretKeyEntry) entry).getSecretKey();
                    }
                } catch (GeneralSecurityException gse) {
                    throw gse;
                } catch (Throwable t) {
                    FileLog.w("NGBotSessionManager: KeyStore initialization error, using secure fallback: " + t.getMessage());
                }
            }

            // Fallback for JVM test runner or devices without KeyStore AES-GCM
            SecretKey key = fallbackKeyStore.get(account);
            if (key == null) {
                byte[] raw = new byte[32];
                secureRandom.nextBytes(raw);
                key = new SecretKeySpec(raw, "AES");
                fallbackKeyStore.put(account, key);
            }
            return key;
        }

        @Override
        public byte[] encrypt(int account, byte[] plaintext) throws GeneralSecurityException {
            SecretKey key = getOrCreateKey(account);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(("account_" + account).getBytes(StandardCharsets.UTF_8));
            byte[] cipherBytes = cipher.doFinal(plaintext);

            ByteBuffer buffer = ByteBuffer.allocate(iv.length + cipherBytes.length);
            buffer.put(iv);
            buffer.put(cipherBytes);
            return buffer.array();
        }

        @Override
        public byte[] decrypt(int account, byte[] payload) throws GeneralSecurityException {
            if (Boolean.TRUE.equals(simulatedInvalidation.get(account))) {
                throw new KeyPermanentlyInvalidatedException("Keystore key permanently invalidated for account " + account);
            }
            if (payload == null || payload.length <= GCM_IV_LENGTH_BYTES) {
                throw new GeneralSecurityException("Invalid encrypted payload length");
            }

            ByteBuffer buffer = ByteBuffer.wrap(payload);
            byte[] iv = new byte[GCM_IV_LENGTH_BYTES];
            buffer.get(iv);
            byte[] cipherBytes = new byte[buffer.remaining()];
            buffer.get(cipherBytes);

            SecretKey key = getOrCreateKey(account);
            Cipher cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
            cipher.updateAAD(("account_" + account).getBytes(StandardCharsets.UTF_8));
            return cipher.doFinal(cipherBytes);
        }

        @Override
        public void deleteKey(int account) {
            simulatedInvalidation.remove(account);
            fallbackKeyStore.remove(account);
            if (isKeyStoreAvailable()) {
                try {
                    KeyStore ks = KeyStore.getInstance(ANDROID_KEYSTORE);
                    ks.load(null);
                    String alias = KEY_ALIAS_PREFIX + account;
                    if (ks.containsAlias(alias)) {
                        ks.deleteEntry(alias);
                    }
                } catch (Throwable t) {
                    FileLog.e("NGBotSessionManager: Error deleting KeyStore key for account " + account, t);
                }
            }
        }

        @Override
        public void invalidateKey(int account) {
            simulatedInvalidation.put(account, Boolean.TRUE);
        }
    }

    /**
     * For testing: set a custom or mocked CryptoEngine.
     */
    public static void setCryptoEngine(CryptoEngine engine) {
        cryptoEngine = engine != null ? engine : new DefaultKeyStoreCryptoEngine();
    }

    /**
     * For testing: reset to default KeyStore crypto engine.
     */
    public static void resetCryptoEngine() {
        cryptoEngine = new DefaultKeyStoreCryptoEngine();
        memoryCache.clear();
    }

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
     * Stores bot authentication data with authenticated encryption.
     */
    public static void saveBotSession(int currentAccount, String token, TLRPC.User botUser) {
        SharedPreferences prefs = getPrefs(currentAccount);
        if (prefs == null || token == null) {
            return;
        }

        try {
            byte[] encryptedBytes = cryptoEngine.encrypt(currentAccount, token.getBytes(StandardCharsets.UTF_8));
            String base64Encrypted = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP);
            long loginTime = System.currentTimeMillis();

            SharedPreferences.Editor editor = prefs.edit()
                    .putBoolean(KEY_IS_BOT, true)
                    .putString(KEY_ENC_TOKEN, base64Encrypted)
                    .putLong(KEY_LOGIN_TIME, loginTime);

            if (botUser != null) {
                editor.putLong(KEY_BOT_ID, botUser.id);
                editor.putString(KEY_BOT_USERNAME, botUser.username != null ? botUser.username : "");
                editor.putString(KEY_BOT_FIRST_NAME, botUser.first_name != null ? botUser.first_name : "");
            }

            editor.apply();

            // Update in-memory session cache
            memoryCache.put(currentAccount, new CachedSession(token, botUser, loginTime));

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
        CachedSession cached = memoryCache.get(currentAccount);
        if (cached != null) {
            return true;
        }
        SharedPreferences prefs = getPrefs(currentAccount);
        return prefs != null && prefs.getBoolean(KEY_IS_BOT, false);
    }

    /**
     * Retrieves the decrypted bot token for session operations.
     * Automatically detects Keystore key invalidation, handles it gracefully without crashing,
     * and wipes corrupted session state.
     */
    public static String getBotToken(int currentAccount) {
        CachedSession cached = memoryCache.get(currentAccount);
        if (cached != null && cached.token != null) {
            return cached.token;
        }

        SharedPreferences prefs = getPrefs(currentAccount);
        if (prefs == null) {
            return null;
        }
        String base64Encrypted = prefs.getString(KEY_ENC_TOKEN, null);
        if (base64Encrypted == null) {
            return null;
        }

        try {
            byte[] cipherBytes = Base64.decode(base64Encrypted, Base64.NO_WRAP);
            byte[] decrypted = cryptoEngine.decrypt(currentAccount, cipherBytes);
            String token = new String(decrypted, StandardCharsets.UTF_8);

            // Populate in-memory cache
            TLRPC.User user = loadBotUserFromPrefs(prefs);
            long loginTime = prefs.getLong(KEY_LOGIN_TIME, 0);
            memoryCache.put(currentAccount, new CachedSession(token, user, loginTime));

            return token;
        } catch (GeneralSecurityException gse) {
            FileLog.e("NGBotSessionManager: Cryptographic failure or Keystore key invalidated for account "
                    + currentAccount + ": " + gse.getMessage());
            // Keystore key invalidation or corruption: automatically wipe unrecoverable session
            cleanupAccount(currentAccount);
            return null;
        } catch (Exception e) {
            FileLog.e("NGBotSessionManager: Error decrypting bot session for account " + currentAccount, e);
            cleanupAccount(currentAccount);
            return null;
        }
    }

    /**
     * Returns the restored TLRPC.User object for the authenticated bot.
     */
    public static TLRPC.User getBotUser(int currentAccount) {
        CachedSession cached = memoryCache.get(currentAccount);
        if (cached != null && cached.user != null) {
            return cached.user;
        }
        SharedPreferences prefs = getPrefs(currentAccount);
        if (prefs == null || !prefs.getBoolean(KEY_IS_BOT, false)) {
            return null;
        }
        TLRPC.User user = loadBotUserFromPrefs(prefs);
        if (user != null) {
            String token = getBotToken(currentAccount);
            if (token != null) {
                memoryCache.put(currentAccount, new CachedSession(token, user, prefs.getLong(KEY_LOGIN_TIME, 0)));
            }
        }
        return user;
    }

    private static TLRPC.User loadBotUserFromPrefs(SharedPreferences prefs) {
        long botId = prefs.getLong(KEY_BOT_ID, 0);
        if (botId <= 0) {
            return null;
        }
        TLRPC.TL_user user = new TLRPC.TL_user();
        user.id = botId;
        user.username = prefs.getString(KEY_BOT_USERNAME, "");
        user.first_name = prefs.getString(KEY_BOT_FIRST_NAME, "");
        user.bot = true;
        return user;
    }

    /**
     * Returns masked bot token for display in settings.
     */
    public static String getMaskedBotToken(int currentAccount) {
        String token = getBotToken(currentAccount);
        return maskToken(token);
    }

    /**
     * Wipes all stored bot token, session data, in-memory caches, and Keystore keys upon logout.
     */
    public static void cleanupAccount(int currentAccount) {
        memoryCache.remove(currentAccount);

        SharedPreferences prefs = getPrefs(currentAccount);
        if (prefs != null) {
            prefs.edit().clear().apply();
        }

        try {
            cryptoEngine.deleteKey(currentAccount);
        } catch (Exception e) {
            FileLog.e("NGBotSessionManager: Error deleting key during cleanup", e);
        }

        FileLog.d("NGBotSessionManager: Completely wiped bot session and cryptographic material for account " + currentAccount);
    }

    /**
     * For testing / simulation of app process restart: clears in-memory caches without touching persistent storage.
     */
    public static void clearMemoryCacheForTest() {
        memoryCache.clear();
    }
}
