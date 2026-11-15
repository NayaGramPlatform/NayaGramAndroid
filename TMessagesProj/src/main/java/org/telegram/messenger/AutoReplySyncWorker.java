package org.telegram.messenger;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import org.telegram.tgnet.ConnectionsManager;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Periodic background watchdog for Auto Answer.
 * Ensures that even if OEM battery savers freeze the app or network drops for hours,
 * WorkManager wakes NayaGram when connected, calls getDifference(), and dispatches pending auto-replies.
 */
public class AutoReplySyncWorker extends Worker {

    private static final String WORK_NAME = "ng_auto_reply_periodic_sync";

    public AutoReplySyncWorker(@NonNull Context context, @NonNull WorkerParameters workerParams) {
        super(context, workerParams);
    }

    public static void schedule(Context context) {
        if (context == null) return;
        try {
            Constraints constraints = new Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build();
            PeriodicWorkRequest request = new PeriodicWorkRequest.Builder(
                    AutoReplySyncWorker.class, 15, TimeUnit.MINUTES)
                    .setConstraints(constraints)
                    .build();
            WorkManager.getInstance(context.getApplicationContext())
                    .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request);
        } catch (Throwable e) {
            FileLog.e("AutoReplySyncWorker schedule error", e);
        }
    }

    public static void cancel(Context context) {
        if (context == null) return;
        try {
            WorkManager.getInstance(context.getApplicationContext()).cancelUniqueWork(WORK_NAME);
        } catch (Throwable e) {
            FileLog.e("AutoReplySyncWorker cancel error", e);
        }
    }

    @NonNull
    @Override
    public Result doWork() {
        final CountDownLatch latch = new CountDownLatch(1);
        try {
            AndroidUtilities.runOnUIThread(() -> {
                try {
                    ApplicationLoader.postInitApplication();
                    if (AutoReplyManager.getInstance().isAutoReplyEnabled()) {
                        AutoReplyManager.getInstance().ensureBackgroundKeepAlive();
                        ApplicationLoader.startPushService();
                        for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                            if (UserConfig.getInstance(a).isClientActivated()) {
                                ConnectionsManager cm = ConnectionsManager.getInstance(a);
                                cm.keepNetworkAliveInBackground();
                                cm.resumeNetworkMaybe();
                                cm.checkConnection();
                                MessagesController.getInstance(a).getDifference();
                            }
                        }
                    }
                } catch (Throwable t) {
                    FileLog.e("AutoReplySyncWorker run error", t);
                } finally {
                    latch.countDown();
                }
            });
            latch.await(15, TimeUnit.SECONDS);
            if (AutoReplyManager.getInstance().isAutoReplyEnabled()) {
                IncomingMessageInterceptor.awaitBackgroundAutoReply(14000);
            }
        } catch (Throwable t) {
            FileLog.e("AutoReplySyncWorker doWork error", t);
        }
        return Result.success();
    }
}
