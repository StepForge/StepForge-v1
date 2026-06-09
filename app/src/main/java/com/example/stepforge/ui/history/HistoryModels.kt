package com.example.stepforge.ui.history

import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.Locale
import kotlin.math.roundToInt

internal const val HISTORY_TEST_PREFIX = "TEST-"

internal enum class HistoryMetric {
    STEPS,
    DISTANCE,
    CALORIES,
    ACTIVE_TIME
}

internal enum class HistoryRange(val days: Int) {
    WEEK(7),
    MONTH(30),
    THREE_MONTHS(90),
    YEAR(365),
    ALL(Int.MAX_VALUE)
}

internal enum class HistoryWeatherMood {
    CLEAR,
    CLOUDY,
    RAIN,
    SNOW,
    FOG,
    NIGHT,
    RAIN_NIGHT,
    SNOW_NIGHT,
    FOG_NIGHT,
    STORM,
    THUNDERSTORM,
    THUNDERSTORM_NIGHT,
    SUNRISE,
    SUNSET,
    WINDY_SUNSET,
    BLIZZARD
}

internal data class HistoryDayUi(
    val date: String,
    val localDate: LocalDate,
    val steps: Int,
    val distanceKm: Float,
    val calories: Int,
    val activeMinutes: Int,
    val waterMl: Int,
    val sleepMinutes: Int,
    val workoutSessions: Int,
    val stepGoal: Int,
    val waterGoal: Int
) {
    fun metricValue(metric: HistoryMetric): Float = when (metric) {
        HistoryMetric.STEPS -> steps.toFloat()
        HistoryMetric.DISTANCE -> distanceKm
        HistoryMetric.CALORIES -> calories.toFloat()
        HistoryMetric.ACTIVE_TIME -> activeMinutes.toFloat()
    }

    fun goalProgress(metric: HistoryMetric): Float = when (metric) {
        HistoryMetric.STEPS -> steps / stepGoal.toFloat()
        HistoryMetric.DISTANCE -> distanceKm / 8f
        HistoryMetric.CALORIES -> calories / 550f
        HistoryMetric.ACTIVE_TIME -> activeMinutes / 60f
    }.coerceIn(0f, 1.35f)
}

internal data class HistoryCalendarDayUi(
    val date: LocalDate,
    val inMonth: Boolean,
    val day: HistoryDayUi?
)

internal data class HistoryAchievementUi(
    val title: String,
    val subtitle: String,
    val progress: Float,
    val level: Int
)

internal data class HistoryUiState(
    val allDays: List<HistoryDayUi>,
    val visibleDays: List<HistoryDayUi>,
    val selectedDay: HistoryDayUi,
    val selectedMetric: HistoryMetric,
    val selectedRange: HistoryRange,
    val visibleMonth: YearMonth,
    val calendarDays: List<HistoryCalendarDayUi>,
    val totalSteps: Int,
    val averageSteps: Int,
    val totalDistanceKm: Float,
    val totalCalories: Int,
    val totalActiveMinutes: Int,
    val topDays: List<HistoryDayUi>,
    val achievements: List<HistoryAchievementUi>,
    val weatherMood: HistoryWeatherMood
)

internal fun String.safeHistoryDate(): LocalDate? {
    if (startsWith(HISTORY_TEST_PREFIX)) return null
    return try {
        LocalDate.parse(this)
    } catch (_: DateTimeParseException) {
        null
    }
}

internal fun LocalDate.shortDayName(): String =
    format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault())).uppercase(Locale.getDefault())

internal fun LocalDate.shortMonthDay(): String =
    format(DateTimeFormatter.ofPattern("MMM d", Locale.getDefault()))

internal fun LocalDate.monthTitle(): String =
    format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()))

internal fun Int.formatHistoryNumber(): String = NumberFormat.getIntegerInstance().format(this)

internal fun Float.formatHistoryKm(): String = when {
    this >= 100f -> String.format(Locale.getDefault(), "%.0f", this)
    this >= 10f -> String.format(Locale.getDefault(), "%.1f", this)
    else -> String.format(Locale.getDefault(), "%.2f", this)
}

internal fun Int.formatHistoryDurationShort(): String {
    val safe = coerceAtLeast(0)
    val h = safe / 60
    val m = safe % 60
    return when {
        h > 0 && m > 0 -> "${h}h ${m}m"
        h > 0 -> "${h}h"
        else -> "${m}m"
    }
}

internal fun stepsToDistanceKm(steps: Int): Float = steps.coerceAtLeast(0) * 0.00075f

internal fun stepsToCalories(steps: Int): Int = (steps.coerceAtLeast(0) * 0.04f).roundToInt()

internal fun buildCalendarDays(month: YearMonth, dayMap: Map<LocalDate, HistoryDayUi>): List<HistoryCalendarDayUi> {
    val first = month.atDay(1).with(java.time.temporal.TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..41).map { offset ->
        val date = first.plusDays(offset.toLong())
        HistoryCalendarDayUi(
            date = date,
            inMonth = YearMonth.from(date) == month,
            day = dayMap[date]
        )
    }
}
