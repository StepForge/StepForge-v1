package com.example.stepforge.ui

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.flow.map

/**
 * DataStore theme_mode: "dark" | "light" | "system"
 */
@Composable
fun rememberUseDarkTheme(context: Context): Boolean {
    val themeKey = stringPreferencesKey("theme_mode")
    val flow = context.stepforgeStore.data.map { prefs -> prefs[themeKey] ?: "system" }
    val mode by flow.collectAsState(initial = "system")

    return when (mode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
}