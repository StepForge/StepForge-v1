package com.example.stepforge.ui.sleep

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import com.example.stepforge.ui.sleep.model.DataAvailability
import com.example.stepforge.ui.sleep.model.InsightSeverity
import com.example.stepforge.ui.sleep.model.ManualSleepEntry
import com.example.stepforge.ui.sleep.model.SleepDay
import com.example.stepforge.ui.sleep.model.SleepInsight
import com.example.stepforge.ui.sleep.model.SleepSessionType
import com.example.stepforge.ui.sleep.model.SleepStageData
import com.example.stepforge.ui.sleep.model.StageType
import com.example.stepforge.ui.sleep.model.TrackingMode
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.Locale

// ── Press scale modifier ──────────────────────────────────────────────────────

@Composable
fun Modifier.pressScale(target: Float = 0.97f): Modifier {
    var pressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue   = if (pressed) target else 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessHigh),
        label         = "pressScale"
    )
    return graphicsLayer { scaleX = scale; scaleY = scale }
        .pointerInput(Unit) {
            detectTapGestures(onPress = {
                pressed = true; tryAwaitRelease(); pressed = false
            })
        }
}

// ── Animated int counter ──────────────────────────────────────────────────────

@Composable
fun SleepCounter(
    target: Int,
    durationMs: Int = 800,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.headlineMedium,
    color: Color = MaterialTheme.colorScheme.onSurface,
    suffix: String = ""
) {
    val value by animateIntAsState(
        targetValue   = target,
        animationSpec = tween(durationMs, easing = EaseOutCubic),
        label         = "counter"
    )
    Text("$value$suffix", style = style, color = color)
}

// ── Base surface card ─────────────────────────────────────────────────────────

@Composable
fun SleepCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val mod = if (onClick != null) modifier.pressScale() else modifier
    Card(
        modifier  = mod,
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick   = onClick ?: {}
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

// ── Data mode chip ────────────────────────────────────────────────────────────

@Composable
fun DataModeChip(availability: DataAvailability, mode: TrackingMode) {
    val cs = MaterialTheme.colorScheme
    val (icon, label) = when (availability) {
        DataAvailability.FULL    -> Icons.Default.Sensors      to stringResource(R.string.sleep_health_connect)
        DataAvailability.LIMITED -> Icons.Default.PhoneAndroid to mode.label()
        DataAvailability.NONE    -> Icons.Default.CloudOff     to stringResource(R.string.sleep_waiting_for_logs)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(cs.surfaceVariant)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment     = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(11.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
    }
}

// ── Empty state ───────────────────────────────────────────────────────────────

@Composable
fun EmptyStateCard(
    onStartTracking: () -> Unit,
    onManualEntry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    SleepCard(modifier = modifier) {
        Column(
            modifier            = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            Box(
                modifier         = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(cs.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Bedtime, null,
                    tint     = cs.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    stringResource(R.string.sleep_no_sleep_data_message),
                    style = MaterialTheme.typography.titleMedium,
                    color = cs.onSurface
                )
                Text(
                    stringResource(R.string.sleep_track_to_unlock),
                    style     = MaterialTheme.typography.bodySmall,
                    color     = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }

            // Primary button
            Button(
                onClick  = onStartTracking,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor   = cs.onPrimary
                )
            ) {
                Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(stringResource(R.string.sleep_start_tracking), style = MaterialTheme.typography.labelLarge)
            }

            // ✅ FIX: OutlinedButton — doğru API kullanımı
            OutlinedButton(
                onClick  = onManualEntry,
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.dp, cs.primary.copy(alpha = 0.5f))
            ) {
                Icon(
                    Icons.Default.EditCalendar, null,
                    tint     = cs.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.sleep_enter_manually),
                    style = MaterialTheme.typography.labelLarge,
                    color = cs.primary
                )
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

// ── Circular score ────────────────────────────────────────────────────────────

@Composable
fun CircularScore(
    score: Int,
    size: Dp = 120.dp,
    strokeWidth: Dp = 8.dp,
    modifier: Modifier = Modifier
) {
    val cs       = MaterialTheme.colorScheme
    val arcColor = scoreColor(score, cs)
    val progress by animateFloatAsState(
        targetValue   = score / 100f,
        animationSpec = tween(900, easing = EaseOutCubic),
        label         = "scoreArc"
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val sw  = strokeWidth.toPx()
            val pad = sw / 2f
            val sz  = Size(this.size.width - sw, this.size.height - sw)
            val tl  = Offset(pad, pad)
            drawArc(
                color      = arcColor.copy(alpha = 0.12f),
                startAngle = -220f, sweepAngle = 260f,
                useCenter  = false, topLeft = tl, size = sz,
                style      = Stroke(sw, cap = StrokeCap.Round)
            )
            drawArc(
                color      = arcColor,
                startAngle = -220f, sweepAngle = 260f * progress,
                useCenter  = false, topLeft = tl, size = sz,
                style      = Stroke(sw, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            SleepCounter(
                target     = score,
                durationMs = 900,
                style      = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                color      = arcColor
            )
            Text(stringResource(R.string.sleep_score_out_of_100), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }
    }
}

// ── Stage timeline helpers ────────────────────────────────────────────────────

private val STAGE_DISPLAY_ORDER = listOf(
    StageType.LIGHT,
    StageType.DEEP,
    StageType.REM,
    StageType.AWAKE
)

private fun mergeStageList(stages: List<SleepStageData>): List<SleepStageData> {
    if (stages.isEmpty()) return emptyList()
    return stages
        .groupBy { it.type }
        .map { (t, list) -> SleepStageData(t, list.sumOf { it.durationMinutes }) }
        .filter { it.durationMinutes > 0 }
        .sortedBy { t -> STAGE_DISPLAY_ORDER.indexOf(t.type).let { i -> if (i < 0) 99 else i } }
}

private fun normalizeStageWeights(
    merged: List<SleepStageData>,
    totalMinutes: Int
): List<Pair<SleepStageData, Float>> {
    val total = totalMinutes.coerceAtLeast(1)
    val minSlice = 0.014f
    val items = merged
        .filter { it.durationMinutes > 0 }
        .map { s ->
            val f = s.durationMinutes / total.toFloat()
            s to maxOf(f, minSlice)
        }
    if (items.isEmpty()) return emptyList()
    val sum = items.sumOf { it.second.toDouble() }.toFloat().coerceAtLeast(1e-6f)
    return items.map { (s, f) -> s to (f / sum) }
}

private fun buildStageMinutesMap(day: SleepDay): Map<StageType, Int> {
    val session = day.mainSession ?: return emptyMap()
    val base = STAGE_DISPLAY_ORDER.associateWith { 0 }.toMutableMap()
    if (day.hasFullData && !session.stages.isNullOrEmpty()) {
        session.stages!!.groupBy { it.type }.forEach { (t, list) ->
            base[t] = list.sumOf { it.durationMinutes }
        }
    } else if (day.hasAnyData) {
        base[StageType.LIGHT] = session.totalMinutes
    }
    return base
}

// ── Sleep timeline ────────────────────────────────────────────────────────────

@Composable
fun SleepTimeline(day: SleepDay, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val session = day.mainSession
    var selectedStage by remember { mutableStateOf<StageType?>(null) }

    val rawStages = remember(day.mainSession, day.hasFullData, day.totalSleepMinutes) {
        when {
            day.hasAnyData && session != null && day.hasFullData && !session.stages.isNullOrEmpty() -> session.stages!!
            day.hasAnyData && session != null -> listOf(
                SleepStageData(StageType.LIGHT, day.totalSleepMinutes.coerceAtLeast(1))
            )
            else -> emptyList()
        }
    }
    val stageMinutes = remember(rawStages) {
        STAGE_DISPLAY_ORDER.associateWith { type ->
            rawStages.filter { it.type == type }.sumOf { it.durationMinutes }
        }
    }
    val denom = day.totalSleepMinutes.coerceAtLeast(1)

    selectedStage?.let { stage ->
        StageDetailBottomSheet(
            type = stage,
            minutes = stageMinutes[stage] ?: 0,
            totalMinutes = denom,
            onDismiss = { selectedStage = null }
        )
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(day.bedTimeStr, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
            Text(day.wakeTimeStr, style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
        }

        when {
            day.hasAnyData && session != null -> {
                val merged = mergeStageList(rawStages)
                FullTimeline(
                    mergedStagesInput = merged,
                    totalMinutes = denom,
                    onStageClick = { selectedStage = it }
                )
            }
            else -> {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(cs.surfaceVariant.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        stringResource(R.string.sleep_no_data),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FullTimeline(
    mergedStagesInput: List<SleepStageData>,
    totalMinutes: Int,
    onStageClick: (StageType) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val merged = mergeStageList(mergedStagesInput)
    val total = totalMinutes.coerceAtLeast(1)
    val weights = normalizeStageWeights(merged, total)

    Row(
        Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.35f))
    ) {
        if (weights.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(cs.outlineVariant.copy(alpha = 0.35f))
            )
        } else {
            weights.forEach { (stage, w) ->
                Box(
                    Modifier
                        .weight(w)
                        .fillMaxHeight()
                        .clickable { onStageClick(stage.type) }
                        .background(stageColor(stage.type, cs))
                )
            }
        }
    }

    Spacer(Modifier.height(10.dp))

    val legendScroll = rememberScrollState()
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(legendScroll),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        STAGE_DISPLAY_ORDER.forEach { type ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onStageClick(type) }
                    .padding(horizontal = 4.dp, vertical = 3.dp)
            ) {
                Box(
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(stageColor(type, cs))
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = type.label(),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

// ── Stage section ─────────────────────────────────────────────────────────────

@Composable
fun StagesSection(day: SleepDay, modifier: Modifier = Modifier) {
    if (!day.hasAnyData) return

    val cs = MaterialTheme.colorScheme
    var showStagesInfo by remember { mutableStateOf(false) }
    var selectedStage by remember { mutableStateOf<StageType?>(null) }

    if (showStagesInfo) {
        AlertDialog(
            onDismissRequest = { showStagesInfo = false },
            title = { Text(stringResource(R.string.sleep_stages_info_title)) },
            text = {
                Text(
                    stringResource(R.string.sleep_stages_info_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showStagesInfo = false }) {
                    Text(stringResource(R.string.sleep_got_it))
                }
            }
        )
    }

    val stageMinutes = buildStageMinutesMap(day)
    val total = day.totalSleepMinutes.coerceAtLeast(1)

    selectedStage?.let { stage ->
        StageDetailBottomSheet(
            type = stage,
            minutes = stageMinutes[stage] ?: 0,
            totalMinutes = total,
            onDismiss = { selectedStage = null }
        )
    }

    SleepCard(modifier = modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                stringResource(R.string.sleep_stages_section_title),
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { showStagesInfo = true },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Info,
                    contentDescription = stringResource(R.string.sleep_stages_info_title),
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
        Spacer(Modifier.height(10.dp))

        StageSummaryStrip(stageMinutes, total, cs)

        Spacer(Modifier.height(14.dp))

        STAGE_DISPLAY_ORDER.forEach { type ->
            val minutes = stageMinutes[type] ?: 0
            StageBreakdownRow(
                type = type,
                minutes = minutes,
                total = total,
                cs = cs,
                onClick = { selectedStage = type }
            )
        }
    }
}

@Composable
private fun StageSummaryStrip(
    map: Map<StageType, Int>,
    total: Int,
    cs: ColorScheme
) {
    val merged = STAGE_DISPLAY_ORDER.mapNotNull { t ->
        val m = map[t] ?: 0
        if (m > 0) SleepStageData(t, m) else null
    }
    val weights = normalizeStageWeights(merged, total.coerceAtLeast(1))
    Row(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.45f))
    ) {
        if (weights.isEmpty()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(cs.outlineVariant.copy(alpha = 0.3f))
            )
        } else {
            weights.forEach { (stage, w) ->
                Box(
                    Modifier
                        .weight(w)
                        .fillMaxHeight()
                        .background(stageColor(stage.type, cs))
                )
            }
        }
    }
}

@Composable
private fun StageBreakdownRow(
    type: StageType,
    minutes: Int,
    total: Int,
    cs: ColorScheme,
    onClick: () -> Unit
) {
    val safeTotal = total.coerceAtLeast(1)
    val frac = (minutes.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f)
    val col = stageColor(type, cs)
    val h = minutes / 60
    val m = minutes % 60
    val dur = when {
        minutes <= 0 -> stringResource(R.string.sleep_duration_zero_minutes)
        h > 0 -> stringResource(R.string.sleep_duration_hours_minutes, h, m)
        else -> stringResource(R.string.sleep_duration_minutes_only, m)
    }

    val animatedProgress by animateFloatAsState(
        targetValue = frac,
        animationSpec = tween(500, easing = EaseOutCubic),
        label = "stage_${type.name}"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(col)
        )
        Text(
            text = type.label(),
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
            color = cs.onSurface,
            maxLines = 1,
            modifier = Modifier.widthIn(max = 108.dp)
        )
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(999.dp)),
            color = col,
            trackColor = col.copy(alpha = 0.12f)
        )
        Text(
            text = "${(frac * 100f).toInt()}%",
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(36.dp)
        )
        Text(
            text = dur,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
            color = cs.onSurface,
            textAlign = TextAlign.End,
            modifier = Modifier.widthIn(min = 48.dp)
        )
        Icon(
            Icons.Default.ChevronRight,
            contentDescription = null,
            tint = cs.onSurfaceVariant.copy(alpha = 0.35f),
            modifier = Modifier.size(18.dp)
        )
    }
}


// ── Stage detail bottom sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StageDetailBottomSheet(
    type: StageType,
    minutes: Int,
    totalMinutes: Int,
    onDismiss: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val accent = stageColor(type, cs)
    val safeTotal = totalMinutes.coerceAtLeast(1)
    val percent = ((minutes.toFloat() / safeTotal.toFloat()) * 100f).toInt().coerceIn(0, 100)
    val duration = formatStageDuration(minutes)
    val benefits = stageBenefits(type)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 48.dp, height = 5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(cs.onSurfaceVariant.copy(alpha = 0.55f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .padding(bottom = 28.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(accent.copy(alpha = 0.13f)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(accent)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = type.label(),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.sleep_stage_detail_duration_percent, duration, percent),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = accent
                    )
                }
            }

            LinearProgressIndicator(
                progress = { (minutes.toFloat() / safeTotal.toFloat()).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(999.dp)),
                color = accent,
                trackColor = accent.copy(alpha = 0.12f)
            )

            HorizontalDivider(color = cs.outlineVariant.copy(alpha = 0.45f))

            Text(
                text = stageDescription(type),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = cs.onSurfaceVariant,
                lineHeight = 26.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = stringResource(R.string.sleep_stage_detail_benefits),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface
                )

                benefits.forEach { benefit ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(accent)
                        )
                        Text(
                            text = benefit,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurfaceVariant
                        )
                    }
                }
            }

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accent.copy(alpha = 0.14f),
                    contentColor = accent
                )
            ) {
                Text(
                    text = stringResource(R.string.close),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}

@Composable
private fun stageDescription(type: StageType): String = when (type) {
    StageType.LIGHT -> stringResource(R.string.sleep_stage_light_description)
    StageType.DEEP -> stringResource(R.string.sleep_stage_deep_description)
    StageType.REM -> stringResource(R.string.sleep_stage_rem_description)
    StageType.AWAKE -> stringResource(R.string.sleep_stage_awake_description)
}

@Composable
private fun stageBenefits(type: StageType): List<String> = when (type) {
    StageType.LIGHT -> listOf(
        stringResource(R.string.sleep_stage_light_benefit_1),
        stringResource(R.string.sleep_stage_light_benefit_2)
    )
    StageType.DEEP -> listOf(
        stringResource(R.string.sleep_stage_deep_benefit_1),
        stringResource(R.string.sleep_stage_deep_benefit_2),
        stringResource(R.string.sleep_stage_deep_benefit_3)
    )
    StageType.REM -> listOf(
        stringResource(R.string.sleep_stage_rem_benefit_1),
        stringResource(R.string.sleep_stage_rem_benefit_2),
        stringResource(R.string.sleep_stage_rem_benefit_3)
    )
    StageType.AWAKE -> listOf(
        stringResource(R.string.sleep_stage_awake_benefit_1)
    )
}

@Composable
private fun formatStageDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        minutes <= 0 -> stringResource(R.string.sleep_duration_zero_minutes)
        h > 0 -> stringResource(R.string.sleep_duration_hours_minutes, h, m)
        else -> stringResource(R.string.sleep_duration_minutes_only, m)
    }
}

// ── Metrics row ───────────────────────────────────────────────────────────────

@Composable
fun MetricsRow(
    day: SleepDay,
    prevDay: SleepDay? = null,
    modifier: Modifier = Modifier
) {
    if (!day.hasAnyData) return

    val cs = MaterialTheme.colorScheme
    var showHrInfo by remember { mutableStateOf(false) }

    if (showHrInfo) {
        AlertDialog(
            onDismissRequest = { showHrInfo = false },
            title = { Text(stringResource(R.string.sleep_hr_info_title)) },
            text = {
                Text(
                    stringResource(R.string.sleep_hr_info_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant
                )
            },
            confirmButton = {
                TextButton(onClick = { showHrInfo = false }) {
                    Text(stringResource(R.string.sleep_got_it))
                }
            }
        )
    }

    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        MetricTile(
            modifier = Modifier
                .weight(1f)
                .height(132.dp),
            label = stringResource(R.string.sleep_metric_heart_rate),
            value = day.heartRateAvg,
            unit = stringResource(R.string.sleep_unit_bpm),
            trendDiff = prevDay?.heartRateAvg?.let { p -> day.heartRateAvg?.minus(p) },
            accentIdx = 0,
            showLabelInfo = true,
            onLabelInfo = { showHrInfo = true },
            heartEmptyState = true
        )
        MetricTile(
            modifier = Modifier
                .weight(1f)
                .height(132.dp),
            label = stringResource(R.string.sleep_metric_wake_ups),
            value = day.interruptionCount,
            unit = stringResource(R.string.sleep_wake_times_unit),
            trendDiff = prevDay?.interruptionCount?.let { p -> -(day.interruptionCount - p) },
            accentIdx = 2,
            showLabelInfo = false,
            onLabelInfo = {},
            heartEmptyState = false
        )
        MetricTile(
            modifier = Modifier
                .weight(1f)
                .height(132.dp),
            label = stringResource(R.string.sleep_metric_score_label),
            value = day.sleepScore,
            unit = "",
            trendDiff = prevDay?.sleepScore?.let { p -> day.sleepScore?.minus(p) },
            accentIdx = 1,
            showLabelInfo = false,
            onLabelInfo = {},
            heartEmptyState = false
        )
    }
}

@Composable
private fun MetricTile(
    modifier: Modifier,
    label: String,
    value: Int?,
    unit: String,
    trendDiff: Int?,
    accentIdx: Int,
    showLabelInfo: Boolean,
    onLabelInfo: () -> Unit,
    heartEmptyState: Boolean
) {
    val cs = MaterialTheme.colorScheme
    val accent = when (accentIdx) {
        0 -> cs.primary
        1 -> cs.secondary
        else -> cs.tertiary
    }

    val trendLabel = trendDiff?.let { d ->
        when {
            d > 0 -> "↑ $d"
            d < 0 -> "↓ ${-d}"
            else -> "—"
        }
    }
    val td = trendDiff
    val trendColor = when {
        td == null -> cs.onSurfaceVariant.copy(alpha = 0.4f)
        td > 0 -> when (accentIdx) {
            0 -> cs.primary
            2 -> cs.error.copy(alpha = 0.85f)
            else -> cs.secondary
        }
        td < 0 -> cs.primary.copy(alpha = 0.85f)
        else -> cs.onSurfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.3f))
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
                if (showLabelInfo) {
                    IconButton(
                        onClick = onLabelInfo,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = stringResource(R.string.sleep_hr_info_title),
                            tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                when {
                    heartEmptyState && value == null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.MonitorHeart,
                                contentDescription = null,
                                tint = accent.copy(alpha = 0.9f),
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = stringResource(R.string.sleep_no_data),
                                style = MaterialTheme.typography.labelLarge,
                                color = cs.onSurface
                            )
                        }
                    }
                    value != null -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = value.toString(),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = accent,
                                maxLines = 1
                            )
                            if (unit.isNotEmpty()) {
                                Text(
                                    text = unit,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = cs.onSurfaceVariant,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                    else -> {
                        Text(
                            text = "—",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(22.dp),
                contentAlignment = Alignment.Center
            ) {
                if (trendLabel != null && value != null) {
                    Text(
                        text = trendLabel,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = trendColor,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ── Insight card ──────────────────────────────────────────────────────────────

@Composable
fun InsightCard(insight: SleepInsight) {
    val cs = MaterialTheme.colorScheme
    var expanded by remember { mutableStateOf(false) }
    val (bg, ic, icon) = when (insight.severity) {
        InsightSeverity.POSITIVE -> Triple(cs.primaryContainer,   cs.onPrimaryContainer,   Icons.Default.CheckCircle)
        InsightSeverity.WARNING  -> Triple(cs.errorContainer,     cs.onErrorContainer,     Icons.Default.Warning)
        InsightSeverity.INFO     -> Triple(cs.secondaryContainer, cs.onSecondaryContainer, Icons.Default.Info)
    }

    SleepCard(onClick = { expanded = !expanded }) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            Box(
                Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = ic, modifier = Modifier.size(16.dp))
            }
            Text(
                stringResource(insight.titleRes),
                style    = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color    = cs.onSurface,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                null,
                tint     = cs.onSurfaceVariant.copy(0.5f),
                modifier = Modifier.size(16.dp)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter   = expandVertically(tween(200)) + fadeIn(tween(160)),
            exit    = shrinkVertically(tween(160)) + fadeOut(tween(120))
        ) {
            Column {
                Spacer(Modifier.height(10.dp))
                HorizontalDivider(color = cs.outlineVariant)
                Spacer(Modifier.height(10.dp))
                Text(
                    stringResource(insight.bodyRes, *insight.bodyArgs),
                    style = MaterialTheme.typography.bodySmall.copy(
                        color      = cs.onSurfaceVariant,
                        lineHeight = 18.sp
                    )
                )
            }
        }
    }
}

// ── Weekly bar chart ──────────────────────────────────────────────────────────

@Composable
fun WeeklyBarChart(days: List<SleepDay>, modifier: Modifier = Modifier) {
    val cs     = MaterialTheme.colorScheme
    val maxH   = days.filter { it.hasAnyData }.maxOfOrNull { it.totalSleepMinutes / 60f } ?: 8f
    val avgMin = days.filter { it.hasAnyData }.map { it.totalSleepMinutes }.average()
        .takeIf { !it.isNaN() }?.toFloat() ?: 0f
    val avgH   = avgMin / 60f
    val today  = java.time.LocalDate.now()

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            Modifier.fillMaxWidth().height(80.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.Bottom
        ) {
            days.forEachIndexed { i, day ->
                val isToday = day.date == today
                val frac    = if (day.hasAnyData) (day.totalSleepMinutes / 60f) / maxH.coerceAtLeast(0.1f) else 0f
                val aFrac by animateFloatAsState(
                    frac, tween(450, i * 45, EaseOutCubic), label = "wb$i"
                )
                val barColor = when {
                    !day.hasAnyData -> cs.surfaceVariant
                    isToday         -> cs.primary
                    else            -> cs.primary.copy(alpha = 0.35f)
                }
                Column(
                    Modifier.weight(1f).padding(horizontal = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    if (isToday && day.hasAnyData) {
                        Text(
                            stringResource(R.string.sleep_target_hours, day.hours),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                color      = cs.primary
                            )
                        )
                    }
                    Spacer(Modifier.height(2.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(aFrac.coerceAtLeast(if (day.hasAnyData) 0.04f else 0.02f))
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(barColor)
                    )
                }
            }
        }

        // Average indicator
        if (avgH > 0f) {
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    Modifier.width(14.dp).height(2.dp)
                        .background(cs.onSurfaceVariant.copy(0.4f))
                )
                Text(
                    stringResource(R.string.sleep_avg_hours_chart, avgH),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            days.forEach { day ->
                val isToday = day.date == today
                Text(
                    day.date.dayOfWeek.name.take(1),
                    style     = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = if (isToday) FontWeight.ExtraBold else FontWeight.Normal,
                        color      = if (isToday) cs.primary else cs.onSurfaceVariant
                    ),
                    modifier  = Modifier.weight(1f),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// ── Score line chart ──────────────────────────────────────────────────────────

@Composable
fun ScoreLineChart(days: List<SleepDay>, modifier: Modifier = Modifier) {
    val cs        = MaterialTheme.colorScheme
    val validDays = days.filter { it.sleepScore != null }
    val col       = cs.primary

    if (validDays.size < 2) {
        Box(modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
            Text(
                stringResource(R.string.sleep_not_enough_data),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant
            )
        }
        return
    }

    val animP by animateFloatAsState(1f, tween(900, easing = EaseOutCubic), label = "slc")

    Box(modifier = modifier.fillMaxWidth().height(64.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            val scores = validDays.map { it.sleepScore!!.toFloat() }
            val minS   = (scores.min() - 5f).coerceAtLeast(0f)
            val maxS   = (scores.max() + 5f).coerceAtMost(100f)
            val range  = (maxS - minS).coerceAtLeast(10f)
            val stepX  = size.width / (validDays.size - 1).toFloat()
            val pts    = scores.mapIndexed { i, s ->
                Offset(i * stepX, size.height - ((s - minS) / range) * size.height)
            }
            val clipW  = size.width * animP

            // Fill area
            val area = Path().apply {
                moveTo(pts.first().x, size.height)
                pts.forEach { lineTo(it.x.coerceAtMost(clipW), it.y) }
                lineTo(pts.last().x.coerceAtMost(clipW), size.height)
                close()
            }
            drawPath(area, Brush.verticalGradient(listOf(col.copy(0.15f), Color.Transparent)))

            // Line
            val line = Path().apply {
                pts.forEachIndexed { i, pt ->
                    val x = pt.x.coerceAtMost(clipW)
                    if (i == 0) moveTo(x, pt.y) else lineTo(x, pt.y)
                }
            }
            drawPath(
                line, col,
                style = Stroke(2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            // Dots
            pts.forEach { pt ->
                if (pt.x <= clipW) {
                    drawCircle(col, 3.dp.toPx(), pt)
                    drawCircle(Color.White, 1.5.dp.toPx(), pt)
                }
            }
        }
    }
}



// ── Premium manual sleep entry bottom sheet ───────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualEntrySheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onSave: (ManualSleepEntry) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()

    var bedTime by remember { mutableStateOf(LocalTime.of(23, 0)) }
    var wakeTime by remember { mutableStateOf(LocalTime.of(7, 0)) }
    var isNap by remember { mutableStateOf(false) }

    var quality by remember { mutableStateOf(ManualSleepQuality.BALANCED) }
    var wakeFeeling by remember { mutableStateOf(ManualWakeFeeling.BALANCED) }
    var interruptions by remember { mutableStateOf(ManualNightInterruptions.NONE) }
    var selectedFactors by remember { mutableStateOf(setOf<ManualSleepFactor>()) }
    var optionalNote by remember { mutableStateOf("") }

    var pickerTarget by remember { mutableStateOf<ManualTimePickerTarget?>(null) }

    val durationMinutes = remember(bedTime, wakeTime) {
        calculateManualSleepDurationMinutes(bedTime, wakeTime)
    }
    val durationText = remember(durationMinutes) { formatManualSleepDuration(durationMinutes) }

    val warningText = when {
        durationMinutes < 5 -> stringResource(R.string.sleep_manual_warning_too_short)
        durationMinutes > 20 * 60 -> stringResource(R.string.sleep_manual_warning_too_long)
        isNap && durationMinutes > 4 * 60 -> stringResource(R.string.sleep_manual_warning_nap_long)
        else -> null
    }
    val canSave = durationMinutes in 5..(20 * 60)

    ModalBottomSheet(
        // Prevent accidental swipe/outside dismiss while the user is scrolling.
        // The sheet closes only from the X button or after saving.
        onDismissRequest = {},
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
        containerColor = cs.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ManualSheetHeader(onDismiss = onDismiss)

            ManualSleepTypeSelector(
                isNap = isNap,
                onNapChange = { isNap = it }
            )

            ManualSleepTimeSection(
                bedTime = bedTime,
                wakeTime = wakeTime,
                durationText = durationText,
                warningText = warningText,
                onBedClick = { pickerTarget = ManualTimePickerTarget.BED },
                onWakeClick = { pickerTarget = ManualTimePickerTarget.WAKE }
            )

            ManualQualityScaleSection(
                selected = quality,
                onSelected = { quality = it }
            )

            ManualWakeFeelingSection(
                selected = wakeFeeling,
                onSelected = { wakeFeeling = it }
            )

            ManualNightInterruptionsSection(
                selected = interruptions,
                onSelected = { interruptions = it }
            )

            ManualSleepFactorsSection(
                selectedFactors = selectedFactors,
                onToggle = { factor ->
                    selectedFactors = if (factor in selectedFactors) {
                        selectedFactors - factor
                    } else {
                        selectedFactors + factor
                    }
                }
            )

            ManualOptionalNoteSection(
                value = optionalNote,
                onValueChange = { input -> optionalNote = input.take(160) }
            )

            Button(
                onClick = {
                    val packedNote = buildPackedManualSleepNote(
                        userNote = optionalNote,
                        quality = quality,
                        wakeFeeling = wakeFeeling,
                        interruptions = interruptions,
                        factors = selectedFactors
                    )

                    onSave(
                        ManualSleepEntry(
                            bedTime,
                            wakeTime,
                            quality.rating,
                            packedNote,
                            if (isNap) SleepSessionType.NAP else SleepSessionType.MAIN
                        )
                    )

                    scope.launch {
                        onDismiss()
                    }
                },
                enabled = canSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = cs.primary,
                    contentColor = cs.onPrimary,
                    disabledContainerColor = cs.surfaceVariant,
                    disabledContentColor = cs.onSurfaceVariant
                )
            ) {
                Text(
                    text = stringResource(R.string.sleep_manual_save_session),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }

    pickerTarget?.let { target ->
        ManualTimePickerDialog(
            title = when (target) {
                ManualTimePickerTarget.BED -> stringResource(R.string.sleep_manual_bedtime)
                ManualTimePickerTarget.WAKE -> stringResource(R.string.sleep_manual_wake_time)
            },
            initialTime = when (target) {
                ManualTimePickerTarget.BED -> bedTime
                ManualTimePickerTarget.WAKE -> wakeTime
            },
            onDismiss = { pickerTarget = null },
            onConfirm = { selectedTime ->
                when (target) {
                    ManualTimePickerTarget.BED -> bedTime = selectedTime
                    ManualTimePickerTarget.WAKE -> wakeTime = selectedTime
                }
                pickerTarget = null
            }
        )
    }
}

@Composable
private fun ManualSheetHeader(onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = stringResource(R.string.sleep_manual_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground
            )
            Text(
                text = stringResource(R.string.sleep_manual_subtitle),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
                lineHeight = 18.sp
            )
        }

        IconButton(onClick = onDismiss) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.close),
                tint = cs.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ManualSleepTypeSelector(
    isNap: Boolean,
    onNapChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.35f))
            .padding(5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        ManualSegmentButton(
            modifier = Modifier.weight(1f),
            selected = !isNap,
            label = stringResource(R.string.sleep_manual_main_sleep),
            icon = Icons.Default.Bedtime,
            onClick = { onNapChange(false) }
        )
        ManualSegmentButton(
            modifier = Modifier.weight(1f),
            selected = isNap,
            label = stringResource(R.string.sleep_manual_nap),
            icon = Icons.Default.Snooze,
            onClick = { onNapChange(true) }
        )
    }
}

@Composable
private fun ManualSegmentButton(
    modifier: Modifier,
    selected: Boolean,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.98f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "manualSegmentScale"
    )

    Row(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(18.dp))
            .background(if (selected) cs.primary.copy(alpha = 0.18f) else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) cs.primary else cs.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            color = if (selected) cs.primary else cs.onSurfaceVariant,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ManualSleepTimeSection(
    bedTime: LocalTime,
    wakeTime: LocalTime,
    durationText: String,
    warningText: String?,
    onBedClick: () -> Unit,
    onWakeClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ManualPremiumCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.sleep_manual_time_range),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface
                )
                Text(
                    text = stringResource(R.string.sleep_manual_time_range_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = durationText,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.primary
                )
                Text(
                    text = stringResource(R.string.sleep_manual_duration),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
        }

        Spacer(Modifier.height(14.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ManualTimeCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.sleep_manual_bedtime),
                time = bedTime,
                isNight = true,
                onClick = onBedClick
            )
            ManualTimeCard(
                modifier = Modifier.weight(1f),
                title = stringResource(R.string.sleep_manual_wake_time),
                time = wakeTime,
                isNight = false,
                onClick = onWakeClick
            )
        }

        if (warningText != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(cs.errorContainer.copy(alpha = 0.42f))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = cs.error,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = warningText,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onErrorContainer,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
private fun ManualTimeCard(
    modifier: Modifier,
    title: String,
    time: LocalTime,
    isNight: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val accent = if (isNight) Color(0xFF7C4DFF) else Color(0xFFFFB547)
    val glow = if (isNight) Color(0xFF3D5AFE) else Color(0xFFFFD166)
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
        label = "manualTimeCardScale"
    )

    Card(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .pressScale()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surfaceVariant.copy(alpha = 0.32f)),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.22f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            accent.copy(alpha = 0.16f),
                            glow.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isNight) Icons.Default.Bedtime else Icons.Default.WbSunny,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
            }

            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = time.formatForDisplay(),
                fontSize = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onSurface,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ManualQualityScaleSection(
    selected: ManualSleepQuality,
    onSelected: (ManualSleepQuality) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ManualPremiumCard {
        ManualSectionHeader(
            title = stringResource(R.string.sleep_manual_quality_title),
            subtitle = stringResource(R.string.sleep_manual_quality_subtitle)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ManualSleepQuality.entries.forEach { item ->
                val selectedItem = item == selected
                val height by animateFloatAsState(
                    targetValue = if (selectedItem) 54f else 34f,
                    animationSpec = tween(220, easing = EaseOutCubic),
                    label = "qualityHeight"
                )

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(height.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                if (selectedItem) cs.primary
                                else cs.surfaceVariant.copy(alpha = 0.75f)
                            )
                            .clickable { onSelected(item) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.rating.toString(),
                            color = if (selectedItem) cs.onPrimary else cs.onSurfaceVariant,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                    Text(
                        text = stringResource(item.shortLabelRes),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (selectedItem) cs.primary else cs.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ManualWakeFeelingSection(
    selected: ManualWakeFeeling,
    onSelected: (ManualWakeFeeling) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ManualPremiumCard {
        ManualSectionHeader(
            title = stringResource(R.string.sleep_manual_wake_feeling_title),
            subtitle = stringResource(R.string.sleep_manual_wake_feeling_subtitle)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ManualWakeFeeling.entries.forEach { item ->
                val isSelected = item == selected
                val scale by animateFloatAsState(
                    targetValue = if (isSelected) 1f else 0.96f,
                    animationSpec = spring(Spring.DampingRatioMediumBouncy, Spring.StiffnessMedium),
                    label = "wakeFeelingScale"
                )

                Card(
                    modifier = Modifier
                        .width(124.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .pressScale()
                        .clickable { onSelected(item) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) cs.primary.copy(alpha = 0.14f) else cs.surfaceVariant.copy(alpha = 0.35f)
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) cs.primary.copy(alpha = 0.45f) else cs.outline.copy(alpha = 0.14f)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(13.dp),
                        verticalArrangement = Arrangement.spacedBy(9.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) cs.primary.copy(alpha = 0.18f) else cs.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.rating.toString(),
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isSelected) cs.primary else cs.onSurfaceVariant
                            )
                        }

                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(item.captionRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            lineHeight = 15.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualNightInterruptionsSection(
    selected: ManualNightInterruptions,
    onSelected: (ManualNightInterruptions) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ManualPremiumCard {
        ManualSectionHeader(
            title = stringResource(R.string.sleep_manual_interruptions_title),
            subtitle = stringResource(R.string.sleep_manual_interruptions_subtitle)
        )

        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
            ManualNightInterruptions.entries.forEach { item ->
                val isSelected = item == selected
                val progress = item.level / 4f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (isSelected) cs.primary.copy(alpha = 0.11f) else cs.surfaceVariant.copy(alpha = 0.32f))
                        .clickable { onSelected(item) }
                        .padding(horizontal = 12.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = cs.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(item.captionRes),
                            style = MaterialTheme.typography.labelSmall,
                            color = cs.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.width(76.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { index ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(18.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if ((index + 1) / 4f <= progress) cs.primary
                                        else cs.outline.copy(alpha = 0.18f)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualSleepFactorsSection(
    selectedFactors: Set<ManualSleepFactor>,
    onToggle: (ManualSleepFactor) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ManualPremiumCard {
        ManualSectionHeader(
            title = stringResource(R.string.sleep_manual_factors_title),
            subtitle = stringResource(R.string.sleep_manual_factors_subtitle)
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ManualSleepFactor.entries.forEach { factor ->
                val selected = factor in selectedFactors
                FilterChip(
                    selected = selected,
                    onClick = { onToggle(factor) },
                    label = {
                        Text(
                            text = stringResource(factor.labelRes),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold
                        )
                    },
                    leadingIcon = if (selected) {
                        {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ManualOptionalNoteSection(
    value: String,
    onValueChange: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme

    ManualPremiumCard {
        ManualSectionHeader(
            title = stringResource(R.string.sleep_manual_note_title),
            subtitle = stringResource(R.string.sleep_manual_note_subtitle)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.take(160)) },
            modifier = Modifier
                .fillMaxWidth()
                .height(106.dp),
            shape = RoundedCornerShape(18.dp),
            placeholder = { Text(stringResource(R.string.sleep_manual_note_placeholder)) },
            maxLines = 3,
            textStyle = MaterialTheme.typography.bodySmall.copy(color = cs.onSurface, lineHeight = 18.sp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = cs.primary,
                unfocusedBorderColor = cs.outline.copy(alpha = 0.28f),
                focusedContainerColor = cs.surface,
                unfocusedContainerColor = cs.surface,
                cursorColor = cs.primary
            )
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = stringResource(R.string.sleep_manual_note_counter, value.length, 160),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun ManualPremiumCard(content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(15.dp),
            content = content
        )
    }
}

@Composable
private fun ManualSectionHeader(
    title: String,
    subtitle: String
) {
    val cs = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.ExtraBold,
            color = cs.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            lineHeight = 16.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ManualTimePickerDialog(
    title: String,
    initialTime: LocalTime,
    onDismiss: () -> Unit,
    onConfirm: (LocalTime) -> Unit
) {
    var selectedTime by remember(initialTime) { mutableStateOf(initialTime) }
    val cs = MaterialTheme.colorScheme
    val isBedPicker = title == stringResource(R.string.sleep_manual_bedtime)
    val accent = if (isBedPicker) Color(0xFF7C4DFF) else Color(0xFFFFB547)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedTime) }) {
                Text(
                    text = stringResource(R.string.sleep_manual_picker_confirm),
                    fontWeight = FontWeight.ExtraBold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.sleep_manual_picker_cancel))
            }
        },
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isBedPicker) Icons.Default.Bedtime else Icons.Default.WbSunny,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = title,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    accent.copy(alpha = 0.18f),
                                    cs.surfaceVariant.copy(alpha = 0.36f)
                                )
                            )
                        )
                        .padding(vertical = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedTime.formatForDisplay(),
                        fontSize = 42.sp,
                        lineHeight = 44.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface,
                        textAlign = TextAlign.Center
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ManualTimeStepperCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.sleep_manual_picker_hour),
                        value = "%02d".format(selectedTime.hour),
                        accent = accent,
                        onMinus = { selectedTime = selectedTime.minusHours(1) },
                        onPlus = { selectedTime = selectedTime.plusHours(1) }
                    )
                    ManualTimeStepperCard(
                        modifier = Modifier.weight(1f),
                        label = stringResource(R.string.sleep_manual_picker_minute),
                        value = "%02d".format(selectedTime.minute),
                        accent = accent,
                        onMinus = { selectedTime = selectedTime.minusMinutes(5) },
                        onPlus = { selectedTime = selectedTime.plusMinutes(5) }
                    )
                }
            }
        },
        shape = RoundedCornerShape(28.dp),
        containerColor = cs.surface
    )
}

@Composable
private fun ManualTimeStepperCard(
    modifier: Modifier,
    label: String,
    value: String,
    accent: Color,
    onMinus: () -> Unit,
    onPlus: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.34f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value,
            fontSize = 30.sp,
            lineHeight = 32.sp,
            color = cs.onSurface,
            fontWeight = FontWeight.ExtraBold
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ManualPickerMiniButton(text = "−", accent = accent, onClick = onMinus)
            ManualPickerMiniButton(text = "+", accent = accent, onClick = onPlus)
        }
    }
}

@Composable
private fun ManualPickerMiniButton(
    text: String,
    accent: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 34.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.16f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = accent,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp
        )
    }
}

private enum class ManualTimePickerTarget { BED, WAKE }

private enum class ManualSleepQuality(
    val rating: Int,
    val labelRes: Int,
    val shortLabelRes: Int,
    val storageName: String
) {
    RESTLESS(1, R.string.sleep_manual_quality_restless, R.string.sleep_manual_quality_short_1, "restless"),
    LIGHT(2, R.string.sleep_manual_quality_light, R.string.sleep_manual_quality_short_2, "light"),
    BALANCED(3, R.string.sleep_manual_quality_balanced, R.string.sleep_manual_quality_short_3, "balanced"),
    DEEP(4, R.string.sleep_manual_quality_deep, R.string.sleep_manual_quality_short_4, "deep"),
    EXCELLENT(5, R.string.sleep_manual_quality_excellent, R.string.sleep_manual_quality_short_5, "excellent")
}

private enum class ManualWakeFeeling(
    val rating: Int,
    val labelRes: Int,
    val captionRes: Int,
    val storageName: String
) {
    DRAINED(1, R.string.sleep_manual_feeling_drained, R.string.sleep_manual_feeling_drained_caption, "drained"),
    LOW(2, R.string.sleep_manual_feeling_low, R.string.sleep_manual_feeling_low_caption, "low"),
    BALANCED(3, R.string.sleep_manual_feeling_balanced, R.string.sleep_manual_feeling_balanced_caption, "balanced"),
    REFRESHED(4, R.string.sleep_manual_feeling_refreshed, R.string.sleep_manual_feeling_refreshed_caption, "refreshed"),
    EXCELLENT(5, R.string.sleep_manual_feeling_excellent, R.string.sleep_manual_feeling_excellent_caption, "excellent")
}

private enum class ManualNightInterruptions(
    val level: Int,
    val labelRes: Int,
    val captionRes: Int,
    val storageName: String
) {
    NONE(0, R.string.sleep_manual_interruptions_none, R.string.sleep_manual_interruptions_none_caption, "none"),
    LIGHT(1, R.string.sleep_manual_interruptions_light, R.string.sleep_manual_interruptions_light_caption, "light"),
    MODERATE(2, R.string.sleep_manual_interruptions_moderate, R.string.sleep_manual_interruptions_moderate_caption, "moderate"),
    HEAVY(4, R.string.sleep_manual_interruptions_heavy, R.string.sleep_manual_interruptions_heavy_caption, "heavy")
}

private enum class ManualSleepFactor(
    val labelRes: Int,
    val storageName: String
) {
    STRESS(R.string.sleep_manual_factor_stress, "stress"),
    CAFFEINE(R.string.sleep_manual_factor_caffeine, "caffeine"),
    SCREEN(R.string.sleep_manual_factor_screen_time, "screen_time"),
    WORKOUT(R.string.sleep_manual_factor_workout, "workout"),
    LATE_MEAL(R.string.sleep_manual_factor_late_meal, "late_meal"),
    NOISE(R.string.sleep_manual_factor_noise, "noise"),
    RELAXED(R.string.sleep_manual_factor_relaxed, "relaxed"),
    SICK(R.string.sleep_manual_factor_sick, "sick")
}

private fun calculateManualSleepDurationMinutes(
    bedTime: LocalTime,
    wakeTime: LocalTime
): Int {
    val start = bedTime.toSecondOfDay() / 60
    val end = wakeTime.toSecondOfDay() / 60
    val day = 24 * 60
    val diff = (end - start + day) % day
    return if (diff == 0) day else diff
}

private fun formatManualSleepDuration(minutes: Int): String {
    val h = minutes / 60
    val m = minutes % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

private fun buildPackedManualSleepNote(
    userNote: String,
    quality: ManualSleepQuality,
    wakeFeeling: ManualWakeFeeling,
    interruptions: ManualNightInterruptions,
    factors: Set<ManualSleepFactor>
): String {
    val factorValue = factors.joinToString("|") { it.storageName }

    return buildString {
        appendLine("quality=${quality.storageName}")
        appendLine("wake_feeling=${wakeFeeling.storageName}")
        appendLine("interruptions=${interruptions.storageName}")
        appendLine("factors=$factorValue")
        appendLine("user_note=${userNote.trim()}")
    }.trim()
}

private fun LocalTime.formatForDisplay(): String {
    val hour12 = when {
        hour == 0 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val suffix = if (hour < 12) "AM" else "PM"
    return "%02d:%02d %s".format(hour12, minute, suffix)
}

private data class ParsedManualSleepMeta(
    val quality: String?,
    val wakeFeeling: String?,
    val interruptions: String?,
    val factors: List<String>,
    val userNote: String?
)

private fun parsePackedManualSleepNote(raw: String?): ParsedManualSleepMeta {
    if (raw.isNullOrBlank()) {
        return ParsedManualSleepMeta(null, null, null, emptyList(), null)
    }

    val map = raw
        .lineSequence()
        .mapNotNull { line ->
            val idx = line.indexOf("=")
            if (idx <= 0) null else line.substring(0, idx) to line.substring(idx + 1)
        }
        .toMap()

    val factors = map["factors"]
        ?.split("|")
        ?.filter { it.isNotBlank() }
        ?: emptyList()

    return ParsedManualSleepMeta(
        quality = map["quality"],
        wakeFeeling = map["wake_feeling"],
        interruptions = map["interruptions"],
        factors = factors,
        userNote = map["user_note"]?.takeIf { it.isNotBlank() }
    )
}

@Composable
fun ManualSleepMetaBlock(rawNote: String) {
    val cs = MaterialTheme.colorScheme
    val meta = remember(rawNote) { parsePackedManualSleepNote(rawNote) }

    if (
        meta.quality == null &&
        meta.wakeFeeling == null &&
        meta.interruptions == null &&
        meta.factors.isEmpty() &&
        meta.userNote == null
    ) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.primary.copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    )
                )
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.sleep_manual_meta_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.onSurface
                    )
                    Text(
                        text = stringResource(R.string.sleep_manual_meta_subtitle),
                        style = MaterialTheme.typography.labelSmall,
                        color = cs.onSurfaceVariant
                    )
                }
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(cs.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                meta.quality?.let {
                    SleepMetaChip(
                        label = stringResource(R.string.sleep_manual_meta_quality),
                        value = manualQualityDisplayLabel(it)
                    )
                }
                meta.wakeFeeling?.let {
                    SleepMetaChip(
                        label = stringResource(R.string.sleep_manual_meta_wake_feeling),
                        value = manualWakeFeelingDisplayLabel(it)
                    )
                }
                meta.interruptions?.let {
                    SleepMetaChip(
                        label = stringResource(R.string.sleep_manual_meta_interruptions),
                        value = manualInterruptionsDisplayLabel(it)
                    )
                }
            }

            if (meta.factors.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sleep_manual_meta_factors),
                        style = MaterialTheme.typography.labelMedium,
                        color = cs.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        meta.factors.forEach { factor ->
                            Text(
                                text = manualFactorDisplayLabel(factor),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(cs.surfaceVariant.copy(alpha = 0.46f))
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = cs.onSurface,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            meta.userNote?.let {
                Text(
                    text = it,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(cs.surfaceVariant.copy(alpha = 0.32f))
                        .padding(14.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurface,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun SleepMetaChip(
    label: String,
    value: String
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .widthIn(min = 96.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cs.primary.copy(alpha = 0.10f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = cs.onSurface,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun manualQualityDisplayLabel(value: String): String = when (value) {
    "restless" -> stringResource(R.string.sleep_manual_quality_restless)
    "light" -> stringResource(R.string.sleep_manual_quality_light)
    "balanced" -> stringResource(R.string.sleep_manual_quality_balanced)
    "deep" -> stringResource(R.string.sleep_manual_quality_deep)
    "excellent" -> stringResource(R.string.sleep_manual_quality_excellent)
    else -> value.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun manualWakeFeelingDisplayLabel(value: String): String = when (value) {
    "drained" -> stringResource(R.string.sleep_manual_feeling_drained)
    "low" -> stringResource(R.string.sleep_manual_feeling_low)
    "balanced" -> stringResource(R.string.sleep_manual_feeling_balanced)
    "refreshed" -> stringResource(R.string.sleep_manual_feeling_refreshed)
    "excellent" -> stringResource(R.string.sleep_manual_feeling_excellent)
    else -> value.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun manualInterruptionsDisplayLabel(value: String): String = when (value) {
    "none" -> stringResource(R.string.sleep_manual_interruptions_none)
    "light" -> stringResource(R.string.sleep_manual_interruptions_light)
    "moderate" -> stringResource(R.string.sleep_manual_interruptions_moderate)
    "heavy" -> stringResource(R.string.sleep_manual_interruptions_heavy)
    else -> value.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

@Composable
private fun manualFactorDisplayLabel(value: String): String = when (value) {
    "stress" -> stringResource(R.string.sleep_manual_factor_stress)
    "caffeine" -> stringResource(R.string.sleep_manual_factor_caffeine)
    "screen_time" -> stringResource(R.string.sleep_manual_factor_screen_time)
    "workout" -> stringResource(R.string.sleep_manual_factor_workout)
    "late_meal" -> stringResource(R.string.sleep_manual_factor_late_meal)
    "noise" -> stringResource(R.string.sleep_manual_factor_noise)
    "relaxed" -> stringResource(R.string.sleep_manual_factor_relaxed)
    "sick" -> stringResource(R.string.sleep_manual_factor_sick)
    else -> value.replace('_', ' ').replaceFirstChar { it.uppercase() }
}

// ── Premium history row ───────────────────────────────────────────────────────

@Composable
fun PremiumHistoryRow(day: SleepDay, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        border = BorderStroke(1.dp, cs.outlineVariant.copy(0.3f))
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text(day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale.getDefault()), fontWeight = FontWeight.Bold)
                Text(
                    stringResource(
                        R.string.sleep_time_range_dash,
                        day.date.dayOfMonth.toString(),
                        day.date.month.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault())
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.sleep_duration_preview, day.hours, day.minutes), fontWeight = FontWeight.ExtraBold, color = cs.primary)
                Text(
                    if (day.sessions.size > 1) {
                        stringResource(R.string.sleep_sessions_count, day.sessions.size)
                    } else {
                        stringResource(R.string.sleep_time_range_dash, day.bedTimeStr, day.wakeTimeStr)
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant
                )
            }
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(8.dp)).background(scoreColor(day.sleepScore ?: 0, cs).copy(0.1f)), contentAlignment = Alignment.Center) {
                Text(day.sleepScore?.toString() ?: stringResource(R.string.sleep_no_value), fontWeight = FontWeight.Bold, color = scoreColor(day.sleepScore ?: 0, cs))
            }
        }
    }
}

private data class StepForgeSleepSurfaceTokens(
    val pageBg: Color,
    val cardBg: Color,
    val elevatedCardBg: Color,
    val subtleCardBg: Color,
    val border: Color,
    val shadow: androidx.compose.ui.unit.Dp,
    val title: Color,
    val body: Color,
    val muted: Color,
    val accent: Color
)

@Composable
private fun rememberStepForgeSleepSurfaceTokens(): StepForgeSleepSurfaceTokens {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    return if (isDark) {
        StepForgeSleepSurfaceTokens(
            pageBg = cs.background,
            cardBg = Color(0xFF101318),
            elevatedCardBg = Color(0xFF12161C),
            subtleCardBg = Color(0xFF181D25),
            border = Color.White.copy(alpha = 0.06f),
            shadow = 0.dp,
            title = Color.White,
            body = Color(0xFFE7ECF4),
            muted = Color(0xFF98A3B5),
            accent = Color(0xFF00EAF2)
        )
    } else {
        StepForgeSleepSurfaceTokens(
            pageBg = Color(0xFFF5F8FC),
            cardBg = Color.White,
            elevatedCardBg = Color(0xFFFFFFFF),
            subtleCardBg = Color(0xFFF0F6F8),
            border = Color(0xFFE1E7EF),
            shadow = 10.dp,
            title = Color(0xFF111827),
            body = Color(0xFF1F2937),
            muted = Color(0xFF667085),
            accent = Color(0xFF12B8B4)
        )
    }
}

@Composable
private fun StepForgeSleepCard(
    modifier: Modifier = Modifier,
    tokens: StepForgeSleepSurfaceTokens = rememberStepForgeSleepSurfaceTokens(),
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = tokens.shadow,
                shape = shape,
                ambientColor = Color.Black.copy(alpha = 0.10f),
                spotColor = Color.Black.copy(alpha = 0.16f)
            )
            .border(
                width = 1.dp,
                color = tokens.border,
                shape = shape
            ),
        shape = shape,
        color = tokens.cardBg,
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        content = {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    )
}

// ── Color helpers ─────────────────────────────────────────────────────────────

fun scoreColor(score: Int, cs: ColorScheme): Color = when {
    score >= 85 -> cs.primary
    score >= 70 -> cs.secondary
    score >= 55 -> cs.tertiary
    else        -> cs.error
}

fun stageColor(type: StageType, cs: ColorScheme): Color = when (type) {
    StageType.DEEP  -> cs.primary
    StageType.REM   -> cs.tertiary
    StageType.LIGHT -> cs.secondary.copy(alpha = 0.6f)
    StageType.AWAKE -> cs.error
}