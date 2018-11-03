package com.example.android.offline;


import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;

import com.sap.cloud.mobile.odata.core.Action0;
import com.sap.cloud.mobile.odata.core.Action1;
import com.sap.cloud.mobile.odata.offline.OfflineODataException;
import com.sap.cloud.mobile.odata.offline.OfflineODataProvider;

import java.util.ArrayDeque;
import java.util.Queue;

public class OfflineODataForegroundService extends Service {
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "Offline OData Channel";

    private static final String CANCEL_ACTION = "offline.odata.action.cancel";

    final Queue<Task> tasks = new ArrayDeque<>();

    private Action1<Task> taskCompleteHandler = new Action1<Task>() {
        @Override
        public void call(Task task) {
            synchronized (tasks) {
                tasks.remove(task);

                if (tasks.poll() == null) {
                    stopForeground(true);
                }
            }
        }
    };

    // This is the object that receives interactions from clients.
    private final IBinder binder = new LocalBinder();

    /**
     * Class for clients to access.  Because we know this service always
     * runs in the same process as its clients, we don't need to deal with
     * IPC.
     */
    public class LocalBinder extends android.os.Binder {
        public OfflineODataForegroundService getService() {
            return OfflineODataForegroundService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();

        createNotificationChannel();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();

        if (action.equals(CANCEL_ACTION)) {
            cancelTasks();
        }

        return START_NOT_STICKY;
    }

    private void startTask(Task task) {
        synchronized (tasks) {
            if (tasks.poll() == null) {
                startForeground(NOTIFICATION_ID, createNotification());
            }

            tasks.add(task);
        }

        task.run();
    }

    private void cancelTasks() {
        synchronized (tasks) {
            for (Task task : tasks) {
                task.cancel();
            }
        }
    }

    public void openStore(OfflineODataProvider offlineODataProvider, @Nullable final Action0 successHandler, @Nullable final Action1<OfflineODataException> failureHandler) {
        Task task = new Task(Operation.OPEN, offlineODataProvider, successHandler, failureHandler);
        startTask(task);
    }

    public void downloadStore(OfflineODataProvider offlineODataProvider, @Nullable final Action0 successHandler, @Nullable final Action1<OfflineODataException> failureHandler) {
        Task task = new Task(Operation.DOWNLOAD, offlineODataProvider, successHandler, failureHandler);
        startTask(task);
    }

    public void uploadStore(OfflineODataProvider offlineODataProvider, @Nullable final Action0 successHandler, @Nullable final Action1<OfflineODataException> failureHandler) {
        Task task = new Task(Operation.UPLOAD, offlineODataProvider, successHandler, failureHandler);
        startTask(task);
    }


    private Notification createNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID);
        NotificationCompat.BigTextStyle bigTextStyle = new NotificationCompat.BigTextStyle();
        bigTextStyle.setBigContentTitle("Syncing Data.");
        builder.setStyle(bigTextStyle);
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.ic_downloading);
        builder.setProgress(100, 0, true);

        // Clicking the notification will return to the app
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        builder.setFullScreenIntent(pendingIntent, false);

//        // Add cancel action
//        Intent cancelIntent = new Intent(this, OfflineODataForegroundService.class);
//        cancelIntent.setAction(CANCEL_ACTION);
//        PendingIntent pendingCancelIntent = PendingIntent.getService(this, 0, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT);
//        builder.addAction(android.R.drawable.ic_menu_delete, "Cancel", pendingCancelIntent);
        return builder.build();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Offline Sync";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            channel.setSound(null, null);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private enum Operation {
        OPEN,
        DOWNLOAD,
        UPLOAD
    }

    private class Task implements Runnable {
        private OfflineODataProvider provider;
        private Action0 successHandler;
        private Action1<OfflineODataException> failureHandler;
        private Operation operation;

        private Task(Operation operation, OfflineODataProvider provider, Action0 successHandler, Action1<OfflineODataException> failureHandler) {
            this.operation = operation;
            this.provider = provider;
            this.successHandler = successHandler;
            this.failureHandler = failureHandler;
        }

        public void run() {
            Action0 success = new Action0() {
                @Override
                public void call() {
                    taskCompleteHandler.call(Task.this);

                    if (successHandler != null) {
                        successHandler.call();
                    }
                }
            };

            Action1<OfflineODataException> failure = new Action1<OfflineODataException>() {
                @Override
                public void call(OfflineODataException e) {
                    taskCompleteHandler.call(Task.this);

                    if (failureHandler != null) {
                        failureHandler.call(e);
                    }
                }
            };

            switch(this.operation) {
                case OPEN:
                    this.provider.open(success, failure);
                    break;
                case UPLOAD:
                    this.provider.upload(success, failure);
                    break;
                case DOWNLOAD:
                    this.provider.download(success, failure);
                    break;
            }
        }

        public void cancel() {
            try {
                if (this.operation == Operation.OPEN) {
                    this.provider.cancelDownload();
                }
                else {
                    this.provider.cancelUpload();
                }
            } catch (OfflineODataException ex) {
                // Failed to cancel
            }
        }
    }
}