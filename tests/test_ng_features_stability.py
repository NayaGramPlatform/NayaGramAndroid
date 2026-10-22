import unittest
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main'

class NayaGramFeaturesStabilityTest(unittest.TestCase):

    def test_guard_settings_icon_and_navigation(self):
        privacy_code = (ROOT / 'java/org/telegram/ui/PrivacySettingsActivity.java').read_text()
        # View type 3 (checkbox) must not be returned for guardRow
        self.assertNotIn('if (position == guardRow) {\n                return 3;\n            }', privacy_code)
        # Binds with dedicated authenticator icon in TextCell (case 5)
        self.assertIn('textCell2.setTextAndValueAndIcon(getString(R.string.NayaGramGuardTitle), guardStatus, true, R.drawable.ic_nayagram_authenticator, true);', privacy_code)
        self.assertIn('textCell2.imageView.setColorFilter(null);', privacy_code)
        # OnClick opens NGSafetyActivity
        self.assertIn('presentFragment(new NGSafetyActivity());', privacy_code)

        # Icon XML exists and contains cyan and purple palette
        icon_path = ROOT / 'res/drawable/ic_nayagram_authenticator.xml'
        self.assertTrue(icon_path.exists())
        icon_xml = icon_path.read_text()
        self.assertIn('#00D4FF', icon_xml)
        self.assertIn('#7C4DFF', icon_xml)

    def test_sender_select_popup_premium_check(self):
        popup_code = (ROOT / 'java/org/telegram/ui/Components/SenderSelectPopup.java').read_text()
        # Must not have bypassed check
        self.assertNotIn('false && peerObj.premium_required', popup_code)
        # Must use currentAccount
        self.assertIn('if (peerObj.premium_required && !UserConfig.getInstance(currentAccount).isPremium()) {', popup_code)

    def test_send_as_peers_preserves_telegram_permission_gates(self):
        chat_object_code = (ROOT / 'java/org/telegram/messenger/ChatObject.java').read_text()
        method_start = chat_object_code.index('public static boolean canSendAsPeers(TLRPC.Chat chat)')
        method_end = chat_object_code.index('public static boolean isHiddenInCommunity', method_start)
        method = chat_object_code[method_start:method_end]
        # Channel send-as eligibility must retain Telegram's original restrictions.
        self.assertIn('if (chat == null)', method)
        self.assertIn('!chat.megagroup && chat.signatures && ChatObject.hasAdminRights(chat) && ChatObject.canWriteToChat(chat)', method)
        self.assertIn('chat.megagroup && (ChatObject.isPublic(chat) || chat.has_geo || chat.has_link)', method)
        self.assertNotIn('if (ChatObject.isDiscussionGroup(chat))', method)
        self.assertNotIn('|| chat.megagroup);', method)

    def test_send_when_online_discovery_and_handling(self):
        enter_view_code = (ROOT / 'java/org/telegram/ui/Components/ChatActivityEnterView.java').read_text()
        # Discoverable in private user chats
        self.assertIn('user != null && !user.bot && !self && dialog_id > 0', enter_view_code)
        # performSendWhenOnline handles online, privacy-restricted, and eligible 0x7FFFFFFE states
        self.assertIn('private void performSendWhenOnline()', enter_view_code)
        self.assertIn('recipient.status instanceof TLRPC.TL_userStatusOnline', enter_view_code)
        self.assertIn('recipient.status instanceof TLRPC.TL_userStatusRecently', enter_view_code)
        self.assertIn('sendMessageInternal(true, 0x7FFFFFFE, 0, 0, true);', enter_view_code)

    def test_support_bulletin_styling(self):
        chat_activity_code = (ROOT / 'java/org/telegram/ui/ChatActivity.java').read_text()
        # Custom vibrant green and white styling with animated star
        self.assertIn('0xFF10B981', chat_activity_code)
        self.assertIn('0xFFFFFFFF', chat_activity_code)
        self.assertIn('bg.setColor(0xFF10B981);', chat_activity_code)
        # Prolonged display duration (5 seconds)
        self.assertIn('BulletinFactory.of(ChatActivity.this).create(layout, Bulletin.DURATION_PROLONG).show();', chat_activity_code)

    def test_guard_unlock_hero_layout(self):
        safety_code = (ROOT / 'java/org/nayagram/platform/security/NGSafetyActivity.java').read_text()
        # Hero slogan and protection shield logo
        self.assertIn('R.string.NGProtectionTagline', safety_code)
        self.assertIn('R.drawable.nayagram_protection_hero', safety_code)
        # 30-second TOTP countdown
        self.assertIn('startCodeCountdown', safety_code)
        # 15 backup codes scroll
        self.assertIn('Save your 15 backup codes', safety_code)
        # English creation date footer
        self.assertIn('R.string.NGAuthenticatorCreatedDate', safety_code)

        # Verify hero drawable existence across densities
        for density in ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']:
            drawable_path = ROOT / f'res/drawable-{density}/nayagram_protection_hero.png'
            self.assertTrue(drawable_path.exists(), f"Missing hero logo in drawable-{density}")

    def test_localized_strings_present(self):
        strings_en = (ROOT / 'res/values/strings.xml').read_text()
        strings_bn = (ROOT / 'res/values-bn/strings.xml').read_text()

        self.assertIn('name="NayaGramGuardTitle"', strings_en)
        self.assertIn('name="NGProtectionTagline"', strings_en)
        self.assertIn('name="NGAuthenticatorCreatedDate"', strings_en)
        self.assertIn('name="SendWhenOnlineUserOnline"', strings_en)
        self.assertIn('name="SendWhenOnlinePrivacyRestricted"', strings_en)

        self.assertIn('name="NayaGramGuardTitle"', strings_bn)
        self.assertIn('name="NGProtectionTagline"', strings_bn)
        self.assertIn('name="NGAuthenticatorCreatedDate"', strings_bn)
        self.assertIn('name="SendWhenOnlineUserOnline"', strings_bn)
        self.assertIn('name="SendWhenOnlinePrivacyRestricted"', strings_bn)

        # Confirm no Bengali words for the NG Authenticator security feature
        self.assertIn('<string name="NayaGramGuardTitle">NG Authenticator</string>', strings_bn)
        self.assertIn('<string name="NGProtectionTagline">Your Security, Our Priority</string>', strings_bn)
        self.assertIn('<string name="NGAuthenticatorCreatedDate">NG Authenticator • Created October 18, 2026</string>', strings_bn)

    def test_sender_select_popup_premium_lock_badge_and_enforcement(self):
        popup_code = (ROOT / 'java/org/telegram/ui/Components/SenderSelectPopup.java').read_text()
        # Must display premium lock badge on sender title when premium is required
        self.assertIn('if (peerObj.premium_required) {', popup_code)
        self.assertIn('ColoredImageSpan span = new ColoredImageSpan(R.drawable.msg_mini_premiumlock);', popup_code)
        self.assertIn('str.setSpan(span, str.length() - 1, str.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);', popup_code)
        # Must enforce premium gate
        self.assertIn('if (peerObj.premium_required && !UserConfig.getInstance(currentAccount).isPremium()) {', popup_code)
        self.assertIn('new SelectSendAsPremiumHintBulletinLayout(', popup_code)

    def test_bulletin_and_undo_toast_styling_not_black(self):
        bulletin_code = (ROOT / 'java/org/telegram/ui/Components/Bulletin.java').read_text()
        # Bulletin background must use NayaGram Cyan stroke and theme-adaptive background
        self.assertIn('gd.setStroke(dp(1.5f), 0xFF00D4FF);', bulletin_code)
        self.assertIn('gd.setColor(0xFFFFFFFF);', bulletin_code)
        self.assertIn('gd.setColor(0xFA0F172A);', bulletin_code)
        self.assertIn('return Theme.isCurrentThemeDark() ? 0xFF0F172A : 0xFFFFFFFF;', bulletin_code)
        self.assertIn('return Theme.isCurrentThemeDark() ? 0xFFFFFFFF : 0xFF0F172A;', bulletin_code)

        # UndoView must also use NayaGram Cyan stroke and theme-adaptive colors
        undo_code = (ROOT / 'java/org/telegram/ui/Components/UndoView.java').read_text()
        self.assertIn('gd.setStroke(AndroidUtilities.dp(1.5f), 0xFF00D4FF);', undo_code)
        self.assertIn('return Theme.isCurrentThemeDark() ? 0xFF0F172A : 0xFFFFFFFF;', undo_code)

        # Default theme colors must be light card and dark text (not dark charcoal)
        theme_colors_code = (ROOT / 'java/org/telegram/ui/ActionBar/ThemeColors.java').read_text()
        self.assertIn('defaultColors[key_undo_background] = 0xffffffff;', theme_colors_code)
        self.assertIn('defaultColors[key_undo_infoColor] = 0xff0f172a;', theme_colors_code)

if __name__ == '__main__':
    unittest.main()
