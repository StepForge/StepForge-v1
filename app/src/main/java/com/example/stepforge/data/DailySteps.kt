package com.example.stepforge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_steps")
data class DailySteps(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val steps: Int
)