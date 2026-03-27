package com.example.stepforge.data

import android.content.Context
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.core.UserProfileSnapshot
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.flow.first
import java.util.Calendar

class ProfileSnapshotResolver(private val context: Context) {

    private val USERNAME = stringPreferencesKey("username")
    private val HEIGHT = intPreferencesKey("height")
    private val WEIGHT = intPreferencesKey("weight")
    private val GENDER = stringPreferencesKey("gender")
    private val BIRTH_DATE = stringPreferencesKey("birth_date")
    private val UNIT = stringPreferencesKey("unit")
    private val STEP_GOAL = intPreferencesKey("step_goal")

    suspend fun load(): UserProfileSnapshot {
        val prefs = context.stepforgeStore.data.first()
        val birthDate = prefs[BIRTH_DATE] ?: ""

        return UserProfileSnapshot(
            username = prefs[USERNAME] ?: "",
            heightCm = prefs[HEIGHT] ?: 0,
            weightKg = prefs[WEIGHT] ?: 0,
            gender = prefs[GENDER] ?: "",
            birthDate = birthDate,
            age = parseAge(birthDate),
            unit = prefs[UNIT] ?: "km",
            goalSteps = prefs[STEP_GOAL] ?: 10_000
        )
    }

    private fun parseAge(birth: String): Int {
        if (birth.isBlank()) return 0
        return try {
            val parts = birth.split("-", "/", ".", " ").filter { it.isNotBlank() }
            val (day, month, year) = when {
                parts.size == 3 && parts[0].length == 4 -> {
                    Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                }
                parts.size == 3 -> {
                    Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                }
                else -> Triple(0, 0, 0)
            }

            if (year == 0) return 0

            val now = Calendar.getInstance()
            var age = now.get(Calendar.YEAR) - year
            val mNow = now.get(Calendar.MONTH) + 1
            val dNow = now.get(Calendar.DAY_OF_MONTH)
            if (mNow < month || (mNow == month && dNow < day)) age--
            if (age < 0) 0 else age
        } catch (_: Exception) {
            0
        }
    }
}
