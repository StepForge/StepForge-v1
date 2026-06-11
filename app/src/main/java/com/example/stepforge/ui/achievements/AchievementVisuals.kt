package com.example.stepforge.ui.achievements

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
internal fun AchievementDarkBackground() {
    val cs = MaterialTheme.colorScheme
    val darkTheme = isSystemInDarkTheme()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = if (darkTheme) {
                        listOf(Color(0xFF02050A), cs.background, Color(0xFF030711))
                    } else {
                        listOf(Color(0xFFF7FBFF), cs.background, Color(0xFFEAF6FF))
                    }
                )
            )
    )
}

@Composable
internal fun AchievementCardShell(
    modifier: Modifier = Modifier,
    highlight: Boolean = false,
    content: @Composable () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val darkTheme = isSystemInDarkTheme()
    val container = if (darkTheme) {
        cs.surface.copy(alpha = if (highlight) 0.90f else 0.78f)
    } else {
        cs.surface.copy(alpha = if (highlight) 0.98f else 0.94f)
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = if (darkTheme) 0.dp else 1.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(24.dp))
                .background(
                    Brush.linearGradient(
                        colors = if (darkTheme) {
                            listOf(
                                Color(0xFF00F5FF).copy(alpha = if (highlight) 0.11f else 0.05f),
                                Color.Transparent,
                                Color(0xFFFFA51F).copy(alpha = if (highlight) 0.08f else 0.03f)
                            )
                        } else {
                            listOf(
                                Color(0xFF00A8B8).copy(alpha = if (highlight) 0.08f else 0.04f),
                                Color.White.copy(alpha = 0.35f),
                                Color(0xFFFFB02E).copy(alpha = if (highlight) 0.07f else 0.03f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    color = if (highlight) Color(0xFF00F5FF).copy(alpha = if (darkTheme) 0.32f else 0.24f) else cs.onSurface.copy(alpha = if (darkTheme) 0.08f else 0.12f),
                    shape = RoundedCornerShape(24.dp)
                )
        ) {
            content()
        }
    }
}

@Composable
internal fun AchievementProgressRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Float = 7f,
    backgroundAlpha: Float = 0.16f
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "achievementProgressRing")
    Canvas(modifier = modifier) {
        val stroke = strokeWidth.dp.toPx()
        val inset = stroke / 2f
        val arcSize = Size(size.width - stroke, size.height - stroke)
        drawArc(
            color = color.copy(alpha = backgroundAlpha),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
        drawArc(
            brush = Brush.sweepGradient(
                colors = listOf(color.copy(alpha = 0.98f), Color(0xFF00F5FF), color.copy(alpha = 0.98f))
            ),
            startAngle = -90f,
            sweepAngle = 360f * animated,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = arcSize,
            style = Stroke(stroke, cap = StrokeCap.Round)
        )
    }
}

@Composable
internal fun MilestoneConnectionLine(
    progress: Float,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(progress.coerceIn(0f, 1f), tween(800), label = "milestoneLine")
    Canvas(modifier = modifier) {
        val y = size.height / 2f
        val start = 20.dp.toPx()
        val end = size.width - 20.dp.toPx()
        drawLine(
            color = Color.White.copy(alpha = 0.12f),
            start = Offset(start, y),
            end = Offset(end, y),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
        drawLine(
            color = Color(0xFF00F5FF),
            start = Offset(start, y),
            end = Offset(start + (end - start) * animated, y),
            strokeWidth = 4.dp.toPx(),
            cap = StrokeCap.Round
        )
    }
}

internal fun AchievementRarity.accentColor(): Color = when (this) {
    AchievementRarity.COMMON -> Color(0xFF00F5FF)
    AchievementRarity.UNCOMMON -> Color(0xFF4AFF8A)
    AchievementRarity.RARE -> Color(0xFF2E9BFF)
    AchievementRarity.EPIC -> Color(0xFF9B5CFF)
    AchievementRarity.LEGENDARY -> Color(0xFFFFB02E)
    AchievementRarity.MYTHIC -> Color(0xFFFF5BD6)
}

internal fun AchievementCategory.accentColor(): Color = when (this) {
    AchievementCategory.ALL -> Color(0xFF00F5FF)
    AchievementCategory.STEPS -> Color(0xFF34FF88)
    AchievementCategory.DISTANCE -> Color(0xFF2EA8FF)
    AchievementCategory.CALORIES -> Color(0xFFFF7A2E)
    AchievementCategory.STREAKS -> Color(0xFFFFB02E)
    AchievementCategory.WORKOUTS -> Color(0xFF4BC7FF)
    AchievementCategory.TIME -> Color(0xFF00F5FF)
    AchievementCategory.WEATHER -> Color(0xFF76D6FF)
    AchievementCategory.SPECIAL -> Color(0xFFB26DFF)
}
