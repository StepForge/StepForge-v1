package com.example.stepforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkoutSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WorkoutSession): Long

    @Query("SELECT * FROM workout_session ORDER BY startTime DESC")
    suspend fun getAll(): List<WorkoutSession>

    @Query("SELECT * FROM workout_session ORDER BY startTime DESC")
    fun observeAll(): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): WorkoutSession?

    @Query("SELECT * FROM workout_session WHERE date = :date ORDER BY startTime DESC")
    suspend fun getAllForDate(date: String): List<WorkoutSession>

    @Query("SELECT * FROM workout_session WHERE date = :date ORDER BY startTime DESC")
    fun observeAllForDate(date: String): Flow<List<WorkoutSession>>

    @Query("SELECT * FROM workout_session ORDER BY startTime DESC LIMIT 1")
    suspend fun getLatest(): WorkoutSession?

    @Query("DELETE FROM workout_session WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM workout_session")
    suspend fun clearAll()

}
