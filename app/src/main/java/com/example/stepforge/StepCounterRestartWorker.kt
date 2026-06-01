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

        android.util.Log.e(
            "STEPFORGE_MIDNIGHT",
            "StepCounterRestartWorker triggered"
        )

        try {
            // Widget refresh her zaman
            StepWidgetProvider.notifyRefresh(ctx)
            StepWidgetCompactProvider.notifyRefresh(ctx)
            StepWidgetLargeProvider.notifyRefresh(ctx)

            // ✅ Servis zaten çalışıyorsa tekrar başlatma
            if (StepCounterService.isServiceRunning) {
                Log.d("StepForgeDebug", "RestartWorker: Service already running, skip.")
                return Result.success()
            }

            val intent = Intent(ctx, StepCounterService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(intent)
            } else {
                ctx.startService(intent)
            }

            Log.d("StepForgeDebug", "RestartWorker started StepCounterService successfully.")
            return Result.success()

        } catch (e: Exception) {

            Log.e("StepForgeDebug", "RestartWorker failed to start service", e)

            android.util.Log.e(
                "STEPFORGE_MIDNIGHT",
                "RestartWorker exception: ${e.message}"
            )

            return Result.success()
        }
    }
}