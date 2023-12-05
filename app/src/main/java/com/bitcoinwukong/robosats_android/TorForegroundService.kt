package com.bitcoinwukong.robosats_android

import android.app.Service
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TorForegroundService : Service() {
    private lateinit var notificationManager: NotificationManager
    private val channelId = "tor_service_channel"
    private val notificationId = 1
    private val refreshIntervalMillis: Long = 300000 // 5 minutes

    private val executor = Executors.newSingleThreadScheduledExecutor()

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        startForeground(notificationId, buildNotification("Initializing..."))

        executor.scheduleAtFixedRate({
            // Refresh orders and update notification here
            val notification = buildNotification("Active orders: [info here]")
            notificationManager.notify(notificationId, notification)
        }, 0, refreshIntervalMillis, TimeUnit.MILLISECONDS)

        return START_STICKY
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            channelId,
            "Tor Service Channel",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)
    }

    private fun buildNotification(contentText: String): Notification {
        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Tor Connection Service")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_notification) // Replace with your icon
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        executor.shutdownNow()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
