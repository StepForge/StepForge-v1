@file:OptIn(ExperimentalFoundationApi::class)

package com.example.stepforge.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun SettingItem(
    icon: ImageVector,
    title: String,
    infoText: String = "",
    openInfoCard: MutableState<String?>,
    onInfoAnchor: (Rect, String) -> Unit,
    disableBringIntoView: Boolean = false,
    alwaysExpanded: Boolean = false,
    darkTheme: Boolean? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()

    val cardBg = if (isDark) Color(0xFF0F1116) else Color(0xFFFFFFFF)
    val borderBrush = if (isDark) {
        Brush.linearGradient(listOf(Color(0xFF1B1C1F), Color(0xFF24262B)))
    } else {
        Brush.linearGradient(listOf(Color(0x1A1A202C), Color(0x101A202C)))
    }

    val titleColor = if (isDark) Color.White else Color(0xFF1A202C)
    val iconTint = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)

    var expanded by remember { mutableStateOf(alwaysExpanded) }
    val bringIntoView = if (!disableBringIntoView) remember { BringIntoViewRequester() } else null
    val scope = rememberCoroutineScope()

    var infoIconRect by remember { mutableStateOf<Rect?>(null) }

    val interaction = remember { MutableInteractionSource() }

    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, borderBrush, RoundedCornerShape(22.dp))
            .background(cardBg, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp)
            .then(
                if (!disableBringIntoView && bringIntoView != null)
                    Modifier.bringIntoViewRequester(bringIntoView)
                else Modifier
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = if (alwaysExpanded) 6.dp else 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    // ✅ Yeni Material3 ripple: deprecated rememberRipple yok
                    .indication(
                        interactionSource = interaction,
                        indication = ripple(color = iconTint.copy(alpha = 0.55f))
                    )
                    .clickable(
                        enabled = !alwaysExpanded,
                        interactionSource = interaction,
                        indication = null
                    ) {
                        expanded = !expanded
                        if (expanded && bringIntoView != null) {
                            scope.launch { bringIntoView.bringIntoView() }
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(icon, null, tint = iconTint)
                Spacer(Modifier.width(10.dp))
                Text(
                    text = title,
                    color = titleColor,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                modifier = Modifier
                    .size(40.dp)
                    .onGloballyPositioned { coords ->
                        infoIconRect = coords.boundsInWindow()
                    },
                onClick = {
                    val willOpen = openInfoCard.value != title
                    openInfoCard.value = if (willOpen) title else null
                    if (willOpen) infoIconRect?.let { onInfoAnchor(it, infoText) }
                }
            ) {
                Icon(Icons.Outlined.Info, null, tint = iconTint)
            }
        }

        if (alwaysExpanded) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                content = content
            )
        } else {
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    content = content
                )
            }
        }
    }
}