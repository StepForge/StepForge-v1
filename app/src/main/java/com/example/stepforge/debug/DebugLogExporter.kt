package com.example.stepforge.debug

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object DebugLogExporter {

    fun exportToTextFile(context: Context, logs: List<DebugLogEntry>): File? {
        return try {
            val dir = File(context.cacheDir, "debug_exports")
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val file = File(dir, "stepforge_debug_logs.txt")
            file.writeText(buildText(logs))
            file
        } catch (e: Exception) {
            DebugLogger.e(
                tag = "DebugExport",
                message = "Failed to export debug logs to file",
                throwable = e
            )
            null
        }
    }

    fun shareFile(context: Context, file: File): Boolean {
        return try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share debug logs"))
            true
        } catch (e: Exception) {
            DebugLogger.e(
                tag = "DebugExport",
                message = "Failed to share debug log file",
                throwable = e
            )
            false
        }
    }

    private fun buildText(logs: List<DebugLogEntry>): String {
        return buildString {
            logs.forEach { log ->
                appendLine("====================================================")
                appendLine("ID: ${log.id}")
                appendLine("Time: ${log.timestamp}")
                appendLine("Level: ${log.level}")
                appendLine("Tag: ${log.tag}")
                appendLine("Thread: ${log.threadName}")
                appendLine("Class: ${log.className}")
                appendLine("Method: ${log.methodName}")
                appendLine("Line: ${log.lineNumber}")
                appendLine("Message: ${log.message}")
                appendLine("Throwable: ${log.throwableType ?: "-"}")

                if (log.metadata.isNotEmpty()) {
                    appendLine("Metadata:")
                    log.metadata.forEach { (k, v) ->
                        appendLine("  $k = $v")
                    }
                }

                if (!log.stackTrace.isNullOrBlank()) {
                    appendLine("Stacktrace:")
                    appendLine(log.stackTrace)
                }
            }
        }
    }
}