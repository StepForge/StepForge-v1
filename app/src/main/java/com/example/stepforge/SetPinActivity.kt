package com.example.stepforge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.stepforge.settings.SetPinScreen
import com.example.stepforge.ui.stepforgeTheme

class SetPinActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            stepforgeTheme(darkTheme = true) {
                SetPinScreen(
                    onClose = { finish() },
                    onPinSaved = { finish() } // kaydedince kapatıyoruz
                )
            }
        }
    }
}