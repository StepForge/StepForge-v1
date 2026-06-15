package com.example.stepforge.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.DirectionsWalk
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random
import com.example.stepforge.R

private enum class OnbPageKind { SENSOR, BACKGROUND, PRIVACY, START }

private data class FeatureChipData(
    val icon: ImageVector,
    val text: String
)

private data class OnboardingPage(
    val kind: OnbPageKind,
    val icon: ImageVector,
    val title: String,
    val body: String,
    val confidence: String,
    val chips: List<FeatureChipData>
)

@Composable
fun OnboardingScreen(
    onSkip: () -> Unit,
    onFinishAndRequestPermissions: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val neonA = if (isDark) Color(0xFF00FFA3) else Color(0xFF4FD1C5)
    val neonB = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)

    val bgBase = cs.background
    val cardBg = cs.surface
    val textMain = cs.onSurface
    val textSub = cs.onSurface.copy(alpha = if (isDark) 0.78f else 0.74f)

    val pages = listOf(
            OnboardingPage(
                kind = OnbPageKind.SENSOR,
                icon = Icons.Outlined.DirectionsWalk,
                title = stringResource(R.string.hc_onboarding_auto_steps),
                body = stringResource(R.string.hc_onboarding_auto_steps_body),
                confidence = stringResource(R.string.hc_onboarding_auto_steps_note),
                chips = listOf(
                    FeatureChipData(Icons.Outlined.Speed, stringResource(R.string.hc_live_steps)),
                    FeatureChipData(Icons.Outlined.Tune, stringResource(R.string.hc_daily_goal)),
                    FeatureChipData(Icons.Outlined.CloudSync, stringResource(R.string.hc_widgets))
                )
            ),
            OnboardingPage(
                kind = OnbPageKind.BACKGROUND,
                icon = Icons.Outlined.SettingsSuggest,
                title = stringResource(R.string.hc_onboarding_background),
                body = stringResource(R.string.hc_onboarding_background_body),
                confidence = stringResource(R.string.hc_onboarding_background_note),
                chips = listOf(
                    FeatureChipData(Icons.Outlined.NotificationsActive, stringResource(R.string.hc_reminders)),
                    FeatureChipData(Icons.Outlined.Speed, stringResource(R.string.hc_consistent_totals)),
                    FeatureChipData(Icons.Outlined.Tune, stringResource(R.string.hc_control_settings))
                )
            ),
            OnboardingPage(
                kind = OnbPageKind.PRIVACY,
                icon = Icons.Outlined.Security,
                title = stringResource(R.string.hc_onboarding_privacy),
                body = stringResource(R.string.hc_onboarding_privacy_body),
                confidence = stringResource(R.string.hc_onboarding_privacy_note),
                chips = listOf(
                    FeatureChipData(Icons.Outlined.Lock, stringResource(R.string.hc_app_lock_optional)),
                    FeatureChipData(Icons.Outlined.Shield, stringResource(R.string.hc_local_first)),
                    FeatureChipData(Icons.Outlined.CloudSync, stringResource(R.string.hc_optional_sync))
                )
            ),
            OnboardingPage(
                kind = OnbPageKind.START,
                icon = Icons.Outlined.NotificationsActive,
                title = stringResource(R.string.hc_onboarding_ready),
                body = stringResource(R.string.hc_onboarding_ready_body),
                confidence = stringResource(R.string.hc_onboarding_ready_note),
                chips = listOf(
                    FeatureChipData(Icons.Outlined.DirectionsWalk, stringResource(R.string.hc_activity_recognition)),
                    FeatureChipData(Icons.Outlined.NotificationsActive, stringResource(R.string.hc_notifications_optional)),
                    FeatureChipData(Icons.Outlined.Security, stringResource(R.string.hc_privacy_controls))
                )
            )
        )

    var index by remember { mutableIntStateOf(0) }
    val page = pages[index]
    val isLast = index == pages.lastIndex

    val progress by animateFloatAsState(
        targetValue = (index + 1) / pages.size.toFloat(),
        animationSpec = tween(durationMillis = 520, easing = FastOutSlowInEasing),
        label = "onboardingProgress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBase)
    ) {
        OnboardingBackground(
            isDark = isDark,
            base = bgBase,
            neonA = neonA,
            neonB = neonB,
            kind = page.kind
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Skip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onSkip) {
                    Text(
                        text = stringResource(R.string.hc_skip),
                        color = textMain.copy(alpha = 0.85f),
                        fontSize = 13.sp
                    )
                }
            }

            // Middle stack
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                HeroCard(
                    isDark = isDark,
                    cardBg = cardBg,
                    textMain = textMain,
                    textSub = textSub,
                    accent = neonB,
                    icon = page.icon,
                    title = page.title,
                    body = page.body,
                    chips = page.chips
                )

                Text(
                    text = page.confidence,
                    color = textSub,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp)
                )
            }

            // Progress area
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DotIndicator(
                    count = pages.size,
                    index = index,
                    active = neonB,
                    inactive = if (isDark) Color(0xFF2A2F3A) else Color(0xFFD7DEE8)
                )

                ThinProgressBar(
                    progress = progress,
                    isDark = isDark,
                    neonA = neonA,
                    neonB = neonB
                )

                Text(
                    text = if (isLast)
                        stringResource(R.string.hc_permissions_on_start)
                    else
                        stringResource(R.string.hc_change_settings_anytime),
                    color = textSub,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Buttons
            Row(modifier = Modifier.fillMaxWidth()) {
                if (index > 0) {
                    OutlinedButton(
                        onClick = { index -= 1 },
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(
                            1.dp,
                            if (isDark) Color(0xFF2A2F3A) else Color(0x1A1A202C)
                        ),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = textMain,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text(stringResource(R.string.hc_back), fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                    Spacer(Modifier.width(12.dp))
                } else {
                    Spacer(Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                }

                val ctaBrush = Brush.horizontalGradient(listOf(neonA, neonB))
                Button(
                    onClick = {
                        if (!isLast) index += 1 else onFinishAndRequestPermissions()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(ctaBrush, RoundedCornerShape(18.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isLast) stringResource(R.string.hc_start) else stringResource(R.string.hc_continue),
                            color = if (isDark) Color.Black else Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

/* =========================
   Background (fullscreen + denser patterns)
   ========================= */

@Composable
private fun OnboardingBackground(
    isDark: Boolean,
    base: Color,
    neonA: Color,
    neonB: Color,
    kind: OnbPageKind
) {
    val infinite = rememberInfiniteTransition(label = "bgInf")
    val pulse by infinite.animateFloat(
        initialValue = 0.018f,
        targetValue = 0.032f,
        animationSpec = infiniteRepeatable(
            animation = tween(3200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().background(base))

        // Soft top radial (neutral)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = Offset(w * 0.52f, h * 0.22f)
            val tint = if (isDark) Color.White else Color.Black
            val r = min(w, h) * 0.95f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        tint.copy(alpha = if (isDark) 0.035f else 0.03f),
                        Color.Transparent
                    ),
                    center = center,
                    radius = r
                ),
                radius = r,
                center = center
            )
        }

        // very subtle diagonal wash
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            neonA.copy(alpha = if (isDark) 0.025f else 0.018f),
                            neonB.copy(alpha = if (isDark) 0.025f else 0.018f)
                        )
                    )
                )
        )

        NoiseLayer(alpha = if (isDark) 0.022f else 0.016f)

        when (kind) {
            OnbPageKind.SENSOR -> DiagonalDashField(alpha = if (isDark) 0.030f else 0.020f, color = neonB)
            OnbPageKind.BACKGROUND -> MultiWaveField(alpha = if (isDark) 0.030f else 0.020f, color = neonA)
            OnbPageKind.PRIVACY -> LockPulse(alpha = pulse, color = neonB)
            OnbPageKind.START -> SoftCheckGlow(alpha = if (isDark) 0.030f else 0.020f, color = neonA)
        }
    }
}

@Composable
private fun NoiseLayer(alpha: Float) {
    val points = remember { List(420) { Offset(Random.nextFloat(), Random.nextFloat()) } }
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        points.forEachIndexed { i, p ->
            val a = alpha * (0.6f + (i % 7) / 10f)
            drawCircle(
                color = Color.White.copy(alpha = a),
                radius = 0.9f,
                center = Offset(p.x * w, p.y * h)
            )
        }
    }
}

/**
 * ✅ Fullscreen “diagonal dash” field: fills ALL screen evenly (no “small area” look)
 */
@Composable
private fun DiagonalDashField(alpha: Float, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val spacing = min(w, h) / 9f
        val dashLen = spacing * 0.42f
        val dy = spacing * 0.72f

        var y = -h * 0.2f
        while (y < h * 1.2f) {
            var x = -w * 0.2f
            while (x < w * 1.2f) {
                drawLine(
                    color = color.copy(alpha = alpha),
                    start = Offset(x, y),
                    end = Offset(x + dashLen, y + dashLen * 0.22f),
                    strokeWidth = 2f,
                    cap = StrokeCap.Round
                )
                x += spacing
            }
            y += dy
        }
    }
}

/**
 * ✅ Multiple waves across screen (not a single line)
 */
@Composable
private fun MultiWaveField(alpha: Float, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        val lines = listOf(0.50f, 0.62f, 0.74f)
        val amp = h * 0.015f
        val cycles = 6.0 * PI

        lines.forEachIndexed { idx, frac ->
            val baseY = h * frac
            val phase = idx * (PI / 3.0)

            val path = Path().apply {
                moveTo(0f, baseY)
                var x = 0f
                while (x <= w) {
                    val y = baseY + amp * sin((x / w) * cycles + phase).toFloat()
                    lineTo(x, y)
                    x += w / 44f
                }
            }

            drawPath(
                path = path,
                color = color.copy(alpha = alpha),
                style = Stroke(width = 3f, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
private fun LockPulse(alpha: Float, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.76f, h * 0.30f)
        val r = min(w, h) * 0.55f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )
    }
}

@Composable
private fun SoftCheckGlow(alpha: Float, color: Color) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val center = Offset(w * 0.28f, h * 0.28f)
        val r = min(w, h) * 0.60f
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                center = center,
                radius = r
            ),
            radius = r,
            center = center
        )
    }
}

/* =========================
   Hero card + smooth icon depth
   ========================= */

@Composable
private fun HeroCard(
    isDark: Boolean,
    cardBg: Color,
    textMain: Color,
    textSub: Color,
    accent: Color,
    icon: ImageVector,
    title: String,
    body: String,
    chips: List<FeatureChipData>
) {
    val border = if (isDark) Color(0xFF1E222B) else Color(0x1A1A202C)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 10.dp else 6.dp,
                shape = RoundedCornerShape(26.dp),
                ambientColor = Color.Black.copy(alpha = if (isDark) 0.30f else 0.08f),
                spotColor = Color.Black.copy(alpha = if (isDark) 0.30f else 0.08f)
            )
            .background(cardBg, RoundedCornerShape(26.dp))
            .border(1.dp, border, RoundedCornerShape(26.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SmoothIconBadge(
            isDark = isDark,
            accent = accent,
            icon = icon
        )

        Text(
            text = title,
            color = textMain,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )

        Text(
            text = body,
            color = textSub,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        FeatureChipsRow(isDark = isDark, accent = accent, chips = chips)
    }
}

/**
 * ✅ Replaces the ugly polygon-like shadow with a smooth radial depth.
 * No glow, just soft depth under the circle.
 */
@Composable
private fun SmoothIconBadge(
    isDark: Boolean,
    accent: Color,
    icon: ImageVector
) {
    val plate = if (isDark) Color(0xFF0F1116) else Color(0xFFF0F3F7)

    Box(
        modifier = Modifier.size(66.dp),
        contentAlignment = Alignment.Center
    ) {
        // Soft depth (radial)
        Canvas(modifier = Modifier.fillMaxSize()) {
            val r = size.minDimension / 2f
            val a = if (isDark) 0.22f else 0.10f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.Black.copy(alpha = a), Color.Transparent),
                    center = center,
                    radius = r * 1.25f
                ),
                radius = r * 1.15f,
                center = center
            )
        }

        // Circle plate (no Compose shadow)
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(plate, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(34.dp)
            )
        }
    }
}

@Composable
private fun FeatureChipsRow(
    isDark: Boolean,
    accent: Color,
    chips: List<FeatureChipData>
) {
    val firstRow = chips.take(2)
    val secondRow = chips.drop(2).take(1)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            firstRow.forEach {
                FeatureChip(
                    isDark = isDark,
                    accent = accent,
                    icon = it.icon,
                    text = it.text,
                    modifier = Modifier.width(150.dp)
                )
            }
        }
        if (secondRow.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                secondRow.forEach {
                    FeatureChip(
                        isDark = isDark,
                        accent = accent,
                        icon = it.icon,
                        text = it.text,
                        modifier = Modifier.width(150.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureChip(
    isDark: Boolean,
    accent: Color,
    icon: ImageVector,
    text: String,
    modifier: Modifier = Modifier
) {
    val bg = if (isDark) Color(0xFF0F1116) else Color(0xFFF0F3F7)
    val border = if (isDark) Color(0xFF262B36) else Color(0x1A1A202C)
    val fg = if (isDark) Color.White.copy(alpha = 0.88f) else Color(0xFF1A202C)

    Row(
        modifier = modifier
            .background(bg, RoundedCornerShape(14.dp))
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(16.dp))
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
    }
}

/* =========================
   Progress UI
   ========================= */

@Composable
private fun ThinProgressBar(
    progress: Float,
    isDark: Boolean,
    neonA: Color,
    neonB: Color
) {
    val track = if (isDark) Color(0xFF202430) else Color(0xFFE2E8F0)
    val fill = Brush.horizontalGradient(
        colors = listOf(
            neonA.copy(alpha = if (isDark) 0.85f else 0.80f),
            neonB.copy(alpha = if (isDark) 0.85f else 0.80f)
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 44.dp)
            .height(6.dp)
            .background(track, RoundedCornerShape(999.dp))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .background(fill, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun DotIndicator(
    count: Int,
    index: Int,
    active: Color,
    inactive: Color
) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { i ->
            val c = if (i == index) active else inactive
            Box(
                modifier = Modifier
                    .size(if (i == index) 10.dp else 8.dp)
                    .background(c, CircleShape)
            )
        }
    }
}
