package com.example.stepforge.settings

import android.app.DatePickerDialog
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Male
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Scale
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Transgender
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.stepforge.AchievementsActivity
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.achievements.AchievementsRepository
import com.example.stepforge.ui.achievements.AchievementsUiState
import com.example.stepforge.ui.achievements.ProfileAchievementEntryCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSettingsScreen(
    onClose: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    // Temadan renkler
    val colors = MaterialTheme.colorScheme
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f

    val darkBg = colors.background
    val cardBg = colors.surface

    // Neon gradient sabit kalsın (marka rengi)
    val neonA = Color(0xFF00FFA3)
    val neonB = Color(0xFF00F5FF)
    val neonBrush = Brush.horizontalGradient(listOf(neonA, neonB))

    // DataStore keys
    val USERNAME = stringPreferencesKey("username")
    val HEIGHT = intPreferencesKey("height")
    val WEIGHT = intPreferencesKey("weight")
    val GENDER = stringPreferencesKey("gender")
    val BIRTH_DATE = stringPreferencesKey("birth_date")
    val UNIT = stringPreferencesKey("unit")

    var username by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var unit by remember { mutableStateOf("km") }

    var lastSavedText by remember { mutableStateOf("All changes saved") }
    var showSavedFlash by remember { mutableStateOf(false) }
    var achievementsState by remember { mutableStateOf(AchievementsUiState.empty()) }

    fun calcAge(birth: String): Int {
        if (birth.isBlank()) return 0
        return try {
            val parts = birth.split("-", "/", ".", " ").filter { it.isNotBlank() }
            val (day, month, year) = when {
                parts.size == 3 && parts[0].length == 4 ->
                    Triple(parts[2].toInt(), parts[1].toInt(), parts[0].toInt())
                parts.size == 3 ->
                    Triple(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
                else -> Triple(0, 0, 0)
            }
            if (year == 0) return 0
            val now = Calendar.getInstance()
            var age = now.get(Calendar.YEAR) - year
            val mNow = now.get(Calendar.MONTH) + 1
            val dNow = now.get(Calendar.DAY_OF_MONTH)
            if (mNow < month || (mNow == month && dNow < day)) age--
            if (age < 0) 0 else age
        } catch (_: Exception) {
            0
        }
    }

    var age by remember { mutableStateOf(0) }

    // ilk yükleme
    LaunchedEffect(Unit) {
        val prefs = ctx.stepforgeStore.data.first()
        username = prefs[USERNAME] ?: ""
        height = prefs[HEIGHT]?.toString() ?: ""
        weight = prefs[WEIGHT]?.toString() ?: ""
        gender = prefs[GENDER] ?: ""
        birthDate = prefs[BIRTH_DATE] ?: ""
        unit = prefs[UNIT] ?: "km"
        age = calcAge(birthDate)
    }

    LaunchedEffect(Unit) {
        achievementsState = withContext(Dispatchers.IO) {
            val db = AppDatabase.getDatabase(ctx)
            val prefs = ctx.stepforgeStore.data.first()
            val goal = prefs[intPreferencesKey("step_goal")] ?: 10_000
            AchievementsRepository.buildState(
                dailySteps = db.dailyStepsDao().getAllSteps(),
                workouts = db.workoutSessionDao().getAll(),
                stepGoal = goal
            )
        }
    }

    // LIVE SAVE + küçük debounce
    LaunchedEffect(username, height, weight, gender, birthDate, unit) {
        delay(200)
        scope.launch {
            ctx.stepforgeStore.edit { ds ->
                ds[USERNAME] = username
                if (height.isNotEmpty()) ds[HEIGHT] = height.toIntOrNull() ?: 0
                if (weight.isNotEmpty()) ds[WEIGHT] = weight.toIntOrNull() ?: 0
                ds[GENDER] = gender
                ds[BIRTH_DATE] = birthDate
                ds[UNIT] = unit
            }
            age = calcAge(birthDate)
            lastSavedText = "All changes saved"
            showSavedFlash = true
            delay(1000)
            showSavedFlash = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Profile",
                            color = colors.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Your data is applied instantly across StepForge.",
                            color = colors.onBackground.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = darkBg)
            )
        }
    ) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(darkBg)
                .padding(pad)
        ) {
            // Hafif blur’lu arka plan efekti (sadece dark’ta biraz daha belirgin)
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .blur(40.dp)
                    .background(if (isDark) Color(0xFF05060A) else colors.background)
            )

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {

                // ----- HEADER KART (Avatar + Özet) -----
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(18.dp, RoundedCornerShape(26.dp)),
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF05070B) else colors.surfaceVariant)
                                .border(2.dp, neonBrush, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = neonB,
                                modifier = Modifier.size(38.dp)
                            )
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (username.isNotBlank()) username else "Tap to set your name",
                                color = colors.onSurface,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            val summary = buildString {
                                if (age > 0) append("$age yrs")
                                if (height.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("${height}cm")
                                }
                                if (weight.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append("${weight}kg")
                                }
                                if (gender.isNotBlank()) {
                                    if (isNotEmpty()) append(" • ")
                                    append(gender)
                                }
                            }
                            Text(
                                text = if (summary.isNotBlank()) summary else "Complete your profile for better insights.",
                                color = colors.onSurface.copy(alpha = 0.75f),
                                fontSize = 12.sp
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = if (isDark) Color(0xFF9FA4B3) else Color(0xFF7C7F8C),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                ProfileAchievementEntryCard(
                    state = achievementsState,
                    onClick = { ctx.startActivity(Intent(ctx, AchievementsActivity::class.java)) }
                )

                // ---- NAME ----
                LabeledFieldCard(
                    title = "Name",
                    description = "This name appears across StepForge in summaries and widgets.",
                    cardBg = cardBg,
                    onSurface = colors.onSurface
                ) {
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = fieldColors(neonB, colors)
                    )
                }

                // ---- HEIGHT & WEIGHT ----
                LabeledFieldCard(
                    title = "Body metrics",
                    description = "Used to estimate distance and calories from your steps.",
                    cardBg = cardBg,
                    onSurface = colors.onSurface
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        MetricField(
                            icon = Icons.Outlined.Straighten,
                            label = "Height (cm)",
                            value = height,
                            onValueChange = { v -> if (v.all(Char::isDigit)) height = v },
                            neon = neonB,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                        MetricField(
                            icon = Icons.Outlined.Scale,
                            label = "Weight (kg)",
                            value = weight,
                            onValueChange = { v -> if (v.all(Char::isDigit)) weight = v },
                            neon = neonB,
                            colors = colors,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // ---- GENDER ----
                LabeledFieldCard(
                    title = "Gender",
                    description = "Optional. Used only in aggregated stats.",
                    cardBg = cardBg,
                    onSurface = colors.onSurface
                ) {
                    GenderSelector(
                        selected = gender,
                        onSelect = { gender = it },
                        neonBrush = neonBrush,
                        isDark = isDark,
                        colors = colors
                    )
                }

                // ---- BIRTH DATE ----
                LabeledFieldCard(
                    title = "Birth date",
                    description = "Helps StepForge understand your age group.",
                    cardBg = cardBg,
                    onSurface = colors.onSurface
                ) {
                    BirthDateRow(
                        birthDate = birthDate,
                        age = age,
                        neon = neonB,
                        onPick = {
                            val cal = Calendar.getInstance()
                            val year = cal.get(Calendar.YEAR)
                            val month = cal.get(Calendar.MONTH)
                            val day = cal.get(Calendar.DAY_OF_MONTH)
                            DatePickerDialog(ctx, { _, y, m, d ->
                                birthDate = String.format("%02d-%02d-%04d", d, m + 1, y)
                            }, year, month, day).show()
                        },
                        colors = colors
                    )
                }

                // ---- UNIT ----
                LabeledFieldCard(
                    title = "Distance unit",
                    description = "Affects how your daily distance is displayed.",
                    cardBg = cardBg,
                    onSurface = colors.onSurface
                ) {
                    UnitSelector(
                        unit = unit,
                        onSelect = { unit = it },
                        neonBrush = neonBrush,
                        isDark = isDark,
                        colors = colors
                    )
                }

                Spacer(Modifier.height(20.dp))

                // ---- Close butonu ----
                Button(
                    onClick = onClose,
                    colors = ButtonDefaults.buttonColors(containerColor = neonB),
                    shape = RoundedCornerShape(50),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        "Close",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 16.sp
                    )
                }

                Spacer(Modifier.height(12.dp))
            }

            // Live saved info çubuğu
            val barColor by animateColorAsState(
                targetValue = if (showSavedFlash) neonB.copy(alpha = 0.9f)
                else if (isDark) Color(0xFF1E222B) else colors.surfaceVariant,
                animationSpec = tween(250),
                label = "saveBarColor"
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 8.dp)
                    .fillMaxWidth(0.6f)
                    .height(28.dp)
                    .background(barColor, RoundedCornerShape(999.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = lastSavedText,
                    color = Color.White,
                    fontSize = 11.sp
                )
            }
        }
    }
}

/* ---------- Yardımcı composable'lar ---------- */

@Composable
private fun LabeledFieldCard(
    title: String,
    description: String,
    cardBg: Color,
    onSurface: Color,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title,
                color = onSurface,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = description,
                color = onSurface.copy(alpha = 0.65f),
                fontSize = 11.sp
            )
            Spacer(Modifier.height(6.dp))
            content()
        }
    }
}

@Composable
private fun MetricField(
    icon: ImageVector,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    neon: Color,
    colors: ColorScheme,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = neon)
        },
        modifier = modifier,
        colors = fieldColors(neon, colors)
    )
}

@Composable
private fun GenderSelector(
    selected: String,
    onSelect: (String) -> Unit,
    neonBrush: Brush,
    isDark: Boolean,
    colors: ColorScheme
) {
    val options = listOf(
        Triple("Male", Icons.Outlined.Male, "M"),
        Triple("Female", Icons.Outlined.Transgender, "F"),
        Triple("Other", Icons.Outlined.Person, "O")
    )

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { (label, icon, _) ->
            val isSelected = selected == label
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "genderScale"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(
                        if (isSelected)
                            if (isDark) Color(0xFF151821) else colors.surfaceVariant
                        else if (isDark) Color(0xFF0F1116) else Color.White,
                        RoundedCornerShape(18.dp)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        brush = if (isSelected) neonBrush
                        else SolidColor(
                            if (isDark) Color(0xFF303341)
                            else colors.outline
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelect(label) },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF00F5FF)
                        else if (isDark) Color(0xFFB0B5C7) else colors.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = label,
                        color = if (isSelected) Color(0xFF00F5FF) else colors.onSurface,
                        fontSize = 13.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun BirthDateRow(
    birthDate: String,
    age: Int,
    neon: Color,
    onPick: () -> Unit,
    colors: ColorScheme
) {
    Row(
        Modifier
            .fillMaxWidth()
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color(0xFF2C2F36), Color(0xFF1E2026))),
                RoundedCornerShape(14.dp)
            )
            .clickable { onPick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            tint = neon
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = if (birthDate.isEmpty()) "Select birth date" else birthDate,
            color = colors.onSurface,
            fontSize = 14.sp
        )
        Spacer(Modifier.weight(1f))
        if (age > 0) {
            Text(
                text = "$age yrs",
                color = colors.onSurface.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun UnitSelector(
    unit: String,
    onSelect: (String) -> Unit,
    neonBrush: Brush,
    isDark: Boolean,
    colors: ColorScheme
) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        listOf("km", "mi").forEach { opt ->
            val isSelected = unit == opt
            val scale by animateFloatAsState(
                targetValue = if (isSelected) 1.05f else 1f,
                animationSpec = tween(250, easing = FastOutSlowInEasing),
                label = "unitScale"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
                    .graphicsLayer(scaleX = scale, scaleY = scale)
                    .background(
                        if (isSelected)
                            if (isDark) Color(0xFF151821) else colors.surfaceVariant
                        else if (isDark) Color(0xFF0F1116) else Color.White,
                        RoundedCornerShape(18.dp)
                    )
                    .border(
                        width = if (isSelected) 2.dp else 1.dp,
                        brush = if (isSelected) neonBrush
                        else SolidColor(
                            if (isDark) Color(0xFF303341) else colors.outline
                        ),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelect(opt) }
                    .padding(vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = opt.uppercase(),
                    color = if (isSelected) Color(0xFF00F5FF) else colors.onSurface,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun fieldColors(
    neon: Color,
    colors: ColorScheme
) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = neon,
    focusedLabelColor = neon,
    unfocusedBorderColor = colors.outline,
    unfocusedLabelColor = colors.onSurfaceVariant,
    focusedTextColor = colors.onSurface,
    unfocusedTextColor = colors.onSurface,
    cursorColor = neon
)