package com.example.stepforge.debug

import android.content.Context
import com.example.stepforge.R

object DebugIssueAnalyzer {

    data class Analysis(
        val probableCause: String,
        val suggestion: String
    )

    fun analyze(context: Context, entry: DebugLogEntry): Analysis {
        val trace = entry.stackTrace.orEmpty()
        val type = entry.throwableType.orEmpty()
        val message = entry.message

        return when {
            type.contains("NullPointerException", ignoreCase = true) ||
                    trace.contains("NullPointerException", ignoreCase = true) ->
                Analysis(
                    probableCause = context.getString(R.string.debug_cause_null),
                    suggestion = context.getString(R.string.debug_suggestion_null)
                )

            type.contains("IllegalStateException", ignoreCase = true) ||
                    trace.contains("IllegalStateException", ignoreCase = true) ->
                Analysis(
                    probableCause = context.getString(R.string.debug_cause_state),
                    suggestion = context.getString(R.string.debug_suggestion_state)
                )

            type.contains("SecurityException", ignoreCase = true) ||
                    trace.contains("SecurityException", ignoreCase = true) ->
                Analysis(
                    probableCause = context.getString(R.string.debug_cause_security),
                    suggestion = context.getString(R.string.debug_suggestion_security)
                )

            type.contains("SQLite", ignoreCase = true) ||
                    trace.contains("Room", ignoreCase = true) ->
                Analysis(
                    probableCause = context.getString(R.string.debug_cause_database),
                    suggestion = context.getString(R.string.debug_suggestion_database)
                )

            trace.contains("Firebase", ignoreCase = true) ||
                    message.contains("Firebase", ignoreCase = true) ->
                Analysis(
                    probableCause = context.getString(R.string.debug_cause_firebase),
                    suggestion = context.getString(R.string.debug_suggestion_firebase)
                )

            else ->
                Analysis(
                    probableCause = context.getString(R.string.debug_cause_unknown),
                    suggestion = context.getString(R.string.debug_suggestion_unknown)
                )
        }
    }
}
