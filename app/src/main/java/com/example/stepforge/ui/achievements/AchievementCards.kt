package com.example.stepforge.ui.achievements

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepforge.R
import kotlinx.coroutines.delay

@Composable
internal fun AchievementHeaderCard(state: AchievementsUiState) {
    val featured = state.bestAchievement ?: state.nextTarget
    AchievementCardShell(highlight = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (featured != null) {
                Image(
                    painter = painterResource(featured.definition.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(112.dp)
                )
            } else {
                AchievementProgressRing(
                    progress = state.completion,
                    color = Color(0xFF00F5FF),
                    modifier = Modifier.size(112.dp)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.ach_milestone_journey),
                    color = Color(0xFF00F5FF),
                    fontSize = 12.sp,
                    letterSpacing = 1.3.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    text = featured?.let { stringResource(it.definition.titleRes) } ?: stringResource(R.string.ach_forge_master),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 23.sp,
                    lineHeight = 25.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = featured?.let { stringResource(it.definition.descriptionRes) } ?: stringResource(R.string.ach_every_step_forges),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                ProgressLine(
                    progress = featured?.progress ?: state.completion,
                    color = featured?.definition?.rarity?.accentColor() ?: Color(0xFF00F5FF)
                )
            }
        }
    }
}

@Composable
internal fun MilestoneJourneyCard(state: AchievementsUiState) {
    val allMilestones = remember(state.achievements) {
        state.achievements
            .filter { it.definition.isLinearStepMilestone() }
            .sortedWith(
                compareBy<AchievementItemUi> { it.definition.target }
                    .thenBy { it.definition.id }
            )
    }
    val activeGroupIndex = remember(allMilestones) {
        if (allMilestones.isEmpty()) {
            0
        } else {
            val firstLockedIndex = allMilestones.indexOfFirst { !it.unlocked }
            if (firstLockedIndex == -1) {
                ((allMilestones.size - 1) / MILESTONE_GROUP_SIZE).coerceAtLeast(0)
            } else {
                firstLockedIndex / MILESTONE_GROUP_SIZE
            }
        }
    }
    val groupStart = activeGroupIndex * MILESTONE_GROUP_SIZE
    val milestones = remember(allMilestones, activeGroupIndex) {
        allMilestones.drop(groupStart).take(MILESTONE_GROUP_SIZE)
    }
    val slots = remember(milestones) {
        milestones.map<AchievementItemUi, AchievementItemUi?> { it } +
                List((MILESTONE_GROUP_SIZE - milestones.size).coerceAtLeast(0)) { null }
    }
    val previousGroupCompleted = groupStart > 0 && allMilestones.take(groupStart).all { it.unlocked }
    val allMilestonesCompleted = allMilestones.isNotEmpty() && allMilestones.all { it.unlocked }
    var showCompletionCloud by remember(activeGroupIndex, allMilestonesCompleted) {
        mutableStateOf(previousGroupCompleted || allMilestonesCompleted)
    }

    LaunchedEffect(activeGroupIndex, allMilestonesCompleted) {
        if (previousGroupCompleted || allMilestonesCompleted) {
            showCompletionCloud = true
            delay(2600)
            showCompletionCloud = false
        }
    }

    val lineProgress = remember(milestones) {
        if (milestones.isEmpty()) {
            0f
        } else {
            val completed = milestones.count { it.unlocked }
            val nextProgress = milestones.firstOrNull { !it.unlocked }?.progress ?: 0f
            ((completed + nextProgress.coerceIn(0f, 1f)) / milestones.size.toFloat()).coerceIn(0f, 1f)
        }
    }

    AchievementCardShell(highlight = true) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.ach_milestone_journey),
                    color = Color(0xFF00F5FF),
                    fontSize = 12.sp,
                    lineHeight = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                AnimatedVisibility(
                    visible = showCompletionCloud,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    MilestoneCompletionCloud()
                }
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val itemWidth = (maxWidth / MILESTONE_GROUP_SIZE).coerceAtMost(72.dp)
                val iconSize = itemWidth.coerceIn(50.dp, 60.dp)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        slots.forEach { item ->
                            Box(
                                modifier = Modifier.width(itemWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item != null) {
                                    Image(
                                        painter = painterResource(item.definition.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(iconSize)
                                            .alpha(if (item.unlocked || item.progress > 0f) 1f else 0.44f)
                                    )
                                } else {
                                    Spacer(Modifier.size(iconSize))
                                }
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        MilestoneConnectionLine(
                            progress = lineProgress,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(28.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            slots.forEach { item ->
                                Box(
                                    modifier = Modifier.width(itemWidth),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (item != null) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(if (item.unlocked) Color(0xFF20F2C4) else Color(0xFF0D111A))
                                                .border(1.dp, Color(0xFF20F2C4).copy(alpha = 0.78f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = if (item.unlocked) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                                                contentDescription = null,
                                                tint = if (item.unlocked) Color.Black else Color(0xFF20F2C4),
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    } else {
                                        Spacer(Modifier.size(22.dp))
                                    }
                                }
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        slots.forEach { item ->
                            if (item != null) {
                                Text(
                                    text = stringResource(item.definition.titleRes),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontSize = 8.6.sp,
                                    lineHeight = 9.4.sp,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.width(itemWidth)
                                )
                            } else {
                                Spacer(Modifier.width(itemWidth))
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        slots.forEach { item ->
                            if (item != null) {
                                MilestoneRequirementChip(
                                    item = item,
                                    modifier = Modifier.width(itemWidth)
                                )
                            } else {
                                Spacer(Modifier.width(itemWidth))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MilestoneCompletionCloud() {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(Color(0xFF00F5FF).copy(alpha = 0.13f))
            .border(1.dp, Color(0xFF20F2C4).copy(alpha = 0.35f), RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        listOf(Color(0xFF20F2C4), Color(0xFFFFB02E), Color(0xFF9B5CFF)).forEach { color ->
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Text(
            text = stringResource(R.string.ach_milestone_group_completed),
            color = Color(0xFF20F2C4),
            fontSize = 7.4.sp,
            lineHeight = 8.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MilestoneRequirementChip(
    item: AchievementItemUi,
    modifier: Modifier = Modifier
) {
    val accent = if (item.unlocked) Color(0xFF20F2C4) else Color(0xFF20F2C4).copy(alpha = 0.82f)
    Row(
        modifier = modifier
            .height(18.dp)
            .padding(horizontal = 1.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (item.unlocked) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(8.dp)
        )
        Spacer(Modifier.width(3.dp))
        Text(
            text = item.definition.requirementText(),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 6.8.sp,
            lineHeight = 7.6.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

private const val MILESTONE_GROUP_SIZE = 5

private fun AchievementDefinition.isLinearStepMilestone(): Boolean {
    if (category != AchievementCategory.STEPS) return false
    return id !in setOf("power_day", "monster_walk")
}

@Composable
internal fun AchievementStatsRow(state: AchievementsUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        AchievementStatCard(
            title = stringResource(R.string.ach_current_streak),
            iconRes = R.drawable.ach_icon_current_streak,
            value = state.currentStreak.toString(),
            valueSuffix = stringResource(R.string.ach_days),
            subtitle = stringResource(R.string.ach_best_streak_format, state.longestStreak),
            color = Color(0xFFFF7A2E),
            progress = null,
            modifier = Modifier.weight(1f)
        )
        AchievementStatCard(
            title = stringResource(R.string.ach_achievements),
            iconRes = R.drawable.ach_icon_achievements,
            value = "${state.unlockedCount}/${state.totalCount}",
            valueSuffix = null,
            subtitle = stringResource(R.string.ach_complete_percent, (state.completion * 100).toInt()),
            color = Color(0xFF00F5FF),
            progress = state.completion,
            modifier = Modifier.weight(1f)
        )
        AchievementStatCard(
            title = stringResource(R.string.ach_total_points),
            iconRes = R.drawable.ach_icon_total_points,
            value = state.totalPoints.formatFullAchievementNumber(),
            valueSuffix = null,
            subtitle = stringResource(R.string.ach_top_percent_of_users, state.localRankPercent()),
            color = Color(0xFFFFB02E),
            progress = state.pointsProgress(),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AchievementStatCard(
    title: String,
    iconRes: Int,
    value: String,
    valueSuffix: String?,
    subtitle: String,
    color: Color,
    progress: Float?,
    modifier: Modifier
) {
    val shape = RoundedCornerShape(9.dp)
    val darkTheme = isSystemInDarkTheme()
    val cs = MaterialTheme.colorScheme
    val cardBg = if (darkTheme) {
        Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = 0.16f),
                Color(0xFF071018).copy(alpha = 0.96f),
                color.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(
                color.copy(alpha = 0.12f),
                cs.surface.copy(alpha = 0.98f),
                color.copy(alpha = 0.06f)
            )
        )
    }
    Box(
        modifier = modifier
            .height(68.dp)
            .clip(shape)
            .background(cardBg)
            .border(1.dp, color.copy(alpha = if (darkTheme) 0.32f else 0.24f), shape)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 7.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = title,
                color = color,
                fontSize = 5.8.sp,
                lineHeight = 6.5.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Image(
                    painter = painterResource(iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(28.dp)
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = value,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            lineHeight = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (valueSuffix != null) {
                            Text(
                                text = valueSuffix,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 5.8.sp,
                                lineHeight = 7.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 4.7.sp,
                        lineHeight = 5.4.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (progress != null) {
                StatProgressLine(progress = progress, color = color, modifier = Modifier.fillMaxWidth())
            } else {
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}


@Composable
private fun StatProgressLine(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Box(
        modifier = modifier
            .height(4.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.09f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                .height(4.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(color)
        )
    }
}

@Composable
internal fun AchievementHorizontalShowcase(
    title: String,
    items: List<AchievementItemUi>,
    emptyText: String,
    compact: Boolean = false,
    onViewAll: (() -> Unit)? = null
) {
    AchievementCardShell {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = title,
                    color = Color(0xFF00F5FF),
                    fontSize = if (compact) 13.sp else 14.sp,
                    lineHeight = if (compact) 14.sp else 15.sp,
                    fontWeight = if (compact) FontWeight.ExtraBold else FontWeight.Medium,
                    letterSpacing = if (compact) 1.sp else 0.65.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (onViewAll != null) {
                    Text(
                        text = stringResource(R.string.ach_view_all),
                        color = Color(0xFF00F5FF),
                        fontSize = 10.5.sp,
                        lineHeight = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .clickable { onViewAll() }
                            .padding(horizontal = 7.dp, vertical = 5.dp)
                    )
                }
            }
            if (items.isEmpty()) {
                Text(emptyText, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            } else {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    items.take(5).forEach { item ->
                        MiniAchievementColumn(item = item, modifier = Modifier.weight(1f), compact = compact)
                    }
                    repeat((5 - items.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                }
            }
        }
    }
}

@Composable
private fun MiniAchievementColumn(
    item: AchievementItemUi,
    modifier: Modifier,
    compact: Boolean
) {
    Column(
        modifier = modifier
            .height(if (compact) 104.dp else 112.dp)
            .padding(horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Image(
            painter = painterResource(item.definition.iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(if (compact) 56.dp else 58.dp)
                .alpha(if (item.unlocked || item.progress > 0f) 1f else 0.38f)
        )
        Text(
            text = stringResource(item.definition.titleRes),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = if (compact) 8.1.sp else 7.0.sp,
            lineHeight = if (compact) 8.8.sp else 7.7.sp,
            fontWeight = FontWeight.Bold,
            maxLines = if (compact) 2 else 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        if (!compact) {
            Text(
                text = stringResource(item.definition.descriptionRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 5.2.sp,
                lineHeight = 5.9.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center
            )
        }
        Spacer(Modifier.weight(1f))
        if (!item.unlocked) {
            ProgressLine(progress = item.progress, color = item.definition.rarity.accentColor(), modifier = Modifier.fillMaxWidth(0.80f))
        } else {
            Spacer(Modifier.height(5.dp))
        }
    }
}

@Composable
internal fun AchievementGallery(
    items: List<AchievementItemUi>,
    onItemClick: (AchievementItemUi) -> Unit = {}
) {
    AchievementCardShell {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val columnCount = when {
                    maxWidth >= 700.dp -> 6
                    maxWidth >= 500.dp -> 5
                    maxWidth >= 350.dp -> 4
                    else -> 3
                }
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items.chunked(columnCount).forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { item ->
                                AchievementGridTile(item = item, modifier = Modifier.weight(1f), onClick = { onItemClick(item) })
                            }
                            repeat((columnCount - rowItems.size).coerceAtLeast(0)) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementGridTile(
    item: AchievementItemUi,
    modifier: Modifier,
    onClick: () -> Unit = {}
) {
    val accent = item.definition.rarity.accentColor()
    val darkTheme = isSystemInDarkTheme()
    val dimAccent = MaterialTheme.colorScheme.onSurfaceVariant
    val cardAccent = if (item.unlocked) accent else dimAccent
    val contentAlpha = if (item.unlocked || item.progress > 0f) 1f else 0.26f
    Column(
        modifier = modifier
            .height(116.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardAccent.copy(alpha = if (item.unlocked) 0.10f else if (darkTheme) 0.045f else 0.075f))
            .border(1.dp, cardAccent.copy(alpha = if (item.unlocked) 0.23f else 0.12f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(contentAlignment = Alignment.TopEnd) {
            Image(
                painter = painterResource(item.definition.iconRes),
                contentDescription = null,
                modifier = Modifier
                    .size(58.dp)
                    .alpha(contentAlpha)
            )
            Icon(
                imageVector = if (item.unlocked) Icons.Outlined.CheckCircle else Icons.Outlined.Lock,
                contentDescription = null,
                tint = if (item.unlocked) Color(0xFF4AFF8A) else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.82f),
                modifier = Modifier.size(15.dp)
            )
        }
        Text(
            text = stringResource(item.definition.titleRes),
            color = if (item.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
            fontSize = 9.5.sp,
            lineHeight = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        ProgressLine(
            progress = item.progress,
            color = if (item.unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            modifier = Modifier.fillMaxWidth(0.86f)
        )
    }
}

@Composable
internal fun CategoryChip(
    label: String,
    selected: Boolean,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) color.copy(alpha = 0.18f) else MaterialTheme.colorScheme.surface.copy(alpha = 0.58f))
            .border(1.dp, if (selected) color.copy(alpha = 0.72f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 13.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1
        )
    }
}

@Composable
internal fun ProgressLine(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Box(
        modifier = modifier
            .height(5.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(Color.White.copy(alpha = 0.10f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0.02f, 1f))
                .height(5.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(Brush.horizontalGradient(listOf(color, Color(0xFF00F5FF))))
        )
    }
}

@Composable
private fun AchievementDefinition.requirementText(): String {
    val value = target.formatAchievementRequirementNumber()
    val suffix = when (category) {
        AchievementCategory.STEPS -> R.string.ach_req_steps
        AchievementCategory.DISTANCE -> R.string.ach_req_km
        AchievementCategory.CALORIES -> R.string.ach_req_kcal
        AchievementCategory.STREAKS -> R.string.ach_req_days
        AchievementCategory.WORKOUTS -> R.string.ach_req_workouts
        AchievementCategory.TIME -> R.string.ach_req_minutes
        AchievementCategory.WEATHER -> R.string.ach_req_times
        AchievementCategory.SPECIAL -> R.string.ach_req_times
        AchievementCategory.ALL -> R.string.ach_req_times
    }
    return stringResource(suffix, value)
}

private fun Float.formatAchievementRequirementNumber(): String {
    val safe = coerceAtLeast(0f)
    return when {
        safe >= 1_000_000f -> {
            val value = safe / 1_000_000f
            if (value % 1f < 0.05f) "${value.toInt()}M" else String.format(java.util.Locale.getDefault(), "%.1fM", value)
        }
        safe >= 100_000f -> "${(safe / 1_000f).toInt()}K"
        safe >= 10_000f -> {
            val value = safe / 1_000f
            if (value % 1f < 0.05f) "${value.toInt()}K" else String.format(java.util.Locale.getDefault(), "%.1fK", value)
        }
        safe >= 1_000f -> java.text.NumberFormat.getIntegerInstance().format(safe.toInt())
        safe % 1f < 0.05f -> safe.toInt().toString()
        else -> String.format(java.util.Locale.getDefault(), "%.1f", safe)
    }
}

internal fun Int.formatFullAchievementNumber(): String = java.text.NumberFormat.getIntegerInstance().format(this)

private fun AchievementsUiState.maxPossiblePoints(): Int = achievementDefinitions.sumOf { it.rarity.points }

private fun AchievementsUiState.pointsProgress(): Float {
    val eliteTarget = (maxPossiblePoints() * 0.45f).coerceAtLeast(1f)
    return (totalPoints / eliteTarget).coerceIn(0f, 1f)
}

private fun AchievementsUiState.localRankPercent(): Int {
    val score = (pointsProgress() * 0.76f + completion.coerceIn(0f, 1f) * 0.24f).coerceIn(0f, 0.99f)
    return kotlin.math.max(1, kotlin.math.round((1f - score) * 100f).toInt())
}

internal fun Int.formatCompactAchievementNumber(): String = when {
    this >= 1_000_000 -> "${this / 1_000_000}M"
    this >= 10_000 -> "${this / 1_000}K"
    else -> java.text.NumberFormat.getIntegerInstance().format(this)
}

internal fun Float.formatAchievementProgressNumber(): String = when {
    this >= 1_000_000f -> String.format(java.util.Locale.getDefault(), "%.1fM", this / 1_000_000f)
    this >= 1_000f -> String.format(java.util.Locale.getDefault(), "%.1fK", this / 1_000f)
    this % 1f < 0.05f -> toInt().toString()
    else -> String.format(java.util.Locale.getDefault(), "%.1f", this)
}
