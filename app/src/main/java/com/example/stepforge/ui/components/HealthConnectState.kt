package com.example.stepforge.ui.components

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object HealthConnectState {
    data class Status(
        val isInstalled: Boolean = false,
        val isConnected: Boolean = false,
        val permissionsGranted: Boolean = false,
        val lastSyncTime: Long = 0L,
        val isSyncing: Boolean = false
    )

    private val _status = MutableStateFlow(Status())
    val status = _status.asStateFlow()

    fun update(newStatus: Status) {
        _status.value = newStatus
    }
}
