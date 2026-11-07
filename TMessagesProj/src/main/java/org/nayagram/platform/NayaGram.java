package org.nayagram.platform;

import org.telegram.messenger.FileLog;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * NayaGram security shield: blocks incoming APK, ZIP, scripts, executables,
 * archives, and malicious files sent by other people.
 * Off by default; the user turns it on in NayaGram Settings.
 */
public class NayaGram {

    private static final Set<String> BLOCKED_EXTENSIONS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "apk", "xapk", "apks", "aab", "zip", "rar", "7z", "exe", "bat", "cmd", "com",
            "vbs", "vbe", "js", "jse", "ps1", "psm1", "sh", "bash", "svg", "tiff", "tif",
            "msi", "msp", "dex", "jar", "iso", "dmg", "img", "dll", "scr", "pif", "gadget",
            "msc", "hta", "cpl", "inf", "reg", "lnk", "apk.1", "bin", "elf", "so", "deb",
            "rpm", "pkg", "app", "ipa", "hap", "tar", "gz", "bz2", "xz", "cab", "html",
            "htm", "xhtml", "url"
    )));

    public static boolean isBlockApkZipEnabled() {
        try {
            return NayaConfig.getInstance().isApkBlockEnabled();
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * True when the file name matches a blocked format (single or double extension) and the shield is on.
     * Callers must only pass incoming files (sentByMe == false).
     */
    public static boolean shouldBlockFile(String fileName) {
        if (!isBlockApkZipEnabled() || fileName == null) {
            return false;
        }
        String lower = fileName.trim().toLowerCase(Locale.ROOT);
        if (lower.isEmpty()) {
            return false;
        }

        // Direct suffix check: endsWith("." + ext) or exact match
        for (String ext : BLOCKED_EXTENSIONS) {
            if (lower.endsWith("." + ext) || lower.equals(ext)) {
                FileLog.d("NayaGram Security Shield: blocked incoming file " + fileName);
                return true;
            }
        }

        // Double-extension cloaking check: split on '.' and check if ANY segment is in the set
        String nameToSplit = lower.startsWith(".") ? lower.substring(1) : lower;
        String[] parts = nameToSplit.split("\\.");
        for (String part : parts) {
            if (BLOCKED_EXTENSIONS.contains(part)) {
                FileLog.d("NayaGram Security Shield: blocked incoming cloaked file " + fileName);
                return true;
            }
        }
        return false;
    }

    /** After a download: true when the shield is on and an incoming file is an Android package under another name. */
    public static boolean shouldBlockDownloadedPayload(boolean sentByMe, File file) {
        return NayaArchiveInspector.shouldBlockIncomingPayload(isBlockApkZipEnabled(), sentByMe, file);
    }

    /** Silent no-op: logs only. Zero screen popups or UI work. */
    public static void showBlockedNotice() {
        FileLog.d("NayaGram Security Shield: file blocked silently.");
    }
}
