package com.example.stepforge.settings

import com.example.stepforge.R

import androidx.compose.ui.res.stringResource

import android.Manifest
import android.app.AlarmManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.MailOutline
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.health.connect.client.HealthConnectClient
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.debug.RuntimeDiagnostics
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val darkBg = cs.background
    val cardBg = cs.surface

    val neonStart = if (isDark) Color(0xFF00FFA3) else Color(0xFF4FD1C5)
    val neonEnd = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
    val neon = Brush.horizontalGradient(listOf(neonStart, neonEnd))

    var message by remember { mutableStateOf(TextFieldValue("")) }
    var email by remember { mutableStateOf(TextFieldValue("")) }
    var includeDiagnostics by remember { mutableStateOf(true) }
    var showAttachInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.hc_feedback_title),
                            color = cs.onBackground,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = stringResource(R.string.hc_feedback_subtitle),
                            color = cs.onBackground.copy(alpha = 0.65f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 14.sp
                            
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = stringResource(R.string.hc_back),
                            tint = cs.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = darkBg
                )
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

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hc_share_thoughts),
                        color = cs.onSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = stringResource(R.string.hc_feedback_description),
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )

                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        placeholder = {
                            Text(
                                stringResource(R.string.hc_feedback_placeholder),
                                color = cs.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = cs.onSurface,
                            fontSize = 14.sp
                        ),
                        maxLines = 10,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = neonEnd,
                            unfocusedBorderColor = cs.outlineVariant,
                            cursorColor = neonEnd,
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface
                        )
                    )

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = {
                            Text(
                                stringResource(R.string.hc_email_optional),
                                color = cs.onSurface.copy(alpha = 0.7f)
                            )
                        },
                        placeholder = {
                            Text(
                                stringResource(R.string.hc_email_reply_hint),
                                color = cs.onSurface.copy(alpha = 0.4f)
                            )
                        },
                        textStyle = LocalTextStyle.current.copy(
                            color = cs.onSurface,
                            fontSize = 14.sp
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = neonEnd,
                            unfocusedBorderColor = cs.outlineVariant,
                            cursorColor = neonEnd,
                            focusedTextColor = cs.onSurface,
                            unfocusedTextColor = cs.onSurface
                        ),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.MailOutline,
                                contentDescription = null,
                                tint = neonEnd
                            )
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = includeDiagnostics,
                            onCheckedChange = { includeDiagnostics = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = neonEnd,
                                uncheckedColor = cs.onSurface.copy(alpha = 0.7f)
                            )
                        )
                        Column(modifier = Modifier.padding(top = 2.dp)) {
                            Text(
                                text = stringResource(R.string.hc_include_diagnostics),
                                color = cs.onSurface,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = stringResource(R.string.hc_diagnostics_info),
                                color = cs.onSurface.copy(alpha = 0.6f),
                                fontSize = 11.sp
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showAttachInfo = true },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(999.dp),
                            border = BorderStroke(1.dp, cs.outlineVariant),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = cs.onSurface,
                                containerColor = Color.Transparent
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = null,
                                tint = cs.onSurface
                            )
                            Spacer(Modifier.padding(3.dp))
                            Text(stringResource(R.string.hc_attach), fontSize = 13.sp)
                        }

                        Button(
                            onClick = {
                                if (message.text.isBlank()) {
                                    Toast.makeText(
                                        ctx,
                                        ctx.getString(R.string.hc_enter_feedback_first),
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@Button
                                }

                                scope.launch {
                                    val subject = ctx.getString(R.string.hc_feedback_subject)

                                    val diagnostics = if (includeDiagnostics) {
                                        buildDiagnosticsBlock(ctx)
                                    } else null

                                    val body = buildString {
                                        appendLine(message.text.trim())
                                        appendLine()

                                        if (email.text.isNotBlank()) {
                                            appendLine(ctx.getString(R.string.hc_preferred_contact_format, email.text.trim()))
                                            appendLine()
                                        }

                                        if (!diagnostics.isNullOrBlank()) {
                                            appendLine(diagnostics.trim())
                                            appendLine()
                                        }

                                        appendLine(buildEmailSignature(ctx))
                                    }

                                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                                        data = "mailto:".toUri()
                                        putExtra(Intent.EXTRA_EMAIL, arrayOf("stepforge0@gmail.com"))
                                        putExtra(Intent.EXTRA_SUBJECT, subject)
                                        putExtra(Intent.EXTRA_TEXT, body)
                                    }

                                    try {
                                        ctx.startActivity(intent)
                                    } catch (_: Exception) {
                                        Toast.makeText(
                                            ctx,
                                            ctx.getString(R.string.hc_no_email_app),
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(999.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(neon, RoundedCornerShape(999.dp))
                                    .padding(horizontal = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Send,
                                    contentDescription = null,
                                    tint = if (isDark) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.padding(3.dp))
                                Text(
                                    stringResource(R.string.hc_submit),
                                    color = if (isDark) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hc_common_issues),
                        color = neonEnd,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    IssueRow(
                        title = stringResource(R.string.hc_issue_steps_title),
                        subtitle = stringResource(R.string.hc_issue_steps_body),
                        emoji = "🚶"
                    )

                    IssueRow(
                        title = stringResource(R.string.hc_issue_history_title),
                        subtitle = stringResource(R.string.hc_issue_history_body),
                        emoji = "📅"
                    )

                    IssueRow(
                        title = stringResource(R.string.hc_issue_health_title),
                        subtitle = stringResource(R.string.hc_issue_health_body),
                        emoji = "🔄"
                    )

                    IssueRow(
                        title = stringResource(R.string.hc_issue_water_title),
                        subtitle = stringResource(R.string.hc_issue_water_body),
                        emoji = "💧"
                    )

                    IssueRow(
                        title = stringResource(R.string.hc_issue_lock_title),
                        subtitle = stringResource(R.string.hc_issue_lock_body),
                        emoji = "🔒"
                    )

                    IssueRow(
                        title = stringResource(R.string.hc_issue_backup_title),
                        subtitle = stringResource(R.string.hc_issue_backup_body),
                        emoji = "☁️"
                    )

                    IssueRow(
                        title = stringResource(R.string.hc_issue_widgets_title),
                        subtitle = stringResource(R.string.hc_issue_widgets_body),
                        emoji = "📲"
                    )
                }
            }

            if (showAttachInfo) {
                AlertDialog(
                    onDismissRequest = { showAttachInfo = false },
                    confirmButton = {
                        TextButton(onClick = { showAttachInfo = false }) {
                            Text(stringResource(R.string.hc_got_it), color = neonEnd)
                        }
                    },
                    title = {
                        Text(
                            text = stringResource(R.string.hc_attach_screenshot),
                            color = cs.onSurface,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(R.string.hc_attach_screenshot_info),
                            color = cs.onSurface.copy(alpha = 0.85f),
                            fontSize = 13.sp
                        )
                    },
                    containerColor = cardBg,
                    shape = RoundedCornerShape(18.dp)
                )
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = stringResource(R.string.hc_need_more_help),
                        color = cs.onSurface,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.SupportAgent,
                            contentDescription = null,
                            tint = neonEnd
                        )
                        Text(
                            text = stringResource(R.string.hc_contact_email_info),
                            color = cs.onSurface.copy(alpha = 0.75f),
                            fontSize = 13.sp
                        )
                    }

                    TextButton(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = "mailto:".toUri()
                                putExtra(Intent.EXTRA_EMAIL, arrayOf("stepforge0@gmail.com"))
                                putExtra(Intent.EXTRA_SUBJECT, ctx.getString(R.string.hc_support_subject))
                            }
                            try {
                                ctx.startActivity(intent)
                            } catch (_: Exception) {
                                Toast.makeText(
                                    ctx,
                                    ctx.getString(R.string.hc_no_email_app),
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.hc_contact_email),
                            color = neonEnd,
                            fontSize = 13.sp
                        )
                    }

                    Text(
                        text = stringResource(R.string.hc_sensitive_screenshot_warning),
                        color = cs.onSurface.copy(alpha = 0.7f),
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun IssueRow(
    title: String,
    subtitle: String,
    emoji: String
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Column {
            Text(
                text = title,
                color = cs.onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle,
                color = cs.onSurface.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }
    }
}

private suspend fun buildDiagnosticsBlock(ctx: Context): String =
    withContext(Dispatchers.IO) {
        val pm = ctx.packageManager
        val pkg = ctx.packageName

        val (versionName, versionCode) = try {
            val pi = pm.getPackageInfo(pkg, 0)
            val name = pi.versionName ?: "unknown"
            val code = if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode else pi.versionCode.toLong()
            name to code
        } catch (_: Exception) {
            "unknown" to 0L
        }

        val isDebuggable = try {
            (ctx.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        } catch (_: Exception) {
            false
        }

        val dm = ctx.resources.displayMetrics
        val locale = Locale.getDefault()

        val notifGranted = if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

        val activityRecGranted =
            ContextCompat.checkSelfPermission(
                ctx,
                Manifest.permission.ACTIVITY_RECOGNITION
            ) == PackageManager.PERMISSION_GRANTED

        val exactAlarmAllowed = try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val am = ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                am.canScheduleExactAlarms()
            } else true
        } catch (_: Exception) {
            false
        }

        val hcSdkStatus = try {
            HealthConnectClient.getSdkStatus(ctx).toString()
        } catch (_: Exception) {
            "unknown"
        }

        val firebaseState = try {
            val u = FirebaseAuth.getInstance().currentUser
            when {
                u == null -> "signed_out"
                u.isAnonymous -> "anonymous"
                else -> "signed_in"
            }
        } catch (_: Exception) {
            "unknown"
        }

        val prefs = try { ctx.stepforgeStore.data.first() } catch (_: Exception) { null }

        fun sKey(name: String) = stringPreferencesKey(name)
        fun iKey(name: String) = intPreferencesKey(name)
        fun bKey(name: String) = booleanPreferencesKey(name)

        val themeMode = prefs?.get(sKey("theme_mode")) ?: "unknown"
        val unit = prefs?.get(sKey("unit")) ?: "unknown"
        val stepGoal = prefs?.get(iKey("step_goal")) ?: -1
        val notifTime = prefs?.get(sKey("notif_time")) ?: "unknown"
        val syncAuto = prefs?.get(iKey("sync_auto_enabled")) ?: -1

        val waterEnabled = prefs?.get(bKey("water_enabled")) ?: false
        val waterInterval = prefs?.get(iKey("water_interval_min")) ?: -1
        val waterStart = prefs?.get(iKey("water_start_hour")) ?: -1
        val waterEnd = prefs?.get(iKey("water_end_hour")) ?: -1

        val appLockEnabled = (prefs?.get(iKey("app_lock_enabled")) ?: 0) == 1
        val appLockBio = (prefs?.get(iKey("app_lock_bio")) ?: 0) == 1
        val appLockTimeout = prefs?.get(iKey("app_lock_timeout")) ?: -1

        val backupEmail = prefs?.get(sKey("backup_email"))
        val backupConnected = !backupEmail.isNullOrBlank()

        val sig = appSignatureSha256(ctx) ?: "unknown"

        val now = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            .format(Date())

        buildString {
            appendLine("---")
            appendLine("Diagnostics (generated $now)")
            appendLine("---")
            appendLine("App")
            appendLine("  package=$pkg")
            appendLine("  version=$versionName ($versionCode)")
            appendLine("  debuggable=$isDebuggable")
            appendLine("  signature_sha256=$sig")
            appendLine()
            appendLine("Device")
            appendLine("  model=${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("  android=${Build.VERSION.RELEASE} (sdk=${Build.VERSION.SDK_INT})")
            appendLine("  locale=${locale.toLanguageTag()}")
            appendLine("  screen=${dm.widthPixels}x${dm.heightPixels} px, densityDpi=${dm.densityDpi}")
            appendLine()
            appendLine("Permissions")
            appendLine("  activity_recognition=$activityRecGranted")
            appendLine("  post_notifications=$notifGranted")
            appendLine("  exact_alarm_allowed=$exactAlarmAllowed")
            appendLine()
            appendLine("Health Connect")
            appendLine("  sdk_status=$hcSdkStatus")
            appendLine()
            appendLine("Account / Cloud")
            appendLine("  firebase_state=$firebaseState")
            appendLine("  backup_connected=$backupConnected")
            appendLine()
            appendLine("Settings snapshot")
            appendLine("  theme_mode=$themeMode")
            appendLine("  unit=$unit")
            appendLine("  step_goal=$stepGoal")
            appendLine("  notif_time=$notifTime")
            appendLine("  sync_auto_enabled=$syncAuto")
            appendLine("  water_enabled=$waterEnabled")
            appendLine("  water_interval_min=$waterInterval")
            appendLine("  water_window=$waterStart..$waterEnd")
            appendLine("  app_lock_enabled=$appLockEnabled")
            appendLine("  app_lock_biometric_allowed=$appLockBio")
            appendLine("  app_lock_timeout_sec=$appLockTimeout")
            appendLine()
            appendLine(RuntimeDiagnostics.build(ctx))
            appendLine("---")
        }
    }

private fun buildEmailSignature(ctx: Context): String {
    val sig = appSignatureSha256(ctx) ?: "unknown"
    val model = "${Build.MANUFACTURER} ${Build.MODEL}"
    return buildString {
        appendLine("--")
        appendLine("stepforge Support")
        appendLine("stepforge0@gmail.com")
        appendLine("Sent from StepForge on $model")
        appendLine("App signature (SHA-256): $sig")
    }
}

private fun appSignatureSha256(ctx: Context): String? {
    return try {
        val pm = ctx.packageManager
        val pkg = ctx.packageName
        val certBytes: ByteArray =
            if (Build.VERSION.SDK_INT >= 28) {
                val pi = pm.getPackageInfo(
                    pkg,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
                val signingInfo = pi.signingInfo ?: return null
                val signers = signingInfo.apkContentsSigners
                if (signers.isEmpty()) return null
                signers[0].toByteArray()
            } else {
                @Suppress("DEPRECATION")
                val pi = pm.getPackageInfo(
                    pkg,
                    PackageManager.GET_SIGNATURES
                )
                @Suppress("DEPRECATION")
                val sigs = pi.signatures
                if (sigs == null || sigs.isEmpty()) return null
                @Suppress("DEPRECATION")
                sigs[0].toByteArray()
            }

        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(certBytes)
        digest.joinToString(":") { b -> "%02X".format(b) }
    } catch (_: Exception) {
        null
    }
}
