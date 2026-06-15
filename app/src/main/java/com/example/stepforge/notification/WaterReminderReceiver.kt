package com.example.stepforge.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.stepforge.R
import com.example.stepforge.WaterReminderActivity
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.core.AppLanguageHelper
import kotlinx.coroutines.runBlocking
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import java.util.Calendar

class WaterReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "water_reminder_channel"
        const val NOTIF_ID = 3001
        const val ACTION_WATER_REMINDER = "com.example.stepforge.ACTION_WATER_REMINDER"
        private const val TAG = "WaterReminderReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_WATER_REMINDER) {
            Log.d(TAG, "Ignored intent action=${intent.action}")
            return
        }

        Log.d(TAG, "onReceive: Water reminder fired")

        // 1) Ayarlar hâlâ aktif mi?
        val KEY_WATER_ENABLED = booleanPreferencesKey("water_enabled")
        val KEY_WATER_INTERVAL_MIN = intPreferencesKey("water_interval_min")
        val KEY_WATER_START_HOUR = intPreferencesKey("water_start_hour")
        val KEY_WATER_END_HOUR = intPreferencesKey("water_end_hour")

        val prefs = runBlocking { context.stepforgeStore.data.first() }

        val enabled = prefs[KEY_WATER_ENABLED] ?: false
        if (!enabled) {
            Log.d(TAG, "Water reminder disabled in prefs; skipping.")
            return
        }

        val intervalMin = prefs[KEY_WATER_INTERVAL_MIN] ?: 60
        val startHour = prefs[KEY_WATER_START_HOUR] ?: 8
        val endHour = prefs[KEY_WATER_END_HOUR] ?: 22

        // 2) Zaman aralığı kontrolü – ancak TEST için esnekleştirelim
        val nowCal = Calendar.getInstance()
        val h = nowCal.get(Calendar.HOUR_OF_DAY)
        Log.d(TAG, "Current hour=$h, window=$startHour..$endHour")

        // Eğer startHour ve endHour mantıklı değilse (ör: aynı, ya da 0), tüm gün çalışsın
        val hasValidWindow = startHour in 0..23 && endHour in 0..24 && startHour != endHour

        if (hasValidWindow) {
            // Normal davranış: sadece verilen aralıkta bildirim göster
            if (h < startHour || h >= endHour) {
                Log.d(
                    TAG,
                    "Outside active window; no notification. (hour=$h, window=$startHour..$endHour)"
                )
                // Bugünkü döngü burada biter; yarın WaterReminderScheduler tekrar kurar.
                return
            }
        } else {
            Log.w(
                TAG,
                "Invalid time window ($startHour..$endHour). Treating as always-allowed for reminders."
            )
        }

        // 3) Bildirimi göster
        showNotification(context)

        // 4) Sonraki reminder için alarm kur
        scheduleNext(context, intervalMin)
    }

    private fun showNotification(context: Context) {
        val textContext = AppLanguageHelper.localizedContext(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Kanal
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                textContext.getString(R.string.hc_water_reminders_channel),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = textContext.getString(R.string.hc_water_reminders_channel_info)
            }
            manager.createNotificationChannel(channel)
        }

        // WaterReminderActivity açan intent
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, WaterReminderActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notif = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk) // su ikonu varsa değiştirebilirsin
            .setContentTitle(textContext.getString(R.string.hc_water_reminder_title))
            .setContentText(textContext.getString(R.string.hc_water_reminder_message))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIF_ID, notif)
        Log.d(TAG, "Water reminder notification shown.")
    }

    private fun scheduleNext(context: Context, intervalMin: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = ACTION_WATER_REMINDER
        }
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val nextTime = System.currentTimeMillis() + intervalMin * 60_000L
        Log.d(TAG, "Scheduling next water reminder at millis=$nextTime (+$intervalMin min)")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission missing; using set() instead of exact.")
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    nextTime,
                    pending
                )
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            nextTime,
            pending
        )
    }
}
