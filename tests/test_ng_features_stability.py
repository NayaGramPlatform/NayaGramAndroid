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
        # OnClick opens existing NayaGramGuardActivity
        self.assertIn('presentFragment(new NayaGramGuardActivity());', privacy_code)

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
        # Custom light card styling with cyan border and dark readable text
        self.assertIn('0xFF00D4FF', chat_activity_code)
        self.assertIn('0xFF0F172A', chat_activity_code)
        self.assertIn('bg.setColor(0xFFFFFFFF);', chat_activity_code)

    def test_localized_strings_present(self):
        strings_en = (ROOT / 'res/values/strings.xml').read_text()
        strings_bn = (ROOT / 'res/values-bn/strings.xml').read_text()

        self.assertIn('name="NayaGramGuardTitle"', strings_en)
        self.assertIn('name="SendWhenOnlineUserOnline"', strings_en)
        self.assertIn('name="SendWhenOnlinePrivacyRestricted"', strings_en)

        self.assertIn('name="NayaGramGuardTitle"', strings_bn)
        self.assertIn('name="SendWhenOnlineUserOnline"', strings_bn)
        self.assertIn('name="SendWhenOnlinePrivacyRestricted"', strings_bn)

if __name__ == '__main__':
    unittest.main()
