package com.example.stepforge.ui.sleep.alarm

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.stepforge.ui.rememberUseDarkTheme
import com.example.stepforge.ui.stepforgeTheme

class SleepReminderActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {

                SleepReminderScreen(
                    initialReminder = AlarmStore.getForEditing(this),
                    onDismiss = { finish() },
                    onSave = {
                        finish()
                    },
                    onTurnOff = {
                        AlarmCore.cancel(this)
                    }
                )

            }
        }
    }
}
