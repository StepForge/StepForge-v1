@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stepforge.ui.insights

import android.content.Context
import android.content.Intent
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.TrendingDown
import androidx.compose.material.icons.outlined.TrendingFlat
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.SettingsActivity
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlin.math.roundToInt
import com.example.stepforge.ui.components.PremiumGateCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InsightsScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val premiumEnabled = rememberPremiumEnabled(ctx)

    var selectedMode by remember { mutableStateOf(InsightsMode.WEEKLY) }
    var showPremiumSheet by remember { mutableStateOf(false) }

    var weeklyInsights by remember { mutableStateOf<PeriodInsights?>(null) }
    var monthlyInsights by remember { mutableStateOf<PeriodInsights?>(null) }

    val neon = Brush.linearGradient(listOf(Color(0xFF00FFA3), Color(0xFF00F5FF)))

    val scope = rememberCoroutineScope()

    var sheet by remember { mutableStateOf<InsightsSheet?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Premium top subtitle anim
    val subtitleAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "subtitleAlpha"
    )

    LaunchedEffect(Unit) {
        weeklyInsights = null
        monthlyInsights = null

        delay(120)
        weeklyInsights = InsightsCalculator.calculateInsights(ctx, InsightsMode.WEEKLY)

        delay(90)
        monthlyInsights = InsightsCalculator.calculateInsights(ctx, InsightsMode.MONTHLY)
    }

    Scaffold(
        containerColor = cs.background,
        topBar = {
            TopAppBar(
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Insights",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp
                        )

                        Text(
                            modifier = Modifier.alpha(subtitleAlpha),
                            text = if (premiumEnabled) "Premium enabled" else "Premium locked",
                            fontSize = 11.sp,
                            color = if (premiumEnabled) Color(0xFF00FFA3)
                            else cs.onSurface.copy(alpha = 0.55f)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            ctx.startActivity(Intent(ctx, SettingsActivity::class.java))
                        }
                    ) {
                        Icon(
                            imageVector = if (premiumEnabled) Icons.Outlined.Info else Icons.Outlined.Lock,
                            contentDescription = "Premium",
                            tint = if (premiumEnabled) Color(0xFF00FFA3) else cs.onSurface.copy(alpha = 0.85f)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = cs.onSurface
                )
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .padding(bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            // Tabs
            PremiumTabs(
                selectedMode = selectedMode,
                premiumEnabled = premiumEnabled,
                onSelect = { selectedMode = it },
                onRequestPremium = { showPremiumSheet = true }
            )


            AnimatedContent(
                targetState = selectedMode,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "modeSwitch"
            ) { mode ->

                val data = if (mode == InsightsMode.WEEKLY) weeklyInsights else monthlyInsights

                if (data == null) {
                    LoadingInsightsCard()
                } else {

                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                        DateRangeCard(
                            startDate = data.startDate,
                            endDate = data.endDate,
                            totalSteps = data.totalSteps,
                            dailyAverage = data.dailyAverage,
                            activeDays = data.activeDays,
                            periodDays = if (mode == InsightsMode.WEEKLY) 7 else 30
                        )

                        HeroChartCard(
                            data = data,
                            neon = neon,
                            onClick = {
                                vibrateSoft(ctx)
                                sheet = InsightsSheet.TrendDetails(data, mode)
                            }
                        )

                        QuickStatsGrid(
                            data = data,
                            onStatClick = { title, value, desc ->
                                vibrateSoft(ctx)
                                sheet = InsightsSheet.StatDetails(title, value, desc)
                            }
                        )

                        PremiumGateCard(
                            premiumEnabled = premiumEnabled,
                            title = "Activity Score",
                            subtitle = "Get a performance score based on consistency and goal success.",
                            onUnlockClick = { showPremiumSheet = true }
                        ) {
                            ActivityScoreCard(
                                data = data,
                                onClick = {
                                    vibrateSoft(ctx)
                                    sheet = InsightsSheet.ActivityScoreInfo(data)
                                }
                            )

                        }

                        PremiumGateCard(
                            premiumEnabled = premiumEnabled,
                            title = "Smart Summary",
                            subtitle = "Unlock premium to see advanced insights and coaching tips.",
                            onUnlockClick = { showPremiumSheet = true }
                        ) {
                            SmartSummaryCard(
                                lines = data.summaryLines,
                                onClick = {
                                    vibrateSoft(ctx)
                                    sheet = InsightsSheet.SummaryDetails(data.summaryLines)
                                }
                            )
                        }




                        Spacer(modifier = Modifier.height(12.dp))
                    }
                    if (showPremiumSheet) {
                        PremiumUnlockSheet(
                            onClose = { showPremiumSheet = false },
                            onEnablePremium = {
                                ctx.startActivity(Intent(ctx, SettingsActivity::class.java))
                                showPremiumSheet = false
                            }
                        )
                    }

                }
            }
        }
    }

    // BottomSheet Details
    if (sheet != null) {
        ModalBottomSheet(
            onDismissRequest = { sheet = null },
            sheetState = sheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 20.dp)
            ) {

                when (val s = sheet) {
                    is InsightsSheet.TrendDetails -> TrendDetailsSheet(
                        data = s.data,
                        mode = s.mode
                    )

                    is InsightsSheet.StatDetails -> StatDetailsSheet(
                        title = s.title,
                        value = s.value,
                        description = s.description
                    )

                    is InsightsSheet.ActivityScoreInfo -> ActivityScoreDetailsSheet(s.data)

                    is InsightsSheet.SummaryDetails -> SummaryDetailsSheet(s.lines)

                    null -> Unit
                }

                Spacer(modifier = Modifier.height(30.dp))
            }
        }
    }
}

/* =========================== SHEET TYPES =========================== */

private sealed class InsightsSheet {
    data class TrendDetails(val data: PeriodInsights, val mode: InsightsMode) : InsightsSheet()
    data class StatDetails(val title: String, val value: String, val description: String) : InsightsSheet()
    data class ActivityScoreInfo(val data: PeriodInsights) : InsightsSheet()
    data class SummaryDetails(val lines: List<String>) : InsightsSheet()
}

/* =========================== PREMIUM TABS =========================== */

@Composable
private fun PremiumTabs(
    selectedMode: InsightsMode,
    premiumEnabled: Boolean,
    onSelect: (InsightsMode) -> Unit,
    onRequestPremium: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cs.surface,
        shadowElevation = 10.dp,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                cs.outline.copy(alpha = 0.08f),
                RoundedCornerShape(18.dp)
            )
    ) {
        TabRow(
            selectedTabIndex = if (selectedMode == InsightsMode.WEEKLY) 0 else 1,
            containerColor = cs.surface,
            indicator = {},
            divider = {}
        ) {

            // WEEKLY
            Tab(
                selected = selectedMode == InsightsMode.WEEKLY,
                onClick = { onSelect(InsightsMode.WEEKLY) },
                text = {
                    Text(
                        text = "Weekly",
                        fontWeight = if (selectedMode == InsightsMode.WEEKLY)
                            FontWeight.Bold
                        else
                            FontWeight.Medium,
                        color = if (selectedMode == InsightsMode.WEEKLY)
                            Color(0xFF00FFA3)
                        else
                            cs.onSurface.copy(alpha = 0.75f)
                    )
                }
            )

            // MONTHLY (PREMIUM LOCK)
            Tab(
                selected = selectedMode == InsightsMode.MONTHLY,
                onClick = {
                    if (premiumEnabled) {
                        onSelect(InsightsMode.MONTHLY)
                    } else {
                        onRequestPremium()
                    }
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Monthly",
                            fontWeight = if (selectedMode == InsightsMode.MONTHLY)
                                FontWeight.Bold
                            else
                                FontWeight.Medium,
                            color = if (selectedMode == InsightsMode.MONTHLY)
                                Color(0xFF00FFA3)
                            else
                                cs.onSurface.copy(alpha = 0.75f)
                        )

                        if (!premiumEnabled) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = cs.onSurface.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            )
        }
    }
}



/* =========================== LOADING =========================== */

@Composable
private fun LoadingInsightsCard() {
    val cs = MaterialTheme.colorScheme

    val pulse = remember { Animatable(0.35f) }

    LaunchedEffect(Unit) {
        while (true) {
            pulse.animateTo(
                0.75f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
            pulse.animateTo(
                0.35f,
                animationSpec = tween(600, easing = FastOutSlowInEasing)
            )
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cs.surface,
        shadowElevation = 12.dp
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Loading insights...", fontWeight = FontWeight.SemiBold)
            Text(
                "Analyzing your activity data...",
                color = cs.onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(cs.onSurface.copy(alpha = 0.06f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(pulse.value)
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00FFA3), Color(0xFF00F5FF))
                            )
                        )
                )
            }
        }
    }
}

/* =========================== DATE RANGE CARD =========================== */

@Composable
private fun DateRangeCard(
    startDate: String,
    endDate: String,
    totalSteps: Int,
    dailyAverage: Int,
    activeDays: Int,
    periodDays: Int
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cs.surface,
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFF00FFA3),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "$startDate  →  $endDate",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${formatNumber(totalSteps)} steps • Avg ${formatNumber(dailyAverage)} • $activeDays/$periodDays active",
                        fontSize = 11.sp,
                        color = cs.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

/* =========================== HERO CHART CARD =========================== */

@Composable
private fun HeroChartCard(
    data: PeriodInsights,
    neon: Brush,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val trendIcon = when {
        data.trendLabel == "Up" -> Icons.Outlined.TrendingUp
        data.trendLabel == "Down" -> Icons.Outlined.TrendingDown
        else -> Icons.Outlined.TrendingFlat
    }

    val trendColor = when {
        data.trendLabel == "Up" -> Color(0xFF00FFA3)
        data.trendLabel == "Down" -> Color(0xFFFF4D6D)
        else -> cs.onSurface.copy(alpha = 0.7f)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(16.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick()
            },
        shape = RoundedCornerShape(24.dp),
        color = cs.surface
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.surface.copy(alpha = 1f),
                            cs.surfaceVariant.copy(alpha = if (isDark) 0.6f else 0.85f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Color(0xFF00FFA3).copy(alpha = 0.10f),
                    RoundedCornerShape(24.dp)
                )
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.BarChart,
                            contentDescription = null,
                            tint = Color(0xFF00FFA3),
                            modifier = Modifier.size(18.dp)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Column {
                            Text(
                                text = "Steps Trend",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Tap to explore details",
                                fontSize = 10.sp,
                                color = cs.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = trendIcon,
                            contentDescription = null,
                            tint = trendColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        val t = if (data.trendLabel == "New") {
                            "New"
                        } else {
                            if (data.trendPercent > 0) "+${data.trendPercent}%" else "${data.trendPercent}%"
                        }

                        Text(
                            text = t,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = trendColor
                        )
                    }
                }

                StepsBarChart(
                    chartData = data.chartData,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    neon = neon,
                    isDark = isDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    MiniInfo("Best", "${data.bestDayLabel} • ${formatNumber(data.bestDaySteps)}")
                    MiniInfo("Worst", "${data.worstDayLabel} • ${formatNumber(data.worstDaySteps)}")
                    MiniInfo("Peak", data.peakHourLabel)
                }
            }
        }
    }
}


/* =========================== BAR CHART =========================== */

@Composable
private fun StepsBarChart(
    chartData: List<DayStepEntry>,
    modifier: Modifier,
    neon: Brush,
    isDark: Boolean
) {
    val max = chartData.maxOfOrNull { it.steps }?.coerceAtLeast(1) ?: 1

    val baseBarColor = if (isDark)
        Color(0xFF20293A)
    else
        Color.Black.copy(alpha = 0.06f)

    Canvas(modifier = modifier) {
        val barCount = chartData.size.coerceAtLeast(1)
        val gap = 10f
        val totalGap = gap * (barCount - 1)
        val barWidth = (size.width - totalGap) / barCount

        chartData.forEachIndexed { index, entry ->
            val ratio = entry.steps.toFloat() / max.toFloat()
            val barHeight = (size.height * ratio).coerceAtLeast(6f)

            val x = index * (barWidth + gap)
            val y = size.height - barHeight

            // background bar
            drawRoundRect(
                color = baseBarColor,
                topLeft = Offset(x, 0f),
                size = Size(barWidth, size.height),
                cornerRadius = CornerRadius(18f, 18f)
            )

            // active bar
            drawRoundRect(
                brush = neon,
                topLeft = Offset(x, y),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(18f, 18f)
            )
        }
    }
}


/* =========================== MINI INFO =========================== */

@Composable
private fun MiniInfo(label: String, value: String) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            fontSize = 10.sp,
            color = cs.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = cs.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/* =========================== QUICK STATS GRID =========================== */

@Composable
private fun QuickStatsGrid(
    data: PeriodInsights,
    onStatClick: (String, String, String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Total Steps",
                value = formatNumber(data.totalSteps),
                desc = "Total amount of steps you walked in this period.",
                modifier = Modifier.weight(1f),
                onClick = onStatClick
            )
            StatCard(
                title = "Daily Avg",
                value = formatNumber(data.dailyAverage),
                desc = "Average steps per day. Great for tracking long-term consistency.",
                modifier = Modifier.weight(1f),
                onClick = onStatClick
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            StatCard(
                title = "Active Days",
                value = data.activeDays.toString(),
                desc = "Days where you reached a minimum activity threshold.",
                modifier = Modifier.weight(1f),
                onClick = onStatClick
            )
            StatCard(
                title = "Consistency",
                value = "${data.consistencyScore}/100",
                desc = "Measures how stable your activity is across the period.",
                modifier = Modifier.weight(1f),
                onClick = onStatClick
            )
        }
    }
}

@Composable
private fun StatCard(
    title: String,
    value: String,
    desc: String,
    modifier: Modifier = Modifier,
    onClick: (String, String, String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val pressAnim = remember { Animatable(1f) }

    Surface(
        modifier = modifier
            .heightIn(min = 76.dp)
            .shadow(12.dp, RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick(title, value, desc)
            },
        shape = RoundedCornerShape(18.dp),
        color = cs.surface
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.surface.copy(alpha = 1f),
                            cs.surfaceVariant.copy(alpha = 0.55f)
                        )
                    )
                )
                .border(
                    1.dp,
                    Color.White.copy(alpha = 0.04f),
                    RoundedCornerShape(18.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    color = cs.onSurface.copy(alpha = 0.65f)
                )
                Text(
                    text = value,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )
                Text(
                    text = "Tap for details",
                    fontSize = 10.sp,
                    color = Color(0xFF00FFA3).copy(alpha = 0.8f)
                )
            }
        }
    }
}

/* =========================== ACTIVITY SCORE =========================== */

@Composable
private fun ActivityScoreCard(
    data: PeriodInsights,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    val score = ((data.consistencyScore * 0.55f) +
            ((data.goalSuccess.coerceAtMost(30) / 30f) * 45f)).roundToInt()
        .coerceIn(0, 100)

    val scoreLabel = when {
        score >= 85 -> "Elite"
        score >= 70 -> "Strong"
        score >= 50 -> "Average"
        score >= 30 -> "Low"
        else -> "Very Low"
    }

    val progressAnim by animateFloatAsState(
        targetValue = score / 100f,
        animationSpec = tween(850, easing = FastOutSlowInEasing),
        label = "scoreProgress"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(22.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick()
            }
            .border(
                1.dp,
                cs.outline.copy(alpha = 0.12f),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.surface.copy(alpha = 1f),
                            cs.surfaceVariant.copy(alpha = 0.50f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Activity Score",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "$score",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFA3)
                )

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = scoreLabel,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Consistency + Goal success",
                        fontSize = 11.sp,
                        color = cs.onSurface.copy(alpha = 0.65f)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(9.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(cs.onSurface.copy(alpha = 0.08f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progressAnim.coerceIn(0.02f, 1f))
                        .height(9.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF00FFA3), Color(0xFF00F5FF))
                            )
                        )
                )
            }

            Text(
                text = "Peak Hour: ${data.peakHourLabel} • Active days: ${data.activeDays}",
                fontSize = 12.sp,
                color = cs.onSurface.copy(alpha = 0.75f)
            )

            Text(
                text = "Tap to learn how this score is calculated",
                fontSize = 10.sp,
                color = Color(0xFF00FFA3).copy(alpha = 0.85f)
            )
        }
    }
}


/* =========================== SMART SUMMARY =========================== */

@Composable
private fun SmartSummaryCard(
    lines: List<String>,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(22.dp, RoundedCornerShape(24.dp))
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) {
                onClick()
            }
            .border(
                1.dp,
                cs.outline.copy(alpha = 0.12f),
                RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        color = cs.surface,
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .background(
                    Brush.verticalGradient(
                        listOf(
                            cs.surface.copy(alpha = 1f),
                            cs.surfaceVariant.copy(alpha = 0.50f)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Smart Summary",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            lines.take(4).forEach { line ->
                Text(
                    text = "• $line",
                    fontSize = 12.sp,
                    color = cs.onSurface.copy(alpha = 0.88f),
                    lineHeight = 16.sp
                )
            }

            Text(
                text = "Tap to view full summary",
                fontSize = 10.sp,
                color = Color(0xFF00FFA3).copy(alpha = 0.85f)
            )
        }
    }
}


/* =========================== DETAILS SHEETS =========================== */

@Composable
private fun TrendDetailsSheet(
    data: PeriodInsights,
    mode: InsightsMode
) {
    val cs = MaterialTheme.colorScheme

    val tipsWeekly = listOf(
        "Try to stay active earlier in the day for better consistency.",
        "A short 15-minute walk daily is more powerful than one big day.",
        "Consistency beats intensity. Small daily wins matter most.",
        "Try adding a quick evening walk after dinner.",
        "Set a small goal and increase it gradually every week."
    )

    val tipsMonthly = listOf(
        "Your monthly progress is shaped by habits, not motivation. Build routines.",
        "If your steps drop mid-month, try planning 2 active days ahead.",
        "Try increasing your daily average by just +300 steps this month.",
        "Consistency streaks create the strongest long-term improvement.",
        "Monthly performance improves when weekends stay active too."
    )

    val tipText = remember(mode) {
        if (mode == InsightsMode.WEEKLY) tipsWeekly.random() else tipsMonthly.random()
    }

    val bestColor = Color(0xFF00FFA3)
    val worstColor = Color(0xFFFF4D6D)

    val periodTitle = if (mode == InsightsMode.WEEKLY) "Last 7 days" else "Last 30 days"

    val bestIndex = remember(data.chartData) {
        data.chartData.indexOfFirst { dayShortName(it.date) == data.bestDayLabel }
            .takeIf { it >= 0 } ?: 0
    }

    val worstIndex = remember(data.chartData) {
        data.chartData.indexOfFirst { dayShortName(it.date) == data.worstDayLabel }
            .takeIf { it >= 0 } ?: 0
    }


    val bestDateText = remember(mode, bestIndex, data.startDate) {
        if (mode == InsightsMode.MONTHLY) formatDateWithOffset(data.startDate, bestIndex) else null
    }

    val worstDateText = remember(mode, worstIndex, data.startDate) {
        if (mode == InsightsMode.MONTHLY) formatDateWithOffset(data.startDate, worstIndex) else null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {

        Text(
            text = "Steps Trend Details",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface
        )

        Text(
            text = "This breakdown highlights your strongest and weakest days over the $periodTitle.",
            fontSize = 13.sp,
            color = cs.onSurface.copy(alpha = 0.72f),
            lineHeight = 18.sp
        )

        // BEST / WORST CARDS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = cs.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Best Day",
                        fontSize = 11.sp,
                        color = cs.onSurface.copy(alpha = 0.65f)
                    )

                    Text(
                        text = data.bestDayLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )

                    if (bestDateText != null) {
                        Text(
                            text = bestDateText,
                            fontSize = 11.sp,
                            color = cs.onSurface.copy(alpha = 0.55f)
                        )
                    }

                    Text(
                        text = "${formatNumber(data.bestDaySteps)} steps",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = bestColor
                    )

                    ProgressBarPremium(
                        ratio = 0.92f,
                        accent = bestColor
                    )
                }
            }

            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp),
                color = cs.surfaceVariant.copy(alpha = 0.35f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Worst Day",
                        fontSize = 11.sp,
                        color = cs.onSurface.copy(alpha = 0.65f)
                    )

                    Text(
                        text = data.worstDayLabel,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )

                    if (worstDateText != null) {
                        Text(
                            text = worstDateText,
                            fontSize = 11.sp,
                            color = cs.onSurface.copy(alpha = 0.55f)
                        )
                    }

                    Text(
                        text = "${formatNumber(data.worstDaySteps)} steps",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = worstColor
                    )

                    ProgressBarPremium(
                        ratio = 0.40f,
                        accent = worstColor
                    )
                }
            }
        }

        // PEAK HOUR CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = cs.surfaceVariant.copy(alpha = 0.35f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Peak Hour",
                    fontSize = 11.sp,
                    color = cs.onSurface.copy(alpha = 0.65f)
                )

                Text(
                    text = data.peakHourLabel,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface
                )

                Text(
                    text = "This is the time range where you were most active in this period.",
                    fontSize = 13.sp,
                    color = cs.onSurface.copy(alpha = 0.72f),
                    lineHeight = 18.sp
                )
            }
        }

        // MONTHLY PREMIUM EXTRAS
        if (mode == InsightsMode.MONTHLY) {

            val weeklyTotals = remember(data.chartData) {
                calculateWeekBuckets(data.chartData)
            }

            val maxWeek = weeklyTotals.maxOrNull()?.coerceAtLeast(1) ?: 1
            val bestWeekIndex = weeklyTotals.indexOf(maxWeek)

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cs.surfaceVariant.copy(alpha = 0.30f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {

                    Text(
                        text = "Monthly Breakdown",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )

                    Text(
                        text = "Premium view splits your month into weekly performance blocks.",
                        fontSize = 12.sp,
                        color = cs.onSurface.copy(alpha = 0.72f),
                        lineHeight = 16.sp
                    )

                    weeklyTotals.forEachIndexed { index, total ->
                        val ratio = (total.toFloat() / maxWeek.toFloat()).coerceIn(0f, 1f)

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = weekLabel(index),
                                    fontSize = 11.sp,
                                    color = cs.onSurface.copy(alpha = 0.70f)
                                )

                                Text(
                                    text = "${formatNumber(total)} steps",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (index == bestWeekIndex) Color(0xFF00FFA3)
                                    else cs.onSurface.copy(alpha = 0.75f)
                                )
                            }

                            ProgressBarPremium(
                                ratio = ratio,
                                accent = Color(0xFF00FFA3)
                            )
                        }
                    }

                    Text(
                        text = "Best Week: ${weekLabel(bestWeekIndex)} • ${formatNumber(maxWeek)} steps",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF00FFA3)
                    )
                }
            }

            // HEATMAP (NEW PREMIUM REAL)
            HeatmapCard(
                chartData = data.chartData,
                startDate = data.startDate,
                title = "Consistency Heatmap",
                subtitle = "A premium view of how consistent your activity was across the month."
            )

            val streakInfo = remember(data.chartData) {
                calculateStreakInfo(data.chartData)
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp),
                color = cs.surfaceVariant.copy(alpha = 0.30f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Streak System",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = cs.onSurface
                    )

                    Text(
                        text = "Premium tracking of your daily momentum and habits.",
                        fontSize = 12.sp,
                        color = cs.onSurface.copy(alpha = 0.72f),
                        lineHeight = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StreakMiniCard(
                            title = "Current Streak",
                            value = "${streakInfo.currentStreak} days",
                            accent = Color(0xFF00FFA3),
                            modifier = Modifier.weight(1f)
                        )

                        StreakMiniCard(
                            title = "Best Streak",
                            value = "${streakInfo.bestStreak} days",
                            accent = Color(0xFF00F5FF),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Text(
                        text = streakInfo.message,
                        fontSize = 12.sp,
                        color = cs.onSurface.copy(alpha = 0.75f),
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // TIP CARD
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            color = Color(0xFF00FFA3).copy(alpha = 0.10f)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Tip",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00FFA3)
                )

                Text(
                    text = tipText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = cs.onSurface.copy(alpha = 0.90f),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
    }
}



private fun calculateWeekBuckets(chartData: List<DayStepEntry>): List<Int> {
    if (chartData.isEmpty()) return listOf(0, 0, 0, 0)

    // Monthly is usually 30 days
    val chunks = chartData.chunked(7)

    val sums = chunks.map { week ->
        week.sumOf { it.steps }
    }.toMutableList()

    // Ensure exactly 4 buckets (Premium UI wants 4)
    while (sums.size < 4) sums.add(0)
    if (sums.size > 4) {
        // merge extra chunk into last bucket
        val extra = sums.drop(4).sum()
        val firstFour = sums.take(4).toMutableList()
        firstFour[3] += extra
        return firstFour
    }

    return sums
}

private fun weekLabel(index: Int): String {
    return when (index) {
        0 -> "Week 1"
        1 -> "Week 2"
        2 -> "Week 3"
        else -> "Week 4"
    }
}


@Composable
private fun ProgressBarPremium(
    ratio: Float,
    accent: Color
) {
    val barHeight = 9.dp
    val safeRatio = ratio.coerceIn(0.04f, 1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(barHeight)
            .clip(RoundedCornerShape(999.dp))
            .background(accent.copy(alpha = 0.18f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(safeRatio)
                .height(barHeight)
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
        )
    }
}


@Composable
private fun HeatmapCard(
    chartData: List<DayStepEntry>,
    startDate: String,
    title: String,
    subtitle: String
) {
    val cs = MaterialTheme.colorScheme

    val maxSteps = remember(chartData) {
        chartData.maxOfOrNull { it.steps }?.coerceAtLeast(1) ?: 1
    }

    val parsedStart = remember(startDate) {
        try {
            java.time.LocalDate.parse(startDate) // yyyy-MM-dd
        } catch (_: Exception) {
            java.time.LocalDate.now()
        }
    }

    // Monday=0 ... Sunday=6
    val startOffset = remember(parsedStart) {
        parsedStart.dayOfWeek.value - 1
    }

    val totalCells = remember(chartData, startOffset) {
        startOffset + chartData.size
    }

    val rows = remember(totalCells) {
        (totalCells / 7) + if (totalCells % 7 != 0) 1 else 0
    }

    val daysHeader = listOf("M", "T", "W", "T", "F", "S", "S")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        color = cs.surface,
        shadowElevation = 12.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {

            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = cs.onSurface.copy(alpha = 0.72f),
                lineHeight = 16.sp
            )

            // HEADER ALIGN FIX
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                daysHeader.forEach { d ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = d,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = cs.onSurface.copy(alpha = 0.55f)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (r in 0 until rows) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {

                        for (c in 0 until 7) {
                            val cellIndex = r * 7 + c
                            val dataIndex = cellIndex - startOffset

                            if (dataIndex < 0 || dataIndex >= chartData.size) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.Transparent)
                                )
                            } else {
                                val entry = chartData[dataIndex]

                                val intensity = (entry.steps.toFloat() / maxSteps.toFloat())
                                    .coerceIn(0f, 1f)

                                val cellColor = when {
                                    entry.steps == 0 -> cs.onSurface.copy(alpha = 0.06f)
                                    intensity < 0.25f -> Color(0xFF00FFA3).copy(alpha = 0.25f)
                                    intensity < 0.55f -> Color(0xFF00FFA3).copy(alpha = 0.45f)
                                    intensity < 0.80f -> Color(0xFF00FFA3).copy(alpha = 0.70f)
                                    else -> Color(0xFF00FFA3)
                                }

                                // TEXT COLOR FIX
                                val numberColor = when {
                                    entry.steps == 0 -> cs.onSurface.copy(alpha = 0.35f)
                                    intensity >= 0.80f -> Color.Black.copy(alpha = 0.75f)
                                    intensity >= 0.55f -> Color.Black.copy(alpha = 0.70f)
                                    else -> Color.White.copy(alpha = 0.75f)
                                }

                                val dateText =
                                    parsedStart.plusDays(dataIndex.toLong()).dayOfMonth.toString()

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(cellColor)
                                        .border(
                                            1.dp,
                                            Color.White.copy(alpha = 0.04f),
                                            RoundedCornerShape(10.dp)
                                        ),
                                    contentAlignment = Alignment.TopEnd
                                ) {
                                    Text(
                                        text = dateText,
                                        fontSize = 7.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = numberColor,
                                        modifier = Modifier.padding(top = 0.dp, end = 6.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Text(
                text = "Brighter blocks mean higher activity days.",
                fontSize = 11.sp,
                color = cs.onSurface.copy(alpha = 0.60f)
            )
        }
    }
}






private fun formatDateWithOffset(startDate: String, offsetDays: Int): String {
    return try {
        val start = parseDate(startDate)
        val d = start.plusDays(offsetDays.toLong())

        val month = d.month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)
        val day = d.dayOfMonth.toString().padStart(2, '0')

        "$day $month"
    } catch (e: Exception) {
        ""
    }
}

private fun parseDate(date: String): java.time.LocalDate {
    return java.time.LocalDate.parse(date)
}



@Composable
private fun StreakMiniCard(
    title: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = cs.surface.copy(alpha = 0.35f)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                color = cs.onSurface.copy(alpha = 0.65f)
            )

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = accent
            )
        }
    }
}


private data class StreakInfo(
    val currentStreak: Int,
    val bestStreak: Int,
    val message: String
)

private fun calculateStreakInfo(chartData: List<DayStepEntry>): StreakInfo {
    if (chartData.isEmpty()) {
        return StreakInfo(
            currentStreak = 0,
            bestStreak = 0,
            message = "No activity data available yet."
        )
    }

    // chartData oldest->newest değilse garanti olsun:
    val sorted = chartData

    var best = 0
    var current = 0
    var temp = 0

    for (entry in sorted) {
        if (entry.steps > 0) {
            temp++
            if (temp > best) best = temp
        } else {
            temp = 0
        }
    }

    // current streak = sondan başlayarak say
    for (i in sorted.indices.reversed()) {
        if (sorted[i].steps > 0) current++
        else break
    }

    val msg = when {
        current >= 14 -> "Elite consistency. You're building a real habit."
        current >= 7 -> "Strong momentum. Keep going to reach a 14-day streak."
        current >= 3 -> "Nice streak. You're close to forming a stable routine."
        current == 1 -> "Good start. Try to make tomorrow another active day."
        else -> "No current streak. Start today and build momentum."
    }

    return StreakInfo(
        currentStreak = current,
        bestStreak = best,
        message = msg
    )
}


@Composable
private fun StatDetailsSheet(
    title: String,
    value: String,
    description: String
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = value,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF00FFA3)
        )

        Text(
            text = description,
            fontSize = 13.sp,
            color = cs.onSurface.copy(alpha = 0.75f),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun ActivityScoreDetailsSheet(
    data: PeriodInsights
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "How Activity Score Works",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Your Activity Score is calculated using a mix of consistency and goal success.",
            fontSize = 13.sp,
            color = cs.onSurface.copy(alpha = 0.75f),
            lineHeight = 18.sp
        )

        Text(
            text = "• Consistency Score: ${data.consistencyScore}/100",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "• Goal Success: ${data.goalSuccess} days",
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = "Tip: Improve your score by walking consistently, not only on one or two big days.",
            fontSize = 13.sp,
            color = Color(0xFF00FFA3),
            lineHeight = 18.sp
        )
    }
}

@Composable
private fun SummaryDetailsSheet(
    lines: List<String>
) {
    val cs = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = "Full Smart Summary",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        lines.take(12).forEach { line ->
            Text(
                text = "• $line",
                fontSize = 13.sp,
                color = cs.onSurface.copy(alpha = 0.82f),
                lineHeight = 18.sp
            )
        }
    }
}

/* =========================== PREMIUM STATE FIX =========================== */

@Composable
fun rememberPremiumEnabled(ctx: Context): Boolean {
    val KEY_PREMIUM_ENABLED = intPreferencesKey("premium_enabled")

    val flow = remember {
        ctx.stepforgeStore.data.map { prefs ->
            (prefs[KEY_PREMIUM_ENABLED] ?: 0) == 1
        }
    }

    val enabled by flow.collectAsState(initial = false)
    return enabled
}



/* =========================== VIBRATION =========================== */

private fun vibrateSoft(ctx: Context) {
    try {
        val vib = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            val vm = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vib.vibrate(VibrationEffect.createOneShot(25, 40))
        } else {
            @Suppress("DEPRECATION")
            vib.vibrate(25)
        }
    } catch (_: Exception) {
    }
}

@Composable
private fun PremiumUnlockSheet(
    onClose: () -> Unit,
    onEnablePremium: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onClose,
        containerColor = cs.surface,
        shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(42.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(cs.onSurface.copy(alpha = 0.2f))
            )

            Text(
                text = "Unlock Premium",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = cs.onSurface
            )

            Text(
                text = "Monthly Insights, Activity Score, Smart Summary and advanced analysis are Premium-only features.",
                fontSize = 13.sp,
                color = cs.onSurface.copy(alpha = 0.7f),
                lineHeight = 18.sp
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                color = cs.background
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    PremiumFeatureRow("Monthly Insights + Trend analysis")
                    PremiumFeatureRow("Activity Score system")
                    PremiumFeatureRow("Compare weeks & months")
                    PremiumFeatureRow("Export Weekly Report")
                    PremiumFeatureRow("Smart Summary (AI-style coaching)")
                }
            }

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(999.dp),
                color = Color(0xFF00FFA3),
                shadowElevation = 10.dp,
                onClick = onEnablePremium
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Enable Premium",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}

@Composable
private fun PremiumFeatureRow(text: String) {
    val cs = MaterialTheme.colorScheme

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Color(0xFF00FFA3))
        )

        Text(
            text = text,
            fontSize = 13.sp,
            color = cs.onSurface.copy(alpha = 0.85f)
        )
    }
}


/* =========================== HELPERS =========================== */
private fun dayShortName(date: String): String {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val localDate = java.time.LocalDate.parse(date, formatter)

        when (localDate.dayOfWeek) {
            java.time.DayOfWeek.MONDAY -> "Mon"
            java.time.DayOfWeek.TUESDAY -> "Tue"
            java.time.DayOfWeek.WEDNESDAY -> "Wed"
            java.time.DayOfWeek.THURSDAY -> "Thu"
            java.time.DayOfWeek.FRIDAY -> "Fri"
            java.time.DayOfWeek.SATURDAY -> "Sat"
            java.time.DayOfWeek.SUNDAY -> "Sun"
        }
    } catch (_: Exception) {
        "?"
    }
}

private fun dayNumber(date: String): String {
    return try {
        val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val localDate = java.time.LocalDate.parse(date, formatter)
        localDate.dayOfMonth.toString()
    } catch (_: Exception) {
        ""
    }
}


private fun formatNumber(value: Int): String {
    return try {
        java.text.NumberFormat.getIntegerInstance().format(value)
    } catch (_: Exception) {
        value.toString()
    }
}
