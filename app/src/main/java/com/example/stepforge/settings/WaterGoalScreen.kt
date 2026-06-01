package com.example.stepforge.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaterGoalScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val neon = Brush.horizontalGradient(listOf(Color(0xFF00F5FF), Color(0xFF00FFA3)))


    val KEY_WATER_GOAL = intPreferencesKey("water_goal_ml")
    var goalMl by remember { mutableIntStateOf(2000) }
    var sliderVal by remember { mutableIntStateOf(2000) }

    LaunchedEffect(Unit) {
        val prefs = ctx.stepforgeStore.data.first()
        val g = prefs[KEY_WATER_GOAL] ?: 2000
        goalMl = g
        sliderVal = g
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daily Water Goal", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Goal",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = cs.onSurface
                    )
                    Text(
                        text = "$goalMl ml",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = if (isDark) Color(0xFF00F5FF) else cs.primary
                    )

                    val minMl = 1000
                    val maxMl = 4500
                    val step = 250

                    Slider(
                        value = sliderVal.toFloat(),
                        onValueChange = {
                            val snapped = ((it / step).roundToInt() * step).coerceIn(minMl, maxMl)
                            sliderVal = snapped
                            goalMl = snapped
                        },
                        valueRange = minMl.toFloat()..maxMl.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF00F5FF),
                            activeTrackColor = Color(0xFF00F5FF)
                        )
                    )

                    Button(
                        onClick = {
                            scope.launch {
                                ctx.stepforgeStore.edit { it[KEY_WATER_GOAL] = goalMl }
                                onBack()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .background(neon, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                        Text("Save", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(
                text = "Tip: Start with a reachable goal (e.g., 2000 ml) and adjust based on your routine.",
                color = cs.onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}
    }