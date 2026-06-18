package com.example.stepforge.ui.progress

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import kotlin.math.ceil
import kotlin.math.roundToInt

@Composable
internal fun CircularGoalRing(
    percent: Int,
    goalText: String,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val colors = progressPalette()
    Box(
        modifier = modifier.padding((3f * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = (7.6f * scale).dp.toPx().coerceAtLeast(6f)
            val ringSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = colors.ringTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(colors.cyan, colors.mint, colors.cyan)),
                startAngle = -88f,
                sweepAngle = (percent.coerceAtLeast(0).coerceAtMost(160) / 160f) * 330f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(R.drawable.progress_shoe_icon),
                contentDescription = null,
                colorFilter = ColorFilter.tint(colors.cyan),
                modifier = Modifier.size((17f * scale).dp)
            )
            Text(
                text = "$percent%",
                color = colors.textMain,
                fontSize = (27f * scale).sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
            Text(
                text = goalText,
                color = colors.textMuted,
                fontSize = (9.2f * scale).sp,
                lineHeight = (10.7f * scale).sp,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
internal fun WeeklyProgressRing(
    active: Boolean,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val colors = progressPalette()
    Box(
        modifier = modifier.size((52f * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = (5.5f * scale).dp.toPx().coerceAtLeast(4.6f)
            val ringSize = Size(size.width - stroke, size.height - stroke)
            val topLeft = Offset(stroke / 2f, stroke / 2f)
            drawArc(
                color = colors.ringTrack,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
            drawArc(
                brush = Brush.sweepGradient(listOf(colors.mint, colors.cyan, colors.mint)),
                startAngle = -90f,
                sweepAngle = if (active) 320f else 40f,
                useCenter = false,
                topLeft = topLeft,
                size = ringSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Image(
            painter = painterResource(R.drawable.progress_fire_icon),
            contentDescription = null,
            modifier = Modifier.size((24f * scale).dp)
        )
    }
}

@Composable
internal fun ProgressLineChart(
    points: List<ChartPoint>,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val colors = progressPalette()
    val safePoints = remember(points) { if (points.isEmpty()) listOf(ChartPoint("", 0)) else points }
    val maxSteps = safePoints.maxOfOrNull { it.steps }?.coerceAtLeast(1) ?: 1
    val chartMax = remember(maxSteps) { niceChartMax(maxSteps) }
    val yLabels = remember(chartMax) { axisValues(chartMax) }
    var selectedIndex by remember(safePoints) {
        mutableIntStateOf(safePoints.indices.maxByOrNull { safePoints[it].steps } ?: 0)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height((164f * scale).dp),
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .width((36f * scale).dp)
                    .height((150f * scale).dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                yLabels.forEach { value ->
                    Text(
                        text = compactAxis(value),
                        color = colors.textFaint,
                        fontSize = (10.6f * scale).sp,
                        maxLines = 1
                    )
                }
            }

            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height((150f * scale).dp)
                    .pointerInput(safePoints, chartMax) {
                        detectTapGestures { tap ->
                            if (safePoints.isNotEmpty()) {
                                val slotWidth = size.width / safePoints.size.toFloat()
                                selectedIndex = (tap.x / slotWidth).toInt().coerceIn(0, safePoints.lastIndex)
                            }
                        }
                    }
            ) {
                val chartHeight = size.height
                val chartWidth = size.width
                val topPad = 10f * density
                val bottomPad = 18f * density
                val usableHeight = chartHeight - topPad - bottomPad
                val gridColor = colors.chartGrid
                val accent = colors.cyan
                val pointRadius = (4.2f * scale).dp.toPx().coerceAtLeast(3.4f)

                yLabels.forEach { value ->
                    val ratio = value.toFloat() / chartMax.toFloat()
                    val y = topPad + usableHeight - (usableHeight * ratio)
                    drawLine(
                        color = gridColor,
                        start = Offset(0f, y),
                        end = Offset(chartWidth, y),
                        strokeWidth = 1.15f
                    )
                }

                val slotWidth = chartWidth / safePoints.size.toFloat()
                fun pointOffset(index: Int, steps: Int): Offset {
                    val x = slotWidth * (index + 0.5f)
                    val ratio = (steps.toFloat() / chartMax.toFloat()).coerceIn(0f, 1f)
                    val y = topPad + usableHeight - (usableHeight * ratio)
                    return Offset(x, y)
                }

                val offsets = safePoints.mapIndexed { index, point -> pointOffset(index, point.steps) }
                val linePath = smoothPath(offsets)
                val areaPath = Path().apply {
                    if (offsets.isNotEmpty()) {
                        moveTo(offsets.first().x, chartHeight - bottomPad)
                        lineTo(offsets.first().x, offsets.first().y)
                        appendSmoothSegments(this, offsets)
                        lineTo(offsets.last().x, chartHeight - bottomPad)
                        close()
                    }
                }

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            colors.cyan.copy(alpha = 0.32f),
                            colors.cyan.copy(alpha = 0.03f)
                        )
                    )
                )
                drawPath(
                    path = linePath,
                    color = accent,
                    style = Stroke(
                        width = (3.1f * scale).dp.toPx().coerceAtLeast(2.6f),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round
                    )
                )

                offsets.forEachIndexed { index, p ->
                    val selected = index == selectedIndex
                    drawCircle(
                        color = colors.cyanGlow.copy(alpha = if (selected) 0.42f else 0.24f),
                        radius = pointRadius * if (selected) 2.8f else 2.1f,
                        center = p
                    )
                    drawCircle(color = accent, radius = pointRadius, center = p)
                }

                val selectedPoint = safePoints[selectedIndex.coerceIn(0, safePoints.lastIndex)]
                val hp = offsets[selectedIndex.coerceIn(0, offsets.lastIndex)]
                val bubbleText = compactValue(selectedPoint.steps)
                val bubbleW = ((bubbleText.length * 8.6f + 24f) * scale).dp.toPx().coerceIn((42f * scale).dp.toPx(), (104f * scale).dp.toPx())
                val bubbleH = (30f * scale).dp.toPx().coerceAtLeast(26f)
                val bx = (hp.x - bubbleW / 2f).coerceIn(0f, chartWidth - bubbleW)
                val by = (hp.y - bubbleH - (24f * scale)).coerceIn(0f, chartHeight - bubbleH)
                drawRoundRect(
                    color = colors.bubbleBg,
                    topLeft = Offset(bx, by),
                    size = Size(bubbleW, bubbleH),
                    cornerRadius = CornerRadius(9f * scale, 9f * scale)
                )
                drawRoundRect(
                    color = accent.copy(alpha = 0.55f),
                    topLeft = Offset(bx, by),
                    size = Size(bubbleW, bubbleH),
                    cornerRadius = CornerRadius(9f * scale, 9f * scale),
                    style = Stroke(width = 1.1f)
                )
                drawContext.canvas.nativeCanvas.drawText(
                    bubbleText,
                    bx + bubbleW / 2f,
                    by + bubbleH / 2f + (4.2f * scale),
                    Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = colors.nativeBubbleText
                        textAlign = Paint.Align.CENTER
                        textSize = (12.2f * scale).sp.toPx()
                        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                    }
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (36f * scale).dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            safePoints.forEach { point ->
                Text(
                    text = point.label,
                    color = colors.textMuted,
                    fontSize = (10.8f * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

private fun smoothPath(points: List<Offset>): Path {
    return Path().apply {
        if (points.isEmpty()) return@apply
        moveTo(points.first().x, points.first().y)
        appendSmoothSegments(this, points)
    }
}

private fun appendSmoothSegments(path: Path, points: List<Offset>) {
    if (points.size < 2) return
    for (index in 1 until points.size) {
        val previous = points[index - 1]
        val current = points[index]
        val midX = (previous.x + current.x) / 2f
        path.cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
    }
}

@Composable
internal fun MiniSparkline(
    values: List<Int>,
    color: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val safe = if (values.isEmpty()) listOf(0, 0, 0) else values
    Canvas(modifier = modifier.height((24f * scale).dp).fillMaxWidth()) {
        val max = safe.maxOrNull()?.coerceAtLeast(1) ?: 1
        val min = safe.minOrNull() ?: 0
        val range = (max - min).coerceAtLeast(1)
        val step = if (safe.size > 1) size.width / (safe.size - 1) else size.width
        val offsets = safe.mapIndexed { index, value ->
            val x = if (safe.size > 1) index * step else size.width / 2f
            val ratio = (value - min).toFloat() / range.toFloat()
            val y = size.height - (size.height * ratio).coerceIn(0f, size.height)
            Offset(x, y)
        }
        val path = smoothPath(offsets)
        drawPath(path, color = color, style = Stroke(width = (2f * scale).dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        val lastX = if (safe.size > 1) (safe.size - 1) * step else size.width / 2f
        val lastRatio = (safe.last() - min).toFloat() / range.toFloat()
        val lastY = size.height - (size.height * lastRatio).coerceIn(0f, size.height)
        drawCircle(color = color, radius = (3.3f * scale).dp.toPx(), center = Offset(lastX, lastY))
    }
}

@Composable
internal fun ProgressBarLine(
    percent: Int,
    color: Color,
    scale: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height((5.5f * scale).dp)
            .clip(CircleShape)
            .background(progressPalette().progressTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                .height((5.5f * scale).dp)
                .clip(CircleShape)
                .background(Brush.horizontalGradient(listOf(color.copy(alpha = 0.65f), color)))
        )
    }
}

private fun niceChartMax(maxSteps: Int): Int {
    val raw = (maxSteps * 1.12f).roundToInt().coerceAtLeast(10_000)
    val bucket = when {
        raw <= 25_000 -> 5_000
        raw <= 100_000 -> 25_000
        raw <= 500_000 -> 100_000
        raw <= 1_000_000 -> 250_000
        else -> 500_000
    }
    return (ceil(raw / bucket.toFloat()).toInt() * bucket).coerceAtLeast(bucket)
}

private fun axisValues(chartMax: Int): List<Int> {
    val step = when {
        chartMax <= 25_000 -> 5_000
        chartMax <= 100_000 -> 25_000
        chartMax <= 500_000 -> 100_000
        chartMax <= 1_000_000 -> 250_000
        else -> 500_000
    }
    val values = generateSequence(chartMax) { previous ->
        (previous - step).takeIf { it >= 0 }
    }.toList()
    return if (values.lastOrNull() == 0) values else values + 0
}

private fun compactAxis(value: Int): String = when {
    value >= 1_000_000 -> "${value / 1_000_000}M"
    value >= 1_000 -> "${value / 1_000}K"
    else -> value.toString()
}

private fun compactValue(value: Int): String = when {
    value >= 1_000_000 -> "${formatDecimal(value / 1_000_000f, 2)}M"
    value >= 100_000 -> "${value / 1_000}K"
    else -> formatNumber(value)
}

@Immutable
internal data class ProgressPalette(
    val backgroundTop: Color,
    val backgroundBottom: Color,
    val cardTop: Color,
    val cardBottom: Color,
    val cardStroke: Color,
    val cyan: Color,
    val cyanGlow: Color,
    val mint: Color,
    val orange: Color,
    val green: Color,
    val textMain: Color,
    val textMuted: Color,
    val textFaint: Color,
    val selectedText: Color,
    val ringTrack: Color,
    val chartGrid: Color,
    val bubbleBg: Color,
    val progressTrack: Color,
    val nativeBubbleText: Int,
    val isDark: Boolean
)

@Composable
internal fun progressPalette(): ProgressPalette {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    return if (isDark) {
        ProgressPalette(
            backgroundTop = Color(0xFF020711),
            backgroundBottom = Color(0xFF00020A),
            cardTop = Color(0xFF071528),
            cardBottom = Color(0xFF030A16),
            cardStroke = Color(0xFF1B3550),
            cyan = Color(0xFF48F1FF),
            cyanGlow = Color(0xFF10D9FF),
            mint = Color(0xFF00FFB2),
            orange = Color(0xFFFF8A19),
            green = Color(0xFF00F5A0),
            textMain = Color(0xFFF7FBFF),
            textMuted = Color(0xFFA7B2C4),
            textFaint = Color(0xFF657187),
            selectedText = Color(0xFF00131A),
            ringTrack = Color.White.copy(alpha = 0.10f),
            chartGrid = Color.White.copy(alpha = 0.08f),
            bubbleBg = Color(0xFF083D46).copy(alpha = 0.96f),
            progressTrack = Color.White.copy(alpha = 0.10f),
            nativeBubbleText = android.graphics.Color.rgb(78, 245, 255),
            isDark = true
        )
    } else {
        ProgressPalette(
            backgroundTop = Color(0xFFF6FBFF),
            backgroundBottom = Color(0xFFEAF3FA),
            cardTop = Color(0xFFFFFFFF),
            cardBottom = Color(0xFFF1F8FE),
            cardStroke = Color(0xFFBFD6E9),
            cyan = Color(0xFF00AFC8),
            cyanGlow = Color(0xFF10C7DF),
            mint = Color(0xFF00B884),
            orange = Color(0xFFE97814),
            green = Color(0xFF00A86B),
            textMain = Color(0xFF06111F),
            textMuted = Color(0xFF5B687A),
            textFaint = Color(0xFF8793A5),
            selectedText = Color(0xFFFFFFFF),
            ringTrack = Color(0xFFD8E6F1),
            chartGrid = Color(0x1A0B1C2F),
            bubbleBg = Color(0xFFE7FBFF).copy(alpha = 0.98f),
            progressTrack = Color(0xFFDCE8F2),
            nativeBubbleText = android.graphics.Color.rgb(0, 151, 171),
            isDark = false
        )
    }
}

internal fun ProgressPalette.backgroundBrush(): Brush = Brush.verticalGradient(listOf(backgroundTop, backgroundBottom))
internal fun ProgressPalette.cardBrush(): Brush = Brush.verticalGradient(listOf(cardTop, cardBottom))
