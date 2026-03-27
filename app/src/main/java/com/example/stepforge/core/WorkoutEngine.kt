package com.example.stepforge.core

import android.util.Log

class WorkoutEngine {

    private var isWalking = false

    private var startSteps = 0
    private var startTime = 0L

    fun onStateChanged(
        state: UserState,
        currentSteps: Int,
        currentTime: Long
    ): WorkoutEvent? {

        when (state) {

            UserState.WALKING -> {
                if (!isWalking) {
                    isWalking = true
                    startSteps = currentSteps
                    startTime = currentTime

                    Log.d("WorkoutEngine", "WORKOUT STARTED")

                    return WorkoutEvent.Started
                }
            }

            UserState.IDLE -> {
                if (isWalking) {
                    isWalking = false

                    val duration = currentTime - startTime
                    val steps = currentSteps - startSteps

                    Log.d("WorkoutEngine", "WORKOUT ENDED: $steps steps")

                    return WorkoutEvent.Finished(
                        steps = steps,
                        duration = duration,
                        startTime = startTime,
                        endTime = currentTime
                    )
                }
            }

            else -> {}
        }

        return null
    }
}