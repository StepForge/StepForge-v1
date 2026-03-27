package com.example.stepforge.debug

data class DebugLogEntry(
    val id: Long,
    val timestamp: Long,
    val level: DebugLogLevel,
    val tag: String,
    val message: String,
    val threadName: String,
    val className: String,
    val methodName: String,
    val lineNumber: Int,
    val stackTrace: String? = null,
    val throwableType: String? = null,
    val packageName: String? = null,
    val sourceFile: String? = null,
    val metadata: Map<String, String> = emptyMap()
)