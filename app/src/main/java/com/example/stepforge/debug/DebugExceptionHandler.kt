package com.example.stepforge.debug

class DebugExceptionHandler(
    private val previous: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        DebugLogger.crash(
            tag = "GlobalCrash",
            message = throwable.message ?: "Uncaught exception",
            throwable = throwable,
            metadata = mapOf(
                "thread" to thread.name
            )
        )

        previous?.uncaughtException(thread, throwable)
    }
}