package com.example.stepforge.debug

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

object DebugLogStore {

    private const val MAX_IN_MEMORY_LOGS = 1500

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val idGen = AtomicLong(System.currentTimeMillis())

    private lateinit var fileStore: DebugLogFileStore

    private val _logs = MutableStateFlow<List<DebugLogEntry>>(emptyList())
    val logs: StateFlow<List<DebugLogEntry>> = _logs

    @Volatile
    private var initialized = false

    @Volatile
    var loggingEnabled: Boolean = true
        private set

    fun init(context: Context) {
        if (initialized) return
        fileStore = DebugLogFileStore(context.applicationContext)
        _logs.value = fileStore.load()
        initialized = true
    }

    fun setLoggingEnabled(enabled: Boolean) {
        loggingEnabled = enabled
    }

    fun nextId(): Long = idGen.incrementAndGet()

    fun append(entry: DebugLogEntry) {
        if (!initialized || !loggingEnabled) return

        _logs.update { current ->
            val updated = (current + entry).takeLast(MAX_IN_MEMORY_LOGS)
            scope.launch { fileStore.save(updated) }
            updated
        }
    }

    fun clear() {
        _logs.value = emptyList()
        if (initialized) {
            scope.launch { fileStore.clear() }
        }
    }

    fun exportFile() = if (initialized) fileStore.exportFile() else null
}