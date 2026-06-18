package com.example.stepforge.ui.water

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.LocalDrink
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.example.stepforge.R
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.DailyWater
import com.example.stepforge.data.WaterIntakeEvent
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.notification.WaterReminderScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun WaterReminderScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val screenBg = if (isDark) Color(0xFF05070B) else Color(0xFFF6FAFB)
    val cardBg = if (isDark) Color(0xFF071119) else Color.White
    val subtleCard = if (isDark) Color(0xFF0B1720) else Color(0xFFF2F8FA)
    val border = if (isDark) Color.White.copy(alpha = 0.09f) else Color.Black.copy(alpha = 0.07f)
    val accent = Color(0xFF00F5C8)
    val gold = Color(0xFFFFD66B)

    val keyEnabled = remember { booleanPreferencesKey("water_enabled") }
    val keyInterval = remember { intPreferencesKey("water_interval_min") }
    val keyStartHour = remember { intPreferencesKey("water_start_hour") }
    val keyEndHour = remember { intPreferencesKey("water_end_hour") }
    val keyGoal = remember { intPreferencesKey("water_goal_ml") }
    val keyToday = remember { intPreferencesKey("water_today_ml") }
    val keyDate = remember { intPreferencesKey("water_date_yyyymmdd") }
    val keyPremium = remember { intPreferencesKey("premium_enabled") }

    val premiumKeys = remember {
        mapOf(
            WaterPremiumFeature.SMART_SCHEDULE.id to booleanPreferencesKey("water_premium_smart_schedule"),
            WaterPremiumFeature.ADAPTIVE.id to booleanPreferencesKey("water_premium_adaptive_reminders"),
            WaterPremiumFeature.CUSTOM_SOUNDS.id to booleanPreferencesKey("water_premium_custom_sounds"),
            WaterPremiumFeature.ANALYTICS.id to booleanPreferencesKey("water_premium_analytics"),
            WaterPremiumFeature.QUIET_HOURS.id to booleanPreferencesKey("water_premium_quiet_hours")
        )
    }

    val db = remember { AppDatabase.getDatabase(context) }
    val waterDao = remember { db.dailyWaterDao() }
    val eventDao = remember { db.waterIntakeEventDao() }
    val dateFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val chipDateFormatter = remember { SimpleDateFormat("MMM d", Locale.getDefault()) }

    var enabled by remember { mutableStateOf(false) }
    var intervalMin by remember { mutableIntStateOf(60) }
    var startHour by remember { mutableIntStateOf(8) }
    var endHour by remember { mutableIntStateOf(22) }
    var goalMl by remember { mutableIntStateOf(2500) }
    var todayMl by remember { mutableIntStateOf(0) }
    var premiumEnabled by remember { mutableStateOf(false) }
    val premiumStates = remember { mutableStateMapOf<String, Boolean>() }

    var showCustomAmountDialog by remember { mutableStateOf(false) }
    var customAmountText by remember { mutableStateOf("") }
    var showGoalDialog by remember { mutableStateOf(false) }
    var goalText by remember { mutableStateOf("") }
    var canUndoWater by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(dateFormatter.format(Date())) }
    var showCalendarDialog by remember { mutableStateOf(false) }
    var datesWithWaterData by remember { mutableStateOf<Set<String>>(emptySet()) }

    val bottlePulse = remember { Animatable(1f) }

    BackHandler(enabled = !showCustomAmountDialog && !showGoalDialog && !showCalendarDialog) {
        onBack()
    }

    fun todayDateString(): String = dateFormatter.format(Date())
    fun isToday(date: String): Boolean = date == todayDateString()

    fun dateToCalendar(date: String): Calendar {
        return Calendar.getInstance().apply {
            time = dateFormatter.parse(date) ?: Date()
        }
    }

    fun calendarToDate(calendar: Calendar): String = dateFormatter.format(calendar.time)

    fun displayDateLabel(date: String): String {
        return if (isToday(date)) {
            context.getString(R.string.wr_today)
        } else {
            chipDateFormatter.format(dateToCalendar(date).time)
        }
    }

    fun eventTimeMillisFor(date: String): Long {
        val selected = dateToCalendar(date)
        val now = Calendar.getInstance()
        selected.set(Calendar.HOUR_OF_DAY, now.get(Calendar.HOUR_OF_DAY))
        selected.set(Calendar.MINUTE, now.get(Calendar.MINUTE))
        selected.set(Calendar.SECOND, now.get(Calendar.SECOND))
        selected.set(Calendar.MILLISECOND, now.get(Calendar.MILLISECOND))
        return selected.timeInMillis
    }

    fun todayInt(): Int {
        val c = Calendar.getInstance()
        return c.get(Calendar.YEAR) * 10000 + (c.get(Calendar.MONTH) + 1) * 100 + c.get(Calendar.DAY_OF_MONTH)
    }

    suspend fun persistWater(date: String, newAmount: Int) {
        val safeAmount = newAmount.coerceAtLeast(0)
        if (isToday(date)) {
            val day = todayInt()
            context.stepforgeStore.edit { prefs ->
                prefs[keyToday] = safeAmount
                prefs[keyDate] = day
                prefs[keyGoal] = goalMl
            }
        }
        waterDao.insertDailyWater(DailyWater(date = date, waterMl = safeAmount))
    }

    fun loadWaterForDate(date: String) {
        scope.launch {
            val loadedWater = withContext(Dispatchers.IO) {
                waterDao.getWaterForDate(date)?.waterMl ?: 0
            }
            val canUndo = withContext(Dispatchers.IO) {
                eventDao.getLatestForDate(date) != null
            }
            selectedDate = date
            todayMl = loadedWater
            canUndoWater = canUndo
        }
    }

    fun addWater(amount: Int) {
        if (amount <= 0) return
        scope.launch {
            val oldTotal = todayMl
            val date = selectedDate
            val maxTotal = (goalMl * 2).coerceAtLeast(goalMl)
            val newTotal = (oldTotal + amount).coerceIn(0, maxTotal)
            val actualAdded = (newTotal - oldTotal).coerceAtLeast(0)
            if (actualAdded <= 0) return@launch
            todayMl = newTotal
            withContext(Dispatchers.IO) {
                persistWater(date, newTotal)
                eventDao.insert(
                    WaterIntakeEvent(
                        date = date,
                        timeMillis = eventTimeMillisFor(date),
                        amountMl = actualAdded
                    )
                )
            }
            canUndoWater = true
            datesWithWaterData = datesWithWaterData + date
            bottlePulse.snapTo(1f)
            bottlePulse.animateTo(1.035f, tween(130, easing = EaseOutCubic))
            bottlePulse.animateTo(1f, tween(180, easing = EaseOutCubic))
        }
    }

    fun undoLastWater() {
        scope.launch {
            val date = selectedDate
            val latest = withContext(Dispatchers.IO) { eventDao.getLatestForDate(date) }
            if (latest == null) {
                canUndoWater = false
                return@launch
            }

            val newTotal = (todayMl - latest.amountMl).coerceAtLeast(0)
            todayMl = newTotal
            val hasRemainingEvents = withContext(Dispatchers.IO) {
                eventDao.deleteById(latest.id)
                persistWater(date, newTotal)
                eventDao.getLatestForDate(date) != null
            }
            canUndoWater = hasRemainingEvents
            datesWithWaterData = if (newTotal > 0) {
                datesWithWaterData + date
            } else {
                datesWithWaterData - date
            }
        }
    }

    fun setEnabled(value: Boolean) {
        enabled = value
        scope.launch {
            context.stepforgeStore.edit { prefs ->
                prefs[keyEnabled] = value
                prefs[keyInterval] = intervalMin
                prefs[keyStartHour] = startHour
                prefs[keyEndHour] = endHour
            }
            if (value) WaterReminderScheduler.schedule(context) else WaterReminderScheduler.cancel(context)
        }
    }

    fun setInterval(value: Int) {
        intervalMin = value
        scope.launch {
            context.stepforgeStore.edit { prefs -> prefs[keyInterval] = value }
            if (enabled) WaterReminderScheduler.schedule(context)
        }
    }

    fun updatePremiumFeature(featureId: String, enabledValue: Boolean) {
        if (!premiumEnabled) {
            Toast.makeText(context, context.getString(R.string.wr_premium_required), Toast.LENGTH_SHORT).show()
            return
        }
        premiumStates[featureId] = enabledValue
        scope.launch {
            premiumKeys[featureId]?.let { key ->
                context.stepforgeStore.edit { prefs -> prefs[key] = enabledValue }
            }
        }
    }

    LaunchedEffect(Unit) {
        val prefs = context.stepforgeStore.data.first()
        enabled = prefs[keyEnabled] ?: false
        intervalMin = prefs[keyInterval] ?: 60
        startHour = prefs[keyStartHour] ?: 8
        endHour = prefs[keyEndHour] ?: 22
        goalMl = prefs[keyGoal] ?: 2500
        premiumEnabled = (prefs[keyPremium] ?: 0) == 1

        val storedDate = prefs[keyDate] ?: 0
        val storedToday = prefs[keyToday] ?: 0
        if (storedDate == todayInt()) {
            todayMl = storedToday
        } else {
            todayMl = 0
            context.stepforgeStore.edit { edit ->
                edit[keyDate] = todayInt()
                edit[keyToday] = 0
            }
        }

        withContext(Dispatchers.IO) {
            waterDao.insertDailyWater(DailyWater(date = todayDateString(), waterMl = todayMl))
        }
        datesWithWaterData = withContext(Dispatchers.IO) {
            waterDao.getAllWater()
                .filter { it.waterMl > 0 }
                .map { it.date }
                .toSet()
        }

        premiumKeys.forEach { (id, key) ->
            premiumStates[id] = prefs[key] ?: false
        }
        canUndoWater = withContext(Dispatchers.IO) {
            eventDao.getLatestForDate(todayDateString()) != null
        }
    }

    LaunchedEffect(goalMl) {
        if (goalMl > 0) {
            context.stepforgeStore.edit { prefs -> prefs[keyGoal] = goalMl }
            withContext(Dispatchers.IO) {
                waterDao.insertDailyWater(DailyWater(date = selectedDate, waterMl = todayMl))
            }
        }
    }

    val progress by remember(todayMl, goalMl) {
        derivedStateOf { todayMl.toFloat() / goalMl.coerceAtLeast(1).toFloat() }
    }
    val percent = (progress.coerceIn(0f, 1f) * 100f).roundToInt()
    val remaining = (goalMl - todayMl).coerceAtLeast(0)
    val selectedDateLabel = displayDateLabel(selectedDate)

    Scaffold(
        containerColor = screenBg,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            if (isDark) Color(0xFF07161D) else Color(0xFFEFFBFA),
                            screenBg,
                            screenBg
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp)
                    .padding(top = 8.dp, bottom = 34.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                WaterFixedHeader(
                    accent = accent,
                    onBack = onBack,
                    onStatsClick = {
                        Toast.makeText(
                            context,
                            if (premiumEnabled) context.getString(R.string.wr_premium_analytics_title) else context.getString(R.string.wr_premium_required),
                            Toast.LENGTH_SHORT
                        ).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                HydrationBottleHero(
                    todayMl = todayMl,
                    goalMl = goalMl,
                    dateLabel = selectedDateLabel,
                    isSelectedToday = isToday(selectedDate),
                    progress = progress.coerceIn(0f, 1f),
                    percent = percent,
                    remainingMl = remaining,
                    pulseScale = bottlePulse.value,
                    cardBg = cardBg,
                    border = border,
                    accent = accent,
                    onGoalClick = {
                        goalText = goalMl.toString()
                        showGoalDialog = true
                    },
                    onDateClick = { showCalendarDialog = true }
                )

                QuickAddSection(
                    cardBg = cardBg,
                    border = border,
                    accent = accent,
                    canUndo = canUndoWater,
                    onAdd = ::addWater,
                    onUndo = ::undoLastWater,
                    onCustom = {
                        customAmountText = ""
                        showCustomAmountDialog = true
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ReminderToggleCard(
                        modifier = Modifier.weight(1f),
                        subtitle = if (enabled) stringResource(R.string.wr_reminders_enabled) else stringResource(R.string.wr_reminders_disabled),
                        enabled = enabled,
                        cardBg = cardBg,
                        border = border,
                        accent = accent,
                        onCheckedChange = ::setEnabled
                    )

                    ReminderIntervalCard(
                        modifier = Modifier.weight(1f),
                        intervalMin = intervalMin,
                        cardBg = cardBg,
                        border = border,
                        accent = accent,
                        onCycle = {
                            val options = listOf(30, 45, 60, 90, 120)
                            val currentIndex = options.indexOf(intervalMin).takeIf { it >= 0 } ?: 2
                            setInterval(options[(currentIndex + 1) % options.size])
                        }
                    )
                }

                HydrationStreakCard(
                    cardBg = cardBg,
                    border = border,
                    accent = accent,
                    todayMl = todayMl,
                    goalMl = goalMl
                )

                WaterPremiumSection(
                    premiumEnabled = premiumEnabled,
                    features = premiumStates.toMap(),
                    onToggleFeature = ::updatePremiumFeature,
                    onUnlockClick = {
                        Toast.makeText(context, context.getString(R.string.wr_open_premium), Toast.LENGTH_SHORT).show()
                    },
                    cardBg = cardBg,
                    subtleCard = subtleCard,
                    border = border,
                    accent = accent,
                    gold = gold
                )
            }
        }
    }

    if (showCustomAmountDialog) {
        WaterAmountDialog(
            title = stringResource(R.string.wr_custom_water_title),
            value = customAmountText,
            placeholder = stringResource(R.string.wr_amount_placeholder),
            confirmText = stringResource(R.string.wr_add),
            onValueChange = { customAmountText = it.filter(Char::isDigit).take(5) },
            onDismiss = { showCustomAmountDialog = false },
            onConfirm = {
                val amount = customAmountText.toIntOrNull() ?: 0
                if (amount > 0) {
                    addWater(amount)
                    showCustomAmountDialog = false
                }
            }
        )
    }

    if (showGoalDialog) {
        WaterAmountDialog(
            title = stringResource(R.string.wr_daily_goal_title),
            value = goalText,
            placeholder = stringResource(R.string.wr_goal_placeholder),
            confirmText = stringResource(R.string.wr_save),
            onValueChange = { goalText = it.filter(Char::isDigit).take(5) },
            onDismiss = { showGoalDialog = false },
            onConfirm = {
                val newGoal = goalText.toIntOrNull() ?: goalMl
                goalMl = newGoal.coerceIn(500, 10000)
                showGoalDialog = false
            }
        )
    }

    if (showCalendarDialog) {
        WaterCalendarDialog(
            selectedDate = selectedDate,
            datesWithData = datesWithWaterData,
            accent = accent,
            cardBg = cardBg,
            border = border,
            onDismiss = { showCalendarDialog = false },
            onDateSelected = { date ->
                showCalendarDialog = false
                loadWaterForDate(date)
            }
        )
    }
}

@Composable
private fun WaterFixedHeader(
    accent: Color,
    onBack: () -> Unit,
    onStatsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .padding(top = 4.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = stringResource(R.string.hc_back),
                tint = cs.onSurface,
                modifier = Modifier.size(27.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.wr_title),
                    color = cs.onSurface,
                    fontSize = 20.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.ExtraBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = Icons.Outlined.WaterDrop,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier
                        .padding(start = 7.dp)
                        .size(18.dp)
                )
            }
            Text(
                text = stringResource(R.string.wr_subtitle),
                color = cs.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Box(
            modifier = Modifier
                .size(40.dp)
                .background(accent.copy(alpha = 0.10f), RoundedCornerShape(16.dp))
                .clickable(onClick = onStatsClick),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.BarChart,
                contentDescription = stringResource(R.string.wr_premium_analytics_title),
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
@Composable
private fun QuickAddSection(
    cardBg: Color,
    border: Color,
    accent: Color,
    canUndo: Boolean,
    onAdd: (Int) -> Unit,
    onUndo: () -> Unit,
    onCustom: () -> Unit
) {
    GlassPanel(cardBg = cardBg, border = border, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(13.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.wr_quick_add),
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(
                            if (canUndo) accent.copy(alpha = 0.11f) else Color.White.copy(alpha = 0.035f),
                            RoundedCornerShape(13.dp)
                        )
                        .border(
                            1.dp,
                            if (canUndo) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.045f),
                            RoundedCornerShape(13.dp)
                        )
                        .clickable(enabled = canUndo, onClick = onUndo),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Outlined.Undo,
                        contentDescription = stringResource(R.string.wr_undo_last),
                        tint = if (canUndo) accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.46f),
                        modifier = Modifier.size(17.dp)
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                QuickAddChip(stringResource(R.string.wr_quick_250), accent, modifier = Modifier.weight(1f)) { onAdd(250) }
                QuickAddChip(stringResource(R.string.wr_quick_500), accent, modifier = Modifier.weight(1f)) { onAdd(500) }
                QuickAddChip(stringResource(R.string.wr_quick_750), accent, modifier = Modifier.weight(1f)) { onAdd(750) }
                QuickAddChip(stringResource(R.string.wr_custom), accent, modifier = Modifier.weight(1f)) { onCustom() }
            }
        }
    }
}

@Composable
private fun QuickAddChip(
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .height(44.dp)
            .background(
                Color.White.copy(alpha = 0.055f),
                RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            Icon(Icons.Outlined.LocalDrink, contentDescription = null, tint = accent, modifier = Modifier.size(13.dp))
            Text(
                text = label,
                color = cs.onSurface,
                fontWeight = FontWeight.Bold,
                fontSize = 8.8.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ReminderToggleCard(
    modifier: Modifier,
    subtitle: String,
    enabled: Boolean,
    cardBg: Color,
    border: Color,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    GlassPanel(cardBg = cardBg, border = border, modifier = modifier.height(118.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = stringResource(R.string.wr_reminders),
                        color = cs.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.8.sp,
                        lineHeight = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        color = cs.onSurfaceVariant,
                        fontSize = 8.8.sp,
                        lineHeight = 10.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Switch(
                    checked = enabled,
                    onCheckedChange = onCheckedChange,
                    modifier = Modifier
                        .size(width = 42.dp, height = 28.dp)
                        .scale(0.82f),
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = accent,
                        uncheckedThumbColor = Color.White.copy(alpha = 0.80f)
                    )
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color.White.copy(alpha = 0.075f), RoundedCornerShape(999.dp))
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    Icons.Outlined.Notifications,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(21.dp)
                )
                Text(
                    text = stringResource(
                        if (enabled) R.string.wr_reminders_body_enabled else R.string.wr_reminders_body_disabled
                    ),
                    color = cs.onSurfaceVariant,
                    fontSize = 8.9.sp,
                    lineHeight = 11.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun ReminderIntervalCard(
    modifier: Modifier,
    intervalMin: Int,
    cardBg: Color,
    border: Color,
    accent: Color,
    onCycle: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    GlassPanel(cardBg = cardBg, border = border, modifier = modifier.height(118.dp)) {
        Column(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = stringResource(R.string.wr_reminder_interval),
                color = cs.onSurface,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.8.sp,
                lineHeight = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(15.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.07f), RoundedCornerShape(15.dp))
                    .clickable(onClick = onCycle)
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Icon(
                    Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = stringResource(R.string.wr_interval_minutes, intervalMin),
                        color = cs.onSurface,
                        fontSize = 15.5.sp,
                        lineHeight = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                    Text(
                        text = intervalSubtitle(intervalMin),
                        color = cs.onSurfaceVariant,
                        fontSize = 8.8.sp,
                        lineHeight = 10.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    contentDescription = null,
                    tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
private fun HydrationStreakCard(
    cardBg: Color,
    border: Color,
    accent: Color,
    todayMl: Int,
    goalMl: Int
) {
    val cs = MaterialTheme.colorScheme
    val completedToday = todayMl >= goalMl
    GlassPanel(cardBg = cardBg, border = border, modifier = Modifier.fillMaxWidth().height(58.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(accent.copy(alpha = 0.11f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.progress_fire_icon),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.wr_daily_streak),
                        color = cs.onSurface,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.6.sp,
                        lineHeight = 10.8.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (completedToday) stringResource(R.string.wr_goal_reached_today) else stringResource(R.string.wr_keep_it_going),
                        color = cs.onSurfaceVariant,
                        fontSize = 7.6.sp,
                        lineHeight = 8.8.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(width = 1.dp, height = 34.dp)
                    .background(Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
            )

            Row(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = stringResource(R.string.wr_streak_days_count),
                        color = accent,
                        fontSize = 21.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = stringResource(R.string.wr_days_suffix).trim(),
                        color = cs.onSurfaceVariant,
                        fontSize = 8.8.sp,
                        lineHeight = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(start = 3.dp, bottom = 2.dp)
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val labels = listOf("M", "T", "W", "T", "F", "S", "S")
                    labels.forEachIndexed { index, label ->
                        val active = index < 6 || completedToday
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.5.dp)
                                    .background(
                                        if (active) accent.copy(alpha = 0.78f) else Color.Transparent,
                                        RoundedCornerShape(999.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (active) accent.copy(alpha = 0.42f) else cs.onSurfaceVariant.copy(alpha = 0.45f),
                                        RoundedCornerShape(999.dp)
                                    )
                            )
                            Text(
                                text = label,
                                color = cs.onSurfaceVariant,
                                fontSize = 5.8.sp,
                                lineHeight = 6.5.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun intervalSubtitle(intervalMin: Int): String {
    return if (intervalMin == 60) {
        stringResource(R.string.wr_interval_every_hour)
    } else {
        stringResource(R.string.wr_interval_cycle, intervalMin)
    }
}

@Composable
internal fun GlassPanel(
    cardBg: Color,
    border: Color,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .background(cardBg, RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.032f), Color.Transparent)),
                RoundedCornerShape(24.dp)
            )
            .border(BorderStroke(1.dp, border), RoundedCornerShape(24.dp))
    ) {
        content()
    }
}

@Composable
private fun WaterAmountDialog(
    title: String,
    value: String,
    placeholder: String,
    confirmText: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val accent = Color(0xFF00F5C8)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.86f),
            shape = RoundedCornerShape(24.dp),
            color = if (isDark) Color(0xFF08131A) else Color.White,
            shadowElevation = 18.dp,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Text(
                    text = title,
                    color = cs.onSurface,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp,
                    lineHeight = 21.sp
                )

                OutlinedTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    placeholder = { Text(placeholder) },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.wr_cancel),
                            color = cs.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Button(
                        onClick = onConfirm,
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.Black
                        )
                    ) {
                        Text(confirmText, fontWeight = FontWeight.ExtraBold)
                    }
                }
            }
        }
    }
}

@Composable
private fun WaterCalendarDialog(
    selectedDate: String,
    datesWithData: Set<String>,
    accent: Color,
    cardBg: Color,
    border: Color,
    onDismiss: () -> Unit,
    onDateSelected: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val keyFormatter = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val monthFormatter = remember { SimpleDateFormat("MMMM yyyy", Locale.getDefault()) }
    val todayDate = remember { keyFormatter.format(Date()) }
    var visibleMonth by remember(selectedDate) {
        mutableStateOf(
            Calendar.getInstance().apply {
                time = keyFormatter.parse(selectedDate) ?: Date()
                set(Calendar.DAY_OF_MONTH, 1)
            }
        )
    }

    val firstDay = (visibleMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
    val daysInMonth = firstDay.getActualMaximum(Calendar.DAY_OF_MONTH)
    val leadingBlanks = (firstDay.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7
    val trailingBlanks = (7 - ((leadingBlanks + daysInMonth) % 7)) % 7
    val days = List(leadingBlanks) { null } +
        (1..daysInMonth).map { it } +
        List(trailingBlanks) { null }
    val weekLabels = listOf("M", "T", "W", "T", "F", "S", "S")

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.90f),
            shape = RoundedCornerShape(26.dp),
            color = cardBg,
            shadowElevation = 18.dp,
            border = BorderStroke(1.dp, border)
        ) {
            Column(
                modifier = Modifier.padding(15.dp),
                verticalArrangement = Arrangement.spacedBy(13.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.wr_calendar_title),
                            color = cs.onSurface,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            text = monthFormatter.format(visibleMonth.time),
                            color = cs.onSurfaceVariant,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.sp,
                            lineHeight = 13.sp
                        )
                    }

                    CalendarNavButton(
                        accent = accent,
                        onClick = {
                            visibleMonth = (visibleMonth.clone() as Calendar).apply {
                                add(Calendar.MONTH, -1)
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    CalendarNavButton(
                        accent = accent,
                        onClick = {
                            visibleMonth = (visibleMonth.clone() as Calendar).apply {
                                add(Calendar.MONTH, 1)
                            }
                        }
                    ) {
                        Icon(
                            Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    weekLabels.forEach { label ->
                        Text(
                            text = label,
                            color = cs.onSurfaceVariant,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    days.chunked(7).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            week.forEach { day ->
                                if (day == null) {
                                    Box(modifier = Modifier.weight(1f).height(34.dp))
                                } else {
                                    val dayCalendar = (visibleMonth.clone() as Calendar).apply {
                                        set(Calendar.DAY_OF_MONTH, day)
                                    }
                                    val date = keyFormatter.format(dayCalendar.time)
                                    val isSelected = date == selectedDate
                                    val isToday = date == todayDate
                                    val hasData = date in datesWithData
                                    val shape = RoundedCornerShape(13.dp)
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(34.dp)
                                            .background(
                                                when {
                                                    isSelected -> accent
                                                    hasData -> accent.copy(alpha = 0.13f)
                                                    else -> Color.White.copy(alpha = 0.045f)
                                                },
                                                shape
                                            )
                                            .border(
                                                1.dp,
                                                when {
                                                    isSelected -> accent
                                                    hasData -> accent.copy(alpha = 0.55f)
                                                    isToday -> accent.copy(alpha = 0.58f)
                                                    else -> Color.White.copy(alpha = 0.055f)
                                                },
                                                shape
                                            )
                                            .clickable { onDateSelected(date) },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = day.toString(),
                                            color = if (isSelected) Color.Black else cs.onSurface,
                                            fontSize = 11.sp,
                                            lineHeight = 13.sp,
                                            fontWeight = if (isSelected || isToday) FontWeight.ExtraBold else FontWeight.SemiBold
                                        )
                                        if (hasData && !isSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .align(Alignment.BottomCenter)
                                                    .padding(bottom = 4.dp)
                                                    .size(3.8.dp)
                                                    .background(accent, RoundedCornerShape(999.dp))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = stringResource(R.string.wr_cancel),
                            color = cs.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Button(
                        onClick = { onDateSelected(todayDate) },
                        modifier = Modifier.weight(1f).height(44.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent.copy(alpha = 0.15f),
                            contentColor = accent
                        )
                    ) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.wr_today),
                            fontWeight = FontWeight.ExtraBold,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarNavButton(
    accent: Color,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(34.dp)
            .background(accent.copy(alpha = 0.10f), RoundedCornerShape(13.dp))
            .border(1.dp, accent.copy(alpha = 0.14f), RoundedCornerShape(13.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
