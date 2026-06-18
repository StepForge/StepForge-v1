package com.example.stepforge.ui.progress

import android.content.Context
import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepforge.AchievementsActivity
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailyStepsDao
import com.example.stepforge.data.WorkoutSessionDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val db = remember(appContext) { AppDatabase.getDatabase(appContext) }
    val dao = remember(db) { db.dailyStepsDao() }
    val workoutDao = remember(db) { db.workoutSessionDao() }

    val viewModel: ProgressViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgressViewModel(appContext, dao, workoutDao) as T
            }
        }
    )

    val state by viewModel.state.collectAsState()
    ProgressScreenContent(
        state = state,
        onBack = onBack,
        onRangeSelected = viewModel::selectRange,
        onAchievementClick = {
            context.startActivity(Intent(context, AchievementsActivity::class.java))
        }
    )
}

private class ProgressViewModel(
    appContext: Context,
    dao: DailyStepsDao,
    workoutDao: WorkoutSessionDao
) : ViewModel() {
    private val repository = ProgressRepository(appContext, dao, workoutDao)
    private val selectedRange = MutableStateFlow(ProgressRange.D7)

    private val _state = MutableStateFlow(ProgressDashboardState.empty(ProgressRange.D7))
    val state: StateFlow<ProgressDashboardState> = _state

    init {
        viewModelScope.launch {
            repository.observeDashboard(selectedRange).collect { dashboard ->
                _state.value = dashboard
            }
        }
    }

    fun selectRange(range: ProgressRange) {
        selectedRange.value = range
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun ProgressScreenContent(
    state: ProgressDashboardState,
    onBack: () -> Unit,
    onRangeSelected: (ProgressRange) -> Unit,
    onAchievementClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                progressPalette().backgroundBrush()
            )
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val scale = (maxWidth.value / 390f).coerceIn(0.84f, 1f)
            val compact = maxWidth < 372.dp
            val horizontal = (18f * scale).dp
            val gap = (14f * scale).dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontal, vertical = (18f * scale).dp),
                verticalArrangement = Arrangement.spacedBy(gap)
            ) {
                ProgressHeader(
                    selectedRange = state.range,
                    compact = compact,
                    scale = scale,
                    onBack = onBack,
                    onRangeSelected = onRangeSelected
                )

                AnimatedContent(
                    targetState = state,
                    transitionSpec = { fadeIn() with fadeOut() },
                    label = "progressRangeContent"
                ) { target ->
                    Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                        ProgressHeroCard(
                            state = target,
                            scale = scale
                        )

                        ProgressMetricCards(
                            state = target,
                            scale = scale
                        )

                        WeeklyStreakCard(
                            state = target,
                            scale = scale
                        )

                        AchievementUnlockCard(
                            achievement = target.achievement,
                            scale = scale,
                            onClick = onAchievementClick
                        )
                    }
                }

                Spacer(Modifier.height((10f * scale).dp))
            }
        }
    }
}
