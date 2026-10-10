package org.nayagram.platform.diagnostics;

import android.util.Log;

public class VoIPCrashDiagnostics {

    private static final String TAG = "NGBot_VoIPDiag";

    public static void logCallState(String stage, long peerId, boolean isVideo) {
        String safeId = peerId == 0 ? "none" : String.valueOf(peerId).substring(0, Math.min(3, String.valueOf(peerId).length())) + "***";
        Log.i(TAG, "Call State Transition: stage=" + stage + ", peer=" + safeId + ", isVideo=" + isVideo);
    }

    public static void logNativeFailure(String subSystem, String errorDetail) {
        String safeDetail = errorDetail != null ? errorDetail.replaceAll("([0-9a-zA-Z_-]{20,})", "[REDACTED]") : "unknown";
        Log.e(TAG, "VoIP Native Engine Warning/Failure: subsystem=" + subSystem + ", detail=" + safeDetail);
    }
}
