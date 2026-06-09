package com.example.stepforge.ui.components

import androidx.compose.runtime.Composable

@Composable
fun PremiumGateCard(
    premiumEnabled: Boolean,
    title: String,
    subtitle: String,
    onUnlockClick: () -> Unit,
    content: @Composable () -> Unit
) {
    content()
}
