package org.nayagram.platform.storage;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.File;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SmartStorageDoctor - Intelligent Cache & Storage Optimizer for NayaGram.
 * 100% Google Play Store Compliant: Helps users manage local device storage safely
 * without deleting essential chat history or personal account credentials.
 */
public final class SmartStorageDoctor {

    private static volatile SmartStorageDoctor sInstance;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    private SmartStorageDoctor() {}

    public static SmartStorageDoctor getInstance() {
        if (sInstance == null) {
            synchronized (SmartStorageDoctor.class) {
                if (sInstance == null) {
                    sInstance = new SmartStorageDoctor();
                }
            }
        }
        return sInstance;
    }

    public static class StorageReport {
        public long cacheSizeBytes;
        public long externalCacheSizeBytes;
        public long mediaTempSizeBytes;
        public long totalReclaimableBytes;
        public int fileCount;

        public String getFormattedTotal() {
            return formatSize(totalReclaimableBytes);
        }

        public String getFormattedCache() {
            return formatSize(cacheSizeBytes + externalCacheSizeBytes);
        }
    }

    public interface ScanCallback {
        void onScanComplete(StorageReport report);
    }

    public interface CleanCallback {
        void onCleanComplete(long bytesFreed, boolean success);
    }

    /**
     * Format raw byte count into human-readable representation (e.g. 250 MB, 1.2 GB).
     */
    public static String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        if (digitGroups >= units.length) digitGroups = units.length - 1;
        return String.format(Locale.US, "%.1f %s", bytes / Math.pow(1024, digitGroups), units[digitGroups]);
    }

    /**
     * Asynchronously scan local cache and reclaimable files.
     */
    public void scanStorage(Context context, ScanCallback callback) {
        final Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            StorageReport report = new StorageReport();

            // 1. Internal Cache
            File cacheDir = appContext.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                report.cacheSizeBytes = calculateDirSize(cacheDir, report);
            }

            // 2. External Cache
            File extCacheDir = appContext.getExternalCacheDir();
            if (extCacheDir != null && extCacheDir.exists()) {
                report.externalCacheSizeBytes = calculateDirSize(extCacheDir, report);
            }

            report.totalReclaimableBytes = report.cacheSizeBytes + report.externalCacheSizeBytes;

            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onScanComplete(report);
                }
            });
        });
    }

    /**
     * Asynchronously clean cache directories safely.
     */
    public void cleanCache(Context context, CleanCallback callback) {
        final Context appContext = context.getApplicationContext();
        executor.execute(() -> {
            long bytesFreed = 0;

            File cacheDir = appContext.getCacheDir();
            if (cacheDir != null && cacheDir.exists()) {
                bytesFreed += deleteDirContents(cacheDir);
            }

            File extCacheDir = appContext.getExternalCacheDir();
            if (extCacheDir != null && extCacheDir.exists()) {
                bytesFreed += deleteDirContents(extCacheDir);
            }

            final long finalFreed = bytesFreed;
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onCleanComplete(finalFreed, true);
                }
            });
        });
    }

    private long calculateDirSize(File dir, StorageReport report) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                size += calculateDirSize(file, report);
            } else {
                size += file.length();
                if (report != null) {
                    report.fileCount++;
                }
            }
        }
        return size;
    }

    private long deleteDirContents(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long deletedBytes = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            if (file.isDirectory()) {
                deletedBytes += deleteDirContents(file);
                file.delete();
            } else {
                long length = file.length();
                if (file.delete()) {
                    deletedBytes += length;
                }
            }
        }
        return deletedBytes;
    }
}
