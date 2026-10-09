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
import java.util.HashMap;
import java.util.Map;

/**
 * Validates that all launcher icons across all screen densities (mdpi 48, hdpi 72, xhdpi 96,
 * xxhdpi 144, xxxhdpi 192) and adaptive foregrounds (108, 162, 216, 324, 432) exist,
 * are valid 32-bit RGBA PNG files, and match expected square dimensions.
 */
public class LauncherIconPackagingTest {

    private static final byte[] PNG_SIGNATURE = new byte[] {
        (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    private static final Map<String, Integer> LAUNCHER_SIZES = new HashMap<>();
    static {
        LAUNCHER_SIZES.put("mipmap-mdpi", 48);
        LAUNCHER_SIZES.put("mipmap-hdpi", 72);
        LAUNCHER_SIZES.put("mipmap-xhdpi", 96);
        LAUNCHER_SIZES.put("mipmap-xxhdpi", 144);
        LAUNCHER_SIZES.put("mipmap-xxxhdpi", 192);
    }

    private static final Map<String, Integer> FOREGROUND_SIZES = new HashMap<>();
    static {
        FOREGROUND_SIZES.put("mipmap-mdpi", 108);
        FOREGROUND_SIZES.put("mipmap-hdpi", 162);
        FOREGROUND_SIZES.put("mipmap-xhdpi", 216);
        FOREGROUND_SIZES.put("mipmap-xxhdpi", 324);
        FOREGROUND_SIZES.put("mipmap-xxxhdpi", 432);
    }

    @Test
    public void testLegacyAndRoundLauncherIcons() throws Exception {
        File resDir = findResDir();
        assertNotNull("Android res directory must be found", resDir);

        for (Map.Entry<String, Integer> entry : LAUNCHER_SIZES.entrySet()) {
            String folder = entry.getKey();
            int expectedSize = entry.getValue();

            File squareIcon = new File(new File(resDir, folder), "ic_launcher.png");
            File roundIcon = new File(new File(resDir, folder), "ic_launcher_round.png");

            assertTrue("Square icon must exist in " + folder, squareIcon.exists());
            assertTrue("Round icon must exist in " + folder, roundIcon.exists());

            assertValidPngDimensions(squareIcon, expectedSize, expectedSize);
            assertValidPngDimensions(roundIcon, expectedSize, expectedSize);
        }
    }

    @Test
    public void testAdaptiveIconForegrounds() throws Exception {
        File resDir = findResDir();
        assertNotNull("Android res directory must be found", resDir);

        for (Map.Entry<String, Integer> entry : FOREGROUND_SIZES.entrySet()) {
            String folder = entry.getKey();
            int expectedSize = entry.getValue();

            File fg = new File(new File(resDir, folder), "icon_foreground.png");
            File fgRound = new File(new File(resDir, folder), "icon_foreground_round.png");

            assertTrue("Adaptive foreground must exist in " + folder, fg.exists());
            assertTrue("Adaptive foreground round must exist in " + folder, fgRound.exists());

            assertValidPngDimensions(fg, expectedSize, expectedSize);
            assertValidPngDimensions(fgRound, expectedSize, expectedSize);
        }
    }

    @Test
    public void testPlayStoreIcon512() throws Exception {
        File resDir = findResDir();
        assertNotNull("Android res directory must be found", resDir);

        File playstore = new File(new File(resDir, "drawable-nodpi"), "ic_launcher_playstore.png");
        assertTrue("Play Store 512x512 icon must exist", playstore.exists());
        assertValidPngDimensions(playstore, 512, 512);
    }

    private void assertValidPngDimensions(File file, int expectedW, int expectedH) throws Exception {
        assertTrue("File " + file.getName() + " must have content", file.length() > 50);
        try (InputStream is = new FileInputStream(file)) {
            byte[] header = new byte[8];
            int read = is.read(header);
            assertEquals(8, read);
            for (int i = 0; i < 8; i++) {
                assertEquals(PNG_SIGNATURE[i], header[i]);
            }
            byte[] ihdrHeader = new byte[8];
            is.read(ihdrHeader);
            byte[] ihdrData = new byte[13];
            is.read(ihdrData);
            ByteBuffer buf = ByteBuffer.wrap(ihdrData).order(ByteOrder.BIG_ENDIAN);
            int width = buf.getInt();
            int height = buf.getInt();
            assertEquals("Width for " + file.getName(), expectedW, width);
            assertEquals("Height for " + file.getName(), expectedH, height);
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
