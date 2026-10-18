#!/usr/bin/env python3
"""Static regression checks preventing fail-open biometric chat unlocks."""
from pathlib import Path

source = Path(
    "TMessagesProj/src/main/java/org/nayagram/platform/security/BiometricChatLocker.java"
).read_text(encoding="utf-8")

no_security_start = source.index("if (!isDeviceSecurityAvailable(activity))")
no_security_end = source.index("if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)", no_security_start)
no_security = source[no_security_start:no_security_end]

fallback_start = source.index("// Fail closed: never grant access when the platform cannot authenticate.")
fallback = source[fallback_start:]

assert "grantSessionAccess(dialogId)" not in no_security, (
    "Chat Locker must not unlock chats when device screen lock is unavailable"
)
assert "onUnlockFailed()" in no_security, "Missing fail-closed callback for insecure devices"
assert "grantSessionAccess(dialogId)" not in fallback, (
    "Unsupported Android versions or prompt failures must never grant chat access"
)
assert "onUnlockFailed()" in fallback, "Missing fail-closed callback for unsupported authentication"
assert "onAuthenticationSucceeded" in source and "grantSessionAccess(dialogId)" in source, (
    "Successful platform authentication must still grant session access"
)
print("PASS: Chat Locker fails closed without device authentication")
