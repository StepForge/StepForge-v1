package com.example.stepforge.ui.insights

import android.content.Context
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.HourlySteps

class InsightsRepository(private val ctx: Context) {

    private val db = AppDatabase.getDatabase(ctx)

    suspend fun getAllDailySteps(): List<DailySteps> {
        return db.dailyStepsDao().getAllSteps()
    }

    suspend fun getAllHourlySteps(): List<HourlySteps> {
        return db.hourlyStepsDao().getAll()
    }
}
