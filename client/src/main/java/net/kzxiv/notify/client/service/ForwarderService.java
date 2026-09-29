package net.kzxiv.notify.client.service;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import androidx.core.app.NotificationCompat;

import net.kzxiv.notify.client.ConfigurationActivity;
import net.kzxiv.notify.client.HttpTransportService;

public class ForwarderService extends Service {

    private static final int NOTIF_ID = 7777;
    private static final long TICK_INTERVAL_MS = 30_000L;
    private static final String CHANNEL_ID = "forwarder_service";

    private Handler handler;
    private Runnable ticker;
    private PowerManager.WakeLock wakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        startForeground(NOTIF_ID, buildNotification());

        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (pm != null) {
            wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Notifikator::ForwarderWakeLock");
            wakeLock.setReferenceCounted(false);
            wakeLock.acquire();
        }

        handler = new Handler(Looper.getMainLooper());
        ticker = new Runnable() {
            @Override
            public void run() {
                try {
                    Intent transport = new Intent(ForwarderService.this, HttpTransportService.class);
                    transport.putExtra("force_flush", true);
                    startService(transport);
                    LogStore.append(ForwarderService.this, "TICK forced flush requested");
                } catch (Exception e) {
                    LogStore.append(ForwarderService.this, "TICK error: " + e.getMessage());
                }
                handler.postDelayed(this, TICK_INTERVAL_MS);
            }
        };
        handler.post(ticker);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (handler != null && ticker != null) {
            handler.removeCallbacks(ticker);
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification buildNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel chan = new NotificationChannel(
                    CHANNEL_ID,
                    "Notification Forwarder",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) nm.createNotificationChannel(chan);
        }

        Intent tapIntent = new Intent(this, ConfigurationActivity.class);
        int piFlags = PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT;
        PendingIntent pi = PendingIntent.getActivity(this, 0, tapIntent, piFlags);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Notification Forwarder is running")
                .setContentText("Retrying unsent notifications every 30s")
                .setSmallIcon(android.R.drawable.stat_notify_sync)
                .setOngoing(true)
                .setContentIntent(pi)
                .build();
    }

    public static void start(Context context) {
        Intent i = new Intent(context, ForwarderService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(i);
        } else {
            context.startService(i);
        }
    }
}
