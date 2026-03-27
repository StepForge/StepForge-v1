package com.example.stepforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyWaterDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailyWater(dailyWater: DailyWater)

    @Query("SELECT * FROM daily_water WHERE date = :date LIMIT 1")
    suspend fun getWaterForDate(date: String): DailyWater?

    @Query("SELECT * FROM daily_water")
    suspend fun getAllWater(): List<DailyWater>

    // ✅ LIVE observe (History için)
    @Query("SELECT * FROM daily_water")
    fun observeAllWater(): Flow<List<DailyWater>>
}