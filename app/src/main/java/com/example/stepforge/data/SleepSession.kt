package com.example.stepforge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir uyku oturumu. Aynı tarihte birden fazla kayıt olabilir (gece, kısa uyku, nap).
 *
 * source: "manual" | "auto" | "health_connect"
 * type: "main" | "nap"
 */
@Entity(tableName = "sleep_session")
data class SleepSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val startTime: Long,
    val endTime: Long,
    val totalMinutes: Int,
    val qualityScore: Int,
    val source: String = "manual",
    val type: String = "main",
    val notes: String = ""
)
