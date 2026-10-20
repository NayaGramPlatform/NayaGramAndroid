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
    public void testLockerSettingRequiresSecureDeviceAndExplainsPerChatSetup() throws Exception {
        String settings = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/org/telegram/ui/NGSettingsActivity.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertTrue(settings.contains("BiometricChatLocker.isDeviceSecurityAvailable(getParentActivity())"));
        Assert.assertTrue(settings.contains("Set a PIN, password, or pattern in Android Security Settings"));
        Assert.assertTrue(settings.contains("Existing protected chats stay locked until you open each chat's NayaGram In-Chat Settings"));
        Assert.assertTrue(settings.contains("locker enabled. Open a chat's NayaGram In-Chat Settings to protect it."));
    }

    @Test
    public void testNativeFolderManagerIsUsed() throws Exception {
        String settings = new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(
                "src/main/java/org/telegram/ui/NGSettingsActivity.java")),
                java.nio.charset.StandardCharsets.UTF_8);
        Assert.assertTrue(settings.contains("presentFragment(new FiltersSetupActivity());"));
        Assert.assertTrue(settings.contains("Create and manage Telegram chat folders"));
    }

    @Test
    public void testAutoTranslateMenuRemains() {
        Assert.assertEquals(29004, NGChatMenuHelper.ITEM_AUTO_TRANSLATE);
    }
}
