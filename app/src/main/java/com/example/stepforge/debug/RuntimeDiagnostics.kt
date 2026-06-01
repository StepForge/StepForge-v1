package com.example.stepforge.debug

import android.app.ActivityManager
import android.app.NotificationManager
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.Build
import android.os.PowerManager
import com.example.stepforge.StepCounterService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object RuntimeDiagnostics {

    @Volatile
    var lastSensorEventTime: Long = 0L

    @Volatile
    var lastAcceptedStepTime: Long = 0L

    @Volatile
    var lastWidgetUpdateTime: Long = 0L

    @Volatile
    var lastNotificationUpdateTime: Long = 0L

    @Volatile
    var lastPersistTime: Long = 0L

    @Volatile
    var lastPersistDurationMs: Long = 0L

    @Volatile
    var pendingFlushCount: Int = 0

    fun build(context: Context): String {

        val sensorManager =
            context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val powerManager =
            context.getSystemService(Context.POWER_SERVICE) as PowerManager

        val activityManager =
            context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val stepCounterExists =
            sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

        val accelExists =
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

        val batteryIgnored =
            powerManager.isIgnoringBatteryOptimizations(context.packageName)

        val notifEnabled =
            notificationManager.areNotificationsEnabled()

        val serviceRunning =
            StepCounterService.isServiceRunning

        val formatter = SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.US
        )

        fun formatTime(ms: Long): String {
            if (ms <= 0L) return "never"
            return formatter.format(Date(ms))
        }

        fun age(ms: Long): String {
            if (ms <= 0L) return "never"

            val diff = System.currentTimeMillis() - ms

            return when {
                diff < 1000L -> "${diff}ms ago"
                diff < 60_000L -> "${diff / 1000}s ago"
                diff < 3_600_000L -> "${diff / 60000}m ago"
                else -> "${diff / 3600000}h ago"
            }
        }

        return buildString {

            appendLine("---")
            appendLine("Runtime State")
            appendLine("---")

            appendLine("service_running=$serviceRunning")
            appendLine("notifications_enabled=$notifEnabled")

            appendLine()

            appendLine("manufacturer=${Build.MANUFACTURER}")
            appendLine("model=${Build.MODEL}")

            appendLine()

            appendLine("battery_optimization_ignored=$batteryIgnored")

            appendLine()

            appendLine("step_counter_sensor=$stepCounterExists")
            appendLine("accelerometer_sensor=$accelExists")

            appendLine()

            appendLine(
                "last_sensor_event=${formatTime(lastSensorEventTime)} (${age(lastSensorEventTime)})"
            )

            appendLine(
                "last_accepted_step=${formatTime(lastAcceptedStepTime)} (${age(lastAcceptedStepTime)})"
            )

            appendLine(
                "last_widget_update=${formatTime(lastWidgetUpdateTime)} (${age(lastWidgetUpdateTime)})"
            )

            appendLine(
                "last_notification_update=${formatTime(lastNotificationUpdateTime)} (${age(lastNotificationUpdateTime)})"
            )

            appendLine(
                "last_persist=${formatTime(lastPersistTime)} (${age(lastPersistTime)})"
            )

            appendLine()

            appendLine("last_persist_duration_ms=$lastPersistDurationMs")
            appendLine("pending_flush_count=$pendingFlushCount")

            appendLine("---")
        }
    }
}