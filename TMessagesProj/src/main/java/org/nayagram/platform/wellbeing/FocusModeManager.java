package org.nayagram.platform.wellbeing;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * FocusModeManager - Digital Wellbeing & Quiet Hours for NayaGram
 * 100% Compliant with Google Play Store Policies.
 * Allows users to mute distractions during study, work, or prayer times while
 * preserving VIP contacts and critical communications.
 */
public final class FocusModeManager {

    private static final String PREFS_NAME = "nayagram_focus_mode_prefs";
    private static final String KEY_FOCUS_ENABLED = "focus_enabled";
    private static final String KEY_FOCUS_UNTIL = "focus_until_timestamp";
    private static final String KEY_SCHEDULE_ENABLED = "schedule_enabled";
    private static final String KEY_SCHEDULE_START_MIN = "schedule_start_minute"; // 0 - 1439
    private static final String KEY_SCHEDULE_END_MIN = "schedule_end_minute";     // 0 - 1439
    private static final String KEY_VIP_DIALOGS = "vip_dialog_ids";

    private static volatile FocusModeManager sInstance;
    private final SharedPreferences preferences;
    private final AtomicBoolean isFocusActive = new AtomicBoolean(false);

    private FocusModeManager(Context context) {
        this.preferences = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        syncState();
    }

    public static FocusModeManager getInstance(Context context) {
        if (sInstance == null) {
            synchronized (FocusModeManager.class) {
                if (sInstance == null) {
                    sInstance = new FocusModeManager(context);
                }
            }
        }
        return sInstance;
    }

    /**
     * Synchronize and evaluate active focus state based on manual timer and scheduled hours.
     */
    public synchronized void syncState() {
        boolean manualEnabled = preferences.getBoolean(KEY_FOCUS_ENABLED, false);
        long until = preferences.getLong(KEY_FOCUS_UNTIL, 0);
        long now = System.currentTimeMillis();

        if (manualEnabled && until > 0 && now >= until) {
            // Timer expired
            preferences.edit()
                    .putBoolean(KEY_FOCUS_ENABLED, false)
                    .putLong(KEY_FOCUS_UNTIL, 0)
                    .apply();
            manualEnabled = false;
        }

        boolean scheduledActive = false;
        if (preferences.getBoolean(KEY_SCHEDULE_ENABLED, false)) {
            scheduledActive = isCurrentlyInSchedule();
        }

        isFocusActive.set(manualEnabled || scheduledActive);
    }

    public boolean isFocusModeActive() {
        syncState();
        return isFocusActive.get();
    }

    /**
     * Start Focus Mode for a specific duration in minutes.
     * @param durationMinutes e.g., 30, 60, 120 (or -1 for indefinite until turned off)
     */
    public synchronized void startFocusMode(int durationMinutes) {
        long until = durationMinutes > 0 ? System.currentTimeMillis() + (durationMinutes * 60L * 1000L) : -1;
        preferences.edit()
                .putBoolean(KEY_FOCUS_ENABLED, true)
                .putLong(KEY_FOCUS_UNTIL, until)
                .apply();
        isFocusActive.set(true);
    }

    /**
     * Stop Focus Mode immediately.
     */
    public synchronized void stopFocusMode() {
        preferences.edit()
                .putBoolean(KEY_FOCUS_ENABLED, false)
                .putLong(KEY_FOCUS_UNTIL, 0)
                .apply();
        isFocusActive.set(false);
    }

    /**
     * Check if an incoming notification should be suppressed during Focus Mode.
     * @param dialogId Telegram dialog/chat ID
     * @param isDirectUserChat True if 1-on-1 private chat
     * @param isMention True if user is directly mentioned
     * @return true to suppress notification, false to allow ringing
     */
    public boolean shouldSuppressNotification(long dialogId, boolean isDirectUserChat, boolean isMention) {
        if (!isFocusModeActive()) {
            return false;
        }

        // Check if sender is a VIP contact (Family, urgent work)
        if (isVipContact(dialogId)) {
            return false;
        }

        // Direct mentions can be allowed optionally or suppressed
        // Standard policy: Group messages and channel broadcasts are suppressed
        return true;
    }

    public synchronized void addVipContact(long dialogId) {
        Set<String> set = new HashSet<>(preferences.getStringSet(KEY_VIP_DIALOGS, new HashSet<>()));
        set.add(String.valueOf(dialogId));
        preferences.edit().putStringSet(KEY_VIP_DIALOGS, set).apply();
    }

    public synchronized void removeVipContact(long dialogId) {
        Set<String> set = new HashSet<>(preferences.getStringSet(KEY_VIP_DIALOGS, new HashSet<>()));
        set.remove(String.valueOf(dialogId));
        preferences.edit().putStringSet(KEY_VIP_DIALOGS, set).apply();
    }

    public boolean isVipContact(long dialogId) {
        Set<String> set = preferences.getStringSet(KEY_VIP_DIALOGS, null);
        return set != null && set.contains(String.valueOf(dialogId));
    }

    public void setSchedule(boolean enabled, int startHour, int startMinute, int endHour, int endMinute) {
        int startTotal = (startHour * 60) + startMinute;
        int endTotal = (endHour * 60) + endMinute;
        preferences.edit()
                .putBoolean(KEY_SCHEDULE_ENABLED, enabled)
                .putInt(KEY_SCHEDULE_START_MIN, startTotal)
                .putInt(KEY_SCHEDULE_END_MIN, endTotal)
                .apply();
        syncState();
    }

    private boolean isCurrentlyInSchedule() {
        int startMin = preferences.getInt(KEY_SCHEDULE_START_MIN, -1);
        int endMin = preferences.getInt(KEY_SCHEDULE_END_MIN, -1);
        if (startMin < 0 || endMin < 0) return false;

        Calendar now = Calendar.getInstance();
        int currentMin = (now.get(Calendar.HOUR_OF_DAY) * 60) + now.get(Calendar.MINUTE);

        if (startMin <= endMin) {
            return currentMin >= startMin && currentMin < endMin;
        } else {
            // Over midnight (e.g. 23:00 to 06:00)
            return currentMin >= startMin || currentMin < endMin;
        }
    }
}
