package com.example.stepforge

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        // ✅ LOCKED_BOOT_COMPLETED'da DataStore erişilemez, sadece BOOT_COMPLETED kullan
        if (action != Intent.ACTION_BOOT_COMPLETED) return

        // ✅ 1) Boot sonrası: midnight reset alarmını yeniden kur (kritik)
        scheduleNextMidnightReset(context)

        // 2) Mevcut davranışın: service için worker (istersen kalabilir)
        val request = OneTimeWorkRequestBuilder<StartStepServiceWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }

    private fun scheduleNextMidnightReset(context: Context) {
        try {
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val alarmIntent = Intent(context, MidnightResetReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                context,
                0,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            // ✅ Android 12+ exact alarm izni yoksa approximate fallback
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        pi
                    )
                } else {
                    Log.w("BootReceiver", "Exact alarm permission missing; using setWindow fallback")
                    // ✅ 5 dakikalık pencere, Doze modunda bile çalışır
                    am.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        5 * 60 * 1000L,
                        pi
                    )
                }
            } else {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.timeInMillis,
                    pi
                )
            }

            Log.d("BootReceiver", "Midnight reset scheduled after boot: $nextMidnight")
        } catch (e: Exception) {
            Log.e("BootReceiver", "scheduleNextMidnightReset failed", e)
        }
    }
}