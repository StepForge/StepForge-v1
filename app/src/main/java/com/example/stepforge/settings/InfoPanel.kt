package com.example.stepforge.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

@Composable
fun InfoPanel(
    text: String,
    maxWidth: Dp? = null,
    modifier: Modifier = Modifier,
    darkTheme: Boolean? = null // ✅ eklendi
) {
    val isDark = darkTheme ?: isSystemInDarkTheme()

    val bg = if (isDark) Color(0xFF111318) else Color(0xFFFFFFFF)
    val border = if (isDark)
        Brush.linearGradient(listOf(Color(0xFF00FFA3), Color(0xFF00F5FF)))
    else
        Brush.linearGradient(listOf(Color(0x334FD1C5), Color(0x332CB6AE)))

    val textColor = if (isDark) Color.White.copy(alpha = 0.95f) else Color(0xFF1A202C)

    val base = modifier
        .then(if (maxWidth != null) Modifier.widthIn(max = maxWidth) else Modifier)
        .shadow(10.dp, RoundedCornerShape(16.dp))
        .background(bg, RoundedCornerShape(16.dp))
        .border(1.dp, border, RoundedCornerShape(16.dp))
        .padding(horizontal = 16.dp, vertical = 12.dp)

    Box(base) {
        Text(
            text = text,
            color = textColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Normal,
            lineHeight = 20.sp
        )
    }
}