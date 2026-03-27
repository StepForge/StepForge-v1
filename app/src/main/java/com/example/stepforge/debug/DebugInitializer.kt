package com.example.stepforge.debug

import android.content.Context
import android.content.pm.ApplicationInfo

object DebugInitializer {

    fun init(context: Context) {
        val isDebuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0

        DebugLogStore.init(context)

        if (isDebuggable) {
            val previous = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler(
                DebugExceptionHandler(previous)
            )
        }

        DebugLogger.i(
            tag = "DebugInit",
            message = "Debug system initialized",
            metadata = mapOf(
                "isDebuggable" to isDebuggable.toString()
            )
        )
    }
}