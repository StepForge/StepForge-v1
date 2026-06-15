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
import com.example.stepforge.R
import com.example.stepforge.data.DailySteps
import com.example.stepforge.data.DailyWater
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.WorkoutSession
import com.example.stepforge.ui.achievements.AchievementRarity
import com.example.stepforge.ui.achievements.AchievementsRepository
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
        context: Context,
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
            achievements = buildAchievements(context, stepsHistory, workouts, stepGoal),
            weatherMood = weatherMood
        )
    }

    private fun estimateActiveMinutes(steps: Int): Int {
        if (steps <= 0) return 0
        return (steps / 105f).roundToInt().coerceAtLeast(1)
    }

    private fun buildAchievements(
        context: Context,
        stepsHistory: List<DailySteps>,
        workouts: List<WorkoutSession>,
        stepGoal: Int
    ): List<HistoryAchievementUi> {
        val achievementsState = AchievementsRepository.buildState(
            dailySteps = stepsHistory,
            workouts = workouts,
            stepGoal = stepGoal
        )

        val selected = (
                achievementsState.recentUnlocked +
                        listOfNotNull(achievementsState.bestAchievement) +
                        achievementsState.nextTargets +
                        achievementsState.achievements
                )
            .distinctBy { it.definition.id }
            .take(3)

        return selected.map { item ->
            HistoryAchievementUi(
                title = context.getString(item.definition.titleRes),
                subtitle = context.getString(item.definition.descriptionRes),
                progress = item.progress,
                level = item.definition.rarity.historyLevel(),
                iconRes = item.definition.iconRes
            )
        }
    }

    private fun AchievementRarity.historyLevel(): Int = when (this) {
        AchievementRarity.COMMON -> 1
        AchievementRarity.UNCOMMON -> 2
        AchievementRarity.RARE -> 3
        AchievementRarity.EPIC -> 4
        AchievementRarity.LEGENDARY -> 5
        AchievementRarity.MYTHIC -> 6
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

            val now = LocalDateTime.now()
            val code = current.optInt("weather_code", -1)
            val isDay = current.optInt("is_day", if (now.hour in 6..20) 1 else 0) == 1
            val windSpeed = current.optDouble("wind_speed_10m", 0.0).toFloat()
            val windGusts = current.optDouble("wind_gusts_10m", windSpeed.toDouble()).toFloat()
            val precipitation = current.optDouble("precipitation", 0.0).toFloat()
            val rain = current.optDouble("rain", 0.0).toFloat()
            val showers = current.optDouble("showers", 0.0).toFloat()
            val snowfall = current.optDouble("snowfall", 0.0).toFloat()
            val cloudCover = current.optDouble("cloud_cover", -1.0).toFloat()

            code.toWeatherMood(
                isDay = isDay,
                now = now,
                windSpeed = windSpeed,
                windGusts = windGusts,
                precipitation = precipitation,
                rain = rain,
                showers = showers,
                snowfall = snowfall,
                cloudCover = cloudCover
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
                capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
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
            append("&current=weather_code,is_day,precipitation,rain,showers,snowfall,cloud_cover,wind_speed_10m,wind_gusts_10m")
            append("&timezone=auto")
            append("&forecast_days=1")
        }

        val connection = (URL(apiUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 5_000
            readTimeout = 5_000
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
            manager.getProviders(true)
                .ifEmpty { listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER) }
                .mapNotNull { provider ->
                    runCatching { manager.getLastKnownLocation(provider) }.getOrNull()
                }
                .maxByOrNull { it.time }
        }.getOrNull()
    }

    private fun Int.toWeatherMood(
        isDay: Boolean,
        now: LocalDateTime,
        windSpeed: Float,
        windGusts: Float,
        precipitation: Float,
        rain: Float,
        showers: Float,
        snowfall: Float,
        cloudCover: Float
    ): HistoryWeatherMood {
        val hour = now.hour
        val isSunriseWindow = isDay && hour in 5..7
        val isSunsetWindow = isDay && hour in 18..20

        val wetAmount = maxOf(precipitation, rain, showers)
        val rainNow = wetAmount >= 0.10f
        val snowNow = snowfall >= 0.05f
        val strongWind = windGusts >= 55f || windSpeed >= 40f
        val rainCode = this in listOf(51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82)
        val snowCode = this in listOf(71, 73, 75, 77, 85, 86)
        val thunderCode = this in listOf(95, 96, 99)

        val dayMood = when {
            thunderCode && rainNow -> HistoryWeatherMood.THUNDERSTORM
            thunderCode && strongWind -> HistoryWeatherMood.STORM
            rainNow && strongWind -> HistoryWeatherMood.STORM
            snowNow -> if (strongWind) HistoryWeatherMood.BLIZZARD else HistoryWeatherMood.SNOW
            rainNow -> HistoryWeatherMood.RAIN
            snowCode -> if (snowNow || precipitation >= 0.10f) {
                if (strongWind) HistoryWeatherMood.BLIZZARD else HistoryWeatherMood.SNOW
            } else {
                HistoryWeatherMood.CLOUDY
            }
            rainCode -> if (rainNow) {
                HistoryWeatherMood.RAIN
            } else {
                HistoryWeatherMood.CLOUDY
            }
            this in listOf(45, 48) -> HistoryWeatherMood.FOG
            this in listOf(2, 3) || cloudCover >= 70f -> HistoryWeatherMood.CLOUDY
            this in listOf(0, 1) -> when {
                isSunriseWindow -> HistoryWeatherMood.SUNRISE
                isSunsetWindow && strongWind -> HistoryWeatherMood.WINDY_SUNSET
                isSunsetWindow -> HistoryWeatherMood.SUNSET
                strongWind -> HistoryWeatherMood.CLOUDY
                else -> HistoryWeatherMood.CLEAR
            }
            else -> fallbackWeatherMood(now)
        }

        if (isDay) return dayMood
        return when (dayMood) {
            HistoryWeatherMood.RAIN -> HistoryWeatherMood.RAIN_NIGHT
            HistoryWeatherMood.SNOW -> HistoryWeatherMood.SNOW_NIGHT
            HistoryWeatherMood.FOG -> HistoryWeatherMood.FOG_NIGHT
            HistoryWeatherMood.STORM,
            HistoryWeatherMood.THUNDERSTORM -> HistoryWeatherMood.THUNDERSTORM_NIGHT
            HistoryWeatherMood.BLIZZARD -> HistoryWeatherMood.BLIZZARD
            else -> HistoryWeatherMood.NIGHT
        }
    }
}
