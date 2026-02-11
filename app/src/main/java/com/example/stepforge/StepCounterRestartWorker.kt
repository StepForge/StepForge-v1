package com.example.stepforge

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.stepforge.widget.StepWidgetCompactProvider
import com.example.stepforge.widget.StepWidgetLargeProvider
import com.example.stepforge.widget.StepWidgetProvider

class StepCounterRestartWorker(
    private val ctx: Context,
    params: WorkerParameters
) : Worker(ctx, params) {

    override fun doWork(): Result {
        try {
            // Widget refresh her zaman
            StepWidgetProvider.notifyRefresh(ctx)
            StepWidgetCompactProvider.notifyRefresh(ctx)
            StepWidgetLargeProvider.notifyRefresh(ctx)

            val intent = Intent(ctx, StepCounterService::class.java)

            // Android 8+ için FGS
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }

            Log.d("StepForgeDebug", "RestartWorker started StepCounterService successfully.")
            return Result.success()

        } catch (e: Exception) {
            Log.e("StepForgeDebug", "RestartWorker failed to start service", e)

            // Android 12+ bazen exception atar ama worker fail olmasın
            return Result.success()
        }
    }
}
