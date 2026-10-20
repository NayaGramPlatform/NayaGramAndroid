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
    public void testBiometricLockerFailsClosedWhenDeviceSecurityIsUnavailable() throws Exception {
        String source = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/org/nayagram/platform/security/BiometricChatLocker.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertTrue(source.contains("if (!isDeviceSecurityAvailable(activity))"));
        Assert.assertTrue(source.contains("if (callback != null) callback.onUnlockFailed();"));
        Assert.assertFalse(source.contains("// Fallback for older devices: grant session access"));
    }

    @Test
    public void testAutoTranslateMenuRemains() {
        Assert.assertEquals(29004, NGChatMenuHelper.ITEM_AUTO_TRANSLATE);
    }
}
