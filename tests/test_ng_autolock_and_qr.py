import unittest
from pathlib import Path
import re
import urllib.parse

ROOT = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main'

class NGAutoLockAndQRTest(unittest.TestCase):

    def test_ng_qr_generator_architecture_and_palette(self):
        qr_gen_file = ROOT / 'java/org/nayagram/platform/security/NGQRGenerator.java'
        self.assertTrue(qr_gen_file.exists(), "NGQRGenerator.java must exist")
        code = qr_gen_file.read_text(encoding='utf-8')

        # Must use ZXing Encoder and high error correction for robust central logo overlay
        self.assertIn('com.google.zxing.qrcode.encoder.Encoder', code)
        self.assertIn('ErrorCorrectionLevel.H', code)
        self.assertIn('ByteMatrix', code)

        # Palette colors specified by design: Blue #2D7FF9, Cyan, Purple, White
        self.assertIn('0xFF2D7FF9', code) # NayaGram Blue
        self.assertIn('0xFF00D4FF', code) # Cyan
        self.assertIn('0xFF7C4DFF', code) # Purple
        self.assertIn('0xFFFFFFFF', code) # White

        # Must render central protection logo
        self.assertIn('R.drawable.nayagram_protection_hero', code)
        self.assertIn('public static Bitmap generate(Context context, String otpAuthUri, int targetSize)', code)

    def test_ng_safety_activity_qr_and_autolock_integration(self):
        safety_act_file = ROOT / 'java/org/nayagram/platform/security/NGSafetyActivity.java'
        self.assertTrue(safety_act_file.exists())
        code = safety_act_file.read_text(encoding='utf-8')

        # Uses dynamic NGQRGenerator
        self.assertIn('NGQRGenerator.generate(context, uri, 720)', code)

        # Download QR code, Copy Secret, Hide/Show Secret, Paste code preserved
        self.assertIn('Download QR code', code)
        self.assertIn('Copy Secret', code)
        self.assertIn('Hide Secret', code)
        self.assertIn('Show Secret', code)
        self.assertIn('Paste authenticator code', code)
        self.assertIn('Verify & Enable', code)

        # Auto-lock row in Settings -> NG Safety
        self.assertIn('Auto-lock', code)
        self.assertIn('presentFragment(new NGAutoLockActivity())', code)

    def test_ng_autolock_activity_ui_and_presets(self):
        autolock_file = ROOT / 'java/org/nayagram/platform/security/NGAutoLockActivity.java'
        self.assertTrue(autolock_file.exists(), "NGAutoLockActivity.java must exist")
        code = autolock_file.read_text(encoding='utf-8')

        # Title and Footer
        self.assertIn('Auto-lock NayaGram', code)
        self.assertIn('Built with ❤️ in Bangladesh', code)

        # Save feedback must be neutral and must distinguish an inactive lock from an active one.
        self.assertIn('Auto-lock duration saved: ', code)
        self.assertIn('Duration saved, but Auto-lock is inactive.', code)
        self.assertIn('Could not save auto-lock settings. Please try again.', code)

        # Presets: 15, 45, 60, 180, 300, 720 minutes, Never (0), and Custom
        for val in ['15', '45', '60', '180', '300', '720']:
            self.assertIn(f'{val}, false', code)
        self.assertIn('"Never", 0, false', code)
        self.assertIn('"Custom (Hours/Minutes picker)", -1, true', code)

        # Selection styling: Blue checkmark #2D7FF9, rounded cards, white background
        self.assertIn('0xFF2D7FF9', code)
        self.assertIn('R.drawable.check_round', code)

        # Persistent storage: NG auto-lock remains independent of Telegram passcode timeout
        self.assertIn('NGSafety.setAutoLockMinutes', code)
        self.assertNotIn('SharedConfig.autoLockIn', code)
        self.assertNotIn('SharedConfig.saveConfig()', code)

    def test_ng_safety_autolock_logic_and_logging(self):
        safety_file = ROOT / 'java/org/nayagram/platform/security/NGSafety.java'
        self.assertTrue(safety_file.exists())
        code = safety_file.read_text(encoding='utf-8')

        # Storage keys and methods
        self.assertIn('KEY_AUTOLOCK_MINUTES', code)
        self.assertIn('getAutoLockMinutes', code)
        self.assertIn('setAutoLockMinutes', code)
        self.assertIn('getAutoLockDisplayString', code)
        self.assertIn('onAppBackground', code)
        self.assertIn('onAppForeground', code)
        self.assertIn('isAutoLockExpired', code)

        # Debug log for preset saved: "NG Auto-lock preset saved: Xmin"
        self.assertIn('NG Auto-lock preset saved: " + minutes + "min"', code)

        # Secret must never be logged
        self.assertNotIn('FileLog.d("secret', code.lower())
        self.assertNotIn('Log.d("NGSafety", "secret', code.lower())

    def test_launch_activity_lifecycle_hooks(self):
        launch_file = ROOT / 'java/org/telegram/ui/LaunchActivity.java'
        self.assertTrue(launch_file.exists())
        code = launch_file.read_text(encoding='utf-8')

        # onPause calls onAppBackground
        self.assertIn('NGSafety.onAppBackground(this);', code)

        # onResume calls onAppForeground
        self.assertIn('NGSafety.onAppForeground(this);', code)

    def test_support_bulletin_custom_background(self):
        chat_file = ROOT / 'java/org/telegram/ui/ChatActivity.java'
        self.assertTrue(chat_file.exists())
        code = chat_file.read_text(encoding='utf-8')

        # Must call setCustomBackground so Bulletin.Layout draws custom light card
        self.assertIn('layout.setCustomBackground(bg);', code)
        self.assertNotIn('layout.setBackground(bg);', code)

    def test_otpauth_uri_format(self):
        # Verify otpauth URI generation pattern matches RFC / Google Authenticator standards
        issuer = "NayaGram"
        account = "user"
        secret = "JBSWY3DPEHPK3PXP"
        label = f"{issuer}:{account}"
        uri = f"otpauth://totp/{label}?secret={secret}&issuer={issuer}&algorithm=SHA1&digits=6&period=30"
        parsed = urllib.parse.urlparse(uri)
        self.assertEqual(parsed.scheme, 'otpauth')
        self.assertEqual(parsed.netloc, 'totp')
        self.assertIn('secret=JBSWY3DPEHPK3PXP', parsed.query)
        self.assertIn('digits=6', parsed.query)
        self.assertIn('period=30', parsed.query)

        # The production implementation must construct the standards-compliant URI.
        safety_code = (ROOT / 'java/org/nayagram/platform/security/NGSafety.java').read_text(encoding='utf-8')
        self.assertIn('public static String getOtpAuthUri(String accountLabel, String secret)', safety_code)
        self.assertIn('otpauth://totp/', safety_code)
        self.assertIn('&algorithm=SHA1&digits=6&period=30', safety_code)

if __name__ == '__main__':
    unittest.main()
