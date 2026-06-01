@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.stepforge.ui.sleep.alarm

import android.app.Activity
import android.app.AlarmManager
import android.app.KeyguardManager
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.speech.SpeechRecognizer
import android.view.WindowManager
import android.view.WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import androidx.core.content.ContextCompat
import com.example.stepforge.R
import com.example.stepforge.ui.sleep.localizedTitle
import com.example.stepforge.ui.sleep.titleFor
import com.example.stepforge.ui.rememberUseDarkTheme
import com.example.stepforge.ui.stepforgeTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ColorScheme
import androidx.compose.ui.draw.shadow


private fun ColorScheme.isAppDarkTheme(): Boolean =
    background.red + background.green + background.blue < 1.5f

private data class SleepReminderSurfaceTokens(
    val pageBg: Color,
    val cardBg: Color,
    val subtleBg: Color,
    val border: Color,
    val shadow: androidx.compose.ui.unit.Dp,
    val title: Color,
    val body: Color,
    val muted: Color,
    val accent: Color,
    val accentOn: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val chipBg: Color,
    val divider: Color,
    val isDark: Boolean
)

@Composable
private fun rememberSleepReminderSurfaceTokens(): SleepReminderSurfaceTokens {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.isAppDarkTheme()

    return if (isDark) {
        SleepReminderSurfaceTokens(
            pageBg = cs.background,
            cardBg = cs.surface.copy(alpha = 0.96f),
            subtleBg = cs.surfaceVariant.copy(alpha = 0.36f),
            border = cs.outlineVariant.copy(alpha = 0.28f),
            shadow = 0.dp,
            title = cs.onSurface,
            body = cs.onSurface,
            muted = cs.onSurfaceVariant,
            accent = cs.primary,
            accentOn = cs.onPrimary,
            accentContainer = cs.primaryContainer,
            onAccentContainer = cs.onPrimaryContainer,
            chipBg = cs.surface.copy(alpha = 0.85f),
            divider = cs.outlineVariant.copy(alpha = 0.35f),
            isDark = true
        )
    } else {
        SleepReminderSurfaceTokens(
            pageBg = cs.background,
            cardBg = cs.surface,
            subtleBg = cs.surfaceVariant,
            border = Color(0xFFDCE4EC),
            shadow = 10.dp,
            title = cs.onBackground,
            body = cs.onSurface,
            muted = cs.onSurfaceVariant,
            accent = cs.primary,
            accentOn = cs.onPrimary,
            accentContainer = cs.primaryContainer,
            onAccentContainer = cs.onPrimaryContainer,
            chipBg = Color(0xFFEEF3F7),
            divider = Color(0xFFE2E8F0),
            isDark = false
        )
    }
}

private fun Modifier.sleepReminderCardChrome(
    tokens: SleepReminderSurfaceTokens,
    shape: RoundedCornerShape
): Modifier {
    return this
        .shadow(
            elevation = tokens.shadow,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.08f),
            spotColor = Color.Black.copy(alpha = 0.14f)
        )
        .border(
            width = 1.dp,
            color = tokens.border,
            shape = shape
        )
}

class AlarmActivity : ComponentActivity() {



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)

        }

        getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)

        if (intent?.action == AlarmReceiver.ACTION_DISMISS ||
            intent?.action == AlarmReceiver.ACTION_SNOOZE
        ) {
            finish()
            return
        }

        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                AlarmRingScreen(
                    onDismissOnly = { dismissOnly() },
                    onSnooze = { snoozeAndFinish() },
                    onTurnOff = { turnOffAndFinish() },
                    showVoiceCommandHint = false
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (intent?.action == AlarmReceiver.ACTION_DISMISS ||
            intent?.action == AlarmReceiver.ACTION_SNOOZE
        ) {
            finish()
        }
    }

    private fun dismissOnly() {
        AlarmService.start(this, AlarmScheduler.ACTION_STOP_ALARM)
        finish()
    }

    private fun snoozeAndFinish() {
        AlarmService.start(this, AlarmScheduler.ACTION_SNOOZE_ALARM)
        finish()
    }

    private fun turnOffAndFinish() {
        AlarmService.start(this, AlarmScheduler.ACTION_STOP_ALARM)
        finish()
    }


}

class AlarmRingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setShowWhenLocked(true)
        setTurnScreenOn(true)
        getSystemService(KeyguardManager::class.java)?.requestDismissKeyguard(this, null)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
        )

        if (shouldCloseForAction(intent)) {
            finish()
            return
        }

        val showVoiceHint = shouldEnableVoiceCommands()
        setContent {
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                AlarmRingScreen(
                    onDismissOnly = { dismissOnly() },
                    onSnooze = { snoozeAndFinish() },
                    onTurnOff = { turnOffAndFinish() },
                    showVoiceCommandHint = showVoiceHint
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (shouldCloseForAction(intent)) {
            finish()
        }
    }

    private fun dismissOnly() {
        AlarmService.start(this, AlarmScheduler.ACTION_STOP_ALARM)
        finish()
    }

    private fun snoozeAndFinish() {
        AlarmService.start(this, AlarmScheduler.ACTION_SNOOZE_ALARM)
        finish()
    }

    private fun turnOffAndFinish() {
        AlarmService.start(this, AlarmScheduler.ACTION_STOP_ALARM)
        finish()
    }

    private fun shouldCloseForAction(intent: Intent?): Boolean {
        return intent?.action == AlarmReceiver.ACTION_DISMISS ||
            intent?.action == AlarmReceiver.ACTION_SNOOZE
    }

    private fun shouldEnableVoiceCommands(): Boolean {
        return SleepReminderStore.get(this)?.voiceCommandsEnabled == true &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED &&
            SpeechRecognizer.isRecognitionAvailable(this)
    }

}

@Composable
fun SleepReminderScreen(
    initialReminder: SleepReminder?,
    onDismiss: () -> Unit,
    onSave: (SleepReminder) -> Unit,
    onTurnOff: (() -> Unit)? = null,

) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val context = LocalContext.current
    val is24Hour = android.text.format.DateFormat.is24HourFormat(context)
    context.getSystemService(AlarmManager::class.java)
    val canExact = AlarmScheduler.canScheduleExactAlarms(context)
    val canFullScreen = AlarmPermissionHelper.canUseFullScreenIntent(context)

    val savedReminder = remember {
        AlarmStore.get(context)
    }
    var alarmEnabled by rememberSaveable { mutableStateOf(savedReminder != null) }
    val systemAlarmLabel = stringResource(R.string.sleep_reminder_system_alarm)
    val themeBackgroundLabel = stringResource(R.string.sleep_reminder_theme_background)
    val defaultMessage = stringResource(R.string.sleep_reminder_time_to_wind_down)

    LaunchedEffect(Unit) {

        if (!AlarmScheduler.canScheduleExactAlarms(context)) {
            context.startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } else if (!AlarmPermissionHelper.canUseFullScreenIntent(context)) {
            context.startActivity(
                AlarmPermissionHelper.fullScreenIntentSettingsIntent(context)
            )
        }
    }

    LaunchedEffect(Unit) {

        android.util.Log.d(
            "STEPFORGE_ALARM",
            "canExact=${AlarmScheduler.canScheduleExactAlarms(context)}"
        )

        android.util.Log.d(
            "STEPFORGE_ALARM",
            "canFullScreen=${AlarmPermissionHelper.canUseFullScreenIntent(context)}"
        )
    }

    var hour24 by rememberSaveable {
        mutableIntStateOf(initialReminder?.hour ?: SleepReminderStore.DEFAULT_DISABLED_HOUR)
    }
    var minute by rememberSaveable {
        mutableIntStateOf(initialReminder?.minute ?: SleepReminderStore.DEFAULT_DISABLED_MINUTE)
    }
    var editingPart by remember { mutableStateOf(TimePart.HOUR) }

    var repeatDaily by rememberSaveable { mutableStateOf(initialReminder?.repeatDaily ?: true) }
    var silent by rememberSaveable { mutableStateOf(initialReminder?.silent ?: false) }
    var vibrate by rememberSaveable { mutableStateOf(initialReminder?.vibrate ?: true) }
    var voiceCommandsEnabled by rememberSaveable {
        mutableStateOf(initialReminder?.voiceCommandsEnabled ?: false)
    }
    var showVoicePermissionInfo by remember { mutableStateOf(false) }
    var showVoiceHelp by remember { mutableStateOf(false) }
    var showVoiceDenied by remember { mutableStateOf(false) }

    var vibrationStyle by rememberSaveable {
        mutableStateOf(
            initialReminder?.vibrationStyle ?: "normal"
        )
    }

    var soundUri by rememberSaveable { mutableStateOf(initialReminder?.effectiveSoundUri) }
    var soundLabel by rememberSaveable {
        mutableStateOf(
            initialReminder?.soundTitle ?: systemAlarmLabel
        )
    }
    var backgroundUri by rememberSaveable { mutableStateOf(initialReminder?.effectiveBackgroundUri) }
    var backgroundLabel by rememberSaveable {
        mutableStateOf(
            initialReminder?.backgroundTitle ?: themeBackgroundLabel
        )
    }
    var messageText by rememberSaveable {
        mutableStateOf(
            initialReminder?.message ?: defaultMessage
        )
    }
    var clockStyle by rememberSaveable {
        mutableStateOf(
            initialReminder?.clockStyle ?: AlarmClockStyles.STYLE_STACKED
        )
    }

    LaunchedEffect(initialReminder) {
        initialReminder?.let {
            hour24 = it.hour
            minute = it.minute
            repeatDaily = it.repeatDaily
            silent = it.silent
            vibrate = it.vibrate
            voiceCommandsEnabled = it.voiceCommandsEnabled
            soundUri = it.effectiveSoundUri
            soundLabel = it.soundTitle ?: systemAlarmLabel
            backgroundUri = it.effectiveBackgroundUri
            backgroundLabel = it.backgroundTitle ?: themeBackgroundLabel
            messageText = it.message
            clockStyle = it.clockStyle
        }
    }
    var showWallpaperEditor by remember { mutableStateOf(false) }

    val microphonePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        voiceCommandsEnabled = granted
        if (!granted) showVoiceDenied = true
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult

        val picked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(
                RingtoneManager.EXTRA_RINGTONE_PICKED_URI,
                Uri::class.java
            )
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra<Uri>(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }

        if (picked != null) {
            soundUri = picked.toString()
            soundLabel = context.getString(R.string.sleep_reminder_system_sound_selected)
        } else {
            soundUri = null
            soundLabel = context.getString(R.string.sleep_reminder_system_alarm)
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            soundUri = uri.toString()
            soundLabel = context.getString(R.string.sleep_reminder_custom_audio_selected)
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            persistReadPermission(context, uri)
            backgroundUri = uri.toString()
            backgroundLabel = context.getString(R.string.sleep_reminder_image_selected)
        }
    }


    val reminder = SleepReminder(
        hour = hour24,
        minute = minute,
        repeatDaily = repeatDaily,
        silent = silent,
        vibrate = vibrate,
        voiceCommandsEnabled = voiceCommandsEnabled,
        vibrationStyle = vibrationStyle,
        soundUriString = soundUri,
        soundTitle = soundLabel,
        backgroundUriString = backgroundUri,
        backgroundTitle = backgroundLabel,
        message = messageText.ifBlank { defaultMessage },
        clockStyle = clockStyle,
        ringtoneUri = soundUri,
        backgroundUri = backgroundUri
    )

    val previewTime = remember(hour24, minute, is24Hour) {
        formatPreviewTime(hour24, minute, is24Hour)
    }

    val nextTriggerText = remember(reminder) {
        val triggerMs = SleepReminderStore.nextTriggerMillis(reminder)
        android.text.format.DateUtils.getRelativeTimeSpanString(
            triggerMs,
            System.currentTimeMillis(),
            android.text.format.DateUtils.MINUTE_IN_MILLIS,
            android.text.format.DateUtils.FORMAT_ABBREV_RELATIVE
        ).toString()
    }


    val scroll = rememberScrollState()

    Scaffold(
        containerColor = tokens.pageBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.sleep_reminder_title),
                        color = tokens.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.sleep_back),
                            tint = tokens.title
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.pageBg
                )
            )
        }
    ) { pad ->
        if (showWallpaperEditor) {

            WallpaperClockScreen(
                backgroundLabel = backgroundLabel,
                selectedBackgroundUri = backgroundUri,
                clockStyle = clockStyle,
                hour24 = hour24,
                minute = minute,
                message = messageText,
                onDismiss = {
                    showWallpaperEditor = false
                },
                onPresetSelected = { preset ->
                    backgroundUri = preset.uriString
                    backgroundLabel = preset.localizedTitle(context)
                },
                onClockStyleSelected = {
                    clockStyle = it.id
                },
                onRandomPreset = {
                    val preset = AlarmBackgroundPresets.random()
                    backgroundUri = preset.uriString
                    backgroundLabel = preset.localizedTitle(context)
                },
                onChooseImage = {
                    imagePickerLauncher.launch(arrayOf("image/*"))
                },
                onClear = {
                    backgroundUri = null
                    backgroundLabel = themeBackgroundLabel
                },
                onSave = {
                    showWallpaperEditor = false
                }
            )

        } else {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(tokens.pageBg)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(pad)
                        .background(tokens.pageBg)
                        .verticalScroll(scroll)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {

                    if (!canExact || !canFullScreen) {
                        AlarmPermissionBanner(
                            canExact = canExact,
                            canFullScreen = canFullScreen,
                            onOpenSettings = {
                                if (!canExact) {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                            data = Uri.parse("package:${context.packageName}")
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                    )
                                } else {
                                    context.startActivity(
                                        AlarmPermissionHelper.fullScreenIntentSettingsIntent(
                                            context
                                        )
                                    )
                                }
                            }
                        )
                    }

                    PreviewCard(
                        previewTime = previewTime,
                        nextTriggerText = nextTriggerText,
                        repeatDaily = repeatDaily,
                        silent = silent,
                        vibrate = vibrate,
                        soundLabel = soundLabel,
                        backgroundLabel = backgroundLabel,
                        alarmEnabled = alarmEnabled
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(240.dp)
                    ) {
                        TimeStudioCard(
                            hour24 = hour24,
                            minute = minute,
                            is24Hour = is24Hour,
                            editingPart = editingPart,
                            onPartChange = { editingPart = it },
                            onDecrease = {
                                when (editingPart) {
                                    TimePart.HOUR -> hour24 = wrapHour(hour24 - 1)
                                    TimePart.MINUTE -> minute = wrapMinute(minute - 1)
                                }
                            },
                            onIncrease = {
                                when (editingPart) {
                                    TimePart.HOUR -> hour24 = wrapHour(hour24 + 1)
                                    TimePart.MINUTE -> minute = wrapMinute(minute + 1)
                                }
                            },
                            onAmClick = {
                                if (hour24 >= 12) hour24 -= 12
                            },
                            onPmClick = {
                                if (hour24 < 12) hour24 += 12
                            },
                            onHourSet = { hour24 = it.coerceIn(0, 23) },
                            onMinuteSet = { minute = it.coerceIn(0, 59) }
                        )
                    }

                    OptionsSection(
                        repeatDaily = repeatDaily,
                        silent = silent,
                        vibrate = vibrate,
                        voiceCommandsEnabled = voiceCommandsEnabled,
                        onRepeatDailyChange = { repeatDaily = it },
                        onSilentChange = { silent = it },
                        onVibrateChange = { vibrate = it },
                        onVoiceCommandsChange = { wantsEnabled ->
                            if (!wantsEnabled) {
                                voiceCommandsEnabled = false
                            } else if (
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                            ) {
                                voiceCommandsEnabled = true
                            } else {
                                showVoicePermissionInfo = true
                            }
                        },
                        onVoiceInfoClick = { showVoiceHelp = true }
                    )

                    ReminderMessageSection(
                        message = messageText,
                        onMessageChange = { messageText = it.take(80) }
                    )


                    SoundSection(
                        soundLabel = soundLabel,
                        onSystemTone = {
                            ringtonePickerLauncher.launch(buildRingtonePickerIntent(soundUri))
                        },
                        onChooseFile = {
                            audioPickerLauncher.launch(arrayOf("audio/*"))
                        },
                        onClear = {
                            soundUri = null
                            soundLabel = systemAlarmLabel
                        }
                    )

                    VibrationStyleSection(
                        selected = vibrationStyle,
                        onSelected = {
                            vibrationStyle = it
                        }
                    )

                    WallpaperClockSummarySection(
                        backgroundLabel = backgroundLabel,
                        selectedBackgroundUri = backgroundUri,
                        clockStyle = clockStyle,
                        onOpenEditor = { showWallpaperEditor = true }
                    )

                    Spacer(modifier = Modifier.height(6.dp))
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(tokens.pageBg)
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, tokens.border),
                        colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                            contentColor = tokens.title
                        )
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.sleep_cancel), textAlign = TextAlign.Center)
                        }
                    }

                    Button(
                        onClick = {
                            AlarmStore.save(context, reminder)
                            AlarmCore.schedule(context, reminder)
                            alarmEnabled = true
                            onSave(reminder)
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = tokens.accent,
                            contentColor = tokens.accentOn
                        )
                    ) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.sleep_reminder_save), fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
                        }
                    }
                }

                if (initialReminder != null && onTurnOff != null) {
                    TextButton(
                        onClick = {
                            onTurnOff()
                            hour24 = SleepReminderStore.DEFAULT_DISABLED_HOUR
                            minute = SleepReminderStore.DEFAULT_DISABLED_MINUTE
                            alarmEnabled = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Text(stringResource(R.string.sleep_reminder_turn_off_reminder), color = cs.error)
                    }
                }
            }
        }
    }

    if (showVoicePermissionInfo) {
        AlertDialog(
            onDismissRequest = { showVoicePermissionInfo = false },
            title = { Text(stringResource(R.string.sleep_reminder_voice_commands)) },
            text = { Text(stringResource(R.string.sleep_reminder_voice_permission_explanation)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVoicePermissionInfo = false
                        microphonePermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                ) {
                    Text(stringResource(R.string.sleep_reminder_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = { showVoicePermissionInfo = false }) {
                    Text(stringResource(R.string.sleep_reminder_not_now))
                }
            }
        )
    }

    if (showVoiceHelp) {
        AlertDialog(
            onDismissRequest = { showVoiceHelp = false },
            title = { Text(stringResource(R.string.sleep_reminder_voice_commands)) },
            text = { Text(stringResource(R.string.sleep_reminder_voice_help_body)) },
            confirmButton = {
                TextButton(onClick = { showVoiceHelp = false }) {
                    Text(stringResource(R.string.sleep_got_it))
                }
            }
        )
    }

    if (showVoiceDenied) {
        val activity = context as? Activity
        val permanentlyDenied = activity?.shouldShowRequestPermissionRationale(
            Manifest.permission.RECORD_AUDIO
        ) == false
        AlertDialog(
            onDismissRequest = { showVoiceDenied = false },
            title = { Text(stringResource(R.string.sleep_reminder_microphone_permission_needed)) },
            text = { Text(stringResource(R.string.sleep_reminder_voice_commands_disabled)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showVoiceDenied = false
                        if (permanentlyDenied) {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                            )
                        }
                    }
                ) {
                    Text(
                        if (permanentlyDenied) {
                            stringResource(R.string.sleep_reminder_open_settings)
                        } else {
                            stringResource(R.string.sleep_got_it)
                        }
                    )
                }
            }
        )
    }
}

@Composable
private fun HeaderBlock(
    onClose: () -> Unit,
    canExact: Boolean,
    previewTime: String
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.sleep_reminder_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground
            )
            Text(
                text = if (canExact) {
                    stringResource(R.string.sleep_reminder_header_subtitle_exact)
                } else {
                    stringResource(R.string.sleep_reminder_header_subtitle_needs_exact)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = cs.onSurfaceVariant
            )
            Text(
                text = previewTime,
                style = MaterialTheme.typography.labelMedium,
                color = cs.primary
            )
        }

        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(cs.primaryContainer)
                .clickable { onClose() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = stringResource(R.string.common_close),
                tint = cs.onPrimaryContainer,
                modifier = Modifier.size(22.dp)
            )
        }
    }
}

@Composable
private fun AlarmPermissionBanner(
    canExact: Boolean,
    canFullScreen: Boolean,
    onOpenSettings: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val shape = RoundedCornerShape(20.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sleepReminderCardChrome(tokens, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = tokens.cardBg)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when {
                    !canExact -> stringResource(R.string.sleep_reminder_exact_alarm_access_needed)
                    !canFullScreen -> stringResource(R.string.sleep_reminder_full_screen_access_needed)
                    else -> stringResource(R.string.sleep_reminder_alarm_access_ready)
                },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tokens.title
            )

            Text(
                text = when {
                    !canExact -> stringResource(R.string.sleep_reminder_exact_alarm_access_body)
                    !canFullScreen -> stringResource(R.string.sleep_reminder_full_screen_access_body)
                    else -> stringResource(R.string.sleep_reminder_alarm_access_ready_body)
                },
                style = MaterialTheme.typography.bodySmall,
                color = tokens.muted
            )

            TextButton(onClick = onOpenSettings) {
                Text(
                    if (!canExact) stringResource(R.string.sleep_reminder_allow_exact_alarms) else stringResource(R.string.sleep_reminder_allow_full_screen_alarms),
                    fontWeight = FontWeight.Bold,
                    color = tokens.accent
                )
            }
        }
    }
}

@Composable
private fun PreviewCard(
    previewTime: String,
    nextTriggerText: String,
    repeatDaily: Boolean,
    silent: Boolean,
    vibrate: Boolean,
    soundLabel: String,
    backgroundLabel: String,
    alarmEnabled: Boolean
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val shape = RoundedCornerShape(24.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp)
            .sleepReminderCardChrome(tokens, shape),
        shape = shape,
        colors = CardDefaults.cardColors(
            containerColor = tokens.cardBg
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = stringResource(R.string.sleep_reminder_preview),
                style = MaterialTheme.typography.labelLarge,
                color = tokens.muted
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (alarmEnabled) stringResource(R.string.sleep_reminder_alarm_active) else stringResource(R.string.sleep_reminder_alarm_disabled),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (alarmEnabled) tokens.accent else cs.error
                    )

                    Text(
                        text = when {
                            alarmEnabled && repeatDaily -> stringResource(R.string.sleep_reminder_daily_reminder)
                            alarmEnabled -> stringResource(R.string.sleep_reminder_one_time_reminder)
                            else -> stringResource(R.string.sleep_reminder_no_alarm_scheduled)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.muted
                    )

                    Text(
                        text = if (alarmEnabled) previewTime else "--:--",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tokens.title,
                        maxLines = 1
                    )

                    Text(
                        text = if (alarmEnabled) stringResource(R.string.sleep_reminder_next_ring, nextTriggerText) else stringResource(R.string.sleep_reminder_no_alarm_scheduled),
                        style = MaterialTheme.typography.bodySmall,
                        color = tokens.muted
                    )
                }

                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(if (alarmEnabled) tokens.accentContainer else cs.errorContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (alarmEnabled) tokens.onAccentContainer else cs.onErrorContainer,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            HorizontalDivider(color = tokens.divider)

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PreviewTag(label = if (repeatDaily) stringResource(R.string.sleep_reminder_repeats_daily) else stringResource(R.string.sleep_reminder_one_time), active = repeatDaily)
                PreviewTag(label = if (silent) stringResource(R.string.sleep_reminder_silent_mode) else stringResource(R.string.sleep_reminder_sound_on), active = !silent)
                PreviewTag(label = if (vibrate) stringResource(R.string.sleep_reminder_vibration_on) else stringResource(R.string.sleep_reminder_no_vibration), active = vibrate)
                PreviewTag(label = soundLabel, active = true)
                PreviewTag(label = backgroundLabel, active = true)
            }
        }
    }
}

@Composable
private fun PreviewTag(
    label: String,
    active: Boolean
) {
    val tokens = rememberSleepReminderSurfaceTokens()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(
                if (active) tokens.accentContainer.copy(alpha = 0.92f)
                else tokens.chipBg
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (active) tokens.onAccentContainer else tokens.muted,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1
        )
    }
}

@Composable
private fun TimeStudioCard(
    hour24: Int,
    minute: Int,
    is24Hour: Boolean,
    editingPart: TimePart,
    onPartChange: (TimePart) -> Unit,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit,
    onAmClick: () -> Unit,
    onPmClick: () -> Unit,
    onHourSet: (Int) -> Unit,
    onMinuteSet: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val shape = RoundedCornerShape(28.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sleepReminderCardChrome(tokens, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = tokens.cardBg)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sleep_reminder_time_studio),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = tokens.title
                )
            }

            WheelTimeSelector(
                hour24 = hour24,
                minute = minute,
                onHourSet = onHourSet,
                onMinuteSet = onMinuteSet
            )
        }
    }
}

@Composable
private fun WheelTimeSelector(
    hour24: Int,
    minute: Int,
    onHourSet: (Int) -> Unit,
    onMinuteSet: (Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val isPm = hour24 >= 12
    val hour12 = hourTo12(hour24)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    listOf(
                        if (tokens.isDark) cs.surface.copy(alpha = 0.96f) else tokens.subtleBg,
                        if (tokens.isDark) cs.surfaceVariant.copy(alpha = 0.38f) else tokens.cardBg
                    )
                )
            )
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        WheelPickerColumn(
            values = (1..12).map { "%02d".format(it) },
            selectedIndex = hour12 - 1,
            modifier = Modifier.weight(1f),
            onSelected = { index -> onHourSet(hour12To24(index + 1, isPm)) }
        )
        Text(
            text = ":",
            color = tokens.title,
            fontSize = 42.sp,
            fontWeight = FontWeight.ExtraBold
        )
        WheelPickerColumn(
            values = (0..59).map { "%02d".format(it) },
            selectedIndex = minute,
            modifier = Modifier.weight(1f),
            onSelected = onMinuteSet
        )
        AmPmWheelPicker(
            isPm = isPm,
            modifier = Modifier.width(90.dp),
            onSelected = { wantsPm ->
                if (wantsPm != isPm) {
                    onHourSet(togglePeriod(hour24))
                }
            }
        )
    }
}

@Composable
private fun AmPmWheelPicker(
    isPm: Boolean,
    modifier: Modifier = Modifier,
    onSelected: (Boolean) -> Unit
) {
    val values = listOf("AM", "PM")
    val selectedIndex = if (isPm) 1 else 0
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = selectedIndex
    )
    val itemHeight = 62.dp
    val centeredIndex = remember(
        listState.firstVisibleItemIndex,
        listState.firstVisibleItemScrollOffset,
        listState.layoutInfo.visibleItemsInfo
    ) {
        val viewportCenter =
            (listState.layoutInfo.viewportStartOffset + listState.layoutInfo.viewportEndOffset) / 2
        listState.layoutInfo.visibleItemsInfo
            .minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2
                kotlin.math.abs(itemCenter - viewportCenter)
            }
            ?.index
            ?: selectedIndex
    }.coerceIn(0, values.lastIndex)

    LaunchedEffect(selectedIndex) {
        if (centeredIndex != selectedIndex && !listState.isScrollInProgress) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            if (centeredIndex != selectedIndex) {
                onSelected(centeredIndex == 1)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .height(186.dp)
            .clip(RoundedCornerShape(22.dp)),
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        horizontalAlignment = Alignment.CenterHorizontally,
        userScrollEnabled = true,
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = itemHeight)
    ) {
        items(values.size) { valueIndex ->
            val active = valueIndex == centeredIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight),
                contentAlignment = Alignment.Center
            ) {
                WheelText(
                    text = values[valueIndex],
                    active = active,
                    onClick = { onSelected(valueIndex == 1) }
                )
            }
        }
    }
}

@Composable
private fun WheelPickerColumn(
    values: List<String>,
    selectedIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val repeatCount = 7
    val cycle = values.size
    val base = (repeatCount / 2) * cycle
    val startIndex = base + selectedIndex.coerceIn(0, cycle - 1)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (startIndex - 1).coerceAtLeast(0))
    val isSmallList = values.size <= 2

    LaunchedEffect(selectedIndex, cycle) {
        val currentCenter = (listState.firstVisibleItemIndex + 1).floorMod(cycle)
        if (currentCenter != selectedIndex.floorMod(cycle) && !listState.isScrollInProgress) {
            listState.scrollToItem((base + selectedIndex - 1).coerceAtLeast(0))
        }
    }

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val centered = (listState.firstVisibleItemIndex + 1).floorMod(cycle)
            if (centered != selectedIndex.floorMod(cycle)) {
                onSelected(centered)
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxHeight(),
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(listState),
        userScrollEnabled = true
    ) {
        items(
            if (isSmallList) values.size
            else repeatCount * cycle
        ) { rawIndex ->
            val valueIndex =
                if (isSmallList) rawIndex
                else rawIndex.floorMod(cycle)
            val centerIndex = listState.firstVisibleItemIndex + 1
            val active = rawIndex == centerIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp),
                contentAlignment = Alignment.Center
            ) {
                WheelText(
                    text = values[valueIndex],
                    active = active,
                    onClick = {
                        onSelected(valueIndex)
                    }
                )
            }
        }
    }
}

@Composable
private fun WheelText(
    text: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val tokens = rememberSleepReminderSurfaceTokens()

    val activeColor = if (tokens.isDark) Color.White else tokens.accent
    val inactiveColor = if (tokens.isDark) {
        Color.White.copy(alpha = 0.22f)
    } else {
        tokens.muted.copy(alpha = 0.45f)
    }

    Text(
        text = text,
        modifier = Modifier.clickable(onClick = onClick),
        color = if (active) activeColor else inactiveColor,
        fontSize = if (active) 44.sp else 32.sp,
        lineHeight = if (active) 52.sp else 38.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        maxLines = 1
    )
}

private fun Int.floorMod(divisor: Int): Int = ((this % divisor) + divisor) % divisor

private fun hourTo12(hour24: Int): Int {
    val raw = hour24 % 12
    return if (raw == 0) 12 else raw
}

private fun wrap12(value: Int): Int {
    val raw = ((value - 1) % 12 + 12) % 12 + 1
    return raw
}

private fun hour12To24(hour12: Int, isPm: Boolean): Int {
    val base = if (hour12 == 12) 0 else hour12
    return if (isPm) base + 12 else base
}

private fun togglePeriod(hour24: Int): Int {
    return if (hour24 >= 12) hour24 - 12 else hour24 + 12
}



@Composable
private fun TimeNumberField(
    label: String,
    value: String,
    onValue: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    TextField(
        value = value,
        onValueChange = onValue,
        modifier = modifier,
        singleLine = true,
        label = { Text(label) },
        shape = RoundedCornerShape(16.dp),
        textStyle = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center
        ),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = cs.surfaceVariant.copy(alpha = 0.32f),
            unfocusedContainerColor = cs.surfaceVariant.copy(alpha = 0.32f),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        )
    )
}




@Composable
private fun OptionsSection(
    repeatDaily: Boolean,
    silent: Boolean,
    vibrate: Boolean,
    voiceCommandsEnabled: Boolean,
    onRepeatDailyChange: (Boolean) -> Unit,
    onSilentChange: (Boolean) -> Unit,
    onVibrateChange: (Boolean) -> Unit,
    onVoiceCommandsChange: (Boolean) -> Unit,
    onVoiceInfoClick: () -> Unit
) {
    val tokens = rememberSleepReminderSurfaceTokens()

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.sleep_reminder_options),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.title
        )

        SettingRow(
            title = stringResource(R.string.sleep_reminder_repeat_daily),
            subtitle = stringResource(R.string.sleep_reminder_repeat_daily_subtitle),
            icon = Icons.Default.Alarm,
            checked = repeatDaily,
            onCheckedChange = onRepeatDailyChange
        )

        SettingRow(
            title = stringResource(R.string.sleep_reminder_silent_notification),
            subtitle = stringResource(R.string.sleep_reminder_silent_notification_subtitle),
            icon = Icons.Default.VolumeOff,
            checked = silent,
            onCheckedChange = onSilentChange
        )

        SettingRow(
            title = stringResource(R.string.sleep_reminder_vibration),
            subtitle = stringResource(R.string.sleep_reminder_vibration_subtitle),
            icon = Icons.Default.Vibration,
            checked = vibrate,
            onCheckedChange = onVibrateChange
        )

        SettingRow(
            title = stringResource(R.string.sleep_reminder_voice_commands),
            subtitle = stringResource(R.string.sleep_reminder_voice_commands_subtitle),
            footnote = stringResource(R.string.sleep_reminder_voice_requires_microphone),
            icon = Icons.Default.Mic,
            checked = voiceCommandsEnabled,
            onCheckedChange = onVoiceCommandsChange,
            onInfoClick = onVoiceInfoClick
        )
    }
}

@Composable
private fun SettingRow(
    title: String,
    subtitle: String,
    footnote: String? = null,
    icon: ImageVector,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onInfoClick: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val shape = RoundedCornerShape(22.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .sleepReminderCardChrome(tokens, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = tokens.cardBg)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(if (checked) tokens.accentContainer else tokens.subtleBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (checked) tokens.onAccentContainer else tokens.muted,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = tokens.title
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = tokens.muted
                )
                if (footnote != null) {
                    Text(
                        text = footnote,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = tokens.accent
                    )
                }
            }

            if (onInfoClick != null) {
                IconButton(onClick = onInfoClick, modifier = Modifier.size(38.dp)) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = tokens.muted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = tokens.accentOn,
                    checkedTrackColor = tokens.accent,
                    uncheckedThumbColor = tokens.muted,
                    uncheckedTrackColor = tokens.subtleBg
                )
            )
        }
    }
}

@Composable
private fun SoundSection(
    soundLabel: String,
    onSystemTone: () -> Unit,
    onChooseFile: () -> Unit,
    onClear: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val shape = RoundedCornerShape(24.dp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.sleep_reminder_alarm_sound),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.title
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .sleepReminderCardChrome(tokens, shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = tokens.cardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(tokens.accentContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.MusicNote, null, tint = tokens.onAccentContainer)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = soundLabel,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = tokens.title,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(R.string.sleep_reminder_alarm_sound_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = tokens.muted
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ChipButton(
                        label = stringResource(R.string.sleep_reminder_system_ringtone),
                        icon = Icons.Default.Notifications,
                        onClick = onSystemTone,
                        modifier = Modifier.weight(1f)
                    )
                    ChipButton(
                        label = stringResource(R.string.sleep_reminder_audio_file),
                        icon = Icons.Default.MusicNote,
                        onClick = onChooseFile,
                        modifier = Modifier.weight(1f)
                    )
                }

                TextButton(onClick = onClear) {
                    Text(
                        stringResource(R.string.sleep_reminder_use_default_sound),
                        color = tokens.accent
                    )
                }
            }
        }
    }
}

@Composable
private fun WallpaperClockSummarySection(
    backgroundLabel: String,
    selectedBackgroundUri: String?,
    clockStyle: String,
    onOpenEditor: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val selectedPreset = AlarmBackgroundPresets.find(selectedBackgroundUri)
    val previewBrush = selectedPreset?.let { presetBrush(it.id, cs) }
        ?: defaultSleepReminderPreviewBrush(cs, tokens)
    val overlayPrimary = if (selectedPreset != null || tokens.isDark) Color.White else tokens.title
    val overlaySecondary = overlayPrimary.copy(alpha = if (tokens.isDark || selectedPreset != null) 0.78f else 0.72f)
    val chipOverlay = if (selectedPreset != null || tokens.isDark) {
        Color.White.copy(alpha = 0.18f)
    } else {
        tokens.accentContainer.copy(alpha = 0.92f)
    }
    val chipText = if (selectedPreset != null || tokens.isDark) Color.White else tokens.onAccentContainer

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.sleep_reminder_wallpaper_clock),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.title
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(164.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(previewBrush)
                .clickable(onClick = onOpenEditor)
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.BottomStart),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = backgroundLabel,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = overlayPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = stringResource(R.string.sleep_reminder_clock_style_format, AlarmClockStyles.titleFor(clockStyle)),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = overlaySecondary
                )
            }

            Text(
                text = stringResource(R.string.sleep_reminder_customize),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(RoundedCornerShape(999.dp))
                    .background(chipOverlay)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                color = chipText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun WallpaperClockScreen(
    backgroundLabel: String,
    selectedBackgroundUri: String?,
    clockStyle: String,
    hour24: Int,
    minute: Int,
    message: String,
    onDismiss: () -> Unit,
    onPresetSelected: (AlarmBackgroundPresets.Preset) -> Unit,
    onClockStyleSelected: (AlarmClockStyles.Style) -> Unit,
    onRandomPreset: () -> Unit,
    onChooseImage: () -> Unit,
    onClear: () -> Unit,
    onSave: () -> Unit
) {
    val tokens = rememberSleepReminderSurfaceTokens()

    Scaffold(
        containerColor = tokens.pageBg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.sleep_reminder_wallpaper_clock_title),
                        color = tokens.title
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.sleep_back),
                            tint = tokens.title
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onChooseImage) {
                        Text(
                            stringResource(R.string.sleep_reminder_background),
                            color = tokens.accent
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = tokens.pageBg
                )
            )
        }
    ) { pad ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(pad)
                .background(tokens.pageBg)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            WallpaperClockLivePreview(
                backgroundLabel = backgroundLabel,
                selectedBackgroundUri = selectedBackgroundUri,
                clockStyle = clockStyle,
                hour24 = hour24,
                minute = minute,
                message = message
            )

            BackgroundSection(
                backgroundLabel = backgroundLabel,
                selectedBackgroundUri = selectedBackgroundUri,
                clockStyle = clockStyle,
                onClockStyleSelected = onClockStyleSelected,
                onPresetSelected = onPresetSelected,
                onRandomPreset = onRandomPreset,
                onChooseImage = onChooseImage,
                onClear = onClear
            )

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(18.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = tokens.accent,
                    contentColor = tokens.accentOn
                )
            ) {
                Text(stringResource(R.string.sleep_reminder_save), fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun defaultSleepReminderPreviewBrush(
    cs: ColorScheme,
    tokens: SleepReminderSurfaceTokens
): Brush {
    return if (tokens.isDark) {
        Brush.verticalGradient(
            listOf(
                Color(0xFF15002D),
                Color(0xFF402C7A),
                Color(0xFFA77963)
            )
        )
    } else {
        Brush.linearGradient(
            listOf(
                Color(0xFFE8F2F6),
                Color(0xFFC5DCE6),
                Color(0xFF8FB3C3)
            )
        )
    }
}

@Composable
private fun WallpaperClockLivePreview(
    backgroundLabel: String,
    selectedBackgroundUri: String?,
    clockStyle: String,
    hour24: Int,
    minute: Int,
    message: String
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val selectedPreset = AlarmBackgroundPresets.find(selectedBackgroundUri)
    val backgroundBitmap = rememberBackgroundBitmap(selectedBackgroundUri)

    val previewBrush = selectedPreset?.let { presetBrush(it.id, cs) }
        ?: defaultSleepReminderPreviewBrush(cs, tokens)

    val previewReminder = SleepReminder(
        hour = hour24,
        minute = minute,
        message = message.ifBlank { stringResource(R.string.sleep_reminder_time_to_wind_down) },
        clockStyle = clockStyle
    )

    val dateText = remember {
        SimpleDateFormat("EEE, MMMM d", Locale.getDefault()).format(Date())
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        AlarmDevicePreview(
            device = AlarmPreviewDevice.PHONE,
            brush = previewBrush,
            backgroundBitmap = backgroundBitmap,
            reminder = previewReminder,
            dateText = dateText,
            modifier = Modifier.weight(0.72f),
            height = 310.dp
        )

        AlarmDevicePreview(
            device = AlarmPreviewDevice.TABLET,
            brush = previewBrush,
            backgroundBitmap = backgroundBitmap,
            reminder = previewReminder,
            dateText = dateText,
            modifier = Modifier.weight(1f),
            height = 250.dp
        )
    }
}

private enum class AlarmPreviewDevice { PHONE, TABLET }

@Composable
private fun AlarmDevicePreview(
    device: AlarmPreviewDevice,
    brush: Brush,
    backgroundBitmap: Bitmap?,
    reminder: SleepReminder,
    dateText: String,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp
) {
    val label = when (device) {
        AlarmPreviewDevice.PHONE -> stringResource(R.string.sleep_reminder_preview_phone)
        AlarmPreviewDevice.TABLET -> stringResource(R.string.sleep_reminder_preview_tablet)
    }
    val panelWidth = if (device == AlarmPreviewDevice.PHONE) 0.84f else 0.80f
    val panelHeight = if (device == AlarmPreviewDevice.PHONE) 160.dp else 140.dp
    val clockBoxHeight = if (device == AlarmPreviewDevice.PHONE) 66.dp else 58.dp

    Box(
        modifier = modifier
            .height(height)
            .clip(RoundedCornerShape(30.dp))
            .background(brush)
    ) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.14f))
        )

        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 14.dp)
                .fillMaxWidth(panelWidth)
                .height(panelHeight)
                .clip(RoundedCornerShape(22.dp))
                .background(Color.Black.copy(alpha = 0.20f))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (reminder.clockStyle == AlarmClockStyles.STYLE_STACKED) 78.dp else clockBoxHeight),
                    contentAlignment = Alignment.Center
                ) {
                    MiniClockDisplay(reminder, Color.White)
                }

                Text(
                    text = dateText,
                    color = Color.White.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 0.dp)
                )

                Text(
                    text = reminder.message,
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Text(
            text = label,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
            color = Color.White.copy(alpha = 0.72f),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun MiniClockDisplay(
    reminder: SleepReminder,
    color: Color
) {
    val time = reminder.formattedTime
    val parts = time.split(":")

    when (reminder.clockStyle) {

        AlarmClockStyles.STYLE_STACKED -> {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy((-6).dp)
            ) {
                Text(
                    text = parts.getOrNull(0) ?: "--",
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )

                Text(
                    text = parts.getOrNull(1) ?: "--",
                    fontSize = 28.sp,
                    lineHeight = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = color
                )
            }
        }

        AlarmClockStyles.STYLE_COMPACT -> {
            Text(
                text = time,
                fontSize = 29.sp,
                lineHeight = 29.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }

        AlarmClockStyles.STYLE_CLASSIC -> {
            Text(
                text = time,
                fontSize = 28.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.Bold,
                color = color,
                textAlign = TextAlign.Center
            )
        }

        AlarmClockStyles.STYLE_NUMERAL -> {
            Text(
                text = time.replace(":", " "),
                fontSize = 26.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.Light,
                color = color,
                textAlign = TextAlign.Center
            )
        }

        else -> {
            Text(
                text = time,
                fontSize = 28.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun BackgroundSection(
    backgroundLabel: String,
    selectedBackgroundUri: String?,
    clockStyle: String,
    onClockStyleSelected: (AlarmClockStyles.Style) -> Unit,
    onPresetSelected: (AlarmBackgroundPresets.Preset) -> Unit,
    onRandomPreset: () -> Unit,
    onChooseImage: () -> Unit,
    onClear: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val shape = RoundedCornerShape(28.dp)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.sleep_reminder_wallpaper_clock),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.title
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .sleepReminderCardChrome(tokens, shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = tokens.cardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WallpaperPreviewStrip(
                    backgroundLabel = backgroundLabel,
                    selectedBackgroundUri = selectedBackgroundUri,
                    onPresetSelected = onPresetSelected
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sleep_reminder_built_in_wallpapers),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.muted
                    )

                    AlarmBackgroundPresets.all.chunked(2).forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            row.forEach { preset ->
                                PresetBackgroundButton(
                                    preset = preset,
                                    selected = selectedBackgroundUri == preset.uriString,
                                    onClick = { onPresetSelected(preset) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (row.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                HorizontalDivider(color = tokens.divider)

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sleep_reminder_clock_style),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tokens.muted
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        AlarmClockStyles.all.forEach { style ->
                            ClockStyleChip(
                                style = style,
                                selected = clockStyle == style.id,
                                onClick = { onClockStyleSelected(style) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    ChipButton(
                        label = stringResource(R.string.sleep_reminder_random_wallpaper),
                        icon = Icons.Default.NotificationsNone,
                        onClick = onRandomPreset,
                        modifier = Modifier.weight(1f)
                    )
                    ChipButton(
                        label = stringResource(R.string.sleep_reminder_choose_image),
                        icon = Icons.Default.Image,
                        onClick = onChooseImage,
                        modifier = Modifier.weight(1f)
                    )
                }

                TextButton(onClick = onClear, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                    Text(stringResource(R.string.sleep_reminder_use_automatic_wallpaper))
                }
            }
        }
    }
}

@Composable
private fun ReminderMessageSection(
    message: String,
    onMessageChange: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val shape = RoundedCornerShape(24.dp)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.sleep_reminder_message_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.title
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .sleepReminderCardChrome(tokens, shape),
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = tokens.cardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = false,
                    maxLines = 2,
                    shape = RoundedCornerShape(18.dp),
                    placeholder = {
                        Text(
                            stringResource(R.string.sleep_reminder_time_to_wind_down),
                            color = tokens.muted
                        )
                    },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = tokens.subtleBg,
                        unfocusedContainerColor = tokens.subtleBg,
                        disabledContainerColor = tokens.subtleBg.copy(alpha = 0.65f),
                        focusedTextColor = tokens.body,
                        unfocusedTextColor = tokens.body,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    )
                )
                Text(
                    text = "${message.length}/80",
                    style = MaterialTheme.typography.labelSmall,
                    color = tokens.muted,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@Composable
private fun WallpaperPreviewStrip(
    backgroundLabel: String,
    selectedBackgroundUri: String?,
    onPresetSelected: (AlarmBackgroundPresets.Preset) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val tokens = rememberSleepReminderSurfaceTokens()
    val selectedPreset = AlarmBackgroundPresets.find(selectedBackgroundUri)
    val previewBrush = selectedPreset?.let { presetBrush(it.id, cs) }
        ?: defaultSleepReminderPreviewBrush(cs, tokens)
    val overlayPrimary = if (selectedPreset != null || tokens.isDark) Color.White else tokens.title
    val overlaySecondary = overlayPrimary.copy(alpha = if (selectedPreset != null || tokens.isDark) 0.78f else 0.72f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(128.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(previewBrush)
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = backgroundLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = overlayPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (selectedPreset != null) stringResource(R.string.sleep_reminder_built_in_wallpaper) else stringResource(R.string.sleep_reminder_gallery_or_auto_wallpaper),
                style = MaterialTheme.typography.labelMedium,
                color = overlaySecondary
            )
        }

        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            AlarmBackgroundPresets.all.take(3).forEach { preset ->
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(presetBrush(preset.id, cs))
                        .clickable { onPresetSelected(preset) }
                )
            }
        }
    }
}

@Composable
private fun PresetBackgroundButton(
    preset: AlarmBackgroundPresets.Preset,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    val brush = presetBrush(preset.id, cs)

    Box(
        modifier = modifier
            .height(54.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(brush)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(
            text = preset.localizedTitle(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            maxLines = 1
        )
    }
}

@Composable
private fun ClockStyleChip(
    style: AlarmClockStyles.Style,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = rememberSleepReminderSurfaceTokens()

    Box(
        modifier = modifier
            .height(48.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) tokens.accentContainer else tokens.chipBg)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(style.titleRes),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = if (selected) tokens.onAccentContainer else tokens.muted,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}


@Composable
private fun ChipButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tokens = rememberSleepReminderSurfaceTokens()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(tokens.accentContainer.copy(alpha = 0.88f))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tokens.onAccentContainer, modifier = Modifier.size(18.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = tokens.onAccentContainer,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun AlarmRingScreen(
    onDismissOnly: () -> Unit,
    onSnooze: () -> Unit,
    onTurnOff: () -> Unit,
    showVoiceCommandHint: Boolean = false
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current
    val reminder = remember { SleepReminderStore.get(context) }
    val fallbackPreset = remember { AlarmBackgroundPresets.random() }
    val backgroundUri = reminder?.effectiveBackgroundUri ?: fallbackPreset.uriString

    val backgroundBitmap = rememberBackgroundBitmap(backgroundUri)
    val backgroundPreset = AlarmBackgroundPresets.find(backgroundUri)
    val fallbackBrush = if (backgroundPreset != null) {
        presetBrush(backgroundPreset.id, cs)
    } else {
        presetBrush(fallbackPreset.id, cs)
    }

    val isDark = cs.background.red + cs.background.green + cs.background.blue < 1.5f
    val panelColor = if (isDark) Color.Black.copy(alpha = 0.34f) else Color.White.copy(alpha = 0.22f)
    val message = reminder?.message?.takeIf { it.isNotBlank() } ?: stringResource(R.string.sleep_reminder_time_to_wind_down)
    val dateText = remember {
        SimpleDateFormat("EEE, MMMM d", Locale.getDefault()).format(Date())
    }

    BackHandler(enabled = true) { }

    Box(modifier = Modifier.fillMaxSize()) {
        if (backgroundBitmap != null) {
            Image(
                bitmap = backgroundBitmap.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(fallbackBrush)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (isDark) 0.38f else 0.22f))
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp, vertical = 26.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.18f))
                        .clickable(onClick = onDismissOnly),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(34.dp),
                    color = panelColor,
                    tonalElevation = 0.dp,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ClockDisplay(reminder = reminder, color = Color.White)
                        Text(
                            text = dateText,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.82f),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = message,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.92f),
                            textAlign = TextAlign.Center,
                            lineHeight = 27.sp,
                            maxLines = 3
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AlarmTag(AlarmClockStyles.titleFor(reminder?.clockStyle ?: AlarmClockStyles.STYLE_STACKED))
                            AlarmTag(if (reminder?.repeatDaily == true) stringResource(R.string.sleep_reminder_daily) else stringResource(R.string.sleep_reminder_once))
                            AlarmTag(if (reminder?.silent == true) stringResource(R.string.sleep_reminder_silent) else stringResource(R.string.sleep_reminder_sound))
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (showVoiceCommandHint) {
                    VoiceCommandBadge()
                }
                DragDismissControl(
                    onSnooze = onSnooze,
                    onDismiss = onDismissOnly,
                    onTurnOff = onTurnOff
                )
            }
        }
    }
}

@Composable
private fun VoiceCommandBadge() {
    val pulse by rememberInfiniteTransition(label = "voice-command-glow").animateFloat(
        initialValue = 0.96f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "voice-command-scale"
    )
    Box(
        modifier = Modifier
            .scale(pulse)
            .clip(RoundedCornerShape(999.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF00D7FF).copy(alpha = 0.42f),
                        Color(0xFF1E1B2E).copy(alpha = 0.94f),
                        Color(0xFF9D5CFF).copy(alpha = 0.36f)
                    )
                )
            )
            .padding(2.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(Color.Black.copy(alpha = 0.62f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Mic,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.86f),
                modifier = Modifier.size(17.dp)
            )
            Text(
                text = stringResource(R.string.sleep_reminder_voice_say_stop_or_snooze),
                color = Color.White,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ClockDisplay(
    reminder: SleepReminder?,
    color: Color
) {
    val time = reminder?.formattedTime ?: "--:--"
    val parts = time.split(":")

    when (reminder?.clockStyle ?: AlarmClockStyles.STYLE_STACKED) {
        AlarmClockStyles.STYLE_COMPACT -> Text(
            text = time,
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.ExtraBold,
            color = color,
            letterSpacing = 0.sp
        )

        AlarmClockStyles.STYLE_CLASSIC -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = time,
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.SemiBold,
                color = color,
                letterSpacing = 0.sp
            )
            Text(
                text = stringResource(R.string.sleep_reminder_title),
                style = MaterialTheme.typography.labelLarge,
                color = color.copy(alpha = 0.72f)
            )
        }

        AlarmClockStyles.STYLE_NUMERAL -> Text(
            text = time.replace(":", "  "),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Light,
            color = color,
            letterSpacing = 0.sp
        )

        else -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = parts.getOrNull(0) ?: "--",
                fontSize = 58.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color
            )
            Text(
                text = parts.getOrNull(1) ?: "--",
                fontSize = 58.sp,
                lineHeight = 56.sp,
                fontWeight = FontWeight.ExtraBold,
                color = color.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun DragDismissControl(
    onSnooze: () -> Unit,
    onDismiss: () -> Unit,
    onTurnOff: () -> Unit
) {
    var dragX by remember { mutableFloatStateOf(0f) }
    val threshold = 120f

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.sleep_reminder_off),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )

            Box(
                modifier = Modifier
                    .size(74.dp)
                    .offset { IntOffset(dragX.roundToInt(), 0) }
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.22f))
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = {
                                when {
                                    dragX > threshold -> onDismiss()
                                    dragX < -threshold -> onTurnOff()
                                }
                                dragX = 0f
                            },
                            onDragCancel = { dragX = 0f },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                dragX = (dragX + dragAmount.x).coerceIn(-170f, 170f)
                            }
                        )
                    }
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = stringResource(R.string.sleep_reminder_stop),
                color = Color.White.copy(alpha = 0.72f),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
        }

        TextButton(onClick = onSnooze) {
            Icon(Icons.Default.Snooze, contentDescription = null, tint = Color.White.copy(alpha = 0.82f))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.sleep_reminder_snooze_5_min),
                color = Color.White.copy(alpha = 0.86f),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun AlarmTag(text: String) {
    Surface(
        shape = RoundedCornerShape(999.dp),
        color = Color.White.copy(alpha = 0.14f),
        tonalElevation = 0.dp
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            color = Color.White,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun rememberBackgroundBitmap(uriString: String?): Bitmap? {
    val context = LocalContext.current

    return produceState<Bitmap?>(initialValue = null, uriString) {
        value = if (uriString.isNullOrBlank() || AlarmBackgroundPresets.find(uriString) != null) {
            null
        } else {
            try {
                val uri = Uri.parse(uriString)
                context.contentResolver.openInputStream(uri)?.use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            } catch (_: Exception) {
                null
            }
        }
    }.value
}

@Composable
private fun presetBrush(
    id: String,
    cs: androidx.compose.material3.ColorScheme
): Brush {
    return when (id) {
        "aurora" -> Brush.linearGradient(
            listOf(Color(0xFF10213F), Color(0xFF4B8D8E), Color(0xFFFFC27A))
        )
        "dawn" -> Brush.verticalGradient(
            listOf(Color(0xFFFFB199), Color(0xFFFFE1A8), Color(0xFF88C9F9))
        )
        "midnight" -> Brush.linearGradient(
            listOf(Color(0xFF070B1E), Color(0xFF232A63), Color(0xFF7B5EA7))
        )
        "forest" -> Brush.linearGradient(
            listOf(Color(0xFF0C2D24), Color(0xFF2E7D5F), Color(0xFFA7D8A2))
        )
        "ocean" -> Brush.verticalGradient(
            listOf(Color(0xFF06283D), Color(0xFF1363DF), Color(0xFF47B5FF))
        )
        else -> Brush.linearGradient(
            listOf(
                cs.background,
                cs.surfaceVariant.copy(alpha = 0.95f),
                cs.background
            )
        )
    }
}

@Composable
private fun VibrationStyleSection(
    selected: String,
    onSelected: (String) -> Unit
) {
    val tokens = rememberSleepReminderSurfaceTokens()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.sleep_reminder_alarm_vibration),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = tokens.title
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            listOf(
                "soft" to stringResource(R.string.sleep_reminder_vibration_soft),
                "normal" to stringResource(R.string.sleep_reminder_vibration_normal),
                "strong" to stringResource(R.string.sleep_reminder_vibration_strong)
            ).forEach { (id, label) ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            if (selected == id) tokens.accentContainer else tokens.chipBg
                        )
                        .clickable { onSelected(id) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontWeight = FontWeight.Bold,
                        color = if (selected == id) tokens.onAccentContainer else tokens.muted
                    )
                }
            }
        }
    }
}



private enum class TimePart {
    HOUR,
    MINUTE
}

private fun buildRingtonePickerIntent(currentSoundUri: String?): Intent {
    return Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
        putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
        putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
        putExtra(
            RingtoneManager.EXTRA_RINGTONE_EXISTING_URI,
            currentSoundUri?.let { Uri.parse(it) }
        )
    }
}

private fun queryRingtoneLabel(context: Context, uri: Uri): String? {
    return try {
        RingtoneManager.getRingtone(context, uri)?.getTitle(context)
    } catch (_: Exception) {
        null
    }
}



private fun persistReadPermission(context: Context, uri: Uri) {
    try {
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )
    } catch (_: Exception) {
    }
}



private fun formatPreviewTime(hour24: Int, minute: Int, is24Hour: Boolean): String {
    return if (is24Hour) {
        "%02d:%02d".format(hour24, minute)
    } else {
        val displayHour = displayHour(hour24, is24Hour)
        val period = if (hour24 >= 12) "PM" else "AM"
        "$displayHour:%02d $period".format(minute)
    }
}

private fun displayHour(hour24: Int, is24Hour: Boolean): String {
    return if (is24Hour) {
        "%02d".format(hour24)
    } else {
        val h = hour24 % 12
        val display = if (h == 0) 12 else h
        "%02d".format(display)
    }
}

private fun wrapHour(value: Int): Int = ((value % 24) + 24) % 24

private fun wrapMinute(value: Int): Int = ((value % 60) + 60) % 60
