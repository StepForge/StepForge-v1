package com.example.stepforge.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.MainActivity
import com.example.stepforge.R
import com.example.stepforge.StepCounterService
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class StepWidgetLargeProvider : AppWidgetProvider() {

    private fun dotFormatter(): DecimalFormat {
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply {
            groupingSeparator = '.'
        }
        return DecimalFormat("#,###", symbols)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateWidgetFromStore(context, manager, id) }
        requestFreshDataFromService(context)
    }

    private fun requestFreshDataFromService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d("StepWidgetLarge", "Skipping FGS request on Android 12+")
            return
        }
        try {
            val intent = Intent(context, StepCounterService::class.java)
            intent.action = "com.example.stepforge.ACTION_WIDGET_REFRESH_REQUEST"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("StepWidgetLarge", "Servisten veri istenemedi: ${e.message}")
        }
    }

    private fun updateWidgetFromStore(context: Context, manager: AppWidgetManager, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.stepforgeStore.data.first()
            val stepsKey = intPreferencesKey("persisted_total_sum")
            val goalKey = intPreferencesKey("step_goal")
            val themeKey = stringPreferencesKey("theme_mode")

            val totalSteps = prefs[stepsKey] ?: 0
            val goal = prefs[goalKey] ?: 10_000
            val themeMode = prefs[themeKey] ?: "system"

            updateRemoteViews(context, manager, widgetId, totalSteps, goal, themeMode)
        }
    }

    private fun updateAllWidgetsFromService(context: Context, manager: AppWidgetManager, steps: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.stepforgeStore.data.first()
            val goalKey = intPreferencesKey("step_goal")
            val themeKey = stringPreferencesKey("theme_mode")

            val goal = prefs[goalKey] ?: 10_000
            val themeMode = prefs[themeKey] ?: "system"

            val ids = manager.getAppWidgetIds(ComponentName(context, StepWidgetLargeProvider::class.java))
            ids.forEach { widgetId ->
                updateRemoteViews(context, manager, widgetId, steps, goal, themeMode)
            }
        }
    }

    private fun updateRemoteViews(
        context: Context,
        manager: AppWidgetManager,
        widgetId: Int,
        steps: Int,
        goal: Int,
        themeMode: String
    ) {
        val safeGoal = goal.coerceAtLeast(1)
        val percentRaw = (steps.toFloat() / safeGoal * 100f)
        val percentClamped = percentRaw.coerceIn(0f, 100f)
        val distanceKm = (steps * 0.762) / 1000.0

        val formatter = dotFormatter()
        val stepsText = formatter.format(steps)
        val goalText = "of ${formatter.format(goal)}"
        val percentText = "${percentRaw.toInt()}% of goal"
        val distanceText = "~${String.format(Locale.getDefault(), "%.1f", distanceKm)} km"

        val isDark = when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> {
                val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                mode == Configuration.UI_MODE_NIGHT_YES
            }
        }

        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val rv = RemoteViews(context.packageName, R.layout.widget_stepforge_large).apply {
            setTextViewText(R.id.textStepsLarge, stepsText)
            setTextViewText(R.id.textGoalLarge, goalText)
            setProgressBar(R.id.progressBarLarge, 100, percentClamped.toInt(), false)
            setTextViewText(R.id.textPercentLarge, percentText)
            setTextViewText(R.id.textDistanceLarge, distanceText)

            if (isDark) {
                setInt(R.id.cardLarge, "setBackgroundResource", R.drawable.widget_compact_bg)
                setTextColor(R.id.textStepsLarge, 0xFFFFFFFF.toInt())
                setTextColor(R.id.textGoalLarge, 0xFFB0BEC5.toInt())
                setTextColor(R.id.textTitleLarge, 0xFFB0BEC5.toInt())
                setTextColor(R.id.textPercentLarge, 0xFF00F5FF.toInt())
                setTextColor(R.id.textDistanceLarge, 0x80FFFFFF.toInt())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setColorStateList(
                        R.id.progressBarLarge,
                        "setProgressBackgroundTintList",
                        ColorStateList.valueOf(0xFF303035.toInt())
                    )
                }
            } else {
                setInt(R.id.cardLarge, "setBackgroundResource", R.drawable.widget_compact_bg_light)
                setTextColor(R.id.textStepsLarge, 0xFF0F172A.toInt())
                setTextColor(R.id.textGoalLarge, 0xFF64748B.toInt())
                setTextColor(R.id.textTitleLarge, 0xFF64748B.toInt())
                setTextColor(R.id.textPercentLarge, 0xFF0EA5E9.toInt())
                setTextColor(R.id.textDistanceLarge, 0xFF64748B.toInt())

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    setColorStateList(
                        R.id.progressBarLarge,
                        "setProgressBackgroundTintList",
                        ColorStateList.valueOf(0xFFE5E7EB.toInt())
                    )
                }
            }

            setOnClickPendingIntent(R.id.rootWidgetLarge, openIntent)
        }

        manager.updateAppWidget(widgetId, rv)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = AppWidgetManager.getInstance(context)

        Log.d("StepWidgetLarge", "onReceive: ${intent.action}")

        when (intent.action) {
            Intent.ACTION_CONFIGURATION_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            ACTION_APPWIDGET_UPDATE_OPTIONS,
            ACTION_REFRESH -> {
                val ids = manager.getAppWidgetIds(ComponentName(context, StepWidgetLargeProvider::class.java))
                ids.forEach { id -> updateWidgetFromStore(context, manager, id) }

                if (intent.action == ACTION_REFRESH) {
                    requestFreshDataFromService(context)
                }
            }

            ACTION_UPDATE_STEPS -> {
                val steps = intent.getIntExtra("steps", 0)
                updateAllWidgetsFromService(context, manager, steps)
            }
        }
    }

    companion object {
        private const val ACTION_APPWIDGET_UPDATE_OPTIONS =
            "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS"

        const val ACTION_REFRESH = "com.example.stepforge.ACTION_REFRESH_WIDGET_LARGE"
        const val ACTION_UPDATE_STEPS = "com.example.stepforge.WIDGET_UPDATE_LARGE"

        fun notifyRefresh(context: Context) {
            val intent = Intent(context, StepWidgetLargeProvider::class.java).apply {
                action = ACTION_REFRESH
            }
            context.sendBroadcast(intent)
        }

        fun sendStepsUpdate(context: Context, steps: Int) {
            val intent = Intent(context, StepWidgetLargeProvider::class.java).apply {
                action = ACTION_UPDATE_STEPS
                putExtra("steps", steps)
            }
            context.sendBroadcast(intent)
        }
    }
}