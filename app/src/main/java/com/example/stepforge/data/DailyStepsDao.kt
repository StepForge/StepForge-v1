package com.example.stepforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DailyStepsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDailySteps(dailySteps: DailySteps)

    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    suspend fun getAllSteps(): List<DailySteps>

    // ✅ LIVE observe (History için)
    @Query("SELECT * FROM daily_steps ORDER BY date DESC")
    fun observeAllSteps(): Flow<List<DailySteps>>

    @Query("DELETE FROM daily_steps")
    suspend fun clearAll()

    @Query("DELETE FROM daily_steps WHERE date LIKE 'TEST-%'")
    suspend fun deleteTestDays()
}