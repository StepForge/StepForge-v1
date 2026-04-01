package com.example.stepforge.ui.streak

import androidx.compose.runtime.Immutable

@Immutable
data class StreakShieldState(
    val todayShieldMinutesLeft: Int = 0,
    val todayShieldMaxMinutes: Int = 0,

    val tomorrowBaseShieldHours: Int = 0,
    val tomorrowGoalBonusHours: Int = 0,
    val tomorrowFinalShieldHours: Int = 0,
    val tomorrowShieldMaxHours: Int = 0,

    val isShieldDrainingNow: Boolean = false,
    val todayStepsCountForDrainCheck: Int = 0,

    val isPremium: Boolean = false,
    val premiumMonthlyRescuesLeft: Int = 0,
    val premiumAutoRescueEnabled: Boolean = false
) {
    val todayShieldProgress: Float
        get() = if (todayShieldMaxMinutes <= 0) 0f
        else (todayShieldMinutesLeft.toFloat() / todayShieldMaxMinutes.toFloat()).coerceIn(0f, 1f)
}

@Immutable
data class PremiumCoachDecision(
    val shouldNotify: Boolean = false,
    val type: PremiumCoachMessageType? = null,
    val stepsRemainingToNextShieldHour: Int = 0,
    val currentShieldMinutesLeft: Int = 0
)

enum class PremiumCoachMessageType {
    STREAK_RISK,
    NEXT_SHIELD_MILESTONE,
    GOAL_ALMOST_COMPLETE,
    RESCUE_AVAILABLE,
    LOW_ACTIVITY_PATTERN
}