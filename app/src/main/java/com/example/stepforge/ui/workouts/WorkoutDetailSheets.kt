package com.example.stepforge.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import java.text.NumberFormat
import kotlin.math.max
import kotlin.math.roundToInt
import java.util.Locale
import java.time.temporal.TemporalAdjusters
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.YearMonth
import java.time.LocalDate
import java.time.Instant
import java.time.DayOfWeek

@Composable
internal fun WorkoutDetailSheetContent(
    sheet: WorkoutSheetType,
    state: WorkoutsDashboardState,
    onClose: () -> Unit
) {
    val isJourneySheet = sheet == WorkoutSheetType.JOURNEY || sheet == WorkoutSheetType.DEEPER

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = sheetTitle(sheet),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = if (isJourneySheet) 19.sp else 24.sp,
                    lineHeight = if (isJourneySheet) 23.sp else 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = sheetSubtitle(sheet),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = if (isJourneySheet) 12.sp else 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = stringResource(R.string.wv2_close))
            }
        }

        when (sheet) {
            WorkoutSheetType.TODAY -> TodaySheet(state)
            WorkoutSheetType.WEEK -> WeekSheet(state)
            WorkoutSheetType.ACTIVE_TIME -> ActiveTimeSheet(state)
            WorkoutSheetType.TRAINING_LOAD -> TrainingLoadSheet(state)
            WorkoutSheetType.STREAK -> StreakSheet(state)
            WorkoutSheetType.CALORIES -> CaloriesSheet(state)
            WorkoutSheetType.DISTANCE -> DistanceSheet(state)
            WorkoutSheetType.INTENSITY -> IntensitySheet(state)
            WorkoutSheetType.ZONES -> ZonesSheet(state)
            WorkoutSheetType.FOCUS -> FocusSheet(state)
            WorkoutSheetType.JOURNEY, WorkoutSheetType.DEEPER -> JourneySheet(state)
        }

        if (sheet != WorkoutSheetType.JOURNEY && sheet != WorkoutSheetType.DEEPER) {
            Button(
                onClick = onClose,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.Black
                )
            ) {
                Text(stringResource(R.string.wv2_close), fontWeight = FontWeight.ExtraBold)
            }
        }

        Spacer(Modifier.height(12.dp))
    }
}

@Composable
private fun TodaySheet(state: WorkoutsDashboardState) {
    DetailMetricGrid(
        metrics = listOf(
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_active_time), state.today.durationMinutes.formatDurationLocalized()),
            DetailMetric(Icons.Outlined.DirectionsWalk, stringResource(R.string.wv2_steps), NumberFormat.getIntegerInstance().format(state.today.steps)),
            DetailMetric(Icons.Outlined.LocationOn, stringResource(R.string.wv2_distance), "${state.today.distanceKm.formatKm()} ${stringResource(R.string.wv2_km)}"),
            DetailMetric(Icons.Outlined.LocalFireDepartment, stringResource(R.string.wv2_calories), "${state.today.calories} ${stringResource(R.string.wv2_kcal_unit)}")
        )
    )
    SessionListPreview(state.today.sessions)
}

@Composable
private fun WeekSheet(state: WorkoutsDashboardState) {
    DetailMetricGrid(
        metrics = listOf(
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_total_active_time), state.weekDurationMinutes.formatDurationLocalized()),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_active_days), stringResource(R.string.wv2_active_days_format, state.activeDays)),
            DetailMetric(Icons.Outlined.LocalFireDepartment, stringResource(R.string.wv2_calories), "${state.weekCalories} ${stringResource(R.string.wv2_kcal_unit)}"),
            DetailMetric(Icons.Outlined.Speed, stringResource(R.string.wv2_training_load), "${state.consistencyScore}/100")
        )
    )
    ThisWeekHeatmapCard(state = state, onClick = {})
}

@Composable
private fun ActiveTimeSheet(state: WorkoutsDashboardState) {
    val values = state.week.map { it.durationMinutes.toFloat() }
    val bestIndex = values.indices.maxByOrNull { values[it] } ?: 0
    val avg = values.filter { it > 0f }.average().toInt()

    DetailMetricGrid(
        metrics = listOf(
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_total_active_time), state.weekDurationMinutes.formatDurationLocalized()),
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_today), state.today.durationMinutes.formatDurationLocalized()),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_vs_last_week), signedPercentText(state.weekDurationMinutes.comparePercent(state.previousWeekDurationMinutes))),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_active_days), stringResource(R.string.wv2_active_days_format, state.activeDays))
        )
    )

    WeeklyBarsOverviewCard(
        title = stringResource(R.string.wv2_total_active_time),
        values = values,
        labels = state.week.map { it.date.localizedDayShort() },
        footerLeft = stringResource(R.string.wv2_peak_day_format, state.week.getOrNull(bestIndex)?.date?.localizedDayShort() ?: "-"),
        footerRight = stringResource(R.string.wv2_avg_duration_format, avg.formatDurationLocalized())
    )
}

@Composable
private fun TrainingLoadSheet(state: WorkoutsDashboardState) {
    val values = state.week.map { it.intensityScore.toFloat() }
    val bestIndex = values.indices.maxByOrNull { values[it] } ?: 0
    val avg = values.filter { it > 0f }.average().toInt()

    DetailMetricGrid(
        metrics = listOf(
            DetailMetric(Icons.Outlined.Speed, stringResource(R.string.wv2_training_load), "${state.consistencyScore}/100"),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_intensity), "${state.intensityScore}/100"),
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_total_active_time), state.weekDurationMinutes.formatDurationLocalized()),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_active_days), stringResource(R.string.wv2_active_days_format, state.activeDays))
        )
    )

    WeeklyBarsOverviewCard(
        title = stringResource(R.string.wv2_training_load),
        values = values,
        labels = state.week.map { it.date.localizedDayShort() },
        footerLeft = stringResource(R.string.wv2_peak_day_format, state.week.getOrNull(bestIndex)?.date?.localizedDayShort() ?: "-"),
        footerRight = stringResource(R.string.wv2_avg_score_format, avg),
        blue = true
    )
}

@Composable
private fun StreakSheet(state: WorkoutsDashboardState) {
    val longestRun = longestActiveRun(state.week)
    DetailMetricGrid(
        metrics = listOf(
            DetailMetric(Icons.Outlined.LocalFireDepartment, stringResource(R.string.wv2_streak), state.activeDays.toString()),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_this_week), stringResource(R.string.wv2_active_days_format, state.activeDays)),
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_longest_run), stringResource(R.string.wv2_days_format, longestRun)),
            DetailMetric(Icons.Outlined.Speed, stringResource(R.string.wv2_consistency), "${state.consistencyScore}/100")
        )
    )
    ThisWeekHeatmapCard(state = state, onClick = {})
}

@Composable
private fun CaloriesSheet(state: WorkoutsDashboardState) {
    val daily = state.week.map { it.calories.toFloat() }
    val bestIndex = daily.indices.maxByOrNull { daily[it] } ?: 0
    val avg = if (daily.isNotEmpty()) daily.average().toInt() else 0
    DetailMetricGrid(
        metrics = listOf(
            DetailMetric(Icons.Outlined.LocalFireDepartment, stringResource(R.string.wv2_calories_burned), stringResource(R.string.wv2_kcal_value_formatted, NumberFormat.getIntegerInstance().format(state.weekCalories))),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_vs_last_week), signedPercentText(state.weekCalories.comparePercent(state.previousWeekCalories))),
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_average_per_day), stringResource(R.string.wv2_kcal_value_format, avg)),
            DetailMetric(Icons.Outlined.Speed, stringResource(R.string.wv2_best_day), state.week.getOrNull(bestIndex)?.date?.localizedDayShort() ?: "-")
        )
    )
    WeeklyBarsOverviewCard(
        title = stringResource(R.string.wv2_daily_calories),
        values = daily,
        labels = state.week.map { it.date.localizedDayShort() },
        footerLeft = stringResource(R.string.wv2_peak_day_format, state.week.getOrNull(bestIndex)?.date?.localizedDayShort() ?: "-"),
        footerRight = stringResource(R.string.wv2_avg_kcal_format, avg)
    )
}

@Composable
private fun DistanceSheet(state: WorkoutsDashboardState) {
    val longestSessionKm = (state.displaySessions.maxOfOrNull { it.distanceMeters } ?: 0) / 1000f
    val activeDayCount = state.week.count { it.distanceMeters > 0 }.coerceAtLeast(1)
    val avgActiveKm = (state.weekDistanceMeters / 1000f) / activeDayCount
    DetailMetricGrid(
        metrics = listOf(
            DetailMetric(Icons.Outlined.LocationOn, stringResource(R.string.wv2_today), stringResource(R.string.wv2_distance_km_format, state.today.distanceKm.formatKm())),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_this_week), stringResource(R.string.wv2_distance_km_format, (state.weekDistanceMeters / 1000f).formatKm())),
            DetailMetric(Icons.Outlined.DirectionsWalk, stringResource(R.string.wv2_longest_workout), stringResource(R.string.wv2_distance_km_format, longestSessionKm.formatKm())),
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_average_active_day), stringResource(R.string.wv2_distance_km_format, avgActiveKm.formatKm()))
        )
    )
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                MiniRoutePath(
                    modifier = Modifier
                        .weight(1f)
                        .height(92.dp)
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(stringResource(R.string.wv2_route_snapshot), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
                    Text(
                        text = stringResource(R.string.wv2_route_week_summary_format, state.activeDays, (state.weekDistanceMeters / 1000f).formatKm()),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = stringResource(R.string.wv2_today_route_distance_format, state.today.distanceKm.formatKm()),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun IntensitySheet(state: WorkoutsDashboardState) {
    val values = state.week.map { it.intensityScore.toFloat() }
    val bestIndex = values.indices.maxByOrNull { values[it] } ?: 0
    val avg = values.filter { it > 0f }.average().toInt()
    DetailMetricGrid(
        metrics = listOf(
            DetailMetric(Icons.Outlined.Speed, stringResource(R.string.wv2_intensity), "${state.intensityScore}/100"),
            DetailMetric(Icons.Outlined.FitnessCenter, stringResource(R.string.wv2_training_load), "${state.consistencyScore}/100"),
            DetailMetric(Icons.Outlined.BarChart, stringResource(R.string.wv2_current_level), intensityLabel(state.intensityScore)),
            DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_best_day), state.week.getOrNull(bestIndex)?.date?.localizedDayShort() ?: "-")
        )
    )
    WeeklyBarsOverviewCard(
        title = stringResource(R.string.wv2_intensity_trend),
        values = values,
        labels = state.week.map { it.date.localizedDayShort() },
        footerLeft = stringResource(R.string.wv2_peak_day_format, state.week.getOrNull(bestIndex)?.date?.localizedDayShort() ?: "-"),
        footerRight = stringResource(R.string.wv2_avg_score_format, avg),
        blue = true
    )
}

@Composable
private fun ZonesSheet(state: WorkoutsDashboardState) {
    val topZone = state.zoneSlices.maxByOrNull { it.percent }
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                DonutChart(slices = state.zoneSlices, modifier = Modifier.size(142.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.zoneSlices.forEach { slice ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(zoneColor(slice.labelType))
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                Text(
                                    text = stringResource(R.string.wv2_percent_label_format, slice.percent, zoneLabel(slice.labelType)),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp
                                )
                                Text(
                                    text = stringResource(R.string.wv2_about_this_week_format, ((state.weekDurationMinutes * slice.percent) / 100f).toInt().formatDurationLocalized()),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 10.sp
                                )
                            }
                        }
                    }
                }
            }
            DetailMetricGrid(
                metrics = listOf(
                    DetailMetric(Icons.Outlined.Speed, stringResource(R.string.wv2_top_zone), topZone?.let { zoneLabel(it.labelType) } ?: "-"),
                    DetailMetric(Icons.Outlined.Timer, stringResource(R.string.wv2_weekly_active_time), state.weekDurationMinutes.formatDurationLocalized())
                )
            )
            Text(
                text = stringResource(R.string.wv2_zone_estimated_note),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun FocusSheet(state: WorkoutsDashboardState) {
    FocusDayCard(state = state, onClick = {})
}

@Composable
private fun JourneySheet(state: WorkoutsDashboardState) {
    val sessions = state.displaySessions
        .filter { it.isDisplayableWorkout() }
        .sortedByDescending { it.startTime }
    val today = LocalDate.now()
    val sessionsByDate = remember(sessions) { sessions.groupBy { it.safeLocalDate() } }
    val activeDates = remember(sessionsByDate) { sessionsByDate.keys }
    var selectedDate by remember { mutableStateOf(today) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }
    var selectedMetric by remember { mutableStateOf(JourneyMetric.TIME) }

    val selectedWeekStart = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val selectedWeekDays = (0..6).map { selectedWeekStart.plusDays(it.toLong()) }
    val selectedWeekSessions = selectedWeekDays.flatMap { sessionsByDate[it].orEmpty() }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        JourneyCalendarCard(
            visibleMonth = visibleMonth,
            selectedDate = selectedDate,
            activeDates = activeDates,
            onPreviousMonth = { visibleMonth = visibleMonth.minusMonths(1) },
            onNextMonth = { visibleMonth = visibleMonth.plusMonths(1) },
            onDateSelected = { date ->
                selectedDate = date
                visibleMonth = YearMonth.from(date)
            }
        )

        JourneyWeekChartCard(
            selectedMetric = selectedMetric,
            onMetricSelected = { selectedMetric = it },
            weekDays = selectedWeekDays,
            sessionsByDate = sessionsByDate,
            selectedDate = selectedDate
        )

        JourneyGoalOverviewCard(
            state = state,
            weekSessions = selectedWeekSessions
        )
    }
}

private enum class JourneyMetric {
    TIME,
    KCAL,
    PULSE
}

@Composable
private fun JourneyCalendarCard(
    visibleMonth: YearMonth,
    selectedDate: LocalDate,
    activeDates: Set<LocalDate>,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val monthTitle = visibleMonth.atDay(1).format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))
    val firstVisibleDay = visibleMonth.atDay(1).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val days = (0..41).map { firstVisibleDay.plusDays(it.toLong()) }

    NeonCard(modifier = Modifier.fillMaxWidth(), glow = true) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                JourneyRoundArrow(text = "‹", onClick = onPreviousMonth)
                Text(
                    text = monthTitle,
                    color = cs.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                JourneyRoundArrow(text = "›", onClick = onNextMonth)
            }

            Row(modifier = Modifier.fillMaxWidth()) {
                localizedWeekdayShortNames().forEach { day ->
                    Text(
                        text = day,
                        color = cs.primary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            days.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        val inMonth = YearMonth.from(date) == visibleMonth
                        val selected = date == selectedDate
                        val active = activeDates.contains(date)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(CircleShape)
                                .clickable { onDateSelected(date) },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 34.dp, height = 30.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .border(1.2.dp, cs.primary.copy(alpha = 0.86f), RoundedCornerShape(999.dp)),
                                    contentAlignment = Alignment.Center
                                ) {}
                            }
                            Text(
                                text = date.dayOfMonth.toString(),
                                color = when {
                                    !inMonth -> cs.onSurfaceVariant.copy(alpha = 0.42f)
                                    active -> Color(0xFF3EDBE6)
                                    selected -> cs.onSurface.copy(alpha = 0.94f)
                                    else -> cs.onSurface.copy(alpha = 0.86f)
                                },
                                fontSize = 15.sp,
                                fontWeight = if (selected || active) FontWeight.ExtraBold else FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JourneyRoundArrow(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = Color.Black,
            fontSize = 28.sp,
            lineHeight = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 1.dp)
        )
    }
}

@Composable
private fun JourneyWeekChartCard(
    selectedMetric: JourneyMetric,
    onMetricSelected: (JourneyMetric) -> Unit,
    weekDays: List<LocalDate>,
    sessionsByDate: Map<LocalDate, List<com.example.stepforge.data.WorkoutSession>>,
    selectedDate: LocalDate
) {
    val cs = MaterialTheme.colorScheme
    val values = weekDays.map { date ->
        val daySessions = sessionsByDate[date].orEmpty()
        when (selectedMetric) {
            JourneyMetric.TIME -> daySessions.sumOf { it.durationMinutes }.toFloat()
            JourneyMetric.KCAL -> daySessions.sumOf { it.caloriesKcal }.toFloat()
            JourneyMetric.PULSE -> {
                val pulseValues = daySessions.map { it.intensityScore() }.filter { it > 0 }
                if (pulseValues.isEmpty()) 0f else pulseValues.average().toFloat()
            }
        }
    }
    val selectedIndex = weekDays.indexOf(selectedDate).coerceAtLeast(0)
    val total = values.sum().roundToInt()
    val unit = when (selectedMetric) {
        JourneyMetric.TIME -> stringResource(R.string.wv2_minutes_unit)
        JourneyMetric.KCAL -> stringResource(R.string.wv2_kcal_unit)
        JourneyMetric.PULSE -> "/100"
    }

    NeonCard(modifier = Modifier.fillMaxWidth(), glow = true) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(22.dp)
            ) {
                JourneyChartTab(stringResource(R.string.wv2_sheet_time), selectedMetric == JourneyMetric.TIME) { onMetricSelected(JourneyMetric.TIME) }
                JourneyChartTab(stringResource(R.string.wv2_kcal_unit), selectedMetric == JourneyMetric.KCAL) { onMetricSelected(JourneyMetric.KCAL) }
                JourneyChartTab(stringResource(R.string.wv2_sheet_pulse), selectedMetric == JourneyMetric.PULSE) { onMetricSelected(JourneyMetric.PULSE) }
            }
            JourneyBarsCanvas(
                values = values,
                selectedIndex = selectedIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.wv2_total_label),
                    color = cs.onSurface.copy(alpha = 0.86f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = when (selectedMetric) {
                        JourneyMetric.TIME -> total.formatDurationLocalized()
                        JourneyMetric.KCAL -> "$total $unit"
                        JourneyMetric.PULSE -> "${values.getOrElse(selectedIndex) { 0f }.roundToInt()}$unit"
                    },
                    color = cs.primary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun JourneyChartTab(text: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() },
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
        )
        Box(
            modifier = Modifier
                .width(if (selected) 36.dp else 0.dp)
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun JourneyBarsCanvas(
    values: List<Float>,
    selectedIndex: Int,
    modifier: Modifier
) {
    val cs = MaterialTheme.colorScheme
    val maxValue = max(100f, (values.maxOrNull() ?: 0f) * 1.15f)
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val left = 36.dp.toPx()
            val right = 10.dp.toPx()
            val top = 8.dp.toPx()
            val bottom = 6.dp.toPx()
            val chartWidth = size.width - left - right
            val chartHeight = size.height - top - bottom
            val baseY = top + chartHeight
            val axisColor = cs.onSurface.copy(alpha = 0.18f)
            val gridColor = cs.onSurface.copy(alpha = 0.12f)
            val labels = listOf(maxValue, maxValue * 0.8f, maxValue * 0.6f, maxValue * 0.4f, maxValue * 0.2f, 0f)

            val axisTextSize = 10.sp.toPx()
            labels.forEachIndexed { index, value ->
                val y = top + chartHeight * (index / (labels.size - 1).toFloat())
                drawLine(gridColor, androidx.compose.ui.geometry.Offset(left, y), androidx.compose.ui.geometry.Offset(size.width - right, y), 1.dp.toPx())
                drawContext.canvas.nativeCanvas.drawText(
                    value.roundToInt().toString(),
                    2.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        color = android.graphics.Color.argb(210, 210, 225, 235)
                        textSize = axisTextSize
                        isAntiAlias = true
                        textAlign = android.graphics.Paint.Align.LEFT
                    }
                )
            }

            drawLine(axisColor, androidx.compose.ui.geometry.Offset(left, baseY), androidx.compose.ui.geometry.Offset(size.width - right, baseY), 1.2.dp.toPx())
            drawLine(axisColor, androidx.compose.ui.geometry.Offset(left, top), androidx.compose.ui.geometry.Offset(left, baseY), 1.2.dp.toPx())

            val slot = chartWidth / values.size.coerceAtLeast(1)
            val barWidth = slot * 0.27f
            values.forEachIndexed { index, value ->
                val x = left + slot * index + (slot - barWidth) / 2f
                val h = (chartHeight * (value / maxValue).coerceIn(0f, 1f)).coerceAtLeast(if (value > 0f) 8.dp.toPx() else 0f)
                val y = baseY - h
                val selected = index == selectedIndex
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        listOf(
                            if (selected) Color(0xFF2DFAFF) else cs.primary.copy(alpha = 0.92f),
                            if (selected) Color(0xFF008C9B) else Color(0xFF00545F).copy(alpha = 0.72f)
                        ),
                        startY = y,
                        endY = baseY
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset(x, y),
                    size = androidx.compose.ui.geometry.Size(barWidth, h),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(5.dp.toPx(), 5.dp.toPx())
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 36.dp, end = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            localizedWeekdayShortNames().forEachIndexed { index, label ->
                Text(
                    text = label,
                    color = if (index == selectedIndex) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp,
                    fontWeight = if (index == selectedIndex) FontWeight.ExtraBold else FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun JourneyGoalOverviewCard(
    state: WorkoutsDashboardState,
    weekSessions: List<com.example.stepforge.data.WorkoutSession>
) {
    val totalMinutes = weekSessions.sumOf { it.durationMinutes }
    val totalCalories = weekSessions.sumOf { it.caloriesKcal }
    val highIntensityMinutes = weekSessions.filter { it.intensityScore() >= 70 }.sumOf { it.durationMinutes }
    val score = state.workoutScore.coerceIn(0, 100)
    val trainingColor = Color(0xFF18E8FF)
    val kcalColor = Color(0xFFFFB02E)
    val pulseColor = Color(0xFFFF4FD8)

    NeonCard(modifier = Modifier.fillMaxWidth(), glow = true) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            JourneyGoalRing(
                score = score,
                trainingColor = trainingColor,
                kcalColor = kcalColor,
                pulseColor = pulseColor,
                modifier = Modifier
                    .width(154.dp)
                    .height(116.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                JourneySideMetric(
                    icon = Icons.Outlined.Timer,
                    label = stringResource(R.string.wv2_training_time),
                    value = totalMinutes.formatDurationLocalized(),
                    accent = trainingColor
                )
                JourneyDivider()
                JourneySideMetric(
                    icon = Icons.Outlined.LocalFireDepartment,
                    label = stringResource(R.string.wv2_burned_kcal),
                    value = "${NumberFormat.getIntegerInstance().format(totalCalories)} ${stringResource(R.string.wv2_kcal_unit)}",
                    accent = kcalColor
                )
                JourneyDivider()
                JourneySideMetric(
                    icon = Icons.Outlined.Speed,
                    label = stringResource(R.string.wv2_intensity),
                    value = highIntensityMinutes.formatDurationLocalized(),
                    accent = pulseColor
                )
            }
        }
    }
}

@Composable
private fun JourneyGoalRing(
    score: Int,
    trainingColor: Color,
    kcalColor: Color,
    pulseColor: Color,
    modifier: Modifier
) {
    val cs = MaterialTheme.colorScheme
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val diameter = minOf(size.width - 16.dp.toPx(), 136.dp.toPx())
            val top = 8.dp.toPx()
            val left = (size.width - diameter) / 2f
            val center = androidx.compose.ui.geometry.Offset(
                x = size.width / 2f,
                y = top + diameter / 2f
            )
            val ringSpecs = listOf(
                Triple(0.dp.toPx(), 8.5.dp.toPx(), trainingColor),
                Triple(15.5.dp.toPx(), 7.5.dp.toPx(), kcalColor),
                Triple(30.dp.toPx(), 6.5.dp.toPx(), pulseColor)
            )
            val sweeps = listOf(
                180f * (score / 100f),
                180f * ((score * 0.84f).coerceAtMost(100f) / 100f),
                180f * ((score * 0.66f).coerceAtMost(100f) / 100f)
            )

            ringSpecs.forEachIndexed { index, (inset, stroke, color) ->
                val topLeft = androidx.compose.ui.geometry.Offset(
                    x = left + stroke / 2f + inset,
                    y = top + stroke / 2f + inset
                )
                val arcSize = androidx.compose.ui.geometry.Size(
                    width = diameter - stroke - inset * 2f,
                    height = diameter - stroke - inset * 2f
                )
                drawArc(
                    color = color.copy(alpha = 0.13f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                drawArc(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            color.copy(alpha = 0.95f),
                            Color.White.copy(alpha = 0.64f),
                            color.copy(alpha = 0.96f)
                        ),
                        center = center
                    ),
                    startAngle = 180f,
                    sweepAngle = sweeps[index].coerceAtLeast(8f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = stroke,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
            drawArc(
                color = cs.primary.copy(alpha = 0.10f),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = androidx.compose.ui.geometry.Offset(
                    x = center.x - 35.dp.toPx(),
                    y = center.y - 35.dp.toPx()
                ),
                size = androidx.compose.ui.geometry.Size(70.dp.toPx(), 70.dp.toPx()),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx())
            )
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = score.toString(),
                    color = cs.onSurface,
                    fontSize = 22.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = "%",
                    color = cs.onSurface,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 2.dp),
                    maxLines = 1
                )
            }
            Text(
                text = stringResource(R.string.wv2_weekly_goal),
                color = cs.onSurfaceVariant,
                fontSize = 9.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun JourneySideMetric(
    icon: ImageVector,
    label: String,
    value: String,
    accent: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(18.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = label,
                color = accent,
                fontSize = 8.5.sp,
                lineHeight = 9.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )
        }
    }
}

@Composable
private fun JourneyDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f))
    )
}

@Composable
private fun FullSessionRow(session: com.example.stepforge.data.WorkoutSession) {
    val kind = session.inferWorkoutKind()
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (kind) {
                        WorkoutKind.WALK -> Icons.Outlined.DirectionsWalk
                        WorkoutKind.RUN -> Icons.Outlined.DirectionsRun
                        WorkoutKind.STRENGTH -> Icons.Outlined.FitnessCenter
                        WorkoutKind.CYCLING -> Icons.Outlined.DirectionsBike
                        WorkoutKind.GENERIC -> Icons.Outlined.Timer
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.durationMinutes.formatDurationLocalized(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = stringResource(
                        R.string.wv2_session_stats_format,
                        NumberFormat.getIntegerInstance().format(session.steps),
                        (session.distanceMeters / 1000f).formatKm(),
                        NumberFormat.getIntegerInstance().format(session.caloriesKcal)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}


@Composable
private fun Int.formatDurationLocalized(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> stringResource(R.string.wv2_duration_hours_minutes_format, hours, minutes)
        hours > 0 -> stringResource(R.string.wv2_duration_hours_format, hours)
        else -> stringResource(R.string.wv2_duration_minutes_format, minutes)
    }
}

@Composable
private fun LocalDate.localizedDayShort(): String {
    return when (dayOfWeek) {
        DayOfWeek.MONDAY -> stringResource(R.string.wv2_day_mon_short)
        DayOfWeek.TUESDAY -> stringResource(R.string.wv2_day_tue_short)
        DayOfWeek.WEDNESDAY -> stringResource(R.string.wv2_day_wed_short)
        DayOfWeek.THURSDAY -> stringResource(R.string.wv2_day_thu_short)
        DayOfWeek.FRIDAY -> stringResource(R.string.wv2_day_fri_short)
        DayOfWeek.SATURDAY -> stringResource(R.string.wv2_day_sat_short)
        DayOfWeek.SUNDAY -> stringResource(R.string.wv2_day_sun_short)
    }
}

@Composable
private fun localizedWeekdayShortNames(): List<String> = listOf(
    stringResource(R.string.wv2_day_mon_short),
    stringResource(R.string.wv2_day_tue_short),
    stringResource(R.string.wv2_day_wed_short),
    stringResource(R.string.wv2_day_thu_short),
    stringResource(R.string.wv2_day_fri_short),
    stringResource(R.string.wv2_day_sat_short),
    stringResource(R.string.wv2_day_sun_short)
)

private data class DetailMetric(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String,
    val value: String
)

@Composable
private fun DetailMetricGrid(metrics: List<DetailMetric>) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        metrics.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { metric ->
                    DetailMetricCard(metric, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun DetailMetricCard(metric: DetailMetric, modifier: Modifier) {
    NeonCard(modifier = modifier.heightIn(min = 92.dp), glow = false) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(metric.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(metric.label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Text(metric.value, color = MaterialTheme.colorScheme.onSurface, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun SessionListPreview(sessions: List<com.example.stepforge.data.WorkoutSession>) {
    if (sessions.isEmpty()) {
        NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
            Text(
                text = stringResource(R.string.wv2_no_workouts_today),
                modifier = Modifier.padding(18.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            sessions.take(5).forEach { session -> FullSessionRow(session) }
        }
    }
}

@Composable
private fun WeeklyBarsOverviewCard(
    title: String,
    values: List<Float>,
    labels: List<String>,
    footerLeft: String,
    footerRight: String,
    blue: Boolean = false
) {
    NeonCard(modifier = Modifier.fillMaxWidth(), glow = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(title, color = MaterialTheme.colorScheme.onSurface, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
            MiniBarChart(
                values = values,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(132.dp),
                blue = blue
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.take(7).forEach { label ->
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(footerLeft, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(footerRight, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun longestActiveRun(days: List<WorkoutDaySummary>): Int {
    var longest = 0
    var current = 0
    days.forEach { day ->
        if (day.active) {
            current += 1
            if (current > longest) longest = current
        } else {
            current = 0
        }
    }
    return longest
}

@Composable
private fun sheetTitle(sheet: WorkoutSheetType): String = when (sheet) {
    WorkoutSheetType.TODAY -> stringResource(R.string.wv2_today)
    WorkoutSheetType.WEEK -> stringResource(R.string.wv2_this_week)
    WorkoutSheetType.ACTIVE_TIME -> stringResource(R.string.wv2_total_active_time)
    WorkoutSheetType.TRAINING_LOAD -> stringResource(R.string.wv2_training_load)
    WorkoutSheetType.STREAK -> stringResource(R.string.wv2_streak)
    WorkoutSheetType.CALORIES -> stringResource(R.string.wv2_calories_burned)
    WorkoutSheetType.DISTANCE -> stringResource(R.string.wv2_distance)
    WorkoutSheetType.INTENSITY -> stringResource(R.string.wv2_intensity)
    WorkoutSheetType.ZONES -> stringResource(R.string.wv2_zone_breakdown)
    WorkoutSheetType.FOCUS -> stringResource(R.string.wv2_focus_day)
    WorkoutSheetType.JOURNEY, WorkoutSheetType.DEEPER -> stringResource(R.string.wv2_journey)
}

@Composable
private fun sheetSubtitle(sheet: WorkoutSheetType): String = when (sheet) {
    WorkoutSheetType.TODAY -> stringResource(R.string.wv2_today_sheet_subtitle)
    WorkoutSheetType.WEEK -> stringResource(R.string.wv2_week_sheet_subtitle)
    WorkoutSheetType.ACTIVE_TIME -> stringResource(R.string.wv2_active_time_sheet_subtitle)
    WorkoutSheetType.TRAINING_LOAD -> stringResource(R.string.wv2_training_load_sheet_subtitle)
    WorkoutSheetType.STREAK -> stringResource(R.string.wv2_streak_sheet_subtitle)
    WorkoutSheetType.CALORIES -> stringResource(R.string.wv2_calories_sheet_subtitle)
    WorkoutSheetType.DISTANCE -> stringResource(R.string.wv2_distance_sheet_subtitle)
    WorkoutSheetType.INTENSITY -> stringResource(R.string.wv2_intensity_sheet_subtitle)
    WorkoutSheetType.ZONES -> stringResource(R.string.wv2_zones_sheet_subtitle)
    WorkoutSheetType.FOCUS -> stringResource(R.string.wv2_focus_sheet_subtitle)
    WorkoutSheetType.JOURNEY, WorkoutSheetType.DEEPER -> stringResource(R.string.wv2_journey_sheet_subtitle)
}
