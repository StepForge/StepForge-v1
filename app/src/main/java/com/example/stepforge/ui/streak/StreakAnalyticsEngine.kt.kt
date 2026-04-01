package com.example.stepforge.ui.streak

import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.HourlySteps
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.math.sqrt

object StreakAnalyticsEngine {

    private val ymd = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    fun perfectThreshold(goal: Int): Int {
        return (goal * 1.5f).roundToInt().coerceAtLeast(goal)
    }

    fun computeCurrentStreakIncludingToday(
        dailyByDate: Map<String, Int>,
        goal: Int,
        today: String
    ): Int {
        var streak = 0
        var cal = Calendar.getInstance().apply { time = ymd.parse(today) ?: time }

        while (true) {
            val d = ymd.format(cal.time)
            val steps = dailyByDate[d] ?: 0
            if (steps >= goal) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    fun computeLongestStreak(
        dailySortedAsc: List<DailySteps>,
        goal: Int
    ): Int {
        var best = 0
        var temp = 0
        for (d in dailySortedAsc) {
            if (d.steps >= goal) {
                temp++
                if (temp > best) best = temp
            } else {
                temp = 0
            }
        }
        return best
    }

    fun computeMostActiveDayLabel(last30: List<DailySteps>): String {
        if (last30.isEmpty()) return "-"
        val max = last30.maxByOrNull { it.steps } ?: return "-"
        return dayShortName(max.date)
    }

    fun computeWeeklyTrend(
        last14: List<DailySteps>
    ): Pair<Int, String> {
        if (last14.isEmpty()) return 0 to "New"
        val last7 = last14.takeLast(7)
        val prev7 = last14.dropLast(7).takeLast(7)

        val cur = last7.sumOf { it.steps }
        val prev = prev7.sumOf { it.steps }

        if (prev <= 0 && cur > 0) return 0 to "New"
        if (prev <= 0 && cur <= 0) return 0 to "New"

        val diff = cur - prev
        val percent = ((diff.toFloat() / prev.toFloat()) * 100f).roundToInt()

        val label = when {
            percent > 3 -> "Up"
            percent < -3 -> "Down"
            else -> "Stable"
        }
        return percent to label
    }

    fun computeConsistencyScore(entries: List<DailySteps>): Int {
        if (entries.isEmpty()) return 0
        val values = entries.map { it.steps }
        val avg = values.average()
        if (avg <= 0.0) return 0

        val variance = values.map { (it - avg) * (it - avg) }.average()
        val stdDev = sqrt(variance)

        val ratio = (stdDev / avg).coerceIn(0.0, 1.5)
        return (100 - (ratio * 60)).roundToInt().coerceIn(0, 100)
    }

    fun computeActivityScore(
        consistencyScore: Int,
        goalCompletedDays: Int,
        periodDays: Int
    ): Int {
        val completionRatio = if (periodDays <= 0) 0f else (goalCompletedDays.toFloat() / periodDays.toFloat())
        val completionScore = (completionRatio * 45f).roundToInt().coerceIn(0, 45)
        val consistencyPart = (consistencyScore * 0.55f).roundToInt().coerceIn(0, 55)
        return (completionScore + consistencyPart).coerceIn(0, 100)
    }

    fun buildLast7Points(last7: List<DailySteps>, goal: Int): List<DayPoint> {
        return last7.map { d ->
            DayPoint(
                date = d.date,
                steps = d.steps,
                hitGoal = d.steps >= goal
            )
        }
    }

    fun buildHeat30(last30: List<DailySteps>, goal: Int): List<HeatCell> {
        val perfectTh = perfectThreshold(goal)
        val maxSteps = (last30.maxOfOrNull { it.steps } ?: 0).coerceAtLeast(1)

        fun level(steps: Int): Int {
            if (steps <= 0) return 0
            val r = steps.toFloat() / maxSteps.toFloat()
            return when {
                r < 0.25f -> 1
                r < 0.55f -> 2
                r < 0.80f -> 3
                else -> 4
            }
        }

        return last30.map { d ->
            HeatCell(
                date = d.date,
                level = level(d.steps),
                hitGoal = d.steps >= goal,
                isPerfect = d.steps >= perfectTh
            )
        }
    }

    fun computePerfectDays(last30: List<DailySteps>, goal: Int): Int {
        val th = perfectThreshold(goal)
        return last30.count { it.steps >= th }
    }

    fun weeklyCompletionRate(last7: List<DailySteps>, goal: Int): Int {
        if (last7.isEmpty()) return 0
        val hit = last7.count { it.steps >= goal }
        return ((hit.toFloat() / last7.size.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    }

    fun peakHourLabel(
        hourlyByDate: Map<String, List<HourlySteps>>,
        dates: List<String>
    ): String {
        val bucket = IntArray(24)

        for (d in dates) {
            val list = hourlyByDate[d].orEmpty()
            for (h in list) {
                if (h.hour in 0..23) bucket[h.hour] += h.steps
            }
        }

        val peak = bucket.indices.maxByOrNull { bucket[it] } ?: return "-"
        return "%02d:00 - %02d:00".format(peak, (peak + 1).coerceAtMost(24))
    }

    fun streakRisk(
        last7: List<DailySteps>,
        goal: Int
    ): Triple<StreakRiskLevel, RiskNoteType, Int> {
        if (last7.size < 7) {
            return Triple(StreakRiskLevel.LOW, RiskNoteType.NOT_ENOUGH_DATA, 0)
        }

        val prev4 = last7.take(4)
        val last3 = last7.takeLast(3)

        val prevAvg = prev4.map { it.steps }.average().coerceAtLeast(1.0)
        val lastAvg = last3.map { it.steps }.average()

        val dropPct = ((prevAvg - lastAvg) / prevAvg * 100.0).roundToInt()
        val today = last7.last()
        val todayHit = today.steps >= goal

        return when {
            todayHit -> Triple(StreakRiskLevel.LOW, RiskNoteType.GOAL_SAFE, 0)
            dropPct >= 35 -> Triple(StreakRiskLevel.HIGH, RiskNoteType.HIGH_DROP, dropPct)
            dropPct >= 15 -> Triple(StreakRiskLevel.MEDIUM, RiskNoteType.MEDIUM_DROP, dropPct)
            else -> Triple(StreakRiskLevel.LOW, RiskNoteType.LOW_RISK, dropPct)
        }
    }

    fun goalPrediction(
        todaySteps: Int,
        goal: Int,
        last7Avg: Int,
        nowHour: Int
    ): Pair<Int, PredictionNoteType> {
        if (goal <= 0) return 0 to PredictionNoteType.GOAL_NOT_SET
        if (todaySteps >= goal) return 100 to PredictionNoteType.GOAL_REACHED

        val startHour = 7
        val endHour = 22
        val totalWindow = (endHour - startHour).coerceAtLeast(1)
        val elapsed = (nowHour - startHour).coerceIn(0, totalWindow)

        val expectedByNow = if (elapsed <= 0) 0
        else ((goal.toFloat() * (elapsed.toFloat() / totalWindow.toFloat()))).roundToInt()

        val diff = todaySteps - expectedByNow

        val base = when {
            diff >= 800 -> 85
            diff >= 0 -> 70
            diff >= -800 -> 50
            else -> 30
        }

        val adjust = when {
            last7Avg >= goal -> 10
            last7Avg >= (goal * 0.8f) -> 0
            else -> -10
        }

        val chance = (base + adjust).coerceIn(5, 95)
        val note = when {
            chance >= 80 -> PredictionNoteType.ON_TRACK
            chance >= 60 -> PredictionNoteType.POSSIBLE
            chance >= 40 -> PredictionNoteType.BEHIND
            else -> PredictionNoteType.RISK
        }

        return chance to note
    }

    private fun dayShortName(date: String): String {
        return try {
            val parts = date.split("-")
            if (parts.size != 3) return "?"
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, parts[0].toInt())
                set(Calendar.MONTH, parts[1].toInt() - 1)
                set(Calendar.DAY_OF_MONTH, parts[2].toInt())
            }
            when (cal.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> "Mon"
                Calendar.TUESDAY -> "Tue"
                Calendar.WEDNESDAY -> "Wed"
                Calendar.THURSDAY -> "Thu"
                Calendar.FRIDAY -> "Fri"
                Calendar.SATURDAY -> "Sat"
                Calendar.SUNDAY -> "Sun"
                else -> "?"
            }
        } catch (_: Exception) {
            "?"
        }
    }


    fun computeCurrentStreakWithTodayOverride(
        dailyByDate: Map<String, Int>,
        goal: Int,
        today: String,
        todayCountsForStreak: Boolean
    ): Int {
        var streak = 0
        var cal = Calendar.getInstance().apply { time = ymd.parse(today) ?: time }
        var firstDay = true

        while (true) {
            val d = ymd.format(cal.time)

            val counts = if (firstDay) {
                todayCountsForStreak
            } else {
                val steps = dailyByDate[d] ?: 0
                steps >= goal
            }

            if (counts) {
                streak++
                cal.add(Calendar.DAY_OF_YEAR, -1)
                firstDay = false
            } else {
                break
            }
        }

        return streak
    }
}