package com.example.stepforge.steps

import android.util.Log
import com.example.stepforge.StepCounterService
import com.example.stepforge.debug.DebugLogger

/**
 * End-of-day analytics only. Never blocks, rejects, or modifies live step counts.
 */
object StepValidationAnalyzer {

    private const val TAG = "StepValidation"

    data class DayAnalysisInput(
        val date: String,
        val totalSteps: Int,
        val goal: Int,
        val maxSingleBurst: Int,
        val sensorResets: Int,
        val hourlySamples: List<Int> = emptyList()
    )

    fun analyzeCompletedDay(input: DayAnalysisInput) {
        val findings = mutableListOf<String>()

        if (input.maxSingleBurst > 120) {
            findings += "Suspicious burst detected (max burst=${input.maxSingleBurst})"
        }
        if (input.sensorResets > 2) {
            findings += "Possible sensor anomaly (${input.sensorResets} counter resets)"
        }
        if (input.totalSteps > 0 && input.hourlySamples.isNotEmpty()) {
            val maxHour = input.hourlySamples.maxOrNull() ?: 0
            val avg = input.hourlySamples.average()
            if (maxHour > avg * 4 && maxHour > 3000) {
                findings += "Unusual cadence detected (hourly spike)"
            }
        }
        if (input.totalSteps > input.goal * 3) {
            findings += "Extreme daily total vs goal (anti-cheat hint)"
        }

        if (findings.isEmpty()) {
            DebugLogger.d(
                StepCounterService.TAG,
                "Day validation: no anomalies",
                metadata = mapOf("date" to input.date, "steps" to input.totalSteps.toString())
            )
            return
        }

        findings.forEach { finding ->
            Log.i(TAG, "[${input.date}] $finding (steps=${input.totalSteps})")
            DebugLogger.i(
                StepCounterService.TAG,
                "Day validation finding",
                metadata = mapOf(
                    "date" to input.date,
                    "finding" to finding,
                    "steps" to input.totalSteps.toString()
                )
            )
        }
    }

    /** Live-path: accumulate stats only; never block steps. */
    @Volatile
    var liveMaxBurst: Int = 0
        private set

    @Volatile
    var liveSensorResets: Int = 0
        private set

    fun noteLiveBurst(diff: Int) {
        if (diff > liveMaxBurst) liveMaxBurst = diff
    }

    fun noteSensorReset() {
        liveSensorResets++
    }

    fun resetLiveCounters() {
        liveMaxBurst = 0
        liveSensorResets = 0
    }

    fun consumeLiveStats(): Pair<Int, Int> {
        val burst = liveMaxBurst
        val resets = liveSensorResets
        resetLiveCounters()
        return burst to resets
    }
}
