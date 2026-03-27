package com.example.stepforge.core

import java.util.Locale

data class UserProfileSnapshot(
    val username: String = "",
    val heightCm: Int = 0,
    val weightKg: Int = 0,
    val gender: String = "",
    val birthDate: String = "",
    val age: Int = 0,
    val unit: String = "km",
    val goalSteps: Int = 10_000
) {
    val hasHeight: Boolean get() = heightCm > 0
    val hasWeight: Boolean get() = weightKg > 0

    fun label(): String {
        return buildString {
            if (username.isNotBlank()) append(username)
            if (age > 0) {
                if (isNotEmpty()) append(" • ")
                append(age)
                append("y")
            }
            if (gender.isNotBlank()) {
                if (isNotEmpty()) append(" • ")
                append(gender.lowercase(Locale.getDefault()))
            }
        }
    }
}
