package com.example.stepforge.ui.components

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.steps.CentralStepState
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Reconciles steps from Health Connect with local steps to prevent duplication.
 */
class HealthConnectImportCoordinator(private val context: Context) {

    private val healthManager = HealthSyncManager(context)
    private val dao = AppDatabase.getDatabase(context).dailyStepsDao()
    private val KEY_LAST_HC_IMPORT = longPreferencesKey("last_hc_import_time")

    /**
     * Imports steps from Health Connect and ensures they are merged correctly.
     * Rule: If HC steps > local steps, we take HC steps as the new truth.
     * We NEVER blindly add HC steps to local steps.
     */
    suspend fun syncSteps() {
        try {
            if (!healthManager.hasEssentialPermissions()) return

            val hcSteps = healthManager.readSteps().toInt()
            val today = CentralStepState.todayKey()
            
            val localSteps = dao.getStepsForDate(today)?.steps ?: 0
            
            // Reconcile: If HC has more steps, it's the more reliable source (likely multiple devices).
            // If local has more, we keep local (HC might be lagging or local is fresher).
            if (hcSteps > localSteps) {
                Log.i("HCImport", "HC steps ($hcSteps) > Local steps ($localSteps). Updating to HC.")
                
                // Update DB with source tagging
                dao.insertDailySteps(DailySteps(today, hcSteps, source = "health_connect"))
                
                // Update DataStore for service persistence
                context.stepforgeStore.edit { prefs ->
                    val PREF_TOTAL = androidx.datastore.preferences.core.intPreferencesKey("pref_total_all")
                    prefs[PREF_TOTAL] = hcSteps
                }

                // Notify Central State
                CentralStepState.emit(hcSteps, date = today)

                // Notify Service to reload its memory and offset
                notifyServiceReload()

            } else {
                Log.d("HCImport", "Local steps ($localSteps) >= HC steps ($hcSteps). No update needed.")
            }

            context.stepforgeStore.edit { it[KEY_LAST_HC_IMPORT] = System.currentTimeMillis() }

        } catch (e: Exception) {
            Log.e("HCImport", "Sync failed", e)
        }
    }

    private fun notifyServiceReload() {
        try {
            val intent = android.content.Intent(context, com.example.stepforge.StepCounterService::class.java).apply {
                action = com.example.stepforge.StepCounterService.ACTION_RELOAD
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("HCImport", "Failed to notify service reload", e)
        }
    }
}
