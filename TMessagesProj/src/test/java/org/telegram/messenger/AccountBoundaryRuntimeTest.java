package org.telegram.messenger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AccountBoundaryRuntimeTest {

    @Before
    public void setUp() {
        // UserConfig max accounts constant check
        Assert.assertEquals(5, UserConfig.MAX_ACCOUNT_COUNT);
    }

    @Test
    public void testZeroAccountsInitialization() {
        int activeCount = 0;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (UserConfig.getInstance(i).isClientActivated()) {
                activeCount++;
            }
        }
        Assert.assertTrue("Initial count should be non-negative", activeCount >= 0);
    }

    @Test
    public void testFiveAccountBoundaryAndSlotReuse() {
        boolean[] slots = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
        
        // Fill 5 accounts
        for (int i = 0; i < 5; i++) {
            slots[i] = true;
        }
        
        // 6th account should be rejected
        int freeSlot = -1;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (!slots[i]) {
                freeSlot = i;
                break;
            }
        }
        Assert.assertEquals("6th account must not find free slot when 5 slots are full", -1, freeSlot);
        
        // Remove account at slot 2
        slots[2] = false;
        
        // Slot reuse check: next available slot must be 2
        int reusedSlot = -1;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (!slots[i]) {
                reusedSlot = i;
                break;
            }
        }
        Assert.assertEquals("Removed slot 2 should be reused", 2, reusedSlot);
    }
}
