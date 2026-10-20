package org.nayagram.platform.security;

import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import org.telegram.ui.Components.ForegroundDetector;

/**
 * Persists NG Safety's app-background timestamp and applies the existing NGSafety gate.
 * No parallel lock screen is created; MainTabsActivity already presents NGSafetyActivity.
 */
public final class NGAutoLockManager {
    private static volatile boolean initialized;
    private static volatile long backgroundElapsedRealtime;

    private NGAutoLockManager() {}

    public static int getTimeoutMinutes(Context context) {
        return NGSafety.getAutoLockMinutes(context);
    }

    public static void saveTimeoutMinutes(Context context, int minutes) {
        int safeMinutes = Math.max(0, Math.min(minutes, 525600));
        try {
            NGSafety.saveAutoLockMinutes(context, safeMinutes);
        } catch (Exception e) {
            throw new IllegalStateException("Could not securely save NG Auto-lock settings", e);
        }
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
        backgroundElapsedRealtime = 0L;
        ForegroundDetector detector = ForegroundDetector.getInstance();
        if (detector != null) {
            detector.addListener(new ForegroundDetector.Listener() {
                @Override public void onBecameForeground() {
                    evaluateExpiredTimeout(app);
                }

                @Override public void onBecameBackground() {
                    long nowElapsed = SystemClock.elapsedRealtime();
                    backgroundElapsedRealtime = nowElapsed;
                    try {
                        NGSafety.saveAutoLockBackgroundWallTime(app, System.currentTimeMillis());
                    } catch (Exception e) {
                        if (org.telegram.messenger.BuildVars.DEBUG_VERSION) {
                            Log.w("NGAutoLock", "Could not persist auto-lock background timestamp", e);
                        }
                    }
                }
            });
            initialized = true;
            // A persisted wall-clock timestamp handles process death/reboot where elapsedRealtime resets.
            evaluateExpiredTimeout(app);
        }
    }

    private static void evaluateExpiredTimeout(Context context) {
        int minutes = NGSafety.getAutoLockMinutes(context);
        if (minutes <= 0) return;
        long backgroundWallTime = NGSafety.getAutoLockBackgroundWallTime(context);
        if (backgroundWallTime <= 0L) return;
        long awayMillis = Math.max(0L, System.currentTimeMillis() - backgroundWallTime);
        if (awayMillis >= minutes * 60_000L && NGSafety.isEnabled(context)) {
            NGSafety.lockSession();
        }
        if (ForegroundDetector.getInstance() != null && ForegroundDetector.getInstance().isForeground()) {
            try {
                NGSafety.saveAutoLockBackgroundWallTime(context, 0L);
            } catch (Exception e) {
                if (org.telegram.messenger.BuildVars.DEBUG_VERSION) {
                    Log.w("NGAutoLock", "Could not clear auto-lock background timestamp", e);
                }
            }
            backgroundElapsedRealtime = 0L;
        }
    }
}
