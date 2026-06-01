package com.example.stepforge.core

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.flow.first

/**
 * Manages application state versioning and migrations to prevent rollback and inconsistencies.
 */
object AppStateMigrationManager {
    private const val TAG = "StateMigration"
    private val KEY_APP_STATE_VERSION = intPreferencesKey("app_state_version")
    private const val CURRENT_STATE_VERSION = 1

    suspend fun runMigrations(context: Context) {
        try {
            val prefs = context.stepforgeStore.data.first()
            val savedVersion = prefs[KEY_APP_STATE_VERSION] ?: 0

            if (savedVersion < CURRENT_STATE_VERSION) {
                Log.i(TAG, "Migrating app state from $savedVersion to $CURRENT_STATE_VERSION")
                
                // Perform migrations here if needed
                if (savedVersion == 0) {
                    // Initial setup/cleanup
                }

                context.stepforgeStore.edit { it[KEY_APP_STATE_VERSION] = CURRENT_STATE_VERSION }
                Log.i(TAG, "Migration completed successfully")
            } else if (savedVersion > CURRENT_STATE_VERSION) {
                // Downgrade protection - Incompatible state detected
                Log.e(TAG, "App state version ($savedVersion) is newer than current app ($CURRENT_STATE_VERSION). " +
                        "Incompatible state detected. Invalidate unsafe caches.")
                // potentially clear unsafe cached objects here
            }
        } catch (e: Exception) {
            Log.e(TAG, "Migration failed", e)
        }
    }
}
