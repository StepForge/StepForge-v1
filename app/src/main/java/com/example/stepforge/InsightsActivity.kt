package com.example.stepforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.stepforgeTheme
import kotlinx.coroutines.flow.map
import com.example.stepforge.ui.insights.InsightsScreen


class InsightsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val ctx = LocalContext.current

            val themeKey = stringPreferencesKey("theme_mode")
            val themeFlow = ctx.stepforgeStore.data.map { prefs -> prefs[themeKey] ?: "system" }
            val themeValue by themeFlow.collectAsState(initial = "system")

            val useDark = when (themeValue) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }

            stepforgeTheme(darkTheme = useDark) {
                InsightsScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
