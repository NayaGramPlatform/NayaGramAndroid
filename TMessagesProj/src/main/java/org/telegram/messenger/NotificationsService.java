/*
 * This is the source code of Telegram for Android v. 1.3.x.
 * It is licensed under GNU GPL v. 2 or later.
 * You should have received a copy of the license in this archive (see LICENSE).
 *
 * Copyright Nikolai Kudashov, 2013-2018.
 */

package org.telegram.messenger;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ServiceInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.ui.LaunchActivity;

public class NotificationsService extends Service {

    public static final String EXTRA_START_FOREGROUND = "ng_foreground";
    private static final String CHANNEL_ID = "ng_auto_reply_bg";
    private static final int FOREGROUND_ID = 70492;
    private static final long HEARTBEAT_MS = 45_000L;

    private final Handler serviceHandler = new Handler(Looper.getMainLooper());
    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;
    private boolean isForegroundActive = false;
    private int heartbeatTick = 0;

    private final Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (!AutoReplyManager.getInstance().isAutoReplyEnabled()) {
                stopForegroundIfActive();
                return;
            }
            heartbeatTick++;
            final boolean shouldSyncDiff = (heartbeatTick % 4 == 0) || ApplicationLoader.mainInterfacePaused || !ApplicationLoader.isScreenOn;
            syncActiveAccounts(shouldSyncDiff);
            serviceHandler.removeCallbacks(this);
            serviceHandler.postDelayed(this, HEARTBEAT_MS);
        }
    };

    private final Runnable networkRestoreSyncRunnable = () -> {
        if (AutoReplyManager.getInstance().isAutoReplyEnabled()) {
            syncActiveAccounts(true);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        ApplicationLoader.postInitApplication();
        if (AutoReplyManager.getInstance().isAutoReplyEnabled()) {
            startForegroundCompat();
        }
        registerNetworkRestoreCallback();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        ApplicationLoader.postInitApplication();
        boolean autoReplyEnabled = AutoReplyManager.getInstance().isAutoReplyEnabled();
        boolean requestedForeground = intent != null && intent.getBooleanExtra(EXTRA_START_FOREGROUND, false);

        if (autoReplyEnabled) {
            startForegroundCompat();
            AutoReplyManager.getInstance().ensureBackgroundKeepAlive();
            syncActiveAccounts(true);
            serviceHandler.removeCallbacks(heartbeatRunnable);
            serviceHandler.postDelayed(heartbeatRunnable, HEARTBEAT_MS);
        } else {
            if (requestedForeground && !isForegroundActive) {
                // Satisfy Android's startForegroundService contract before dropping foreground state
                startForegroundCompat();
            }
            serviceHandler.removeCallbacks(heartbeatRunnable);
            stopForegroundIfActive();
        }
        return START_STICKY;
    }

    private void startForegroundCompat() {
        try {
            Notification notification = buildServiceNotification();
            if (Build.VERSION.SDK_INT >= 29) {
                startForeground(FOREGROUND_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            } else {
                startForeground(FOREGROUND_ID, notification);
            }
            isForegroundActive = true;
        } catch (Throwable t) {
            FileLog.e("NotificationsService startForeground error", t);
        }
    }

    private void stopForegroundIfActive() {
        if (!isForegroundActive) {
            return;
        }
        try {
            if (Build.VERSION.SDK_INT >= 24) {
                stopForeground(STOP_FOREGROUND_REMOVE);
            } else {
                stopForeground(true);
            }
        } catch (Throwable ignored) {}
        isForegroundActive = false;
    }

    private Notification buildServiceNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null && nm.getNotificationChannel(CHANNEL_ID) == null) {
                    NotificationChannel ch = new NotificationChannel(
                            CHANNEL_ID,
                            "Auto Answer Background Service",
                            NotificationManager.IMPORTANCE_MIN
                    );
                    ch.setShowBadge(false);
                    ch.enableLights(false);
                    ch.enableVibration(false);
                    ch.setSound(null, null);
                    nm.createNotificationChannel(ch);
                }
            } catch (Throwable ignored) {}
        }

        Intent openIntent = new Intent(this, LaunchActivity.class);
        openIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.notification)
                .setContentTitle("NayaGram Auto Answer Active")
                .setContentText("Replying automatically in background while keeping offline status private")
                .setOngoing(true)
                .setSilent(true)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setContentIntent(contentIntent)
                .build();
    }

    private void registerNetworkRestoreCallback() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N || networkCallback != null) {
            return;
        }
        try {
            connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager != null) {
                networkCallback = new ConnectivityManager.NetworkCallback() {
                    @Override
                    public void onAvailable(@NonNull Network network) {
                        serviceHandler.removeCallbacks(networkRestoreSyncRunnable);
                        serviceHandler.postDelayed(networkRestoreSyncRunnable, 800L);
                    }
                };
                connectivityManager.registerDefaultNetworkCallback(networkCallback);
            }
        } catch (Throwable t) {
            FileLog.e("NotificationsService registerNetworkCallback error", t);
        }
    }

    private void unregisterNetworkRestoreCallback() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && connectivityManager != null && networkCallback != null) {
            try {
                connectivityManager.unregisterNetworkCallback(networkCallback);
            } catch (Throwable ignored) {}
            networkCallback = null;
        }
    }

    private void syncActiveAccounts(boolean fetchDifference) {
        AndroidUtilities.runOnUIThread(() -> {
            try {
                for (int a = 0; a < UserConfig.MAX_ACCOUNT_COUNT; a++) {
                    if (UserConfig.getInstance(a).isClientActivated()) {
                        ConnectionsManager cm = ConnectionsManager.getInstance(a);
                        cm.keepNetworkAliveInBackground();
                        cm.checkConnection();
                        if (fetchDifference) {
                            MessagesController.getInstance(a).getDifference();
                        }
                    }
                }
            } catch (Throwable t) {
                FileLog.e("NotificationsService sync error", t);
            }
        });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        serviceHandler.removeCallbacksAndMessages(null);
        unregisterNetworkRestoreCallback();
        stopForegroundIfActive();
        super.onDestroy();
        SharedPreferences preferences = MessagesController.getGlobalNotificationsSettings();
        if (preferences.getBoolean("pushService", true) || AutoReplyManager.getInstance().isAutoReplyEnabled()) {
            Intent intent = new Intent("org.nayagram.platform.start");
            intent.setPackage(getPackageName());
            sendBroadcast(intent);
        }
    }
}
