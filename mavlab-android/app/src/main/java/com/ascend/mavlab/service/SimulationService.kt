package com.ascend.mavlab.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.ascend.mavlab.R
import com.ascend.mavlab.core.common.AppRuntime

/**
 * Owns the foreground simulation runtime.
 */
class SimulationService : Service() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, notification())
        AppRuntime.start(applicationContext)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        AppRuntime.stop()
        super.onDestroy()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.simulation_service_channel),
            NotificationManager.IMPORTANCE_LOW,
        )
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private fun notification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle(getString(R.string.simulation_service_title))
            .setContentText(getString(R.string.simulation_service_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    private companion object {
        const val CHANNEL_ID = "mavlab_simulation"
        const val NOTIFICATION_ID = 1001
    }
}
