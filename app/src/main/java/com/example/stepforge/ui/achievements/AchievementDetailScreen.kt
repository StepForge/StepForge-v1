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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

private enum class AchievementDetailShareTarget {
    FACEBOOK,
    X,
    INSTAGRAM,
    MORE
}

@Composable
internal fun AchievementDetailRoute(
    item: AchievementItemUi,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val darkTheme = isSystemInDarkTheme()
    val cs = MaterialTheme.colorScheme
    val title = stringResource(item.definition.titleRes)
    val description = stringResource(item.definition.descriptionRes)
    val status = achievementDetailStatusText(item)
    val preparing = stringResource(R.string.ach_share_preparing)
    val accent = if (item.unlocked) item.definition.rarity.accentColor() else cs.onSurfaceVariant
    val contentAlpha = if (item.unlocked) 1f else 0.42f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .background(
                Brush.verticalGradient(
                    colors = if (darkTheme) {
                        listOf(Color(0xFF02050A), accent.copy(alpha = if (item.unlocked) 0.22f else 0.10f), Color(0xFF02050A))
                    } else {
                        listOf(Color(0xFFF8FCFF), accent.copy(alpha = if (item.unlocked) 0.13f else 0.06f), Color(0xFFEFF7FD))
                    }
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
                    .height(56.dp),
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
                        .background(cs.surface.copy(alpha = if (darkTheme) 0.62f else 0.88f))
                        .border(1.dp, cs.onSurface.copy(alpha = 0.14f), RoundedCornerShape(16.dp))
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.common_close),
                        tint = cs.onSurface,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AchievementDetailMainCard(
                    item = item,
                    title = title,
                    description = description,
                    accent = accent,
                    contentAlpha = contentAlpha
                )
            }

            if (item.unlocked) {
                AchievementDetailSharePanel(
                    onFacebook = { shareAchievement(context, item, title, description, status, preparing, AchievementDetailShareTarget.FACEBOOK) },
                    onX = { shareAchievement(context, item, title, description, status, preparing, AchievementDetailShareTarget.X) },
                    onInstagram = { shareAchievement(context, item, title, description, status, preparing, AchievementDetailShareTarget.INSTAGRAM) },
                    onMore = { shareAchievement(context, item, title, description, status, preparing, AchievementDetailShareTarget.MORE) }
                )
            } else {
                Spacer(Modifier.height(18.dp))
            }
        }
    }
}

@Composable
private fun AchievementDetailMainCard(
    item: AchievementItemUi,
    title: String,
    description: String,
    accent: Color,
    contentAlpha: Float
) {
    val cs = MaterialTheme.colorScheme
    AchievementCardShell(highlight = item.unlocked) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = if (item.unlocked) 0.10f else 0.04f)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(item.definition.iconRes),
                    contentDescription = null,
                    modifier = Modifier
                        .size(if (item.unlocked) 190.dp else 172.dp)
                        .alpha(contentAlpha)
                )
            }

            Text(
                text = title,
                color = if (item.unlocked) cs.onSurface else cs.onSurfaceVariant,
                fontSize = 29.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = description,
                color = cs.onSurfaceVariant,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )

            AchievementDetailInfoCard(item = item, accent = accent)
        }
    }
}

@Composable
private fun AchievementDetailInfoCard(item: AchievementItemUi, accent: Color) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(accent.copy(alpha = if (item.unlocked) 0.10f else 0.06f))
            .border(1.dp, accent.copy(alpha = 0.24f), RoundedCornerShape(20.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ach_detail_how_to_unlock),
                color = cs.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = achievementDetailRarityLabel(item.definition.rarity),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Text(
            text = item.definition.requirementTextForDetail(),
            color = cs.onSurfaceVariant,
            fontSize = 11.sp,
            lineHeight = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (!item.unlocked) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.progressValueText(),
                    color = cs.onSurface,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${(item.progress.coerceIn(0f, 1f) * 100f).toInt()}%",
                    color = accent,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            ProgressLine(progress = item.progress, color = accent)
        } else {
            Text(
                text = stringResource(R.string.ach_detail_completed_clean),
                color = accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun AchievementDetailSharePanel(
    onFacebook: () -> Unit,
    onX: () -> Unit,
    onInstagram: () -> Unit,
    onMore: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(cs.surface.copy(alpha = if (isSystemInDarkTheme()) 0.78f else 0.92f))
            .border(1.dp, cs.onSurface.copy(alpha = 0.10f), RoundedCornerShape(28.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = stringResource(R.string.ach_share_title),
            color = cs.onSurface.copy(alpha = 0.68f),
            fontSize = 15.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShareButton(label = "f", color = Color(0xFF3F78F2), onClick = onFacebook)
            ShareButton(label = "𝕏", color = Color(0xFF030303), onClick = onX)
            ShareButton(label = "◎", color = Color(0xFFE843B5), onClick = onInstagram)
            ShareButton(label = "•••", color = Color(0xFF8BA2B2), onClick = onMore)
        }
    }
}

@Composable
private fun ShareButton(label: String, color: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(58.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(color)
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
private fun achievementDetailStatusText(item: AchievementItemUi): String = when {
    item.unlocked -> stringResource(R.string.ach_status_unlocked)
    item.progress > 0f -> stringResource(R.string.ach_status_in_progress)
    else -> stringResource(R.string.ach_status_locked)
}

@Composable
private fun achievementDetailRarityLabel(rarity: AchievementRarity): String = when (rarity) {
    AchievementRarity.COMMON -> stringResource(R.string.ach_rarity_common)
    AchievementRarity.UNCOMMON -> stringResource(R.string.ach_rarity_uncommon)
    AchievementRarity.RARE -> stringResource(R.string.ach_rarity_rare)
    AchievementRarity.EPIC -> stringResource(R.string.ach_rarity_epic)
    AchievementRarity.LEGENDARY -> stringResource(R.string.ach_rarity_legendary)
    AchievementRarity.MYTHIC -> stringResource(R.string.ach_rarity_mythic)
}

@Composable
private fun AchievementDefinition.requirementTextForDetail(): String {
    val value = target.formatAchievementRequirementNumberForDetail()
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

@Composable
private fun AchievementItemUi.progressValueText(): String {
    val unit = when (definition.category) {
        AchievementCategory.STEPS -> stringResource(R.string.ach_unit_steps)
        AchievementCategory.DISTANCE -> stringResource(R.string.ach_unit_km)
        AchievementCategory.CALORIES -> stringResource(R.string.ach_unit_kcal)
        AchievementCategory.STREAKS -> stringResource(R.string.ach_unit_days)
        AchievementCategory.WORKOUTS -> stringResource(R.string.ach_unit_workouts)
        AchievementCategory.TIME -> stringResource(R.string.ach_unit_minutes)
        AchievementCategory.WEATHER -> stringResource(R.string.ach_unit_times)
        AchievementCategory.SPECIAL -> stringResource(R.string.ach_unit_times)
        AchievementCategory.ALL -> stringResource(R.string.ach_unit_times)
    }
    return stringResource(
        R.string.ach_detail_progress_value,
        current.formatAchievementRequirementNumberForDetail(),
        definition.target.formatAchievementRequirementNumberForDetail(),
        unit
    )
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
    target: AchievementDetailShareTarget
) {
    Toast.makeText(context, preparingMessage, Toast.LENGTH_LONG).show()
    val uri = runCatching { createAchievementShareImage(context, item, title, description, status) }.getOrNull()
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
            AchievementDetailShareTarget.FACEBOOK -> setPackage("com.facebook.katana")
            AchievementDetailShareTarget.X -> setPackage("com.twitter.android")
            AchievementDetailShareTarget.INSTAGRAM -> setPackage("com.instagram.android")
            AchievementDetailShareTarget.MORE -> Unit
        }
    }
    runCatching { context.startActivity(shareIntent) }.getOrElse {
        context.startActivity(Intent.createChooser(shareIntent.setPackage(null), title).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}

private fun createAchievementShareImage(
    context: Context,
    item: AchievementItemUi,
    title: String,
    description: String,
    status: String
): android.net.Uri {
    val metrics = context.resources.displayMetrics
    val width = metrics.widthPixels.coerceAtLeast(1080)
    val height = metrics.heightPixels.coerceAtLeast((width * 2.0f).toInt())
    val accent = item.definition.rarity.accentColor().toAndroidColorInt()
    val gold = AndroidColor.rgb(214, 180, 112)
    val offWhite = AndroidColor.rgb(245, 240, 228)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)

    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            0f,
            0f,
            width.toFloat(),
            height.toFloat(),
            intArrayOf(
                AndroidColor.rgb(4, 14, 36),
                AndroidColor.rgb(5, 20, 56),
                blendColor(AndroidColor.rgb(6, 26, 64), accent, 0.10f),
                AndroidColor.rgb(4, 14, 36)
            ),
            floatArrayOf(0f, 0.22f, 0.72f, 1f),
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

    val shimmer = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader = LinearGradient(
            width * 0.18f,
            0f,
            width * 0.82f,
            height.toFloat(),
            intArrayOf(AndroidColor.argb(18, 255, 255, 255), AndroidColor.argb(0, 255, 255, 255), AndroidColor.argb(16, 255, 255, 255)),
            null,
            Shader.TileMode.CLAMP
        )
    }
    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), shimmer)

    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = offWhite
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(220, 230, 235, 243)
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.NORMAL)
    }
    val accentPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = gold
        textAlign = Paint.Align.CENTER
        typeface = android.graphics.Typeface.create(android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD)
    }
    val dividerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(220, 202, 174, 109)
        strokeWidth = width * 0.0028f
    }

    val statusY = height * 0.095f
    titlePaint.textSize = width * 0.075f
    canvas.drawText(status, width / 2f, statusY, titlePaint)
    val lineY = statusY + width * 0.040f
    canvas.drawLine(width * 0.18f, lineY, width * 0.44f, lineY, dividerPaint)
    canvas.drawLine(width * 0.56f, lineY, width * 0.82f, lineY, dividerPaint)
    accentPaint.textSize = width * 0.035f
    canvas.drawText("◇", width / 2f, lineY + width * 0.014f, accentPaint)

    val iconBitmap = BitmapFactory.decodeResource(context.resources, item.definition.iconRes)?.trimTransparentPixels()
    if (iconBitmap != null) {
        val maxIcon = (width * 0.62f).toInt()
        val iconTop = (height * 0.16f).toInt()
        val ratio = iconBitmap.width.toFloat() / iconBitmap.height.toFloat()
        val targetW: Int
        val targetH: Int
        if (ratio >= 1f) {
            targetW = maxIcon
            targetH = (maxIcon / ratio).toInt()
        } else {
            targetH = maxIcon
            targetW = (maxIcon * ratio).toInt()
        }
        val left = (width - targetW) / 2
        val top = iconTop
        val rect = Rect(left, top, left + targetW, top + targetH)
        canvas.drawBitmap(iconBitmap, null, rect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
    }

    titlePaint.textSize = width * 0.080f
    drawCenteredMultiline(canvas, title, titlePaint, width / 2f, height * 0.53f, width * 0.085f, (width * 0.80f).toInt())
    bodyPaint.textSize = width * 0.043f
    drawCenteredMultiline(canvas, description, bodyPaint, width / 2f, height * 0.615f, width * 0.054f, (width * 0.78f).toInt())

    val chipLeft = width * 0.22f
    val chipTop = height * 0.685f
    val chipRight = width * 0.78f
    val chipBottom = chipTop + height * 0.052f
    val chipRect = android.graphics.RectF(chipLeft, chipTop, chipRight, chipBottom)
    val chipBg = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.argb(26, 255, 255, 255) }
    val chipStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(208, 202, 174, 109)
        style = Paint.Style.STROKE
        strokeWidth = width * 0.0025f
    }
    canvas.drawRoundRect(chipRect, width * 0.03f, width * 0.03f, chipBg)
    canvas.drawRoundRect(chipRect, width * 0.03f, width * 0.03f, chipStroke)
    accentPaint.textSize = width * 0.032f
    canvas.drawText("✦ ${context.getString(R.string.ach_share_earned_in_stepforge)}", width / 2f, chipTop + (chipBottom - chipTop) * 0.66f, accentPaint)

    val footerTop = height * 0.742f
    val footerBottom = height * 0.948f
    val footerRect = android.graphics.RectF(width * 0.08f, footerTop, width * 0.92f, footerBottom)
    val footerHeight = footerRect.height()
    val footerBg = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(132, 10, 26, 52)
    }
    val footerStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(110, 110, 150, 210)
        style = Paint.Style.STROKE
        strokeWidth = width * 0.0025f
    }
    canvas.drawRoundRect(footerRect, width * 0.04f, width * 0.04f, footerBg)
    canvas.drawRoundRect(footerRect, width * 0.04f, width * 0.04f, footerStroke)

    val iconBox = android.graphics.RectF(
        width * 0.115f,
        footerTop + footerHeight * 0.22f,
        width * 0.355f,
        footerTop + footerHeight * 0.78f
    )
    val iconPanel = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(120, 14, 31, 67)
    }
    canvas.drawRoundRect(iconBox, width * 0.03f, width * 0.03f, iconPanel)

    runCatching {
        val drawable = context.packageManager.getApplicationIcon(context.packageName)
        val pad = width * 0.018f
        drawable.setBounds(
            (iconBox.left + pad).roundToInt(),
            (iconBox.top + pad).roundToInt(),
            (iconBox.right - pad).roundToInt(),
            (iconBox.bottom - pad).roundToInt()
        )
        drawable.draw(canvas)
    }.onFailure {
        val fallbackIcon = BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher)
        if (fallbackIcon != null) {
            val pad = width * 0.018f
            val logoRect = Rect(
                (iconBox.left + pad).roundToInt(),
                (iconBox.top + pad).roundToInt(),
                (iconBox.right - pad).roundToInt(),
                (iconBox.bottom - pad).roundToInt()
            )
            canvas.drawBitmap(fallbackIcon, null, logoRect, Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
        }
    }

    titlePaint.textAlign = Paint.Align.LEFT
    bodyPaint.textAlign = Paint.Align.LEFT
    titlePaint.textSize = width * 0.060f
    val textX = width * 0.40f
    val textMaxWidth = (footerRect.right - textX - width * 0.055f).roundToInt()
    canvas.drawText(context.getString(R.string.ach_share_app_title), textX, footerTop + footerHeight * 0.25f, titlePaint)

    drawLeftFittedMultiline(
        canvas = canvas,
        text = context.getString(R.string.ach_share_app_subtitle_clean) + " " + context.getString(R.string.ach_share_app_cta_clean),
        paint = bodyPaint,
        startX = textX,
        startY = footerTop + footerHeight * 0.44f,
        maxWidth = textMaxWidth,
        maxBottom = footerRect.bottom - footerHeight * 0.10f,
        maxLines = 5,
        initialTextSize = width * 0.037f,
        minTextSize = width * 0.024f
    )

    val file = File(context.cacheDir, "achievement_share_${item.definition.id}.png")
    FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, out) }
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
}

private fun drawLeftFittedMultiline(
    canvas: AndroidCanvas,
    text: String,
    paint: Paint,
    startX: Float,
    startY: Float,
    maxWidth: Int,
    maxBottom: Float,
    maxLines: Int,
    initialTextSize: Float,
    minTextSize: Float
) {
    var chosenSize = initialTextSize
    var chosenLines = emptyList<String>()
    var chosenLineHeight = initialTextSize * 1.24f

    var size = initialTextSize
    while (size >= minTextSize) {
        paint.textSize = size
        val lineHeight = size * 1.24f
        val lines = wrapTextLines(text, paint, maxWidth)
        val fitsLineCount = lines.size <= maxLines
        val fitsHeight = startY + ((lines.size - 1).coerceAtLeast(0) * lineHeight) <= maxBottom
        chosenSize = size
        chosenLines = lines
        chosenLineHeight = lineHeight
        if (fitsLineCount && fitsHeight) break
        size -= 2f
    }

    paint.textSize = chosenSize
    val visibleLines = if (chosenLines.size <= maxLines) {
        chosenLines
    } else {
        chosenLines.take(maxLines)
    }
    visibleLines.forEachIndexed { index, line ->
        canvas.drawText(line, startX, startY + index * chosenLineHeight, paint)
    }
}

private fun wrapTextLines(text: String, paint: Paint, maxWidth: Int): List<String> {
    val words = text.split(" ").filter { it.isNotBlank() }
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
    return lines
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

private fun Bitmap.trimTransparentPixels(): Bitmap {
    var minX = width
    var minY = height
    var maxX = -1
    var maxY = -1
    val pixels = IntArray(width * height)
    getPixels(pixels, 0, width, 0, 0, width, height)
    for (y in 0 until height) {
        for (x in 0 until width) {
            val alpha = pixels[y * width + x] ushr 24
            if (alpha > 8) {
                if (x < minX) minX = x
                if (x > maxX) maxX = x
                if (y < minY) minY = y
                if (y > maxY) maxY = y
            }
        }
    }
    if (maxX < minX || maxY < minY) return this
    return Bitmap.createBitmap(this, minX, minY, maxX - minX + 1, maxY - minY + 1)
}

private fun Color.toAndroidColorInt(): Int = AndroidColor.argb(
    (alpha * 255).roundToInt().coerceIn(0, 255),
    (red * 255).roundToInt().coerceIn(0, 255),
    (green * 255).roundToInt().coerceIn(0, 255),
    (blue * 255).roundToInt().coerceIn(0, 255)
)

private fun blendColor(a: Int, b: Int, ratio: Float): Int {
    val t = ratio.coerceIn(0f, 1f)
    val r = (AndroidColor.red(a) * (1f - t) + AndroidColor.red(b) * t).roundToInt()
    val g = (AndroidColor.green(a) * (1f - t) + AndroidColor.green(b) * t).roundToInt()
    val blue = (AndroidColor.blue(a) * (1f - t) + AndroidColor.blue(b) * t).roundToInt()
    return AndroidColor.rgb(r, g, blue)
}
