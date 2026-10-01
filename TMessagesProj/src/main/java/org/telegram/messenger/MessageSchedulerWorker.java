package org.telegram.messenger;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

/**
 * MessageSchedulerWorker - Background worker for sending scheduled messages
 * Executes message sending at scheduled time
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
            // Get message ID from input data
            long messageId = getInputData().getLong("message_id", -1);
            
            if (messageId == -1) {
                FileLog.e(TAG + ": Invalid message ID");
                return Result.failure();
            }
            
            // Send the scheduled message
            MessageScheduler scheduler = MessageScheduler.getInstance(getApplicationContext());
            scheduler.sendScheduledMessage(messageId);
            
            FileLog.d(TAG + ": Successfully sent scheduled message " + messageId);
            return Result.success();
            
        } catch (Exception e) {
            FileLog.e(TAG, e);
            // Retry on failure
            return Result.retry();
        }
    }
}
