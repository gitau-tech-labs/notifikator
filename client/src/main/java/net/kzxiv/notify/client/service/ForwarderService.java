package net.kzxiv.notify.client.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import net.kzxiv.notify.client.ConfigurationActivity
import net.kzxiv.notify.client.HttpTransportService
import net.kzxiv.notify.client.NotificationService

class ForwarderService : Service() {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var ticker: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIF_ID, buildNotification())

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Notifikator::ForwarderWakeLock").apply {
            setReferenceCounted(false)
            acquire()
        }

        // 30-second retry loop
        ticker = scope.launch {
            while (isActive) {
                try {
                    // Ask the notification service to flush anything unsent.
                    // We start the existing HttpTransportService which already
                    // knows how to deliver queued payloads.
                    val transport = Intent(this@ForwarderService, HttpTransportService::class.java)
                    transport.putExtra("force_flush", true)
                    startService(transport)

                    LogStore.append(this@ForwarderService, "TICK forced flush requested")
                } catch (e: Exception) {
                    LogStore.append(this@ForwarderService, "TICK error: ${e.message}")
                }
                delay(30_000L)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onDestroy() {
        ticker?.cancel()
        wakeLock?.let { if (it.isHeld) it.release() }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun buildNotification(): Notification {
        val channelId = "forwarder_service"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val chan = NotificationChannel(
                channelId,
                "Notification Forwarder",
                NotificationManager.IMPORTANCE_LOW
            )
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(chan)
        }

        val tapIntent = Intent(this, ConfigurationActivity::class.java)
        val pi = PendingIntent.getActivity(
            this, 0, tapIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Notification Forwarder is running")
            .setContentText("Retrying unsent notifications every 30s")
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .setContentIntent(pi)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 7777

        fun start(context: Context) {
            val i = Intent(context, ForwarderService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(i)
            } else {
                context.startService(i)
            }
        }
    }
}
