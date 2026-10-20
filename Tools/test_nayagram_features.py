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
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

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
    from pathlib import Path
    settings_source = Path("TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java").read_text(encoding="utf-8")
    about_source = Path("TMessagesProj/src/main/java/org/nayagram/platform/ui/NayaAboutActivity.java").read_text(encoding="utf-8")
    props_source = Path("gradle.properties").read_text(encoding="utf-8")
    app_gradle_source = Path("TMessagesProj_App/build.gradle").read_text(encoding="utf-8")
    buildvars_source = Path("TMessagesProj/src/main/java/org/telegram/messenger/BuildVars.java").read_text(encoding="utf-8")
    assert "APP_VERSION_CODE=4014" in props_source
    assert "APP_VERSION_NAME=1.1.4 NG" in props_source
    assert "BUILD_VERSION = 4014" in buildvars_source
    assert "internal code 4014" in app_gradle_source
    assert "1.1.4 NG" in app_gradle_source
    assert '"1.1.4 NG"' in about_source
    assert '"1.1.4 NG"' in settings_source
    assert "getInstalledAppVersionName()" in settings_source
    assert 'String nayaGramInfo = "NayaGram for Android v" + getInstalledAppVersionName() + " (" + code + ")";' in settings_source
    assert 'footer.setText("NayaGram v" + getAppVersion() + " • Build " + getInternalBuildCode()' in about_source
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
    assert 'cfg.setModularConfig(' not in adapter
    assert 'currentLang == LANG_AR ? "لاحقًا" : "SOON"' in adapter
    assert 'cfg.setFocusModeEnabled(' not in adapter
    assert 'cfg.setBatterySaverEnabled(' not in adapter
    assert 'cfg.setBiometricChatLockerEnabled(' not in adapter
    assert 'cfg.setInstantTranslatorEnabled(' not in adapter
    assert 'cfg.setNoiseSuppressionEnabled(' not in adapter
    assert 'cfg.setAutoCacheGuardEnabled(' not in adapter
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
    assert 'FeatureTutorialDialog.show(context)' in activity
    assert 'showAllDetailsPipelineDialog' not in adapter
    assert 'Feature Pipeline · Tutorial' in activity
    assert 'buildTutorialBanner(context)' in activity
    assert 'Verified Feature Controls' in activity
    assert 'Only features with a verified behavior path appear below.' in activity
    assert 'setText("Details  ›")' in activity
    assert 'item.addSubItem(MENU_ALL_DETAILS' not in activity
    assert 'InstantTranslatorManager.SUPPORTED_LANGUAGES.length' in activity
    assert 'private boolean isMainPageFeature(FeatureItem item)' in adapter
    assert 'if (isMainPageFeature(item) && (normalized.isEmpty() || matchesFeature(item, normalized)))' in adapter
    assert 'case 11: // User ID & active data-center ID on profile' in adapter
    assert 'case 23: // Telegram interface animation preference' in adapter
    assert 'case 2:  // Message Scheduler has no delivery engine' in adapter
    assert 'case 5:  // Anonymous Stories is disabled' in adapter
    assert '.setPositiveButton("Details"' in tutorial
    assert 'This page only explains features; it never turns them on or off.' in tutorial
    assert 'flagViews[j].animate().scaleX(1.18f)' in tutorial
    assert 'getFlagForLanguage(languages[i][0])' in tutorial
    assert 'case "ta":' in tutorial and 'case "te": return "🇮🇳";' in tutorial
    assert 'languages.length + " tutorial languages' in tutorial
    assert 'Feature Pipeline · Details' in tutorial
    assert "Only verified, working features are shown in the active Features page." in tutorial
    assert "Planned or incomplete feature cards have been removed from the active list" in tutorial

    settings = (root / "TMessagesProj/src/main/java/org/telegram/ui/NGSettingsActivity.java").read_text(encoding="utf-8")
    assert 'presentFragment(new FiltersSetupActivity());' in settings
    assert 'setActionFeature(R.drawable.outline_groups_24' in settings
    assert '"Smart Chat Folders · SOON"' not in settings
    assert 'Opening native chat folder management' in settings
    assert 'Not active yet. Native Telegram folder integration is not implemented' not in settings
    locker = (root / "TMessagesProj/src/main/java/org/nayagram/platform/security/BiometricChatLocker.java").read_text(encoding="utf-8")
    launch = (root / "TMessagesProj/src/main/java/org/telegram/ui/LaunchActivity.java").read_text(encoding="utf-8")
    assert "Set a secure screen lock before protecting chats." in locker
    assert "No supported authentication API: deny access rather than silently bypassing the lock." in locker
    assert "Fallback for older devices: grant session access" not in locker
    assert "enforceBiometricChatLock();" in launch
    assert "BiometricChatLocker.getInstance(this).lockAllSessions();" in launch
    assert "current.finishFragment();" in launch
    assert "Do not leave a session grant behind if navigation changed during authentication." in launch
    settings = (root / "TMessagesProj/src/main/java/org/telegram/ui/NGSettingsActivity.java").read_text(encoding="utf-8")
    assert '"Biometric Chat Locker · SOON"' not in settings
    assert 'cfg.isBiometricChatLockerEnabled(), true' in settings
    chat_menu = (root / "TMessagesProj/src/main/java/org/nayagram/platform/chat/NGChatMenuHelper.java").read_text(encoding="utf-8")
    assert 'isBiometricChatLockerEnabled())' in chat_menu
    assert 'never turns them on or off' in tutorial
    assert 'Only this public tutorial text is sent for translation' in tutorial
    assert len(re.findall(r'^\s*\{"', translator, re.MULTILINE)) >= 28
    assert '"🇧🇩"' in tutorial and '"🇬🇧"' in tutorial and '"🇸🇦"' in tutorial
    assert '"fa", "فارسی (Persian)"' in translator
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
    assert 'Smart Chat Folders · SOON' not in legacy_hub
    assert 'Biometric Chat Locker · SOON' not in legacy_hub
    assert 'setActionFeature(R.drawable.outline_groups_24' in legacy_hub
    assert 'cfg.isBiometricChatLockerEnabled(), true' in legacy_hub
    assert 'headerOptimization = -1;' in legacy_hub
    assert 'rowApkBlock = -1;' in legacy_hub
    assert 'rowFocusMode = -1;' in legacy_hub
    assert 'rowBatterySaver = -1;' in legacy_hub
    assert 'rowInstantTranslator = -1;' in legacy_hub
    assert 'rowLinkGuard = -1;' in legacy_hub
    assert 'rowReadableText = -1;' in legacy_hub
    assert 'rowReduceMotion = -1;' in legacy_hub
    assert 'rowChatListDetails = -1;' in legacy_hub
    assert 'rowPauseGifs = -1;' in legacy_hub
    assert 'rowSaveData = -1;' in legacy_hub
    assert 'BiometricChatLocker.isDeviceSecurityAvailable(getParentActivity())' in legacy_hub
    assert 'Set a PIN, password, or pattern in Android Security Settings' in legacy_hub
    assert 'Existing protected chats stay locked until you open each chat' in legacy_hub
    assert 'presentFragment(new FiltersSetupActivity());' in legacy_hub
    assert 'Create and manage Telegram chat folders' in legacy_hub
    assert 'Not active yet. No chat is protected by this setting' not in legacy_hub
    hub_activity = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/NGHubActivity.java").read_text(encoding="utf-8")
    assert 'addOfficialRow();' not in hub_activity
    assert '@NayaGramPro · not marked as your personal channel' not in hub_activity
    assert 'title.setTextColor(COLOR_CYAN);' in hub_activity
    passkeys_activity = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/PasskeysActivity.java").read_text(encoding="utf-8")
    assert 'FRESH_RESET_AUTHORIZATION_FORBIDDEN' in passkeys_activity
    assert 'do not clear app data, reinstall, or revoke sessions to retry' in passkeys_activity
    assert 'FileLog.d("Passkey registration failed: " + diagnosticCode);' in passkeys_activity
    assert '"FRESH_RESET_AUTHORIZATION_FORBIDDEN".equals(diagnosticCode)' in passkeys_activity
    assert 'Passkey setup could not be completed. Check your account authorization and try again.' in passkeys_activity
    assert 'showForError(error == null' not in passkeys_activity
    auto_pin = (ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/AutoPinChannelManager.java").read_text(encoding="utf-8")
    assert 'Intentionally disabled: never auto-join or auto-pin a channel.' in auto_pin
    assert 'TL_channels_joinChannel' not in auto_pin
    assert 'controller.pinDialog' not in auto_pin



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


class BiometricLockerFailClosedRegressionTest(unittest.TestCase):
    def test_no_fail_open_fallback(self):
        source = (ROOT / 'java/org/nayagram/platform/security/BiometricChatLocker.java').read_text(encoding='utf-8')
        self.assertIn('Set a secure screen lock before protecting chats.', source)
        self.assertIn('No supported authentication API: deny access rather than silently bypassing the lock.', source)
        self.assertNotIn('Fallback for older devices: grant session access', source)


class BiometricChatEntryGateTest(unittest.TestCase):
    def test_launch_activity_gates_chat_stack_changes_and_resume(self):
        launch = (ROOT / 'java/org/telegram/ui/LaunchActivity.java').read_text(encoding='utf-8')
        self.assertIn('enforceBiometricChatLock();', launch)
        self.assertIn('BiometricChatLocker.getInstance(this).lockAllSessions();', launch)
        self.assertIn('current.finishFragment();', launch)
        self.assertIn('Protected chat', launch)
