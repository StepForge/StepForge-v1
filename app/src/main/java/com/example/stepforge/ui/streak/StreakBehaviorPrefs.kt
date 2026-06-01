package com.example.stepforge.ui.streak

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object StreakBehaviorPrefs {

    // Shared with StepCounterService sleep heuristics (same DataStore keys)
    val RUNTIME_SLEEP_MODE = booleanPreferencesKey("runtime_sleep_mode")
    val AUTO_SLEEP_ACTIVE = booleanPreferencesKey("auto_sleep_active")
    val SLEEP_LAST_SCREEN_OFF_MS = longPreferencesKey("sleep_last_screen_off_ms")

    val STREAK_BEHAVIOR_STATE = stringPreferencesKey("streak_behavior_state")
    val STREAK_INTERNAL_HEALTH = intPreferencesKey("streak_internal_health")

    val STREAK_LAST_BEHAVIOR_TICK_MS = longPreferencesKey("streak_last_behavior_tick_ms")
    val STREAK_RESCUED_UNTIL_MS = longPreferencesKey("streak_rescued_until_ms")
    val STREAK_CRITICAL_GRACE_UNTIL_MS = longPreferencesKey("streak_critical_grace_until_ms")

    val STREAK_LOST_SNAPSHOT_DAYS = intPreferencesKey("streak_lost_snapshot_days")
    val STREAK_LOST_AT_MS = longPreferencesKey("streak_lost_at_ms")
    val STREAK_RECOVERY_UNTIL_MS = longPreferencesKey("streak_recovery_until_ms")

    val STREAK_RESCUE_DIALOG_PENDING = booleanPreferencesKey("streak_rescue_dialog_pending")
    val STREAK_LOST_DIALOG_PENDING = booleanPreferencesKey("streak_lost_dialog_pending")

    val STREAK_PROTECTED_DATES = stringPreferencesKey("streak_protected_dates")

    /** Restored streak count shown until DB-backed streak catches up. */
    val STREAK_DISPLAY_OVERRIDE_DAYS = intPreferencesKey("streak_display_override_days")
}
