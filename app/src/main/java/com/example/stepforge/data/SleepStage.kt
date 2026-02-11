package com.example.stepforge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Bir oturum içindeki uyku evreleri (light/deep/REM/wake).
 * Zorunlu değil ama Health Connect'ten geldiğinde saklayacağız.
 */
@Entity(tableName = "sleep_stage")
data class SleepStage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,      // SleepSession.id ile ilişki
    val stageType: String,    // "light", "deep", "rem", "awake" vb.
    val startTime: Long,
    val endTime: Long
)