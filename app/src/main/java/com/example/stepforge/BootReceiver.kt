package com.example.stepforge

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {

        val action = intent.action ?: return

        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) return

        val request = OneTimeWorkRequestBuilder<StartStepServiceWorker>()
            .setInitialDelay(10, TimeUnit.SECONDS)
            .build()

        WorkManager.getInstance(context).enqueue(request)
    }
}
