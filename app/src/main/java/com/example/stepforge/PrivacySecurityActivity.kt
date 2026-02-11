package com.example.stepforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.stepforge.settings.PrivacySecurityScreen
import com.example.stepforge.ui.stepforgeTheme
import com.example.stepforge.ui.rememberUseDarkTheme


class PrivacySecurityActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                PrivacySecurityScreen(onBack = { finish() })
            }
        }
    }
}