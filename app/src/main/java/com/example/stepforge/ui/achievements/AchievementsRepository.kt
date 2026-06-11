package com.example.stepforge.ui.achievements

import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.WorkoutSession
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.max

internal object AchievementsRepository {
    fun buildState(
        dailySteps: List<DailySteps>,
        workouts: List<WorkoutSession>,
        stepGoal: Int
    ): AchievementsUiState {
        val cleanDays = dailySteps
            .mapNotNull { entry ->
                val date = runCatching { LocalDate.parse(entry.date) }.getOrNull() ?: return@mapNotNull null
                date to entry.steps.coerceAtLeast(0)
            }
            .groupBy { it.first }
            .map { (date, entries) -> date to entries.maxOf { it.second } }
            .sortedBy { it.first }

        val dayMap = cleanDays.toMap()
        val steps = cleanDays.map { it.second }
        val totalSteps = steps.sumOf { it.toLong() }
        val bestDaySteps = steps.maxOrNull() ?: 0
        val goalHits = steps.count { it >= stepGoal.coerceAtLeast(1) }
        val activeDays = steps.count { it > 0 }
        val totalDistanceKm = totalSteps * 0.00075f
        val totalCalories = (totalSteps * 0.04f).toInt().coerceAtLeast(0)
        val bestCalories = (bestDaySteps * 0.04f).toInt().coerceAtLeast(0)
        val totalActiveMinutes = (totalSteps / 105f).toInt().coerceAtLeast(0)
        val bestActiveMinutes = (bestDaySteps / 105f).toInt().coerceAtLeast(0)
        val currentStreak = currentGoalStreak(dayMap, stepGoal.coerceAtLeast(1))
        val longestStreak = longestGoalStreak(dayMap, stepGoal.coerceAtLeast(1))
        val maxActiveDaysInWeek = maxActiveDaysInRollingWindow(cleanDays, 7)
        val maxActiveDaysInMonth = maxActiveDaysInRollingWindow(cleanDays, 30)
        val workoutCount = workouts.size
        val walkWorkouts = workouts.count { it.source.contains("walk", ignoreCase = true) || it.steps > 0 }
        val runWorkouts = workouts.count { it.source.contains("run", ignoreCase = true) || it.avgStepsPerMinute >= 130 }
        val cycleWorkouts = workouts.count { it.source.contains("cycl", ignoreCase = true) || it.source.contains("bike", ignoreCase = true) }
        val earlyWorkouts = workouts.count { it.startHour() in 5..8 }
        val nightWorkouts = workouts.count { it.startHour() >= 21 || it.startHour() <= 4 }
        val weekendActiveDays = cleanDays.count { (date, value) -> value > 0 && (date.dayOfWeek == DayOfWeek.SATURDAY || date.dayOfWeek == DayOfWeek.SUNDAY) }
        val comebackCount = comebackEvents(cleanDays, stepGoal.coerceAtLeast(1))
        val newRecord = if (bestDaySteps > 0) 1f else 0f
        val weeklyChampion = if (maxActiveDaysInWeek >= 5) 1f else 0f

        val values = mapOf(
            "first_steps" to totalSteps.toFloat(),
            "daily_goal" to goalHits.toFloat(),
            "power_day" to bestDaySteps.toFloat(),
            "monster_walk" to bestDaySteps.toFloat(),
            "total_steps_bronze" to totalSteps.toFloat(),
            "total_steps_silver" to totalSteps.toFloat(),
            "total_steps_gold" to totalSteps.toFloat(),
            "million_steps" to totalSteps.toFloat(),
            "distance_explorer" to totalDistanceKm,
            "city_walker" to totalDistanceKm,
            "long_road" to totalDistanceKm,
            "marathon_spirit" to totalDistanceKm,
            "distance_master" to totalDistanceKm,
            "first_burn" to totalCalories.toFloat(),
            "calorie_burner" to totalCalories.toFloat(),
            "fire_engine" to totalCalories.toFloat(),
            "inferno_day" to bestCalories.toFloat(),
            "active_hour" to bestActiveMinutes.toFloat(),
            "two_hour_push" to bestActiveMinutes.toFloat(),
            "endurance_pro" to totalActiveMinutes.toFloat(),
            "first_workout" to workoutCount.toFloat(),
            "workout_builder" to workoutCount.toFloat(),
            "workout_machine" to workoutCount.toFloat(),
            "walker" to walkWorkouts.toFloat(),
            "runner" to runWorkouts.toFloat(),
            "cyclist" to cycleWorkouts.toFloat(),
            "streak_spark" to longestStreak.toFloat(),
            "week_streak" to longestStreak.toFloat(),
            "month_streak" to longestStreak.toFloat(),
            "unbreakable_streak" to longestStreak.toFloat(),
            "streak_shield" to 0f,
            "second_chance" to 0f,
            "building_week" to maxActiveDaysInWeek.toFloat(),
            "strong_week" to maxActiveDaysInWeek.toFloat(),
            "perfect_week" to maxActiveDaysInWeek.toFloat(),
            "active_month" to maxActiveDaysInMonth.toFloat(),
            "elite_month" to maxActiveDaysInMonth.toFloat(),
            "goal_hunter" to goalHits.toFloat(),
            "goal_machine" to goalHits.toFloat(),
            "goal_master" to goalHits.toFloat(),
            "early_bird" to earlyWorkouts.toFloat(),
            "night_walker" to nightWorkouts.toFloat(),
            "weekend_warrior" to weekendActiveDays.toFloat(),
            "rain_walker" to 0f,
            "snow_walker" to 0f,
            "comeback" to comebackCount.toFloat(),
            "new_record" to newRecord,
            "top_day" to newRecord,
            "weekly_champion" to weeklyChampion,
            "legendary_walker" to totalSteps.toFloat()
        )

        val items = achievementDefinitions.map { definition ->
            val current = values[definition.id] ?: 0f
            val progress = if (definition.target <= 0f) 0f else (current / definition.target).coerceIn(0f, 1f)
            AchievementItemUi(
                definition = definition,
                current = current,
                progress = progress,
                unlocked = current >= definition.target
            )
        }
        val unlocked = items.filter { it.unlocked }
        val inProgress = items.filter { !it.unlocked && it.progress > 0f }
            .sortedWith(compareByDescending<AchievementItemUi> { it.progress }.thenByDescending { it.definition.rarity.points })
        val totalPoints = unlocked.sumOf { it.definition.rarity.points }
        val summaries = AchievementCategory.values()
            .filter { it != AchievementCategory.ALL }
            .map { category ->
                val bucket = items.filter { it.definition.category == category }
                AchievementCategorySummaryUi(
                    category = category,
                    unlocked = bucket.count { it.unlocked },
                    total = bucket.size
                )
            }

        return AchievementsUiState(
            achievements = items,
            unlockedCount = unlocked.size,
            totalCount = items.size,
            completion = if (items.isEmpty()) 0f else unlocked.size / items.size.toFloat(),
            totalPoints = totalPoints,
            currentStreak = currentStreak,
            longestStreak = longestStreak,
            bestAchievement = unlocked.maxByOrNull { it.definition.rarity.points } ?: inProgress.firstOrNull(),
            nextTarget = inProgress.firstOrNull(),
            recentUnlocked = unlocked.sortedByDescending { it.definition.rarity.points }.take(5),
            nextTargets = inProgress.take(5),
            categorySummaries = summaries
        )
    }

    private fun WorkoutSession.startHour(): Int = runCatching {
        Instant.ofEpochMilli(startTime).atZone(ZoneId.systemDefault()).hour
    }.getOrDefault(12)

    private fun currentGoalStreak(dayMap: Map<LocalDate, Int>, stepGoal: Int): Int {
        var date = LocalDate.now()
        if ((dayMap[date] ?: 0) < stepGoal) date = date.minusDays(1)
        var streak = 0
        while ((dayMap[date] ?: 0) >= stepGoal) {
            streak++
            date = date.minusDays(1)
        }
        return streak
    }

    private fun longestGoalStreak(dayMap: Map<LocalDate, Int>, stepGoal: Int): Int {
        if (dayMap.isEmpty()) return 0
        val start = dayMap.keys.minOrNull() ?: return 0
        val end = dayMap.keys.maxOrNull() ?: return 0
        var best = 0
        var current = 0
        var date = start
        while (!date.isAfter(end)) {
            if ((dayMap[date] ?: 0) >= stepGoal) {
                current++
                best = max(best, current)
            } else {
                current = 0
            }
            date = date.plusDays(1)
        }
        return best
    }

    private fun maxActiveDaysInRollingWindow(days: List<Pair<LocalDate, Int>>, windowDays: Long): Int {
        var best = 0
        val activeDates = days.filter { it.second > 0 }.map { it.first }
        activeDates.forEach { end ->
            val start = end.minusDays(windowDays - 1)
            best = max(best, activeDates.count { !it.isBefore(start) && !it.isAfter(end) })
        }
        return best
    }

    private fun comebackEvents(days: List<Pair<LocalDate, Int>>, stepGoal: Int): Int {
        if (days.size < 4) return 0
        val map = days.toMap()
        return days.count { (date, steps) ->
            steps >= stepGoal &&
                    (map[date.minusDays(1)] ?: 0) < 1_000 &&
                    (map[date.minusDays(2)] ?: 0) < 1_000 &&
                    (map[date.minusDays(3)] ?: 0) < 1_000
        }
    }
}
