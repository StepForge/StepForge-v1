package com.example.stepforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.achievements.AchievementsRepository
import com.example.stepforge.ui.achievements.AchievementsRoute
import com.example.stepforge.ui.achievements.AchievementsUiState
import com.example.stepforge.ui.rememberUseDarkTheme
import com.example.stepforge.ui.stepforgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AchievementsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDark = rememberUseDarkTheme(this)
            var state by remember { mutableStateOf(AchievementsUiState.empty()) }

            LaunchedEffect(Unit) {
                state = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(this@AchievementsActivity)
                    val prefs = this@AchievementsActivity.stepforgeStore.data.first()
                    val stepGoal = prefs[intPreferencesKey("step_goal")] ?: 10_000
                    AchievementsRepository.buildState(
                        dailySteps = db.dailyStepsDao().getAllSteps(),
                        workouts = db.workoutSessionDao().getAll(),
                        stepGoal = stepGoal
                    )
                }
            }

            stepforgeTheme(darkTheme = useDark) {
                AchievementsRoute(
                    state = state,
                    onBack = { finish() }
                )
            }
        }
    }
}
