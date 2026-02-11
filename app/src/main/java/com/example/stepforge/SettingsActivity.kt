package com.example.stepforge

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.stepforge.notification.ReminderReceiver
import com.example.stepforge.settings.SettingsScreen
import com.example.stepforge.ui.components.HealthSyncManager
import com.example.stepforge.ui.components.SleepSyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.map


class SettingsActivity : ComponentActivity() {

    private lateinit var healthSyncManager: HealthSyncManager
    private lateinit var sleepSyncManager: SleepSyncManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        healthSyncManager = HealthSyncManager(this)
        sleepSyncManager = SleepSyncManager(this)

        permissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted: Set<String> ->
            val required = healthSyncManager.getPermissionStrings()
            val allGranted = granted.containsAll(required)

            if (allGranted) {
                Toast.makeText(this, "Health Connect Connected ✅", Toast.LENGTH_SHORT).show()

                // Steps sync (mevcut)
                healthSyncManager.syncStepsData()

                // Sleep sync (yeni)
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val ok = sleepSyncManager.syncLast7Days()
                        Log.d("HealthConnect", "Sleep sync result=$ok")
                    } catch (e: Exception) {
                        Log.e("HealthConnect", "Sleep sync error", e)
                    }
                }
            } else {
                val missing = required - granted
                Log.e("HealthConnect", "Missing permissions: $missing")
                Toast.makeText(this, "Permissions Missing ❌", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            SettingsScreen(
                activity = this,
                onLaunchAndroidPermission = { launchPermissions() },
                onBack = { finish() }
            )
        }
    }


    private fun launchPermissions() {
        val availability = HealthConnectClient.getSdkStatus(this)

        if (availability == HealthConnectClient.SDK_UNAVAILABLE) {
            Toast.makeText(this, "Health Connect not supported ❌", Toast.LENGTH_LONG).show()
            return
        }
        if (availability == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED) {
            Toast.makeText(this, "Please install/update Health Connect ⚠️", Toast.LENGTH_LONG).show()
            return
        }

        try {
            // ✅ tek doğru set: HealthSyncManager permission seti
            val perms = healthSyncManager.getPermissionStrings()
            Log.d("HealthConnect", "Launching permissions: $perms")
            permissionLauncher.launch(perms)
        } catch (e: Exception) {
            Log.e("HealthConnect", "Error launching permissions", e)
            Toast.makeText(this, "Error launching permissions", Toast.LENGTH_SHORT).show()
        }
    }

    fun scheduleReminder(hour: Int, minute: Int) {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.e("Settings", "Exact alarm permission missing")
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                return
            }
        }

        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this,
            2001,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        if (calendar.timeInMillis <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Toast.makeText(this, "Reminder set for ${calendar.time}", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("Settings", "Alarm scheduling failed", e)
        }
    }
}