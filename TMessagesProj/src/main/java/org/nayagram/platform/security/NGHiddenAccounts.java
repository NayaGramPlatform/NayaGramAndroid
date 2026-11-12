package org.nayagram.platform.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.util.Base64;

import androidx.biometric.BiometricManager;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MediaDataController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.NotificationsController;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.UserConfig;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.ColoredImageSpan;
import org.telegram.ui.Components.ItemOptions;
import org.telegram.ui.LaunchActivity;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Set;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * NGHiddenAccounts - Passcode & biometric protected hidden accounts for NayaGram.
 * Stores all state locally on-device (PBKDF2WithHmacSHA1 + 16-byte SecureRandom salt).
 */
public final class NGHiddenAccounts {

    private static final String PREFS = "ng_hidden_accounts";
    private static final String KEY_HASH = "code_hash";
    private static final String KEY_SALT = "code_salt";
    private static final String KEY_TYPE = "code_type";
    private static final String KEY_IDS = "hidden_user_ids";
    private static final String KEY_LAST_VISIBLE = "last_visible_account";
    private static final String KEY_FINGERPRINT = "fingerprint";
    private static final String KEY_VIBRATE = "vibrate";
    private static final String KEY_HIDE_ON_BACKGROUND = "hide_on_background";
    private static final String KEY_KEPT_UNLOCKED = "kept_unlocked";

    private static final int ITERATIONS = 10000;
    private static final int KEY_BITS = 160;
    private static final long RELOCK_DELAY_MS = 30_000L;

    private static volatile boolean unlocked = false;
    private static volatile long unlockedAtMs = 0L;
    private static volatile boolean backgrounded = false;
    private static volatile Set<Long> cachedIds = null;

    private static final Runnable relockRunnable = NGHiddenAccounts::lock;

    private NGHiddenAccounts() {}

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static synchronized void init() {
        loadIds();
        if (!isHideOnBackground() && prefs().getBoolean(KEY_KEPT_UNLOCKED, false)) {
            unlocked = true;
            unlockedAtMs = SystemClock.elapsedRealtime();
        }
    }

    private static Set<Long> loadIds() {
        if (cachedIds != null) {
            return cachedIds;
        }
        Set<Long> set = new HashSet<>();
        try {
            Set<String> raw = prefs().getStringSet(KEY_IDS, null);
            if (raw != null) {
                for (String s : raw) {
                    try {
                        long v = Long.parseLong(s);
                        if (v != 0) set.add(v);
                    } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Throwable t) {
            FileLog.e(t);
        }
        cachedIds = set;
        return set;
    }

    private static synchronized void saveIds(Set<Long> updated) {
        cachedIds = new HashSet<>(updated);
        Set<String> strSet = new HashSet<>();
        for (Long id : updated) {
            if (id != null && id != 0) {
                strSet.add(String.valueOf(id));
            }
        }
        prefs().edit().putStringSet(KEY_IDS, strSet).apply();
    }

    // ---- Queries ----

    public static boolean isUnlocked() {
        return unlocked;
    }

    public static boolean isHidden(int account) {
        if (!UserConfig.isValidAccount(account) || !UserConfig.getInstance(account).isClientActivated()) {
            return false;
        }
        long userId = UserConfig.getInstance(account).getClientUserId();
        if (userId == 0) return false;
        return loadIds().contains(userId);
    }

    public static boolean isConcealed(int account) {
        return isHidden(account) && !unlocked;
    }

    public static boolean isListed(int account) {
        return UserConfig.isValidAccount(account)
                && UserConfig.getInstance(account).isClientActivated()
                && (!isHidden(account) || account == UserConfig.selectedAccount);
    }

    public static int listedCount() {
        int count = 0;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (isListed(a)) count++;
        }
        return count;
    }

    public static boolean hasHidden() {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (isHidden(a)) return true;
        }
        return false;
    }

    /**
     * Requires at least one other activated account to remain visible while hidden accounts are locked.
     */
    public static boolean canHide(int account) {
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (a != account && UserConfig.getInstance(a).isClientActivated() && !isHidden(a)) {
                return true;
            }
        }
        return false;
    }

    public static void setHidden(int account, boolean hidden) {
        if (!UserConfig.isValidAccount(account) || !UserConfig.getInstance(account).isClientActivated()) {
            return;
        }
        long userId = UserConfig.getInstance(account).getClientUserId();
        if (userId == 0) return;

        Set<Long> updated = new HashSet<>(loadIds());
        if (hidden) {
            updated.add(userId);
        } else {
            updated.remove(userId);
        }
        saveIds(updated);
        onHiddenChanged(account, hidden);
        if (hidden && !unlocked) {
            leaveConcealedAccount();
        }
    }

    private static void onHiddenChanged(int account, boolean hidden) {
        try {
            NotificationsController.getInstance(account).onHiddenChanged(hidden);
        } catch (Throwable ignored) {}
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.nayagramHiddenAccountsChanged);
        int vis = visibleAccount();
        if (vis >= 0) {
            try {
                MediaDataController.getInstance(vis).buildShortcuts();
            } catch (Throwable ignored) {}
        }
    }

    public static void addHideOption(ItemOptions o, BaseFragment fragment, int account) {
        if (o == null || fragment == null) return;
        if (isHidden(account)) {
            o.add(R.drawable.msg_view_file, "Show account", () -> {
                setHidden(account, false);
                BulletinFactory.of(fragment)
                        .createSimpleBulletin(R.raw.chats_infotip, "Account is now visible in your main list.")
                        .show();
            });
        } else {
            o.add(R.drawable.msg_archive_hide, "Hide account", () -> requestHide(fragment, account));
        }
    }

    public static void requestHide(BaseFragment fragment, int account) {
        if (fragment == null) return;
        if (!canHide(account)) {
            BulletinFactory.of(fragment)
                    .createSimpleBulletin(R.raw.chats_infotip, "Keep at least one visible account to hide another.")
                    .show();
            return;
        }
        if (!hasCode()) {
            fragment.presentFragment(new NGHiddenAccountsActivity(account));
            return;
        }
        boolean leaving = (account == UserConfig.selectedAccount && !unlocked);
        setHidden(account, true);
        if (!leaving) {
            BulletinFactory.of(fragment)
                    .createSimpleBulletin(R.raw.chats_infotip, "Account hidden. Long-press Add Account and enter passcode to open it.")
                    .show();
        }
    }

    public static CharSequence withHiddenMark(int account, CharSequence name) {
        if (name == null || !isHidden(account)) return name;
        SpannableStringBuilder sb = new SpannableStringBuilder(name).append("  h");
        ColoredImageSpan span = new ColoredImageSpan(R.drawable.msg_archive_hide);
        span.setSize(AndroidUtilities.dp(15f));
        sb.setSpan(span, sb.length() - 1, sb.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        return sb;
    }

    // ---- Lock & Lifecycle ----

    public static void unlock() {
        AndroidUtilities.cancelRunOnUIThread(relockRunnable);
        unlocked = true;
        unlockedAtMs = SystemClock.elapsedRealtime();
        if (!isHideOnBackground()) {
            prefs().edit().putBoolean(KEY_KEPT_UNLOCKED, true).apply();
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.nayagramHiddenAccountsChanged);
    }

    public static void lock() {
        AndroidUtilities.cancelRunOnUIThread(relockRunnable);
        if (!unlocked) return;
        unlocked = false;
        prefs().edit().remove(KEY_KEPT_UNLOCKED).apply();
        leaveConcealedAccount();
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.nayagramHiddenAccountsChanged);
    }

    public static void onAppPaused() {
        if (!unlocked || !isHideOnBackground()) return;
        backgrounded = true;
        AndroidUtilities.cancelRunOnUIThread(relockRunnable);
        AndroidUtilities.runOnUIThread(relockRunnable, RELOCK_DELAY_MS);
    }

    public static void onAppResumed() {
        AndroidUtilities.cancelRunOnUIThread(relockRunnable);
        if (!unlocked || !backgrounded) return;
        backgrounded = false;
        if (isHideOnBackground() && (SystemClock.elapsedRealtime() - unlockedAtMs) >= RELOCK_DELAY_MS) {
            lock();
        }
    }

    public static void onScreenOff() {
        if (unlocked && isHideOnBackground()) {
            lock();
        }
    }

    public static void ensureSelectedVisible() {
        if (!isConcealed(UserConfig.selectedAccount)) return;
        int target = visibleAccountOrFreeSlot();
        if (target >= 0 && target != UserConfig.selectedAccount) {
            UserConfig.selectedAccount = target;
            UserConfig.getInstance(0).saveConfig(false);
        }
    }

    public static void leaveConcealedAccount() {
        if (!isConcealed(UserConfig.selectedAccount)) return;
        int target = visibleAccountOrFreeSlot();
        if (target >= 0 && LaunchActivity.instance != null && target != UserConfig.selectedAccount) {
            LaunchActivity.instance.switchToAccount(target, true);
        }
    }

    public static void onAccountSelected(int account) {
        if (!isHidden(account)) {
            prefs().edit().putInt(KEY_LAST_VISIBLE, account).apply();
            if (hasHidden()) {
                try {
                    MediaDataController.getInstance(account).buildShortcuts();
                } catch (Throwable ignored) {}
            }
        }
    }

    public static void onLoggedOut(long userId) {
        if (userId == 0) return;
        Set<Long> updated = new HashSet<>(loadIds());
        if (updated.remove(userId)) {
            saveIds(updated);
            NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.nayagramHiddenAccountsChanged);
        }
    }

    public static int visibleAccount() {
        int last = prefs().getInt(KEY_LAST_VISIBLE, -1);
        if (UserConfig.isValidAccount(last) && UserConfig.getInstance(last).isClientActivated() && !isHidden(last)) {
            return last;
        }
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (UserConfig.getInstance(a).isClientActivated() && !isHidden(a)) {
                return a;
            }
        }
        return -1;
    }

    public static int visibleAccountOrFreeSlot() {
        int vis = visibleAccount();
        if (vis >= 0) return vis;
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (!UserConfig.getInstance(a).isClientActivated()) {
                return a;
            }
        }
        return -1;
    }

    public static void disable() {
        AndroidUtilities.cancelRunOnUIThread(relockRunnable);
        unlocked = false;
        prefs().edit()
                .remove(KEY_HASH)
                .remove(KEY_SALT)
                .remove(KEY_TYPE)
                .remove(KEY_FINGERPRINT)
                .remove(KEY_VIBRATE)
                .remove(KEY_HIDE_ON_BACKGROUND)
                .remove(KEY_KEPT_UNLOCKED)
                .apply();
        Set<Integer> wereHidden = new HashSet<>();
        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
            if (isHidden(a)) wereHidden.add(a);
        }
        saveIds(new HashSet<>());
        for (int a : wereHidden) {
            onHiddenChanged(a, false);
        }
        NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.nayagramHiddenAccountsChanged);
    }

    // ---- Preferences & Passcode ----

    public static boolean hasCode() {
        return prefs().contains(KEY_HASH);
    }

    public static int getCodeType() {
        return prefs().getInt(KEY_TYPE, SharedConfig.PASSCODE_TYPE_PIN);
    }

    public static boolean isFingerprintEnabled() {
        return prefs().getBoolean(KEY_FINGERPRINT, false);
    }

    public static void setFingerprintEnabled(boolean value) {
        prefs().edit().putBoolean(KEY_FINGERPRINT, value).apply();
    }

    public static boolean isVibrateEnabled() {
        return prefs().getBoolean(KEY_VIBRATE, true);
    }

    public static void setVibrateEnabled(boolean value) {
        prefs().edit().putBoolean(KEY_VIBRATE, value).apply();
    }

    public static boolean isHideOnBackground() {
        return prefs().getBoolean(KEY_HIDE_ON_BACKGROUND, true);
    }

    public static void setHideOnBackground(boolean value) {
        SharedPreferences.Editor ed = prefs().edit().putBoolean(KEY_HIDE_ON_BACKGROUND, value);
        if (value) {
            ed.remove(KEY_KEPT_UNLOCKED);
        } else if (unlocked) {
            ed.putBoolean(KEY_KEPT_UNLOCKED, true);
        }
        ed.apply();
    }

    public static boolean isFingerprintHardwareAvailable() {
        try {
            Context ctx = ApplicationLoader.applicationContext;
            if (ctx == null) return false;
            return BiometricManager.from(ctx).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
                    == BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable t) {
            return false;
        }
    }

    public static boolean checkCode(String input) {
        if (input == null) return false;
        SharedPreferences p = prefs();
        String hash = p.getString(KEY_HASH, null);
        String salt = p.getString(KEY_SALT, null);
        if (hash == null || salt == null) return false;
        try {
            byte[] expected = Base64.decode(hash, Base64.NO_WRAP);
            byte[] saltBytes = Base64.decode(salt, Base64.NO_WRAP);
            byte[] actual = derive(input, saltBytes);
            return MessageDigest.isEqual(expected, actual);
        } catch (Throwable e) {
            FileLog.e(e);
            return false;
        }
    }

    public static void saveCode(String code, int type) {
        if (code == null || code.isEmpty()) return;
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        byte[] derived = derive(code, salt);
        prefs().edit()
                .putString(KEY_HASH, Base64.encodeToString(derived, Base64.NO_WRAP))
                .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
                .putInt(KEY_TYPE, type)
                .apply();
        unlock();
    }

    private static byte[] derive(String code, byte[] salt) {
        PBEKeySpec spec = new PBEKeySpec(code.toCharArray(), salt, ITERATIONS, KEY_BITS);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new RuntimeException("Failed to derive passcode hash", e);
        } finally {
            spec.clearPassword();
        }
    }

    // ---- Entry points ----

    public static boolean askCode(BaseFragment fragment, Runnable onAccepted) {
        if (!hasCode() || fragment == null || fragment.getParentActivity() == null) {
            return false;
        }
        NGHiddenAccountsPasscodeDialog dialog = new NGHiddenAccountsPasscodeDialog(
                fragment.getParentActivity(),
                NGHiddenAccountsPasscodeDialog.MODE_UNLOCK,
                onAccepted
        );
        dialog.show();
        return true;
    }

    public static void openSetOrChangeCode(BaseFragment fragment, Runnable onSaved) {
        if (fragment == null || fragment.getParentActivity() == null) return;
        int mode = hasCode()
                ? NGHiddenAccountsPasscodeDialog.MODE_CHANGE
                : NGHiddenAccountsPasscodeDialog.MODE_CREATE;
        NGHiddenAccountsPasscodeDialog dialog = new NGHiddenAccountsPasscodeDialog(
                fragment.getParentActivity(),
                mode,
                onSaved
        );
        dialog.show();
    }

    public static boolean openFromLogin(BaseFragment fragment, int account) {
        if (!isConcealed(account) || fragment == null) return false;
        return askCode(fragment, () -> {
            unlock();
            if (fragment.getParentActivity() instanceof LaunchActivity) {
                ((LaunchActivity) fragment.getParentActivity()).switchToAccount(account, true);
            }
            fragment.finishFragment();
        });
    }

    public static boolean askUnlock(BaseFragment fragment) {
        if (!hasCode() || fragment == null) return false;
        return askCode(fragment, () -> {
            unlock();
            fragment.presentFragment(new NGHiddenAccountsListActivity(), false, true);
        });
    }
}
