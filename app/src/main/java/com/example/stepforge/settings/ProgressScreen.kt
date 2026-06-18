@file:OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)

package com.example.stepforge.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepforge.R
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailyStepsDao
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sqrt

/* ---------------------------------------------------
   MODELS
--------------------------------------------------- */

@Immutable
private data class DayStat(
    val date: LocalDate,
    val steps: Int,
    val distanceKm: Float,
    val activeMin: Int
)

@Immutable
private data class ProgressUiState(
    val days: List<DayStat> = emptyList()
)

@Immutable
private data class ProgressSummary(
    val weekSteps: Int,
    val previousWeekSteps: Int,
    val weekDistanceKm: Float,
    val weekActiveMin: Int,
    val weekCalories: Int,
    val activeDays: Int,
    val goalDays: Int,
    val averageSteps: Int,
    val bestWeekDay: DayStat?,
    val allTimeBestDay: DayStat?,
    val consistencyScore: Int,
    val movementScore: Int,
    val trendPercent: Int,
    val todaySteps: Int,
    val todayRemaining: Int,
    val todayProgress: Float,
    val thirtyDayActiveDays: Int,
    val thirtyDayGoalDays: Int
)

private data class Stats(val avg: Int, val best: Int, val totalKm: Float)

/* ---------------------------------------------------
   VIEWMODEL
--------------------------------------------------- */

private class ProgressViewModel(
    private val dao: DailyStepsDao
) : ViewModel() {

    private val _ui = MutableStateFlow(ProgressUiState())
    val ui: StateFlow<ProgressUiState> = _ui

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            val all = dao.getAllSteps()
                .sortedBy { it.date }
                .mapNotNull { ds ->
                    val date = runCatching { LocalDate.parse(ds.date) }.getOrNull() ?: return@mapNotNull null
                    DayStat(
                        date = date,
                        steps = ds.steps.coerceAtLeast(0),
                        distanceKm = (ds.steps.coerceAtLeast(0) * 0.75f) / 1000f,
                        activeMin = (ds.steps.coerceAtLeast(0) / 110f).roundToInt()
                    )
                }
            _ui.value = ProgressUiState(days = all)
        }
    }
}

/* ---------------------------------------------------
   PUBLIC ENTRY
--------------------------------------------------- */

@Composable
fun ProgressScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val db = remember { AppDatabase.getDatabase(context) }
    val dao = remember { db.dailyStepsDao() }

    val vm: ProgressViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ProgressViewModel(dao) as T
            }
        }
    )

    val state by vm.ui.collectAsState()
    ProgressScreenContent(days = state.days, onBack = onBack)
}

/* ---------------------------------------------------
   UI
--------------------------------------------------- */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgressScreenContent(
    days: List<DayStat>,
    onBack: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val pageBg = if (isDark) Color(0xFF050812) else Color(0xFFF4F7FB)
    val cardBg = if (isDark) Color(0xEE101622) else Color(0xFFFFFFFF)
    val elevatedBg = if (isDark) Color(0xFF111827) else Color(0xFFF9FBFF)
    val cardBorder = if (isDark) Color.White.copy(alpha = 0.08f) else Color(0x1A0F172A)
    val cardShadow = if (isDark) 12.dp else 8.dp

    val textMain = if (isDark) Color(0xFFF8FAFC) else Color(0xFF0F172A)
    val textSub = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    val faintText = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)

    val accentA = if (isDark) Color(0xFF00FFA3) else Color(0xFF0EA5A3)
    val accentB = if (isDark) Color(0xFF00D5FF) else Color(0xFF2563EB)
    val accentC = if (isDark) Color(0xFF9F7AEA) else Color(0xFF7C3AED)

    val accent = Brush.horizontalGradient(listOf(accentA, accentB, accentC))
    val softAccent = Brush.linearGradient(
        listOf(
            accentA.copy(alpha = if (isDark) 0.24f else 0.14f),
            accentB.copy(alpha = if (isDark) 0.18f else 0.12f),
            accentC.copy(alpha = if (isDark) 0.18f else 0.10f)
        )
    )

    val ctx = LocalContext.current
    var stepGoal by remember { mutableIntStateOf(10_000) }
    LaunchedEffect(Unit) {
        val prefs = ctx.stepforgeStore.data.first()
        stepGoal = (prefs[intPreferencesKey("step_goal")] ?: 10_000).coerceAtLeast(1)
    }

    val today = remember { LocalDate.now() }
    val todayWeekStart = remember(today) { startOfWeek(today) }

    val firstDataDate = days.minOfOrNull { it.date }
    val defaultFirstWeek = todayWeekStart.minusWeeks(8)
    val firstWeekStart = remember(firstDataDate, defaultFirstWeek) {
        val dataWeek = firstDataDate?.let { startOfWeek(it) } ?: defaultFirstWeek
        if (dataWeek.isBefore(defaultFirstWeek)) dataWeek else defaultFirstWeek
    }

    val pageCount = remember(firstWeekStart, todayWeekStart) {
        weeksBetweenInclusive(firstWeekStart, todayWeekStart) + 1
    }.coerceAtLeast(1)

    val initialPage = (pageCount - 1).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    var selectedIndex by remember { mutableIntStateOf(-1) }
    LaunchedEffect(pagerState.currentPage) { selectedIndex = -1 }

    val currentWeekStart = firstWeekStart.plusWeeks(pagerState.currentPage.toLong())
    val currentBars = remember(currentWeekStart, days) { buildWeekBars(currentWeekStart, days) }
    val previousBars = remember(currentWeekStart, days) { buildWeekBars(currentWeekStart.minusWeeks(1), days) }
    val recentThirty = remember(today, days) { buildRecentDays(today.minusDays(29), 30, days) }
    val summary = remember(currentBars, previousBars, recentThirty, days, stepGoal, today) {
        computeProgressSummary(
            currentWeek = currentBars,
            previousWeek = previousBars,
            recentThirty = recentThirty,
            allDays = days,
            stepGoal = stepGoal,
            today = today
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
            .drawBehind {
                drawCircle(
                    color = accentB.copy(alpha = if (isDark) 0.16f else 0.08f),
                    radius = size.maxDimension * 0.42f,
                    center = Offset(size.width * 0.12f, size.height * 0.03f)
                )
                drawCircle(
                    color = accentC.copy(alpha = if (isDark) 0.14f else 0.07f),
                    radius = size.maxDimension * 0.36f,
                    center = Offset(size.width * 0.98f, size.height * 0.28f)
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.hc_progress),
                        fontWeight = FontWeight.SemiBold,
                        color = textMain
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = textMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = Color.Transparent
                )
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ProgressHeroCard(
                    summary = summary,
                    weekStart = currentWeekStart,
                    stepGoal = stepGoal,
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    shadowDp = cardShadow,
                    accent = accent,
                    softAccent = softAccent,
                    textMain = textMain,
                    textSub = textSub,
                    faintText = faintText
                )

                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 320.dp),
                    beyondViewportPageCount = 1
                ) { page ->
                    val ws = firstWeekStart.plusWeeks(page.toLong())
                    val bars = remember(ws, days) { buildWeekBars(ws, days) }

                    val anim = remember(page) { Animatable(0f) }
                    LaunchedEffect(page) {
                        anim.snapTo(0f)
                        anim.animateTo(1f, tween(680, easing = FastOutSlowInEasing))
                    }

                    PremiumBarsCard(
                        bars = bars,
                        today = today,
                        stepGoal = stepGoal,
                        pageAnim = anim.value,
                        accent = accent,
                        accentA = accentA,
                        accentB = accentB,
                        cardBg = cardBg,
                        borderColor = cardBorder,
                        shadowDp = cardShadow,
                        textMain = textMain,
                        textSub = textSub,
                        faintText = faintText,
                        selectedIndex = selectedIndex,
                        onSelectIndex = { idx -> selectedIndex = if (selectedIndex == idx) -1 else idx }
                    )
                }

                InsightGrid(
                    summary = summary,
                    cardBg = cardBg,
                    elevatedBg = elevatedBg,
                    borderColor = cardBorder,
                    shadowDp = cardShadow,
                    accent = accent,
                    accentA = accentA,
                    accentB = accentB,
                    textMain = textMain,
                    textSub = textSub
                )

                ThirtyDayHeatmapCard(
                    days = recentThirty,
                    stepGoal = stepGoal,
                    summary = summary,
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    shadowDp = cardShadow,
                    accentA = accentA,
                    accentB = accentB,
                    textMain = textMain,
                    textSub = textSub,
                    faintText = faintText
                )

                DetailCardPremium(
                    bars = currentBars,
                    selectedIndex = selectedIndex,
                    stepGoal = stepGoal,
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    shadowDp = cardShadow,
                    accent = accent,
                    accentA = accentA,
                    textMain = textMain,
                    textSub = textSub
                )

                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/* ---------------------------------------------------
   PREMIUM HERO
--------------------------------------------------- */

@Composable
private fun ProgressHeroCard(
    summary: ProgressSummary,
    weekStart: LocalDate,
    stepGoal: Int,
    cardBg: Color,
    borderColor: Color,
    shadowDp: Dp,
    accent: Brush,
    softAccent: Brush,
    textMain: Color,
    textSub: Color,
    faintText: Color
) {
    PremiumCard(
        cardBg = cardBg,
        borderColor = borderColor,
        shadowDp = shadowDp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(softAccent)
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    PremiumBadge(text = "PROGRESS LAB", accent = accent)
                    Text(
                        text = "Movement Progress",
                        color = textMain,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 26.sp,
                        lineHeight = 28.sp
                    )
                    Text(
                        text = formatWeekRangeNumeric(weekStart),
                        color = textSub,
                        fontSize = 13.sp
                    )
                }

                ScoreOrb(
                    score = summary.movementScore,
                    accent = accent,
                    textMain = textMain,
                    textSub = faintText
                )
            }

            ProgressTrack(
                progress = (summary.weekSteps.toFloat() / (stepGoal * 7f)).coerceIn(0f, 1f),
                accent = accent,
                trackColor = textMain.copy(alpha = 0.10f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                HeroMetric(
                    label = "Week steps",
                    value = formatSteps(summary.weekSteps),
                    sub = "${summary.goalDays}/7 goal days",
                    textMain = textMain,
                    textSub = textSub,
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    label = "Distance",
                    value = formatKm(summary.weekDistanceKm),
                    sub = "${summary.weekActiveMin} min active",
                    textMain = textMain,
                    textSub = textSub,
                    modifier = Modifier.weight(1f)
                )
                HeroMetric(
                    label = "Calories",
                    value = "${summary.weekCalories}",
                    sub = "estimated kcal",
                    textMain = textMain,
                    textSub = textSub,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun PremiumBadge(text: String, accent: Brush) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(accent)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = text,
            color = Color(0xFF061016),
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.7.sp
        )
    }
}

@Composable
private fun ScoreOrb(
    score: Int,
    accent: Brush,
    textMain: Color,
    textSub: Color
) {
    Box(
        modifier = Modifier
            .size(82.dp)
            .clip(CircleShape)
            .background(textMain.copy(alpha = 0.06f))
            .border(1.dp, textMain.copy(alpha = 0.12f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = accent,
                radius = size.minDimension * 0.46f,
                center = center,
                alpha = 0.22f
            )
            drawArc(
                brush = accent,
                startAngle = -90f,
                sweepAngle = 360f * (score / 100f).coerceIn(0f, 1f),
                useCenter = false,
                topLeft = Offset(size.width * 0.09f, size.height * 0.09f),
                size = androidx.compose.ui.geometry.Size(size.width * 0.82f, size.height * 0.82f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 8f)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = score.toString(),
                color = textMain,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 23.sp
            )
            Text(
                text = "score",
                color = textSub,
                fontSize = 10.sp
            )
        }
    }
}

@Composable
private fun HeroMetric(
    label: String,
    value: String,
    sub: String,
    textMain: Color,
    textSub: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(textMain.copy(alpha = 0.055f))
            .border(1.dp, textMain.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Text(label, color = textSub, fontSize = 11.sp, maxLines = 1)
        Text(value, color = textMain, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, maxLines = 1)
        Text(sub, color = textSub.copy(alpha = 0.82f), fontSize = 10.sp, maxLines = 1)
    }
}

/* ---------------------------------------------------
   WEEKLY CHART
--------------------------------------------------- */

@Composable
private fun PremiumBarsCard(
    bars: List<DayStat>,
    today: LocalDate,
    stepGoal: Int,
    pageAnim: Float,
    accent: Brush,
    accentA: Color,
    accentB: Color,
    cardBg: Color,
    borderColor: Color,
    shadowDp: Dp,
    textMain: Color,
    textSub: Color,
    faintText: Color,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val chartMax = maxOf((bars.maxOfOrNull { it.steps } ?: 0), stepGoal).coerceAtLeast(1)
    val goalRatio = (stepGoal.toFloat() / chartMax.toFloat()).coerceIn(0f, 1f)
    val dash = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f)
    val stats = computeStats(bars)

    PremiumCard(cardBg = cardBg, borderColor = borderColor, shadowDp = shadowDp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Daily Timeline", color = textMain, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
                    Text("Tap a day to inspect it", color = textSub, fontSize = 12.sp)
                }
                Text(
                    text = "Goal ${formatSteps(stepGoal)}",
                    color = textSub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(205.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 4.dp)
                ) {
                    val topPad = size.height * 0.06f
                    val botPad = size.height * 0.12f
                    val usable = size.height - topPad - botPad
                    val y = (size.height - botPad) - (usable * goalRatio)

                    drawLine(
                        color = textMain.copy(alpha = 0.14f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 3f,
                        pathEffect = dash
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    bars.forEachIndexed { idx, stat ->
                        val ratio = (stat.steps.toFloat() / chartMax.toFloat()).coerceIn(0f, 1f)
                        val animatedRatio = ratio * pageAnim
                        val heightDp = (160.dp * animatedRatio).coerceAtLeast(8.dp)
                        val isSel = idx == selectedIndex
                        val isToday = stat.date == today
                        val hitGoal = stat.steps >= stepGoal

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onSelectIndex(idx) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            if (isSel || isToday) {
                                Text(
                                    text = if (stat.steps > 0) formatSteps(stat.steps) else "0",
                                    color = if (isSel) accentA else accentB,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(Modifier.height(5.dp))
                            } else {
                                Spacer(Modifier.height(17.dp))
                            }

                            Box(
                                modifier = Modifier
                                    .width(if (isSel) 28.dp else 22.dp)
                                    .height(heightDp)
                                    .shadow(if (isSel) 12.dp else 0.dp, RoundedCornerShape(999.dp), clip = false)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (hitGoal || isSel) accent else Brush.verticalGradient(
                                            listOf(
                                                accentB.copy(alpha = 0.52f),
                                                accentA.copy(alpha = 0.72f)
                                            )
                                        )
                                    )
                                    .border(
                                        width = if (isSel || isToday) 1.dp else 0.dp,
                                        color = if (isSel) accentA.copy(alpha = 0.75f) else textMain.copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(999.dp)
                                    )
                            )

                            Spacer(Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        when {
                                            isSel -> accentA.copy(alpha = 0.72f)
                                            isToday -> accentB.copy(alpha = 0.60f)
                                            else -> textMain.copy(alpha = 0.10f)
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                bars.forEachIndexed { idx, stat ->
                    DayLabel(
                        stat = stat,
                        today = today,
                        selected = idx == selectedIndex,
                        textSub = textSub,
                        accentA = accentA,
                        accentB = accentB,
                        modifier = Modifier.weight(1f)
                    ) { onSelectIndex(idx) }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(textMain.copy(alpha = 0.045f))
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                CompactStat("Avg", formatSteps(stats.avg), textMain, faintText)
                CompactStat("Best", formatSteps(stats.best), textMain, faintText)
                CompactStat("Total", formatKm(stats.totalKm), textMain, faintText)
            }
        }
    }
}

@Composable
private fun DayLabel(
    stat: DayStat,
    today: LocalDate,
    selected: Boolean,
    textSub: Color,
    accentA: Color,
    accentB: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val wd = stat.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val isToday = stat.date == today
    Text(
        text = if (isToday) "$wd\nToday" else "$wd\n${stat.date.dayOfMonth}",
        color = when {
            selected -> accentA
            isToday -> accentB
            else -> textSub
        },
        fontSize = 10.sp,
        lineHeight = 12.sp,
        textAlign = TextAlign.Center,
        fontWeight = if (selected || isToday) FontWeight.ExtraBold else FontWeight.Medium,
        modifier = modifier.clickable { onClick() },
        maxLines = 2
    )
}

/* ---------------------------------------------------
   INSIGHTS
--------------------------------------------------- */

@Composable
private fun InsightGrid(
    summary: ProgressSummary,
    cardBg: Color,
    elevatedBg: Color,
    borderColor: Color,
    shadowDp: Dp,
    accent: Brush,
    accentA: Color,
    accentB: Color,
    textMain: Color,
    textSub: Color
) {
    PremiumCard(cardBg = cardBg, borderColor = borderColor, shadowDp = shadowDp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "Smart Insights",
                subtitle = "Clean read of your weekly movement pattern.",
                textMain = textMain,
                textSub = textSub
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                InsightTile(
                    title = "Momentum",
                    value = formatTrend(summary.trendPercent),
                    subtitle = "vs last week",
                    icon = if (summary.trendPercent >= 0) "↗" else "↘",
                    elevatedBg = elevatedBg,
                    accent = if (summary.trendPercent >= 0) accentA else Color(0xFFFF6B6B),
                    textMain = textMain,
                    textSub = textSub,
                    modifier = Modifier.weight(1f)
                )
                InsightTile(
                    title = "Consistency",
                    value = "${summary.consistencyScore}%",
                    subtitle = consistencyLabel(summary.consistencyScore),
                    icon = "◆",
                    elevatedBg = elevatedBg,
                    accent = accentB,
                    textMain = textMain,
                    textSub = textSub,
                    modifier = Modifier.weight(1f)
                )
            }

            ProgressTrack(
                progress = summary.todayProgress,
                accent = accent,
                trackColor = textMain.copy(alpha = 0.10f)
            )

            TodayProjectionRow(
                todaySteps = summary.todaySteps,
                remaining = summary.todayRemaining,
                progress = summary.todayProgress,
                textMain = textMain,
                textSub = textSub
            )
        }
    }
}

@Composable
private fun InsightTile(
    title: String,
    value: String,
    subtitle: String,
    icon: String,
    elevatedBg: Color,
    accent: Color,
    textMain: Color,
    textSub: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(elevatedBg)
            .border(1.dp, textMain.copy(alpha = 0.07f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(icon, color = accent, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp)
            }
            Text(title, color = textSub, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Text(value, color = textMain, fontWeight = FontWeight.ExtraBold, fontSize = 23.sp)
        Text(subtitle, color = textSub, fontSize = 11.sp, lineHeight = 13.sp)
    }
}

@Composable
private fun TodayProjectionRow(
    todaySteps: Int,
    remaining: Int,
    progress: Float,
    textMain: Color,
    textSub: Color
) {
    val note = when {
        remaining <= 0 -> "Goal secured today. Keep the rhythm clean."
        progress >= 0.70f -> "Close to goal. One focused walk should finish it."
        progress >= 0.35f -> "Solid start. Add a medium walk to stay on pace."
        todaySteps > 0 -> "You started moving. Build momentum with short sessions."
        else -> "No movement yet today. Start small and protect your progress."
    }

    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Today projection", color = textMain, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(
                text = if (remaining <= 0) "Complete" else "${formatSteps(remaining)} left",
                color = textMain,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp
            )
        }
        Text(note, color = textSub, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

/* ---------------------------------------------------
   30 DAY HEATMAP
--------------------------------------------------- */

@Composable
private fun ThirtyDayHeatmapCard(
    days: List<DayStat>,
    stepGoal: Int,
    summary: ProgressSummary,
    cardBg: Color,
    borderColor: Color,
    shadowDp: Dp,
    accentA: Color,
    accentB: Color,
    textMain: Color,
    textSub: Color,
    faintText: Color
) {
    PremiumCard(cardBg = cardBg, borderColor = borderColor, shadowDp = shadowDp) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SectionHeader(
                title = "30-Day Activity Map",
                subtitle = "${summary.thirtyDayActiveDays} active days • ${summary.thirtyDayGoalDays} goal days",
                textMain = textMain,
                textSub = textSub
            )

            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                days.chunked(10).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        row.forEach { stat ->
                            val ratio = (stat.steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
                            val bg = when {
                                stat.steps <= 0 -> textMain.copy(alpha = 0.065f)
                                ratio >= 1f -> accentA.copy(alpha = 0.88f)
                                ratio >= 0.65f -> accentB.copy(alpha = 0.66f)
                                ratio >= 0.30f -> accentB.copy(alpha = 0.36f)
                                else -> accentB.copy(alpha = 0.18f)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(24.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(bg)
                                    .border(1.dp, textMain.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Less", color = faintText, fontSize = 11.sp)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    listOf(
                        textMain.copy(alpha = 0.065f),
                        accentB.copy(alpha = 0.18f),
                        accentB.copy(alpha = 0.36f),
                        accentB.copy(alpha = 0.66f),
                        accentA.copy(alpha = 0.88f)
                    ).forEach { c ->
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(c)
                        )
                    }
                }
                Text("More", color = faintText, fontSize = 11.sp)
            }
        }
    }
}

/* ---------------------------------------------------
   DETAIL
--------------------------------------------------- */

@Composable
private fun DetailCardPremium(
    bars: List<DayStat>,
    selectedIndex: Int,
    stepGoal: Int,
    cardBg: Color,
    borderColor: Color,
    shadowDp: Dp,
    accent: Brush,
    accentA: Color,
    textMain: Color,
    textSub: Color
) {
    val stat = bars.getOrNull(selectedIndex)

    AnimatedContent(
        targetState = stat,
        transitionSpec = { fadeIn(tween(180)) with fadeOut(tween(120)) },
        label = "detailCard"
    ) { s ->
        PremiumCard(cardBg = cardBg, borderColor = borderColor, shadowDp = shadowDp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (s == null) {
                    SectionHeader(
                        title = "Day Analysis",
                        subtitle = "Tap any bar above to open a focused breakdown.",
                        textMain = textMain,
                        textSub = textSub
                    )
                    ProgressTrack(
                        progress = 0f,
                        accent = accent,
                        trackColor = textMain.copy(alpha = 0.10f)
                    )
                } else {
                    val wd = s.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    val goalProgress = (s.steps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)
                    val avg = if (bars.isNotEmpty()) bars.sumOf { it.steps } / bars.size else 0
                    val delta = s.steps - avg
                    val pct = if (avg > 0) (delta * 100f / avg).roundToInt() else 0
                    val quality = when {
                        s.steps >= stepGoal -> "Goal completed"
                        goalProgress >= 0.75f -> "Strong progress"
                        goalProgress >= 0.40f -> "Building day"
                        s.steps > 0 -> "Light movement"
                        else -> "No activity"
                    }

                    SectionHeader(
                        title = "$wd • ${s.date.dayOfMonth}/${s.date.monthValue}",
                        subtitle = quality,
                        textMain = textMain,
                        textSub = textSub
                    )

                    ProgressTrack(
                        progress = goalProgress,
                        accent = accent,
                        trackColor = textMain.copy(alpha = 0.10f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniDetail("Steps", formatSteps(s.steps), textMain, textSub)
                        MiniDetail("Distance", formatKm(s.distanceKm), textMain, textSub)
                        MiniDetail("Active", "${s.activeMin} min", textMain, textSub)
                    }

                    Text(
                        text = if (delta >= 0)
                            "${abs(pct)}% above this week’s average"
                        else
                            "${abs(pct)}% below this week’s average",
                        color = accentA,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}

/* ---------------------------------------------------
   SHARED COMPONENTS
--------------------------------------------------- */

@Composable
private fun PremiumCard(
    cardBg: Color,
    borderColor: Color,
    shadowDp: Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(26.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(shadowDp, shape, clip = false)
            .clip(shape)
            .background(cardBg)
            .border(1.dp, borderColor, shape)
    ) {
        content()
    }
}

@Composable
private fun SectionHeader(
    title: String,
    subtitle: String,
    textMain: Color,
    textSub: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, color = textMain, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp)
        Text(subtitle, color = textSub, fontSize = 12.sp, lineHeight = 16.sp)
    }
}

@Composable
private fun ProgressTrack(
    progress: Float,
    accent: Brush,
    trackColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(trackColor)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .fillMaxHeight()
                .clip(RoundedCornerShape(999.dp))
                .background(accent)
        )
    }
}

@Composable
private fun CompactStat(label: String, value: String, textMain: Color, textSub: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, color = textSub, fontSize = 11.sp)
        Text(value, color = textMain, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
    }
}

@Composable
private fun MiniDetail(label: String, value: String, textMain: Color, textSub: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = textSub)
        Text(value, fontWeight = FontWeight.ExtraBold, color = textMain, fontSize = 16.sp)
    }
}

/* ---------------------------------------------------
   CALCULATIONS
--------------------------------------------------- */

private fun computeStats(bars: List<DayStat>): Stats {
    val avg = if (bars.isNotEmpty()) bars.sumOf { it.steps } / bars.size else 0
    val best = bars.maxOfOrNull { it.steps } ?: 0
    val totalKm = bars.sumOf { it.distanceKm.toDouble() }.toFloat()
    return Stats(avg = avg, best = best, totalKm = totalKm)
}

private fun computeProgressSummary(
    currentWeek: List<DayStat>,
    previousWeek: List<DayStat>,
    recentThirty: List<DayStat>,
    allDays: List<DayStat>,
    stepGoal: Int,
    today: LocalDate
): ProgressSummary {
    val weekSteps = currentWeek.sumOf { it.steps }
    val previousSteps = previousWeek.sumOf { it.steps }
    val weekDistance = currentWeek.sumOf { it.distanceKm.toDouble() }.toFloat()
    val weekActiveMin = currentWeek.sumOf { it.activeMin }
    val weekCalories = estimateCalories(weekSteps)
    val activeDays = currentWeek.count { it.steps > 0 }
    val goalDays = currentWeek.count { it.steps >= stepGoal }
    val averageSteps = if (currentWeek.isNotEmpty()) weekSteps / currentWeek.size else 0
    val bestWeekDay = currentWeek.maxByOrNull { it.steps }?.takeIf { it.steps > 0 }
    val allTimeBest = allDays.maxByOrNull { it.steps }?.takeIf { it.steps > 0 }
    val consistency = computeConsistencyScore(currentWeek, stepGoal)
    val trendPercent = when {
        previousSteps <= 0 && weekSteps <= 0 -> 0
        previousSteps <= 0 -> 100
        else -> (((weekSteps - previousSteps) * 100f) / previousSteps).roundToInt()
    }.coerceIn(-999, 999)

    val goalRate = goalDays / 7f
    val volumeScore = (weekSteps.toFloat() / (stepGoal * 7f)).coerceIn(0f, 1f)
    val trendScore = ((trendPercent + 40f) / 80f).coerceIn(0f, 1f)
    val movementScore = ((goalRate * 42f) + (volumeScore * 34f) + (consistency * 0.18f) + (trendScore * 6f))
        .roundToInt()
        .coerceIn(0, 100)

    val todaySteps = currentWeek.firstOrNull { it.date == today }?.steps ?: allDays.firstOrNull { it.date == today }?.steps ?: 0
    val remaining = (stepGoal - todaySteps).coerceAtLeast(0)
    val todayProgress = (todaySteps.toFloat() / stepGoal.toFloat()).coerceIn(0f, 1f)

    return ProgressSummary(
        weekSteps = weekSteps,
        previousWeekSteps = previousSteps,
        weekDistanceKm = weekDistance,
        weekActiveMin = weekActiveMin,
        weekCalories = weekCalories,
        activeDays = activeDays,
        goalDays = goalDays,
        averageSteps = averageSteps,
        bestWeekDay = bestWeekDay,
        allTimeBestDay = allTimeBest,
        consistencyScore = consistency,
        movementScore = movementScore,
        trendPercent = trendPercent,
        todaySteps = todaySteps,
        todayRemaining = remaining,
        todayProgress = todayProgress,
        thirtyDayActiveDays = recentThirty.count { it.steps > 0 },
        thirtyDayGoalDays = recentThirty.count { it.steps >= stepGoal }
    )
}

private fun computeConsistencyScore(days: List<DayStat>, stepGoal: Int): Int {
    if (days.isEmpty()) return 0
    val activeDays = days.count { it.steps > 0 }
    val activeRatio = activeDays / days.size.toFloat()
    val values = days.map { it.steps.toDouble() }
    val avg = values.average().coerceAtLeast(1.0)
    val variance = values.sumOf { (it - avg) * (it - avg) } / values.size
    val stability = (1f - (sqrt(variance) / avg).toFloat()).coerceIn(0f, 1f)
    val goalBalance = (days.count { it.steps >= stepGoal } / days.size.toFloat()).coerceIn(0f, 1f)
    return ((activeRatio * 45f) + (stability * 35f) + (goalBalance * 20f)).roundToInt().coerceIn(0, 100)
}

private fun estimateCalories(steps: Int): Int {
    return (steps * 0.04f).roundToInt().coerceAtLeast(0)
}

/* ---------------------------------------------------
   HELPERS
--------------------------------------------------- */

private fun startOfWeek(date: LocalDate): LocalDate {
    val dow = date.dayOfWeek.value
    return date.minusDays((dow - DayOfWeek.MONDAY.value).toLong())
}

private fun buildWeekBars(weekStart: LocalDate, allDays: List<DayStat>): List<DayStat> {
    val map = allDays.associateBy { it.date }
    return (0..6).map { i ->
        val d = weekStart.plusDays(i.toLong())
        map[d] ?: DayStat(date = d, steps = 0, distanceKm = 0f, activeMin = 0)
    }
}

private fun buildRecentDays(startDate: LocalDate, count: Int, allDays: List<DayStat>): List<DayStat> {
    val map = allDays.associateBy { it.date }
    return (0 until count).map { i ->
        val d = startDate.plusDays(i.toLong())
        map[d] ?: DayStat(date = d, steps = 0, distanceKm = 0f, activeMin = 0)
    }
}

private fun formatWeekRangeNumeric(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    return "${weekStart.dayOfMonth}/${weekStart.monthValue} - ${end.dayOfMonth}/${end.monthValue}, ${end.year}"
}

private fun weeksBetweenInclusive(fromWeekStart: LocalDate, toWeekStart: LocalDate): Int {
    val days = java.time.temporal.ChronoUnit.DAYS.between(fromWeekStart, toWeekStart)
    return (days / 7).toInt().coerceAtLeast(0)
}

private fun formatSteps(value: Int): String {
    val absValue = abs(value)
    return when {
        absValue >= 1_000_000 -> String.format(Locale.getDefault(), "%.1fM", value / 1_000_000f)
        absValue >= 10_000 -> String.format(Locale.getDefault(), "%.1fK", value / 1_000f)
        else -> String.format(Locale.getDefault(), "%,d", value)
    }
}

private fun formatKm(value: Float): String {
    return if (value >= 10f) {
        String.format(Locale.getDefault(), "%.0f km", value)
    } else {
        String.format(Locale.getDefault(), "%.1f km", value)
    }
}

private fun formatTrend(value: Int): String {
    return when {
        value > 0 -> "+$value%"
        value < 0 -> "$value%"
        else -> "0%"
    }
}

private fun consistencyLabel(score: Int): String {
    return when {
        score >= 82 -> "Elite rhythm"
        score >= 65 -> "Stable week"
        score >= 42 -> "Building"
        score > 0 -> "Needs rhythm"
        else -> "No data yet"
    }
}
