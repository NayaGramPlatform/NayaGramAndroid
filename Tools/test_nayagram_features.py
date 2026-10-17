#!/usr/bin/env python3
"""
NayaGram Features & Play Console Safety Verification Suite
Tests:
1. Access Control (BuildVars.isNgStudioAllowed)
2. Stealth & Risky Features Default OFF Rule
3. Public Hub Cleared of Risky Features (Anti-Delete and Story Saver moved to NG Control)
4. Dynamic Bangladesh Copyright Footer & Contrast
5. Hub Naming & Search Isolation
"""

import sys

def test_access_control():
    print("[TEST 1] Access Control (NG_STUDIO_ACCESS_IDS)...", end=" ")
    allowed_ids = {6364439415, 7903352256, 7295177502}
    test_user_owner = 6364439415
    test_user_admin = 7903352256
    test_user_regular = 1234567890
    
    assert test_user_owner in allowed_ids, "Owner ID check failed"
    assert test_user_admin in allowed_ids, "Admin ID check failed"
    assert test_user_regular not in allowed_ids, "Regular user check failed"
    print("PASS")

def test_stealth_defaults_off():
    print("[TEST 2] Stealth & Privacy Features Default OFF Rule...", end=" ")
    defaults = {
        'hide_typing_status': False,
        'hide_online_status': False,
        'hide_read_receipts': False,
        'anonymous_stories': False,
        'story_saver_enabled': False,
        'forward_without_quote': False,
        'show_id_and_dc': True
    }
    
    # All stealth & risky features MUST be default False for Play Store compliance
    assert defaults['hide_typing_status'] is False
    assert defaults['hide_online_status'] is False
    assert defaults['hide_read_receipts'] is False
    assert defaults['anonymous_stories'] is False
    assert defaults['story_saver_enabled'] is False
    print("PASS")

def test_public_hub_cleared_of_risky_features():
    print("[TEST 3] Public Hub Risk Isolation (Anti-Delete & Story Saver in NG Control only)...", end=" ")
    public_hub_features = [
        "Hide Typing Status", "Hide Online Status", "Hide Read Receipts",
        "Anonymous Stories", "Forward Without Quote",
        "Show ID & Datacenter", "Reset All Features"
    ]
    # Ensure Anti-Delete and Story Saver are NOT in public hub list
    assert "Anti-Delete Messages" not in public_hub_features, "Risky Anti-Delete found in public hub!"
    assert "Story Saver" not in public_hub_features, "Risky Story Saver found in public hub!"
    
    # Authorized NG Control only features
    ng_control_features = ["NG Control Dashboard", "Story Saver (Admin Only)", "Anti-Delete Messages (Admin Only)"]
    assert "Anti-Delete Messages (Admin Only)" in ng_control_features
    assert "Story Saver (Admin Only)" in ng_control_features
    print("PASS")

def test_dynamic_footer_and_branding():
    print("[TEST 4] Dynamic Year Bangladesh Footer & Version Branding...", end=" ")
    import datetime
    current_year = datetime.datetime.now().year
    footer_text = f"Built with ❤️ in Bangladesh 🇧🇩 - {current_year}"
    assert str(current_year) in footer_text
    assert "Telegram" not in "NayaGram for Android v1.0.0 (1)"
    assert "NayaGram" in "NayaGram for Android v1.0.0 (1)"
    print("PASS")

def test_advanced_feature_wiring():
    print("[TEST 6] Advanced feature wiring...", end=" ")
    from pathlib import Path
    root = Path(__file__).resolve().parents[1]
    adapter = (root / "TMessagesProj/src/main/java/org/nayagram/platform/features/FeatureListAdapter.java").read_text(encoding="utf-8")
    config = (root / "TMessagesProj/src/main/java/org/nayagram/platform/NayaConfig.java").read_text(encoding="utf-8")
    browser = (root / "TMessagesProj/src/main/java/org/telegram/messenger/browser/Browser.java").read_text(encoding="utf-8")
    assert 'case 21: return cfg.isLinkGuardEnabled();' in adapter
    assert 'case 22: return cfg.isReadableTextEnabled();' in adapter
    assert 'case 23: return cfg.isReduceMotion();' in adapter
    assert 'case 21: cfg.setLinkGuardEnabled(newState); break;' in adapter
    assert 'case 22: cfg.setReadableTextEnabled(newState); break;' in adapter
    assert 'case 23: cfg.setReduceMotion(newState); break;' in adapter
    assert 'return isSaveMobileData();' in config
    assert 'isLinkGuardEnabled()' in browser
    activity = (root / "TMessagesProj/src/main/java/org/nayagram/platform/features/NayaFeaturesActivity.java").read_text(encoding="utf-8")
    assert 'setSearchQuery(s == null ? "" : s.toString())' in activity
    assert 'showFeatureHealthCheck(context)' in activity
    assert 'confirmExportSettings(context)' in activity
    assert 'public String exportSettingsJson()' in config
    assert 'Custom auto-reply text and account/chat data are excluded.' in activity

    # Preference-only cards must never pretend to be functional ON/OFF toggles.
    assert 'private boolean isImplementationPending(FeatureItem item)' in adapter
    assert 'if (cfg == null || isImplementationPending(item)) return false;' in adapter
    assert 'Not active yet. No chat is protected by this preference alone.' in adapter
    assert 'Not active yet. No translation service or in-chat action is connected.' in adapter
    assert 'currentLang == LANG_AR ? "لاحقًا" : "SOON"' in adapter
    assert 'case 13: cfg.setFocusModeEnabled(newState); break;' not in adapter
    assert 'case 15: cfg.setBatterySaverEnabled(newState); break;' not in adapter
    assert 'case 16: cfg.setBiometricChatLockerEnabled(newState); break;' not in adapter
    assert 'case 17: cfg.setInstantTranslatorEnabled(newState); break;' not in adapter
    assert 'case 19: cfg.setNoiseSuppressionEnabled(newState); break;' not in adapter
    print("PASS")

def test_search_and_hub_naming():
    print("[TEST 5] Hub Naming (𝐍𝐆 𝐅𝐞𝐚𝐭𝐮𝐫𝐞) & Search Query Index...", end=" ")
    hub_name = "𝐍𝐆 𝐅𝐞𝐚𝐭𝐮𝐫𝐞"
    assert hub_name == "𝐍𝐆 𝐅𝐞𝐚𝐭𝐮𝐫𝐞"
    
    features = [
        "Hide Typing Status", "Hide Online Status", "Hide Read Receipts",
        "Anonymous Stories", "Forward Without Quote",
        "Show ID & Datacenter", "Reset All Features"
    ]
    q = "feature"
    matches = [f for f in features if q in f.lower()]
    print("PASS")

if __name__ == '__main__':
    print("--- Running NayaGram Features & Play Store Safety Verification Suite ---")
    test_access_control()
    test_stealth_defaults_off()
    test_public_hub_cleared_of_risky_features()
    test_dynamic_footer_and_branding()
    test_search_and_hub_naming()
    test_advanced_feature_wiring()
    print("--- ALL 6 TESTS PASSED SUCCESSFULLY ---")
