package com.example.stepforge.core

sealed class WorkoutEvent {
    object Started : WorkoutEvent()

    data class Finished(
        val steps: Int,
        val duration: Long,
        val startTime: Long,
        val endTime: Long
    ) : WorkoutEvent()
}