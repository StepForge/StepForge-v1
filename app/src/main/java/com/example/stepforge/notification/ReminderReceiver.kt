package com.example.stepforge.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.MainActivity
import com.example.stepforge.R
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.core.AppLanguageHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "daily_reminder_channel"
        const val NOTIF_ID = 2001
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("ReminderReceiver", "Daily reminder triggered")

        CoroutineScope(Dispatchers.IO).launch {
            val store = context.stepforgeStore.data.first()
            val totalSteps = store[intPreferencesKey("persisted_total_sum")] ?: 0

            showNotification(context, totalSteps)
        }
    }

    private fun showNotification(context: Context, steps: Int) {
        val textContext = AppLanguageHelper.localizedContext(context)

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Channel (sadece reminder için, alarm değil)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                textContext.getString(R.string.hc_daily_reminders_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = textContext.getString(R.string.hc_daily_reminders_channel_info)
            }

            notificationManager.createNotificationChannel(channel)
        }

        // ✅ App açma intent
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ✅ Mesaj
        val title = textContext.getString(R.string.hc_move_reminder_title)
        val message = if (steps < 1000) {
            textContext.getString(R.string.hc_move_reminder_low)
        } else {
            textContext.getString(R.string.hc_move_reminder_progress, steps)
        }

        // ✅ Notification (SADE - alarm yok)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk) // varsa, yoksa ic_launcher kullan
            .setContentTitle(title)
            .setContentText(message)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID, notification)
    }
}
