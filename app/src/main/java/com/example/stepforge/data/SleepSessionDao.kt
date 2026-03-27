package com.example.stepforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SleepSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SleepSession): Long

    @Query("SELECT * FROM sleep_session WHERE date = :date ORDER BY startTime DESC")
    suspend fun getSessionsForDate(date: String): List<SleepSession>

    @Query("SELECT * FROM sleep_session WHERE date = :date ORDER BY startTime DESC LIMIT 1")
    suspend fun getSessionForDate(date: String): SleepSession?

    @Query("SELECT * FROM sleep_session ORDER BY startTime DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<SleepSession>

    @Query("DELETE FROM sleep_session WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sleep_session WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM sleep_session WHERE source = :source AND date BETWEEN :fromDate AND :toDate")
    suspend fun deleteBySourceAndDateRange(source: String, fromDate: String, toDate: String)

    @Query("SELECT * FROM sleep_session WHERE date = :date ORDER BY startTime DESC")
    fun observeSessionsForDate(date: String): Flow<List<SleepSession>>

    @Query("SELECT * FROM sleep_session WHERE date = :date ORDER BY startTime DESC LIMIT 1")
    fun observeSessionForDate(date: String): Flow<SleepSession?>
}
