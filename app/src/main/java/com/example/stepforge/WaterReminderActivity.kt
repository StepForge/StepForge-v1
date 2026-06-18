package com.example.stepforge

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.stepforge.ui.water.WaterReminderScreen
import com.example.stepforge.ui.stepforgeTheme
import com.example.stepforge.ui.rememberUseDarkTheme

class WaterReminderActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                WaterReminderScreen(onBack = ::closeWaterReminder)
            }
        }
    }

    private fun closeWaterReminder() {
        if (isTaskRoot) {
            startActivity(
                Intent(this, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }
            )
        }
        finish()
    }
}
