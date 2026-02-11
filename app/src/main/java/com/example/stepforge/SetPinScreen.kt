package com.example.stepforge.settings

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.data.stepforgeStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetPinScreen(
    onClose: () -> Unit,
    onPinSaved: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val KEY_APP_LOCK_ENABLED = intPreferencesKey("app_lock_enabled")
    val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    val KEY_APP_LOCK_BIOMETRIC = intPreferencesKey("app_lock_bio")

    val KEY_SEC_Q1 = stringPreferencesKey("security_q1")
    val KEY_SEC_A1 = stringPreferencesKey("security_a1")
    val KEY_SEC_Q2 = stringPreferencesKey("security_q2")
    val KEY_SEC_A2 = stringPreferencesKey("security_a2")

    val darkBg = Color(0xFF0B0B0E)
    val cardBg = Color(0xFF111318)
    val neonA = Color(0xFF00FFA3)
    val neonB = Color(0xFF00F5FF)
    val neon = Brush.horizontalGradient(listOf(neonA, neonB))

    var pin by remember { mutableStateOf(TextFieldValue("")) }
    var pinConfirm by remember { mutableStateOf(TextFieldValue("")) }
    var pinError by remember { mutableStateOf<String?>(null) }

    // 10 sabit soru
    val questionList = listOf(
        "What is your favorite color?",
        "What is your favorite food?",
        "What city were you born in?",
        "What is the name of your first pet?",
        "What is your favorite movie?",
        "What is your favorite sport?",
        "What is your favorite game?",
        "What is your favorite car brand?",
        "What is your favorite song?",
        "What is your favorite season?"
    )

    // Soru 1
    var q1Expanded by remember { mutableStateOf(false) }
    var q1Text by remember { mutableStateOf(questionList[0]) }
    var a1 by remember { mutableStateOf(TextFieldValue("")) }

    // Soru 2
    var q2Expanded by remember { mutableStateOf(false) }
    // Başlangıçta ikinci soru için farklı bir varsayılan seçelim
    var q2Text by remember { mutableStateOf(questionList[1]) }
    var a2 by remember { mutableStateOf(TextFieldValue("")) }

    var aError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Set App Lock PIN",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Create a PIN and 2 security answers.",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // --- PIN KARTI ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = neonB,
                            modifier = Modifier.size(32.dp)
                        )
                        Column {
                            Text(
                                text = "Create your PIN",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "This 4‑digit PIN will be required when opening stepforge.",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }

                    OutlinedTextField(
                        value = pin,
                        onValueChange = {
                            if (it.text.length <= 4 && it.text.all(Char::isDigit))
                                pin = it
                            pinError = null
                        },
                        singleLine = true,
                        label = { Text("PIN (4 digits)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            unfocusedBorderColor = Color(0xFF2F323A),
                            cursorColor = Color(0xFF00F5FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = {
                            if (it.text.length <= 4 && it.text.all(Char::isDigit))
                                pinConfirm = it
                            pinError = null
                        },
                        singleLine = true,
                        label = { Text("Confirm PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            unfocusedBorderColor = Color(0xFF2F323A),
                            cursorColor = Color(0xFF00F5FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (pinError != null) {
                        Text(
                            text = pinError ?: "",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // --- SECURITY QUESTIONS KARTI ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Security questions",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "If you forget your PIN, stepforge will ask these questions so you can reset it.",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(8.dp))

                    // SORU 1
                    Text(
                        text = "Question 1",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(Color(0xFF151821), RoundedCornerShape(12.dp))
                            .clickable { q1Expanded = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = q1Text,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = q1Expanded,
                        onDismissRequest = { q1Expanded = false }
                    ) {
                        questionList.forEach { q ->
                            DropdownMenuItem(
                                text = { Text(q) },
                                onClick = {
                                    q1Text = q
                                    // Eğer Soru 2 ile çakışıyorsa, Soru 2'yi değiştirelim
                                    if (q2Text == q1Text) {
                                        q2Text = questionList.first { it != q1Text }
                                    }
                                    q1Expanded = false
                                }
                            )
                        }
                    }

                    OutlinedTextField(
                        value = a1,
                        onValueChange = {
                            a1 = it
                            aError = null
                        },
                        singleLine = true,
                        placeholder = { Text("Answer for Question 1") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            unfocusedBorderColor = Color(0xFF2F323A),
                            cursorColor = Color(0xFF00F5FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    Spacer(Modifier.height(6.dp))

                    // SORU 2
                    Text(
                        text = "Question 2",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .background(Color(0xFF151821), RoundedCornerShape(12.dp))
                            .clickable { q2Expanded = true }
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = q2Text,
                                color = Color.White,
                                fontSize = 13.sp
                            )
                            Icon(
                                imageVector = Icons.Outlined.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = q2Expanded,
                        onDismissRequest = { q2Expanded = false }
                    ) {
                        questionList.forEach { q ->
                            // Soru 1 ile aynı olmasın
                            if (q != q1Text) {
                                DropdownMenuItem(
                                    text = { Text(q) },
                                    onClick = {
                                        q2Text = q
                                        q2Expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = a2,
                        onValueChange = {
                            a2 = it
                            aError = null
                        },
                        singleLine = true,
                        placeholder = { Text("Answer for Question 2") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF00F5FF),
                            unfocusedBorderColor = Color(0xFF2F323A),
                            cursorColor = Color(0xFF00F5FF),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )

                    if (aError != null) {
                        Text(
                            text = aError ?: "",
                            color = Color(0xFFFF8A80),
                            fontSize = 11.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            Button(
                onClick = {
                    val p = pin.text.trim()
                    val pc = pinConfirm.text.trim()
                    val ans1 = a1.text.trim()
                    val ans2 = a2.text.trim()

                    if (p.length != 4 || !p.all(Char::isDigit) || p != pc) {
                        pinError = "PINs must match and be 4 digits."
                        return@Button
                    }
                    if (ans1.length < 2 || ans2.length < 2) {
                        aError = "Please enter answers for both questions."
                        return@Button
                    }
                    if (q1Text == q2Text) {
                        aError = "Questions must be different."
                        return@Button
                    }

                    scope.launch {
                        ctx.stepforgeStore.edit { prefs ->
                            prefs[KEY_APP_LOCK_PIN] = p
                            prefs[KEY_APP_LOCK_ENABLED] = 1
                            prefs[KEY_APP_LOCK_BIOMETRIC] = 0

                            prefs[KEY_SEC_Q1] = q1Text
                            prefs[KEY_SEC_A1] = ans1.lowercase().trim()
                            prefs[KEY_SEC_Q2] = q2Text
                            prefs[KEY_SEC_A2] = ans2.lowercase().trim()
                        }
                        Toast.makeText(
                            ctx,
                            "App Lock PIN created.",
                            Toast.LENGTH_SHORT
                        ).show()
                        onPinSaved()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
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
                        "Save PIN",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}