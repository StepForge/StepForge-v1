package com.example.stepforge.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.DirectionsBike
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.math.roundToInt

@Composable
internal fun WorkoutMetricTabChartCard(
    state: WorkoutsDashboardState,
    selectedMetric: WorkoutChartMetric,
    onMetricSelected: (WorkoutChartMetric) -> Unit,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val values = when (selectedMetric) {
        WorkoutChartMetric.DURATION -> state.week.map { it.durationMinutes.toFloat() }
        WorkoutChartMetric.CALORIES -> state.week.map { it.calories.toFloat() }
        WorkoutChartMetric.INTENSITY -> state.week.map { it.intensityScore.toFloat() }
    }
    val todayIndex = state.week.indexOfFirst { it.date == LocalDate.now() }.let { if (it >= 0) it else 0 }
    var selectedIndex by remember(selectedMetric, state.week) { mutableIntStateOf(todayIndex) }
    val safeSelectedIndex = selectedIndex.coerceIn(0, values.lastIndex.coerceAtLeast(0))
    val tooltip = when (selectedMetric) {
        WorkoutChartMetric.DURATION -> stringResource(R.string.wv2_minutes_format, values.getOrElse(safeSelectedIndex) { 0f }.roundToInt())
        WorkoutChartMetric.CALORIES -> stringResource(R.string.wv2_kcal_format, values.getOrElse(safeSelectedIndex) { 0f }.roundToInt())
        WorkoutChartMetric.INTENSITY -> stringResource(R.string.wv2_score_format, values.getOrElse(safeSelectedIndex) { 0f }.roundToInt())
    }

    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(332.dp),
        onClick = onClick,
        glow = true
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            MetricTabs(
                selected = selectedMetric,
                onSelected = onMetricSelected
            )

            Text(
                text = when (selectedMetric) {
                    WorkoutChartMetric.DURATION -> stringResource(R.string.wv2_active_time_min)
                    WorkoutChartMetric.CALORIES -> stringResource(R.string.wv2_calories_kcal)
                    WorkoutChartMetric.INTENSITY -> stringResource(R.string.wv2_intensity_score)
                },
                color = cs.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold
            )

            AnimatedLineAreaChart(
                values = values,
                days = state.week.map { it.date.localizedDayShort() },
                selectedIndex = safeSelectedIndex,
                todayIndex = todayIndex,
                tooltip = tooltip,
                onIndexSelected = { selectedIndex = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            )
        }
    }
}

@Composable
private fun MetricTabs(
    selected: WorkoutChartMetric,
    onSelected: (WorkoutChartMetric) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(42.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(cs.onSurface.copy(alpha = if (cs.background.luminance() < 0.5f) 0.055f else 0.045f))
            .border(1.dp, cs.outline.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        WorkoutChartMetric.entries.forEach { metric ->
            val active = selected == metric
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (active) {
                            Brush.horizontalGradient(
                                listOf(
                                    cs.primary.copy(alpha = 0.28f),
                                    Color(0xFF00F5FF).copy(alpha = 0.16f)
                                )
                            )
                        } else {
                            Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                        }
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSelected(metric) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (metric) {
                        WorkoutChartMetric.DURATION -> stringResource(R.string.wv2_duration)
                        WorkoutChartMetric.CALORIES -> stringResource(R.string.wv2_calories)
                        WorkoutChartMetric.INTENSITY -> stringResource(R.string.wv2_intensity)
                    },
                    color = if (active) cs.primary else cs.onSurfaceVariant,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
internal fun WorkoutSummaryMetricCard(
    state: WorkoutsDashboardState,
    onActiveTimeClick: () -> Unit,
    onTrainingLoadClick: () -> Unit
) {
    val durationPercent = (state.weekDurationMinutes / 240f).coerceIn(0f, 1f)
    val readiness = state.consistencyScore.coerceIn(0, 100)
    NeonCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(96.dp),
        glow = true
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SummaryBlock(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Timer,
                progress = durationPercent,
                title = stringResource(R.string.wv2_total_active_time),
                value = state.weekDurationMinutes.formatDurationLocalized(),
                note = stringResource(
                    R.string.wv2_vs_last_week_format,
                    signedPercentText(state.weekDurationMinutes.comparePercent(state.previousWeekDurationMinutes))
                ),
                onClick = onActiveTimeClick
            )

            Box(
                modifier = Modifier
                    .height(58.dp)
                    .width(1.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
            )

            SummaryBlock(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Speed,
                progress = readiness / 100f,
                title = stringResource(R.string.wv2_training_load),
                value = stringResource(R.string.wv2_score_number_format, readiness),
                note = when {
                    readiness >= 80 -> stringResource(R.string.wv2_ready_to_push)
                    readiness >= 45 -> stringResource(R.string.wv2_balanced_load)
                    readiness > 0 -> stringResource(R.string.wv2_building_load)
                    else -> stringResource(R.string.wv2_no_data_short)
                },
                onClick = onTrainingLoadClick
            )
        }
    }
}

@Composable
private fun SummaryBlock(
    modifier: Modifier,
    icon: ImageVector,
    progress: Float,
    title: String,
    value: String,
    note: String,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ProgressRing(
            progress = progress,
            size = 48.dp,
            stroke = 5.5.dp,
            center = {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            }
        )
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = title,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.5.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = note,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 8.5.sp,
                lineHeight = 10.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun TodayWorkoutSummaryCard(
    state: WorkoutsDashboardState,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        glow = false
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProgressRing(
                    progress = state.workoutScore / 100f,
                    size = 58.dp,
                    stroke = 5.5.dp,
                    center = {
                        ProgressRing(
                            progress = (state.today.durationMinutes / 60f).coerceIn(0f, 1f),
                            size = 43.dp,
                            stroke = 4.dp,
                            center = {}
                        )
                    }
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.wv2_today),
                        color = cs.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = todayNote(state),
                        color = cs.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                SmallPillButton(
                    text = stringResource(R.string.wv2_view_details),
                    onClick = onClick
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniMetric(icon = Icons.Outlined.DirectionsWalk, value = NumberFormat.getIntegerInstance().format(state.today.steps), label = stringResource(R.string.wv2_steps))
                MetricDivider()
                MiniMetric(icon = Icons.Outlined.LocationOn, value = state.today.distanceKm.formatKm(), label = stringResource(R.string.wv2_km))
                MetricDivider()
                MiniMetric(icon = Icons.Outlined.LocalFireDepartment, value = NumberFormat.getIntegerInstance().format(state.today.calories), label = stringResource(R.string.wv2_kcal_unit), accent = Color(0xFFFFB02E))
                MetricDivider()
                MiniMetric(icon = Icons.Outlined.Timer, value = state.today.durationMinutes.formatDurationLocalized(), label = stringResource(R.string.wv2_min_unit))
            }
        }
    }
}

@Composable
private fun todayNote(state: WorkoutsDashboardState): String {
    return when {
        state.today.sessions.isEmpty() -> stringResource(R.string.wv2_today_empty_note)
        state.today.durationMinutes >= 45 -> stringResource(R.string.wv2_today_strong_note)
        state.today.durationMinutes >= 15 -> stringResource(R.string.wv2_today_good_note)
        else -> stringResource(R.string.wv2_today_start_note)
    }
}

@Composable
internal fun ThisWeekHeatmapCard(
    state: WorkoutsDashboardState,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        glow = false
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wv2_this_week),
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface,
                        fontSize = 17.sp
                    )
                    HeatmapLegend()
                }
                Text(
                    text = stringResource(R.string.wv2_active_days_format, state.activeDays),
                    color = cs.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }

            HeatmapDots(
                week = state.week,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun HeatmapLegend() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(
            text = stringResource(R.string.wv2_less),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        repeat(3) { i ->
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f + i * 0.18f))
            )
        }
        Text(
            text = stringResource(R.string.wv2_more),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
internal fun WorkoutBentoGrid(
    state: WorkoutsDashboardState,
    onOpenSheet: (WorkoutSheetType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkoutStreakCard(
                state = state,
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(WorkoutSheetType.STREAK) }
            )
            WorkoutCaloriesCard(
                state = state,
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(WorkoutSheetType.CALORIES) }
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WorkoutDistanceCard(
                state = state,
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(WorkoutSheetType.DISTANCE) }
            )
            WorkoutIntensityCard(
                state = state,
                modifier = Modifier.weight(1f),
                onClick = { onOpenSheet(WorkoutSheetType.INTENSITY) }
            )
        }

        WorkoutZoneBreakdownCard(
            state = state,
            onClick = { onOpenSheet(WorkoutSheetType.ZONES) }
        )
    }
}

@Composable
private fun WorkoutStreakCard(
    state: WorkoutsDashboardState,
    modifier: Modifier,
    onClick: () -> Unit
) {
    BentoSmallCard(
        modifier = modifier.height(132.dp),
        title = stringResource(R.string.wv2_streak),
        icon = Icons.Outlined.LocalFireDepartment,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(
                text = state.activeDays.toString(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Text(
                text = stringResource(R.string.wv2_workouts_count_word),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        Spacer(Modifier.height(4.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            state.week.forEach { day ->
                DayMiniDot(active = day.active, label = day.date.localizedDayShort())
            }
        }
    }
}

@Composable
private fun WorkoutCaloriesCard(
    state: WorkoutsDashboardState,
    modifier: Modifier,
    onClick: () -> Unit
) {
    BentoSmallCard(
        modifier = modifier.height(132.dp),
        title = stringResource(R.string.wv2_calories_burned),
        icon = Icons.Outlined.LocalFireDepartment,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = NumberFormat.getIntegerInstance().format(state.weekCalories),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )
                Text(
                    text = stringResource(R.string.wv2_kcal_unit),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            MiniBarChart(
                values = state.week.map { it.calories.toFloat() },
                modifier = Modifier
                    .width(68.dp)
                    .height(68.dp)
                    .padding(top = 4.dp),
                dayMode = true
            )
        }
    }
}

@Composable
private fun WorkoutDistanceCard(
    state: WorkoutsDashboardState,
    modifier: Modifier,
    onClick: () -> Unit
) {
    BentoSmallCard(
        modifier = modifier.height(132.dp),
        title = stringResource(R.string.wv2_distance),
        icon = Icons.Outlined.LocationOn,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.width(68.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = state.today.distanceKm.formatKm(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false
                )
                Text(
                    text = stringResource(R.string.wv2_km),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
            Spacer(Modifier.weight(1f))
            MiniRoutePath(
                modifier = Modifier
                    .width(58.dp)
                    .height(46.dp)
            )
        }
    }
}

@Composable
private fun WorkoutIntensityCard(
    state: WorkoutsDashboardState,
    modifier: Modifier,
    onClick: () -> Unit
) {
    BentoSmallCard(
        modifier = modifier.height(132.dp),
        title = stringResource(R.string.wv2_intensity),
        icon = Icons.Outlined.FlashOn,
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = intensityLabel(state.intensityScore),
                    color = if (state.intensityScore >= 70) Color(0xFF48A7FF) else MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = intensityNote(state.intensityScore),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 8.sp,
                    lineHeight = 10.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            MiniBarChart(
                values = state.week.map { it.intensityScore.toFloat() },
                modifier = Modifier
                    .width(68.dp)
                    .height(68.dp)
                    .padding(top = 4.dp),
                blue = true,
                dayMode = true
            )
        }
    }
}

@Composable
internal fun intensityLabel(score: Int): String = when {
    score >= 72 -> stringResource(R.string.wv2_intensity_high)
    score >= 45 -> stringResource(R.string.wv2_intensity_moderate)
    score > 0 -> stringResource(R.string.wv2_intensity_low)
    else -> stringResource(R.string.wv2_no_data_short)
}

@Composable
private fun intensityNote(score: Int): String = when {
    score >= 72 -> stringResource(R.string.wv2_strong_load)
    score >= 45 -> stringResource(R.string.wv2_balanced_load)
    score > 0 -> stringResource(R.string.wv2_building_load)
    else -> stringResource(R.string.wv2_no_data_short)
}

@Composable
private fun WorkoutZoneBreakdownCard(
    state: WorkoutsDashboardState,
    onClick: () -> Unit
) {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        glow = false
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.wv2_zone_breakdown),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 0.4.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DonutChart(
                    slices = state.zoneSlices,
                    modifier = Modifier.size(112.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    state.zoneSlices.forEach { slice ->
                        ZoneLegendRow(slice)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoneLegendRow(slice: WorkoutZoneSlice) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(9.dp)
                .clip(CircleShape)
                .background(zoneColor(slice.labelType))
        )
        Text(
            text = stringResource(
                R.string.wv2_percent_label_format,
                slice.percent,
                zoneLabel(slice.labelType)
            ),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun FocusDayCard(
    state: WorkoutsDashboardState,
    onClick: () -> Unit
) {
    val focusKinds = listOf(WorkoutKind.WALK, WorkoutKind.RUN, WorkoutKind.CYCLING)
    val sessionsByKind = state.todaySessions
        .filter { it.durationMinutes >= 5 }
        .associateBy { normalizeFocusKind(it.inferWorkoutKind()) }

    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        glow = false
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        Icons.Outlined.BarChart,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.wv2_focus_day),
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 17.sp
                    )
                    Text(
                        text = stringResource(R.string.wv2_today_focus),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = stringResource(R.string.wv2_planned_format, 3),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                focusKinds.forEach { kind ->
                    FocusMiniCard(
                        session = sessionsByKind[kind],
                        kind = kind,
                        completed = sessionsByKind[kind] != null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

private fun normalizeFocusKind(kind: WorkoutKind): WorkoutKind {
    return when (kind) {
        WorkoutKind.STRENGTH -> WorkoutKind.CYCLING
        else -> kind
    }
}

private fun defaultTitleType(kind: WorkoutKind): WorkoutTitleType {
    return when (kind) {
        WorkoutKind.WALK -> WorkoutTitleType.MORNING_WALK
        WorkoutKind.RUN -> WorkoutTitleType.OUTDOOR_RUN
        WorkoutKind.CYCLING -> WorkoutTitleType.CYCLING
        WorkoutKind.STRENGTH -> WorkoutTitleType.FULL_BODY_STRENGTH
        WorkoutKind.GENERIC -> WorkoutTitleType.WORKOUT
    }
}

private fun plannedDurationForKind(kind: WorkoutKind): Int {
    return when (kind) {
        WorkoutKind.WALK -> 30
        WorkoutKind.RUN -> 45
        WorkoutKind.CYCLING -> 35
        WorkoutKind.STRENGTH -> 25
        WorkoutKind.GENERIC -> 30
    }
}

@Composable
private fun focusEffortLabel(kind: WorkoutKind): String {
    return when (kind) {
        WorkoutKind.WALK -> stringResource(R.string.wv2_intensity_low)
        WorkoutKind.RUN, WorkoutKind.CYCLING -> stringResource(R.string.wv2_intensity_moderate)
        WorkoutKind.STRENGTH -> stringResource(R.string.wv2_focus_day)
        WorkoutKind.GENERIC -> stringResource(R.string.wv2_intensity_moderate)
    }
}

@Composable
private fun FocusMiniCard(
    session: com.example.stepforge.data.WorkoutSession?,
    kind: WorkoutKind,
    completed: Boolean,
    modifier: Modifier
) {
    val durationMinutes = session?.durationMinutes?.coerceAtLeast(0) ?: plannedDurationForKind(kind)
    val accent = when (kind) {
        WorkoutKind.CYCLING -> Color(0xFF19E0B2)
        WorkoutKind.RUN -> Color(0xFF1FE6FF)
        WorkoutKind.WALK -> MaterialTheme.colorScheme.primary
        WorkoutKind.STRENGTH -> Color(0xFF00D084)
        WorkoutKind.GENERIC -> MaterialTheme.colorScheme.primary
    }
    val title = when (kind) {
        WorkoutKind.WALK -> stringResource(R.string.wv2_walk)
        WorkoutKind.RUN -> stringResource(R.string.wv2_run)
        WorkoutKind.CYCLING -> stringResource(R.string.wv2_cycling)
        else -> titleForKind(defaultTitleType(kind))
    }
    val subtitle = if (session != null) {
        durationMinutes.formatDurationLocalized()
    } else {
        stringResource(R.string.wv2_open_slot)
    }

    Column(
        modifier = modifier
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.032f))
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            lineHeight = 12.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = subtitle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(if (completed) accent else Color.Transparent)
                    .border(
                        1.4.dp,
                        if (completed) Color.Transparent else accent.copy(alpha = 0.62f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (completed) {
                    Text(
                        text = "✓",
                        color = Color.Black,
                        fontSize = 9.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 1.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkoutJourneyCard(
    state: WorkoutsDashboardState,
    onClick: () -> Unit
) {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        glow = false
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.wv2_journey),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp
                )
                Row(
                    modifier = Modifier.clickable { onClick() },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wv2_see_all),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Icon(
                        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }

            if (state.journeyItems.isEmpty()) {
                EmptyMiniMessage(
                    title = stringResource(R.string.wv2_no_sessions),
                    subtitle = stringResource(R.string.wv2_journey_empty)
                )
            } else {
                JourneyTimeline(
                    items = state.journeyItems.take(3)
                )
            }
        }
    }
}

@Composable
private fun JourneyTimeline(items: List<WorkoutJourneyItem>) {
    val today = LocalDate.now()
    val lineColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.70f)

    Box(modifier = Modifier.fillMaxWidth()) {
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .matchParentSize()
                .padding(start = 8.dp, top = 8.dp, bottom = 8.dp)
        ) {
            val x = 9.dp.toPx()
            drawLine(
                color = lineColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, size.height),
                strokeWidth = 2.6.dp.toPx()
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            var lastGroup: DayGroupLabel? = null
            items.forEachIndexed { index, item ->
                val group = item.date.dayGroupLabel(today)
                if (group != lastGroup) {
                    JourneyGroupHeader(
                        group = group,
                        date = item.date,
                        isFirst = index == 0
                    )
                    lastGroup = group
                }
                JourneyRow(
                    item = item,
                    isLast = index == items.lastIndex
                )
            }
        }
    }
}

@Composable
private fun JourneyGroupHeader(
    group: DayGroupLabel,
    date: LocalDate,
    isFirst: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(40.dp))
        Text(
            text = when (group) {
                DayGroupLabel.TODAY -> stringResource(R.string.wv2_today)
                DayGroupLabel.YESTERDAY -> stringResource(R.string.wv2_yesterday)
                DayGroupLabel.OLDER -> date.toString()
            },
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun JourneyRow(
    item: WorkoutJourneyItem,
    isLast: Boolean
) {
    val accent = when (item.kind) {
        WorkoutKind.CYCLING -> Color(0xFF19E0B2)
        WorkoutKind.RUN -> Color(0xFF00E9FF)
        WorkoutKind.WALK -> Color(0xFF22F4FF)
        WorkoutKind.STRENGTH -> Color(0xFF00D084)
        WorkoutKind.GENERIC -> MaterialTheme.colorScheme.primary
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(18.dp)
                .height(62.dp),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val x = size.width / 2f
                val dotY = size.height / 2f
                drawCircle(
                    color = accent.copy(alpha = 0.18f),
                    radius = 8.2.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, dotY)
                )
                drawCircle(
                    color = accent,
                    radius = 5.4.dp.toPx(),
                    center = androidx.compose.ui.geometry.Offset(x, dotY)
                )
            }
        }

        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF0F5670).copy(alpha = 0.95f),
                            accent.copy(alpha = 0.22f)
                        )
                    )
                )
                .border(1.1.dp, accent.copy(alpha = 0.62f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(item.kind.icon(), null, tint = accent, modifier = Modifier.size(18.dp))
        }

        Column(
            modifier = Modifier.width(132.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = titleForKind(item.titleType),
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.secondaryText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (item.kind == WorkoutKind.CYCLING) {
            Spacer(modifier = Modifier.width(70.dp))
        } else if (item.kind == WorkoutKind.STRENGTH) {
            MiniBarChart(
                values = item.chartValues,
                modifier = Modifier
                    .width(78.dp)
                    .height(24.dp),
                dayMode = false
            )
        } else {
            MiniWaveGraph(
                values = item.chartValues,
                kind = item.kind,
                modifier = Modifier
                    .width(82.dp)
                    .height(24.dp)
            )
        }

        Text(
            text = item.primaryMetric,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.End,
            maxLines = 1,
            modifier = Modifier.width(56.dp)
        )

        Icon(
            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(15.dp)
        )
    }
}

@Composable
internal fun DiveDeeperCard(onClick: () -> Unit) {
    NeonCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        glow = false
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ProgressRing(
                progress = 0.72f,
                size = 54.dp,
                stroke = 6.dp,
                center = { Icon(Icons.Outlined.History, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.wv2_dive_deeper),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.wv2_dive_deeper_body),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun BentoSmallCard(
    modifier: Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    NeonCard(
        modifier = modifier,
        onClick = onClick,
        glow = false
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 13.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Text(
                    text = title.uppercase(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.35.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
            }
            content()
        }
    }
}

@Composable
private fun DayMiniDot(active: Boolean, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape)
                .background(if (active) MaterialTheme.colorScheme.primary else Color.Transparent)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = if (active) 0f else 0.28f), CircleShape)
        )
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false
        )
    }
}

@Composable
private fun MiniMetric(icon: ImageVector, value: String, label: String, accent: Color? = null) {
    val tint = accent ?: MaterialTheme.colorScheme.primary
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun MetricDivider() {
    Box(
        modifier = Modifier
            .height(30.dp)
            .width(1.dp)
            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.14f))
    )
}

@Composable
private fun SmallPillButton(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.13f))
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 10.sp,
            lineHeight = 12.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(12.dp))
    }
}

@Composable
private fun SectionHeader(title: String, action: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
        Row(
            modifier = Modifier.clickable { onClick() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(action, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun EmptyMiniMessage(title: String, subtitle: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.035f))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.ExtraBold)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
    }
}

@Composable
internal fun zoneLabel(type: ZoneLabelType): String = when (type) {
    ZoneLabelType.PEAK -> stringResource(R.string.wv2_zone_peak)
    ZoneLabelType.CARDIO -> stringResource(R.string.wv2_zone_cardio)
    ZoneLabelType.FAT_BURN -> stringResource(R.string.wv2_zone_fat_burn)
    ZoneLabelType.WARM_UP -> stringResource(R.string.wv2_zone_warm_up)
}

@Composable
private fun titleForKind(type: WorkoutTitleType): String = when (type) {
    WorkoutTitleType.OUTDOOR_RUN -> stringResource(R.string.wv2_outdoor_run)
    WorkoutTitleType.MORNING_WALK -> stringResource(R.string.wv2_walk)
    WorkoutTitleType.FULL_BODY_STRENGTH -> stringResource(R.string.wv2_strength)
    WorkoutTitleType.CYCLING -> stringResource(R.string.wv2_cycling)
    WorkoutTitleType.WORKOUT -> stringResource(R.string.wv2_workout)
}

private fun WorkoutKind.icon(): ImageVector = when (this) {
    WorkoutKind.WALK -> Icons.Outlined.DirectionsWalk
    WorkoutKind.RUN -> Icons.Outlined.DirectionsRun
    WorkoutKind.STRENGTH -> Icons.Outlined.FitnessCenter
    WorkoutKind.CYCLING -> Icons.Outlined.DirectionsBike
    WorkoutKind.GENERIC -> Icons.Outlined.Timer
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
