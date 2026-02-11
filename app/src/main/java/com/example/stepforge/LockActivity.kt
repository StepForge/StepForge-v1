package com.example.stepforge

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.stepforgeTheme
import com.example.stepforge.ui.rememberUseDarkTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class LockActivity : FragmentActivity() {

    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    private val KEY_APP_LOCK_ENABLED = intPreferencesKey("app_lock_enabled")
    private val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    private val KEY_APP_LOCK_BIOMETRIC = intPreferencesKey("app_lock_bio")
    private val KEY_APP_LOCK_SESSION_UNLOCKED = intPreferencesKey("app_lock_session_unlocked")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(
            this,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    runBlocking {
                        applicationContext.stepforgeStore.edit { prefs ->
                            prefs[KEY_APP_LOCK_SESSION_UNLOCKED] = 1
                        }
                    }
                    setResult(RESULT_OK)
                    finish()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    // kullanıcı iptal ederse PIN ekranında kalır
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(
                        this@LockActivity,
                        "Biometric authentication failed.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        )

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock StepForge")
            .setSubtitle("Use your biometric credential to unlock.")
            .setNegativeButtonText("Cancel")
            .setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )
            .build()

        setContent {
            // DÜZELTME: Artık sabit dark değil, DataStore’daki theme_mode’u kullanıyoruz
            val useDark = rememberUseDarkTheme(this)
            stepforgeTheme(darkTheme = useDark) {
                LockScreen(
                    onBack = {
                        setResult(RESULT_CANCELED)
                        finish()
                    },
                    onBiometricRequested = {
                        biometricPrompt.authenticate(promptInfo)
                    },
                    onUnlockSuccess = {
                        runBlocking {
                            applicationContext.stepforgeStore.edit { prefs ->
                                prefs[KEY_APP_LOCK_SESSION_UNLOCKED] = 1
                            }
                        }
                        setResult(RESULT_OK)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LockScreen(
    onBack: () -> Unit,
    onBiometricRequested: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bg = cs.background
    val cardBg = cs.surface

    val KEY_APP_LOCK_ENABLED = intPreferencesKey("app_lock_enabled")
    val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    val KEY_APP_LOCK_BIOMETRIC = intPreferencesKey("app_lock_bio")
    val KEY_APP_LOCK_SESSION_UNLOCKED = intPreferencesKey("app_lock_session_unlocked")

    // Güvenlik soru key'leri
    val KEY_SEC_Q1 = stringPreferencesKey("security_q1")
    val KEY_SEC_A1 = stringPreferencesKey("security_a1")

    var storedPin by remember { mutableStateOf<String?>(null) }
    var biometricAllowed by remember { mutableStateOf(false) }

    var pinField by remember { mutableStateOf(TextFieldValue("")) }
    var pinError by remember { mutableStateOf<String?>(null) }

    // Neonlar: dark’ta parlak, light’ta mat
    val neonA = if (isDark) Color(0xFF00FFA3) else Color(0xFF4FD1C5)
    val neonB = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
    val neon = Brush.horizontalGradient(listOf(neonA, neonB))

    // Forgot PIN state
    var showSecurityDialog by remember { mutableStateOf(false) }
    var secQuestion by remember { mutableStateOf<String?>(null) }
    var secAnswer by remember { mutableStateOf<String?>(null) }
    var secUserAnswer by remember { mutableStateOf(TextFieldValue("")) }
    var newPin by remember { mutableStateOf(TextFieldValue("")) }
    var newPinConfirm by remember { mutableStateOf(TextFieldValue("")) }

    // Biometric availability
    val biometricManager = BiometricManager.from(ctx)
    val biometricAvailable by remember {
        mutableStateOf(
            when (biometricManager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                        BiometricManager.Authenticators.BIOMETRIC_WEAK
            )) {
                BiometricManager.BIOMETRIC_SUCCESS -> true
                else -> false
            }
        )
    }

    LaunchedEffect(Unit) {
        val prefs = runBlocking { ctx.stepforgeStore.data.first() }
        val enabled = (prefs[KEY_APP_LOCK_ENABLED] ?: 0) == 1
        if (!enabled) {
            ctx.stepforgeStore.edit { it[KEY_APP_LOCK_SESSION_UNLOCKED] = 1 }
            onUnlockSuccess()
            return@LaunchedEffect
        }
        storedPin = prefs[KEY_APP_LOCK_PIN]
        biometricAllowed = (prefs[KEY_APP_LOCK_BIOMETRIC] ?: 0) == 1

        secQuestion = prefs[KEY_SEC_Q1]
        secAnswer = prefs[KEY_SEC_A1]?.lowercase()?.trim()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unlock StepForge", color = cs.onBackground) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bg)
                .padding(pad),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(26.dp),
                elevation = CardDefaults.cardElevation(if (isDark) 12.dp else 6.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = neonB,
                        modifier = Modifier.size(40.dp)
                    )
                    Text(
                        text = "Enter your PIN to unlock StepForge.",
                        color = cs.onSurface,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )

                    OutlinedTextField(
                        value = pinField,
                        onValueChange = {
                            if (it.text.length <= 4 && it.text.all(Char::isDigit))
                                pinField = it
                            pinError = null
                        },
                        singleLine = true,
                        placeholder = { Text("••••", color = cs.onSurface.copy(alpha = 0.4f)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = if (pinError == null) neonB else Color(0xFFFF8A80),
                            unfocusedBorderColor = cs.outlineVariant,
                            cursorColor = neonB,
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface
                        )
                    )

                    if (pinError != null) {
                        Text(
                            text = pinError ?: "",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            val entered = pinField.text.trim()
                            val correct = storedPin
                            if (entered.isNotEmpty() && correct != null && entered == correct) {
                                runBlocking {
                                    ctx.stepforgeStore.edit { prefs ->
                                        prefs[KEY_APP_LOCK_SESSION_UNLOCKED] = 1
                                    }
                                }
                                onUnlockSuccess()
                            } else {
                                pinError = "Incorrect PIN. Try again."
                                pinField = TextFieldValue("")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(neon, RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "Unlock",
                                color = if (isDark) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    TextButton(
                        onClick = {
                            if (secQuestion.isNullOrBlank() || secAnswer.isNullOrBlank()) {
                                Toast.makeText(
                                    ctx,
                                    "Security question not configured. Use Privacy & Security to reset App Lock.",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                secUserAnswer = TextFieldValue("")
                                newPin = TextFieldValue("")
                                newPinConfirm = TextFieldValue("")
                                showSecurityDialog = true
                            }
                        }
                    ) {
                        Text(
                            text = "Forgot PIN?",
                            color = if (isDark) Color(0xFFFFB74D) else Color(0xFFFB8C00),
                            fontSize = 13.sp
                        )
                    }

                    if (biometricAvailable && biometricAllowed) {
                        TextButton(onClick = { onBiometricRequested() }) {
                            Text(
                                text = "Use biometric instead",
                                color = neonB,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Güvenlik sorusu dialog'u (tema uyumlu)
            if (showSecurityDialog && secQuestion != null && secAnswer != null) {
                AlertDialog(
                    onDismissRequest = { showSecurityDialog = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                val answerOk =
                                    secUserAnswer.text.trim().lowercase() == secAnswer!!.lowercase()
                                val np = newPin.text.trim()
                                val npc = newPinConfirm.text.trim()

                                if (!answerOk) {
                                    Toast.makeText(
                                        ctx,
                                        "Security answer is incorrect.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }
                                if (np.length != 4 || !np.all(Char::isDigit) || np != npc) {
                                    Toast.makeText(
                                        ctx,
                                        "PINs must match and be 4 digits.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@TextButton
                                }

                                scope.launch {
                                    ctx.stepforgeStore.edit { prefs ->
                                        prefs[KEY_APP_LOCK_PIN] = np
                                        prefs[KEY_APP_LOCK_ENABLED] = 1
                                    }
                                    Toast.makeText(
                                        ctx,
                                        "PIN reset successfully.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    showSecurityDialog = false
                                    storedPin = np
                                }
                            }
                        ) {
                            Text("Reset PIN", color = neonB)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showSecurityDialog = false }) {
                            Text("Cancel", color = cs.onSurface.copy(alpha = 0.85f))
                        }
                    },
                    title = {
                        Text(
                            text = "Reset PIN",
                            color = cs.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = secQuestion ?: "",
                                color = cs.onSurface.copy(alpha = 0.9f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                            OutlinedTextField(
                                value = secUserAnswer,
                                onValueChange = { secUserAnswer = it },
                                singleLine = true,
                                placeholder = { Text("Your answer") },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = neonB,
                                    unfocusedBorderColor = cs.outlineVariant,
                                    cursorColor = neonB,
                                    focusedTextColor = cs.onSurface,
                                    unfocusedTextColor = cs.onSurface
                                )
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Choose a new 4‑digit PIN:",
                                color = cs.onSurface.copy(alpha = 0.85f),
                                fontSize = 13.sp
                            )
                            OutlinedTextField(
                                value = newPin,
                                onValueChange = {
                                    if (it.text.length <= 4 && it.text.all(Char::isDigit))
                                        newPin = it
                                },
                                singleLine = true,
                                placeholder = { Text("New PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = neonB,
                                    unfocusedBorderColor = cs.outlineVariant,
                                    cursorColor = neonB,
                                    focusedTextColor = cs.onSurface,
                                    unfocusedTextColor = cs.onSurface
                                )
                            )
                            OutlinedTextField(
                                value = newPinConfirm,
                                onValueChange = {
                                    if (it.text.length <= 4 && it.text.all(Char::isDigit))
                                        newPinConfirm = it
                                },
                                singleLine = true,
                                placeholder = { Text("Confirm new PIN") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = neonB,
                                    unfocusedBorderColor = cs.outlineVariant,
                                    cursorColor = neonB,
                                    focusedTextColor = cs.onSurface,
                                    unfocusedTextColor = cs.onSurface
                                )
                            )
                        }
                    },
                    containerColor = cs.surface,
                    shape = RoundedCornerShape(18.dp)
                )
            }
        }
    }
}