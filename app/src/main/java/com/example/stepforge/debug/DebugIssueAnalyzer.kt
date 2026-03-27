package com.example.stepforge.debug

object DebugIssueAnalyzer {

    data class Analysis(
        val probableCause: String,
        val suggestion: String
    )

    fun analyze(entry: DebugLogEntry): Analysis {
        val trace = entry.stackTrace.orEmpty()
        val type = entry.throwableType.orEmpty()
        val message = entry.message

        return when {
            type.contains("NullPointerException", ignoreCase = true) ||
                    trace.contains("NullPointerException", ignoreCase = true) ->
                Analysis(
                    probableCause = "A nullable object was accessed without a safety check.",
                    suggestion = "Check the referenced object and add null validation before use."
                )

            type.contains("IllegalStateException", ignoreCase = true) ||
                    trace.contains("IllegalStateException", ignoreCase = true) ->
                Analysis(
                    probableCause = "The app entered an invalid runtime state for the requested operation.",
                    suggestion = "Inspect the surrounding state transitions and guard the operation with preconditions."
                )

            type.contains("SecurityException", ignoreCase = true) ||
                    trace.contains("SecurityException", ignoreCase = true) ->
                Analysis(
                    probableCause = "A permission, package identity, or restricted API issue occurred.",
                    suggestion = "Verify manifest permissions, runtime permissions, and external service configuration."
                )

            type.contains("SQLite", ignoreCase = true) ||
                    trace.contains("Room", ignoreCase = true) ->
                Analysis(
                    probableCause = "A local database query, migration, or entity mismatch may have failed.",
                    suggestion = "Check DAO queries, migration versioning, and entity-field compatibility."
                )

            trace.contains("Firebase", ignoreCase = true) ||
                    message.contains("Firebase", ignoreCase = true) ->
                Analysis(
                    probableCause = "A Firebase API request or authentication flow may have failed.",
                    suggestion = "Verify Firebase configuration, connectivity, auth state, and request payload."
                )

            else ->
                Analysis(
                    probableCause = "No high-confidence automatic diagnosis is available.",
                    suggestion = "Inspect the full stack trace, related logs, and runtime context."
                )
        }
    }
}