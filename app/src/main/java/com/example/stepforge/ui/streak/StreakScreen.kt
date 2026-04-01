@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stepforge.ui.streak

import android.content.Intent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.ShowChart
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import com.example.stepforge.SettingsActivity
import com.example.stepforge.ui.components.PremiumGate
import com.example.stepforge.ui.insights.rememberPremiumEnabled

@Composable
fun StreakScreen(
    onBack: () -> Unit,
) {
    val ctx = LocalContext.current
    val vm = remember { StreakViewModel(ctx.applicationContext) }
    val state by vm.ui.collectAsState()

    LaunchedEffect(Unit) {
        vm.load()
    }

    StreakScreenContent(
        state = state,
        onBack = onBack
    )
}

@Composable
private fun StreakScreenContent(
    state: StreakUiState,
    onBack: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val neon = Brush.horizontalGradient(listOf(Color(0xFF00FFA3), Color(0xFF00F5FF)))
    val surface = cs.surface
    val cardShadow = if (isDark) 14.dp else 10.dp
    val border = cs.outline.copy(alpha = if (isDark) 0.10f else 0.12f)

    val ctx = LocalContext.current
    val premiumEnabled = rememberPremiumEnabled(ctx)

    val riskNoteText = when (state.streakRiskNoteType) {
        RiskNoteType.NOT_ENOUGH_DATA -> stringResource(R.string.risk_note_not_enough_data)
        RiskNoteType.GOAL_SAFE -> stringResource(R.string.risk_note_goal_safe)
        RiskNoteType.HIGH_DROP -> stringResource(
            R.string.risk_note_high,
            state.streakRiskDropPercent
        )
        RiskNoteType.MEDIUM_DROP -> stringResource(
            R.string.risk_note_medium,
            state.streakRiskDropPercent
        )
        RiskNoteType.LOW_RISK -> stringResource(R.string.risk_note_low)
    }

    val predictionNoteText = when (state.goalPredictionNoteType) {
        PredictionNoteType.GOAL_NOT_SET -> stringResource(R.string.prediction_note_goal_not_set)
        PredictionNoteType.GOAL_REACHED -> stringResource(R.string.prediction_note_goal_reached)
        PredictionNoteType.ON_TRACK -> stringResource(R.string.prediction_note_on_track)
        PredictionNoteType.POSSIBLE -> stringResource(R.string.prediction_note_possible)
        PredictionNoteType.BEHIND -> stringResource(R.string.prediction_note_behind)
        PredictionNoteType.RISK -> stringResource(R.string.prediction_note_risk)
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.streak_analytics), fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (premiumEnabled) stringResource(R.string.premium_enabled)
                            else stringResource(R.string.premium_preview),
                            fontSize = 11.sp,
                            color = if (premiumEnabled) Color(0xFF00FFA3) else cs.onSurface.copy(alpha = 0.6f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { ctx.startActivity(Intent(ctx, SettingsActivity::class.java)) }
                    ) {
                        Icon(
                            imageVector = if (premiumEnabled) Icons.Outlined.Info else Icons.Outlined.Lock,
                            contentDescription = "Premium",
                            tint = if (premiumEnabled) Color(0xFF00FFA3) else cs.onSurface.copy(alpha = 0.9f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { pad ->

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(pad)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 32.dp)
        ) {

            item(key = "hero") {
                ElevatedPremiumCard(shadow = cardShadow, border = border, bg = surface) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                stringResource(R.string.current_streak),
                                fontSize = 12.sp,
                                color = cs.onSurface.copy(alpha = 0.65f)
                            )
                            Text(
                                text = "${state.currentStreakDays}",
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00FFA3)
                            )
                            Text(
                                text = "${stringResource(R.string.days)} • ${stringResource(R.string.goal)} ${formatNumber(state.goalSteps)}",
                                fontSize = 12.sp,
                                color = cs.onSurface.copy(alpha = 0.75f)
                            )

                            val sub = if (state.todayCompletedGoal) {
                                stringResource(R.string.goal_completed_today)
                            } else {
                                stringResource(R.string.not_completed_yet)
                            }

                            Text(
                                text = sub,
                                fontSize = 12.sp,
                                color = if (state.todayCompletedGoal) Color(0xFF00F5FF)
                                else cs.onSurface.copy(alpha = 0.6f)
                            )
                        }

                        RingMini(
                            progress = (state.todaySteps.toFloat() / state.goalSteps.coerceAtLeast(1)).coerceIn(0f, 1f),
                            neon = neon,
                            label = stringResource(R.string.today),
                            value = formatNumber(state.todaySteps)
                        )
                    }
                }
            }

            item(key = "basicRow") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        MiniMetricCard(
                            title = stringResource(R.string.longest_streak),
                            value = stringResource(R.string.days_suffix, state.longestStreakDays),
                            icon = Icons.Outlined.ShowChart,
                            shadow = cardShadow,
                            border = border
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        MiniMetricCard(
                            title = stringResource(R.string.perfect_threshold),
                            value = formatNumber(state.perfectDayThresholdSteps),
                            icon = Icons.Outlined.Bolt,
                            shadow = cardShadow,
                            border = border
                        )
                    }
                }
            }

            item(key = "shieldStatus") {
                ShieldStatusCard(
                    todayShieldMinutesLeft = state.shieldTodayMinutesLeft,
                    todayShieldMaxMinutes = state.shieldTodayMaxMinutes,
                    tomorrowBaseHours = state.shieldTomorrowBaseHours,
                    tomorrowGoalBonusHours = state.shieldTomorrowGoalBonusHours,
                    tomorrowFinalHours = state.shieldTomorrowFinalHours,
                    isPremium = state.isPremium,
                    shadow = cardShadow,
                    border = border
                )
            }

            if (state.isPremium) {
                item(key = "premiumRescue") {
                    PremiumRescueCard(
                        rescuesLeft = state.premiumRescuesLeft,
                        enabled = state.premiumAutoRescueEnabled,
                        shadow = cardShadow,
                        border = border
                    )
                }
            }

            item(key = "premiumGate") {
                PremiumGate(
                    premiumEnabled = premiumEnabled,
                    title = stringResource(R.string.premium_analytics),
                    subtitle = stringResource(R.string.premium_analytics_subtitle),
                    onUnlockClick = { ctx.startActivity(Intent(ctx, SettingsActivity::class.java)) }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        ElevatedPremiumCard(shadow = cardShadow, border = border, bg = surface) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(18.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                Color(0xFF00F5FF).copy(alpha = 0.15f),
                                                CircleShape
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Outlined.ShowChart,
                                            null,
                                            tint = Color(0xFF00F5FF),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Text(
                                        stringResource(R.string.weekly_analysis),
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp
                                    )
                                }

                                Row(
                                    Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricLine(stringResource(R.string.avg_day), formatNumber(state.weeklyAverageSteps))
                                    MetricLine(stringResource(R.string.goal_hit), "${state.weeklyGoalCompletionRate}%")
                                    MetricLine(
                                        stringResource(R.string.trend),
                                        "${state.weeklyTrendPercent}%",
                                        isTrend = true,
                                        trendUp = state.weeklyTrendLabel == "Up"
                                    )
                                }

                                StepsMiniBars(data = state.last7Steps, neon = neon)

                                val trendWord = when (state.weeklyTrendLabel) {
                                    "Up" -> stringResource(R.string.trend_up)
                                    "Down" -> stringResource(R.string.trend_down)
                                    "Stable" -> stringResource(R.string.trend_stable)
                                    else -> stringResource(R.string.trend_new)
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(cs.onSurface.copy(alpha = 0.05f))
                                        .padding(14.dp)
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.activity_insight,
                                            trendWord,
                                            state.weeklyTrendPercent
                                        ),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = cs.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }

                        ElevatedPremiumCard(shadow = cardShadow, border = border, bg = surface) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.monthly_heatmap),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = cs.onSurface
                                )

                                Heatmap30(
                                    cells = state.last30Heat,
                                    accent = Color(0xFF00FFA3)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    MetricLine(stringResource(R.string.active), "${state.monthlyActiveDays}/30")
                                    MetricLine(stringResource(R.string.goal_hit), "${state.monthlyGoalCompletedDays}/30")
                                    MetricLine(stringResource(R.string.perfect), "${state.perfectDaysLast30}/30")
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(modifier = Modifier.weight(1f)) {
                                ScoreCard(
                                    title = stringResource(R.string.consistency),
                                    score = state.consistencyScore,
                                    note = stringResource(R.string.consistency_note),
                                    shadow = cardShadow,
                                    border = border
                                )
                            }
                            Box(modifier = Modifier.weight(1f)) {
                                ScoreCard(
                                    title = stringResource(R.string.activity),
                                    score = state.activityScore,
                                    note = stringResource(R.string.activity_note),
                                    shadow = cardShadow,
                                    border = border
                                )
                            }
                        }

                        ElevatedPremiumCard(shadow = cardShadow, border = border, bg = surface) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.activity_behavior),
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 18.sp,
                                    color = cs.onSurface
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        BehaviorMetricBox(
                                            modifier = Modifier.weight(1f),
                                            label = stringResource(R.string.peak_hour),
                                            value = state.peakHourLabel.replace(" - ", "—"),
                                            icon = Icons.Outlined.AccessTime,
                                            iconColor = Color(0xFF00F5FF)
                                        )
                                        BehaviorMetricBox(
                                            modifier = Modifier.weight(1f),
                                            label = stringResource(R.string.most_active),
                                            value = when (state.mostActiveDayLabel) {
                                                "Mon" -> "Monday"
                                                "Tue" -> "Tuesday"
                                                "Wed" -> "Wednesday"
                                                "Thu" -> "Thursday"
                                                "Fri" -> "Friday"
                                                "Sat" -> "Saturday"
                                                "Sun" -> "Sunday"
                                                else -> state.mostActiveDayLabel
                                            },
                                            icon = Icons.Outlined.CalendarToday,
                                            iconColor = Color(0xFF00FFA3)
                                        )
                                    }

                                    val rhythmLabel = if (state.peakHourLabel.contains("PM")) {
                                        stringResource(R.string.activity_rhythm_evening)
                                    } else {
                                        stringResource(R.string.activity_rhythm_daytime)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(cs.onSurface.copy(alpha = 0.04f))
                                            .padding(16.dp)
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            Icon(
                                                Icons.Outlined.Info,
                                                null,
                                                tint = Color(0xFF00F5FF).copy(alpha = 0.7f),
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Text(
                                                text = stringResource(R.string.activity_behavior_note, rhythmLabel),
                                                fontSize = 12.sp,
                                                color = cs.onSurface.copy(alpha = 0.6f),
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ElevatedPremiumCard(shadow = cardShadow, border = border, bg = surface) {
                            Column(
                                Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Outlined.WarningAmber,
                                        contentDescription = null,
                                        tint = riskColor(state.streakRiskLevel)
                                    )
                                    Text(stringResource(R.string.streak_risk), fontWeight = FontWeight.SemiBold)
                                }

                                Text(
                                    text = riskNoteText,
                                    color = cs.onSurface.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(stringResource(R.string.goal_prediction), fontWeight = FontWeight.SemiBold)
                                PredictionBar(
                                    chance = state.goalPredictionChance,
                                    neon = neon,
                                    note = predictionNoteText
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ElevatedPremiumCard(
    shadow: androidx.compose.ui.unit.Dp,
    border: Color,
    bg: Color,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(shadow, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, border, RoundedCornerShape(24.dp)),
        color = bg
    ) { content() }
}

@Composable
private fun MiniMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    shadow: androidx.compose.ui.unit.Dp,
    border: Color,
) {
    val cs = MaterialTheme.colorScheme
    ElevatedPremiumCard(shadow = shadow, border = border, bg = cs.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 100.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color(0xFF00F5FF),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    color = cs.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium
                )
            }

            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )
        }
    }
}

@Composable
private fun MetricLine(label: String, value: String, isTrend: Boolean = false, trendUp: Boolean = true) {
    val cs = MaterialTheme.colorScheme
    Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label.uppercase(),
            fontSize = 9.sp,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp,
            color = cs.onSurface.copy(alpha = 0.45f)
        )
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = value,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onSurface
            )
            if (isTrend) {
                val trendColor = if (trendUp) Color(0xFF00FFA3) else Color(0xFFFF4D6D)
                Icon(
                    imageVector = if (trendUp) Icons.AutoMirrored.Filled.TrendingUp else Icons.AutoMirrored.Filled.TrendingDown,
                    contentDescription = null,
                    tint = trendColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun RingMini(
    progress: Float,
    neon: Brush,
    label: String,
    value: String,
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(104.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = 10f
            val radius = size.minDimension / 2f - stroke
            drawArc(
                color = cs.onSurface.copy(alpha = 0.08f),
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
            )
            drawArc(
                brush = neon,
                startAngle = -90f,
                sweepAngle = 360f * progress.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
                size = Size(radius * 2, radius * 2),
                topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 10.sp, color = cs.onSurface.copy(alpha = 0.65f))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StepsMiniBars(
    data: List<DayPoint>,
    neon: Brush,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val trackColor = if (isDark) Color(0xFF1A1F26) else Color(0xFFE9ECEF)
    val maxSteps = (data.maxOfOrNull { it.steps } ?: 0).coerceAtLeast(1)
    val days = listOf("M", "T", "W", "T", "F", "S", "S")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp)
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        data.forEachIndexed { index, p ->
            val targetRatio = (p.steps.toFloat() / maxSteps.toFloat()).coerceIn(0.1f, 1f)
            val animatedRatio by animateFloatAsState(
                targetValue = targetRatio,
                animationSpec = tween(1000, delayMillis = index * 40, easing = FastOutSlowInEasing),
                label = "barHeight"
            )

            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (p.steps >= 1000) "${(p.steps / 1000)}k" else "${p.steps}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (p.hitGoal) Color(0xFF00FFA3) else cs.onSurface.copy(alpha = 0.4f)
                )

                Spacer(Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(trackColor)
                        .border(1.dp, cs.onSurface.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(animatedRatio)
                            .background(
                                if (p.hitGoal) neon
                                else Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF374151),
                                        Color(0xFF111827)
                                    )
                                )
                            )
                    )

                    if (p.hitGoal) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            Color.White.copy(0.2f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    text = days.getOrNull(index % 7) ?: "",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    color = if (p.hitGoal) Color(0xFF00F5FF) else cs.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun Heatmap30(
    cells: List<HeatCell>,
    accent: Color,
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val premiumGreen = if (isDark) accent else Color(0xFF22C55E)
    val emptyCellColor = if (isDark) Color(0xFF1A1F26) else Color(0xFFF3F4F6)

    val cols = 6
    val rows = (cells.size / cols).coerceAtLeast(1)
    val spacing = 8.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(spacing)) {
            repeat(rows) { r ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(cols) { c ->
                        val idx = r * cols + c
                        if (idx < cells.size) {
                            val cell = cells[idx]

                            val cellBg = when (cell.level) {
                                0 -> emptyCellColor
                                1 -> premiumGreen.copy(alpha = if (isDark) 0.2f else 0.15f)
                                2 -> premiumGreen.copy(alpha = if (isDark) 0.5f else 0.40f)
                                3 -> premiumGreen.copy(alpha = if (isDark) 0.8f else 0.70f)
                                else -> premiumGreen
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(cellBg)
                                    .then(
                                        if (cell.isPerfect) Modifier.border(
                                            1.5.dp,
                                            if (isDark) Color(0xFF00F5FF) else Color(0xFF0EA5E9),
                                            RoundedCornerShape(6.dp)
                                        ) else Modifier
                                    )
                            )
                        } else {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    stringResource(R.string.less),
                    fontSize = 10.sp,
                    color = cs.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(0.15f, 0.4f, 0.7f, 1f).forEach { opacity ->
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(premiumGreen.copy(alpha = opacity))
                        )
                    }
                }
                Text(
                    stringResource(R.string.more),
                    fontSize = 10.sp,
                    color = cs.onSurface.copy(alpha = 0.4f),
                    fontWeight = FontWeight.Bold
                )
            }

            if (cells.any { it.isPerfect }) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(
                        Modifier
                            .size(10.dp)
                            .border(
                                1.2.dp,
                                if (isDark) Color(0xFF00F5FF) else Color(0xFF0EA5E9),
                                CircleShape
                            )
                    )
                    Text(
                        stringResource(R.string.perfect_day),
                        fontSize = 10.sp,
                        color = cs.onSurface.copy(alpha = 0.5f),
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun ScoreCard(
    title: String,
    score: Int,
    note: String,
    shadow: androidx.compose.ui.unit.Dp,
    border: Color,
) {
    val cs = MaterialTheme.colorScheme
    ElevatedPremiumCard(shadow = shadow, border = border, bg = cs.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 220.dp)
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    title,
                    fontSize = 12.sp,
                    color = cs.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
                Text(
                    "$score/100",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFA3)
                )
            }

            Text(
                note,
                fontSize = 11.sp,
                color = cs.onSurface.copy(alpha = 0.65f),
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun PredictionBar(
    chance: Int,
    neon: Brush,
    note: String,
) {
    val cs = MaterialTheme.colorScheme
    val v = chance.coerceIn(0, 100)
    val barBg = cs.onSurface.copy(alpha = 0.08f)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.chance), fontSize = 11.sp, color = cs.onSurface.copy(alpha = 0.65f))
            Text("$v%", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00F5FF))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(barBg)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth((v / 100f).coerceIn(0.02f, 1f))
                    .height(10.dp)
                    .background(neon)
            )
        }

        AnimatedContent(
            targetState = note,
            transitionSpec = { fadeIn() togetherWith fadeOut() },
            label = "predictionNote"
        ) { s ->
            Text(
                text = s,
                fontSize = 12.sp,
                color = cs.onSurface.copy(alpha = 0.75f),
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun BehaviorMetricBox(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
) {
    val cs = MaterialTheme.colorScheme
    val fontSize = if (value.length > 8) 14.sp else 18.sp

    Column(
        modifier = modifier
            .height(115.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cs.onSurface.copy(alpha = 0.04f))
            .border(1.dp, cs.onSurface.copy(alpha = 0.03f), RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // ✅ Başlık alanı sabit yükseklik
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 34.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier
                    .size(15.dp)
                    .padding(top = 1.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.8.sp,
                color = cs.onSurface.copy(alpha = 0.45f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 12.sp,
                modifier = Modifier.weight(1f)
            )
        }

        Text(
            text = value,
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            color = cs.onSurface,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            lineHeight = if (value.length > 8) 18.sp else 22.sp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private fun riskColor(level: StreakRiskLevel): Color {
    return when (level) {
        StreakRiskLevel.LOW -> Color(0xFF00FFA3)
        StreakRiskLevel.MEDIUM -> Color(0xFFFFB74D)
        StreakRiskLevel.HIGH -> Color(0xFFFF4D6D)
    }
}

private fun formatNumber(v: Int): String {
    return try {
        java.text.NumberFormat.getIntegerInstance().format(v)
    } catch (_: Exception) {
        v.toString()
    }
}

private fun Modifier.debugOutline(color: Color): Modifier = this.then(
    Modifier.drawBehind {
        drawRect(color = color, style = Stroke(width = 2f))
    }
)

@Composable
private fun ShieldStatusCard(
    todayShieldMinutesLeft: Int,
    todayShieldMaxMinutes: Int,
    tomorrowBaseHours: Int,
    tomorrowGoalBonusHours: Int,
    tomorrowFinalHours: Int,
    isPremium: Boolean,
    shadow: androidx.compose.ui.unit.Dp,
    border: Color
) {
    val cs = MaterialTheme.colorScheme

    val totalProgress = if (todayShieldMaxMinutes <= 0) {
        0f
    } else {
        (todayShieldMinutesLeft.toFloat() / todayShieldMaxMinutes.toFloat()).coerceIn(0f, 1f)
    }

    ElevatedPremiumCard(shadow = shadow, border = border, bg = cs.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.streak_shield_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )

            Text(
                text = stringResource(
                    R.string.streak_shield_left_format,
                    todayShieldMinutesLeft / 60,
                    todayShieldMinutesLeft % 60
                ),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00FFA3)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(cs.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(totalProgress.coerceIn(0.02f, 1f))
                        .height(10.dp)
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00FFA3), Color(0xFF00F5FF))
                            )
                        )
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = stringResource(R.string.streak_shield_tomorrow),
                    fontSize = 12.sp,
                    color = cs.onSurface.copy(alpha = 0.65f)
                )

                Text(
                    text = stringResource(R.string.streak_shield_hours_format, tomorrowFinalHours),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )

                Text(
                    text = stringResource(
                        R.string.streak_shield_breakdown_format,
                        tomorrowBaseHours,
                        tomorrowGoalBonusHours
                    ),
                    fontSize = 11.sp,
                    color = cs.onSurface.copy(alpha = 0.72f)
                )

                if (isPremium) {
                    Text(
                        text = stringResource(R.string.streak_shield_premium_cap_active),
                        fontSize = 11.sp,
                        color = Color(0xFF00F5FF)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumRescueCard(
    rescuesLeft: Int,
    enabled: Boolean,
    shadow: androidx.compose.ui.unit.Dp,
    border: Color
) {
    val cs = MaterialTheme.colorScheme

    ElevatedPremiumCard(shadow = shadow, border = border, bg = cs.surface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.premium_rescue_title),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )

            Text(
                text = stringResource(R.string.premium_rescue_left_format, rescuesLeft),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00F5FF)
            )

            Text(
                text = if (enabled) {
                    stringResource(R.string.premium_rescue_auto_enabled)
                } else {
                    stringResource(R.string.premium_rescue_auto_disabled)
                },
                fontSize = 12.sp,
                color = cs.onSurface.copy(alpha = 0.72f)
            )
        }
    }
}