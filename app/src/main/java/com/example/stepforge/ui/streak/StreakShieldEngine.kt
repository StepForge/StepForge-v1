package com.example.stepforge.ui.streak

object StreakShieldEngine {

    const val MIN_STEPS_FOR_SHIELD = 2000
    const val MIN_STEPS_TO_STOP_DRAIN = 1000

    const val FREE_MAX_SHIELD_HOURS = 12
    const val PREMIUM_MAX_SHIELD_HOURS = 16

    const val PREMIUM_MONTHLY_RESCUES = 5
    const val PREMIUM_RESCUE_HOURS = 24

    data class ShieldResult(
        val baseHours: Int,
        val goalBonusHours: Int,
        val finalHours: Int,
        val maxHours: Int
    )

    fun calculateDailyEarnedShieldHours(
        steps: Int,
        goal: Int,
        isPremium: Boolean
    ): ShieldResult {
        val safeSteps = steps.coerceAtLeast(0)
        val safeGoal = goal.coerceAtLeast(1000)

        val baseHours = calculateBaseShieldHours(safeSteps)
        val goalBonusHours = calculateGoalBonusHours(
            steps = safeSteps,
            goal = safeGoal
        )

        val maxHours = if (isPremium) {
            PREMIUM_MAX_SHIELD_HOURS
        } else {
            FREE_MAX_SHIELD_HOURS
        }

        val finalHours = (baseHours + goalBonusHours).coerceAtMost(maxHours)

        return ShieldResult(
            baseHours = baseHours,
            goalBonusHours = goalBonusHours,
            finalHours = finalHours,
            maxHours = maxHours
        )
    }

    fun calculateBaseShieldHours(steps: Int): Int {
        val safeSteps = steps.coerceAtLeast(0)

        if (safeSteps < MIN_STEPS_FOR_SHIELD) return 0

        return (safeSteps / 1000).coerceAtLeast(0)
    }

    fun calculateGoalBonusHours(
        steps: Int,
        goal: Int
    ): Int {
        val safeSteps = steps.coerceAtLeast(0)
        val safeGoal = goal.coerceAtLeast(1000)

        if (safeSteps < safeGoal) return 0

        return when {
            safeGoal < 10_000 -> 1
            safeGoal < 15_000 -> 2
            else -> 3
        }
    }

    fun shouldShieldDrain(todaySteps: Int): Boolean {
        return todaySteps < MIN_STEPS_TO_STOP_DRAIN
    }

    fun canEarnShield(todaySteps: Int): Boolean {
        return todaySteps >= MIN_STEPS_FOR_SHIELD
    }

    fun getPremiumRescueHours(): Int = PREMIUM_RESCUE_HOURS

    fun getMonthlyPremiumRescueLimit(): Int = PREMIUM_MONTHLY_RESCUES
}