package org.telegram.messenger;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * MessageSchedulerWorker - Background worker for sending scheduled messages
 * Executes message sending at scheduled time using WorkManager
 */
public class MessageSchedulerWorker extends Worker {
    private static final String TAG = "MessageSchedulerWorker";

    public MessageSchedulerWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            long messageId = getInputData().getLong("message_id", -1);
            if (messageId == -1) {
                FileLog.e(TAG + ": Invalid or missing message_id input");
                return Result.failure();
            }

            MessageScheduler scheduler = MessageScheduler.getInstance(getApplicationContext());
            if (scheduler == null) {
                FileLog.e(TAG + ": MessageScheduler instance is null");
                return Result.retry();
            }

            MessageScheduler.ScheduledMessage message = scheduler.getScheduledMessage(messageId);
            if (message == null) {
                FileLog.e(TAG + ": Scheduled message " + messageId + " not found in database");
                return Result.failure();
            }

            scheduler.sendScheduledMessage(messageId);
            FileLog.d(TAG + ": Successfully processed scheduled message " + messageId);
            return Result.success();

        } catch (Exception e) {
            FileLog.e(TAG + ": Exception in doWork", e);
            return Result.retry();
        }
    }
}
