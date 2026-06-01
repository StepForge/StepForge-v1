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
        behaviorBufferMinutes: Int,
        rescueUsedForDay: Boolean,
        rescuedActive: Boolean = false
    ): DayQualificationResult {
        return StreakBehaviorEngine.qualifyDay(
            steps = steps,
            goal = goal,
            internalBufferMinutes = behaviorBufferMinutes,
            rescuedActive = rescuedActive,
            rescueUsedForDay = rescueUsedForDay
        )
    }
}