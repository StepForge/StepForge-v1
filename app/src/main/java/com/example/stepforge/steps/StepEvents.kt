package com.example.stepforge.steps

import kotlinx.coroutines.flow.StateFlow

/**
 * Backward-compatible facade over [CentralStepState].
 */
object StepEvents {

    val todaySteps: StateFlow<Int> = CentralStepState.todaySteps

    fun emitTodaySteps(value: Int) {
        CentralStepState.emit(value)
    }

    val snapshot get() = CentralStepState.snapshot
}
