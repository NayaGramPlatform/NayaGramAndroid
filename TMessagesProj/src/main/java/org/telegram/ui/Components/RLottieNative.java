package org.telegram.ui.Components;

import android.graphics.Bitmap;
import android.os.Trace;
import androidx.annotation.Nullable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.Map;

/**
 * Thin wrapper around a native Lottie animation pointer.
 */
public final class RLottieNative {
    private final int[] mMetaData;
    private long mNativePtr;
    private final AtomicBoolean mRecycled = new AtomicBoolean(false);

    private RLottieNative(long nativePtr, int[] metaData) {
        mNativePtr = nativePtr;
        mMetaData = metaData;
    }

    public static RLottieNative createFromFile(
            String path,
            String json,
            int w, int h,
            boolean precache,
            int[] colorReplacement,
            boolean limitFps,
            int fitzModifier) {
        return createFromFile(path, json, w, h, null, precache, colorReplacement, limitFps, fitzModifier, null);
    }

    public static RLottieNative createFromFile(
            String path, String json, int w, int h, @Nullable int[] metaOut,
            boolean precache, int[] colorReplacement, boolean limitFps, int fitzModifier,
            @Nullable Map<String, Integer> layerColors) {
        int[] meta = new int[3];
        long ptr = create(path, json, w, h, meta, precache, colorReplacement, limitFps, fitzModifier, layerColors);
        if (ptr == 0) {
            return null;
        }
        if (metaOut != null && metaOut.length == 3) {
            System.arraycopy(meta, 0, metaOut, 0, 3);
        }
        return new RLottieNative(ptr, meta);
    }

    public static RLottieNative createFromFile(
            String path,
            String json,
            @Nullable int[] metaOut,
            @Nullable int[] colorReplacement,
            int fitzModifier,
            @Nullable Map<String, Integer> layerColors) {
        return createFromFile(path, json, 0, 0, metaOut, false, colorReplacement, false, fitzModifier, layerColors);
    }

    public static RLottieNative createFromRawJson(
            String json,
            @Nullable int[] metaOut,
            @Nullable int[] colorReplacement,
            @Nullable Map<String, Integer> layerColors) {
        return createFromRawJson(json, "", metaOut, colorReplacement, layerColors);
    }

    public static RLottieNative createFromRawJson(
            String json,
            @Nullable int[] metaOut,
            @Nullable int[] colorReplacement) {
        return createFromRawJson(json, "", metaOut, colorReplacement, null);
    }

    public static RLottieNative createFromRawJson(
            String json,
            String name,
            int[] colorReplacement) {
        return createFromRawJson(json, name, null, colorReplacement, null);
    }

    public static RLottieNative createFromRawJson(
            String json,
            String name,
            @Nullable int[] metaOut,
            int[] colorReplacement) {
        return createFromRawJson(json, name, metaOut, colorReplacement, null);
    }

    public static RLottieNative createFromRawJson(
            String json, String name, @Nullable int[] metaOut, int[] colorReplacement,
            @Nullable Map<String, Integer> layerColors) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        int[] meta = new int[3];
        String[] layerNames = layerColors == null ? null : layerColors.keySet().toArray(new String[0]);
        int[] layerValues = layerColors == null ? null : layerNamesToColors(layerNames, layerColors);
        long ptr = createWithJson(json, name, meta, colorReplacement, layerNames, layerValues);
        if (ptr == 0) {
            return null;
        }
        if (metaOut != null && metaOut.length == 3) {
            System.arraycopy(meta, 0, metaOut, 0, 3);
        }
        return new RLottieNative(ptr, meta);
    }

    public int getFrame(int frame, Bitmap bitmap, boolean clear) {
        checkNotRecycled();
        return getFrame(mNativePtr, frame, bitmap, clear);
    }

    public int getFrameCount() {
        return mMetaData[0];
    }

    public int getFps() {
        return mMetaData[1];
    }

    public boolean isRecycled() {
        return mRecycled.get();
    }

    public void recycle() {
        if (mRecycled.compareAndSet(false, true)) {
            long ptr = mNativePtr;
            mNativePtr = 0;
            if (ptr != 0) {
                destroy(ptr);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            if (!mRecycled.get()) {
                recycle();
            }
        } finally {
            super.finalize();
        }
    }

    private void checkNotRecycled() {
        if (mRecycled.get()) {
            throw new IllegalStateException("Called method on a recycled RLottie instance");
        }
    }

    public static long create(String src, String json, int w, int h, int[] params, boolean precache, int[] colorReplacement, boolean limitFps, int fitzModifier) {
        return create(src, json, w, h, params, precache, colorReplacement, limitFps, fitzModifier, null);
    }

    private static long create(String src, String json, int w, int h, int[] params, boolean precache, int[] colorReplacement, boolean limitFps, int fitzModifier, @Nullable Map<String, Integer> layerColors) {
        Trace.beginSection("RLottieNative#create");
        try {
            String[] layerNames = layerColors == null ? null : layerColors.keySet().toArray(new String[0]);
            int[] layerValues = layerColors == null ? null : layerNamesToColors(layerNames, layerColors);
            return nCreate(src, json, w, h, params, precache, colorReplacement, limitFps, fitzModifier, layerNames, layerValues);
        } finally {
            Trace.endSection();
        }
    }

    private static long createWithJson(String json, String name, int[] params, int[] colorReplacement, String[] layerNames, int[] layerColors) {
        Trace.beginSection("RLottieNative#createWithJson");
        try {
            return nCreateWithJson(json, name, params, colorReplacement, layerNames, layerColors);
        } finally {
            Trace.endSection();
        }
    }

    public static int getFrame(long ptr, int frame, Bitmap bitmap, boolean clear) {
        Trace.beginSection("RLottieNative#getFrame");
        try {
            return nGetFrame(ptr, frame, bitmap, clear);
        } finally {
            Trace.endSection();
        }
    }

    private static int[] layerNamesToColors(String[] layerNames, Map<String, Integer> layerColors) {
        int[] result = new int[layerNames.length];
        for (int i = 0; i < layerNames.length; i++) {
            result[i] = layerColors.get(layerNames[i]);
        }
        return result;
    }

    public static void destroy(long ptr) {
        Trace.beginSection("RLottieNative#destroy");
        try {
            nDestroy(ptr);
        } finally {
            Trace.endSection();
        }
    }

    public static long getFramesCount(String src, String json) {
        final RLottieNative rLottieNative = createFromFile(src, json, 0, 0, false, null, false, 0);
        if (rLottieNative != null) {
            final int framesCount = rLottieNative.getFrameCount();
            rLottieNative.recycle();
            return framesCount;
        }
        return 0;
    }

    public static double getDuration(String src, String json) {
        final RLottieNative rLottieNative = createFromFile(src, json, 0, 0, false, null, false, 0);
        if (rLottieNative != null) {
            final int framesCount = rLottieNative.getFrameCount();
            final int fps = rLottieNative.getFps();
            rLottieNative.recycle();
            return (double) framesCount / fps;
        }
        return 0;
    }

    private static native long nCreate(String src, String json, int w, int h, int[] params, boolean precache, int[] colorReplacement, boolean limitFps, int fitzModifier, String[] layerNames, int[] layerColors);
    private static native long nCreateWithJson(String json, String name, int[] params, int[] colorReplacement, String[] layerNames, int[] layerColors);
    private static native int nGetFrame(long ptr, int frame, Bitmap bitmap, boolean clear);
    private static native void nDestroy(long ptr);
}
