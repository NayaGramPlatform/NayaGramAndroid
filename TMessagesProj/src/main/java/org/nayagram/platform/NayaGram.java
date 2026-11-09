package org.nayagram.platform;

import org.telegram.messenger.FileLog;
import org.telegram.messenger.FileLoader;
import org.telegram.tgnet.TLRPC;
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
        return shouldBlockFile(fileName, 0);
    }

    /**
     * Blocks incoming dangerous files.
     * Ancient archives older than 7 days from historical backup remain unblocked.
     */
    public static boolean shouldBlockFile(String fileName, int messageDate) {
        if (!isBlockApkZipEnabled() || fileName == null) {
            return false;
        }
        long activatedAt = NayaConfig.getInstance().getApkBlockActivatedAt();
        if (messageDate > 0 && activatedAt > 0 && (activatedAt - messageDate) > (7 * 86400L)) {
            return false;
        }
        String lower = fileName.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("[blocked file] ")) {
            lower = lower.substring("[blocked file] ".length()).trim();
        } else if (lower.startsWith("[blocked] ")) {
            lower = lower.substring("[blocked] ".length()).trim();
        }
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
        String[] parts = nameToSplit.split(java.util.regex.Pattern.quote("."));
        for (String part : parts) {
            if (BLOCKED_EXTENSIONS.contains(part)) {
                FileLog.d("NayaGram Security Shield: blocked incoming cloaked file " + fileName);
                return true;
            }
        }
        return false;
    }

    /**
     * Checks both filename and MIME type of document for robust blocking.
     */
    public static boolean shouldBlockDocument(TLRPC.Document document, int messageDate) {
        if (!isBlockApkZipEnabled() || document == null) {
            return false;
        }
        String fileName = FileLoader.getDocumentFileName(document);
        if (shouldBlockFile(fileName, messageDate)) {
            return true;
        }
        if (document.mime_type != null) {
            String mime = document.mime_type.toLowerCase(Locale.ROOT);
            if (mime.equals("application/vnd.android.package-archive") ||
                mime.equals("application/zip") ||
                mime.equals("application/x-zip-compressed") ||
                mime.equals("application/x-rar-compressed") ||
                mime.equals("application/x-7z-compressed") ||
                mime.equals("application/x-tar") ||
                mime.equals("application/gzip")) {
                FileLog.d("NayaGram Security Shield: blocked document by MIME " + mime);
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

    public static CharSequence formatUserStatusText(CharSequence statusText) {
        if (statusText == null) {
            return null;
        }
        String str = statusText.toString().trim();
        if (isStatusWord(str, "online")) {
            if (!NayaConfig.getInstance().isRadiantOnlineAuraEnabled()
                    && !NayaConfig.getInstance().isRadiantPresenceEnabled()) {
                return statusText;
            }
            return "Online" + str.substring("online".length());
        }
        if (isStatusWord(str, "offline")) {
            if (!NayaConfig.getInstance().isStealthOfflinePresenceEnabled()
                    && !NayaConfig.getInstance().isRadiantPresenceEnabled()) {
                return statusText;
            }
            return "Offline" + str.substring("offline".length());
        }
        return statusText;
    }

    private static boolean isStatusWord(String value, String word) {
        if (value.length() < word.length()) {
            return false;
        }
        if (!value.regionMatches(true, 0, word, 0, word.length())) {
            return false;
        }
        return value.length() == word.length() || !Character.isLetter(value.charAt(word.length()));
    }

    public static int getStatusTextColor(boolean isOnline, boolean isDarkTheme) {
        if (isOnline) {
            return isDarkTheme ? 0xFF34D399 : 0xFF059669;
        }
        return isDarkTheme ? 0xFF38BDF8 : 0xFF0284C7;
    }

    public static int getRadiantOnlineColor() {
        if (!NayaConfig.getInstance().isRadiantOnlineAuraEnabled()
                && !NayaConfig.getInstance().isRadiantPresenceEnabled()) {
            return 0xFF10B981;
        }
        int step = (int) ((System.currentTimeMillis() / 1500L) % 4);
        if (step == 0) return 0xFF10B981;
        if (step == 1) return 0xFF3B82F6;
        if (step == 2) return 0xFF00E5FF;
        return 0xFF8B5CF6;
    }

    public static int getRadiantOfflineColor() {
        return getRadiantOfflineColor(true);
    }

    public static int getRadiantOfflineColor(boolean isDarkTheme) {
        if (!NayaConfig.getInstance().isStealthOfflinePresenceEnabled()
                && !NayaConfig.getInstance().isRadiantPresenceEnabled()) {
            return isDarkTheme ? 0xFF94A3B8 : 0xFF64748B;
        }
        return isDarkTheme ? 0xFF38BDF8 : 0xFF0284C7;
    }
}
