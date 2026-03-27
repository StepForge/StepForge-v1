package com.example.stepforge.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SleepStageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(stage: SleepStage): Long

    @Query("SELECT * FROM sleep_stage WHERE sessionId = :sessionId ORDER BY startTime ASC")
    suspend fun getStagesForSession(sessionId: Long): List<SleepStage>

    @Query("DELETE FROM sleep_stage WHERE sessionId = :sessionId")
    suspend fun deleteBySessionId(sessionId: Long)

    @Query("DELETE FROM sleep_stage WHERE sessionId IN (SELECT id FROM sleep_session WHERE date = :date)")
    suspend fun deleteByDate(date: String)

    @Query("DELETE FROM sleep_stage WHERE sessionId IN (SELECT id FROM sleep_session WHERE source = :source AND date BETWEEN :fromDate AND :toDate)")
    suspend fun deleteBySourceAndDateRange(source: String, fromDate: String, toDate: String)
}
