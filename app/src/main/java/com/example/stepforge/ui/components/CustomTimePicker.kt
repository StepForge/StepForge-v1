package com.example.stepforge.ui.components

import android.widget.NumberPicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.viewinterop.AndroidView
import com.example.stepforge.ui.rememberUseDarkTheme

@Composable
fun CustomTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var hour by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }

    val ctx = LocalContext.current
    val useDark = rememberUseDarkTheme(ctx)

    // DARK: orijinal görünüm
    // LIGHT: daha koyu gri kart ki sayılar net görünsün
    val cardBg = if (useDark) {
        Color(0xFF111318)
    } else {
        Color(0xFFCFD3E0) // önce 0xFFE0E5EC idi, şimdi bir ton daha koyu
    }

    val titleColor = if (useDark) Color.White else Color(0xFF1A202C)
    val textColor = titleColor
    val accent = if (useDark) Color(0xFF00F5FF) else Color(0xFF4FD1C5)
    val buttonTextOnAccent = if (useDark) Color.Black else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = cardBg)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Select time",
                    color = titleColor,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Hour picker
                    AndroidView(
                        factory = { context ->
                            NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 23
                                value = hour
                                setFormatter { v: Int -> "%02d".format(v) }
                                setOnValueChangedListener { _, _, newVal -> hour = newVal }
                            }
                        },
                        update = { picker: NumberPicker -> picker.value = hour },
                        modifier = Modifier.height(120.dp)
                    )

                    Text(
                        ":",
                        color = textColor,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )

                    // Minute picker
                    AndroidView(
                        factory = { context ->
                            NumberPicker(context).apply {
                                minValue = 0
                                maxValue = 59
                                value = minute
                                setFormatter { v: Int -> "%02d".format(v) }
                                setOnValueChangedListener { _, _, newVal -> minute = newVal }
                            }
                        },
                        update = { picker: NumberPicker -> picker.value = minute },
                        modifier = Modifier.height(120.dp)
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowLeft,
                            contentDescription = null,
                            tint = accent
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("Cancel", color = accent)
                    }
                    Button(
                        onClick = { onConfirm(hour, minute) },
                        colors = ButtonDefaults.buttonColors(containerColor = accent),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Set", color = buttonTextOnAccent)
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = buttonTextOnAccent
                        )
                    }
                }
            }
        }
    }
}