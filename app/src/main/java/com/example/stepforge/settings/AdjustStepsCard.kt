package com.example.stepforge.settings

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun AdjustStepsCard(
    modifier: Modifier = Modifier,
    currentSteps: Int,
    dailyGoal: Int = 10000,
    onApply: (Int) -> Unit,
    onReset: () -> Unit,
    darkTheme: Boolean? = null // ✅ eklendi (opsiyonel)
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()

    var text by remember { mutableStateOf("") }
    var showWarningDialog by remember { mutableStateOf(false) }

    // ✅ Light palet (soft)
    val bg = if (isDark) Color(0xFF090A0D) else Color(0xFFFFFFFF)
    val cardShape = RoundedCornerShape(26.dp)

    // stepforge neon paleti – light’ta daha mat
    val neonA = if (isDark) Color(0xFF00FFC3) else Color(0xFF4FD1C5)
    val neonB = if (isDark) Color(0xFF00E0FF) else Color(0xFF2CB6AE)

    val textMain = if (isDark) Color.White else Color(0xFF1A202C)
    val textSub = if (isDark) Color(0xFFBFC4D0) else Color(0xFF5B6472)

    val innerBg = if (isDark) Color(0xFF050608) else Color(0xFFF0F3F7)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, cardShape)
            .drawNeonOuter(cardShape, neonB.copy(alpha = if (isDark) 1f else 0.7f))
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        // WARNING BANNER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) Color(0xFF2A2010) else Color(0xFFFFF4E6),
                    RoundedCornerShape(12.dp)
                )
                .clickable { showWarningDialog = true }
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = Color(0xFFFFB74D),
                modifier = Modifier.size(20.dp)
            )

            Spacer(Modifier.width(10.dp))

            Text(
                text = "Synchronization notice • Tap to read",
                color = Color(0xFFFFB74D),
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }



        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            contentAlignment = Alignment.Center
        ) {
            val target = (text.toIntOrNull() ?: currentSteps).coerceAtLeast(0)

            NeonRing(
                progress = (target.toFloat() / dailyGoal.toFloat()).coerceIn(0f, 1f),
                startColor = neonA,
                endColor = neonB,
                isDark = isDark
            )

            Column(
                modifier = Modifier.fillMaxWidth(0.7f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .background(innerBg, RoundedCornerShape(16.dp))
                        .borderGlow(neonB),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DirectionsRun,
                        contentDescription = null,
                        tint = neonB,
                        modifier = Modifier.padding(start = 12.dp, end = 8.dp)
                    )
                    OutlinedTextField(
                        value = text,
                        onValueChange = { v -> text = v.filter { it.isDigit() }.take(6) },
                        singleLine = true,
                        placeholder = {
                            Text(
                                if (currentSteps > 0) currentSteps.toString() else "Enter steps",
                                color = textSub
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = MaterialTheme.typography.titleMedium.copy(
                            color = textMain,
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = neonB,
                            focusedTextColor = textMain,
                            unfocusedTextColor = textMain
                        )
                    )
                }

                Spacer(Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .background(innerBg, RoundedCornerShape(50))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(
                                    Brush.horizontalGradient(listOf(neonA, neonB)),
                                    CircleShape
                                )
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Live Ring",
                            color = textMain,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))

        val previewTarget = text.toIntOrNull() ?: currentSteps
        val pct = (previewTarget.toFloat() / dailyGoal * 100f).coerceIn(0f, 999f)
        Text(
            text = "Preview: ${pct.roundToInt()}% of daily goal",
            color = textSub,
            fontSize = 13.sp
        )

        Spacer(Modifier.height(18.dp))

        Button(
            onClick = {
                val final = text.toIntOrNull()
                if (final != null) onApply(final)
            },
            enabled = text.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp),
            shape = RoundedCornerShape(32.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            contentPadding = PaddingValues()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Brush.horizontalGradient(listOf(neonA, neonB)), RoundedCornerShape(32.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "APPLY",
                    color = if (isDark) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        }



        Spacer(Modifier.height(10.dp))

        OutlinedButton(
            onClick = {
                text = ""
                onReset()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(32.dp),
            border = BorderStroke(
                1.dp,
                if (isDark) Color(0xFF3A3D46) else Color(0x1A1A202C)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = innerBg,
                contentColor = textMain
            )
        ) {
            Icon(
                imageVector = Icons.Outlined.Restore,
                contentDescription = null,
                tint = textMain
            )
            Spacer(Modifier.width(8.dp))
            Text("Reset Steps", fontSize = 15.sp)
        }

        if (showWarningDialog) {
            AlertDialog(
                onDismissRequest = { showWarningDialog = false },
                containerColor = if (isDark) Color(0xFF111318) else Color.White,
                titleContentColor = if (isDark) Color.White else Color.Black,
                textContentColor = if (isDark) Color(0xFFBFC4D0) else Color(0xFF444444),
                confirmButton = {
                    Button(
                        onClick = { showWarningDialog = false },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF00F5FF) else MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(
                            "Understood",
                            color = if (isDark) Color.Black else Color.White
                        )
                    }
                },
                title = {
                    Text(
                        "Synchronization Warning",
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Text(
                        "Manually adjusting or resetting steps may cause display inconsistencies if Health Connect is active. The system might override these changes during the next synchronization cycle."
                    )
                }
            )
        }
    }
}

@Composable
fun NeonRing(
    progress: Float,
    startColor: Color,
    endColor: Color,
    isDark: Boolean
) {
    val animated by animateFloatAsState(
        targetValue = progress,
        label = "neonRingProgress"
    )

    val trackColor = if (isDark) Color(0xFF232731) else Color(0xFFD7DEE8)

    // ✅ Light’ta gradienti belirginleştiren ara duraklar
    val cA = startColor
    val cB = endColor

    // Light için ekstra “cyan” ve “green” vurgu (stepforge kimliği)
    val cC = if (isDark) cB else Color(0xFF00D2FF)  // cyan
    val cD = if (isDark) cA else Color(0xFF00E7A8)  // green

    Canvas(Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val minSide = min(w, h)
        val strokeWidth = minSide * 0.10f
        val radius = minSide / 2f - strokeWidth / 2f

        // Track
        drawArc(
            color = trackColor,
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset((w - 2 * radius) / 2f, (h - 2 * radius) / 2f),
            size = Size(2 * radius, 2 * radius),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // Progress arc
        val sweep = 360f * animated
        drawArc(
            brush = Brush.sweepGradient(
                colors = if (isDark) {
                    listOf(cA, cB, cA)
                } else {
                    // ✅ Daha belirgin karışım
                    listOf(cA, cC, cB, cD, cA)
                }
            ),
            startAngle = -90f,
            sweepAngle = sweep,
            useCenter = false,
            topLeft = Offset((w - 2 * radius) / 2f, (h - 2 * radius) / 2f),
            size = Size(2 * radius, 2 * radius),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // ✅ Light’ta “ışık” hissi: üst bölgede çok hafif highlight
        if (!isDark && sweep > 8f) {
            drawArc(
                color = Color.White.copy(alpha = 0.20f),
                startAngle = -95f,
                sweepAngle = minOf(40f, sweep),
                useCenter = false,
                topLeft = Offset((w - 2 * radius) / 2f, (h - 2 * radius) / 2f),
                size = Size(2 * radius, 2 * radius),
                style = Stroke(width = strokeWidth * 0.55f, cap = StrokeCap.Round)
            )
        }
    }
}

fun Modifier.drawNeonOuter(
    shape: Shape,
    color: Color
): Modifier = this.then(
    Modifier.drawBehind {
        val outline: Outline = shape.createOutline(size, layoutDirection, this)
        val stroke = Stroke(1.5.dp.toPx())
        val path = when (outline) {
            is Outline.Rounded -> Path().apply { addRoundRect(outline.roundRect) }
            is Outline.Generic -> outline.path
            else -> Path()
        }

        drawPath(
            path = path,
            brush = Brush.linearGradient(listOf(color, color), start = Offset.Zero, end = Offset(size.width, size.height)),
            style = stroke,
            alpha = if (color.alpha >= 1f) 0.95f else 0.75f
        )

        drawPath(
            path = path,
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = if (color.alpha >= 1f) 0.2f else 0.12f), Color.Transparent),
                center = Offset(size.width / 2f, size.height),
                radius = size.minDimension
            ),
            alpha = 1f
        )
    }
)

fun Modifier.borderGlow(
    color: Color
): Modifier = this.then(
    Modifier.drawBehind {
        val stroke = Stroke(1.5.dp.toPx())
        val rr = RoundRect(
            0f,
            0f,
            size.width,
            size.height,
            16.dp.toPx(),
            16.dp.toPx()
        )
        val path = Path().apply { addRoundRect(rr) }

        drawPath(
            path = path,
            brush = Brush.linearGradient(listOf(color, color), start = Offset.Zero, end = Offset(size.width, size.height)),
            style = stroke,
            alpha = 0.9f
        )
    }
)