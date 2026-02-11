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
        // Log ekleyelim ki çalıştığını görelim
        Log.d("ReminderReceiver", "Alarm çaldı! Bildirim hazırlanıyor...")

        CoroutineScope(Dispatchers.IO).launch {
            // Sadece adım sayısını al, 'isEnabled' kontrolünü kaldırdık.
            val store = context.stepforgeStore.data.first()
            val totalSteps = store[intPreferencesKey("persisted_total_sum")] ?: 0

            showNotification(context, totalSteps)
        }
    }

    private fun showNotification(context: Context, steps: Int) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // 1. Kanal Oluştur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Daily Reminders",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Reminds you to walk every day"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. Tıklama Aksiyonu
        val contentIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // 3. Mesaj
        val title = "Time to Walk! 🏃"
        val message = if (steps < 1000) {
            "You haven't moved much today. Let's take a walk!"
        } else {
            "Keep it up! You have reached $steps steps so far."
        }

        // 4. Göster
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIF_ID, notification)
        Log.d("ReminderReceiver", "Bildirim gönderildi.")
    }
}
