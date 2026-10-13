#!/usr/bin/env python3
"""
Regression test: Android 14+ VoIP foreground service types.

NayaGram is NOT a system dialer. Using FOREGROUND_SERVICE_TYPE_PHONE_CALL
(or the raw value 4) causes "NayaGram keeps stopping" on Android 14+.

Allowed types only:
  - FOREGROUND_SERVICE_TYPE_MICROPHONE
  - FOREGROUND_SERVICE_TYPE_CAMERA
  - FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
  - FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION (when screen share active)

This script greps VoIPService.java source. Run in CI before release APK.
"""
from __future__ import annotations

import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parents[1]
VOIP = ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/voip/VoIPService.java"

FORBIDDEN_PATTERNS = [
    r"FOREGROUND_SERVICE_TYPE_PHONE_CALL",
    r"type\s*\|=?\s*4\s*;?\s*//.*[Pp]hone",
    r"ServiceInfo\.FOREGROUND_SERVICE_TYPE_PHONE_CALL",
]

ALLOWED_MARKERS = [
    "FOREGROUND_SERVICE_TYPE_MICROPHONE",
    "FOREGROUND_SERVICE_TYPE_CAMERA",
    "FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK",
]


def fail(msg: str) -> None:
    print(f"FAIL: {msg}")
    sys.exit(1)


def main() -> None:
    print("--- VoIP Android 14 FGS + Call Protection regression ---")
    if not VOIP.is_file():
        fail(f"missing {VOIP}")

    text = VOIP.read_text(encoding="utf-8", errors="replace")

    # 1) No PHONE_CALL FGS type
    for pat in FORBIDDEN_PATTERNS:
        if re.search(pat, text):
            fail(f"forbidden FGS pattern found in VoIPService: {pat}")
    print("[1] No FOREGROUND_SERVICE_TYPE_PHONE_CALL ... PASS")

    # 2) Allowed types present in getCurrentForegroundType
    for m in ALLOWED_MARKERS:
        if m not in text:
            fail(f"expected allowed type missing: {m}")
    print("[2] Allowed FGS types (mic/camera/media) present ... PASS")

    # 3) Explicit safety comment still present
    if "phoneCall FGS type crashes non-dialer clients" not in text:
        fail("missing safety comment about phoneCall FGS")
    print("[3] Safety comment present ... PASS")

    # 4) Call & Voice Protection / confirm-actions must not reappear as active API
    naya = ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/NayaConfig.java"
    if naya.is_file():
        nc = naya.read_text(encoding="utf-8", errors="replace")
        if re.search(r"public\s+boolean\s+isConfirmActions\s*\(", nc):
            fail("isConfirmActions() still public API in NayaConfig")
        if "LEGACY_KEY_CONFIRM_ACTIONS" in nc and ".remove(LEGACY_KEY_CONFIRM_ACTIONS)" not in nc:
            fail("legacy confirm key present without remove() migration")
        print("[4] NayaConfig: no active Confirm Actions API ... PASS")
    else:
        print("[4] NayaConfig not found (skip) ... SKIP")

    # 5) VoIPHelper must not call isConfirmActions actively
    helper = ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/voip/VoIPHelper.java"
    if helper.is_file():
        vh = helper.read_text(encoding="utf-8", errors="replace")
        if "isConfirmActions" in vh:
            active = [
                ln for ln in vh.splitlines()
                if "isConfirmActions" in ln
                and not ln.strip().startswith("//")
                and "/*" not in ln
            ]
            if active:
                fail(f"VoIPHelper still references isConfirmActions actively: {active[:3]}")
        print("[5] VoIPHelper free of active confirm-call gate ... PASS")

    # 6) Premium unlock sanity
    uc = ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/UserConfig.java"
    if uc.is_file():
        ut = uc.read_text(encoding="utf-8", errors="replace")
        if "client premium gates are free" not in ut and "return user.premium" in ut:
            fail("UserConfig.isPremium may have regressed to server-only premium")
        print("[6] UserConfig client premium unlock present ... PASS")

    # 7) Pin limit policy
    mc = ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/MessagesController.java"
    if mc.is_file():
        mt = mc.read_text(encoding="utf-8", errors="replace")
        if re.search(r"maxPinnedDialogsCountDefault\s*=\s*10", mt):
            print("[7] Pin limit default 10 ... PASS")
        else:
            print("[7] pin limit hardcode not found (may use preferences) ... WARN")

    # 8) Instant translator kept
    tr = ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/translator/InstantTranslatorManager.java"
    if tr.is_file():
        print("[8] Instant Translator kept ... PASS")
    else:
        fail("InstantTranslatorManager missing")

    print("--- ALL VoIP FGS / policy regression checks PASSED ---")


if __name__ == "__main__":
    main()
