package com.example.stepforge.settings

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import com.example.stepforge.ui.components.HealthConnectState
import com.example.stepforge.ui.components.HealthSyncManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun IntegrationModal(
    sheet: IntegrationSheet?,
    themeMode: String,
    onDismiss: () -> Unit,
    onConnectHealthConnect: () -> Unit
) {
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    val scrim = if (isDark) Color.Black.copy(alpha = 0.60f) else Color.Black.copy(alpha = 0.35f)
    val cardBg = if (isDark) Color(0xFF0B0B0D) else Color(0xFFF6F7FA)

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val healthManager = remember { HealthSyncManager(ctx) }

    // ✅ Yeni: İzin durumunu tutan state
    val hcStatus by HealthConnectState.status.collectAsState()
    var isConnected by remember { mutableStateOf(false) }

    // ✅ Yeni: Modal açıldığında izinleri kontrol et
    LaunchedEffect(sheet, hcStatus) {
        if (sheet == IntegrationSheet.HealthConnect) {
            isConnected = hcStatus.permissionsGranted
        }
    }


    AnimatedVisibility(
        visible = sheet != null,
        enter = fadeIn() + scaleIn(initialScale = 0.95f),
        exit = fadeOut() + scaleOut(targetScale = 0.95f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(scrim)
                .pointerInput(Unit) { detectTapGestures { } },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .heightIn(max = 620.dp),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = if (isDark) 14.dp else 10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Back",
                                tint = if (isDark) Color.White else Color(0xFF111111)
                            )
                        }
                    }

                    when (sheet) {
                        IntegrationSheet.HealthConnect -> {
                            HealthConnectCard(
                                dark = isDark,
                                isConnected = isConnected, // ✅ Gönderiliyor
                                onConnect = {
                                    onConnectHealthConnect()
                                    // ✅ İzin penceresinden dönünce kontrol et
                                    scope.launch {
                                        delay(1500)
                                        isConnected = healthManager.hasEssentialPermissions()
                                        if (isConnected) {
                                            Log.d("StepForge", "Health Connect synced")
                                        }
                                    }
                                },
                                onLater = onDismiss
                            )
                        }
                        null -> Unit
                    }
                }
            }
        }
    }
}

@Composable
fun HealthConnectCard(
    dark: Boolean,
    isConnected: Boolean, // ✅ Yeni Parametre
    onConnect: () -> Unit,
    onLater: () -> Unit
) {
    // --- colors ---
    val surface = if (dark) Color(0xFF0B0B0D) else Color(0xFFF6F7FA)
    val heroBg = if (dark) Color(0xFF0B0B0D) else Color(0xFFF1F5F9)
    val heroOutline = if (dark) Color.Transparent else Color(0x140F172A)

    val textMain = if (dark) Color.White else Color(0xFF0F172A)
    val textSub = if (dark) Color.White.copy(alpha = 0.72f) else Color(0xFF475569)

    val ringCyan = if (dark) Color(0xFF00F5FF) else Color(0xFF18D5FF)
    val ringGreen = if (dark) Color(0xFF00FFA3) else Color(0xFF2DE6B6)
    val ringGradient = Brush.sweepGradient(listOf(ringCyan, ringGreen, ringCyan))
    val ringBg = if (dark) surface else Color(0xFFFBFCFF)

    val glowAlpha = if (dark) 0.22f else 0.08f
    val glowRadiusMul = if (dark) 1.25f else 1.45f

    // ✅ Bağlıysa butonu gri yap
    val connectBrush = if (isConnected) {
        Brush.horizontalGradient(listOf(Color.Gray, Color.DarkGray))
    } else if (dark) {
        Brush.horizontalGradient(listOf(Color(0xFF00F5FF), Color(0xFF00FFA3)))
    } else {
        Brush.horizontalGradient(listOf(Color(0xFF18D5FF), Color(0xFF3B82F6)))
    }
    val connectTextColor = if (dark || !isConnected) Color.Black else Color.White

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val heroShape = RoundedCornerShape(22.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(heroShape)
                .background(
                    brush = if (dark) {
                        Brush.verticalGradient(listOf(Color(0xFF0B0B0D), Color(0xFF101218)))
                    } else {
                        Brush.verticalGradient(listOf(Color(0xFFF8FAFF), heroBg))
                    }
                )
                .border(1.dp, heroOutline, heroShape)
                .drawBehind {
                    if (!dark) {
                        val w = size.width
                        val h = size.height
                        drawRoundRect(
                            brush = Brush.radialGradient(
                                colors = listOf(ringGreen.copy(alpha = 0.10f), Color.Transparent),
                                center = Offset(w * 0.35f, h * 0.40f),
                                radius = w * 0.9f
                            ),
                            topLeft = Offset.Zero,
                            size = Size(w, h),
                            cornerRadius = CornerRadius(22.dp.toPx(), 22.dp.toPx())
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
                Box(
                    modifier = Modifier.size(160.dp).drawBehind {
                        val r = size.minDimension / 2f
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(ringCyan.copy(alpha = glowAlpha), Color.Transparent),
                                center = center,
                                radius = r * glowRadiusMul
                            ),
                            radius = r * 1.10f,
                            center = center
                        )
                    }
                )
                Box(
                    modifier = Modifier.size(160.dp).border(5.dp, ringGradient, CircleShape).background(ringBg, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.health_connect_logo),
                        contentDescription = "Health Connect Logo",
                        modifier = Modifier.size(84.dp).clip(RoundedCornerShape(18.dp))
                    )
                }
            }
        }

        Spacer(Modifier.height(18.dp))

        // ✅ BURASI: Dinamik Başlık
        Text(
            text = if (isConnected) "Successfully\nConnected" else "Health Connect\nIntegration",
            fontSize = 22.sp,
            color = if (isConnected) ringGreen else textMain,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp
        )

        Spacer(Modifier.height(10.dp))

        // ✅ BURASI: Dinamik Açıklama
        Text(
            text = if (isConnected) "StepForge is now synced with your Health account. Your activity data will be imported automatically."
            else "Connect your Google Health account to import walking data into StepForge. All your records are unified in one place.",
            color = textSub,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 10.dp)
        )

        Spacer(Modifier.height(22.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Button(
                onClick = onConnect,
                enabled = !isConnected, // ✅ Bağlıysa butonu devredışı bırak
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues()
            ) {
                Box(
                    modifier = Modifier.fillMaxSize().background(connectBrush, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isConnected) "Connected" else "Connect", fontWeight = FontWeight.Bold, color = connectTextColor)
                }
            }

            OutlinedButton(
                onClick = onLater,
                modifier = Modifier.weight(1f).height(46.dp),
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, if (dark) Color.Gray.copy(alpha = 0.4f) else Color(0x330F172A)),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = textMain)
            ) {
                Text(if (isConnected) "Close" else "Later", fontWeight = FontWeight.SemiBold)
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}
