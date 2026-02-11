package com.example.stepforge.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "water_intake_event",
    indices = [
        Index(value = ["date", "timeMillis"])
    ]
)
data class WaterIntakeEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,        // "yyyy-MM-dd"
    val timeMillis: Long,    // event time
    val amountMl: Int        // 250 / 500 / custom...
)