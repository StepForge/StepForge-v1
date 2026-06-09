package com.example.stepforge.ui.workouts

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
internal fun NeonCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    glow: Boolean = false,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val shape = RoundedCornerShape(26.dp)
    val bg = if (isDark) {
        Brush.linearGradient(
            listOf(
                Color(0xFF11161E).copy(alpha = 0.98f),
                Color(0xFF090D12).copy(alpha = 0.98f),
                Color(0xFF111820).copy(alpha = 0.95f)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                Color(0xFFF2F8FA)
            )
        )
    }
    val border = if (glow) cs.primary.copy(alpha = if (isDark) 0.40f else 0.26f) else cs.outline.copy(alpha = if (isDark) 0.12f else 0.14f)
    val clickModifier = if (onClick != null) {
        Modifier.clickable(
            indication = null,
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick
        )
    } else Modifier

    Box(
        modifier = modifier
            .shadow(if (isDark) 10.dp else 8.dp, shape, clip = false)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
            .then(clickModifier)
    ) {
        if (glow) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.radialGradient(
                            listOf(
                                cs.primary.copy(alpha = if (isDark) 0.11f else 0.055f),
                                Color.Transparent
                            ),
                            center = Offset(0.25f, 0.0f),
                            radius = 850f
                        )
                    )
            )
        }
        content()
    }
}

@Composable
internal fun AnimatedLineAreaChart(
    values: List<Float>,
    days: List<String>,
    selectedIndex: Int,
    todayIndex: Int = -1,
    tooltip: String,
    onIndexSelected: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1100, easing = FastOutSlowInEasing),
        label = "workoutsLineChartProgress"
    )
    val safeValues = if (values.isEmpty()) List(7) { 0f } else values.take(7).let {
        if (it.size == 7) it else it + List(7 - it.size) { 0f }
    }
    val axisMax = niceChartAxisMax(safeValues.maxOrNull() ?: 0f)
    val safeSelectedIndex = selectedIndex.coerceIn(0, safeValues.lastIndex)
    val selectedValue = safeValues.getOrElse(safeSelectedIndex) { 0f }
    val safeDayLabels = days.take(7).let {
        if (it.size == 7) it else it + List(7 - it.size) { "" }
    }

    BoxWithConstraints(modifier = modifier) {
        val axisLabelWidth = 54.dp
        val leftPad = axisLabelWidth + 12.dp
        val rightPad = 8.dp
        val topPad = 24.dp
        val bottomPad = 38.dp
        val chartW = maxWidth - leftPad - rightPad
        val chartH = maxHeight - topPad - bottomPad
        val slotW = (chartW.value / safeValues.size.coerceAtLeast(1)).dp
        val selectedX = (leftPad.value + slotW.value * (safeSelectedIndex + 0.5f)).dp
        val selectedY = topPad + chartH - chartH * (selectedValue / axisMax).coerceIn(0f, 1f)
        val tooltipWidth = 82.dp
        val tooltipHeight = 30.dp
        val tooltipX = (selectedX - tooltipWidth / 2).coerceIn(0.dp, maxWidth - tooltipWidth)
        val tooltipY = (selectedY - 48.dp).coerceIn(0.dp, maxHeight - tooltipHeight - 8.dp)
        val axisLabels = listOf(axisMax, axisMax * 0.75f, axisMax * 0.5f, axisMax * 0.25f, 0f)
        val axisLabelHeight = 16.dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val left = leftPad.toPx()
            val right = rightPad.toPx()
            val top = topPad.toPx()
            val bottom = bottomPad.toPx()
            val chartWidth = size.width - left - right
            val chartHeight = size.height - top - bottom
            val slotWidth = chartWidth / safeValues.size.coerceAtLeast(1)
            val baselineY = top + chartHeight
            val chartRight = left + chartWidth
            val points = safeValues.mapIndexed { index, value ->
                val x = left + slotWidth * (index + 0.5f)
                val y = baselineY - (value / axisMax).coerceIn(0f, 1f) * chartHeight
                Offset(x, y)
            }
            val linePoints = buildList {
                add(Offset(left, baselineY))
                addAll(points)
                add(Offset(chartRight, baselineY))
            }

            repeat(5) { i ->
                val y = top + chartHeight * (i / 4f)
                drawLine(
                    color = cs.onSurface.copy(alpha = 0.10f),
                    start = Offset(left, y),
                    end = Offset(size.width - right, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            drawLine(
                color = cs.onSurface.copy(alpha = 0.12f),
                start = Offset(left, top + chartHeight),
                end = Offset(size.width - right, top + chartHeight),
                strokeWidth = 1.dp.toPx()
            )

            val areaPath = Path()
            if (linePoints.size >= 2) {
                areaPath.moveTo(left, baselineY)
                linePoints.forEach { point ->
                    areaPath.lineTo(point.x, point.y)
                }
                areaPath.lineTo(chartRight, baselineY)
                areaPath.close()
                clipRect(
                    left = left,
                    top = top,
                    right = left + chartWidth * progress,
                    bottom = baselineY
                ) {
                    drawPath(
                        path = areaPath,
                        brush = Brush.verticalGradient(
                            listOf(cs.primary.copy(alpha = 0.26f), Color.Transparent),
                            startY = top,
                            endY = baselineY
                        )
                    )
                }
            }

            if (linePoints.size >= 2) {
                val linePath = Path().apply {
                    moveTo(linePoints.first().x, linePoints.first().y)
                    for (i in 1..linePoints.lastIndex) {
                        val prev = linePoints[i - 1]
                        val cur = linePoints[i]
                        val midX = (prev.x + cur.x) / 2f
                        cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
                    }
                }
                clipRect(
                    left = left,
                    top = top,
                    right = left + chartWidth * progress,
                    bottom = baselineY
                ) {
                    drawPath(
                        path = linePath,
                        color = cs.primary.copy(alpha = 0.34f),
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawPath(
                        path = linePath,
                        brush = Brush.horizontalGradient(
                            listOf(Color(0xFF00FFA3), Color(0xFF00F5FF), Color(0xFF32A7FF))
                        ),
                        style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
            }

            points.forEachIndexed { index, point ->
                if (index / points.lastIndex.coerceAtLeast(1).toFloat() <= progress + 0.01f) {
                    drawCircle(color = Color.White.copy(alpha = 0.88f), radius = 4.4.dp.toPx(), center = point)
                    drawCircle(color = cs.primary, radius = 2.5.dp.toPx(), center = point)
                }
            }

            points.getOrNull(safeSelectedIndex)?.let { selected ->
                drawLine(
                    color = cs.primary.copy(alpha = 0.48f),
                    start = Offset(selected.x, top),
                    end = Offset(selected.x, top + chartHeight),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }

        axisLabels.forEachIndexed { index, label ->
            val labelY = (topPad.value + chartH.value * (index / 4f) - axisLabelHeight.value / 2f).dp
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 0.dp, y = labelY)
                    .width(axisLabelWidth)
                    .height(axisLabelHeight),
                contentAlignment = Alignment.CenterEnd
            ) {
                androidx.compose.material3.Text(
                    text = formatAxisLabel(label),
                    color = cs.onSurfaceVariant,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = leftPad, top = topPad, end = rightPad)
                .height((chartH.value + bottomPad.value).dp)
                .fillMaxWidth()
        ) {
            safeDayLabels.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onIndexSelected(index) }
                )
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = leftPad, end = rightPad)
                .fillMaxWidth()
                .height(26.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            safeDayLabels.forEachIndexed { index, day ->
                val isSelectedDay = index == safeSelectedIndex
                val isToday = index == todayIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) { onIndexSelected(index) },
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.material3.Text(
                        text = day,
                        color = when {
                            isSelectedDay -> cs.primary
                            isToday -> Color(0xFF00FFA3)
                            else -> cs.onSurfaceVariant
                        },
                        fontSize = 10.sp,
                        fontWeight = if (isSelectedDay || isToday) FontWeight.ExtraBold else FontWeight.SemiBold
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset(x = tooltipX, y = tooltipY)
                .width(tooltipWidth)
                .height(tooltipHeight)
                .clip(RoundedCornerShape(9.dp))
                .background(Color(0xFF071921).copy(alpha = 0.96f))
                .border(1.dp, cs.primary.copy(alpha = 0.42f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Text(
                text = tooltip,
                color = cs.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

private fun niceChartAxisMax(maxValue: Float): Float {
    val padded = max(maxValue * 1.12f, 1f)
    return when {
        padded <= 80f -> 80f
        padded <= 120f -> 120f
        padded <= 160f -> 160f
        padded <= 250f -> 250f
        padded <= 500f -> 500f
        padded <= 1000f -> 1000f
        else -> ((padded / 500f).toInt() + 1) * 500f
    }
}

private fun formatAxisLabel(value: Float): String {
    return value.toInt().toString()
}

@Composable
internal fun ProgressRing(
    progress: Float,
    size: Dp,
    stroke: Dp,
    center: @Composable () -> Unit
) {
    val animated by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(950, easing = FastOutSlowInEasing),
        label = "ringProgress"
    )
    val cs = MaterialTheme.colorScheme
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = stroke.toPx()
            val arcDimension = this.size.minDimension - strokePx
            val arcSize = Size(arcDimension, arcDimension)
            val topLeft = Offset((this.size.width - arcSize.width) / 2f, (this.size.height - arcSize.height) / 2f)
            drawArc(
                color = cs.onSurface.copy(alpha = 0.09f),
                startAngle = -220f,
                sweepAngle = 260f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(Color(0xFF00FFA3), Color(0xFF00F5FF), Color(0xFF35A8FF), Color(0xFF00FFA3))),
                startAngle = -220f,
                sweepAngle = 260f * animated,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
        }
        center()
    }
}

@Composable
internal fun MiniBarChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    blue: Boolean = false,
    dayMode: Boolean = false
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "miniBarProgress"
    )
    val safe = if (values.isEmpty()) List(5) { 0f } else values.takeLast(7)
    val maxValue = safe.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    Canvas(modifier = modifier) {
        val usable = if (dayMode) safe.takeLast(5) else safe
        val gap = if (dayMode) 7.dp.toPx() else 5.dp.toPx()
        val barW = ((size.width - gap * (usable.size - 1)) / usable.size).coerceAtLeast(3.dp.toPx())
        usable.forEachIndexed { index, value ->
            val ratio = (value / maxValue).coerceIn(0f, 1f)
            val minHeight = if (dayMode) size.height * 0.10f else size.height * 0.12f
            val h = max(minHeight, size.height * ratio) * progress
            val x = index * (barW + gap)
            val y = size.height - h
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        if (blue) Color(0xFF48A7FF) else Color(0xFF28EEFF),
                        if (blue) Color(0xFF163A7A).copy(alpha = 0.20f) else Color(0xFF00D9A6).copy(alpha = 0.20f)
                    ),
                    startY = y,
                    endY = size.height
                ),
                topLeft = Offset(x, y),
                size = Size(barW, h),
                cornerRadius = CornerRadius(barW / 2f, barW / 2f)
            )
        }
    }
}

@Composable
internal fun MiniRoutePath(modifier: Modifier = Modifier) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "routeProgress"
    )
    val cs = MaterialTheme.colorScheme
    Canvas(modifier = modifier) {
        val startPoint = Offset(size.width * 0.12f, size.height * 0.70f)
        val endPoint = Offset(size.width * 0.88f, size.height * 0.28f)
        val path = Path().apply {
            moveTo(startPoint.x, startPoint.y)
            cubicTo(size.width * 0.25f, size.height * 0.50f, size.width * 0.35f, size.height * 0.80f, size.width * 0.50f, size.height * 0.58f)
            cubicTo(size.width * 0.64f, size.height * 0.32f, size.width * 0.72f, size.height * 0.58f, endPoint.x, endPoint.y)
        }
        clipRect(right = size.width * progress) {
            drawPath(path, color = cs.primary.copy(alpha = 0.24f), style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round))
            drawPath(path, brush = Brush.horizontalGradient(listOf(Color(0xFF00FFA3), Color(0xFF00F5FF))), style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round))
        }
        drawCircle(Color(0xFF00FFA3), radius = 6.dp.toPx(), center = startPoint)
        drawCircle(Color(0xFF00F5FF), radius = 6.dp.toPx(), center = endPoint)
    }
}

@Composable
internal fun DonutChart(
    slices: List<WorkoutZoneSlice>,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(1050, easing = FastOutSlowInEasing),
        label = "donutProgress"
    )
    Canvas(modifier = modifier) {
        val stroke = 18.dp.toPx()
        val rect = Rect(
            left = stroke / 2f,
            top = stroke / 2f,
            right = size.width - stroke / 2f,
            bottom = size.height - stroke / 2f
        )
        drawArc(
            color = Color.White.copy(alpha = 0.07f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = rect.topLeft,
            size = rect.size,
            style = Stroke(width = stroke, cap = StrokeCap.Butt)
        )
        var start = -90f
        slices.forEach { slice ->
            val sweep = 360f * (slice.percent / 100f) * progress
            drawArc(
                color = zoneColor(slice.labelType),
                startAngle = start,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = rect.topLeft,
                size = rect.size,
                style = Stroke(width = stroke, cap = StrokeCap.Butt)
            )
            start += sweep
        }
    }
}

internal fun zoneColor(type: ZoneLabelType): Color = when (type) {
    ZoneLabelType.PEAK -> Color(0xFFFF5FA2)
    ZoneLabelType.CARDIO -> Color(0xFF7C4DFF)
    ZoneLabelType.FAT_BURN -> Color(0xFF00D9FF)
    ZoneLabelType.WARM_UP -> Color(0xFF00D084)
}

@Composable
internal fun HeatmapDots(
    week: List<WorkoutDaySummary>,
    selectedDate: java.time.LocalDate = java.time.LocalDate.now(),
    todayDate: java.time.LocalDate = java.time.LocalDate.now(),
    onDaySelected: (java.time.LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = modifier,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        week.take(7).forEach { day ->
            val isSelected = day.date == selectedDate
            val isToday = day.date == todayDate
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) { onDaySelected(day.date) }
                    .padding(vertical = 2.dp)
            ) {
                androidx.compose.material3.Text(
                    text = day.date.dayShort(),
                    color = when {
                        isSelected -> MaterialTheme.colorScheme.primary
                        isToday -> Color(0xFF00FFA3)
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontSize = 9.sp,
                    fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.Bold,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(width = 32.dp, height = 34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (isSelected || isToday) 1.dp else 0.dp,
                            color = when {
                                isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.68f)
                                isToday -> Color(0xFF00FFA3).copy(alpha = 0.46f)
                                else -> Color.Transparent
                            },
                            shape = RoundedCornerShape(10.dp)
                        )
                        .padding(horizontal = 4.dp, vertical = 5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(width = 24.dp, height = 24.dp)) {
                        val maxDots = 9
                        val activeDots = when {
                            day.future -> 0
                            day.durationMinutes <= 0 -> 1
                            else -> ((day.durationMinutes / 8f).toInt() + 2).coerceIn(1, maxDots)
                        }
                        val dot = 5.dp.toPx()
                        val gap = 4.dp.toPx()
                        repeat(maxDots) { index ->
                            val col = index % 3
                            val row = index / 3
                            val alpha = if (index < activeDots) {
                                0.35f + (index.coerceAtMost(activeDots) / maxDots.toFloat()) * 0.55f
                            } else {
                                0.08f
                            }
                            drawCircle(
                                color = primary.copy(alpha = alpha),
                                radius = dot / 2f,
                                center = Offset(
                                    x = col * (dot + gap) + dot / 2f,
                                    y = row * (dot + gap) + dot / 2f
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
internal fun MiniWaveGraph(
    values: List<Float>,
    kind: WorkoutKind,
    modifier: Modifier = Modifier
) {
    val progress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "waveProgress"
    )
    val color = when (kind) {
        WorkoutKind.STRENGTH -> Color(0xFF00D084)
        WorkoutKind.CYCLING -> Color(0xFF00F5FF)
        else -> Color(0xFF00F5FF)
    }
    Canvas(modifier = modifier) {
        if (values.isEmpty()) return@Canvas

        val minValue = values.minOrNull() ?: 0f
        val maxValue = values.maxOrNull() ?: 1f
        val range = (maxValue - minValue).coerceAtLeast(0.12f)
        val leftPad = size.width * 0.03f
        val rightPad = size.width * 0.03f
        val topPad = size.height * 0.16f
        val bottomPad = size.height * 0.18f
        val usableWidth = (size.width - leftPad - rightPad).coerceAtLeast(1f)
        val usableHeight = (size.height - topPad - bottomPad).coerceAtLeast(1f)

        val points = values.mapIndexed { index, value ->
            val normalized = ((value - minValue) / range).coerceIn(0f, 1f)
            Offset(
                x = leftPad + usableWidth * (index / values.lastIndex.coerceAtLeast(1).toFloat()),
                y = size.height - bottomPad - usableHeight * normalized
            )
        }

        val linePath = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (i in 1..points.lastIndex) {
                val prev = points[i - 1]
                val cur = points[i]
                val midX = (prev.x + cur.x) / 2f
                cubicTo(midX, prev.y, midX, cur.y, cur.x, cur.y)
            }
        }

        val fillPath = Path().apply {
            addPath(linePath)
            lineTo(points.last().x, size.height - bottomPad * 0.35f)
            lineTo(points.first().x, size.height - bottomPad * 0.35f)
            close()
        }

        clipRect(right = size.width * progress) {
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    listOf(color.copy(alpha = 0.18f), Color.Transparent),
                    startY = topPad,
                    endY = size.height
                )
            )
            drawPath(
                path = linePath,
                color = color.copy(alpha = 0.16f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = linePath,
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.88f), Color.White.copy(alpha = 0.94f), color)
                ),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )
        }

        points.forEachIndexed { index, point ->
            if (index == 0 || index == points.lastIndex || values[index] == maxValue) {
                drawCircle(color = color.copy(alpha = 0.18f), radius = 4.dp.toPx(), center = point)
                drawCircle(color = color, radius = 2.dp.toPx(), center = point)
            }
        }
    }
}
