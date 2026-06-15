package com.example.stepforge.ui.sleep.model

import androidx.annotation.StringRes
import java.time.LocalDate
import java.time.LocalTime

// ── Enums ─────────────────────────────────────────────────────────────────────

enum class DataAvailability { FULL, LIMITED, NONE }
enum class SleepSessionType { MAIN, NAP }

enum class TrackingMode {
    SENSOR, PHONE, MANUAL
}

enum class StageType {
    DEEP, REM, LIGHT, AWAKE
}

enum class InsightSeverity { POSITIVE, WARNING, INFO }

// ── Data classes ──────────────────────────────────────────────────────────────

data class SleepStageData(
    val type: StageType,
    val durationMinutes: Int
)

data class SleepSessionInfo(
    val id: Long,
    val type: SleepSessionType,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val totalMinutes: Int,
    val qualityScore: Int,
    val source: String,
    val notes: String = "",
    val stages: List<SleepStageData>? = null
)

data class SleepInsight(
    @StringRes val titleRes: Int,
    @StringRes val bodyRes: Int,
    val bodyArgs: Array<Any> = emptyArray(),
    val severity: InsightSeverity
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SleepInsight
        if (titleRes != other.titleRes) return false
        if (bodyRes != other.bodyRes) return false
        if (!bodyArgs.contentEquals(other.bodyArgs)) return false
        if (severity != other.severity) return false
        return true
    }

    override fun hashCode(): Int {
        var result = titleRes
        result = 31 * result + bodyRes
        result = 31 * result + bodyArgs.contentHashCode()
        result = 31 * result + severity.hashCode()
        return result
    }
}

data class ManualSleepEntry(
    val bedTime: LocalTime,
    val wakeTime: LocalTime,
    val qualityRating: Int = 3,
    val notes: String = "",
    val type: SleepSessionType = SleepSessionType.MAIN,
    val date: LocalDate = LocalDate.now()
)

// ── SleepDay ──────────────────────────────────────────────────────────────────

data class SleepDay(
    val date: LocalDate,
    val availability: DataAvailability = DataAvailability.FULL,
    val mode: TrackingMode = TrackingMode.SENSOR,
    val sessions: List<SleepSessionInfo> = emptyList(),
    val heartRateAvg: Int? = null,
    val consistencyScore: Float = 0f
) {
    val mainSession: SleepSessionInfo?
        get() = sessions.filter { it.type == SleepSessionType.MAIN }.maxByOrNull { it.totalMinutes }
            ?: sessions.maxByOrNull { it.totalMinutes }

    val bedTime: LocalTime? get() = mainSession?.startTime
    val wakeTime: LocalTime? get() = mainSession?.endTime

    val totalSleepMinutes: Int get() = sessions.sumOf { it.totalMinutes }
    val hours: Int   get() = totalSleepMinutes / 60
    val minutes: Int get() = totalSleepMinutes % 60

    val bedTimeStr: String get() = bedTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "--:--"
    val wakeTimeStr: String get() = wakeTime?.let { "%02d:%02d".format(it.hour, it.minute) } ?: "--:--"

    val hasAnyData: Boolean get() = sessions.isNotEmpty()
    val hasFullData: Boolean get() = availability == DataAvailability.FULL
    val id: Long? get() = mainSession?.id

    val deepMinutes: Int? get() = mainSession?.stages?.find { it.type == StageType.DEEP }?.durationMinutes
    val remMinutes: Int? get() = mainSession?.stages?.find { it.type == StageType.REM }?.durationMinutes
    val lightMinutes: Int? get() = mainSession?.stages?.find { it.type == StageType.LIGHT }?.durationMinutes
    val awakeMinutes: Int? get() = mainSession?.stages?.find { it.type == StageType.AWAKE }?.durationMinutes

    val interruptionCount: Int get() = mainSession?.stages?.count { it.type == StageType.AWAKE } ?: 0

    val sleepScore: Int?
        get() {
            if (!hasAnyData) return null
            return mainSession?.qualityScore
        }
}
