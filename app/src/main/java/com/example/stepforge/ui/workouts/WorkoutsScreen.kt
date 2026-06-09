package com.example.stepforge.ui.workouts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.WorkoutSession
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onBack: () -> Unit = {},
    highlightedSessionId: Long = -1L
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val dao = remember { AppDatabase.getDatabase(context).workoutSessionDao() }

    var sessions by remember { mutableStateOf<List<WorkoutSession>>(emptyList()) }
    var selectedMetric by remember { mutableStateOf(WorkoutChartMetric.DURATION) }
    var activeSheet by remember { mutableStateOf<WorkoutSheetType?>(null) }

    LaunchedEffect(dao) {
        dao.observeAll().collectLatest { loaded ->
            sessions = loaded
        }
    }

    val state = remember(sessions) {
        buildWorkoutsDashboardState(sessions)
    }

    var highlightedHandled by remember(highlightedSessionId) { mutableStateOf(false) }

    LaunchedEffect(highlightedSessionId, state.displaySessions) {
        if (
            !highlightedHandled &&
            highlightedSessionId > 0L &&
            state.displaySessions.any { it.id == highlightedSessionId }
        ) {
            activeSheet = WorkoutSheetType.JOURNEY
            highlightedHandled = true
        }
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(workoutsPageBrush(isDark, cs))
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 18.dp,
                top = 28.dp,
                end = 18.dp,
                bottom = 34.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WorkoutsHeader(
                    onBack = onBack,
                    onCalendarClick = { activeSheet = WorkoutSheetType.WEEK },
                    onFilterClick = { activeSheet = WorkoutSheetType.JOURNEY }
                )
            }

            item {
                WorkoutMetricTabChartCard(
                    state = state,
                    selectedMetric = selectedMetric,
                    onMetricSelected = { selectedMetric = it },
                    onClick = { /* Chart day selection stays inline. No sheet here. */ }
                )
            }

            item {
                WorkoutSummaryMetricCard(
                    state = state,
                    onActiveTimeClick = { activeSheet = WorkoutSheetType.ACTIVE_TIME },
                    onTrainingLoadClick = { activeSheet = WorkoutSheetType.TRAINING_LOAD }
                )
            }

            item {
                TodayWorkoutSummaryCard(
                    state = state,
                    onClick = { activeSheet = WorkoutSheetType.TODAY }
                )
            }

            item {
                ThisWeekHeatmapCard(
                    state = state,
                    onClick = { activeSheet = WorkoutSheetType.WEEK }
                )
            }

            item {
                WorkoutBentoGrid(
                    state = state,
                    onOpenSheet = { activeSheet = it }
                )
            }

            item {
                FocusDayCard(
                    state = state,
                    onClick = { activeSheet = WorkoutSheetType.FOCUS }
                )
            }

            item {
                WorkoutJourneyCard(
                    state = state,
                    onClick = { activeSheet = WorkoutSheetType.JOURNEY }
                )
            }

            item {
                DiveDeeperCard(
                    onClick = { activeSheet = WorkoutSheetType.JOURNEY }
                )
            }

            item { Spacer(modifier = Modifier.height(4.dp)) }
        }
    }

    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { activeSheet = null },
            sheetState = sheetState,
            containerColor = if (isDark) Color(0xFF0E1218) else cs.surface,
            contentColor = cs.onSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 10.dp, bottom = 6.dp)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .height(4.dp)
                            .fillMaxWidth(0.14f)
                            .background(
                                cs.onSurface.copy(alpha = 0.26f),
                                RoundedCornerShape(999.dp)
                            )
                    )
                }
            }
        ) {
            WorkoutDetailSheetContent(
                sheet = activeSheet ?: WorkoutSheetType.TODAY,
                state = state,
                onClose = { activeSheet = null }
            )
        }
    }
}

@Composable
private fun WorkoutsHeader(
    onBack: () -> Unit,
    onCalendarClick: () -> Unit,
    onFilterClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(34.dp)
                .background(cs.surfaceVariant.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.wv2_back),
                tint = cs.onSurface,
                modifier = Modifier.size(17.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.wv2_title),
                color = cs.onBackground,
                fontSize = 22.sp,
                lineHeight = 25.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.wv2_subtitle_short),
                color = cs.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        IconButton(
            onClick = onCalendarClick,
            modifier = Modifier
                .size(34.dp)
                .background(cs.primary.copy(alpha = 0.16f), RoundedCornerShape(999.dp))
        ) {
            Icon(
                imageVector = Icons.Outlined.CalendarToday,
                contentDescription = stringResource(R.string.wv2_calendar),
                tint = cs.primary,
                modifier = Modifier.size(17.dp)
            )
        }

        IconButton(
            onClick = onFilterClick,
            modifier = Modifier
                .size(34.dp)
                .background(cs.surfaceVariant.copy(alpha = 0.34f), RoundedCornerShape(999.dp))
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(R.string.wv2_filter),
                tint = cs.onSurfaceVariant,
                modifier = Modifier.size(17.dp)
            )
        }
    }
}

@Composable
private fun workoutsPageBrush(
    isDark: Boolean,
    cs: androidx.compose.material3.ColorScheme
): Brush {
    return if (isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF05070A),
                Color(0xFF080C10),
                Color(0xFF05070A)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFF6FAFC),
                Color(0xFFEFF6F8),
                Color(0xFFF8FBFD)
            )
        )
    }
}
