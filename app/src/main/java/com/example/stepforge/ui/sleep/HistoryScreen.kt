package com.example.stepforge.ui.sleep

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.stepforge.R
import com.example.stepforge.ui.sleep.data.SleepRepository
import com.example.stepforge.ui.sleep.model.SleepDay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun HistoryScreen(
    history: List<SleepDay>,
    onBack: () -> Unit,
    onDelete: (Long) -> Unit = {},
    onDeleteAllForDay: (LocalDate) -> Unit = {}
) {
    val cs = MaterialTheme.colorScheme

    if (history.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(stringResource(R.string.sleep_no_history_found), color = cs.onBackground)
                Spacer(Modifier.height(16.dp))
                Button(onClick = onBack) { Text(stringResource(R.string.sleep_go_back)) }
            }
        }
        return
    }

    val scroll = rememberScrollState()
    val todayIndex = history.indexOfLast { it.date == LocalDate.now() }
    var selIdx by remember(history) {
        mutableIntStateOf(
            if (todayIndex >= 0) todayIndex else history.lastIndex
        )
    }
    val safeIdx = selIdx.coerceIn(0, history.lastIndex)
    val selected = history[safeIdx]
    val prev = history.getOrNull(safeIdx - 1)
    val insights = remember(selected) { SleepRepository.generateInsights(selected) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        val multi = selected.sessions.size > 1
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text(stringResource(R.string.sleep_delete_title)) },
            text = {
                Text(
                    if (multi) stringResource(
                        R.string.sleep_delete_day_all_body,
                        selected.sessions.size,
                        selected.date.dayOfMonth,
                        selected.date.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                    )
                    else stringResource(R.string.sleep_delete_body)
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (multi) onDeleteAllForDay(selected.date)
                        else selected.id?.let { onDelete(it) }
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = cs.error)
                ) {
                    Text(
                        if (multi) stringResource(R.string.sleep_delete_day_all_confirm)
                        else stringResource(R.string.sleep_delete)
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(stringResource(R.string.sleep_cancel))
                }
            }
        )
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 52.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceVariant)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = cs.onSurface, modifier = Modifier.size(20.dp))
            }
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.sleep_history_title), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold), color = cs.onBackground)
                Text(stringResource(R.string.sleep_last_30_days_overview), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            }
            if (selected.hasAnyData) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.sleep_delete), tint = cs.error.copy(alpha = 0.8f))
                }
            }
        }

        WeeklySummary(history.takeLast(7), history.dropLast(7).takeLast(7))

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.CalendarMonth, null, tint = cs.primary, modifier = Modifier.size(16.dp))
                Text(stringResource(R.string.sleep_select_day), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = cs.onBackground)
            }
            ModernDaySelector(history, safeIdx) { selIdx = it }
        }

        AnimatedContent(
            targetState = selected,
            transitionSpec = { fadeIn(tween(220)).togetherWith(fadeOut(tween(180))) },
            label = "histDetail"
        ) { day ->
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                DayHeaderCard(day)

                val manualNotes = day.sessions
                    .mapNotNull { it.notes.takeIf { note -> note.isNotBlank() } }
                    .firstOrNull()

                if (manualNotes != null) {
                    ManualSleepMetaBlock(manualNotes)
                }

                if (day.hasAnyData) {
                    SleepCard {
                        Text(stringResource(R.string.sleep_timeline_title), style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
                        Spacer(Modifier.height(12.dp))
                        SleepTimeline(day)
                    }
                    StagesSection(day)
                    MetricsRow(day, prev)
                }
            }
        }

        TrendsCard(history.takeLast(7))

        if (insights.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.sleep_daily_analysis), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = cs.onBackground)
                insights.forEach { InsightCard(it) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun ModernDaySelector(
    days: List<SleepDay>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val scrollState = rememberScrollState()

    LaunchedEffect(Unit) {
        val targetScroll = (selectedIndex * 60).coerceAtMost(scrollState.maxValue)
        scrollState.scrollTo(targetScroll)
    }
    LaunchedEffect(selectedIndex) {
        scrollState.animateScrollTo((selectedIndex * 60).coerceAtMost(scrollState.maxValue))
    }

    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        days.forEachIndexed { i, day ->
            val isSelected = i == selectedIndex
            val hasData = day.hasAnyData
            val score = day.sleepScore

            val bg = if (isSelected)
                Brush.verticalGradient(listOf(cs.primary, cs.secondary))
            else
                Brush.verticalGradient(listOf(cs.surface, cs.surface))

            Column(
                modifier = Modifier
                    .width(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(bg)
                    .border(
                        1.dp,
                        if (isSelected) Color.Transparent else cs.outlineVariant.copy(alpha = 0.5f),
                        RoundedCornerShape(16.dp)
                    )
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onSelect(i) }
                    .padding(vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    day.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) cs.onPrimary.copy(0.8f) else cs.onSurfaceVariant
                    )
                )
                Text(
                    day.date.dayOfMonth.toString(),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isSelected) cs.onPrimary else cs.onSurface
                    )
                )

                Box(
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isSelected -> cs.onPrimary.copy(alpha = 0.6f)
                                !hasData -> cs.outlineVariant.copy(alpha = 0.3f)
                                score != null && score >= 85 -> cs.primary
                                score != null && score >= 70 -> cs.secondary
                                else -> cs.error.copy(alpha = 0.7f)
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun WeeklySummary(thisWeek: List<SleepDay>, lastWeek: List<SleepDay>) {
    val cs       = MaterialTheme.colorScheme
    val withData = thisWeek.filter { it.hasAnyData }
    val avgMin   = if (withData.isEmpty()) 0f else withData.map { it.totalSleepMinutes }.average().toFloat()
    val prevMin  = lastWeek.filter { it.hasAnyData }.map { it.totalSleepMinutes }.average()
        .takeIf { !it.isNaN() }?.toFloat() ?: avgMin
    val diff     = avgMin - prevMin
    val scores   = withData.mapNotNull { it.sleepScore }
    val avgScore = if (scores.isEmpty()) null else scores.average().toInt()
    val avgH     = (avgMin / 60).toInt()
    val avgM     = (avgMin % 60).toInt()

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SleepCard(Modifier.weight(1f)) {
            Text(stringResource(R.string.sleep_weekly_avg), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            if (avgMin > 0f) {
                SleepCounter(
                    avgH,
                    800,
                    MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    cs.onSurface,
                    stringResource(R.string.sleep_duration_hours_suffix, avgM)
                )
                val (arrow, ac) = if (diff >= 0) {
                    stringResource(R.string.sleep_trend_up_minutes, diff.toInt()) to cs.primary
                } else {
                    stringResource(R.string.sleep_trend_down_minutes, (-diff).toInt()) to cs.error
                }
                Text(arrow, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = ac))
            } else {
                Text(stringResource(R.string.sleep_no_value), style = MaterialTheme.typography.titleLarge, color = cs.onSurfaceVariant)
            }
        }
        SleepCard(Modifier.weight(1f)) {
            Text(stringResource(R.string.sleep_avg_score), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            if (avgScore != null) {
                val sc = scoreColor(avgScore, cs)
                SleepCounter(avgScore, 900, MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), sc)
                Text(scoreLabel(avgScore), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, color = sc))
            } else {
                Text(stringResource(R.string.sleep_no_value), style = MaterialTheme.typography.titleLarge, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DayHeaderCard(day: SleepDay) {
    val cs    = MaterialTheme.colorScheme
    val score = day.sleepScore
    val col   = if (score != null) scoreColor(score, cs) else cs.onSurfaceVariant

    SleepCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Column {
                Text(day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = cs.onSurface)
                Text(
                    stringResource(
                        R.string.sleep_time_range_dash,
                        day.date.dayOfMonth.toString(),
                        day.date.month.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault())
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DataModeChip(day.availability, day.mode)
                if (score != null) {
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(col.copy(0.1f)).padding(horizontal = 10.dp, vertical = 6.dp)) {
                        Text(score.toString(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold), color = col)
                    }
                }
            }
        }

        if (day.hasAnyData) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = cs.outlineVariant.copy(0.5f))
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                InfoChip(stringResource(R.string.sleep_duration_chip), stringResource(R.string.sleep_duration_preview, day.hours, day.minutes), cs.primary)
                InfoChip(stringResource(R.string.sleep_bedtime_label), day.bedTimeStr, cs.secondary)
                InfoChip(stringResource(R.string.sleep_wake_time_label), day.wakeTimeStr, cs.tertiary)
                InfoChip(stringResource(R.string.sleep_quality_chip), scoreLabel(score ?: 0), col)
            }
        } else {
            Spacer(Modifier.height(12.dp))
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(cs.surfaceVariant.copy(alpha = 0.5f)).padding(12.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.sleep_no_data_for_day), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun InfoChip(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.ExtraBold), color = MaterialTheme.colorScheme.onSurface)
        Box(Modifier.size(4.dp).clip(CircleShape).background(color))
    }
}

@Composable
private fun TrendsCard(days: List<SleepDay>) {
    val cs = MaterialTheme.colorScheme
    SleepCard {
        Text(stringResource(R.string.sleep_weekly_trend), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = cs.onSurface)
        Spacer(Modifier.height(12.dp))
        WeeklyBarChart(days)
        Spacer(Modifier.height(16.dp))
        HorizontalDivider(color = cs.outlineVariant.copy(0.5f))
        Spacer(Modifier.height(12.dp))
        ScoreLineChart(days)
    }
}
