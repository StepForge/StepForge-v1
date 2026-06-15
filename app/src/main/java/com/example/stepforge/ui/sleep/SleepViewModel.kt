package com.example.stepforge.ui.sleep

import android.app.Application
import android.content.Context
import android.widget.Toast
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepforge.R
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.SleepMainSessionDeduper
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.SleepStage
import com.example.stepforge.ui.components.SleepSyncManager
import com.example.stepforge.ui.sleep.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

data class HealthConnectUiState(
    val sdkStatus: Int = HealthConnectClient.SDK_UNAVAILABLE,
    val hasAllPermissions: Boolean = false,
    val lastSyncMs: Long? = null,
    val isSyncing: Boolean = false
)

class SleepViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val sessionDao = db.sleepSessionDao()
    private val stageDao = db.sleepStageDao()

    private val sleepSyncManager = SleepSyncManager(application)
    private val hcPrefs =
        application.getSharedPreferences("sleep_health_connect_prefs", Context.MODE_PRIVATE)

    private val _history = MutableStateFlow<List<SleepDay>>(emptyList())
    val history: StateFlow<List<SleepDay>> = _history.asStateFlow()

    private val _healthConnectUi = MutableStateFlow(HealthConnectUiState())
    val healthConnectUiState: StateFlow<HealthConnectUiState> = _healthConnectUi.asStateFlow()

    init {
        refresh()
        refreshHealthConnectUi()
    }

    fun getHealthConnectPermissionStrings(): Set<String> = sleepSyncManager.getPermissionStrings()

    fun refreshHealthConnectUi() {
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            val status = HealthConnectClient.getSdkStatus(app)
            val hasAll = when (status) {
                HealthConnectClient.SDK_AVAILABLE -> !sleepSyncManager.hasMissingPermissions()
                else -> false
            }
            val last = hcPrefs.getLong(PREF_HC_LAST_SYNC_MS, 0L).takeIf { it > 0L }
            _healthConnectUi.value = _healthConnectUi.value.copy(
                sdkStatus = status,
                hasAllPermissions = hasAll,
                lastSyncMs = last
            )
        }
    }

    fun onHealthConnectPermissionResult(granted: Set<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val required = sleepSyncManager.getPermissionStrings()
            val ok = granted.containsAll(required)
            if (ok) {
                sleepSyncManager.syncLast7Days()
                hcPrefs.edit().putLong(PREF_HC_LAST_SYNC_MS, System.currentTimeMillis()).apply()
                refresh()
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.sleep_sync_successful),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(R.string.sleep_permissions_missing),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            refreshHealthConnectUi()
        }
    }

    fun syncHealthConnect() {
        if (_healthConnectUi.value.isSyncing) return
        viewModelScope.launch(Dispatchers.IO) {
            val app = getApplication<Application>()
            if (HealthConnectClient.getSdkStatus(app) != HealthConnectClient.SDK_AVAILABLE) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        app,
                        app.getString(R.string.sleep_hc_not_supported),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }
            if (sleepSyncManager.hasMissingPermissions()) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(
                        app,
                        app.getString(R.string.sleep_permissions_missing),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return@launch
            }
            _healthConnectUi.update { it.copy(isSyncing = true) }
            try {
                sleepSyncManager.syncLast7Days()
                hcPrefs.edit().putLong(PREF_HC_LAST_SYNC_MS, System.currentTimeMillis()).apply()
                refresh()
            } finally {
                _healthConnectUi.update { it.copy(isSyncing = false) }
                refreshHealthConnectUi()
            }
        }
    }

    fun refresh() {
        viewModelScope.launch(Dispatchers.IO) {
            SleepMainSessionDeduper.deduplicate(sessionDao, stageDao)
            val sessions = sessionDao.getRecentSessions(100)
            val sessionGrouped = sessions.groupBy { it.date }

            val today = LocalDate.now()
            val fullHistory = (29 downTo 0).map { i ->
                val date = today.minusDays(i.toLong())
                val sessionsForDate = sessionGrouped[date.toString()] ?: emptyList()

                if (sessionsForDate.isNotEmpty()) {
                    val sessionInfos = sessionsForDate.map { session ->
                        val stages = stageDao.getStagesForSession(session.id)
                        mapToSessionInfo(session, stages)
                    }

                    SleepDay(
                        date = date,
                        sessions = sessionInfos,
                        availability = if (sessionInfos.any { !it.stages.isNullOrEmpty() })
                            DataAvailability.FULL
                        else
                            DataAvailability.LIMITED,
                        mode = if (sessionInfos.any { it.source == "health_connect" })
                            TrackingMode.SENSOR
                        else
                            TrackingMode.MANUAL
                    )
                } else {
                    SleepDay(date = date, availability = DataAvailability.NONE)
                }
            }

            _history.value = fullHistory
        }
    }

    private fun mapToSessionInfo(session: SleepSession, stages: List<SleepStage>): SleepSessionInfo {
        val bedTime = Instant.ofEpochMilli(session.startTime).atZone(ZoneId.systemDefault()).toLocalTime()
        val wakeTime = Instant.ofEpochMilli(session.endTime).atZone(ZoneId.systemDefault()).toLocalTime()
        val type = if (session.type == "nap") SleepSessionType.NAP else SleepSessionType.MAIN

        val stageData = if (stages.isNotEmpty()) {
            stages
                .groupBy { it.stageType.lowercase() }
                .mapNotNull { (rawType, groupedStages) ->
                    val type = when (rawType) {
                        "deep" -> StageType.DEEP
                        "rem" -> StageType.REM
                        "awake" -> StageType.AWAKE
                        "light" -> StageType.LIGHT
                        else -> null
                    } ?: return@mapNotNull null

                    val durationMinutes = groupedStages.sumOf { s ->
                        ((s.endTime - s.startTime) / 60000).toInt().coerceAtLeast(0)
                    }

                    SleepStageData(
                        type = type,
                        durationMinutes = durationMinutes
                    )
                }
        } else {
            null
        }

        return SleepSessionInfo(
            id = session.id,
            type = type,
            startTime = bedTime,
            endTime = wakeTime,
            totalMinutes = session.totalMinutes,
            qualityScore = session.qualityScore,
            source = session.source,
            notes = session.notes,
            stages = stageData
        )
    }

    fun getWeeklyTrend(historyList: List<SleepDay>): List<SleepDay> {
        val today = LocalDate.now()
        val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekDates = (0..6).map { monday.plusDays(it.toLong()) }

        val historyMap = historyList.associateBy { it.date }
        return weekDates.map { date ->
            historyMap[date] ?: SleepDay(date = date, availability = DataAvailability.NONE)
        }
    }

    fun saveManualEntry(entry: ManualSleepEntry) {
        viewModelScope.launch(Dispatchers.IO) {
            val date = entry.date

            val existing = sessionDao.getSessionsForDate(date.toString())

            if (entry.type == SleepSessionType.MAIN) {
                // 🔥 Manual override:
                // Aynı gün için tüm main sleep'leri sil (HC + manual dahil)
                existing
                    .filter { it.type == "main" }
                    .forEach { sessionDao.deleteById(it.id) }
            }

            val start = entry.bedTime.atDate(date).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            var end = entry.wakeTime.atDate(date).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            if (end < start) end = entry.wakeTime.atDate(date.plusDays(1)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

            val durationHours = ((end - start) / 60000f) / 60f

            val durationScore = when {
                durationHours >= 7.5f -> 90
                durationHours >= 6.5f -> 75
                durationHours >= 5.5f -> 60
                else -> 45
            }

            // kullanıcı slider etkisi ama dominant değil
            val qualityBoost = (entry.qualityRating - 3) * 5 // -10 to +10

            val finalScore = (durationScore + qualityBoost).coerceIn(40, 100)

            val session = SleepSession(
                date = date.toString(),
                startTime = start,
                endTime = end,
                totalMinutes = ((end - start) / 60000).toInt(),
                qualityScore = finalScore,
                source = "manual",
                type = if (entry.type == SleepSessionType.NAP) "nap" else "main",
                notes = entry.notes
            )
            val insertedId = sessionDao.insert(session)

            sleepSyncManager.writeSleepSession(
                startMillis = start,
                endMillis = end,
                title = if (entry.type == SleepSessionType.NAP) {
                    getApplication<Application>().getString(R.string.sleep_hc_session_nap)
                } else {
                    getApplication<Application>().getString(R.string.sleep_hc_session_main)
                }
            )

            refresh()
        }
    }

    fun deleteSleepSession(sessionId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            stageDao.deleteBySessionId(sessionId)
            sessionDao.deleteById(sessionId)
            refresh()
        }
    }

    fun deleteAllSessionsForDate(date: LocalDate) {
        viewModelScope.launch(Dispatchers.IO) {
            val key = date.toString()
            stageDao.deleteByDate(key)
            sessionDao.deleteByDate(key)
            refresh()
        }
    }

    companion object {
        private const val PREF_HC_LAST_SYNC_MS = "health_connect_sleep_last_sync_ms"
    }
}
