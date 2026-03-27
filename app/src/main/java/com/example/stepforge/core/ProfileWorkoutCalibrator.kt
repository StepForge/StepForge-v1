package com.example.stepforge.core

import kotlin.math.max
import kotlin.math.roundToInt

class ProfileWorkoutCalibrator {

    data class CalibratedWorkout(
        val distanceMeters: Int,
        val calories: Int,
        val avgSpm: Int,
        val paceSecondsPerKm: Int,
        val qualityScore: Int,
        val estimatedSpeedKmh: Float
    )

    fun calibrate(
        steps: Int,
        durationMin: Int,
        durationMs: Long,
        profile: UserProfileSnapshot
    ): CalibratedWorkout {
        val safeSteps = steps.coerceAtLeast(0)
        val safeDurationMin = durationMin.coerceAtLeast(0)
        val stepLengthMeters = stepLengthMeters(profile)
        val distance = (safeSteps * stepLengthMeters).roundToInt().coerceAtLeast(0)

        val avgSpm = if (safeDurationMin > 0) {
            (safeSteps / safeDurationMin.toFloat()).roundToInt()
        } else 0

        val calories = caloriesKcal(
            profile = profile,
            steps = safeSteps,
            durationMinutes = safeDurationMin,
            distanceMeters = distance
        )

        val paceSecondsPerKm = paceSecondsPerKm(durationMs, distance)
        val estimatedSpeedKmh = estimatedSpeedKmh(durationMs, distance)
        val qualityScore = sessionQualityScore(profile, safeSteps, safeDurationMin, distance)

        return CalibratedWorkout(
            distanceMeters = distance,
            calories = calories,
            avgSpm = avgSpm,
            paceSecondsPerKm = paceSecondsPerKm,
            qualityScore = qualityScore,
            estimatedSpeedKmh = estimatedSpeedKmh
        )
    }

    fun stepLengthMeters(profile: UserProfileSnapshot): Float {
        val heightBased = if (profile.heightCm > 0) {
            (profile.heightCm * 0.415f) / 100f
        } else {
            0.75f
        }
        return heightBased.coerceIn(0.45f, 1.20f)
    }

    private fun paceSecondsPerKm(durationMs: Long, distanceMeters: Int): Int {
        if (durationMs <= 0L || distanceMeters <= 0) return 0
        val km = distanceMeters / 1000f
        if (km <= 0f) return 0
        return ((durationMs / 1000f) / km).roundToInt().coerceAtLeast(1)
    }

    private fun caloriesKcal(
        profile: UserProfileSnapshot,
        steps: Int,
        durationMinutes: Int,
        distanceMeters: Int
    ): Int {
        if (steps <= 0) return 0

        val weightFactor = when {
            profile.weightKg > 0 -> profile.weightKg / 70f
            else -> 1f
        }.coerceIn(0.5f, 2.5f)

        val paceFactor = when {
            durationMinutes <= 0 -> 1f
            steps / max(1, durationMinutes) >= 120 -> 1.25f
            steps / max(1, durationMinutes) >= 95 -> 1.10f
            steps / max(1, durationMinutes) >= 70 -> 1.00f
            else -> 0.92f
        }

        val distanceFactor = when {
            distanceMeters <= 0 -> 1f
            distanceMeters >= 5000 -> 1.10f
            distanceMeters >= 3000 -> 1.05f
            else -> 1f
        }

        val base = steps * 0.035f
        return (base * weightFactor * paceFactor * distanceFactor).roundToInt().coerceAtLeast(0)
    }

    private fun sessionQualityScore(
        profile: UserProfileSnapshot,
        steps: Int,
        durationMinutes: Int,
        distanceMeters: Int
    ): Int {
        if (steps <= 0 || durationMinutes <= 0) return 0

        val cadence = steps / durationMinutes.toFloat()
        val cadenceScore = when {
            cadence >= 120f -> 40f
            cadence >= 100f -> 35f
            cadence >= 80f -> 28f
            cadence >= 60f -> 20f
            else -> 10f
        }

        val durationScore = when {
            durationMinutes >= 45 -> 30f
            durationMinutes >= 30 -> 24f
            durationMinutes >= 15 -> 18f
            else -> 10f
        }

        val distanceScore = when {
            distanceMeters >= 4000 -> 25f
            distanceMeters >= 2500 -> 20f
            distanceMeters >= 1200 -> 14f
            else -> 8f
        }

        val profileBonus = when {
            profile.hasHeight && profile.hasWeight -> 5f
            profile.hasHeight || profile.hasWeight -> 2f
            else -> 0f
        }

        return (cadenceScore + durationScore + distanceScore + profileBonus)
            .roundToInt()
            .coerceIn(0, 100)
    }

    private fun estimatedSpeedKmh(durationMs: Long, distanceMeters: Int): Float {
        if (durationMs <= 0L || distanceMeters <= 0) return 0f
        val hours = durationMs / 3_600_000f
        if (hours <= 0f) return 0f
        return (distanceMeters / 1000f) / hours
    }
}
