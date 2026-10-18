#!/usr/bin/env python3
"""RFC 6238 regression vectors and NayaGram Guard integration guardrails."""
import hashlib
import hmac
import pathlib
import struct

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/security/NayaGramGuard.java"
UI = ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/security/NayaGramGuardActivity.java"
PRIVACY = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/PrivacySettingsActivity.java"
TABS = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java"
LAUNCH = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java"
GRADLE = ROOT / "TMessagesProj/build.gradle"


def totp(secret: bytes, timestamp: int, digits: int = 6, step: int = 30) -> str:
    counter = timestamp // step
    digest = hmac.new(secret, struct.pack(">Q", counter), hashlib.sha1).digest()
    offset = digest[-1] & 0x0F
    binary = struct.unpack(">I", digest[offset:offset + 4])[0] & 0x7FFFFFFF
    return str(binary % (10 ** digits)).zfill(digits)


def main() -> None:
    # RFC 6238 Appendix B, SHA-1, last six digits of the published 8-digit codes.
    secret = b"12345678901234567890"
    vectors = {
        59: "287082",
        1111111109: "081804",
        1111111111: "050471",
        1234567890: "005924",
        2000000000: "279037",
        20000000000: "353130",
    }
    for timestamp, expected in vectors.items():
        actual = totp(secret, timestamp)
        assert actual == expected, f"RFC 6238 vector at {timestamp}: {actual} != {expected}"
    print(f"PASS: {len(vectors)} RFC 6238 SHA-1 TOTP vectors")

    java = JAVA.read_text(encoding="utf-8")
    ui = UI.read_text(encoding="utf-8")
    privacy = PRIVACY.read_text(encoding="utf-8")
    tabs = TABS.read_text(encoding="utf-8")
    launch = LAUNCH.read_text(encoding="utf-8")
    gradle = GRADLE.read_text(encoding="utf-8")

    for token in (
        "EncryptedSharedPreferences.create",
        "MasterKey.KeyScheme.AES256_GCM",
        "PrefKeyEncryptionScheme.AES256_SIV",
        "PrefValueEncryptionScheme.AES256_GCM",
        "new DefaultSecretGenerator(32)",
        "public static void cancelSetup(Context context)",
        "verifier.setTimePeriod(30)",
        "verifier.setAllowedTimePeriodDiscrepancy(1)",
        "private static final int BACKUP_COUNT = 15",
        "Local TOTP only, no data sent to Google server",
    ):
        assert token in java, f"Missing security requirement: {token}"
    assert "getBoolean(ENABLED, false)" in java, "Guard must default OFF"
    assert "BarcodeFormat.QR_CODE" in ui and "Copy Secret" in ui
    assert "NayaGramGuard.cancelSetup(context)" in ui
    assert "NayaGramGuardActivity" in privacy and "guardRow = rowCount++" in privacy
    # Guard is a navigation row, not a toggle; opening its management screen must not disable it.
    assert "presentFragment(new NayaGramGuardActivity());" in privacy
    assert "textCell2.setTextAndValueAndIcon(getString(R.string.NayaGramGuardTitle), guardStatus, true, R.drawable.ic_nayagram_authenticator, true);" in privacy
    assert "textCell2.setContentDescription(getString(R.string.NayaGramGuardTitle) + \", \" + guardStatus);" in privacy
    assert "if (position == guardRow) {\\n                return 3;\\n            }" not in privacy
    assert "setTextAndCheck(\"NayaGram Guard (Google Authenticator)\"" not in privacy
    assert "NayaGramGuard.isEnabled" in tabs and "new NayaGramGuardActivity(true)" in tabs
    assert "NayaGramGuard.lockSession();" in launch
    assert "topFragment instanceof NayaGramGuardActivity" in launch
    assert "dev.samstevens.totp:totp:1.7.1" in gradle
    assert "androidx.security:security-crypto" in gradle
    print("PASS: Guard storage, UI, settings, login-gate and dependency guardrails")


if __name__ == "__main__":
    main()
