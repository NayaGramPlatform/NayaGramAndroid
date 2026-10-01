package org.telegram.messenger;

import android.app.Application;
import androidx.work.Configuration;
import androidx.work.WorkManager;
import java.util.List;

/**
 * SchedulerInitializer - Initialize Message Scheduler on app start
 * Handles WorkManager configuration and pending message recovery
 * Fully compatible with Java 8 and Android 21+
 */
public class SchedulerInitializer {
    private static final String TAG = "SchedulerInitializer";
    private static volatile boolean isInitialized = false;

    /**
     * Initialize scheduler system on app startup
     * Call this from ApplicationLoader.onCreate()
     */
    public static synchronized void initializeScheduler(Application app) {
        if (isInitialized) {
            return;
        }
        if (app == null) {
            FileLog.e(TAG + ": Application instance is null, cannot initialize scheduler");
            return;
        }

        try {
            // 1. Configure WorkManager safely (handle already-initialized cases gracefully)
            try {
                Configuration config = new Configuration.Builder()
                        .setMinimumLoggingLevel(android.util.Log.INFO)
                        .build();
                WorkManager.initialize(app, config);
                FileLog.d(TAG + ": WorkManager initialized with custom configuration");
            } catch (IllegalStateException e) {
                FileLog.d(TAG + ": WorkManager was already initialized: " + e.getMessage());
            } catch (Exception e) {
                FileLog.e(TAG + ": WorkManager initialization warning", e);
            }

            // 2. Initialize scheduler database, AutoReplyManager, and MessageScheduler
            MessageSchedulerDatabase.getInstance(app);
            AutoReplyManager.getInstance();
            MessageScheduler.getInstance(app);

            // 3. Mark as initialized
            isInitialized = true;

            // 4. Recover pending scheduled messages on app start
            recoverPendingMessages(app);

            FileLog.d(TAG + ": Scheduler system initialized successfully");
        } catch (Exception e) {
            FileLog.e(TAG + ": Failed to initialize scheduler system", e);
        }
    }

    /**
     * Recover pending scheduled messages on app restart or process death
     * Re-schedule any messages that were pending before app was killed
     */
    public static void recoverPendingMessages(Application app) {
        if (app == null) return;
        try {
            MessageScheduler scheduler = MessageScheduler.getInstance(app);
            if (scheduler == null) {
                FileLog.e(TAG + ": MessageScheduler instance is null during recovery");
                return;
            }

            int numAccounts = Math.max(1, UserConfig.getActivatedAccountsCount());

            for (int i = 0; i < numAccounts; i++) {
                try {
                    List<MessageScheduler.ScheduledMessage> pendingMessages = scheduler.getPendingMessages(i);
                    if (pendingMessages == null || pendingMessages.isEmpty()) {
                        FileLog.d(TAG + ": No pending messages to recover for account index " + i);
                        continue;
                    }

                    FileLog.d(TAG + ": Recovering " + pendingMessages.size() + " pending messages for account index " + i);
                    for (MessageScheduler.ScheduledMessage message : pendingMessages) {
                        if (message == null) {
                            continue;
                        }
                        if (message.status != MessageScheduler.ScheduledMessage.STATUS_PENDING) {
                            continue;
                        }

                        try {
                            scheduler.scheduleMessageSending(message.id, message.scheduleTime);
                            FileLog.d(TAG + ": Re-scheduled message #" + message.id);
                        } catch (Exception e) {
                            FileLog.e(TAG + ": Failed to re-schedule message #" + message.id, e);
                        }
                    }
                } catch (Exception e) {
                    FileLog.e(TAG + ": Error recovering messages for account index " + i, e);
                }
            }
        } catch (Exception e) {
            FileLog.e(TAG + ": Failed to recover pending messages on startup", e);
        }
    }
}
