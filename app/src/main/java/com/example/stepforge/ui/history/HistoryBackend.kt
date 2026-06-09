package com.example.stepforge.ui.history

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.DailyWater
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.WorkoutSession
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal object HistoryBackend {
    fun buildState(
        stepsHistory: List<DailySteps>,
        waterHistory: List<DailyWater>,
        sleepHistory: List<SleepSession>,
        workouts: List<WorkoutSession>,
        stepGoal: Int,
        waterGoal: Int,
        selectedDate: LocalDate?,
        selectedMetric: HistoryMetric,
        selectedRange: HistoryRange,
        visibleMonth: YearMonth,
        weatherMood: HistoryWeatherMood
    ): HistoryUiState {
        val today = LocalDate.now()
        val waterMap = waterHistory.associateBy { it.date }
        val sleepMap = sleepHistory.groupBy { it.date }
        val workoutMap = workouts.groupBy { it.date }

        val cleanDays = stepsHistory
            .asSequence()
            .filter { !it.date.startsWith(HISTORY_TEST_PREFIX) }
            .mapNotNull { daily ->
                val date = daily.date.safeHistoryDate() ?: return@mapNotNull null
                val dayWorkouts = workoutMap[daily.date].orEmpty()
                val activeMinutes = dayWorkouts.sumOf { it.durationMinutes }.takeIf { it > 0 }
                    ?: estimateActiveMinutes(daily.steps)
                val workoutCalories = dayWorkouts.sumOf { it.caloriesKcal }
                val workoutDistanceKm = dayWorkouts.sumOf { it.distanceMeters } / 1000f
                HistoryDayUi(
                    date = daily.date,
                    localDate = date,
                    steps = daily.steps.coerceAtLeast(0),
                    distanceKm = workoutDistanceKm.takeIf { it > 0f } ?: stepsToDistanceKm(daily.steps),
                    calories = workoutCalories.takeIf { it > 0 } ?: stepsToCalories(daily.steps),
                    activeMinutes = activeMinutes,
                    waterMl = waterMap[daily.date]?.waterMl ?: 0,
                    sleepMinutes = sleepMap[daily.date].orEmpty().sumOf { it.totalMinutes }.coerceAtLeast(0),
                    workoutSessions = dayWorkouts.size,
                    stepGoal = stepGoal.coerceAtLeast(1),
                    waterGoal = waterGoal.coerceAtLeast(1)
                )
            }
            .distinctBy { it.date }
            .sortedByDescending { it.localDate }
            .toList()

        val fallbackDay = HistoryDayUi(
            date = today.toString(),
            localDate = today,
            steps = 0,
            distanceKm = 0f,
            calories = 0,
            activeMinutes = 0,
            waterMl = 0,
            sleepMinutes = 0,
            workoutSessions = 0,
            stepGoal = stepGoal.coerceAtLeast(1),
            waterGoal = waterGoal.coerceAtLeast(1)
        )
        val allDays = cleanDays.ifEmpty { listOf(fallbackDay) }
        val selected = selectedDate?.let { date -> allDays.firstOrNull { it.localDate == date } } ?: allDays.first()

        val visibleDays = if (selectedRange == HistoryRange.ALL) {
            allDays
        } else {
            val minDate = today.minusDays((selectedRange.days - 1).toLong())
            allDays.filter { !it.localDate.isBefore(minDate) }
        }.ifEmpty { allDays.take(1) }

        val dayMap = allDays.associateBy { it.localDate }
        val totalSteps = visibleDays.sumOf { it.steps }
        val totalDistance = visibleDays.sumOf { it.distanceKm.toDouble() }.toFloat()
        val totalCalories = visibleDays.sumOf { it.calories }
        val totalActive = visibleDays.sumOf { it.activeMinutes }
        val avgSteps = if (visibleDays.isEmpty()) 0 else totalSteps / visibleDays.size
        val topDays = visibleDays.sortedByDescending { it.metricValue(selectedMetric) }.take(3)

        return HistoryUiState(
            allDays = allDays,
            visibleDays = visibleDays,
            selectedDay = selected,
            selectedMetric = selectedMetric,
            selectedRange = selectedRange,
            visibleMonth = visibleMonth,
            calendarDays = buildCalendarDays(visibleMonth, dayMap),
            totalSteps = totalSteps,
            averageSteps = avgSteps,
            totalDistanceKm = totalDistance,
            totalCalories = totalCalories,
            totalActiveMinutes = totalActive,
            topDays = topDays,
            achievements = buildAchievements(allDays, stepGoal),
            weatherMood = weatherMood
        )
    }

    private fun estimateActiveMinutes(steps: Int): Int {
        if (steps <= 0) return 0
        return (steps / 105f).roundToInt().coerceAtLeast(1)
    }

    private fun buildAchievements(days: List<HistoryDayUi>, stepGoal: Int): List<HistoryAchievementUi> {
        val totalSteps = days.sumOf { it.steps }
        val streak = currentGoalStreak(days, stepGoal)
        val totalDistance = days.sumOf { it.distanceKm.toDouble() }.toFloat()
        val activeDays = days.count { it.activeMinutes > 0 || it.steps > 0 }
        return listOf(
            HistoryAchievementUi("100K", "Steps", (totalSteps / 100_000f).coerceIn(0f, 1f), 1),
            HistoryAchievementUi("30", "Day Streak", (streak / 30f).coerceIn(0f, 1f), 2),
            HistoryAchievementUi("42K", "Marathon", (totalDistance / 42.2f).coerceIn(0f, 1f), 3),
            HistoryAchievementUi("10", "Active Days", (activeDays / 10f).coerceIn(0f, 1f), 4)
        )
    }

    private fun currentGoalStreak(days: List<HistoryDayUi>, stepGoal: Int): Int {
        val sorted = days.sortedByDescending { it.localDate }
        var streak = 0
        for (day in sorted) {
            if (day.steps >= stepGoal) streak++ else break
        }
        return streak
    }

    internal fun fallbackWeatherMood(now: LocalDateTime = LocalDateTime.now()): HistoryWeatherMood {
        val hour = now.hour
        val month = now.monthValue
        return when {
            hour in 5..7 -> HistoryWeatherMood.SUNRISE
            hour in 18..20 -> HistoryWeatherMood.SUNSET
            hour < 5 || hour >= 21 -> HistoryWeatherMood.NIGHT
            month == 12 || month == 1 || month == 2 -> HistoryWeatherMood.SNOW
            month in 10..11 -> HistoryWeatherMood.RAIN
            else -> HistoryWeatherMood.CLEAR
        }
    }

    internal suspend fun resolveWeatherMood(context: Context): HistoryWeatherMood = withContext(Dispatchers.IO) {
        val fallback = fallbackWeatherMood()
        runCatching {
            if (!hasUsableNetwork(context)) return@runCatching fallback
            val location = lastKnownLocation(context) ?: return@runCatching fallback
            val current = fetchOpenMeteoCurrent(location) ?: return@runCatching fallback
            val code = current.optInt("weather_code", -1)
            val isDay = current.optInt("is_day", if (LocalDateTime.now().hour in 6..20) 1 else 0) == 1
            val windSpeed = current.optDouble("wind_speed_10m", 0.0).toFloat()
            code.toWeatherMood(
                isDay = isDay,
                now = LocalDateTime.now(),
                windSpeed = windSpeed
            )
        }.getOrElse { fallback }
    }

    @Suppress("DEPRECATION")
    private fun hasUsableNetwork(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        return runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val network = manager.activeNetwork ?: return@runCatching false
                val capabilities = manager.getNetworkCapabilities(network) ?: return@runCatching false
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
            } else {
                manager.activeNetworkInfo?.isConnected == true
            }
        }.getOrDefault(false)
    }

    private fun fetchOpenMeteoCurrent(location: Location): JSONObject? {
        val apiUrl = buildString {
            append("https://api.open-meteo.com/v1/forecast")
            append("?latitude=${location.latitude}")
            append("&longitude=${location.longitude}")
            append("&current=weather_code,is_day,wind_speed_10m")
            append("&timezone=auto")
            append("&forecast_days=1")
        }

        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4_000
            readTimeout = 4_000
            setRequestProperty("Accept", "application/json")
        }

        return try {
            val responseCode = runCatching { connection.responseCode }.getOrElse { return null }
            if (responseCode !in 200..299) return null
            val response = runCatching {
                connection.inputStream.bufferedReader().use { it.readText() }
            }.getOrElse { return null }
            JSONObject(response).optJSONObject("current")
        } catch (_: Throwable) {
            null
        } finally {
            runCatching { connection.disconnect() }
        }
    }

    private fun lastKnownLocation(context: Context): Location? {
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) return null
        val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return runCatching {
            val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
            providers.mapNotNull { provider ->
                runCatching {
                    if (!manager.isProviderEnabled(provider)) null else manager.getLastKnownLocation(provider)
                }.getOrNull()
            }.maxByOrNull { it.time }
        }.getOrNull()
    }

    private fun Int.toWeatherMood(
        isDay: Boolean,
        now: LocalDateTime,
        windSpeed: Float
    ): HistoryWeatherMood {
        val hour = now.hour
        val isSunriseWindow = isDay && hour in 5..7
        val isSunsetWindow = isDay && hour in 18..20

        val dayMood = when (this) {
            0, 1 -> when {
                isSunriseWindow -> HistoryWeatherMood.SUNRISE
                isSunsetWindow && windSpeed >= 20f -> HistoryWeatherMood.WINDY_SUNSET
                isSunsetWindow -> HistoryWeatherMood.SUNSET
                else -> HistoryWeatherMood.CLEAR
            }
            2, 3 -> when {
                isSunriseWindow -> HistoryWeatherMood.SUNRISE
                isSunsetWindow && windSpeed >= 20f -> HistoryWeatherMood.WINDY_SUNSET
                isSunsetWindow -> HistoryWeatherMood.SUNSET
                else -> HistoryWeatherMood.CLOUDY
            }
            45, 48 -> HistoryWeatherMood.FOG
            51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> HistoryWeatherMood.RAIN
            71, 73, 75, 77, 85, 86 -> if (windSpeed >= 28f) HistoryWeatherMood.BLIZZARD else HistoryWeatherMood.SNOW
            95 -> HistoryWeatherMood.STORM
            96, 99 -> HistoryWeatherMood.THUNDERSTORM
            else -> fallbackWeatherMood(now)
        }
        if (isDay) return dayMood
        return when (dayMood) {
            HistoryWeatherMood.RAIN -> HistoryWeatherMood.RAIN_NIGHT
            HistoryWeatherMood.SNOW -> HistoryWeatherMood.SNOW_NIGHT
            HistoryWeatherMood.FOG -> HistoryWeatherMood.FOG_NIGHT
            HistoryWeatherMood.STORM, HistoryWeatherMood.THUNDERSTORM -> HistoryWeatherMood.THUNDERSTORM_NIGHT
            HistoryWeatherMood.CLOUDY, HistoryWeatherMood.CLEAR, HistoryWeatherMood.SUNRISE, HistoryWeatherMood.SUNSET, HistoryWeatherMood.WINDY_SUNSET -> HistoryWeatherMood.NIGHT
            HistoryWeatherMood.BLIZZARD -> HistoryWeatherMood.BLIZZARD
            else -> dayMood
        }
    }
}
