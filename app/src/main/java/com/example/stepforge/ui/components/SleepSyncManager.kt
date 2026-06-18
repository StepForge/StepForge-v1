package com.example.stepforge.ui.components

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.stepforge.R
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.SleepStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale
import kotlin.math.abs

class SleepSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "SleepSyncManager"
        private const val FIVE_MINUTES_MS = 5 * 60 * 1000L
    }

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    private val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getWritePermission(SleepSessionRecord::class)
    )

    fun getPermissionStrings(): Set<String> = permissions

    suspend fun hasMissingPermissions(): Boolean {
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            permissions.any { it !in granted }
        } catch (e: Exception) {
            Log.e(TAG, "Check error", e)
            true
        }
    }

    suspend fun getGrantedPermissions(): Set<String> {
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            emptySet()
        }
    }

    suspend fun syncLast7Days(): Boolean = withContext(Dispatchers.IO) {
        try {
            val granted = client.permissionController.getGrantedPermissions()
            val missing = permissions - granted
            if (missing.isNotEmpty()) {
                Log.w(TAG, "Missing Sleep permissions: $missing")
                return@withContext false
            }

            val now = ZonedDateTime.now()
            val start = now.minusDays(7).truncatedTo(ChronoUnit.DAYS)
            val end = now

            val sessionResponse = client.readRecords(
                ReadRecordsRequest(
                    recordType = SleepSessionRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start.toInstant(), end.toInstant())
                )
            )

            val db = AppDatabase.getDatabase(context)
            val sessionDao = db.sleepSessionDao()
            val stageDao = db.sleepStageDao()

            val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val fromDate = sdfDate.format(Date(start.toInstant().toEpochMilli()))
            val toDate = sdfDate.format(Date(end.toInstant().toEpochMilli()))

            // Keep manual records, refresh only Health Connect source rows
            stageDao.deleteBySourceAndDateRange("health_connect", fromDate, toDate)
            sessionDao.deleteBySourceAndDateRange("health_connect", fromDate, toDate)

            val localSessions = sessionDao.getRecentSessions(1000)
                .filter { it.source != "health_connect" }

            var wroteSessions = 0
            var skippedStepForgeEchoes = 0
            var wroteStages = 0

            for (record in sessionResponse.records) {
                val startMs = record.startTime.toEpochMilli()
                val endMs = record.endTime.toEpochMilli()
                if (endMs <= startMs) continue

                val localDate = sdfDate.format(Date(endMs))

                val durationMin = ChronoUnit.MINUTES.between(
                    record.startTime.atZone(ZoneId.systemDefault()),
                    record.endTime.atZone(ZoneId.systemDefault())
                ).toInt().coerceAtLeast(0)

                val title = record.title.orEmpty()
                val notes = record.notes.orEmpty()
                val type = inferSessionType(title = title, notes = notes, durationMin = durationMin)

                // Prevent Health Connect echo-back: when StepForge writes a manual Nap/Main Sleep
                // into Health Connect, the next sync can read the same record back. Without this
                // guard, a manual Nap can be imported again as a Main Sleep and corrupt the day.
                if (isStepForgeEchoRecord(title, notes) &&
                    hasOverlappingLocalSession(localSessions, startMs, endMs)
                ) {
                    skippedStepForgeEchoes++
                    continue
                }

                val quality = (50 + durationMin / 10).coerceIn(0, 100)

                val sessionId = sessionDao.insert(
                    SleepSession(
                        date = localDate,
                        startTime = startMs,
                        endTime = endMs,
                        totalMinutes = durationMin,
                        qualityScore = quality,
                        source = "health_connect",
                        type = type
                    )
                )
                wroteSessions++

                try {
                    val mappedStages = extractMappedStages(record.stages)
                    for (ms in mappedStages) {
                        stageDao.insert(
                            SleepStage(
                                sessionId = sessionId,
                                stageType = ms.type,          // ✅ deep/rem/light/awake
                                startTime = ms.startMs,
                                endTime = ms.endMs
                            )
                        )
                        wroteStages++
                    }
                } catch (_: Exception) {
                }
            }

            Log.d(
                TAG,
                "Sleep sync wrote sessions=$wroteSessions stages=$wroteStages skippedEchoes=$skippedStepForgeEchoes"
            )
            wroteSessions > 0
        } catch (e: Exception) {
            Log.e(TAG, "syncLast7Days error", e)
            false
        }
    }


    private data class MappedStage(
        val type: String,   // deep/rem/light/awake
        val startMs: Long,
        val endMs: Long
    )

    private fun isStepForgeEchoRecord(title: String, notes: String): Boolean {
        val raw = "$title $notes".lowercase(Locale.getDefault())
        return raw.contains("stepforge")
    }

    private fun inferSessionType(title: String, notes: String, durationMin: Int): String {
        val raw = "$title $notes".lowercase(Locale.getDefault())
        val isNap = raw.contains("nap") ||
                raw.contains("şekerleme") ||
                raw.contains("sekerleme") ||
                raw.contains("nickerchen") ||
                raw.contains("kurzschlaf")

        if (isNap) return "nap"

        // Health Connect does not provide a reliable native Nap/Main flag.
        // For StepForge-authored echo records without a localized title, short sleep is safer as Nap.
        if (raw.contains("stepforge") && durationMin in 1..240) return "nap"

        return "main"
    }

    private fun hasOverlappingLocalSession(
        localSessions: List<SleepSession>,
        startMs: Long,
        endMs: Long
    ): Boolean {
        val durationMs = (endMs - startMs).coerceAtLeast(1L)
        return localSessions.any { local ->
            val startsClose = abs(local.startTime - startMs) <= FIVE_MINUTES_MS
            val endsClose = abs(local.endTime - endMs) <= FIVE_MINUTES_MS
            val overlapMs = (minOf(local.endTime, endMs) - maxOf(local.startTime, startMs))
                .coerceAtLeast(0L)
            val shorterMs = minOf((local.endTime - local.startTime).coerceAtLeast(1L), durationMs)

            (startsClose && endsClose) || overlapMs >= (shorterMs * 8L / 10L)
        }
    }

    private fun mapHealthConnectStageToInternal(stageAny: Any?): String? {
        val raw = stageAny?.toString()?.uppercase(Locale.US) ?: return null

        return when {
            raw.contains("DEEP") -> "deep"
            raw.contains("REM") -> "rem"
            raw.contains("LIGHT") -> "light"
            raw.contains("AWAKE") -> "awake"

            // ignore:
            raw.contains("UNKNOWN") -> null
            raw.contains("OUT_OF_BED") -> null
            raw.contains("UNSPECIFIED") -> null
            else -> null
        }
    }

    private fun extractMappedStages(
        recordStages: List<androidx.health.connect.client.records.SleepSessionRecord.Stage>?
    ): List<MappedStage> {
        if (recordStages.isNullOrEmpty()) return emptyList()

        return recordStages.mapNotNull { st ->
            val mapped = mapHealthConnectStageToInternal(st.stage) ?: return@mapNotNull null
            val startMs = st.startTime.toEpochMilli()
            val endMs = st.endTime.toEpochMilli()
            if (endMs <= startMs) return@mapNotNull null

            MappedStage(
                type = mapped,
                startMs = startMs,
                endMs = endMs
            )
        }.sortedBy { it.startMs }
    }

    suspend fun writeSleepSession(
        startMillis: Long,
        endMillis: Long,
        title: String = context.getString(R.string.hc_stepforge_sleep)
    ): Boolean = withContext(Dispatchers.IO) {

        try {

            val start = Instant.ofEpochMilli(startMillis)
            val end = Instant.ofEpochMilli(endMillis)

            val record = SleepSessionRecord(
                startTime = start,
                endTime = end,
                startZoneOffset = ZoneId.systemDefault().rules.getOffset(start),
                endZoneOffset = ZoneId.systemDefault().rules.getOffset(end),
                title = title,
                notes = context.getString(R.string.hc_synced_from_stepforge)
            )

            client.insertRecords(listOf(record))

            Log.d(TAG, "Sleep session synced successfully")
            true

        } catch (e: Exception) {

            Log.e(TAG, "Failed to sync sleep session", e)
            false
        }
    }
}
