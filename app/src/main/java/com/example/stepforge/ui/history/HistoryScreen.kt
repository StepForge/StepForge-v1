package com.example.stepforge.ui.history

import android.app.Activity
import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.AchievementsActivity
import com.example.stepforge.HistoryDetailActivity
import com.example.stepforge.R
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.DailyWater
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.SleepSessionDao
import com.example.stepforge.data.WorkoutSession
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class HistorySheetType {
    CALENDAR,
    TOP_DAYS,
    ACHIEVEMENTS
}

@Composable
internal fun HistoryRoute(
    activity: Activity,
    stepsHistory: List<DailySteps>,
    waterHistory: List<DailyWater>,
    workouts: List<WorkoutSession>,
    sleepDao: SleepSessionDao,
    stepGoal: Int,
    waterGoal: Int,
    onBack: () -> Unit
) {
    var sleepHistory by remember { mutableStateOf<List<SleepSession>>(emptyList()) }
    var selectedMetric by remember { mutableStateOf(HistoryMetric.STEPS) }
    var selectedRange by remember { mutableStateOf(HistoryRange.MONTH) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var visibleMonth by remember { mutableStateOf(YearMonth.now()) }
    var weatherMood by remember { mutableStateOf(HistoryBackend.fallbackWeatherMood()) }

    LaunchedEffect(Unit) {
        sleepHistory = withContext(Dispatchers.IO) { sleepDao.getRecentSessions(365) }
    }

    LaunchedEffect(Unit) {
        weatherMood = HistoryBackend.resolveWeatherMood(activity)
    }

    val uiState = remember(
        stepsHistory,
        waterHistory,
        workouts,
        sleepHistory,
        stepGoal,
        waterGoal,
        selectedDate,
        selectedMetric,
        selectedRange,
        visibleMonth,
        weatherMood
    ) {
        HistoryBackend.buildState(
            context = activity,
            stepsHistory = stepsHistory,
            waterHistory = waterHistory,
            sleepHistory = sleepHistory,
            workouts = workouts,
            stepGoal = stepGoal,
            waterGoal = waterGoal,
            selectedDate = selectedDate,
            selectedMetric = selectedMetric,
            selectedRange = selectedRange,
            visibleMonth = visibleMonth,
            weatherMood = weatherMood
        )
    }

    HistoryScreen(
        state = uiState,
        onBack = onBack,
        onMetricSelected = { selectedMetric = it },
        onRangeSelected = { selectedRange = it },
        onDateSelected = { date ->
            selectedDate = date
            visibleMonth = YearMonth.from(date)
        },
        onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
        onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
        onOpenDetails = { day ->
            activity.startActivity(
                Intent(activity, HistoryDetailActivity::class.java).apply {
                    putExtra("date", day.date)
                    putExtra("steps", day.steps)
                    putExtra("waterMl", day.waterMl)
                }
            )
        },
        onViewAchievements = {
            activity.startActivity(Intent(activity, AchievementsActivity::class.java))
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreen(
    state: HistoryUiState,
    onBack: () -> Unit,
    onMetricSelected: (HistoryMetric) -> Unit,
    onRangeSelected: (HistoryRange) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onOpenDetails: (HistoryDayUi) -> Unit,
    onViewAchievements: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    var activeSheet by remember { mutableStateOf<HistorySheetType?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = cs.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(innerPadding)
        ) {
            HistorySolidHistoryBackground()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 8.dp, bottom = 26.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    HistoryHeader(
                        onBack = onBack,
                        onCalendarClick = { activeSheet = HistorySheetType.CALENDAR }
                    )
                }
                item {
                    HistoryMetricSelector(
                        selected = state.selectedMetric,
                        onSelected = onMetricSelected
                    )
                }
                item {
                    HistoryMountainHeroCard(state = state)
                }
                item {
                    HistoryThisWeekCard(
                        state = state,
                        onRangeSelected = onRangeSelected,
                        onDateSelected = onDateSelected
                    )
                }
                item {
                    HistoryOverviewCard(state = state)
                }
                item {
                    HistoryCalendarCard(
                        state = state,
                        onDateSelected = { date ->
                            val day = state.calendarDays.firstOrNull { it.date == date }?.day
                            if (day != null) {
                                onOpenDetails(day)
                            } else {
                                onDateSelected(date)
                            }
                        },
                        onPreviousMonth = onPreviousMonth,
                        onNextMonth = onNextMonth
                    )
                }
                item {
                    HistoryTrendCard(
                        state = state,
                        onDateSelected = onDateSelected
                    )
                }
                item {
                    HistoryTopDaysCard(
                        state = state,
                        onOpenDetails = onOpenDetails,
                        onViewAll = { activeSheet = HistorySheetType.TOP_DAYS }
                    )
                }
                item {
                    HistoryAchievementsCard(
                        state = state,
                        onViewAll = onViewAchievements
                    )
                }
            }
        }
    }

    activeSheet?.let { sheet ->
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 4.dp)
                        .size(width = 46.dp, height = 4.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f))
                )
            }
        ) {
            when (sheet) {
                HistorySheetType.CALENDAR -> HistoryCalendarSheetContent(
                    state = state,
                    onDateSelected = onDateSelected,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth
                )
                HistorySheetType.TOP_DAYS -> HistoryTopDaysSheetContent(
                    state = state,
                    onOpenDetails = onOpenDetails
                )
                HistorySheetType.ACHIEVEMENTS -> HistoryAchievementsSheetContent(state = state)
            }
        }
    }
}

@Composable
private fun HistoryHeader(
    onBack: () -> Unit,
    onCalendarClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(cs.surface.copy(alpha = 0.56f))
                .border(1.dp, cs.primary.copy(alpha = 0.16f), CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.h3_back),
                tint = cs.onSurface,
                modifier = Modifier.size(17.dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stringResource(R.string.h3_title),
                color = cs.onBackground,
                fontSize = 20.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = stringResource(R.string.h3_subtitle),
                color = cs.onSurfaceVariant,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(cs.surface.copy(alpha = 0.58f))
                .border(1.dp, Color(0xFFFF4FC3).copy(alpha = 0.38f), RoundedCornerShape(13.dp))
                .clickable { onCalendarClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = stringResource(R.string.h3_calendar_view),
                tint = cs.onSurface,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun HistoryMetricSelector(
    selected: HistoryMetric,
    onSelected: (HistoryMetric) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        HistoryMetric.values().forEach { metric ->
            val accent = metric.historyAccent()
            val selectedProgress by animateFloatAsState(if (selected == metric) 1f else 0f, tween(280), label = "metricSelected")
            val borderColor = accent.copy(alpha = 0.18f + selectedProgress * 0.70f)
            Column(
                modifier = Modifier
                    .weight(1f)
                    .height(68.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        lerp(
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.76f),
                            accent.copy(alpha = 0.14f),
                            selectedProgress
                        )
                    )
                    .border(1.dp, borderColor, RoundedCornerShape(18.dp))
                    .clickable { onSelected(metric) }
                    .padding(horizontal = 5.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = metric.historyIcon(),
                    contentDescription = metricLabel(metric),
                    tint = accent,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = metricLabel(metric),
                    color = if (selected == metric) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    fontSize = if (metric == HistoryMetric.ACTIVE_TIME) 8.4.sp else 9.4.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun metricLabel(metric: HistoryMetric): String = when (metric) {
    HistoryMetric.STEPS -> stringResource(R.string.h3_steps)
    HistoryMetric.DISTANCE -> stringResource(R.string.h3_distance)
    HistoryMetric.CALORIES -> stringResource(R.string.h3_calories)
    HistoryMetric.ACTIVE_TIME -> stringResource(R.string.h3_active_time)
}

private fun HistoryMetric.historyIcon(): ImageVector = when (this) {
    HistoryMetric.STEPS -> Icons.Outlined.DirectionsWalk
    HistoryMetric.DISTANCE -> Icons.Outlined.LocationOn
    HistoryMetric.CALORIES -> Icons.Outlined.LocalFireDepartment
    HistoryMetric.ACTIVE_TIME -> Icons.Outlined.AccessTime
}

private fun HistoryMetric.historyAccent(): Color = when (this) {
    HistoryMetric.STEPS -> Color(0xFFFF4FC3)
    HistoryMetric.DISTANCE -> Color(0xFF2EA8FF)
    HistoryMetric.CALORIES -> Color(0xFFFF8A34)
    HistoryMetric.ACTIVE_TIME -> Color(0xFF42F06D)
}

@Composable
private fun HistoryCalendarSheetContent(
    state: HistoryUiState,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.h3_calendar_view),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        item {
            HistoryCalendarCard(
                state = state,
                onDateSelected = onDateSelected,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth
            )
        }
        item {
            HistorySelectedDayPanel(state = state)
        }
    }
}

@Composable
private fun HistorySelectedDayPanel(state: HistoryUiState) {
    val day = state.selectedDay
    val accent = state.selectedMetric.historyAccent()
    PremiumHistoryCard(glow = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = day.localDate.shortMonthDay(),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 18.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.h3_selected_day_summary),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = day.metricDisplay(state.selectedMetric),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 22.sp,
                        lineHeight = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = metricHeroUnit(state.selectedMetric),
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(R.string.h3_goal_progress),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${(day.goalProgress(state.selectedMetric).coerceIn(0f, 1f) * 100f).toInt()}%",
                        color = accent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(day.goalProgress(state.selectedMetric).coerceIn(0f, 1f))
                            .height(8.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(accent)
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HistoryQuickMetric(label = stringResource(R.string.h3_steps), value = day.steps.formatHistoryNumber(), modifier = Modifier.weight(1f))
                HistoryQuickMetric(label = stringResource(R.string.h3_distance), value = "${day.distanceKm.formatHistoryKm()} ${stringResource(R.string.h3_km)}", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HistoryQuickMetric(label = stringResource(R.string.h3_calories), value = "${day.calories.formatHistoryNumber()} ${stringResource(R.string.h3_kcal)}", modifier = Modifier.weight(1f))
                HistoryQuickMetric(label = stringResource(R.string.h3_active_time), value = day.activeMinutes.formatHistoryDurationShort(), modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                HistoryQuickMetric(label = stringResource(R.string.h3_water), value = "${day.waterMl} ml", modifier = Modifier.weight(1f))
                HistoryQuickMetric(label = stringResource(R.string.h3_sleep), value = day.sleepMinutes.formatHistoryDurationShort(), modifier = Modifier.weight(1f))
                HistoryQuickMetric(label = stringResource(R.string.h3_sessions), value = day.workoutSessions.formatHistoryNumber(), modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HistoryTopDaysSheetContent(
    state: HistoryUiState,
    onOpenDetails: (HistoryDayUi) -> Unit
) {
    val days = remember(state.visibleDays, state.selectedMetric) {
        state.visibleDays.sortedByDescending { it.metricValue(state.selectedMetric) }.take(6)
    }
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.h3_top_days),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
        item {
            PremiumHistoryCard(glow = false) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = stringResource(R.string.h3_best_performance),
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = metricHeroUnit(state.selectedMetric),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        days.firstOrNull()?.let { best ->
                            Text(
                                text = best.metricDisplay(state.selectedMetric),
                                color = Color(0xFFFFB02E),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    HistoryTopDaysPodiumVisual(
                        days = days.take(6),
                        metric = state.selectedMetric,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(188.dp)
                    )
                }
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.h3_top_six_days),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                days.chunked(3).forEach { rowDays ->
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowDays.forEachIndexed { rowIndex, day ->
                            val rank = days.indexOf(day) + 1
                            HistoryTopDayBoardTile(
                                day = day,
                                rank = rank,
                                metric = state.selectedMetric,
                                onClick = { onOpenDetails(day) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        repeat((3 - rowDays.size).coerceAtLeast(0)) { Spacer(modifier = Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryTopDayBoardTile(
    day: HistoryDayUi,
    rank: Int,
    metric: HistoryMetric,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = when (rank) {
        1 -> Color(0xFFFFB02E)
        2 -> Color(0xFF18E8FF)
        3 -> Color(0xFF9B5CFF)
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        modifier = modifier
            .height(104.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(accent),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = rank.toString(),
                    color = Color.Black,
                    fontSize = 10.sp,
                    lineHeight = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.offset(y = (-0.5).dp)
                )
            }
            Text(
                text = day.localDate.shortMonthDay(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Text(day.metricDisplay(metric), color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(metricHeroUnit(metric), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 9.5.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HistoryAchievementsSheetContent(state: HistoryUiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.h3_achievements),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold
        )
        state.achievements.forEach { achievement ->
            HistorySheetRow(
                rank = achievement.level,
                title = achievement.title,
                value = achievement.subtitle,
                subtitle = "${(achievement.progress * 100f).toInt()}%",
                accent = when (achievement.level) {
                    1 -> Color(0xFFFFB02E)
                    2 -> Color(0xFFFFC13D)
                    3 -> Color(0xFF59F27A)
                    else -> Color(0xFF18E8FF)
                },
                onClick = {}
            )
        }
    }
}

@Composable
private fun HistorySheetRow(
    rank: Int,
    title: String,
    value: String,
    subtitle: String,
    accent: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.10f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(accent),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = rank.toString(),
                color = Color.Black,
                fontSize = 11.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.offset(y = (-0.5).dp)
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun HistoryQuickMetric(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.56f))
            .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
