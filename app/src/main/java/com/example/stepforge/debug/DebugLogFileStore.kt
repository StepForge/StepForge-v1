package com.example.stepforge.debug

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class DebugLogFileStore(private val context: Context) {

    companion object {
        private const val FILE_NAME = "debug_logs.json"
        private const val MAX_PERSISTED_LOGS = 1200
    }

    private val lock = ReentrantLock()

    private val file: File
        get() = File(context.filesDir, FILE_NAME)

    fun load(): List<DebugLogEntry> = lock.withLock {
        if (!file.exists()) return emptyList()

        return try {
            val text = file.readText()
            if (text.isBlank()) return emptyList()

            val arr = JSONArray(text)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    add(obj.toEntry())
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(entries: List<DebugLogEntry>) = lock.withLock {
        try {
            val safe = entries.takeLast(MAX_PERSISTED_LOGS)
            val arr = JSONArray()
            safe.forEach { arr.put(it.toJson()) }
            file.writeText(arr.toString())
        } catch (_: Exception) {
        }
    }

    fun clear() = lock.withLock {
        try {
            if (file.exists()) file.delete()
        } catch (_: Exception) {
        }
    }

    fun exportFile(): File? = lock.withLock {
        return if (file.exists()) file else null
    }

    private fun DebugLogEntry.toJson(): JSONObject {
        val metadataObj = JSONObject()
        metadata.forEach { (k, v) -> metadataObj.put(k, v) }

        return JSONObject().apply {
            put("id", id)
            put("timestamp", timestamp)
            put("level", level.name)
            put("tag", tag)
            put("message", message)
            put("threadName", threadName)
            put("className", className)
            put("methodName", methodName)
            put("lineNumber", lineNumber)
            put("stackTrace", stackTrace)
            put("throwableType", throwableType)
            put("packageName", packageName)
            put("sourceFile", sourceFile)
            put("metadata", metadataObj)
        }
    }

    private fun JSONObject.toEntry(): DebugLogEntry {
        val metadataMap = mutableMapOf<String, String>()
        val metadataObj = optJSONObject("metadata")
        if (metadataObj != null) {
            val keys = metadataObj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                metadataMap[key] = metadataObj.optString(key, "")
            }
        }

        return DebugLogEntry(
            id = optLong("id", 0L),
            timestamp = optLong("timestamp", 0L),
            level = runCatching { DebugLogLevel.valueOf(optString("level")) }.getOrDefault(DebugLogLevel.DEBUG),
            tag = optString("tag", "Unknown"),
            message = optString("message", ""),
            threadName = optString("threadName", ""),
            className = optString("className", ""),
            methodName = optString("methodName", ""),
            lineNumber = optInt("lineNumber", -1),
            stackTrace = optString("stackTrace", null),
            throwableType = optString("throwableType", null),
            packageName = optString("packageName", null),
            sourceFile = optString("sourceFile", null),
            metadata = metadataMap
        )
    }
}