package com.example.stepforge

import android.app.Application
import com.example.stepforge.debug.AnrWatchdog
import com.example.stepforge.debug.DebugInitializer
import com.example.stepforge.debug.DebugLogger

class StepForgeApp : Application() {

    private var anrWatchdog: AnrWatchdog? = null

    override fun onCreate() {
        super.onCreate()

        DebugInitializer.init(this)

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