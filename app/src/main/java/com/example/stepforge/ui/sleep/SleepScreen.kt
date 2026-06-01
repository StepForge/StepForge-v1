package com.example.stepforge.ui.sleep

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepforge.R

@Composable
fun SleepScreen(
    vm: SleepViewModel = viewModel(),
    onBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val history by vm.history.collectAsState()
    val hc by vm.healthConnectUiState.collectAsState()

    val permissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { granted ->
        vm.onHealthConnectPermissionResult(granted)
    }

    LaunchedEffect(Unit) {
        vm.refreshHealthConnectUi()
    }

    val today = remember(history) {
        history.find { it.date == java.time.LocalDate.now() }
    }
    val prevDay = remember(history) { history.dropLast(1).lastOrNull() }

    val weeklyData = remember(history) { vm.getWeeklyTrend(history) }

    var showHistory by remember { mutableStateOf(false) }

    val onHealthConnectClick: () -> Unit = {
        when {
            hc.isSyncing -> Unit
            hc.sdkStatus == HealthConnectClient.SDK_AVAILABLE && hc.hasAllPermissions ->
                vm.syncHealthConnect()
            hc.sdkStatus == HealthConnectClient.SDK_AVAILABLE ->
                permissionLauncher.launch(vm.getHealthConnectPermissionStrings())
            hc.sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                val market =
                    "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
                val web =
                    "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(market)))
                } catch (_: Exception) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(web)))
                }
                Toast.makeText(
                    context,
                    context.getString(R.string.sleep_hc_update),
                    Toast.LENGTH_LONG
                ).show()
            }
            else -> Toast.makeText(
                context,
                context.getString(R.string.sleep_hc_not_supported),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    AnimatedContent(
        targetState = showHistory,
        transitionSpec = {
            val enter = fadeIn(tween(220)) +
                    slideInHorizontally(tween(220)) { if (targetState) it / 12 else -it / 12 }
            val exit = fadeOut(tween(180))
            enter.togetherWith(exit)
        },
        label = "nav"
    ) { isHistory ->
        if (isHistory) {
            HistoryScreen(
                history = history,
                onBack = { showHistory = false },
                onDelete = { vm.deleteSleepSession(it) },
                onDeleteAllForDay = { vm.deleteAllSessionsForDate(it) }
            )
        } else {
            if (today != null) {
                DashboardScreen(
                    today = today,
                    prevDay = prevDay,
                    weekHistory = weeklyData,
                    onBack = onBack,
                    onOpenHistory = { showHistory = true },
                    onManualSave = { vm.saveManualEntry(it) },
                    healthConnectState = hc,
                    onHealthConnectClick = onHealthConnectClick
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}
