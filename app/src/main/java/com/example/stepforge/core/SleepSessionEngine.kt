package com.example.stepforge.core

sealed class SleepSessionTransition {
    data class Started(
        val startTimeMs: Long,
        val reason: String
    ) : SleepSessionTransition()

    data class Finished(
        val startTimeMs: Long,
        val endTimeMs: Long,
        val durationMinutes: Int,
        val reason: String
    ) : SleepSessionTransition()
}

class SleepSessionEngine(
    private val idleStartThresholdMs: Long = 35 * 60_000L,
    private val wakeConfirmThresholdMs: Long = 3 * 60_000L,
    private val nightStartHour: Int = 20,
    private val nightEndHour: Int = 10
) {
    private var probableSleepActive = false
    private var probableSleepStartMs = 0L
    private var lastStepMs = 0L
    private var lastMotionMs = 0L
    private var lastScreenOffMs = 0L

    fun onStep(nowMs: Long) {
        lastStepMs = nowMs
    }

    fun onMotion(nowMs: Long, motionDetected: Boolean) {
        if (motionDetected) {
            lastMotionMs = nowMs
        }
    }

    fun onScreenOff(nowMs: Long) {
        lastScreenOffMs = nowMs
    }

    fun onScreenOn(nowMs: Long): SleepSessionTransition.Finished? {
        if (!probableSleepActive) return null
        return finish(nowMs, "screen_on")
    }

    fun tick(nowMs: Long, hourOfDay: Int): SleepSessionTransition? {
        if (!probableSleepActive) {
            if (isNightHour(hourOfDay) && isIdle(nowMs)) {
                probableSleepActive = true
                probableSleepStartMs = maxOf(lastStepMs, lastMotionMs, lastScreenOffMs).coerceAtMost(nowMs)
                if (probableSleepStartMs <= 0L) probableSleepStartMs = nowMs
                return SleepSessionTransition.Started(
                    startTimeMs = probableSleepStartMs,
                    reason = "idle_night"
                )
            }
            return null
        }

        if (!isIdle(nowMs)) {
            val quietDurationMs = nowMs - maxOf(lastStepMs, lastMotionMs)
            if (quietDurationMs >= wakeConfirmThresholdMs) {
                return finish(nowMs, "motion_or_step_resume")
            }
        }

        return null
    }

    fun reset() {
        probableSleepActive = false
        probableSleepStartMs = 0L
        lastStepMs = 0L
        lastMotionMs = 0L
        lastScreenOffMs = 0L
    }

    private fun isIdle(nowMs: Long): Boolean {
        val stepIdle = lastStepMs <= 0L || nowMs - lastStepMs > idleStartThresholdMs
        val motionIdle = lastMotionMs <= 0L || nowMs - lastMotionMs > idleStartThresholdMs
        return stepIdle && motionIdle
    }

    private fun isNightHour(hourOfDay: Int): Boolean {
        // Covers classic sleep (20:00-05:59) and delayed sleep cases (06:00-10:59).
        return hourOfDay >= nightStartHour || hourOfDay <= nightEndHour
    }

    private fun finish(nowMs: Long, reason: String): SleepSessionTransition.Finished {
        val endTimeMs = nowMs.coerceAtLeast(probableSleepStartMs)
        val durationMinutes = ((endTimeMs - probableSleepStartMs).coerceAtLeast(0L) / 60_000L).toInt()
        probableSleepActive = false
        val start = probableSleepStartMs
        probableSleepStartMs = 0L
        return SleepSessionTransition.Finished(
            startTimeMs = start,
            endTimeMs = endTimeMs,
            durationMinutes = durationMinutes,
            reason = reason
        )
    }
}
