package com.example.stepforge.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Sistem teması (dark/light) değiştiğinde widget'ları anında yeniler.
 * Sadece theme_mode = "system" ise tetikler.
 */
class WidgetThemeChangeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_CONFIGURATION_CHANGED) return

        val pending = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val themeKey = stringPreferencesKey("theme_mode")
                val prefs = context.stepforgeStore.data.first()
                val mode = prefs[themeKey] ?: "system"

                if (mode == "system") {
                    StepWidgetProvider.notifyRefresh(context)
                    StepWidgetCompactProvider.notifyRefresh(context)
                    StepWidgetLargeProvider.notifyRefresh(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}