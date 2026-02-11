package com.example.stepforge.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.WaterIntakeEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomWaterIntakeScreen(onBack: () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val neon = Brush.horizontalGradient(listOf(Color(0xFF00F5FF), Color(0xFF00FFA3)))

    val db = remember { AppDatabase.getDatabase(ctx) }
    val eventDao = remember { db.waterIntakeEventDao() }
    val waterDao = remember { db.dailyWaterDao() }

    // we update totals by reading stored today from DataStore in WaterReminderScreen,
    // so here we only add an event; WaterReminderScreen will recalc on resume.
    // But to make it immediate, we will also update DailyWater row roughly (best-effort).
    val dateStr = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) }

    var amount by remember { mutableStateOf("") }
    var selectedPreset by remember { mutableIntStateOf(0) }

    val presets = listOf(150, 200, 300, 400, 750)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Intake", fontWeight = FontWeight.SemiBold) },
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = cs.surface),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = cs.primary)
                        Text("Enter amount (ml)", fontWeight = FontWeight.SemiBold, color = cs.onSurface)
                    }

                    OutlinedTextField(
                        value = amount,
                        onValueChange = { v -> amount = v.filter(Char::isDigit).take(4) },
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = { Text("e.g. 300") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            cursorColor = Color(0xFF00F5FF)
                        )
                    )

                    Text(
                        "Quick presets",
                        color = cs.onSurface.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        presets.forEach { p ->
                            val sel = selectedPreset == p
                            val scale by animateFloatAsState(
                                targetValue = if (sel) 1.03f else 1f,
                                animationSpec = tween(180, easing = FastOutSlowInEasing),
                                label = "presetScale"
                            )
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(42.dp)
                                    .background(
                                        if (sel) cs.surfaceVariant else cs.surface,
                                        RoundedCornerShape(14.dp)
                                    )
                                    .clickable {
                                        selectedPreset = p
                                        amount = p.toString()
                                    }
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$p",
                                    color = if (sel) cs.primary else cs.onSurface,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            val v = amount.toIntOrNull() ?: 0
                            if (v <= 0) return@Button

                            scope.launch(Dispatchers.IO) {
                                eventDao.insert(
                                    WaterIntakeEvent(
                                        date = dateStr,
                                        timeMillis = System.currentTimeMillis(),
                                        amountMl = v
                                    )
                                )
                                // best-effort: bump daily_water
                                val existing = waterDao.getWaterForDate(dateStr)?.waterMl ?: 0
                                waterDao.insertDailyWater(
                                    com.example.stepforge.data.DailyWater(date = dateStr, waterMl = existing + v)
                                )
                            }
                            onBack()
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
                            Text("Add", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            Spacer(Modifier.height(6.dp))
            Text(
                text = "Tip: Use smaller entries to reflect real sips. Your timeline will show each entry.",
                color = cs.onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}