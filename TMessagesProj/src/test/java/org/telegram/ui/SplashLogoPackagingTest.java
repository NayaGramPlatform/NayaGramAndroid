package org.telegram.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * LaunchActivity's Android 12+ splash must show the NayaGram mark, not the
 * legacy Telegram paper-plane animation.
 */
public class SplashLogoPackagingTest {

    @Test
    public void startThemesUseNayaGramSplashLogo() throws Exception {
        assertSplashTheme(read("TMessagesProj/src/main/res/values-v31/styles.xml"));
        assertSplashTheme(read("TMessagesProj/src/main/res/values-night/styles.xml"));
    }

    @Test
    public void splashLogoDrawableUsesNgLogoAndLegacyPlaneIsGone() throws Exception {
        String splash = read("TMessagesProj/src/main/res/drawable/splash_logo.xml");
        assertTrue(splash.contains("@drawable/ng_logo"));
        assertFalse(splash.toLowerCase().contains("telegram"));
        assertFalse(splash.contains("2aabee"));

        String legacy = read("TMessagesProj/src/main/res/drawable/tg_splash_320.xml");
        assertTrue(legacy.contains("@drawable/splash_logo"));
        assertFalse(legacy.contains("2aabee"));
        assertFalse(legacy.contains("plane"));

        File logo = find("TMessagesProj/src/main/res/drawable/ng_logo.png");
        assertTrue(logo.isFile());
        assertTrue(logo.length() > 100);
        byte[] header = Files.readAllBytes(logo.toPath());
        assertEquals((byte) 0x89, header[0]);
        assertEquals((byte) 0x50, header[1]);
        assertEquals((byte) 0x4E, header[2]);
        assertEquals((byte) 0x47, header[3]);
    }

    private static void assertSplashTheme(String xml) {
        assertTrue(xml.contains("android:windowSplashScreenAnimatedIcon\">@drawable/splash_logo<"));
        assertFalse(xml.contains("tg_splash"));
        assertFalse(xml.contains("telegram_logo"));
    }

    private static String read(String relative) throws Exception {
        return new String(Files.readAllBytes(find(relative).toPath()), StandardCharsets.UTF_8);
    }

    private static File find(String relative) {
        File[] candidates = new File[] {
            new File(relative),
            new File("../" + relative),
            new File("../../" + relative)
        };
        for (File candidate : candidates) {
            if (candidate.isFile()) {
                return candidate;
            }
        }
        return new File(relative);
    }
}
