package com.example.stepforge.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingBlock(
    icon: ImageVector,
    title: String,
    infoText: String = "",
    openInfoCard: MutableState<String?>,
    onInfoAnchor: (Rect, String) -> Unit,
    darkTheme: Boolean? = null, // ✅ yeni (opsiyonel)
    content: @Composable ColumnScope.() -> Unit
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()

    val cardBg = if (isDark) Color(0xFF111318) else Color(0xFFFFFFFF)
    val titleColor = if (isDark) Color.White else Color(0xFF1A202C)
    val iconTint = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)

    var iconWindowRect by remember { mutableStateOf<Rect?>(null) }

    Card(
        Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(icon, null, tint = iconTint)
                    Spacer(Modifier.width(8.dp))
                    Text(title, color = titleColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                IconButton(
                    modifier = Modifier.onGloballyPositioned { c ->
                        iconWindowRect = c.boundsInRoot()
                    },
                    onClick = {
                        val willOpen = openInfoCard.value != title
                        openInfoCard.value = if (willOpen) title else null
                        if (willOpen) iconWindowRect?.let { onInfoAnchor(it, infoText) }
                    }
                ) {
                    Icon(Icons.Outlined.Info, null, tint = iconTint)
                }
            }
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}