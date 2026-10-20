package org.nayagram.platform.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import org.telegram.ui.Components.ForegroundDetector;

/**
 * Persists NG Safety's app-background timestamp and applies the existing NGSafety gate.
 * No parallel lock screen is created; MainTabsActivity already presents NGSafetyActivity.
 */
public final class NGAutoLockManager {
    private static final String PREFS = "ng_auto_lock";
    private static final String KEY_MINUTES = "timeout_minutes";
    private static final String KEY_BACKGROUND_WALL_TIME = "background_wall_time";
    private static final String KEY_BACKGROUND_ELAPSED = "background_elapsed";
    private static volatile boolean initialized;
    private static volatile long backgroundElapsedRealtime;

    private NGAutoLockManager() {}

    public static int getTimeoutMinutes(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getInt(KEY_MINUTES, 0);
    }

    public static void saveTimeoutMinutes(Context context, int minutes) {
        int safeMinutes = Math.max(0, Math.min(minutes, 525600));
        context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putInt(KEY_MINUTES, safeMinutes).apply();
        if (org.telegram.messenger.BuildVars.DEBUG_VERSION) {
            Log.d("NGAutoLock", "NG Auto-lock preset saved: " + safeMinutes + "min");
        }
    }

    public static synchronized void initialize(Context context) {
        if (initialized) {
            evaluateExpiredTimeout(context);
            return;
        }
        Context app = context.getApplicationContext();
        SharedPreferences prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        backgroundElapsedRealtime = prefs.getLong(KEY_BACKGROUND_ELAPSED, 0L);
        ForegroundDetector detector = ForegroundDetector.getInstance();
        if (detector != null) {
            detector.addListener(new ForegroundDetector.Listener() {
                @Override public void onBecameForeground() {
                    evaluateExpiredTimeout(app);
                }

                @Override public void onBecameBackground() {
                    long nowElapsed = SystemClock.elapsedRealtime();
                    backgroundElapsedRealtime = nowElapsed;
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                            .putLong(KEY_BACKGROUND_WALL_TIME, System.currentTimeMillis())
                            .putLong(KEY_BACKGROUND_ELAPSED, nowElapsed).apply();
                }
            });
            initialized = true;
            // A persisted wall-clock timestamp handles process death/reboot where elapsedRealtime resets.
            evaluateExpiredTimeout(app);
        }
    }

    private static void evaluateExpiredTimeout(Context context) {
        SharedPreferences prefs = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        int minutes = prefs.getInt(KEY_MINUTES, 0);
        if (minutes <= 0) return;
        long backgroundWallTime = prefs.getLong(KEY_BACKGROUND_WALL_TIME, 0L);
        if (backgroundWallTime <= 0L) return;
        long awayMillis = Math.max(0L, System.currentTimeMillis() - backgroundWallTime);
        if (awayMillis >= minutes * 60_000L && NGSafety.isEnabled(context)) {
            NGSafety.lockSession();
        }
        if (ForegroundDetector.getInstance() != null && ForegroundDetector.getInstance().isForeground()) {
            prefs.edit().remove(KEY_BACKGROUND_WALL_TIME).remove(KEY_BACKGROUND_ELAPSED).apply();
            backgroundElapsedRealtime = 0L;
        }
    }
}
