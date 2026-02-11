package com.example.stepforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface WaterIntakeEventDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: WaterIntakeEvent): Long

    @Query("SELECT * FROM water_intake_event WHERE date = :date ORDER BY timeMillis DESC")
    suspend fun getAllForDate(date: String): List<WaterIntakeEvent>

    @Query("SELECT * FROM water_intake_event WHERE date = :date ORDER BY timeMillis DESC LIMIT :limit")
    suspend fun getRecentForDate(date: String, limit: Int): List<WaterIntakeEvent>

    @Query("SELECT * FROM water_intake_event WHERE date = :date ORDER BY timeMillis DESC LIMIT 1")
    suspend fun getLatestForDate(date: String): WaterIntakeEvent?

    @Query("DELETE FROM water_intake_event WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM water_intake_event WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM water_intake_event")
    suspend fun clearAll()
}