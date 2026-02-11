package com.example.stepforge.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.backup.CloudBackupManager
import com.example.stepforge.backup.CloudBackupManager.RestoreResult
import com.example.stepforge.backup.CloudBackupManager.RestoreResult.*
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SyncBackupScreen(
    onBack: () -> Unit,
    onSelectGoogleAccount: () -> Unit,
    onDisconnectGoogleAccount: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bg = cs.background
    val cardBg = cs.surface

    val neonBlue = if (isDark) Color(0xFF00E0FF) else Color(0xFF38BDF8)
    val neonGreen = if (isDark) Color(0xFF00FFA3) else Color(0xFF4ADE80)

    val borderBrush = Brush.linearGradient(
        if (isDark) listOf(Color(0xFF1B1C21), Color(0xFF23262C))
        else listOf(Color(0xFFE2E8F0), Color(0xFFCBD5F5))
    )
    val iconGradient = Brush.linearGradient(listOf(neonBlue, neonGreen))

    val KEY_SYNC_AUTO = intPreferencesKey("sync_auto_enabled")
    val KEY_BACKUP_EMAIL = stringPreferencesKey("backup_email")
    val KEY_BACKUP_LAST_TIME = longPreferencesKey("backup_last_time")
    val KEY_LAST_AUTO_BACKUP = longPreferencesKey("last_auto_backup_time")

    val cloudManager = remember { CloudBackupManager(ctx) }

    var autoSyncEnabled by remember { mutableStateOf(false) }
    var isBackingUp by remember { mutableStateOf(false) }
    var lastBackup by remember { mutableStateOf<Date?>(null) }
    var backupStatus by remember { mutableStateOf<String?>(null) }
    var syncStatus by remember { mutableStateOf("Inactive") }

    var isRestoring by remember { mutableStateOf(false) }
    var restoreStatus by remember { mutableStateOf<String?>(null) }

    // ✅ Connected email artık DataStore'dan canlı okunuyor (UI anında güncellenir)
    val connectedEmailFlow = remember {
        ctx.stepforgeStore.data.map { prefs -> prefs[KEY_BACKUP_EMAIL] }
    }
    val connectedEmail by connectedEmailFlow.collectAsState(initial = null)

    // ---- DataStore'dan yükle + otomatik backup ----
    LaunchedEffect(Unit) {
        val prefs = ctx.stepforgeStore.data.first()
        autoSyncEnabled = (prefs[KEY_SYNC_AUTO] ?: 0) == 1
        syncStatus = if (autoSyncEnabled) "Active" else "Inactive"

        val savedTime = prefs[KEY_BACKUP_LAST_TIME] ?: 0L
        if (savedTime > 0L) {
            lastBackup = Date(savedTime)
        }

        // Otomatik backup: günde 1 kez
        val lastAuto = prefs[KEY_LAST_AUTO_BACKUP] ?: 0L
        val now = System.currentTimeMillis()
        val oneDayMs = TimeUnit.DAYS.toMillis(1)

        if (autoSyncEnabled && now - lastAuto > oneDayMs) {
            isBackingUp = true
            backupStatus = null
            val ok = withContext(Dispatchers.IO) { cloudManager.uploadToCloud() }
            isBackingUp = false
            if (ok) {
                val nowDate = Date()
                lastBackup = nowDate
                backupStatus = "Automatic backup completed."
                ctx.stepforgeStore.edit { p ->
                    p[KEY_BACKUP_LAST_TIME] = nowDate.time
                    p[KEY_LAST_AUTO_BACKUP] = now
                }
            } else {
                backupStatus = "Automatic backup failed."
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Sync & Backup",
                            color = cs.onBackground,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Automatically sync and backup your data.",
                            color = cs.onBackground.copy(alpha = 0.65f),
                            fontSize = 13.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = bg)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(pad)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(
                        Brush.horizontalGradient(
                            listOf(neonBlue.copy(alpha = 0.15f), neonGreen.copy(alpha = 0.15f))
                        )
                    )
            )

            // ---------- AUTOMATIC SYNC ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderBrush, RoundedCornerShape(26.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isDark) Color(0xFF0A0C10) else Color(0xFFE5E9F2),
                                        CircleShape
                                    )
                                    .border(1.3.dp, iconGradient, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CloudUpload,
                                    contentDescription = null,
                                    tint = neonBlue,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Automatic Sync",
                                    color = cs.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Keep your step data safely synced across your devices.",
                                    color = cs.onSurface.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        val switchScale by animateFloatAsState(
                            targetValue = if (autoSyncEnabled) 1.08f else 1f,
                            animationSpec = tween(180, easing = FastOutSlowInEasing),
                            label = "syncSwitchScale"
                        )
                        Switch(
                            checked = autoSyncEnabled,
                            onCheckedChange = { enabled ->
                                autoSyncEnabled = enabled
                                syncStatus = if (enabled) "Active" else "Inactive"
                                scope.launch {
                                    ctx.stepforgeStore.edit { p ->
                                        p[KEY_SYNC_AUTO] = if (enabled) 1 else 0
                                    }
                                }
                            },
                            modifier = Modifier.scale(switchScale),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = neonGreen,
                                checkedTrackColor = neonGreen.copy(alpha = 0.3f)
                            )
                        )
                    }

                    AnimatedVisibility(visible = autoSyncEnabled) {
                        Column(
                            modifier = Modifier.padding(top = 6.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            SmallBullet("Daily steps and distance are synced securely.")
                            SmallBullet("History is automatically restored on new devices.")
                        }
                    }
                }
            }

            // ---------- BACKUP STATUS ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderBrush, RoundedCornerShape(26.dp))
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(
                                        if (isDark) Color(0xFF0A0C10) else Color(0xFFE5E9F2),
                                        CircleShape
                                    )
                                    .border(1.3.dp, iconGradient, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = neonGreen,
                                    modifier = Modifier.size(22.dp)
                                )
                            }

                            Column {
                                Text(
                                    text = "Backup Status",
                                    color = cs.onSurface,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )

                                val lastBackupText = lastBackup?.let {
                                    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                                    "Last backup: ${sdf.format(it)}"
                                } ?: "Last backup: Never"

                                Text(
                                    text = lastBackupText,
                                    color = cs.onSurface.copy(alpha = 0.7f),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        val label = when {
                            isBackingUp -> "Backing up…"
                            backupStatus != null -> "Updated"
                            else -> "Idle"
                        }
                        Text(text = label, color = neonBlue, fontSize = 11.sp)
                    }

                    if (backupStatus != null) {
                        Text(
                            text = backupStatus!!,
                            color = cs.onSurface.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ---------- MANUAL BACKUP ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderBrush, RoundedCornerShape(26.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Manual Backup",
                        color = cs.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Create a fresh copy of your activity data in the cloud.",
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    val interaction = remember { MutableInteractionSource() }
                    val targetScale = if (isBackingUp) 0.96f else 1f
                    val btnScale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                        label = "manualBackupScale"
                    )

                    Box(
                        modifier = Modifier
                            .width(160.dp)
                            .height(44.dp)
                            .scale(btnScale)
                            .background(
                                color = if (isDark) Color(0xFF080A0F) else Color.White,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(neonBlue, neonGreen)),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable(
                                enabled = !isBackingUp,
                                interactionSource = interaction,
                                indication = null
                            ) {
                                isBackingUp = true
                                backupStatus = null
                                scope.launch {
                                    val ok = cloudManager.uploadToCloud()
                                    isBackingUp = false
                                    if (ok) {
                                        val now = Date()
                                        lastBackup = now
                                        backupStatus = "Cloud backup completed successfully."
                                        ctx.stepforgeStore.edit { p -> p[KEY_BACKUP_LAST_TIME] = now.time }
                                        Toast.makeText(ctx, "Backup uploaded to cloud", Toast.LENGTH_SHORT).show()
                                    } else {
                                        backupStatus = "Cloud backup failed. Try again later."
                                        Toast.makeText(ctx, "Cloud backup failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Sync,
                                contentDescription = null,
                                tint = if (isDark) Color.White else Color(0xFF111827),
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = if (isBackingUp) "Backing up…" else "Backup",
                                color = if (isDark) Color.White else Color(0xFF111827),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            // ---------- RESTORE FROM CLOUD ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderBrush, RoundedCornerShape(26.dp))
                        .padding(horizontal = 18.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Restore from Cloud",
                        color = cs.onSurface,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Bring back your preferences and full step history from your latest backup.",
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    val interaction = remember { MutableInteractionSource() }
                    val targetScale = if (isRestoring) 0.96f else 1f
                    val btnScale by animateFloatAsState(
                        targetValue = targetScale,
                        animationSpec = tween(160, easing = FastOutSlowInEasing),
                        label = "restoreScale"
                    )

                    Box(
                        modifier = Modifier
                            .width(180.dp)
                            .height(44.dp)
                            .scale(btnScale)
                            .background(
                                color = if (isDark) Color(0xFF080A0F) else Color.White,
                                shape = RoundedCornerShape(999.dp)
                            )
                            .border(
                                width = 1.5.dp,
                                brush = Brush.horizontalGradient(listOf(neonGreen, neonBlue)),
                                shape = RoundedCornerShape(999.dp)
                            )
                            .clickable(
                                enabled = !isRestoring,
                                interactionSource = interaction,
                                indication = null
                            ) {
                                isRestoring = true
                                restoreStatus = null
                                scope.launch {
                                    val result: RestoreResult = cloudManager.restoreFromCloud()
                                    isRestoring = false
                                    restoreStatus = when (result) {
                                        SUCCESS ->
                                            "Restore completed. History and key settings reloaded.\n" +
                                                    "Profile details (name, height, weight, birth date, PIN) stay only on this device."
                                        NO_BACKUP -> "No cloud backup found for this account."
                                        CORRUPT_DATA -> "Backup file is corrupted or incompatible."
                                        NETWORK_ERROR -> "Network error while contacting the cloud. Please try again."
                                        UNKNOWN_ERROR -> "Unknown error during restore. Please try again later."
                                    }
                                    Toast.makeText(
                                        ctx,
                                        when (result) {
                                            SUCCESS -> "Restore completed"
                                            NO_BACKUP -> "No backup available for this account."
                                            CORRUPT_DATA -> "Restore failed: corrupt backup."
                                            NETWORK_ERROR -> "Network error during restore."
                                            UNKNOWN_ERROR -> "Restore failed: unknown error."
                                        },
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (isRestoring) "Restoring…" else "Restore Now",
                            color = if (isDark) Color.White else Color(0xFF111827),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (restoreStatus != null) {
                        Text(
                            text = restoreStatus!!,
                            color = cs.onSurface.copy(alpha = 0.85f),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // ---------- CONNECTED GOOGLE ACCOUNT ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderBrush, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.MailOutline,
                            contentDescription = null,
                            tint = neonBlue
                        )
                        Column {
                            Text(
                                text = "Connected Account",
                                color = cs.onSurface,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Connected as: ${connectedEmail ?: "Not connected"}",
                                color = cs.onSurface.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    Text(
                        text = "Select your Google account to link it with your backups.",
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    Text(
                        text = "Cloud backups are linked to your Google account. " +
                                "If you reinstall StepForge or switch devices, use the same Google account " +
                                "and tap Restore from Cloud to bring back your history and settings.",
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        TextButton(
                            onClick = { onSelectGoogleAccount() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (connectedEmail == null) "Select Google Account" else "Change Account",
                                color = neonBlue,
                                fontSize = 13.sp
                            )
                        }

                        TextButton(
                            onClick = { onDisconnectGoogleAccount() },
                            modifier = Modifier.weight(1f),
                            enabled = connectedEmail != null
                        ) {
                            Text(
                                text = "Disconnect",
                                color = Color(0xFFFF8A80),
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // ---------- Security & bottom info ----------
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, borderBrush, RoundedCornerShape(22.dp))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VerifiedUser,
                            contentDescription = null,
                            tint = neonGreen
                        )
                        Text(
                            text = "Security & Privacy",
                            color = cs.onSurface,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    SmallBullet("Your data is encrypted during sync.")
                    SmallBullet("No ads, no tracking.")
                    SmallBullet("You can disconnect your account at any time.")
                }
            }

            Text(
                text = "Your data is securely stored and never shared without your permission.",
                color = cs.onSurface.copy(alpha = 0.7f),
                fontSize = 11.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
            )
        }
    }
}

@Composable
private fun SmallBullet(text: String) {
    val cs = MaterialTheme.colorScheme
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .padding(top = 4.dp, end = 6.dp)
                .size(4.dp)
                .background(Color(0xFF00F5FF), CircleShape)
        )
        Text(
            text = text,
            color = cs.onSurface.copy(alpha = 0.8f),
            fontSize = 12.sp
        )
    }
}