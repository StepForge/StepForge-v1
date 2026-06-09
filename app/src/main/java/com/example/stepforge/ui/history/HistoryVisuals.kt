package com.example.stepforge.ui.history

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import java.time.LocalDate
import kotlin.math.max

@Composable
internal fun PremiumHistoryCard(
    modifier: Modifier = Modifier,
    glow: Boolean = false,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = cs.surface.copy(alpha = if (glow) 0.90f else 0.82f)),
        border = BorderStroke(1.dp, if (glow) cs.primary.copy(alpha = 0.34f) else cs.onSurface.copy(alpha = 0.08f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .background(cs.surface.copy(alpha = if (glow) 0.16f else 0.04f))
        ) {
            content()
        }
    }
}

@Composable
internal fun HistorySolidHistoryBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    )
}

@Composable
internal fun WeatherMountainCanvas(
    modifier: Modifier,
    mood: HistoryWeatherMood,
    metric: HistoryMetric
) {
    val imageRes = when (mood) {
        HistoryWeatherMood.CLEAR -> R.drawable.history_mountain_sunny
        HistoryWeatherMood.CLOUDY -> R.drawable.history_mountain_cloudy
        HistoryWeatherMood.RAIN -> R.drawable.history_mountain_rainy
        HistoryWeatherMood.SNOW -> R.drawable.history_mountain_snowy
        HistoryWeatherMood.FOG -> R.drawable.history_mountain_foggy
        HistoryWeatherMood.NIGHT -> R.drawable.history_mountain_night
        HistoryWeatherMood.RAIN_NIGHT -> R.drawable.history_mountain_rainy_night
        HistoryWeatherMood.SNOW_NIGHT -> R.drawable.history_mountain_snowy_night
        HistoryWeatherMood.FOG_NIGHT -> R.drawable.history_mountain_foggy_night
        HistoryWeatherMood.STORM -> R.drawable.history_mountain_stormy
        HistoryWeatherMood.THUNDERSTORM -> R.drawable.history_mountain_thunderstorm
        HistoryWeatherMood.THUNDERSTORM_NIGHT -> R.drawable.history_mountain_thunderstorm_night
        HistoryWeatherMood.SUNRISE -> R.drawable.history_mountain_sunrise
        HistoryWeatherMood.SUNSET -> R.drawable.history_mountain_sunset
        HistoryWeatherMood.WINDY_SUNSET -> R.drawable.history_mountain_windy_sunset
        HistoryWeatherMood.BLIZZARD -> R.drawable.history_mountain_blizzard
    }
    Box(modifier = modifier.clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))) {
        Image(
            painter = painterResource(imageRes),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.04f),
                            Color.Transparent,
                            Color.Black.copy(alpha = 0.22f)
                        )
                    )
                )
        )
    }
}


internal data class HistoryRangeChartPoint(
    val date: LocalDate,
    val label: String,
    val value: Float,
    val valueText: String,
    val showLabel: Boolean = true
)

@Composable
internal fun HistoryRangeBarChart(
    points: List<HistoryRangeChartPoint>,
    metric: HistoryMetric,
    selectedDate: LocalDate,
    axisMax: Float,
    modifier: Modifier,
    onBarSelected: (LocalDate) -> Unit
) {
    val accent = when (metric) {
        HistoryMetric.STEPS -> Color(0xFFFF4FC3)
        HistoryMetric.DISTANCE -> Color(0xFF2EA8FF)
        HistoryMetric.CALORIES -> Color(0xFFFF8A24)
        HistoryMetric.ACTIVE_TIME -> Color(0xFF42F06D)
    }
    val safePoints = points.ifEmpty {
        listOf(HistoryRangeChartPoint(LocalDate.now(), "", 0f, "0", false))
    }
    Canvas(
        modifier = modifier.pointerInput(safePoints, axisMax) {
            detectTapGestures { offset ->
                val left = 36.dp.toPx()
                val right = 36.dp.toPx()
                val chartWidth = (size.width.toFloat() - left - right).coerceAtLeast(1f)
                val slot = chartWidth / safePoints.size.coerceAtLeast(1)
                val rawIndex = ((offset.x - left) / slot).toInt().coerceIn(0, safePoints.lastIndex)
                onBarSelected(safePoints[rawIndex].date)
            }
        }
    ) {
        val left = 36.dp.toPx()
        val right = 38.dp.toPx()
        val top = 24.dp.toPx()
        val bottom = 30.dp.toPx()
        val chartW = (size.width - left - right).coerceAtLeast(1f)
        val chartH = (size.height - top - bottom).coerceAtLeast(1f)
        val baseY = top + chartH
        val maxValue = max(1f, axisMax)
        val axisPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(190, 215, 225, 236)
            textSize = 9.5.sp.toPx()
            textAlign = android.graphics.Paint.Align.LEFT
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(190, 215, 225, 236)
            textSize = when {
                safePoints.size <= 7 -> 9.5.sp.toPx()
                safePoints.size <= 31 -> 8.5.sp.toPx()
                else -> 8.2.sp.toPx()
            }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val bubblePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val gridValues = historyGridValues(maxValue)
        gridValues.forEach { value ->
            val y = top + chartH * (1f - (value / maxValue).coerceIn(0f, 1f))
            drawLine(
                Color.White.copy(alpha = 0.09f),
                Offset(left, y),
                Offset(size.width - right, y),
                1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                value.formatAxisCompact(),
                size.width - right + 7.dp.toPx(),
                y + 4.dp.toPx(),
                axisPaint
            )
        }
        val slot = chartW / safePoints.size.coerceAtLeast(1)
        val widthScale = when {
            safePoints.size <= 7 -> 0.28f
            safePoints.size <= 31 -> 0.38f
            else -> 0.46f
        }
        val barW = (slot * widthScale).coerceIn(4.dp.toPx(), 18.dp.toPx())
        val selectedIndex = safePoints.indexOfFirst { it.date == selectedDate }
            .takeIf { it >= 0 }
            ?: safePoints.indexOfLast { it.value > 0f }.takeIf { it >= 0 }
            ?: safePoints.lastIndex
        safePoints.forEachIndexed { index, point ->
            val x = left + slot * index + (slot - barW) / 2f
            val h = (point.value / maxValue).coerceIn(0f, 1f) * chartH
            val visibleHeight = h.coerceAtLeast(if (point.value > 0f) 6.dp.toPx() else 0f)
            val y = baseY - visibleHeight
            val selected = index == selectedIndex
            if (selected) {
                val centerX = x + barW / 2f
                drawLine(
                    color = accent.copy(alpha = 0.72f),
                    start = Offset(centerX, top - 2.dp.toPx()),
                    end = Offset(centerX, baseY),
                    strokeWidth = 1.2.dp.toPx()
                )
                val bubbleText = point.valueText
                val bubbleW = (bubblePaint.measureText(bubbleText) + 14.dp.toPx()).coerceAtLeast(34.dp.toPx())
                val bubbleH = 20.dp.toPx()
                val bubbleLeft = (centerX - bubbleW / 2f).coerceIn(left, size.width - right - bubbleW)
                val bubbleTop = (y - bubbleH - 8.dp.toPx()).coerceAtLeast(0f)
                drawRoundRect(
                    color = accent,
                    topLeft = Offset(bubbleLeft, bubbleTop),
                    size = Size(bubbleW, bubbleH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                drawContext.canvas.nativeCanvas.drawText(
                    bubbleText,
                    bubbleLeft + bubbleW / 2f,
                    bubbleTop + 14.dp.toPx(),
                    bubblePaint
                )
            }
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        if (selected) accent else accent.copy(alpha = 0.78f),
                        Color(0xFF6C217E).copy(alpha = if (selected) 0.78f else 0.52f)
                    ),
                    startY = y,
                    endY = baseY
                ),
                topLeft = Offset(x, y),
                size = Size(barW, visibleHeight),
                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
            )
            if (point.showLabel) {
                drawContext.canvas.nativeCanvas.drawText(
                    point.label,
                    x + barW / 2f,
                    size.height - 6.dp.toPx(),
                    labelPaint
                )
            }
        }
        drawLine(Color.White.copy(alpha = 0.10f), Offset(left, baseY), Offset(size.width - right, baseY), 1.dp.toPx())
    }
}


@Composable
internal fun HistoryTrendCurveChart(
    points: List<HistoryRangeChartPoint>,
    metric: HistoryMetric,
    selectedDate: LocalDate,
    axisMax: Float,
    modifier: Modifier,
    onPointSelected: (LocalDate) -> Unit
) {
    val accent = when (metric) {
        HistoryMetric.STEPS -> Color(0xFFFF8A24)
        HistoryMetric.DISTANCE -> Color(0xFF2EA8FF)
        HistoryMetric.CALORIES -> Color(0xFFFF6A38)
        HistoryMetric.ACTIVE_TIME -> Color(0xFF42F06D)
    }
    val safePoints = points.ifEmpty {
        listOf(HistoryRangeChartPoint(LocalDate.now(), "", 0f, "0", false))
    }
    val selectedIndexFallback = safePoints.indexOfFirst { it.date == selectedDate }
        .takeIf { it >= 0 }
        ?: safePoints.indexOfLast { it.value > 0f }.takeIf { it >= 0 }
        ?: safePoints.lastIndex

    Canvas(
        modifier = modifier.pointerInput(safePoints, axisMax) {
            detectTapGestures { offset ->
                val left = 42.dp.toPx()
                val right = 14.dp.toPx()
                val chartWidth = (size.width.toFloat() - left - right).coerceAtLeast(1f)
                val slot = chartWidth / safePoints.size.coerceAtLeast(1)
                val index = ((offset.x - left) / slot).toInt().coerceIn(0, safePoints.lastIndex)
                onPointSelected(safePoints[index].date)
            }
        }
    ) {
        val left = 42.dp.toPx()
        val right = 14.dp.toPx()
        val top = 22.dp.toPx()
        val bottom = 28.dp.toPx()
        val chartW = (size.width - left - right).coerceAtLeast(1f)
        val chartH = (size.height - top - bottom).coerceAtLeast(1f)
        val baseY = top + chartH
        val maxValue = max(1f, axisMax)
        val selectedIndex = selectedIndexFallback.coerceIn(0, safePoints.lastIndex)

        val axisPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(188, 210, 223, 235)
            textSize = 9.2.sp.toPx()
            textAlign = android.graphics.Paint.Align.RIGHT
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(176, 210, 223, 235)
            textSize = when {
                safePoints.size <= 7 -> 9.2.sp.toPx()
                safePoints.size <= 31 -> 8.2.sp.toPx()
                else -> 8.0.sp.toPx()
            }
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val bubblePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 9.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        historyGridValues(maxValue).forEach { value ->
            val y = top + chartH * (1f - (value / maxValue).coerceIn(0f, 1f))
            drawLine(
                Color.White.copy(alpha = 0.085f),
                Offset(left, y),
                Offset(size.width - right, y),
                1.dp.toPx()
            )
            drawContext.canvas.nativeCanvas.drawText(
                value.formatAxisCompact(),
                left - 7.dp.toPx(),
                y + 4.dp.toPx(),
                axisPaint
            )
        }

        val slot = chartW / safePoints.size.coerceAtLeast(1)
        val offsets = safePoints.mapIndexed { index, point ->
            val x = left + slot * index + slot / 2f
            val y = top + chartH * (1f - (point.value / maxValue).coerceIn(0f, 1f))
            Offset(x, y)
        }

        if (offsets.isNotEmpty()) {
            val areaPath = smoothAreaPath(offsets, baseY)
            drawPath(
                path = areaPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        accent.copy(alpha = 0.18f),
                        accent.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    startY = top,
                    endY = baseY
                )
            )
            val curvePath = smoothCurvePath(offsets)
            drawPath(
                path = curvePath,
                color = accent.copy(alpha = 0.26f),
                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
            )
            drawPath(
                path = curvePath,
                brush = Brush.horizontalGradient(
                    listOf(
                        accent.copy(alpha = 0.72f),
                        Color(0xFFFFC13D).copy(alpha = 0.95f),
                        accent.copy(alpha = 0.84f)
                    )
                ),
                style = Stroke(width = 2.8.dp.toPx(), cap = StrokeCap.Round)
            )

            offsets.forEachIndexed { index, point ->
                val isSelected = index == selectedIndex
                val radius = if (isSelected) 3.8.dp.toPx() else 2.8.dp.toPx()
                drawCircle(accent.copy(alpha = if (isSelected) 0.34f else 0.20f), radius + 5.dp.toPx(), point)
                drawCircle(accent.copy(alpha = 0.95f), radius, point)
            }

            val selectedPoint = offsets[selectedIndex]
            drawLine(
                color = accent.copy(alpha = 0.70f),
                start = Offset(selectedPoint.x, top - 2.dp.toPx()),
                end = Offset(selectedPoint.x, baseY),
                strokeWidth = 1.15.dp.toPx()
            )
            val bubbleText = safePoints[selectedIndex].value.formatTrendBubble(metric)
            val bubbleW = (bubblePaint.measureText(bubbleText) + 16.dp.toPx()).coerceAtLeast(40.dp.toPx())
            val bubbleH = 20.dp.toPx()
            val bubbleLeft = (selectedPoint.x - bubbleW / 2f).coerceIn(left, size.width - right - bubbleW)
            val bubbleTop = (selectedPoint.y - bubbleH - 9.dp.toPx()).coerceIn(0f, baseY - bubbleH)
            drawRoundRect(
                color = accent,
                topLeft = Offset(bubbleLeft, bubbleTop),
                size = Size(bubbleW, bubbleH),
                cornerRadius = CornerRadius(7.dp.toPx(), 7.dp.toPx())
            )
            drawContext.canvas.nativeCanvas.drawText(
                bubbleText,
                bubbleLeft + bubbleW / 2f,
                bubbleTop + 14.dp.toPx(),
                bubblePaint
            )
        }

        safePoints.forEachIndexed { index, point ->
            if (point.showLabel) {
                val x = left + slot * index + slot / 2f
                drawContext.canvas.nativeCanvas.drawText(
                    point.label,
                    x,
                    size.height - 5.dp.toPx(),
                    labelPaint
                )
            }
        }
        drawLine(Color.White.copy(alpha = 0.10f), Offset(left, baseY), Offset(size.width - right, baseY), 1.dp.toPx())
    }
}



private fun historyGridValues(maxValue: Float): List<Float> {
    val safe = maxValue.coerceAtLeast(1f)
    return when {
        safe <= 15_000f -> listOf(safe, safe * 2f / 3f, safe / 3f, 0f)
        else -> listOf(safe, safe * 0.75f, safe * 0.5f, safe * 0.25f, 0f)
    }
}

private fun Float.formatAxisCompact(): String {
    val safe = this.coerceAtLeast(0f)
    return when {
        safe >= 1_000_000f -> if (safe % 1_000_000f < 1f) "${(safe / 1_000_000f).toInt()}M" else String.format(java.util.Locale.getDefault(), "%.1fM", safe / 1_000_000f)
        safe >= 1_000f -> "${(safe / 1_000f).toInt()}K"
        else -> safe.toInt().toString()
    }
}

@Composable
internal fun HistoryBarChart(
    days: List<HistoryDayUi>,
    metric: HistoryMetric,
    selectedDate: java.time.LocalDate,
    modifier: Modifier
) {
    val accent = when (metric) {
        HistoryMetric.STEPS -> Color(0xFFFF4FC3)
        HistoryMetric.DISTANCE -> Color(0xFF2EA8FF)
        HistoryMetric.CALORIES -> Color(0xFFFF8A24)
        HistoryMetric.ACTIVE_TIME -> Color(0xFF42F06D)
    }
    val animatedDays = days.ifEmpty { listOf() }
    Canvas(modifier = modifier) {
        val maxValue = max(1f, animatedDays.maxOfOrNull { it.metricValue(metric) } ?: 1f)
        val left = 34.dp.toPx()
        val right = 12.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 24.dp.toPx()
        val chartW = size.width - left - right
        val chartH = size.height - top - bottom
        val baseY = top + chartH
        repeat(4) { index ->
            val y = top + chartH * (index / 3f)
            drawLine(Color.White.copy(alpha = 0.08f), Offset(left, y), Offset(size.width - right, y), 1.dp.toPx())
        }
        val slot = chartW / animatedDays.size.coerceAtLeast(1)
        val barW = slot * 0.34f
        animatedDays.forEachIndexed { i, day ->
            val h = (day.metricValue(metric) / maxValue).coerceIn(0f, 1f) * chartH
            val x = left + slot * i + (slot - barW) / 2f
            val selected = day.localDate == selectedDate
            drawRoundRect(
                brush = Brush.verticalGradient(
                    listOf(
                        if (selected) accent else accent.copy(alpha = 0.72f),
                        Color(0xFF5B246D).copy(alpha = 0.54f)
                    ),
                    startY = baseY - h,
                    endY = baseY
                ),
                topLeft = Offset(x, baseY - h),
                size = Size(barW, h.coerceAtLeast(if (day.metricValue(metric) > 0f) 8.dp.toPx() else 0f)),
                cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
            )
        }
    }
}

@Composable
internal fun HistoryLineChart(
    days: List<HistoryDayUi>,
    metric: HistoryMetric,
    modifier: Modifier
) {
    val accent = when (metric) {
        HistoryMetric.STEPS -> Color(0xFFFF8A24)
        HistoryMetric.DISTANCE -> Color(0xFF2EA8FF)
        HistoryMetric.CALORIES -> Color(0xFFFF5A34)
        HistoryMetric.ACTIVE_TIME -> Color(0xFF42F06D)
    }
    Canvas(modifier = modifier) {
        val maxValue = max(1f, days.maxOfOrNull { it.metricValue(metric) } ?: 1f)
        val left = 28.dp.toPx()
        val right = 10.dp.toPx()
        val top = 10.dp.toPx()
        val bottom = 18.dp.toPx()
        val chartW = size.width - left - right
        val chartH = size.height - top - bottom
        repeat(4) { index ->
            val y = top + chartH * (index / 3f)
            drawLine(Color.White.copy(alpha = 0.08f), Offset(left, y), Offset(size.width - right, y), 1.dp.toPx())
        }
        val points = days.mapIndexed { index, day ->
            val x = left + chartW * (index / (days.size - 1).coerceAtLeast(1).toFloat())
            val y = top + chartH * (1f - (day.metricValue(metric) / maxValue).coerceIn(0f, 1f))
            Offset(x, y)
        }
        points.zipWithNext().forEach { (a, b) ->
            drawLine(accent.copy(alpha = 0.92f), a, b, 2.2.dp.toPx(), cap = StrokeCap.Round)
        }
        points.forEach { point ->
            drawCircle(accent.copy(alpha = 0.24f), 7.dp.toPx(), point)
            drawCircle(accent, 3.dp.toPx(), point)
        }
    }
}


private fun smoothCurvePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points.first().x, points.first().y)
    if (points.size == 1) return path
    val tension = 0.18f
    for (i in 0 until points.lastIndex) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { p2 }
        val cp1 = Offset(
            x = p1.x + (p2.x - p0.x) * tension,
            y = p1.y + (p2.y - p0.y) * tension
        )
        val cp2 = Offset(
            x = p2.x - (p3.x - p1.x) * tension,
            y = p2.y - (p3.y - p1.y) * tension
        )
        path.cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p2.x, p2.y)
    }
    return path
}

private fun smoothAreaPath(points: List<Offset>, baseY: Float): Path {
    val path = smoothCurvePath(points)
    if (points.isEmpty()) return path
    path.lineTo(points.last().x, baseY)
    path.lineTo(points.first().x, baseY)
    path.close()
    return path
}

private fun Float.formatTrendBubble(metric: HistoryMetric): String = when (metric) {
    HistoryMetric.STEPS, HistoryMetric.CALORIES -> formatShortCount()
    HistoryMetric.DISTANCE -> {
        val safe = coerceAtLeast(0f)
        if (safe >= 10f) String.format(java.util.Locale.getDefault(), "%.0f", safe)
        else String.format(java.util.Locale.getDefault(), "%.1f", safe)
    }
    HistoryMetric.ACTIVE_TIME -> toInt().coerceAtLeast(0).toString()
}

private fun Float.formatShortCount(): String {
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
        else -> safe.toInt().toString()
    }
}


@Composable
internal fun HistoryCalendarRing(
    progress: Float,
    color: Color,
    selected: Boolean,
    modifier: Modifier
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(450), label = "historyCalendarRing")
    Canvas(modifier = modifier) {
        val stroke = if (selected) 4.dp.toPx() else 3.dp.toPx()
        drawArc(
            color = color.copy(alpha = if (selected) 0.30f else 0.18f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = Offset(stroke / 2f, stroke / 2f),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        if (selected) drawCircle(color.copy(alpha = 0.20f), radius = size.minDimension / 2f)
    }
}

@Composable
internal fun AchievementHex(progress: Float, color: Color, modifier: Modifier) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(600), label = "achievementHex")
    Canvas(modifier = modifier) {
        val path = Path().apply {
            moveTo(size.width * 0.50f, 0f)
            lineTo(size.width * 0.93f, size.height * 0.25f)
            lineTo(size.width * 0.93f, size.height * 0.75f)
            lineTo(size.width * 0.50f, size.height)
            lineTo(size.width * 0.07f, size.height * 0.75f)
            lineTo(size.width * 0.07f, size.height * 0.25f)
            close()
        }
        drawPath(path, color.copy(alpha = 0.10f))
        drawPath(path, color.copy(alpha = 0.95f), style = Stroke(2.2.dp.toPx()))
        drawCircle(color.copy(alpha = 0.16f), size.minDimension * 0.30f, Offset(size.width / 2f, size.height / 2f))
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = Offset(size.width * 0.23f, size.height * 0.23f),
            size = Size(size.width * 0.54f, size.height * 0.54f),
            style = Stroke(3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}

@Composable
internal fun HistoryTopDaysPodiumVisual(
    days: List<HistoryDayUi>,
    metric: HistoryMetric,
    modifier: Modifier
) {
    val safeDays = days.take(6)
    val accents = listOf(
        Color(0xFFFFB02E),
        Color(0xFF18E8FF),
        Color(0xFF9B5CFF),
        Color(0xFF18E8FF),
        Color(0xFF18E8FF),
        Color(0xFF18E8FF)
    )
    Canvas(modifier = modifier) {
        val left = 18.dp.toPx()
        val right = 18.dp.toPx()
        val top = 42.dp.toPx()
        val bottom = 30.dp.toPx()
        val chartW = (size.width - left - right).coerceAtLeast(1f)
        val chartH = (size.height - top - bottom).coerceAtLeast(1f)
        val baseY = top + chartH
        val maxValue = (safeDays.maxOfOrNull { it.metricValue(metric) } ?: 1f).coerceAtLeast(1f)
        val slot = chartW / safeDays.size.coerceAtLeast(1)
        val labelPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(205, 232, 238, 246)
            textSize = 9.4.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val valuePaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.WHITE
            textSize = 10.5.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }
        val rankPaint = android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.BLACK
            textSize = 10.sp.toPx()
            textAlign = android.graphics.Paint.Align.CENTER
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
        }

        drawRoundRect(
            color = Color.White.copy(alpha = 0.045f),
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(18.dp.toPx(), 18.dp.toPx())
        )

        safeDays.forEachIndexed { index, day ->
            val accent = accents.getOrElse(index) { Color(0xFF18E8FF) }
            val centerX = left + slot * index + slot / 2f
            val barW = (slot * 0.40f).coerceIn(17.dp.toPx(), 31.dp.toPx())
            val normalized = (day.metricValue(metric) / maxValue).coerceIn(0f, 1f)
            val barH = (chartH * (0.24f + normalized * 0.50f)).coerceAtLeast(18.dp.toPx())
            val x = centerX - barW / 2f
            val y = baseY - barH

            drawRoundRect(
                color = accent.copy(alpha = 0.16f),
                topLeft = Offset(x - 6.dp.toPx(), y - 8.dp.toPx()),
                size = Size(barW + 12.dp.toPx(), barH + 8.dp.toPx()),
                cornerRadius = CornerRadius(16.dp.toPx(), 16.dp.toPx())
            )
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(accent.copy(alpha = 0.92f), accent.copy(alpha = 0.34f)),
                    startY = y,
                    endY = baseY
                ),
                topLeft = Offset(x, y),
                size = Size(barW, barH),
                cornerRadius = CornerRadius(12.dp.toPx(), 12.dp.toPx())
            )
            val rankCenterY = y - 4.dp.toPx()
            drawCircle(
                color = accent,
                radius = 11.dp.toPx(),
                center = Offset(centerX, rankCenterY)
            )
            drawContext.canvas.nativeCanvas.drawText(
                day.metricDisplayRaw(metric),
                centerX,
                (rankCenterY - 17.dp.toPx()).coerceAtLeast(14.dp.toPx()),
                valuePaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                (index + 1).toString(),
                centerX,
                rankCenterY + 3.5.dp.toPx(),
                rankPaint
            )
            drawContext.canvas.nativeCanvas.drawText(
                day.localDate.shortMonthDay(),
                centerX,
                size.height - 9.dp.toPx(),
                labelPaint
            )
        }
    }
}

private fun HistoryDayUi.metricDisplayRaw(metric: HistoryMetric): String = when (metric) {
    HistoryMetric.STEPS -> steps.toFloat().formatShortCount()
    HistoryMetric.DISTANCE -> distanceKm.formatHistoryKm()
    HistoryMetric.CALORIES -> calories.toFloat().formatShortCount()
    HistoryMetric.ACTIVE_TIME -> activeMinutes.toFloat().formatShortCount()
}
