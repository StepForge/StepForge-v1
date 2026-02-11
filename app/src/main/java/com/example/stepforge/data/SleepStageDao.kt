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
}