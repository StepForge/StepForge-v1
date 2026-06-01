package com.example.stepforge.ui.components

import com.example.stepforge.ui.streak.PremiumCoachDecision
import com.example.stepforge.ui.streak.PremiumCoachMessageType
import com.example.stepforge.ui.streak.StreakBehaviorEngine
import com.example.stepforge.ui.streak.StreakBehaviorState
import kotlin.math.max

object PremiumCoachEngine {

    data class Input(
        val isPremium: Boolean,
        val aiCoachEnabled: Boolean,

        val todaySteps: Int,
        val goal: Int,

        val streakBehaviorState: StreakBehaviorState,
        val streakHealthPercent: Int,
        val premiumRescuesLeft: Int,

        val nowHour: Int,
        val last7AverageSteps: Int
    )

    fun evaluate(input: Input): PremiumCoachDecision {
        if (!input.isPremium) return PremiumCoachDecision()
        if (!input.aiCoachEnabled) return PremiumCoachDecision()

        val todaySteps = input.todaySteps.coerceAtLeast(0)
        val goal = input.goal.coerceAtLeast(1000)
        val goalRemaining = (goal - todaySteps).coerceAtLeast(0)

        val stepsToNextMilestone = nextActivityMilestoneSteps(todaySteps)?.let { milestone ->
            (milestone - todaySteps).coerceAtLeast(0)
        } ?: 0

        if (goalRemaining in 1..500) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.GOAL_ALMOST_COMPLETE,
                stepsRemainingToNextShieldHour = stepsToNextMilestone,
                currentShieldMinutesLeft = input.streakHealthPercent
            )
        }

        if (stepsToNextMilestone in 1..300) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.NEXT_SHIELD_MILESTONE,
                stepsRemainingToNextShieldHour = stepsToNextMilestone,
                currentShieldMinutesLeft = input.streakHealthPercent
            )
        }

        if (input.streakBehaviorState == StreakBehaviorState.CRITICAL && input.nowHour >= 14) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.STREAK_RISK,
                stepsRemainingToNextShieldHour = stepsToNextMilestone,
                currentShieldMinutesLeft = input.streakHealthPercent
            )
        }

        if (input.streakBehaviorState == StreakBehaviorState.CRITICAL &&
            input.premiumRescuesLeft > 0
        ) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.RESCUE_AVAILABLE,
                stepsRemainingToNextShieldHour = stepsToNextMilestone,
                currentShieldMinutesLeft = input.streakHealthPercent
            )
        }

        val expectedLow = max(1500, (input.last7AverageSteps * 0.35f).toInt())
        if (todaySteps < expectedLow && input.nowHour in 15..20) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.LOW_ACTIVITY_PATTERN,
                stepsRemainingToNextShieldHour = stepsToNextMilestone,
                currentShieldMinutesLeft = input.streakHealthPercent
            )
        }

        return PremiumCoachDecision()
    }

    private fun nextActivityMilestoneSteps(todaySteps: Int): Int? {
        val safeSteps = todaySteps.coerceAtLeast(0)
        return when {
            safeSteps < StreakBehaviorEngine.MIN_STEPS_TO_EARN_BUFFER -> StreakBehaviorEngine.MIN_STEPS_TO_EARN_BUFFER
            safeSteps < StreakBehaviorEngine.MIN_STEPS_TO_PAUSE_DECAY -> StreakBehaviorEngine.MIN_STEPS_TO_PAUSE_DECAY
            safeSteps >= 12_000 -> null
            else -> ((safeSteps / 1000) + 1) * 1000
        }
    }
}
