package com.example.stepforge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalTime
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun CustomTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Select time",
    subtitle: String = "Choose the exact time.",
    accent: Color = Color(0xFF00F5C8)
) {
    var pendingTime by remember(initialHour, initialMinute) {
        mutableStateOf(LocalTime.of(initialHour.coerceIn(0, 23), initialMinute.coerceIn(0, 59)))
    }

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val stepForgeAccent = Color(0xFF00F5C8)
    val cardBg = if (isDark) Color(0xFF05070B) else Color(0xFFF8FAF9)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = cardBg,
            shadowElevation = 24.dp,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                stepForgeAccent.copy(alpha = if (isDark) 0.08f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        color = cs.onSurface,
                        fontSize = 23.sp,
                        lineHeight = 25.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = subtitle,
                        color = cs.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(104.dp)
                        .background(
                            if (isDark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.045f),
                            RoundedCornerShape(24.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = pendingTime.formatShort(),
                        color = cs.onSurface,
                        fontSize = 46.sp,
                        lineHeight = 48.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SingleTimeStepButton(
                        text = "−1h",
                        modifier = Modifier.weight(1f),
                        onClick = { pendingTime = pendingTime.minusHours(1) }
                    )
                    SingleTimeStepButton(
                        text = "+1h",
                        modifier = Modifier.weight(1f),
                        onClick = { pendingTime = pendingTime.plusHours(1) }
                    )
                    SingleTimeStepButton(
                        text = "−5m",
                        modifier = Modifier.weight(1f),
                        onClick = { pendingTime = pendingTime.minusMinutes(5) }
                    )
                    SingleTimeStepButton(
                        text = "+5m",
                        modifier = Modifier.weight(1f),
                        onClick = { pendingTime = pendingTime.plusMinutes(5) }
                    )
                }

                HorizontalDivider(color = cs.outline.copy(alpha = 0.10f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = stepForgeAccent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = { onConfirm(pendingTime.hour, pendingTime.minute) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = stepForgeAccent,
                            contentColor = if (isDark) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = "Set",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SingleTimeStepButton(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(40.dp)
            .background(Color(0xFF00F5C8).copy(alpha = 0.11f), RoundedCornerShape(999.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
fun CustomTimePicker(
    bedTime: LocalTime,
    wakeTime: LocalTime,
    onConfirm: (LocalTime, LocalTime) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Sleep time",
    subtitle: String = "Drag the two handles to set bedtime and wake time."
) {
    var pendingBedTime by remember(bedTime) { mutableStateOf(bedTime) }
    var pendingWakeTime by remember(wakeTime) { mutableStateOf(wakeTime) }

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val cardBg = if (isDark) Color(0xFF05070B) else Color(0xFFF8FAF9)
    val accent = Color(0xFF00F5C8)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(34.dp),
            color = cardBg,
            shadowElevation = 28.dp,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = if (isDark) 0.08f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = title,
                        color = cs.onSurface,
                        fontSize = 24.sp,
                        lineHeight = 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = subtitle,
                        color = cs.onSurfaceVariant,
                        fontSize = 12.sp,
                        lineHeight = 15.sp
                    )
                }

                SleepRangeDial(
                    bedTime = pendingBedTime,
                    wakeTime = pendingWakeTime,
                    onChange = { start, end ->
                        pendingBedTime = start
                        pendingWakeTime = end
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(330.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TimeSummaryChip(
                        label = "Bedtime",
                        value = pendingBedTime.formatShort(),
                        modifier = Modifier.weight(1f)
                    )
                    TimeSummaryChip(
                        label = "Wake time",
                        value = pendingWakeTime.formatShort(),
                        modifier = Modifier.weight(1f)
                    )
                }

                HorizontalDivider(color = cs.outline.copy(alpha = 0.10f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            color = accent,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = { onConfirm(pendingBedTime, pendingWakeTime) },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = if (isDark) Color.Black else Color.White
                        )
                    ) {
                        Text(
                            text = "Set",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
}

private enum class SleepRangeHandle {
    START,
    END
}

@Composable
private fun SleepRangeDial(
    bedTime: LocalTime,
    wakeTime: LocalTime,
    onChange: (LocalTime, LocalTime) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val accent = Color(0xFF00F5C8)
    val secondAccent = Color(0xFF21E6A3)
    val track = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.10f)
    val durationText = formatDurationMinutes(sleepDurationMinutes(bedTime, wakeTime))

    val latestBedTime by rememberUpdatedState(bedTime)
    val latestWakeTime by rememberUpdatedState(wakeTime)
    var activeHandle by remember { mutableStateOf<SleepRangeHandle?>(null) }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .size(292.dp)
                .pointerInput(Unit) {
                    fun Offset.toTimeOnDial(): LocalTime {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = x - center.x
                        val dy = y - center.y
                        val degrees = ((Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f + 360f) % 360f)
                        val totalMinutes = ((degrees / 360f) * 24f * 60f / 5f).roundToInt() * 5
                        val normalized = totalMinutes.floorMod(24 * 60)
                        return LocalTime.of(normalized / 60, normalized % 60)
                    }

                    fun handlePosition(time: LocalTime): Offset {
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = minOf(size.width, size.height) * 0.39f
                        val angle = (time.toDayFraction() * 360f - 90f).toRadians()
                        return Offset(
                            center.x + (cos(angle) * radius).toFloat(),
                            center.y + (sin(angle) * radius).toFloat()
                        )
                    }

                    fun updateHandle(offset: Offset) {
                        val picked = offset.toTimeOnDial()
                        when (activeHandle) {
                            SleepRangeHandle.START -> onChange(picked, latestWakeTime)
                            SleepRangeHandle.END -> onChange(latestBedTime, picked)
                            null -> Unit
                        }
                    }

                    detectDragGestures(
                        onDragStart = { offset ->
                            val startHandle = handlePosition(latestBedTime)
                            val endHandle = handlePosition(latestWakeTime)
                            val startDistance = hypot((offset.x - startHandle.x).toDouble(), (offset.y - startHandle.y).toDouble())
                            val endDistance = hypot((offset.x - endHandle.x).toDouble(), (offset.y - endHandle.y).toDouble())
                            activeHandle = if (startDistance <= endDistance) SleepRangeHandle.START else SleepRangeHandle.END
                            updateHandle(offset)
                        },
                        onDrag = { change, _ ->
                            updateHandle(change.position)
                        },
                        onDragEnd = { activeHandle = null },
                        onDragCancel = { activeHandle = null }
                    )
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension * 0.39f
            val stroke = 25.dp.toPx()

            val startFraction = bedTime.toDayFraction()
            val sweepFraction = sleepDurationMinutes(bedTime, wakeTime) / (24f * 60f)
            val endFraction = wakeTime.toDayFraction()

            drawCircle(
                color = track,
                radius = radius,
                center = center,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            drawArc(
                brush = Brush.sweepGradient(
                    listOf(accent, secondAccent, accent),
                    center = center
                ),
                startAngle = -90f + 360f * startFraction,
                sweepAngle = 360f * sweepFraction,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )

            fun drawHandle(
                fraction: Float,
                active: Boolean,
                fill: Color,
                icon: String
            ) {
                val angle = (fraction * 360f - 90f).toRadians()
                val position = Offset(
                    center.x + (cos(angle) * radius).toFloat(),
                    center.y + (sin(angle) * radius).toFloat()
                )

                drawCircle(
                    color = Color.Black.copy(alpha = if (isDark) 0.36f else 0.14f),
                    radius = if (active) 20.dp.toPx() else 17.dp.toPx(),
                    center = position
                )
                drawCircle(
                    color = fill,
                    radius = if (active) 17.dp.toPx() else 15.dp.toPx(),
                    center = position
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.18f),
                    radius = if (active) 10.dp.toPx() else 8.dp.toPx(),
                    center = position
                )

                drawIntoCanvas { canvas ->
                    val iconPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
                        color = android.graphics.Color.WHITE
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 13.sp.toPx()
                        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
                    }
                    val y = position.y - (iconPaint.descent() + iconPaint.ascent()) / 2f
                    canvas.nativeCanvas.drawText(icon, position.x, y, iconPaint)
                }
            }

            drawHandle(
                fraction = startFraction,
                active = activeHandle == SleepRangeHandle.START,
                fill = accent,
                icon = "☾"
            )
            drawHandle(
                fraction = endFraction,
                active = activeHandle == SleepRangeHandle.END,
                fill = secondAccent,
                icon = "☀"
            )
        }

        Text(
            text = durationText,
            color = cs.onSurface,
            fontSize = 38.sp,
            lineHeight = 40.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun TimeSummaryChip(
    label: String,
    value: String,
    modifier: Modifier
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    Column(
        modifier = modifier
            .background(
                if (isDark) Color.White.copy(alpha = 0.055f) else Color.Black.copy(alpha = 0.045f),
                RoundedCornerShape(18.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            color = cs.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
        Text(
            text = value,
            color = cs.onSurface,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

private fun sleepDurationMinutes(start: LocalTime, end: LocalTime): Int {
    val startMinutes = start.hour * 60 + start.minute
    val endMinutes = end.hour * 60 + end.minute
    return if (endMinutes > startMinutes) {
        endMinutes - startMinutes
    } else {
        (24 * 60 - startMinutes) + endMinutes
    }
}

private fun formatDurationMinutes(minutes: Int): String {
    val hours = minutes / 60
    val mins = minutes % 60
    return when {
        hours > 0 && mins > 0 -> "${hours}h ${mins}m"
        hours > 0 -> "${hours}h"
        else -> "${mins}m"
    }
}

private fun LocalTime.toDayFraction(): Float {
    return ((hour * 60 + minute) / (24f * 60f)).coerceIn(0f, 1f)
}

private fun LocalTime.formatShort(): String = "%02d:%02d".format(hour, minute)

private fun Float.toRadians(): Double = this / 180f * PI

private fun Int.floorMod(mod: Int): Int = ((this % mod) + mod) % mod
