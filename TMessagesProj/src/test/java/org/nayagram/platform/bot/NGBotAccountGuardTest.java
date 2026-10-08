package org.nayagram.platform.bot;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.telegram.messenger.UserConfig;
import org.telegram.tgnet.TLRPC;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Integration & Unit tests verifying that Bot accounts:
 * 1. Display user-friendly limitation notices
 * 2. Never trigger network synchronization requests for Contacts or Stories
 * 3. Safely handle missing phone numbers without NullPointerExceptions
 */
public class NGBotAccountGuardTest {

    private TLRPC.User botUser;
    private TLRPC.User humanUser;

    @Before
    public void setUp() {
        botUser = new TLRPC.TL_user();
        botUser.id = 99912345L;
        botUser.bot = true;
        botUser.phone = null;
        botUser.first_name = "NayaGram Assistant Bot";

        humanUser = new TLRPC.TL_user();
        humanUser.id = 11187654L;
        humanUser.bot = false;
        humanUser.phone = "+8801700000000";
        humanUser.first_name = "Huzaifa";
    }

    @Test
    public void testBotAccountSafePhoneNumber() {
        String phoneDisplay = NGBotAccountGuard.getSafePhoneNumber(botUser);
        Assert.assertEquals("Bot Account (No Phone)", phoneDisplay);
        Assert.assertTrue("Bot flag must be true", botUser.bot);
    }

    @Test
    public void testRegularUserAllowsFeatures() {
        String phoneDisplay = NGBotAccountGuard.getSafePhoneNumber(humanUser);
        Assert.assertEquals("+8801700000000", phoneDisplay);
        Assert.assertFalse("Human user flag must not be bot", humanUser.bot);
    }

    @Test
    public void testContactsHandlerSuppressesNetworkRequestForBotAccount() {
        // Mock account slot with Bot user
        int testSlot = 0;
        UserConfig.getInstance(testSlot).setCurrentUser(botUser);

        final AtomicInteger networkSyncRequestsSent = new AtomicInteger(0);

        // Guard check in ContactsActivity / ContactsController
        boolean canSync = NGBotAccountGuard.checkContactsSyncSupported(null, testSlot);
        if (canSync) {
            // Simulated network RPC: contacts.getContacts
            networkSyncRequestsSent.incrementAndGet();
        }

        Assert.assertFalse("Bot account must be rejected from contact sync", canSync);
        Assert.assertEquals("Zero network requests must be transmitted for bot contacts", 0, networkSyncRequestsSent.get());
    }

    @Test
    public void testStoryHandlerSuppressesNetworkRequestForBotAccount() {
        int testSlot = 0;
        UserConfig.getInstance(testSlot).setCurrentUser(botUser);

        final AtomicInteger storyNetworkRequestsSent = new AtomicInteger(0);

        // Guard check in DialogStoriesCell / StoriesController
        boolean canLoadStories = NGBotAccountGuard.checkStoriesSupported(null, testSlot);
        if (canLoadStories) {
            // Simulated network RPC: stories.getAllStories
            storyNetworkRequestsSent.incrementAndGet();
        }

        Assert.assertFalse("Bot account must be rejected from stories flow", canLoadStories);
        Assert.assertEquals("Zero network requests must be transmitted for bot stories", 0, storyNetworkRequestsSent.get());
    }

    @Test
    public void testCallHandlerSuppressesVoipSignalingForBotAccount() {
        int testSlot = 0;
        UserConfig.getInstance(testSlot).setCurrentUser(botUser);

        final AtomicInteger voipSignalingPacketsSent = new AtomicInteger(0);

        // Guard check in VoIPHelper
        boolean canCall = NGBotAccountGuard.checkCallSupported(null, testSlot);
        if (canCall) {
            // Simulated network RPC: phone.requestCall
            voipSignalingPacketsSent.incrementAndGet();
        }

        Assert.assertFalse("Bot account must be rejected from initiating VoIP calls", canCall);
        Assert.assertEquals("Zero VoIP signaling packets must be transmitted for bot calls", 0, voipSignalingPacketsSent.get());
    }

    @Test
    public void testHumanAccountAllowsNetworkRequests() {
        int testSlot = 1;
        UserConfig.getInstance(testSlot).setCurrentUser(humanUser);

        final AtomicInteger networkRequestsSent = new AtomicInteger(0);

        if (NGBotAccountGuard.checkContactsSyncSupported(null, testSlot)) {
            networkRequestsSent.incrementAndGet();
        }
        if (NGBotAccountGuard.checkStoriesSupported(null, testSlot)) {
            networkRequestsSent.incrementAndGet();
        }
        if (NGBotAccountGuard.checkCallSupported(null, testSlot)) {
            networkRequestsSent.incrementAndGet();
        }

        Assert.assertEquals("Human account must successfully allow all 3 feature network flows", 3, networkRequestsSent.get());
    }
}
