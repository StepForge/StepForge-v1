package com.example.stepforge.ui.workouts

import com.example.stepforge.data.WorkoutSession
import java.text.DecimalFormat
import java.time.Instant
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

internal enum class WorkoutChartMetric {
    DURATION,
    CALORIES,
    INTENSITY
}

internal enum class WorkoutSheetType {
    TODAY,
    WEEK,
    ACTIVE_TIME,
    TRAINING_LOAD,
    STREAK,
    CALORIES,
    DISTANCE,
    INTENSITY,
    ZONES,
    FOCUS,
    JOURNEY,
    DEEPER
}

internal enum class WorkoutKind {
    WALK,
    RUN,
    STRENGTH,
    CYCLING,
    GENERIC
}

internal data class WorkoutDaySummary(
    val date: LocalDate,
    val sessions: List<WorkoutSession>,
    val durationMinutes: Int,
    val steps: Int,
    val calories: Int,
    val distanceMeters: Int,
    val intensityScore: Int,
    val active: Boolean,
    val future: Boolean
) {
    val distanceKm: Float = distanceMeters / 1000f
}

internal data class WorkoutZoneSlice(
    val labelType: ZoneLabelType,
    val percent: Int
)

internal enum class ZoneLabelType {
    PEAK,
    CARDIO,
    FAT_BURN,
    WARM_UP
}

internal data class WorkoutJourneyItem(
    val session: WorkoutSession,
    val date: LocalDate,
    val kind: WorkoutKind,
    val titleType: WorkoutTitleType,
    val primaryMetric: String,
    val secondaryText: String,
    val chartValues: List<Float>
)

internal enum class WorkoutTitleType {
    OUTDOOR_RUN,
    MORNING_WALK,
    FULL_BODY_STRENGTH,
    CYCLING,
    WORKOUT
}

internal data class WorkoutsDashboardState(
    val today: WorkoutDaySummary,
    val yesterday: WorkoutDaySummary?,
    val week: List<WorkoutDaySummary>,
    val previousWeek: List<WorkoutDaySummary>,
    val displaySessions: List<WorkoutSession>,
    val todaySessions: List<WorkoutSession>,
    val journeyItems: List<WorkoutJourneyItem>,
    val zoneSlices: List<WorkoutZoneSlice>,
    val weekDurationMinutes: Int,
    val previousWeekDurationMinutes: Int,
    val weekCalories: Int,
    val previousWeekCalories: Int,
    val weekDistanceMeters: Int,
    val previousWeekDistanceMeters: Int,
    val activeDays: Int,
    val workoutScore: Int,
    val intensityScore: Int,
    val consistencyScore: Int
)

internal fun buildWorkoutsDashboardState(
    rawSessions: List<WorkoutSession>,
    today: LocalDate = LocalDate.now()
): WorkoutsDashboardState {
    val sessions = rawSessions
        .filter { it.isDisplayableWorkout() }
        .sortedByDescending { it.startTime }

    val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val previousWeekStart = startOfWeek.minusDays(7)

    val weekDays = (0..6).map { startOfWeek.plusDays(it.toLong()) }
    val previousDays = (0..6).map { previousWeekStart.plusDays(it.toLong()) }

    val sessionsByDate = sessions.groupBy { it.safeLocalDate() }
    val week = weekDays.map { date ->
        summarizeDay(date, sessionsByDate[date].orEmpty(), today)
    }
    val previousWeek = previousDays.map { date ->
        summarizeDay(date, sessionsByDate[date].orEmpty(), today)
    }

    val todaySummary = week.firstOrNull { it.date == today }
        ?: summarizeDay(today, sessionsByDate[today].orEmpty(), today)

    val yesterday = week.firstOrNull { it.date == today.minusDays(1) }
    val todaySessions = sessionsByDate[today].orEmpty().sortedByDescending { it.startTime }

    val weekDuration = week.sumOf { it.durationMinutes }
    val previousWeekDuration = previousWeek.sumOf { it.durationMinutes }
    val weekCalories = week.sumOf { it.calories }
    val previousWeekCalories = previousWeek.sumOf { it.calories }
    val weekDistance = week.sumOf { it.distanceMeters }
    val previousWeekDistance = previousWeek.sumOf { it.distanceMeters }
    val activeDays = week.count { it.active }
    val intensity = week.map { it.intensityScore }.filter { it > 0 }.averageOrZero().roundToInt().coerceIn(0, 100)
    val consistency = ((activeDays / 7f) * 100f).roundToInt().coerceIn(0, 100)
    val durationScore = ((weekDuration / 240f) * 100f).roundToInt().coerceIn(0, 100)
    val workoutScore = ((durationScore * 0.45f) + (consistency * 0.35f) + (intensity * 0.20f))
        .roundToInt()
        .coerceIn(0, 100)

    val journey = sessions
        .take(8)
        .map { session ->
            val kind = session.inferWorkoutKind()
            WorkoutJourneyItem(
                session = session,
                date = session.safeLocalDate(),
                kind = kind,
                titleType = session.titleType(kind),
                primaryMetric = session.primaryMetricForJourney(kind),
                secondaryText = session.secondaryJourneyText(),
                chartValues = session.estimatedChartValues(kind)
            )
        }

    return WorkoutsDashboardState(
        today = todaySummary,
        yesterday = yesterday,
        week = week,
        previousWeek = previousWeek,
        displaySessions = sessions,
        todaySessions = todaySessions,
        journeyItems = journey,
        zoneSlices = estimateZones(sessions.take(20)),
        weekDurationMinutes = weekDuration,
        previousWeekDurationMinutes = previousWeekDuration,
        weekCalories = weekCalories,
        previousWeekCalories = previousWeekCalories,
        weekDistanceMeters = weekDistance,
        previousWeekDistanceMeters = previousWeekDistance,
        activeDays = activeDays,
        workoutScore = workoutScore,
        intensityScore = intensity,
        consistencyScore = consistency
    )
}

private fun summarizeDay(
    date: LocalDate,
    sessions: List<WorkoutSession>,
    today: LocalDate
): WorkoutDaySummary {
    val duration = sessions.sumOf { it.durationMinutes }
    val steps = sessions.sumOf { it.steps }
    val calories = sessions.sumOf { it.caloriesKcal }
    val distance = sessions.sumOf { it.distanceMeters }
    val intensity = sessions.map { it.intensityScore() }.filter { it > 0 }.averageOrZero().roundToInt().coerceIn(0, 100)
    return WorkoutDaySummary(
        date = date,
        sessions = sessions.sortedByDescending { it.startTime },
        durationMinutes = duration,
        steps = steps,
        calories = calories,
        distanceMeters = distance,
        intensityScore = intensity,
        active = sessions.isNotEmpty() && duration >= 5,
        future = date.isAfter(today)
    )
}

internal fun WorkoutSession.isDisplayableWorkout(): Boolean {
    return durationMinutes >= 5 && (steps >= 250 || distanceMeters >= 120 || caloriesKcal >= 25)
}

internal fun WorkoutSession.safeLocalDate(): LocalDate {
    return runCatching { LocalDate.parse(date, DateTimeFormatter.ISO_LOCAL_DATE) }
        .getOrElse {
            Instant.ofEpochMilli(startTime)
                .atZone(ZoneId.systemDefault())
                .toLocalDate()
        }
}

internal fun WorkoutSession.inferWorkoutKind(): WorkoutKind {
    val pace = avgStepsPerMinute
    val distanceKm = distanceMeters / 1000f
    return when {
        pace >= 120 || (distanceKm >= 1.8f && durationMinutes <= 22) -> WorkoutKind.RUN
        distanceKm >= 8f && steps < 1_500 -> WorkoutKind.CYCLING
        steps >= 250 -> WorkoutKind.WALK
        else -> WorkoutKind.GENERIC
    }
}

internal fun WorkoutSession.intensityScore(): Int {
    val paceScore = (avgStepsPerMinute / 1.6f).roundToInt()
    val calorieScore = if (durationMinutes > 0) ((caloriesKcal / durationMinutes.toFloat()) * 12f).roundToInt() else 0
    val durationScore = (durationMinutes * 1.35f).roundToInt()
    return ((paceScore * 0.45f) + (calorieScore * 0.30f) + (durationScore * 0.25f))
        .roundToInt()
        .coerceIn(0, 100)
}

internal fun WorkoutSession.titleType(kind: WorkoutKind): WorkoutTitleType {
    return when (kind) {
        WorkoutKind.RUN -> WorkoutTitleType.OUTDOOR_RUN
        WorkoutKind.WALK -> WorkoutTitleType.MORNING_WALK
        WorkoutKind.CYCLING -> WorkoutTitleType.CYCLING
        WorkoutKind.STRENGTH,
        WorkoutKind.GENERIC -> WorkoutTitleType.WORKOUT
    }
}

private fun WorkoutSession.primaryMetricForJourney(kind: WorkoutKind): String {
    val df = DecimalFormat("#.#")
    return when (kind) {
        WorkoutKind.CYCLING,
        WorkoutKind.RUN,
        WorkoutKind.WALK -> "${df.format(distanceMeters / 1000f)} km"
        WorkoutKind.STRENGTH,
        WorkoutKind.GENERIC -> "$durationMinutes min"
    }
}

private fun WorkoutSession.secondaryJourneyText(): String {
    return "$durationMinutes min / ${endTimeLabel()}"
}

private fun WorkoutSession.endTimeLabel(): String {
    val endMillis = startTime + durationMinutes.coerceAtLeast(0) * 60_000L
    val formatter = DateTimeFormatter.ofPattern("HH:mm", Locale.getDefault())
    return runCatching {
        Instant.ofEpochMilli(endMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalTime()
            .format(formatter)
    }.getOrDefault("--:--")
}

private fun WorkoutSession.estimatedChartValues(kind: WorkoutKind): List<Float> {
    val segments = 9
    val intensity = intensityScore().coerceIn(0, 100) / 100f
    val pace = (avgStepsPerMinute / 160f).coerceIn(0f, 1f)
    val distance = (distanceMeters / 7000f).coerceIn(0f, 1f)
    val duration = (durationMinutes / 90f).coerceIn(0.15f, 1f)
    val seed = ((startTime / 60000L).toInt() xor steps xor caloriesKcal).absoluteValue

    return (0 until segments).map { index ->
        val progress = if (segments <= 1) 0f else index / (segments - 1).toFloat()
        val envelope = when (kind) {
            WorkoutKind.RUN -> listOf(0.22f, 0.42f, 0.68f, 0.44f, 0.77f, 0.58f, 0.81f, 0.63f, 0.70f)[index]
            WorkoutKind.WALK -> listOf(0.20f, 0.36f, 0.32f, 0.40f, 0.38f, 0.47f, 0.44f, 0.49f, 0.46f)[index]
            WorkoutKind.STRENGTH -> listOf(0.15f, 0.28f, 0.46f, 0.62f, 0.34f, 0.55f, 0.42f, 0.31f, 0.22f)[index]
            WorkoutKind.CYCLING -> listOf(0.24f, 0.39f, 0.36f, 0.48f, 0.45f, 0.56f, 0.54f, 0.62f, 0.59f)[index]
            WorkoutKind.GENERIC -> listOf(0.18f, 0.32f, 0.36f, 0.30f, 0.42f, 0.38f, 0.46f, 0.41f, 0.44f)[index]
        }
        val pulse = (((seed + index * 17) % 9) / 8f - 0.5f) * 0.16f
        val trend = ((progress - 0.5f) * 0.06f)
        (0.10f + envelope + intensity * 0.12f + pace * 0.08f + distance * 0.08f + duration * 0.06f + pulse + trend)
            .coerceIn(0.08f, 0.96f)
    }
}

private fun estimateZones(sessions: List<WorkoutSession>): List<WorkoutZoneSlice> {
    if (sessions.isEmpty()) {
        return listOf(
            WorkoutZoneSlice(ZoneLabelType.PEAK, 0),
            WorkoutZoneSlice(ZoneLabelType.CARDIO, 0),
            WorkoutZoneSlice(ZoneLabelType.FAT_BURN, 0),
            WorkoutZoneSlice(ZoneLabelType.WARM_UP, 0)
        )
    }

    var peak = 0
    var cardio = 0
    var fatBurn = 0
    var warm = 0

    sessions.forEach { session ->
        val minutes = session.durationMinutes.coerceAtLeast(1)
        when {
            session.intensityScore() >= 78 -> peak += minutes
            session.intensityScore() >= 58 -> cardio += minutes
            session.intensityScore() >= 35 -> fatBurn += minutes
            else -> warm += minutes
        }
    }

    val total = (peak + cardio + fatBurn + warm).coerceAtLeast(1)
    fun pct(value: Int) = ((value / total.toFloat()) * 100f).roundToInt().coerceIn(0, 100)

    val raw = listOf(
        WorkoutZoneSlice(ZoneLabelType.PEAK, pct(peak)),
        WorkoutZoneSlice(ZoneLabelType.CARDIO, pct(cardio)),
        WorkoutZoneSlice(ZoneLabelType.FAT_BURN, pct(fatBurn)),
        WorkoutZoneSlice(ZoneLabelType.WARM_UP, pct(warm))
    )

    val diff = 100 - raw.sumOf { it.percent }
    return if (diff == 0 || raw.all { it.percent == 0 }) raw else raw.mapIndexed { index, slice ->
        if (index == raw.indexOf(raw.maxBy { it.percent })) slice.copy(percent = (slice.percent + diff).coerceIn(0, 100)) else slice
    }
}

internal fun List<Int>.comparePercent(previous: Int): Int {
    val current = sum()
    if (previous <= 0) return if (current > 0) 100 else 0
    return (((current - previous) / previous.toFloat()) * 100f).roundToInt()
}

internal fun Int.comparePercent(previous: Int): Int {
    if (previous <= 0) return if (this > 0) 100 else 0
    return (((this - previous) / previous.toFloat()) * 100f).roundToInt()
}

internal fun Float.formatKm(): String = DecimalFormat("#.#").format(this)

internal fun Int.formatDurationCompact(): String {
    val hours = this / 60
    val minutes = this % 60
    return when {
        hours > 0 && minutes > 0 -> "${hours}h ${minutes}m"
        hours > 0 -> "${hours}h"
        else -> "${minutes}m"
    }
}

internal fun LocalDate.dayShort(): String {
    return dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()).take(1).uppercase(Locale.getDefault())
}

internal fun LocalDate.chartDayShort(): String {
    return dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
        .take(3)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ENGLISH) else it.toString() }
}

internal fun LocalDate.dayGroupLabel(today: LocalDate): DayGroupLabel {
    return when (this) {
        today -> DayGroupLabel.TODAY
        today.minusDays(1) -> DayGroupLabel.YESTERDAY
        else -> DayGroupLabel.OLDER
    }
}

internal enum class DayGroupLabel {
    TODAY,
    YESTERDAY,
    OLDER
}

private fun List<Int>.averageOrZero(): Double = if (isEmpty()) 0.0 else average()
private fun List<Int>.sum(): Int = fold(0) { acc, v -> acc + v }

internal fun signedPercentText(percent: Int): String {
    return if (percent >= 0) "+$percent%" else "-${abs(percent)}%"
}
