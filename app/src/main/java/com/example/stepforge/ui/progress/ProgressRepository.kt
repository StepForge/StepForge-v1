package com.example.stepforge.ui.progress

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.DailyStepsDao
import com.example.stepforge.data.WorkoutSessionDao
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.achievements.AchievementItemUi
import com.example.stepforge.ui.achievements.AchievementsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

internal class ProgressRepository(
    private val appContext: Context,
    private val dao: DailyStepsDao,
    private val workoutDao: WorkoutSessionDao
) {
    private val stepGoalKey = intPreferencesKey("step_goal")

    fun observeDashboard(rangeFlow: Flow<ProgressRange>): Flow<ProgressDashboardState> {
        return combine(
            dao.observeAllSteps(),
            workoutDao.observeAll(),
            appContext.stepforgeStore.data,
            rangeFlow
        ) { rows, workouts, prefs, range ->
            val goal = (prefs[stepGoalKey] ?: 10_000).coerceAtLeast(1)
            buildDashboard(rows, workouts, goal, range)
        }
    }

    private fun buildDashboard(
        rows: List<DailySteps>,
        workouts: List<com.example.stepforge.data.WorkoutSession>,
        stepGoal: Int,
        range: ProgressRange
    ): ProgressDashboardState {
        val today = LocalDate.now()
        val start = today.minusDays((range.days - 1).toLong())
        val previousStart = start.minusDays(range.days.toLong())
        val previousEnd = start.minusDays(1)
        val activeThreshold = minOf(stepGoal, 1_000).coerceAtLeast(250)

        val stepsByDate = rows.mapNotNull { row ->
            val date = runCatching { LocalDate.parse(row.date) }.getOrNull() ?: return@mapNotNull null
            date to row.steps.coerceAtLeast(0)
        }.toMap()

        val days = (0 until range.days).map { offset ->
            val date = start.plusDays(offset.toLong())
            buildDaily(date, stepsByDate[date] ?: 0, stepGoal, activeThreshold)
        }

        val totalSteps = days.sumOf { it.steps }
        val previousTotal = sumStepsBetween(stepsByDate, previousStart, previousEnd)
        val trend = if (previousTotal > 0) {
            ((totalSteps - previousTotal) * 100f / previousTotal.toFloat())
        } else if (totalSteps > 0) 100f else 0f

        val weekStart = startOfWeek(today)
        val weekDays = (0..6).map { offset ->
            val date = weekStart.plusDays(offset.toLong())
            buildDaily(date, stepsByDate[date] ?: 0, stepGoal, activeThreshold)
        }

        val currentStreak = computeCurrentStreak(today, stepsByDate, activeThreshold)
        val weeklyGoalHits = weekDays.count { it.goalHit }
        val bestDay = days.maxByOrNull { it.steps }?.takeIf { it.steps > 0 }
        val periodGoal = stepGoal * range.days
        val goalPercent = ((totalSteps * 100f) / periodGoal.toFloat()).roundToInt().coerceAtLeast(0)

        val achievementsState = AchievementsRepository.buildState(rows, workouts, stepGoal)
        val achievement = (achievementsState.nextTarget
            ?: achievementsState.bestAchievement
            ?: achievementsState.achievements.firstOrNull())
            ?.toProgressAchievement()
            ?: ProgressDashboardState.empty(range).achievement

        return ProgressDashboardState(
            range = range,
            today = today,
            stepGoal = stepGoal,
            periodGoal = periodGoal,
            totalSteps = totalSteps,
            distanceKm = totalSteps.stepDistanceKm(),
            calories = totalSteps.stepCalories(),
            activeMinutes = totalSteps.stepActiveMinutes(),
            goalPercent = goalPercent,
            trendPercent = trend,
            chartPoints = buildChartPoints(days, range, today),
            weekDays = weekDays,
            currentStreakDays = currentStreak,
            bestDay = bestDay,
            weeklyGoalHits = weeklyGoalHits,
            achievement = achievement
        )
    }


    private fun AchievementItemUi.toProgressAchievement(): AchievementUnlockProgress {
        val percent = (progress.coerceIn(0f, 1f) * 100f).roundToInt().coerceIn(0, 100)
        return AchievementUnlockProgress(
            titleRes = definition.titleRes,
            descriptionRes = definition.descriptionRes,
            iconRes = definition.iconRes,
            current = current,
            target = definition.target,
            progress = progress.coerceIn(0f, 1f),
            percent = percent,
            category = definition.category,
            unlocked = unlocked
        )
    }

    private fun buildDaily(
        date: LocalDate,
        steps: Int,
        stepGoal: Int,
        activeThreshold: Int
    ): DailyProgress {
        return DailyProgress(
            date = date,
            steps = steps,
            distanceKm = steps.stepDistanceKm(),
            calories = steps.stepCalories(),
            activeMinutes = steps.stepActiveMinutes(),
            isActive = steps >= activeThreshold,
            goalHit = steps >= stepGoal
        )
    }

    private fun buildChartPoints(
        days: List<DailyProgress>,
        range: ProgressRange,
        today: LocalDate
    ): List<ChartPoint> {
        return when (range) {
            ProgressRange.D7 -> days.map {
                ChartPoint(
                    label = shortDayLabel(it.date),
                    steps = it.steps
                )
            }

            ProgressRange.W4 -> days.chunked(7).takeLast(4).mapIndexed { index, chunk ->
                ChartPoint(label = "W${index + 1}", steps = chunk.sumOf { it.steps })
            }

            ProgressRange.M6 -> {
                val monthTotals = days.groupBy { YearMonth.from(it.date) }
                    .mapValues { entry -> entry.value.sumOf { it.steps } }
                val firstMonth = YearMonth.from(today).minusMonths(5)
                (0..5).map { index ->
                    val month = firstMonth.plusMonths(index.toLong())
                    ChartPoint(
                        label = shortMonthLabel(month),
                        steps = monthTotals[month] ?: 0
                    )
                }
            }
        }
    }

    private fun shortDayLabel(date: LocalDate): String {
        return date.dayOfWeek
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .replace(".", "")
            .take(3)
    }

    private fun shortMonthLabel(month: YearMonth): String {
        return month.month
            .getDisplayName(TextStyle.SHORT, Locale.getDefault())
            .replace(".", "")
            .take(3)
    }

    private fun computeCurrentStreak(
        today: LocalDate,
        stepsByDate: Map<LocalDate, Int>,
        activeThreshold: Int
    ): Int {
        var cursor = today
        var streak = 0
        while ((stepsByDate[cursor] ?: 0) >= activeThreshold) {
            streak++
            cursor = cursor.minusDays(1)
            if (streak > 999) break
        }
        return streak
    }

    private fun sumStepsBetween(
        stepsByDate: Map<LocalDate, Int>,
        start: LocalDate,
        end: LocalDate
    ): Int {
        if (end.isBefore(start)) return 0
        var date = start
        var total = 0
        while (!date.isAfter(end)) {
            total += stepsByDate[date] ?: 0
            date = date.plusDays(1)
        }
        return total
    }

    private fun startOfWeek(date: LocalDate): LocalDate {
        val diff = date.dayOfWeek.value - DayOfWeek.MONDAY.value
        return date.minusDays(diff.toLong())
    }
}
