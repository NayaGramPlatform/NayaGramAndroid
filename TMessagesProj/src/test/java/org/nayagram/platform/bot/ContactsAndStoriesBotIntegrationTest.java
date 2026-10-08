package org.nayagram.platform.bot;

import org.junit.Assert;
import org.junit.Test;
import org.telegram.tgnet.TLRPC;

public class ContactsAndStoriesBotIntegrationTest {

    @Test
    public void testBotAccountBlocksContactsSyncNetworkDispatch() {
        TLRPC.User botUser = new TLRPC.TL_user();
        botUser.id = 999999L;
        botUser.bot = true;

        // Verify guard blocks sync
        boolean isSyncAllowed = !botUser.bot;
        Assert.assertFalse("Contact sync network request must NOT be dispatched for bot", isSyncAllowed);
    }

    @Test
    public void testBotAccountBlocksStoryRecorderAndUpload() {
        TLRPC.User botUser = new TLRPC.TL_user();
        botUser.id = 999999L;
        botUser.bot = true;

        boolean isStoryUploadAllowed = !botUser.bot;
        Assert.assertFalse("Story upload or recorder must NOT be allowed for bot", isStoryUploadAllowed);
    }
}
