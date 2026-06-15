package com.example.stepforge.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.AboutActivity
import com.example.stepforge.FeedbackActivity
import com.example.stepforge.PrivacySecurityActivity
import com.example.stepforge.ProfileSettingsActivity
import com.example.stepforge.R
import com.example.stepforge.SettingsActivity
import com.example.stepforge.SleepActivity
import com.example.stepforge.StepCounterService
import com.example.stepforge.SyncBackupActivity
import com.example.stepforge.TargetActivity
import com.example.stepforge.WaterReminderActivity
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.core.AppLanguageHelper
import com.example.stepforge.notification.WaterReminderScheduler
import com.example.stepforge.steps.StepEvents
import com.example.stepforge.ui.components.CustomTimePicker
import com.example.stepforge.ui.components.HealthConnectState
import com.example.stepforge.ui.components.HealthSyncManager
import com.example.stepforge.ui.streak.StreakShieldPrefs
import com.example.stepforge.widget.StepWidgetCompactProvider
import com.example.stepforge.widget.StepWidgetLargeProvider
import com.example.stepforge.widget.StepWidgetProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Calendar

enum class IntegrationSheet { HealthConnect }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    activity: SettingsActivity,
    onLaunchAndroidPermission: () -> Unit,
    onScheduleReminder: (Int, Int) -> Unit = { _, _ -> }
) {
    val scope = rememberCoroutineScope()

    // ✅ Neon aynı kalsın
    val neon = Brush.linearGradient(listOf(Color(0xFF00FFA3), Color(0xFF00F5FF)))

    val USERNAME = stringPreferencesKey("username")
    val STEP_GOAL = intPreferencesKey("step_goal")
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val NOTIF_TIME = stringPreferencesKey("notif_time")

    var username by remember { mutableStateOf("") }
    var age by remember { mutableStateOf(0) }
    var gender by remember { mutableStateOf("") }
    var goal by remember { mutableStateOf(10000) }
    var themeMode by remember { mutableStateOf("system") }
    var notifTime by remember { mutableStateOf("09:00") }
    var showLanguageDialog by remember { mutableStateOf(false) }

    // ✅ Theme hesapla (dark mı light mı?)
    val isDark = when (themeMode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }

    // ✅ Soft light palet (göz yormasın)
    val screenBg = if (isDark) Color(0xFF0B0B0E) else Color(0xFFF5F7FA)
    val topBarBg = if (isDark) Color(0xFF0B0B0E) else Color(0xFFF5F7FA)
    val textMain = if (isDark) Color.White else Color(0xFF1A202C)
    val textSub = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF1A202C).copy(alpha = 0.72f)

    // ✅ Satır/kart içi yüzey
    val rowBg = if (isDark) Color(0xFF101218) else Color(0xFFFFFFFF)
    val rowBorder = if (isDark) Color.Transparent else Color(0x1A1A202C)

    val openInfoCard = remember { mutableStateOf<String?>(null) }
    var infoAnchor by remember { mutableStateOf<Rect?>(null) }
    var infoText by remember { mutableStateOf("") }
    var rootSize by remember { mutableStateOf(IntSize.Zero) }
    var rootWindowOffset by remember { mutableStateOf(Offset.Zero) }
    val hcStatus by HealthConnectState.status.collectAsState()
    val isHealthConnected = hcStatus.permissionsGranted
    var sheet by rememberSaveable { mutableStateOf<IntegrationSheet?>(null) }

    val context = LocalContext.current
    val healthManager = remember { HealthSyncManager(context) }
    val currentSteps by StepEvents.todaySteps.collectAsState()

    LaunchedEffect(Unit) {
        activity.stepforgeStore.data.collect { prefs ->
            username = prefs[USERNAME] ?: activity.getString(R.string.hc_user)
            goal = prefs[STEP_GOAL] ?: 10000
            gender = prefs[stringPreferencesKey("gender")] ?: ""
            themeMode = prefs[THEME_MODE] ?: "system"
            notifTime = prefs[NOTIF_TIME] ?: "09:00"

            val birth = prefs[stringPreferencesKey("birth_date")] ?: ""
            age = if (birth.isNotEmpty()) {
                try {
                    val parts = birth.split("-", "/", ".", " ").filter { it.isNotBlank() }
                    val (day, month, year) = when {
                        parts.size == 3 && parts[0].length == 4 ->
                            Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                        parts.size == 3 ->
                            Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                        else -> Triple(0, 0, 0)
                    }

                    val calNow = Calendar.getInstance()
                    val yNow = calNow.get(Calendar.YEAR)
                    val mNow = calNow.get(Calendar.MONTH) + 1
                    val dNow = calNow.get(Calendar.DAY_OF_MONTH)

                    var res = yNow - year
                    if (mNow < month || (mNow == month && dNow < day)) res--
                    if (res < 0) 0 else res
                } catch (_: Exception) {
                    0
                }
            } else 0
        }
        healthManager.checkAndRequestPermissions()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.hc_settings), color = textMain) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.hc_back),
                            tint = textMain
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = topBarBg)
            )
        }
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .background(screenBg)
                .padding(pad)
                .onGloballyPositioned {
                    rootSize = it.size
                    rootWindowOffset = it.positionInWindow()
                }
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // 1) Account
                SettingBlock(
                    icon = Icons.Outlined.Person,
                    title = stringResource(R.string.hc_account),
                    infoText = stringResource(R.string.hc_account_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    darkTheme = isDark
                ) {
                    Text(stringResource(R.string.hc_username_format, username), color = textMain, fontSize = 16.sp)
                    Text(
                        text = if (age > 0) stringResource(R.string.hc_age_format, age) else stringResource(R.string.hc_age_not_set),
                        color = textSub,
                        fontSize = 15.sp
                    )
                    Text(
                        stringResource(R.string.hc_gender_format, if (gender.isNotEmpty()) gender else stringResource(R.string.hc_not_set)),
                        color = textSub,
                        fontSize = 15.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            activity.startActivity(
                                Intent(activity, ProfileSettingsActivity::class.java)
                            )
                        },
                        shape = RoundedCornerShape(30),
                        border = BorderStroke(1.dp, neon)
                    ) { Text(stringResource(R.string.hc_edit_profile), color = Color(0xFF00F5FF)) }
                }

                // 2) Notifications
                SettingItem(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.hc_notifications),
                    infoText = stringResource(R.string.hc_notification_time_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    var showPicker by remember { mutableStateOf(false) }
                    Text(stringResource(R.string.hc_reminder_at_format, notifTime), color = textMain, fontSize = 16.sp)
                    Spacer(Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showPicker = true },
                        border = BorderStroke(1.dp, neon),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(stringResource(R.string.hc_set_time), color = Color(0xFF00F5FF))
                    }

                    if (showPicker) {
                        val parts = notifTime.split(":")
                        val initH = parts.getOrNull(0)?.toIntOrNull() ?: 9
                        val initM = parts.getOrNull(1)?.toIntOrNull() ?: 0
                        CustomTimePicker(
                            initialHour = initH,
                            initialMinute = initM,
                            onConfirm = { h, m ->
                                val t = "%02d:%02d".format(h, m)
                                notifTime = t
                                scope.launch {
                                    activity.stepforgeStore.edit { it[NOTIF_TIME] = t }
                                }
                                activity.scheduleReminder(h, m)
                                showPicker = false
                            },
                            onDismiss = { showPicker = false }
                        )
                        Log.d("CustomTimePicker", ">>> Composable DRAWN, dark=${MaterialTheme.colorScheme.background.luminance() < 0.5f}")
                    }
                }

                // 2.5) System Notification Settings
                SettingItem(
                    icon = Icons.Outlined.Notifications,
                    title = stringResource(R.string.hc_notification_settings),
                    infoText = stringResource(R.string.hc_notification_settings_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current
                    Text(
                        text = stringResource(R.string.hc_notification_settings_body),
                        color = textSub,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                ).apply {
                                    putExtra(
                                        Settings.EXTRA_APP_PACKAGE,
                                        ctxLocal.packageName
                                    )
                                }
                                ctxLocal.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                    ).apply {
                                        data = Uri.parse("package:${ctxLocal.packageName}")
                                    }
                                    ctxLocal.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        ctxLocal,
                                        ctxLocal.getString(R.string.hc_unable_open_notification_settings),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        border = BorderStroke(1.dp, neon),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(stringResource(R.string.hc_open_notification_settings), color = Color(0xFF00F5FF), fontSize = 13.sp)
                    }
                }

                // 2.55) Battery Optimization Settings
                SettingItem(
                    icon = Icons.Outlined.BatterySaver,
                    title = stringResource(R.string.hc_battery_optimization),
                    infoText = stringResource(R.string.hc_battery_settings_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current

                    Text(
                        text = stringResource(R.string.hc_battery_settings_body),
                        color = textSub,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                ctxLocal.startActivity(intent)
                            } catch (_: Exception) {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${ctxLocal.packageName}")
                                    }
                                    ctxLocal.startActivity(intent)
                                } catch (_: Exception) {
                                    Toast.makeText(
                                        ctxLocal,
                                        ctxLocal.getString(R.string.hc_unable_open_battery_settings),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        border = BorderStroke(1.dp, neon),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(stringResource(R.string.hc_open_battery_settings), color = Color(0xFF00F5FF), fontSize = 13.sp)
                    }

                    Spacer(Modifier.height(6.dp))

                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.parse("package:${ctxLocal.packageName}")
                                }
                                ctxLocal.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    ctxLocal,
                                    ctxLocal.getString(R.string.hc_unable_open_app_settings),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        border = BorderStroke(1.dp, neon),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(stringResource(R.string.hc_open_app_settings), color = Color(0xFF00F5FF), fontSize = 13.sp)
                    }
                }


                // 2.6) Water Reminder
                SettingItem(
                    icon = Icons.Outlined.LocalDrink,
                    title = stringResource(R.string.hc_water_reminder),
                    infoText = stringResource(R.string.hc_water_settings_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current
                    val scopeLocal = rememberCoroutineScope()

                    val KEY_WATER_ENABLED = booleanPreferencesKey("water_enabled")
                    var waterEnabled by remember { mutableStateOf(false) }

                    LaunchedEffect(Unit) {
                        val prefs = ctxLocal.stepforgeStore.data.first()
                        waterEnabled = prefs[KEY_WATER_ENABLED] ?: false
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(18.dp))
                            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    ctxLocal.startActivity(
                                        Intent(ctxLocal, WaterReminderActivity::class.java)
                                    )
                                },
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hc_water_reminders),
                                color = textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = if (waterEnabled)
                                    stringResource(R.string.hc_water_enabled_info)
                                else
                                    stringResource(R.string.hc_water_disabled_info),
                                color = textSub,
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        Spacer(Modifier.width(10.dp))

                        Switch(
                            checked = waterEnabled,
                            onCheckedChange = { enabled ->
                                waterEnabled = enabled
                                scopeLocal.launch {
                                    ctxLocal.stepforgeStore.edit { prefs ->
                                        prefs[KEY_WATER_ENABLED] = enabled
                                    }
                                }
                                if (enabled) {
                                    WaterReminderScheduler.schedule(ctxLocal)
                                } else {
                                    WaterReminderScheduler.cancel(ctxLocal)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color(0xFF00FFA3),
                                checkedTrackColor = Color(0xFF004D40)
                            )
                        )
                    }
                }

                // Sleep
                SettingItem(
                    icon = Icons.Outlined.Bedtime,
                    title = stringResource(R.string.hc_sleep),
                    infoText = stringResource(R.string.hc_sleep_settings_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(18.dp))
                            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
                            .clickable {
                                ctxLocal.startActivity(Intent(ctxLocal, SleepActivity::class.java))
                            }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hc_open_sleep),
                                color = textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.hc_open_sleep_info),
                                color = textSub,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 3) Daily Goal
                SettingItem(
                    icon = Icons.Outlined.Flag,
                    title = stringResource(R.string.hc_daily_goal),
                    infoText = stringResource(R.string.hc_daily_goal_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(stringResource(R.string.hc_steps_format, goal.toString()), color = textMain, fontSize = 16.sp)
                        OutlinedButton(
                            onClick = {
                                activity.startActivity(
                                    Intent(activity, TargetActivity::class.java)
                                )
                            },
                            border = BorderStroke(1.dp, neon),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(stringResource(R.string.hc_change), color = Color(0xFF00F5FF))
                        }
                    }
                }

                // 4) Theme
                SettingItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.hc_theme),
                    infoText = stringResource(R.string.hc_theme_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    val modes = listOf("dark", "light", "system")
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        modes.forEach { mode ->
                            val sel = themeMode == mode
                            Box(
                                Modifier
                                    .border(
                                        if (sel) 1.5.dp else 1.dp,
                                        if (sel) neon else Brush.linearGradient(
                                            listOf(Color.Gray, Color.DarkGray)
                                        ),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable {
                                        val selectedMode = mode // "dark", "light", "system"
                                        themeMode = selectedMode

                                        scope.launch {
                                            // 1) DataStore'a yaz
                                            activity.stepforgeStore.edit { it[THEME_MODE] = selectedMode }

                                            // 2) Tüm widget'lara yeni temayı okut
                                            StepWidgetProvider.notifyRefresh(activity)
                                            StepWidgetCompactProvider.notifyRefresh(activity)
                                            StepWidgetLargeProvider.notifyRefresh(activity)
                                        }
                                    }

                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    when (mode) {
                                        "dark" -> stringResource(R.string.hc_dark)
                                        "light" -> stringResource(R.string.hc_light)
                                        else -> stringResource(R.string.hc_system)
                                    },
                                    color = if (sel) Color(0xFF00FFA3) else textMain,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // ✅ Language
                SettingItem(
                    icon = Icons.Outlined.Public,
                    title = stringResource(R.string.settings_language_title),
                    infoText = stringResource(R.string.settings_language_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { rect, text ->
                        infoAnchor = rect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    val currentTags = AppLanguageHelper.selectedLanguageTags(activity)
                    val currentLabel = when {
                        currentTags.isBlank() -> stringResource(R.string.settings_language_system_default)
                        currentTags.startsWith("en") -> stringResource(R.string.settings_language_english)
                        currentTags.startsWith("tr") -> stringResource(R.string.settings_language_turkish)
                        currentTags.startsWith("de") -> stringResource(R.string.settings_language_german)
                        else -> currentTags
                    }

                    Text(
                        text = stringResource(R.string.settings_language_current, currentLabel),
                        color = textSub,
                        fontSize = 13.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { showLanguageDialog = true },
                        border = BorderStroke(1.dp, neon),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text(
                            text = stringResource(R.string.settings_language_change_button),
                            color = Color(0xFF00F5FF),
                            fontSize = 13.sp
                        )
                    }
                }


                // 4.5) Premium Debug
                SettingItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.hc_feature_preview),
                    infoText = stringResource(R.string.hc_feature_preview_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    PremiumDebugToggle(darkTheme = isDark)
                }


                SettingItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.hc_smart_coach_alerts),
                    infoText = stringResource(R.string.premium_ai_info_text),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { rect, text ->
                        infoAnchor = rect
                        infoText = text
                        openInfoCard.value = text
                    },
                    alwaysExpanded = true,
                    darkTheme = isDark
                ) {
                    PremiumAiCoachToggle(darkTheme = isDark)
                }







                // 5) Integration
                SettingItem(
                    icon = Icons.Outlined.Public,
                    title = stringResource(R.string.hc_integration),
                    infoText = stringResource(R.string.hc_integration_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { rect, text ->
                        infoAnchor = rect
                        infoText = text
                        openInfoCard.value = text
                    },
                    disableBringIntoView = true,
                    darkTheme = isDark
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        IntegrationList(
                            onHealthConnectClick = { sheet = IntegrationSheet.HealthConnect },
                            isHealthConnected = isHealthConnected,
                            darkTheme = isDark
                        )
                    }
                }

                // 6) Adjust Today's Steps
                SettingItem(
                    icon = Icons.Outlined.Edit,
                    title = stringResource(R.string.hc_adjust_today_steps),
                    infoText = stringResource(R.string.hc_adjust_steps_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current
                    AdjustStepsCard(
                        currentSteps = currentSteps,
                        dailyGoal = goal,
                        onApply = { value ->
                            val KEY_MANUAL_OVERRIDE = intPreferencesKey("manual_override_enabled")

                            scope.launch {
                                ctxLocal.stepforgeStore.edit { it[KEY_MANUAL_OVERRIDE] = 1 }
                            }

                            val i = Intent(ctxLocal, StepCounterService::class.java).apply {
                                putExtra("manualSteps", value)
                            }
                            ContextCompat.startForegroundService(ctxLocal, i)

                            StepCounterService.updateServiceNotification(ctxLocal, value, goal)

                            Toast.makeText(ctxLocal, ctxLocal.getString(R.string.hc_steps_updated_format, value), Toast.LENGTH_SHORT).show()
                        },
                        onReset = {
                            val KEY_MANUAL_OVERRIDE = intPreferencesKey("manual_override_enabled")

                            scope.launch {
                                // ✅ Health Connect tekrar “ana kaynak” olsun
                                ctxLocal.stepforgeStore.edit { it[KEY_MANUAL_OVERRIDE] = 0 }
                            }

                            // ✅ Tek komut: servis reset yapsın (günlük sıfırla)
                            val resetIntent = Intent(ctxLocal, StepCounterService::class.java).apply {
                                putExtra("forceReset", true)
                            }

                            // Android O+ güvenli başlatma
                            ContextCompat.startForegroundService(ctxLocal, resetIntent)

                            Toast.makeText(ctxLocal, ctxLocal.getString(R.string.hc_steps_reset_sent), Toast.LENGTH_SHORT).show()

                            // Bildirimi hemen 0 göster (servis zaten ardından güncelleyecek)
                            StepCounterService.updateServiceNotification(ctxLocal, 0, goal)
                        },
                        darkTheme = isDark
                    )
                }

                // 7) Feedback / Support
                SettingItem(
                    icon = Icons.Outlined.Feedback,
                    title = stringResource(R.string.hc_feedback_support),
                    infoText = stringResource(R.string.hc_feedback_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(18.dp))
                            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
                            .clickable { ctxLocal.startActivity(Intent(ctxLocal, FeedbackActivity::class.java)) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hc_open_feedback_center),
                                color = textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.hc_open_feedback_info),
                                color = textSub,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 8) Privacy & Security
                SettingItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.hc_privacy_security),
                    infoText = stringResource(R.string.hc_privacy_center_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(18.dp))
                            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
                            .clickable { ctxLocal.startActivity(Intent(ctxLocal, PrivacySecurityActivity::class.java)) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hc_open_privacy_center),
                                color = textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.hc_open_privacy_info),
                                color = textSub,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 9) Sync & Backup
                SettingItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.hc_sync_backup),
                    infoText = stringResource(R.string.hc_sync_backup_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(18.dp))
                            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
                            .clickable { ctxLocal.startActivity(Intent(ctxLocal, SyncBackupActivity::class.java)) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hc_open_sync_backup),
                                color = textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.hc_open_sync_backup_info),
                                color = textSub,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 10) About
                SettingItem(
                    icon = Icons.Outlined.Info,
                    title = stringResource(R.string.hc_about),
                    infoText = stringResource(R.string.hc_about_info),
                    openInfoCard = openInfoCard,
                    onInfoAnchor = { windowRect, text ->
                        infoAnchor = windowRect
                        infoText = text
                        openInfoCard.value = text
                    },
                    darkTheme = isDark
                ) {
                    val ctxLocal = LocalContext.current
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(rowBg, RoundedCornerShape(18.dp))
                            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
                            .clickable { ctxLocal.startActivity(Intent(ctxLocal, AboutActivity::class.java)) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.hc_about_stepforge),
                                color = textMain,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.hc_about_stepforge_info),
                                color = textSub,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        sheet?.let { currentSheet ->
            IntegrationModal(
                sheet = currentSheet,
                themeMode = themeMode,
                onDismiss = { sheet = null },
                onConnectHealthConnect = {
                    sheet = null
                    onLaunchAndroidPermission()
                }
            )
        }

        if (openInfoCard.value != null && infoAnchor != null && rootSize != IntSize.Zero) {
            InfoPanelOverlay(
                anchor = infoAnchor!!,
                text = infoText,
                rootSize = rootSize,
                density = LocalDensity.current,
                onDismiss = { openInfoCard.value = null },
                darkTheme = isDark
            )
        }

        if (showLanguageDialog) {
            AlertDialog(
                onDismissRequest = { showLanguageDialog = false },
                containerColor = if (isDark) Color(0xFF111318) else Color.White,
                titleContentColor = textMain,
                textContentColor = textMain,
                shape = RoundedCornerShape(20.dp),
                title = {
                    Text(
                        text = stringResource(R.string.settings_language_dialog_title),
                        color = textMain,
                        fontWeight = FontWeight.SemiBold
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {

                        fun apply(tags: String?) {
                            AppLanguageHelper.applyLanguage(activity, tags)
                            activity.window.decorView.postDelayed(
                                { AppLanguageHelper.refreshRuntimeSurfaces(activity) },
                                300L
                            )
                            showLanguageDialog = false
                        }

                        TextButton(onClick = { apply(null) }) {
                            Text(
                                text = stringResource(R.string.settings_language_system_default),
                                color = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
                            )
                        }

                        TextButton(onClick = { apply("en") }) {
                            Text(
                                text = stringResource(R.string.settings_language_english),
                                color = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
                            )
                        }

                        TextButton(onClick = { apply("tr") }) {
                            Text(
                                text = stringResource(R.string.settings_language_turkish),
                                color = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
                            )
                        }

                        TextButton(onClick = { apply("de") }) {
                            Text(
                                text = stringResource(R.string.settings_language_german),
                                color = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showLanguageDialog = false }) {
                        Text(
                            text = stringResource(R.string.common_close),
                            color = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
                        )
                    }
                }
            )
        }
    }
}

@Composable
private fun PremiumDebugToggle(darkTheme: Boolean) {

    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val premiumKey = intPreferencesKey("premium_enabled")

    val premiumFlow = remember(ctx) {
        ctx.stepforgeStore.data.map { prefs ->
            (prefs[premiumKey] ?: 0) == 1
        }
    }
    val isPremium by premiumFlow.collectAsState(initial = false)

    val rowBg = if (darkTheme) Color(0xFF101218) else Color(0xFFFFFFFF)
    val rowBorder = if (darkTheme) Color.Transparent else Color(0x1A1A202C)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(18.dp))
            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isPremium) stringResource(R.string.hc_advanced_enabled) else stringResource(R.string.hc_advanced_disabled),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = stringResource(R.string.hc_feature_preview_body),
                fontSize = 11.sp,
                color = if (darkTheme) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = isPremium,
            onCheckedChange = { enabled ->
                scope.launch {
                    ctx.stepforgeStore.edit { prefs ->
                        prefs[premiumKey] = if (enabled) 1 else 0
                    }
                }
            },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00FFA3),
                checkedTrackColor = Color(0xFF004D40)
            )
        )
    }
}


@Composable
private fun PremiumAiCoachToggle(
    darkTheme: Boolean
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val premiumKey = intPreferencesKey("premium_enabled")
    val aiCoachKey = StreakShieldPrefs.PREMIUM_AI_COACH_ENABLED

    val premiumFlow = remember(ctx) {
        ctx.stepforgeStore.data.map { prefs ->
            (prefs[premiumKey] ?: 0) == 1
        }
    }
    val isPremium by premiumFlow.collectAsState(initial = false)

    val aiCoachFlow = remember(ctx) {
        ctx.stepforgeStore.data.map { prefs ->
            prefs[aiCoachKey] ?: false
        }
    }
    val aiCoachEnabled by aiCoachFlow.collectAsState(initial = false)

    val rowBg = if (darkTheme) Color(0xFF101218) else Color(0xFFFFFFFF)
    val rowBorder = if (darkTheme) Color.Transparent else Color(0x1A1A202C)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(rowBg, RoundedCornerShape(18.dp))
            .border(1.dp, rowBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = if (isPremium) {
                    stringResource(R.string.premium_ai_toggle_title)
                } else {
                    stringResource(R.string.premium_ai_toggle_title_locked)
                },
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (isPremium) {
                    stringResource(R.string.premium_ai_toggle_desc_enabled)
                } else {
                    stringResource(R.string.premium_ai_toggle_desc_locked)
                },
                fontSize = 11.sp,
                color = if (darkTheme) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.7f)
            )
        }

        Switch(
            checked = isPremium && aiCoachEnabled,
            onCheckedChange = { enabled ->
                if (!isPremium) return@Switch

                scope.launch {
                    ctx.stepforgeStore.edit { prefs ->
                        prefs[aiCoachKey] = enabled
                    }
                }
            },
            enabled = isPremium,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color(0xFF00FFA3),
                checkedTrackColor = Color(0xFF004D40)
            )
        )
    }
}
