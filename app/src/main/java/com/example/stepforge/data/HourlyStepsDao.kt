package com.example.stepforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface HourlyStepsDao {

    // date+hour PK olduğu için REPLACE saat başına tek satır sağlar
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(hourlySteps: HourlySteps)

    @Query("SELECT * FROM hourly_steps WHERE date = :date ORDER BY hour ASC")
    suspend fun getForDate(date: String): List<HourlySteps>

    @Query("SELECT steps FROM hourly_steps WHERE date = :date AND hour = :hour LIMIT 1")
    suspend fun getStepsForDateHour(date: String, hour: Int): Int?

    // Cloud backup / debug için
    @Query("SELECT * FROM hourly_steps ORDER BY date DESC, hour DESC")
    suspend fun getAll(): List<HourlySteps>

    @Query("DELETE FROM hourly_steps")
    suspend fun clearAll()
}