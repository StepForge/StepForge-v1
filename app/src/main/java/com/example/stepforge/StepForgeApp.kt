package com.example.stepforge

import android.app.Application
import com.example.stepforge.core.AppLanguageHelper
import com.example.stepforge.core.AppStateMigrationManager
import com.example.stepforge.debug.AnrWatchdog
import com.example.stepforge.debug.DebugInitializer
import com.example.stepforge.debug.DebugLogger
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

class StepForgeApp : Application() {

    private var anrWatchdog: AnrWatchdog? = null

    override fun onCreate() {
        super.onCreate()

        AppLanguageHelper.initialize(this)
        DebugInitializer.init(this)

        MainScope().launch {
            AppStateMigrationManager.runMigrations(this@StepForgeApp)
        }

        anrWatchdog = AnrWatchdog(
            timeoutMs = 6000L,
            checkIntervalMs = 1500L
        ) { blockedMs ->
            DebugLogger.e(
                tag = "ANR-Watchdog",
                message = "Main thread blocked for ${blockedMs}ms",
                metadata = mapOf(
                    "blockedMs" to blockedMs.toString()
                )
            )
        }

        anrWatchdog?.start()
    }
}
