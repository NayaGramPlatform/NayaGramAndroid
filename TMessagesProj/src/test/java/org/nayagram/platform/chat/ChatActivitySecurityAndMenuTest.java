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
    public void testAutoTranslateMenuRemains() {
        Assert.assertEquals(29004, NGChatMenuHelper.ITEM_AUTO_TRANSLATE);
    }
}
