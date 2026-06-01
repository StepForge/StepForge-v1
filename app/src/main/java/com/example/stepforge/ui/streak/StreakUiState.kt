package com.example.stepforge.ui.streak

import androidx.compose.runtime.Immutable

@Immutable
data class StreakUiState(
    val isLoading: Boolean = true,

    // Basic (free)
    val todaySteps: Int = 0,
    val goalSteps: Int = 10_000,
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val todayCompletedGoal: Boolean = false,

    // Premium analytics
    val perfectDayThresholdSteps: Int = 15_000,
    val perfectDaysLast30: Int = 0,

    val weeklyAverageSteps: Int = 0,
    val weeklyGoalCompletionRate: Int = 0,
    val weeklyTrendLabel: String = "Stable",
    val weeklyTrendPercent: Int = 0,

    val monthlyActiveDays: Int = 0,
    val monthlyGoalCompletedDays: Int = 0,
    val monthlyGoalMissedDays: Int = 0,

    val consistencyScore: Int = 0,
    val activityScore: Int = 0,

    val peakHourLabel: String = "-",
    val mostActiveDayLabel: String = "-",

    val streakRiskLevel: StreakRiskLevel = StreakRiskLevel.LOW,
    val streakRiskNoteType: RiskNoteType = RiskNoteType.NOT_ENOUGH_DATA,
    val streakRiskDropPercent: Int = 0,

    val goalPredictionChance: Int = 0,
    val goalPredictionNoteType: PredictionNoteType = PredictionNoteType.GOAL_NOT_SET,

    val last7Steps: List<DayPoint> = emptyList(),
    val last30Heat: List<HeatCell> = emptyList(),

    val isPremium: Boolean = false,

    val streakBehaviorState: StreakBehaviorState = StreakBehaviorState.ACTIVE,
    val streakStateMessage: StreakStateMessage = StreakStateMessage.SAFE,
    val streakHealthPercent: Int = 100,

    val premiumRescuesLeft: Int = 0,
    val premiumAiCoachEnabled: Boolean = false,

    val showRescueDialog: Boolean = false,
    val showLostRestoreDialog: Boolean = false,
    val lostStreakSnapshot: Int = 0,
    val recoveryWindowActive: Boolean = false,
    val recovery: StreakRecoveryState = StreakRecoveryState()
)

@Immutable
data class DayPoint(
    val date: String, // "yyyy-MM-dd"
    val steps: Int,
    val hitGoal: Boolean
)

@Immutable
data class HeatCell(
    val date: String,     // "yyyy-MM-dd"
    val level: Int,       // 0..4 (intensity)
    val hitGoal: Boolean,
    val isPerfect: Boolean
)

enum class StreakRiskLevel {
    LOW, MEDIUM, HIGH
}

enum class RiskNoteType {
    NOT_ENOUGH_DATA,
    GOAL_SAFE,
    HIGH_DROP,
    MEDIUM_DROP,
    LOW_RISK
}

enum class PredictionNoteType {
    GOAL_NOT_SET,
    GOAL_REACHED,
    ON_TRACK,
    POSSIBLE,
    BEHIND,
    RISK
}
