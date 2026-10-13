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

    // -------------------------------------------------------------------------
    // Factory methods
    // -------------------------------------------------------------------------

    /**
     * Creates an instance from a Lottie file, optionally overriding its JSON
     * content.
     *
     * @param path             absolute path to the .json / .tgs file
     * @param json             override JSON string, or {@code null} to read from file
     * @param colorReplacement optional color replacement table, may be {@code null}
     * @param fitzModifier     Fitzpatrick skin-tone modifier (0 = none)
     * @return a new instance, or {@code null} if the native layer failed
     */
    public static RLottieNative createFromFile(
            String path, String json, @Nullable int[] metaOut,
            @Nullable int[] colorReplacement, int fitzModifier,
            @Nullable Map<String, Integer> layerColors) {
        int[] meta = new int[3];
        long ptr = create(path, json, meta, colorReplacement, fitzModifier, layerColors);
        if (ptr == 0) {
            return null;
        }
        if (metaOut != null && metaOut.length == 3) {
            System.arraycopy(meta, 0, metaOut, 0, 3);
        }
        return new RLottieNative(ptr, meta);
    }

    public static RLottieNative createFromFile(String path) {
        return createFromFile(path, null, null, null, 0, null);
    }


    /**
     * Creates an instance from a raw JSON string using an animation
     * {@code name} for debugging.
     *
     * @param json             Lottie JSON string (must not be null or empty)
     * @param colorReplacement optional color replacement table, may be {@code null}
     * @return a new instance, or {@code null} if the native layer failed
     */
    public static RLottieNative createFromRawJson(
            String json, @Nullable int[] metaOut, int[] colorReplacement,
            @Nullable Map<String, Integer> layerColors) {
        if (json == null || json.isEmpty()) {
            return null;
        }
        int[] meta = new int[3];
        String[] layerNames = layerColors == null ? null : layerColors.keySet().toArray(new String[0]);
        int[] layerValues = layerColors == null ? null : layerNamesToColors(layerNames, layerColors);
        long ptr = createWithJson(json, meta, colorReplacement, layerNames, layerValues);
        if (ptr == 0) {
            return null;
        }
        if (metaOut != null && metaOut.length == 3) {
            System.arraycopy(meta, 0, metaOut, 0, 3);
        }
        return new RLottieNative(ptr, meta);
    }

    public static RLottieNative createFromRawJson(String json) {
        return createFromRawJson(json, null, null, null);
    }

    public static RLottieNative createFromRawJson(String json, @Nullable int[] metaOut) {
        return createFromRawJson(json, metaOut, null, null);
    }

    // -------------------------------------------------------------------------
    // Instance API
    // -------------------------------------------------------------------------

    /**
     * Renders {@code frame} into {@code bitmap}.
     *
     * @param frame  zero-based frame index
     * @param bitmap target bitmap; must be {@link Bitmap.Config#ARGB_8888} or
     *               {@link Bitmap.Config#ALPHA_8}. Alpha8 selects tlottie's
     *               direct alpha-only renderer.
     * @param clear  whether to clear the bitmap before rendering
     * @return the frame index that was rendered, -1 on error, or -5 when the
     *         decoder is busy (caller should retry after a short delay)
     * @throws IllegalStateException if already recycled
     */
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

    private static long create(String src, String json, int[] params, int[] colorReplacement, int fitzModifier, @Nullable Map<String, Integer> layerColors) {
        Trace.beginSection("RLottieNative#create");
        try {
            String[] layerNames = layerColors == null ? null : layerColors.keySet().toArray(new String[0]);
            int[] layerValues = layerColors == null ? null : layerNamesToColors(layerNames, layerColors);
            return nCreate(src, json, params, colorReplacement, fitzModifier, layerNames, layerValues);
        } finally {
            Trace.endSection();
        }
    }

    /**
     * Creates a native animation from a JSON string and returns the raw pointer.
     * Prefer {@link #createFromRawJson} for new code.
     */
    private static long createWithJson(String json, int[] params, int[] colorReplacement, String[] layerNames, int[] layerColors) {
        Trace.beginSection("RLottieNative#createWithJson");
        try {
            return nCreateWithJson(json, params, colorReplacement, layerNames, layerColors);
        } finally {
            Trace.endSection();
        }
    }

    /**
     * Renders a frame directly using a raw pointer.
     * Prefer the instance method {@link #getFrame(int, Bitmap, boolean)} for new code.
     */
    private static int getFrame(long ptr, int frame, Bitmap bitmap, boolean clear) {
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

    /**
     * Destroys a native animation directly using a raw pointer.
     * Prefer {@link #recycle()} for new code.
     */
    private static void destroy(long ptr) {
        Trace.beginSection("RLottieNative#destroy");
        try {
            nDestroy(ptr);
        } finally {
            Trace.endSection();
        }
    }

    public static long getFramesCount(String src, String json) {
        final RLottieNative rLottieNative = createFromFile(src, json, null, null, 0, null);
        if (rLottieNative != null) {
            final int framesCount = rLottieNative.getFrameCount();
            rLottieNative.recycle();
            return framesCount;
        }
        return 0;
    }

    public static double getDuration(String src, String json) {
        final RLottieNative rLottieNative = createFromFile(src, json, null, null, 0, null);
        if (rLottieNative != null) {
            final int framesCount = rLottieNative.getFrameCount();
            final int fps = rLottieNative.getFps();
            rLottieNative.recycle();
            return (double) framesCount / fps;
        }
        return 0;
    }



    // -------------------------------------------------------------------------
    // Native
    // -------------------------------------------------------------------------

    private static native long nCreate(String src, String json, int[] params, int[] colorReplacement, int fitzModifier, String[] layerNames, int[] layerColors);

    private static native long nCreateWithJson(String json, int[] params, int[] colorReplacement, String[] layerNames, int[] layerColors);

    private static native int nGetFrame(long ptr, int frame, Bitmap bitmap, boolean clear);
    private static native void nDestroy(long ptr);
}
