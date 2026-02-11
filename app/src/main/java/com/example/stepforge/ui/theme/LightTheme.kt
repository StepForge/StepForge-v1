package com.example.stepforge.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * stepforge Light Theme (soft + mat, göz yormayan)
 * Referans: off-white / blue-gray background + mat turquoise accents.
 */
val LightColors = lightColorScheme(
    // Accent (mat turkuaz)
    primary = Color(0xFF4FD1C5),        // mat turquoise
    secondary = Color(0xFF2CB6AE),      // biraz daha koyu turkuaz

    // Surfaces
    background = Color(0xFFF5F7FA),     // açık gri-mavi (sende örnek)
    surface = Color(0xFFFFFFFF),        // kart içi beyaz ama arka plan zaten kırık
    surfaceVariant = Color(0xFFF0F3F7), // kart ayrımı

    // Text
    onPrimary = Color(0xFF0F172A),
    onSecondary = Color(0xFF0F172A),

    onBackground = Color(0xFF1A202C),   // lacivertimsi siyah (örnek)
    onSurface = Color(0xFF1A202C),
    onSurfaceVariant = Color(0xFF5B6472),

    // Outlines
    outline = Color(0x1A1A202C),
    outlineVariant = Color(0x101A202C),

    // Error
    error = Color(0xFFEF4444),
    onError = Color.White
)