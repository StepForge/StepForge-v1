package com.example.stepforge

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters

class StartStepServiceWorker(
    private val context: Context,
    params: WorkerParameters
) : Worker(context, params) {

    override fun doWork(): Result {
        return try {
            if (StepCounterService.isServiceRunning) {
                Log.d("StepForgeDebug", "Worker: Service already running, skip start.")
                return Result.success()
            }

            val serviceIntent = Intent(context, StepCounterService::class.java)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }

            Log.d("StepForgeDebug", "StartStepServiceWorker started StepCounterService.")
            Result.success()

        } catch (e: Exception) {
            Log.e("StepForgeDebug", "StartStepServiceWorker failed", e)
            Result.success()
        }
    }

}
