package org.nayagram.platform;

import android.os.SystemClock;
import android.widget.Toast;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.File;
import java.util.Locale;

/**
 * NayaGram security shield: blocks incoming APK and ZIP files sent by other people.
 * Off by default; the user turns it on in NayaGram Settings.
 */
public class NayaGram {

    private static final String[] BLOCKED_EXTENSIONS = {".apk", ".xapk", ".apks", ".zip"};
    private static final long NOTICE_COOLDOWN_MS = 8000L;
    private static volatile long lastNoticeTime;

    public static boolean isBlockApkZipEnabled() {
        try {
            return NayaConfig.getInstance().isApkBlockEnabled();
        } catch (Throwable e) {
            return false;
        }
    }

    /** True when the file name is an APK or ZIP and the shield is on. Callers must only pass incoming files. */
    public static boolean shouldBlockFile(String fileName) {
        if (!isBlockApkZipEnabled() || fileName == null) {
            return false;
        }
        String lower = fileName.trim().toLowerCase(Locale.ROOT);
        for (String ext : BLOCKED_EXTENSIONS) {
            if (lower.endsWith(ext)) {
                FileLog.d("NayaGram Security Shield: blocked incoming file " + fileName);
                return true;
            }
        }
        return false;
    }

    /** After a download: true when the shield is on and an incoming file is an Android package under another name. */
    public static boolean shouldBlockDownloadedPayload(boolean sentByMe, File file) {
        return NayaArchiveInspector.shouldBlockIncomingPayload(isBlockApkZipEnabled(), sentByMe, file);
    }

    /** Shows the Bengali notice, at most once per cooldown so a burst of files does not spam the screen. */
    public static void showBlockedNotice() {
        long now = SystemClock.elapsedRealtime();
        if (now - lastNoticeTime < NOTICE_COOLDOWN_MS) {
            return;
        }
        lastNoticeTime = now;
        AndroidUtilities.runOnUIThread(() -> {
            try {
                Toast.makeText(ApplicationLoader.applicationContext,
                        "নিরাপত্তার কারণে APK বা ZIP ফাইলটি ব্লক করা হয়েছে। Settings থেকে এই সুরক্ষা বন্ধ করা যাবে।",
                        Toast.LENGTH_LONG).show();
            } catch (Throwable ignored) {
            }
        });
    }
}
