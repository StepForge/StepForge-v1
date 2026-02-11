package com.example.stepforge.settings

import android.app.Activity
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Stars
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.StepCounterService
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.widget.StepWidgetProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyGoalScreen(activity: Activity) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bg = cs.background
    val cardBg = cs.surface

    val neonA = if (isDark) Color(0xFF00FFA3) else Color(0xFF4FD1C5)
    val neonB = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
    val neon = Brush.horizontalGradient(listOf(neonA, neonB))

    val STEP_GOAL = intPreferencesKey("step_goal")

    var selectedGoal by remember { mutableStateOf(10000) }
    var currentGoal by remember { mutableStateOf(10000) }

    // İlk değerleri DataStore'dan yükle
    LaunchedEffect(Unit) {
        val prefs = ctx.stepforgeStore.data.first()
        val g = prefs[STEP_GOAL] ?: 10000
        currentGoal = g
        selectedGoal = g
    }

    // Değişiklikleri küçük gecikmeyle kaydet
    LaunchedEffect(selectedGoal) {
        delay(200)
        if (selectedGoal != currentGoal) {
            scope.launch {
                ctx.stepforgeStore.edit { it[STEP_GOAL] = selectedGoal }
                StepCounterService.updateTarget(selectedGoal)
                StepWidgetProvider.notifyRefresh(ctx)
                currentGoal = selectedGoal
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Daily Goal",
                            color = cs.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Tune your target for the perfect challenge.",
                            color = cs.onBackground.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { activity.finish() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
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
                .background(bg)
                .padding(pad)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                GoalSummaryCard(
                    selectedGoal = selectedGoal,
                    neonA = neonA,
                    neonB = neonB,
                    cardBg = cardBg
                )

                QuickGoalRow(
                    selected = selectedGoal,
                    onSelect = { selectedGoal = it },
                    cardBg = cardBg
                )

                FineTuneCard(
                    selected = selectedGoal,
                    onChange = { selectedGoal = it },
                    neonB = neonB,
                    cardBg = cardBg
                )

                SaveHintChip(
                    text = "Changes are saved automatically",
                    cardBg = if (isDark) cs.surfaceVariant else Color(0xFFE5E9F2)
                )

                Spacer(Modifier.height(12.dp))

                val interaction = remember { MutableInteractionSource() }
                val closeScale by animateFloatAsState(
                    targetValue = 1f,
                    animationSpec = tween(200, easing = FastOutSlowInEasing),
                    label = "closeScale"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .scale(closeScale)
                        .shadow(if (isDark) 14.dp else 6.dp, RoundedCornerShape(999.dp))
                        .background(Color.Transparent)
                        .clickable(
                            interactionSource = interaction,
                            indication = null
                        ) {
                            Toast
                                .makeText(
                                    ctx,
                                    "Goal is already saved.",
                                    Toast.LENGTH_SHORT
                                )
                                .show()
                            activity.finish()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                neon,
                                RoundedCornerShape(999.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Close",
                            color = if (isDark) Color.Black else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                InfoCard(cardBg = cardBg)
            }
        }
    }
}

@Composable
private fun GoalSummaryCard(
    selectedGoal: Int,
    neonA: Color,
    neonB: Color,
    cardBg: Color
) {
    val min = 3000f
    val max = 20000f
    val clamped = selectedGoal.coerceIn(min.toInt(), max.toInt()).toFloat()
    val progress = ((clamped - min) / (max - min)).coerceIn(0f, 1f)

    val animated by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600, easing = FastOutSlowInEasing),
        label = "goalProgress"
    )

    // DÜZELTME BURADA: Rengi Canvas dışında hesaplıyoruz
    val backgroundColor = MaterialTheme.colorScheme.background
    val isDarkBackground = backgroundColor.luminance() < 0.5f
    val trackColor = if (isDarkBackground) Color(0xFF202430) else Color(0xFFE2E8F0)

    val textPrimary = MaterialTheme.colorScheme.onSurface

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(18.dp, RoundedCornerShape(26.dp)),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Today's target",
                color = textPrimary.copy(alpha = 0.7f),
                fontSize = 13.sp
            )

            Spacer(Modifier.height(8.dp))

            Box(
                modifier = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    val strokeWidth = 18f
                    val radius = size.minDimension / 2f - strokeWidth

                    // DÜZELTME: Hesapladığımız değişkeni burada kullanıyoruz
                    drawArc(
                        color = trackColor,
                        startAngle = -210f,
                        sweepAngle = 240f,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2f,
                            (size.height - radius * 2) / 2f
                        )
                    )

                    drawArc(
                        brush = Brush.sweepGradient(
                            listOf(neonA, neonB, neonA)
                        ),
                        startAngle = -210f,
                        sweepAngle = 240f * animated,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                        size = Size(radius * 2, radius * 2),
                        topLeft = Offset(
                            (size.width - radius * 2) / 2f,
                            (size.height - radius * 2) / 2f
                        )
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Flag,
                            contentDescription = null,
                            tint = neonB,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = selectedGoal.toString(),
                            color = textPrimary,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            textAlign = TextAlign.Center
                        )
                    }
                    Text(
                        text = "steps / day",
                        color = textPrimary.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            val percentOf10k = (selectedGoal / 10000f * 100f).roundToInt()
            Text(
                text = "≈ $percentOf10k% of the classic 10,000 steps",
                color = textPrimary.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}


@Composable
private fun QuickGoalRow(
    selected: Int,
    onSelect: (Int) -> Unit,
    cardBg: Color
) {
    val textMain = MaterialTheme.colorScheme.onSurface

    val presets = listOf(
        5000 to "5K",
        7500 to "7.5K",
        10000 to "10K",
        12000 to "12K",
        15000 to "15K"
    )

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        presets.forEach { (value, label) ->
            val isSelected = value == selected
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = tween(200, easing = FastOutSlowInEasing),
                label = "presetScale"
            )
            val interaction = remember { MutableInteractionSource() }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(64.dp)
                    .scale(scale)
                    .shadow(10.dp, RoundedCornerShape(18.dp))
                    .background(
                        if (isSelected)
                            MaterialTheme.colorScheme.surfaceVariant
                        else
                            cardBg,
                        RoundedCornerShape(18.dp)
                    )
                    .clickable(
                        interactionSource = interaction,
                        indication = null
                    ) { onSelect(value) }
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF00F5FF) else textMain,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "$value",
                        color = textMain.copy(alpha = 0.6f),
                        fontSize = 10.sp,
                        maxLines = 1,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

@Composable
private fun FineTuneCard(
    selected: Int,
    onChange: (Int) -> Unit,
    neonB: Color,
    cardBg: Color
) {
    val textMain = MaterialTheme.colorScheme.onSurface
    val textSub = textMain.copy(alpha = 0.7f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Bolt,
                    contentDescription = null,
                    tint = neonB,
                    modifier = Modifier.size(18.dp)
                )
                Column {
                    Text(
                        text = "Fine tune",
                        color = textMain,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Adjust your goal in steps of 500 for a perfect fit.",
                        color = textSub,
                        fontSize = 11.sp
                    )
                }
            }

            Spacer(Modifier.height(6.dp))

            val min = 3000
            val max = 20000
            val step = 500

            var sliderValue by remember { mutableStateOf(selected.toFloat()) }
            LaunchedEffect(selected) {
                sliderValue = selected.toFloat()
            }

            Slider(
                value = sliderValue,
                onValueChange = {
                    sliderValue = it
                    val snapped = (it / step).roundToInt() * step
                    onChange(snapped.coerceIn(min, max))
                },
                valueRange = min.toFloat()..max.toFloat(),
                colors = SliderDefaults.colors(
                    thumbColor = neonB,
                    activeTrackColor = neonB,
                    inactiveTrackColor = if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
                        Color(0xFF2A2F3A) else Color(0xFFE0E5EC)
                )
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Min 3K",
                    color = textSub,
                    fontSize = 11.sp
                )
                Text(
                    text = "Max 20K",
                    color = textSub,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun SaveHintChip(
    text: String,
    cardBg: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .shadow(8.dp, RoundedCornerShape(999.dp))
                .background(cardBg, RoundedCornerShape(999.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun InfoCard(cardBg: Color) {
    val textMain = MaterialTheme.colorScheme.onSurface
    val textSub = textMain.copy(alpha = 0.78f)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Stars,
                    contentDescription = null,
                    tint = Color(0xFF00F5FF),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Insights",
                    color = textMain,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "A realistic goal is one you can reach most days. " +
                        "Start lower, then gradually increase as you build consistency.",
                color = textSub,
                fontSize = 12.sp
            )
        }
    }
}