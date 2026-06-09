package com.example.stepforge.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import java.time.LocalDate

@Composable
internal fun HistoryMountainHeroCard(
    state: HistoryUiState
) {
    PremiumHistoryCard(glow = false) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(202.dp)
        ) {
            WeatherMountainCanvas(
                modifier = Modifier.matchParentSize(),
                mood = state.weatherMood,
                metric = state.selectedMetric
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 18.dp, end = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.40f))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 9.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    Text(
                        text = state.heroMetricDisplay(),
                        color = Color.White,
                        fontSize = 21.sp,
                        lineHeight = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = metricHeroUnit(state.selectedMetric),
                        color = Color.White.copy(alpha = 0.92f),
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                }
            }
        }
    }
}

@Composable
private fun HistoryUiState.heroMetricDisplay(): String {
    return when (selectedMetric) {
        HistoryMetric.STEPS -> visibleDays.sumOf { it.steps }.toFloat().formatHeroCompactCount()
        HistoryMetric.DISTANCE -> visibleDays.sumOf { it.distanceKm.toDouble() }.toFloat().formatHistoryKm()
        HistoryMetric.CALORIES -> visibleDays.sumOf { it.calories }.toFloat().formatHeroCompactCount()
        HistoryMetric.ACTIVE_TIME -> visibleDays.sumOf { it.activeMinutes }.historyDurationText()
    }
}

private fun Float.formatHeroCompactCount(): String {
    val safe = coerceAtLeast(0f)
    return when {
        safe >= 1_000_000f -> {
            val value = safe / 1_000_000f
            if (value >= 10f || value % 1f < 0.05f) "${value.toInt()}M" else String.format(java.util.Locale.getDefault(), "%.1fM", value)
        }
        safe >= 1_000f -> {
            val value = safe / 1_000f
            if (value >= 100f || value % 1f < 0.05f) "${value.toInt()}K" else String.format(java.util.Locale.getDefault(), "%.1fK", value)
        }
        else -> safe.toInt().formatHistoryNumber()
    }
}

@Composable
private fun HistoryRangeTabs(
    selected: HistoryRange,
    onSelected: (HistoryRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        listOf(
            HistoryRange.WEEK to stringResource(R.string.h3_range_7d),
            HistoryRange.MONTH to stringResource(R.string.h3_range_30d),
            HistoryRange.YEAR to stringResource(R.string.h3_range_1y),
            HistoryRange.ALL to stringResource(R.string.h3_range_all)
        ).forEach { (range, label) ->
            val isSelected = selected == range
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(34.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surface.copy(alpha = 0.44f)
                    )
                    .border(
                        1.dp,
                        if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.72f)
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        RoundedCornerShape(999.dp)
                    )
                    .clickable { onSelected(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
internal fun HistoryThisWeekCard(
    state: HistoryUiState,
    onRangeSelected: (HistoryRange) -> Unit,
    onDateSelected: (LocalDate) -> Unit
) {
    val points = state.rangeChartPoints()
    val selectedDate = state.selectedDay.localDate
    val axisMax = state.rangeAxisMax(points)
    PremiumHistoryCard(glow = false) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = state.rangeTitle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HistoryRangeTabs(
                selected = state.selectedRange,
                onSelected = onRangeSelected,
                modifier = Modifier.fillMaxWidth()
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = state.rangeMetricTotal(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 34.sp,
                    lineHeight = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = state.metricSummaryTitle(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 13.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            HistoryRangeBarChart(
                points = points,
                metric = state.selectedMetric,
                selectedDate = selectedDate,
                axisMax = axisMax,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(178.dp),
                onBarSelected = onDateSelected
            )
        }
    }
}

@Composable
private fun HistoryUiState.rangeChartPoints(): List<HistoryRangeChartPoint> {
    val today = LocalDate.now()
    val map = allDays.associateBy { it.localDate }
    return when (selectedRange) {
        HistoryRange.WEEK -> (6 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val day = map[date] ?: emptyHistoryDay(date, selectedDay)
            HistoryRangeChartPoint(
                date = date,
                label = historyWeekdayLabel(date),
                value = day.metricValue(selectedMetric),
                valueText = day.metricDisplayBubble(selectedMetric)
            )
        }
        HistoryRange.MONTH, HistoryRange.THREE_MONTHS -> (29 downTo 0).mapIndexed { index, offset ->
            val date = today.minusDays(offset.toLong())
            val day = map[date] ?: emptyHistoryDay(date, selectedDay)
            val periodLabel = when (index) {
                0 -> "0"
                5 -> "5"
                10 -> "10"
                15 -> "15"
                20 -> "20"
                25 -> "25"
                29 -> "30"
                else -> ""
            }
            HistoryRangeChartPoint(
                date = date,
                label = periodLabel,
                value = day.metricValue(selectedMetric),
                valueText = day.metricDisplayBubble(selectedMetric),
                showLabel = periodLabel.isNotBlank()
            )
        }
        HistoryRange.YEAR, HistoryRange.ALL -> (11 downTo 0).mapIndexed { index, offset ->
            val monthStart = today.withDayOfMonth(1).minusMonths(offset.toLong())
            val monthDays = allDays.filter { it.localDate.year == monthStart.year && it.localDate.monthValue == monthStart.monthValue }
            val monthValue = monthDays.sumOf { it.metricValue(selectedMetric).toDouble() }.toFloat()
            HistoryRangeChartPoint(
                date = monthStart,
                label = (index + 1).toString(),
                value = monthValue,
                valueText = monthValue.metricValueTextBubble(selectedMetric)
            )
        }
    }
}

private fun HistoryUiState.rangeAxisMax(points: List<HistoryRangeChartPoint>): Float {
    val maxValue = points.maxOfOrNull { it.value } ?: 0f
    return when (selectedMetric) {
        HistoryMetric.STEPS -> when (selectedRange) {
            HistoryRange.WEEK -> dynamicAxisMax(minimum = 15_000f, actualMax = maxValue)
            HistoryRange.MONTH, HistoryRange.THREE_MONTHS -> dynamicAxisMax(minimum = 20_000f, actualMax = maxValue)
            HistoryRange.YEAR, HistoryRange.ALL -> dynamicAxisMax(minimum = 200_000f, actualMax = maxValue)
        }
        HistoryMetric.DISTANCE -> dynamicAxisMax(minimum = 10f, actualMax = maxValue)
        HistoryMetric.CALORIES -> dynamicAxisMax(minimum = 600f, actualMax = maxValue)
        HistoryMetric.ACTIVE_TIME -> dynamicAxisMax(minimum = 90f, actualMax = maxValue)
    }
}

private fun dynamicAxisMax(minimum: Float, actualMax: Float): Float {
    val safeMax = actualMax.coerceAtLeast(0f)
    if (safeMax <= minimum) return minimum
    return niceAxis(safeMax * 1.15f)
}

private fun niceAxis(value: Float): Float {
    if (value <= 0f) return 1f
    return when {
        value <= 10f -> 10f
        value <= 30f -> 30f
        value <= 60f -> 60f
        value <= 100f -> 100f
        value <= 300f -> 300f
        value <= 600f -> 600f
        value <= 1_000f -> 1_000f
        value <= 5_000f -> 5_000f
        value <= 10_000f -> 10_000f
        value <= 15_000f -> 15_000f
        value <= 20_000f -> 20_000f
        value <= 30_000f -> 30_000f
        value <= 50_000f -> 50_000f
        value <= 100_000f -> 100_000f
        value <= 150_000f -> 150_000f
        value <= 200_000f -> 200_000f
        value <= 250_000f -> 250_000f
        value <= 500_000f -> 500_000f
        value <= 1_000_000f -> 1_000_000f
        else -> kotlin.math.ceil(value / 500_000f) * 500_000f
    }
}

private fun emptyHistoryDay(date: LocalDate, template: HistoryDayUi): HistoryDayUi = template.copy(
    date = date.toString(),
    localDate = date,
    steps = 0,
    distanceKm = 0f,
    calories = 0,
    activeMinutes = 0,
    waterMl = 0,
    sleepMinutes = 0,
    workoutSessions = 0
)

@Composable
private fun HistoryDayUi.metricDisplayBubble(metric: HistoryMetric): String = when (metric) {
    HistoryMetric.STEPS -> steps.formatHistoryNumber()
    HistoryMetric.DISTANCE -> distanceKm.formatHistoryKm()
    HistoryMetric.CALORIES -> calories.formatHistoryNumber()
    HistoryMetric.ACTIVE_TIME -> activeMinutes.historyDurationText()
}

@Composable
private fun Float.metricValueTextBubble(metric: HistoryMetric): String = when (metric) {
    HistoryMetric.STEPS, HistoryMetric.CALORIES -> toInt().formatHistoryNumber()
    HistoryMetric.ACTIVE_TIME -> toInt().historyDurationText()
    HistoryMetric.DISTANCE -> formatHistoryKm()
}

@Composable
private fun historyWeekdayLabel(date: LocalDate): String = when (date.dayOfWeek) {
    java.time.DayOfWeek.MONDAY -> stringResource(R.string.h3_mon)
    java.time.DayOfWeek.TUESDAY -> stringResource(R.string.h3_tue)
    java.time.DayOfWeek.WEDNESDAY -> stringResource(R.string.h3_wed)
    java.time.DayOfWeek.THURSDAY -> stringResource(R.string.h3_thu)
    java.time.DayOfWeek.FRIDAY -> stringResource(R.string.h3_fri)
    java.time.DayOfWeek.SATURDAY -> stringResource(R.string.h3_sat)
    java.time.DayOfWeek.SUNDAY -> stringResource(R.string.h3_sun)
}

private fun LocalDate.historyMonthLabel(): String =
    java.time.format.DateTimeFormatter.ofPattern("MMM", java.util.Locale.getDefault()).format(this)

@Composable
private fun HistoryUiState.rangeTitle(): String = when (selectedRange) {
    HistoryRange.WEEK -> stringResource(R.string.h3_this_week)
    HistoryRange.MONTH, HistoryRange.THREE_MONTHS -> stringResource(R.string.h3_this_month)
    HistoryRange.YEAR -> stringResource(R.string.h3_this_year)
    HistoryRange.ALL -> stringResource(R.string.h3_all_time)
}

@Composable
internal fun HistoryOverviewCard(state: HistoryUiState) {
    PremiumHistoryCard(glow = false) {
        Column(
            modifier = Modifier.padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.h3_overview_range_format, state.rangeTitle()),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                fontWeight = FontWeight.ExtraBold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OverviewMetricTile(
                    icon = Icons.Outlined.DirectionsWalk,
                    label = stringResource(R.string.h3_total_steps),
                    value = state.totalSteps.formatHistoryNumber(),
                    color = Color(0xFFFF8A24),
                    modifier = Modifier.weight(1f)
                )
                OverviewMetricTile(
                    icon = Icons.Outlined.EmojiEvents,
                    label = stringResource(R.string.h3_avg_steps),
                    value = state.averageSteps.formatHistoryNumber(),
                    color = Color(0xFF9B5CFF),
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OverviewMetricTile(
                    icon = Icons.Outlined.LocationOn,
                    label = stringResource(R.string.h3_total_distance),
                    value = "${state.totalDistanceKm.formatHistoryKm()} ${stringResource(R.string.h3_km)}",
                    color = Color(0xFF229DFF),
                    modifier = Modifier.weight(1f)
                )
                OverviewMetricTile(
                    icon = Icons.Outlined.LocalFireDepartment,
                    label = stringResource(R.string.h3_total_calories),
                    value = "${state.totalCalories.formatHistoryNumber()} ${stringResource(R.string.h3_kcal)}",
                    color = Color(0xFFFF5A34),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun OverviewMetricTile(
    icon: ImageVector,
    label: String,
    value: String,
    color: Color,
    modifier: Modifier
) {
    Row(
        modifier = modifier
            .heightIn(min = 76.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.09f))
            .border(1.dp, color.copy(alpha = 0.17f), RoundedCornerShape(18.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(21.dp))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp, lineHeight = 11.sp, maxLines = 2, overflow = TextOverflow.Clip)
            Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Clip)
        }
    }
}

@Composable
internal fun HistoryCalendarCard(
    state: HistoryUiState,
    onDateSelected: (LocalDate) -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    PremiumHistoryCard(glow = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.h3_calendar_view),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { onPreviousMonth() }
                        .padding(6.dp)
                )
                Text(
                    text = state.visibleMonth.atDay(1).monthTitle(),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    modifier = Modifier.width(126.dp),
                    textAlign = TextAlign.Center
                )
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .clickable { onNextMonth() }
                        .padding(6.dp)
                )
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf(
                    stringResource(R.string.h3_mon),
                    stringResource(R.string.h3_tue),
                    stringResource(R.string.h3_wed),
                    stringResource(R.string.h3_thu),
                    stringResource(R.string.h3_fri),
                    stringResource(R.string.h3_sat),
                    stringResource(R.string.h3_sun)
                ).forEach { label ->
                    Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.80f), fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
                }
            }
            state.calendarDays.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { calendarDay ->
                        HistoryCalendarCell(
                            item = calendarDay,
                            selected = calendarDay.date == state.selectedDay.localDate,
                            metric = state.selectedMetric,
                            onClick = { onDateSelected(calendarDay.date) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(stringResource(R.string.h3_low), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                Spacer(Modifier.width(8.dp))
                listOf(Color(0xFF37C66D), Color(0xFF7ED957), Color(0xFFFFC13D), Color(0xFFFF6A38), Color(0xFFFF4FC3)).forEach { color ->
                    Box(Modifier.size(8.dp).clip(CircleShape).background(color))
                    Spacer(Modifier.width(5.dp))
                }
                Text(stringResource(R.string.h3_high), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun HistoryCalendarCell(
    item: HistoryCalendarDayUi,
    selected: Boolean,
    metric: HistoryMetric,
    onClick: () -> Unit,
    modifier: Modifier
) {
    val day = item.day
    val progress = day?.goalProgress(metric)?.coerceIn(0f, 1f) ?: 0f
    val accent = progress.calendarColor(metric)
    Box(
        modifier = modifier
            .height(39.dp)
            .clip(CircleShape)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (day != null) {
            HistoryCalendarRing(progress = progress, color = accent, selected = selected, modifier = Modifier.size(if (selected) 35.dp else 31.dp))
        } else if (selected) {
            Box(
                Modifier
                    .size(31.dp)
                    .clip(CircleShape)
                    .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f), CircleShape)
            )
        }
        Text(
            text = item.date.dayOfMonth.toString(),
            color = when {
                selected -> Color.White
                !item.inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.36f)
                day != null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.90f)
                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            },
            fontSize = 12.sp,
            fontWeight = if (selected || day != null) FontWeight.ExtraBold else FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun HistoryTrendCard(
    state: HistoryUiState,
    onDateSelected: (LocalDate) -> Unit
) {
    val points = state.rangeChartPoints()
    val axisMax = state.rangeAxisMax(points)
    PremiumHistoryCard(glow = false) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.h3_trend_title, metricLabelForCards(state.selectedMetric)),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp,
                lineHeight = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            HistoryTrendCurveChart(
                points = points,
                metric = state.selectedMetric,
                selectedDate = state.selectedDay.localDate,
                axisMax = axisMax,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(162.dp),
                onPointSelected = onDateSelected
            )
        }
    }
}

@Composable
internal fun HistoryTopDaysCard(
    state: HistoryUiState,
    onOpenDetails: (HistoryDayUi) -> Unit,
    onViewAll: () -> Unit
) {
    val items = state.topDays.take(3)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.h3_top_days),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = stringResource(R.string.h3_view_all),
                color = Color(0xFFFF4FC3),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onViewAll() }
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEachIndexed { index, day ->
                TopDayTile(
                    day = day,
                    metric = state.selectedMetric,
                    rank = index + 1,
                    onClick = { onOpenDetails(day) },
                    modifier = Modifier.weight(1f)
                )
            }
            repeat((3 - items.size).coerceAtLeast(0)) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun TopDayTile(
    day: HistoryDayUi,
    metric: HistoryMetric,
    rank: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = listOf(Color(0xFFFFB02E), Color(0xFF18E8FF), Color(0xFF9B5CFF))
    val accent = colors.getOrElse(rank - 1) { Color(0xFF18E8FF) }
    Column(
        modifier = modifier
            .height(112.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(accent.copy(alpha = 0.12f))
            .border(1.dp, accent.copy(alpha = 0.26f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 11.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
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
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
            Text(
                text = day.localDate.shortMonthDay(),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
        }
        Text(
            text = day.metricDisplay(metric),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = metricHeroUnit(metric),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 10.5.sp,
            lineHeight = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
internal fun HistoryAchievementsCard(state: HistoryUiState, onViewAll: () -> Unit) {
    PremiumHistoryCard(glow = false) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.h3_achievements),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = stringResource(R.string.h3_view_all),
                    color = Color(0xFFFF4FC3),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onViewAll() }
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                state.achievements.forEach { achievement ->
                    AchievementBadge(achievement = achievement, modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(achievement: HistoryAchievementUi, modifier: Modifier) {
    val accent = when (achievement.level) {
        1 -> Color(0xFFFFB02E)
        2 -> Color(0xFFFFC13D)
        3 -> Color(0xFF59F27A)
        else -> Color(0xFF18E8FF)
    }
    Column(
        modifier = modifier
            .height(102.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(accent.copy(alpha = 0.09f))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(17.dp))
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AchievementHex(progress = achievement.progress, color = accent, modifier = Modifier.size(38.dp))
        Spacer(Modifier.height(5.dp))
        Text(achievement.title, color = MaterialTheme.colorScheme.onSurface, fontSize = 10.5.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(achievementSubtitle(achievement.level), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.8.sp, lineHeight = 9.5.sp, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun achievementSubtitle(level: Int): String = when (level) {
    1 -> stringResource(R.string.h3_steps)
    2 -> stringResource(R.string.h3_day_streak)
    3 -> stringResource(R.string.h3_marathon_steps)
    else -> stringResource(R.string.h3_active_days)
}

@Composable
private fun HistoryUiState.metricSummaryTitle(): String = when (selectedMetric) {
    HistoryMetric.STEPS -> stringResource(R.string.h3_steps)
    HistoryMetric.DISTANCE -> stringResource(R.string.h3_distance)
    HistoryMetric.CALORIES -> stringResource(R.string.h3_calories)
    HistoryMetric.ACTIVE_TIME -> stringResource(R.string.h3_active_time)
}

@Composable
private fun HistoryUiState.rangeMetricTotal(): String = when (selectedMetric) {
    HistoryMetric.STEPS -> totalSteps.formatHistoryNumber()
    HistoryMetric.DISTANCE -> "${totalDistanceKm.formatHistoryKm()} ${stringResource(R.string.h3_km)}"
    HistoryMetric.CALORIES -> "${totalCalories.formatHistoryNumber()} ${stringResource(R.string.h3_kcal)}"
    HistoryMetric.ACTIVE_TIME -> totalActiveMinutes.historyDurationText()
}

@Composable
internal fun HistoryDayUi.metricDisplay(metric: HistoryMetric): String = when (metric) {
    HistoryMetric.STEPS -> steps.formatHistoryNumber()
    HistoryMetric.DISTANCE -> distanceKm.formatHistoryKm()
    HistoryMetric.CALORIES -> calories.formatHistoryNumber()
    HistoryMetric.ACTIVE_TIME -> activeMinutes.historyDurationText()
}

@Composable
internal fun metricHeroUnit(metric: HistoryMetric): String = when (metric) {
    HistoryMetric.STEPS -> stringResource(R.string.h3_steps)
    HistoryMetric.DISTANCE -> stringResource(R.string.h3_km)
    HistoryMetric.CALORIES -> stringResource(R.string.h3_kcal)
    HistoryMetric.ACTIVE_TIME -> stringResource(R.string.h3_active_time)
}

@Composable
internal fun metricLabelForCards(metric: HistoryMetric): String = when (metric) {
    HistoryMetric.STEPS -> stringResource(R.string.h3_steps)
    HistoryMetric.DISTANCE -> stringResource(R.string.h3_distance)
    HistoryMetric.CALORIES -> stringResource(R.string.h3_calories)
    HistoryMetric.ACTIVE_TIME -> stringResource(R.string.h3_active_time)
}

@Composable
internal fun Int.historyDurationText(): String {
    val safe = coerceAtLeast(0)
    val h = safe / 60
    val m = safe % 60
    return when {
        h > 0 && m > 0 -> stringResource(R.string.h3_duration_h_m, h, m)
        h > 0 -> stringResource(R.string.h3_duration_h, h)
        else -> stringResource(R.string.h3_duration_m, m)
    }
}

private fun Float.calendarColor(metric: HistoryMetric): Color {
    val palette = when (metric) {
        HistoryMetric.STEPS -> listOf(Color(0xFF2AB56B), Color(0xFFFFC13D), Color(0xFFFF6A38), Color(0xFFFF4FC3))
        HistoryMetric.DISTANCE -> listOf(Color(0xFF2AB56B), Color(0xFF18E8FF), Color(0xFF2EA8FF), Color(0xFF7B61FF))
        HistoryMetric.CALORIES -> listOf(Color(0xFF2AB56B), Color(0xFFFFC13D), Color(0xFFFF8A34), Color(0xFFFF4F3D))
        HistoryMetric.ACTIVE_TIME -> listOf(Color(0xFF2AB56B), Color(0xFF7ED957), Color(0xFF18E8FF), Color(0xFFFF4FC3))
    }
    return when {
        this < 0.25f -> palette[0]
        this < 0.55f -> palette[1]
        this < 0.85f -> palette[2]
        else -> palette[3]
    }
}
