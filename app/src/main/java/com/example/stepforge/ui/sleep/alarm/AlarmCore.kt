package com.example.stepforge.ui.sleep.alarm

import android.app.ActivityOptions
import android.app.AlarmManager
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.text.format.DateFormat
import android.view.View
import android.widget.RemoteViews
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.stepforge.R as AppR

import java.text.Normalizer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

data class SleepReminder(
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val repeatDaily: Boolean = true,
    val silent: Boolean = false,
    val vibrate: Boolean = true,
    val voiceCommandsEnabled: Boolean = false,
    val vibrationStyle: String = "normal",
    val soundUriString: String? = null,
    val soundTitle: String? = null,
    val backgroundUriString: String? = null,
    val backgroundTitle: String? = null,
    val message: String = "",
    val clockStyle: String = AlarmClockStyles.STYLE_STACKED,
    val ringtoneUri: String? = null,
    val backgroundUri: String? = null
) {
    val effectiveSoundUri: String?
        get() = soundUriString ?: ringtoneUri

    val effectiveBackgroundUri: String?
        get() = backgroundUriString ?: backgroundUri

    val formattedTime: String
        get() = "%02d:%02d".format(Locale.getDefault(), hour, minute)
}

object SleepReminderStore {

    private const val PREFS_NAME = "sleep_reminder_prefs"
    const val DEFAULT_DISABLED_HOUR = 11
    const val DEFAULT_DISABLED_MINUTE = 0

    private const val KEY_HOUR = "hour"
    private const val KEY_MINUTE = "minute"
    private const val KEY_ENABLED = "enabled"
    private const val KEY_REPEAT = "repeat_daily"
    private const val KEY_SILENT = "silent"
    private const val KEY_VIBRATE = "vibrate"
    private const val KEY_VOICE_COMMANDS = "voice_commands"
    private const val KEY_VIBRATION_STYLE = "vibration_style"
    private const val KEY_RINGTONE = "ringtone_uri"
    private const val KEY_RINGTONE_TITLE = "ringtone_title"
    private const val KEY_BACKGROUND = "background_uri"
    private const val KEY_BACKGROUND_TITLE = "background_title"
    private const val KEY_MESSAGE = "message"
    private const val KEY_CLOCK_STYLE = "clock_style"

    fun save(context: Context, reminder: SleepReminder) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_HOUR, reminder.hour)
            .putInt(KEY_MINUTE, reminder.minute)
            .putBoolean(KEY_ENABLED, reminder.enabled)
            .putBoolean(KEY_REPEAT, reminder.repeatDaily)
            .putBoolean(KEY_SILENT, reminder.silent)
            .putBoolean(KEY_VIBRATE, reminder.vibrate)
            .putBoolean(KEY_VOICE_COMMANDS, reminder.voiceCommandsEnabled)
            .putString(KEY_VIBRATION_STYLE, reminder.vibrationStyle)
            .putString(KEY_RINGTONE, reminder.effectiveSoundUri)
            .putString(KEY_RINGTONE_TITLE, reminder.soundTitle)
            .putString(KEY_BACKGROUND, reminder.effectiveBackgroundUri)
            .putString(KEY_BACKGROUND_TITLE, reminder.backgroundTitle)
            .putString(KEY_MESSAGE, reminder.message)
            .putString(KEY_CLOCK_STYLE, reminder.clockStyle)
            .apply()
    }

    fun get(context: Context): SleepReminder? {
        return read(context, includeDisabled = false)
    }

    fun getForEditing(context: Context): SleepReminder? {
        return read(context, includeDisabled = true)
    }

    private fun read(context: Context, includeDisabled: Boolean): SleepReminder? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (!prefs.contains(KEY_HOUR)) return null
        val enabled = prefs.getBoolean(KEY_ENABLED, true)
        if (!enabled && !includeDisabled) return null

        val soundUri = prefs.getString(KEY_RINGTONE, null)
        val backgroundUri = prefs.getString(KEY_BACKGROUND, null)

        return SleepReminder(
            hour = prefs.getInt(KEY_HOUR, DEFAULT_DISABLED_HOUR),
            minute = prefs.getInt(KEY_MINUTE, 0),
            enabled = enabled,
            repeatDaily = prefs.getBoolean(KEY_REPEAT, true),
            silent = prefs.getBoolean(KEY_SILENT, false),
            vibrate = prefs.getBoolean(KEY_VIBRATE, true),
            voiceCommandsEnabled = prefs.getBoolean(KEY_VOICE_COMMANDS, false),
            vibrationStyle = prefs.getString(
                KEY_VIBRATION_STYLE,
                "normal"
            ) ?: "normal",
            soundUriString = soundUri,
            soundTitle = prefs.getString(KEY_RINGTONE_TITLE, null) ?: context.getString(AppR.string.sleep_reminder_system_alarm),
            backgroundUriString = backgroundUri,
            backgroundTitle = prefs.getString(KEY_BACKGROUND_TITLE, null) ?: context.getString(AppR.string.sleep_reminder_theme_background),
            message = prefs.getString(KEY_MESSAGE, null) ?: context.getString(AppR.string.sleep_reminder_time_to_wind_down),
            clockStyle = prefs.getString(KEY_CLOCK_STYLE, null) ?: AlarmClockStyles.STYLE_STACKED,
            ringtoneUri = soundUri,
            backgroundUri = backgroundUri
        )
    }

    fun clear(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .apply()
    }

    fun disableReminder(context: Context) {
        val current = read(context, includeDisabled = true)
            ?: SleepReminder(hour = DEFAULT_DISABLED_HOUR, minute = DEFAULT_DISABLED_MINUTE)
        save(
            context,
            current.copy(
                hour = DEFAULT_DISABLED_HOUR,
                minute = DEFAULT_DISABLED_MINUTE,
                enabled = false
            )
        )
    }

    fun nextTriggerMillis(
        reminder: SleepReminder,
        fromMillis: Long = System.currentTimeMillis()
    ): Long {
        val now = Calendar.getInstance().apply { timeInMillis = fromMillis }

        val target = Calendar.getInstance().apply {
            timeInMillis = fromMillis
            set(Calendar.HOUR_OF_DAY, reminder.hour)
            set(Calendar.MINUTE, reminder.minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (!target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }

        return target.timeInMillis
    }
}

object AlarmBackgroundPresets {
    const val PREFIX = "stepforge-preset://"

    data class Preset(
        val id: String,
        @StringRes val titleRes: Int
    ) {
        val uriString: String = "$PREFIX$id"
    }

    val all = listOf(
        Preset("aurora", AppR.string.sleep_bg_aurora),
        Preset("dawn", AppR.string.sleep_bg_dawn),
        Preset("midnight", AppR.string.sleep_bg_midnight),
        Preset("forest", AppR.string.sleep_bg_forest),
        Preset("ocean", AppR.string.sleep_bg_ocean),
        Preset("ember", AppR.string.sleep_bg_ember)
    )

    fun random(): Preset = all[Random.nextInt(all.size)]

    fun find(uriString: String?): Preset? {
        if (uriString.isNullOrBlank() || !uriString.startsWith(PREFIX)) return null
        val id = uriString.removePrefix(PREFIX)
        return all.firstOrNull { it.id == id }
    }

    fun withFallback(context: Context, reminder: SleepReminder): SleepReminder {
        if (!reminder.effectiveBackgroundUri.isNullOrBlank()) return reminder
        val preset = random()
        return reminder.copy(
            backgroundUriString = preset.uriString,
            backgroundTitle = context.getString(preset.titleRes),
            backgroundUri = preset.uriString
        )
    }
}

object AlarmClockStyles {
    const val STYLE_STACKED = "stacked"
    const val STYLE_COMPACT = "compact"
    const val STYLE_CLASSIC = "classic"
    const val STYLE_NUMERAL = "numeral"

    data class Style(
        val id: String,
        @StringRes val titleRes: Int
    )

    val all = listOf(
        Style(STYLE_STACKED, AppR.string.sleep_clock_stacked),
        Style(STYLE_COMPACT, AppR.string.sleep_clock_compact),
        Style(STYLE_CLASSIC, AppR.string.sleep_clock_classic),
        Style(STYLE_NUMERAL, AppR.string.sleep_clock_numeral)
    )

    fun titleFor(context: Context, id: String): String {
        val titleRes = all.firstOrNull { it.id == id }?.titleRes ?: AppR.string.sleep_clock_stacked
        return context.getString(titleRes)
    }
}

object AlarmPermissionHelper {
    fun canUseFullScreenIntent(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE ||
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .canUseFullScreenIntent()
    }

    fun fullScreenIntentSettingsIntent(context: Context): Intent {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }
    }
}

object AlarmStore {
    fun save(context: Context, reminder: SleepReminder) = SleepReminderStore.save(context, reminder)
    fun get(context: Context): SleepReminder? = SleepReminderStore.get(context)
    fun getForEditing(context: Context): SleepReminder? = SleepReminderStore.getForEditing(context)
    fun clear(context: Context) = SleepReminderStore.clear(context)
    fun disable(context: Context) = SleepReminderStore.disableReminder(context)
}

object AlarmCore {
    fun schedule(context: Context, reminder: SleepReminder): Boolean {
        return AlarmScheduler.scheduleReminder(context, reminder)
    }

    fun cancel(context: Context) {
        AlarmScheduler.cancelReminder(context)
    }

    fun scheduleSnooze(context: Context, minutes: Int = 5): Boolean {
        return AlarmScheduler.scheduleSnooze(context, minutes)
    }

    fun dismissCurrent(context: Context) {
        AlarmScheduler.dismissCurrent(context)
    }
}

object AlarmScheduler {

    const val ACTION_TRIGGER_ALARM = "com.example.stepforge.action.TRIGGER_SLEEP_ALARM"
    const val ACTION_STOP_ALARM = "com.example.stepforge.action.STOP_SLEEP_ALARM"
    const val ACTION_SNOOZE_ALARM = "com.example.stepforge.action.SNOOZE_SLEEP_ALARM"
    const val ACTION_OPEN_ALARM_SCREEN = "com.example.stepforge.action.OPEN_SLEEP_ALARM_SCREEN"
    const val ACTION_TRIGGER_REMINDER = ACTION_TRIGGER_ALARM
    const val ACTION_SNOOZE_REMINDER = ACTION_SNOOZE_ALARM
    const val ACTION_DISABLE_REMINDER = ACTION_STOP_ALARM
    private const val ACTION_LEGACY_TRIGGER = "com.example.stepforge.action.SLEEP_REMINDER"

    const val NOTIFICATION_ID = 5101

    const val REQUEST_TRIGGER = 2401
    const val REQUEST_SNOOZE = 2402
    const val REQUEST_OPEN_SCREEN = 5102
    const val REQUEST_NOTIFICATION_SNOOZE = 5103
    const val REQUEST_NOTIFICATION_STOP = 5104
    private const val DAILY_REQUEST_CODE = REQUEST_TRIGGER
    private const val SNOOZE_REQUEST_CODE = REQUEST_SNOOZE
    const val FULL_SCREEN_REQUEST_CODE = REQUEST_OPEN_SCREEN
    const val NOTIFICATION_SNOOZE_REQUEST_CODE = REQUEST_NOTIFICATION_SNOOZE
    const val NOTIFICATION_STOP_REQUEST_CODE = REQUEST_NOTIFICATION_STOP

    fun scheduleReminder(context: Context, reminder: SleepReminder): Boolean {
        cancelDailyReminder(context)
        val reminderWithBackground = AlarmBackgroundPresets.withFallback(context, reminder.copy(enabled = true))
        SleepReminderStore.save(context, reminderWithBackground)
        return scheduleInternal(
            context = context,
            triggerAtMillis = SleepReminderStore.nextTriggerMillis(reminderWithBackground),
            requestCode = DAILY_REQUEST_CODE
        )
    }

    fun scheduleDailyReminder(context: Context, hour: Int, minute: Int): Boolean {
        return scheduleReminder(context, SleepReminder(hour, minute))
    }

    fun scheduleNextReminderFromStore(context: Context): Boolean {
        cancelDailyReminder(context)
        val reminder = SleepReminderStore.get(context)?.let { AlarmBackgroundPresets.withFallback(context, it) }
            ?: return false
        SleepReminderStore.save(context, reminder)
        return scheduleInternal(
            context = context,
            triggerAtMillis = SleepReminderStore.nextTriggerMillis(reminder),
            requestCode = DAILY_REQUEST_CODE
        )
    }

    fun scheduleSnooze(context: Context, minutes: Int = 5): Boolean {
        cancelPending(context, SNOOZE_REQUEST_CODE)
        val triggerAtMillis = System.currentTimeMillis() + minutes * 60_000L
        return scheduleInternal(
            context = context,
            triggerAtMillis = triggerAtMillis,
            requestCode = SNOOZE_REQUEST_CODE
        )
    }

    fun cancelReminder(context: Context) {
        cancelDailyReminder(context)
        cancelPending(context, SNOOZE_REQUEST_CODE)
        AlarmSoundPlayer.stop()
        AlarmRuntimeGuard.clear()
        cancelNotification(context)
        context.stopService(Intent(context, AlarmService::class.java))
        SleepReminderStore.disableReminder(context)
    }

    fun dismissCurrent(context: Context) {
        cancelPending(context, SNOOZE_REQUEST_CODE)
        AlarmSoundPlayer.stop()
        AlarmRuntimeGuard.clear()
        cancelNotification(context)
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    private fun scheduleInternal(
        context: Context,
        triggerAtMillis: Long,
        requestCode: Int
    ): Boolean {
        val alarmManager = context.getSystemService(AlarmManager::class.java)

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = ACTION_TRIGGER_ALARM
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return if (canScheduleExactAlarms(context)) {
            val showIntent = PendingIntent.getActivity(
                context,
                requestCode + 10_000,
                Intent(context, AlarmRingActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                pendingIntent
            )
            true
        } else {
            alarmManager.setWindow(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                15 * 60_000L,
                pendingIntent
            )
            false
        }
    }

    private fun cancelDailyReminder(context: Context) {
        cancelPending(context, DAILY_REQUEST_CODE)
    }

    private fun cancelPending(context: Context, requestCode: Int) {
        val actions = listOf(
            ACTION_TRIGGER_ALARM,
            ACTION_SNOOZE_ALARM,
            ACTION_STOP_ALARM,
            ACTION_LEGACY_TRIGGER,
            null
        )
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        actions.forEach { action ->
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                this.action = action
            }
            PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.let { pendingIntent ->
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        }

        listOf(requestCode + 10_000, FULL_SCREEN_REQUEST_CODE).forEach { activityRequestCode ->
            PendingIntent.getActivity(
                context,
                activityRequestCode,
                Intent(context, AlarmRingActivity::class.java),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )?.cancel()
        }

        listOf(NOTIFICATION_SNOOZE_REQUEST_CODE, NOTIFICATION_STOP_REQUEST_CODE).forEach { notificationRequestCode ->
            actions.forEach { action ->
                val intent = Intent(context, AlarmReceiver::class.java).apply {
                    this.action = action
                }
                PendingIntent.getBroadcast(
                    context,
                    notificationRequestCode,
                    intent,
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )?.cancel()
            }
        }
    }

    private fun cancelNotification(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }
}

object AlarmSoundPlayer {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null

    fun isRunning(): Boolean = mediaPlayer != null || vibrator != null

    fun start(context: Context, reminder: SleepReminder?) {
        if (isRunning()) return
        stop()

        val soundUri = try {
            reminder?.effectiveSoundUri?.let { Uri.parse(it) }
        } catch (_: Exception) {
            null
        } ?: Settings.System.DEFAULT_ALARM_ALERT_URI

        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(context, soundUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (_: Exception) {
            stop()
        }

        if (reminder?.vibrate != false) {
            startVibration(context, reminder)
        }
    }

    fun stop() {
        mediaPlayer?.run {
            try {
                stop()
            } catch (_: Exception) {
            }
            release()
        }
        mediaPlayer = null

        vibrator?.cancel()
        vibrator = null
    }

    private fun startVibration(
        context: Context,
        reminder: SleepReminder?
    ) {

        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(
                android.os.VibratorManager::class.java
            ).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        val pattern = when (reminder?.vibrationStyle) {

            "soft" -> longArrayOf(
                0,
                120,
                120,
                120
            )

            "strong" -> longArrayOf(
                0,
                700,
                250,
                700
            )

            else -> longArrayOf(
                0,
                500,
                300,
                500
            )
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            vibrator?.vibrate(
                VibrationEffect.createWaveform(
                    pattern,
                    0
                )
            )

        } else {

            @Suppress("DEPRECATION")
            vibrator?.vibrate(
                pattern,
                0
            )
        }
    }
}

object AlarmNotificationHelper {

    fun buildRingingNotification(
        context: Context,
        reminder: SleepReminder,
        launchFullscreen: Boolean
    ): Notification {
        ensureChannel(context)
        val alarmTime = formatNotificationAlarmTime(context, reminder.hour, reminder.minute)
        val timeText = alarmTime.displayTime
        val message = reminder.message.ifBlank {
            context.getString(AppR.string.sleep_reminder_time_to_wind_down)
        }

        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            action = AlarmScheduler.ACTION_OPEN_ALARM_SCREEN
            addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            )
        }

        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            AlarmScheduler.FULL_SCREEN_REQUEST_CODE,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val snoozeIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmScheduler.ACTION_SNOOZE_ALARM
        }

        val stopIntent = Intent(context, AlarmService::class.java).apply {
            action = AlarmScheduler.ACTION_STOP_ALARM
        }

        val snoozePendingIntent = servicePendingIntent(
            context,
            AlarmScheduler.NOTIFICATION_SNOOZE_REQUEST_CODE,
            snoozeIntent
        )

        val stopPendingIntent = servicePendingIntent(
            context,
            AlarmScheduler.NOTIFICATION_STOP_REQUEST_CODE,
            stopIntent
        )
        val notificationTitle = context.getString(AppR.string.sleep_reminder_notification_title)
        val notificationText = context.getString(
            AppR.string.sleep_reminder_notification_text,
            message,
            timeText
        )

        val headsUpView = RemoteViews(
            context.packageName,
            AppR.layout.notification_sleep_alarm_heads_up
        ).apply {
            setTextViewText(
                AppR.id.alarm_notification_app_name,
                context.getString(AppR.string.app_name)
            )

            setTextViewText(
                AppR.id.alarm_notification_time,
                alarmTime.time
            )

            if (alarmTime.amPm.isNullOrBlank()) {
                setViewVisibility(AppR.id.alarm_notification_time_ampm, View.GONE)
            } else {
                setTextViewText(AppR.id.alarm_notification_time_ampm, alarmTime.amPm)
                setViewVisibility(AppR.id.alarm_notification_time_ampm, View.VISIBLE)
            }

            setTextViewText(
                AppR.id.action_stop_alarm,
                context.getString(AppR.string.sleep_reminder_action_stop_short)
            )

            setTextViewText(
                AppR.id.action_snooze_alarm,
                context.getString(AppR.string.sleep_reminder_action_snooze_short)
            )

            setOnClickPendingIntent(
                AppR.id.alarm_notification_root,
                fullScreenPendingIntent
            )

            setOnClickPendingIntent(
                AppR.id.action_stop_alarm,
                stopPendingIntent
            )

            setOnClickPendingIntent(
                AppR.id.action_snooze_alarm,
                snoozePendingIntent
            )
        }

        return NotificationCompat.Builder(context, AlarmReceiver.RINGING_CHANNEL_ID)
            .setSmallIcon(AppR.drawable.ic_walk)
            .setContentTitle(notificationTitle)
            .setContentText(notificationText)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setDefaults(0)
            .setSound(null)
            .setVibrate(null)
            .setColorized(false)
            .setContentIntent(fullScreenPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, launchFullscreen)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

            // Aynı sade layout: collapsed, heads-up ve expanded hepsinde büyütülmüş görünüm.
            .setCustomContentView(headsUpView)
            .setCustomHeadsUpContentView(headsUpView)
            .setCustomBigContentView(headsUpView)
            .build()
    }

    private data class NotificationAlarmTime(
        val time: String,
        val amPm: String?,
    ) {
        val displayTime: String
            get() = if (amPm.isNullOrBlank()) time else "$time $amPm"
    }

    private fun formatNotificationAlarmTime(context: Context, hour: Int, minute: Int): NotificationAlarmTime {
        if (DateFormat.is24HourFormat(context)) {
            return NotificationAlarmTime(
                time = "%02d:%02d".format(Locale.getDefault(), hour, minute),
                amPm = null
            )
        }
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val formatted = SimpleDateFormat("h:mm a", Locale.getDefault()).format(calendar.time).trim()
        val spaceIndex = formatted.lastIndexOf(' ')
        return if (spaceIndex > 0) {
            NotificationAlarmTime(
                time = formatted.substring(0, spaceIndex),
                amPm = formatted.substring(spaceIndex + 1)
            )
        } else {
            NotificationAlarmTime(time = formatted, amPm = null)
        }
    }

    private fun servicePendingIntent(
        context: Context,
        requestCode: Int,
        intent: Intent
    ): PendingIntent {
        val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PendingIntent.getForegroundService(context, requestCode, intent, flags)
        } else {
            PendingIntent.getService(context, requestCode, intent, flags)
        }
    }

    fun show(context: Context, launchFullscreen: Boolean = false) {
        val reminder = SleepReminderStore.get(context) ?: return
        val notification = buildRingingNotification(context, reminder, launchFullscreen)
        if (
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            NotificationManagerCompat.from(context)
                .notify(AlarmScheduler.NOTIFICATION_ID, notification)
        }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val ringingChannel = NotificationChannel(
            AlarmReceiver.RINGING_CHANNEL_ID,
            context.getString(AppR.string.sleep_reminder_ringing_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(AppR.string.sleep_reminder_ringing_channel_description)
            setSound(null, null)
            enableVibration(false)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val normalChannel = NotificationChannel(
            AlarmReceiver.CHANNEL_ID,
            context.getString(AppR.string.sleep_reminder_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(AppR.string.sleep_reminder_channel_description)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        val silentChannel = NotificationChannel(
            AlarmReceiver.SILENT_CHANNEL_ID,
            context.getString(AppR.string.sleep_reminder_silent_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(AppR.string.sleep_reminder_silent_channel_description)
            setSound(null, null)
            enableVibration(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(ringingChannel)
        manager.createNotificationChannel(normalChannel)
        manager.createNotificationChannel(silentChannel)
    }
}

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = when (intent.action) {
            ACTION_SNOOZE,
            AlarmScheduler.ACTION_SNOOZE_ALARM -> AlarmScheduler.ACTION_SNOOZE_ALARM
            ACTION_DISMISS,
            AlarmScheduler.ACTION_STOP_ALARM -> AlarmScheduler.ACTION_STOP_ALARM
            else -> AlarmScheduler.ACTION_TRIGGER_ALARM
        }
        if (action == AlarmScheduler.ACTION_TRIGGER_ALARM && SleepReminderStore.get(context) == null) {
            return
        }
        if (
            action == AlarmScheduler.ACTION_TRIGGER_ALARM &&
            AlarmPresentationDecider.shouldOpenFullScreen(context)
        ) {
            acquireAlarmWakeLock(context)
            AlarmFullscreenLauncher.launch(context)
        }
        AlarmService.start(context, action)
    }

    companion object {
        const val CHANNEL_ID = "sleep_alarm_channel_v4"
        const val SILENT_CHANNEL_ID = "sleep_alarm_silent_channel_v1"
        const val RINGING_CHANNEL_ID = "sleep_alarm_ringing_channel_v7"
        const val NOTIFICATION_ID = AlarmScheduler.NOTIFICATION_ID

        const val ACTION_SNOOZE = "sleep_alarm_snooze"
        const val ACTION_DISMISS = "sleep_alarm_dismiss"
    }
}

private enum class AlarmLifecycleState {
    Idle,
    Scheduled,
    Ringing,
    Snoozed,
    Stopped,
    Consumed
}

private object AlarmRuntimeGuard {
    @Volatile private var lastTriggerElapsed: Long = 0L
    @Volatile private var state: AlarmLifecycleState = AlarmLifecycleState.Idle

    fun isDuplicateTrigger(): Boolean {
        val now = SystemClock.elapsedRealtime()
        if (state == AlarmLifecycleState.Ringing && now - lastTriggerElapsed < 8_000L) return true
        lastTriggerElapsed = now
        return false
    }

    fun isStillEnabled(context: Context): Boolean {
        return SleepReminderStore.get(context) != null
    }

    fun beginRinging(): Boolean {
        if (state == AlarmLifecycleState.Ringing) return false
        state = AlarmLifecycleState.Ringing
        lastTriggerElapsed = SystemClock.elapsedRealtime()
        return true
    }

    fun markScheduled() {
        state = AlarmLifecycleState.Scheduled
    }

    fun markSnoozed() {
        state = AlarmLifecycleState.Snoozed
    }

    fun markStopped() {
        state = AlarmLifecycleState.Stopped
    }

    fun markConsumed() {
        state = AlarmLifecycleState.Consumed
    }

    fun clear() {
        state = AlarmLifecycleState.Idle
    }
}

object AlarmPresentationDecider {
    fun shouldLaunchFullscreen(context: Context): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        return !powerManager.isInteractive || keyguardManager.isKeyguardLocked
    }

    fun shouldOpenFullScreen(context: Context): Boolean = shouldLaunchFullscreen(context)
}

object AlarmFullscreenLauncher {
    fun launch(context: Context) {
        val fullScreenIntent = Intent(context, AlarmRingActivity::class.java).apply {
            action = AlarmScheduler.ACTION_OPEN_ALARM_SCREEN
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_CLEAR_TOP or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            AlarmScheduler.FULL_SCREEN_REQUEST_CODE,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    setPendingIntentBackgroundActivityStartMode(
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                    )
                }
                pendingIntent.send(context, 0, null, null, null, null, options.toBundle())
            } else {
                pendingIntent.send()
            }
        } catch (_: Exception) {
            try {
                context.startActivity(fullScreenIntent)
            } catch (_: Exception) {
            }
        }
    }
}

private fun acquireAlarmWakeLock(context: Context): PowerManager.WakeLock {
    val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    return pm.newWakeLock(
        PowerManager.PARTIAL_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
        "StepForge:SleepAlarm"
    ).apply {
        setReferenceCounted(false)
        acquire(10_000L)
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && SleepReminderStore.get(context) != null) {
            AlarmScheduler.scheduleNextReminderFromStore(context)
        }
    }
}

class AlarmService : Service() {

    private var voiceCommandEngine: VoiceCommandEngine? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        stopVoiceCommands()
        AlarmSoundPlayer.stop()
        cancelNotification()
        stopForegroundCompat()
        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            AlarmScheduler.ACTION_STOP_ALARM -> stopAlarm(scheduleNextDaily = true)
            AlarmScheduler.ACTION_SNOOZE_ALARM -> snoozeAlarm()
            else -> startRinging()
        }

        return START_NOT_STICKY
    }

    private fun startRinging() {
        val reminder = SleepReminderStore.get(this) ?: run {
            stopAlarm(scheduleNextDaily = false)
            return
        }
        if (!AlarmRuntimeGuard.beginRinging()) {
            return
        }

        val launchFullscreen = AlarmPresentationDecider.shouldOpenFullScreen(this)
        val notification = AlarmNotificationHelper.buildRingingNotification(
            context = this,
            reminder = reminder,
            launchFullscreen = launchFullscreen
        )
        startAlarmForeground(notification, reminder)

        AlarmSoundPlayer.start(this, reminder)
        startVoiceCommandsIfAllowed(reminder)

        if (launchFullscreen) {
            acquireAlarmWakeLock(this)
            launchRingScreen()
        }
    }

    private fun snoozeAlarm() {
        AlarmRuntimeGuard.markSnoozed()
        stopVoiceCommands()
        AlarmSoundPlayer.stop()
        cancelNotification()
        closeRingScreen(AlarmReceiver.ACTION_SNOOZE)
        AlarmScheduler.scheduleSnooze(this, 5)
        stopForegroundCompat()
        stopSelf()
    }

    private fun stopAlarm(scheduleNextDaily: Boolean) {
        val reminder = SleepReminderStore.get(this)
        stopVoiceCommands()
        AlarmSoundPlayer.stop()
        cancelNotification()
        closeRingScreen(AlarmReceiver.ACTION_DISMISS)

        if (scheduleNextDaily && reminder?.repeatDaily == true) {
            AlarmRuntimeGuard.markScheduled()
            AlarmScheduler.scheduleNextReminderFromStore(this)
        } else if (scheduleNextDaily && reminder != null) {
            AlarmRuntimeGuard.markConsumed()
            SleepReminderStore.disableReminder(this)
        } else {
            AlarmRuntimeGuard.markStopped()
        }

        stopForegroundCompat()
        stopSelf()
    }

    private fun launchRingScreen() {
        AlarmFullscreenLauncher.launch(this)
    }

    private fun closeRingScreen(action: String) {
        val closeIntent = Intent(this, AlarmRingActivity::class.java).apply {
            this.action = action
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        try {
            startActivity(closeIntent)
        } catch (_: Exception) {
        }
    }

    private fun cancelNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(AlarmScheduler.NOTIFICATION_ID)
    }

    private fun startVoiceCommandsIfAllowed(reminder: SleepReminder) {
        if (!reminder.voiceCommandsEnabled) return
        if (
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(this)) return
        voiceCommandEngine?.destroy()
        try {
            voiceCommandEngine = AndroidSpeechVoiceCommandEngine(
                context = this,
                onStop = { start(this, AlarmScheduler.ACTION_STOP_ALARM) },
                onSnooze = { start(this, AlarmScheduler.ACTION_SNOOZE_ALARM) }
            ).also { it.startListening() }
        } catch (_: Exception) {
            voiceCommandEngine = null
        }
    }

    private fun stopVoiceCommands() {
        voiceCommandEngine?.destroy()
        voiceCommandEngine = null
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun foregroundServiceTypes(reminder: SleepReminder): Int {
        var types = ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
            reminder.voiceCommandsEnabled &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED
        ) {
            types = types or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        return types
    }

    private fun startAlarmForeground(notification: Notification, reminder: SleepReminder) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                startForeground(
                    AlarmScheduler.NOTIFICATION_ID,
                    notification,
                    foregroundServiceTypes(reminder)
                )
            } catch (_: SecurityException) {
                startForeground(
                    AlarmScheduler.NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            }
        } else {
            startForeground(AlarmScheduler.NOTIFICATION_ID, notification)
        }
    }

    companion object {
        fun start(context: Context, action: String) {
            val intent = Intent(context, AlarmService::class.java).apply {
                this.action = action
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}

private interface VoiceCommandEngine {
    fun startListening()
    fun stopListening()
    fun destroy()
    fun onCommand(command: String)
}

private class AndroidSpeechVoiceCommandEngine(
    private val context: Context,
    private val onStop: () -> Unit,
    private val onSnooze: () -> Unit
) : VoiceCommandEngine {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var active = false
    private var commandHandled = false
    private var sessionActive = false
    private val restartSessionRunnable = Runnable { beginSession() }

    override fun startListening() {
        if (active || !SpeechRecognizer.isRecognitionAvailable(context)) return
        active = true
        commandHandled = false
        ensureRecognizer()
        scheduleSessionRestart(delayMs = 0L)
    }

    override fun stopListening() {
        active = false
        handler.removeCallbacks(restartSessionRunnable)
        sessionActive = false
        try {
            recognizer?.cancel()
        } catch (_: Exception) {
        }
    }

    override fun destroy() {
        active = false
        commandHandled = true
        sessionActive = false
        handler.removeCallbacks(restartSessionRunnable)
        recognizer?.run {
            try {
                cancel()
            } catch (_: Exception) {
            }
            destroy()
        }
        recognizer = null
    }

    override fun onCommand(command: String) {
        handleMatches(listOf(command))
    }

    private fun ensureRecognizer() {
        if (recognizer != null) return
        recognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    sessionActive = true
                }

                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() {
                    // Oturum kapanmadan yenisini hazırla; mikrofon göstergesi sönmesin.
                    scheduleSessionRestart(delayMs = 0L)
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    handleMatches(matches)
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onError(error: Int) {
                    sessionActive = false
                    if (!active || commandHandled) return
                    when (error) {
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> destroy()
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> scheduleSessionRestart(delayMs = 80L)
                        SpeechRecognizer.ERROR_CLIENT -> {
                            resetRecognizer()
                            scheduleSessionRestart(delayMs = 120L)
                        }
                        else -> scheduleSessionRestart(delayMs = 0L)
                    }
                }

                override fun onResults(results: Bundle?) {
                    sessionActive = false
                    val matches = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    handleMatches(matches)
                    if (!commandHandled) {
                        scheduleSessionRestart(delayMs = 0L)
                    }
                }
            })
        }
    }

    private fun resetRecognizer() {
        recognizer?.run {
            try {
                cancel()
            } catch (_: Exception) {
            }
            destroy()
        }
        recognizer = null
    }

    private fun beginSession() {
        if (!active || commandHandled) return
        ensureRecognizer()
        if (sessionActive) {
            scheduleSessionRestart(delayMs = 50L)
            return
        }
        try {
            recognizer?.startListening(recognizerIntent())
            sessionActive = true
        } catch (_: Exception) {
            sessionActive = false
            scheduleSessionRestart(delayMs = 100L)
        }
    }

    private fun scheduleSessionRestart(delayMs: Long) {
        if (!active || commandHandled) return
        handler.removeCallbacks(restartSessionRunnable)
        if (delayMs <= 0L) {
            handler.post(restartSessionRunnable)
        } else {
            handler.postDelayed(restartSessionRunnable, delayMs)
        }
    }

    private fun recognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault().toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Alarm bitene kadar oturum mümkün olduğunca açık kalsın.
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 300_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 300_000L)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 300_000L)
        }
    }

    private fun handleMatches(matches: List<String>) {
        if (commandHandled || matches.isEmpty()) return
        val normalized = matches.map { normalizeServiceVoiceCommand(it) }
        when {
            normalized.any { ServiceAlarmVoiceCommandMatcher.isStopCommand(it) } -> {
                commandHandled = true
                destroy()
                onStop()
            }

            normalized.any { ServiceAlarmVoiceCommandMatcher.isSnoozeCommand(it) } -> {
                commandHandled = true
                destroy()
                onSnooze()
            }
        }
    }
}

private object ServiceAlarmVoiceCommandMatcher {
    private val stopPhrases = listOf(
        "stop",
        "dismiss",
        "turn off",
        "stop alarm",
        "durdur",
        "kapat",
        "alarmi durdur",
        "alarmi kapat",
        "stopp",
        "stoppen",
        "verwerfen",
        "beenden",
        "ausschalten"
    )

    private val snoozePhrases = listOf(
        "snooze",
        "remind me",
        "later",
        "snooze alarm",
        "ertele",
        "birazdan",
        "bes dakika ertele",
        "5 dakika ertele",
        "schlummern",
        "erinnern",
        "spater",
        "funf minuten"
    )

    fun isStopCommand(command: String): Boolean {
        return stopPhrases.any { command == it || command.contains(it) }
    }

    fun isSnoozeCommand(command: String): Boolean {
        return snoozePhrases.any { command == it || command.contains(it) }
    }
}

private fun normalizeServiceVoiceCommand(raw: String): String {
    val turkishSafe = raw
        .lowercase(Locale.ROOT)
        .replace('ı', 'i')
        .replace('ğ', 'g')
        .replace('ü', 'u')
        .replace('ş', 's')
        .replace('ö', 'o')
        .replace('ç', 'c')
    val withoutMarks = Normalizer.normalize(turkishSafe, Normalizer.Form.NFD)
        .replace(Regex("\\p{Mn}+"), "")
    return withoutMarks
        .replace(Regex("[^a-z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}
