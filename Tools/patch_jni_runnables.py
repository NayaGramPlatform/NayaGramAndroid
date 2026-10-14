# -*- coding: utf-8 -*-
import pathlib
p = pathlib.Path("TMessagesProj/src/main/java/org/telegram/tgnet/ConnectionsManager.java")
t = p.read_text(encoding="utf-8")
if "nativeUpdateRunnables" in t:
    print("runnables already")
    raise SystemExit(0)
old = """    @Keep
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
"""
new = """    private static final Runnable[] nativeUpdateRunnables = new Runnable[UserConfig.MAX_ACCOUNT_COUNT];
    private static final Runnable[] nativeSessionRunnables = new Runnable[UserConfig.MAX_ACCOUNT_COUNT];
    static {
        for (int i = 0; i < UserConfig.MAX_ACCOUNT_COUNT; i++) {
            final int account = i;
            nativeUpdateRunnables[i] = () -> {
                try {
                    MessagesController controller = AccountInstance.getInstance(account).getMessagesController();
                    if (controller != null) {
                        controller.updateTimerProc();
                    }
                } catch (Throwable ignored) {
                }
            };
            nativeSessionRunnables[i] = () -> {
                try {
                    MessagesController controller = AccountInstance.getInstance(account).getMessagesController();
                    if (controller != null) {
                        controller.getDifference();
                    }
                } catch (Throwable ignored) {
                }
            };
        }
    }

    @Keep
    public static void onUpdate(final int currentAccount) {
        try {
            if (Utilities.stageQueue == null || currentAccount < 0 || currentAccount >= nativeUpdateRunnables.length) {
                return;
            }
            Utilities.stageQueue.postRunnable(nativeUpdateRunnables[currentAccount]);
        } catch (Throwable ignored) {
        }
    }

    @Keep
    public static void onSessionCreated(final int currentAccount) {
        try {
            if (Utilities.stageQueue == null || currentAccount < 0 || currentAccount >= nativeSessionRunnables.length) {
                return;
            }
            Utilities.stageQueue.postRunnable(nativeSessionRunnables[currentAccount]);
        } catch (Throwable ignored) {
        }
    }
"""
if old not in t:
    raise SystemExit("onUpdate block missing")
p.write_text(t.replace(old, new, 1), encoding="utf-8")
print("jni runnables installed")
