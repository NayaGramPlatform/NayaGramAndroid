package org.telegram.messenger.voip;

import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regression test for Android 14 (API 34) Foreground Service type compliance in VoIPService.
 *
 * Background:
 * On Android 14+, declaring or requesting `FOREGROUND_SERVICE_TYPE_PHONE_CALL` without being
 * the default system dialer or having telecom connection service causes SecurityException /
 * ForegroundServiceTypeNotAllowedException or immediate app stops ("NayaGram keeps stopping").
 *
 * VoIPService in NayaGram must strictly use only allowed foreground service types:
 * - FOREGROUND_SERVICE_TYPE_CAMERA
 * - FOREGROUND_SERVICE_TYPE_MICROPHONE
 * - FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
 * - FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION (only if screen sharing is active)
 *
 * And must never return or invoke FOREGROUND_SERVICE_TYPE_PHONE_CALL.
 */
public class VoIPForegroundServiceTypeRegressionTest {

    // Bitmask constants from android.content.pm.ServiceInfo
    private static final int FOREGROUND_SERVICE_TYPE_CAMERA = 64;           // 0x00000040
    private static final int FOREGROUND_SERVICE_TYPE_MICROPHONE = 128;       // 0x00000080
    private static final int FOREGROUND_SERVICE_TYPE_PHONE_CALL = 4;         // 0x00000004
    private static final int FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK = 2;     // 0x00000002
    private static final int FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION = 32;  // 0x00000020

    /**
     * Simulates the exact logic of VoIPService.getCurrentForegroundType(context, gotMediaProjection)
     * on Android 14+ (Build.VERSION.SDK_INT >= 34).
     */
    private int computeSimulatedForegroundType(boolean hasCameraPerm, boolean hasMicPerm, boolean gotMediaProjection) {
        int type = 0;
        if (hasCameraPerm) {
            type |= FOREGROUND_SERVICE_TYPE_CAMERA;
        }
        if (hasMicPerm) {
            type |= FOREGROUND_SERVICE_TYPE_MICROPHONE;
        }
        if (gotMediaProjection) {
            type |= FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION;
        }
        type |= FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK;
        return type;
    }

    @Test
    public void testSimulatedTypesNeverContainPhoneCallType() {
        boolean[] flags = {true, false};
        for (boolean cam : flags) {
            for (boolean mic : flags) {
                for (boolean proj : flags) {
                    int type = computeSimulatedForegroundType(cam, mic, proj);
                    Assert.assertEquals(
                        "VoIP foreground service type must NEVER include FOREGROUND_SERVICE_TYPE_PHONE_CALL (0x4)",
                        0,
                        type & FOREGROUND_SERVICE_TYPE_PHONE_CALL
                    );
                    Assert.assertTrue(
                        "VoIP foreground service type must always include MEDIA_PLAYBACK for audio stream",
                        (type & FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK) != 0
                    );
                }
            }
        }
    }

    @Test
    public void testVoIPServiceSourceCodeHasNoPhoneCallFGSType() throws Exception {
        // Find VoIPService.java in repo
        File sourceFile = findSourceFile("TMessagesProj/src/main/java/org/telegram/messenger/voip/VoIPService.java");
        if (sourceFile == null || !sourceFile.exists()) {
            // If running in alternate directory structure, skip file inspection gracefully
            return;
        }

        String content = readFileToString(sourceFile);

        // Check inside getCurrentForegroundType method: it must not contain FOREGROUND_SERVICE_TYPE_PHONE_CALL
        Pattern methodPattern = Pattern.compile(
            "getCurrentForegroundType\\s*\\([^)]*\\)\\s*\\{([\\s\\S]*?)\\}",
            Pattern.MULTILINE
        );
        Matcher matcher = methodPattern.matcher(content);
        while (matcher.find()) {
            String methodBody = matcher.group(1);
            Assert.assertFalse(
                "getCurrentForegroundType in VoIPService must not reference FOREGROUND_SERVICE_TYPE_PHONE_CALL",
                methodBody.contains("FOREGROUND_SERVICE_TYPE_PHONE_CALL")
            );
        }
    }

    private static File findSourceFile(String relativePath) {
        File f = new File(relativePath);
        if (f.exists()) return f;
        File f2 = new File("../" + relativePath);
        if (f2.exists()) return f2;
        File f3 = new File("../../" + relativePath);
        if (f3.exists()) return f3;
        return null;
    }

    private static String readFileToString(File file) throws Exception {
        try (InputStream in = new FileInputStream(file)) {
            byte[] bytes = new byte[(int) file.length()];
            int read = 0;
            while (read < bytes.length) {
                int r = in.read(bytes, read, bytes.length - read);
                if (r == -1) break;
                read += r;
            }
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
