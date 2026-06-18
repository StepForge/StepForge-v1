package com.example.stepforge.ui.water

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import java.text.NumberFormat
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun HydrationBottleHero(
    todayMl: Int,
    goalMl: Int,
    dateLabel: String,
    isSelectedToday: Boolean,
    progress: Float,
    percent: Int,
    remainingMl: Int,
    pulseScale: Float,
    cardBg: Color,
    border: Color,
    accent: Color,
    onGoalClick: () -> Unit,
    onDateClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val numberFormat = NumberFormat.getIntegerInstance()
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 850, easing = EaseOutCubic),
        label = "hydrationBottleProgress"
    )

    GlassPanel(cardBg = cardBg, border = border, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(0.92f)
                        .height(286.dp),
                    contentAlignment = Alignment.Center
                ) {
                    HydrationBottleCanvas(
                        progress = animatedProgress,
                        accent = accent,
                        modifier = Modifier
                            .size(width = 214.dp, height = 278.dp)
                            .scale(pulseScale)
                    )
                }

                Column(
                    modifier = Modifier.weight(1.08f),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(999.dp))
                            .border(1.dp, accent.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
                            .clickable(onClick = onDateClick)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.CalendarMonth, contentDescription = null, tint = accent, modifier = Modifier.size(15.dp))
                        Text(dateLabel, color = accent, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }

                    Text(
                        text = stringResource(R.string.wr_water_amount_format, numberFormat.format(todayMl)),
                        color = cs.onSurface,
                        fontSize = 31.sp,
                        lineHeight = 33.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.wr_goal_amount_format, numberFormat.format(goalMl)),
                        color = cs.onSurfaceVariant,
                        fontSize = 14.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    WaterMiniProgress(percent = percent, accent = accent)

                    Row(
                        modifier = Modifier
                            .background(Color.White.copy(alpha = 0.050f), RoundedCornerShape(18.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                        Column {
                            Text(
                                text = if (remainingMl == 0) {
                                    stringResource(R.string.wr_goal_completed_title)
                                } else {
                                    stringResource(R.string.wr_remaining_format, numberFormat.format(remainingMl))
                                },
                                color = cs.onSurface,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                lineHeight = 15.sp
                            )
                            Text(
                                text = if (remainingMl == 0) {
                                    stringResource(
                                        if (isSelectedToday) {
                                            R.string.wr_goal_completed_subtitle
                                        } else {
                                            R.string.wr_goal_completed_selected_subtitle
                                        }
                                    )
                                } else {
                                    stringResource(
                                        if (isSelectedToday) {
                                            R.string.wr_hydrated_today_format
                                        } else {
                                            R.string.wr_hydrated_selected_format
                                        },
                                        percent
                                    )
                                },
                                color = cs.onSurfaceVariant,
                                fontSize = 10.5.sp,
                                lineHeight = 13.sp
                            )
                        }
                    }

                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WaterScaleChip(
                    label = stringResource(R.string.wr_scale_zero),
                    modifier = Modifier.weight(1f)
                )
                WaterScaleChip(
                    label = stringResource(R.string.wr_scale_current, numberFormat.format(todayMl)),
                    accent = accent,
                    modifier = Modifier.weight(1f)
                )
                WaterScaleChip(
                    label = stringResource(R.string.wr_scale_goal, numberFormat.format(goalMl)),
                    accent = accent,
                    modifier = Modifier.weight(1f),
                    highlighted = true,
                    onClick = onGoalClick
                )
            }
        }
    }
}

@Composable
private fun WaterScaleChip(
    label: String,
    modifier: Modifier,
    accent: Color? = null,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null
) {
    val shape = RoundedCornerShape(999.dp)
    val textColor = accent ?: MaterialTheme.colorScheme.onSurfaceVariant
    val baseModifier = modifier
        .height(30.dp)
        .background(Color.White.copy(alpha = 0.045f), shape)
        .then(
            if (highlighted) {
                Modifier.border(
                    BorderStroke(
                        1.3.dp,
                        Brush.horizontalGradient(
                            listOf(
                                Color(0xFF00B8FF),
                                Color(0xFF00F5C8)
                            )
                        )
                    ),
                    shape
                )
            } else {
                Modifier
            }
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(
        modifier = baseModifier,
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun WaterMiniProgress(
    percent: Int,
    accent: Color
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((percent / 100f).coerceIn(0f, 1f))
                    .height(8.dp)
                    .background(accent, RoundedCornerShape(999.dp))
            )
        }
        Text(stringResource(R.string.wr_percent_format, percent), color = accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
private fun HydrationBottleCanvas(
    progress: Float,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        val bottleLeft = w * 0.24f
        val bottleTop = h * 0.12f
        val bottleRight = w * 0.76f
        val bottleBottom = h * 0.91f
        val bottleWidth = bottleRight - bottleLeft
        val bottleHeight = bottleBottom - bottleTop
        val radius = bottleWidth * 0.18f

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.18f),
                    Color.White.copy(alpha = 0.050f),
                    Color.White.copy(alpha = 0.12f)
                )
            ),
            topLeft = Offset(bottleLeft, bottleTop),
            size = Size(bottleWidth, bottleHeight),
            cornerRadius = CornerRadius(radius, radius)
        )

        val innerLeft = bottleLeft + bottleWidth * 0.09f
        val innerRight = bottleRight - bottleWidth * 0.09f
        val innerBottom = bottleBottom - bottleHeight * 0.055f
        val innerTop = bottleTop + bottleHeight * 0.09f
        val innerWidth = innerRight - innerLeft
        val innerHeight = innerBottom - innerTop

        val safeProgress = progress.coerceIn(0f, 1f)
        if (safeProgress > 0.012f) {
            val fillHeight = innerHeight * safeProgress
            val waterTop = innerBottom - fillHeight
            val wave = Path().apply {
                moveTo(innerLeft, waterTop)
                val segments = 10
                for (i in 0..segments) {
                    val x = innerLeft + innerWidth * i / segments
                    val y = waterTop + sin((i / segments.toFloat()) * PI.toFloat() * 2f) * 2.4.dp.toPx()
                    lineTo(x, y)
                }
                lineTo(innerRight, innerBottom)
                lineTo(innerLeft, innerBottom)
                close()
            }

            drawPath(
                path = wave,
                brush = Brush.verticalGradient(
                    listOf(
                        accent.copy(alpha = 0.92f),
                        Color(0xFF00AFC9).copy(alpha = 0.72f),
                        Color(0xFF006D84).copy(alpha = 0.52f)
                    )
                )
            )

            repeat(5) { i ->
                val bubbleX = innerLeft + innerWidth * (0.25f + (i % 3) * 0.22f)
                val bubbleY = waterTop + fillHeight * (0.20f + i * 0.13f)
                if (bubbleY < innerBottom - 6.dp.toPx()) {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = (1.2f + i % 2).dp.toPx(),
                        center = Offset(bubbleX, bubbleY)
                    )
                }
            }
        }

        repeat(8) { i ->
            val y = bottleTop + bottleHeight * (0.18f + i * 0.085f)
            drawLine(
                color = accent.copy(alpha = 0.42f),
                start = Offset(bottleLeft + bottleWidth * 0.15f, y),
                end = Offset(bottleLeft + bottleWidth * if (i % 3 == 0) 0.30f else 0.23f, y),
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = accent.copy(alpha = 0.42f),
                start = Offset(bottleRight - bottleWidth * 0.15f, y),
                end = Offset(bottleRight - bottleWidth * if (i % 3 == 0) 0.30f else 0.23f, y),
                strokeWidth = 1.1.dp.toPx(),
                cap = StrokeCap.Round
            )
        }

        drawRoundRect(
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.Transparent,
                    Color.White.copy(alpha = 0.10f)
                )
            ),
            topLeft = Offset(bottleLeft, bottleTop),
            size = Size(bottleWidth, bottleHeight),
            cornerRadius = CornerRadius(radius, radius),
            style = Stroke(width = 3.dp.toPx())
        )

        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(bottleLeft + bottleWidth * 0.18f, bottleTop + bottleHeight * 0.11f),
            end = Offset(bottleLeft + bottleWidth * 0.18f, bottleBottom - bottleHeight * 0.11f),
            strokeWidth = 4.5.dp.toPx(),
            cap = StrokeCap.Round
        )

    }
}
