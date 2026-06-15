package com.example.stepforge

import androidx.compose.ui.res.stringResource

import android.os.Bundle
import android.widget.Toast
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.core.content.ContextCompat
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.DailyWater
import com.example.stepforge.data.HourlySteps
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.rememberUseDarkTheme
import com.example.stepforge.ui.stepforgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

class HistoryDetailActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val date = intent.getStringExtra("date") ?: ""
        val steps = intent.getIntExtra("steps", 0)
        val waterMl = intent.getIntExtra("waterMl", 0)

        setContent {
            stepforgeTheme(darkTheme = rememberUseDarkTheme(this)) {
                HistoryDetailScreen(
                    date = date,
                    initialSteps = steps,
                    initialWaterMl = waterMl,
                    onBack = { finish() }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryDetailScreen(
    date: String,
    initialSteps: Int,
    initialWaterMl: Int,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val darkBg = cs.background
    val cardBg = cs.surface

    val neonA = if (isDark) Color(0xFF00FFA3) else Color(0xFF4FD1C5)
    val neonB = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
    val neon = Brush.horizontalGradient(listOf(neonA, neonB))

    val unknownDate = stringResource(R.string.hc_unknown_date)
    val prettyDate = remember(date, unknownDate) {
        if (date.isBlank()) return@remember unknownDate
        try {
            val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val sdfOut = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault())
            sdfOut.format(sdfIn.parse(date)!!)
        } catch (e: Exception) {
            date
        }
    }

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val dayUpdatedMessage = stringResource(R.string.h3_day_updated)
    var steps by remember(date, initialSteps) { mutableStateOf(initialSteps) }
    var waterMl by remember(date, initialWaterMl) { mutableStateOf(initialWaterMl) }
    var hourly by remember { mutableStateOf<List<HourlySteps>>(emptyList()) }
    var sleepSession by remember { mutableStateOf<SleepSession?>(null) }

    LaunchedEffect(date) {
        if (date.isNotBlank()) {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(ctx)
                val savedSteps = db.dailyStepsDao().getStepsForDate(date)?.steps ?: initialSteps
                val savedWater = db.dailyWaterDao().getWaterForDate(date)?.waterMl ?: initialWaterMl
                val loadedHourly = db.hourlyStepsDao().getForDate(date)
                val loadedSleep = db.sleepSessionDao().getSessionForDate(date)
                withContext(Dispatchers.Main) {
                    steps = savedSteps.coerceAtLeast(0)
                    waterMl = savedWater.coerceAtLeast(0)
                    hourly = loadedHourly
                    sleepSession = loadedSleep
                }
            }
        } else {
            steps = initialSteps.coerceAtLeast(0)
            waterMl = initialWaterMl.coerceAtLeast(0)
            hourly = emptyList()
            sleepSession = null
        }
    }

    fun selectedDateIsToday(): Boolean = date == LocalDate.now().toString()

    fun selectedDateAsInt(): Int {
        val parts = date.split("-")
        return if (parts.size == 3) {
            (parts[0].toIntOrNull() ?: 0) * 10000 +
                    (parts[1].toIntOrNull() ?: 0) * 100 +
                    (parts[2].toIntOrNull() ?: 0)
        } else 0
    }

    fun saveSelectedDayData(newSteps: Int, newWaterMl: Int) {
        if (date.isBlank()) return

        val safeSteps = newSteps.coerceAtLeast(0)
        val safeWater = newWaterMl.coerceAtLeast(0)

        scope.launch {
            withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(ctx)
                db.dailyStepsDao().insertDailySteps(DailySteps(date, safeSteps))
                db.dailyWaterDao().insertDailyWater(DailyWater(date = date, waterMl = safeWater))

                if (selectedDateIsToday()) {
                    val manualOverrideKey = intPreferencesKey("manual_override_enabled")
                    val waterTodayKey = intPreferencesKey("water_today_ml")
                    val waterDateKey = intPreferencesKey("water_date_yyyymmdd")
                    val dateInt = selectedDateAsInt()

                    ctx.stepforgeStore.edit { prefs ->
                        prefs[manualOverrideKey] = 1
                        prefs[waterTodayKey] = safeWater
                        if (dateInt > 0) prefs[waterDateKey] = dateInt
                    }

                    ContextCompat.startForegroundService(
                        ctx,
                        Intent(ctx, StepCounterService::class.java).apply {
                            putExtra("manualSteps", safeSteps)
                        }
                    )
                    StepCounterService.updateServiceNotification(ctx, safeSteps, 10000)
                }
            }

            steps = safeSteps
            waterMl = safeWater
            Toast.makeText(ctx, dayUpdatedMessage, Toast.LENGTH_SHORT).show()
        }
    }

    val distanceKm = (steps * 0.8) / 1000.0
    val calories = steps * 0.032
    val activeMinutes = (steps / 110.0).roundToInt()

    // ✅ Water Reminder’daki goal’u buraya bağla (DataStore: water_goal_ml)
    val KEY_WATER_GOAL = intPreferencesKey("water_goal_ml")
    val waterGoalFlow = remember {
        ctx.stepforgeStore.data.map { prefs -> prefs[KEY_WATER_GOAL] ?: 2000 }
    }
    val waterGoalMl by waterGoalFlow.collectAsState(initial = 2000)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.hc_day_summary),
                            color = cs.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = prettyDate,
                            color = cs.onBackground.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.hc_back),
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBg)
                .padding(pad)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                DayRingCard(
                    steps = steps,
                    target = 10000,
                    distanceKm = distanceKm,
                    calories = calories,
                    neonA = neonA,
                    neonB = neonB,
                    cardBg = cardBg
                )

                StatsRowCard(
                    distanceKm = distanceKm,
                    calories = calories,
                    activeMinutes = activeMinutes,
                    waterMl = waterMl,
                    cardBg = cardBg
                )

                SleepDetailCard(
                    session = sleepSession,
                    cardBg = cardBg,
                    neon = neon
                )

                HourlyChartCard(
                    hourlyData = hourly,
                    cardBg = cardBg,
                    neon = neon
                )

                // ✅ Burada artık sabit 2000 değil, WaterReminder goal’u
                WaterDetailCard(
                    waterMl = waterMl,
                    dailyTargetMl = waterGoalMl,
                    cardBg = cardBg,
                    neon = neon
                )

                HistoryAdjustDayCard(
                    steps = steps,
                    waterMl = waterMl,
                    cardBg = cardBg,
                    onSave = { newSteps, newWaterMl ->
                        saveSelectedDayData(newSteps, newWaterMl)
                    }
                )

                TipsCard(cardBg = cardBg)
            }
        }
    }
}

/* --------- TOTAL STEPS CARD (gradient arc) --------- */

@Composable
private fun DayRingCard(
    steps: Int,
    target: Int,
    distanceKm: Double,
    calories: Double,
    neonA: Color,
    neonB: Color,
    cardBg: Color
) {
    val cs = MaterialTheme.colorScheme

    val progress = (steps.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(800, easing = FastOutSlowInEasing),
        label = "dayRingProgress"
    )

    val ringGradient = Brush.sweepGradient(listOf(neonA, neonB, neonA))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(26.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.hc_total_steps),
                color = cs.onSurface.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))

            Box(
                modifier = Modifier.size(220.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 26f
                    val radius = size.minDimension / 2f - strokeWidth

                    drawArc(
                        color = if (cs.background.luminance() < 0.5f) Color(0xFF202430) else Color(0xFFE2E8F0),
                        startAngle = -210f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2f,
                            (size.height - radius * 2) / 2f
                        )
                    )

                    drawArc(
                        brush = ringGradient,
                        startAngle = -210f,
                        sweepAngle = 240f * animated,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2f,
                            (size.height - radius * 2) / 2f
                        )
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = steps.toString(),
                        color = cs.onSurface,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = stringResource(R.string.widget_steps_label),
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "${(progress * 100).roundToInt()}% of $target",
                        color = Color(0xFF00F5FF),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            val dfKm = remember { DecimalFormat("#.#") }
            val dfCal = remember { DecimalFormat("#.#") }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SmallStatChip(label = stringResource(R.string.hc_distance), value = "${dfKm.format(distanceKm)} km")
                SmallStatChip(label = stringResource(R.string.hc_calories), value = "${dfCal.format(calories)} kcal")
            }
        }
    }
}

@Composable
private fun SmallStatChip(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, color = cs.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(text = value, color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}

/* --------- STAT ROW, SLEEP, HOURLY, WATER, TIPS --------- */

@Composable
private fun StatsRowCard(
    distanceKm: Double,
    calories: Double,
    activeMinutes: Int,
    waterMl: Int,
    cardBg: Color
) {
    val dfKm = remember { DecimalFormat("#.#") }
    val dfCal = remember { DecimalFormat("#.#") }
    val dfL = remember { DecimalFormat("#.#") }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatItem(icon = Icons.Outlined.Route, label = stringResource(R.string.hc_distance), value = "${dfKm.format(distanceKm)} km")
            StatItem(icon = Icons.Outlined.LocalFireDepartment, label = stringResource(R.string.hc_calories), value = "${dfCal.format(calories)} kcal")
            StatItem(
                icon = Icons.Outlined.Timer,
                label = stringResource(R.string.hc_active),
                value = stringResource(R.string.wv2_minutes_format, activeMinutes)
            )
            StatItem(
                icon = Icons.Outlined.LocalDrink,
                label = stringResource(R.string.hc_water),
                value = if (waterMl > 0) "${dfL.format(waterMl / 1000f)} L" else stringResource(R.string.hc_not_logged)
            )
        }
    }
}

@Composable
private fun StatItem(icon: ImageVector, label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(if (cs.background.luminance() < 0.5f) Color(0xFF141821) else Color(0xFFE5E9F2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF00F5FF), modifier = Modifier.size(18.dp))
        }
        Text(text = label, color = cs.onSurface.copy(alpha = 0.7f), fontSize = 11.sp)
        Text(text = value, color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SleepDetailCard(
    session: SleepSession?,
    cardBg: Color,
    neon: Brush
) {
    val cs = MaterialTheme.colorScheme
    val minutes = session?.totalMinutes ?: 0
    val quality = session?.qualityScore ?: 0

    val hours = minutes / 60
    val mins = minutes % 60

    val targetMin = 8 * 60
    val progress = (minutes.toFloat() / targetMin.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "sleepProgress"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (cs.background.luminance() < 0.5f) Color(0xFF101218) else Color(0xFFE5E9F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.Bedtime, contentDescription = null, tint = Color(0xFFB388FF), modifier = Modifier.size(18.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = stringResource(R.string.hc_sleep), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (session == null) stringResource(R.string.hc_no_sleep_day)
                        else if (session.source == "health_connect") stringResource(R.string.hc_logged_health_connect)
                        else stringResource(R.string.hc_logged_manually),
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }

                if (session != null) {
                    Text(text = "$quality/100", color = Color(0xFF00F5FF), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            if (session != null) {
                Text(text = "${hours}h ${mins}m", color = cs.onSurface, fontSize = 18.sp, fontWeight = FontWeight.Bold)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(14.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(if (cs.background.luminance() < 0.5f) Color(0xFF202430) else Color(0xFFE2E8F0)),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animated)
                            .height(14.dp)
                            .background(neon)
                    )
                }

                val insight = when {
                    minutes >= 8 * 60 -> stringResource(R.string.hc_sleep_great)
                    minutes >= 7 * 60 -> stringResource(R.string.hc_sleep_good)
                    minutes >= 6 * 60 -> stringResource(R.string.hc_sleep_low)
                    else -> stringResource(R.string.hc_sleep_very_low)
                }

                Text(text = insight, color = cs.onSurface.copy(alpha = 0.75f), fontSize = 11.sp)
            } else {
                Text(
                    text = stringResource(R.string.hc_sleep_sync_tip),
                    color = cs.onSurface.copy(alpha = 0.75f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/* =======================
   ✅ HOURLY FIX HERE
   ======================= */

@Composable
private fun HourlyChartCard(
    hourlyData: List<HourlySteps>,
    cardBg: Color,
    neon: Brush
) {
    val cs = MaterialTheme.colorScheme
    var selectedHour by remember { mutableStateOf<Int?>(null) }
    var selectedSteps by remember { mutableStateOf(0) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(text = stringResource(R.string.hc_hourly_activity), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(R.string.hc_hourly_chart_hint),
                color = cs.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(8.dp))

            HourlyStepsChartFixed(
                hourlyData = hourlyData,
                neon = neon,
                onBarClick = { hour, steps ->
                    selectedHour = hour
                    selectedSteps = steps
                }
            )

            Spacer(Modifier.height(6.dp))

            if (selectedHour != null) {
                val h = selectedHour!!
                val label = String.format("%02d:00 – %02d:00", h, (h + 1) % 24)
                Text(
                    text = if (selectedSteps > 0) {
                        stringResource(R.string.hc_hour_steps_format, label, selectedSteps)
                    } else {
                        stringResource(R.string.hc_no_steps_hour_format, label)
                    },
                    color = cs.onSurface.copy(alpha = 0.85f),
                    fontSize = 11.sp
                )
            } else {
                Text(
                    text = stringResource(R.string.hc_hourly_chart_tip),
                    color = cs.onSurface.copy(alpha = 0.7f),
                    fontSize = 11.sp
                )
            }
        }
    }
}

/**
 * ✅ FIX:
 * hourly_steps is a snapshot table (total steps up to that hour).
 * Some hours can be missing. If we just do (current - prevMissingAs0) we get "day total in one hour".
 *
 * So:
 * 1) Build a 0..23 snapshot array
 * 2) Carry-forward last known snapshot for missing hours
 * 3) Diff successive hours
 */
@Composable
private fun HourlyStepsChartFixed(
    hourlyData: List<HourlySteps>,
    neon: Brush,
    onBarClick: (hour: Int, stepsInThatHour: Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    // Map hour -> total snapshot at end of that hour
    val snapshotByHour: Map<Int, Int> = hourlyData.associate { it.hour to it.steps }

    // Build full snapshot timeline with carry-forward
    val snap = IntArray(24)
    var last = 0
    for (h in 0..23) {
        val v = snapshotByHour[h]
        if (v != null) last = v
        snap[h] = last
    }

    // Convert to per-hour deltas
    val perHour = IntArray(24)
    for (h in 0..23) {
        val prev = if (h == 0) 0 else snap[h - 1]
        perHour[h] = (snap[h] - prev).coerceAtLeast(0)
    }

    val maxStepsRaw = perHour.maxOrNull() ?: 0
    val visualMax = maxOf(maxStepsRaw, 500) // avoid ultra-flat chart
    val maxSteps = visualMax.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                (0..23).forEach { h ->
                    val steps = perHour[h]
                    val ratio = (steps.toFloat() / maxSteps).coerceIn(0f, 1f)

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(ratio)
                                .background(neon, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                .clickable { onBarClick(h, steps) }
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            listOf(0, 3, 6, 9, 12, 15, 18, 21).forEach { h ->
                Text(
                    text = "${h}h",
                    color = cs.onSurface.copy(alpha = 0.6f),
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun WaterDetailCard(
    waterMl: Int,
    dailyTargetMl: Int,
    cardBg: Color,
    neon: Brush
) {
    val cs = MaterialTheme.colorScheme
    val progress = (waterMl.toFloat() / dailyTargetMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "waterDetailProgress"
    )

    val dfL = remember { DecimalFormat("#.#") }
    val goalL = dailyTargetMl / 1000f
    val todayL = waterMl / 1000f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(if (cs.background.luminance() < 0.5f) Color(0xFF0A1014) else Color(0xFFE5E9F2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = Icons.Outlined.LocalDrink, contentDescription = null, tint = Color(0xFF00B0FF), modifier = Modifier.size(18.dp))
                }
                Column {
                    Text(text = stringResource(R.string.hc_water_intake), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = if (waterMl > 0) "${dfL.format(todayL)} / ${dfL.format(goalL)} L" else stringResource(R.string.hc_no_water_day),
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(14.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (cs.background.luminance() < 0.5f) Color(0xFF202430) else Color(0xFFE2E8F0)),
                contentAlignment = Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animated)
                        .height(14.dp)
                        .background(neon)
                )
            }

            Text(
                text = when {
                    waterMl == 0 -> stringResource(R.string.hc_water_log_tip)
                    progress >= 1f -> stringResource(R.string.hc_water_goal_reached_day)
                    progress >= 0.5f -> stringResource(R.string.hc_water_halfway_day)
                    else -> stringResource(R.string.hc_water_sips_tip)
                },
                color = cs.onSurface.copy(alpha = 0.75f),
                fontSize = 11.sp
            )
        }
    }
}


@Composable
private fun HistoryAdjustDayCard(
    steps: Int,
    waterMl: Int,
    cardBg: Color,
    onSave: (steps: Int, waterMl: Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    var showEditSheet by remember { mutableStateOf(false) }
    var editableSteps by remember(steps) { mutableStateOf(steps) }
    var editableWaterMl by remember(waterMl) { mutableStateOf(waterMl) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.07f) else Color.Black.copy(alpha = 0.07f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(13.dp))
                        .background(Color(0xFF00F5FF).copy(alpha = if (isDark) 0.14f else 0.10f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Create,
                        contentDescription = null,
                        tint = Color(0xFF00F5FF),
                        modifier = Modifier.size(19.dp)
                    )
                }

                Spacer(Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.h3_adjust_day_data),
                        color = cs.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.h3_adjust_day_data_info),
                        color = cs.onSurface.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }

                Button(
                    onClick = {
                        editableSteps = steps
                        editableWaterMl = waterMl
                        showEditSheet = true
                    },
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF00F5FF),
                        contentColor = Color.Black
                    )
                ) {
                    Text(
                        text = stringResource(R.string.h3_edit),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistoryAdjustPreviewChip(
                    label = stringResource(R.string.h3_adjust_steps),
                    value = steps.toString(),
                    modifier = Modifier.weight(1f)
                )
                HistoryAdjustPreviewChip(
                    label = stringResource(R.string.h3_adjust_water),
                    value = "$waterMl ml",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    if (showEditSheet) {
        Dialog(onDismissRequest = { showEditSheet = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cs.surface,
                shadowElevation = 18.dp,
                modifier = Modifier.fillMaxWidth(0.92f),
                border = BorderStroke(
                    1.dp,
                    if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { showEditSheet = false }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = stringResource(R.string.hc_back),
                                tint = Color(0xFF00F5FF)
                            )
                        }

                        Text(
                            text = stringResource(R.string.h3_edit_day),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )
                    }

                    OutlinedTextField(
                        value = editableSteps.toString(),
                        onValueChange = {
                            if (it.all(Char::isDigit)) {
                                editableSteps = it.toIntOrNull() ?: editableSteps
                            }
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.h3_adjust_steps)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            cursorColor = Color(0xFF00F5FF),
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = editableWaterMl.toString(),
                        onValueChange = {
                            if (it.all(Char::isDigit)) {
                                editableWaterMl = it.toIntOrNull() ?: editableWaterMl
                            }
                        },
                        singleLine = true,
                        label = { Text(stringResource(R.string.h3_adjust_water)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            cursorColor = Color(0xFF00F5FF),
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface
                        )
                    )

                    Button(
                        onClick = {
                            onSave(editableSteps, editableWaterMl)
                            showEditSheet = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF00F5FF),
                            contentColor = Color.Black
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.h3_save_changes),
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryAdjustPreviewChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(17.dp))
            .background(
                if (cs.background.luminance() < 0.5f) {
                    Color.White.copy(alpha = 0.055f)
                } else {
                    Color.Black.copy(alpha = 0.045f)
                }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = cs.onSurface.copy(alpha = 0.62f),
            fontSize = 10.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
        Text(
            text = value,
            color = cs.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}


@Composable
private fun TipsCard(cardBg: Color) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(text = stringResource(R.string.hc_insights), color = Color(0xFF00F5FF), fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(
                text = stringResource(R.string.hc_recovery_insight),
                color = cs.onSurface.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}
