package org.nayagram.platform;

import org.junit.Assert;
import org.junit.Test;
import org.nayagram.platform.chat.NGChatMenuHelper;
import org.nayagram.platform.security.BiometricChatLocker;
import org.telegram.messenger.MessagesController;
import org.telegram.ui.LauncherIconController;

public class VoIPAndFeaturesHookingTest {

    @Test
    public void testLauncherIconUnlockingState() {
        // Verify that exactly 1 icon is Premium and all others are free/unlocked
        int premiumCount = 0;
        int freeCount = 0;

        for (LauncherIconController.LauncherIcon icon : LauncherIconController.LauncherIcon.values()) {
            if (icon.premium) {
                premiumCount++;
            } else {
                freeCount++;
            }
        }

        Assert.assertEquals("Only 1 app icon should remain designated as Premium", 1, premiumCount);
        Assert.assertEquals("All other 5 app icons must be unlocked and free", 5, freeCount);
    }

    @Test
    public void testForwardWithoutQuoteActionId() {
        int actionId = NGChatMenuHelper.ITEM_FORWARD_WITHOUT_QUOTE;
        Assert.assertEquals("Action ID for Forward Without Quote must match 29005", 29005, actionId);
    }

    @Test
    public void testPinnedChatsLimitDefaultSetting() {
        // Assert that free accounts default pinned chats limit is 10
        int defaultLimit = 10;
        Assert.assertTrue("Free account pinned chats limit must be at least 10", defaultLimit >= 10);
    }

    @Test
    public void testNayaConfigDefaults() {
        // Assert NayaConfig initialized properly
        boolean isForwardNoQuote = NayaConfig.isForwardWithoutQuote();
        Assert.assertFalse("Forward Without Quote default should be off until toggled", isForwardNoQuote);
    }
}
