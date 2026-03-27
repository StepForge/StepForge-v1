package com.example.stepforge.core

sealed class WorkoutSessionTransition {
    data class Started(
        val startTimeMs: Long,
        val startSteps: Int
    ) : WorkoutSessionTransition()

    data class Finished(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val startSteps: Int,
        val endSteps: Int,
        val stepsTaken: Int,
        val durationMs: Long,
        val durationMinutes: Int
    ) : WorkoutSessionTransition()
}

class WorkoutSessionEngine(
    private val stopThresholdMs: Long = 25_000L
) {
    private var active = false
    private var sessionStartTimeMs = 0L
    private var sessionStartSteps = 0
    private var lastStepTimeMs = 0L
    private var lastTotalSteps = 0

    fun onStep(nowMs: Long, totalSteps: Int): List<WorkoutSessionTransition> {
        val events = mutableListOf<WorkoutSessionTransition>()

        if (!active) {
            startSession(nowMs, totalSteps)
            events += WorkoutSessionTransition.Started(nowMs, totalSteps)
        } else if (lastStepTimeMs > 0L && nowMs - lastStepTimeMs > stopThresholdMs) {
            events += finishSession(lastStepTimeMs)
            startSession(nowMs, totalSteps)
            events += WorkoutSessionTransition.Started(nowMs, totalSteps)
        }

        lastStepTimeMs = nowMs
        lastTotalSteps = totalSteps
        return events
    }

    fun tick(nowMs: Long): WorkoutSessionTransition.Finished? {
        if (!active) return null
        if (lastStepTimeMs <= 0L) return null
        if (nowMs - lastStepTimeMs <= stopThresholdMs) return null

        val finished = finishSession(lastStepTimeMs)
        active = false
        return finished
    }

    fun isActive(): Boolean = active

    fun reset() {
        active = false
        sessionStartTimeMs = 0L
        sessionStartSteps = 0
        lastStepTimeMs = 0L
        lastTotalSteps = 0
    }

    private fun startSession(nowMs: Long, totalSteps: Int) {
        active = true
        sessionStartTimeMs = nowMs
        sessionStartSteps = totalSteps
    }

    private fun finishSession(endTimeMs: Long): WorkoutSessionTransition.Finished {
        val endSteps = lastTotalSteps
        val stepsTaken = (endSteps - sessionStartSteps).coerceAtLeast(0)
        val durationMs = (endTimeMs - sessionStartTimeMs).coerceAtLeast(0L)
        val durationMinutes = ((durationMs + 59_999L) / 60_000L).toInt().coerceAtLeast(1)

        return WorkoutSessionTransition.Finished(
            startTimeMs = sessionStartTimeMs,
            endTimeMs = endTimeMs,
            startSteps = sessionStartSteps,
            endSteps = endSteps,
            stepsTaken = stepsTaken,
            durationMs = durationMs,
            durationMinutes = durationMinutes
        )
    }
}
