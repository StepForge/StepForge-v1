package com.example.stepforge.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "workout_session",
    indices = [
        Index(value = ["date", "startTime"])
    ]
)
data class WorkoutSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,            // "yyyy-MM-dd"
    val startTime: Long,         // session start millis
    val endTime: Long,           // session end millis
    val durationMinutes: Int,    // total duration in minutes
    val steps: Int,              // steps taken during session
    val distanceMeters: Int,     // estimated distance in meters
    val caloriesKcal: Int,       // estimated calories
    val avgStepsPerMinute: Int,  // average cadence
    val source: String = "auto_walk"
)