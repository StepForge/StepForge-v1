package com.example.stepforge.core

import android.app.LocaleManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import com.example.stepforge.StepCounterService
import com.example.stepforge.widget.StepWidgetCompactProvider
import com.example.stepforge.widget.StepWidgetLargeProvider
import com.example.stepforge.widget.StepWidgetProvider
import java.util.Locale

object AppLanguageHelper {

    private const val PREFS_NAME = "stepforge_app_language"
    private const val KEY_LANGUAGE_TAGS = "language_tags"
    private const val SYSTEM_LANGUAGE = "system"

    fun initialize(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_LANGUAGE_TAGS)) {
            val existingTags = platformLanguageTags(context)
                .ifBlank { AppCompatDelegate.getApplicationLocales().toLanguageTags() }
            if (existingTags.isNotBlank()) {
                prefs.edit().putString(KEY_LANGUAGE_TAGS, existingTags).commit()
            }
        }

        val storedTags = prefs.getString(KEY_LANGUAGE_TAGS, null) ?: return
        val locales = if (storedTags == SYSTEM_LANGUAGE) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(storedTags)
        }
        if (AppCompatDelegate.getApplicationLocales().toLanguageTags() != locales.toLanguageTags()) {
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    fun applyLanguage(context: Context, tags: String?) {
        val storedTags = tags?.takeIf { it.isNotBlank() } ?: SYSTEM_LANGUAGE
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_LANGUAGE_TAGS, storedTags)
            .commit()

        val locales = if (storedTags == SYSTEM_LANGUAGE) {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(storedTags)
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }

    fun selectedLanguageTags(context: Context): String {
        val storedTags = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LANGUAGE_TAGS, null)
        if (storedTags != null) {
            return if (storedTags == SYSTEM_LANGUAGE) "" else storedTags
        }

        return platformLanguageTags(context)
            .ifBlank { AppCompatDelegate.getApplicationLocales().toLanguageTags() }
    }

    fun localizedContext(context: Context): Context {
        val tags = selectedLanguageTags(context)
        if (tags.isBlank()) return context

        val configuration = Configuration(context.resources.configuration).apply {
            setLocales(LocaleList.forLanguageTags(tags))
        }
        return context.createConfigurationContext(configuration)
    }

    fun selectedLocale(context: Context): Locale {
        return localizedContext(context).resources.configuration.locales[0] ?: Locale.getDefault()
    }

    fun refreshRuntimeSurfaces(context: Context) {
        val appContext = context.applicationContext
        runCatching {
            ContextCompat.startForegroundService(
                appContext,
                Intent(appContext, StepCounterService::class.java).apply {
                    action = StepCounterService.ACTION_RELOAD
                }
            )
        }
        StepWidgetProvider.notifyRefresh(appContext)
        StepWidgetCompactProvider.notifyRefresh(appContext)
        StepWidgetLargeProvider.notifyRefresh(appContext)
    }

    private fun platformLanguageTags(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return ""
        return context.getSystemService(LocaleManager::class.java)
            ?.applicationLocales
            ?.toLanguageTags()
            .orEmpty()
    }
}
