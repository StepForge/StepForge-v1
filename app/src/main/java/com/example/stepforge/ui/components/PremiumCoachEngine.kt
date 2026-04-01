package com.example.stepforge.ui.components

import com.example.stepforge.ui.streak.PremiumCoachDecision
import com.example.stepforge.ui.streak.PremiumCoachMessageType
import com.example.stepforge.ui.streak.StreakShieldEngine
import kotlin.math.max

object PremiumCoachEngine {

    data class Input(
        val isPremium: Boolean,
        val aiCoachEnabled: Boolean,

        val todaySteps: Int,
        val goal: Int,

        val shieldMinutesLeft: Int,
        val premiumRescuesLeft: Int,

        val nowHour: Int,
        val last7AverageSteps: Int
    )

    fun evaluate(input: Input): PremiumCoachDecision {
        if (!input.isPremium) return PremiumCoachDecision()
        if (!input.aiCoachEnabled) return PremiumCoachDecision()

        val todaySteps = input.todaySteps.coerceAtLeast(0)
        val goal = input.goal.coerceAtLeast(1000)

        val nextShieldMilestone = nextShieldMilestoneSteps(todaySteps)
        val stepsToNextShieldHour = if (nextShieldMilestone == null) {
            0
        } else {
            (nextShieldMilestone - todaySteps).coerceAtLeast(0)
        }

        val goalRemaining = (goal - todaySteps).coerceAtLeast(0)

        // 1) Rescue warning
        if (input.shieldMinutesLeft in 1..120 && input.premiumRescuesLeft > 0) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.RESCUE_AVAILABLE,
                stepsRemainingToNextShieldHour = stepsToNextShieldHour,
                currentShieldMinutesLeft = input.shieldMinutesLeft
            )
        }

        // 2) Goal almost complete
        if (goalRemaining in 1..500) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.GOAL_ALMOST_COMPLETE,
                stepsRemainingToNextShieldHour = stepsToNextShieldHour,
                currentShieldMinutesLeft = input.shieldMinutesLeft
            )
        }

        // 3) Next shield milestone close
        if (stepsToNextShieldHour in 1..300) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.NEXT_SHIELD_MILESTONE,
                stepsRemainingToNextShieldHour = stepsToNextShieldHour,
                currentShieldMinutesLeft = input.shieldMinutesLeft
            )
        }

        // 4) Streak risk
        val shouldDrain = StreakShieldEngine.shouldShieldDrain(todaySteps)
        if (shouldDrain && input.shieldMinutesLeft in 1..240 && input.nowHour >= 14) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.STREAK_RISK,
                stepsRemainingToNextShieldHour = stepsToNextShieldHour,
                currentShieldMinutesLeft = input.shieldMinutesLeft
            )
        }

        // 5) Below usual pace
        val expectedLow = max(1500, (input.last7AverageSteps * 0.35f).toInt())
        if (todaySteps < expectedLow && input.nowHour in 15..20) {
            return PremiumCoachDecision(
                shouldNotify = true,
                type = PremiumCoachMessageType.LOW_ACTIVITY_PATTERN,
                stepsRemainingToNextShieldHour = stepsToNextShieldHour,
                currentShieldMinutesLeft = input.shieldMinutesLeft
            )
        }

        return PremiumCoachDecision()
    }

    private fun nextShieldMilestoneSteps(todaySteps: Int): Int? {
        val safeSteps = todaySteps.coerceAtLeast(0)

        return when {
            safeSteps < 2000 -> 2000
            safeSteps >= 12_000 -> null
            else -> ((safeSteps / 1000) + 1) * 1000
        }
    }
}