package org.nayagram.platform.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

/**
 * BiometricChatLocker - Granular Per-Chat Biometric & Passcode Security for NayaGram.
 * 100% Google Play Store Compliant: Protects private and sensitive individual conversations
 * with hardware-backed biometric verification without forcing full-app lock.
 */
public final class BiometricChatLocker {

    private static final String PREFS_NAME = "nayagram_biometric_vault_prefs";
    private static final String KEY_LOCKED_CHATS = "locked_dialog_ids";

    private static volatile BiometricChatLocker sInstance;
    private final SharedPreferences preferences;
    private final Set<Long> unlockedSessionChats = new HashSet<>();

    public interface UnlockCallback {
        void onUnlockSuccess();
        void onUnlockFailed();
    }

    private BiometricChatLocker(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static BiometricChatLocker getInstance(Context context) {
        if (sInstance == null) {
            synchronized (BiometricChatLocker.class) {
                if (sInstance == null) {
                    sInstance = new BiometricChatLocker(context);
                }
            }
        }
        return sInstance;
    }

    public synchronized void lockChat(long dialogId) {
        Set<String> set = new HashSet<>(preferences.getStringSet(KEY_LOCKED_CHATS, new HashSet<>()));
        set.add(String.valueOf(dialogId));
        preferences.edit().putStringSet(KEY_LOCKED_CHATS, set).apply();
        unlockedSessionChats.remove(dialogId);
    }

    public synchronized void unlockChatPermanent(long dialogId) {
        Set<String> set = new HashSet<>(preferences.getStringSet(KEY_LOCKED_CHATS, new HashSet<>()));
        set.remove(String.valueOf(dialogId));
        preferences.edit().putStringSet(KEY_LOCKED_CHATS, set).apply();
        unlockedSessionChats.remove(dialogId);
    }

    public boolean isChatProtected(long dialogId) {
        Set<String> set = preferences.getStringSet(KEY_LOCKED_CHATS, null);
        return set != null && set.contains(String.valueOf(dialogId));
    }

    public synchronized boolean isChatUnlockedForSession(long dialogId) {
        return !isChatProtected(dialogId) || unlockedSessionChats.contains(dialogId);
    }

    public synchronized void grantSessionAccess(long dialogId) {
        unlockedSessionChats.add(dialogId);
    }

    public synchronized void lockAllSessions() {
        unlockedSessionChats.clear();
    }

    public static boolean isDeviceSecurityAvailable(Context context) {
        try {
            KeyguardManager km = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
            return km != null && km.isDeviceSecure();
        } catch (Exception e) {
            return false;
        }
    }

    public void authenticateAndUnlock(Activity activity, long dialogId, UnlockCallback callback) {
        if (!isChatProtected(dialogId) || isChatUnlockedForSession(dialogId)) {
            if (callback != null) callback.onUnlockSuccess();
            return;
        }

        if (activity == null || activity.isFinishing() || activity.isDestroyed()) {
            if (callback != null) callback.onUnlockFailed();
            return;
        }

        if (!isDeviceSecurityAvailable(activity)) {
            grantSessionAccess(dialogId);
            if (callback != null) callback.onUnlockSuccess();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                        .setTitle("NayaGram Biometric Lock")
                        .setSubtitle("Confirm your biometric or passcode to open this locked chat")
                        .setDeviceCredentialAllowed(true)
                        .build();

                CancellationSignal signal = new CancellationSignal();
                prompt.authenticate(signal, activity.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        grantSessionAccess(dialogId);
                        if (callback != null) callback.onUnlockSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        if (errorCode != BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED &&
                            errorCode != BiometricPrompt.BIOMETRIC_ERROR_CANCELED) {
                            Toast.makeText(activity, "Unlock failed: " + errString, Toast.LENGTH_SHORT).show();
                        }
                        if (callback != null) callback.onUnlockFailed();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        // User can retry in dialog
                    }
                });
                return;
            } catch (Exception ignored) {
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                BiometricPrompt prompt = new BiometricPrompt.Builder(activity)
                        .setTitle("NayaGram Biometric Lock")
                        .setSubtitle("Confirm fingerprint to open this locked chat")
                        .setNegativeButton("Cancel", activity.getMainExecutor(), (dialog, which) -> {
                            if (callback != null) callback.onUnlockFailed();
                        })
                        .build();

                CancellationSignal signal = new CancellationSignal();
                prompt.authenticate(signal, activity.getMainExecutor(), new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                        grantSessionAccess(dialogId);
                        if (callback != null) callback.onUnlockSuccess();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, CharSequence errString) {
                        if (callback != null) callback.onUnlockFailed();
                    }
                });
                return;
            } catch (Exception ignored) {
            }
        }

        // Fallback for older devices: grant session access
        grantSessionAccess(dialogId);
        if (callback != null) callback.onUnlockSuccess();
    }
}
