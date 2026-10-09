package org.nayagram.platform.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.telegram.tgnet.TLRPC;

/**
 * End-to-End tests for NayaGram Telegram Bot Token authentication:
 * - Token validation across standard and edge-case inputs
 * - Sensitive token redaction from logs and toString representations
 * - MTProto constructor integrity
 * - Error mapping for invalid tokens, revoked tokens, FloodWait, and network failures
 */
public class NGBotLoginEndToEndTest {

    private static final String SAMPLE_VALID_TOKEN = "7123456789:AAFl1aBcDeFgHiJkLmNoPqRsTuVwXyZ_-abc";
    private static final String SAMPLE_ANOTHER_TOKEN = "123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ123456789";

    @Test
    public void testBotTokenValidation() {
        // Valid tokens
        assertTrue(NGBotLoginHelper.isValidBotToken(SAMPLE_VALID_TOKEN));
        assertTrue(NGBotLoginHelper.isValidBotToken(SAMPLE_ANOTHER_TOKEN));
        assertTrue(NGBotLoginHelper.isValidBotToken("9876543210:ABC-DEF_1234567890abcdefghijklmnopqrstuv"));

        // Invalid tokens
        assertFalse(NGBotLoginHelper.isValidBotToken(null));
        assertFalse(NGBotLoginHelper.isValidBotToken(""));
        assertFalse(NGBotLoginHelper.isValidBotToken("   "));
        assertFalse(NGBotLoginHelper.isValidBotToken("plain_text_without_colon"));
        assertFalse(NGBotLoginHelper.isValidBotToken("12345:shortsecret"));
        assertFalse(NGBotLoginHelper.isValidBotToken("notdigits:ABCdefGhIJKlmNoPQRsTUVwxyZ123456789"));
        assertFalse(NGBotLoginHelper.isValidBotToken(":ABCdefGhIJKlmNoPQRsTUVwxyZ123456789"));
        assertFalse(NGBotLoginHelper.isValidBotToken("123456789:"));
    }

    @Test
    public void testSensitiveTokenRedactionInLogs() {
        String masked = NGBotSessionManager.maskToken(SAMPLE_VALID_TOKEN);
        assertNotNull(masked);
        assertTrue("Masked token must preserve bot id prefix", masked.startsWith("7123456789:"));
        assertTrue("Masked token must contain asterisks", masked.contains("***"));
        assertFalse("Masked token must never leak full secret key", masked.contains("AAFl1aBcDeFgHiJkLmNoPqRsTuVwXyZ_-abc"));

        // Short tokens and edge cases
        assertEquals("[null]", NGBotSessionManager.maskToken(null));
        assertEquals("[empty]", NGBotSessionManager.maskToken(""));
        assertEquals("****", NGBotSessionManager.maskToken("1234"));
    }

    @Test
    public void testTLRequestToStringRedactsSecret() {
        TL_auth_importBotAuthorization req = new TL_auth_importBotAuthorization();
        req.api_id = 12345;
        req.api_hash = "abcdef";
        req.bot_auth_token = SAMPLE_VALID_TOKEN;

        String str = req.toString();
        assertNotNull(str);
        assertTrue("ToString must include constructor", str.contains("constructor=67a3ff2c"));
        assertFalse("ToString must NEVER reveal plaintext secret token", str.contains(SAMPLE_VALID_TOKEN));
        assertTrue("ToString must contain masked token", str.contains("7123456789:AA***bc"));
    }

    @Test
    public void testBotLoginSuccessSimulation() {
        TLRPC.TL_auth_authorization authResult = new TLRPC.TL_auth_authorization();
        TLRPC.TL_user botUser = new TLRPC.TL_user();
        botUser.id = 7123456789L;
        botUser.first_name = "NayaGram Assistant";
        botUser.username = "nayagram_bot";
        botUser.bot = true;
        authResult.user = botUser;

        assertNotNull(authResult.user);
        assertTrue("Authorized account must be marked as bot", authResult.user.bot);
        assertEquals(7123456789L, authResult.user.id);
        assertEquals("nayagram_bot", authResult.user.username);
    }

    @Test
    public void testErrorHandling_InvalidToken() {
        TLRPC.TL_error error = new TLRPC.TL_error();
        error.code = 400;
        error.text = "ACCESS_TOKEN_INVALID";

        String userMsg = NGBotLoginHelper.getFriendlyErrorMessage(error);
        assertNotNull(userMsg);
        assertTrue("User message must explain invalid token clearly", userMsg.contains("invalid"));
        assertTrue("User message should guide user to BotFather", userMsg.contains("@BotFather"));
        assertFalse("User message must not leak raw technical code", userMsg.contains("400"));
    }

    @Test
    public void testErrorHandling_ExpiredToken() {
        TLRPC.TL_error error = new TLRPC.TL_error();
        error.code = 400;
        error.text = "ACCESS_TOKEN_EXPIRED";

        String userMsg = NGBotLoginHelper.getFriendlyErrorMessage(error);
        assertNotNull(userMsg);
        assertTrue("User message must explain token expiration", userMsg.contains("expired") || userMsg.contains("revoked"));
    }

    @Test
    public void testErrorHandling_FloodWait() {
        TLRPC.TL_error error = new TLRPC.TL_error();
        error.code = 420;
        error.text = "FLOOD_WAIT_45";

        String userMsg = NGBotLoginHelper.getFriendlyErrorMessage(error);
        assertNotNull(userMsg);
        assertTrue("User message must indicate too many attempts", userMsg.contains("Too many"));
        assertTrue("User message must include the wait seconds", userMsg.contains("45 seconds"));
    }

    @Test
    public void testErrorHandling_NetworkFailure() {
        // Null error represents transport/network failure
        String userMsgNull = NGBotLoginHelper.getFriendlyErrorMessage(null);
        assertNotNull(userMsgNull);
        assertTrue("Null error should report network issue", userMsgNull.contains("Network connection"));

        // Server internal error 500
        TLRPC.TL_error serverError = new TLRPC.TL_error();
        serverError.code = 500;
        serverError.text = "INTERNAL_SERVER_ERROR";
        String serverMsg = NGBotLoginHelper.getFriendlyErrorMessage(serverError);
        assertNotNull(serverMsg);
        assertTrue("500 error should report server error", serverMsg.contains("server error"));
    }
}
