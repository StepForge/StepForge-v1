package com.example.stepforge.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.example.stepforge.ui.theme.DarkColors
import com.example.stepforge.ui.theme.LightColors
import com.example.stepforge.ui.theme.stepforgeTypography


@Composable
fun stepforgeTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
)
{
    val colors = if (darkTheme) DarkColors else LightColors

    MaterialTheme(
        colorScheme = colors,
        typography = stepforgeTypography,
        content = content
    )

}


