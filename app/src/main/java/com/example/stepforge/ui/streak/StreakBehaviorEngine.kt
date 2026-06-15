package com.example.stepforge.ui.streak

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.Calendar
import kotlin.math.roundToInt

object StreakBehaviorEngine {

    const val MIN_STEPS_TO_PAUSE_DECAY = 1000
    const val MIN_STEPS_TO_EARN_BUFFER = 2000
    const val PREMIUM_MONTHLY_RESCUES = 5
    const val PREMIUM_RESCUE_PROTECTION_MS = 24L * 60L * 60L * 1000L
    const val CRITICAL_GRACE_MS = 8L * 60L * 60L * 1000L
    const val LOST_RECOVERY_WINDOW_HOURS = 24
    const val LOST_RECOVERY_WINDOW_MS = LOST_RECOVERY_WINDOW_HOURS * 60L * 60L * 1000L

    private const val MAX_INTERNAL_BUFFER_MINUTES = 12 * 60
    private const val PREMIUM_MAX_INTERNAL_BUFFER_MINUTES = 16 * 60
    private const val HEALTH_DECAY_PER_AWAKE_MINUTE = 1
    private const val MAX_PROTECTED_DATES = 120

    data class BehaviorSnapshot(
        val state: StreakBehaviorState,
        val stateMessage: StreakStateMessage,
        val healthPercent: Int,
        val internalBufferMinutes: Int,
        val isPremium: Boolean,
        val premiumRescuesLeft: Int,
        val rescuedUntilMs: Long,
        val lostSnapshotDays: Int,
        val recoveryWindowActive: Boolean,
        val showRescueDialog: Boolean,
        val showLostDialog: Boolean
    )

    fun todayKey(): String = LocalDate.now(ZoneId.systemDefault()).toString()

    private fun yesterdayKey(): String = LocalDate.now(ZoneId.systemDefault())
        .minusDays(1)
        .toString()

    private fun previousDateKey(date: String): String {
        return runCatching { LocalDate.parse(date).minusDays(1).toString() }
            .getOrDefault(yesterdayKey())
    }

    fun currentMonthKey(): String {
        val now = LocalDate.now(ZoneId.systemDefault())
        return "%04d-%02d".format(now.year, now.monthValue)
    }

    fun parseState(raw: String?): StreakBehaviorState {
        return runCatching { StreakBehaviorState.valueOf(raw ?: "") }
            .getOrDefault(StreakBehaviorState.ACTIVE)
    }

    fun stateMessageFor(state: StreakBehaviorState): StreakStateMessage {
        return when (state) {
            StreakBehaviorState.ACTIVE -> StreakStateMessage.SAFE
            StreakBehaviorState.STABLE -> StreakStateMessage.SAFE
            StreakBehaviorState.UNSTABLE -> StreakStateMessage.NEEDS_ATTENTION
            StreakBehaviorState.CRITICAL -> StreakStateMessage.CLOSE_TO_ENDING
            StreakBehaviorState.RESCUED -> StreakStateMessage.RESCUED
            StreakBehaviorState.LOST -> StreakStateMessage.LOST
        }
    }

    fun healthPercentFromBuffer(bufferMinutes: Int, isPremium: Boolean): Int {
        val max = if (isPremium) PREMIUM_MAX_INTERNAL_BUFFER_MINUTES else MAX_INTERNAL_BUFFER_MINUTES
        if (max <= 0) return 0
        return ((bufferMinutes.toFloat() / max.toFloat()) * 100f).roundToInt().coerceIn(0, 100)
    }

    fun readProtectedDates(raw: String?): Set<String> {
        if (raw.isNullOrBlank()) return emptySet()
        return raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.toSet()
    }

    fun writeProtectedDates(dates: Set<String>): String {
        return dates.sortedDescending().take(MAX_PROTECTED_DATES).joinToString(",")
    }

    fun isDayProtected(date: String, protectedDates: Set<String>): Boolean {
        return protectedDates.contains(date)
    }

    fun qualifyDay(
        steps: Int,
        goal: Int,
        internalBufferMinutes: Int,
        rescuedActive: Boolean,
        rescueUsedForDay: Boolean
    ): StreakDayQualifier.DayQualificationResult {
        val safeSteps = steps.coerceAtLeast(0)
        val safeGoal = goal.coerceAtLeast(1000)
        val reachedGoal = safeSteps >= safeGoal
        val protectedByBehavior = !reachedGoal && (internalBufferMinutes > 0 || rescuedActive)
        val protectedByPremiumRescue = !reachedGoal && rescueUsedForDay

        return StreakDayQualifier.DayQualificationResult(
            countsForStreak = reachedGoal || protectedByBehavior || protectedByPremiumRescue,
            reachedGoal = reachedGoal,
            protectedByShield = protectedByBehavior,
            protectedByPremiumRescue = protectedByPremiumRescue
        )
    }

    fun deriveState(
        healthPercent: Int,
        rescuedUntilMs: Long,
        graceUntilMs: Long,
        lostAtMs: Long,
        recoveryUntilMs: Long,
        nowMs: Long
    ): StreakBehaviorState {
        if (lostAtMs > 0L) {
            if (nowMs <= recoveryUntilMs) return StreakBehaviorState.LOST
        }
        if (rescuedUntilMs > nowMs) return StreakBehaviorState.RESCUED
        if (graceUntilMs > nowMs) return StreakBehaviorState.CRITICAL

        return when {
            healthPercent >= 70 -> StreakBehaviorState.ACTIVE
            healthPercent >= 50 -> StreakBehaviorState.STABLE
            healthPercent >= 25 -> StreakBehaviorState.UNSTABLE
            else -> StreakBehaviorState.CRITICAL
        }
    }

    fun isUserAwakeForDecay(
        prefs: Preferences,
        todaySteps: Int,
        lastStepTimeMs: Long,
        nowMs: Long
    ): Boolean {
        val runtimeSleep = prefs[StreakBehaviorPrefs.RUNTIME_SLEEP_MODE] ?: false
        val autoSleep = prefs[StreakBehaviorPrefs.AUTO_SLEEP_ACTIVE] ?: false
        if (runtimeSleep || autoSleep) return false

        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        if (hour in 0..6 && todaySteps < 500) return false

        val screenOffMs = prefs[StreakBehaviorPrefs.SLEEP_LAST_SCREEN_OFF_MS] ?: 0L
        val noRecentSteps = lastStepTimeMs > 0L && (nowMs - lastStepTimeMs) >= 20 * 60_000L
        if (screenOffMs > 0L && (nowMs - screenOffMs) >= 15 * 60_000L && noRecentSteps) {
            return false
        }

        return true
    }

    fun calculateEarnedBufferMinutes(steps: Int, goal: Int, isPremium: Boolean): Int {
        val result = StreakShieldEngine.calculateDailyEarnedShieldHours(
            steps = steps,
            goal = goal,
            isPremium = isPremium
        )
        val cap = if (isPremium) PREMIUM_MAX_INTERNAL_BUFFER_MINUTES else MAX_INTERNAL_BUFFER_MINUTES
        return (result.finalHours * 60).coerceAtMost(cap)
    }

    fun premiumRescuesLeft(prefs: Preferences): Int {
        val currentMonth = currentMonthKey()
        val savedMonth = prefs[StreakShieldPrefs.PREMIUM_RESCUE_MONTH] ?: currentMonth
        val used = if (savedMonth == currentMonth) {
            prefs[StreakShieldPrefs.PREMIUM_RESCUE_USED_COUNT] ?: 0
        } else {
            0
        }
        return (PREMIUM_MONTHLY_RESCUES - used).coerceAtLeast(0)
    }

    suspend fun readSnapshot(context: android.content.Context): BehaviorSnapshot {
        val prefs = context.stepforgeStore.data.first()
        val isPremium = (prefs[androidx.datastore.preferences.core.intPreferencesKey("premium_enabled")] ?: 0) == 1
        val buffer = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
        val health = prefs[StreakBehaviorPrefs.STREAK_INTERNAL_HEALTH]
            ?: healthPercentFromBuffer(buffer, isPremium)
        val rescuedUntil = prefs[StreakBehaviorPrefs.STREAK_RESCUED_UNTIL_MS] ?: 0L
        val graceUntil = prefs[StreakBehaviorPrefs.STREAK_CRITICAL_GRACE_UNTIL_MS] ?: 0L
        val lostAt = prefs[StreakBehaviorPrefs.STREAK_LOST_AT_MS] ?: 0L
        val recoveryUntil = prefs[StreakBehaviorPrefs.STREAK_RECOVERY_UNTIL_MS] ?: 0L
        val now = System.currentTimeMillis()

        val state = deriveState(health, rescuedUntil, graceUntil, lostAt, recoveryUntil, now)
        return BehaviorSnapshot(
            state = state,
            stateMessage = stateMessageFor(state),
            healthPercent = health.coerceIn(0, 100),
            internalBufferMinutes = buffer,
            isPremium = isPremium,
            premiumRescuesLeft = premiumRescuesLeft(prefs),
            rescuedUntilMs = rescuedUntil,
            lostSnapshotDays = prefs[StreakBehaviorPrefs.STREAK_LOST_SNAPSHOT_DAYS] ?: 0,
            recoveryWindowActive = lostAt > 0L && now <= recoveryUntil,
            showRescueDialog = prefs[StreakBehaviorPrefs.STREAK_RESCUE_DIALOG_PENDING] == true,
            showLostDialog = prefs[StreakBehaviorPrefs.STREAK_LOST_DIALOG_PENDING] == true
        )
    }

    suspend fun tickBehavior(
        context: android.content.Context,
        todaySteps: Int,
        goal: Int,
        lastStepTimeMs: Long
    ) {
        val store = context.stepforgeStore
        val prefs = store.data.first()
        val now = System.currentTimeMillis()
        val isPremium = (prefs[androidx.datastore.preferences.core.intPreferencesKey("premium_enabled")] ?: 0) == 1

        var buffer = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
        var lastTick = prefs[StreakBehaviorPrefs.STREAK_LAST_BEHAVIOR_TICK_MS] ?: 0L
        if (lastTick <= 0L || lastTick > now) lastTick = now
        val rescuedUntil = prefs[StreakBehaviorPrefs.STREAK_RESCUED_UNTIL_MS] ?: 0L
        val graceUntil = prefs[StreakBehaviorPrefs.STREAK_CRITICAL_GRACE_UNTIL_MS] ?: 0L
        val lostAt = prefs[StreakBehaviorPrefs.STREAK_LOST_AT_MS] ?: 0L
        val recoveryUntil = prefs[StreakBehaviorPrefs.STREAK_RECOVERY_UNTIL_MS] ?: 0L

        if (lostAt > 0L && now > recoveryUntil) {
            clearLostState(store, now)
            return
        }

        if (rescuedUntil > now) {
            buffer = PREMIUM_MAX_INTERNAL_BUFFER_MINUTES.coerceAtLeast(buffer)
        }

        val earned = calculateEarnedBufferMinutes(todaySteps, goal, isPremium)
        if (earned > buffer) buffer = earned

        val shouldDecay = todaySteps < MIN_STEPS_TO_PAUSE_DECAY
        val awake = isUserAwakeForDecay(prefs, todaySteps, lastStepTimeMs, now)

        if (shouldDecay && buffer > 0 && rescuedUntil <= now) {
            if (awake) {
                val elapsedAwakeMinutes = ((now - lastTick) / 60_000L).toInt().coerceIn(0, 5)
                if (elapsedAwakeMinutes > 0) {
                    buffer = (buffer - elapsedAwakeMinutes * HEALTH_DECAY_PER_AWAKE_MINUTE).coerceAtLeast(0)
                }
            }
        }

        val health = healthPercentFromBuffer(buffer, isPremium)
        var state = deriveState(health, rescuedUntil, graceUntil, lostAt, recoveryUntil, now)

        var rescueDialogPending = prefs[StreakBehaviorPrefs.STREAK_RESCUE_DIALOG_PENDING] == true

        if (state == StreakBehaviorState.CRITICAL && graceUntil <= now && rescuedUntil <= now) {
            if (isPremium && premiumRescuesLeft(prefs) > 0) {
                rescueDialogPending = true
            } else if (buffer <= 0) {
                return
            }
        }

        if (graceUntil > 0L && graceUntil <= now && buffer <= 0 && rescuedUntil <= now) {
            return
        }

        store.edit {
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = buffer
            it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = if (isPremium) {
                PREMIUM_MAX_INTERNAL_BUFFER_MINUTES
            } else {
                MAX_INTERNAL_BUFFER_MINUTES
            }
            it[StreakBehaviorPrefs.STREAK_INTERNAL_HEALTH] = health
            it[StreakBehaviorPrefs.STREAK_BEHAVIOR_STATE] = state.name
            it[StreakBehaviorPrefs.STREAK_LAST_BEHAVIOR_TICK_MS] = now
            it[StreakBehaviorPrefs.STREAK_RESCUE_DIALOG_PENDING] = rescueDialogPending
        }
    }

    suspend fun markLost(
        context: android.content.Context,
        lostStreakDays: Int,
        missedDate: String = yesterdayKey()
    ) {
        val now = System.currentTimeMillis()
        StreakRecoveryManager.triggerRecovery(
            context = context,
            streakDays = lostStreakDays,
            recoveryHours = LOST_RECOVERY_WINDOW_HOURS
        )
        context.stepforgeStore.edit {
            it[StreakBehaviorPrefs.STREAK_BEHAVIOR_STATE] = StreakBehaviorState.LOST.name
            it[StreakBehaviorPrefs.STREAK_LOST_AT_MS] = now
            it[StreakBehaviorPrefs.STREAK_RECOVERY_UNTIL_MS] = now + LOST_RECOVERY_WINDOW_MS
            it[StreakBehaviorPrefs.STREAK_LOST_SNAPSHOT_DAYS] = lostStreakDays.coerceAtLeast(0)
            it[StreakBehaviorPrefs.STREAK_LOST_DATE] = missedDate
            it[StreakBehaviorPrefs.STREAK_DISPLAY_OVERRIDE_DAYS] = 0
            it[StreakBehaviorPrefs.STREAK_RESCUE_DIALOG_PENDING] = false
            it[StreakBehaviorPrefs.STREAK_LOST_DIALOG_PENDING] = true
            it[StreakBehaviorPrefs.STREAK_CRITICAL_GRACE_UNTIL_MS] = 0L
            it[StreakBehaviorPrefs.STREAK_RESCUED_UNTIL_MS] = 0L
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = 0
        }
    }

    private suspend fun clearLostState(
        store: androidx.datastore.core.DataStore<Preferences>,
        now: Long
    ) {
        store.edit {
            it[StreakBehaviorPrefs.STREAK_LOST_AT_MS] = 0L
            it[StreakBehaviorPrefs.STREAK_RECOVERY_UNTIL_MS] = 0L
            it[StreakBehaviorPrefs.STREAK_LOST_SNAPSHOT_DAYS] = 0
            it[StreakBehaviorPrefs.STREAK_LOST_DATE] = ""
            it[StreakBehaviorPrefs.STREAK_DISPLAY_OVERRIDE_DAYS] = 0
            it[StreakBehaviorPrefs.STREAK_LOST_DIALOG_PENDING] = false
            it[StreakBehaviorPrefs.STREAK_BEHAVIOR_STATE] = StreakBehaviorState.ACTIVE.name
        }
    }

    suspend fun applyManualRescue(context: android.content.Context): Boolean {
        val store = context.stepforgeStore
        val prefs = store.data.first()
        val now = System.currentTimeMillis()
        val lostAt = prefs[StreakBehaviorPrefs.STREAK_LOST_AT_MS] ?: 0L
        val recoveryUntil = prefs[StreakBehaviorPrefs.STREAK_RECOVERY_UNTIL_MS] ?: 0L
        if (lostAt > 0L && now <= recoveryUntil) return false

        val isPremium = (prefs[androidx.datastore.preferences.core.intPreferencesKey("premium_enabled")] ?: 0) == 1
        if (!isPremium) return false
        if (premiumRescuesLeft(prefs) <= 0) return false

        val currentMonth = currentMonthKey()
        val savedMonth = prefs[StreakShieldPrefs.PREMIUM_RESCUE_MONTH] ?: currentMonth
        val used = if (savedMonth == currentMonth) {
            prefs[StreakShieldPrefs.PREMIUM_RESCUE_USED_COUNT] ?: 0
        } else {
            0
        }

        val rescueMinutes = StreakShieldEngine.getPremiumRescueHours() * 60

        store.edit {
            it[StreakShieldPrefs.PREMIUM_RESCUE_MONTH] = currentMonth
            it[StreakShieldPrefs.PREMIUM_RESCUE_USED_COUNT] = used + 1
            it[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] = todayKey()
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = rescueMinutes
            it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = rescueMinutes
            it[StreakBehaviorPrefs.STREAK_RESCUED_UNTIL_MS] = now + PREMIUM_RESCUE_PROTECTION_MS
            it[StreakBehaviorPrefs.STREAK_CRITICAL_GRACE_UNTIL_MS] = 0L
            it[StreakBehaviorPrefs.STREAK_BEHAVIOR_STATE] = StreakBehaviorState.RESCUED.name
            it[StreakBehaviorPrefs.STREAK_INTERNAL_HEALTH] = 100
            it[StreakBehaviorPrefs.STREAK_RESCUE_DIALOG_PENDING] = false
            it[StreakBehaviorPrefs.STREAK_LOST_AT_MS] = 0L
            it[StreakBehaviorPrefs.STREAK_LOST_DIALOG_PENDING] = false
        }
        return true
    }

    suspend fun dismissRescueDialog(context: android.content.Context) {
        val now = System.currentTimeMillis()
        context.stepforgeStore.edit {
            it[StreakBehaviorPrefs.STREAK_RESCUE_DIALOG_PENDING] = false
            it[StreakBehaviorPrefs.STREAK_CRITICAL_GRACE_UNTIL_MS] = now + CRITICAL_GRACE_MS
            it[StreakBehaviorPrefs.STREAK_BEHAVIOR_STATE] = StreakBehaviorState.CRITICAL.name
        }
    }

    suspend fun dismissLostDialog(context: android.content.Context) {
        context.stepforgeStore.edit {
            it[StreakBehaviorPrefs.STREAK_LOST_DIALOG_PENDING] = false
        }
    }

    suspend fun restoreLostStreak(context: android.content.Context): Boolean {
        val store = context.stepforgeStore
        val prefs = store.data.first()
        val snapshot = prefs[StreakBehaviorPrefs.STREAK_LOST_SNAPSHOT_DAYS] ?: 0
        val recoveryUntil = prefs[StreakBehaviorPrefs.STREAK_RECOVERY_UNTIL_MS] ?: 0L
        val lostAt = prefs[StreakBehaviorPrefs.STREAK_LOST_AT_MS] ?: 0L
        val savedLostDate = prefs[StreakBehaviorPrefs.STREAK_LOST_DATE] ?: ""
        val now = System.currentTimeMillis()
        if (snapshot <= 0 && savedLostDate.isBlank()) return false
        if (lostAt <= 0L && savedLostDate.isBlank()) return false
        if (recoveryUntil > 0L && now > recoveryUntil) return false

        val missedDate = savedLostDate.ifBlank { yesterdayKey() }
        val protectedDates = readProtectedDates(
            prefs[StreakBehaviorPrefs.STREAK_PROTECTED_DATES]
        ).toMutableSet()
        protectedDates.add(missedDate)

        store.edit {
            it[StreakBehaviorPrefs.STREAK_PROTECTED_DATES] = writeProtectedDates(protectedDates)
            it[StreakBehaviorPrefs.STREAK_LAST_RESTORED_DATE] = missedDate
            it[StreakBehaviorPrefs.STREAK_LOST_AT_MS] = 0L
            it[StreakBehaviorPrefs.STREAK_RECOVERY_UNTIL_MS] = 0L
            it[StreakBehaviorPrefs.STREAK_LOST_SNAPSHOT_DAYS] = 0
            it[StreakBehaviorPrefs.STREAK_LOST_DATE] = ""
            it[StreakBehaviorPrefs.STREAK_LOST_DIALOG_PENDING] = false
            it[StreakBehaviorPrefs.STREAK_BEHAVIOR_STATE] = StreakBehaviorState.ACTIVE.name
            it[StreakBehaviorPrefs.STREAK_INTERNAL_HEALTH] = 80
            it[StreakBehaviorPrefs.STREAK_DISPLAY_OVERRIDE_DAYS] = 0
            it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = 6 * 60
            it[StreakBehaviorPrefs.STREAK_RESCUED_UNTIL_MS] = 0L
        }
        return true
    }

    fun applyDisplayOverride(computedStreak: Int, overrideDays: Int): Int {
        if (overrideDays <= 0) return computedStreak
        return maxOf(computedStreak, overrideDays)
    }

    suspend fun onDayQualifiedWithoutGoal(context: android.content.Context, date: String) {
        val store = context.stepforgeStore
        val prefs = store.data.first()
        val dates = readProtectedDates(prefs[StreakBehaviorPrefs.STREAK_PROTECTED_DATES]).toMutableSet()
        dates.add(date)
        store.edit {
            it[StreakBehaviorPrefs.STREAK_PROTECTED_DATES] = writeProtectedDates(dates)
        }
    }

    suspend fun computeQuickStreakDays(
        context: android.content.Context,
        goal: Int,
        todaySteps: Int
    ): Int {
        val db = com.example.stepforge.data.AppDatabase.getDatabase(context)
        val all = db.dailyStepsDao().getAllSteps().filter { !it.date.startsWith("TEST-") }
        val mapByDate = all
            .groupBy { it.date }
            .mapValues { entry -> entry.value.maxOf { it.steps } }
        val today = todayKey()
        val prefs = context.stepforgeStore.data.first()
        val protected = readProtectedDates(prefs[StreakBehaviorPrefs.STREAK_PROTECTED_DATES])
        val buffer = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
        val rescueDate = prefs[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] ?: ""
        val rescuedUntil = prefs[StreakBehaviorPrefs.STREAK_RESCUED_UNTIL_MS] ?: 0L
        val rescuedActive = rescuedUntil > System.currentTimeMillis()
        val todayQualification = qualifyDay(
            steps = todaySteps,
            goal = goal,
            internalBufferMinutes = buffer,
            rescuedActive = rescuedActive,
            rescueUsedForDay = rescueDate == today
        )
        return StreakAnalyticsEngine.computeCurrentStreakWithTodayOverride(
            dailyByDate = mapByDate,
            goal = goal,
            today = today,
            todayCountsForStreak = todayQualification.countsForStreak,
            protectedDates = protected,
            todayProtectedBridge = todayQualification.countsForStreak && !todayQualification.reachedGoal
        )
    }

    suspend fun checkAndMarkLostIfNeeded(
        context: android.content.Context,
        goal: Int,
        todaySteps: Int
    ): Boolean {
        val prefs = context.stepforgeStore.data.first()
        val now = System.currentTimeMillis()
        val lostAt = prefs[StreakBehaviorPrefs.STREAK_LOST_AT_MS] ?: 0L
        val recoveryUntil = prefs[StreakBehaviorPrefs.STREAK_RECOVERY_UNTIL_MS] ?: 0L
        if (lostAt > 0L && now <= recoveryUntil) return true

        val today = todayKey()
        val yesterday = yesterdayKey()

        // Today is still live. Only yesterday can close a streak and trigger recovery.

        val db = com.example.stepforge.data.AppDatabase.getDatabase(context)
        val dailyByDate = db.dailyStepsDao()
            .getAllSteps()
            .filter { !it.date.startsWith("TEST-") }
            .groupBy { it.date }
            .mapValues { entry -> entry.value.maxOf { it.steps } }

        val protectedDates = readProtectedDates(prefs[StreakBehaviorPrefs.STREAK_PROTECTED_DATES])
        val yesterdayQualified = (dailyByDate[yesterday] ?: 0) >= goal || protectedDates.contains(yesterday)
        if (yesterdayQualified) return false

        val alreadyRestoredYesterday = prefs[StreakBehaviorPrefs.STREAK_LAST_RESTORED_DATE] == yesterday
        if (alreadyRestoredYesterday) return false

        val streakBeforeMiss = StreakAnalyticsEngine.computeClosedStreakEndingAt(
            dailyByDate = dailyByDate,
            goal = goal,
            endDate = previousDateKey(yesterday),
            protectedDates = protectedDates
        )

        if (streakBeforeMiss <= 0) return false

        markLost(context, streakBeforeMiss, missedDate = yesterday)
        return true
    }

    suspend fun syncEarnedBuffer(context: android.content.Context, steps: Int, goal: Int) {
        val prefs = context.stepforgeStore.data.first()
        val isPremium = (prefs[androidx.datastore.preferences.core.intPreferencesKey("premium_enabled")] ?: 0) == 1
        val earned = calculateEarnedBufferMinutes(steps, goal, isPremium)
        val current = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
        if (earned > current) {
            context.stepforgeStore.edit {
                it[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] = earned
                it[StreakShieldPrefs.SHIELD_TODAY_MAX_MINUTES] = if (isPremium) {
                    PREMIUM_MAX_INTERNAL_BUFFER_MINUTES
                } else {
                    MAX_INTERNAL_BUFFER_MINUTES
                }
                it[StreakShieldPrefs.SHIELD_GENERATED_FOR_DATE] = todayKey()
            }
        }
    }
}
