package org.nayagram.platform.security;

import android.content.Context;
import java.util.List;

/**
 * @deprecated Renamed to {@link NGSafety}. Preserved for backward compatibility.
 */
@Deprecated
public final class NayaGramGuard {
    private NayaGramGuard() {}

    public static boolean isEnabled(Context context) {
        return NGSafety.isEnabled(context);
    }

    public static boolean isSessionVerified() {
        return NGSafety.isSessionVerified();
    }

    public static void markSessionVerified() {
        NGSafety.markSessionVerified();
    }

    public static void markSessionVerified(Context context) {
        NGSafety.markSessionVerified(context);
    }

    public static void lockSession() {
        NGSafety.lockSession();
    }

    public static void lockSession(Context context) {
        NGSafety.lockSession(context);
    }

    public static String beginSetup(Context context) throws Exception {
        return NGSafety.beginSetup(context);
    }

    public static void cancelSetup(Context context) {
        NGSafety.cancelSetup(context);
    }

    public static String getPendingSecret(Context context) throws Exception {
        return NGSafety.getPendingSecret(context);
    }

    public static String getOtpAuthUri(String accountLabel, String secret) {
        return NGSafety.getOtpAuthUri(accountLabel, secret);
    }

    public static boolean verifyCode(String secret, String code) {
        return NGSafety.verifyCode(secret, code);
    }

    public static List<String> confirmSetup(Context context, String code) throws Exception {
        return NGSafety.confirmSetup(context, code);
    }

    public static boolean verifyStoredCode(Context context, String code) {
        return NGSafety.verifyStoredCode(context, code);
    }

    public static void disable(Context context) throws Exception {
        NGSafety.disable(context);
    }
}
