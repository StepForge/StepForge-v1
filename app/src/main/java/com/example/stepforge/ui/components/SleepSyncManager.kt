package com.example.stepforge.ui.components

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.SleepStage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

class SleepSyncManager(private val context: Context) {

    companion object {
        private const val TAG = "SleepSyncManager"
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

            var wroteSessions = 0
            var wroteStages = 0

            for (record in sessionResponse.records) {
                val startMs = record.startTime.toEpochMilli()
                val endMs = record.endTime.toEpochMilli()
                val localDate = sdfDate.format(Date(endMs))

                val durationMin = ChronoUnit.MINUTES.between(
                    record.startTime.atZone(ZoneId.systemDefault()),
                    record.endTime.atZone(ZoneId.systemDefault())
                ).toInt().coerceAtLeast(0)

                val quality = (50 + durationMin / 10).coerceIn(0, 100)

                val sessionId = sessionDao.insert(
                    SleepSession(
                        date = localDate,
                        startTime = startMs,
                        endTime = endMs,
                        totalMinutes = durationMin,
                        qualityScore = quality,
                        source = "health_connect"
                    )
                )
                wroteSessions++

                try {
                    val stages = record.stages
                    for (st in stages) {
                        val stageTypeStr = try {
                            st.stage.toString().lowercase(Locale.getDefault())
                        } catch (_: Exception) {
                            "unknown"
                        }

                        stageDao.insert(
                            SleepStage(
                                sessionId = sessionId,
                                stageType = stageTypeStr,
                                startTime = st.startTime.toEpochMilli(),
                                endTime = st.endTime.toEpochMilli()
                            )
                        )
                        wroteStages++
                    }
                } catch (_: Exception) {
                }
            }

            Log.d(TAG, "Sleep sync wrote sessions=$wroteSessions stages=$wroteStages")
            wroteSessions > 0
        } catch (e: Exception) {
            Log.e(TAG, "syncLast7Days error", e)
            false
        }
    }
}
