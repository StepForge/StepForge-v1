package com.example.stepforge

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.lifecycle.lifecycleScope
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.onboarding.OnboardingScreen
import com.example.stepforge.ui.stepforgeTheme
import com.example.stepforge.ui.rememberUseDarkTheme
import kotlinx.coroutines.launch

class OnboardingActivity : ComponentActivity() {

    private val KEY_ONBOARDING_DONE =
        intPreferencesKey("onboarding_done")

    private val permissionsLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            goToApp()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {

                val requestNow = remember { mutableStateOf(false) }

                OnboardingScreen(
                    onSkip = { goToApp() },
                    onFinishAndRequestPermissions = { requestNow.value = true }
                )

                if (requestNow.value) {
                    requestNow.value = false
                    requestPermissions()
                }
            }
        }
    }

    private fun requestPermissions() {
        val perms = buildList {
            add(Manifest.permission.ACTIVITY_RECOGNITION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }.toTypedArray()

        permissionsLauncher.launch(perms)
    }

    private fun goToApp() {
        lifecycleScope.launch {
            applicationContext.stepforgeStore.edit {
                it[KEY_ONBOARDING_DONE] = 1
            }
            startActivity(Intent(this@OnboardingActivity, MainActivity::class.java))
            finish()
        }
    }
}
