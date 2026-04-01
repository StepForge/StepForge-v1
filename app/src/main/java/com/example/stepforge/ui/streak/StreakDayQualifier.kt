package com.example.stepforge.ui.streak

object StreakDayQualifier {

    data class DayQualificationResult(
        val countsForStreak: Boolean,
        val reachedGoal: Boolean,
        val protectedByShield: Boolean,
        val protectedByPremiumRescue: Boolean
    )

    fun qualifyDay(
        steps: Int,
        goal: Int,
        shieldMinutesLeft: Int,
        rescueUsedForDay: Boolean
    ): DayQualificationResult {
        val safeSteps = steps.coerceAtLeast(0)
        val safeGoal = goal.coerceAtLeast(1000)

        val reachedGoal = safeSteps >= safeGoal
        val activeEnoughToStopDrain = safeSteps >= StreakShieldEngine.MIN_STEPS_TO_STOP_DRAIN
        val protectedByShield = !reachedGoal && activeEnoughToStopDrain && shieldMinutesLeft > 0
        val protectedByPremiumRescue = !reachedGoal && activeEnoughToStopDrain && rescueUsedForDay

        val countsForStreak = reachedGoal || protectedByShield || protectedByPremiumRescue

        return DayQualificationResult(
            countsForStreak = countsForStreak,
            reachedGoal = reachedGoal,
            protectedByShield = protectedByShield,
            protectedByPremiumRescue = protectedByPremiumRescue
        )
    }
}