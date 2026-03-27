package com.example.stepforge

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import java.util.Calendar
import com.example.stepforge.widget.StepWidgetCompactProvider
import com.example.stepforge.widget.StepWidgetLargeProvider
import com.example.stepforge.widget.StepWidgetProvider


class MidnightResetReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {

        android.util.Log.e(
            "STEPFORGE_MIDNIGHT",
            "MidnightResetReceiver triggered"
        )

        // 🔹 1) Günlük adımları sıfırlamak için StepCounterService’i başlat
        val serviceIntent = Intent(context, StepCounterService::class.java).apply {
            putExtra("forceReset", true)
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("MidnightReset", "Service start failed: ${e.message}")
        }

        // 🔹 2) Ertesi gece saat 00:00 için yeni alarm kur
        try {
            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val alarmIntent = Intent(context, MidnightResetReceiver::class.java)
            val pending = PendingIntent.getBroadcast(
                context,
                0,
                alarmIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

            // ✅ Android 12+ için özel izin kontrolü
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (am.canScheduleExactAlarms()) {
                    am.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        pending
                    )
                } else {
                    // izin verilmemiş ama SecurityException atmadan fallback
                    Log.w("MidnightReset", "Exact alarm permission not granted")
                    am.set(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        pending
                    )
                }
            } else {
                am.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextMidnight.timeInMillis,
                    pending
                )
            }

            Log.d("MidnightReset", "Next midnight reset scheduled for: $nextMidnight")

        } catch (e: SecurityException) {
            Log.e("MidnightReset", "Exact alarm permission denied", e)
        } catch (e: Exception) {
            Log.e("MidnightReset", "Alarm scheduling failed: ${e.message}")
        }
        // ✅ Eğer servis başlayamazsa en azından widget refresh
        StepWidgetProvider.notifyRefresh(context)
        StepWidgetCompactProvider.notifyRefresh(context)
        StepWidgetLargeProvider.notifyRefresh(context)
    }
}