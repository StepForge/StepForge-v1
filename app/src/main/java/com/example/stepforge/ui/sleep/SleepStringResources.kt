package com.example.stepforge.ui.sleep

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.example.stepforge.R
import com.example.stepforge.ui.sleep.alarm.AlarmBackgroundPresets
import com.example.stepforge.ui.sleep.alarm.AlarmClockStyles
import com.example.stepforge.ui.sleep.model.StageType
import com.example.stepforge.ui.sleep.model.TrackingMode

@StringRes
fun scoreLabelRes(score: Int): Int = when {
    score >= 85 -> R.string.sleep_score_excellent
    score >= 70 -> R.string.sleep_score_good
    score >= 55 -> R.string.sleep_score_fair
    else -> R.string.sleep_score_poor
}

@Composable
fun scoreLabel(score: Int): String = stringResource(scoreLabelRes(score))

@StringRes
fun qualitySliderLabelRes(rating: Int): Int = when (rating) {
    1 -> R.string.sleep_score_poor
    2 -> R.string.sleep_score_fair
    3 -> R.string.sleep_score_good
    4 -> R.string.sleep_quality_great
    else -> R.string.sleep_score_excellent
}

@Composable
fun qualitySliderLabel(rating: Int): String = stringResource(qualitySliderLabelRes(rating))

@StringRes
fun StageType.labelRes(): Int = when (this) {
    StageType.DEEP -> R.string.sleep_stage_deep
    StageType.REM -> R.string.sleep_stage_rem
    StageType.LIGHT -> R.string.sleep_stage_light
    StageType.AWAKE -> R.string.sleep_stage_awake
}

@Composable
fun StageType.label(): String = stringResource(labelRes())

@StringRes
fun TrackingMode.labelRes(): Int = when (this) {
    TrackingMode.SENSOR -> R.string.sleep_health_connect
    TrackingMode.PHONE -> R.string.sleep_tracking_mode_phone
    TrackingMode.MANUAL -> R.string.sleep_tracking_mode_manual
}

@Composable
fun TrackingMode.label(): String = stringResource(labelRes())

@StringRes
fun AlarmClockStyles.styleTitleRes(styleId: String): Int = when (styleId) {
    AlarmClockStyles.STYLE_COMPACT -> R.string.sleep_clock_compact
    AlarmClockStyles.STYLE_CLASSIC -> R.string.sleep_clock_classic
    AlarmClockStyles.STYLE_NUMERAL -> R.string.sleep_clock_numeral
    else -> R.string.sleep_clock_stacked
}

fun AlarmClockStyles.titleFor(context: Context, styleId: String): String {
    return context.getString(styleTitleRes(styleId))
}

@Composable
fun AlarmClockStyles.titleFor(styleId: String): String {
    return stringResource(styleTitleRes(styleId))
}

@StringRes
fun AlarmBackgroundPresets.presetTitleRes(presetId: String): Int = when (presetId) {
    "dawn" -> R.string.sleep_bg_dawn
    "midnight" -> R.string.sleep_bg_midnight
    "forest" -> R.string.sleep_bg_forest
    "ocean" -> R.string.sleep_bg_ocean
    "ember" -> R.string.sleep_bg_ember
    else -> R.string.sleep_bg_aurora
}

fun AlarmBackgroundPresets.titleFor(context: Context, presetId: String): String {
    return context.getString(presetTitleRes(presetId))
}

@Composable
fun AlarmBackgroundPresets.titleFor(presetId: String): String {
    return stringResource(presetTitleRes(presetId))
}

fun AlarmBackgroundPresets.Preset.localizedTitle(context: Context): String {
    return context.getString(titleRes)
}

@Composable
fun AlarmBackgroundPresets.Preset.localizedTitle(): String {
    return stringResource(titleRes)
}
