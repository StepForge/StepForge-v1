package com.example.stepforge.ui.water

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Cloud
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R

enum class WaterPremiumFeature(
    val id: String,
    val titleRes: Int,
    val subtitleRes: Int,
    val premiumIconRes: Int,
    val icon: ImageVector
) {
    SMART_SCHEDULE(
        id = "smart_schedule",
        titleRes = R.string.wr_premium_smart_title,
        subtitleRes = R.string.wr_premium_smart_subtitle,
        premiumIconRes = R.drawable.wr_premium_smart_calendar,
        icon = Icons.Outlined.NotificationsActive
    ),
    ADAPTIVE(
        id = "adaptive_reminders",
        titleRes = R.string.wr_premium_adaptive_title,
        subtitleRes = R.string.wr_premium_adaptive_subtitle,
        premiumIconRes = R.drawable.wr_premium_adaptive_weather,
        icon = Icons.Outlined.Cloud
    ),
    CUSTOM_SOUNDS(
        id = "custom_sounds",
        titleRes = R.string.wr_premium_sounds_title,
        subtitleRes = R.string.wr_premium_sounds_subtitle,
        premiumIconRes = R.drawable.wr_premium_custom_bell,
        icon = Icons.Outlined.NotificationsActive
    ),
    ANALYTICS(
        id = "analytics",
        titleRes = R.string.wr_premium_analytics_title,
        subtitleRes = R.string.wr_premium_analytics_subtitle,
        premiumIconRes = R.drawable.wr_premium_analytics_bars,
        icon = Icons.Outlined.BarChart
    ),
    QUIET_HOURS(
        id = "quiet_hours",
        titleRes = R.string.wr_premium_quiet_title,
        subtitleRes = R.string.wr_premium_quiet_subtitle,
        premiumIconRes = R.drawable.wr_premium_quiet_moon,
        icon = Icons.Outlined.Bedtime
    )
}

@Composable
fun WaterPremiumSection(
    premiumEnabled: Boolean,
    features: Map<String, Boolean>,
    onToggleFeature: (String, Boolean) -> Unit,
    onUnlockClick: () -> Unit,
    cardBg: Color,
    subtleCard: Color,
    border: Color,
    accent: Color,
    gold: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        PremiumHeader(gold = gold)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            WaterPremiumFeature.values().forEach { feature ->
                PremiumFeatureTile(
                    feature = feature,
                    checked = features[feature.id] == true,
                    locked = !premiumEnabled,
                    subtleCard = subtleCard,
                    border = if (premiumEnabled) border else gold.copy(alpha = 0.24f),
                    accent = accent,
                    gold = gold,
                    onClick = {
                        if (premiumEnabled) {
                            onToggleFeature(feature.id, !(features[feature.id] == true))
                        } else {
                            onUnlockClick()
                        }
                    }
                )
            }
        }

        if (!premiumEnabled) {
            PremiumGateBanner(
                cardBg = cardBg,
                gold = gold,
                accent = accent,
                onUnlockClick = onUnlockClick
            )
        } else {
            GlassPanel(cardBg = cardBg, border = border, modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = accent, modifier = Modifier.size(30.dp))
                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                        Text(
                            text = stringResource(R.string.wr_premium_unlocked_title),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.wr_premium_unlocked_subtitle),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = stringResource(R.string.wr_unlocked),
                        color = accent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 9.dp, vertical = 5.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumHeader(gold: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(gold.copy(alpha = 0.24f))
        )
        Row(
            modifier = Modifier.padding(horizontal = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text("♕", color = gold, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                text = stringResource(R.string.wr_premium_features),
                color = gold,
                fontSize = 13.2.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                maxLines = 1
            )
        }
        Spacer(
            Modifier
                .weight(1f)
                .height(1.dp)
                .background(gold.copy(alpha = 0.24f))
        )
    }
}

@Composable
private fun PremiumFeatureTile(
    feature: WaterPremiumFeature,
    checked: Boolean,
    locked: Boolean,
    subtleCard: Color,
    border: Color,
    accent: Color,
    gold: Color,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .width(126.dp)
            .height(176.dp)
            .background(subtleCard, RoundedCornerShape(22.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (locked) gold.copy(alpha = 0.06f) else accent.copy(alpha = 0.04f),
                        Color.Transparent
                    )
                ),
                RoundedCornerShape(22.dp)
            )
            .border(BorderStroke(1.dp, border), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 9.dp, vertical = 10.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .background((if (locked) gold else accent).copy(alpha = 0.09f), RoundedCornerShape(999.dp))
                    .border(1.dp, (if (locked) gold else accent).copy(alpha = 0.18f), RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (locked) {
                    Image(
                        painter = painterResource(feature.premiumIconRes),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.size(34.dp)
                    )
                } else {
                    Icon(
                        imageVector = feature.icon,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(23.dp)
                    )
                }
            }

            Spacer(Modifier.height(7.dp))

            Text(
                text = stringResource(feature.titleRes),
                color = cs.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.8.sp,
                lineHeight = 12.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.height(5.dp))

            Text(
                text = stringResource(feature.subtitleRes),
                color = cs.onSurfaceVariant,
                fontSize = 8.5.sp,
                lineHeight = 10.5.sp,
                textAlign = TextAlign.Center,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(Modifier.weight(1f))

            if (locked) {
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .border(1.dp, gold.copy(alpha = 0.30f), RoundedCornerShape(999.dp))
                        .background(gold.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 9.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.wr_premium),
                        color = gold,
                        fontSize = 8.sp,
                        lineHeight = 9.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
            } else {
                Switch(
                    checked = checked,
                    onCheckedChange = { onClick() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.75f)
                    )
                )
            }
        }

        if (locked) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(Color.Black.copy(alpha = 0.26f), RoundedCornerShape(7.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.42f), RoundedCornerShape(7.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.86f),
                    modifier = Modifier.size(12.dp)
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(8.dp)
                    .background(if (checked) accent else cs.outline.copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            )
        }
    }
}

@Composable
private fun PremiumGateBanner(
    cardBg: Color,
    gold: Color,
    accent: Color,
    onUnlockClick: () -> Unit
) {
    GlassPanel(cardBg = cardBg, border = gold.copy(alpha = 0.30f), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            gold.copy(alpha = 0.13f),
                            accent.copy(alpha = 0.055f),
                            Color.Transparent
                        )
                    )
                )
                .padding(13.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.wr_premium_crown),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(58.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.wr_unlock_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.6.sp,
                        lineHeight = 14.2.sp,
                        maxLines = 3
                    )
                    Text(
                        text = stringResource(R.string.wr_unlock_subtitle),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 8.8.sp,
                        lineHeight = 10.5.sp,
                        maxLines = 3
                    )
                }

                Button(
                    onClick = onUnlockClick,
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = Color.Black),
                    modifier = Modifier.widthIn(min = 102.dp).height(50.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(15.dp))
                        Text(stringResource(R.string.wr_upgrade), fontWeight = FontWeight.ExtraBold, fontSize = 11.sp)
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PremiumBenefit(text = stringResource(R.string.wr_benefit_smart), accent = accent)
                PremiumBenefit(text = stringResource(R.string.wr_benefit_insights), accent = accent)
                PremiumBenefit(text = stringResource(R.string.wr_benefit_results), accent = accent)
            }
        }
    }
}

@Composable
private fun PremiumBenefit(
    text: String,
    accent: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(11.dp)
        )
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 7.8.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
