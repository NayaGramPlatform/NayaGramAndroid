package org.telegram.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Validates that the new NG intro logo exists, is properly formed across all
 * drawable densities, and meets the packaging requirements for APK and AAB releases.
 */
public class IntroLogoPackagingTest {

    private static final byte[] PNG_SIGNATURE = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final String[] DENSITY_DIRS = new String[] {
        "drawable",
        "drawable-hdpi",
        "drawable-xhdpi",
        "drawable-xxhdpi",
        "drawable-xxxhdpi"
    };

    @Test
    public void testIntroLogoExistsInAllDensities() {
        File resDir = findResDir();
        assertNotNull("Android res directory must be found", resDir);

        for (String density : DENSITY_DIRS) {
            File logoFile = new File(new File(resDir, density), "nayagram_intro_logo.png");
            assertTrue("Logo must exist in " + density + " for valid APK/AAB packaging", logoFile.exists());
            assertTrue("Logo in " + density + " must not be empty", logoFile.length() > 100);
        }
    }

    @Test
    public void testIntroLogoPngHeaderAndDimensions() throws Exception {
        File resDir = findResDir();
        assertNotNull("Android res directory must be found", resDir);

        for (String density : DENSITY_DIRS) {
            File logoFile = new File(new File(resDir, density), "nayagram_intro_logo.png");
            try (InputStream is = new FileInputStream(logoFile)) {
                byte[] header = new byte[8];
                int read = is.read(header);
                assertEquals("PNG header length for " + density, 8, read);
                for (int i = 0; i < 8; i++) {
                    assertEquals("PNG signature byte " + i + " for " + density, PNG_SIGNATURE[i], header[i]);
                }

                // Read IHDR chunk length and type
                byte[] ihdrHeader = new byte[8];
                is.read(ihdrHeader);
                // Read 4 bytes width, 4 bytes height, 1 byte bit depth, 1 byte color type
                byte[] ihdrData = new byte[13];
                is.read(ihdrData);
                ByteBuffer buf = ByteBuffer.wrap(ihdrData).order(ByteOrder.BIG_ENDIAN);
                int width = buf.getInt();
                int height = buf.getInt();
                int bitDepth = buf.get() & 0xFF;
                int colorType = buf.get() & 0xFF;

                assertTrue("Width must be positive in " + density, width > 0);
                assertTrue("Height must be positive in " + density, height > 0);
                assertEquals("Width and height should match (square icon) in " + density, width, height);
                assertEquals("Bit depth should be 8-bit in " + density, 8, bitDepth);
                assertTrue("Color type must include alpha (RGBA=6) in " + density, colorType == 6 || colorType == 2);
            }
        }
    }

    private static File findResDir() {
        File[] candidates = new File[] {
            new File("TMessagesProj/src/main/res"),
            new File("src/main/res"),
            new File("../TMessagesProj/src/main/res")
        };
        for (File c : candidates) {
            if (c.exists() && c.isDirectory()) {
                return c;
            }
        }
        return null;
    }
}
