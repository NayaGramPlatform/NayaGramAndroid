package org.telegram.messenger;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import androidx.work.BackoffPolicy;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import org.telegram.tgnet.TLRPC;

import java.util.concurrent.TimeUnit;

/**
 * MessageScheduler - Handles scheduling and sending messages at specified times
 * Features:
 * - Schedule message for specific date/time
 * - Recurring scheduled messages
 * - Background job execution using WorkManager
 * - Notification for scheduled messages
 */
public class MessageScheduler {
    
    private static MessageScheduler instance;
    private Context context;
    private MessageSchedulerDatabase database;
    private AlarmManager alarmManager;
    private static final String TAG = "MessageScheduler";
    
    private MessageScheduler(Context context) {
        this.context = context;
        this.database = MessageSchedulerDatabase.getInstance(context);
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }
    
    /**
     * Get singleton instance
     */
    public static synchronized MessageScheduler getInstance(Context context) {
        if (instance == null) {
            instance = new MessageScheduler(context);
        }
        return instance;
    }
    
    /**
     * Schedule a message to be sent at specified time
     * 
     * @param dialogId The dialog ID to send message to
     * @param messageText The message text
     * @param scheduleTime Unix timestamp when to send
     * @param accountId The account ID
     * @return Scheduled message ID
     */
    public long scheduleMessage(long dialogId, String messageText, long scheduleTime, int accountId) {
        // Create scheduled message object
        ScheduledMessage scheduledMessage = new ScheduledMessage();
        scheduledMessage.dialogId = dialogId;
        scheduledMessage.messageText = messageText;
        scheduledMessage.scheduleTime = scheduleTime;
        scheduledMessage.accountId = accountId;
        scheduledMessage.createdAt = System.currentTimeMillis() / 1000;
        scheduledMessage.status = ScheduledMessage.STATUS_PENDING;
        
        // Save to database
        long messageId = database.insertScheduledMessage(scheduledMessage);
        
        // Schedule work for sending
        scheduleMessageSending(messageId, scheduleTime);
        
        FileLog.d(TAG + ": Scheduled message " + messageId + " for time " + scheduleTime);
        return messageId;
    }
    
    /**
     * Schedule message sending using WorkManager
     */
    private void scheduleMessageSending(long messageId, long scheduleTime) {
        long delayMillis = (scheduleTime * 1000) - System.currentTimeMillis();
        
        if (delayMillis <= 0) {
            // Send immediately if time has passed
            sendScheduledMessage(messageId);
            return;
        }
        
        // Create work request
        Data inputData = new Data.Builder()
                .putLong("message_id", messageId)
                .build();
        
        OneTimeWorkRequest workRequest = new OneTimeWorkRequest.Builder(MessageSchedulerWorker.class)
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setBackoffCriteria(
                        BackoffPolicy.LINEAR,
                        OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                        TimeUnit.MILLISECONDS
                )
                .setConstraints(new Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build())
                .setInputData(inputData)
                .build();
        
        WorkManager.getInstance(context).enqueueUniqueWork(
                "schedule_message_" + messageId,
                androidx.work.ExistingWorkPolicy.KEEP,
                workRequest
        );
    }
    
    /**
     * Send scheduled message immediately
     */
    public void sendScheduledMessage(long messageId) {
        ScheduledMessage message = database.getScheduledMessage(messageId);
        if (message == null) {
            return;
        }
        
        try {
            MessagesController controller = MessagesController.getInstance(message.accountId);
            
            // Create send message params
            SendMessagesHelper.SendMessageParams params = SendMessagesHelper.SendMessageParams.of(
                    message.messageText,
                    message.dialogId,
                    null,
                    null,
                    null,
                    true,
                    null,
                    null,
                    null,
                    true,
                    0,
                    0,
                    null,
                    false
            );
            
            SendMessagesHelper.getInstance(message.accountId).sendMessage(params);
            
            // Update status to sent
            message.status = ScheduledMessage.STATUS_SENT;
            message.sentAt = System.currentTimeMillis() / 1000;
            database.updateScheduledMessage(message);
            
            FileLog.d(TAG + ": Sent scheduled message " + messageId);
        } catch (Exception e) {
            FileLog.e(TAG, e);
            message.status = ScheduledMessage.STATUS_FAILED;
            database.updateScheduledMessage(message);
        }
    }
    
    /**
     * Cancel scheduled message
     */
    public void cancelScheduledMessage(long messageId) {
        ScheduledMessage message = database.getScheduledMessage(messageId);
        if (message == null) {
            return;
        }
        
        // Cancel work
        WorkManager.getInstance(context).cancelUniqueWork("schedule_message_" + messageId);
        
        // Update status
        message.status = ScheduledMessage.STATUS_CANCELLED;
        database.updateScheduledMessage(message);
        
        FileLog.d(TAG + ": Cancelled scheduled message " + messageId);
    }
    
    /**
     * Get all pending scheduled messages
     */
    public java.util.List<ScheduledMessage> getPendingMessages(int accountId) {
        return database.getPendingMessages(accountId);
    }
    
    /**
     * Get scheduled message by ID
     */
    public ScheduledMessage getScheduledMessage(long messageId) {
        return database.getScheduledMessage(messageId);
    }
    
    /**
     * Delete scheduled message
     */
    public void deleteScheduledMessage(long messageId) {
        database.deleteScheduledMessage(messageId);
        WorkManager.getInstance(context).cancelUniqueWork("schedule_message_" + messageId);
    }
    
    /**
     * Reschedule message to new time
     */
    public void rescheduleMessage(long messageId, long newScheduleTime) {
        ScheduledMessage message = database.getScheduledMessage(messageId);
        if (message == null) {
            return;
        }
        
        // Cancel old work
        WorkManager.getInstance(context).cancelUniqueWork("schedule_message_" + messageId);
        
        // Update time and reschedule
        message.scheduleTime = newScheduleTime;
        message.status = ScheduledMessage.STATUS_PENDING;
        database.updateScheduledMessage(message);
        
        scheduleMessageSending(messageId, newScheduleTime);
        
        FileLog.d(TAG + ": Rescheduled message " + messageId + " to time " + newScheduleTime);
    }
    
    /**
     * Get scheduled messages count
     */
    public int getScheduledMessagesCount(int accountId) {
        return database.getScheduledMessagesCount(accountId);
    }
    
    /**
     * Scheduled message data class
     */
    public static class ScheduledMessage {
        public static final int STATUS_PENDING = 0;
        public static final int STATUS_SENT = 1;
        public static final int STATUS_FAILED = 2;
        public static final int STATUS_CANCELLED = 3;
        
        public long id;
        public long dialogId;
        public String messageText;
        public long scheduleTime;
        public int accountId;
        public int status;
        public long createdAt;
        public long sentAt;
        
        @Override
        public String toString() {
            return "ScheduledMessage{" +
                    "id=" + id +
                    ", dialogId=" + dialogId +
                    ", messageText='" + messageText + '\'' +
                    ", scheduleTime=" + scheduleTime +
                    ", accountId=" + accountId +
                    ", status=" + status +
                    ", createdAt=" + createdAt +
                    ", sentAt=" + sentAt +
                    '}';
        }
    }
}
