package com.example.stepforge.ui.progress

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import com.example.stepforge.R
import com.example.stepforge.ui.achievements.AchievementCategory
import com.example.stepforge.ui.achievements.achievementDefinitions
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale
import kotlin.math.roundToInt

internal enum class ProgressRange(@StringRes val labelRes: Int, val days: Int) {
    D7(R.string.progress_range_7d, 7),
    W4(R.string.progress_range_4w, 28),
    M6(R.string.progress_range_6m, 183)
}

@Immutable
internal data class DailyProgress(
    val date: LocalDate,
    val steps: Int,
    val distanceKm: Float,
    val calories: Int,
    val activeMinutes: Int,
    val isActive: Boolean,
    val goalHit: Boolean
)

@Immutable
internal data class ChartPoint(
    val label: String,
    val steps: Int
)

@Immutable
internal data class AchievementUnlockProgress(
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
    @DrawableRes val iconRes: Int,
    val current: Float,
    val target: Float,
    val progress: Float,
    val percent: Int,
    val category: AchievementCategory,
    val unlocked: Boolean
)

@Immutable
internal data class ProgressDashboardState(
    val range: ProgressRange,
    val today: LocalDate,
    val stepGoal: Int,
    val periodGoal: Int,
    val totalSteps: Int,
    val distanceKm: Float,
    val calories: Int,
    val activeMinutes: Int,
    val goalPercent: Int,
    val trendPercent: Float,
    val chartPoints: List<ChartPoint>,
    val weekDays: List<DailyProgress>,
    val currentStreakDays: Int,
    val bestDay: DailyProgress?,
    val weeklyGoalHits: Int,
    val achievement: AchievementUnlockProgress
) {
    companion object {
        fun empty(range: ProgressRange = ProgressRange.D7): ProgressDashboardState {
            val today = LocalDate.now()
            return ProgressDashboardState(
                range = range,
                today = today,
                stepGoal = 10_000,
                periodGoal = 10_000 * range.days,
                totalSteps = 0,
                distanceKm = 0f,
                calories = 0,
                activeMinutes = 0,
                goalPercent = 0,
                trendPercent = 0f,
                chartPoints = emptyList(),
                weekDays = emptyList(),
                currentStreakDays = 0,
                bestDay = null,
                weeklyGoalHits = 0,
                achievement = achievementDefinitions.first().let { definition ->
                    AchievementUnlockProgress(
                        titleRes = definition.titleRes,
                        descriptionRes = definition.descriptionRes,
                        iconRes = definition.iconRes,
                        current = 0f,
                        target = definition.target,
                        progress = 0f,
                        percent = 0,
                        category = definition.category,
                        unlocked = false
                    )
                }
            )
        }
    }
}

internal fun Int.stepDistanceKm(): Float = this * 0.00073f
internal fun Int.stepCalories(): Int = (this * 0.0385f).roundToInt().coerceAtLeast(0)
internal fun Int.stepActiveMinutes(): Int = (this / 115f).roundToInt().coerceAtLeast(0)

internal fun formatNumber(value: Int): String = NumberFormat.getIntegerInstance(Locale.getDefault()).format(value)
internal fun formatDecimal(value: Float, decimals: Int = 1): String = "%.${decimals}f".format(Locale.getDefault(), value)

internal fun formatKm(value: Float): String = formatDecimal(value, 1)

internal fun formatCompactGoalNumber(value: Int): String = when {
    value >= 1_000_000 -> "${formatDecimal(value / 1_000_000f, 2)}M"
    value >= 100_000 -> "${value / 1_000}K"
    value >= 10_000 -> "${value / 1_000}K"
    else -> formatNumber(value)
}

internal fun formatMinutes(totalMinutes: Int): String {
    val safe = totalMinutes.coerceAtLeast(0)
    val hours = safe / 60
    val minutes = safe % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}

internal fun formatHoursOnly(totalMinutes: Int): String {
    val hours = (totalMinutes.coerceAtLeast(0) / 60).coerceAtLeast(0)
    return "${hours}h"
}

internal fun formatTrend(value: Float): String {
    val prefix = if (value >= 0f) "↑" else "↓"
    return "$prefix ${formatDecimal(kotlin.math.abs(value), 1)}%"
}
