package com.example.stepforge.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.util.Calendar
import java.util.Date

object WaterReminderScheduler {

    private val KEY_WATER_ENABLED = booleanPreferencesKey("water_enabled")
    private val KEY_WATER_INTERVAL_MIN = intPreferencesKey("water_interval_min")
    private val KEY_WATER_START_HOUR = intPreferencesKey("water_start_hour")
    private val KEY_WATER_END_HOUR = intPreferencesKey("water_end_hour")

    private const val TAG = "WaterReminderScheduler"

    /**
     * Switch açıldığında çağır: bugünden itibaren aktif saat aralığı içinde ilk alarmı kurar.
     */
    fun schedule(context: Context) {
        val prefs = runBlocking { context.stepforgeStore.data.first() }
        val enabled = prefs[KEY_WATER_ENABLED] ?: false
        if (!enabled) {
            Log.d(TAG, "schedule: water_enabled=false, not scheduling.")
            return
        }

        val intervalMin = prefs[KEY_WATER_INTERVAL_MIN] ?: 60
        val startHour = prefs[KEY_WATER_START_HOUR] ?: 8
        val endHour = prefs[KEY_WATER_END_HOUR] ?: 22

        Log.d(TAG, "schedule: enabled, interval=$intervalMin, window=$startHour..$endHour")

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_WATER_REMINDER
        }
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val now = Calendar.getInstance()
        val hNow = now.get(Calendar.HOUR_OF_DAY)

        val first = Calendar.getInstance().apply {
            // Eğer pencere geçersizse (örn. start=end), direk interval sonrası olarak ele al
            val validWindow = startHour in 0..23 && endHour in 0..24 && startHour != endHour

            if (!validWindow) {
                timeInMillis = System.currentTimeMillis() + intervalMin * 60_000L
            } else if (hNow < startHour) {
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            } else if (hNow in startHour until endHour) {
                // aktif aralıktayız, interval sonrası
                timeInMillis = System.currentTimeMillis() + intervalMin * 60_000L
            } else {
                // bugünün aralığı geçti, yarın startHour
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, startHour)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }

        val triggerAt = first.timeInMillis
        Log.d(TAG, "First water reminder scheduled at: ${Date(triggerAt)}")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "Exact alarm permission missing; using set() instead of exact.")
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
                return
            }
        }

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt,
            pending
        )
    }

    /**
     * Switch kapandığında çağır: mevcut alarmı iptal eder.
     */
    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, WaterReminderReceiver::class.java).apply {
            action = WaterReminderReceiver.ACTION_WATER_REMINDER
        }
        val pending = PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pending)
        Log.d(TAG, "All water reminders cancelled.")
    }
}