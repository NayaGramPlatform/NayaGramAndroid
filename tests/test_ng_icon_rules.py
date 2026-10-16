import unittest
from pathlib import Path
import re
import xml.etree.ElementTree as ET
ROOT = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main'
class IconRules(unittest.TestCase):
 def test_crystal_is_only_premium_icon(self):
  text=(ROOT/'java/org/telegram/ui/LauncherIconController.java').read_text()
  self.assertRegex(text, r'NOX\("NGCrystal"[^\n]+true\)')
  self.assertNotRegex(text,r'(?:DEFAULT|VINTAGE|AQUA|PREMIUM|TURBO)\([^\n]+true\)')
  self.assertIn('return !icon.premium || hasIconPremium();',text)
  self.assertIn('getCurrentUser().premium',text)
 def test_free_colors_and_crystal_resources(self):
  doc=ET.parse(ROOT/'AndroidManifest.xml'); a='{http://schemas.android.com/apk/res/android}'
  aliases={x.get(a+'name'):x.get(a+'icon') for x in doc.findall('.//activity-alias')}
  self.assertEqual(aliases['org.nayagram.platform.NGBlue'],'@mipmap/icon_2_launcher')
  self.assertEqual(aliases['org.nayagram.platform.NGCrystal'],'@mipmap/icon_6_launcher')
  self.assertEqual(aliases['org.nayagram.platform.NGPurple'],'@mipmap/icon_3_launcher')
  self.assertEqual(aliases['org.nayagram.platform.PremiumIcon'],'@mipmap/icon_4_launcher')
 def test_neon_remains_default(self):
  text=(ROOT/'java/org/telegram/ui/LauncherIconController.java').read_text()
  self.assertRegex(text,r'DEFAULT\("NGSky", R.drawable.icon_background_sa, R.mipmap.icon_foreground_sa')
  doc=ET.parse(ROOT/'AndroidManifest.xml');a='{http://schemas.android.com/apk/res/android}'
  enabled=[x for x in doc.findall('.//activity-alias') if x.get(a+'enabled')=='true']
  self.assertEqual(enabled[0].get(a+'icon'),'@mipmap/ic_launcher')
if __name__=='__main__':unittest.main()
