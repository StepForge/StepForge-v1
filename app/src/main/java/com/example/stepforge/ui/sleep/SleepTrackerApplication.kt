package com.example.stepforge.ui.sleep

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.stepforge.R

class SleepTrackerApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = getSystemService(NotificationManager::class.java)

            NotificationChannel(
                ALARM_CHANNEL_ID,
                getString(R.string.sleep_channel_alarm_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.sleep_channel_alarm_desc)
                setSound(null, null)
                enableVibration(false)
                enableLights(true)
                manager.createNotificationChannel(this)
            }

            NotificationChannel(
                ALARM_SERVICE_CHANNEL_ID,
                getString(R.string.sleep_channel_alarm_service_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.sleep_channel_alarm_service_desc)
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                manager.createNotificationChannel(this)
            }

            NotificationChannel(
                TRACKING_CHANNEL_ID,
                getString(R.string.sleep_channel_tracking_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.sleep_channel_tracking_desc)
                setShowBadge(false)
                manager.createNotificationChannel(this)
            }
        }
    }

    companion object {
        const val ALARM_CHANNEL_ID = "sleep_alarm_alert_v2"
        const val ALARM_SERVICE_CHANNEL_ID = "sleep_alarm_service_v2"
        const val TRACKING_CHANNEL_ID = "sleep_tracking_channel"
    }
}