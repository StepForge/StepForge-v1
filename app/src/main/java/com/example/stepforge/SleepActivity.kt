package com.example.stepforge

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import com.example.stepforge.settings.SleepScreen
import com.example.stepforge.ui.components.SleepSyncManager
import com.example.stepforge.ui.stepforgeTheme // Paket yolunu projenize göre kontrol edin
import com.example.stepforge.ui.rememberUseDarkTheme

class SleepActivity : ComponentActivity() {

    private lateinit var sleepSyncManager: SleepSyncManager
    private lateinit var permissionLauncher: ActivityResultLauncher<Set<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sleepSyncManager = SleepSyncManager(this)

        permissionLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract()
        ) { granted ->
            val required = sleepSyncManager.getPermissionStrings()
            val ok = granted.containsAll(required)
            if (ok) {
                Toast.makeText(this, "Health Connect sleep permission granted ✅", Toast.LENGTH_SHORT).show()
            } else {
                Log.w("SleepActivity", "Missing: ${required - granted}")
                Toast.makeText(this, "Sleep permission missing ❌", Toast.LENGTH_SHORT).show()
            }
        }

        setContent {
            // rememberUseDarkTheme parametresini projenize göre ayarlayın (context veya activity gerekebilir)
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                SleepScreen(
                    onBack = { finish() },
                    onLaunchHealthConnectPermissions = { launchSleepPermissions() }
                )
            }
        }
    }

    private fun launchSleepPermissions() {
        val availability = HealthConnectClient.getSdkStatus(this)

        when (availability) {
            HealthConnectClient.SDK_AVAILABLE -> {
                // Her şey yolunda, izinleri iste
                permissionLauncher.launch(sleepSyncManager.getPermissionStrings())
            }
            HealthConnectClient.SDK_UNAVAILABLE -> {
                Toast.makeText(this, "Health Connect is not supported on this device ❌", Toast.LENGTH_LONG).show()
            }
            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> {
                // Android 13 ve altı için: Uygulama yüklü değil veya güncel değil
                val uriString = "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(uriString)))
                } catch (e: Exception) {
                    // Eğer Play Store linki hata verirse standart web linkine yönlendir
                    val webUri = "https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata"
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(webUri)))
                }
                Toast.makeText(this, "Please install or update Health Connect ⚠️", Toast.LENGTH_LONG).show()
            }
        }
    }
}
