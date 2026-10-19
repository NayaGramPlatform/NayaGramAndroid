import unittest
from pathlib import Path
from PIL import Image
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1] / 'TMessagesProj/src/main'
DENSITIES = ['mdpi', 'hdpi', 'xhdpi', 'xxhdpi', 'xxxhdpi']

class TestLauncherIconsDensityAndCrop(unittest.TestCase):

    def test_density_assets_exist_and_no_black_borders(self):
        targets = {
            'NG Cyan': ('icon_foreground', 'ic_launcher'),
            'NG Blue': ('icon_2_foreground', 'icon_2_launcher'),
            'NG Neon': ('icon_5_foreground', 'icon_5_launcher'),
        }
        for name, (fg_name, legacy_name) in targets.items():
            for density in DENSITIES:
                fg_path = ROOT / f"res/mipmap-{density}/{fg_name}.png"
                self.assertTrue(fg_path.exists(), f"Missing foreground asset {fg_path} for {name}")
                with Image.open(fg_path) as im:
                    self.assertEqual(im.mode, 'RGBA', f"{fg_path} must be RGBA")
                    corners = [
                        im.getpixel((0, 0)),
                        im.getpixel((im.width - 1, 0)),
                        im.getpixel((0, im.height - 1)),
                        im.getpixel((im.width - 1, im.height - 1))
                    ]
                    for c in corners:
                        self.assertEqual(c[3], 0, f"Corner in {fg_path} must be transparent to avoid black border, got {c}")

                    # Check adaptive safe zone: content bounding box must be within inner 72% of canvas
                    bbox = im.getbbox()
                    self.assertIsNotNone(bbox, f"{fg_path} has no non-empty content")
                    w, h = im.size
                    content_w = bbox[2] - bbox[0]
                    content_h = bbox[3] - bbox[1]
                    self.assertLessEqual(content_w / w, 0.72, f"{fg_path} content too wide ({content_w}/{w}), risks cropping")
                    self.assertLessEqual(content_h / h, 0.72, f"{fg_path} content too tall ({content_h}/{h}), risks cropping")

                legacy_path = ROOT / f"res/mipmap-{density}/{legacy_name}.png"
                self.assertTrue(legacy_path.exists(), f"Missing legacy launcher asset {legacy_path} for {name}")
                with Image.open(legacy_path) as im:
                    self.assertEqual(im.mode, 'RGBA', f"{legacy_path} must be RGBA")
                    corners = [
                        im.getpixel((0, 0)),
                        im.getpixel((im.width - 1, 0)),
                        im.getpixel((0, im.height - 1)),
                        im.getpixel((im.width - 1, im.height - 1))
                    ]
                    for c in corners:
                        self.assertEqual(c[3], 0, f"Corner in {legacy_path} must be transparent to avoid black box on launchers, got {c}")

    def test_adaptive_icon_xml_declarations(self):
        adaptive_dir = ROOT / 'res/mipmap-anydpi-v26'
        self.assertTrue((adaptive_dir / 'ic_launcher.xml').exists())
        self.assertTrue((adaptive_dir / 'icon_2_launcher.xml').exists())
        self.assertTrue((adaptive_dir / 'icon_5_launcher.xml').exists())

        for xml_file in ['ic_launcher.xml', 'icon_2_launcher.xml', 'icon_5_launcher.xml']:
            tree = ET.parse(adaptive_dir / xml_file)
            root = tree.getroot()
            self.assertEqual(root.tag, 'adaptive-icon')
            bg = root.find('background')
            fg = root.find('foreground')
            self.assertIsNotNone(bg, f"Missing background in {xml_file}")
            self.assertIsNotNone(fg, f"Missing foreground in {xml_file}")

if __name__ == '__main__':
    unittest.main()
