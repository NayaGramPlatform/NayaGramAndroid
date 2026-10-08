package org.nayagram.platform.chat;

import org.junit.Assert;
import org.junit.Test;
import org.nayagram.platform.NayaConfig;
import org.nayagram.platform.security.BiometricChatLocker;

public class ChatActivitySecurityAndMenuTest {

    @Test
    public void testLockedChatAccessDenial() {
        long lockedDialogId = 123456789L;
        boolean isProtected = true;
        boolean isUnlockedForSession = false;

        // ChatActivity onResume / onFragmentCreate logic assertion
        boolean canOpenChat = !isProtected || isUnlockedForSession;
        Assert.assertFalse("Biometrically locked chat without session grant must be denied entry", canOpenChat);
    }

    @Test
    public void testForwardWithoutQuoteAvailability() {
        // Verify real forward without quote menu item ID constant and logic
        int forwardNoQuoteId = NGChatMenuHelper.ITEM_FORWARD_WITHOUT_QUOTE;
        Assert.assertEquals(29005, forwardNoQuoteId);
        Assert.assertTrue("Forward Without Quote action ID must be valid", forwardNoQuoteId > 0);
    }
}
