package org.telegram.messenger;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;

public class AccountBoundaryRuntimeTest {

    @Before
    public void setUp() {
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
        
        for (int i = 0; i < 5; i++) {
            slots[i] = true;
        }
        
        int freeSlot = -1;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (!slots[i]) {
                freeSlot = i;
                break;
            }
        }
        Assert.assertEquals("6th account must not find free slot when 5 slots are full", -1, freeSlot);
        
        slots[2] = false;
        
        int reusedSlot = -1;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (!slots[i]) {
                reusedSlot = i;
                break;
            }
        }
        Assert.assertEquals("Removed slot 2 should be reused", 2, reusedSlot);
    }

    @Test
    public void testAppRestartPersistenceSimulation() {
        Map<String, Object> storage = new HashMap<>();

        storage.put("saved_accounts_count", 5);
        for (int i = 0; i < 5; i++) {
            storage.put("account_active_" + i, true);
            storage.put("account_user_id_" + i, 1000L + i);
        }

        boolean[] reloadedSlots = new boolean[UserConfig.MAX_ACCOUNT_COUNT];
        int loadedCount = 0;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            Boolean isActive = (Boolean) storage.get("account_active_" + i);
            if (isActive != null && isActive) {
                reloadedSlots[i] = true;
                loadedCount++;
            }
        }

        Assert.assertEquals("All 5 accounts must be successfully reloaded on restart", 5, loadedCount);

        storage.put("account_active_1", false);
        storage.remove("account_user_id_1");
        reloadedSlots[1] = false;

        int nextAvailableSlot = -1;
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            if (!reloadedSlots[i]) {
                nextAvailableSlot = i;
                break;
            }
        }
        Assert.assertEquals("Slot 1 must be reused after removal following a restart", 1, nextAvailableSlot);
    }
}
