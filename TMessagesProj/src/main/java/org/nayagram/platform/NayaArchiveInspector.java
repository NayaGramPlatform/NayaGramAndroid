package org.nayagram.platform;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Finds Android app packages hidden under another name. An APK is a ZIP archive, so a renamed APK
 * still carries AndroidManifest.xml or classes.dex at its root. DOCX and XLSX are ZIP files too,
 * but they never contain those entries, so they are allowed. Pure Java, no Android APIs, so it can be unit tested.
 */
public final class NayaArchiveInspector {

    private static final String MANIFEST_ENTRY = "AndroidManifest.xml";
    private static final Pattern DEX_ENTRY = Pattern.compile("classes\\d*\\.dex");
    private static final int ZIP_SIGNATURE = 0x504B0304; // "PK\3\4"

    private NayaArchiveInspector() {
    }

    /** True when the file starts with the ZIP local-file signature. */
    public static boolean isZipFile(File file) {
        if (file == null || !file.isFile()) {
            return false;
        }
        try (DataInputStream in = new DataInputStream(new FileInputStream(file))) {
            return in.readInt() == ZIP_SIGNATURE;
        } catch (IOException e) {
            return false;
        }
    }

    /** True when the entry name is an Android package payload at the archive root. */
    public static boolean isAndroidPayloadEntry(String entryName) {
        if (entryName == null) {
            return false;
        }
        return MANIFEST_ENTRY.equals(entryName) || DEX_ENTRY.matcher(entryName).matches();
    }

    /** True when the file is a ZIP-format file that contains an Android package payload. */
    public static boolean containsAndroidPayload(File file) {
        if (!isZipFile(file)) {
            return false;
        }
        try (ZipFile zip = new ZipFile(file)) {
            Enumeration<? extends ZipEntry> entries = zip.entries();
            while (entries.hasMoreElements()) {
                if (isAndroidPayloadEntry(entries.nextElement().getName())) {
                    return true;
                }
            }
        } catch (Exception e) {
            return false;
        }
        return false;
    }

    /** Decision after a download finishes: only incoming files, only when the shield is on. */
    public static boolean shouldBlockIncomingPayload(boolean shieldOn, boolean sentByMe, File file) {
        return shieldOn && !sentByMe && containsAndroidPayload(file);
    }
}
