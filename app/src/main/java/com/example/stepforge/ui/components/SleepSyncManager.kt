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
            // grantedPermissions her zaman en güncel durumu Health Connect uygulamasından sorgular
            val granted = client.permissionController.getGrantedPermissions()
            // İhtiyaç duyduğumuz izinlerden hangileri verilmemiş?
            val missing = permissions.filter { it !in granted }
            missing.isNotEmpty()
        } catch (e: Exception) {
            Log.e("SleepSync", "Check error", e)
            true
        }
    }

    // SleepSyncManager.kt içine, hasMissingPermissions fonksiyonunun altına ekle:
    suspend fun getGrantedPermissions(): Set<String> {
        return try {
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            emptySet()
        }
    }


    /**
     * Son 7 günün uyku verisini Health Connect'ten çekip Room'a yazar.
     *
     * ✅ DATE FIX:
     * "date" alanını startTime yerine endTime'ın *local* tarihinden üretiriz.
     * (22:20–04:00 gibi uykular sabah bittiği güne yazılsın)
     */
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
            var wroteSessions = 0
            var wroteStages = 0

            for (record in sessionResponse.records) {
                val startMs = record.startTime.toEpochMilli()
                val endMs = record.endTime.toEpochMilli()

                // ✅ FIX: endTime'ın local tarihine göre kaydet
                val localDate = sdfDate.format(Date(endMs))

                val durationMin = ChronoUnit.MINUTES.between(
                    record.startTime.atZone(ZoneId.systemDefault()),
                    record.endTime.atZone(ZoneId.systemDefault())
                ).toInt().coerceAtLeast(0)

                val quality = (50 + durationMin / 10).coerceIn(0, 100)

                // date için overwrite
                sessionDao.deleteByDate(localDate)

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

                // Stage varsa yazmayı dene (API farklarına dayanıklı)
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