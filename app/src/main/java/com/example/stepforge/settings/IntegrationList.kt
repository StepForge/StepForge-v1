package com.example.stepforge.settings

import androidx.compose.ui.res.stringResource

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
            title = stringResource(R.string.hc_health_connect_integration),
            description = if (isHealthConnected)
                stringResource(R.string.hc_health_connected_steps)
            else
                stringResource(R.string.hc_health_connect_steps_info),
            onClick = onHealthConnectClick,
            darkTheme = darkTheme
        )
    }
}
