package com.example.stepforge.steps

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * StepEvents – StepCounterService'ten gelen anlık adım verisini
 * UI'ya (Compose tarafına) aktaran global akış.
 */
object StepEvents {

    // Güncel adım sayısı
    private val _todaySteps = MutableStateFlow(0)
    val todaySteps: StateFlow<Int> = _todaySteps

    // Servis her yeni adımda burayı güncelliyor
    fun emitTodaySteps(value: Int) {
        _todaySteps.value = value
    }
}
