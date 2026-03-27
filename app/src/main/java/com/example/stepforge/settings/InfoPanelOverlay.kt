package com.example.stepforge.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

/**
 * SettingsScreen’de çağırdığın InfoPanel overlay’i.
 * Ekranın üst katmanında görünür ve boş alana tıklayınca kapanır.
 */
@Composable
fun InfoPanelOverlay(
    anchor: Rect,              // i ikonunun window koordinatları, SettingBlock/Item'dan geliyor
    text: String,              // gösterilecek açıklama
    rootSize: IntSize,         // SettingsScreen root ölçüsü (px)
    density: Density,          // LocalDensity.current’ı geçiriyorsun
    onDismiss: () -> Unit,     // boş alana tıklandığında kapat
    darkTheme: Boolean? = null // ✅ eklendi (opsiyonel) — mevcut çağrılar bozulmaz
) {
    var panelSize by remember { mutableStateOf(IntSize.Zero) }
    val margin: Dp = 8.dp
    val marginPx = with(density) { margin.toPx() }
    val rootW = rootSize.width.toFloat()
    val rootH = rootSize.height.toFloat()
    val maxPanelWidthPx = (rootW - 2 * marginPx).coerceAtLeast(80f)
    val maxPanelWidthDp = with(density) { maxPanelWidthPx.toDp() }

    fun computeOffset(): IntOffset {
        val pw = (if (panelSize.width > 0) panelSize.width.toFloat() else maxPanelWidthPx * 0.8f)
            .coerceAtMost(maxPanelWidthPx)
        val ph = (if (panelSize.height > 0) panelSize.height.toFloat() else with(density) { 64.dp.toPx() })

        val centerX = (anchor.left + anchor.right) / 2f
        val rawX = centerX - pw / 2f
        val minX = marginPx
        val maxX = (rootW - pw - marginPx).coerceAtLeast(minX)
        val x = rawX.coerceIn(minX, maxX)

        val fromTop = anchor.top - ph - marginPx
        val minY = marginPx
        val maxY = (rootH - ph - marginPx).coerceAtLeast(minY)
        val y = if (fromTop >= minY) fromTop.coerceIn(minY, maxY)
        else (anchor.bottom + marginPx).coerceIn(minY, maxY)

        return IntOffset(x.toInt(), y.toInt())
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .zIndex(100f)
            .pointerInput(Unit) { detectTapGestures { onDismiss() } },
        contentAlignment = Alignment.TopStart
    ) {
        var visible by remember { mutableStateOf(false) }

        LaunchedEffect(Unit) {
            visible = true
        }

        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(
                animationSpec = tween(220)
            ) + scaleIn(
                initialScale = 0.92f,
                animationSpec = tween(260, easing = FastOutSlowInEasing)
            ),

            exit = fadeOut(
                animationSpec = tween(150)
            ) + scaleOut(
                targetScale = 0.92f,
                animationSpec = tween(180)
            )
        ) {
            Box(
                Modifier
                    .onGloballyPositioned { panelSize = it.size }
                    .zIndex(101f)
                    .then(
                        Modifier
                            .align(Alignment.TopStart)
                            .offset { computeOffset() }
                    )
            ) {
                InfoPanel(
                    text = text,
                    maxWidth = maxPanelWidthDp,
                    darkTheme = darkTheme // ✅ artık Settings’teki isDark ile senkron
                )
            }
        }
    }
}