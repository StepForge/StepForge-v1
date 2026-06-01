package com.example.stepforge

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Route
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.WorkoutSession
import kotlinx.coroutines.flow.collectLatest
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutsScreen(
    onBack: () -> Unit,
    highlightedSessionId: Long = -1L
) {
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    // ✅ Premium Light Theme Colors
    val lightPrimaryGradientStart = Color(0xFF0EA5E9)  // Sky blue
    val lightPrimaryGradientEnd = Color(0xFF06B6D4)    // Cyan
    val lightSecondaryGradientStart = Color(0xFF10B981) // Emerald
    val lightSecondaryGradientEnd = Color(0xFF14B8A6)   // Teal

    val lightCardBg = if (isDark) cs.surface else Color(0xFFFFFFFF)
    val lightPanelBg = if (isDark) Color(0xFF121821) else Color(0xFFF0F9FF)  // ✅ Hafif mavi tonu
    val lightPanelBgStrong = if (isDark) Color(0xFF161D27) else Color(0xFFE0F2FE)  // ✅ Daha kuvvetli mavi
    val lightBorderSoft = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFF0EA5E9).copy(alpha = 0.15f)  // ✅ Mavi border

    // Neon colors
    val neonA = if (isDark) Color(0xFF00FFA3) else Color(0xFF10B981)  // ✅ Emerald (daha canlı)
    val neonB = if (isDark) Color(0xFF00F5FF) else Color(0xFF0EA5E9)  // ✅ Sky blue (daha canlı)

    val bg = if (isDark) cs.background else Color(0xFFF8FAFD)
    val cardBg = if (isDark) cs.surface else Color(0xFFFFFFFF)
    val panelBg = if (isDark) Color(0xFF121821) else Color(0xFFF5F9FF)
    val panelBgStrong = if (isDark) Color(0xFF161D27) else Color(0xFFEDF2F9)
    val borderSoft = if (isDark) Color.White.copy(alpha = 0.06f) else Color(0xFFE2E8F0)

    val db = remember { AppDatabase.getDatabase(ctx) }
    val dao = remember { db.workoutSessionDao() }

    var sessions by remember { mutableStateOf<List<WorkoutSession>>(emptyList()) }
    var selectedId by remember { mutableLongStateOf(highlightedSessionId) }

    val today = LocalDate.now(ZoneId.systemDefault())
    val todayKey = today.toString()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val firstDayOfWeek = remember { WeekFields.of(Locale.getDefault()).firstDayOfWeek }

    var selectedWeekOffset by rememberSaveable { mutableIntStateOf(0) }

    val sessionsByDate = remember(sessions) {
        sessions.groupBy { it.date }
    }

    val currentWeekDays = remember(today, selectedWeekOffset, firstDayOfWeek) {
        val baseDate = today.plusWeeks(selectedWeekOffset.toLong())
        val startOfWeek = baseDate.with(firstDayOfWeek)
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    var selectedDate by rememberSaveable {
        mutableStateOf(todayKey)
    }

    LaunchedEffect(todayKey, selectedWeekOffset, currentWeekDays) {
        selectedDate = if (selectedWeekOffset == 0) {
            todayKey
        } else {
            currentWeekDays.lastOrNull()?.format(dateFormatter) ?: todayKey
        }
    }

    val selectedDaySessions = remember(selectedDate, sessionsByDate) {
        sessionsByDate[selectedDate].orEmpty().sortedByDescending { it.startTime }
    }

    LaunchedEffect(selectedDate, selectedDaySessions) {
        if (selectedDaySessions.isNotEmpty()) {
            if (selectedDaySessions.none { it.id == selectedId }) {
                selectedId = selectedDaySessions.first().id
            }
        } else {
            selectedId = -1L
        }
    }

    LaunchedEffect(Unit) {
        dao.observeAll().collectLatest { list ->
            sessions = list.filter { it.isMeaningfulWorkoutSession() }
            if (selectedId == -1L && sessions.isNotEmpty()) {
                selectedId = sessions.firstOrNull()?.id ?: -1L
            }
            if (highlightedSessionId != -1L && sessions.any { it.id == highlightedSessionId }) {
                selectedId = highlightedSessionId
                sessions.firstOrNull { it.id == highlightedSessionId }?.let { highlighted ->
                    selectedDate = highlighted.date
                }
            }
        }
    }

    val todaySessions = remember(sessions, todayKey) {
        sessions.filter { it.date == todayKey }
    }

    val todayMinutes = remember(todaySessions) { todaySessions.sumOf { it.durationMinutes } }
    val todaySteps = remember(todaySessions) { todaySessions.sumOf { it.steps } }
    val todayDistanceMeters = remember(todaySessions) { todaySessions.sumOf { it.distanceMeters } }
    val todayCalories = remember(todaySessions) { todaySessions.sumOf { it.caloriesKcal } }
    val selectedSession = remember(selectedDaySessions, selectedId) {
        selectedDaySessions.firstOrNull { it.id == selectedId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.workouts_title),
                            color = cs.onBackground,
                            fontSize = 23.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.2).sp
                        )
                        Text(
                            text = stringResource(R.string.workouts_open_subtitle),
                            color = cs.onBackground.copy(alpha = 0.72f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            WorkoutPremiumBackground()

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(bottom = 28.dp)
            ) {
                item {
                    WorkoutHeroCard(
                        minutes = todayMinutes,
                        steps = todaySteps,
                        distanceMeters = todayDistanceMeters,
                        calories = todayCalories,
                        sessionCount = todaySessions.size,
                        cardBg = cardBg,
                        neonA = neonA,
                        neonB = neonB
                    )
                }

                item {
                    WorkoutTodayStatsRow(
                        todaySessions = todaySessions,
                        cardBg = cardBg,
                        panelBg = panelBg
                    )
                }

                item {
                    SelectedWorkoutCard(
                        session = selectedSession,
                        cardBg = cardBg,
                        panelBg = panelBg,
                        neonA = neonA,
                        neonB = neonB
                    )
                }

                item {
                    WorkoutsWeeklyInsightCard(
                        sessions = sessions,
                        cardBg = cardBg,
                        panelBg = panelBg,
                        panelBgStrong = panelBgStrong,
                        borderSoft = borderSoft,
                        neonA = neonA,
                        neonB = neonB
                    )
                }

                item {
                    WorkoutWeekTimelineCard(
                        weekDays = currentWeekDays,
                        sessionsByDate = sessionsByDate,
                        selectedDate = selectedDate,
                        onSelectDate = { selectedDate = it },
                        onPrevWeek = { selectedWeekOffset -= 1 },
                        onNextWeek = {
                            if (selectedWeekOffset < 0) selectedWeekOffset += 1
                        },
                        cardBg = cardBg,
                        panelBg = panelBg,
                        borderSoft = borderSoft
                    )
                }

                item {
                    SelectedDaySummaryCard(
                        selectedDate = selectedDate,
                        sessions = selectedDaySessions,
                        totalMinutes = selectedDaySessions.sumOf { it.durationMinutes },
                        totalSteps = selectedDaySessions.sumOf { it.steps },
                        totalDistanceMeters = selectedDaySessions.sumOf { it.distanceMeters },
                        totalCalories = selectedDaySessions.sumOf { it.caloriesKcal },
                        cardBg = cardBg,
                        panelBg = panelBgStrong
                    )
                }

                item {
                    SelectedDaySessionsCard(
                        sessions = selectedDaySessions,
                        selectedSessionId = selectedId,
                        onSelectSession = { selectedId = it },
                        cardBg = cardBg,
                        borderSoft = borderSoft
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutHeroCard(
    minutes: Int,
    steps: Int,
    distanceMeters: Int,
    calories: Int,
    sessionCount: Int,
    cardBg: Color,
    neonA: Color,
    neonB: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val km = distanceMeters / 1000f
    val df = remember { DecimalFormat("#.#") }

    val movementScore = remember(minutes, steps) {
        (
                (minutes.coerceAtMost(90) / 90f) * 45f +
                        (steps.coerceAtMost(6000) / 6000f) * 55f
                ).toInt().coerceIn(0, 100)
    }

    val progress by animateFloatAsState(
        targetValue = movementScore / 100f,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "workoutHeroProgress"
    )

    val heroSurface = if (isDark) {
        Brush.verticalGradient(
            listOf(
                neonB.copy(alpha = 0.14f),
                neonA.copy(alpha = 0.10f),
                cardBg
            )
        )
    } else {
        // ✅ Light: Daha canlı gradyan
        Brush.verticalGradient(
            listOf(
                Color(0xFFDCF2FF),  // Açık sky blue
                Color(0xFFD1FAE5),  // Açık emerald
                Color.White
            )
        )
    }

    val metricBg = if (isDark) Color(0xFF131821) else Color.White

    HeroSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(heroSurface)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.workouts_today_summary),
                        color = cs.onSurface.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.2.sp
                    )
                    Text(
                        text = if (minutes > 0) "$minutes min" else "0 min",
                        color = cs.onSurface,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp
                    )
                    Text(
                        text = "$sessionCount ${stringResource(R.string.workouts_sessions_label).lowercase(Locale.getDefault())} • $steps ${stringResource(R.string.workouts_steps_label).lowercase(Locale.getDefault())}",
                        color = cs.onSurface.copy(alpha = 0.68f),
                        fontSize = 12.sp
                    )
                }

                Box(
                    modifier = Modifier.size(90.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val stroke = 9f
                        drawArc(
                            color = cs.onSurface.copy(alpha = 0.08f),
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = stroke,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                        drawArc(
                            brush = Brush.sweepGradient(listOf(neonA, neonB, neonA)),
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(
                                width = stroke,
                                cap = androidx.compose.ui.graphics.StrokeCap.Round
                            )
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = movementScore.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )
                        Text(
                            text = stringResource(R.string.workouts_score),
                            fontSize = 10.sp,
                            color = cs.onSurface.copy(alpha = 0.60f)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WorkoutMetricCapsule(
                    icon = Icons.Outlined.Route,
                    label = stringResource(R.string.workouts_today_distance),
                    value = "${df.format(km)} km",
                    modifier = Modifier.weight(1f),
                    bg = metricBg
                )
                WorkoutMetricCapsule(
                    icon = Icons.Outlined.LocalFireDepartment,
                    label = stringResource(R.string.workouts_today_calories),
                    value = "$calories kcal",
                    modifier = Modifier.weight(1f),
                    bg = metricBg
                )
            }
        }
    }
}


@Composable
private fun WorkoutPremiumBackground(
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val lightA = if (isDark) {
        cs.primary.copy(alpha = 0.10f)
    } else {
        Color(0xFF0EA5E9).copy(alpha = 0.08f)  // ✅ Daha güçlü mavi glow
    }

    val lightB = if (isDark) {
        cs.secondary.copy(alpha = 0.08f)
    } else {
        Color(0xFF10B981).copy(alpha = 0.06f)  // ✅ Hafif yeşil glow
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        cs.background,
                        cs.background.copy(alpha = 0.992f),
                        cs.background
                    )
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sol üst ana ışık
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(lightA, Color.Transparent),
                    center = Offset(w * 0.16f, h * 0.10f),
                    radius = w * 0.48f
                ),
                radius = w * 0.48f,
                center = Offset(w * 0.16f, h * 0.10f)
            )

            // Sağ üst ikincil çok hafif ışık
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(lightB, Color.Transparent),
                    center = Offset(w * 0.82f, h * 0.24f),
                    radius = w * 0.34f
                ),
                radius = w * 0.34f,
                center = Offset(w * 0.82f, h * 0.24f)
            )
        }

        WorkoutNoiseOverlay(
            modifier = Modifier.fillMaxSize(),
            alpha = if (isDark) 0.030f else 0.012f
        )
    }
}

@Composable
private fun WorkoutNoiseOverlay(
    modifier: Modifier = Modifier,
    alpha: Float
) {
    val points = remember {
        List(180) {
            Offset(
                x = kotlin.random.Random.nextFloat(),
                y = kotlin.random.Random.nextFloat()
            )
        }
    }

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        points.forEachIndexed { index, p ->
            val a = alpha * (0.65f + ((index % 5) * 0.08f))
            drawCircle(
                color = Color.White.copy(alpha = a),
                radius = 0.8f,
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}


@Composable
private fun HeroSurfaceCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(28.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bgBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                cs.surface.copy(alpha = 0.99f),
                cs.surfaceVariant.copy(alpha = 0.80f),
                cs.surface.copy(alpha = 0.98f)
            ),
            start = Offset.Zero,
            end = Offset(1200f, 1800f)
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color.White,
                Color(0xFFF9FBFF),
                Color.White
            ),
            start = Offset.Zero,
            end = Offset(1200f, 1800f)
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 16.dp else 12.dp,  // ✅ 6dp → 12dp
                shape = shape,
                clip = false,
                ambientColor = if (isDark) Color.Black else Color(0xFF0EA5E9).copy(alpha = 0.15f),  // ✅ Mavi glow
                spotColor = if (isDark) Color.Black else Color(0xFF0EA5E9).copy(alpha = 0.20f)
            )
            .clip(shape)
            .background(bgBrush)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.05f)
                else Color(0xFFEEF2F7),
                shape
            )
            .drawBehind {
                if (isDark) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.05f), Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.70f, size.height * 0.36f)
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(60f, 60f)
                    )
                }
            }
    ) {
        content()
    }
}


@Composable
private fun SecondarySurfacePanel(
    modifier: Modifier = Modifier,
    bg: Color,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val panelBrush = if (isDark) {
        Brush.linearGradient(
            listOf(
                bg.copy(alpha = 0.985f),
                bg.copy(alpha = 0.92f)
            ),
            start = Offset.Zero,
            end = Offset(400f, 480f)
        )
    } else {
        Brush.linearGradient(
            listOf(
                bg,
                bg.copy(alpha = 0.97f)
            ),
            start = Offset.Zero,
            end = Offset(400f, 480f)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(panelBrush)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.045f)
                else Color(0xFFF1F5F9),
                shape
            )
            .drawBehind {
                if (isDark) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(Color.White.copy(alpha = 0.03f), Color.Transparent),
                            start = Offset.Zero,
                            end = Offset(size.width * 0.72f, size.height * 0.38f)
                        ),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f, 40f)
                    )
                }
            }
    ) {
        content()
    }
}


@Composable
private fun PassiveSurfacePanel(
    modifier: Modifier = Modifier,
    bg: Color,
    shape: RoundedCornerShape = RoundedCornerShape(16.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val brush = if (isDark) {
        Brush.linearGradient(
            listOf(
                bg.copy(alpha = 0.96f),
                bg.copy(alpha = 0.90f)
            ),
            start = Offset.Zero,
            end = Offset(280f, 280f)
        )
    } else {
        Brush.linearGradient(
            listOf(
                bg.copy(alpha = 0.98f),
                bg.copy(alpha = 0.94f)
            ),
            start = Offset.Zero,
            end = Offset(280f, 280f)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.035f)
                else Color.Black.copy(alpha = 0.045f),
                shape
            )
    ) {
        content()
    }
}

@Composable
private fun PremiumSurfaceCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val cardGradient = if (isDark) {
        Brush.linearGradient(
            listOf(
                cs.surface.copy(alpha = 0.985f),
                cs.surfaceVariant.copy(alpha = 0.72f),
                cs.surface.copy(alpha = 0.975f)
            ),
            start = Offset.Zero,
            end = Offset(900f, 1400f)
        )
    } else {
        Brush.linearGradient(
            listOf(
                cs.surface.copy(alpha = 1f),
                cs.surfaceVariant.copy(alpha = 0.52f),
                cs.surface.copy(alpha = 0.995f)
            ),
            start = Offset.Zero,
            end = Offset(900f, 1400f)
        )
    }

    Box(
        modifier = modifier
            .shadow(
                elevation = if (isDark) 14.dp else 10.dp,  // ✅ 8dp → 10dp
                shape = shape,
                clip = false,
                ambientColor = if (isDark) Color.Black else Color(0xFF0EA5E9).copy(alpha = 0.12f),
                spotColor = if (isDark) Color.Black else Color(0xFF0EA5E9).copy(alpha = 0.18f)
            )
            .clip(shape)
            .background(cardGradient)
            .border(
                width = 1.dp,
                color = if (isDark) {
                    Color.White.copy(alpha = 0.055f)
                } else {
                    Color.Black.copy(alpha = 0.06f)
                },
                shape = shape
            )
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = 0.05f)
                            } else {
                                Color.White.copy(alpha = 0.38f)
                            },
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.75f, size.height * 0.42f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(34f, 34f)
                )
            }
    ) {
        content()
    }
}

@Composable
private fun WorkoutTodayStatsRow(
    todaySessions: List<WorkoutSession>,
    cardBg: Color,
    panelBg: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val totalSessions = todaySessions.size
    val avgPace = if (todaySessions.isNotEmpty()) {
        todaySessions.map { it.avgStepsPerMinute }.average().toInt()
    } else {
        0
    }
    val longest = todaySessions.maxByOrNull { it.durationMinutes }?.durationMinutes ?: 0

    PremiumSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TodayStatPill(
                title = stringResource(R.string.workouts_sessions_label),
                value = totalSessions.toString(),
                modifier = Modifier.weight(1f),
                bg = panelBg
            )

            TodayStatPill(
                title = stringResource(R.string.workouts_avg_pace),
                value = if (avgPace > 0) "$avgPace spm" else "-",
                modifier = Modifier.weight(1f),
                bg = panelBg
            )

            TodayStatPill(
                title = stringResource(R.string.workouts_longest_label),
                value = if (longest > 0) "$longest min" else "-",
                modifier = Modifier.weight(1f),
                bg = panelBg
            )
        }
    }
}

@Composable
private fun TodayStatPill(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    bg: Color
) {
    val cs = MaterialTheme.colorScheme

    SecondarySurfacePanel(
        modifier = modifier.height(84.dp),
        bg = bg,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = cs.onSurface.copy(alpha = 0.60f),
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )

            Text(
                text = value,
                color = cs.onSurface,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SelectedWorkoutCard(
    session: WorkoutSession?,
    cardBg: Color,
    panelBg: Color,
    neonA: Color,
    neonB: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val df = remember { DecimalFormat("#.#") }

    val topPanelBg = if (isDark) Color(0xFF121822) else Color(0xFFF6F9FD)
    val bottomPanelBg = if (isDark) Color(0xFF151B24) else Color(0xFFF0F4FA)

    HeroSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.workouts_selected_session),
                        color = cs.onSurface.copy(alpha = 0.68f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.3.sp
                    )
                    Text(
                        text = stringResource(R.string.workouts_focused_session_details),
                        color = cs.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            if (isDark) {
                                Brush.horizontalGradient(
                                    listOf(
                                        neonA.copy(alpha = 0.18f),
                                        neonB.copy(alpha = 0.18f)
                                    )
                                )
                            } else {
                                // ✅ Light: Daha kuvvetli gradyan
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF10B981).copy(alpha = 0.25f),
                                        Color(0xFF0EA5E9).copy(alpha = 0.25f)
                                    )
                                )
                            }
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = if (session != null) {
                            stringResource(R.string.workouts_live_selection)
                        } else {
                            stringResource(R.string.workouts_no_session)
                        },
                        color = cs.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            if (session == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    topPanelBg.copy(alpha = 0.98f),
                                    topPanelBg.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .padding(20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.workouts_no_sessions),
                        color = cs.onSurface.copy(alpha = 0.65f),
                        fontSize = 13.sp
                    )
                }
            } else {
                val distanceKm = session.distanceMeters / 1000f
                val start = timeFormat.format(Date(session.startTime))
                val end = timeFormat.format(Date(session.endTime))
                val dateText = dateFormat.format(Date(session.startTime))

                val intensityHigh = stringResource(R.string.workouts_intensity_high)
                val intensityModerate = stringResource(R.string.workouts_intensity_moderate)
                val intensityLight = stringResource(R.string.workouts_intensity_light)

                val intensityLabel = when {
                    session.avgStepsPerMinute >= 120 && session.durationMinutes >= 25 -> intensityHigh
                    session.avgStepsPerMinute >= 100 -> intensityModerate
                    else -> intensityLight
                }

                val intensityColor = when (intensityLabel) {
                    intensityHigh -> Color(0xFFFF6B6B)
                    intensityModerate -> Color(0xFFFFB74D)
                    else -> Color(0xFF00FFA3)
                }

                val coachingText = when {
                    session.durationMinutes >= 40 && session.avgStepsPerMinute >= 110 ->
                        stringResource(R.string.workouts_coaching_strong)
                    session.durationMinutes >= 20 && session.avgStepsPerMinute >= 95 ->
                        stringResource(R.string.workouts_coaching_balanced)
                    session.durationMinutes < 15 ->
                        stringResource(R.string.workouts_coaching_short)
                    else ->
                        stringResource(R.string.workouts_coaching_steady)
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    topPanelBg.copy(alpha = 0.98f),
                                    topPanelBg.copy(alpha = 0.92f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.04f) else Color(0x120F172A),
                            RoundedCornerShape(24.dp)
                        )
                        .padding(18.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = dateText,
                            color = cs.onSurface.copy(alpha = 0.68f),
                            fontSize = 12.sp
                        )

                        Text(
                            text = "${session.durationMinutes} min • ${session.steps} steps",
                            color = cs.onSurface,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.3).sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            WorkoutInfoTile(
                                title = stringResource(R.string.workouts_avg_pace),
                                value = "${session.avgStepsPerMinute} spm",
                                modifier = Modifier.weight(1f),
                                bg = bottomPanelBg
                            )
                            WorkoutInfoTile(
                                title = stringResource(R.string.workouts_today_distance),
                                value = "${df.format(distanceKm)} km",
                                modifier = Modifier.weight(1f),
                                bg = bottomPanelBg
                            )
                            WorkoutInfoTile(
                                title = stringResource(R.string.workouts_today_calories),
                                value = "${session.caloriesKcal} kcal",
                                modifier = Modifier.weight(1f),
                                bg = bottomPanelBg
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        intensityColor.copy(alpha = 0.18f),
                                        intensityColor.copy(alpha = 0.10f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                intensityColor.copy(alpha = 0.18f),
                                RoundedCornerShape(999.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = intensityLabel,
                            color = intensityColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = stringResource(R.string.workouts_source_auto),
                        color = cs.onSurface.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    neonA.copy(alpha = 0.08f),
                                    neonB.copy(alpha = 0.08f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.03f) else Color(0x100F172A),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(16.dp)
                ) {
                    Text(
                        text = coachingText,
                        color = cs.onSurface.copy(alpha = 0.80f),
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkoutInfoTile(
                        title = stringResource(R.string.workouts_started_at),
                        value = start,
                        modifier = Modifier.weight(1f),
                        bg = bottomPanelBg
                    )
                    WorkoutInfoTile(
                        title = stringResource(R.string.workouts_finished_at),
                        value = end,
                        modifier = Modifier.weight(1f),
                        bg = bottomPanelBg
                    )
                    WorkoutInfoTile(
                        title = stringResource(R.string.workouts_session_source),
                        value = stringResource(R.string.workouts_source_auto),
                        modifier = Modifier.weight(1f),
                        bg = bottomPanelBg
                    )
                }
            }
        }
    }
}


@Composable
private fun WorkoutMetricCapsule(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    bg: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val parts = remember(value) {
        val chunks = value.trim().split(" ")
        when {
            chunks.size >= 2 -> {
                val unit = chunks.last()
                val main = chunks.dropLast(1).joinToString(" ")
                main to unit
            }
            else -> value to ""
        }
    }

    Row(
        modifier = modifier
            .height(88.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        bg.copy(alpha = 0.98f),
                        bg.copy(alpha = 0.92f)
                    ),
                    start = Offset.Zero,
                    end = Offset(300f, 300f)
                )
            )
            .drawBehind {
                // ✅ 1. İç ışıltı (her iki tema için)
                drawRoundRect(
                    brush = Brush.linearGradient(
                        listOf(
                            Color.White.copy(alpha = if (isDark) 0.04f else 0.10f),
                            Color.Transparent
                        ),
                        start = Offset.Zero,
                        end = Offset(size.width * 0.7f, size.height * 0.4f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f)
                )

                // ✅ 2. Dış gradient border (sadece light tema)
                if (!isDark) {
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF0EA5E9).copy(alpha = 0.25f),
                                Color(0xFF10B981).copy(alpha = 0.20f)
                            )
                        ),
                        size = size,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(40f, 40f),
                        style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f)
                    )
                }
            }
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.05f) else Color.Transparent,  // ✅ Light'ta transparent (gradient border kullanıyoruz)
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF00F5FF).copy(alpha = 0.14f),
                            Color(0xFF00FFA3).copy(alpha = 0.12f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color(0xFF00CFEA),
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.fillMaxHeight(),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                color = cs.onSurface.copy(alpha = 0.62f),
                fontSize = 11.sp,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(4.dp))

            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = parts.first,
                    color = cs.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1
                )

                if (parts.second.isNotBlank()) {
                    Text(
                        text = parts.second,
                        color = cs.onSurface.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutInfoTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    bg: Color
) {
    val cs = MaterialTheme.colorScheme

    SecondarySurfacePanel(
        modifier = modifier.heightIn(min = 112.dp),
        bg = bg,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = cs.onSurface.copy(alpha = 0.60f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                minLines = 2,
                maxLines = 2
            )

            Text(
                text = value,
                color = cs.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 18.sp,
                minLines = 2,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun WorkoutsWeeklyInsightCard(
    sessions: List<WorkoutSession>,
    cardBg: Color,
    panelBg: Color,
    panelBgStrong: Color,
    borderSoft: Color,
    neonA: Color,
    neonB: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val last7 = remember(sessions) { sessions.take(7) }

    val totalMinutes = remember(last7) { last7.sumOf { it.durationMinutes } }
    val totalSteps = remember(last7) { last7.sumOf { it.steps } }
    val totalCalories = remember(last7) { last7.sumOf { it.caloriesKcal } }
    val avgPace = remember(last7) {
        if (last7.isNotEmpty()) last7.map { it.avgStepsPerMinute }.average().toInt() else 0
    }

    val bestSession = remember(last7) { last7.maxByOrNull { it.steps } }

    val consistencyLabel = when {
        last7.size >= 5 -> stringResource(R.string.workouts_consistency_strong)
        last7.size >= 3 -> stringResource(R.string.workouts_consistency_building)
        last7.isNotEmpty() -> stringResource(R.string.workouts_consistency_starting)
        else -> stringResource(R.string.workouts_consistency_none)
    }

    val weeklyState = when {
        last7.isEmpty() -> stringResource(R.string.workouts_no_recent_activity)
        totalMinutes >= 150 && avgPace >= 105 -> stringResource(R.string.workouts_high_momentum)
        totalMinutes >= 90 -> stringResource(R.string.workouts_balanced_week)
        totalMinutes >= 40 -> stringResource(R.string.workouts_building_routine)
        else -> stringResource(R.string.workouts_low_activity)
    }

    val coachNote = when {
        last7.isEmpty() -> stringResource(R.string.workouts_weekly_note_empty)
        totalMinutes >= 150 && avgPace >= 105 -> stringResource(R.string.workouts_weekly_note_strong)
        totalMinutes >= 90 -> stringResource(R.string.workouts_weekly_note_balanced)
        totalMinutes >= 40 -> stringResource(R.string.workouts_weekly_note_building)
        else -> stringResource(R.string.workouts_weekly_note_light)
    }

    HeroSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = stringResource(R.string.workouts_weekly_insight),
                        color = cs.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = stringResource(R.string.workouts_weekly_insight_subtitle),
                        color = cs.onSurface.copy(alpha = 0.65f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    neonA.copy(alpha = 0.18f),
                                    neonB.copy(alpha = 0.18f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = weeklyState,
                        color = cs.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkoutInsightMini(
                        title = stringResource(R.string.workouts_duration_label),
                        value = "$totalMinutes min",
                        modifier = Modifier.weight(1f),
                        bg = panelBg
                    )
                    WorkoutInsightMini(
                        title = stringResource(R.string.workouts_avg_pace),
                        value = if (avgPace > 0) "$avgPace spm" else "-",
                        modifier = Modifier.weight(1f),
                        bg = panelBg
                    )
                    WorkoutInsightMini(
                        title = stringResource(R.string.workouts_sessions_label),
                        value = last7.size.toString(),
                        modifier = Modifier.weight(1f),
                        bg = panelBg
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WorkoutInsightMini(
                        title = stringResource(R.string.workouts_steps_label),
                        value = totalSteps.toString(),
                        modifier = Modifier.weight(1f),
                        bg = panelBgStrong
                    )
                    WorkoutInsightMini(
                        title = stringResource(R.string.workouts_calories_label),
                        value = "$totalCalories kcal",
                        modifier = Modifier.weight(1f),
                        bg = panelBgStrong
                    )
                    WorkoutInsightMini(
                        title = stringResource(R.string.consistency),
                        value = consistencyLabel,
                        modifier = Modifier.weight(1f),
                        bg = panelBgStrong
                    )
                }
            }

            if (bestSession != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    neonA.copy(alpha = 0.10f),
                                    neonB.copy(alpha = 0.10f)
                                )
                            )
                        )
                        .border(
                            1.dp,
                            if (isDark) Color.White.copy(alpha = 0.03f) else Color(0x100F172A),
                            RoundedCornerShape(22.dp)
                        )
                        .padding(16.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.workouts_best_recent_session),
                            color = cs.onSurface.copy(alpha = 0.66f),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${bestSession.steps} steps • ${bestSession.durationMinutes} min • ${bestSession.avgStepsPerMinute} spm",
                            color = cs.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0xFF121822) else Color(0xFFF4F8FD),
                                if (isDark) Color(0xFF10151C) else Color(0xFFF8FBFE)
                            )
                        )
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = coachNote,
                    color = cs.onSurface.copy(alpha = 0.82f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun WorkoutInsightMini(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    bg: Color
) {
    val cs = MaterialTheme.colorScheme

    // ✅ Başlık uzunluğuna göre font boyutu
    val titleFontSize = when {
        title.length >= 12 -> 9.sp   // "Consistency" gibi uzun başlıklar
        title.length >= 8 -> 9.5.sp
        else -> 10.sp
    }

    val valueFontSize = when {
        value.length >= 10 -> 11.sp  // ✅ Biraz daha küçük
        value.length >= 7 -> 12.sp
        else -> 14.sp  // ✅ 15sp → 14sp
    }

    SecondarySurfacePanel(
        modifier = modifier.height(102.dp),
        bg = bg,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 10.dp, vertical = 10.dp),  // ✅ 12dp → 10dp
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = cs.onSurface.copy(alpha = 0.60f),
                fontSize = titleFontSize,  // ✅ Dinamik font
                lineHeight = 11.sp,  // ✅ 12sp → 11sp
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis  // ✅ Taşarsa ...
            )

            Text(
                text = value,
                color = cs.onSurface,
                fontSize = valueFontSize,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 16.sp,  // ✅ 18sp → 16sp
                maxLines = 2,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis  // ✅ Taşarsa ...
            )
        }
    }
}

@Composable
private fun WorkoutWeekTimelineCard(
    weekDays: List<LocalDate>,
    sessionsByDate: Map<String, List<WorkoutSession>>,
    selectedDate: String,
    onSelectDate: (String) -> Unit,
    onPrevWeek: () -> Unit,
    onNextWeek: () -> Unit,
    cardBg: Color,
    panelBg: Color,
    borderSoft: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val formatter = remember { DateTimeFormatter.ofPattern("yyyy-MM-dd") }
    val shortDayFormatter = remember { DateTimeFormatter.ofPattern("EEE", Locale.getDefault()) }

    val dayTotals = remember(weekDays, sessionsByDate) {
        weekDays.map { day ->
            val key = day.format(formatter)
            val daySessions = sessionsByDate[key].orEmpty()
            val totalMinutes = daySessions.sumOf { it.durationMinutes }
            val totalSteps = daySessions.sumOf { it.steps }
            Triple(day, key, totalMinutes to totalSteps)
        }
    }

    val maxMinutes = remember(dayTotals) {
        (dayTotals.maxOfOrNull { it.third.first } ?: 0).coerceAtLeast(1)
    }

    val selectedDayData = remember(selectedDate, sessionsByDate) {
        val sessions = sessionsByDate[selectedDate].orEmpty()
        Triple(
            sessions.size,
            sessions.sumOf { it.durationMinutes },
            sessions.sumOf { it.steps }
        )
    }

    val weekRangeText = remember(weekDays) {
        val first = weekDays.firstOrNull()
        val last = weekDays.lastOrNull()
        if (first != null && last != null) {
            "${first.dayOfMonth}/${first.monthValue}" to "${last.dayOfMonth}/${last.monthValue}"
        } else {
            "" to ""
        }
    }

    val weeklyTotalMinutes = dayTotals.sumOf { it.third.first }

    val stateLabel = when {
        weeklyTotalMinutes >= 180 -> stringResource(R.string.workouts_high_momentum)
        weeklyTotalMinutes >= 90 -> stringResource(R.string.workouts_balanced_week)
        weeklyTotalMinutes >= 40 -> stringResource(R.string.workouts_building_routine)
        else -> stringResource(R.string.workouts_low_activity)
    }

    HeroSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.workouts_workout_timeline),
                        color = cs.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = stringResource(
                            R.string.workouts_week_range,
                            weekRangeText.first,
                            weekRangeText.second
                        ),
                        color = if (isDark) Color(0xFF00F5FF) else Color(0xFF0EA5E9),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp
                    )
                    Text(
                        text = stringResource(R.string.workouts_weekly_strip_subtitle),
                        color = cs.onSurface.copy(alpha = 0.62f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 15.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    Color(0xFF00F5FF).copy(alpha = 0.16f),
                                    Color(0xFF00FFA3).copy(alpha = 0.12f)
                                )
                            )
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp)
                ) {
                    Text(
                        text = stateLabel,
                        color = cs.onSurface,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                MiniWeekAction(
                    text = "← ${stringResource(R.string.workouts_previous_week)}",
                    onClick = onPrevWeek
                )
                MiniWeekAction(
                    text = "${stringResource(R.string.workouts_next_week)} →",
                    onClick = onNextWeek,
                    enabled = weekDays.last() < LocalDate.now(ZoneId.systemDefault())
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (isDark) Color(0xFF10151D) else Color(0xFFF4F8FD),
                                if (isDark) Color(0xFF0F131A) else Color(0xFFF8FBFF)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.03f) else Color(0x100F172A),
                        RoundedCornerShape(24.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    dayTotals.forEach { (day, key, pair) ->
                        val totalMinutes = pair.first
                        val ratio = (totalMinutes.toFloat() / maxMinutes.toFloat()).coerceIn(0f, 1f)
                        val selected = key == selectedDate
                        val isToday = day == LocalDate.now(ZoneId.systemDefault())
                        val hasWorkout = totalMinutes > 0

                        val animatedBarHeight by animateFloatAsState(
                            targetValue = if (hasWorkout) (16f + (ratio * 42f)) else 10f,
                            animationSpec = tween(550, easing = FastOutSlowInEasing),
                            label = "timelineBarHeight"
                        )

                        val animatedWidth by animateFloatAsState(
                            targetValue = if (selected) 18f else 10f,
                            animationSpec = tween(250, easing = FastOutSlowInEasing),
                            label = "timelineBarWidth"
                        )

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onSelectDate(key) },
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.height(18.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected && hasWorkout) {
                                    Text(
                                        text = "${totalMinutes}${stringResource(R.string.m)}",
                                        color = if (isDark) Color(0xFF00F5FF) else Color(0xFF0EA5E9),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .width(28.dp)
                                    .height(78.dp),
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                Box(
                                    modifier = Modifier
                                        .width(10.dp)
                                        .height(78.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            if (isDark) Color(0xFF1D2430) else Color(0xFFD9E4F2)
                                        )
                                )

                                Box(
                                    modifier = Modifier
                                        .width(animatedWidth.dp)
                                        .height(animatedBarHeight.dp)
                                        .clip(RoundedCornerShape(999.dp))
                                        .background(
                                            when {
                                                selected -> Brush.verticalGradient(
                                                    listOf(Color(0xFF00F5FF), Color(0xFF00FFA3))
                                                )
                                                hasWorkout -> Brush.verticalGradient(
                                                    listOf(
                                                        Color(0xFF00F5FF).copy(alpha = 0.60f),
                                                        Color(0xFF00FFA3).copy(alpha = 0.48f)
                                                    )
                                                )
                                                else -> Brush.verticalGradient(
                                                    listOf(
                                                        cs.onSurface.copy(alpha = 0.08f),
                                                        cs.onSurface.copy(alpha = 0.04f)
                                                    )
                                                )
                                            }
                                        )
                                )

                                if (isToday) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopCenter)
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (selected) Color(0xFF00F5FF)
                                                else cs.primary.copy(alpha = 0.75f)
                                            )
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = day.format(shortDayFormatter),
                                    fontSize = 10.sp,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) cs.onSurface else cs.onSurface.copy(alpha = 0.60f),
                                    maxLines = 1
                                )
                                Text(
                                    text = "${day.dayOfMonth}/${day.monthValue}",
                                    fontSize = 10.sp,
                                    color = if (isToday)
                                        (if (isDark) Color(0xFF00F5FF) else Color(0xFF0EA5E9))
                                    else
                                        cs.onSurface.copy(alpha = 0.60f),
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                WeekQuickInfoChip(
                    text = stringResource(R.string.workouts_week_sessions_short, selectedDayData.first),
                    modifier = Modifier.weight(1f)
                )
                WeekQuickInfoChip(
                    text = stringResource(R.string.workouts_week_duration_short, selectedDayData.second),
                    modifier = Modifier.weight(1f)
                )
                WeekQuickInfoChip(
                    text = stringResource(R.string.workouts_week_steps_short, selectedDayData.third),
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}


@Composable
private fun WeekQuickInfoChip(
    text: String,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val bg = if (isDark) Color(0xFF12161E) else Color(0xFFF4F8FD)

    PassiveSurfacePanel(
        modifier = modifier.height(42.dp),
        bg = bg,
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = text,
            color = cs.onSurface.copy(alpha = 0.80f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}


@Composable
private fun MiniWeekAction(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        if (isDark) Color(0xFF12161E).copy(alpha = 0.98f) else Color.White,
                        if (isDark) Color(0xFF10141A).copy(alpha = 0.92f) else Color(0xFFF8FBFE)
                    )
                )
            )
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.05f) else Color.White.copy(alpha = 0.28f),
                RoundedCornerShape(999.dp)
            )
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            color = if (enabled) cs.onSurface else cs.onSurface.copy(alpha = 0.30f),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
        )
    }
}


@Composable
private fun SelectedDaySummaryCard(
    selectedDate: String,
    sessions: List<WorkoutSession>,
    totalMinutes: Int,
    totalSteps: Int,
    totalDistanceMeters: Int,
    totalCalories: Int,
    cardBg: Color,
    panelBg: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val df = remember { DecimalFormat("#.#") }
    val km = totalDistanceMeters / 1000f

    val dayScore = remember(totalMinutes, totalSteps) {
        (((totalMinutes.coerceAtMost(90) / 90f) * 40f) +
                ((totalSteps.coerceAtMost(6000) / 6000f) * 60f)).toInt().coerceIn(0, 100)
    }

    val summaryState = when {
        sessions.isEmpty() -> stringResource(R.string.workouts_day_state_no_sessions)
        totalMinutes >= 60 && totalSteps >= 4000 -> stringResource(R.string.workouts_day_state_strong)
        totalMinutes >= 30 -> stringResource(R.string.workouts_day_state_balanced)
        else -> stringResource(R.string.workouts_day_state_light)
    }

    HeroSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.workouts_selected_day),
                        color = cs.onSurface.copy(alpha = 0.65f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.25.sp
                    )
                    Text(
                        text = selectedDate,
                        color = cs.onSurface,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.2).sp
                    )
                    Text(
                        text = stringResource(R.string.workouts_selected_day_subtitle),
                        color = cs.onSurface.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        lineHeight = 16.sp
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Color(0xFF00F5FF).copy(alpha = 0.16f),
                                        Color(0xFF00FFA3).copy(alpha = 0.14f)
                                    )
                                )
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "$dayScore/100",
                            color = if (isDark) Color(0xFF00F5FF) else Color(0xFF0EA5E9),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = summaryState,
                        color = cs.onSurface.copy(alpha = 0.65f),
                        fontSize = 11.sp
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectedDayStatTile(
                    title = stringResource(R.string.workouts_steps_label),
                    value = totalSteps.toString(),
                    modifier = Modifier.weight(1f),
                    bg = panelBg
                )
                SelectedDayStatTile(
                    title = stringResource(R.string.workouts_distance_label),
                    value = "${df.format(km)} km",
                    modifier = Modifier.weight(1f),
                    bg = panelBg
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                SelectedDayStatTile(
                    title = stringResource(R.string.workouts_calories_label),
                    value = "$totalCalories kcal",
                    modifier = Modifier.weight(1f),
                    bg = panelBg
                )
                SelectedDayStatTile(
                    title = stringResource(R.string.workouts_duration_label),
                    value = "$totalMinutes min",
                    modifier = Modifier.weight(1f),
                    bg = panelBg
                )
            }
        }
    }
}

@Composable
private fun SelectedDayStatTile(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    bg: Color
) {
    val cs = MaterialTheme.colorScheme
    val valueFontSize = if (value.length >= 8) 12.sp else 15.sp

    SecondarySurfacePanel(
        modifier = modifier.height(94.dp),
        bg = bg,
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                color = cs.onSurface.copy(alpha = 0.60f),
                fontSize = 10.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                minLines = 2,
                maxLines = 2
            )

            Text(
                text = value,
                color = cs.onSurface,
                fontSize = valueFontSize,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}


@Composable
private fun SelectedDaySessionsCard(
    sessions: List<WorkoutSession>,
    selectedSessionId: Long,
    onSelectSession: (Long) -> Unit,
    cardBg: Color,
    borderSoft: Color
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    HeroSurfaceCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = stringResource(R.string.workouts_sessions_on_selected_day),
                    color = cs.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.15).sp
                )
                Text(
                    text = stringResource(R.string.workouts_tap_session_hint),
                    color = cs.onSurface.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp
                )
            }

            if (sessions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(
                                    if (isDark) Color(0xFF12161E).copy(alpha = 0.98f) else Color(0xFFF3F6FB),
                                    if (isDark) Color(0xFF10141A).copy(alpha = 0.92f) else Color(0xFFF8FBFE)
                                )
                            )
                        )
                        .padding(18.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.workouts_no_sessions),
                        color = cs.onSurface.copy(alpha = 0.65f),
                        fontSize = 12.sp
                    )
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    sessions.forEach { session ->
                        SelectedDaySessionRow(
                            session = session,
                            selected = session.id == selectedSessionId,
                            onClick = { onSelectSession(session.id) }
                        )
                    }
                }
            }
        }
    }
}

private fun WorkoutSession.safeEndTime(): Long {
    val computedEnd = startTime + (durationMinutes.coerceAtLeast(1) * 60_000L)
    return if (endTime > startTime) endTime else computedEnd
}

private fun WorkoutSession.isMeaningfulWorkoutSession(): Boolean {
    if (source == "test") return false
    return steps >= 500 || durationMinutes >= 5
}

@Composable
private fun SelectedDaySessionRow(
    session: WorkoutSession,
    selected: Boolean,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    val start = timeFormat.format(Date(session.startTime))
    val end = timeFormat.format(Date(session.endTime))

    val intensityHigh = stringResource(R.string.workouts_intensity_high)
    val intensityModerate = stringResource(R.string.workouts_intensity_moderate)
    val intensityLight = stringResource(R.string.workouts_intensity_light)

    val intensityLabel = when {
        session.avgStepsPerMinute >= 120 && session.durationMinutes >= 25 -> intensityHigh
        session.avgStepsPerMinute >= 95 -> intensityModerate
        else -> intensityLight
    }

    val intensityColor = when (intensityLabel) {
        intensityHigh -> Color(0xFFFF6B6B)
        intensityModerate -> Color(0xFFFFB74D)
        else -> Color(0xFF00FFA3)
    }

    val rowBg = when {
        selected && isDark -> Color(0xFF16202A)
        selected && !isDark -> Color(0xFFF0F9FF)
        !selected && isDark -> Color(0xFF11151C)
        else -> if (isDark) Color(0xFF11151C) else Color(0xFFFBFCFE)
    }

    val borderColor = if (selected) {
        if (isDark) Color(0xFF34E6FF) else Color(0xFF16B8FF)
    } else {
        if (isDark) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.05f)
    }

    val scale by animateFloatAsState(
        targetValue = if (selected) 1.015f else 1f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "selectedDaySessionScale"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            rowBg.copy(alpha = 0.98f),
                            rowBg.copy(alpha = 0.92f)
                        ),
                        start = Offset.Zero,
                        end = Offset(400f, 260f)
                    )
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(intensityColor)
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$start - $end",
                    color = cs.onSurface.copy(alpha = 0.65f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "${session.steps}",
                        color = cs.onSurface,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.workouts_steps_label).lowercase(Locale.getDefault()),
                        color = cs.onSurface.copy(alpha = 0.78f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "• ${session.durationMinutes} min",
                        color = cs.onSurface.copy(alpha = 0.72f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = "${session.avgStepsPerMinute} spm • ${session.caloriesKcal} kcal",
                    color = cs.onSurface.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 15.sp
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                intensityColor.copy(alpha = 0.18f),
                                intensityColor.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .border(
                        1.dp,
                        intensityColor.copy(alpha = 0.18f),
                        RoundedCornerShape(999.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = intensityLabel,
                    color = intensityColor,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}


@Composable
private fun PremiumInnerPanel(
    modifier: Modifier = Modifier,
    bg: Color,
    shape: RoundedCornerShape = RoundedCornerShape(18.dp),
    content: @Composable BoxScope.() -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val panelGradient = if (isDark) {
        Brush.linearGradient(
            listOf(
                bg.copy(alpha = 0.985f),
                bg.copy(alpha = 0.90f)
            ),
            start = Offset.Zero,
            end = Offset(320f, 380f)
        )
    } else {
        Brush.linearGradient(
            listOf(
                bg.copy(alpha = 1f),
                bg.copy(alpha = 0.94f)
            ),
            start = Offset.Zero,
            end = Offset(320f, 380f)
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(panelGradient)
            .border(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.045f)
                else Color.Black.copy(alpha = 0.055f),
                shape
            )
            .drawBehind {
                drawRoundRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            if (isDark) {
                                Color.White.copy(alpha = 0.035f)
                            } else {
                                Color.White.copy(alpha = 0.30f)
                            },
                            Color.Transparent
                        ),
                        start = Offset(0f, 0f),
                        end = Offset(size.width * 0.78f, size.height * 0.42f)
                    ),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(22f, 22f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}




