package com.example.stepforge.ui.streak

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.HourlySteps
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StreakViewModel(
    private val appContext: Context
) : ViewModel() {

    private val _ui = MutableStateFlow(StreakUiState())
    val ui: StateFlow<StreakUiState> = _ui

    private val KEY_STEP_GOAL = intPreferencesKey("step_goal")
    private val billing: StreakRestoreBilling = MockStreakRestoreBilling

    private val ymd = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    init {
        viewModelScope.launch {
            StreakRecoveryManager.recoveryState(appContext).collect { recovery ->
                if (recovery.expiredVisibleState) {
                    StreakRecoveryManager.expireIfNeeded(appContext)
                }
                _ui.update { current ->
                    current.copy(
                        recovery = recovery,
                        showLostRestoreDialog = recovery.visible,
                        lostStreakSnapshot = recovery.lostStreakDays,
                        recoveryWindowActive = recovery.visible
                    )
                }
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(isLoading = true)

            val state = withContext(Dispatchers.IO) {
                val db = AppDatabase.getDatabase(appContext)
                val dailyDao = db.dailyStepsDao()
                val hourlyDao = db.hourlyStepsDao()

                val prefs = appContext.stepforgeStore.data.first()
                val goal = prefs[KEY_STEP_GOAL] ?: 10_000
                val perfectTh = StreakAnalyticsEngine.perfectThreshold(goal)

                val allDaily: List<DailySteps> = dailyDao.getAllSteps()
                    .filter { !it.date.startsWith("TEST-") }

                val mapByDate: Map<String, Int> = allDaily
                    .groupBy { it.date }
                    .mapValues { entry -> entry.value.maxOf { it.steps } }
                val sortedAsc = mapByDate
                    .map { (date, steps) -> DailySteps(date = date, steps = steps) }
                    .sortedBy { it.date }

                val today = ymd.format(Date())
                val todaySteps = mapByDate[today] ?: 0
                val todayHit = todaySteps >= goal

                fun lastNDates(n: Int): List<String> {
                    val out = ArrayList<String>(n)
                    val cal = Calendar.getInstance().apply {
                        time = ymd.parse(today) ?: Date()
                    }
                    cal.add(Calendar.DAY_OF_YEAR, -(n - 1))
                    repeat(n) {
                        out.add(ymd.format(cal.time))
                        cal.add(Calendar.DAY_OF_YEAR, 1)
                    }
                    return out
                }

                fun listForDates(dates: List<String>): List<DailySteps> {
                    return dates.map { d ->
                        DailySteps(date = d, steps = mapByDate[d] ?: 0)
                    }
                }

                val dates7 = lastNDates(7)
                val dates14 = lastNDates(14)
                val dates30 = lastNDates(30)

                val last7 = listForDates(dates7)
                val last14 = listForDates(dates14)
                val last30 = listForDates(dates30)

                val behavior = StreakBehaviorEngine.readSnapshot(appContext)
                val protectedDates = StreakBehaviorEngine.readProtectedDates(
                    prefs[StreakBehaviorPrefs.STREAK_PROTECTED_DATES]
                )

                val bufferMinutes = prefs[StreakShieldPrefs.SHIELD_TODAY_MINUTES_LEFT] ?: 0
                val rescueUsedDate = prefs[StreakShieldPrefs.PREMIUM_RESCUE_USED_FOR_DATE] ?: ""
                val rescueUsedToday = rescueUsedDate == today
                val rescuedActive = behavior.rescuedUntilMs > System.currentTimeMillis()

                val todayQualification = StreakDayQualifier.qualifyDay(
                    steps = todaySteps,
                    goal = goal,
                    behaviorBufferMinutes = bufferMinutes,
                    rescueUsedForDay = rescueUsedToday,
                    rescuedActive = rescuedActive
                )

                val computedStreak = StreakAnalyticsEngine.computeCurrentStreakWithTodayOverride(
                    dailyByDate = mapByDate,
                    goal = goal,
                    today = today,
                    todayCountsForStreak = todayQualification.countsForStreak,
                    protectedDates = protectedDates,
                    todayProtectedBridge = todayQualification.countsForStreak && !todayQualification.reachedGoal
                )

                val currentStreak = if (behavior.state == StreakBehaviorState.LOST) {
                    0
                } else {
                    computedStreak
                }

                val longestStreak = StreakAnalyticsEngine.computeLongestStreak(
                    dailySortedAsc = sortedAsc,
                    goal = goal,
                    protectedDates = protectedDates
                ).let { maxOf(it, currentStreak) }

                val weeklyAvg = (last7.sumOf { it.steps } / last7.size.coerceAtLeast(1))
                val weeklyCompletion = StreakAnalyticsEngine.weeklyCompletionRate(last7, goal)
                val (trendPct, trendLabel) = StreakAnalyticsEngine.computeWeeklyTrend(last14)

                val monthlyActive = last30.count { it.steps > 0 }
                val monthlyHit = last30.count { it.steps >= goal }
                val monthlyMiss = 30 - monthlyHit

                val consistency = StreakAnalyticsEngine.computeConsistencyScore(last30)
                val activityScore = StreakAnalyticsEngine.computeActivityScore(
                    consistencyScore = consistency,
                    goalCompletedDays = monthlyHit,
                    periodDays = 30
                )

                val perfectDays = StreakAnalyticsEngine.computePerfectDays(last30, goal)

                val hourlyByDate: Map<String, List<HourlySteps>> = buildMap {
                    for (d in dates30) {
                        put(d, hourlyDao.getForDate(d))
                    }
                }
                val peakHour = StreakAnalyticsEngine.peakHourLabel(hourlyByDate, dates30)
                val mostActiveDay = StreakAnalyticsEngine.computeMostActiveDayLabel(last30)

                val (riskLevel, riskNoteType, riskDropPercent) = StreakAnalyticsEngine.streakRisk(last7, goal)

                val nowHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                val (chance, chanceNoteType) = StreakAnalyticsEngine.goalPrediction(
                    todaySteps = todaySteps,
                    goal = goal,
                    last7Avg = weeklyAvg,
                    nowHour = nowHour
                )

                val isPremium = (prefs[intPreferencesKey("premium_enabled")] ?: 0) == 1
                val premiumAiCoachEnabled = prefs[StreakShieldPrefs.PREMIUM_AI_COACH_ENABLED] ?: false

                val lostSnapshot = behavior.lostSnapshotDays

                StreakUiState(
                    isLoading = false,

                    todaySteps = todaySteps,
                    goalSteps = goal,
                    currentStreakDays = currentStreak,
                    longestStreakDays = longestStreak,
                    todayCompletedGoal = todayHit,

                    perfectDayThresholdSteps = perfectTh,
                    perfectDaysLast30 = perfectDays,

                    weeklyAverageSteps = weeklyAvg,
                    weeklyGoalCompletionRate = weeklyCompletion,
                    weeklyTrendLabel = trendLabel,
                    weeklyTrendPercent = trendPct,

                    monthlyActiveDays = monthlyActive,
                    monthlyGoalCompletedDays = monthlyHit,
                    monthlyGoalMissedDays = monthlyMiss,

                    consistencyScore = consistency,
                    activityScore = activityScore,

                    peakHourLabel = peakHour,
                    mostActiveDayLabel = mostActiveDay,

                    streakRiskLevel = riskLevel,
                    streakRiskNoteType = riskNoteType,
                    streakRiskDropPercent = riskDropPercent,

                    goalPredictionChance = chance,
                    goalPredictionNoteType = chanceNoteType,

                    last7Steps = StreakAnalyticsEngine.buildLast7Points(last7, goal),
                    last30Heat = StreakAnalyticsEngine.buildHeat30(last30, goal),

                    isPremium = isPremium,

                    streakBehaviorState = behavior.state,
                    streakStateMessage = behavior.stateMessage,
                    streakHealthPercent = behavior.healthPercent,

                    premiumRescuesLeft = behavior.premiumRescuesLeft,
                    premiumAiCoachEnabled = premiumAiCoachEnabled,

                    showRescueDialog = behavior.showRescueDialog && isPremium,
                    showLostRestoreDialog = _ui.value.recovery.visible,
                    lostStreakSnapshot = lostSnapshot,
                    recoveryWindowActive = behavior.recoveryWindowActive || _ui.value.recovery.visible,
                    recovery = _ui.value.recovery
                )
            }

            _ui.value = state
        }
    }

    fun onProtectStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            StreakBehaviorEngine.applyManualRescue(appContext)
            load()
        }
    }

    fun onDismissRescue() {
        viewModelScope.launch(Dispatchers.IO) {
            StreakBehaviorEngine.dismissRescueDialog(appContext)
            load()
        }
    }

    fun onRestoreStreak() {
        _ui.update { current ->
            current.copy(
                recovery = current.recovery.copy(visible = false),
                showLostRestoreDialog = false,
                recoveryWindowActive = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            val result = billing.purchaseRestore()
            if (result.success) {
                StreakRecoveryManager.restoreStreak(appContext)
            }
            load()
        }
    }

    fun onDismissLostRestore() {
        _ui.update { current ->
            current.copy(
                recovery = current.recovery.copy(visible = false),
                showLostRestoreDialog = false,
                recoveryWindowActive = false
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            StreakRecoveryManager.dismissRecovery(appContext)
            load()
        }
    }
}
