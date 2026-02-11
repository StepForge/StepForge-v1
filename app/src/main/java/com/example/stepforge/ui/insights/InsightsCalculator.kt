package com.example.stepforge.ui.insights

import android.content.Context
import com.example.stepforge.data.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

object InsightsCalculator {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    private fun getDayLabel(dateStr: String): String {
        return try {
            val d = dateFormat.parse(dateStr) ?: return "?"
            val cal = Calendar.getInstance().apply { time = d }
            val dow = cal.get(Calendar.DAY_OF_WEEK)

            when (dow) {
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

    private fun formatPeakHour(hour: Int): String {
        val start = hour.coerceIn(0, 23)
        val end = (start + 1).coerceAtMost(24)
        return "%02d:00 - %02d:00".format(start, end)
    }

    private fun safeAverage(total: Int, days: Int): Int {
        if (days <= 0) return 0
        return (total.toFloat() / days.toFloat()).roundToInt()
    }

    private fun computeConsistency(entries: List<DayStepEntry>): Int {
        if (entries.isEmpty()) return 0
        val values = entries.map { it.steps }
        val avg = values.average()

        if (avg <= 0.0) return 0

        val variance = values.map { (it - avg) * (it - avg) }.average()
        val stdDev = kotlin.math.sqrt(variance)

        // stdDev yüksekse consistency düşer
        val ratio = (stdDev / avg).coerceIn(0.0, 1.5)

        // 0 ratio -> 100 score, 1 ratio -> 40 score, 1.5 ratio -> 10 score
        val score = (100 - (ratio * 60)).roundToInt().coerceIn(0, 100)

        return score
    }

    private fun computeTrendPercent(currentTotal: Int, previousTotal: Int): Pair<Int, String> {
        if (previousTotal <= 0 && currentTotal > 0) return Pair(0, "New")
        if (previousTotal <= 0 && currentTotal <= 0) return Pair(0, "New")

        val diff = currentTotal - previousTotal
        val percent = ((diff.toFloat() / previousTotal.toFloat()) * 100f).roundToInt()

        val label = when {
            percent > 3 -> "Up"
            percent < -3 -> "Down"
            else -> "Stable"
        }

        return Pair(percent, label)
    }

    private fun computeSummaryLines(
        mode: InsightsMode,
        totalSteps: Int,
        avg: Int,
        bestSteps: Int,
        worstSteps: Int,
        activeDays: Int,
        periodDays: Int,
        consistency: Int,
        trendPercent: Int,
        trendLabel: String
    ): List<String> {

        val lines = mutableListOf<String>()

        val modeLabel = if (mode == InsightsMode.WEEKLY) "week" else "month"

        lines.add("You recorded ${formatNumber(totalSteps)} steps this $modeLabel.")

        if (trendLabel == "New") {
            lines.add("This is your first tracked $modeLabel. Keep going.")
        } else {
            val trendText = when {
                trendPercent > 0 -> "+$trendPercent% vs previous"
                trendPercent < 0 -> "$trendPercent% vs previous"
                else -> "No change vs previous"
            }
            lines.add("Trend: $trendText.")
        }

        if (avg > 0) {
            lines.add("Daily average: ${formatNumber(avg)} steps.")
        }

        if (bestSteps > 0) {
            lines.add("Your strongest day hit ${formatNumber(bestSteps)} steps.")
        }

        if (worstSteps >= 0) {
            lines.add("Your weakest day dropped to ${formatNumber(worstSteps)} steps.")
        }

        if (periodDays > 0) {
            val percentActive = ((activeDays.toFloat() / periodDays.toFloat()) * 100f).roundToInt()
            lines.add("Active days: $activeDays/$periodDays ($percentActive%).")
        }

        if (consistency > 0) {
            val label = when {
                consistency >= 80 -> "Very consistent"
                consistency >= 60 -> "Consistent"
                consistency >= 40 -> "Unstable"
                else -> "Very unstable"
            }
            lines.add("Consistency: $consistency/100 ($label).")
        }

        return lines
    }

    private fun formatNumber(value: Int): String {
        return try {
            java.text.NumberFormat.getIntegerInstance().format(value)
        } catch (_: Exception) {
            value.toString()
        }
    }

    private fun getPeriodDates(mode: InsightsMode): Pair<String, String> {
        val cal = Calendar.getInstance()

        val end = dateFormat.format(cal.time)

        if (mode == InsightsMode.WEEKLY) {
            cal.add(Calendar.DAY_OF_YEAR, -6)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -29)
        }

        val start = dateFormat.format(cal.time)

        return Pair(start, end)
    }

    private fun getPreviousPeriodDates(mode: InsightsMode): Pair<String, String> {
        val cal = Calendar.getInstance()

        if (mode == InsightsMode.WEEKLY) {
            cal.add(Calendar.DAY_OF_YEAR, -7)
            val end = dateFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -6)
            val start = dateFormat.format(cal.time)
            return Pair(start, end)
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -30)
            val end = dateFormat.format(cal.time)
            cal.add(Calendar.DAY_OF_YEAR, -29)
            val start = dateFormat.format(cal.time)
            return Pair(start, end)
        }
    }

    private fun isDateInRange(date: String, start: String, end: String): Boolean {
        return date >= start && date <= end
    }

    suspend fun calculateInsights(
        context: Context,
        mode: InsightsMode,
        goal: Int = 10000,
        activeThreshold: Int = 3000
    ): PeriodInsights = withContext(Dispatchers.IO) {

        val db = AppDatabase.getDatabase(context)
        val dailyDao = db.dailyStepsDao()
        val hourlyDao = db.hourlyStepsDao()

        val (startDate, endDate) = getPeriodDates(mode)
        val (prevStart, prevEnd) = getPreviousPeriodDates(mode)

        val allDays = dailyDao.getAllSteps()

        val currentEntries = allDays
            .filter { isDateInRange(it.date, startDate, endDate) }
            .sortedBy { it.date }
            .map { DayStepEntry(date = it.date, steps = it.steps) }

        val prevEntries = allDays
            .filter { isDateInRange(it.date, prevStart, prevEnd) }
            .sortedBy { it.date }
            .map { DayStepEntry(date = it.date, steps = it.steps) }

        val totalSteps = currentEntries.sumOf { it.steps }
        val previousTotalSteps = prevEntries.sumOf { it.steps }

        val periodDays = if (mode == InsightsMode.WEEKLY) 7 else 30
        val dailyAverage = safeAverage(totalSteps, currentEntries.size.coerceAtLeast(1))

        val bestDay = currentEntries.maxByOrNull { it.steps }
        val worstDay = currentEntries.minByOrNull { it.steps }

        val bestDayLabel = bestDay?.let { getDayLabel(it.date) } ?: "-"
        val bestDaySteps = bestDay?.steps ?: 0

        val worstDayLabel = worstDay?.let { getDayLabel(it.date) } ?: "-"
        val worstDaySteps = worstDay?.steps ?: 0

        val activeDays = currentEntries.count { it.steps >= activeThreshold }
        val goalSuccess = currentEntries.count { it.steps >= goal }

        val consistencyScore = computeConsistency(currentEntries)

        // Peak Hour
        val hourCounter = mutableMapOf<Int, Int>()

        try {
            for (entry in currentEntries) {
                val hours = hourlyDao.getForDate(entry.date)
                for (h in hours) {
                    val existing = hourCounter[h.hour] ?: 0
                    hourCounter[h.hour] = existing + h.steps
                }
            }
        } catch (_: Exception) {
            // Eğer hourly yoksa sorun değil
        }

        val peakHour = hourCounter.entries.maxByOrNull { it.value }?.key ?: 0
        val peakHourLabel = formatPeakHour(peakHour)

        val (trendPercent, trendLabel) = computeTrendPercent(totalSteps, previousTotalSteps)

        val summaryLines = computeSummaryLines(
            mode = mode,
            totalSteps = totalSteps,
            avg = dailyAverage,
            bestSteps = bestDaySteps,
            worstSteps = worstDaySteps,
            activeDays = activeDays,
            periodDays = periodDays,
            consistency = consistencyScore,
            trendPercent = trendPercent,
            trendLabel = trendLabel
        )

        return@withContext PeriodInsights(
            startDate = startDate,
            endDate = endDate,
            totalSteps = totalSteps,
            dailyAverage = dailyAverage,
            bestDayLabel = bestDayLabel,
            bestDaySteps = bestDaySteps,
            worstDayLabel = worstDayLabel,
            worstDaySteps = worstDaySteps,
            activeDays = activeDays,
            peakHourLabel = peakHourLabel,
            consistencyScore = consistencyScore,
            goalSuccess = goalSuccess,
            trendPercent = trendPercent,
            trendLabel = trendLabel,
            summaryLines = summaryLines,
            chartData = currentEntries
        )
    }
}
