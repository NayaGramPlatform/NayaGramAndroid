# -*- coding: utf-8 -*-
import pathlib

def patch_gradle():
    p = pathlib.Path("gradle.properties")
    t = p.read_text(encoding="utf-8")
    old = "android.enableR8.fullMode=true"
    new = "android.enableR8.fullMode=false"
    if old in t:
        p.write_text(t.replace(old, new, 1), encoding="utf-8")
        print("r8 full mode off")
    elif new in t:
        print("r8 already off")
    else:
        raise SystemExit("r8 flag missing")

def patch_proguard():
    p = pathlib.Path("TMessagesProj/proguard-rules.pro")
    t = p.read_text(encoding="utf-8")
    line = "-keep class org.telegram.tgnet.ConnectionsManager { *; }\n"
    extra = "-keep class org.telegram.tgnet.ConnectionsManager$* { *; }\n"
    if "ConnectionsManager$*" in t:
        print("proguard already")
        return
    if line not in t:
        raise SystemExit("proguard keep missing")
    p.write_text(t.replace(line, line + extra, 1), encoding="utf-8")
    print("proguard inner keep")

def patch_cm():
    p = pathlib.Path("TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java")
    t = p.read_text(encoding="utf-8")
    if "Native libtmessages calls this off the Java stack" in t:
        print("connections already")
        return
    old = """    public static void onUpdate(final int currentAccount) {
        Utilities.stageQueue.postRunnable(() -> AccountInstance.getInstance(currentAccount).getMessagesController().updateTimerProc());
    }

    public static void onSessionCreated(final int currentAccount) {
        Utilities.stageQueue.postRunnable(() -> AccountInstance.getInstance(currentAccount).getMessagesController().getDifference());
    }

    public static void onConnectionStateChanged(final int state, final int currentAccount) {
        AndroidUtilities.runOnUIThread(() -> {
            getInstance(currentAccount).connectionState = state;
            AccountInstance.getInstance(currentAccount).getNotificationCenter().postNotificationName(NotificationCenter.didUpdateConnectionState);
        });
    }

    public static void onLogout(final int currentAccount) {
        AndroidUtilities.runOnUIThread(() -> {
            AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
            if (accountInstance.getUserConfig().getClientUserId() != 0) {
                accountInstance.getUserConfig().clearConfig();
                accountInstance.getMessagesController().performLogout(0);
            }
        });
    }
"""
    new = """    @Keep
    public static void onUpdate(final int currentAccount) {
        // Android 11: an exception leaving this JNI callback SIGSEGVs (top_shadow_frame_ == null).
        try {
            if (Utilities.stageQueue == null || currentAccount < 0) {
                return;
            }
            Utilities.stageQueue.postRunnable(() -> {
                try {
                    MessagesController controller = AccountInstance.getInstance(currentAccount).getMessagesController();
                    if (controller != null) {
                        controller.updateTimerProc();
                    }
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    @Keep
    public static void onSessionCreated(final int currentAccount) {
        try {
            if (Utilities.stageQueue == null || currentAccount < 0) {
                return;
            }
            Utilities.stageQueue.postRunnable(() -> {
                try {
                    MessagesController controller = AccountInstance.getInstance(currentAccount).getMessagesController();
                    if (controller != null) {
                        controller.getDifference();
                    }
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    @Keep
    public static void onConnectionStateChanged(final int state, final int currentAccount) {
        try {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    ConnectionsManager manager = getInstance(currentAccount);
                    if (manager == null) {
                        return;
                    }
                    manager.connectionState = state;
                    AccountInstance.getInstance(currentAccount).getNotificationCenter().postNotificationName(NotificationCenter.didUpdateConnectionState);
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }

    @Keep
    public static void onLogout(final int currentAccount) {
        try {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    AccountInstance accountInstance = AccountInstance.getInstance(currentAccount);
                    if (accountInstance != null && accountInstance.getUserConfig().getClientUserId() != 0) {
                        accountInstance.getUserConfig().clearConfig();
                        accountInstance.getMessagesController().performLogout(0);
                    }
                } catch (Throwable ignored) {
                }
            });
        } catch (Throwable ignored) {
        }
    }
"""
    if old not in t:
        raise SystemExit("onUpdate block missing")
    t = t.replace(old, new, 1)
    old2 = """    public static void onInternalPushReceived(final int currentAccount) {
        KeepAliveJob.startJob();
    }
"""
    new2 = """    @Keep
    public static void onInternalPushReceived(final int currentAccount) {
        try {
            KeepAliveJob.startJob();
        } catch (Throwable ignored) {
        }
    }
"""
    if old2 not in t:
        raise SystemExit("onInternalPushReceived missing")
    t = t.replace(old2, new2, 1)
    p.write_text(t, encoding="utf-8")
    print("connections guarded")

if __name__ == "__main__":
    patch_gradle()
    patch_proguard()
    patch_cm()
    print("done")
