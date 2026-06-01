package com.example.stepforge.ui.components

import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy.Companion.calories
import androidx.health.connect.client.units.Length
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class HealthSyncManager(private val context: Context) {

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    // HEALTH CONNECT İZİN KÜMESİ – DOĞRUDAN HealthPermission nesneleri
    private val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),

        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getWritePermission(DistanceRecord::class),

        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getWritePermission(TotalCaloriesBurnedRecord::class),

        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),

        HealthPermission.getReadPermission(HeartRateRecord::class)
    )

    /**
     * Activity'den izin istemek için izin listesini dışarıya açıyoruz.
     * SettingsActivity ve MainActivity tarafından kullanılır.
     */
    fun getPermissionStrings(): Set<String> = permissions

    /**
     * Kullanıcıdan eksik izinleri kontrol eder.
     * Settings ekranında bağlantı durumunu göstermek için kullanılır.
     */
    suspend fun hasMissingPermissions(): Boolean {
        return try {
            // YERİNE GELECEK KOD:
            val granted = client.permissionController.getGrantedPermissions()

            val missing: Set<String> = permissions - granted
            missing.isNotEmpty()
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Arka planda izinleri kontrol eder ve eksik varsa Log basar.
     * Uygulama açılışında kontrol amaçlı kullanılabilir.
     */
    fun checkAndRequestPermissions() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                // YERİNE GELECEK KOD:
                val granted = client.permissionController.getGrantedPermissions()

                val missing: Set<String> = permissions - granted

                if (missing.isNotEmpty()) {
                    Log.d("HealthSyncManager", "Missing permissions: $missing")
                } else {
                    Log.d("HealthSyncManager", "All permissions granted ✅")
                }
            } catch (e: Exception) {
                Log.e("HealthSync", "Permission check error", e)
            }
        }
    }

    // 1. Verilen izinlerin listesini gerçek zamanlı çeken fonksiyon
    suspend fun getGrantedPermissions(): Set<String> {
        return try {
            // Değiştirildi: healthConnectClient -> client
            client.permissionController.getGrantedPermissions()
        } catch (e: Exception) {
            emptySet()
        }
    }

    // HealthSyncManager.kt içindeki hasEssentialPermissions fonksiyonunu bununla değiştir:
    suspend fun hasEssentialPermissions(): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                // ✅ S10 gibi cihazlarda izin durumunu en taze haliyle almak için
                // permissionController'ı doğrudan ve IO thread üzerinde sorguluyoruz.
                val granted = client.permissionController.getGrantedPermissions()

                // Adımları okuma izni (Kritik olan bu)
                val stepRead = HealthPermission.getReadPermission(
                    StepsRecord::class
                )

                // ✅ Eğer OKUMA izni listede varsa TRUE döner
                granted.contains(stepRead)
            } catch (e: Exception) {
                Log.e("HealthSync", "Permission check failed on older Android", e)
                false
            }
        }
    }





    /**
     * Tüm izinlerin verilip verilmediğini kontrol eder.
     */
    suspend fun isFullyConnected(): Boolean {
        return try {
            // YERİNE GELECEK KOD:
            val granted = client.permissionController.getGrantedPermissions()

            granted.containsAll(permissions)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Hangi izinlerin eksik olduğunu döndürür.
     * DÜZELTME: Dönüş tipi Set<HealthPermission> değil, Set<String> olmalı.
     */
    suspend fun getMissingPermissions(): Set<String> {
        return try {
            val granted = client.permissionController.getGrantedPermissions()
            permissions - granted
        } catch (_: Exception) {
            emptySet()
        }
    }

    /**
     * Bugünkü adım toplamını okur ve döndürür.
     */
    suspend fun readHealthConnectSteps(): Long {
        return try {
            // YERİNE GELECEK KOD:
            val granted = client.permissionController.getGrantedPermissions()

            if (!granted.containsAll(permissions)) {
                Log.w("HealthSync", "Cannot read steps: Missing permissions")
                return 0L
            }

            val today = LocalDate.now()
            val start = today.atStartOfDay().toInstant(ZoneOffset.UTC)
            val end = start.plus(1, ChronoUnit.DAYS)

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )
            response.records.fold(0L) { acc, rec -> acc + rec.count }
        } catch (e: Exception) {
            Log.e("HealthSync", "Error reading steps", e)
            0L
        }
    }

    /**
     * Senkronizasyon testi yapar (Sadece Log basar).
     */
    fun syncStepsData() {
        CoroutineScope(Dispatchers.IO).launch {
            val steps = readHealthConnectSteps()
            Log.d("HealthSync", "Background Sync Test: Today's steps: $steps")
        }
    }

    /**
     * Bugünkü adımları okuyan helper (MainActivity kullanıyor).
     */
    suspend fun readSteps(): Long = withContext(Dispatchers.IO) {
        try {
            val today = LocalDate.now()
            val client = HealthConnectClient.getOrCreate(context)

            val start = today.atStartOfDay(
                ZoneOffset.systemDefault().rules.getOffset(Instant.now())
            ).toInstant()
            val end = start.plus(1, ChronoUnit.DAYS)

            val response = client.readRecords(
                ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(start, end)
                )
            )

            response.records.fold(0L) { acc, rec -> acc + rec.count }
        } catch (e: Exception) {
            0L
        }
    }

    suspend fun writeStepsToHealthConnect(
        steps: Long,
        start: Instant,
        end: Instant
    ): Boolean {

        return try {

            val record = StepsRecord(
                count = steps,
                startTime = start,
                endTime = end,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(start),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(end)
            )

            client.insertRecords(listOf(record))

            Log.d("HealthSync", "Steps synced successfully")
            true

        } catch (e: Exception) {

            Log.e("HealthSync", "Failed to sync steps", e)
            false
        }
    }

    suspend fun writeDistanceToHealthConnect(
        meters: Double,
        start: Instant,
        end: Instant
    ): Boolean {

        return try {

            val record = DistanceRecord(
                distance = Length.meters(meters),
                startTime = start,
                endTime = end,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(start),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(end)
            )

            client.insertRecords(listOf(record))

            Log.d("HealthSync", "Distance synced")
            true

        } catch (e: Exception) {

            Log.e("HealthSync", "Distance sync failed", e)
            false
        }
    }

    suspend fun writeCaloriesToHealthConnect(
        caloriesValue: Double,
        start: Instant,
        end: Instant
    ): Boolean {

        return try {

            val record = TotalCaloriesBurnedRecord(
                energy = calories(caloriesValue),
                startTime = start,
                endTime = end,
                startZoneOffset = ZoneOffset.systemDefault().rules.getOffset(start),
                endZoneOffset = ZoneOffset.systemDefault().rules.getOffset(end)
            )

            client.insertRecords(listOf(record))

            Log.d("HealthSync", "Calories synced")
            true

        } catch (e: Exception) {

            Log.e("HealthSync", "Calories sync failed", e)
            false
        }
    }
}