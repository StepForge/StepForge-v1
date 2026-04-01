package com.example.stepforge

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
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.stepforge.core.WorkoutEvent
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.HourlySteps
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.WorkoutSession
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.debug.DebugLogger
import com.example.stepforge.debug.StepSafetyGuard
import com.example.stepforge.steps.StepEvents
import com.example.stepforge.ui.streak.StreakDayQualifier
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
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt
import com.example.stepforge.ui.streak.StreakShieldEngine
import com.example.stepforge.ui.streak.StreakShieldPrefs
import com.example.stepforge.ui.components.PremiumCoachEngine
import com.example.stepforge.ui.components.PremiumCoachNotifier


class StepCounterService : Service(), SensorEventListener {

//    private val profileResolver by lazy { ProfileSnapshotResolver(this) }
//    private val workoutCalibrator = ProfileWorkoutCalibrator()
//    private val sessionEngine = WorkoutSessionEngine()
//    private lateinit var workoutEngine: WorkoutEngine
//    private var lastState: UserState = UserState.IDLE
//    private lateinit var motionEngine: MotionEngine
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastMotionMs = 0L
    private var lastDateDebug = ""

    private val MOTION_STEP_TIMEOUT = 10000L
    private var sleepStartMs = 0L
    private var sleepEndMs = 0L

    private var inSleepMode = false
    private var stepSensor: Sensor? = null

    private var stepDetector: Sensor? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var notifJob: Job? = null
    private var lastCounterValueForMotion = 0
    private var lastCounterCheckTime = 0L

    @Volatile private var cachedGoal: Int = 10000
    @Volatile private var lastDetectorStepTime = 0L
    @Volatile private var detectorEverWorked: Boolean = false
    @Volatile private var lastSensorValue = 0
    @Volatile private var lastSensorEventTime = System.currentTimeMillis()
    @Volatile private var sensorOffset = 0
    @Volatile private var totalSteps = 0
    @Volatile private var lastResetMs: Long = 0L
    @Volatile private var lastWidgetUpdateMs = 0L
    @Volatile private var lastPersistMs = 0L
    @Volatile private var lastNotificationUpdateMs = 0L
    // --- Walking tracking ---
    private var sessionStartSteps: Int = 0
    private var sessionStartTime: Long = 0
    private var lastStepTime: Long = System.currentTimeMillis()
    private var isWalking = false
    private var activityCheckJob: Job? = null

    private val INACTIVITY_THRESHOLD_MS = 3 * 60 * 60 * 1000L // 3 hours
    private var currentDay = todayKey()
    private var hasCelebrated = false

    private val MOTION_THRESHOLD = 0.25f
    private val SLEEP_START_AFTER_MS = 20 * 60_000L   // 20 dk hareketsizlik -> uyku
    private val WAKE_AFTER_MS = 2 * 60_000L           // 2 dk hareket -> uyandı

    private var hourlyTickerJob: Job? = null
    private val dao by lazy { AppDatabase.getDatabase(this).dailyStepsDao() }


    private val stepSafetyGuard = StepSafetyGuard()
    private val premiumCoachNotifier by lazy { PremiumCoachNotifier(this) }
    companion object {
        const val CHANNEL_ID = "step_channel"
        const val NOTIF_ID = 1001
        const val ACTION_GOAL_COMPLETED = "com.example.stepforge.GOAL_COMPLETED"
        const val TAG = "StepForgeDebug"

        const val ALERT_CHANNEL_ID = "StepForgeAlerts"

        private const val WALK_SESSION_STOP_THRESHOLD_MS = 25_000L
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

        val LAST_RESET_DATE = androidx.datastore.preferences.core.stringPreferencesKey("last_reset_date")

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

            DebugLogger.i(
                TAG,
                "startForeground succeeded",
                metadata = mapOf(
                    "notificationId" to NOTIF_ID.toString()
                )
            )

            true
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground blocked: ${t.javaClass.name} ${t.message}", t)

            DebugLogger.e(
                TAG,
                "startForeground blocked: ${t.javaClass.name} ${t.message}",
                throwable = t,
                metadata = mapOf(
                    "notificationId" to NOTIF_ID.toString(),
                    "isForegroundStartNotAllowed" to isStartForegroundNotAllowed(t).toString()
                )
            )

            // ❗ stopSelf YOK. Servisi öldürme.
            // Bu hata olsa bile servis yaşamaya devam etsin.
            false
        }
    }

    private fun getToday(): String {
        return java.time.LocalDate
            .now(java.time.ZoneId.systemDefault())
            .toString()
    }


    // --- screen tracking ---
    private var screenOnTime: Long = 0
    private var screenReceiver: BroadcastReceiver? = null

    override fun onCreate() {
        super.onCreate()

//        workoutEngine = WorkoutEngine()
//        motionEngine = MotionEngine()
//
        serviceScope.launch {
            targetFlow.collect {
                cachedGoal = it
            }
        }
//
//        handler.post(object : Runnable {
//            override fun run() {
//
//                motionEngine.update()
//
//                val currentState = motionEngine.getState()
//
//                sessionEngine.tick(System.currentTimeMillis())?.let { event ->
//                    if (event is WorkoutSessionTransition.Finished) {
//                        finishWalkingSession(event.endTimeMs)
//
//                        DebugLogger.i(TAG, "SESSION TIMEOUT STOP (v3)")
//                    }
//                }
//
//                if (currentState != lastState) {
//
//                    DebugLogger.d(
//                        TAG,
//                        "STATE CHANGE DETECTED IN SERVICE",
//                        metadata = mapOf(
//                            "oldState" to lastState.name,
//                            "newState" to currentState.name
//                        )
//                    )
//
//                    val event = workoutEngine.onStateChanged(
//                        state = currentState,
//                        currentSteps = totalSteps,
//                        currentTime = System.currentTimeMillis()
//                    )
//
//                    if (event != null) {
//                        handleWorkoutEvent(event)
//                    }
//
//                    lastState = currentState
//                }
//
//                handler.postDelayed(this, 1000)
//            }
//        })

        // -------------------------------------------------
        // StepCounterService / lifecycle start
        // -------------------------------------------------
        isServiceRunning = true
        createChannel()

        Log.d(TAG, "Servis Oluşturuluyor (onCreate)...")
        DebugLogger.i(
            TAG,
            "StepCounterService onCreate started"
        )

        // -------------------------------------------------
        // Restore persisted sensor/session state
        // -------------------------------------------------
        serviceScope.launch {

            val prefs = stepforgeStore.data.first()

            lastSensorValue = prefs[PREF_LAST_SENSOR] ?: 0
            sensorOffset = prefs[PREF_OFFSET] ?: 0
            totalSteps = prefs[PREF_TOTAL] ?: 0
            val currentGoal = (prefs[intPreferencesKey("step_goal")] ?: 10000)
                .coerceIn(1000, 100000)

            Log.d(TAG, "DB'den Yüklenen: Total=$totalSteps, Offset=$sensorOffset, LastSensor=$lastSensorValue")

            DebugLogger.i(
                TAG,
                "Persisted step state restored",
                metadata = mapOf(
                    "totalSteps" to totalSteps.toString(),
                    "sensorOffset" to sensorOffset.toString(),
                    "lastSensorValue" to lastSensorValue.toString(),
                    "goal" to currentGoal.toString()
                )
            )

            StepEvents.emitTodaySteps(totalSteps)

            // 🔥 NOTIFICATION BURADA OLMALI
            val notif = buildNotification(totalSteps)
            safeStartForeground(notif)
        }

        // -------------------------------------------------
        // Notification updater
        // -------------------------------------------------
        startNotificationUpdater()

        // -------------------------------------------------
        // Sensor setup
        // -------------------------------------------------
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetector = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)

        DebugLogger.i(
            TAG,
            "Sensors resolved",
            metadata = mapOf(
                "stepSensorExists" to (stepSensor != null).toString(),
                "stepDetectorExists" to (stepDetector != null).toString()
            )
        )

        // -------------------------------------------------
        // Step detector listener
        // -------------------------------------------------
        stepDetector?.let {
            try {
                sensorManager.registerListener(
                    detectorListener,
                    it,
                    SensorManager.SENSOR_DELAY_UI
                )
                Log.d(TAG, "Step detector listener registered")
                DebugLogger.i(TAG, "Step detector listener registered")
            } catch (e: Exception) {
                Log.e(TAG, "Step detector register error", e)
                DebugLogger.e(TAG, "Step detector register failed", e)
            }
        }

        // -------------------------------------------------
        // Step counter listener
        // -------------------------------------------------
        stepSensor?.let {
            try {
                sensorManager.unregisterListener(this)
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)

                Log.d(TAG, "Sensor listener registered in onCreate.")
                DebugLogger.i(TAG, "Step counter listener registered in onCreate")
            } catch (e: Exception) {
                Log.e(TAG, "Sensor register error in onCreate", e)
                DebugLogger.e(TAG, "Step counter register failed in onCreate", e)
            }
        }

        // -------------------------------------------------
        // Initial widget sync + reset scheduler + watchdog
        // -------------------------------------------------
        StepWidgetProvider.sendStepsUpdate(applicationContext, totalSteps)
        StepWidgetCompactProvider.sendStepsUpdate(applicationContext, totalSteps)
        StepWidgetLargeProvider.sendStepsUpdate(applicationContext, totalSteps)

        DebugLogger.d(
            TAG,
            "Initial widget sync sent",
            metadata = mapOf("totalSteps" to totalSteps.toString())
        )

        scheduleMidnightReset()
        startHourlyTicker()
        startSensorWatchdog()

        // -------------------------------------------------
        // Observe goal updates for notification refresh
        // -------------------------------------------------
        serviceScope.launch {
            targetFlow.collect {
                DebugLogger.d(
                    TAG,
                    "Target flow changed",
                    metadata = mapOf("newTarget" to it.toString())
                )
                updateNotifAsync(totalSteps)
            }
        }

        // -------------------------------------------------
        // Alerts + walking monitor
        // -------------------------------------------------
        createAlertChannel()
        startActivityMonitoring()
        lastStepTime = System.currentTimeMillis()
        startShieldDrainTicker()

        // -------------------------------------------------
        // Screen on/off receiver for sleep heuristics
        // -------------------------------------------------
        screenReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val now = System.currentTimeMillis()

                when (intent?.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        screenOnTime = now

                        Log.d(TAG, "Screen ON detected")
                        DebugLogger.d(
                            TAG,
                            "Screen ON received",
                            metadata = mapOf("screenOnTime" to screenOnTime.toString())
                        )

                        serviceScope.launch {
                            try {
                                val prefs = stepforgeStore.data.first()

                                val active = prefs[AUTO_SLEEP_ACTIVE] ?: false
                                val startMs = prefs[AUTO_SLEEP_START_TIME] ?: 0L

                                if (!active || startMs == 0L) return@launch

                                delay(5 * 60 * 1000L)

                                val stillOn = screenOnTime == now
                                if (!stillOn) return@launch

                                stepforgeStore.edit { pref ->
                                    pref[REAL_AWAKE_TIME] = screenOnTime
                                    pref[LAST_KNOWN_WAKE_TIME] = screenOnTime
                                    pref[LAST_HEAVY_USAGE] = System.currentTimeMillis()
                                }


                                val endMs = screenOnTime
                                finalizeAutoSleepSession(
                                    startMs = startMs,
                                    endMs = endMs
                                )

                            } catch (e: Exception) {
                                Log.e(TAG, "auto sleep finalize error", e)
                                DebugLogger.e(TAG, "Auto sleep finalize error", e)
                            }
                        }
                    }

                    Intent.ACTION_SCREEN_OFF -> {
                        Log.d(TAG, "Screen OFF detected")
                        DebugLogger.d(
                            TAG,
                            "Screen OFF received",
                            metadata = mapOf("time" to now.toString())
                        )

                        serviceScope.launch {
                            try {
                                val nowMs = System.currentTimeMillis()

                                stepforgeStore.edit { prefs ->
                                    prefs[SLEEP_LAST_SCREEN_OFF_MS] = nowMs
                                }

                                delay(15 * 60 * 1000L)

                                val prefs = stepforgeStore.data.first()
                                val lastStep = prefs[LAST_STEP_TIME] ?: 0L
                                val lastMotion = prefs[LAST_MOTION_TIME] ?: 0L
                                val alreadyActive = prefs[AUTO_SLEEP_ACTIVE] ?: false

                                if (alreadyActive) return@launch

                                val idleThreshold = 15 * 60 * 1000L
                                val now2 = System.currentTimeMillis()

                                val stepIdle = now2 - lastStep > idleThreshold
                                val motionIdle = now2 - lastMotion > idleThreshold

                                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                                val nightTime = (hour >= 21 || hour <= 5)

                                if (stepIdle && motionIdle && nightTime) {
                                    val startCandidate = maxOf(lastStep, lastMotion).coerceAtMost(now2)

                                    stepforgeStore.edit { p ->
                                        p[AUTO_SLEEP_ACTIVE] = true
                                        p[AUTO_SLEEP_START_TIME] = startCandidate
                                    }

                                    Log.d(TAG, "🌙 AUTO SLEEP STARTED at ${Date(startCandidate)}")
                                    DebugLogger.i(
                                        TAG,
                                        "Auto sleep started from screen-off flow",
                                        metadata = mapOf(
                                            "startCandidate" to startCandidate.toString()
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "ACTION_SCREEN_OFF sleep detect error", e)
                                DebugLogger.e(TAG, "ACTION_SCREEN_OFF sleep detect error", e)
                            }
                        }

                        serviceScope.launch {
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

                                val startCandidate = maxOf(lastStep, lastMotion).coerceAtMost(nowMs)

                                if (stepIdle && motionIdle && nightTime) {
                                    stepforgeStore.edit { p ->
                                        p[AUTO_SLEEP_ACTIVE] = true
                                        p[AUTO_SLEEP_START_TIME] = startCandidate
                                    }

                                    Log.d(TAG, "🌙 AUTO SLEEP STARTED at ${Date(nowMs)}")
                                    DebugLogger.i(
                                        TAG,
                                        "Auto sleep started from fallback screen-off detector",
                                        metadata = mapOf(
                                            "startCandidate" to startCandidate.toString()
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "auto sleep start detection error", e)
                                DebugLogger.e(TAG, "Auto sleep start detection error", e)
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(screenReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(screenReceiver, filter)
        }

        // -------------------------------------------------
        // Accelerometer setup for motion heuristics
        // -------------------------------------------------
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        accelerometer?.let {
            sensorManager.registerListener(accelListener, it, SensorManager.SENSOR_DELAY_NORMAL)
            DebugLogger.i(TAG, "Accelerometer listener registered")
        }

        lastMotionMs = System.currentTimeMillis()

        Log.d(TAG, "StepForge watchdog systems active")
        DebugLogger.i(TAG, "StepForge watchdog systems active")

        android.util.Log.e(
            "STEPFORGE_MIDNIGHT",
            "StepCounterService started total=$totalSteps sensor=$lastSensorValue offset=$sensorOffset"
        )
    }


    private suspend fun finalizeAutoSleepSession(
        startMs: Long,
        endMs: Long
    ) {
        if (startMs <= 0L || endMs <= startMs) {
            DebugLogger.w(
                TAG,
                "Auto sleep finalize ignored because timestamps are invalid",
                metadata = mapOf(
                    "startMs" to startMs.toString(),
                    "endMs" to endMs.toString()
                )
            )
            return
        }

        val durationMs = endMs - startMs
        val durationMin = (durationMs / 60_000L).toInt()

        if (durationMin < 20) {
            stepforgeStore.edit {
                it[AUTO_SLEEP_ACTIVE] = false
                it[AUTO_SLEEP_START_TIME] = 0L
                it[PROBABLE_SLEEP_READY] = false
            }

            DebugLogger.d(
                TAG,
                "Auto sleep discarded because duration too short",
                metadata = mapOf(
                    "startMs" to startMs.toString(),
                    "endMs" to endMs.toString(),
                    "durationMin" to durationMin.toString()
                )
            )
            return
        }

        val saveDate = java.time.Instant.ofEpochMilli(endMs)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()
        val qualityScore = calculateSleepQuality(durationMin)
        val sleepDao = AppDatabase.getDatabase(this@StepCounterService).sleepSessionDao()

        sleepDao.insert(
            SleepSession(
                date = saveDate,
                startTime = startMs,
                endTime = endMs,
                totalMinutes = durationMin,
                qualityScore = qualityScore,
                source = "auto"
            )
        )

        stepforgeStore.edit {
            it[AUTO_SLEEP_ACTIVE] = false
            it[AUTO_SLEEP_START_TIME] = 0L
            it[PROBABLE_SLEEP_START] = startMs
            it[PROBABLE_SLEEP_END] = endMs
            it[PROBABLE_SLEEP_READY] = true
            it[REAL_AWAKE_TIME] = endMs
            it[LAST_KNOWN_WAKE_TIME] = endMs
            it[LAST_HEAVY_USAGE] = System.currentTimeMillis()
        }

        DebugLogger.i(
            TAG,
            "Auto sleep saved",
            metadata = mapOf(
                "date" to saveDate,
                "startMs" to startMs.toString(),
                "endMs" to endMs.toString(),
                "durationMin" to durationMin.toString(),
                "qualityScore" to qualityScore.toString(),
                "source" to "auto"
            )
        )
    }

    private fun calculateSleepQuality(durationMin: Int): Int {
        return when {
            durationMin >= 8 * 60 -> 95
            durationMin >= 7 * 60 -> 90
            durationMin >= 6 * 60 -> 84
            durationMin >= 5 * 60 -> 78
            durationMin >= 4 * 60 -> 70
            durationMin >= 2 * 60 -> 60
            else -> 50
        }
    }


    override fun onDestroy() {
        super.onDestroy()

        handler.removeCallbacksAndMessages(null)

        DebugLogger.w(TAG, "StepCounterService onDestroy started")

        try {
            screenReceiver?.let { unregisterReceiver(it) }
            screenReceiver = null
            DebugLogger.d(TAG, "screenReceiver unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Receiver unregister error", e)
            DebugLogger.e(TAG, "Receiver unregister error", e)
        }

        try {
            sensorManager.unregisterListener(this)
            DebugLogger.d(TAG, "step counter listener unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Sensor unregister error", e)
            DebugLogger.e(TAG, "Sensor unregister error", e)
        }

        notifJob?.cancel()
        notifJob = null
        DebugLogger.d(TAG, "notifJob cancelled")

        activityCheckJob?.cancel()
        activityCheckJob = null
        DebugLogger.d(TAG, "activityCheckJob cancelled")

        hourlyTickerJob?.cancel()
        hourlyTickerJob = null
        DebugLogger.d(TAG, "hourlyTickerJob cancelled")

        serviceScope.cancel()
        DebugLogger.d(TAG, "serviceScope cancelled")

        try {
            sensorManager.unregisterListener(accelListener)
            DebugLogger.d(TAG, "accelerometer listener unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Accelerometer unregister error", e)
            DebugLogger.e(TAG, "Accelerometer unregister error", e)
        }

        try {
            sensorManager.unregisterListener(detectorListener)
            DebugLogger.d(TAG, "step detector listener unregistered successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Step detector unregister error", e)
            DebugLogger.e(TAG, "Step detector unregister error", e)
        }

        Log.w(TAG, "Service destroyed.")
        DebugLogger.w(
            TAG,
            "Service destroyed",
            metadata = mapOf(
                "finalTotalSteps" to totalSteps.toString(),
                "lastSensorValue" to lastSensorValue.toString(),
                "sensorOffset" to sensorOffset.toString(),
                "currentDay" to currentDay
            )
        )

        isServiceRunning = false
    }




    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createChannel()

        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e(TAG, "ACTIVITY_RECOGNITION permission missing! Stopping service.")
            DebugLogger.e(
                TAG,
                "ACTIVITY_RECOGNITION permission missing! Stopping service."
            )
            stopSelf()
            return START_NOT_STICKY
        }

        try {
            val action = intent?.action
            val forceSteps = intent?.getIntExtra("force_update_steps", -1) ?: -1
            val forceGoal = intent?.getIntExtra("force_update_goal", -1) ?: -1
            val manual = intent?.getIntExtra("manualSteps", -1) ?: -1
            val forceReset = intent?.getBooleanExtra("forceReset", false) == true

            DebugLogger.d(
                TAG,
                "onStartCommand received",
                metadata = mapOf(
                    "action" to (action ?: "null"),
                    "startId" to startId.toString(),
                    "flags" to flags.toString(),
                    "forceSteps" to forceSteps.toString(),
                    "forceGoal" to forceGoal.toString(),
                    "manualSteps" to manual.toString(),
                    "forceReset" to forceReset.toString(),
                    "currentTotalSteps" to totalSteps.toString(),
                    "lastSensorValue" to lastSensorValue.toString(),
                    "sensorOffset" to sensorOffset.toString()
                )
            )

            if (action == "com.example.stepforge.ACTION_WIDGET_REFRESH_REQUEST") {
                Log.d(TAG, "Widget refresh request. Sending: $totalSteps")
                DebugLogger.d(
                    TAG,
                    "Widget refresh request received",
                    metadata = mapOf(
                        "totalSteps" to totalSteps.toString()
                    )
                )

                StepWidgetProvider.sendStepsUpdate(applicationContext, totalSteps)
                StepWidgetCompactProvider.sendStepsUpdate(applicationContext, totalSteps)
                StepWidgetLargeProvider.sendStepsUpdate(applicationContext, totalSteps)

                DebugLogger.d(
                    TAG,
                    "Widget refresh updates sent",
                    metadata = mapOf(
                        "totalSteps" to totalSteps.toString()
                    )
                )
            }

            if (action == "TEST_NOTIFICATION") {
                Log.d(TAG, "Test bildirimi tetiklendi!")
                DebugLogger.i(TAG, "Test notification action triggered")
                sendAlertNotification("Test Başarılı! 🎉", "Bu bir deneme bildirimidir.")
            }

            if (forceSteps >= 0) {
                if (totalSteps < forceSteps) {
                    Log.d(TAG, "UI Zorla Güncelleme: Eski=$totalSteps, Yeni=$forceSteps")
                    DebugLogger.i(
                        TAG,
                        "Force update steps applied",
                        metadata = mapOf(
                            "oldTotalSteps" to totalSteps.toString(),
                            "newTotalSteps" to forceSteps.toString(),
                            "lastSensorValue" to lastSensorValue.toString()
                        )
                    )

                    totalSteps = forceSteps

                    if (lastSensorValue != 0) {
                        sensorOffset = totalSteps - lastSensorValue
                        Log.d(TAG, "Offset recalculated: $sensorOffset (LastSensor=$lastSensorValue)")
                        DebugLogger.d(
                            TAG,
                            "Offset recalculated after force update",
                            metadata = mapOf(
                                "sensorOffset" to sensorOffset.toString(),
                                "lastSensorValue" to lastSensorValue.toString(),
                                "totalSteps" to totalSteps.toString()
                            )
                        )
                    }

                    StepEvents.emitTodaySteps(totalSteps)
                    DebugLogger.d(
                        TAG,
                        "StepEvents emitted after force update",
                        metadata = mapOf(
                            "todaySteps" to totalSteps.toString()
                        )
                    )

                    saveContinuously(totalSteps)
                }

                serviceScope.launch {
                    val notif = buildNotification(totalSteps)
                    safeStartForeground(notif)
                }
            } else {
                serviceScope.launch {
                    val notif = buildNotification(totalSteps)
                    safeStartForeground(notif)
                }
            }

            if (manual >= 0) {
                Log.d(TAG, "Manuel Ayarlama: $manual")
                DebugLogger.i(
                    TAG,
                    "Manual steps override received",
                    metadata = mapOf(
                        "manualSteps" to manual.toString(),
                        "previousTotalSteps" to totalSteps.toString(),
                        "lastSensorValue" to lastSensorValue.toString()
                    )
                )

                sensorOffset = manual - lastSensorValue
                totalSteps = manual

                DebugLogger.d(
                    TAG,
                    "Manual override applied to in-memory state",
                    metadata = mapOf(
                        "sensorOffset" to sensorOffset.toString(),
                        "totalSteps" to totalSteps.toString()
                    )
                )

                StepEvents.emitTodaySteps(manual)
                DebugLogger.d(
                    TAG,
                    "StepEvents emitted after manual override",
                    metadata = mapOf(
                        "todaySteps" to manual.toString()
                    )
                )

                checkGoalCelebrate(manual)
                updateNotifAsync(manual)
                saveContinuously(manual)

                serviceScope.launch {
                    try {
                        dao.insertDailySteps(DailySteps(todayKey(), manual))
                        DebugLogger.d(
                            TAG,
                            "Manual daily steps saved to database",
                            metadata = mapOf(
                                "date" to todayKey(),
                                "steps" to manual.toString()
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Manual saveDaily failed", e)
                        DebugLogger.e(TAG, "Manual saveDaily failed", e)
                        stepSafetyGuard.registerError("manual_save_daily_failed")
                    }
                }

                StepWidgetProvider.sendStepsUpdate(applicationContext, manual)
                StepWidgetCompactProvider.sendStepsUpdate(applicationContext, manual)
                StepWidgetLargeProvider.sendStepsUpdate(applicationContext, manual)

                DebugLogger.d(
                    TAG,
                    "Widgets updated after manual override",
                    metadata = mapOf(
                        "steps" to manual.toString()
                    )
                )
            }

            if (forceReset) {
                DebugLogger.w(TAG, "forceReset requested from intent")
                resetForNewDay()
            }

            if (stepSensor == null) {
                sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
                stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)

                DebugLogger.d(
                    TAG,
                    "stepSensor was null, requested again from SensorManager",
                    metadata = mapOf(
                        "stepSensorExists" to (stepSensor != null).toString()
                    )
                )
            }

            stepSensor?.let { sensor ->
                try {
                    val sinceLast = System.currentTimeMillis() - lastSensorEventTime
                    if (sinceLast > 25_000L) {
                        sensorManager.unregisterListener(this)
                        sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL)

                        Log.w(
                            TAG,
                            "Sensor listener re-registered in onStartCommand (no events for ${sinceLast}ms)."
                        )
                        DebugLogger.w(
                            TAG,
                            "Sensor listener re-registered in onStartCommand",
                            metadata = mapOf(
                                "sinceLastSensorEventMs" to sinceLast.toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sensor register error in onStartCommand", e)
                    DebugLogger.e(TAG, "Sensor register error in onStartCommand", e)
                    stepSafetyGuard.registerError("sensor_register_on_start_command_failed")
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "onStartCommand crash prevented", e)
            DebugLogger.e(TAG, "onStartCommand crash prevented", e)
            stepSafetyGuard.registerError("on_start_command_crash_prevented")
        }
        return START_STICKY
    }


    override fun onBind(intent: Intent?): IBinder? = null
    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {


            Sensor.TYPE_STEP_COUNTER -> {

                val currentCounter = event.values[0].toInt()
                val now = System.currentTimeMillis()

                if (lastCounterValueForMotion != 0) {
                    val diff = currentCounter - lastCounterValueForMotion
                    val timeDiff = now - lastCounterCheckTime

                    // 🔥 Fallback step detection
                    if (diff > 0 && diff < 20) {
                        // çok büyük sıçrama değil → gerçek adım kabul et

                        DebugLogger.d(
                            TAG,
                            "Fallback step detected (STEP_COUNTER)",
                            metadata = mapOf(
                                "diff" to diff.toString(),
                                "timeDiffMs" to timeDiff.toString()
                            )
                        )
                    }
                }

                lastCounterValueForMotion = currentCounter
                lastCounterCheckTime = now

                if (stepSensor == null) return

                DebugLogger.d(
                    TAG,
                    "Sensor event received",
                    metadata = mapOf(
                        "sensorType" to (event.sensor?.type?.toString() ?: "null"),
                        "rawValue" to (event.values.getOrNull(0)?.toString() ?: "null"),
                        "lastSensorValue" to lastSensorValue.toString(),
                        "totalSteps" to totalSteps.toString(),
                        "offset" to sensorOffset.toString()
                    )
                )

                val sensorVal = event.values[0].toInt()
                val nowMs = System.currentTimeMillis()
                lastSensorEventTime = nowMs

                if (lastSensorValue == 0 || sensorVal < lastSensorValue) {
                    sensorOffset = if (totalSteps == 0) -sensorVal else totalSteps - sensorVal
                    Log.d(
                        TAG,
                        "Sensor baseline/offset adjusted. sensor=$sensorVal offset=$sensorOffset total=$totalSteps"
                    )
                    DebugLogger.i(
                        TAG,
                        "Sensor baseline/offset adjusted",
                        metadata = mapOf(
                            "sensorVal" to sensorVal.toString(),
                            "offset" to sensorOffset.toString(),
                            "totalSteps" to totalSteps.toString()
                        )
                    )
                }

                val today = todayKey()
                if (today != lastDateDebug) {
                    midnightDebug(
                        reason = "DATE CHANGED",
                        steps = totalSteps
                    )
                    DebugLogger.i(
                        TAG,
                        "Date changed detected in sensor stream",
                        metadata = mapOf(
                            "today" to today,
                            "lastDateDebug" to lastDateDebug,
                            "totalSteps" to totalSteps.toString()
                        )
                    )
                    lastDateDebug = today
                }

                if (kotlin.math.abs(sensorVal - lastSensorValue) > 2000) {
                    Log.w(TAG, "Sensor jump detected, recalibrating offset")
                    DebugLogger.w(
                        TAG,
                        "Sensor jump detected, recalibrating offset",
                        metadata = mapOf(
                            "sensorVal" to sensorVal.toString(),
                            "lastSensorValue" to lastSensorValue.toString(),
                            "totalSteps" to totalSteps.toString()
                        )
                    )
                    sensorOffset = totalSteps - sensorVal
                }

                val previousSensorValue = lastSensorValue
                lastSensorValue = sensorVal

                if (sensorVal < previousSensorValue) {
                    midnightDebug(
                        reason = "SENSOR COUNTER RESET",
                        steps = sensorVal
                    )
                    DebugLogger.w(
                        TAG,
                        "Sensor counter reset detected",
                        metadata = mapOf(
                            "sensorVal" to sensorVal.toString(),
                            "previousSensorValue" to previousSensorValue.toString()
                        )
                    )
                }

                val calculatedSteps = sensorOffset + sensorVal
                val previousTotal = totalSteps
                val diff = calculatedSteps - previousTotal
                val detectorAgeMs = nowMs - lastDetectorStepTime
                val motionAgeMs = nowMs - lastMotionMs

                if (detectorEverWorked && detectorAgeMs > 6_000L) {
                    DebugLogger.w(
                        TAG,
                        "Step detector has not reported recently, but counting will continue from step counter",
                        metadata = mapOf(
                            "detectorAgeMs" to detectorAgeMs.toString(),
                            "lastDetectorStepTime" to lastDetectorStepTime.toString()
                        )
                    )
                }

                if (diff in 1..80) {
                    // normal
                } else if (diff > 80) {
                    DebugLogger.w(TAG, "Abnormal step burst ignored")
                    return
                }

                if (calculatedSteps > previousTotal) {
                    val now = System.currentTimeMillis()
                    val stopThresholdMs = WALK_SESSION_STOP_THRESHOLD_MS
                    val previousStepTime = lastStepTime
                    val stepGapMs = now - previousStepTime

                    if (stepGapMs < 180L) {
                        DebugLogger.w(
                            TAG,
                            "Rapid step burst accepted from hardware counter",
                            metadata = mapOf(
                                "stepGapMs" to stepGapMs.toString(),
                                "diff" to diff.toString(),
                                "calculatedSteps" to calculatedSteps.toString()
                            )
                        )
                    }

                    if (motionAgeMs > MOTION_STEP_TIMEOUT) {
                        DebugLogger.w(
                            TAG,
                            "Step accepted even though motion watchdog is stale",
                            metadata = mapOf(
                                "motionAgeMs" to motionAgeMs.toString(),
                                "timeoutMs" to MOTION_STEP_TIMEOUT.toString(),
                                "diff" to diff.toString()
                            )
                        )
                    }

                    if (isWalking && (now - previousStepTime > stopThresholdMs)) {
                        Log.d(TAG, "Walking session timeout reached, finishing previous session")
                        DebugLogger.i(
                            TAG,
                            "Walking session timeout reached, finishing previous session",
                            metadata = mapOf(
                                "now" to now.toString(),
                                "lastStepTime" to previousStepTime.toString(),
                                "stopThresholdMs" to stopThresholdMs.toString()
                            )
                        )
                        finishWalkingSession()                    }

                    totalSteps = calculatedSteps
                    lastStepTime = now
                    lastMotionMs = now


//                    val events = sessionEngine.onStep(now, totalSteps)
//
//                    for (event in events) {
//                        when (event) {
//                            is WorkoutSessionTransition.Started -> {
//                                isWalking = true
//                                sessionStartSteps = event.startSteps
//                                sessionStartTime = event.startTimeMs
//                            }
//                            is WorkoutSessionTransition.Finished -> {
//                                finishWalkingSession(event.endTimeMs)
//                            }
//                        }
//                    }


                    Log.d(
                        TAG,
                        "Step accepted: prev=$previousTotal new=$totalSteps sensor=$sensorVal diff=$diff offset=$sensorOffset"
                    )
                    DebugLogger.i(
                        TAG,
                        "Step accepted",
                        metadata = mapOf(
                            "previousTotal" to previousTotal.toString(),
                            "newTotal" to totalSteps.toString(),
                            "calculatedSteps" to calculatedSteps.toString(),
                            "sensorVal" to sensorVal.toString(),
                            "offset" to sensorOffset.toString(),
                            "diff" to diff.toString(),
                            "stepGapMs" to stepGapMs.toString()
                        )
                    )



                    if (diff > 0) {
                        if (!isWalking) {
                            isWalking = true
                            sessionStartSteps = previousTotal
                            sessionStartTime = now
                            Log.d(TAG, "Yürüyüş başladı: $sessionStartSteps")
                            DebugLogger.i(
                                TAG,
                                "Walking session started",
                                metadata = mapOf(
                                    "sessionStartSteps" to sessionStartSteps.toString(),
                                    "sessionStartTime" to sessionStartTime.toString()
                                )
                            )
                        }

                        serviceScope.launch {
                            try {
                                stepforgeStore.edit { prefs ->
                                    prefs[LAST_STEP_TIME] = now
                                    prefs[LAST_MOTION_TIME] = now
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "write LAST_STEP_TIME / LAST_MOTION_TIME error", e)
                                DebugLogger.e(
                                    TAG,
                                    "Failed to write LAST_STEP_TIME / LAST_MOTION_TIME",
                                    throwable = e
                                )
                                stepSafetyGuard.registerError("write_last_step_time_failed")
                            }
                        }
                    }

                    StepEvents.emitTodaySteps(totalSteps)
                    DebugLogger.d(
                        TAG,
                        "StepEvents emitted",
                        metadata = mapOf("todaySteps" to totalSteps.toString())
                    )

                    checkGoalCelebrate(totalSteps)
                    maybeUpdateNotificationThrottled()
                    maybePersistStepsThrottled()
                    maybeUpdateWidgetsThrottled()
                    maybeRunPremiumCoach()

                    DebugLogger.d(
                        TAG,
                        "Widgets updated after step accept",
                        metadata = mapOf("todaySteps" to totalSteps.toString())
                    )



                } else if (calculatedSteps < previousTotal) {
                    sensorOffset = previousTotal - sensorVal
                    Log.d(TAG, "Offset fix: sensor=$sensorVal newOffset=$sensorOffset")
                    DebugLogger.w(
                        TAG,
                        "Offset corrected because calculatedSteps < previousTotal",
                        metadata = mapOf(
                            "sensorVal" to sensorVal.toString(),
                            "previousTotal" to previousTotal.toString(),
                            "newOffset" to sensorOffset.toString(),
                            "calculatedSteps" to calculatedSteps.toString()
                        )
                    )
                } else {
                    DebugLogger.d(
                        TAG,
                        "Sensor event produced no step change",
                        metadata = mapOf(
                            "sensorVal" to sensorVal.toString(),
                            "calculatedSteps" to calculatedSteps.toString(),
                            "totalSteps" to totalSteps.toString()
                        )
                    )
                }
            }
        }
    }

    private fun startSensorWatchdog() {
        DebugLogger.i(TAG, "Sensor watchdog started")

        serviceScope.launch {
            while (isActive) {
                try {
                    val diff = System.currentTimeMillis() - lastSensorEventTime
                    val softBlockActive = stepSafetyGuard.isSoftBlockActive()

                    if (diff > 20_000L) {
                        Log.w(TAG, "Sensor watchdog restarting listener")
                        DebugLogger.w(
                            TAG,
                            "Sensor watchdog restarting listener",
                            metadata = mapOf(
                                "diffSinceLastSensorEventMs" to diff.toString(),
                                "stepSensorExists" to (stepSensor != null).toString(),
                                "softBlockActive" to softBlockActive.toString()
                            )
                        )

                        if (!softBlockActive) {
                            try {
                                sensorManager.unregisterListener(this@StepCounterService)
                                DebugLogger.d(TAG, "Sensor watchdog unregistered old listener")
                            } catch (e: Exception) {
                                Log.e(TAG, "Sensor watchdog unregister failed", e)
                                DebugLogger.e(TAG, "Sensor watchdog unregister failed", e)
                                stepSafetyGuard.registerError("sensor_watchdog_unregister_failed")
                            }

                            stepSensor?.let { sensor ->
                                try {
                                    sensorManager.registerListener(
                                        this@StepCounterService,
                                        sensor,
                                        SensorManager.SENSOR_DELAY_NORMAL
                                    )
                                    DebugLogger.i(
                                        TAG,
                                        "Sensor watchdog registered listener again",
                                        metadata = mapOf(
                                            "sensorType" to sensor.type.toString()
                                        )
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Sensor watchdog register failed", e)
                                    DebugLogger.e(TAG, "Sensor watchdog register failed", e)
                                    stepSafetyGuard.registerError("sensor_watchdog_register_failed")
                                }
                            }
                        } else {
                            DebugLogger.w(
                                TAG,
                                "Sensor watchdog skipped re-register because soft block is active",
                                metadata = mapOf(
                                    "diffSinceLastSensorEventMs" to diff.toString()
                                )
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Sensor watchdog error", e)
                    DebugLogger.e(TAG, "Sensor watchdog error", e)
                    stepSafetyGuard.registerError("sensor_watchdog_loop_error")
                }

                delay(20_000L)
            }

            DebugLogger.w(TAG, "Sensor watchdog stopped because coroutine is no longer active")
        }
    }

    @Volatile
    private var lastNotifiedSteps: Int = -1

    private fun updateNotifAsync(sum: Int) {
        if (sum == lastNotifiedSteps) {
            DebugLogger.d(
                TAG,
                "Notification update skipped because value did not change",
                metadata = mapOf(
                    "sum" to sum.toString(),
                    "lastNotifiedSteps" to lastNotifiedSteps.toString()
                )
            )
            return
        }

        lastNotifiedSteps = sum

        DebugLogger.d(
            TAG,
            "Notification update scheduled",
            metadata = mapOf(
                "sum" to sum.toString()
            )
        )

        serviceScope.launch {
            try {
                val notif = buildNotification(sum)
                val started = safeStartForeground(notif)

                DebugLogger.d(
                    TAG,
                    "Notification update executed",
                    metadata = mapOf(
                        "sum" to sum.toString(),
                        "foregroundStarted" to started.toString()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "updateNotifAsync failed", e)
                DebugLogger.e(TAG, "updateNotifAsync failed", e)
            }
        }
    }

    private fun buildNotification(sum: Int): Notification {
        val prefGoal = cachedGoal.coerceIn(1000, 100000)

        val fmtSteps = formatInt(sum)
        val fmtGoal = formatInt(prefGoal)

        val expandedText = """
$fmtSteps steps today

Daily goal: $fmtGoal steps
Keep walking 🚶
""".trimIndent()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle("StepForge")
            .setContentText("$fmtSteps steps / Goal $fmtGoal")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(expandedText)
            )
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setForegroundServiceBehavior(
                NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE
            )
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }


    private fun saveContinuously(sum: Int) {
        serviceScope.launch {
            val persistedKey = intPreferencesKey("persisted_total_sum")
            stepforgeStore.edit {
                it[PREF_LAST_SENSOR] = lastSensorValue
                it[PREF_OFFSET] = sensorOffset
                it[PREF_TOTAL] = sum
                it[persistedKey] = sum
            }
        }
    }

    private fun saveDaily(day: String, sum: Int) {
        serviceScope.launch {
            dao.insertDailySteps(DailySteps(day, sum))
        }
    }

    private fun saveHourlySnapshot(newTotalSteps: Int) {
        val now = Calendar.getInstance()
        val hourNow = now.get(Calendar.HOUR_OF_DAY)
        val dateStr = todayKey()

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
                DebugLogger.d(
                    TAG,
                    "Hourly snapshot upserted",
                    metadata = mapOf(
                        "date" to dateStr,
                        "hour" to hourNow.toString(),
                        "totalSteps" to newTotalSteps.toString()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "saveHourlySnapshot error", e)
                DebugLogger.e(
                    TAG,
                    "saveHourlySnapshot error",
                    throwable = e,
                    metadata = mapOf(
                        "date" to dateStr,
                        "hour" to hourNow.toString(),
                        "totalSteps" to newTotalSteps.toString()
                    )
                )
            }
        }
    }

    private suspend fun hasResetToday(): Boolean {
        val today = currentDay
        val prefs = stepforgeStore.data.first()
        return (prefs[LAST_RESET_DATE] ?: "") == today
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
        android.util.Log.e(
            "STEPFORGE_MIDNIGHT",
            "resetForNewDay() CALLED steps=$totalSteps sensor=$lastSensorValue offset=$sensorOffset"
        )

        DebugLogger.w(
            TAG,
            "resetForNewDay called",
            metadata = mapOf(
                "totalSteps" to totalSteps.toString(),
                "lastSensorValue" to lastSensorValue.toString(),
                "sensorOffset" to sensorOffset.toString(),
                "currentDay" to currentDay
            )
        )

        serviceScope.launch(Dispatchers.IO) {
            val today = currentDay

            if (hasResetToday()) {
                Log.w(TAG, "resetForNewDay ignored: already reset for today=$today")
                DebugLogger.w(
                    TAG,
                    "resetForNewDay ignored because reset already done today",
                    metadata = mapOf(
                        "today" to today
                    )
                )
                return@launch
            }

            try {
                stepforgeStore.edit { p ->
                    p[LAST_RESET_DATE] = today
                }
                DebugLogger.d(
                    TAG,
                    "LAST_RESET_DATE guard written",
                    metadata = mapOf(
                        "today" to today
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Failed to set LAST_RESET_DATE guard", e)
                DebugLogger.e(
                    TAG,
                    "Failed to set LAST_RESET_DATE guard",
                    throwable = e,
                    metadata = mapOf(
                        "today" to today
                    )
                )
            }

            try {
                withContext(Dispatchers.Main) {
                    try {
                        sensorManager.unregisterListener(this@StepCounterService)
                        DebugLogger.d(TAG, "Sensor listener unregistered before day reset")
                    } catch (e: Exception) {
                        Log.e(TAG, "Sensor unregister during reset failed", e)
                        DebugLogger.e(TAG, "Sensor unregister during reset failed", e)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Main dispatcher sensor unregister block failed", e)
                DebugLogger.e(TAG, "Main dispatcher sensor unregister block failed", e)
            }

            val dayToSave = currentDay
            val currentSensorValue = lastSensorValue
            val stepsToSave = totalSteps
            saveTomorrowShieldFromTodaySteps(stepsToSave)

            midnightDebug(reason = "RESET DAY CALLED", steps = stepsToSave)
            Log.w(TAG, "RESET DAY triggered. Saving day=$dayToSave totalSteps=$stepsToSave")
            DebugLogger.w(
                TAG,
                "RESET DAY triggered",
                metadata = mapOf(
                    "dayToSave" to dayToSave,
                    "stepsToSave" to stepsToSave.toString(),
                    "currentSensorValue" to currentSensorValue.toString()
                )
            )

            hasCelebrated = false

            try {
                dao.insertDailySteps(DailySteps(dayToSave, stepsToSave))
                DebugLogger.i(
                    TAG,
                    "Previous day steps saved before reset",
                    metadata = mapOf(
                        "date" to dayToSave,
                        "steps" to stepsToSave.toString()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "MidnightReset DB store error", e)
                DebugLogger.e(
                    TAG,
                    "MidnightReset DB store error",
                    throwable = e,
                    metadata = mapOf(
                        "date" to dayToSave,
                        "steps" to stepsToSave.toString()
                    )
                )
            }

            try {
                stepforgeStore.edit {
                    it[PREF_LAST_SENSOR] = 0
                    it[PREF_OFFSET] = -currentSensorValue
                    it[PREF_TOTAL] = 0
                }

                val persistedKey = intPreferencesKey("persisted_total_sum")
                stepforgeStore.edit { prefs ->
                    prefs[persistedKey] = 0
                }

                DebugLogger.i(
                    TAG,
                    "Datastore reset completed for new day",
                    metadata = mapOf(
                        "currentSensorValue" to currentSensorValue.toString()
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "MidnightReset datastore write error", e)
                DebugLogger.e(
                    TAG,
                    "MidnightReset datastore write error",
                    throwable = e
                )
            }

            currentDay = todayKey()
            totalSteps = 0
            sensorOffset = -currentSensorValue
            lastSensorValue = currentSensorValue
            lastResetMs = System.currentTimeMillis()
            activateTodayShieldFromStoredTomorrow()

            stepforgeStore.edit {
                it[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] = ""
            }

            DebugLogger.i(
                TAG,
                "In-memory reset state applied",
                metadata = mapOf(
                    "currentDay" to currentDay,
                    "totalSteps" to totalSteps.toString(),
                    "sensorOffset" to sensorOffset.toString(),
                    "lastSensorValue" to lastSensorValue.toString(),
                    "lastResetMs" to lastResetMs.toString()
                )
            )

            try {
                saveHourlySnapshot(0)
                DebugLogger.d(TAG, "Hourly zero snapshot requested after reset")
            } catch (e: Exception) {
                Log.e(TAG, "Hourly zero snapshot request failed", e)
                DebugLogger.e(TAG, "Hourly zero snapshot request failed", e)
            }

            StepEvents.emitTodaySteps(0)
            DebugLogger.d(TAG, "StepEvents emitted with zero after reset")

            updateNotifAsync(0)

            StepWidgetProvider.sendStepsUpdate(applicationContext, 0)
            StepWidgetCompactProvider.sendStepsUpdate(applicationContext, 0)
            StepWidgetLargeProvider.sendStepsUpdate(applicationContext, 0)

            DebugLogger.d(TAG, "All widgets updated with zero after reset")

            try {
                withContext(Dispatchers.Main) {
                    scheduleMidnightReset()
                }
                DebugLogger.d(TAG, "Next midnight reset scheduled after reset")
            } catch (e: Exception) {
                Log.e(TAG, "Scheduling next midnight reset failed", e)
                DebugLogger.e(TAG, "Scheduling next midnight reset failed", e)
            }

            withContext(Dispatchers.Main) {
                delay(1200)
                stepSensor?.let {
                    try {
                        sensorManager.registerListener(
                            this@StepCounterService,
                            it,
                            SensorManager.SENSOR_DELAY_NORMAL
                        )
                        DebugLogger.i(
                            TAG,
                            "Sensor listener re-registered after reset",
                            metadata = mapOf(
                                "sensorType" to it.type.toString()
                            )
                        )
                    } catch (e: Exception) {
                        Log.e(TAG, "Sensor register failed after reset", e)
                        DebugLogger.e(TAG, "Sensor register failed after reset", e)
                    }
                }
            }

            startHourlyTicker()
            DebugLogger.i(TAG, "Hourly ticker restarted after reset")
        }
    }


    private fun scheduleMidnightReset() {
        try {
            val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmMgr.canScheduleExactAlarms()) {
                    Log.w(TAG, "Exact alarm permission not granted.")
                    DebugLogger.w(
                        TAG,
                        "Exact alarm permission not granted. Midnight reset scheduling skipped."
                    )
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
                this,
                0,
                intent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            alarmMgr.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                nextMidnight.timeInMillis,
                pi
            )

            Log.d(TAG, "Midnight reset scheduled for ${Date(nextMidnight.timeInMillis)}")
            DebugLogger.i(
                TAG,
                "Midnight reset scheduled",
                metadata = mapOf(
                    "triggerAtMillis" to nextMidnight.timeInMillis.toString(),
                    "triggerAtText" to Date(nextMidnight.timeInMillis).toString()
                )
            )
        } catch (e: SecurityException) {
            Log.e(TAG, "Cannot schedule exact alarm: ${e.message}")
            DebugLogger.e(
                TAG,
                "Cannot schedule exact alarm: ${e.message}",
                throwable = e
            )
        } catch (e: Exception) {
            Log.e(TAG, "scheduleMidnightReset error: ${e.message}")
            DebugLogger.e(
                TAG,
                "scheduleMidnightReset error: ${e.message}",
                throwable = e
            )
        }
    }

    private fun startHourlyTicker() {
        hourlyTickerJob?.cancel()
        DebugLogger.d(TAG, "Existing hourlyTickerJob cancelled before restart")

        hourlyTickerJob = serviceScope.launch(Dispatchers.IO) {
            DebugLogger.i(TAG, "Hourly ticker started")

            while (isActive) {
                val now = Calendar.getInstance()

                val nextHour = Calendar.getInstance().apply {
                    timeInMillis = now.timeInMillis
                    add(Calendar.HOUR_OF_DAY, 1)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                val delayMs = (nextHour.timeInMillis - System.currentTimeMillis())
                    .coerceIn(1000L, 60 * 60 * 1000L)

                DebugLogger.d(
                    TAG,
                    "Hourly ticker waiting for next hour",
                    metadata = mapOf(
                        "delayMs" to delayMs.toString(),
                        "nextHourMillis" to nextHour.timeInMillis.toString(),
                        "nextHourText" to Date(nextHour.timeInMillis).toString()
                    )
                )

                delay(delayMs)

                try {
                    android.util.Log.e(
                        "STEPFORGE_MIDNIGHT",
                        "Hourly ticker snapshot triggered hour=${Calendar.getInstance().get(Calendar.HOUR_OF_DAY)} steps=$totalSteps"
                    )

                    val hNow = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

                    if (hNow == 0) {
                        Log.w(TAG, "Hourly ticker: midnight hour=0, skipping snapshot to avoid race with reset.")
                        DebugLogger.w(
                            TAG,
                            "Hourly ticker skipped snapshot at midnight to avoid race with reset",
                            metadata = mapOf(
                                "hourNow" to hNow.toString(),
                                "totalSteps" to totalSteps.toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Hourly ticker error", e)
                    DebugLogger.e(TAG, "Hourly ticker error", e)
                }
            }

            DebugLogger.w(TAG, "Hourly ticker stopped because coroutine is no longer active")
        }
    }

    private fun checkGoalCelebrate(current: Int) {
        serviceScope.launch {
            try {
                val goal = getLatestGoal()

                DebugLogger.d(
                    TAG,
                    "checkGoalCelebrate invoked",
                    metadata = mapOf(
                        "currentSteps" to current.toString(),
                        "goal" to goal.toString(),
                        "hasCelebrated" to hasCelebrated.toString()
                    )
                )

                if (current >= goal && !hasCelebrated && goal > 1000) {
                    hasCelebrated = true

                    Log.d(TAG, "Goal celebration triggered. current=$current goal=$goal")
                    DebugLogger.i(
                        TAG,
                        "Goal celebration triggered",
                        metadata = mapOf(
                            "currentSteps" to current.toString(),
                            "goal" to goal.toString()
                        )
                    )

                    sendBroadcast(Intent(ACTION_GOAL_COMPLETED))
                    DebugLogger.d(TAG, "ACTION_GOAL_COMPLETED broadcast sent")

                    sendAlertNotification(
                        "Goal Reached! 🏆",
                        "Congratulations! You've reached your daily goal of $goal steps."
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "checkGoalCelebrate error", e)
                DebugLogger.e(TAG, "checkGoalCelebrate error", e)
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
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                setShowBadge(false)
                enableVibration(false)
                enableLights(false)
            }

            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun formatInt(value: Int): String {
        return String.format(Locale.getDefault(), "%,d", value)
    }

    private fun todayKey(): String {
        val zone = java.time.ZoneId.systemDefault()
        val today = java.time.LocalDate.now(zone)
        return today.toString() // yyyy-MM-dd
    }

    private fun startActivityMonitoring() {
        activityCheckJob?.cancel()

        activityCheckJob = serviceScope.launch {
            while (isActive) {
                val now = System.currentTimeMillis()
                val stopThresholdMs = WALK_SESSION_STOP_THRESHOLD_MS

                if (isWalking && (now - lastStepTime > stopThresholdMs)) {
                    Log.d(TAG, "Walking session inactivity threshold reached, finishing session")
                    DebugLogger.d(
                        TAG,
                        "Walking session inactivity threshold reached, finishing session",
                        metadata = mapOf(
                            "now" to now.toString(),
                            "lastStepTime" to lastStepTime.toString(),
                            "stopThresholdMs" to stopThresholdMs.toString()
                        )
                    )
                    finishWalkingSession()                }

                val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                if (hour in 10..21) {
                    if (now - lastStepTime > INACTIVITY_THRESHOLD_MS) {
                        Log.d(TAG, "Inactivity alert triggered")
                        DebugLogger.d(
                            TAG,
                            "Inactivity alert triggered",
                            metadata = mapOf(
                                "hour" to hour.toString(),
                                "now" to now.toString(),
                                "lastStepTime" to lastStepTime.toString(),
                                "inactivityThresholdMs" to INACTIVITY_THRESHOLD_MS.toString()
                            )
                        )

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

    private fun estimateDistanceMeters(steps: Int): Int {
        return (steps * 0.75f).roundToInt()
    }

    private fun estimateCaloriesKcal(steps: Int): Int {
        return (steps * 0.04f).roundToInt()
    }

    private suspend fun saveWorkoutSession(
        startTime: Long,
        endTime: Long,
        stepsTaken: Int,
        durationMin: Int
    ): Long {

        val dateStr = java.time.Instant.ofEpochMilli(startTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        val distanceMeters = estimateDistanceMeters(stepsTaken)
        val calories = estimateCaloriesKcal(stepsTaken)
        val avgSpm = if (durationMin > 0) stepsTaken / durationMin else 0

        val session = WorkoutSession(
            date = dateStr,
            startTime = startTime,
            endTime = endTime,
            durationMinutes = durationMin,
            steps = stepsTaken,
            distanceMeters = distanceMeters,
            caloriesKcal = calories,
            avgStepsPerMinute = avgSpm,
            source = "auto"
        )

        val id = AppDatabase.getDatabase(applicationContext)
            .workoutSessionDao()
            .insert(session)

        DebugLogger.i(
            TAG,
            "Workout saved (fallback)",
            metadata = mapOf(
                "steps" to stepsTaken.toString(),
                "durationMin" to durationMin.toString(),
                "distanceMeters" to distanceMeters.toString(),
                "calories" to calories.toString(),
                "avgSpm" to avgSpm.toString()
            )
        )

        return id
    }

    private fun finishWalkingSession(sessionEndTimeMs: Long? = null) {

        if (sessionStartTime <= 0L) {
            isWalking = false
            return
        }

        val endTime = sessionEndTimeMs ?: System.currentTimeMillis()

        val stepsTaken = (totalSteps - sessionStartSteps).coerceAtLeast(0)
        val durationMs = (endTime - sessionStartTime).coerceAtLeast(0L)
        val durationMin = (durationMs / 1000 / 60).toInt().coerceAtLeast(1)

        DebugLogger.d(
            TAG,
            "Session finished (v3)",
            metadata = mapOf(
                "steps" to stepsTaken.toString(),
                "durationMin" to durationMin.toString()
            )
        )

        // 🔥 filtre (çok önemli)
        val shouldSave = durationMs >= 3 * 60_000L && stepsTaken >= 500

        if (shouldSave) {
            serviceScope.launch(Dispatchers.IO) {
                try {

                    val sessionId = saveWorkoutSession(
                        startTime = sessionStartTime,
                        endTime = endTime,
                        stepsTaken = stepsTaken,
                        durationMin = durationMin
                    )

                    DebugLogger.i(TAG, "Workout stored (v3)")

                    withContext(Dispatchers.Main) {
                        sendWorkoutSummaryNotification(
                            sessionId = sessionId,
                            stepsTaken = stepsTaken,
                            durationMin = durationMin
                        )
                    }

                } catch (e: Exception) {
                    DebugLogger.e(TAG, "Workout save failed", e)
                }
            }
        } else {
            DebugLogger.d(TAG, "Workout ignored (too small)")
        }

        isWalking = false
        sessionStartTime = 0L
        sessionStartSteps = totalSteps
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

    private fun sendWorkoutSummaryNotification(
        sessionId: Long,
        stepsTaken: Int,
        durationMin: Int
    ) {
        try {
            val openIntent = Intent(this, WorkoutsActivity::class.java).apply {
                putExtra("session_id", sessionId)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            val pendingIntent = PendingIntent.getActivity(
                this,
                sessionId.toInt(),
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val title = "Workout completed"
            val message = "You walked $stepsTaken steps in $durationMin minutes."

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_walk)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        "You completed a walking workout with $stepsTaken steps in $durationMin minutes. Tap to view details."
                    )
                )
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            notificationManager.notify(
                (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
                notification
            )

            Log.d(TAG, "Workout notification sent. sessionId=$sessionId")
            DebugLogger.d(
                TAG,
                "Workout notification sent",
                metadata = mapOf(
                    "sessionId" to sessionId.toString(),
                    "stepsTaken" to stepsTaken.toString(),
                    "durationMin" to durationMin.toString(),
                    "title" to title,
                    "message" to message
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Workout notification failed", e)
            DebugLogger.e(
                TAG,
                "Workout notification failed",
                throwable = e,
                metadata = mapOf(
                    "sessionId" to sessionId.toString(),
                    "stepsTaken" to stepsTaken.toString(),
                    "durationMin" to durationMin.toString()
                )
            )
        }
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
        DebugLogger.w(
            TAG,
            "onTaskRemoved triggered, scheduling service restart",
            metadata = mapOf(
                "totalSteps" to totalSteps.toString(),
                "lastSensorValue" to lastSensorValue.toString(),
                "sensorOffset" to sensorOffset.toString(),
                "isServiceRunning" to isServiceRunning.toString()
            )
        )

        val restartIntent = Intent(applicationContext, StepCounterService::class.java).apply {
            setPackage(packageName)
        }

        val pendingIntent = PendingIntent.getService(
            applicationContext,
            2001,
            restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            SystemClock.elapsedRealtime() + 3000,
            pendingIntent
        )

        DebugLogger.d(
            TAG,
            "Restart alarm scheduled from onTaskRemoved",
            metadata = mapOf(
                "triggerAfterMs" to "3000"
            )
        )
    }

    private fun startNotificationUpdater() {
        if (notifJob != null) {
            DebugLogger.d(TAG, "startNotificationUpdater skipped because notifJob already exists")
            return
        }

        notifJob = serviceScope.launch {
            DebugLogger.i(TAG, "Notification updater started")

            while (isActive) {
                try {
                    val goal = getLatestGoal()
                    val softBlockActive = stepSafetyGuard.isSoftBlockActive()

                    if (!softBlockActive) {
                        val notification = buildNotification(totalSteps)

                        safeStartForeground(notification)

                        // StepWidgetProvider.sendStepsUpdate(applicationContext, totalSteps)
                        // StepWidgetCompactProvider.sendStepsUpdate(applicationContext, totalSteps)
                        // StepWidgetLargeProvider.sendStepsUpdate(applicationContext, totalSteps)

                        Log.d(TAG, "Notification auto-updated: steps=$totalSteps goal=$goal")
                        DebugLogger.d(
                            TAG,
                            "Notification auto-updated",
                            metadata = mapOf(
                                "totalSteps" to totalSteps.toString(),
                                "goal" to goal.toString(),
                                "softBlockActive" to "false"
                            )
                        )
                    } else {
                        DebugLogger.w(
                            TAG,
                            "Notification updater skipped cycle because soft block is active",
                            metadata = mapOf(
                                "totalSteps" to totalSteps.toString(),
                                "goal" to goal.toString()
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Notification updater loop error", e)
                    DebugLogger.e(
                        TAG,
                        "Notification updater loop error",
                        throwable = e,
                        metadata = mapOf(
                            "totalSteps" to totalSteps.toString()
                        )
                    )
                    stepSafetyGuard.registerError("notification_updater_loop_error")
                }

                delay(60_000L)
            }

            DebugLogger.w(TAG, "Notification updater stopped because coroutine is no longer active")
        }
    }

    private val accelListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent) {
            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val magnitude = sqrt(x * x + y * y + z * z)
            val motion = abs(magnitude - SensorManager.GRAVITY_EARTH)
            val now = System.currentTimeMillis()

            if (motion > MOTION_THRESHOLD) {
                lastMotionMs = now

                serviceScope.launch {
                    stepforgeStore.edit { prefs ->
                        prefs[LAST_MOTION_TIME] = now
                    }
                }

                if (inSleepMode) {
                    sleepEndMs = now
                }
            }

            if (!inSleepMode && lastMotionMs > 0L && (now - lastMotionMs) > SLEEP_START_AFTER_MS) {
                inSleepMode = true
                sleepStartMs = lastMotionMs
                sleepEndMs = 0L

                Log.d(TAG, "🌙 Motion sleep mode started at ${Date(sleepStartMs)}")
                DebugLogger.d(
                    TAG,
                    "Motion sleep mode started",
                    metadata = mapOf(
                        "sleepStartMs" to sleepStartMs.toString(),
                        "sleepStartText" to Date(sleepStartMs).toString(),
                        "motion" to motion.toString()
                    )
                )
            }

            if (inSleepMode && sleepEndMs > 0L && (now - sleepEndMs) > WAKE_AFTER_MS) {
                inSleepMode = false

                val start = sleepStartMs
                val end = sleepEndMs

                if (start > 0L && end > start) {
                    saveProbableSleep(start, end)

                    Log.d(TAG, "☀️ Motion wake detected. Sleep saved: ${Date(start)} -> ${Date(end)}")
                    DebugLogger.d(
                        TAG,
                        "Motion wake detected and probable sleep saved",
                        metadata = mapOf(
                            "startMs" to start.toString(),
                            "endMs" to end.toString(),
                            "startText" to Date(start).toString(),
                            "endText" to Date(end).toString()
                        )
                    )
                }

                sleepStartMs = 0L
                sleepEndMs = 0L
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }



    private fun saveProbableSleep(startMs: Long, endMs: Long) {
        serviceScope.launch {
            stepforgeStore.edit { prefs ->
                prefs[PROBABLE_SLEEP_READY] = true
                prefs[PROBABLE_SLEEP_START] = startMs
                prefs[PROBABLE_SLEEP_END] = endMs
            }
        }
    }

    private val detectorListener = object : SensorEventListener {

        override fun onSensorChanged(event: SensorEvent?) {

            if (event?.sensor?.type != Sensor.TYPE_STEP_DETECTOR) return

            lastDetectorStepTime = System.currentTimeMillis()
            detectorEverWorked = true
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    private fun midnightDebug(reason: String, steps: Int) {
        val time = java.text.SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            java.util.Locale.getDefault()
        ).format(java.util.Date())

        val message = """
        ===== MIDNIGHT DEBUG =====
        reason: $reason
        time: $time
        steps: $steps
        thread: ${Thread.currentThread().name}
        ==========================
    """.trimIndent()

        android.util.Log.e("STEPFORGE_MIDNIGHT", message)

        DebugLogger.e(
            TAG,
            "Midnight debug event",
            metadata = mapOf(
                "reason" to reason,
                "time" to time,
                "steps" to steps.toString(),
                "thread" to Thread.currentThread().name
            )
        )
    }


    private fun maybeUpdateWidgetsThrottled() {
        val now = System.currentTimeMillis()
        if (now - lastWidgetUpdateMs < 5000L) return
        lastWidgetUpdateMs = now

        StepWidgetProvider.sendStepsUpdate(applicationContext, totalSteps)
        StepWidgetCompactProvider.sendStepsUpdate(applicationContext, totalSteps)
        StepWidgetLargeProvider.sendStepsUpdate(applicationContext, totalSteps)
    }

    private fun maybePersistStepsThrottled() {
        val now = System.currentTimeMillis()
        if (now - lastPersistMs < 4000L) return
        lastPersistMs = now

        saveContinuously(totalSteps)
        saveDaily(todayKey(), totalSteps)
    }

    private fun maybeUpdateNotificationThrottled() {
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateMs < 3000L) return
        lastNotificationUpdateMs = now

        updateNotifAsync(totalSteps)
    }


    private fun handleWorkoutEvent(event: WorkoutEvent) {
        when (event) {

            is WorkoutEvent.Started -> {
                Log.d(TAG, "Workout started")

                DebugLogger.i(
                    TAG,
                    "Workout started",
                    metadata = emptyMap()
                )
            }

            is WorkoutEvent.Finished -> {
                Log.d(TAG, "Workout finished: ${event.steps} steps")

                DebugLogger.i(
                    TAG,
                    "Workout finished",
                    metadata = mapOf(
                        "steps" to event.steps.toString(),
                        "durationSec" to (event.duration / 1000).toString()
                    )
                )

                // 🔥 BURASI SONRAKİ ADIMDA BÜYÜYECEK
                // şimdilik sadece log
            }
        }
    }


    private fun shieldDateKey(): String {
        return java.time.LocalDate.now(java.time.ZoneId.systemDefault()).toString()
    }

    private fun currentMonthKey(): String {
        val now = java.time.LocalDate.now(java.time.ZoneId.systemDefault())
        return "%04d-%02d".format(now.year, now.monthValue)
    }

    private suspend fun isPremiumEnabled(): Boolean {
        val prefs = stepforgeStore.data.first()
        return (prefs[intPreferencesKey("premium_enabled")] ?: 0) == 1
    }

    private suspend fun getPremiumRescuesLeft(): Int {
        val prefs = stepforgeStore.data.first()
        val currentMonth = currentMonthKey()
        val savedMonth = prefs[StreakShieldPrefs.PREMIUM_RESCUE_MONTH] ?: currentMonth
        val usedCount = if (savedMonth == currentMonth) {
            prefs[StreakShieldPrefs.PREMIUM_RESCUE_USED_COUNT] ?: 0
        } else {
            0
        }

        return (StreakShieldEngine.getMonthlyPremiumRescueLimit() - usedCount).coerceAtLeast(0)
    }

    private suspend fun consumePremiumRescueIfAvailable(): Boolean {
        val premium = isPremiumEnabled()
        if (!premium) return false

        val prefs = stepforgeStore.data.first()
        val currentMonth = currentMonthKey()
        val savedMonth = prefs[StreakShieldPrefs.PREMIUM_RESCUE_MONTH] ?: currentMonth
        val usedCount = if (savedMonth == currentMonth) {
            prefs[StreakShieldPrefs.PREMIUM_RESCUE_USED_COUNT] ?: 0
        } else {
            0
        }

        if (usedCount >= StreakShieldEngine.getMonthlyPremiumRescueLimit()) {
            return false
        }

        stepforgeStore.edit {
            it[StreakShieldPrefs.PREMIUM_RESCUE_MONTH] = currentMonth
            it[StreakShieldPrefs.PREMIUM_RESCUE_USED_COUNT] = usedCount + 1
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = StreakShieldEngine.getPremiumRescueHours() * 60
            it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = StreakShieldEngine.getPremiumRescueHours() * 60
            it[StreakShieldPrefs.SHIELD_LAST_DECAY_AT_MS] = System.currentTimeMillis()
            it[StreakShieldPrefs.SHIELD_GENERATED_FOR_DATE] = shieldDateKey()
            it[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] = shieldDateKey()
        }

        DebugLogger.i(
            TAG,
            "Premium rescue consumed",
            metadata = mapOf(
                "currentMonth" to currentMonth,
                "newUsedCount" to (usedCount + 1).toString()
            )
        )

        return true
    }

    private suspend fun saveTomorrowShieldFromTodaySteps(todaySteps: Int) {
        val goal = getLatestGoal()
        val premium = isPremiumEnabled()

        val result = StreakShieldEngine.calculateDailyEarnedShieldHours(
            steps = todaySteps,
            goal = goal,
            isPremium = premium
        )

        stepforgeStore.edit {
            it[StreakShieldPrefs.SHIELD_TOMORROW_BASE_HOURS] = result.baseHours
            it[StreakShieldPrefs.SHIELD_TOMORROW_GOAL_BONUS_HOURS] = result.goalBonusHours
            it[StreakShieldPrefs.SHIELD_TOMORROW_FINAL_HOURS] = result.finalHours
            it[StreakShieldPrefs.SHIELD_TOMORROW_MAX_HOURS] = result.maxHours
        }

        DebugLogger.i(
            TAG,
            "Tomorrow shield calculated from today steps",
            metadata = mapOf(
                "todaySteps" to todaySteps.toString(),
                "goal" to goal.toString(),
                "isPremium" to premium.toString(),
                "baseHours" to result.baseHours.toString(),
                "goalBonusHours" to result.goalBonusHours.toString(),
                "finalHours" to result.finalHours.toString(),
                "maxHours" to result.maxHours.toString()
            )
        )
    }

    private suspend fun activateTodayShieldFromStoredTomorrow() {
        val prefs = stepforgeStore.data.first()

        val finalHours = prefs[StreakShieldPrefs.SHIELD_TOMORROW_FINAL_HOURS] ?: 0
        val maxHours = prefs[StreakShieldPrefs.SHIELD_TOMORROW_MAX_HOURS] ?: 0

        val minutes = finalHours * 60
        val maxMinutes = maxHours * 60

        stepforgeStore.edit {
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = minutes
            it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = maxMinutes
            it[StreakShieldPrefs.SHIELD_LAST_DECAY_AT_MS] = System.currentTimeMillis()
            it[StreakShieldPrefs.SHIELD_GENERATED_FOR_DATE] = shieldDateKey()
        }

        DebugLogger.i(
            TAG,
            "Today shield activated from stored tomorrow shield",
            metadata = mapOf(
                "minutes" to minutes.toString(),
                "maxMinutes" to maxMinutes.toString(),
                "date" to shieldDateKey()
            )
        )
    }


    private fun startShieldDrainTicker() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val prefs = stepforgeStore.data.first()
                    val currentShieldMinutes = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
                    val lastDecayAt = prefs[StreakShieldPrefs.SHIELD_LAST_DECAY_AT_MS] ?: 0L

                    if (currentShieldMinutes > 0) {
                        val shouldDrain = StreakShieldEngine.shouldShieldDrain(totalSteps)
                        val now = System.currentTimeMillis()

                        if (shouldDrain) {
                            val elapsedMinutes = ((now - lastDecayAt) / 60_000L).toInt()

                            if (elapsedMinutes > 0) {
                                val newMinutes = (currentShieldMinutes - elapsedMinutes).coerceAtLeast(0)

                                stepforgeStore.edit {
                                    it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = newMinutes
                                    it[StreakShieldPrefs.SHIELD_LAST_DECAY_AT_MS] = now
                                }

                                DebugLogger.d(
                                    TAG,
                                    "Shield drained",
                                    metadata = mapOf(
                                        "oldMinutes" to currentShieldMinutes.toString(),
                                        "elapsedMinutes" to elapsedMinutes.toString(),
                                        "newMinutes" to newMinutes.toString(),
                                        "todaySteps" to totalSteps.toString()
                                    )
                                )

                                if (newMinutes == 0) {
                                    val rescueUsed = consumePremiumRescueIfAvailable()
                                    if (!rescueUsed) {
                                        DebugLogger.w(
                                            TAG,
                                            "Shield depleted and no premium rescue available",
                                            metadata = mapOf(
                                                "todaySteps" to totalSteps.toString()
                                            )
                                        )
                                    }
                                }
                            }
                        } else {
                            stepforgeStore.edit {
                                it[StreakShieldPrefs.SHIELD_LAST_DECAY_AT_MS] = now
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Shield drain ticker error", e)
                    DebugLogger.e(TAG, "Shield drain ticker error", e)
                }

                delay(60_000L)
            }
        }
    }


    private suspend fun didTodayCountForStreak(): Boolean {
        val prefs = stepforgeStore.data.first()
        val goal = getLatestGoal()
        val shieldMinutesLeft = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
        val rescueDate = prefs[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] ?: ""
        val rescueUsedToday = rescueDate == shieldDateKey()

        val result = StreakDayQualifier.qualifyDay(
            steps = totalSteps,
            goal = goal,
            shieldMinutesLeft = shieldMinutesLeft,
            rescueUsedForDay = rescueUsedToday
        )

        DebugLogger.d(
            TAG,
            "didTodayCountForStreak evaluated",
            metadata = mapOf(
                "steps" to totalSteps.toString(),
                "goal" to goal.toString(),
                "shieldMinutesLeft" to shieldMinutesLeft.toString(),
                "rescueUsedToday" to rescueUsedToday.toString(),
                "countsForStreak" to result.countsForStreak.toString(),
                "reachedGoal" to result.reachedGoal.toString(),
                "protectedByShield" to result.protectedByShield.toString(),
                "protectedByPremiumRescue" to result.protectedByPremiumRescue.toString()
            )
        )

        return result.countsForStreak
    }


    private fun maybeRunPremiumCoach() {
        serviceScope.launch {
            try {
                val prefs = stepforgeStore.data.first()

                val isPremium = (prefs[intPreferencesKey("premium_enabled")] ?: 0) == 1
                val aiCoachEnabled = prefs[StreakShieldPrefs.PREMIUM_AI_COACH_ENABLED] ?: false
                val shieldMinutesLeft = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0

                if (!isPremium || !aiCoachEnabled) return@launch

                val goal = getLatestGoal()

                val dates7 = buildList {
                    val cal = Calendar.getInstance()
                    cal.add(Calendar.DAY_OF_YEAR, -6)
                    repeat(7) {
                        add(todayKeyFromCalendar(cal))
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                }

                val dailyDao = AppDatabase.getDatabase(applicationContext).dailyStepsDao()
                val allDaily = dailyDao.getAllSteps().associateBy { it.date }
                val last7Average = dates7.map { d -> allDaily[d]?.steps ?: 0 }.average().toInt()

                val decision = PremiumCoachEngine.evaluate(
                    PremiumCoachEngine.Input(
                        isPremium = isPremium,
                        aiCoachEnabled = aiCoachEnabled,
                        todaySteps = totalSteps,
                        goal = goal,
                        shieldMinutesLeft = shieldMinutesLeft,
                        premiumRescuesLeft = getPremiumRescuesLeft(),
                        nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
                        last7AverageSteps = last7Average
                    )
                )

                premiumCoachNotifier.maybeNotify(decision)

                DebugLogger.d(
                    TAG,
                    "Premium coach evaluated",
                    metadata = mapOf(
                        "isPremium" to isPremium.toString(),
                        "aiCoachEnabled" to aiCoachEnabled.toString(),
                        "todaySteps" to totalSteps.toString(),
                        "goal" to goal.toString(),
                        "shieldMinutesLeft" to shieldMinutesLeft.toString(),
                        "premiumRescuesLeft" to getPremiumRescuesLeft().toString(),
                        "decisionShouldNotify" to decision.shouldNotify.toString(),
                        "decisionType" to (decision.type?.name ?: "null")
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Premium coach evaluation failed", e)
                DebugLogger.e(TAG, "Premium coach evaluation failed", e)
            }
        }
    }

    private fun todayKeyFromCalendar(calendar: Calendar): String {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d-%02d-%02d".format(year, month, day)
    }
}










