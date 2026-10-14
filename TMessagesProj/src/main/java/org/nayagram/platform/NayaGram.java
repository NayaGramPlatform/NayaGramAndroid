package org.nayagram.platform;

import org.telegram.messenger.FileLog;

/**
 * NayaGram - Core Feature Manager for NayaGram Android Client.
 * Manages security blocking for APK, ZIP, EXE and other pro privacy features
 * matching Novagram / NayaGram architecture standards.
 */
public class NayaGram {

    public static boolean isBlockApkZipEnabled() {
        try {
            return NayaConfig.getInstance().isApkBlockEnabled();
        } catch (Throwable e) {
            return false;
        }
    }

    public static boolean shouldBlockFile(String fileName) {
        if (!isBlockApkZipEnabled() || fileName == null) {
            return false;
        }
        String lower = fileName.toLowerCase();
        boolean blocked = lower.endsWith(".apk") || 
                          lower.endsWith(".zip") || 
                          lower.endsWith(".jar") || 
                          lower.endsWith(".exe") || 
                          lower.endsWith(".rar") || 
                          lower.endsWith(".iso") ||
                          lower.endsWith(".bat") ||
                          lower.endsWith(".cmd");
        if (blocked) {
            FileLog.d("NayaGram Security Shield: Blocked malicious/restricted file -> " + fileName);
        }
        return blocked;
    }
}
