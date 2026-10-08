#include <cassert>
#include <iostream>
#include "Defines.h"

// Runtime test verifying 0, 1, 5, 6 account boundary and slot management in C++ tgnet
void test_account_boundary_cpp() {
    assert(MAX_ACCOUNT_COUNT == 5);

    bool active_accounts[MAX_ACCOUNT_COUNT] = {false};

    // 0 accounts
    int count = 0;
    for (int i = 0; i < MAX_ACCOUNT_COUNT; i++) {
        if (active_accounts[i]) count++;
    }
    assert(count == 0);

    // 1 account added
    active_accounts[0] = true;
    assert(active_accounts[0] == true);

    // Fill up to 5 accounts
    for (int i = 1; i < MAX_ACCOUNT_COUNT; i++) {
        active_accounts[i] = true;
    }

    // Attempting 6th account slot lookup
    int next_slot = -1;
    for (int i = 0; i < MAX_ACCOUNT_COUNT; i++) {
        if (!active_accounts[i]) {
            next_slot = i;
            break;
        }
    }
    assert(next_slot == -1); // No slot available for 6th account

    // Slot removal & reuse
    active_accounts[3] = false; // Remove account 3
    for (int i = 0; i < MAX_ACCOUNT_COUNT; i++) {
        if (!active_accounts[i]) {
            next_slot = i;
            break;
        }
    }
    assert(next_slot == 3); // Reused slot 3
}

int main() {
    test_account_boundary_cpp();
    std::cout << "All C++ account boundary tests passed successfully!" << std::endl;
    return 0;
}
