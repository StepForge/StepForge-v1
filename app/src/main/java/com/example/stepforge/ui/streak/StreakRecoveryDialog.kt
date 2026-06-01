package com.example.stepforge.ui.streak

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun StreakRecoveryDialog(
    streakDays: Int,
    formattedPrice: String,
    remainingText: String,
    onDismiss: () -> Unit,
    onRestore: () -> Unit
) {

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val glow = rememberInfiniteTransition(label = "glow")

    val scale by glow.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2200,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val alpha by glow.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    val topColor =
        if (isDark) Color(0xFF00FFD5)
        else Color(0xFF00BFA5)

    val bottomColor =
        if (isDark) Color(0xFF00C2FF)
        else Color(0xFF0091EA)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.76f)),
            contentAlignment = Alignment.Center
        ) {

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp)
                .heightIn(max = 720.dp),
            shape = RoundedCornerShape(34.dp),
            colors = CardDefaults.cardColors(
                containerColor =
                    if (isDark)
                        Color(0xFF111315)
                    else
                        Color.White
            )
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 26.dp)
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                /*
                 * FIRE ICON
                 */

                Box(
                    contentAlignment = Alignment.Center
                ) {

                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(scale)
                            .alpha(alpha)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        topColor.copy(alpha = 0.38f),
                                        Color.Transparent
                                    )
                                ),
                                shape = RoundedCornerShape(999.dp)
                            )
                    )

                    Card(
                        shape = RoundedCornerShape(999.dp),
                        colors = CardDefaults.cardColors(
                            containerColor =
                                if (isDark)
                                    Color(0xFF171A1D)
                                else
                                    Color(0xFFF5F7FA)
                        )
                    ) {

                        Box(
                            modifier = Modifier.size(108.dp),
                            contentAlignment = Alignment.Center
                        ) {

                            Icon(
                                imageVector = Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = topColor,
                                modifier = Modifier.size(56.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                /*
                 * TITLE
                 */

                Text(
                    text = "You lost your $streakDays day streak",
                    fontSize = 30.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = cs.onSurface,
                    textAlign = TextAlign.Center,
                    lineHeight = 34.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                /*
                 * DESCRIPTION
                 */

                Text(
                    text =
                        "Restore your streak before the recovery window expires.\n" +
                                "Your milestones, stats and progress will remain intact.",
                    fontSize = 14.sp,
                    color = cs.onSurface.copy(alpha = 0.72f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                /*
                 * PRICE CARD
                 */

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            if (isDark)
                                Color(0xFF171A1D)
                            else
                                Color(0xFFF5F7FA)
                    )
                ) {

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {

                        Text(
                            text = formattedPrice,
                            fontSize = 52.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = topColor
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "One-time recovery",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text =
                                "Recover your current streak instantly and continue progressing without losing your milestones.",
                            fontSize = 13.sp,
                            color = cs.onSurface.copy(alpha = 0.68f),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))

                /*
                 * TIMER
                 */

                Text(
                    text = remainingText,
                    color = topColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                /*
                 * RESTORE BUTTON
                 */

                Button(
                    onClick = onRestore,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(999.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    contentPadding = PaddingValues()
                ) {

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        topColor,
                                        bottomColor
                                    )
                                ),
                                shape = RoundedCornerShape(999.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {

                        Text(
                            text = "Restore streak",
                            fontWeight = FontWeight.ExtraBold,
                            color =
                                if (isDark)
                                    Color.Black
                                else
                                    Color.White,
                            fontSize = 16.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                /*
                 * DISMISS
                 */

                TextButton(
                    onClick = onDismiss
                ) {

                    Text(
                        text = "Dismiss",
                        color = cs.onSurface.copy(alpha = 0.6f),
                        fontSize = 14.sp
                    )
                }
            }
        }
        }
    }
}
