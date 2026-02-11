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

    @Query("SELECT * FROM sleep_session WHERE date = :date LIMIT 1")
    suspend fun getSessionForDate(date: String): SleepSession?

    @Query("SELECT * FROM sleep_session ORDER BY date DESC LIMIT :limit")
    suspend fun getRecentSessions(limit: Int): List<SleepSession>

    // ✅ date bazlı overwrite için yardımcı
    @Query("DELETE FROM sleep_session WHERE date = :date")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM sleep_session WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM sleep_session WHERE date = :date LIMIT 1")
    fun observeSessionForDate(date: String): Flow<SleepSession?>

}