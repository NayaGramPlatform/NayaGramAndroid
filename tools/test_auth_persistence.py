#!/usr/bin/env python3
import sys
import re
import pathlib

def test_auth_activation_persistence():
    print("[TEST] Running Authentication Activation & Session Persistence Regression Suite...")
    p = pathlib.Path(__file__).parent.parent / "TMessagesProj" / "src" / "main" / "java" / "org" / "telegram" / "messenger" / "UserConfig.java"
    assert p.exists(), f"UserConfig.java not found at {p}"
    
    content = p.read_text(encoding="utf-8")
    
    # Locate isClientActivated method
    match = re.search(r'public\s+boolean\s+isClientActivated\s*\(\)\s*\{([^}]+)\}', content)
    assert match, "isClientActivated() method definition not found in UserConfig.java"
    
    body = match.group(1).strip()
    print(f"  [AUDIT] Found isClientActivated body: {body}")
    
    # Ensure it returns currentUser != null
    assert "currentUser != null" in body, "isClientActivated must check currentUser != null"
    
    # Ensure it DOES NOT require currentUser.premium (which would invalidate non-premium sessions)
    assert "currentUser.premium" not in body, "CRITICAL ERROR: isClientActivated must not require currentUser.premium!"
    assert "isPremium" not in body, "CRITICAL ERROR: isClientActivated must not require isPremium!"
    
    print("  [PASS] isClientActivated decoupled from premium status: non-premium user sessions persist across restarts.")
    print("[SUCCESS] All auth persistence regression tests PASSED.")
    return 0

if __name__ == '__main__':
    sys.exit(test_auth_activation_persistence())
