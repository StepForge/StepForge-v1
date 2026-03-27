package com.example.stepforge.debug

import android.util.Log
import java.io.PrintWriter
import java.io.StringWriter

object DebugLogger {

    private const val DEFAULT_TAG = "StepForgeDebug"

    fun v(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        log(DebugLogLevel.VERBOSE, tag, message, throwable, metadata)
    }

    fun d(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        log(DebugLogLevel.DEBUG, tag, message, throwable, metadata)
    }

    fun i(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        log(DebugLogLevel.INFO, tag, message, throwable, metadata)
    }

    fun w(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        log(DebugLogLevel.WARNING, tag, message, throwable, metadata)
    }

    fun e(tag: String = DEFAULT_TAG, message: String, throwable: Throwable? = null, metadata: Map<String, String> = emptyMap()) {
        log(DebugLogLevel.ERROR, tag, message, throwable, metadata)
    }

    fun crash(tag: String = DEFAULT_TAG, message: String, throwable: Throwable, metadata: Map<String, String> = emptyMap()) {
        log(DebugLogLevel.CRASH, tag, message, throwable, metadata)
    }

    private fun log(
        level: DebugLogLevel,
        tag: String,
        message: String,
        throwable: Throwable?,
        metadata: Map<String, String>
    ) {
        val trace = resolveCaller()
        val stackTraceText = throwable?.stackTraceToStringSafe()

        val entry = DebugLogEntry(
            id = DebugLogStore.nextId(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            threadName = Thread.currentThread().name,
            className = trace.className,
            methodName = trace.methodName,
            lineNumber = trace.lineNumber,
            stackTrace = stackTraceText,
            throwableType = throwable?.javaClass?.name,
            packageName = trace.packageName,
            sourceFile = trace.fileName,
            metadata = metadata
        )

        DebugLogStore.append(entry)

        if (true) {
            when (level) {
                DebugLogLevel.VERBOSE -> Log.v(tag, message, throwable)
                DebugLogLevel.DEBUG -> Log.d(tag, message, throwable)
                DebugLogLevel.INFO -> Log.i(tag, message, throwable)
                DebugLogLevel.WARNING -> Log.w(tag, message, throwable)
                DebugLogLevel.ERROR, DebugLogLevel.CRASH -> Log.e(tag, message, throwable)
            }
        }
    }

    private fun Throwable.stackTraceToStringSafe(): String {
        return try {
            val sw = StringWriter()
            val pw = PrintWriter(sw)
            printStackTrace(pw)
            sw.toString()
        } catch (_: Exception) {
            message ?: javaClass.name
        }
    }

    private fun resolveCaller(): CallerInfo {
        val stack = Throwable().stackTrace
        val loggerClass = DebugLogger::class.java.name

        val element = stack.firstOrNull {
            !it.className.startsWith(loggerClass) &&
                    !it.className.startsWith("java.lang.Thread") &&
                    !it.className.contains("DebugLogger")
        } ?: stack.getOrNull(0)

        if (element == null) {
            return CallerInfo("", "", -1, "", "")
        }

        val packageName = element.className.substringBeforeLast('.', "")
        return CallerInfo(
            className = element.className,
            methodName = element.methodName,
            lineNumber = element.lineNumber,
            fileName = element.fileName ?: "",
            packageName = packageName
        )
    }

    private data class CallerInfo(
        val className: String,
        val methodName: String,
        val lineNumber: Int,
        val fileName: String,
        val packageName: String
    )
}