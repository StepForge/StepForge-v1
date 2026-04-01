package com.example.stepforge.ui.streak

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

object StreakShieldPrefs {

    // Today active shield
    val SHIELD_TODAY_MINUTES_LEFT = intPreferencesKey("shield_today_minutes_left")
    val SHIELD_TODAY_MAX_MINUTES = intPreferencesKey("shield_today_max_minutes")

    // Shield generation source
    val SHIELD_GENERATED_FOR_DATE = stringPreferencesKey("shield_generated_for_date")
    val SHIELD_LAST_DECAY_AT_MS = longPreferencesKey("shield_last_decay_at_ms")

    // Tomorrow earned shield breakdown
    val SHIELD_TOMORROW_BASE_HOURS = intPreferencesKey("shield_tomorrow_base_hours")
    val SHIELD_TOMORROW_GOAL_BONUS_HOURS = intPreferencesKey("shield_tomorrow_goal_bonus_hours")
    val SHIELD_TOMORROW_FINAL_HOURS = intPreferencesKey("shield_tomorrow_final_hours")
    val SHIELD_TOMORROW_MAX_HOURS = intPreferencesKey("shield_tomorrow_max_hours")

    // Premium rescue system
    val PREMIUM_RESCUE_MONTH = stringPreferencesKey("premium_rescue_month")
    val PREMIUM_RESCUE_USED_COUNT = intPreferencesKey("premium_rescue_used_count")
    val PREMIUM_AUTO_RESCUE_ENABLED = booleanPreferencesKey("premium_auto_rescue_enabled")
    val PREMIUM_RESCUE_USED_FOR_DATE = stringPreferencesKey("premium_rescue_used_for_date")

    // Premium coach notifications
    val PREMIUM_AI_COACH_ENABLED = booleanPreferencesKey("premium_ai_coach_enabled")
    val PREMIUM_AI_LAST_NOTIFY_AT_MS = longPreferencesKey("premium_ai_last_notify_at_ms")
    val PREMIUM_AI_LAST_NOTIFY_TYPE = stringPreferencesKey("premium_ai_last_notify_type")
}