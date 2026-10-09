package org.nayagram.platform.auth;

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests verifying Telegram Bot Token validation and MTProto constructor integrity.
 */
public class NGBotAuthTest {

    @Test
    public void testTLConstructorId() {
        Assert.assertEquals("Constructor must match MTProto auth.importBotAuthorization#67a3ff2c",
                0x67a3ff2c, TL_auth_importBotAuthorization.constructor);
    }

    @Test
    public void testValidBotTokens() {
        Assert.assertTrue(NGBotLoginHelper.isValidBotToken("123456789:ABCdefGhIJKlmNoPQRsTUVwxyZ123456789"));
        Assert.assertTrue(NGBotLoginHelper.isValidBotToken("9876543210:AAFl1aBcDeFgHiJkLmNoPqRsTuVwXyZ_-abc"));
        Assert.assertTrue(NGBotLoginHelper.isValidBotToken("1122334455:ABC-DEF_1234567890abcdefghijklmnopqrstuv"));
    }

    @Test
    public void testInvalidBotTokens() {
        Assert.assertFalse(NGBotLoginHelper.isValidBotToken(""));
        Assert.assertFalse(NGBotLoginHelper.isValidBotToken(null));
        Assert.assertFalse(NGBotLoginHelper.isValidBotToken("   "));
        Assert.assertFalse(NGBotLoginHelper.isValidBotToken("random_string_without_colon"));
        Assert.assertFalse(NGBotLoginHelper.isValidBotToken("12345:shortsecret"));
        Assert.assertFalse(NGBotLoginHelper.isValidBotToken("notdigits:ABCdefGhIJKlmNoPQRsTUVwxyZ123456789"));
        Assert.assertFalse(NGBotLoginHelper.isValidBotToken(":ABCdefGhIJKlmNoPQRsTUVwxyZ123456789"));
        Assert.assertFalse(NGBotLoginHelper.isValidBotToken("123456789:"));
    }
}
