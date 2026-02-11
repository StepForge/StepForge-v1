package com.example.stepforge

import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.HourlySteps
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.steps.StepEvents
import com.example.stepforge.widget.StepWidgetCompactProvider
import com.example.stepforge.widget.StepWidgetLargeProvider
import com.example.stepforge.widget.StepWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notifJob: Job? = null

    @Volatile private var lastSensorValue = 0
    @Volatile private var sensorOffset = 0
    @Volatile private var totalSteps = 0

    // --- Walking tracking ---
    private var sessionStartSteps: Int = 0
    private var sessionStartTime: Long = 0
    private var lastStepTime: Long = System.currentTimeMillis()
    private var isWalking = false
    private var activityCheckJob: Job? = null

    private val INACTIVITY_THRESHOLD_MS = 3 * 60 * 60 * 1000L // 3 hours

    private var currentDay = todayKey()
    private var hasCelebrated = false


    private var hourlyTickerJob: Job? = null
    private val dao by lazy { AppDatabase.getDatabase(this).dailyStepsDao() }

    companion object {
        const val CHANNEL_ID = "step_channel"
        const val NOTIF_ID = 1001
        const val ACTION_GOAL_COMPLETED = "com.example.stepforge.GOAL_COMPLETED"
        const val TAG = "StepForgeDebug"

        const val ALERT_CHANNEL_ID = "StepForgeAlerts"

        @Volatile
        var isServiceRunning: Boolean = false

        private val _targetFlow = MutableStateFlow(10000)
        val targetFlow = _targetFlow.asStateFlow()

        fun updateTarget(newTarget: Int) {
            _targetFlow.value = newTarget
        }

        fun updateServiceNotification(context: Context, currentSteps: Int, currentGoal: Int) {
            val intent = Intent(context, StepCounterService::class.java).apply {
                putExtra("force_update_steps", currentSteps)
                putExtra("force_update_goal", currentGoal)
            }
            context.startService(intent)
        }

        val LAST_HEAVY_USAGE = longPreferencesKey("last_heavy_usage")

        // ✅ Sleep heuristics keys (manual "Samsung Health style")
        // ✅ Sleep heuristics keys (manual "Samsung Health style")
        val REAL_AWAKE_TIME = longPreferencesKey("real_awake_time")
        // 🟣 Auto sleep engine state
        val AUTO_SLEEP_ACTIVE = booleanPreferencesKey("auto_sleep_active")
        val AUTO_SLEEP_START_TIME = longPreferencesKey("auto_sleep_start_time")


        val SLEEP_LAST_SCREEN_OFF_MS = longPreferencesKey("sleep_last_screen_off_ms")

        // 🟢 Auto sleep engine feed keys
        val LAST_STEP_TIME = longPreferencesKey("last_step_time")
        val LAST_MOTION_TIME = longPreferencesKey("last_motion_time")
        val LAST_KNOWN_WAKE_TIME = longPreferencesKey("last_known_wake_time")

        val PROBABLE_SLEEP_START = longPreferencesKey("probable_sleep_start")
        val PROBABLE_SLEEP_END = longPreferencesKey("probable_sleep_end")
        val PROBABLE_SLEEP_READY = booleanPreferencesKey("probable_sleep_ready")

    }



    private val PREF_LAST_SENSOR = intPreferencesKey("pref_last_sensor")
    private val PREF_OFFSET = intPreferencesKey("pref_sensor_offset")
    private val PREF_TOTAL = intPreferencesKey("pref_total_all")

    private fun isStartForegroundNotAllowed(t: Throwable): Boolean {
        var cur: Throwable? = t
        while (cur != null) {
            if (cur.javaClass.name == "android.app.ForegroundServiceStartNotAllowedException") return true
            cur = cur.cause
        }
        return false
    }

    private fun safeStartForeground(notification: Notification): Boolean {
        return try {
            startForeground(NOTIF_ID, notification)
            true
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground blocked: ${t.javaClass.name} ${t.message}", t)

            // ❗ stopSelf YOK. Servisi öldürme.
            // Bu hata olsa bile servis yaşamaya devam etsin.
            false
        }
    }


    // --- screen tracking ---
    private var screenOnTime: Long = 0
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        createChannel()
        Log.d(TAG, "Servis Oluşturuluyor (onCreate)...")

        var currentGoal = 10000
        runBlocking {
            val prefs = stepforgeStore.data.first()
            lastSensorValue = prefs[PREF_LAST_SENSOR] ?: 0
            sensorOffset = prefs[PREF_OFFSET] ?: 0
            totalSteps = prefs[PREF_TOTAL] ?: 0
            currentGoal = prefs[intPreferencesKey("step_goal")] ?: 10000

            Log.d(TAG, "DB'den Yüklenen: Total=$totalSteps, Offset=$sensorOffset, LastSensor=$lastSensorValue")
            StepEvents.emitTodaySteps(totalSteps)
        }

        // 🔥 Foreground başlatmayı dene ama fail olursa servisi durdurma
        val ok = safeStartForeground(buildNotificationSync(totalSteps, currentGoal))
        if (!ok) {
            Log.w(TAG, "Foreground start failed, continuing service anyway...")
        }

        // 🔥 Notification updater her durumda çalışmalı
        startNotificationUpdater()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

        stepSensor?.let {
            try {
                // double register olmasın
                sensorManager.unregisterListener(this)

                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                Log.d(TAG, "Sensor listener registered in onCreate.")
            } catch (e: Exception) {
                Log.e(TAG, "Sensor register error in onCreate", e)
            }
        }

        StepWidgetProvider.sendStepsUpdate(applicationContext, totalSteps)
        StepWidgetCompactProvider.sendStepsUpdate(applicationContext, totalSteps)
        StepWidgetLargeProvider.sendStepsUpdate(applicationContext, totalSteps)

        scheduleMidnightReset()
        startHourlyTicker()

        serviceScope.launch {
            targetFlow.collect {
                updateNotifAsync(totalSteps)
            }
        }

        createAlertChannel()
        startActivityMonitoring()
        lastStepTime = System.currentTimeMillis()

        // ✅ Sleep heuristics receiver
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val now = System.currentTimeMillis()

                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        screenOnTime = now
                    }

                    Intent.ACTION_SCREEN_OFF -> {
                        val duration = now - screenOnTime

                        // 1) Screen off anını kaydet
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                context?.stepforgeStore?.edit { prefs ->
                                    prefs[SLEEP_LAST_SCREEN_OFF_MS] = now
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "write SLEEP_LAST_SCREEN_OFF_MS error", e)
                            }
                        }

                        // 2) Eğer ekran 5 dk+ açık kaldıysa gerçek uyanış kabul et
                        if (duration > 5 * 60 * 1000L) {
                            CoroutineScope(Dispatchers.IO).launch {
                                stepforgeStore.edit { prefs ->
                                    prefs[REAL_AWAKE_TIME] = screenOnTime
                                    prefs[LAST_KNOWN_WAKE_TIME] = screenOnTime
                                    prefs[LAST_HEAVY_USAGE] = now
                                }
                            }
                        }

                        // ===============================
                        // Auto sleep finalize
                        // ===============================
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val prefs = stepforgeStore.data.first()

                                val active = prefs[AUTO_SLEEP_ACTIVE] ?: false
                                val startMs = prefs[AUTO_SLEEP_START_TIME] ?: 0L
                                val endMs = prefs[REAL_AWAKE_TIME] ?: 0L

                                if (!active || startMs == 0L || endMs == 0L) return@launch

                                val durationMin = ((endMs - startMs) / 60_000L).toInt()

                                if (durationMin < 60) {
                                    stepforgeStore.edit {
                                        it[AUTO_SLEEP_ACTIVE] = false
                                        it[AUTO_SLEEP_START_TIME] = 0L
                                    }
                                    return@launch
                                }

                                val saveDate = SimpleDateFormat(
                                    "yyyy-MM-dd",
                                    Locale.getDefault()
                                ).format(Date(endMs))

                                val dao = AppDatabase
                                    .getDatabase(this@StepCounterService)
                                    .sleepSessionDao()

                                dao.deleteByDate(saveDate)

                                dao.insert(
                                    SleepSession(
                                        date = saveDate,
                                        startTime = startMs,
                                        endTime = endMs,
                                        totalMinutes = durationMin,
                                        qualityScore = 80,
                                        source = "auto"
                                    )
                                )

                                stepforgeStore.edit {
                                    it[AUTO_SLEEP_ACTIVE] = false
                                    it[AUTO_SLEEP_START_TIME] = 0L
                                }

                                stepforgeStore.edit {
                                    it[PROBABLE_SLEEP_START] = startMs
                                    it[PROBABLE_SLEEP_END] = endMs
                                    it[PROBABLE_SLEEP_READY] = true
                                }

                                Log.d(TAG, "AUTO SLEEP SAVED: ${durationMin} min")

                            } catch (e: Exception) {
                                Log.e(TAG, "auto sleep finalize error", e)
                            }
                        }

                        // ===============================
                        // Auto sleep start detection
                        // ===============================
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                val prefs = stepforgeStore.data.first()

                                val lastStep = prefs[LAST_STEP_TIME] ?: 0L
                                val lastMotion = prefs[LAST_MOTION_TIME] ?: 0L
                                val isActive = prefs[AUTO_SLEEP_ACTIVE] ?: false

                                val nowMs = System.currentTimeMillis()

                                if (isActive) return@launch

                                val idleThreshold = 15 * 60 * 1000L

                                val stepIdle = nowMs - lastStep > idleThreshold
                                val motionIdle = nowMs - lastMotion > idleThreshold

                                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                val nightTime = (hour >= 21 || hour <= 5)

                                if (stepIdle && motionIdle && nightTime) {
                                    stepforgeStore.edit { p ->
                                        p[AUTO_SLEEP_ACTIVE] = true
                                        p[AUTO_SLEEP_START_TIME] = nowMs
                                    }

                                    Log.d(TAG, "🌙 AUTO SLEEP STARTED at ${Date(nowMs)}")
                                }

                            } catch (e: Exception) {
                                Log.e(TAG, "auto sleep start detection error", e)
                            }
                        }
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }

        registerReceiver(screenReceiver, filter)
    }


    override fun onDestroy() {
        super.onDestroy()

        try {
            screenReceiver?.let { unregisterReceiver(it) }
            screenReceiver = null
        } catch (e: Exception) {
            Log.e(TAG, "Receiver unregister error", e)
        }

        try {
            sensorManager.unregisterListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Sensor unregister error", e)
        }

        notifJob?.cancel()
        notifJob = null

        activityCheckJob?.cancel()
        activityCheckJob = null

        hourlyTickerJob?.cancel()
        hourlyTickerJob = null

        serviceScope.cancel()

        Log.w(TAG, "Service destroyed.")
        isServiceRunning = false

    }




    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "ACTIVITY_RECOGNITION permission missing! Stopping service.")
            stopSelf()
            return START_NOT_STICKY
        }


        try {
            // 1) Widget refresh isteği
            if (intent?.action == "com.example.stepforge.ACTION_WIDGET_REFRESH_REQUEST") {
                Log.d(TAG, "Widget refresh request. Sending: $totalSteps")

                StepWidgetProvider.sendStepsUpdate(applicationContext, totalSteps)
                StepWidgetCompactProvider.sendStepsUpdate(applicationContext, totalSteps)
                StepWidgetLargeProvider.sendStepsUpdate(applicationContext, totalSteps)
            }

            // 2) Test notification
            if (intent?.action == "TEST_NOTIFICATION") {
                Log.d(TAG, "Test bildirimi tetiklendi!")
                sendAlertNotification("Test Başarılı! 🎉", "Bu bir deneme bildirimidir.")
            }

            // 3) Zorla notif update (Settings ekranından gelen)
            val forceSteps = intent?.getIntExtra("force_update_steps", -1) ?: -1
            val forceGoal = intent?.getIntExtra("force_update_goal", -1) ?: -1

            if (forceSteps >= 0) {
                if (totalSteps < forceSteps) {
                    Log.d(TAG, "UI Zorla Güncelleme: Eski=$totalSteps, Yeni=$forceSteps")
                    totalSteps = forceSteps

                    if (lastSensorValue != 0) {
                        sensorOffset = totalSteps - lastSensorValue
                        Log.d(TAG, "Offset recalculated: $sensorOffset (LastSensor=$lastSensorValue)")
                    }

                    StepEvents.emitTodaySteps(totalSteps)
                    saveContinuously(totalSteps)
                }

                val goalToUse = if (forceGoal > 0) forceGoal else runBlocking { getLatestGoal() }
                val notif = buildNotificationSync(totalSteps, goalToUse)

                // 🔥 Foreground dene ama başarısız olsa bile service'i öldürme
                safeStartForeground(notif)

            } else {
                val currentGoal = runBlocking { getLatestGoal() }
                val notification = buildNotificationSync(totalSteps, currentGoal)

                // 🔥 Foreground dene ama başarısız olsa bile service'i öldürme
                safeStartForeground(notification)
            }

            // 4) Manuel step ayarlama (kullanıcı elle set ettiyse)
            val manual = intent?.getIntExtra("manualSteps", -1) ?: -1
            if (manual >= 0) {
                Log.d(TAG, "Manuel Ayarlama: $manual")

                sensorOffset = manual - lastSensorValue
                totalSteps = manual

                StepEvents.emitTodaySteps(manual)
                checkGoalCelebrate(manual)
                updateNotifAsync(manual)

                saveContinuously(manual)

            // ✅ DB’ye insert yerine overwrite gibi davran
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        dao.insertDailySteps(DailySteps(todayKey(), manual))
                    } catch (e: Exception) {
                        Log.e(TAG, "Manual saveDaily failed", e)
                    }
                }


                StepWidgetProvider.sendStepsUpdate(applicationContext, manual)
                StepWidgetCompactProvider.sendStepsUpdate(applicationContext, manual)
                StepWidgetLargeProvider.sendStepsUpdate(applicationContext, manual)
            }

            // 5) Gün reset
            if (intent?.getBooleanExtra("forceReset", false) == true) {
                resetForNewDay()
            }

            // Sensor yeniden başlatma
            if (stepSensor == null) {
                sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
                stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
            }

            stepSensor?.let {
                try {
                    // 🔥 önce eski listener'ı kaldır
                    sensorManager.unregisterListener(this)

                    // 🔥 sonra tekrar register et
                    sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)

                    Log.d(TAG, "Sensor listener registered successfully.")
                } catch (e: Exception) {
                    Log.e(TAG, "Sensor register error in onStartCommand", e)
                }
            }


        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand crash prevented", e)
        }

        startNotificationUpdater()

        // 🔥 EN KRİTİK SATIR:
        // Android öldürürse yeniden başlat
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? = null
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type != Sensor.TYPE_STEP_COUNTER) return
        if (stepSensor == null) return
        if (isNewDay()) resetForNewDay()

        val sensorVal = event.values[0].toInt()

        if (lastSensorValue == 0 || sensorVal < lastSensorValue) {
            sensorOffset = if (totalSteps == 0) -sensorVal else totalSteps - sensorVal
        }


        lastSensorValue = sensorVal
        val calculatedSteps = sensorOffset + sensorVal

        if (calculatedSteps > totalSteps) {
            val diff = calculatedSteps - totalSteps
            totalSteps = calculatedSteps


            if (diff > 0) {
                val now = System.currentTimeMillis()
                val stopThresholdMs = 2 * 60 * 1000L


                if (isWalking && (now - lastStepTime > stopThresholdMs)) {
                    finishWalkingSession()
                }

                if (!isWalking) {
                    isWalking = true
                    sessionStartSteps = totalSteps
                    sessionStartTime = now
                    Log.d(TAG, "Yürüyüş başladı: $sessionStartSteps")
                }

                lastStepTime = now

                // ============================
                // Sleep engine feed (CRITICAL)
                // ============================
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        stepforgeStore.edit { prefs ->
                            prefs[LAST_STEP_TIME] = now
                            prefs[LAST_MOTION_TIME] = now
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "write LAST_STEP_TIME / LAST_MOTION_TIME error", e)
                    }
                }
            }


            StepEvents.emitTodaySteps(totalSteps)
            checkGoalCelebrate(totalSteps)
            updateNotifAsync(totalSteps)
            saveContinuously(totalSteps)
            saveDaily(todayKey(), totalSteps)
            saveHourlySnapshot(totalSteps)

            StepWidgetProvider.sendStepsUpdate(applicationContext, totalSteps)
            StepWidgetCompactProvider.sendStepsUpdate(applicationContext, totalSteps)
            StepWidgetLargeProvider.sendStepsUpdate(applicationContext, totalSteps)

        } else if (calculatedSteps < totalSteps) {
            sensorOffset = totalSteps - sensorVal
            Log.d(TAG, "Offset fix: sensor=$sensorVal newOffset=$sensorOffset")
        }


    }

    @Volatile
    private var lastNotifiedSteps: Int = -1

    private fun updateNotifAsync(sum: Int) {
        if (sum == lastNotifiedSteps) return
        lastNotifiedSteps = sum

        serviceScope.launch {
            val notif = buildNotification(sum)
            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(NOTIF_ID, notif)
        }
    }

    private suspend fun buildNotification(sum: Int): Notification {
        val prefGoal = getLatestGoal()
        val fmtSteps = formatInt(sum)
        val fmtGoal = formatInt(prefGoal)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle("StepForge")
            .setContentText("$fmtSteps steps / Goal $fmtGoal")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine("$fmtSteps steps today")
                    .addLine("Daily Goal: $fmtGoal steps")
                    .setSummaryText("Keep walking 🚶")
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun buildNotificationSync(sum: Int, goal: Int): Notification {
        val fmtSteps = formatInt(sum)
        val fmtGoal = formatInt(goal)

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle("StepForge")
            .setContentText("$fmtSteps steps / Goal $fmtGoal")
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine("$fmtSteps steps today")
                    .addLine("Daily Goal: $fmtGoal steps")
                    .setSummaryText("Keep walking 🚶")
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun saveContinuously(sum: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            stepforgeStore.edit {
                it[PREF_LAST_SENSOR] = lastSensorValue
                it[PREF_OFFSET] = sensorOffset
                it[PREF_TOTAL] = sum
            }
            val persistedKey = intPreferencesKey("persisted_total_sum")
            stepforgeStore.edit { prefs ->
                prefs[persistedKey] = sum
            }
        }
    }

    private fun saveDaily(day: String, sum: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.insertDailySteps(DailySteps(day, sum))
        }
    }

    private fun saveHourlySnapshot(newTotalSteps: Int) {
        val now = Calendar.getInstance()
        val hourNow = now.get(Calendar.HOUR_OF_DAY)
        val dateStr = todayKey()

        // 🔥 CRITICAL FIX:
        // Midnight reset anında eski günün adımlarını yeni güne yazmasını engeller
        if (hourNow == 0 && newTotalSteps > 0) {
            Log.w(TAG, "Skipping hourly snapshot at midnight (hour=0) to prevent wrong day data")
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                val hourlyDao = AppDatabase
                    .getDatabase(this@StepCounterService)
                    .hourlyStepsDao()

                hourlyDao.upsert(
                    HourlySteps(
                        date = dateStr,
                        hour = hourNow,
                        steps = newTotalSteps
                    )
                )

                Log.d(TAG, "Hourly snapshot upserted: date=$dateStr hour=$hourNow total=$newTotalSteps")
            } catch (e: Exception) {
                Log.e(TAG, "saveHourlySnapshot error", e)
            }
        }
    }


    private fun isNewDay(): Boolean {
        val today = todayKey()
        if (today != currentDay) {
            currentDay = today
            return true
        }
        return false
    }

    private fun resetForNewDay() {
        try {
            sensorManager.unregisterListener(this)
        } catch (_: Exception) {}

        val dayToSave = currentDay
        val currentSensorValue = lastSensorValue

        Log.w(TAG, "RESET DAY triggered. Saving day=$dayToSave totalSteps=$totalSteps")

        hasCelebrated = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // ✅ Önce eski günü DB’ye kaydet
                dao.insertDailySteps(DailySteps(dayToSave, totalSteps))

                // ✅ Preferences sıfırla
                stepforgeStore.edit {
                    it[PREF_LAST_SENSOR] = 0
                    it[PREF_OFFSET] = -currentSensorValue
                    it[PREF_TOTAL] = 0
                }

                val persistedKey = intPreferencesKey("persisted_total_sum")
                stepforgeStore.edit { prefs ->
                    prefs[persistedKey] = 0
                }

            } catch (e: Exception) {
                Log.e(TAG, "MidnightReset DB store error", e)
            }

            withContext(Dispatchers.Main) {
                scheduleMidnightReset()
            }
        }

        // ✅ Yeni güne geçiş burada kesin yapılmalı
        currentDay = todayKey()

        totalSteps = 0
        sensorOffset = -currentSensorValue
        lastSensorValue = currentSensorValue
        saveHourlySnapshot(0)


        StepEvents.emitTodaySteps(0)
        updateNotifAsync(0)

        StepWidgetProvider.sendStepsUpdate(applicationContext, 0)
        StepWidgetCompactProvider.sendStepsUpdate(applicationContext, 0)
        StepWidgetLargeProvider.sendStepsUpdate(applicationContext, 0)

        CoroutineScope(Dispatchers.Main).launch {
            delay(1200)
            stepSensor?.let {
                try {
                    sensorManager.registerListener(
                        this@StepCounterService,
                        it,
                        SensorManager.SENSOR_DELAY_NORMAL
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Sensor register failed after reset", e)
                }
            }
        }

        startHourlyTicker()
    }


    private fun scheduleMidnightReset() {
        try {
            val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmMgr.canScheduleExactAlarms()) {
                    Log.w(TAG, "Exact alarm permission not granted.")
                    return
                }
            }

            val nextMidnight = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }

            val intent = Intent(this, MidnightResetReceiver::class.java)
            val pi = PendingIntent.getBroadcast(
                this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMidnight.timeInMillis,
                pi
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule exact alarm: ${e.message}")
        } catch (e: Exception) {
            Log.e(TAG, "scheduleMidnightReset error: ${e.message}")
        }
    }

    private fun startHourlyTicker() {
        hourlyTickerJob?.cancel()

        hourlyTickerJob = serviceScope.launch(Dispatchers.IO) {
            while (isActive) {
                val now = Calendar.getInstance()

                // Next exact hour (HH:00:00.000)
                val nextHour = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    add(Calendar.HOUR_OF_DAY, 1)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val delayMs = (nextHour.timeInMillis - System.currentTimeMillis()).coerceAtLeast(1L)
                delay(delayMs)

                try {
                    // write snapshot for the hour we just reached
                    // example: at 19:00, write hour=19 with totalSteps snapshot
                    saveHourlySnapshot(totalSteps)
                    Log.d(TAG, "Hourly ticker wrote snapshot: date=${todayKey()} hour=${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)} total=$totalSteps")
                } catch (e: Exception) {
                    Log.e(TAG, "Hourly ticker error", e)
                }
            }
        }
    }

    private fun checkGoalCelebrate(current: Int) {
        serviceScope.launch {
            val goal = getLatestGoal()
            if (current >= goal && !hasCelebrated && goal > 1000) {
                hasCelebrated = true
                sendBroadcast(Intent(ACTION_GOAL_COMPLETED))
                sendAlertNotification(
                    "Goal Reached! 🏆",
                    "Congratulations! You've reached your daily goal of $goal steps."
                )
            }
        }
    }

    private suspend fun getLatestGoal(): Int {
        return try {
            val prefs = stepforgeStore.data.first()
            prefs[intPreferencesKey("step_goal")] ?: 10000
        } catch (_: Exception) {
            10000
        }
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "StepForge Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun formatInt(value: Int): String {
        return String.format(Locale.getDefault(), "%,d", value)
    }

    private fun todayKey(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    private fun startActivityMonitoring() {
        activityCheckJob?.cancel()
        activityCheckJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val stopThresholdMs = 2 * 60 * 1000L

                if (isWalking && (now - lastStepTime > stopThresholdMs)) {
                    finishWalkingSession()
                }

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (hour in 10..21) {
                    if (now - lastStepTime > INACTIVITY_THRESHOLD_MS) {
                        sendAlertNotification(
                            "Time to Move! ⚠️",
                            "You haven't moved in 3 hours. Let's take a short walk!"
                        )
                        lastStepTime = now
                    }
                }

                delay(5 * 60 * 1000L)
            }
        }
    }

    private fun finishWalkingSession() {
        val endSteps = totalSteps
        val stepsTaken = endSteps - sessionStartSteps
        val durationMs = lastStepTime - sessionStartTime
        val durationMin = (durationMs / 1000 / 60).toInt().coerceAtLeast(1)

        Log.d(TAG, "Yürüyüş Bitti: $stepsTaken adım, $durationMin dk")

        val stepsPerMinute = if (durationMin > 0) stepsTaken / durationMin else 0
        if (stepsTaken >= 1500 && stepsPerMinute > 20) {
            sendAlertNotification(
                "Great Walk! 🚶‍♂️",
                "You just walked $stepsTaken steps in $durationMin mins."
            )
        }
        isWalking = false
    }

    private fun sendAlertNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createAlertChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                "Activity Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for walking summaries and inactivity"
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        Log.w(TAG, "onTaskRemoved: App removed from recents, scheduling restart...")

        val restartIntent = Intent(applicationContext, StepCounterService::class.java)
        restartIntent.setPackage(packageName)

        val pendingIntent = PendingIntent.getService(
            applicationContext,
            2001,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        // 3 saniye sonra tekrar başlat
        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 3000,
            pendingIntent
        )
    }

    private fun startNotificationUpdater() {
        if (notifJob != null) return

        notifJob = serviceScope.launch {
            while (isActive) {
                try {
                    val goal = getLatestGoal()
                    val notification = buildNotificationSync(totalSteps, goal)

                    // Foreground garanti kalsın
                    safeStartForeground(notification)

                    // Widget update
                    StepWidgetProvider.sendStepsUpdate(applicationContext, totalSteps)
                    StepWidgetCompactProvider.sendStepsUpdate(applicationContext, totalSteps)
                    StepWidgetLargeProvider.sendStepsUpdate(applicationContext, totalSteps)

                    Log.d(TAG, "Notification auto-updated: steps=$totalSteps goal=$goal")

                } catch (e: Exception) {
                    Log.e(TAG, "Notification updater loop error", e)
                }

                delay(60_000)
            }
        }
    }

}