package com.example.stepforge

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Create
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.DailyWater
import com.example.stepforge.data.SleepSession
import com.example.stepforge.ui.rememberUseDarkTheme
import com.example.stepforge.ui.stepforgeTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

private val TimelineGutter = 64.dp
private const val TEST_PREFIX = "TEST-"

class HistoryActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = AppDatabase.getDatabase(this)
        val stepsDao = db.dailyStepsDao()
        val waterDao = db.dailyWaterDao()
        val sleepDao = db.sleepSessionDao()

        setContent {
            var history by remember { mutableStateOf<List<DailySteps>>(emptyList()) }
            var waterMap by remember { mutableStateOf<Map<String, DailyWater>>(emptyMap()) }
            var sleepMap by remember { mutableStateOf<Map<String, SleepSession>>(emptyMap()) }

            val scope = rememberCoroutineScope()

            LaunchedEffect(Unit) {
                val (steps, waterList, sleepList) = withContext(Dispatchers.IO) {
                    Triple(
                        stepsDao.getAllSteps(),
                        waterDao.getAllWater(),
                        sleepDao.getRecentSessions(365)
                    )
                }
                history = steps
                waterMap = waterList.associateBy { it.date }
                sleepMap = sleepList.associateBy { it.date }
            }

            stepforgeTheme(darkTheme = rememberUseDarkTheme(this)) {
                NeonHistoryScreen(
                    history = history,
                    waterMap = waterMap,
                    activity = this@HistoryActivity,
                    onForceReload = {
                        scope.launch(Dispatchers.IO) {
                            val steps = stepsDao.getAllSteps()
                            val waterList = waterDao.getAllWater()
                            val sleepList = sleepDao.getRecentSessions(365)

                            withContext(Dispatchers.Main) {
                                history = steps
                                waterMap = waterList.associateBy { it.date }
                                sleepMap = sleepList.associateBy { it.date }
                            }
                        }
                    },
                    sleepMap = sleepMap
                )
            }
        }
    }
}

private sealed class HistoryRow {
    data class Year(val year: String) : HistoryRow()
    data class Entry(val daily: DailySteps) : HistoryRow()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeonHistoryScreen(
    history: List<DailySteps>,
    waterMap: Map<String, DailyWater>,
    activity: Activity,
    onForceReload: () -> Unit,
    sleepMap: Map<String, SleepSession>
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bg = cs.background

    val timeLineGradient = Brush.verticalGradient(
        colors = listOf(
            if (isDark) Color(0xFF00F5FF) else Color(0xFF38BDF8),
            if (isDark) Color(0xFF00FFA3) else Color(0xFF22C55E)
        )
    )

    val state = rememberLazyListState()

    val rows = remember(history) {
        val grouped = history.groupBy { daily ->
            if (daily.date.startsWith(TEST_PREFIX)) "TEST" else daily.date.substring(0, 4)
        }
        buildList<HistoryRow> {
            grouped.keys.sortedWith(
                compareByDescending<String> { if (it == "TEST") "0000" else it }
            ).forEach { year ->
                add(HistoryRow.Year(year))
                val entries = grouped[year].orEmpty()
                addAll(entries.sortedByDescending { it.date }.map { HistoryRow.Entry(it) })
            }
        }
    }

    // ✅ Focus: visible list item merkezine en yakın Entry index
    val focusedEntryIndexComputed by remember {
        derivedStateOf {
            val layout = state.layoutInfo
            val viewportCenter = layout.viewportStartOffset +
                    (layout.viewportEndOffset - layout.viewportStartOffset) / 2

            layout.visibleItemsInfo
                .filter { rows.getOrNull(it.index) is HistoryRow.Entry }
                .minByOrNull { info ->
                    val center = info.offset + info.size / 2
                    abs(center - viewportCenter)
                }?.index
        }
    }

    // ✅ Fallback: ilk açılışta visibleItemsInfo boşsa yine de bir entry focuslansın
    val firstEntryIndex = remember(rows) {
        rows.indexOfFirst { it is HistoryRow.Entry }.takeIf { it >= 0 }
    }

    val focusedEntryIndex = focusedEntryIndexComputed ?: firstEntryIndex

    val sortedHistory = remember(history) { history.sortedByDescending { it.date } }
    val last7Days = remember(sortedHistory) { sortedHistory.take(7) }
    val weeklyTotalSteps = remember(last7Days) { last7Days.sumOf { it.steps } }
    val weeklyAvgSteps = remember(last7Days) { if (last7Days.isEmpty()) 0 else weeklyTotalSteps / last7Days.size }
    val bestDayWeekly = remember(last7Days) { last7Days.maxByOrNull { it.steps } }
    val currentStreak = remember(sortedHistory) { calculateStepStreak(sortedHistory) }
    val weeklyChartData = remember(last7Days) { last7Days.reversed() }

    val scope = rememberCoroutineScope()

    val firstYearIndex = remember(rows) {
        rows.indexOfFirst { it is HistoryRow.Year && it.year != "TEST" }.coerceAtLeast(0)
    }

    val last30Index = remember(rows) {
        val date30 = sortedHistory.getOrNull(29)?.date
        if (date30 == null) 0
        else rows.indexOfFirst { it is HistoryRow.Entry && it.daily.date == date30 }.takeIf { it >= 0 } ?: 0
    }

    val last7Index = remember(rows) {
        val date7 = sortedHistory.getOrNull(6)?.date
        if (date7 == null) 0
        else rows.indexOfFirst { it is HistoryRow.Entry && it.daily.date == date7 }.takeIf { it >= 0 } ?: 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "History",
                        color = cs.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(pad)
        ) {
            if (history.isEmpty()) {
                Text(
                    "No data yet",
                    color = cs.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    HistoryPremiumOverviewCard(
                        weeklyTotalSteps = weeklyTotalSteps,
                        weeklyAvgSteps = weeklyAvgSteps,
                        bestDay = bestDayWeekly,
                        currentStreak = currentStreak,
                        chartData = weeklyChartData,
                        onJumpLast7 = { scope.launch { state.animateScrollToItem(last7Index) } },
                        onJumpLast30 = { scope.launch { state.animateScrollToItem(last30Index) } },
                        onJumpThisYear = { scope.launch { state.animateScrollToItem(firstYearIndex) } }
                    )

                    HistoryTimelineContainer(
                        rows = rows,
                        state = state,
                        focusedEntryIndex = focusedEntryIndex,
                        timeLineGradient = timeLineGradient,
                        waterMap = waterMap,
                        activity = activity,
                        onForceReload = onForceReload
                    )
                }
            }
        }
    }
}

/* ======================================================
   PREMIUM HISTORY OVERVIEW CARD (APPLE STYLE)
   ====================================================== */

@Composable
private fun HistoryPremiumOverviewCard(
    weeklyTotalSteps: Int,
    weeklyAvgSteps: Int,
    bestDay: DailySteps?,
    currentStreak: Int,
    chartData: List<DailySteps>,
    onJumpLast7: () -> Unit,
    onJumpLast30: () -> Unit,
    onJumpThisYear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val neon = Brush.horizontalGradient(listOf(Color(0xFF00FFA3), Color(0xFF00F5FF)))

    val bestDayLabel = remember(bestDay) {
        if (bestDay == null) "None"
        else {
            try {
                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfOut = SimpleDateFormat("EEE dd MMM", Locale.getDefault())
                sdfOut.format(sdfIn.parse(bestDay.date)!!)
            } catch (_: Exception) {
                bestDay.date
            }
        }
    }

    val bestDaySteps = bestDay?.steps ?: 0

    Card(
        modifier = modifier
            .shadow(18.dp, RoundedCornerShape(26.dp))
            .padding(bottom = 4.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        shape = RoundedCornerShape(26.dp),
        border = BorderStroke(
            1.dp,
            if (isDark) Color.White.copy(alpha = 0.06f) else Color.Black.copy(alpha = 0.06f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "This Week",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )
                    Text(
                        text = "Premium overview of your last 7 days",
                        fontSize = 11.sp,
                        color = cs.onSurface.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(Color(0xFF00FFA3).copy(alpha = 0.15f))
                        .border(
                            width = 1.dp,
                            color = Color(0xFF00FFA3).copy(alpha = 0.35f),
                            shape = RoundedCornerShape(999.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "🔥 $currentStreak days",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFA3),
                        maxLines = 1
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HistoryMiniMetric("Total", formatNumber(weeklyTotalSteps))
                HistoryMiniMetric("Avg/day", formatNumber(weeklyAvgSteps))
                HistoryMiniMetric("Best", formatNumber(bestDaySteps))
            }

            Text(
                text = "Best day: $bestDayLabel",
                fontSize = 11.sp,
                color = cs.onSurface.copy(alpha = 0.75f)
            )

            MiniWeeklyStepsChart(
                data = chartData,
                neon = neon
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HistoryQuickButton("Last 7", onClick = onJumpLast7, modifier = Modifier.weight(1f))
                HistoryQuickButton("Last 30", onClick = onJumpLast30, modifier = Modifier.weight(1f))
                HistoryQuickButton("This Year", onClick = onJumpThisYear, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryMiniMetric(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(text = label, fontSize = 10.sp, color = cs.onSurface.copy(alpha = 0.65f))
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = cs.onSurface)
    }
}

@Composable
private fun HistoryQuickButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (isDark) Color.White.copy(alpha = 0.06f)
                else Color.Black.copy(alpha = 0.06f)
            )
            .clickable { onClick() }
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface.copy(alpha = 0.85f)
        )
    }
}

@Composable
private fun MiniWeeklyStepsChart(data: List<DailySteps>, neon: Brush) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val max = remember(data) { data.maxOfOrNull { it.steps }?.coerceAtLeast(1) ?: 1 }
    val baseBarColor = if (isDark) Color(0xFF1E2635) else Color.Black.copy(alpha = 0.06f)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(92.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEach { entry ->
            val ratio = (entry.steps.toFloat() / max.toFloat()).coerceIn(0.06f, 1f)

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Bottom
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(68.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(baseBarColor),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(ratio)
                            .clip(RoundedCornerShape(999.dp))
                            .background(neon)
                    )
                }

                val label = remember(entry.date) {
                    try {
                        val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val sdfOut = SimpleDateFormat("EE", Locale.getDefault())
                        sdfOut.format(sdfIn.parse(entry.date)!!)
                    } catch (_: Exception) {
                        "?"
                    }
                }

                Text(
                    text = label,
                    fontSize = 9.sp,
                    color = cs.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

private fun calculateStepStreak(sortedHistory: List<DailySteps>): Int {
    if (sortedHistory.isEmpty()) return 0
    val map = sortedHistory.associateBy { it.date }

    var streak = 0
    val current = try { java.time.LocalDate.now() } catch (_: Exception) { return 0 }

    var day = current
    while (true) {
        val entry = map[day.toString()]
        if (entry != null && entry.steps > 0) {
            streak++
            day = day.minusDays(1)
        } else break
    }
    return streak
}

private data class StreakResult(val current: Int, val best: Int)

private fun calculateStreak(history: List<DailySteps>): StreakResult {
    if (history.isEmpty()) return StreakResult(current = 0, best = 0)

    val sorted = history
        .filter { !it.date.startsWith(TEST_PREFIX) }
        .sortedBy { it.date }

    var best = 0
    var temp = 0
    for (day in sorted) {
        if (day.steps > 0) {
            temp++
            if (temp > best) best = temp
        } else temp = 0
    }

    var current = 0
    for (i in sorted.indices.reversed()) {
        if (sorted[i].steps > 0) current++ else break
    }

    return StreakResult(current = current, best = best)
}

@Composable
private fun YearHeader(year: String) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val label = if (year == "TEST") "Test Data" else year

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 64.dp, top = 6.dp, bottom = 6.dp),
        color = Color.Transparent
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground
            )

            Spacer(modifier = Modifier.width(10.dp))

            Box(
                modifier = Modifier
                    .height(2.dp)
                    .weight(1f)
                    .background(
                        if (isDark) Color.White.copy(alpha = 0.08f)
                        else Color.Black.copy(alpha = 0.08f),
                        RoundedCornerShape(999.dp)
                    )
            )
        }
    }
}

@Composable
private fun HistoryEntryCard(
    date: String,
    steps: Int,
    waterMl: Int,
    isFocused: Boolean,
    onChanged: () -> Unit,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val dao = remember { AppDatabase.getDatabase(ctx).dailyStepsDao() }
    val waterDao = remember { AppDatabase.getDatabase(ctx).dailyWaterDao() }

    var showEditSheet by remember { mutableStateOf(false) }
    var editableSteps by remember { mutableStateOf(steps) }
    var editableWaterMl by remember { mutableStateOf(waterMl) }

    val distance = (steps * 0.75) / 1000.0
    val neon = Brush.linearGradient(listOf(Color(0xFF00FFA3), Color(0xFF00F5FF)))
    val grayBorder = if (isDark) Color(0x20FFFFFF) else Color(0x14000000)

    // ✅ Daha belirgin focus anim
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.07f else 1f,
        animationSpec = tween(260),
        label = "historyScale"
    )
    val alpha by animateFloatAsState(
        targetValue = if (isFocused) 1f else 0.62f,
        animationSpec = tween(260),
        label = "historyAlpha"
    )

    val (prettyDate, isTest) = remember(date) {
        if (date.startsWith(TEST_PREFIX)) {
            val base = date.removePrefix(TEST_PREFIX)
            val formatted = try {
                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfOut = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdfOut.format(sdfIn.parse(base)!!)
            } catch (_: Exception) {
                base
            }
            formatted to true
        } else {
            val formatted = try {
                val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdfOut = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                sdfOut.format(sdfIn.parse(date)!!)
            } catch (_: Exception) {
                date
            }
            formatted to false
        }
    }

    val dfKm = remember { DecimalFormat("#.#") }
    val dfL = remember { DecimalFormat("#.#") }
    val waterText = if (waterMl > 0) "${dfL.format(waterMl / 1000f)} L" else "Not logged"

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Canvas(
            modifier = Modifier
                .width(TimelineGutter)
                .height(66.dp)
        ) {
            val cx = size.width / 2f
            val cy = size.height / 2f

            drawCircle(
                color = Color.White.copy(alpha = if (isFocused) 0.18f else 0.06f),
                radius = if (isFocused) 18.dp.toPx() else 14.dp.toPx(),
                center = Offset(cx, cy)
            )

            drawCircle(
                brush = Brush.radialGradient(
                    listOf(Color(0xFF00F5FF), Color(0xFF00FFA3))
                ),
                radius = if (isFocused) 10.dp.toPx() else 7.dp.toPx(),
                center = Offset(cx, cy)
            )
        }

        Surface(
            modifier = Modifier
                .padding(start = 10.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    this.alpha = alpha
                }
                .weight(1f)
                .clickable { onClick() },
            shape = RoundedCornerShape(24.dp),
            color = cs.surface,
            // ✅ glow-like elevation
            shadowElevation = if (isFocused) 22.dp else 8.dp,
            border = BorderStroke(
                width = if (isFocused) 2.4.dp else 1.dp,
                brush = if (isFocused) neon else SolidColor(grayBorder)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                cs.surface.copy(alpha = 1f),
                                cs.surfaceVariant.copy(alpha = if (isDark) 0.55f else 0.75f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = prettyDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = cs.onSurface.copy(alpha = 0.65f)
                    )

                    if (isTest) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TEST",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFB74D)
                        )
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    IconButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Create,
                            contentDescription = "Edit",
                            tint = Color(0xFF38E3FF),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = steps.toString(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFocused) Color(0xFF00F5FF) else cs.onSurface
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Distance", fontSize = 10.sp, color = cs.onSurface.copy(alpha = 0.55f))
                        Text(
                            text = "${dfKm.format(distance)} km",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("Water", fontSize = 10.sp, color = cs.onSurface.copy(alpha = 0.55f))
                        Text(
                            text = waterText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (waterMl > 0) Color(0xFF80DEEA)
                            else cs.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
            }
        }
    }

    if (showEditSheet) {
        Dialog(onDismissRequest = { showEditSheet = false }) {
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = cs.surface,
                shadowElevation = 18.dp,
                modifier = Modifier.fillMaxWidth(0.92f)
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
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color(0xFF00F5FF)
                            )
                        }

                        Text(
                            text = "Edit Day",
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
                        label = { Text("Steps") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editableWaterMl.toString(),
                        onValueChange = {
                            if (it.all(Char::isDigit)) {
                                editableWaterMl = it.toIntOrNull() ?: editableWaterMl
                            }
                        },
                        singleLine = true,
                        label = { Text("Water (ml)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Button(
                        onClick = {
                            CoroutineScope(Dispatchers.IO).launch {
                                dao.insertDailySteps(DailySteps(date, editableSteps))
                                waterDao.insertDailyWater(DailyWater(date = date, waterMl = editableWaterMl))
                                withContext(Dispatchers.Main) { onChanged() }
                            }

                            val intent = Intent(ctx, StepCounterService::class.java).apply {
                                putExtra("manualSteps", editableSteps)
                            }
                            ctx.startService(intent)

                            Toast.makeText(ctx, "Day updated", Toast.LENGTH_SHORT).show()
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
                        Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        }
    }
}

private fun formatNumber(value: Int): String {
    return try {
        DecimalFormat("#,###").format(value).replace(",", ".")
    } catch (_: Exception) {
        value.toString()
    }
}

@Composable
private fun ColumnScope.HistoryTimelineContainer(
    rows: List<HistoryRow>,
    state: androidx.compose.foundation.lazy.LazyListState,
    focusedEntryIndex: Int?,
    timeLineGradient: Brush,
    waterMap: Map<String, DailyWater>,
    activity: Activity,
    onForceReload: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val containerBorder =
        if (isDark) Color.White.copy(alpha = 0.08f)
        else Color.Black.copy(alpha = 0.08f)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .weight(1f),
        shape = RoundedCornerShape(28.dp),
        color = cs.surface,
        shadowElevation = 18.dp,
        border = BorderStroke(1.dp, containerBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(TimelineGutter)
                    .align(Alignment.CenterStart)
            ) {
                val cx = size.width / 2f
                drawLine(
                    brush = timeLineGradient,
                    start = Offset(cx, 0f),
                    end = Offset(cx, size.height),
                    strokeWidth = 3.5f
                )
            }

            LazyColumn(
                state = state,
                contentPadding = PaddingValues(
                    top = 22.dp,
                    bottom = 26.dp,
                    end = 16.dp,
                    start = 0.dp
                ),
                verticalArrangement = Arrangement.spacedBy(26.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                itemsIndexed(
                    rows,
                    key = { _, row ->
                        when (row) {
                            is HistoryRow.Year -> "year-${row.year}"
                            is HistoryRow.Entry -> "entry-${row.daily.date}"
                        }
                    }
                ) { index, row ->
                    when (row) {
                        is HistoryRow.Year -> YearHeader(row.year)
                        is HistoryRow.Entry -> {
                            val isFocused = focusedEntryIndex != null && index == focusedEntryIndex
                            val water = waterMap[row.daily.date]?.waterMl ?: 0

                            HistoryEntryCard(
                                date = row.daily.date,
                                steps = row.daily.steps,
                                waterMl = water,
                                isFocused = isFocused,
                                onChanged = onForceReload,
                                onClick = {
                                    val intent = Intent(activity, HistoryDetailActivity::class.java).apply {
                                        putExtra("date", row.daily.date)
                                        putExtra("steps", row.daily.steps)
                                        putExtra("waterMl", water)
                                    }
                                    activity.startActivity(intent)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}