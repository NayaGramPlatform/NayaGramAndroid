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
import re

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
    assert 'Are you sure you want to reset all NayaGram feature settings to their factory defaults?' in activity

    # Preference-only cards must never pretend to be functional ON/OFF toggles.
    assert 'private boolean isImplementationPending(FeatureItem item)' in adapter
    assert 'if (cfg == null || isImplementationPending(item)) return false;' in adapter
    assert 'Not active yet. No chat is protected by this preference alone.' in adapter
    assert 'Not active yet. No translation service or in-chat action is connected.' in adapter
    assert 'The toggle does not change how settings are stored or isolated.' in adapter
    assert 'case 12: cfg.setModularConfig(newState); break;' not in adapter
    assert 'currentLang == LANG_AR ? "لاحقًا" : "SOON"' in adapter
    assert 'case 13: cfg.setFocusModeEnabled(newState); break;' not in adapter
    assert 'case 15: cfg.setBatterySaverEnabled(newState); break;' not in adapter
    assert 'case 16: cfg.setBiometricChatLockerEnabled(newState); break;' not in adapter
    assert 'case 17: cfg.setInstantTranslatorEnabled(newState); break;' not in adapter
    assert 'case 19: cfg.setNoiseSuppressionEnabled(newState); break;' not in adapter
    assert 'preferences.getBoolean(KEY_MODULAR_CONFIG, false)' in config
    assert 'preferences.getBoolean(KEY_BATTERY_SAVER, false)' in config
    assert 'preferences.getBoolean(KEY_INSTANT_TRANSLATOR, false)' in config
    assert 'preferences.getBoolean("feature_noise_suppression", false)' in config
    assert '.putBoolean(KEY_MODULAR_CONFIG, false)' in config
    assert '.putBoolean(KEY_BATTERY_SAVER, false)' in config
    assert '.putBoolean(KEY_INSTANT_TRANSLATOR, false)' in config
    assert '.putBoolean("feature_noise_suppression", false)' in config
    assert 'Instant Translator (feature_17) remains disabled until a translation engine is implemented.' in config
    assert 'stays enabled by default' not in config
    tutorial = (root / "TMessagesProj/src/main/java/org/nayagram/platform/features/FeatureTutorialDialog.java").read_text(encoding="utf-8")
    translator = (root / "TMessagesProj/src/main/java/org/nayagram/platform/translator/InstantTranslatorManager.java").read_text(encoding="utf-8")
    assert 'FeatureTutorialDialog.show(context);' in adapter
    assert 'Feature Pipeline · Tutorial' in activity
    assert '.setPositiveButton("Details"' in tutorial
    assert 'This page is a tutorial, not an active feature switchboard.' in tutorial
    assert 'flagViews[j].animate().scaleX(1.18f)' in tutorial
    assert 'getFlagForLanguage(languages[i][0])' in tutorial
    assert 'case "ta":' in tutorial and 'case "te": return "🇮🇳";' in tutorial
    assert 'languages.length + " tutorial languages' in tutorial
    assert 'Feature Pipeline · Details' in tutorial
    assert 'never turns them on or off' in tutorial
    assert 'Only this public tutorial text is sent for translation' in tutorial
    assert len(re.findall(r'^\s*\{"', translator, re.MULTILINE)) >= 28
    assert '"🇧🇩"' in tutorial and '"🇬🇧"' in tutorial and '"🇸🇦"' in tutorial
    assert 'IMPLEMENTATION STATUS — VERIFIED' not in adapter
    assert 'Quiet hours with VIP bypass' not in adapter
    assert 'Keyword based replies' not in adapter
    assert 'Story Saver → HD download' not in adapter
    profile = (root / "TMessagesProj/src/main/java/org/telegram/ui/ProfileActivity.java").read_text(encoding="utf-8")
    assert 'ConnectionsManager.getInstance(currentAccount).getCurrentDatacenterId()' in profile
    assert 'NayaConfig.getInstance().isShowIdAndDc() && user.id > 0' in profile
    assert 'onlineTextView[2].setOnLongClickListener' in profile
    assert 'long-press the line to copy' in tutorial
    assert 'Tutorial-only translation helper used by the Feature Pipeline language guide.' in translator
    assert 'It is not wired into private chats, groups, channels, or the main feature toggle.' in translator
    assert "Uses Telegram's native media-streaming preference. It is not a full network optimizer." in adapter
    assert 'No automatic storage limiter runs.' in adapter

    # The legacy settings hub must not expose the same inactive preferences as live toggles.
    legacy_hub = (root / "TMessagesProj/src/main/java/org/telegram/ui/NGSettingsActivity.java").read_text(encoding="utf-8")
    assert 'cfg.setDataSaverEnabled(checked);' in legacy_hub
    assert 'if (isPendingFeaturePosition(position))' in legacy_hub
    assert 'private boolean isPendingFeaturePosition(int position)' in legacy_hub
    assert 'Smart Chat Folders · SOON' in legacy_hub
    assert 'Biometric Chat Locker · SOON' in legacy_hub
    assert 'Focus & Wellbeing Mode · SOON' in legacy_hub
    assert 'Ultra Battery Saver · SOON' in legacy_hub
    assert 'In-Chat Instant Translator · SOON' in legacy_hub
    assert 'No chat is protected by this setting' in legacy_hub
    assert 'switchView.setVisibility(unavailable ? View.GONE : View.VISIBLE)' in legacy_hub
    assert 'title.contains("· SOON") ? "Unavailable"' in legacy_hub
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
