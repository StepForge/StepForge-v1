@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stepforge


import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.DirectionsWalk
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.steps.CentralStepState
import com.example.stepforge.ui.components.GoalKonfetti
import com.example.stepforge.ui.components.HealthConnectImportCoordinator
import com.example.stepforge.ui.components.HealthConnectState
import com.example.stepforge.ui.components.HealthSyncManager
import com.example.stepforge.ui.stepforgeTheme
import com.example.stepforge.ui.streak.StreakBehaviorEngine
import com.google.firebase.Firebase
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.analytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity() {

    private lateinit var analytics: FirebaseAnalytics

    // App Lock keys
    private val KEY_APP_LOCK_ENABLED = intPreferencesKey("app_lock_enabled")
    private val KEY_APP_LOCK_TIMEOUT = intPreferencesKey("app_lock_timeout")
    private val KEY_APP_LOCK_LAST_BG = longPreferencesKey("app_lock_last_bg")
    private val KEY_APP_LOCK_SESSION_UNLOCKED = intPreferencesKey("app_lock_session_unlocked")

    @Volatile
    private var isLockScreenShowing: Boolean = false

    private suspend fun getAppLockTimeoutSeconds(): Int {
        val prefs = applicationContext.stepforgeStore.data.first()
        return prefs[KEY_APP_LOCK_TIMEOUT] ?: 0
    }

    private suspend fun getLastBackgroundTime(): Long {
        val prefs = applicationContext.stepforgeStore.data.first()
        return prefs[KEY_APP_LOCK_LAST_BG] ?: 0L
    }

    private suspend fun setLastBackgroundTime(value: Long) {
        applicationContext.stepforgeStore.edit { prefs ->
            prefs[KEY_APP_LOCK_LAST_BG] = value
        }
    }

    private suspend fun isAppLockEnabled(): Boolean {
        val prefs = applicationContext.stepforgeStore.data.first()
        return (prefs[KEY_APP_LOCK_ENABLED] ?: 0) == 1
    }

    private suspend fun isSessionUnlocked(): Boolean {
        val prefs = applicationContext.stepforgeStore.data.first()
        return (prefs[KEY_APP_LOCK_SESSION_UNLOCKED] ?: 0) == 1
    }

    private suspend fun setSessionUnlocked(unlocked: Boolean) {
        applicationContext.stepforgeStore.edit { prefs ->
            prefs[KEY_APP_LOCK_SESSION_UNLOCKED] = if (unlocked) 1 else 0
        }
    }

    private val lockLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        isLockScreenShowing = false
        if (result.resultCode != RESULT_OK) {
            finish()
        } else {
            lifecycleScope.launch {
                setSessionUnlocked(true)
            }
        }
    }

    private fun requestIgnoreBatteryOptimizationsOnce() {
        try {
            val prefs = getSharedPreferences("stepforge_prefs", MODE_PRIVATE)
            val askedBefore = prefs.getBoolean("battery_opt_asked", false)
            if (askedBefore) return

            // ✅ API 23+ guard (senin minSdk 29 ama IDE kafası karışıyorsa)
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            val pkg = packageName

            if (!pm.isIgnoringBatteryOptimizations(pkg)) {
                Log.w("StepForge", "Battery optimization is ON. Requesting whitelist...")

                prefs.edit().putBoolean("battery_opt_asked", true).apply()

                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$pkg")
                }
                startActivity(intent)
            } else {
                Log.d("StepForge", "Battery optimization already ignored.")
            }

        } catch (e: Exception) {
            Log.e("StepForge", "Battery optimization request failed", e)
        }
    }



    private val KEY_ONBOARDING_DONE = intPreferencesKey("onboarding_done")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ✅ Hemen UI'ı aç, loading göster
        setContent {
            MainRoot()
        }

        analytics = Firebase.analytics

        lifecycleScope.launch {
            val prefs = applicationContext.stepforgeStore.data.first()
            val seen = (prefs[KEY_ONBOARDING_DONE] ?: 0) == 1

            if (!seen) {
                startActivity(Intent(this@MainActivity, OnboardingActivity::class.java))
                finish()
                return@launch
            }

            requestIgnoreBatteryOptimizationsOnce()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = getSystemService(AlarmManager::class.java)
                if (am != null && !am.canScheduleExactAlarms()) {
                    try {
                        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    } catch (_: Exception) {
                    }
                }
            }

            askPermissionsAndStartService()
        }
    }



    override fun onStart() {
        super.onStart()

        lifecycleScope.launch {
            ensureMidnightResetConsistency()

            if (!isAppLockEnabled()) return@launch
            if (isLockScreenShowing) return@launch

            val timeout = getAppLockTimeoutSeconds()
            val lastBg = getLastBackgroundTime()
            val now = System.currentTimeMillis()

            if (lastBg == 0L) return@launch

            val diffSeconds = (now - lastBg) / 1000L
            val shouldLock = when (timeout) {
                0 -> true
                else -> diffSeconds >= timeout
            }

            if (shouldLock && !isSessionUnlocked()) {
                isLockScreenShowing = true
                val intent = Intent(this@MainActivity, LockActivity::class.java)
                lockLauncher.launch(intent)
            }
        }
    }

    override fun onStop() {
        super.onStop()

        lifecycleScope.launch {
            setLastBackgroundTime(System.currentTimeMillis())
            setSessionUnlocked(false)
        }
    }

    private fun askPermissionsAndStartService() {
        val need = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            need.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                need.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (need.isNotEmpty()) {
            // ✅ FIX: permissions değil, need
            permissionLauncher.launch(need.toTypedArray())
        } else {
            startStepService()
        }
    }

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val activityGranted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

            if (!activityGranted) {
                Log.w("StepForge", "ACTIVITY_RECOGNITION denied. StepCounterService will not be started to avoid FGS crash.")
                return@registerForActivityResult
            }

            startStepService()
        }



    private fun startStepService() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.w("StepForge", "StepCounterService start blocked: ACTIVITY_RECOGNITION missing.")
            return
        }

        val work = PeriodicWorkRequestBuilder<StepCounterRestartWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "StepCounterRestartWorker",
            ExistingPeriodicWorkPolicy.KEEP,
            work
        )

        val serviceIntent = Intent(this, StepCounterService::class.java)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (e: Exception) {
            Log.e("StepForge", "Service start failed!", e)
        }
    }

    private suspend fun hasServiceResetToday(): Boolean {
        val prefs = applicationContext.stepforgeStore.data.first()
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            .format(java.util.Date())
        return (prefs[StepCounterService.LAST_RESET_DATE] ?: "") == today
    }

    private suspend fun ensureMidnightResetConsistency() {
        try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val today = sdf.format(java.util.Date())

            if (hasServiceResetToday()) {
                Log.d("StepForge", "ensureMidnightResetConsistency: already reset today, skipping.")
                return
            }

            val prefs = applicationContext.stepforgeStore.data.first()
            val last = prefs[StepCounterService.LAST_RESET_DATE] ?: ""

            if (last == today) {
                Log.d("StepForge", "ensureMidnightResetConsistency: LAST_RESET_DATE already today, skipping.")
                return
            }

            val persistedSum = prefs[intPreferencesKey("persisted_total_sum")] ?: -1
            if (persistedSum == 0) {
                Log.d("StepForge", "ensureMidnightResetConsistency: persisted_total_sum=0, skipping force reset.")
                return
            }

            Log.w("StepForge", "Missed midnight reset detected. Forcing reset. last=$last today=$today")

            val resetIntent = Intent(this, StepCounterService::class.java).apply {
                putExtra("forceReset", true)
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(resetIntent)
            } else {
                startService(resetIntent)
            }
        } catch (e: Exception) {
            Log.e("StepForge", "ensureMidnightResetConsistency error", e)
        }
    }
}

/* ======================= MAIN ROOT ======================= */

@Composable
private fun MainRoot() {
    val ctx = LocalContext.current

    val themeKey = stringPreferencesKey("theme_mode")
    val themeFlow = remember {
        ctx.stepforgeStore.data.map { prefs -> prefs[themeKey] ?: "system" }
    }
    val themeValue by themeFlow.collectAsState(initial = "system")

    val useDark = when (themeValue) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    stepforgeTheme(darkTheme = useDark) {
        MainHomeScreen()
    }
}

/* ======================= HOME SCREEN (NEW DESIGN) ======================= */

@Composable
private fun MainHomeScreen() {

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val KEY_MANUAL_OVERRIDE = intPreferencesKey("manual_override_enabled")
    val manualOverrideFlow = remember {
        ctx.stepforgeStore.data.map { it[KEY_MANUAL_OVERRIDE] ?: 0 }
    }
    val manualOverrideEnabled by manualOverrideFlow.collectAsState(initial = 0)

    val stepSnapshot by CentralStepState.snapshot.collectAsState()
    val realSteps = stepSnapshot.steps
    val target = stepSnapshot.goal

    val hcCoordinator = remember { HealthConnectImportCoordinator(ctx) }
    val hcStatus by HealthConnectState.status.collectAsState()

    val displaySteps = remember(realSteps, manualOverrideEnabled) {
        realSteps // Artık CentralStepState tüm kaynakları (sensor + HC) birleştirmiş olmalı
    }

    val vibrationContext = ctx

    LaunchedEffect(displaySteps, target, vibrationContext) {
        StepCounterService.updateServiceNotification(vibrationContext, displaySteps, target)
    }

    var celebrating by remember { mutableStateOf(false) }
    var showGoalKonfetti by remember { mutableStateOf(false) }

    // STREAK state
    var streak by remember { mutableStateOf(0) }
    LaunchedEffect(displaySteps, target) {
        streak = calculateStreak(ctx, target, displaySteps)
    }

    DisposableEffect(hcCoordinator) {
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                scope.launch {
                    try {
                        val healthManager = HealthSyncManager(ctx)
                        val connected = healthManager.isFullyConnected()
                        val permissions = healthManager.hasEssentialPermissions()

                        HealthConnectState.update(
                            HealthConnectState.Status(
                                isInstalled = true, // Client created successfully
                                isConnected = connected,
                                permissionsGranted = permissions,
                                lastSyncTime = System.currentTimeMillis()
                            )
                        )

                        if (permissions && manualOverrideEnabled == 0) {
                            hcCoordinator.syncSteps()
                        }
                    } catch (_: Exception) {
                    }
                }
            }
        }
        val activity = ctx as? ComponentActivity
        activity?.lifecycle?.addObserver(lifecycleObserver)
        onDispose { activity?.lifecycle?.removeObserver(lifecycleObserver) }
    }

    LaunchedEffect(realSteps, target, vibrationContext) {
        if (realSteps >= target && !celebrating) {
            celebrating = true
            showGoalKonfetti = true
            vibrateOnce(vibrationContext)
            delay(3000)
            celebrating = false
        }
    }

    val cs = colorScheme

    Scaffold(
        containerColor = cs.background,
        topBar = { HomeTopBar() },
        bottomBar = {
            HomeBottomBar(
                selectedIndex = 0,
                onSelect = { index ->
                    when (index) {
                        0 -> Unit
                        1 -> ctx.startActivity(Intent(ctx, HistoryActivity::class.java))
                        2 -> ctx.startActivity(Intent(ctx, WaterReminderActivity::class.java))
                        3 -> ctx.startActivity(Intent(ctx, InsightsActivity::class.java))
                        4 -> ctx.startActivity(Intent(ctx, ProfileSettingsActivity::class.java))
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.background,
                            cs.background.copy(alpha = 0.98f)
                        )
                    )
                )
                .padding(padding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                MainHeroCard(
                    steps = displaySteps,
                    target = target,
                    celebrating = celebrating,
                    streak = streak
                )

                DailyWeeklyRow(
                    dailyAvg = displaySteps,
                    weeklyKcal = (displaySteps * 0.04).roundToInt()
                )

                SleepSummaryCard()
                WorkoutsHomeCard()
                Spacer(modifier = Modifier.height(8.dp))


            }

            GoalKonfetti(
                visible = showGoalKonfetti,
                modifier = Modifier.fillMaxSize(),
                onFinished = { showGoalKonfetti = false }
            )
        }
    }
}

/* ---------- Top bar ---------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeTopBar() {
    val cs = MaterialTheme.colorScheme
    val ctx = LocalContext.current

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = "StepForge",
                color = cs.onBackground,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold
            )
        },
        navigationIcon = {
            // SOLDA INSIGHTS ICONU
            IconButton(
                onClick = {
                    ctx.startActivity(Intent(ctx, ProgressActivity::class.java))
                }
            ) {
                // Şimdilik Material icon kullanalım (light/dark uyumlu)
                Icon(
                    imageVector = Icons.Outlined.Insights,
                    contentDescription = "Insights",
                    tint = cs.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        actions = {
            // SAĞDA SETTINGS ICONU
            IconButton(
                onClick = {
                    ctx.startActivity(Intent(ctx, SettingsActivity::class.java))
                }
            ) {
                Icon(
                    imageVector = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = cs.onBackground,
                    modifier = Modifier.size(22.dp)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent
        )
    )
}


/* ---------- Bottom Nav ---------- */

@Composable
private fun HomeBottomBar(
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val cs = colorScheme
    val items = listOf("Home", "History", "Water", "Insights", "Profile")
    val icons: List<ImageVector> = listOf(
        Icons.Filled.Home,
        Icons.Filled.History,
        Icons.Filled.WaterDrop,
        Icons.Outlined.BarChart,
        Icons.Filled.Person
    )
    val accents = listOf(
        Color(0xFF00F5FF),
        Color(0xFFA970FF),
        Color(0xFF2EA8FF),
        Color(0xFFFFB340),
        Color(0xFF00FFA3)
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(68.dp)
                .shadow(
                    elevation = 18.dp,
                    shape = RoundedCornerShape(30.dp),
                    ambientColor = Color.Black.copy(alpha = 0.34f),
                    spotColor = Color(0xFF00F5FF).copy(alpha = 0.10f)
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.White.copy(alpha = 0.07f),
                            cs.primary.copy(alpha = 0.18f),
                            Color(0xFF00FFA3).copy(alpha = 0.10f),
                            Color.White.copy(alpha = 0.06f)
                        )
                    ),
                    shape = RoundedCornerShape(30.dp)
                ),
            shape = RoundedCornerShape(30.dp),
            color = Color(0xFF090D12).copy(alpha = 0.98f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items.forEachIndexed { index, label ->
                    val selected = selectedIndex == index
                    val accent = accents[index]
                    val selectedProgress by animateFloatAsState(
                        targetValue = if (selected) 1f else 0f,
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        label = "premiumBottomSelected"
                    )
                    val scale by animateFloatAsState(
                        targetValue = if (selected) 1.04f else 0.96f,
                        animationSpec = tween(260, easing = FastOutSlowInEasing),
                        label = "premiumBottomScale"
                    )

                    Box(
                        modifier = Modifier
                            .weight(if (selected) 1.32f else 1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                brush = Brush.linearGradient(
                                    listOf(
                                        accent.copy(alpha = 0.06f * selectedProgress),
                                        Color(0xFF101821).copy(alpha = 0.22f * selectedProgress),
                                        Color(0xFF05080D).copy(alpha = 0.02f)
                                    )
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    listOf(
                                        accent.copy(alpha = 0.30f * selectedProgress),
                                        Color.White.copy(alpha = 0.06f * selectedProgress),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable { onSelect(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    alpha = 0.72f + selectedProgress * 0.28f
                                )
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(if (selected) 32.dp else 28.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = if (selected) {
                                                listOf(
                                                    accent.copy(alpha = 0.28f),
                                                    accent.copy(alpha = 0.12f),
                                                    Color.Transparent
                                                )
                                            } else {
                                                listOf(
                                                    Color.White.copy(alpha = 0.05f),
                                                    Color.Transparent
                                                )
                                            }
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = icons[index],
                                    contentDescription = label,
                                    modifier = Modifier.size(if (selected) 19.dp else 18.dp),
                                    tint = if (selected) accent else cs.onSurface.copy(alpha = 0.58f)
                                )
                            }

                            Spacer(Modifier.height(2.dp))

                            Text(
                                text = label,
                                fontSize = if (selected) 9.sp else 8.4.sp,
                                lineHeight = 9.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                maxLines = 1,
                                color = if (selected) accent else cs.onSurface.copy(alpha = 0.48f)
                            )

                            Spacer(Modifier.height(3.dp))

                            Box(
                                modifier = Modifier
                                    .width(if (selected) 18.dp else 4.dp)
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (selected) accent.copy(alpha = 0.95f)
                                        else Color.Transparent
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}


/* ---------- Main hero (goal badge + ring + mini streak + stats) ---------- */

@Composable
private fun MainHeroCard(
    steps: Int,
    target: Int,
    celebrating: Boolean,
    streak: Int
) {
    val cs = colorScheme
    val ctx = LocalContext.current
    val neonA = Color(0xFF00FFA3)
    val neonB = Color(0xFF00F5FF)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 290.dp)
            .shadow(10.dp, RoundedCornerShape(28.dp)),    // shadow azaltıldı
        shape = RoundedCornerShape(28.dp),
        color = cs.surface
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            cs.surface.copy(alpha = 1f),
                            cs.surfaceVariant.copy(alpha = 0.9f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DailyGoalLeftBadge(steps = steps, target = target)
                    StreakBadge(
                        streak = streak,
                        onClick = {
                            ctx.startActivity(Intent(ctx, StreakActivity::class.java))
                        }
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HeroRing(
                        steps = steps,
                        target = target,
                        celebrating = celebrating,
                        neonA = neonA,
                        neonB = neonB
                    )
                }

                DistanceActiveRow(steps = steps)
            }
        }
    }
}

@Composable
private fun DailyGoalLeftBadge(
    steps: Int,
    target: Int
) {
    val cs = colorScheme

    val left = (target - steps).coerceAtLeast(0)
    val label = if (left <= 0) "GOAL DONE" else "${NumberFormat.getIntegerInstance().format(left)} LEFT"

    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFF0B707F),
                            Color(0xFF20C4A1)
                        )
                    )
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = label,
                color = Color(0xFFE6FDFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Text(
            text = "Daily Goal",
            color = cs.onSurface.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}


@Composable
private fun StreakBadge(
    streak: Int,
    onClick: () -> Unit
) {
    val cs = colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val streakLabel = if (streak <= 0) "0" else streak.toString()

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .padding(top = 2.dp, end = 2.dp)
            .clickable { onClick() }
    ) {

        Box(
            modifier = Modifier.size(36.dp),
            contentAlignment = Alignment.Center
        ) {

            Icon(
                painter = painterResource(id = R.drawable.ic_streak_calendar_vec),
                contentDescription = "Streak",
                tint = cs.primary,
                modifier = Modifier.size(36.dp)
            )

            Text(
                text = streakLabel,
                color = if (isDark) Color.White else cs.onSurface,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Text(
            text = "Streak",
            color = cs.onSurface.copy(alpha = 0.7f),
            fontSize = 11.sp
        )
    }
}

@Composable
private fun HeroRing(
    steps: Int,
    target: Int,
    celebrating: Boolean,
    neonA: Color,
    neonB: Color
) {
    val cs = colorScheme
    val rawProgress = steps.toFloat() / target.coerceAtLeast(1)
    val progress = rawProgress.coerceIn(0.04f, 1f)
    val animatedSweep by animateFloatAsState(
        targetValue = 260f * progress,
        animationSpec = tween(durationMillis = 900, easing = LinearEasing),
        label = "heroSweep"
    )

    val iconSize = 88.dp
    val formattedSteps = remember(steps) {
        NumberFormat.getIntegerInstance().format(steps)
    }

    Box(
        modifier = Modifier.size(230.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeOuter = 22f
            val strokeInner = 8f
            val radius = size.minDimension / 2f - strokeOuter
            val center = Offset(size.width / 2f, size.height / 2f)

            // Track
            drawArc(
                color = Color(0xFF20293A),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeOuter, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )

            // Inner dotted-like ring
            drawArc(
                color = Color(0x4020293A),
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeInner, cap = StrokeCap.Round),
                size = Size((radius - 20f) * 2, (radius - 20f) * 2),
                topLeft = Offset(center.x - (radius - 20f), center.y - (radius - 20f))
            )

            // Progress
            drawArc(
                brush = Brush.sweepGradient(
                    0f to neonA,
                    0.5f to neonB,
                    1f to neonA
                ),
                startAngle = 140f,
                sweepAngle = animatedSweep,
                useCenter = false,
                style = Stroke(width = strokeOuter, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset(center.x - radius, center.y - radius)
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_walk),
                contentDescription = "runner",
                modifier = Modifier.size(iconSize),
                contentScale = ContentScale.Fit
            )

            Text(
                text = formattedSteps,
                color = cs.onSurface,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Today's Steps",
                color = cs.onSurface.copy(alpha = 0.7f),
                fontSize = 13.sp
            )
        }
    }
}


/* ---------- Distance / Active row (hero kartın içi) ---------- */

@Composable
private fun DistanceActiveRow(
    steps: Int
) {
    val cs = colorScheme
    val distanceKm = (steps * 0.75) / 1000.0
    val activeMinutes = (steps / 110.0).roundToInt()

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                text = "Distance",
                color = cs.onSurface.copy(alpha = 0.65f),
                fontSize = 11.sp
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = String.format("%.1f", distanceKm),
                    color = cs.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "km",
                    color = cs.onSurface,
                    fontSize = 13.sp
                )
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "Active Time",
                color = cs.onSurface.copy(alpha = 0.65f),
                fontSize = 11.sp
            )
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = activeMinutes.toString(),
                    color = cs.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "min",
                    color = cs.onSurface,
                    fontSize = 13.sp
                )
            }
        }
    }
}


/* ---------- Günlük ortalama / Haftalık toplam ---------- */

@Composable
private fun DailyWeeklyRow(
    dailyAvg: Int? = null,
    weeklyKcal: Int? = null
) {
    val cs = colorScheme
    if (dailyAvg == null && weeklyKcal == null) return

    val isDark = cs.background.luminance() < 0.5f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        dailyAvg?.let {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = if (isDark) 2.dp else 4.dp,
                shadowElevation = if (isDark) 4.dp else 6.dp,
                color = cs.surface
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    cs.surface.copy(alpha = if (isDark) 1f else 0.98f),
                                    cs.surfaceVariant.copy(alpha = if (isDark) 0.95f else 0.96f)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.DirectionsWalk,
                                contentDescription = null,
                                tint = cs.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Daily Average",
                                color = cs.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = buildAnnotatedString {
                                append(NumberFormat.getIntegerInstance().format(it))
                                append(" ")
                                withStyle(
                                    SpanStyle(
                                        color = cs.primary,
                                        fontSize = 13.sp
                                    )
                                ) { append("steps") }
                            },
                            color = cs.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        weeklyKcal?.let {
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(18.dp),
                tonalElevation = if (isDark) 2.dp else 4.dp,
                shadowElevation = if (isDark) 4.dp else 6.dp,
                color = cs.surface
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    cs.surface.copy(alpha = if (isDark) 1f else 0.98f),
                                    cs.surfaceVariant.copy(alpha = if (isDark) 0.95f else 0.96f)
                                )
                            )
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = cs.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Weekly Total",
                                color = cs.onSurfaceVariant,
                                fontSize = 11.sp
                            )
                        }

                        Text(
                            text = buildAnnotatedString {
                                append(NumberFormat.getIntegerInstance().format(it))
                                append(" ")
                                withStyle(
                                    SpanStyle(
                                        color = cs.secondary,
                                        fontSize = 13.sp
                                    )
                                ) { append("kcal") }
                            },
                            color = cs.onSurface,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}


/* ---------- Sleep card ---------- */

@Composable
private fun SleepSummaryCard() {
    val cs = colorScheme
    val ctx = LocalContext.current

    // Bugünün tarihi
    val today = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.format(java.util.Date())
    }

    // Flow üzerinden canlı dinleme
    val sessionFlow = remember {
        AppDatabase.getDatabase(ctx).sleepSessionDao().observeSessionForDate(today)
    }
    val session by sessionFlow.collectAsState(initial = null)

    val totalMin = session?.totalMinutes ?: 0
    val hours = totalMin / 60
    val mins = totalMin % 60
    val quality = session?.qualityScore ?: 0
    val hasData = session != null

    val targetMinutes = 8 * 60f
    val progressTarget =
        if (totalMin <= 0) 0f else (totalMin / targetMinutes).coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = progressTarget,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "sleepProgress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable {
                ctx.startActivity(Intent(ctx, SleepActivity::class.java))
            },
        shape = RoundedCornerShape(22.dp),
        color = cs.surface
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            cs.surface.copy(alpha = 1f),
                            cs.surfaceVariant.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Bedtime,
                        contentDescription = null,
                        tint = Color(0xFFE3B5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Sleep",
                        color = Color(0xFFE3B5FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = if (hasData) "${hours}h ${mins}m" else "No data",
                    color = cs.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = cs.outlineVariant)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val timeFormat = remember {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                }

                val start = session?.startTime?.let {
                    timeFormat.format(java.util.Date(it))
                } ?: "--:--"

                val end = session?.endTime?.let {
                    timeFormat.format(java.util.Date(it))
                } ?: "--:--"

                SleepMiniBox("Start", start)
                SleepMiniBox("End", end)
                SleepMiniBox("Quality", if (hasData) quality.toString() else "--")
            }

            Spacer(modifier = Modifier.height(9.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(cs.surfaceVariant)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    cs.secondary,
                                    cs.primary
                                )
                            )
                        )
                )
            }
        }
    }
}


@Composable
private fun WorkoutsHomeCard() {
    val cs = colorScheme
    val ctx = LocalContext.current

    val today = remember {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.format(java.util.Date())
    }

    val dao = remember { AppDatabase.getDatabase(ctx).workoutSessionDao() }
    var todaySessions by remember { mutableStateOf<List<com.example.stepforge.data.WorkoutSession>>(emptyList()) }

    LaunchedEffect(Unit) {
        dao.observeAllForDate(today).collect { list ->
            todaySessions = list
        }
    }

    val totalMinutes = remember(todaySessions) { todaySessions.sumOf { it.durationMinutes } }
    val totalSteps = remember(todaySessions) { todaySessions.sumOf { it.steps } }
    val totalCalories = remember(todaySessions) { todaySessions.sumOf { it.caloriesKcal } }
    val totalDistanceKm = remember(todaySessions) {
        todaySessions.sumOf { it.distanceMeters } / 1000f
    }

    val df = remember { java.text.DecimalFormat("#.#") }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 150.dp)
            .shadow(6.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .clickable {
                ctx.startActivity(Intent(ctx, WorkoutsActivity::class.java))
            },
        shape = RoundedCornerShape(22.dp),
        color = if (cs.background.luminance() < 0.5f) cs.surface else Color.White
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            cs.surface.copy(alpha = 1f),
                            cs.surfaceVariant.copy(alpha = 0.96f)
                        )
                    )
                )
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.run_workout),
                        contentDescription = null,
                        tint = Color(0xFF00F5FF),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.workouts_today_card_title),
                        color = Color(0xFF00F5FF),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = if (todaySessions.isEmpty()) "0" else todaySessions.size.toString(),
                    color = cs.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Divider(color = cs.outlineVariant)

            if (todaySessions.isEmpty()) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.workouts_today_card_empty),
                    color = cs.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = androidx.compose.ui.res.stringResource(
                        R.string.workouts_today_card_minutes,
                        totalMinutes
                    ),
                    color = cs.onSurface,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = androidx.compose.ui.res.stringResource(
                        R.string.workouts_today_card_steps,
                        totalSteps,
                        df.format(totalDistanceKm),
                        totalCalories
                    ),
                    color = cs.onSurface.copy(alpha = 0.75f),
                    fontSize = 12.sp
                )
            }
        }
    }
}


@Composable
private fun SleepMiniBox(
    label: String,
    value: String
) {
    val cs = colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            color = cs.onSurface.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
        Text(
            text = value,
            color = cs.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


/* ======================= VIBRATION HELPER ======================= */

fun vibrateOnce(ctx: Context) {
    try {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager =
                ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(
                VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            )
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    } catch (e: Exception) {
        Log.e("stepforge", "Vibration error", e)
    }
}

private suspend fun calculateStreak(
    context: Context,
    goal: Int,
    todaySteps: Int
): Int {
    return withContext(Dispatchers.IO) {
        try {
            StreakBehaviorEngine.computeQuickStreakDays(
                context = context.applicationContext,
                goal = goal,
                todaySteps = todaySteps
            )
        } catch (e: Exception) {
            Log.e("StepForge", "Streak calculation error", e)
            0
        }
    }
}