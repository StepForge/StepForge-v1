package com.example.stepforge.steps

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.LocalDate
import java.time.ZoneId

/**
 * Single source of truth for live step count across UI, widgets, notifications,
 * workouts, streak, and insights. Updated immediately on every sensor change.
 */
object CentralStepState {

    data class Snapshot(
        val steps: Int = 0,
        val goal: Int = 10_000,
        val date: String = todayKey(),
        val updatedAtMs: Long = 0L,
        val isRestored: Boolean = false
    )

    private val _snapshot = MutableStateFlow(Snapshot())
    val snapshot: StateFlow<Snapshot> = _snapshot.asStateFlow()

    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps.asStateFlow()

    fun todayKey(): String = LocalDate.now(ZoneId.systemDefault()).toString()

    fun emit(steps: Int, goal: Int? = null, date: String = todayKey(), isRestored: Boolean = false) {
        val safeSteps = steps.coerceAtLeast(0)
        val safeGoal = (goal ?: _snapshot.value.goal).coerceIn(1000, 100_000)
        val now = System.currentTimeMillis()
        _snapshot.value = Snapshot(
            steps = safeSteps,
            goal = safeGoal,
            date = date,
            updatedAtMs = now,
            isRestored = isRestored
        )
        _todaySteps.value = safeSteps
    }

    fun currentSteps(): Int = _snapshot.value.steps
    fun currentGoal(): Int = _snapshot.value.goal
}
