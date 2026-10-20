package org.nayagram.platform.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.FileLog;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Local-only RFC 6238 TOTP authentication layer (NG Authenticator / NGSafety)
 * and Auto-lock lifecycle controller.
 * No Google API or network request is used.
 * Privacy Policy: Local TOTP only, no data sent to Google server.
 * The TOTP secret, auto-lock configuration, and backup hashes are stored in encrypted preferences.
 */
public final class NGSafety {
    private static final String PREFS = "ng_safety_secure";
    private static final String LEGACY_PREFS = "nayagram_guard_secure";
    private static final String ENABLED = "enabled";
    private static final String KEY_TOTP_SECRET = "totp_secret";
    private static final String KEY_PENDING_SECRET = "pending_secret";
    private static final String BACKUP_PREFIX = "backup_hash_";
    private static final int BACKUP_COUNT = 15;
    private static final String BACKUP_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    public static final String KEY_AUTOLOCK_MINUTES = "autolock_minutes";
    private static final String KEY_LAST_BG_TIME = "last_bg_time";
    private static final String KEY_LAST_VERIFIED_TIME = "last_verified_time";
    public static final int DEFAULT_AUTOLOCK_MINUTES = 60; // 1 hour

    private static volatile boolean sessionVerified;
    private static Runnable backgroundLockRunnable;
    // Monotonic clock for same-process background intervals; wall-clock timestamp remains
    // persisted as a fallback when Android kills and later recreates the process.
    private static volatile long backgroundElapsedRealtime = 0L;

    private NGSafety() {}

    private static SharedPreferences prefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        SharedPreferences primary = EncryptedSharedPreferences.create(
                context.getApplicationContext(),
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);

        // Migrate seamlessly from legacy preferences if present
        if (!primary.contains(ENABLED)) {
            try {
                SharedPreferences legacy = EncryptedSharedPreferences.create(
                        context.getApplicationContext(),
                        LEGACY_PREFS,
                        masterKey,
                        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
                if (legacy.contains(ENABLED)) {
                    SharedPreferences.Editor editor = primary.edit();
                    for (java.util.Map.Entry<String, ?> entry : legacy.getAll().entrySet()) {
                        Object val = entry.getValue();
                        if (val instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) val);
                        else if (val instanceof String) editor.putString(entry.getKey(), (String) val);
                        else if (val instanceof Integer) editor.putInt(entry.getKey(), (Integer) val);
                        else if (val instanceof Long) editor.putLong(entry.getKey(), (Long) val);
                        else if (val instanceof Float) editor.putFloat(entry.getKey(), (Float) val);
                    }
                    editor.apply();
                }
            } catch (Throwable ignored) {
            }
        }
        return primary;
    }

    public static boolean isEnabled(Context context) {
        try {
            return prefs(context).getBoolean(ENABLED, false);
        } catch (Exception e) {
            // Fail closed when encrypted state cannot be read.
            return true;
        }
    }

    public static boolean isSessionVerified() {
        return sessionVerified;
    }

    public static void markSessionVerified() {
        sessionVerified = true;
    }

    public static void markSessionVerified(Context context) {
        sessionVerified = true;
        try {
            prefs(context).edit().putLong(KEY_LAST_VERIFIED_TIME, System.currentTimeMillis()).apply();
        } catch (Throwable ignored) {
        }
    }

    public static void lockSession() {
        sessionVerified = false;
    }

    public static int getAutoLockMinutes(Context context) {
        try {
            return prefs(context).getInt(KEY_AUTOLOCK_MINUTES, DEFAULT_AUTOLOCK_MINUTES);
        } catch (Exception e) {
            return DEFAULT_AUTOLOCK_MINUTES;
        }
    }

    public static boolean setAutoLockMinutes(Context context, int minutes) {
        if (minutes < 0 || minutes > 24 * 60) {
            return false;
        }
        try {
            prefs(context).edit().putInt(KEY_AUTOLOCK_MINUTES, minutes).apply();
            FileLog.d("NG Auto-lock preset saved: " + minutes + "min");
            return getAutoLockMinutes(context) == minutes;
        } catch (Exception e) {
            FileLog.e(e);
            return false;
        }
    }

    public static String getAutoLockDisplayString(int minutes) {
        if (minutes == 0) return "Never";
        if (minutes == 15) return "15 minutes";
        if (minutes == 45) return "45 minutes";
        if (minutes == 60) return "1 hour";
        if (minutes == 180) return "3 hours";
        if (minutes == 300) return "5 hours";
        if (minutes == 720) return "12 hours";
        int hours = minutes / 60;
        int mins = minutes % 60;
        if (hours > 0 && mins > 0) return hours + "h " + mins + "m";
        if (hours > 0) return hours + " hour" + (hours > 1 ? "s" : "");
        return mins + " minutes";
    }

    public static void onAppBackground(Context context) {
        if (!isEnabled(context)) return;
        long now = System.currentTimeMillis();
        backgroundElapsedRealtime = SystemClock.elapsedRealtime();
        try {
            prefs(context).edit().putLong(KEY_LAST_BG_TIME, now).apply();
        } catch (Throwable ignored) {
        }
        int minutes = getAutoLockMinutes(context);
        if (minutes > 0) {
            cancelBackgroundLockRunnable();
            backgroundLockRunnable = () -> lockSession();
            AndroidUtilities.runOnUIThread(backgroundLockRunnable, (long) minutes * 60 * 1000L);
        }
    }

    public static void onAppForeground(Context context) {
        cancelBackgroundLockRunnable();
        if (!isEnabled(context)) return;
        if (isAutoLockExpired(context)) {
            lockSession();
        }
        backgroundElapsedRealtime = 0L;
        try {
            prefs(context).edit().putLong(KEY_LAST_BG_TIME, 0L).apply();
        } catch (Throwable ignored) {
        }
    }

    public static boolean isAutoLockExpired(Context context) {
        if (!isEnabled(context)) return false;
        int minutes = getAutoLockMinutes(context);
        if (minutes == 0) return false;
        try {
            long lastBg = prefs(context).getLong(KEY_LAST_BG_TIME, 0L);
            if (lastBg > 0) {
                long elapsed;
                long startedAtElapsed = backgroundElapsedRealtime;
                if (startedAtElapsed > 0L) {
                    // Wall-clock edits must not shorten or extend a live background timeout.
                    elapsed = SystemClock.elapsedRealtime() - startedAtElapsed;
                } else {
                    // After process death, elapsedRealtime's old value is not available;
                    // use the persisted wall-clock timestamp as a best-effort fallback.
                    elapsed = System.currentTimeMillis() - lastBg;
                }
                return elapsed >= ((long) minutes * 60 * 1000L);
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void cancelBackgroundLockRunnable() {
        if (backgroundLockRunnable != null) {
            AndroidUtilities.cancelRunOnUIThread(backgroundLockRunnable);
            backgroundLockRunnable = null;
        }
    }

    public static String beginSetup(Context context) throws Exception {
        // The generator argument is Base32 characters: 32 characters = 160 bits of entropy.
        String secret = new DefaultSecretGenerator(32).generate();
        prefs(context).edit().putString(KEY_PENDING_SECRET, secret).apply();
        return secret;
    }

    public static void cancelSetup(Context context) {
        try {
            prefs(context).edit().remove(KEY_PENDING_SECRET).apply();
        } catch (Exception ignored) {
            // A pending secret is never promoted to the active secret before confirmation.
        }
    }

    public static String getPendingSecret(Context context) throws Exception {
        return prefs(context).getString(KEY_PENDING_SECRET, null);
    }

    public static String getOtpAuthUri(String accountLabel, String secret) {
        String user = accountLabel == null || accountLabel.trim().isEmpty() ? "user" : accountLabel.trim();
        // Keep the issuer/account separator colon literal as required by otpauth URI labels.
        String label = "NayaGram:" + urlEncode(user);
        return "otpauth://totp/" + label + "?secret=" + urlEncode(secret)
                + "&issuer=" + urlEncode("NayaGram") + "&algorithm=SHA1&digits=6&period=30";
    }

    private static String urlEncode(String value) {
        try {
            return java.net.URLEncoder.encode(value, "UTF-8").replace("+", "%20");
        } catch (Exception e) {
            return value;
        }
    }

    public static boolean verifyCode(String secret, String code) {
        if (secret == null || code == null || !code.matches("\\d{6}")) {
            return false;
        }
        try {
            DefaultCodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
            verifier.setTimePeriod(30);
            verifier.setAllowedTimePeriodDiscrepancy(1);
            return verifier.isValidCode(secret, code);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Completes enrollment only after a valid live TOTP code is entered.
     * Returns fifteen one-time backup codes; only their SHA-256 hashes are retained.
     */
    public static List<String> confirmSetup(Context context, String code) throws Exception {
        SharedPreferences secure = prefs(context);
        String secret = secure.getString(KEY_PENDING_SECRET, null);
        if (secret == null || !verifyCode(secret, code)) {
            return null;
        }
        ArrayList<String> backupCodes = new ArrayList<>(BACKUP_COUNT);
        SharedPreferences.Editor editor = secure.edit().putString(KEY_TOTP_SECRET, secret).remove(KEY_PENDING_SECRET);
        SecureRandom random = new SecureRandom();
        Set<String> uniqueCodes = new HashSet<>();
        while (uniqueCodes.size() < BACKUP_COUNT) {
            StringBuilder raw = new StringBuilder(10);
            for (int j = 0; j < 10; j++) {
                raw.append(BACKUP_ALPHABET.charAt(random.nextInt(BACKUP_ALPHABET.length())));
            }
            uniqueCodes.add(raw.toString());
        }
        int index = 0;
        for (String display : uniqueCodes) {
            backupCodes.add(display);
            editor.putString(BACKUP_PREFIX + index++, sha256(display));
        }
        editor.putBoolean(ENABLED, true);
        editor.putLong(KEY_LAST_VERIFIED_TIME, System.currentTimeMillis());
        editor.apply();
        markSessionVerified();
        return backupCodes;
    }

    public static boolean verifyStoredCode(Context context, String code) {
        try {
            SharedPreferences secure = prefs(context);
            if (!secure.getBoolean(ENABLED, false)) {
                return true;
            }
            String secret = secure.getString(KEY_TOTP_SECRET, null);
            if (verifyCode(secret, code)) {
                markSessionVerified(context);
                return true;
            }
            if (code == null || code.length() < 6 || code.length() > 10) {
                return false;
            }
            String candidate = sha256(code.trim().toUpperCase(Locale.US));
            for (int i = 0; i < BACKUP_COUNT; i++) {
                String stored = secure.getString(BACKUP_PREFIX + i, null);
                if (stored != null && constantTimeEquals(stored, candidate)) {
                    secure.edit().remove(BACKUP_PREFIX + i)
                            .putLong(KEY_LAST_VERIFIED_TIME, System.currentTimeMillis())
                            .apply();
                    markSessionVerified();
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    public static void disable(Context context) throws Exception {
        cancelBackgroundLockRunnable();
        SharedPreferences secure = prefs(context);
        SharedPreferences.Editor editor = secure.edit().clear();
        editor.apply();
        try {
            MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            SharedPreferences legacy = EncryptedSharedPreferences.create(
                    context.getApplicationContext(),
                    LEGACY_PREFS,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
            legacy.edit().clear().apply();
        } catch (Throwable ignored) {}
        lockSession();
    }

    private static String sha256(String value) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder out = new StringBuilder(digest.length * 2);
        for (byte b : digest) {
            out.append(String.format(Locale.US, "%02x", b & 0xff));
        }
        return out.toString();
    }

    private static boolean constantTimeEquals(String a, String b) {
        return MessageDigest.isEqual(a.getBytes(StandardCharsets.UTF_8), b.getBytes(StandardCharsets.UTF_8));
    }
}
