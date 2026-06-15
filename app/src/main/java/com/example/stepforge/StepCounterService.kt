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
import android.content.pm.ServiceInfo
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
import com.example.stepforge.core.WorkoutSessionEngine
import com.example.stepforge.core.WorkoutSessionTransition
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.HourlySteps
import com.example.stepforge.data.SleepMainSessionDeduper
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.WorkoutSession
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.core.AppLanguageHelper
import com.example.stepforge.debug.DebugLogger
import com.example.stepforge.debug.RuntimeDiagnostics
import com.example.stepforge.debug.StepSafetyGuard
import com.example.stepforge.steps.CentralStepState
import com.example.stepforge.steps.StepEvents
import com.example.stepforge.steps.StepValidationAnalyzer
import com.example.stepforge.ui.components.PremiumCoachEngine
import com.example.stepforge.ui.components.PremiumCoachNotifier
import com.example.stepforge.ui.streak.StreakBehaviorEngine
import com.example.stepforge.ui.streak.StreakDayQualifier
import com.example.stepforge.ui.streak.StreakShieldEngine
import com.example.stepforge.ui.streak.StreakShieldPrefs
import com.example.stepforge.widget.StepWidgetCompactProvider
import com.example.stepforge.widget.StepWidgetLargeProvider
import com.example.stepforge.widget.StepWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt


class StepCounterService : Service(), SensorEventListener {

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val sessionEngine = WorkoutSessionEngine(
        inactivityTimeoutMs = WALK_SESSION_INACTIVITY_TIMEOUT_MS
    )
    private lateinit var sensorManager: SensorManager
    private var accelerometer: Sensor? = null

    private var lastMotionMs = 0L
    private var lastMotionWriteMs = 0L
    private var lastDateDebug = ""

    private val MOTION_STEP_TIMEOUT = 10000L
    private var sleepStartMs = 0L
    private var sleepEndMs = 0L

    private var wakeMotionStartMs = 0L
    private var screenOffToken = 0L

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
    @Volatile private var foregroundStarted = false
    private var persistJob: Job? = null
    private var widgetSyncJob: Job? = null
    private var coachJob: Job? = null
    @Volatile private var pendingPersistSteps: Int = 0
    @Volatile private var pendingWidgetSteps: Int = 0
    @Volatile private var lastWidgetSyncMs: Long = 0L
    @Volatile private var lastNotificationUpdateMs: Long = 0L
    @Volatile private var lastRuntimeMotionPersistMs: Long = 0L

    private val PERSIST_DEBOUNCE_MS = 2_000L
    private val WIDGET_SYNC_INTERVAL_MS = 1_000L
    private val NOTIFICATION_UPDATE_INTERVAL_MS = 1_000L
    private val RUNTIME_MOTION_PERSIST_INTERVAL_MS = 15_000L
    private val SENSOR_WATCHDOG_STALL_MS = 90_000L

    // --- Walking tracking ---

    @Volatile private var sessionStartSteps: Int = 0
    @Volatile private var sessionStartTime: Long = 0
    @Volatile private var lastStepTime: Long = System.currentTimeMillis()
    @Volatile private var isWalking = false
    private var activityCheckJob: Job? = null

    private val INACTIVITY_THRESHOLD_MS = 3 * 60 * 60 * 1000L // 3 hours
    private var currentDay = todayKey()
    private var hasCelebrated = false

    private val MOTION_THRESHOLD = 1.2f
    private val SLEEP_START_AFTER_MS = 20 * 60_000L   // 20 dk hareketsizlik -> uyku
    private val WAKE_AFTER_MS = 2 * 60_000L           // 2 dk hareket -> uyandı

    private var hourlyTickerJob: Job? = null
    private val dao by lazy { AppDatabase.getDatabase(this).dailyStepsDao() }
    private val resetMutex = kotlinx.coroutines.sync.Mutex()

    private val stepSafetyGuard = StepSafetyGuard()
    private val premiumCoachNotifier by lazy { PremiumCoachNotifier(this) }
    companion object {
        const val CHANNEL_ID = "step_channel"
        const val NOTIF_ID = 1001
        const val ACTION_GOAL_COMPLETED = "com.example.stepforge.GOAL_COMPLETED"
        const val TAG = "StepForgeDebug"
        const val ACTION_RELOAD = "com.example.stepforge.ACTION_RELOAD"

        // Sleep runtime state
        val RUNTIME_SLEEP_MODE = booleanPreferencesKey("runtime_sleep_mode")
        val RUNTIME_SLEEP_START = longPreferencesKey("runtime_sleep_start")
        val RUNTIME_SLEEP_END = longPreferencesKey("runtime_sleep_end")
        val RUNTIME_WAKE_MOTION_START = longPreferencesKey("runtime_wake_motion_start")

        // Walking runtime state
        val RUNTIME_IS_WALKING = booleanPreferencesKey("runtime_is_walking")
        val RUNTIME_SESSION_START_TIME = longPreferencesKey("runtime_session_start_time")
        val RUNTIME_SESSION_START_STEPS = intPreferencesKey("runtime_session_start_steps")

        const val ALERT_CHANNEL_ID = "StepForgeAlerts"

        private const val WALK_SESSION_INACTIVITY_TIMEOUT_MS = 10L * 60L * 1000L
        private const val WALK_SESSION_MERGE_GAP_MS = 15L * 60L * 1000L
        private const val WALK_SESSION_MIN_STEPS = 500
        private const val WALK_SESSION_MIN_DURATION_MS = 5L * 60L * 1000L
        private const val WALK_SESSION_MIN_REAL_MOTION_STEPS = 300
        @Volatile
        var isServiceRunning: Boolean = false

        @Volatile private var lastCoachRunMs = 0L
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
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(
                    NOTIF_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
                )
            } else {
                startForeground(NOTIF_ID, notification)
            }
            DebugLogger.i(TAG, "startForeground succeeded (once)")
            true
        } catch (t: Throwable) {
            Log.e(TAG, "startForeground blocked: ${t.javaClass.name} ${t.message}", t)
            DebugLogger.e(
                TAG,
                "startForeground blocked: ${t.javaClass.name} ${t.message}",
                throwable = t,
                metadata = mapOf(
                    "isForegroundStartNotAllowed" to isStartForegroundNotAllowed(t).toString()
                )
            )
            false
        }
    }

    private fun ensureForegroundOnce(notification: Notification) {
        if (foregroundStarted) return
        if (safeStartForeground(notification)) {
            foregroundStarted = true
        } else {
            // Eğer foreground başlatılamazsa, servisin düzgün çalışmayacağını biliyoruz.
            // Kritik durumda stopSelf() çağrılabilir veya tekrar deneme stratejisi kurulabilir.
            Log.e(TAG, "Failed to start foreground service, background execution may be limited.")
        }
    }

    private fun updateForegroundNotification(steps: Int) {
        runCatching {
            val notification = buildNotification(steps)
            if (!foregroundStarted) {
                ensureForegroundOnce(notification)
                return
            }
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIF_ID, notification)

            RuntimeDiagnostics.lastNotificationUpdateTime = System.currentTimeMillis()
        }.onFailure { t ->
            Log.e(TAG, "Notification update failed", t)
            DebugLogger.e(TAG, "Notification update failed", t)
        }
    }


    private fun publishLiveSteps(newTotal: Int, diff: Int) {
        val safeTotal = newTotal.coerceAtLeast(0)

        RuntimeDiagnostics.lastAcceptedStepTime =
            System.currentTimeMillis()
        totalSteps = safeTotal
        CentralStepState.emit(safeTotal, cachedGoal, todayKey())
        StepEvents.emitTodaySteps(safeTotal)

        if (diff > 0) {
            StepValidationAnalyzer.noteLiveBurst(diff)
        }

        checkGoalCelebrate(safeTotal)
        val now = System.currentTimeMillis()
        if (now - lastNotificationUpdateMs >= NOTIFICATION_UPDATE_INTERVAL_MS) {
            lastNotificationUpdateMs = now
            updateForegroundNotification(safeTotal)
        }
        schedulePersist(safeTotal)
        scheduleWidgetSync(safeTotal)

        if (diff > 0 && now - lastRuntimeMotionPersistMs >= RUNTIME_MOTION_PERSIST_INTERVAL_MS) {
            lastRuntimeMotionPersistMs = now
            serviceScope.launch {
                try {
                    stepforgeStore.edit { prefs ->
                        prefs[LAST_STEP_TIME] = now
                        prefs[LAST_MOTION_TIME] = now
                    }
                } catch (e: Exception) {
                    stepSafetyGuard.registerError("write_last_step_time_failed")
                }
            }
        }

        coachJob?.cancel()
        coachJob = serviceScope.launch {
            delay(2_000L)
            maybeRunPremiumCoach()
            StreakBehaviorEngine.syncEarnedBuffer(applicationContext, safeTotal, cachedGoal)
        }
    }

    private fun schedulePersist(steps: Int) {
        pendingPersistSteps = steps.coerceAtLeast(0)
        if (persistJob?.isActive == true) return

        persistJob = serviceScope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            flushPersistNow(pendingPersistSteps)
        }
    }

    private suspend fun flushPersistNow(steps: Int) {

        RuntimeDiagnostics.pendingFlushCount++

        val start = System.currentTimeMillis()

        try {

            val persistedKey = intPreferencesKey("persisted_total_sum")
            val day = todayKey()

            stepforgeStore.edit {
                it[PREF_LAST_SENSOR] = lastSensorValue
                it[PREF_OFFSET] = sensorOffset
                it[PREF_TOTAL] = steps
                it[persistedKey] = steps
            }

            withContext(Dispatchers.IO) {
                dao.insertDailySteps(DailySteps(day, steps))
            }

            RuntimeDiagnostics.lastPersistTime =
                System.currentTimeMillis()

            RuntimeDiagnostics.lastPersistDurationMs =
                System.currentTimeMillis() - start

        } finally {

            RuntimeDiagnostics.pendingFlushCount =
                (RuntimeDiagnostics.pendingFlushCount - 1)
                    .coerceAtLeast(0)
        }
    }

    private fun scheduleWidgetSync(steps: Int) {
        val safeSteps = steps.coerceAtLeast(0)
        pendingWidgetSteps = safeSteps
        val now = System.currentTimeMillis()

        if (now - lastWidgetSyncMs >= WIDGET_SYNC_INTERVAL_MS) {
            widgetSyncJob?.cancel()
            sendWidgetStepsNow(safeSteps)
            return
        }

        if (widgetSyncJob?.isActive == true) return

        widgetSyncJob = serviceScope.launch {
            val waitMs = (WIDGET_SYNC_INTERVAL_MS - (System.currentTimeMillis() - lastWidgetSyncMs))
                .coerceAtLeast(0L)
            delay(waitMs)
            sendWidgetStepsNow(pendingWidgetSteps)
        }
    }

    private fun sendWidgetStepsNow(steps: Int) {
        val safeSteps = steps.coerceAtLeast(0)
        StepWidgetProvider.sendStepsUpdate(applicationContext, safeSteps)
        StepWidgetCompactProvider.sendStepsUpdate(applicationContext, safeSteps)
        StepWidgetLargeProvider.sendStepsUpdate(applicationContext, safeSteps)

        val now = System.currentTimeMillis()
        lastWidgetSyncMs = now
        RuntimeDiagnostics.lastWidgetUpdateTime = now
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

        serviceScope.launch {
            targetFlow.collect {
                cachedGoal = it
            }
        }

        // ✅ Workout Session Watchdog
        handler.post(object : Runnable {
            override fun run() {
                val nowMs = System.currentTimeMillis()
                val finished = sessionEngine.tick(nowMs)

                if (finished != null) {
                    handleSessionFinished(finished)
                }

                handler.postDelayed(this, 30_000L)
            }
        })

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
        // ✅ Hemen foreground'a al, crash riskini ortadan kaldır
        val immediateNotif = buildNotification(0)
        ensureForegroundOnce(immediateNotif)

// ✅ Sonra DataStore'dan gerçek değerleri arka planda yükle
        serviceScope.launch {

            val prefs = stepforgeStore.data.first()

            lastSensorValue = prefs[PREF_LAST_SENSOR] ?: 0
            sensorOffset = prefs[PREF_OFFSET] ?: 0
            totalSteps = prefs[PREF_TOTAL] ?: 0

            // Restore runtime states
            inSleepMode = prefs[RUNTIME_SLEEP_MODE] ?: false
            sleepStartMs = prefs[RUNTIME_SLEEP_START] ?: 0L
            sleepEndMs = prefs[RUNTIME_SLEEP_END] ?: 0L
            wakeMotionStartMs = prefs[RUNTIME_WAKE_MOTION_START] ?: 0L

            isWalking = prefs[RUNTIME_IS_WALKING] ?: false
            sessionStartTime = prefs[RUNTIME_SESSION_START_TIME] ?: 0L
            sessionStartSteps = prefs[RUNTIME_SESSION_START_STEPS] ?: 0
            if (isWalking && sessionStartTime > 0L) {
                sessionEngine.restoreActiveSession(
                    startTimeMs = sessionStartTime,
                    startSteps = sessionStartSteps,
                    lastStepTimeMs = lastStepTime,
                    totalSteps = totalSteps
                )
            }
            val currentGoal = (prefs[intPreferencesKey("step_goal")] ?: 10000)
                .coerceIn(1000, 100000)
            cachedGoal = currentGoal

            Log.d(TAG, "DB'den Yüklenen: Total=$totalSteps, Offset=$sensorOffset, LastSensor=$lastSensorValue")

            CentralStepState.emit(totalSteps, currentGoal, todayKey())

            updateForegroundNotification(totalSteps)
            serviceScope.launch { flushPersistNow(totalSteps) }
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
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)

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
                updateForegroundNotification(totalSteps)
            }
        }

        // -------------------------------------------------
        // Alerts + walking monitor
        // -------------------------------------------------
        createAlertChannel()
        startActivityMonitoring()
        lastStepTime = System.currentTimeMillis()
        startStreakBehaviorTicker()

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

                        screenOffToken = System.currentTimeMillis()
                        val token = screenOffToken

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

                                if (token != screenOffToken) {
                                    return@launch
                                }

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
            sensorManager.registerListener(accelListener, it, SensorManager.SENSOR_DELAY_UI)
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
        val db = AppDatabase.getDatabase(this@StepCounterService)
        val sleepDao = db.sleepSessionDao()
        val stageDao = db.sleepStageDao()

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

        SleepMainSessionDeduper.deduplicate(sleepDao, stageDao)

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
        DebugLogger.w(TAG, "StepCounterService onDestroy started")

        // ANR riskini tamamen bitirmek için runBlocking yerine GlobalScope benzeri
        // ama kısıtlı bir yaşam döngüsü kullanıyoruz.
        try {
            // Sadece çok hızlı bir deneme yap, eğer kilit varsa bekleme.
            serviceScope.launch(Dispatchers.IO) {
                flushPersistNow(totalSteps)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Final flush launch failed", e)
        }

        handler.removeCallbacksAndMessages(null)
        persistJob?.cancel()

        try {
            screenReceiver?.let { unregisterReceiver(it) }
            screenReceiver = null
        } catch (e: Exception) {
            Log.e(TAG, "Receiver unregister error", e)
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

            if (action == ACTION_RELOAD) {
                DebugLogger.i(TAG, "ACTION_RELOAD received. Refreshing localized runtime surfaces.")
                serviceScope.launch {
                    val prefs = stepforgeStore.data.first()
                    val currentGoal = (prefs[intPreferencesKey("step_goal")] ?: 10000)
                        .coerceIn(1000, 100000)
                    cachedGoal = currentGoal

                    createChannel()
                    createAlertChannel()
                    CentralStepState.emit(totalSteps, currentGoal, todayKey())
                    updateForegroundNotification(totalSteps)
                    StepWidgetProvider.notifyRefresh(applicationContext)
                    StepWidgetCompactProvider.notifyRefresh(applicationContext)
                    StepWidgetLargeProvider.notifyRefresh(applicationContext)
                }
            }

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
                val textContext = AppLanguageHelper.localizedContext(this)
                sendAlertNotification(
                    textContext.getString(R.string.hc_test_notification_title),
                    textContext.getString(R.string.hc_test_notification_message)
                )
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

                    val previousForce = totalSteps
                    totalSteps = forceSteps

                    if (lastSensorValue != 0) {
                        sensorOffset = totalSteps - lastSensorValue
                    }

                    publishLiveSteps(forceSteps, forceSteps - previousForce)
                } else {
                    updateForegroundNotification(totalSteps)
                }
            } else {
                updateForegroundNotification(totalSteps)
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

                val previousManual = totalSteps
                sensorOffset = manual - lastSensorValue
                totalSteps = manual

                publishLiveSteps(manual, (manual - previousManual).coerceAtLeast(0))

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
        val sensor = event?.sensor ?: return
        val values = event.values ?: return
        if (values.isEmpty()) return

        when (sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val currentCounter = values[0].toInt()
                val now = System.currentTimeMillis()

                if (lastCounterValueForMotion != 0) {
                    val diff = currentCounter - lastCounterValueForMotion
                    val timeDiff = now - lastCounterCheckTime

                    // 🔥 Fallback step detection
                    if (diff > 0 && diff < 20) {
                        // çok büyük sıçrama değil → gerçek adım kabul et
                        Log.v(TAG, "Fallback step detected: diff=$diff")
                    }
                }

                lastCounterValueForMotion = currentCounter
                lastCounterCheckTime = now

                if (stepSensor == null) return

                val sensorVal = values[0].toInt()
                val nowMs = System.currentTimeMillis()
                lastSensorEventTime = nowMs
                RuntimeDiagnostics.lastSensorEventTime = nowMs

                if (lastSensorValue == 0) {
                    // ✅ Sadece ilk başlatma: offset'i kur
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

                    sensorOffset = totalSteps - sensorVal
                    StepValidationAnalyzer.noteSensorReset()
                    midnightDebug(
                        reason = "SENSOR COUNTER RESET",
                        steps = sensorVal
                    )
                    DebugLogger.w(
                        TAG,
                        "Sensor counter reset detected, offset recalculated",
                        metadata = mapOf(
                            "sensorVal" to sensorVal.toString(),
                            "previousSensorValue" to previousSensorValue.toString(),
                            "newOffset" to sensorOffset.toString(),
                            "totalSteps" to totalSteps.toString()
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

                if (diff > 80) {
                    DebugLogger.w(
                        TAG,
                        "Large step burst accepted (end-of-day validation may log)",
                        metadata = mapOf(
                            "diff" to diff.toString(),
                            "sensorVal" to sensorVal.toString(),
                            "totalSteps" to totalSteps.toString()
                        )
                    )
                }

                if (calculatedSteps > previousTotal) {
                    val now = System.currentTimeMillis()
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

                    lastStepTime = now
                    lastMotionMs = now

                    val events = sessionEngine.onStep(now, calculatedSteps)
                    for (event in events) {
                        when (event) {
                            is WorkoutSessionTransition.Started -> {
                                isWalking = true
                                sessionStartSteps = event.startSteps
                                sessionStartTime = event.startTimeMs
                                persistRuntimeState()
                            }
                            is WorkoutSessionTransition.Finished -> {
                                handleSessionFinished(event)
                            }
                        }
                    }

                    Log.d(
                        TAG,
                        "Step accepted: prev=$previousTotal new=$calculatedSteps sensor=$sensorVal diff=$diff"
                    )
                    publishLiveSteps(calculatedSteps, diff)



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

                    if (diff > SENSOR_WATCHDOG_STALL_MS) {
                        Log.w(TAG, "Sensor watchdog restarting listener (stalled)")
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

                delay(60_000L)
            }

            DebugLogger.w(TAG, "Sensor watchdog stopped because coroutine is no longer active")
        }
    }

    @Volatile
    private var lastNotifiedSteps: Int = -1

    private fun updateNotifAsync(sum: Int) {
        if (sum == lastNotifiedSteps) return
        lastNotifiedSteps = sum
        updateForegroundNotification(sum)
    }

    private fun buildNotification(sum: Int): Notification {
        val textContext = AppLanguageHelper.localizedContext(this)
        val prefGoal = cachedGoal.coerceIn(1000, 100000)

        val fmtSteps = formatInt(sum)
        val fmtGoal = formatInt(prefGoal)

        val expandedText = textContext.getString(
            R.string.hc_foreground_expanded_format,
            fmtSteps,
            fmtGoal
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_walk)
            .setContentTitle(textContext.getString(R.string.app_name))
            .setContentText(textContext.getString(R.string.hc_step_notification_format, fmtSteps, fmtGoal))
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
            try {
                val persistedKey = intPreferencesKey("persisted_total_sum")
                stepforgeStore.edit {
                    it[PREF_LAST_SENSOR] = lastSensorValue
                    it[PREF_OFFSET] = sensorOffset
                    it[PREF_TOTAL] = sum
                    it[persistedKey] = sum
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveContinuously failed", e)
            }
        }
    }

    private fun saveDaily(day: String, sum: Int) {
        serviceScope.launch {
            try {
                dao.insertDailySteps(DailySteps(day, sum))
            } catch (e: Exception) {
                Log.e(TAG, "saveDaily failed", e)
            }
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
        val today = todayKey()  // ✅ currentDay değil, anlık tarih
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
            resetMutex.withLock {   // ✅ Sadece bu satırı ekle (withLock açılışı)
                val today = todayKey()

                if (hasResetToday()) {
                    Log.w(TAG, "resetForNewDay ignored: already reset for today=$today")
                    DebugLogger.w(
                        TAG,
                        "resetForNewDay ignored because reset already done today",
                        metadata = mapOf(
                            "today" to today
                        )
                    )
                    return@withLock   // ✅ return@launch yerine return@withLock
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

                val dayToSave = java.time.LocalDate
                    .now(java.time.ZoneId.systemDefault())
                    .minusDays(1)
                    .toString()  // Reset her zaman tamamlanan günü, yani dünü kaydeder.
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
                val goalForValidation = cachedGoal

                try {
                    val (maxBurst, sensorResets) = StepValidationAnalyzer.consumeLiveStats()
                    serviceScope.launch(Dispatchers.IO) {
                        val hourlyDao = AppDatabase.getDatabase(this@StepCounterService).hourlyStepsDao()
                        val hourly = hourlyDao.getForDate(dayToSave).map { it.steps }
                        StepValidationAnalyzer.analyzeCompletedDay(
                            StepValidationAnalyzer.DayAnalysisInput(
                                date = dayToSave,
                                totalSteps = stepsToSave,
                                goal = goalForValidation,
                                maxSingleBurst = maxBurst,
                                sensorResets = sensorResets,
                                hourlySamples = hourly
                            )
                        )
                    }

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
                    val persistedKey = intPreferencesKey("persisted_total_sum")
                    stepforgeStore.edit {
                        it[PREF_LAST_SENSOR] = 0
                        it[PREF_OFFSET] = -currentSensorValue
                        it[PREF_TOTAL] = 0
                        it[persistedKey] = 0   // ✅ Tek atomik write
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

                inSleepMode = false
                sleepStartMs = 0L
                sleepEndMs = 0L
                wakeMotionStartMs = 0L

                isWalking = false
                sessionStartTime = 0L
                sessionStartSteps = totalSteps

                persistRuntimeState()

                val savedGoal = cachedGoal
                serviceScope.launch {
                    val prefsBeforeReset = stepforgeStore.data.first()
                    val bufferYesterday = prefsBeforeReset[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
                    val rescueDate = prefsBeforeReset[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] ?: ""
                    val rescuedUntil = prefsBeforeReset[com.example.stepforge.ui.streak.StreakBehaviorPrefs.STREAK_RESCUED_UNTIL_MS] ?: 0L
                    val qualification = StreakDayQualifier.qualifyDay(
                        steps = stepsToSave,
                        goal = savedGoal,
                        behaviorBufferMinutes = bufferYesterday,
                        rescueUsedForDay = rescueDate == dayToSave,
                        rescuedActive = rescuedUntil > System.currentTimeMillis()
                    )
                    if (qualification.countsForStreak && !qualification.reachedGoal) {
                        StreakBehaviorEngine.onDayQualifiedWithoutGoal(applicationContext, dayToSave)
                    }
                }

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

                CentralStepState.emit(0, cachedGoal, todayKey())
                updateForegroundNotification(0)
                serviceScope.launch { flushPersistNow(0) }
                scheduleWidgetSync(0)

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
    }


    private fun scheduleMidnightReset() {
        try {
            val alarmMgr = getSystemService(ALARM_SERVICE) as AlarmManager

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

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmMgr.canScheduleExactAlarms()) {
                    Log.w(TAG, "Exact alarm permission not granted. Using inexact fallback.")
                    DebugLogger.w(
                        TAG,
                        "Exact alarm permission not granted. Using inexact fallback for midnight reset."
                    )
                    alarmMgr.setWindow(
                        AlarmManager.RTC_WAKEUP,
                        nextMidnight.timeInMillis,
                        5 * 60 * 1000L,  // ✅ 5 dk pencere
                        pi
                    )
                    return
                }
            }

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
                        continue  // ✅ Bunu ekle, snapshot kaydedilmesin
                    }
                    saveHourlySnapshot(totalSteps)  // ✅ Bunu ekle, sadece hNow != 0 iken çalışsın
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
                val goal = cachedGoal

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
                        AppLanguageHelper.localizedContext(this@StepCounterService)
                            .getString(R.string.hc_goal_reached_title),
                        AppLanguageHelper.localizedContext(this@StepCounterService)
                            .getString(R.string.hc_goal_reached_message, goal)
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
            val textContext = AppLanguageHelper.localizedContext(this)
            val channel = NotificationChannel(
                CHANNEL_ID,
                textContext.getString(R.string.hc_stepforge_service),
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
        return String.format(AppLanguageHelper.selectedLocale(this), "%,d", value)
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

                // ❌ Handler kodu SİLİNDİ - onCreate'de zaten var

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
                            AppLanguageHelper.localizedContext(this@StepCounterService)
                                .getString(R.string.hc_inactivity_title),
                            AppLanguageHelper.localizedContext(this@StepCounterService)
                                .getString(R.string.hc_inactivity_message)
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
        val durationMs = (endTime - startTime).coerceAtLeast(0L)
        if (!shouldPersistWorkoutSession(stepsTaken, durationMs)) {
            DebugLogger.d(
                TAG,
                "Workout session kept out of DB",
                metadata = mapOf(
                    "steps" to stepsTaken.toString(),
                    "durationMs" to durationMs.toString()
                )
            )
            return -1L
        }

        val dateStr = java.time.Instant.ofEpochMilli(startTime)
            .atZone(java.time.ZoneId.systemDefault())
            .toLocalDate()
            .toString()

        val dao = AppDatabase.getDatabase(applicationContext).workoutSessionDao()
        val latest = dao.getLatest()
        if (latest != null && canMergeWorkoutSessions(latest, dateStr, startTime)) {
            val mergedStart = minOf(latest.startTime, startTime)
            val mergedEnd = maxOf(latest.endTime, endTime)
            val mergedSteps = (latest.steps + stepsTaken).coerceAtLeast(0)
            val mergedDurationMin = (((mergedEnd - mergedStart).coerceAtLeast(0L) + 59_999L) / 60_000L)
                .toInt()
                .coerceAtLeast(1)
            val mergedDistanceMeters = estimateDistanceMeters(mergedSteps)
            val mergedCalories = estimateCaloriesKcal(mergedSteps)
            val mergedAvgSpm = if (mergedDurationMin > 0) {
                mergedSteps / mergedDurationMin
            } else {
                0
            }

            dao.insert(
                latest.copy(
                    date = dateStr,
                    startTime = mergedStart,
                    endTime = mergedEnd,
                    durationMinutes = mergedDurationMin,
                    steps = mergedSteps,
                    distanceMeters = mergedDistanceMeters,
                    caloriesKcal = mergedCalories,
                    avgStepsPerMinute = mergedAvgSpm,
                    source = "auto_walk"
                )
            )

            DebugLogger.i(
                TAG,
                "Workout merged with previous session",
                metadata = mapOf(
                    "sessionId" to latest.id.toString(),
                    "mergedSteps" to mergedSteps.toString(),
                    "mergedDurationMin" to mergedDurationMin.toString()
                )
            )

            return latest.id
        }

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
            source = "auto_walk"
        )

        val id = dao.insert(session)

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

    private fun shouldPersistWorkoutSession(stepsTaken: Int, durationMs: Long): Boolean {
        if (stepsTaken <= 0) return false
        if (stepsTaken >= WALK_SESSION_MIN_STEPS) return true

        val hasRealMovementDuration = durationMs >= WALK_SESSION_MIN_DURATION_MS &&
                stepsTaken >= WALK_SESSION_MIN_REAL_MOTION_STEPS

        return hasRealMovementDuration
    }

    private fun canMergeWorkoutSessions(
        latest: WorkoutSession,
        dateStr: String,
        startTime: Long
    ): Boolean {
        if (latest.source == "test") return false
        if (latest.date != dateStr) return false
        if (startTime < latest.startTime) return false
        val gapMs = startTime - latest.endTime
        return gapMs in 0L..WALK_SESSION_MERGE_GAP_MS
    }

    private fun handleSessionFinished(finished: WorkoutSessionTransition.Finished) {
        val stepsTaken = finished.stepsTaken
        val durationMin = finished.durationMinutes

        DebugLogger.i(
            TAG,
            "Session finished by engine",
            metadata = mapOf(
                "startTime" to finished.startTimeMs.toString(),
                "endTime" to finished.endTimeMs.toString(),
                "steps" to stepsTaken.toString(),
                "durationMin" to durationMin.toString()
            )
        )

        if (!shouldPersistWorkoutSession(stepsTaken, finished.durationMs)) {
            DebugLogger.d(TAG, "Session ignored (too small)")
            isWalking = false
            sessionStartTime = 0L
            sessionStartSteps = totalSteps
            persistRuntimeState()
            return
        }

        serviceScope.launch(Dispatchers.IO) {
            try {
                val sessionId = saveWorkoutSession(
                    startTime = finished.startTimeMs,    // ✅ Engine'den gelen DOĞRU başlangıç
                    endTime = finished.endTimeMs,        // ✅ Engine'den gelen DOĞRU bitiş
                    stepsTaken = stepsTaken,
                    durationMin = durationMin
                )

                if (sessionId <= 0L) return@launch

                DebugLogger.i(
                    TAG,
                    "Workout saved successfully",
                    metadata = mapOf("sessionId" to sessionId.toString())
                )

                withContext(Dispatchers.Main) {
                    sendWorkoutSummaryNotification(
                        sessionId = sessionId,
                        stepsTaken = stepsTaken,
                        durationMin = durationMin
                    )
                }

            } catch (e: Exception) {
                Log.e(TAG, "Workout save failed", e)
                DebugLogger.e(TAG, "Workout save failed", e)
            }
        }

        isWalking = false
        sessionStartTime = 0L
        sessionStartSteps = totalSteps

        persistRuntimeState()
    }

    private fun finishWalkingSession(sessionEndTimeMs: Long? = null) {

        if (sessionStartTime <= 0L) {
            isWalking = false
            sessionEngine.reset()
            persistRuntimeState()
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
        val shouldSave = shouldPersistWorkoutSession(stepsTaken, durationMs)

        if (shouldSave) {
            serviceScope.launch(Dispatchers.IO) {
                try {

                    val sessionId = saveWorkoutSession(
                        startTime = sessionStartTime,
                        endTime = endTime,
                        stepsTaken = stepsTaken,
                        durationMin = durationMin
                    )

                    if (sessionId <= 0L) return@launch

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
        sessionEngine.reset()
        persistRuntimeState()
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

            val textContext = AppLanguageHelper.localizedContext(this)
            val title = textContext.getString(R.string.hc_workout_completed)
            val message = textContext.getString(
                R.string.hc_workout_completed_message,
                stepsTaken,
                durationMin
            )

            val notificationManager =
                getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            val notification = NotificationCompat.Builder(this, ALERT_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_walk)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(
                    NotificationCompat.BigTextStyle().bigText(
                        textContext.getString(
                            R.string.hc_workout_notification_message,
                            stepsTaken,
                            durationMin
                        )
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
            val textContext = AppLanguageHelper.localizedContext(this)
            val channel = NotificationChannel(
                ALERT_CHANNEL_ID,
                textContext.getString(R.string.hc_activity_alerts_channel),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = textContext.getString(R.string.hc_activity_alerts_channel_info)
                enableVibration(true)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)

        // Uygulama kapatıldığında ana thread'i (Main) asla bloklamıyoruz.
        // Veri kaydını arka plana atıp sistemin servisi temizlemesine izin veriyoruz.
        serviceScope.launch(Dispatchers.IO) {
            try {
                flushPersistNow(totalSteps)
            } catch (e: Exception) {
                Log.e(TAG, "Final flush failed in onTaskRemoved", e)
            }
        }

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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
                // ✅ İzin yoksa set() ile yeniden başlatmayı dene
                alarmManager.set(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 3000,
                    pendingIntent
                )
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    SystemClock.elapsedRealtime() + 3000,
                    pendingIntent
                )
            }
        } catch (se: SecurityException) {
            Log.e(TAG, "onTaskRemoved: alarm scheduling failed due to SecurityException", se)
        }

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
            while (isActive) {
                try {
                    val goal = cachedGoal  // ✅ DataStore yerine cache
                    val softBlockActive = stepSafetyGuard.isSoftBlockActive()

                    if (!softBlockActive) {
                        updateForegroundNotification(totalSteps)

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

        override fun onSensorChanged(event: SensorEvent?) {
            val values = event?.values ?: return
            if (values.size < 3) return

            val x = values[0]
            val y = values[1]
            val z = values[2]

            val magnitude = sqrt(x * x + y * y + z * z)
            val motion = abs(magnitude - SensorManager.GRAVITY_EARTH)

            val now = System.currentTimeMillis()

            // ------------------------------------------------
            // REAL MOTION DETECTED
            // ------------------------------------------------

            if (motion > MOTION_THRESHOLD) {

                lastMotionMs = now

                // Daha seyrek datastore write
                if (now - lastMotionWriteMs > 2 * 60_000L) {

                    lastMotionWriteMs = now

                    serviceScope.launch {
                        try {
                            stepforgeStore.edit { prefs ->
                                prefs[LAST_MOTION_TIME] = now
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "LAST_MOTION_TIME write failed", e)
                        }
                    }
                }

                // ------------------------------------------------
                // WAKE DETECTION
                // ------------------------------------------------

                if (inSleepMode) {

                    // İlk hareket anı
                    if (wakeMotionStartMs == 0L) {
                        wakeMotionStartMs = now

                        DebugLogger.d(
                            TAG,
                            "Wake motion started",
                            metadata = mapOf(
                                "time" to now.toString()
                            )
                        )
                    }

                    // Hareket 2 dk sürdüyse gerçekten uyanmış say
                    if ((now - wakeMotionStartMs) > WAKE_AFTER_MS) {

                        inSleepMode = false

                        persistRuntimeState()

                        val start = sleepStartMs
                        val end = wakeMotionStartMs

                        if (start > 0L && end > start) {

                            saveProbableSleep(start, end)

                            Log.d(
                                TAG,
                                "☀️ Wake confirmed. Sleep saved: ${Date(start)} -> ${Date(end)}"
                            )

                            DebugLogger.d(
                                TAG,
                                "Wake confirmed and sleep saved",
                                metadata = mapOf(
                                    "startMs" to start.toString(),
                                    "endMs" to end.toString(),
                                    "durationMin" to ((end - start) / 60000L).toString()
                                )
                            )
                        }

                        sleepStartMs = 0L
                        sleepEndMs = 0L
                        wakeMotionStartMs = 0L
                    }
                }

            } else {

                // Hareket kesildi
                wakeMotionStartMs = 0L
            }

            // ------------------------------------------------
            // AUTO SLEEP START
            // ------------------------------------------------

            if (
                !inSleepMode &&
                lastMotionMs > 0L &&
                (now - lastMotionMs) > SLEEP_START_AFTER_MS
            ) {

                inSleepMode = true

                sleepStartMs = lastMotionMs
                sleepEndMs = 0L
                wakeMotionStartMs = 0L

                persistRuntimeState()

                Log.d(
                    TAG,
                    "🌙 Motion sleep mode started at ${Date(sleepStartMs)}"
                )

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
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
    }


    private fun persistRuntimeState() {
        serviceScope.launch {
            try {
                stepforgeStore.edit { prefs ->

                    prefs[RUNTIME_SLEEP_MODE] = inSleepMode
                    prefs[RUNTIME_SLEEP_START] = sleepStartMs
                    prefs[RUNTIME_SLEEP_END] = sleepEndMs
                    prefs[RUNTIME_WAKE_MOTION_START] = wakeMotionStartMs

                    prefs[RUNTIME_IS_WALKING] = isWalking
                    prefs[RUNTIME_SESSION_START_TIME] = sessionStartTime
                    prefs[RUNTIME_SESSION_START_STEPS] = sessionStartSteps
                }
            } catch (e: Exception) {
                Log.e(TAG, "persistRuntimeState failed", e)
            }
        }
    }


    private fun saveProbableSleep(startMs: Long, endMs: Long) {
        serviceScope.launch {
            try {
                stepforgeStore.edit { prefs ->
                    prefs[PROBABLE_SLEEP_READY] = true
                    prefs[PROBABLE_SLEEP_START] = startMs
                    prefs[PROBABLE_SLEEP_END] = endMs
                }
            } catch (e: Exception) {
                Log.e(TAG, "saveProbableSleep failed", e)
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

        val rescueHours = StreakShieldEngine.getPremiumRescueHours()  // 24h
        val rescueMinutes = rescueHours * 60

        stepforgeStore.edit {
            it[StreakShieldPrefs.PREMIUM_RESCUE_MONTH] = currentMonth
            it[StreakShieldPrefs.PREMIUM_RESCUE_USED_COUNT] = usedCount + 1
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = rescueMinutes  // ✅ 24h = 1440 dakika
            it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = rescueMinutes
            it[StreakShieldPrefs.SHIELD_LAST_DECAY_AT_MS] = System.currentTimeMillis()
            it[StreakShieldPrefs.SHIELD_GENERATED_FOR_DATE] = shieldDateKey()
            it[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] = shieldDateKey()
        }

        DebugLogger.i(
            TAG,
            "Premium rescue consumed",
            metadata = mapOf(
                "currentMonth" to currentMonth,
                "newUsedCount" to (usedCount + 1).toString(),
                "rescueHours" to rescueHours.toString(),
                "rescueMinutes" to rescueMinutes.toString()
            )
        )

        return true
    }

    private suspend fun saveTomorrowShieldFromTodaySteps(todaySteps: Int) {
        val goal = cachedGoal
        val premium = isPremiumEnabled()

        val result = StreakShieldEngine.calculateDailyEarnedShieldHours(
            steps = todaySteps,
            goal = goal,
            isPremium = premium
        )

        // ✅ Hem bugünkü hem yarının shield'ını kaydet
        stepforgeStore.edit {
            // ✅ Bugünkü shield (gece yarısı carry-over için)
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = result.finalHours * 60
            it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = result.maxHours * 60

            // ✅ Yarının shield breakdown
            it[StreakShieldPrefs.SHIELD_TOMORROW_BASE_HOURS] = result.baseHours
            it[StreakShieldPrefs.SHIELD_TOMORROW_GOAL_BONUS_HOURS] = result.goalBonusHours
            it[StreakShieldPrefs.SHIELD_TOMORROW_FINAL_HOURS] = result.finalHours
            it[StreakShieldPrefs.SHIELD_TOMORROW_MAX_HOURS] = result.maxHours
        }

        DebugLogger.i(
            TAG,
            "Today & tomorrow shield calculated",
            metadata = mapOf(
                "todaySteps" to todaySteps.toString(),
                "goal" to goal.toString(),
                "isPremium" to premium.toString(),
                "baseHours" to result.baseHours.toString(),
                "goalBonusHours" to result.goalBonusHours.toString(),
                "finalHours" to result.finalHours.toString(),
                "maxHours" to result.maxHours.toString(),
                "todayMinutes" to (result.finalHours * 60).toString()
            )
        )
    }

    private suspend fun updateTodayShieldFromCurrentSteps(currentSteps: Int) {
        val prefs = stepforgeStore.data.first()
        val goal = cachedGoal
        val premium = isPremiumEnabled()

        val shieldGeneratedForDate = prefs[StreakShieldPrefs.SHIELD_GENERATED_FOR_DATE] ?: ""
        val today = shieldDateKey()

        // ✅ Eğer bugün için zaten shield oluşturulduysa, güncelleme
        if (shieldGeneratedForDate == today) {
            val result = StreakShieldEngine.calculateDailyEarnedShieldHours(
                steps = currentSteps,
                goal = goal,
                isPremium = premium
            )

            val newMinutes = result.finalHours * 60
            val currentMinutes = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0

            // ✅ Sadece artıyorsa güncelle (azalmayı drain hallediyor)
            if (newMinutes > currentMinutes) {
                stepforgeStore.edit {
                    it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = newMinutes
                    it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = result.maxHours * 60
                }

                DebugLogger.i(
                    TAG,
                    "Today shield updated from current steps",
                    metadata = mapOf(
                        "currentSteps" to currentSteps.toString(),
                        "oldMinutes" to currentMinutes.toString(),
                        "newMinutes" to newMinutes.toString(),
                        "baseHours" to result.baseHours.toString(),
                        "goalBonusHours" to result.goalBonusHours.toString()
                    )
                )
            }
        } else {
            // ✅ İlk kez bugün için shield oluşturuluyor
            val result = StreakShieldEngine.calculateDailyEarnedShieldHours(
                steps = currentSteps,
                goal = goal,
                isPremium = premium
            )

            val newMinutes = result.finalHours * 60

            stepforgeStore.edit {
                it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = newMinutes
                it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = result.maxHours * 60
                it[StreakShieldPrefs.SHIELD_GENERATED_FOR_DATE] = today
                it[StreakShieldPrefs.SHIELD_LAST_DECAY_AT_MS] = System.currentTimeMillis()
            }

            DebugLogger.i(
                TAG,
                "Today shield initialized for the first time",
                metadata = mapOf(
                    "currentSteps" to currentSteps.toString(),
                    "shieldMinutes" to newMinutes.toString(),
                    "baseHours" to result.baseHours.toString(),
                    "goalBonusHours" to result.goalBonusHours.toString()
                )
            )
        }
    }

    private suspend fun activateTodayShieldFromStoredTomorrow() {
        val prefs = stepforgeStore.data.first()

        val finalHours = prefs[StreakShieldPrefs.SHIELD_TOMORROW_FINAL_HOURS] ?: 0
        val maxHours = prefs[StreakShieldPrefs.SHIELD_TOMORROW_MAX_HOURS] ?: 0
        val isPremium = (prefs[intPreferencesKey("premium_enabled")] ?: 0) == 1

        // ✅ 1. Dünden kalan shield'ı al
        val yesterdayMinutesLeft = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0

        // ✅ 2. Premium carry-over hesapla
        val carryOverMinutes = if (isPremium && yesterdayMinutesLeft > 0) {
            (yesterdayMinutesLeft.coerceAtMost(4 * 60))  // Max 4 saat
        } else {
            0
        }

        // ✅ 3. Toplam shield hesapla (earned + carry-over, ama cap'i aşmasın)
        val earnedMinutes = finalHours * 60
        val totalMinutesBeforeCap = earnedMinutes + carryOverMinutes

        val premiumCapMinutes = if (isPremium) 16 * 60 else 12 * 60
        val finalMinutes = totalMinutesBeforeCap.coerceAtMost(premiumCapMinutes)

        val maxMinutes = maxHours * 60

        stepforgeStore.edit {
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = finalMinutes
            it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = maxMinutes
            it[StreakShieldPrefs.SHIELD_LAST_DECAY_AT_MS] = System.currentTimeMillis()
            it[StreakShieldPrefs.SHIELD_GENERATED_FOR_DATE] = shieldDateKey()
        }

        DebugLogger.i(
            TAG,
            "Today shield activated with carry-over",
            metadata = mapOf(
                "earnedMinutes" to earnedMinutes.toString(),
                "carryOverMinutes" to carryOverMinutes.toString(),
                "finalMinutes" to finalMinutes.toString(),
                "maxMinutes" to maxMinutes.toString(),
                "isPremium" to isPremium.toString(),
                "date" to shieldDateKey()
            )
        )
    }


    private fun startStreakBehaviorTicker() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val lastStepTime = stepforgeStore.data.first()[LAST_STEP_TIME] ?: System.currentTimeMillis()
                    StreakBehaviorEngine.tickBehavior(
                        context = applicationContext,
                        todaySteps = totalSteps,
                        goal = cachedGoal,
                        lastStepTimeMs = lastStepTime
                    )
                    StreakBehaviorEngine.checkAndMarkLostIfNeeded(
                        context = applicationContext,
                        goal = cachedGoal,
                        todaySteps = totalSteps
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Streak behavior ticker error", e)
                    DebugLogger.e(TAG, "Streak behavior ticker error", e)
                }

                delay(60_000L)
            }
        }
    }


    private suspend fun didTodayCountForStreak(): Boolean {
        val prefs = stepforgeStore.data.first()
        val goal = cachedGoal
        val shieldMinutesLeft = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
        val rescueDate = prefs[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] ?: ""
        val rescueUsedToday = rescueDate == shieldDateKey()

        val rescuedUntil = prefs[com.example.stepforge.ui.streak.StreakBehaviorPrefs.STREAK_RESCUED_UNTIL_MS] ?: 0L
        val result = StreakDayQualifier.qualifyDay(
            steps = totalSteps,
            goal = goal,
            behaviorBufferMinutes = shieldMinutesLeft,
            rescueUsedForDay = rescueUsedToday,
            rescuedActive = rescuedUntil > System.currentTimeMillis()
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
        val now = System.currentTimeMillis()
        if (now - lastCoachRunMs < 5 * 60 * 1000L) return  // ✅ 5 dakikada bir çalışsın
        lastCoachRunMs = now

        serviceScope.launch {
            try {
                val prefs = stepforgeStore.data.first()

                val isPremium = (prefs[intPreferencesKey("premium_enabled")] ?: 0) == 1
                val aiCoachEnabled = prefs[StreakShieldPrefs.PREMIUM_AI_COACH_ENABLED] ?: false
                val behavior = StreakBehaviorEngine.readSnapshot(applicationContext)

                if (!isPremium || !aiCoachEnabled) return@launch

                val goal = cachedGoal

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
                        streakBehaviorState = behavior.state,
                        streakHealthPercent = behavior.healthPercent,
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
                        "streakState" to behavior.state.name,
                        "streakHealth" to behavior.healthPercent.toString(),
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
