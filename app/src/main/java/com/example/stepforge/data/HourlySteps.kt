package com.example.stepforge.data

import androidx.room.Entity

/**
 * Saatlik snapshot tablosu.
 * Her gün + saat için tek satır tutulur.
 * steps = günün o ana kadarki toplam adımı (snapshot).
 */
@Entity(
    tableName = "hourly_steps",
    primaryKeys = ["date", "hour"]
)
data class HourlySteps(
    val date: String,   // "yyyy-MM-dd"
    val hour: Int,      // 0..23
    val steps: Int,     // o ana kadarki toplam adım (snapshot)
    val source: String = "sensor"
)
