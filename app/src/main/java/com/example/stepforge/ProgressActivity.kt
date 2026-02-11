package com.example.stepforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.stepforge.settings.ProgressScreen
import com.example.stepforge.ui.rememberUseDarkTheme
import com.example.stepforge.ui.stepforgeTheme

class ProgressActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                ProgressScreen(
                    onBack = { finish() }
                )
            }
        }
    }
}
