package org.nayagram.platform.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Local-only RFC 6238 TOTP guard. No Google API or network request is used.
 * Privacy Policy: Local TOTP only, no data sent to Google server.
 * The TOTP secret and one-time backup-code hashes are stored in encrypted preferences.
 */
public final class NayaGramGuard {
    private static final String PREFS = "nayagram_guard_secure";
    private static final String ENABLED = "enabled";
    private static final String SECRET = "totp_secret";
    private static final String PENDING_SECRET = "pending_secret";
    private static final String BACKUP_PREFIX = "backup_hash_";
    private static final int BACKUP_COUNT = 10;
    private static volatile boolean sessionVerified;

    private NayaGramGuard() {}

    private static SharedPreferences prefs(Context context) throws Exception {
        MasterKey masterKey = new MasterKey.Builder(context.getApplicationContext())
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build();
        return EncryptedSharedPreferences.create(
                context.getApplicationContext(),
                PREFS,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM);
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

    public static void lockSession() {
        sessionVerified = false;
    }

    public static String beginSetup(Context context) throws Exception {
        // 160-bit secret, compatible with Google Authenticator and RFC 6238.
        String secret = new DefaultSecretGenerator(160).generate();
        prefs(context).edit().putString(PENDING_SECRET, secret).apply();
        return secret;
    }

    public static String getPendingSecret(Context context) throws Exception {
        return prefs(context).getString(PENDING_SECRET, null);
    }

    public static String getOtpAuthUri(String accountLabel, String secret) {
        String label = "NayaGram:" + (accountLabel == null || accountLabel.trim().isEmpty() ? "user" : accountLabel.trim());
        return "otpauth://totp/" + urlEncode(label) + "?secret=" + urlEncode(secret)
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
            CodeVerifier verifier = new DefaultCodeVerifier(new DefaultCodeGenerator(), new SystemTimeProvider());
            verifier.setTimePeriod(30);
            verifier.setAllowedTimePeriodDiscrepancy(1);
            return verifier.isValidCode(secret, code);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Completes enrollment only after a valid live TOTP code is entered.
     * Returns ten one-time backup codes; only their SHA-256 hashes are retained.
     */
    public static List<String> confirmSetup(Context context, String code) throws Exception {
        SharedPreferences secure = prefs(context);
        String secret = secure.getString(PENDING_SECRET, null);
        if (secret == null || !verifyCode(secret, code)) {
            return null;
        }
        ArrayList<String> backupCodes = new ArrayList<>(BACKUP_COUNT);
        SharedPreferences.Editor editor = secure.edit().putString(SECRET, secret).remove(PENDING_SECRET);
        SecureRandom random = new SecureRandom();
        for (int i = 0; i < BACKUP_COUNT; i++) {
            byte[] bytes = new byte[5];
            random.nextBytes(bytes);
            String raw = Base64.encodeToString(bytes, Base64.NO_WRAP | Base64.NO_PADDING)
                    .replace("+", "A").replace("/", "B").toUpperCase(Locale.US);
            String display = raw.substring(0, Math.min(8, raw.length()));
            backupCodes.add(display);
            editor.putString(BACKUP_PREFIX + i, sha256(display));
        }
        editor.putBoolean(ENABLED, true).apply();
        markSessionVerified();
        return backupCodes;
    }

    public static boolean verifyStoredCode(Context context, String code) {
        try {
            SharedPreferences secure = prefs(context);
            if (!secure.getBoolean(ENABLED, false)) {
                return true;
            }
            String secret = secure.getString(SECRET, null);
            if (verifyCode(secret, code)) {
                markSessionVerified();
                return true;
            }
            if (code == null || code.length() < 6 || code.length() > 10) {
                return false;
            }
            String candidate = sha256(code.trim().toUpperCase(Locale.US));
            for (int i = 0; i < BACKUP_COUNT; i++) {
                String stored = secure.getString(BACKUP_PREFIX + i, null);
                if (stored != null && constantTimeEquals(stored, candidate)) {
                    secure.edit().remove(BACKUP_PREFIX + i).apply();
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
        SharedPreferences secure = prefs(context);
        SharedPreferences.Editor editor = secure.edit().clear();
        editor.apply();
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
