package com.example.stepforge.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.example.stepforge.R

/**
 * Integration kartlarını tek yerde toplayan küçük yardımcı composable.
 * SettingsScreen içinden çağrılır.
 */
@Composable
fun IntegrationList(
    onHealthConnectClick: () -> Unit,
    isHealthConnected: Boolean,
    darkTheme: Boolean? = null
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IntegrationCard(
            logoRes = R.drawable.health_connect_logo,
            title = "Health Connect Integration",
            description = if (isHealthConnected)
                "Connected • Your steps are synced via Health Connect."
            else
                "Connect your fitness data via Health Connect in one place.",
            onClick = onHealthConnectClick,
            darkTheme = darkTheme
        )
    }
}
