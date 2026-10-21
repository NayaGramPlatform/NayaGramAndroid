#!/usr/bin/env python3
"""RFC 6238 regression vectors and NGSafety integration guardrails."""
import hashlib
import hmac
import pathlib
import struct

ROOT = pathlib.Path(__file__).resolve().parents[1]
JAVA = ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/security/NGSafety.java"
UI = ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/security/NGSafetyActivity.java"
PRIVACY = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/PrivacySettingsActivity.java"
TABS = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/MainTabsActivity.java"
LAUNCH = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java"
GRADLE = ROOT / "TMessagesProj/build.gradle"
PASSKEYS = ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/PasskeysController.java"
BUILD_VARS = ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java"
PASSKEYS_UI = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/PasskeysActivity.java"


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
    passkeys = PASSKEYS.read_text(encoding="utf-8")
    build_vars = BUILD_VARS.read_text(encoding="utf-8")
    passkeys_ui = PASSKEYS_UI.read_text(encoding="utf-8")

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
    assert 'private static final String KEY_SESSION_VERIFIED = "session_verified"' in java
    assert 'putBoolean(KEY_SESSION_VERIFIED, true)' in java, "Successful verification must persist session state"
    assert 'sessionVerified = prefs(context).getBoolean(KEY_SESSION_VERIFIED, false)' in java, "A process restart must restore a verified session when timeout has not expired"
    assert "KEY_LAST_VERIFIED_TIME" in java, "Successful verification time must be persisted"
    assert "BarcodeFormat.QR_CODE" in ui and "Copy Secret" in ui
    assert "NGSafety.cancelSetup(context)" in ui
    assert 'verificationError.animate().alpha(1f).translationY(0f).setDuration(220).start();' in ui
    assert 'codeInput.setError("Invalid or already-used code")' not in ui, "Invalid-code feedback must not use the unreadable system error tooltip"
    assert "NGSafetyActivity" in privacy and "guardRow = rowCount++" in privacy
    # Authenticator is a navigation row, not a toggle; opening its management screen must not disable it.
    assert "presentFragment(new NGSafetyActivity());" in privacy
    assert "textCell2.setTextAndValueAndIcon(getString(R.string.NayaGramGuardTitle), guardStatus, true, R.drawable.ic_nayagram_authenticator, true);" in privacy
    assert "textCell2.setContentDescription(getString(R.string.NayaGramGuardTitle) + \", \" + guardStatus);" in privacy
    assert "if (position == guardRow) {\\n                return 3;\\n            }" not in privacy
    assert "NGSafety.isEnabled" in tabs and "new NGSafetyActivity(true)" in tabs
    assert "NGSafety.onAppBackground(this);" in launch, "Guard must process app background lifecycle"
    assert "NGSafety.onAppForeground(this);" in launch, "Guard must process app foreground lifecycle"
    activity_resume_start = launch.index("protected void onResume()")
    native_passcode_check = launch.index("onPasscodeResume();", activity_resume_start)
    guard_restore_before_fragments = launch.index("NGSafety.onAppForeground(this);", activity_resume_start)
    custom_fragments_resume = launch.index("actionBarLayout.onResume();", activity_resume_start)
    assert native_passcode_check < guard_restore_before_fragments < custom_fragments_resume, "Restore NG Guard before custom fragments resume, but only after Telegram native passcode state is known"
    assert "NGSafety.isEnabled(LaunchActivity.this) && !NGSafety.isSessionVerified()" in launch, "Guard must re-check after Telegram passcode unlock"
    assert "if (now < lastBg) return true;" in java, "Auto-lock must fail closed on clock rollback"
    # Changing a timeout while foregrounded must not inherit an old background timestamp.
    setter_start = java.index("public static boolean setAutoLockMinutes")
    setter_end = java.index("public static String getAutoLockDisplayString", setter_start)
    setter = java[setter_start:setter_end]
    assert '.putLong(KEY_LAST_BG_TIME, 0L)' in setter, "Saving a new timeout must clear stale background timing"
    background_start = java.index("public static void onAppBackground")
    background_end = java.index("public static void onAppForeground", background_start)
    background = java[background_start:background_end]
    assert "backgroundElapsedRealtime = SystemClock.elapsedRealtime();" in background
    assert "runOnUIThread" not in background and "Runnable" not in background, "Auto-lock must not depend on a delayed UI callback that can race lifecycle transitions"
    foreground_start = java.index("public static void onAppForeground")
    foreground_end = java.index("public static boolean isAutoLockExpired", foreground_start)
    foreground = java[foreground_start:foreground_end]
    assert "boolean expired = enabled && isAutoLockExpired(context);" in foreground
    assert "if (expired)" in foreground and "lockSession(context);" in foreground
    assert "prefs(context).edit().putLong(KEY_LAST_BG_TIME, 0L).apply();" in foreground, "Foreground must clear stale timestamps after expiry is evaluated"
    assert "FileLog.e(e);\n            return true;" in java, "Auto-lock storage errors must fail closed"
    assert 'throw new Exception("Failed to migrate legacy NG Safety preferences", e);' in java, "Legacy migration errors must not be cached as disabled"
    assert "cachedPreferences = primary;" in java, "Encrypted preferences should be reused across lifecycle calls"
    assert "int autoLockMinutes = secure.getInt(KEY_AUTOLOCK_MINUTES, DEFAULT_AUTOLOCK_MINUTES);" in java
    assert "editor.putInt(KEY_AUTOLOCK_MINUTES, autoLockMinutes).apply();" in java, "Disabling NG Guard must preserve the auto-lock preference"
    assert 'mins + (mins == 1 ? " minute" : " minutes")' in java, "One-minute duration must use singular grammar"
    assert "topFragment instanceof NGSafetyActivity" in launch
    assert "private TextView autoLockSubtitle;" in ui
    auto_lock_ui = (ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/security/NGAutoLockActivity.java").read_text(encoding="utf-8")
    assert "optionsExpanded = true;" in auto_lock_ui and "optionsPanel.setVisibility(View.VISIBLE);" in auto_lock_ui, "Auto-lock choices must be visible when the page opens"
    login_ui = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/LoginActivity.java").read_text(encoding="utf-8")
    assert 'if (subtitleView != null && "CANCELLED".equals(err))' not in login_ui, "Cancelled passkey prompt must not leave a stale login title"
    assert "NGSafety.getAutoLockMinutes(getParentActivity())" in ui, "Auto-lock subtitle must refresh when settings resumes"
    assert "NGSafety.onAppForeground(LaunchActivity.this);" in launch, "Native passcode dismissal must re-evaluate NG auto-lock"
    delegate_start = launch.index("PasscodeView.PasscodeViewDelegate delegate = view -> {")
    foreground_recheck = launch.index("NGSafety.onAppForeground(LaunchActivity.this);", delegate_start)
    gate_recheck = launch.index("NGSafety.isEnabled(LaunchActivity.this) && !NGSafety.isSessionVerified()", delegate_start)
    assert foreground_recheck < gate_recheck, "Reconcile auto-lock before restoring the main interface"
    content_restore = launch.index("actionBarLayout.getView().setVisibility(View.VISIBLE);", delegate_start)
    fragment_rebuild = launch.index("actionBarLayout.rebuildFragments(INavigationLayout.REBUILD_FLAG_REBUILD_LAST);", delegate_start)
    passcode_dismissed = launch.index("NotificationCenter.passcodeDismissed", delegate_start)
    assert gate_recheck < content_restore and gate_recheck < fragment_rebuild and gate_recheck < passcode_dismissed, "NG gate must run before Telegram content is restored"
    assert "retain the persisted background" in launch, "Do not clear auto-lock timing while Telegram passcode is still visible"
    assert "dev.samstevens.totp:totp:1.7.1" in gradle
    assert "androidx.security:security-crypto" in gradle
    print("PASS: NGSafety storage, UI, settings, login-gate and dependency guardrails")

    # Keep passkeys on native Credential Manager and the server challenge/verification flow.
    for token in (
        "CredentialManager.create(context)",
        "new TL_account.initPasskeyRegistration()",
        "new TL_account.registerPasskey()",
        "new TL_account.initPasskeyLogin()",
        "new TL_account.finishPasskeyLogin()",
        "CreatePublicKeyCredentialRequest(requestJson)",
        "new GetPublicKeyCredentialOption(requestJson)",
        "BUNDLE_KEY_REGISTRATION_RESPONSE_JSON",
        "BUNDLE_KEY_AUTHENTICATION_RESPONSE_JSON",
        "passkeyResponse.attestation_object",
        "passkeyResponse.signature",
        "ConnectionsManager.getInstance(currentAccount).sendRequestTyped",
    ):
        assert token in passkeys, f"Missing native/server-verified passkey flow: {token}"
    assert "androidx.credentials:credentials:" in gradle
    assert "androidx.credentials:credentials-play-services-auth:" in gradle
    assert "if (!BuildVars.SUPPORTS_PASSKEYS) return;" in passkeys
    # Passkey UI is intentionally hidden from Privacy & Security; the underlying
    # Telegram Credential Manager/server-verified implementation remains untouched.
    assert "passkeysRow = rowCount++" not in privacy
    assert "loadPasskeys();" not in privacy
    assert "new PasskeysActivity(currentPasskeys)" not in privacy
    assert "R.drawable.menu_passkey_add" not in privacy
    assert "getString(R.string.NGPasskeyTitle)" not in privacy
    print("PASS: NG Passkey is hidden from Privacy & Security without altering Telegram auth")
    print("PASS: Android Credential Manager passkey registration/login stays server-verified")

    # NG Hub privacy regression: this legacy helper must never silently join or pin channels.
    auto_pin = (ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/AutoPinChannelManager.java").read_text(encoding="utf-8")
    assert "Intentionally disabled: never auto-join or auto-pin a channel." in auto_pin
    assert "TL_channels_joinChannel" not in auto_pin
    assert "controller.pinDialog" not in auto_pin
    print("PASS: NG Hub never auto-joins or auto-pins an external channel")

    # Telegram's server-provided premium_required flag must remain enforced for Send As.
    sender_select = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/SenderSelectPopup.java").read_text(encoding="utf-8")
    assert "peerObj.premium_required && !UserConfig.getInstance(currentAccount).isPremium()" in sender_select, "Send As must preserve Telegram Premium gating"
    print("PASS: Send As respects Telegram's premium_required entitlement")
    send_as_hint = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/SelectSendAsPremiumHintBulletinLayout.java").read_text(encoding="utf-8")
    assert "background.setColor(0xFFFFFFFF)" in send_as_hint
    assert "setCustomBackground(background)" in send_as_hint
    assert "textView.setTextColor(0xFF0F172A)" in send_as_hint, "Group/channel Send As hint must stay readable on a light background"
    print("PASS: Group/channel Send As hint uses a readable NayaGram light theme")

if __name__ == "__main__":
    main()
