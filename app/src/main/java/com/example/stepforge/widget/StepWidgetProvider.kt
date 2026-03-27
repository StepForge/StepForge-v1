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
import android.os.Bundle
import android.util.Log
import android.util.SizeF
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

class StepWidgetProvider : AppWidgetProvider() {

    private fun dotFormatter(): DecimalFormat {
        val symbols = DecimalFormatSymbols(Locale.getDefault()).apply { groupingSeparator = '.' }
        return DecimalFormat("#,###", symbols)
    }

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> updateWidgetFromStore(context, manager, id) }
        requestFreshDataFromService(context)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        manager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        updateWidgetFromStore(context, manager, appWidgetId)
    }

    private fun requestFreshDataFromService(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) return
        try {
            val intent = Intent(context, StepCounterService::class.java).apply {
                action = "com.example.stepforge.ACTION_WIDGET_REFRESH_REQUEST"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.startForegroundService(intent)
            else context.startService(intent)
        } catch (e: Exception) {
            Log.e("StepWidgetProvider", "requestFreshDataFromService failed: ${e.message}")
        }
    }

    private fun chooseLayout(manager: AppWidgetManager, widgetId: Int): Int {
        val options = manager.getAppWidgetOptions(widgetId)
        val minH = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)
        val minW = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)

        return when {
            // Yükseklik 110dp üzerindeyse Large (4x2)
            minH >= 110 -> R.layout.widget_stepforge_large

            // Genişlik 250dp üzerindeyse Medium (4x1)
            minW >= 250 -> R.layout.widget_stepforge

            // Diğer her durumda Compact (2x1)
            else -> R.layout.widget_stepforge_compact
        }
    }

    private fun updateWidgetFromStore(context: Context, manager: AppWidgetManager, widgetId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.stepforgeStore.data.first()
            val stepsKey = intPreferencesKey("persisted_total_sum")
            val goalKey = intPreferencesKey("step_goal")
            val themeKey = stringPreferencesKey("theme_mode")

            val steps = prefs[stepsKey] ?: 0
            val goal = prefs[goalKey] ?: 10_000
            val themeMode = prefs[themeKey] ?: "system"

            updateRemoteViews(context, manager, widgetId, steps, goal, themeMode)
        }
    }

    private fun updateAllWidgetsFromService(context: Context, manager: AppWidgetManager, steps: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val prefs = context.stepforgeStore.data.first()
            val goalKey = intPreferencesKey("step_goal")
            val themeKey = stringPreferencesKey("theme_mode")

            val goal = prefs[goalKey] ?: 10_000
            val themeMode = prefs[themeKey] ?: "system"

            val ids = manager.getAppWidgetIds(ComponentName(context, StepWidgetProvider::class.java))
            ids.forEach { widgetId ->
                updateRemoteViews(context, manager, widgetId, steps, goal, themeMode)
            }
        }
    }

    private fun isDark(context: Context, themeMode: String): Boolean {
        return when (themeMode) {
            "dark" -> true
            "light" -> false
            else -> {
                val mode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                mode == Configuration.UI_MODE_NIGHT_YES
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
        val dark = isDark(context, themeMode)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // ✅ Android 12+ için tüm geçiş yolları (Size Mapping)
            val viewMapping = mapOf(
                // Küçük boyutlarda Compact'a düş
                SizeF(50f, 60f) to getPopulatedView(context, R.layout.widget_stepforge_compact, steps, goal, dark),
                // Genişlediğinde Medium'a geç
                SizeF(250f, 60f) to getPopulatedView(context, R.layout.widget_stepforge, steps, goal, dark),
                // Uzadığında Large'a geç
                SizeF(250f, 110f) to getPopulatedView(context, R.layout.widget_stepforge_large, steps, goal, dark)
            )
            manager.updateAppWidget(widgetId, RemoteViews(viewMapping))
        } else {
            // ✅ Android 11 ve altı için manuel seçim
            val layoutId = chooseLayout(manager, widgetId)
            manager.updateAppWidget(widgetId, getPopulatedView(context, layoutId, steps, goal, dark))
        }
    }

    private fun getPopulatedView(
        context: Context,
        layoutId: Int,
        steps: Int,
        goal: Int,
        dark: Boolean
    ): RemoteViews {
        val rv = RemoteViews(context.packageName, layoutId)
        val formatter = dotFormatter()
        val stepsText = formatter.format(steps)
        val safeGoal = goal.coerceAtLeast(1)
        val percent = (steps.toFloat() / safeGoal * 100f).coerceIn(0f, 100f).toInt()
        val openIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        when (layoutId) {
            R.layout.widget_stepforge_compact -> {
                rv.setTextViewText(R.id.textStepsCompact, stepsText)
                rv.setTextViewText(R.id.textLabelCompact, context.getString(R.string.widget_steps_label))
                rv.setProgressBar(R.id.progressBarCompact, 100, percent, false)
                applyTheme(
                    rv,
                    dark,
                    true,
                    R.id.rootWidgetCompactInner,
                    R.id.textStepsCompact,
                    R.id.textLabelCompact,
                    R.id.progressBarCompact
                )
                rv.setOnClickPendingIntent(R.id.rootWidgetCompact, openIntent)
            }

            R.layout.widget_stepforge -> {
                rv.setTextViewText(R.id.textSteps, stepsText)
                rv.setTextViewText(
                    R.id.textGoal,
                    context.getString(R.string.widget_goal_format, formatter.format(goal))
                )
                rv.setProgressBar(R.id.progressBar, 100, percent, false)
                applyTheme(
                    rv,
                    dark,
                    false,
                    R.id.rootWidgetInner,
                    R.id.textSteps,
                    R.id.textGoal,
                    R.id.progressBar
                )
                rv.setOnClickPendingIntent(R.id.rootWidget, openIntent)
            }

            R.layout.widget_stepforge_large -> {
                val dist = (steps * 0.762) / 1000.0
                rv.setTextViewText(R.id.textTitleLarge, context.getString(R.string.widget_today))
                rv.setTextViewText(R.id.textStepsLarge, stepsText)
                rv.setTextViewText(
                    R.id.textGoalLarge,
                    context.getString(R.string.widget_goal_of_format, formatter.format(goal))
                )
                rv.setProgressBar(R.id.progressBarLarge, 100, percent, false)
                rv.setTextViewText(
                    R.id.textPercentLarge,
                    context.getString(R.string.widget_percent_of_goal_format, percent)
                )
                rv.setTextViewText(
                    R.id.textDistanceLarge,
                    context.getString(
                        R.string.widget_distance_approx_km_format,
                        String.format(Locale.getDefault(), "%.1f", dist)
                    )
                )
                applyThemeLarge(rv, dark)
                rv.setOnClickPendingIntent(R.id.rootWidgetLarge, openIntent)
            }
        }
        return rv
    }

    private fun applyTheme(rv: RemoteViews, dark: Boolean, isCompact: Boolean, root: Int, steps: Int, label: Int, progress: Int) {
        val bg = if (dark) R.drawable.widget_compact_bg else R.drawable.widget_compact_bg_light
        rv.setInt(root, "setBackgroundResource", bg)
        rv.setTextColor(steps, if (dark) 0xFFFFFFFF.toInt() else 0xFF0F172A.toInt())
        rv.setTextColor(label, if (dark) 0xFFB0BEC5.toInt() else if (isCompact) 0xFF6B7280.toInt() else 0xFF64748B.toInt())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setColorStateList(progress, "setProgressBackgroundTintList", ColorStateList.valueOf(if (dark) 0xFF303035.toInt() else 0xFFE5E7EB.toInt()))
        }
    }

    private fun applyThemeLarge(rv: RemoteViews, dark: Boolean) {
        val bg = if (dark) R.drawable.widget_compact_bg else R.drawable.widget_compact_bg_light
        rv.setInt(R.id.cardLarge, "setBackgroundResource", bg)
        rv.setTextColor(R.id.textStepsLarge, if (dark) 0xFFFFFFFF.toInt() else 0xFF0F172A.toInt())
        rv.setTextColor(R.id.textPercentLarge, if (dark) 0xFF00F5FF.toInt() else 0xFF0EA5E9.toInt())
        rv.setTextColor(R.id.textDistanceLarge, if (dark) 0x80FFFFFF.toInt() else 0xFF64748B.toInt())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            rv.setColorStateList(R.id.progressBarLarge, "setProgressBackgroundTintList", ColorStateList.valueOf(if (dark) 0xFF303035.toInt() else 0xFFE5E7EB.toInt()))
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        val manager = AppWidgetManager.getInstance(context)

        when (intent.action) {
            Intent.ACTION_CONFIGURATION_CHANGED,
            AppWidgetManager.ACTION_APPWIDGET_UPDATE,
            ACTION_APPWIDGET_UPDATE_OPTIONS,
            ACTION_REFRESH -> {
                val ids = manager.getAppWidgetIds(ComponentName(context, StepWidgetProvider::class.java))
                ids.forEach { id -> updateWidgetFromStore(context, manager, id) }

                if (intent.action == ACTION_REFRESH) requestFreshDataFromService(context)
            }

            ACTION_UPDATE_STEPS -> {
                val steps = intent.getIntExtra("steps", 0)
                updateAllWidgetsFromService(context, manager, steps)
            }
        }
    }

    companion object {
        private const val ACTION_APPWIDGET_UPDATE_OPTIONS = "android.appwidget.action.APPWIDGET_UPDATE_OPTIONS"
        const val ACTION_REFRESH = "com.example.stepforge.ACTION_REFRESH_WIDGET"
        const val ACTION_UPDATE_STEPS = "com.example.stepforge.WIDGET_UPDATE"

        fun notifyRefresh(context: Context) {
            context.sendBroadcast(Intent(context, StepWidgetProvider::class.java).apply { action = ACTION_REFRESH })
        }

        fun sendStepsUpdate(context: Context, steps: Int) {
            context.sendBroadcast(
                Intent(context, StepWidgetProvider::class.java).apply {
                    action = ACTION_UPDATE_STEPS
                    putExtra("steps", steps)
                }
            )
        }
    }
}
