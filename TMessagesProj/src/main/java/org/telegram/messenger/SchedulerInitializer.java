package org.telegram.messenger;

import android.app.Application;

import androidx.work.Configuration;
import androidx.work.WorkManager;

/**
 * SchedulerInitializer - Initialize Message Scheduler on app start
 * Handles WorkManager configuration and pending message recovery
 */
public class SchedulerInitializer {
    private static final String TAG = "SchedulerInitializer";

    /**
     * Initialize scheduler system on app startup
     * Call this from ApplicationLoader.onCreate()
     */
    public static void initializeScheduler(Application app) {
        try {
            // Configure WorkManager with custom configuration
            Configuration config = new Configuration.Builder()
                    .setMinimumLoggingLevel(android.util.Log.INFO)
                    .build();
            WorkManager.initialize(app, config);

            // Initialize scheduler database and manager
            MessageSchedulerDatabase.getInstance(app);
            AutoReplyManager.getInstance();
            MessageScheduler.getInstance(app);

            // Recover pending messages on app start
            recoverPendingMessages(app);

            FileLog.d(TAG + ": Scheduler system initialized successfully");
        } catch (Exception e) {
            FileLog.e(TAG + ": Failed to initialize scheduler", e);
        }
    }

    /**
     * Recover pending scheduled messages on app restart
     * Re-schedule any messages that were pending before app was killed
     */
    private static void recoverPendingMessages(Application app) {
        try {
            MessageScheduler scheduler = MessageScheduler.getInstance(app);
            if (scheduler == null) return;

            // Get accounts and recover pending messages for each
            int numAccounts = UserConfig.getActivatedAccountsCount();
            for (int i = 0; i < numAccounts; i++) {
                int accountId = UserConfig.getInstance(i).getClientUserId();
                java.util.List<MessageScheduler.ScheduledMessage> pendingMessages =
                        scheduler.getPendingMessages(i);

                if (pendingMessages != null && !pendingMessages.isEmpty()) {
                    FileLog.d(TAG + ": Recovering " + pendingMessages.size() +
                            " pending messages for account " + accountId);

                    for (MessageScheduler.ScheduledMessage message : pendingMessages) {
                        try {
                            scheduler.scheduleMessageSending(message.id, message.scheduleTime);
                            FileLog.d(TAG + ": Re-scheduled message " + message.id);
                        } catch (Exception e) {
                            FileLog.e(TAG + ": Failed to re-schedule message " + message.id, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            FileLog.e(TAG + ": Failed to recover pending messages", e);
        }
    }
}
