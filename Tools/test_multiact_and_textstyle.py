#!/usr/bin/env python3
import re
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent

def test_multi_account_enhancements():
    user_config = (ROOT / "TMessagesProj/src/main/java/org/telegram/messenger/UserConfig.java").read_text(encoding="utf-8")
    assert "public final static int MAX_ACCOUNT_COUNT = 15;" in user_config, "MAX_ACCOUNT_COUNT should be 15"
    assert "public final static int MAX_ACCOUNT_DEFAULT_COUNT = MAX_ACCOUNT_COUNT;" in user_config, "MAX_ACCOUNT_DEFAULT_COUNT should equal MAX_ACCOUNT_COUNT"

    settings = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/SettingsActivity.java").read_text(encoding="utf-8")
    # Verify freeAccount search and LoginActivity(freeAccount)
    assert "int freeAccount = -1;" in settings
    assert "presentFragment(new LoginActivity(targetAccount));" in settings
    assert "NGBotLoginHelper.showBotLoginDialog(SettingsActivity.this, targetAccount);" in settings
    # Verify AccountCell has switchView for per-account notification toggles
    assert "public Switch switchView;" in settings
    assert "switchView.setChecked(NotificationsController.isAccountNotificationsEnabled(account)" in settings
    assert "canAddAccount = UserConfig.getActivatedAccountsCount() < UserConfig.MAX_ACCOUNT_COUNT;" in settings
    hub = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/NGHubActivity.java").read_text(encoding="utf-8")
    assert "buildAccountSwitcherCard" in hub
    assert "showAccountSwitchDialog" in hub
    assert "Switch 👥" in hub
    print("PASS: Multi-account (15 accounts + safe add account + NG Hub switcher)")

def test_text_style_feature():
    helper = (ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/textstyle/NGTextStyleHelper.java").read_text(encoding="utf-8")
    for style in ["STYLE_REGULAR", "STYLE_SPOILER", "STYLE_BOLD", "STYLE_ITALIC", "STYLE_MONO", "STYLE_STRIKE", "STYLE_UNDERLINE", "STYLE_QUOTE"]:
        assert style in helper, f"Missing {style} in NGTextStyleHelper"
    assert "makeSelectedBold()" in helper
    assert "makeSelectedItalic()" in helper
    assert "makeSelectedSpoiler()" in helper
    assert "makeSelectedQuote()" in helper
    assert "makeSelectedMono()" in helper
    assert "makeSelectedStrike()" in helper
    assert "makeSelectedUnderline()" in helper
    assert "Text style" in helper
    assert "Select any style" in helper

    chat_menu = (ROOT / "TMessagesProj/src/main/java/org/nayagram/platform/chat/NGChatMenuHelper.java").read_text(encoding="utf-8")
    assert "ITEM_TEXT_STYLE = 29005;" in chat_menu
    assert "headerItem.lazilyAddSubItem(ITEM_TEXT_STYLE, R.drawable.msg_text_outlined, \"Text style\")" in chat_menu
    assert "NGTextStyleHelper.showDialog(activity);" in chat_menu

    enter_view = (ROOT / "TMessagesProj/src/main/java/org/telegram/ui/Components/ChatActivityEnterView.java").read_text(encoding="utf-8")
    assert "NGTextStyleHelper.applyStyle(messageEditText);" in enter_view
    print("PASS: Text style persistent formatting & chat menu integration")

if __name__ == "__main__":
    test_multi_account_enhancements()
    test_text_style_feature()
    print("ALL MULTI-ACCOUNT & TEXT STYLE TESTS PASSED!")
