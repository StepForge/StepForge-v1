package com.example.stepforge.settings

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteSweep
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.work.WorkManager
import com.example.stepforge.MidnightResetReceiver
import com.example.stepforge.SetPinActivity
import com.example.stepforge.StepCounterService
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.notification.ReminderReceiver
import com.example.stepforge.notification.WaterReminderScheduler
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySecurityScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val colors = MaterialTheme.colorScheme

    val darkBg = colors.background
    val cardBg = colors.surface

    // Light/Dark accent: light’ta mat turkuaz, dark’ta neon
    val isLight = colors.background.luminance() > 0.5f
    val accentA = if (isLight) Color(0xFF4FD1C5) else Color(0xFF00FFA3)
    val accentB = if (isLight) Color(0xFF2CB6AE) else Color(0xFF00F5FF)
    val neonBorder = Brush.horizontalGradient(listOf(accentA, accentB))

    val topTitle = colors.onBackground
    val topSub = colors.onBackground.copy(alpha = 0.65f)

    val surfaceText = colors.onSurface
    val surfaceSub = colors.onSurfaceVariant

    // DataStore keys
    val KEY_APP_LOCK_ENABLED = intPreferencesKey("app_lock_enabled")   // 0/1
    val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")        // "1234"
    val KEY_APP_LOCK_BIOMETRIC = intPreferencesKey("app_lock_bio")     // 0/1
    val KEY_APP_LOCK_TIMEOUT = intPreferencesKey("app_lock_timeout")   // seconds, 0=Immediate

    // Cloud/account related key (senin projende kullanılıyor)
    val KEY_BACKUP_EMAIL = stringPreferencesKey("backup_email")

    // state
    var appLockEnabled by remember { mutableStateOf(false) }
    var biometricAllowed by remember { mutableStateOf(false) }
    var currentPin by remember { mutableStateOf<String?>(null) }
    var timeoutSeconds by remember { mutableStateOf(0) }

    var showSetPinDialog by remember { mutableStateOf(false) } // şimdilik UI'da tetiklenmiyor, dursun
    var showChangePinDialog by remember { mutableStateOf(false) }
    var showDisablePinDialog by remember { mutableStateOf(false) }
    var showForgotPinDialog by remember { mutableStateOf(false) }
    var showForgotConfirmDialog by remember { mutableStateOf(false) }

    var pinField by remember { mutableStateOf(TextFieldValue("")) }
    var pinConfirmField by remember { mutableStateOf(TextFieldValue("")) }
    var pinOldField by remember { mutableStateOf(TextFieldValue("")) }

    // Clear data dialogs
    var showClearWarning by remember { mutableStateOf(false) }
    var showClearConfirm by remember { mutableStateOf(false) }

    // typed confirmation
    var clearConfirmText by remember { mutableStateOf(TextFieldValue("")) }
    var clearAcknowledge by remember { mutableStateOf(false) }

    // wiping progress
    var isWiping by remember { mutableStateOf(false) }
    var wipeResultText by remember { mutableStateOf<String?>(null) }

    // Biometric availability
    val biometricManager = BiometricManager.from(ctx)
    val biometricAvailable by remember {
        mutableStateOf(
            when (
                biometricManager.canAuthenticate(
                    BiometricManager.Authenticators.BIOMETRIC_STRONG or
                            BiometricManager.Authenticators.BIOMETRIC_WEAK
                )
            ) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                else -> false
            }
        )
    }

    LaunchedEffect(Unit) {
        val prefs = ctx.stepforgeStore.data.first()
        appLockEnabled = (prefs[KEY_APP_LOCK_ENABLED] ?: 0) == 1
        biometricAllowed = (prefs[KEY_APP_LOCK_BIOMETRIC] ?: 0) == 1
        currentPin = prefs[KEY_APP_LOCK_PIN]
        timeoutSeconds = prefs[KEY_APP_LOCK_TIMEOUT] ?: 0
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Privacy & Security",
                            color = topTitle,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Control your data and manage security preferences.",
                            color = topSub,
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = topTitle
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBg)
                .padding(pad)
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(neonBorder)
            )

            // 1) Data Permissions
            PrivacyCard(
                icon = Icons.Outlined.Security,
                title = "Data Permissions",
                subtitle = "Choose what health data StepForge can access.",
                cardBg = cardBg,
                borderBrush = neonBorder,
                titleColor = surfaceText,
                subtitleColor = surfaceSub,
                iconTint = accentB
            ) {
                val ctxLocal = LocalContext.current
                Text(
                    text = "StepForge relies on Activity Recognition, notification access and optional " +
                            "Health Connect permissions to track and display your activity.",
                    color = surfaceText.copy(alpha = 0.82f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Android app permissions",
                            color = surfaceText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Open the system page where you can grant or revoke StepForge permissions.",
                            color = surfaceSub,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = "package:${ctxLocal.packageName}".toUri()
                                }
                                ctxLocal.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(ctxLocal, "Unable to open system settings.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accentB,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text("Review", fontSize = 12.sp)
                    }
                }
            }

            // 2) Account Privacy
            PrivacyCard(
                icon = Icons.Outlined.People,
                title = "Account Privacy",
                subtitle = "Manage who can see your progress or activity data.",
                cardBg = cardBg,
                borderBrush = neonBorder,
                titleColor = surfaceText,
                subtitleColor = surfaceSub,
                iconTint = accentB
            ) {
                Text(
                    text = "StepForge stores your profile and step history locally on this device. " +
                            "If you enable Sync & Backup, your supported history/settings can be backed up to the cloud.",
                    color = surfaceText.copy(alpha = 0.82f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "If you connect Health Connect, your data access is governed by Health Connect permissions.",
                    color = surfaceSub,
                    fontSize = 12.sp
                )
            }

            // 3) App Lock
            PrivacyCard(
                icon = Icons.Outlined.Lock,
                title = "App Lock",
                subtitle = "Enable PIN or biometric lock for extra security.",
                cardBg = cardBg,
                borderBrush = if (appLockEnabled) {
                    Brush.horizontalGradient(listOf(accentA, accentB, accentA))
                } else neonBorder,
                titleColor = surfaceText,
                subtitleColor = surfaceSub,
                iconTint = accentB
            ) {
                val ctxLocal = LocalContext.current
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (appLockEnabled)
                                    "App Lock is currently enabled with a 4‑digit PIN."
                                else
                                    "Protect StepForge with a 4‑digit PIN requested when opening the app.",
                                color = surfaceText.copy(alpha = 0.82f),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "Biometric unlock can be allowed on top of the PIN if your device supports it.",
                                color = surfaceSub,
                                fontSize = 11.sp
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Switch(
                            checked = appLockEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) {
                                    if (currentPin.isNullOrEmpty()) {
                                        val intent = Intent(ctxLocal, SetPinActivity::class.java)
                                        ctxLocal.startActivity(intent)
                                    } else {
                                        appLockEnabled = true
                                        scope.launch {
                                            ctxLocal.stepforgeStore.edit { prefs ->
                                                prefs[KEY_APP_LOCK_ENABLED] = 1
                                            }
                                        }
                                    }
                                } else {
                                    if (currentPin.isNullOrEmpty()) {
                                        appLockEnabled = false
                                        biometricAllowed = false
                                        scope.launch {
                                            ctxLocal.stepforgeStore.edit { prefs ->
                                                prefs[KEY_APP_LOCK_ENABLED] = 0
                                                prefs[KEY_APP_LOCK_BIOMETRIC] = 0
                                            }
                                        }
                                    } else {
                                        showDisablePinDialog = true
                                    }
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = accentA,
                                checkedTrackColor = accentA.copy(alpha = 0.25f)
                            )
                        )
                    }

                    if (appLockEnabled) {
                        Divider(
                            color = colors.outlineVariant,
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 6.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "PIN options",
                                    color = surfaceText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "Update your PIN or reset App Lock if you forgot it.",
                                    color = surfaceSub,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            TextButton(onClick = { showChangePinDialog = true }) {
                                Text("Change", color = accentB, fontSize = 12.sp)
                            }
                            Spacer(Modifier.width(4.dp))
                            TextButton(onClick = { showForgotPinDialog = true }) {
                                Text("Forgot?", color = Color(0xFFFFB74D), fontSize = 12.sp)
                            }
                        }

                        Spacer(Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Allow biometric unlock",
                                    color = surfaceText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = if (biometricAvailable)
                                        "Use your fingerprint or face as an alternative to PIN."
                                    else
                                        "Biometrics are not available or not enrolled on this device.",
                                    color = surfaceSub,
                                    fontSize = 11.sp
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Switch(
                                checked = biometricAllowed && biometricAvailable,
                                onCheckedChange = { enabled ->
                                    if (!biometricAvailable) {
                                        Toast.makeText(
                                            ctxLocal,
                                            "No biometric hardware or enrolled credentials.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    } else {
                                        biometricAllowed = enabled
                                        scope.launch {
                                            ctxLocal.stepforgeStore.edit { prefs ->
                                                prefs[KEY_APP_LOCK_BIOMETRIC] = if (enabled) 1 else 0
                                            }
                                        }
                                    }
                                },
                                enabled = biometricAvailable,
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = accentA,
                                    checkedTrackColor = accentA.copy(alpha = 0.25f)
                                )
                            )
                        }

                        Spacer(Modifier.height(6.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Lock when leaving StepForge",
                                color = surfaceText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Choose how quickly App Lock should trigger after you leave the app.",
                                color = surfaceSub,
                                fontSize = 11.sp
                            )

                            val options = listOf(
                                0 to "Immediately (on every return)",
                                5 to "After 5 seconds",
                                30 to "After 30 seconds",
                                60 to "After 60 seconds"
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                options.forEach { (seconds, label) ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        RadioButton(
                                            selected = timeoutSeconds == seconds,
                                            onClick = {
                                                timeoutSeconds = seconds
                                                scope.launch {
                                                    ctxLocal.stepforgeStore.edit { prefs ->
                                                        prefs[KEY_APP_LOCK_TIMEOUT] = seconds
                                                    }
                                                }
                                            },
                                            colors = RadioButtonDefaults.colors(
                                                selectedColor = accentB,
                                                unselectedColor = colors.onSurfaceVariant
                                            )
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            text = label,
                                            color = surfaceText.copy(alpha = 0.9f),
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4) Clear Data (Local + Cloud + Account sign-out)
            PrivacyCard(
                icon = Icons.Outlined.DeleteSweep,
                title = "Clear Data",
                subtitle = "Delete StepForge data from this device and cloud backup.",
                cardBg = cardBg,
                borderBrush = neonBorder,
                titleColor = surfaceText,
                subtitleColor = surfaceSub,
                iconTint = accentB
            ) {
                Text(
                    text = "This is a destructive action. It will attempt to:\n" +
                            "• Delete local preferences and all local history (steps, water, sleep)\n" +
                            "• Delete your cloud backup data (latest backups for the signed-in user)\n" +
                            "• Sign you out from the backup account\n\n" +
                            "Health Connect data is managed by Health Connect and is not deleted here.",
                    color = surfaceText.copy(alpha = 0.82f),
                    fontSize = 13.sp
                )

                Spacer(Modifier.height(10.dp))

                if (wipeResultText != null) {
                    Text(
                        text = wipeResultText!!,
                        color = colors.onSurface.copy(alpha = 0.85f),
                        fontSize = 12.sp
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showClearWarning = true },
                    enabled = !isWiping,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFEF9A9A),
                        containerColor = Color.Transparent
                    )
                ) {
                    Text(if (isWiping) "Deleting…" else "Delete Everything", fontSize = 12.sp)
                }
            }

            // 5) Privacy Policy
            PrivacyCard(
                icon = Icons.Outlined.Description,
                title = "Privacy Policy",
                subtitle = "Read how we collect and protect your data.",
                cardBg = cardBg,
                borderBrush = neonBorder,
                titleColor = surfaceText,
                subtitleColor = surfaceSub,
                iconTint = accentB
            ) {
                val ctxLocal = LocalContext.current
                Text(
                    text = "Our Privacy Policy explains what information StepForge processes, how it is stored, " +
                            "and how optional integrations such as Health Connect and cloud backup are handled.",
                    color = surfaceText.copy(alpha = 0.82f),
                    fontSize = 13.sp
                )
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(
                        onClick = {
                            try {
                                val intent = Intent(
                                    Intent.ACTION_VIEW,
                                    "https://stepforge.github.io/Privacy-Terms/privacy.html".toUri()
                                )
                                ctxLocal.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(ctxLocal, "Unable to open Privacy Policy in browser.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, colors.outlineVariant),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = accentB,
                            containerColor = Color.Transparent
                        )
                    ) {
                        Text("Open", fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // ===================== DIALOGS =====================

    val dialogBg = colors.surface
    val dialogTitle = colors.onSurface
    val dialogText = colors.onSurfaceVariant
    val fieldBorder = colors.outlineVariant

    // Set PIN (şu an tetiklenmiyor ama build için dursun)
    if (showSetPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showSetPinDialog = false
                pinField = TextFieldValue("")
                pinConfirmField = TextFieldValue("")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val pin = pinField.text.trim()
                        val confirm = pinConfirmField.text.trim()
                        if (pin.length == 4 && pin.all(Char::isDigit) && pin == confirm) {
                            appLockEnabled = true
                            currentPin = pin
                            showSetPinDialog = false
                            scope.launch {
                                ctx.stepforgeStore.edit { prefs ->
                                    prefs[KEY_APP_LOCK_ENABLED] = 1
                                    prefs[KEY_APP_LOCK_PIN] = pin
                                }
                            }
                            pinField = TextFieldValue("")
                            pinConfirmField = TextFieldValue("")
                            Toast.makeText(ctx, "App Lock enabled.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "PINs must match and be 4 digits.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Save", color = accentB) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showSetPinDialog = false
                        pinField = TextFieldValue("")
                        pinConfirmField = TextFieldValue("")
                    }
                ) { Text("Cancel", color = dialogTitle.copy(alpha = 0.85f)) }
            },
            title = {
                Text("Set App Lock PIN", color = dialogTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Choose a 4‑digit PIN. You will be asked to enter this PIN when opening StepForge.",
                        color = dialogText,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = pinField,
                        onValueChange = { if (it.text.length <= 4 && it.text.all(Char::isDigit)) pinField = it },
                        singleLine = true,
                        placeholder = { Text("••••") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentB,
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = accentB,
                            focusedTextColor = dialogTitle,
                            unfocusedTextColor = dialogTitle
                        )
                    )
                    OutlinedTextField(
                        value = pinConfirmField,
                        onValueChange = { if (it.text.length <= 4 && it.text.all(Char::isDigit)) pinConfirmField = it },
                        singleLine = true,
                        placeholder = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentB,
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = accentB,
                            focusedTextColor = dialogTitle,
                            unfocusedTextColor = dialogTitle
                        )
                    )
                }
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Change PIN
    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                pinOldField = TextFieldValue("")
                pinField = TextFieldValue("")
                pinConfirmField = TextFieldValue("")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val oldPin = pinOldField.text.trim()
                        val newPin = pinField.text.trim()
                        val confirm = pinConfirmField.text.trim()

                        if (currentPin.isNullOrEmpty() || oldPin != currentPin) {
                            Toast.makeText(ctx, "Current PIN is incorrect.", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        if (newPin.length == 4 && newPin.all(Char::isDigit) && newPin == confirm) {
                            currentPin = newPin
                            showChangePinDialog = false
                            scope.launch {
                                ctx.stepforgeStore.edit { prefs -> prefs[KEY_APP_LOCK_PIN] = newPin }
                            }
                            pinOldField = TextFieldValue("")
                            pinField = TextFieldValue("")
                            pinConfirmField = TextFieldValue("")
                            Toast.makeText(ctx, "PIN updated.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "New PINs must match and be 4 digits.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Save", color = accentB) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showChangePinDialog = false
                        pinOldField = TextFieldValue("")
                        pinField = TextFieldValue("")
                        pinConfirmField = TextFieldValue("")
                    }
                ) { Text("Cancel", color = dialogTitle.copy(alpha = 0.85f)) }
            },
            title = { Text("Change PIN", color = dialogTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your current PIN, then choose a new 4‑digit PIN.",
                        color = dialogText,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = pinOldField,
                        onValueChange = { if (it.text.length <= 4 && it.text.all(Char::isDigit)) pinOldField = it },
                        singleLine = true,
                        placeholder = { Text("Current PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentB,
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = accentB,
                            focusedTextColor = dialogTitle,
                            unfocusedTextColor = dialogTitle
                        )
                    )
                    OutlinedTextField(
                        value = pinField,
                        onValueChange = { if (it.text.length <= 4 && it.text.all(Char::isDigit)) pinField = it },
                        singleLine = true,
                        placeholder = { Text("New PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, autoCorrect = false),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentB,
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = accentB,
                            focusedTextColor = dialogTitle,
                            unfocusedTextColor = dialogTitle
                        )
                    )
                    OutlinedTextField(
                        value = pinConfirmField,
                        onValueChange = { if (it.text.length <= 4 && it.text.all(Char::isDigit)) pinConfirmField = it },
                        singleLine = true,
                        placeholder = { Text("Confirm new PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentB,
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = accentB,
                            focusedTextColor = dialogTitle,
                            unfocusedTextColor = dialogTitle
                        )
                    )
                }
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Disable PIN
    if (showDisablePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showDisablePinDialog = false
                pinOldField = TextFieldValue("")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val oldPin = pinOldField.text.trim()
                        if (currentPin.isNullOrEmpty() || oldPin != currentPin) {
                            Toast.makeText(ctx, "PIN is incorrect.", Toast.LENGTH_SHORT).show()
                        } else {
                            appLockEnabled = false
                            biometricAllowed = false
                            scope.launch {
                                ctx.stepforgeStore.edit { prefs ->
                                    prefs[KEY_APP_LOCK_ENABLED] = 0
                                    prefs[KEY_APP_LOCK_BIOMETRIC] = 0
                                }
                            }
                            showDisablePinDialog = false
                            pinOldField = TextFieldValue("")
                            Toast.makeText(ctx, "App Lock disabled.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Disable", color = Color(0xFFEF9A9A)) }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDisablePinDialog = false
                        pinOldField = TextFieldValue("")
                    }
                ) { Text("Cancel", color = dialogTitle.copy(alpha = 0.85f)) }
            },
            title = { Text("Disable App Lock?", color = dialogTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Enter your current PIN to turn off App Lock.",
                        color = dialogText,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = pinOldField,
                        onValueChange = { if (it.text.length <= 4 && it.text.all(Char::isDigit)) pinOldField = it },
                        singleLine = true,
                        placeholder = { Text("Current PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentB,
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = accentB,
                            focusedTextColor = dialogTitle,
                            unfocusedTextColor = dialogTitle
                        )
                    )
                }
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Forgot PIN info
    if (showForgotPinDialog) {
        AlertDialog(
            onDismissRequest = { showForgotPinDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showForgotPinDialog = false
                        showForgotConfirmDialog = true
                    }
                ) { Text("Reset App Lock", color = Color(0xFFEF9A9A)) }
            },
            dismissButton = {
                TextButton(onClick = { showForgotPinDialog = false }) {
                    Text("Cancel", color = dialogTitle.copy(alpha = 0.85f))
                }
            },
            title = { Text("Forgot your PIN?", color = dialogTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
            text = {
                Text(
                    text = "If you reset App Lock, your existing PIN and biometric setting will be removed. " +
                            "App Lock will be turned off and you will need to configure it again later.\n\n" +
                            "This does NOT delete your step data or preferences.",
                    color = dialogText,
                    fontSize = 13.sp
                )
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // Forgot PIN confirm: type RESET
    if (showForgotConfirmDialog) {
        var confirm by remember { mutableStateOf(TextFieldValue("")) }
        AlertDialog(
            onDismissRequest = { showForgotConfirmDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (confirm.text.trim().equals("RESET", ignoreCase = true)) {
                            appLockEnabled = false
                            biometricAllowed = false
                            currentPin = null
                            scope.launch {
                                ctx.stepforgeStore.edit { prefs ->
                                    prefs[KEY_APP_LOCK_ENABLED] = 0
                                    prefs[KEY_APP_LOCK_BIOMETRIC] = 0
                                    prefs[KEY_APP_LOCK_PIN] = ""
                                }
                            }
                            showForgotConfirmDialog = false
                            confirm = TextFieldValue("")
                            Toast.makeText(ctx, "App Lock has been reset.", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(ctx, "Type RESET to confirm.", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) { Text("Confirm", color = Color(0xFFEF9A9A)) }
            },
            dismissButton = {
                TextButton(onClick = { showForgotConfirmDialog = false }) {
                    Text("Cancel", color = dialogTitle.copy(alpha = 0.85f))
                }
            },
            title = { Text("Type RESET to continue", color = dialogTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "This action removes your current PIN and biometric unlock. Type RESET to confirm:",
                        color = dialogText,
                        fontSize = 13.sp
                    )
                    OutlinedTextField(
                        value = confirm,
                        onValueChange = { confirm = it },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF9A9A),
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = Color(0xFFEF9A9A),
                            focusedTextColor = dialogTitle,
                            unfocusedTextColor = dialogTitle
                        )
                    )
                }
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    // ===================== CLEAR DATA dialogs =====================

    if (showClearWarning) {
        AlertDialog(
            onDismissRequest = { showClearWarning = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearWarning = false
                        clearConfirmText = TextFieldValue("")
                        clearAcknowledge = false
                        showClearConfirm = true
                    }
                ) { Text("Continue", color = Color(0xFFFFB74D)) }
            },
            dismissButton = {
                TextButton(onClick = { showClearWarning = false }) {
                    Text("Cancel", color = dialogTitle.copy(alpha = 0.85f))
                }
            },
            title = { Text("Delete everything?", color = dialogTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "This is irreversible and may remove data you cannot recover.\n\n" +
                                "StepForge will attempt to delete:\n" +
                                "• Local preferences and all local history (steps, water, sleep)\n" +
                                "• Cloud backup data for the currently signed-in user\n" +
                                "• Backup account session (sign-out)\n\n" +
                                "Health Connect data is not deleted here.",
                        color = dialogText,
                        fontSize = 13.sp
                    )
                }
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(18.dp)
        )
    }

    if (showClearConfirm) {
        AlertDialog(
            onDismissRequest = { if (!isWiping) showClearConfirm = false },
            confirmButton = {
                TextButton(
                    enabled = !isWiping,
                    onClick = {
                        val phraseOk = clearConfirmText.text.trim().equals("DELETE ALL", ignoreCase = true)
                        if (!clearAcknowledge) {
                            Toast.makeText(ctx, "Please acknowledge the warning checkbox.", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        if (!phraseOk) {
                            Toast.makeText(ctx, "Type DELETE ALL to confirm.", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }

                        isWiping = true
                        wipeResultText = null

                        scope.launch {
                            val result = wipeEverything(ctx, KEY_BACKUP_EMAIL)
                            isWiping = false
                            showClearConfirm = false
                            wipeResultText = result

                            // UI state reset (ekran açık kalırsa tutarlı olsun)
                            appLockEnabled = false
                            biometricAllowed = false
                            currentPin = null
                            timeoutSeconds = 0

                            Toast.makeText(ctx, "Delete complete. ${result.take(80)}", Toast.LENGTH_LONG).show()
                            // Kullanıcı isterse geri çıkabilir
                            onBack()
                        }
                    }
                ) { Text(if (isWiping) "Deleting…" else "Delete", color = Color(0xFFEF9A9A)) }
            },
            dismissButton = {
                TextButton(
                    enabled = !isWiping,
                    onClick = {
                        showClearConfirm = false
                        clearConfirmText = TextFieldValue("")
                        clearAcknowledge = false
                    }
                ) { Text("Cancel", color = dialogTitle.copy(alpha = 0.85f)) }
            },
            title = { Text("Final confirmation", color = dialogTitle, fontWeight = FontWeight.SemiBold, fontSize = 16.sp) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "To permanently delete StepForge local data and attempt cloud deletion, type:",
                        color = dialogText,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "DELETE ALL",
                        color = Color(0xFFFF8A80),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = clearConfirmText,
                        onValueChange = { clearConfirmText = it },
                        singleLine = true,
                        enabled = !isWiping,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFEF9A9A),
                            unfocusedBorderColor = fieldBorder,
                            cursorColor = Color(0xFFEF9A9A),
                            focusedTextColor = dialogTitle,
                            unfocusedTextColor = dialogTitle
                        )
                    )

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = clearAcknowledge,
                            onCheckedChange = { if (!isWiping) clearAcknowledge = it }
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "I understand this cannot be undone.",
                            color = dialogTitle.copy(alpha = 0.9f),
                            fontSize = 12.sp
                        )
                    }

                    if (isWiping) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(18.dp)
        )
    }
}

@Composable
private fun PrivacyCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    cardBg: Color,
    borderBrush: Brush,
    titleColor: Color,
    subtitleColor: Color,
    iconTint: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(width = 1.dp, brush = borderBrush, shape = RoundedCornerShape(22.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
                Column {
                    Text(text = title, color = titleColor, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Text(text = subtitle, color = subtitleColor, fontSize = 12.sp)
                }
            }
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

private suspend fun wipeEverything(
    ctx: Context,
    backupEmailKey: Preferences.Key<String>
): String = withContext(Dispatchers.IO) {
    val ok = mutableListOf<String>()
    val warn = mutableListOf<String>()

    fun addWarn(t: Throwable, label: String) {
        warn.add("$label: ${t.message ?: t.javaClass.simpleName}")
    }

    try {
        ctx.stopService(Intent(ctx, StepCounterService::class.java))
        ok.add("service stopped")
    } catch (t: Throwable) {
        addWarn(t, "stop service")
    }

    try {
        val wm = WorkManager.getInstance(ctx)
        wm.cancelUniqueWork("StepCounterRestartWorker")
        ok.add("work cancelled")
    } catch (t: Throwable) {
        addWarn(t, "cancel work")
    }

    try {
        val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val reminderPi = PendingIntent.getBroadcast(
            ctx,
            2001,
            Intent(ctx, ReminderReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(reminderPi)

        val midnightPi = PendingIntent.getBroadcast(
            ctx,
            0,
            Intent(ctx, MidnightResetReceiver::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        am.cancel(midnightPi)

        ok.add("alarms cancelled")
    } catch (t: Throwable) {
        addWarn(t, "cancel alarms")
    }

    try {
        WaterReminderScheduler.cancel(ctx)
        ok.add("water reminders cancelled")
    } catch (t: Throwable) {
        addWarn(t, "cancel water reminders")
    }

    try {
        val auth = FirebaseAuth.getInstance()
        val user = auth.currentUser
        if (user == null) {
            warn.add("cloud: no signed-in user, skipped")
        } else {
            val uid = user.uid
            val fs = FirebaseFirestore.getInstance()

            try {
                val backupsCol = fs.collection("users")
                    .document(uid)
                    .collection("backups")

                val snaps = backupsCol.get().await()
                for (d in snaps.documents) {
                    d.reference.delete().await()
                }

                try {
                    fs.collection("users").document(uid).delete().await()
                } catch (_: Throwable) {
                }

                ok.add("cloud backup deleted")
            } catch (t: Throwable) {
                addWarn(t, "cloud delete")
            }
        }

        try {
            auth.signOut()
            ok.add("firebase signed out")
        } catch (t: Throwable) {
            addWarn(t, "firebase signOut")
        }

        try {
            val gso = GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN
            )
                .requestEmail()
                .build()

            val googleClient = GoogleSignIn.getClient(ctx, gso)
            try {
                googleClient.signOut().await()
            } catch (_: Throwable) {
            }
            try {
                googleClient.revokeAccess().await()
            } catch (_: Throwable) {
            }

            ok.add("google disconnected")
        } catch (t: Throwable) {
            addWarn(t, "google disconnect")
        }
    } catch (t: Throwable) {
        addWarn(t, "auth/cloud block")
    }

    try {
        AppDatabase.getDatabase(ctx).clearAllTables()
        ok.add("local database cleared")
    } catch (t: Throwable) {
        addWarn(t, "clear local database")
    }

    try {
        ctx.stepforgeStore.edit { prefs ->
            prefs.clear()
        }
        ok.add("local preferences cleared")
    } catch (t: Throwable) {
        addWarn(t, "clear datastore")
        try {
            ctx.stepforgeStore.edit { prefs -> prefs.remove(backupEmailKey) }
        } catch (_: Throwable) {
        }
    }

    buildString {
        append("Done: ")
        append(ok.joinToString(", ").ifBlank { "no steps completed" })
        if (warn.isNotEmpty()) {
            append(". Warnings: ")
            append(warn.joinToString(" | "))
        }
    }
}