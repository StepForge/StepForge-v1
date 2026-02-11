package com.example.stepforge.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailyWater
import com.example.stepforge.data.WaterIntakeEvent
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.notification.WaterReminderScheduler
import com.example.stepforge.ui.components.GoalKonfetti
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.imePadding

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WaterReminderScreen(
    onBack: () -> Unit
) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val neonA = Color(0xFF00FFA3)
    val neonB = Color(0xFF00F5FF)
    val neon = Brush.horizontalGradient(listOf(neonB, neonA))

    val cardBg = if (isDark) cs.surface else Color(0xFFF7FAFD)
    val border = if (isDark) Color.Transparent else Color(0x220F172A)
    val shadow = if (isDark) 12.dp else 9.dp

    val KEY_WATER_ENABLED = booleanPreferencesKey("water_enabled")
    val KEY_WATER_INTERVAL_MIN = intPreferencesKey("water_interval_min")
    val KEY_WATER_START_HOUR = intPreferencesKey("water_start_hour")
    val KEY_WATER_END_HOUR = intPreferencesKey("water_end_hour")
    val KEY_WATER_GOAL = intPreferencesKey("water_goal_ml")
    val KEY_WATER_TODAY = intPreferencesKey("water_today_ml")
    val KEY_WATER_DATE = intPreferencesKey("water_date_yyyymmdd")

    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    fun todayDateString(): String = dateFormatter.format(Date())

    fun todayInt(): Int {
        val c = Calendar.getInstance()
        val y = c.get(Calendar.YEAR)
        val m = c.get(Calendar.MONTH) + 1
        val d = c.get(Calendar.DAY_OF_MONTH)
        return y * 10000 + m * 100 + d
    }

    val db = remember { AppDatabase.getDatabase(ctx) }
    val waterDao = remember { db.dailyWaterDao() }
    val eventDao = remember { db.waterIntakeEventDao() }

    var enabled by remember { mutableStateOf(false) }
    var intervalMin by remember { mutableIntStateOf(60) }
    var startHour by remember { mutableIntStateOf(8) }
    var endHour by remember { mutableIntStateOf(22) }

    var goalMl by remember { mutableIntStateOf(2000) }
    var todayMl by remember { mutableIntStateOf(0) }

    var events by remember { mutableStateOf<List<WaterIntakeEvent>>(emptyList()) }
    var cooldown by remember { mutableStateOf(false) }

    var showGoalSheet by remember { mutableStateOf(false) }
    var showCustomSheet by remember { mutableStateOf(false) }

    var showGoalKonfetti by remember { mutableStateOf(false) }
    var showGoalMessage by remember { mutableStateOf(false) }
    var goalCelebratedToday by remember { mutableStateOf(false) }

    suspend fun reloadEvents() {
        events = withContext(Dispatchers.IO) { eventDao.getAllForDate(todayDateString()) }
    }

    LaunchedEffect(Unit) {
        val prefs = ctx.stepforgeStore.data.first()

        enabled = prefs[KEY_WATER_ENABLED] ?: false
        intervalMin = prefs[KEY_WATER_INTERVAL_MIN] ?: 60
        startHour = prefs[KEY_WATER_START_HOUR] ?: 8
        endHour = prefs[KEY_WATER_END_HOUR] ?: 22

        val storedGoal = prefs[KEY_WATER_GOAL] ?: 2000
        val storedToday = prefs[KEY_WATER_TODAY] ?: 0
        val storedDate = prefs[KEY_WATER_DATE] ?: 0

        goalMl = storedGoal
        val t = todayInt()
        todayMl = if (storedDate == t) storedToday else 0

        goalCelebratedToday = false
        showGoalKonfetti = false
        showGoalMessage = false

        if (storedDate != t) {
            ctx.stepforgeStore.edit { p ->
                p[KEY_WATER_DATE] = t
                p[KEY_WATER_TODAY] = 0
            }
            todayMl = 0
        }

        scope.launch(Dispatchers.IO) {
            waterDao.insertDailyWater(DailyWater(date = todayDateString(), waterMl = todayMl))
        }
        reloadEvents()
    }

    LaunchedEffect(enabled, intervalMin, startHour, endHour) {
        ctx.stepforgeStore.edit { p ->
            p[KEY_WATER_ENABLED] = enabled
            p[KEY_WATER_INTERVAL_MIN] = intervalMin
            p[KEY_WATER_START_HOUR] = startHour
            p[KEY_WATER_END_HOUR] = endHour
        }
        if (enabled) WaterReminderScheduler.schedule(ctx) else WaterReminderScheduler.cancel(ctx)
    }

    LaunchedEffect(goalMl, todayMl) {
        ctx.stepforgeStore.edit { p ->
            p[KEY_WATER_GOAL] = goalMl
            p[KEY_WATER_TODAY] = todayMl
            p[KEY_WATER_DATE] = todayInt()
        }
        scope.launch(Dispatchers.IO) {
            waterDao.insertDailyWater(DailyWater(date = todayDateString(), waterMl = todayMl))
        }
    }

    LaunchedEffect(todayMl, goalMl) {
        if (!goalCelebratedToday && goalMl > 0 && todayMl >= goalMl) {
            goalCelebratedToday = true
            showGoalMessage = true
            showGoalKonfetti = true
            delay(3500)
        }
    }

    val progress = (todayMl.toFloat() / goalMl.coerceAtLeast(1)).coerceIn(0f, 1f)
    val progressAlpha by animateFloatAsState(
        targetValue = if (enabled) 1f else 0.4f,
        animationSpec = tween(durationMillis = if (enabled) 240 else 200, easing = EaseOut),
        label = "progressAlpha"
    )
    val flowVisible by remember { derivedStateOf { enabled } }

    val expectedByNowMl by remember(enabled, goalMl, startHour, endHour) {
        derivedStateOf {
            if (!enabled) 0
            else {
                val now = Calendar.getInstance()
                val nowMin = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE)
                val startMin = startHour * 60
                val endMin = endHour * 60
                val totalWindow = (endMin - startMin).coerceAtLeast(1)
                val elapsed = (nowMin - startMin).coerceIn(0, totalWindow)
                (goalMl * (elapsed.toFloat() / totalWindow.toFloat())).roundToInt()
            }
        }
    }

    val deviationMl = todayMl - expectedByNowMl
    val statusText = when {
        !enabled -> "Water reminder is disabled"
        deviationMl >= -150 -> "Hydration: Optimal"
        deviationMl >= -350 -> "Hydration: Slightly behind"
        else -> "Hydration: Behind schedule"
    }

    val animatedMl = remember { Animatable(0f) }
    var lastTargetMl by remember { mutableIntStateOf(0) }
    val scalePulse = remember { Animatable(1f) }

    LaunchedEffect(todayMl) {
        val targetVal = todayMl
        if (lastTargetMl == 0 && targetVal == 0) {
            animatedMl.snapTo(0f)
            lastTargetMl = 0
            return@LaunchedEffect
        }

        lastTargetMl = targetVal
        animatedMl.animateTo(
            targetVal.toFloat(),
            tween(durationMillis = 240, easing = LinearOutSlowInEasing)
        )

        scalePulse.snapTo(1f)
        scalePulse.animateTo(1.03f, tween(120, easing = EaseOut))
        scalePulse.animateTo(1f, tween(120, easing = EaseOut))
    }

    val progressAnim = remember { Animatable(0f) }
    LaunchedEffect(progress) {
        progressAnim.animateTo(
            progress,
            tween(durationMillis = 420, easing = androidx.compose.animation.core.EaseOutCubic)
        )
    }

    // ✅ NEW: HydrationFlowBar için ayrı animasyon (su ekledikçe smooth dolsun)
    val flowActualAnim = remember { Animatable(0f) }
    val flowExpectedAnim = remember { Animatable(0f) }
    LaunchedEffect(todayMl, expectedByNowMl, goalMl) {
        val safeGoal = goalMl.coerceAtLeast(1).toFloat()

        val actualP = (todayMl.toFloat() / safeGoal).coerceIn(0f, 1f)
        val expectedP = (expectedByNowMl.toFloat() / safeGoal).coerceIn(0f, 1f)

        // Bar dolum animasyonu biraz daha "smooth"
        flowActualAnim.animateTo(actualP, tween(durationMillis = 380, easing = LinearOutSlowInEasing))
        flowExpectedAnim.animateTo(expectedP, tween(durationMillis = 380, easing = LinearOutSlowInEasing))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Water Reminder", color = cs.onBackground, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = cs.onBackground)
                    }
                },
                actions = {
                    IOSLikeToggle(checked = enabled, onCheckedChange = { enabled = it })
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(pad)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                AnimatedVisibility(
                    visible = showGoalMessage,
                    enter = fadeIn(tween(200)) + slideInVertically { -it / 6 },
                    exit = fadeOut(tween(160)) + slideOutVertically { -it / 6 }
                ) {
                    ElevatedGlassCard(cardBg = cardBg, border = border, shadow = shadow) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Outlined.Star, contentDescription = null, tint = Color(0xFF00F5FF))
                            Column {
                                Text("Goal completed!", color = cs.onSurface, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                Text(
                                    "Great job staying hydrated today.",
                                    color = cs.onSurface.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                ElevatedGlassCard(cardBg = cardBg, border = border, shadow = shadow) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            SemiCircleProgress(
                                progress = progressAnim.value,
                                alpha = progressAlpha,
                                neonStart = neonB,
                                neonEnd = neonA,
                                trackColor = cs.surfaceVariant.copy(alpha = 0.20f),
                                isDark = isDark
                            )

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                val formatted = NumberFormat.getIntegerInstance().format(animatedMl.value.roundToInt())
                                Text(
                                    text = "$formatted ml",
                                    color = cs.onSurface.copy(alpha = if (enabled) 1f else 0.7f),
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .graphicsLayerScale(scalePulse.value)
                                )

                                val remaining = (goalMl - todayMl).coerceAtLeast(0)
                                Text(
                                    text = "${NumberFormat.getIntegerInstance().format(remaining)} ml remaining",
                                    color = cs.onSurface.copy(alpha = 0.7f),
                                    fontSize = 13.sp
                                )
                            }
                        }

                        AnimatedContent(
                            targetState = statusText,
                            transitionSpec = {
                                (fadeIn(tween(180)) + slideInVertically { it / 6 }) with
                                        (fadeOut(tween(140)) + slideOutVertically { -it / 6 })
                            },
                            label = "hydrationStatus"
                        ) { s ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.LocalDrink,
                                    contentDescription = null,
                                    tint = cs.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(s, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            }
                        }

                        AnimatedVisibility(
                            visible = !enabled,
                            enter = fadeIn(tween(200)) + scaleIn(tween(220, easing = EaseOut)),
                            exit = fadeOut(tween(140)) + scaleOut(tween(140))
                        ) {
                            Text("Water reminder is disabled", color = cs.onSurface.copy(alpha = 0.7f), fontSize = 13.sp)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = flowVisible,
                    enter = fadeIn(tween(180)) + expandVerticallySoft(),
                    exit = fadeOut(tween(140)) + shrinkVerticallySoft()
                ) {
                    ElevatedGlassCard(cardBg = cardBg, border = border, shadow = shadow) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text("Hydration Analysis", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                            HydrationFlowBar(
                                actual = todayMl,
                                expected = expectedByNowMl,
                                goal = goalMl,
                                isDark = isDark,
                                neonStart = neonB,
                                neonEnd = neonA,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(
                                    "Expected by now: ${NumberFormat.getIntegerInstance().format(expectedByNowMl)} ml",
                                    color = cs.onSurface.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                                Text(
                                    "Actual: ${NumberFormat.getIntegerInstance().format(todayMl)} ml",
                                    color = cs.onSurface.copy(alpha = 0.75f),
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }

                val buttonsEnabledAlpha by animateFloatAsState(
                    targetValue = if (enabled) 1f else 0.5f,
                    animationSpec = tween(200, easing = EaseOut),
                    label = "btnAlpha"
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    AddWaterButton(
                        label = "+250 ml",
                        enabled = enabled && !cooldown,
                        alpha = buttonsEnabledAlpha,
                        brush = neon,
                        onClick = {
                            scope.launch {
                                addWaterAndEvent(
                                    db = db,
                                    amount = 250,
                                    goalMl = goalMl,
                                    todayMl = todayMl,
                                    setToday = { todayMl = it },
                                    setEvents = { events = it }
                                )
                                cooldown = true
                                delay(300)
                                cooldown = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )

                    AddWaterButton(
                        label = "+500 ml",
                        enabled = enabled && !cooldown,
                        alpha = buttonsEnabledAlpha,
                        brush = neon,
                        onClick = {
                            scope.launch {
                                addWaterAndEvent(
                                    db = db,
                                    amount = 500,
                                    goalMl = goalMl,
                                    todayMl = todayMl,
                                    setToday = { todayMl = it },
                                    setEvents = { events = it }
                                )
                                cooldown = true
                                delay(300)
                                cooldown = false
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                }

                TwoEqualButtonsRow(
                    left = { m ->
                        Button(
                            onClick = { showGoalSheet = true },
                            enabled = enabled,
                            modifier = m.height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.surfaceVariant,
                                contentColor = cs.onSurface
                            )
                        ) { Text("Daily Goal", fontWeight = FontWeight.SemiBold) }
                    },
                    right = { m ->
                        Button(
                            onClick = { showCustomSheet = true },
                            enabled = enabled && !cooldown,
                            modifier = m.height(46.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = cs.surfaceVariant,
                                contentColor = cs.onSurface
                            )
                        ) { Text("Custom", fontWeight = FontWeight.SemiBold) }
                    }
                )

                AnimatedVisibility(
                    visible = enabled && events.isNotEmpty(),
                    enter = fadeIn(tween(180)) + slideInVertically { it / 6 },
                    exit = fadeOut(tween(120)) + slideOutVertically { it / 6 }
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                undoLastEvent(
                                    db = db,
                                    dateStr = todayDateString(),
                                    goalMl = goalMl,
                                    todayMl = todayMl,
                                    setToday = { todayMl = it },
                                    setEvents = { events = it }
                                )
                            }
                        },
                        enabled = enabled && !cooldown,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = cs.surfaceVariant, contentColor = cs.onSurface)
                    ) {
                        Icon(Icons.Outlined.Undo, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Undo last", fontWeight = FontWeight.SemiBold)
                    }
                }

                ElevatedGlassCard(cardBg = cardBg, border = border, shadow = shadow) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Today's Timeline", color = cs.onSurface, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)

                        if (events.isEmpty()) {
                            Text("No entries yet.", color = cs.onSurface.copy(alpha = 0.65f), fontSize = 12.sp)
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 220.dp)
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                events.forEach { ev -> TimelineRow(event = ev) }
                            }
                        }

                        Text("Today resets at 00:00", color = cs.onSurface.copy(alpha = 0.55f), fontSize = 11.sp)
                    }
                }
            }

            GoalKonfetti(
                visible = showGoalKonfetti,
                modifier = Modifier.fillMaxSize(),
                onFinished = { showGoalKonfetti = false }
            )

            if (showGoalSheet) {
                PremiumGoalSheet(
                    isDark = isDark,
                    neon = neon,
                    initialGoal = goalMl,
                    onDismiss = { showGoalSheet = false },
                    onSave = { newGoal ->
                        goalMl = newGoal
                        showGoalSheet = false
                    }
                )
            }

            if (showCustomSheet) {
                PremiumCustomIntakeSheet(
                    isDark = isDark,
                    neon = neon,
                    onDismiss = { showCustomSheet = false },
                    onAdd = { amount ->
                        scope.launch {
                            addWaterAndEvent(
                                db = db,
                                amount = amount,
                                goalMl = goalMl,
                                todayMl = todayMl,
                                setToday = { todayMl = it },
                                setEvents = { events = it }
                            )
                            cooldown = true
                            delay(300)
                            cooldown = false
                        }
                        showCustomSheet = false
                    }
                )
            }
        }
    }
}

/* =========================
   PREMIUM SHEETS
   ========================= */

@Composable
private fun PremiumGoalSheet(
    isDark: Boolean,
    neon: Brush,
    initialGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val sheetBg = if (isDark) Color(0xFF0F1116) else Color(0xFFF7FAFD)
    val border = if (isDark) Color(0xFF222633) else Color(0x220F172A)

    var goal by remember { mutableIntStateOf(initialGoal) }
    var sliderVal by remember { mutableStateOf(initialGoal.toFloat()) }

    val minMl = 1000
    val maxMl = 4500
    val step = 250

    SheetScrim(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .shadow(18.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(sheetBg)
                .border(1.dp, border, RoundedCornerShape(26.dp))
                .padding(18.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Daily water goal",
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                Text(
                    text = "${NumberFormat.getIntegerInstance().format(goal)} ml",
                    color = cs.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 28.sp
                )

                Slider(
                    value = sliderVal,
                    onValueChange = {
                        sliderVal = it
                        val snapped = ((it / step).roundToInt() * step).coerceIn(minMl, maxMl)
                        goal = snapped
                    },
                    valueRange = minMl.toFloat()..maxMl.toFloat(),
                    colors = SliderDefaults.colors(
                        thumbColor = Color(0xFF00F5FF),
                        activeTrackColor = Color(0xFF00F5FF),
                        inactiveTrackColor = if (isDark) Color(0xFF2A2F3A) else Color(0xFFE0E5EC)
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1.0 L", color = cs.onSurface.copy(alpha = 0.65f), fontSize = 12.sp)
                    Text("4.5 L", color = cs.onSurface.copy(alpha = 0.65f), fontSize = 12.sp)
                }

                Text(
                    text = "Choose a goal you can hit consistently.",
                    color = cs.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { onSave(goal) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .height(42.dp)
                                .padding(horizontal = 14.dp)
                                .background(neon, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumCustomIntakeSheet(
    isDark: Boolean,
    neon: Brush,
    onDismiss: () -> Unit,
    onAdd: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val sheetBg = if (isDark) Color(0xFF0F1116) else Color(0xFFF7FAFD)
    val border = if (isDark) Color(0xFF222633) else Color(0x220F172A)

    var value by remember { mutableStateOf("") }

    // ✅ Tekrarsız, mantıklı preset listesi
    val presets = listOf(150, 200, 250, 300, 500, 750)

    // ✅ Basit validasyon (min/max)
    val parsed = value.toIntOrNull() ?: 0
    val minMl = 50
    val maxMl = 2000
    val isValid = parsed in minMl..maxMl

    SheetScrim(onDismiss = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .shadow(18.dp, RoundedCornerShape(26.dp))
                .clip(RoundedCornerShape(26.dp))
                .background(sheetBg)
                .border(1.dp, border, RoundedCornerShape(26.dp))
                .padding(18.dp)
                // ✅ Klavye açılınca butonlar kapanmasın
                .imePadding()
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Custom intake",
                    color = cs.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                // Input + ml suffix
                OutlinedTextField(
                    value = value,
                    onValueChange = { v -> value = v.filter(Char::isDigit).take(4) },
                    singleLine = true,
                    placeholder = { Text("e.g. 300") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    suffix = { Text("ml", color = cs.onSurface.copy(alpha = 0.65f)) },
                    supportingText = {
                        val t = when {
                            value.isBlank() -> "Enter an amount between $minMl and $maxMl ml."
                            isValid -> "Looks good."
                            else -> "Please enter $minMl–$maxMl ml."
                        }
                        Text(t, color = cs.onSurface.copy(alpha = 0.7f), fontSize = 12.sp)
                    },
                    isError = value.isNotBlank() && !isValid,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF00F5FF),
                        cursorColor = Color(0xFF00F5FF)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Quick presets",
                    color = cs.onSurface.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                // ✅ 2 satır x 3 sütun, eşit genişlik, tekrar yok
                Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        presets.take(3).forEach { p ->
                            PresetChip(
                                value = p,
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            ) { value = p.toString() }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        presets.drop(3).take(3).forEach { p ->
                            PresetChip(
                                value = p,
                                isDark = isDark,
                                modifier = Modifier.weight(1f)
                            ) { value = p.toString() }
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))

                // ✅ Aksiyonlar: aynı baseline, Add daha büyük ve premium
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }

                    Spacer(Modifier.weight(1f))

                    Button(
                        onClick = {
                            val ml = value.toIntOrNull() ?: 0
                            if (ml in minMl..maxMl) onAdd(ml)
                        },
                        enabled = isValid,
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .height(52.dp)                // ✅ daha büyük
                                .widthIn(min = 140.dp)        // ✅ daha geniş, çirkin küçük kalmasın
                                .background(neon, RoundedCornerShape(999.dp))
                                .padding(horizontal = 18.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Add",
                                color = Color.Black,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PresetChip(
    value: Int,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (isDark) Color(0xFF151821) else cs.surfaceVariant
    val border = if (isDark) Color(0xFF2A2F3A) else Color(0x220F172A)

    Box(
        modifier = modifier
            .height(46.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$value ml",
            color = cs.onSurface,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun SheetScrim(
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    val scrim = Color.Black.copy(alpha = 0.55f)

    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(160)) + slideInVertically(tween(220, easing = FastOutSlowInEasing)) { it / 2 },
        exit = fadeOut(tween(120)) + slideOutVertically(tween(180)) { it / 2 }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onDismiss() },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 18.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { },
                contentAlignment = Alignment.BottomCenter
            ) {
                content()
            }
        }
    }
}

@Composable
private fun TwoEqualButtonsRow(
    left: @Composable (Modifier) -> Unit,
    right: @Composable (Modifier) -> Unit,
    gap: Int = 12
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val itemWidth = (maxWidth - gap.dp) / 2

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(gap.dp)
        ) {
            left(Modifier.width(itemWidth))
            right(Modifier.width(itemWidth))
        }
    }
}

/* =========================
   Helpers
   ========================= */

@Composable
private fun IOSLikeToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val w = 62.dp
    val h = 28.dp
    val pad = 3.dp

    val trackOn = if (isDark) Color(0xFF0B2C2B) else Color(0xFFDAF7F2)
    val trackOff = if (isDark) Color(0xFF1A1C23) else Color(0xFFE6EDF6)
    val border = if (isDark) Color(0xFF2B2F3A) else Color(0x220F172A)
    val knob = Color.White

    val t = remember { Animatable(if (checked) 1f else 0f) }
    LaunchedEffect(checked) {
        t.animateTo(
            targetValue = if (checked) 1f else 0f,
            animationSpec = tween(durationMillis = if (checked) 240 else 200, easing = EaseOut)
        )
    }

    val interaction = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(w, h)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.Transparent)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .clickable(interactionSource = interaction, indication = null) { onCheckedChange(!checked) }
            .padding(pad)
    ) {
        val bg = lerpColor(trackOff, trackOn, t.value)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(999.dp))
                .background(bg)
        )

        val knobSize = h - pad * 2
        val maxX = (w - knobSize - pad * 2)
        val x = maxX * t.value

        Text(
            text = "ON",
            color = cs.onSurface.copy(alpha = 0.75f * t.value),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 8.dp)
        )

        Text(
            text = "OFF",
            color = cs.onSurface.copy(alpha = 0.75f * (1f - t.value)),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp)
        )

        Box(
            modifier = Modifier
                .size(knobSize)
                .offset(x = x, y = 0.dp)
                .clip(CircleShape)
                .background(knob)
        )
    }
}

@Composable
private fun ElevatedGlassCard(
    cardBg: Color,
    border: Color,
    shadow: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadow, shape, clip = false)
            .clip(shape)
            .background(cardBg)
            .then(if (border != Color.Transparent) Modifier.border(1.dp, border, shape) else Modifier)
    ) {
        content()
    }
}

@Composable
private fun SemiCircleProgress(
    progress: Float,
    alpha: Float,
    neonStart: Color,
    neonEnd: Color,
    trackColor: Color,
    isDark: Boolean
) {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(220.dp)
    ) {
        val w = size.width
        val h = size.height
        val stroke = min(w, h) * 0.08f
        val padding = stroke * 0.8f

        val diameter = min(w, h) * 1.15f
        val topLeft = Offset((w - diameter) / 2f, (h - diameter) / 2f + padding)
        val arcSize = Size(diameter, diameter)

        drawArc(
            color = trackColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            alpha = alpha
        )

        val sweep = (180f * progress.coerceIn(0f, 1f))
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(neonStart, neonEnd, neonStart),
                center = Offset(w / 2f, topLeft.y + diameter / 2f)
            ),
            startAngle = 180f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
            alpha = alpha
        )

        if (sweep > 6f) {
            drawArc(
                color = Color.White.copy(alpha = if (isDark) 0.16f else 0.22f),
                startAngle = 180f + sweep - 10f,
                sweepAngle = 10f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = stroke * 0.55f, cap = StrokeCap.Round),
                alpha = alpha
            )
        }
    }
}

/**
 * ✅ CHANGED:
 * Burada artık "actual/expected/goal" yerine doğrudan progress değerleri alıyoruz.
 * Böylece dışarıda animasyon yapıp buraya smooth progress gönderebiliyoruz.
 */
@Composable
private fun HydrationFlowBar(
    actual: Int,
    expected: Int,
    goal: Int,
    isDark: Boolean,
    neonStart: Color,
    neonEnd: Color,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val bg = if (isDark) Color(0xFF161A22) else Color(0xFFE6EDF6)
    val tick = cs.onSurface.copy(alpha = if (isDark) 0.22f else 0.16f)

    val goalSafe = goal.coerceAtLeast(1)

    val actualTarget = (actual.toFloat() / goalSafe.toFloat()).coerceIn(0f, 1f)
    val expectedTarget = (expected.toFloat() / goalSafe.toFloat()).coerceIn(0f, 1f)

    // ✅ Smooth animasyon (bar dolumu + expected line kayması)
    val actualAnim by animateFloatAsState(
        targetValue = actualTarget,
        animationSpec = tween(durationMillis = 420, easing = LinearOutSlowInEasing),
        label = "waterFlowActual"
    )
    val expectedAnim by animateFloatAsState(
        targetValue = expectedTarget,
        animationSpec = tween(durationMillis = 420, easing = LinearOutSlowInEasing),
        label = "waterFlowExpected"
    )

    Box(
        modifier = modifier
            .height(12.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(actualAnim)
                .background(Brush.horizontalGradient(listOf(neonStart, neonEnd)))
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // expected marker
            val px = size.width * expectedAnim
            drawLine(
                color = cs.secondary.copy(alpha = 0.45f),
                start = Offset(px, 0f),
                end = Offset(px, size.height),
                strokeWidth = 2f
            )

            // ticks
            val tickCount = 12
            for (i in 0..tickCount) {
                val xPos = size.width * (i / tickCount.toFloat())
                drawLine(
                    color = tick,
                    start = Offset(xPos, size.height),
                    end = Offset(xPos, size.height - 4f),
                    strokeWidth = 1.5f
                )
            }
        }
    }
}

@Composable
private fun TimelineRow(event: WaterIntakeEvent) {
    val cs = MaterialTheme.colorScheme
    val sdf = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val time = sdf.format(Date(event.timeMillis))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(cs.primary.copy(alpha = 0.9f))
        )
        Text(
            text = "$time  •  ${event.amountMl} ml",
            color = cs.onSurface.copy(alpha = 0.9f),
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun AddWaterButton(
    label: String,
    enabled: Boolean,
    alpha: Float,
    brush: Brush,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val press = remember { Animatable(1f) }
    LaunchedEffect(enabled) { if (!enabled) press.snapTo(1f) }

    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(52.dp)
            .graphicsLayer(scaleX = press.value, scaleY = press.value, alpha = alpha)
            .clip(shape)
            .background(brush)
            .clickable(enabled = enabled, interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(text = label, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 18.sp)
    }
}

private fun expandVerticallySoft() =
    androidx.compose.animation.expandVertically(animationSpec = tween(220, easing = EaseOut))

private fun shrinkVerticallySoft() =
    androidx.compose.animation.shrinkVertically(animationSpec = tween(180, easing = EaseOut))

private fun lerpColor(a: Color, b: Color, t: Float): Color {
    val tt = t.coerceIn(0f, 1f)
    return Color(
        red = a.red + (b.red - a.red) * tt,
        green = a.green + (b.green - a.green) * tt,
        blue = a.blue + (b.blue - a.blue) * tt,
        alpha = a.alpha + (b.alpha - a.alpha) * tt
    )
}

private fun Modifier.graphicsLayerScale(scale: Float): Modifier =
    this.then(Modifier.graphicsLayer(scaleX = scale, scaleY = scale))

/* =========================
   Persist helpers
   ========================= */

private suspend fun addWaterAndEvent(
    db: AppDatabase,
    amount: Int,
    goalMl: Int,
    todayMl: Int,
    setToday: (Int) -> Unit,
    setEvents: (List<WaterIntakeEvent>) -> Unit
) {
    val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val now = System.currentTimeMillis()

    val newVal = (todayMl + amount).coerceAtMost(goalMl * 2)
    setToday(newVal)

    withContext(Dispatchers.IO) {
        db.waterIntakeEventDao().insert(
            WaterIntakeEvent(
                date = dateStr,
                timeMillis = now,
                amountMl = amount
            )
        )

        db.dailyWaterDao().insertDailyWater(DailyWater(date = dateStr, waterMl = newVal))
        setEvents(db.waterIntakeEventDao().getAllForDate(dateStr))
    }
}

private suspend fun undoLastEvent(
    db: AppDatabase,
    dateStr: String,
    goalMl: Int,
    todayMl: Int,
    setToday: (Int) -> Unit,
    setEvents: (List<WaterIntakeEvent>) -> Unit
) {
    withContext(Dispatchers.IO) {
        val last = db.waterIntakeEventDao().getLatestForDate(dateStr) ?: return@withContext
        db.waterIntakeEventDao().deleteById(last.id)

        val newToday = (todayMl - last.amountMl).coerceAtLeast(0).coerceAtMost(goalMl * 2)
        setToday(newToday)

        db.dailyWaterDao().insertDailyWater(DailyWater(date = dateStr, waterMl = newToday))
        setEvents(db.waterIntakeEventDao().getAllForDate(dateStr))
    }
}