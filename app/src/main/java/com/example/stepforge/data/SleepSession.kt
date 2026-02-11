package com.example.stepforge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir uyku oturumu: tek bir gece için toplam uyku süresi.
 *
 * source: "manual" | "health_connect"
 */
@Entity(tableName = "sleep_session")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val totalMinutes: Int,
    val qualityScore: Int,
    val source: String = "manual"
)