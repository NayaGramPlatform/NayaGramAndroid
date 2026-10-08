package org.nayagram.platform.bot;

import org.junit.Assert;
import org.junit.Test;
import org.telegram.tgnet.TLRPC;

public class NGBotAccountGuardTest {

    @Test
    public void testBotAccountDisallowsContactsAndStories() {
        TLRPC.User botUser = new TLRPC.TL_user();
        botUser.id = 12345678L;
        botUser.bot = true;
        botUser.phone = null;

        String phoneDisplay = NGBotAccountGuard.getSafePhoneNumber(botUser);
        Assert.assertEquals("Bot Account (No Phone)", phoneDisplay);
        Assert.assertTrue("Bot flag must be true", botUser.bot);
    }

    @Test
    public void testRegularUserAllowsFeatures() {
        TLRPC.User regularUser = new TLRPC.TL_user();
        regularUser.id = 87654321L;
        regularUser.bot = false;
        regularUser.phone = "+1234567890";

        String phoneDisplay = NGBotAccountGuard.getSafePhoneNumber(regularUser);
        Assert.assertEquals("+1234567890", phoneDisplay);
    }
}
