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

enum class WorkoutSessionState {
    Idle,
    Detecting,
    ActiveWorkout,
    Paused,
    Finalizing,
    Saved,
    Discarded
}

class WorkoutSessionEngine(
    private val inactivityTimeoutMs: Long = 10L * 60L * 1000L,
    private val detectionStaleMs: Long = 90_000L,
    private val minStartDurationMs: Long = 30_000L,
    private val minStartSteps: Int = 30,
    private val minStartCadenceSpm: Int = 40,
    private val pauseAfterMs: Long = 2L * 60L * 1000L
) {
    private var state = WorkoutSessionState.Idle
    private var detectingStartTimeMs = 0L
    private var detectingStartSteps = 0
    private var sessionStartTimeMs = 0L
    private var sessionStartSteps = 0
    private var lastStepTimeMs = 0L
    private var lastTotalSteps = 0

    fun onStep(nowMs: Long, totalSteps: Int): List<WorkoutSessionTransition> {
        val events = mutableListOf<WorkoutSessionTransition>()
        val safeTotal = totalSteps.coerceAtLeast(0)
        val gapMs = if (lastStepTimeMs > 0L) nowMs - lastStepTimeMs else 0L

        if (lastTotalSteps > 0 && safeTotal <= lastTotalSteps) {
            lastStepTimeMs = nowMs
            return events
        }

        if (isActive() && gapMs > inactivityTimeoutMs) {
            events += finishSession(lastStepTimeMs)
            beginDetection(nowMs, safeTotal)
        } else {
            when (state) {
                WorkoutSessionState.Idle,
                WorkoutSessionState.Saved,
                WorkoutSessionState.Discarded,
                WorkoutSessionState.Finalizing -> beginDetection(nowMs, safeTotal)

                WorkoutSessionState.Detecting -> {
                    if (gapMs > detectionStaleMs) {
                        beginDetection(nowMs, safeTotal)
                    } else if (hasWalkingPattern(nowMs, safeTotal)) {
                        startSessionFromDetection()
                        events += WorkoutSessionTransition.Started(
                            startTimeMs = sessionStartTimeMs,
                            startSteps = sessionStartSteps
                        )
                    }
                }

                WorkoutSessionState.ActiveWorkout,
                WorkoutSessionState.Paused -> {
                    state = WorkoutSessionState.ActiveWorkout
                }
            }
        }

        lastStepTimeMs = nowMs
        lastTotalSteps = safeTotal
        return events
    }

    fun tick(nowMs: Long): WorkoutSessionTransition.Finished? {
        if (state == WorkoutSessionState.Detecting && lastStepTimeMs > 0L && nowMs - lastStepTimeMs > detectionStaleMs) {
            resetToDiscarded()
            return null
        }

        if (!isActive()) return null
        if (lastStepTimeMs <= 0L) return null

        val idleMs = nowMs - lastStepTimeMs
        if (idleMs > pauseAfterMs && idleMs <= inactivityTimeoutMs) {
            state = WorkoutSessionState.Paused
            return null
        }
        if (idleMs <= inactivityTimeoutMs) return null

        state = WorkoutSessionState.Finalizing
        val finished = finishSession(lastStepTimeMs)
        state = WorkoutSessionState.Idle
        return finished
    }

    fun isActive(): Boolean =
        state == WorkoutSessionState.ActiveWorkout || state == WorkoutSessionState.Paused

    fun currentState(): WorkoutSessionState = state

    fun restoreActiveSession(
        startTimeMs: Long,
        startSteps: Int,
        lastStepTimeMs: Long,
        totalSteps: Int
    ) {
        if (startTimeMs <= 0L) {
            reset()
            return
        }

        state = WorkoutSessionState.ActiveWorkout
        detectingStartTimeMs = 0L
        detectingStartSteps = 0
        sessionStartTimeMs = startTimeMs
        sessionStartSteps = startSteps.coerceAtLeast(0)
        this.lastStepTimeMs = lastStepTimeMs.coerceAtLeast(startTimeMs)
        lastTotalSteps = totalSteps.coerceAtLeast(startSteps)
    }

    fun reset() {
        state = WorkoutSessionState.Idle
        detectingStartTimeMs = 0L
        detectingStartSteps = 0
        sessionStartTimeMs = 0L
        sessionStartSteps = 0
        lastStepTimeMs = 0L
        lastTotalSteps = 0
    }

    private fun beginDetection(nowMs: Long, totalSteps: Int) {
        state = WorkoutSessionState.Detecting
        detectingStartTimeMs = nowMs
        detectingStartSteps = totalSteps
        lastStepTimeMs = nowMs
        lastTotalSteps = totalSteps
    }

    private fun hasWalkingPattern(nowMs: Long, totalSteps: Int): Boolean {
        val detectionDurationMs = nowMs - detectingStartTimeMs
        if (detectionDurationMs < minStartDurationMs) return false

        val detectionSteps = (totalSteps - detectingStartSteps).coerceAtLeast(0)
        if (detectionSteps < minStartSteps) return false

        val cadenceSpm = detectionSteps * 60_000L / detectionDurationMs.coerceAtLeast(1L)
        return cadenceSpm >= minStartCadenceSpm
    }

    private fun startSessionFromDetection() {
        state = WorkoutSessionState.ActiveWorkout
        sessionStartTimeMs = detectingStartTimeMs
        sessionStartSteps = detectingStartSteps
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

    private fun resetToDiscarded() {
        state = WorkoutSessionState.Discarded
        detectingStartTimeMs = 0L
        detectingStartSteps = 0
        sessionStartTimeMs = 0L
        sessionStartSteps = 0
        lastStepTimeMs = 0L
        lastTotalSteps = 0
    }
}
