package com.example.stepforge.ui.achievements

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Shader
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.example.stepforge.R
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

private enum class AchievementShareTarget {
    FACEBOOK,
    X,
    INSTAGRAM,
    MORE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AchievementsRoute(
    state: AchievementsUiState,
    onBack: () -> Unit
) {
    var selectedCategory by remember { mutableStateOf(AchievementCategory.ALL) }
    var selectedRarity by remember { mutableStateOf<AchievementRarity?>(null) }
    var showRaritySheet by remember { mutableStateOf(false) }
    var showLibrarySheet by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val openDetail: (AchievementItemUi) -> Unit = { item ->
        context.startActivity(
            Intent(context, com.example.stepforge.AchievementDetailActivity::class.java).apply {
                putExtra(com.example.stepforge.AchievementDetailActivity.EXTRA_ACHIEVEMENT_ID, item.definition.id)
            }
        )
    }
    val librarySheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )
    val filteredItems = remember(state.achievements, selectedCategory, selectedRarity) {
        state.achievements.filter { item ->
            val categoryMatches = selectedCategory == AchievementCategory.ALL || item.definition.category == selectedCategory
            val rarityMatches = selectedRarity == null || item.definition.rarity == selectedRarity
            categoryMatches && rarityMatches
        }
    }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
    ) { inner ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(inner)
        ) {
            AchievementDarkBackground()
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item { AchievementTopHeader(state = state, onBack = onBack) }
                item { MilestoneJourneyCard(state = state) }
                item { AchievementStatsRow(state = state) }
                item {
                    AchievementHorizontalShowcase(
                        title = stringResource(R.string.ach_recently_unlocked),
                        items = state.recentUnlocked,
                        emptyText = stringResource(R.string.ach_no_unlocked_yet),
                        onViewAll = { showLibrarySheet = true }
                    )
                }
                item {
                    AchievementHorizontalShowcase(
                        title = stringResource(R.string.ach_next_targets),
                        items = state.nextTargets,
                        emptyText = stringResource(R.string.ach_no_targets_yet),
                        compact = true,
                        onViewAll = { showLibrarySheet = true }
                    )
                }
                item {
                    AchievementCategoryFilter(
                        selected = selectedCategory,
                        selectedRarity = selectedRarity,
                        onSelected = { selectedCategory = it },
                        onRarityClick = { showRaritySheet = true }
                    )
                }
                item {
                    AchievementGallery(
                        items = filteredItems,
                        onItemClick = openDetail
                    )
                }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
    if (showLibrarySheet) {
        ModalBottomSheet(
            onDismissRequest = { },
            sheetState = librarySheetState,
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            dragHandle = null
        ) {
            AchievementLibrarySheetContent(
                state = state,
                onClose = { showLibrarySheet = false },
                onOpenDetail = { item ->
                    showLibrarySheet = false
                    openDetail(item)
                }
            )
        }
    }

    if (showRaritySheet) {
        AchievementRarityFilterSheet(
            selected = selectedRarity,
            onSelected = { rarity ->
                selectedRarity = rarity
                showRaritySheet = false
            },
            onClose = { showRaritySheet = false }
        )
    }

}

@Composable
private fun AchievementTopHeader(
    state: AchievementsUiState,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f), RoundedCornerShape(14.dp))
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.ach_back),
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = stringResource(R.string.ach_page_title_display),
                color = MaterialTheme.colorScheme.onBackground,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.ach_page_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.5.sp,
                lineHeight = 12.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        AchievementRankBadge(points = state.totalPoints)
    }
}

@Composable
private fun AchievementRankBadge(points: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .width(128.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(17.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f))
            .border(1.dp, Color(0xFF00F5FF).copy(alpha = 0.22f), RoundedCornerShape(17.dp))
            .padding(horizontal = 8.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ach_icon_legend_rank),
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(1.dp)
        ) {
            Text(
                text = stringResource(R.string.ach_legend_rank),
                color = Color(0xFF00F5FF),
                fontSize = 8.2.sp,
                lineHeight = 9.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = points.formatFullAchievementNumber(),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                lineHeight = 19.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.ach_total_points_subtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 8.2.sp,
                lineHeight = 9.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AchievementLibrarySheetContent(
    state: AchievementsUiState,
    onClose: () -> Unit,
    onOpenDetail: (AchievementItemUi) -> Unit
) {
    val ordered = remember(state.achievements) {
        state.achievements.sortedWith(
            compareBy<AchievementItemUi> { it.definition.category.ordinal }
                .thenBy { it.definition.target }
                .thenBy { it.definition.id }
        )
    }
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.92f),
        contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 4.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = stringResource(R.string.ach_library_title),
                        color = MaterialTheme.colorScheme.onSurface,
                        fontSize = 19.sp,
                        lineHeight = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = stringResource(R.string.ach_profile_card_subtitle, state.unlockedCount, state.totalCount),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp,
                        lineHeight = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
        item { AchievementLibrarySummaryRow(state = state) }
        ordered.chunked(4).forEach { rowItems ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    rowItems.forEach { item ->
                        AchievementLibraryGridTile(
                            item = item,
                            modifier = Modifier.weight(1f),
                            onClick = { onOpenDetail(item) }
                        )
                    }
                    repeat((4 - rowItems.size).coerceAtLeast(0)) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun AchievementLibrarySummaryRow(state: AchievementsUiState) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        AchievementLibraryMiniStat(
            label = stringResource(R.string.ach_achievements),
            value = "${state.unlockedCount}/${state.totalCount}",
            color = Color(0xFF00F5FF),
            modifier = Modifier.weight(1f)
        )
        AchievementLibraryMiniStat(
            label = stringResource(R.string.ach_total_points),
            value = state.totalPoints.formatFullAchievementNumber(),
            color = Color(0xFFFFB02E),
            modifier = Modifier.weight(1f)
        )
        AchievementLibraryMiniStat(
            label = stringResource(R.string.ach_complete_percent, (state.completion * 100).toInt()),
            value = "${(state.completion * 100).toInt()}%",
            color = Color(0xFF4AFF8A),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AchievementLibraryMiniStat(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.10f))
            .border(1.dp, color.copy(alpha = 0.24f), RoundedCornerShape(16.dp))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, fontSize = 15.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 8.8.sp, lineHeight = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AchievementLibraryGridTile(
    item: AchievementItemUi,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val accent = item.definition.rarity.accentColor()
    val cardAccent = if (item.unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant
    val contentAlpha = if (item.unlocked) 1f else 0.34f
    Column(
        modifier = modifier
            .height(124.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardAccent.copy(alpha = if (item.unlocked) 0.09f else 0.055f))
            .border(1.dp, cardAccent.copy(alpha = if (item.unlocked) 0.23f else 0.13f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 5.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Image(
            painter = painterResource(item.definition.iconRes),
            contentDescription = null,
            modifier = Modifier
                .size(56.dp)
                .alpha(contentAlpha)
        )
        Text(
            text = stringResource(item.definition.titleRes),
            color = if (item.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.70f),
            fontSize = 8.6.sp,
            lineHeight = 9.2.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = achievementLibraryStatusText(item),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 7.5.sp,
            lineHeight = 8.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!item.unlocked) {
            ProgressLine(progress = item.progress, color = if (item.unlocked) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), modifier = Modifier.fillMaxWidth(0.78f))
        }
    }
}

@Composable
private fun AchievementLibraryDetailCloud(item: AchievementItemUi) {
    val accent = item.definition.rarity.accentColor()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(accent.copy(alpha = 0.11f))
            .border(1.dp, accent.copy(alpha = 0.30f), RoundedCornerShape(22.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Image(
            painter = painterResource(item.definition.iconRes),
            contentDescription = null,
            modifier = Modifier.size(58.dp)
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(item.definition.titleRes),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                lineHeight = 15.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(item.definition.descriptionRes),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 11.5.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            ProgressLine(progress = item.progress, color = accent, modifier = Modifier.fillMaxWidth(0.92f))
        }
        Text(
            text = achievementLibraryStatusText(item),
            color = accent,
            fontSize = 8.2.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(accent.copy(alpha = 0.13f))
                .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(999.dp))
                .padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun achievementLibraryStatusText(item: AchievementItemUi): String = when {
    item.unlocked -> stringResource(R.string.ach_status_unlocked)
    item.progress > 0f -> stringResource(R.string.ach_status_in_progress)
    else -> stringResource(R.string.ach_status_locked)
}


@Composable
private fun AchievementCategoryFilter(
    selected: AchievementCategory,
    selectedRarity: AchievementRarity?,
    onSelected: (AchievementCategory) -> Unit,
    onRarityClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.ach_gallery),
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f)
            )
            val rarityAccent = selectedRarity?.accentColor() ?: MaterialTheme.colorScheme.onSurfaceVariant
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(13.dp))
                    .background(rarityAccent.copy(alpha = if (selectedRarity != null) 0.16f else 0.08f))
                    .border(1.dp, rarityAccent.copy(alpha = if (selectedRarity != null) 0.48f else 0.12f), RoundedCornerShape(13.dp))
                    .clickable { onRarityClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.FilterList, null, tint = rarityAccent, modifier = Modifier.size(19.dp))
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AchievementCategory.values().forEach { category ->
                CategoryChip(
                    label = categoryLabel(category),
                    selected = selected == category,
                    color = category.accentColor(),
                    onClick = { onSelected(category) }
                )
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AchievementRarityFilterSheet(
    selected: AchievementRarity?,
    onSelected: (AchievementRarity?) -> Unit,
    onClose: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = { onClose() },
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.ach_rarity_filter_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 19.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
                AchievementRarityOption(
                    label = stringResource(R.string.ach_rarity_all),
                    color = Color(0xFF00F5FF),
                    selected = selected == null,
                    onClick = { onSelected(null) }
                )
                AchievementRarity.values().forEach { rarity ->
                    AchievementRarityOption(
                        label = rarityLabel(rarity),
                        color = rarity.accentColor(),
                        selected = selected == rarity,
                        onClick = { onSelected(rarity) }
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementRarityOption(
    label: String,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = if (selected) 0.16f else 0.07f))
            .border(1.dp, color.copy(alpha = if (selected) 0.55f else 0.16f), RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Text(
            text = label,
            color = if (selected) color else MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (selected) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun AchievementDetailPage(
    item: AchievementItemUi,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val title = stringResource(item.definition.titleRes)
    val description = stringResource(item.definition.descriptionRes)
    val status = achievementLibraryStatusText(item)
    val preparing = stringResource(R.string.ach_share_preparing)
    val accent = if (item.unlocked) item.definition.rarity.accentColor() else MaterialTheme.colorScheme.onSurfaceVariant
    val contentAlpha = if (item.unlocked) 1f else 0.42f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF02050A))
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF02050A),
                        if (item.unlocked) accent.copy(alpha = 0.22f) else Color(0xFF111317),
                        Color(0xFF02050A)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 22.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = status,
                    color = accent,
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(42.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.09f))
                        .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.15f), RoundedCornerShape(16.dp))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(item.definition.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (item.unlocked) 218.dp else 190.dp)
                        .alpha(contentAlpha)
                )

                Spacer(Modifier.height(18.dp))

                Text(
                    text = title,
                    color = if (item.unlocked) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 29.sp,
                    lineHeight = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    maxLines = 4,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )

                Spacer(Modifier.height(18.dp))

                AchievementDetailProgressCard(item = item, accent = accent)

                if (!item.unlocked) {
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = stringResource(R.string.ach_detail_locked_hint),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
                        fontSize = 11.sp,
                        lineHeight = 13.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            if (item.unlocked) {
                AchievementSharePanel(
                    onFacebook = { shareAchievement(context, item, title, description, status, preparing, AchievementShareTarget.FACEBOOK) },
                    onX = { shareAchievement(context, item, title, description, status, preparing, AchievementShareTarget.X) },
                    onInstagram = { shareAchievement(context, item, title, description, status, preparing, AchievementShareTarget.INSTAGRAM) },
                    onMore = { shareAchievement(context, item, title, description, status, preparing, AchievementShareTarget.MORE) }
                )
            }
        }
    }
}

@Composable
private fun AchievementDetailProgressCard(
    item: AchievementItemUi,
    accent: Color
) {
    val rarityText = rarityLabel(item.definition.rarity)
    val requirement = item.definition.requirementText()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF071018).copy(alpha = if (item.unlocked) 0.88f else 0.58f))
            .border(1.dp, accent.copy(alpha = if (item.unlocked) 0.34f else 0.16f), RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = rarityText,
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "${(item.progress.coerceIn(0f, 1f) * 100f).roundToInt()}%",
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
        ProgressLine(
            progress = item.progress,
            color = accent,
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = stringResource(R.string.ach_detail_how_to_unlock),
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 12.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = requirement,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            lineHeight = 15.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun AchievementSharePanel(
    onFacebook: () -> Unit,
    onX: () -> Unit,
    onInstagram: () -> Unit,
    onMore: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.ach_share_title),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            fontSize = 16.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.62f))
                .border(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f), RoundedCornerShape(28.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            AchievementSocialButton(label = "f", color = Color(0xFF3F7BFF), onClick = onFacebook)
            AchievementSocialButton(label = "𝕏", color = Color(0xFF050505), onClick = onX)
            AchievementSocialButton(label = "◎", color = Color(0xFFFF4FC3), onClick = onInstagram)
            AchievementSocialButton(label = "•••", color = Color(0xFF7893A8), onClick = onMore)
        }
    }
}

@Composable
private fun AchievementSocialButton(
    label: String,
    color: Color,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color.copy(alpha = 0.88f))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = if (label == "•••") 20.sp else 24.sp,
            lineHeight = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun rarityLabel(rarity: AchievementRarity): String = when (rarity) {
    AchievementRarity.COMMON -> stringResource(R.string.ach_rarity_common)
    AchievementRarity.UNCOMMON -> stringResource(R.string.ach_rarity_uncommon)
    AchievementRarity.RARE -> stringResource(R.string.ach_rarity_rare)
    AchievementRarity.EPIC -> stringResource(R.string.ach_rarity_epic)
    AchievementRarity.LEGENDARY -> stringResource(R.string.ach_rarity_legendary)
    AchievementRarity.MYTHIC -> stringResource(R.string.ach_rarity_mythic)
}


@Composable
private fun AchievementDefinition.requirementText(): String {
    return stringResource(unlockHintRes())
}

private fun Float.formatAchievementRequirementNumberForDetail(): String {
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

private fun shareAchievement(
    context: Context,
    item: AchievementItemUi,
    title: String,
    description: String,
    status: String,
    preparingMessage: String,
    target: AchievementShareTarget
) {
    Toast.makeText(context, preparingMessage, Toast.LENGTH_LONG).show()
    val uri = runCatching {
        createAchievementShareImage(context, item, title, description, status)
    }.getOrNull()

    if (uri == null) {
        val fallback = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "$title\n$description")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(fallback, title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return
    }

    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        when (target) {
            AchievementShareTarget.FACEBOOK -> setPackage("com.facebook.katana")
            AchievementShareTarget.X -> setPackage("com.twitter.android")
            AchievementShareTarget.INSTAGRAM -> setPackage("com.instagram.android")
            AchievementShareTarget.MORE -> Unit
        }
    }
    runCatching {
        context.startActivity(shareIntent)
    }.getOrElse {
        val chooser = Intent.createChooser(shareIntent.setPackage(null), title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}

private fun createAchievementShareImage(
    context: Context,
    item: AchievementItemUi,
    title: String,
    description: String,
    status: String
): android.net.Uri {
    val width = 1080
    val height = 1920
    val accent = item.definition.rarity.accentColor().toAndroidColorInt()
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f, 0f, width.toFloat(), height.toFloat(),
            intArrayOf(
                AndroidColor.rgb(2, 5, 10),
                darkenColor(accent, 0.24f),
                AndroidColor.rgb(3, 16, 28),
                AndroidColor.rgb(2, 5, 10)
            ),
            floatArrayOf(0f, 0.42f, 0.78f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(26, AndroidColor.red(accent), AndroidColor.green(accent), AndroidColor.blue(accent))
    }
    canvas.drawCircle(width / 2f, 650f, 360f, glowPaint)

    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.WHITE
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(215, 176, 190, 210)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }

    textPaint.textSize = 62f
    canvas.drawText(status, width / 2f, 190f, textPaint)

    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(150, AndroidColor.red(accent), AndroidColor.green(accent), AndroidColor.blue(accent))
        strokeWidth = 4f
    }
    canvas.drawLine(260f, 250f, 470f, 250f, dividerPaint)
    canvas.drawLine(610f, 250f, 820f, 250f, dividerPaint)
    textPaint.textSize = 38f
    canvas.drawText("✦", width / 2f, 265f, textPaint)

    val iconBitmap = BitmapFactory.decodeResource(context.resources, item.definition.iconRes)
    if (iconBitmap != null) {
        val targetRect = Rect(245, 430, 835, 1020)
        canvas.drawBitmap(iconBitmap, null, targetRect, Paint(Paint.ANTI_ALIAS_FLAG))
    }

    textPaint.textSize = 68f
    drawCenteredMultiline(canvas, title, textPaint, width / 2f, 1140f, 76f, width - 170)
    mutedPaint.textSize = 38f
    drawCenteredMultiline(canvas, description, mutedPaint, width / 2f, 1270f, 48f, width - 190)

    val statRect = android.graphics.RectF(150f, 1430f, 930f, 1540f)
    val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.argb(48, 255, 255, 255) }
    val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = accent
        style = Paint.Style.STROKE
        strokeWidth = 4f
    }
    canvas.drawRoundRect(statRect, 48f, 48f, badgePaint)
    canvas.drawRoundRect(statRect, 48f, 48f, strokePaint)
    accentPaint.textSize = 34f
    canvas.drawText(context.getString(R.string.ach_share_earned_in_stepforge), width / 2f, 1498f, accentPaint)

    val footerRect = android.graphics.RectF(72f, 1650f, 1008f, 1848f)
    val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            72f, 1650f, 1008f, 1848f,
            intArrayOf(AndroidColor.argb(235, 8, 20, 34), AndroidColor.argb(235, 16, 70, 94)),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRoundRect(footerRect, 46f, 46f, footerPaint)
    val footerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(120, AndroidColor.red(accent), AndroidColor.green(accent), AndroidColor.blue(accent))
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    canvas.drawRoundRect(footerRect, 46f, 46f, footerStroke)

    val logoPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = accent }
    canvas.drawCircle(162f, 1749f, 56f, logoPaint)
    textPaint.textSize = 50f
    canvas.drawText("S", 162f, 1767f, textPaint)

    textPaint.textAlign = Paint.Align.LEFT
    textPaint.textSize = 48f
    canvas.drawText(context.getString(R.string.ach_share_app_title), 245f, 1732f, textPaint)
    mutedPaint.textAlign = Paint.Align.LEFT
    mutedPaint.textSize = 30f
    canvas.drawText(context.getString(R.string.ach_share_app_subtitle), 245f, 1784f, mutedPaint)
    mutedPaint.textSize = 25f
    canvas.drawText(context.getString(R.string.ach_share_app_cta), 245f, 1825f, mutedPaint)

    val file = File(context.cacheDir, "achievement_share_${item.definition.id}.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun drawCenteredMultiline(
    canvas: AndroidCanvas,
    text: String,
    paint: Paint,
    centerX: Float,
    startY: Float,
    lineHeight: Float,
    maxWidth: Int
) {
    val words = text.split(" ")
    val lines = mutableListOf<String>()
    var current = ""
    words.forEach { word ->
        val attempt = if (current.isBlank()) word else "$current $word"
        if (paint.measureText(attempt) <= maxWidth || current.isBlank()) {
            current = attempt
        } else {
            lines += current
            current = word
        }
    }
    if (current.isNotBlank()) lines += current
    val top = startY - ((lines.size - 1) * lineHeight / 2f)
    lines.take(4).forEachIndexed { index, line ->
        canvas.drawText(line, centerX, top + index * lineHeight, paint)
    }
}

private fun Color.toAndroidColorInt(): Int = AndroidColor.argb(
    (alpha * 255).roundToInt().coerceIn(0, 255),
    (red * 255).roundToInt().coerceIn(0, 255),
    (green * 255).roundToInt().coerceIn(0, 255),
    (blue * 255).roundToInt().coerceIn(0, 255)
)

private fun darkenColor(color: Int, factor: Float): Int {
    val r = (AndroidColor.red(color) * factor).roundToInt().coerceIn(0, 255)
    val g = (AndroidColor.green(color) * factor).roundToInt().coerceIn(0, 255)
    val b = (AndroidColor.blue(color) * factor).roundToInt().coerceIn(0, 255)
    return AndroidColor.rgb(r, g, b)
}

@Composable
internal fun ProfileAchievementEntryCard(
    state: AchievementsUiState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val featured = state.bestAchievement ?: state.nextTarget
    AchievementCardShell(modifier = modifier, highlight = true) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick() }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (featured != null) {
                Image(
                    painter = painterResource(featured.definition.iconRes),
                    contentDescription = null,
                    modifier = Modifier.size(70.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00F5FF).copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Outlined.EmojiEvents, null, tint = Color(0xFF00F5FF), modifier = Modifier.size(34.dp))
                }
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(
                    text = stringResource(R.string.ach_profile_card_title),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.ach_profile_card_subtitle, state.unlockedCount, state.totalCount),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                ProgressLine(progress = state.completion, color = Color(0xFF00F5FF))
            }
            Icon(
                imageVector = Icons.Outlined.EmojiEvents,
                contentDescription = null,
                tint = Color(0xFFFFB02E),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun categoryLabel(category: AchievementCategory): String = when (category) {
    AchievementCategory.ALL -> stringResource(R.string.ach_cat_all)
    AchievementCategory.STEPS -> stringResource(R.string.ach_cat_steps)
    AchievementCategory.DISTANCE -> stringResource(R.string.ach_cat_distance)
    AchievementCategory.CALORIES -> stringResource(R.string.ach_cat_calories)
    AchievementCategory.STREAKS -> stringResource(R.string.ach_cat_streaks)
    AchievementCategory.WORKOUTS -> stringResource(R.string.ach_cat_workouts)
    AchievementCategory.TIME -> stringResource(R.string.ach_cat_time)
    AchievementCategory.WEATHER -> stringResource(R.string.ach_cat_weather)
    AchievementCategory.SPECIAL -> stringResource(R.string.ach_cat_special)
}
