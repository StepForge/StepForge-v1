@file:OptIn(ExperimentalAnimationApi::class, ExperimentalFoundationApi::class)

package com.example.stepforge.settings

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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
import kotlin.math.roundToInt

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
                        steps = ds.steps,
                        distanceKm = (ds.steps * 0.75f) / 1000f,
                        activeMin = (ds.steps / 110f).roundToInt()
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
    val context = androidx.compose.ui.platform.LocalContext.current
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

    val pageBg = cs.background

    // ✅ card surfaces tuned for light mode visibility
    val cardBg = if (isDark) cs.surface else Color(0xFFF7FAFD) // off-white
    val cardBorder = if (isDark) Color.Transparent else Color(0x220F172A) // subtle outline
    val cardShadow = if (isDark) 10.dp else 8.dp

    val titleColor = cs.onBackground
    val textMain = cs.onSurface
    val textSub = cs.onSurface.copy(alpha = 0.72f)

    val neonA = if (isDark) Color(0xFF00FFA3) else Color(0xFF4FD1C5)
    val neonB = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
    val neon = Brush.horizontalGradient(listOf(neonA, neonB))

    val ctx = androidx.compose.ui.platform.LocalContext.current
    var stepGoal by remember { mutableIntStateOf(10_000) }
    LaunchedEffect(Unit) {
        val prefs = ctx.stepforgeStore.data.first()
        stepGoal = prefs[intPreferencesKey("step_goal")] ?: 10_000
    }

    val today = remember { LocalDate.now() }
    val todayWeekStart = remember(today) { startOfWeek(today) }

    val firstDataDate = days.minOfOrNull { it.date } ?: todayWeekStart.minusWeeks(12)
    val firstWeekStart = remember(firstDataDate) { startOfWeek(firstDataDate) }

    val pageCount = remember(firstWeekStart, todayWeekStart) {
        weeksBetweenInclusive(firstWeekStart, todayWeekStart) + 1
    }
    val initialPage = (pageCount - 1).coerceAtLeast(0)

    val pagerState = rememberPagerState(initialPage = initialPage, pageCount = { pageCount })

    var selectedIndex by remember { mutableIntStateOf(-1) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(pageBg)
    ) {
        TopAppBar(
            title = { Text("Progress", fontWeight = FontWeight.SemiBold, color = titleColor) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null, tint = titleColor)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = pageBg)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            val headerWeekStart = firstWeekStart.plusWeeks(pagerState.currentPage.toLong())

            ElevatedCard(
                cardBg = cardBg,
                borderColor = cardBorder,
                shadowDp = cardShadow
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text("Daily timeline", color = textMain, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
                    Text(formatWeekRangeNumeric(headerWeekStart), color = textSub, fontSize = 14.sp)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 300.dp),
                beyondViewportPageCount = 1
            ) { page ->
                val ws = firstWeekStart.plusWeeks(page.toLong())
                val bars = remember(ws, days) { buildWeekBars(ws, days) }

                LaunchedEffect(page) { selectedIndex = -1 }

                val anim = remember(page) { Animatable(0f) }
                LaunchedEffect(page) {
                    anim.snapTo(0f)
                    anim.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                }

                BarsCard(
                    bars = bars,
                    today = today,
                    stepGoal = stepGoal.coerceAtLeast(1),
                    pageAnim = anim.value,
                    neon = neon,
                    cardBg = cardBg,
                    borderColor = cardBorder,
                    shadowDp = cardShadow,
                    textMain = textMain,
                    textSub = textSub,
                    selectedIndex = selectedIndex,
                    onSelectIndex = { idx -> selectedIndex = idx }
                )
            }

            val currentWeekStart = firstWeekStart.plusWeeks(pagerState.currentPage.toLong())
            val currentBars = remember(currentWeekStart, days) { buildWeekBars(currentWeekStart, days) }

            val stats = computeStats(currentBars)
            StatsCardFilled(
                stats = stats,
                cardBg = cardBg,
                borderColor = cardBorder,
                shadowDp = cardShadow,
                textMain = textMain,
                textSub = textSub
            )

            DetailCardFilled(
                bars = currentBars,
                selectedIndex = selectedIndex,
                cardBg = cardBg,
                borderColor = cardBorder,
                shadowDp = cardShadow,
                textMain = textMain,
                textSub = textSub,
                accent = MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(10.dp))
        }
    }
}

/* ---------------------------------------------------
   REUSABLE DEPTH CARD WRAPPER
--------------------------------------------------- */

@Composable
private fun ElevatedCard(
    cardBg: Color,
    borderColor: Color,
    shadowDp: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(shadowDp, shape, clip = false)
            .clip(shape)
            .background(cardBg)
            .then(
                if (borderColor != Color.Transparent) Modifier.border(1.dp, borderColor, shape)
                else Modifier
            )
    ) {
        content()
    }
}

/* ---------------------------------------------------
   CHART CARD
--------------------------------------------------- */

@Composable
private fun BarsCard(
    bars: List<DayStat>,
    today: LocalDate,
    stepGoal: Int,
    pageAnim: Float,
    neon: Brush,
    cardBg: Color,
    borderColor: Color,
    shadowDp: androidx.compose.ui.unit.Dp,
    textMain: Color,
    textSub: Color,
    selectedIndex: Int,
    onSelectIndex: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val track = if (isDark) Color(0xFF1E2430) else Color(0xFFDDE6F0)
    val selectedOutline = cs.primary.copy(alpha = 0.95f)

    val dash = PathEffect.dashPathEffect(floatArrayOf(16f, 14f), 0f)

    val maxSteps = (bars.maxOfOrNull { it.steps } ?: 0).coerceAtLeast(1)
    val chartMax = maxOf(maxSteps, stepGoal)
    val goalRatio = (stepGoal.toFloat() / chartMax.toFloat()).coerceIn(0f, 1f)

    ElevatedCard(cardBg = cardBg, borderColor = borderColor, shadowDp = shadowDp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
            ) {
                // dashed goal line
                androidx.compose.foundation.Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp)
                ) {
                    val topPad = size.height * 0.06f
                    val botPad = size.height * 0.10f
                    val usable = (size.height - topPad - botPad).coerceAtLeast(1f)
                    val y = (size.height - botPad) - (usable * goalRatio)

                    drawLine(
                        color = cs.onSurface.copy(alpha = if (isDark) 0.26f else 0.20f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 4f,
                        pathEffect = dash
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bars.forEachIndexed { idx, stat ->
                        val ratio = (stat.steps.toFloat() / chartMax.toFloat()).coerceIn(0f, 1f)
                        val animatedRatio = ratio * pageAnim

                        val heightDp = (160.dp * animatedRatio).coerceAtLeast(6.dp)
                        val isSel = idx == selectedIndex

                        val brush = if (isSel) neon else Brush.verticalGradient(
                            listOf(
                                cs.secondary.copy(alpha = 0.78f),
                                cs.primary.copy(alpha = 0.95f)
                            )
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height(heightDp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(brush)
                            )

                            Spacer(Modifier.height(8.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(track)
                            )
                        }
                    }
                }

                // click overlay
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    bars.forEachIndexed { idx, _ ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { onSelectIndex(if (selectedIndex == idx) -1 else idx) }
                        )
                    }
                }
            }

            // labels + today
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                bars.forEachIndexed { idx, stat ->
                    val isSel = idx == selectedIndex
                    val isToday = stat.date == today

                    val wd = stat.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    val dm = "${stat.date.dayOfMonth}/${stat.date.monthValue}"

                    val label = buildString {
                        append(wd)
                        append("\n")
                        append(dm)
                        if (isToday) {
                            append("\n")
                            append("Today")
                        }
                    }

                    Text(
                        text = label,
                        color = when {
                            isSel -> selectedOutline
                            isToday -> cs.secondary.copy(alpha = 0.95f)
                            else -> textSub
                        },
                        fontSize = 10.sp,
                        fontWeight = when {
                            isSel || isToday -> FontWeight.SemiBold
                            else -> FontWeight.Normal
                        },
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectIndex(if (selectedIndex == idx) -1 else idx) },
                        maxLines = 3,
                        lineHeight = 12.sp
                    )
                }
            }

            Text(
                text = "Goal line: $stepGoal steps",
                color = textSub.copy(alpha = 0.95f),
                fontSize = 11.sp
            )
        }
    }
}

/* ---------------------------------------------------
   STATS + DETAIL
--------------------------------------------------- */

private data class Stats(val avg: Int, val best: Int, val totalKm: Float)

private fun computeStats(bars: List<DayStat>): Stats {
    val avg = if (bars.isNotEmpty()) bars.sumOf { it.steps } / bars.size else 0
    val best = bars.maxOfOrNull { it.steps } ?: 0
    val totalKm = bars.sumOf { it.distanceKm.toDouble() }.toFloat()
    return Stats(avg = avg, best = best, totalKm = totalKm)
}

@Composable
private fun StatsCardFilled(
    stats: Stats,
    cardBg: Color,
    borderColor: Color,
    shadowDp: androidx.compose.ui.unit.Dp,
    textMain: Color,
    textSub: Color
) {
    ElevatedCard(cardBg = cardBg, borderColor = borderColor, shadowDp = shadowDp) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem(label = "Avg / day", value = stats.avg.toString(), textMain = textMain, textSub = textSub)
            StatItem(label = "Best day", value = stats.best.toString(), textMain = textMain, textSub = textSub)
            StatItem(label = "Total", value = "${stats.totalKm.roundToInt()} km", textMain = textMain, textSub = textSub)
        }
    }
}

@Composable
private fun StatItem(label: String, value: String, textMain: Color, textSub: Color) {
    Column {
        Text(label, fontSize = 12.sp, color = textSub)
        Spacer(Modifier.height(6.dp))
        Text(value, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = textMain)
    }
}

@Composable
private fun DetailCardFilled(
    bars: List<DayStat>,
    selectedIndex: Int,
    cardBg: Color,
    borderColor: Color,
    shadowDp: androidx.compose.ui.unit.Dp,
    textMain: Color,
    textSub: Color,
    accent: Color
) {
    val stat = bars.getOrNull(selectedIndex)

    AnimatedContent(
        targetState = stat,
        transitionSpec = { fadeIn(tween(180)) with fadeOut(tween(120)) },
        label = "detailCard"
    ) { s ->
        ElevatedCard(cardBg = cardBg, borderColor = borderColor, shadowDp = shadowDp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (s == null) {
                    Text(
                        "Great consistency!",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = textMain
                    )
                    Text(
                        "Swipe to change week. Tap a bar to see details.",
                        color = textSub,
                        fontSize = 13.sp
                    )
                } else {
                    val wd = s.date.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())
                    Text(
                        "$wd · ${s.date.dayOfMonth}/${s.date.monthValue}/${s.date.year}",
                        fontWeight = FontWeight.SemiBold,
                        color = textMain,
                        fontSize = 16.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        MiniDetail("Steps", s.steps.toString(), textMain, textSub)
                        MiniDetail(
                            "Distance",
                            String.format(Locale.getDefault(), "%.1f km", s.distanceKm),
                            textMain,
                            textSub
                        )
                        MiniDetail("Active", "${s.activeMin} min", textMain, textSub)
                    }

                    val avg = if (bars.isNotEmpty()) bars.sumOf { it.steps } / bars.size else 0
                    val delta = s.steps - avg
                    val pct = if (avg > 0) (delta * 100f / avg).roundToInt() else 0

                    Text(
                        text = if (delta >= 0)
                            "You walked $pct% more than this week’s average."
                        else
                            "You walked ${-pct}% less than this week’s average.",
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun MiniDetail(label: String, value: String, textMain: Color, textSub: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, fontSize = 12.sp, color = textSub)
        Text(value, fontWeight = FontWeight.Bold, color = textMain, fontSize = 16.sp)
    }
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

private fun formatWeekRangeNumeric(weekStart: LocalDate): String {
    val end = weekStart.plusDays(6)
    return "${weekStart.dayOfMonth}/${weekStart.monthValue} – ${end.dayOfMonth}/${end.monthValue}, ${end.year}"
}

private fun weeksBetweenInclusive(fromWeekStart: LocalDate, toWeekStart: LocalDate): Int {
    val days = java.time.temporal.ChronoUnit.DAYS.between(fromWeekStart, toWeekStart)
    return (days / 7).toInt().coerceAtLeast(0)
}