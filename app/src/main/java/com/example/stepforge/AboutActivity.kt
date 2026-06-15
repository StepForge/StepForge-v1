@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stepforge

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.ui.stepforgeTheme
import com.example.stepforge.ui.rememberUseDarkTheme
import kotlin.random.Random
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.stepforge.debug.DebugPanelActivity

class AboutActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                AboutScreen(onBack = { finish() })

            }
        }
    }
}



private data class Star(
    val x: Float, // 0..1
    val y: Float, // 0..1
    val r: Float, // px-ish
    val a: Float  // 0..1
)

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bg = if (isDark) Color(0xFF0B0B0E) else cs.background
    val cardBg = cs.surface

    val textMain = cs.onSurface
    val textSub = cs.onSurface.copy(alpha = 0.78f)

    val neonA = Color(0xFF00FFA3)
    val neonB = Color(0xFF00F5FF)
    val neon = Brush.horizontalGradient(listOf(neonA, neonB))

    val privacyUrl = "https://stepforge.github.io/Privacy-Terms/privacy.html"
    val termsUrl = "https://stepforge.github.io/Privacy-Terms/terms.html"
    val supportEmail = "stepforge0@gmail.com"

    val stars = remember {
        List(55) {
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                r = Random.nextInt(1, 4).toFloat(),
                a = (0.35f + Random.nextFloat() * 0.55f).coerceIn(0.35f, 0.9f)
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hc_about), color = cs.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.hc_back),
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
            // Stabil starfield
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(alpha = if (isDark) 0.22f else 0.10f)
            ) {
                val w = size.width
                val h = size.height
                stars.forEach { s ->
                    drawCircle(
                        color = Color(0xFF00F5FF).copy(alpha = s.a),
                        radius = s.r,
                        center = Offset(s.x * w, s.y * h)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                var logoTapCount by rememberSaveable { mutableStateOf(0) }
                val debugUnlocked = logoTapCount >= 7

                SimpleLogo(
                    neonA = neonA,
                    neonB = neonB,
                    isDark = isDark,
                    onTap = {
                        if (logoTapCount < 7) {
                            logoTapCount += 1
                        }
                    }
                )

                AboutCard(
                    bg = cardBg,
                    neon = neon,
                    textMain = textMain,
                    textSub = textSub,
                    isDark = isDark,
                    privacyUrl = privacyUrl,
                    termsUrl = termsUrl,
                    supportEmail = supportEmail,
                    debugUnlocked = debugUnlocked
                )

                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SimpleLogo(
    neonA: Color,
    neonB: Color,
    isDark: Boolean,
    onTap: () -> Unit
) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(700),
        label = "aboutLogoScale"
    )

    val border = Brush.linearGradient(listOf(neonA, neonB))
    val innerBg = if (isDark) Color(0xFF050608) else Color(0xFFF4F5FA)

    Box(
        modifier = Modifier
            .size(140.dp)
            .graphicsLayer(scaleX = scale, scaleY = scale)
            .clickable { onTap() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .shadow(
                    elevation = if (isDark) 0.dp else 8.dp,
                    shape = RoundedCornerShape(40.dp),
                    clip = false
                )
                .clip(RoundedCornerShape(40.dp))
                .background(innerBg)
                .border(2.dp, border, RoundedCornerShape(40.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_walk),
                contentDescription = stringResource(R.string.hc_stepforge_icon),
                tint = neonB,
                modifier = Modifier.size(90.dp)
            )
        }
    }
}

@Composable
private fun AboutCard(
    bg: Color,
    neon: Brush,
    textMain: Color,
    textSub: Color,
    isDark: Boolean,
    privacyUrl: String,
    termsUrl: String,
    supportEmail: String,
    debugUnlocked: Boolean
) {
    val ctx = LocalContext.current

    // Version from PackageManager (BuildConfig yok)
    val (versionName, versionCode) = remember {
        try {
            val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
            val name = pi.versionName ?: "1.0"
            val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
            name to code
        } catch (_: Exception) {
            "1.0" to 1L
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (isDark) 12.dp else 6.dp,
                shape = RoundedCornerShape(26.dp)
            )
            .background(bg, RoundedCornerShape(26.dp))
            .border(1.dp, neon, RoundedCornerShape(26.dp))
            .padding(horizontal = 20.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.app_name),
            color = textMain,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = stringResource(R.string.hc_about_tagline),
            color = textSub,
            fontSize = 13.5.sp,
            lineHeight = 20.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 560.dp)
        )

        Text(
            text = stringResource(R.string.hc_version_build_format, versionName, versionCode),
            color = textSub.copy(alpha = 0.9f),
            fontSize = 12.sp
        )

        Divider(color = if (isDark) Color(0xFF1E222B) else Color(0xFFE0E5EC))

        SectionTitle(stringResource(R.string.hc_what_you_can_do), textMain)
        Bullet(stringResource(R.string.hc_about_track_steps), textSub)
        Bullet(stringResource(R.string.hc_about_daily_goal), textSub)
        Bullet(stringResource(R.string.hc_about_history), textSub)
        Bullet(stringResource(R.string.hc_about_widgets), textSub, icon = Icons.Outlined.Widgets)
        Bullet(stringResource(R.string.hc_about_reminders), textSub)
        Bullet(stringResource(R.string.hc_about_water), textSub)
        Bullet(stringResource(R.string.hc_about_sleep), textSub)

        Divider(color = if (isDark) Color(0xFF1E222B) else Color(0xFFE0E5EC))

        SectionTitle(stringResource(R.string.hc_privacy_data), textMain)
        Bullet(stringResource(R.string.hc_about_local_data), textSub, icon = Icons.Outlined.PrivacyTip)
        Bullet(stringResource(R.string.hc_about_health_permissions), textSub)
        Bullet(stringResource(R.string.hc_about_app_lock), textSub, icon = Icons.Outlined.Lock)

        Divider(color = if (isDark) Color(0xFF1E222B) else Color(0xFFE0E5EC))

        SectionTitle(stringResource(R.string.hc_integrations), textMain)
        Bullet(stringResource(R.string.hc_about_health_connect), textSub)
        Bullet(stringResource(R.string.hc_about_sync_backup), textSub, icon = Icons.Outlined.Cloud)
        Bullet(stringResource(R.string.hc_about_diagnostics), textSub, icon = Icons.Outlined.Sync)

        Divider(color = if (isDark) Color(0xFF1E222B) else Color(0xFFE0E5EC))

        SectionTitle(stringResource(R.string.hc_support), textMain)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // ✅ Text sığmazsa alt satıra düşmesin, ellipsis olsun
            ActionOutlinedButton(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.hc_contact),
                icon = Icons.Outlined.SupportAgent,
                borderBrush = neon,
                textColor = textMain,
                onClick = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:")
                        putExtra(Intent.EXTRA_EMAIL, arrayOf(supportEmail))
                        putExtra(Intent.EXTRA_SUBJECT, "StepForge – Support")
                    }
                    try {
                        ctx.startActivity(intent)
                    } catch (_: Exception) {
                        Toast
                            .makeText(ctx, ctx.getString(R.string.hc_no_email_app), Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            )

            // ✅ Feedback ekranına git
            ActionFilledButton(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.hc_feedback),
                brush = neon,
                isDark = isDark,
                onClick = {
                    ctx.startActivity(Intent(ctx, FeedbackActivity::class.java))
                }
            )
        }

        Divider(color = if (isDark) Color(0xFF1E222B) else Color(0xFFE0E5EC))

        SectionTitle(stringResource(R.string.hc_legal), textMain)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ActionOutlinedButton(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.hc_privacy),
                icon = Icons.Outlined.Policy,
                borderBrush = neon,
                textColor = textMain,
                onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(privacyUrl))) }
            )

            ActionOutlinedButton(
                modifier = Modifier.weight(1f),
                label = stringResource(R.string.hc_terms),
                icon = Icons.Outlined.Policy,
                borderBrush = neon,
                textColor = textMain,
                onClick = { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(termsUrl))) }
            )
        }

        if (debugUnlocked && BuildConfig.DEBUG) {
            Divider(color = if (isDark) Color(0xFF1E222B) else Color(0xFFE0E5EC))

            SectionTitle(stringResource(R.string.hc_developer_tools), textMain)

            ActionOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                label = stringResource(R.string.hc_open_debug_console),
                icon = Icons.Outlined.Info,
                borderBrush = neon,
                textColor = textMain,
                onClick = {
                    ctx.startActivity(Intent(ctx, DebugPanelActivity::class.java))
                }
            )
        }

        Text(
            text = stringResource(R.string.hc_developed_by),
            color = textSub.copy(alpha = 0.9f),
            fontSize = 12.sp
        )
    }
}

@Composable
private fun ActionOutlinedButton(
    modifier: Modifier = Modifier,
    label: String,
    icon: ImageVector,
    borderBrush: Brush,
    textColor: Color,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, borderBrush),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = textColor,
            containerColor = Color.Transparent
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = textColor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun ActionFilledButton(
    modifier: Modifier = Modifier,
    label: String,
    brush: Brush,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(22.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush, RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                color = if (isDark) Color.Black else Color.White
            )
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        color = color,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun Bullet(
    text: String,
    color: Color,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        if (icon != null) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp, end = 8.dp)
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.06f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF00F5FF),
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .padding(top = 7.dp, end = 10.dp)
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00F5FF))
            )
        }

        Text(
            text = text,
            color = color,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}
