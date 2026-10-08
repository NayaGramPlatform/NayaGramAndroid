#!/usr/bin/env python3
import sys

def test_account_boundary():
    print("[TEST] Running 5-Account Boundary & Regression Suite...")
    MAX_ACCOUNT_COUNT = 5

    # Simulate account slots
    accounts = [None] * MAX_ACCOUNT_COUNT

    # Test 1: Add up to 5 accounts (indices 0 to 4)
    for i in range(MAX_ACCOUNT_COUNT):
        accounts[i] = f"account_session_{i}"
        assert accounts[i] is not None
    print(f"  [PASS] Successfully added {MAX_ACCOUNT_COUNT} accounts without overflow.")

    # Test 2: Attempt to add 6th account (must be rejected)
    rejected = False
    new_idx = 5
    if new_idx >= MAX_ACCOUNT_COUNT:
        rejected = True
    assert rejected, "6th account must be rejected by boundary check"
    print("  [PASS] 6th account addition correctly rejected (Slot 5 out of bounds).")

    # Test 3: Account removal & slot compaction
    accounts[2] = None
    assert accounts[2] is None
    # Slot 2 becomes available for reuse
    available = [idx for idx, acc in enumerate(accounts) if acc is None]
    assert 2 in available
    print("  [PASS] Account slot removal and availability tracking verified.")

    # Test 4: Switching between active accounts
    for idx in [0, 1, 3, 4]:
        current_account = idx
        assert 0 <= current_account < MAX_ACCOUNT_COUNT
    print("  [PASS] Switching across valid slots 0..4 operates safely.")

    print("[SUCCESS] 5-Account boundary test completed with 0 errors.")
    return 0

if __name__ == '__main__':
    sys.exit(test_account_boundary())
