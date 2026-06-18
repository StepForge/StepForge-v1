package com.example.stepforge.ui.progress

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import com.example.stepforge.ui.achievements.AchievementCategory
import java.util.Locale

@Composable
internal fun ProgressHeader(
    selectedRange: ProgressRange,
    compact: Boolean,
    scale: Float,
    onBack: () -> Unit,
    onRangeSelected: (ProgressRange) -> Unit
) {
    val colors = progressPalette()
    val headerScale = if (compact) scale * 0.86f else scale * 0.90f
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((10f * headerScale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size((30f * headerScale).dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = stringResource(R.string.progress_back),
                tint = colors.textMain,
                modifier = Modifier.size((23f * headerScale).dp)
            )
        }

        Text(
            text = stringResource(R.string.progress_title),
            color = colors.textMain,
            fontSize = (27f * headerScale).sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            letterSpacing = (-0.55f).sp,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis
        )

        RangeSelector(
            selectedRange = selectedRange,
            scale = headerScale,
            onRangeSelected = onRangeSelected,
            modifier = Modifier.widthIn(
                min = (146f * headerScale).dp,
                max = (176f * headerScale).dp
            )
        )
    }
}

@Composable
private fun RangeSelector(
    selectedRange: ProgressRange,
    scale: Float,
    modifier: Modifier = Modifier,
    onRangeSelected: (ProgressRange) -> Unit
) {
    val colors = progressPalette()
    val shape = RoundedCornerShape((24f * scale).dp)
    Row(
        modifier = modifier
            .height((31f * scale).dp)
            .clip(shape)
            .background(if (colors.isDark) Color.White.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.82f))
            .border((1f * scale).dp, colors.cardStroke.copy(alpha = 0.76f), shape)
            .padding((3f * scale).dp),
        horizontalArrangement = Arrangement.spacedBy((2f * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ProgressRange.entries.forEach { range ->
            val selected = range == selectedRange
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape((20f * scale).dp))
                    .background(
                        if (selected) Brush.horizontalGradient(listOf(colors.cyanGlow, colors.cyan))
                        else Brush.horizontalGradient(listOf(Color.Transparent, Color.Transparent))
                    )
                    .border(
                        width = if (selected) (1f * scale).dp else 0.dp,
                        color = if (selected) Color.White.copy(alpha = 0.24f) else Color.Transparent,
                        shape = RoundedCornerShape((20f * scale).dp)
                    )
                    .clickable { onRangeSelected(range) },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(range.labelRes),
                    color = if (selected) colors.selectedText else colors.textMuted,
                    fontSize = (10.8f * scale).sp,
                    fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    lineHeight = (10.8f * scale).sp,
                    style = LocalTextStyle.current.copy(
                        platformStyle = PlatformTextStyle(includeFontPadding = false)
                    ),
                    modifier = Modifier
                        .fillMaxHeight()
                        .wrapContentSize(Alignment.Center)
                )
            }
        }
    }
}

@Composable
internal fun ProgressHeroCard(
    state: ProgressDashboardState,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val colors = progressPalette()
    val rangeLabel = stringResource(state.range.labelRes).lowercase(Locale.getDefault())
    val numberFont = when {
        state.totalSteps >= 1_000_000 -> 31f
        state.totalSteps >= 100_000 -> 37f
        else -> 43f
    }

    PremiumCard(
        modifier = modifier.fillMaxWidth(),
        scale = scale,
        contentPadding = (14f * scale).dp
    ) {
        Column(verticalArrangement = Arrangement.spacedBy((10f * scale).dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = (8f * scale).dp),
                    verticalArrangement = Arrangement.spacedBy((4f * scale).dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Rounded.ShowChart, colors.cyan, scale)
                        Spacer(Modifier.width((9f * scale).dp))
                        Text(
                            text = stringResource(R.string.progress_total_steps).uppercase(Locale.getDefault()),
                            color = colors.textMuted,
                            fontSize = (10.8f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (1.6f * scale).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                    }
                    Text(
                        text = formatNumber(state.totalSteps),
                        color = colors.cyan,
                        fontSize = (numberFont * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        letterSpacing = (-1.4f).sp,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = stringResource(R.string.progress_steps_unit),
                        color = colors.textMain,
                        fontSize = (22f * scale).sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = formatTrend(state.trendPercent),
                            color = if (state.trendPercent >= 0f) colors.green else colors.orange,
                            fontSize = (15.5f * scale).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Spacer(Modifier.width((7f * scale).dp))
                        Text(
                            text = stringResource(R.string.progress_vs_last_range, rangeLabel),
                            color = colors.textMuted,
                            fontSize = (11.8f * scale).sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                CircularGoalRing(
                    percent = state.goalPercent,
                    goalText = stringResource(R.string.progress_goal_ring_text, formatCompactGoalNumber(state.periodGoal)),
                    scale = scale,
                    modifier = Modifier.size((108f * scale).dp)
                )
            }

            ProgressLineChart(
                points = state.chartPoints,
                scale = scale,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
internal fun ProgressMetricCards(
    state: ProgressDashboardState,
    scale: Float
) {
    val colors = progressPalette()
    val sparkValues = state.chartPoints.map { it.steps }
    val rangeLabel = stringResource(state.range.labelRes).lowercase(Locale.getDefault())
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy((9f * scale).dp)
    ) {
        MetricCard(
            icon = Icons.Rounded.LocationOn,
            label = stringResource(R.string.progress_distance).uppercase(Locale.getDefault()),
            value = formatKm(state.distanceKm),
            unit = stringResource(R.string.progress_km_unit),
            accent = colors.cyan,
            sparkValues = sparkValues,
            scale = scale,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            iconRes = R.drawable.progress_fire_icon,
            label = stringResource(R.string.progress_calories).uppercase(Locale.getDefault()),
            value = formatNumber(state.calories),
            unit = stringResource(R.string.progress_kcal_unit),
            accent = colors.orange,
            sparkValues = sparkValues.map { it.stepCalories() },
            scale = scale,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            icon = Icons.Rounded.AccessTime,
            label = stringResource(R.string.progress_active_time).uppercase(Locale.getDefault()),
            value = formatHoursOnly(state.activeMinutes),
            unit = stringResource(R.string.progress_this_range, rangeLabel),
            accent = colors.cyan,
            sparkValues = sparkValues.map { it.stepActiveMinutes() },
            scale = scale,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector? = null,
    iconRes: Int? = null,
    label: String,
    value: String,
    unit: String,
    accent: Color,
    sparkValues: List<Int>,
    scale: Float,
    modifier: Modifier = Modifier
) {
    val colors = progressPalette()
    val valueFont = when {
        value.length >= 7 -> 17.5f
        value.length >= 5 -> 18.8f
        else -> 20.2f
    }
    val unitFont = if (unit.length > 7) 7.5f else 8.2f
    PremiumCard(
        modifier = modifier.height((112f * scale).dp),
        scale = scale,
        contentPadding = (8.5f * scale).dp
    ) {
        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.spacedBy((4f * scale).dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((5.5f * scale).dp)
            ) {
                MetricIconBadge(
                    icon = icon,
                    iconRes = iconRes,
                    accent = accent,
                    scale = scale
                )
                Text(
                    text = label,
                    color = colors.textMuted,
                    fontSize = (7.4f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (0.28f * scale).sp,
                    maxLines = 1,
                    textAlign = TextAlign.End,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy((3.5f * scale).dp)
            ) {
                Text(
                    text = value,
                    color = colors.textMain,
                    fontSize = (valueFont * scale).sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
                Text(
                    text = unit,
                    color = colors.textMuted,
                    fontSize = (unitFont * scale).sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = (2.4f * scale).dp),
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }

            Spacer(Modifier.weight(1f))
            MiniSparkline(
                values = sparkValues,
                color = accent,
                scale = scale,
                modifier = Modifier.height((18f * scale).dp)
            )
        }
    }
}

@Composable
internal fun WeeklyStreakCard(
    state: ProgressDashboardState,
    scale: Float
) {
    val colors = progressPalette()
    PremiumCard(
        modifier = Modifier.fillMaxWidth(),
        scale = scale,
        contentPadding = (12f * scale).dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy((9f * scale).dp)
        ) {
            WeeklyProgressRing(active = state.currentStreakDays > 0, scale = scale)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy((2.5f * scale).dp)
            ) {
                Text(
                    text = stringResource(R.string.progress_weekly_streak).uppercase(Locale.getDefault()),
                    color = colors.textMuted,
                    fontSize = (8.4f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (0.55f * scale).sp,
                    maxLines = 2,
                    lineHeight = (9.5f * scale).sp,
                    overflow = TextOverflow.Clip
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = state.currentStreakDays.toString(),
                        color = colors.textMain,
                        fontSize = (24f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Spacer(Modifier.width((4f * scale).dp))
                    Text(
                        text = stringResource(R.string.progress_days_unit),
                        color = colors.textMuted,
                        fontSize = (10.2f * scale).sp,
                        modifier = Modifier.padding(bottom = (3f * scale).dp),
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }
                Text(
                    text = if (state.currentStreakDays > 0) stringResource(R.string.progress_keep_it_up_fire) else stringResource(R.string.progress_start_streak_today),
                    color = colors.textMuted,
                    fontSize = (8.9f * scale).sp,
                    lineHeight = (10.5f * scale).sp,
                    maxLines = 2,
                    overflow = TextOverflow.Clip
                )
            }
            WeekCheckRow(days = state.weekDays, scale = scale)
        }
    }
}

@Composable
private fun WeekCheckRow(
    days: List<DailyProgress>,
    scale: Float
) {
    val colors = progressPalette()
    Row(
        horizontalArrangement = Arrangement.spacedBy((3.2f * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        days.forEach { day ->
            val letter = day.date.dayOfWeek.getDisplayName(java.time.format.TextStyle.SHORT, Locale.getDefault()).replace(".", "").take(1)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = letter,
                    color = if (day.date == LocalDateProvider.today()) colors.cyan else colors.textMuted,
                    fontSize = (8.3f * scale).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height((4f * scale).dp))
                Box(
                    modifier = Modifier
                        .size((21.5f * scale).dp)
                        .clip(CircleShape)
                        .background(if (day.isActive) colors.mint.copy(alpha = 0.14f) else Color.Transparent)
                        .border(
                            (1.2f * scale).dp,
                            if (day.isActive) colors.mint else colors.cyan.copy(alpha = 0.35f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (day.isActive) {
                        Image(
                            painter = painterResource(R.drawable.progress_check_icon),
                            contentDescription = null,
                            colorFilter = ColorFilter.tint(colors.mint),
                            modifier = Modifier.size((12f * scale).dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun AchievementUnlockCard(
    achievement: AchievementUnlockProgress,
    scale: Float,
    onClick: () -> Unit
) {
    val colors = progressPalette()
    PremiumCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        scale = scale,
        contentPadding = (11f * scale).dp
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = null,
                tint = colors.textMuted.copy(alpha = 0.86f),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size((17f * scale).dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = (17f * scale).dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy((9f * scale).dp)
            ) {
                AchievementBadge(achievement = achievement, scale = scale)

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy((3.5f * scale).dp)
                ) {
                    Text(
                        text = stringResource(R.string.progress_achievement_unlocking).uppercase(Locale.getDefault()),
                        color = colors.green,
                        fontSize = (8.4f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (0.65f * scale).sp,
                        maxLines = 2,
                        lineHeight = (9.4f * scale).sp,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = stringResource(achievement.titleRes),
                        color = colors.textMain,
                        fontSize = (15f * scale).sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 2,
                        lineHeight = (16.4f * scale).sp,
                        overflow = TextOverflow.Clip
                    )
                    Text(
                        text = stringResource(achievement.descriptionRes),
                        color = colors.textMuted,
                        fontSize = (9f * scale).sp,
                        lineHeight = (10.2f * scale).sp,
                        maxLines = 2,
                        overflow = TextOverflow.Clip
                    )
                    ProgressBarLine(
                        percent = achievement.percent,
                        color = colors.cyan,
                        scale = scale,
                        modifier = Modifier.fillMaxWidth(0.88f)
                    )
                    Text(
                        text = achievementProgressText(achievement),
                        color = colors.green,
                        fontSize = (9.8f * scale).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Clip
                    )
                }

                Box(
                    modifier = Modifier
                        .width((50f * scale).dp)
                        .height((54f * scale).dp)
                        .clip(RoundedCornerShape((13f * scale).dp))
                        .background(if (colors.isDark) Color.White.copy(alpha = 0.055f) else Color.White.copy(alpha = 0.70f))
                        .border((1f * scale).dp, colors.cyan.copy(alpha = 0.35f), RoundedCornerShape((13f * scale).dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${achievement.percent}%",
                            color = colors.cyan,
                            fontSize = (16.2f * scale).sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip
                        )
                        Text(
                            text = stringResource(R.string.progress_to_unlock),
                            color = colors.textMuted,
                            fontSize = (7.1f * scale).sp,
                            textAlign = TextAlign.Center,
                            lineHeight = (8f * scale).sp,
                            maxLines = 2,
                            overflow = TextOverflow.Clip
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementBadge(
    achievement: AchievementUnlockProgress,
    scale: Float
) {
    val colors = progressPalette()
    Box(
        modifier = Modifier
            .size((58f * scale).dp)
            .clip(RoundedCornerShape((16f * scale).dp))
            .background(Brush.radialGradient(listOf(colors.cyan.copy(alpha = 0.22f), colors.cardBottom)))
            .border((1.6f * scale).dp, Color(0xFFF3C15A).copy(alpha = 0.82f), RoundedCornerShape((16f * scale).dp)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(achievement.iconRes),
            contentDescription = null,
            modifier = Modifier.size((45f * scale).dp)
        )
    }
}

@Composable
private fun achievementProgressText(achievement: AchievementUnlockProgress): String {
    val suffix = when (achievement.category) {
        AchievementCategory.STEPS -> stringResource(R.string.progress_steps_unit)
        AchievementCategory.DISTANCE -> stringResource(R.string.progress_km_unit)
        AchievementCategory.CALORIES -> stringResource(R.string.progress_kcal_unit)
        AchievementCategory.TIME -> "min"
        AchievementCategory.STREAKS -> stringResource(R.string.progress_days_unit)
        else -> ""
    }
    val current = formatAchievementAmount(achievement.current, achievement.category)
    val target = formatAchievementAmount(achievement.target, achievement.category)
    return if (suffix.isBlank()) "$current / $target" else "$current / $target $suffix"
}

private fun formatAchievementAmount(value: Float, category: AchievementCategory): String {
    return when (category) {
        AchievementCategory.DISTANCE -> formatDecimal(value, if (value < 10f) 1 else 0)
        AchievementCategory.TIME -> value.toInt().coerceAtLeast(0).toString()
        else -> when {
            value >= 1_000_000f -> "${formatDecimal(value / 1_000_000f, 1)}M"
            value >= 10_000f -> "${(value / 1_000f).toInt()}K"
            value % 1f == 0f -> value.toInt().toString()
            else -> formatDecimal(value, 1)
        }
    }
}

@Composable
internal fun PremiumCard(
    modifier: Modifier = Modifier,
    scale: Float,
    contentPadding: androidx.compose.ui.unit.Dp,
    content: @Composable () -> Unit
) {
    val colors = progressPalette()
    val shape = RoundedCornerShape((22f * scale).dp)
    Box(
        modifier = modifier
            .shadow(
                (12f * scale).dp,
                shape,
                clip = false,
                ambientColor = colors.cyan.copy(alpha = if (colors.isDark) 0.12f else 0.08f),
                spotColor = colors.cyan.copy(alpha = if (colors.isDark) 0.11f else 0.07f)
            )
            .clip(shape)
            .background(colors.cardBrush())
            .border((1f * scale).dp, colors.cardStroke.copy(alpha = 0.86f), shape)
            .padding(contentPadding)
    ) {
        content()
    }
}

@Composable
private fun MetricIconBadge(
    icon: ImageVector?,
    iconRes: Int?,
    accent: Color,
    scale: Float
) {
    Box(
        modifier = Modifier
            .size((29f * scale).dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        if (iconRes != null) {
            Image(
                painter = painterResource(iconRes),
                contentDescription = null,
                modifier = Modifier.size((17.5f * scale).dp)
            )
        } else if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size((17.5f * scale).dp)
            )
        }
    }
}

@Composable
private fun IconBadge(
    icon: ImageVector,
    accent: Color,
    scale: Float
) {
    Box(
        modifier = Modifier
            .size((38f * scale).dp)
            .clip(CircleShape)
            .background(accent.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size((21f * scale).dp)
        )
    }
}

private object LocalDateProvider {
    fun today(): java.time.LocalDate = java.time.LocalDate.now()
}
