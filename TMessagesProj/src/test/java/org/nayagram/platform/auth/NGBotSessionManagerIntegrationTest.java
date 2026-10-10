package org.nayagram.platform.auth;

import android.content.Context;
import android.content.SharedPreferences;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.tgnet.TLRPC;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Integration tests for NGBotSessionManager covering:
 * 1. AES-GCM encryption at rest & seamless session restoration across app process restarts.
 * 2. Android KeyStore key invalidation handling without crashing, with automatic unrecoverable state cleanup.
 * 3. Complete data & cryptographic key wiping upon account logout.
 * 4. Multi-account isolation across all 5 accounts.
 * 5. Sensitive credential redaction in logs and masked representations.
 */
public class NGBotSessionManagerIntegrationTest {

    private final Map<String, MockSharedPreferences> mockPrefsMap = new HashMap<>();

    @Before
    public void setUp() {
        mockPrefsMap.clear();
        NGBotSessionManager.resetCryptoEngine();

        // Setup mock ApplicationLoader context for tests
        ApplicationLoader.applicationContext = new MockContext() {
            @Override
            public SharedPreferences getSharedPreferences(String name, int mode) {
                MockSharedPreferences prefs = mockPrefsMap.get(name);
                if (prefs == null) {
                    prefs = new MockSharedPreferences();
                    mockPrefsMap.put(name, prefs);
                }
                return prefs;
            }
        };
    }

    @After
    public void tearDown() {
        for (int i = 0; i < 5; i++) {
            NGBotSessionManager.cleanupAccount(i);
        }
        NGBotSessionManager.resetCryptoEngine();
        mockPrefsMap.clear();
    }

    @Test
    public void testSessionSaveAndRestoreAfterAppRestart() {
        int account = 0;
        String rawToken = "7123456789:AAF_XyZ987TestTokenSecretForNayaGramAuth_Integration";
        TLRPC.TL_user user = new TLRPC.TL_user();
        user.id = 7123456789L;
        user.username = "nayagram_support_bot";
        user.first_name = "NayaGram Assistant";
        user.bot = true;

        // 1. Save session
        NGBotSessionManager.saveBotSession(account, rawToken, user);

        // Verify stored data is encrypted and doesn't contain raw secret
        SharedPreferences prefs = ApplicationLoader.applicationContext.getSharedPreferences("ng_bot_session_0", Context.MODE_PRIVATE);
        assertTrue("is_bot flag must be true in prefs", prefs.getBoolean("is_bot", false));
        String encryptedPayload = prefs.getString("enc_token", null);
        assertNotNull("Encrypted token must be saved", encryptedPayload);
        assertFalse("Raw token must NEVER appear in encrypted storage", encryptedPayload.contains("TestTokenSecretForNayaGramAuth"));

        // 2. Simulate process death / app restart by clearing all memory caches
        NGBotSessionManager.clearMemoryCacheForTest();

        // 3. Verify session restoration after restart
        assertTrue("Account must be recognized as bot after restart", NGBotSessionManager.isBotAccount(account));
        String restoredToken = NGBotSessionManager.getBotToken(account);
        assertEquals("Restored token must match original decrypted token", rawToken, restoredToken);

        TLRPC.User restoredUser = NGBotSessionManager.getBotUser(account);
        assertNotNull("Restored bot user must not be null", restoredUser);
        assertEquals("Bot ID must match", 7123456789L, restoredUser.id);
        assertEquals("Username must match", "nayagram_support_bot", restoredUser.username);
        assertEquals("First name must match", "NayaGram Assistant", restoredUser.first_name);
        assertTrue("User must be marked as bot", restoredUser.bot);

        // Verify masked representation
        String masked = NGBotSessionManager.getMaskedBotToken(account);
        assertTrue("Masked token must start with bot ID", masked.startsWith("7123456789:"));
        assertTrue("Masked token must contain asterisks", masked.contains("***"));
        assertFalse("Masked token must not contain the full secret", masked.contains("TestTokenSecretForNayaGramAuth"));
    }

    @Test
    public void testKeystoreKeyInvalidationHandling() {
        int account = 0;
        String rawToken = "8123456789:InvalidationTestTokenSecret_AES256GCM";
        TLRPC.TL_user user = new TLRPC.TL_user();
        user.id = 8123456789L;
        user.username = "invalidation_test_bot";
        user.bot = true;

        NGBotSessionManager.saveBotSession(account, rawToken, user);

        // Clear memory cache so next retrieval attempts to decrypt using KeyStore
        NGBotSessionManager.clearMemoryCacheForTest();

        // Simulate Keystore key invalidation (e.g. user reset device lock screen or biometrics changed)
        NGBotSessionManager.DefaultKeyStoreCryptoEngine engine = new NGBotSessionManager.DefaultKeyStoreCryptoEngine();
        engine.invalidateKey(account);
        NGBotSessionManager.setCryptoEngine(engine);

        // Retrieve token - must NOT throw unhandled crash, must return null and automatically clean up
        String tokenAfterInvalidation = NGBotSessionManager.getBotToken(account);
        assertNull("Invalidated key must result in null token", tokenAfterInvalidation);

        // Verify compromised/unusable session was wiped
        assertFalse("Account must no longer be marked as bot after key invalidation", NGBotSessionManager.isBotAccount(account));
        assertEquals("[null]", NGBotSessionManager.getMaskedBotToken(account));
        assertNull("Bot user must be null after invalidation cleanup", NGBotSessionManager.getBotUser(account));
    }

    @Test
    public void testDataWipeUponLogout() {
        int account1 = 1;
        int account2 = 2;

        TLRPC.TL_user user1 = new TLRPC.TL_user();
        user1.id = 111111L;
        user1.username = "bot_one";
        user1.bot = true;

        TLRPC.TL_user user2 = new TLRPC.TL_user();
        user2.id = 222222L;
        user2.username = "bot_two";
        user2.bot = true;

        NGBotSessionManager.saveBotSession(account1, "111111:TokenOneSecretXYZ", user1);
        NGBotSessionManager.saveBotSession(account2, "222222:TokenTwoSecretABC", user2);

        // User logs out of account 1
        NGBotSessionManager.cleanupAccount(account1);

        // Verify account 1 is completely wiped
        assertFalse("Account 1 must not be marked as bot after logout", NGBotSessionManager.isBotAccount(account1));
        assertNull("Account 1 token must be null after logout", NGBotSessionManager.getBotToken(account1));
        assertNull("Account 1 user must be null after logout", NGBotSessionManager.getBotUser(account1));
        assertEquals("[null]", NGBotSessionManager.getMaskedBotToken(account1));

        SharedPreferences prefs1 = ApplicationLoader.applicationContext.getSharedPreferences("ng_bot_session_1", Context.MODE_PRIVATE);
        assertFalse("Account 1 prefs must be empty", prefs1.contains("enc_token"));

        // Verify account 2 remains completely intact and operational (multi-account isolation)
        assertTrue("Account 2 must still be active", NGBotSessionManager.isBotAccount(account2));
        assertEquals("222222:TokenTwoSecretABC", NGBotSessionManager.getBotToken(account2));
        assertNotNull("Account 2 user must still be accessible", NGBotSessionManager.getBotUser(account2));
    }

    @Test
    public void testMultiAccountIsolationAndRestart() {
        // Test all 5 supported accounts simultaneously
        for (int i = 0; i < 5; i++) {
            TLRPC.TL_user bot = new TLRPC.TL_user();
            bot.id = 1000L + i;
            bot.username = "bot_account_" + i;
            bot.bot = true;
            NGBotSessionManager.saveBotSession(i, (1000 + i) + ":SecretAccountToken_" + i, bot);
        }

        // Simulate app kill & restart
        NGBotSessionManager.clearMemoryCacheForTest();

        // Verify all 5 accounts restored their respective secrets independently
        for (int i = 0; i < 5; i++) {
            assertTrue("Account " + i + " must be bot", NGBotSessionManager.isBotAccount(i));
            assertEquals((1000 + i) + ":SecretAccountToken_" + i, NGBotSessionManager.getBotToken(i));
            TLRPC.User u = NGBotSessionManager.getBotUser(i);
            assertNotNull(u);
            assertEquals(1000L + i, u.id);
            assertEquals("bot_account_" + i, u.username);
        }
    }

    @Test
    public void testSensitiveTokenRedaction() {
        assertEquals("[null]", NGBotSessionManager.maskToken(null));
        assertEquals("[empty]", NGBotSessionManager.maskToken("   "));
        assertEquals("****", NGBotSessionManager.maskToken("1234"));
        assertEquals("12345:****", NGBotSessionManager.maskToken("12345:abc"));

        String masked = NGBotSessionManager.maskToken("987654321:AAFlkmz098234857SecretPart99");
        assertEquals("987654321:AA***99", masked);
        assertFalse("Must never expose middle of secret token", masked.contains("SecretPart"));
    }

    // --- Mock SharedPreferences and Context for JVM test environment ---

    private static class MockContext extends android.test.mock.MockContext {
    }

    private static class MockSharedPreferences implements SharedPreferences {
        private final Map<String, Object> values = new HashMap<>();

        @Override
        public Map<String, ?> getAll() { return new HashMap<>(values); }

        @Override
        public String getString(String key, String defValue) {
            Object v = values.get(key);
            return v instanceof String ? (String) v : defValue;
        }

        @Override
        public java.util.Set<String> getStringSet(String key, java.util.Set<String> defValues) { return defValues; }

        @Override
        public int getInt(String key, int defValue) {
            Object v = values.get(key);
            return v instanceof Integer ? (Integer) v : defValue;
        }

        @Override
        public long getLong(String key, long defValue) {
            Object v = values.get(key);
            return v instanceof Long ? (Long) v : defValue;
        }

        @Override
        public float getFloat(String key, float defValue) {
            Object v = values.get(key);
            return v instanceof Float ? (Float) v : defValue;
        }

        @Override
        public boolean getBoolean(String key, boolean defValue) {
            Object v = values.get(key);
            return v instanceof Boolean ? (Boolean) v : defValue;
        }

        @Override
        public boolean contains(String key) { return values.containsKey(key); }

        @Override
        public Editor edit() { return new MockEditor(this); }

        @Override
        public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        @Override
        public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {}

        private static class MockEditor implements Editor {
            private final MockSharedPreferences parent;
            private final Map<String, Object> temp = new HashMap<>();
            private boolean clearRequested = false;

            MockEditor(MockSharedPreferences parent) { this.parent = parent; }

            @Override
            public Editor putString(String key, String value) { temp.put(key, value); return this; }
            @Override
            public Editor putStringSet(String key, java.util.Set<String> values) { return this; }
            @Override
            public Editor putInt(String key, int value) { temp.put(key, value); return this; }
            @Override
            public Editor putLong(String key, long value) { temp.put(key, value); return this; }
            @Override
            public Editor putFloat(String key, float value) { temp.put(key, value); return this; }
            @Override
            public Editor putBoolean(String key, boolean value) { temp.put(key, value); return this; }
            @Override
            public Editor remove(String key) { temp.put(key, this); return this; }
            @Override
            public Editor clear() { clearRequested = true; return this; }
            @Override
            public boolean commit() { apply(); return true; }

            @Override
            public void apply() {
                if (clearRequested) { parent.values.clear(); }
                for (Map.Entry<String, Object> entry : temp.entrySet()) {
                    if (entry.getValue() == this) {
                        parent.values.remove(entry.getKey());
                    } else {
                        parent.values.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        }
    }
}
