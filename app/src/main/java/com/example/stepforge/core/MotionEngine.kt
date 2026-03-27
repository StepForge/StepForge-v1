package com.example.stepforge.core

import android.content.Context
import android.os.SystemClock

class MotionEngine {

    private var currentState: UserState = UserState.IDLE

    private var lastStepTime: Long = 0L
    private var lastStateChange: Long = 0L

    fun onStepDetected() {
        val now = SystemClock.elapsedRealtime()
        lastStepTime = now

        if (currentState != UserState.WALKING) {
            changeState(UserState.WALKING)
        }
    }

    fun update() {
        val now = SystemClock.elapsedRealtime()

        // 8 saniye adım yoksa → IDLE
        if (currentState == UserState.WALKING) {
            if (now - lastStepTime > 8000) {
                changeState(UserState.IDLE)
            }
        }
    }

    private fun changeState(newState: UserState) {
        if (newState == currentState) return

        currentState = newState
        lastStateChange = SystemClock.elapsedRealtime()
    }

    fun getState(): UserState = currentState
}