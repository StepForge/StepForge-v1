package com.example.stepforge.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_water")
data class DailyWater(
    @PrimaryKey val date: String, // "yyyy-MM-dd"
    val waterMl: Int              // o gün içilen su (ml)
)