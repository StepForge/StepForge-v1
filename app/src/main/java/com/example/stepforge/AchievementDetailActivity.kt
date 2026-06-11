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
import com.example.stepforge.ui.achievements.AchievementDetailRoute
import com.example.stepforge.ui.achievements.AchievementItemUi
import com.example.stepforge.ui.achievements.AchievementsRepository
import com.example.stepforge.ui.rememberUseDarkTheme
import com.example.stepforge.ui.stepforgeTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AchievementDetailActivity : ComponentActivity() {
    companion object {
        const val EXTRA_ACHIEVEMENT_ID = "extra_achievement_id"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val achievementId = intent.getStringExtra(EXTRA_ACHIEVEMENT_ID).orEmpty()

        setContent {
            val useDark = rememberUseDarkTheme(this)
            var item by remember { mutableStateOf<AchievementItemUi?>(null) }

            LaunchedEffect(achievementId) {
                item = withContext(Dispatchers.IO) {
                    val db = AppDatabase.getDatabase(this@AchievementDetailActivity)
                    val prefs = this@AchievementDetailActivity.stepforgeStore.data.first()
                    val stepGoal = prefs[intPreferencesKey("step_goal")] ?: 10_000
                    AchievementsRepository.buildState(
                        dailySteps = db.dailyStepsDao().getAllSteps(),
                        workouts = db.workoutSessionDao().getAll(),
                        stepGoal = stepGoal
                    ).achievements.firstOrNull { it.definition.id == achievementId }
                }
            }

            stepforgeTheme(darkTheme = useDark) {
                item?.let { achievement ->
                    AchievementDetailRoute(
                        item = achievement,
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}
