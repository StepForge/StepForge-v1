@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.stepforge.ui.sleep

import android.content.Intent
import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Snooze
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.stepforge.R
import com.example.stepforge.ui.sleep.alarm.SleepReminder
import com.example.stepforge.ui.sleep.alarm.SleepReminderActivity
import com.example.stepforge.ui.sleep.data.SleepRepository
import com.example.stepforge.ui.sleep.model.ManualSleepEntry
import com.example.stepforge.ui.sleep.model.SleepDay
import com.example.stepforge.ui.sleep.model.SleepInsight
import com.example.stepforge.ui.sleep.model.SleepSessionInfo
import com.example.stepforge.ui.sleep.model.SleepSessionType
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    today: SleepDay,
    prevDay: SleepDay?,
    weekHistory: List<SleepDay>,
    onBack: () -> Unit,
    onOpenHistory: () -> Unit,
    onManualSave: (ManualSleepEntry) -> Unit,
    healthConnectState: HealthConnectUiState,
    onHealthConnectClick: () -> Unit
) {
    val cs       = MaterialTheme.colorScheme
    val scroll   = rememberScrollState()
    val context = LocalContext.current
    val baseInsights = remember(today) {
        if (today.hasAnyData) SleepRepository.generateInsights(today) else emptyList()
    }

    val weeklyInsight = remember(weekHistory) {
        SleepRepository.generateWeeklyConsistencyInsight(weekHistory)
    }

    val insights = remember(baseInsights, weeklyInsight) {
        if (weeklyInsight != null)
            listOf(weeklyInsight) + baseInsights
        else
            baseInsights
    }

    var showManualEntry by remember { mutableStateOf(false) }
    val manualSheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { it != SheetValue.Hidden }
    )

    var reminder: SleepReminder? by remember { mutableStateOf(null) }

    val lifecycleOwner = LocalLifecycleOwner.current

    fun reloadReminder() {
        reminder = com.example.stepforge.ui.sleep.alarm.AlarmStore.get(context)
    }

    LaunchedEffect(Unit) {
        reloadReminder()
    }

    DisposableEffect(lifecycleOwner) {

        val observer = LifecycleEventObserver { _, event ->

            if (event == Lifecycle.Event.ON_RESUME) {
                reloadReminder()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                reloadReminder()
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (showManualEntry) {
        ManualEntrySheet(
            sheetState = manualSheetState,
            onDismiss  = { showManualEntry = false },
            onSave     = { entry ->
                onManualSave(entry)
                showManualEntry = false
            }
        )
    }


    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(cs.background)
            .verticalScroll(scroll)
            .padding(horizontal = 16.dp)
            .padding(top = 52.dp, bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        DashHeader(
            today = today,
            reminder = reminder,
            onBack = onBack,
            onManualEntry = { showManualEntry = true },
            onReminderClick = {
                context.startActivity(
                    Intent(
                        context,
                        SleepReminderActivity::class.java
                    )
                )
            }
        )

        HealthConnectStatusCard(
            state = healthConnectState,
            onPrimaryClick = onHealthConnectClick
        )

        ScoreCard(today, onLogClick = { showManualEntry = true })

        if (today.sessions.size > 1) {
            MultiSessionList(today.sessions)
        }

        TimelineCard(today)

        if (today.hasAnyData) {
            StagesSection(today)
            MetricsRow(today, prevDay)
        }

        TrendCard(weekHistory)

        HistoryPreviewCard(weekHistory, prevDay, onOpenHistory)

        if (insights.isNotEmpty()) {
            InsightsSection(insights)
        }

        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun HealthConnectStatusCard(
    state: HealthConnectUiState,
    onPrimaryClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val context = LocalContext.current

    val fullyConnected =
        state.sdkStatus == HealthConnectClient.SDK_AVAILABLE && state.hasAllPermissions

    var tick by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000L)
            tick++
        }
    }

    val subtitle = remember(fullyConnected, state.lastSyncMs, state.sdkStatus, tick) {
        when (state.sdkStatus) {
            HealthConnectClient.SDK_UNAVAILABLE ->
                context.getString(R.string.sleep_hc_not_supported)

            HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
                context.getString(R.string.sleep_hc_update)

            HealthConnectClient.SDK_AVAILABLE -> {
                if (fullyConnected) {
                    val last = state.lastSyncMs
                    if (last != null) {
                        val rel = DateUtils.getRelativeTimeSpanString(
                            last,
                            System.currentTimeMillis(),
                            DateUtils.MINUTE_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_RELATIVE
                        ).toString()
                        context.getString(R.string.sleep_last_sync, rel)
                    } else {
                        context.getString(R.string.sleep_last_sync_never)
                    }
                } else {
                    context.getString(R.string.sleep_permissions_missing)
                }
            }

            else -> context.getString(R.string.sleep_hc_not_supported)
        }
    }

    val buttonLabel = when {
        state.isSyncing -> stringResource(R.string.sleep_syncing)
        fullyConnected -> stringResource(R.string.sleep_sync_now)

        state.sdkStatus == HealthConnectClient.SDK_AVAILABLE ->
            stringResource(R.string.sleep_hc_connect)

        state.sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED ->
            stringResource(R.string.sleep_hc_update)

        else -> stringResource(R.string.sleep_hc_not_supported)
    }

    val buttonEnabled = !state.isSyncing &&
            (
                    state.sdkStatus == HealthConnectClient.SDK_AVAILABLE ||
                            state.sdkStatus == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED
                    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = cs.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(
                        if (fullyConnected) {
                            cs.primary.copy(alpha = 0.12f)
                        } else {
                            cs.error.copy(alpha = 0.12f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (state.isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        strokeWidth = 2.dp,
                        color = cs.primary
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.HealthAndSafety,
                        contentDescription = null,
                        tint = if (fullyConnected) cs.primary else cs.error,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(min = 0.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = stringResource(R.string.sleep_health_connect),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = cs.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = cs.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            TextButton(
                onClick = onPrimaryClick,
                enabled = buttonEnabled,
                modifier = Modifier.widthIn(max = 104.dp),
                contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp)
            ) {
                Text(
                    text = buttonLabel,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    softWrap = false
                )
            }
        }
    }
}



@Composable
private fun MultiSessionList(sessions: List<SleepSessionInfo>) {
    val cs = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = stringResource(R.string.sleep_todays_sessions),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = cs.onSurface
        )
        sessions.forEach { session ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = cs.surface)
            ) {
                Row(
                    Modifier
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        if (session.type == SleepSessionType.MAIN) Icons.Default.Bedtime else Icons.Default.Snooze,
                        contentDescription = null,
                        tint = cs.primary,
                        modifier = Modifier
                            .size(22.dp)
                            .padding(top = 2.dp)
                    )
                    Column(
                        Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (session.type == SleepSessionType.MAIN)
                                stringResource(R.string.sleep_session_main_label)
                            else
                                stringResource(R.string.sleep_session_nap_label),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurface
                        )
                        Text(
                            text = stringResource(R.string.sleep_time_range_en_dash, session.startTime, session.endTime),
                            style = MaterialTheme.typography.bodySmall,
                            color = cs.onSurfaceVariant
                        )
                    }
                    Text(
                        text = stringResource(R.string.sleep_duration_preview, session.totalMinutes / 60, session.totalMinutes % 60),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = cs.primary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DashHeader(
    today: SleepDay,
    reminder: SleepReminder?,
    onBack: () -> Unit,
    onManualEntry: () -> Unit,
    onReminderClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(cs.surfaceVariant.copy(alpha = 0.75f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = onBack
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = stringResource(R.string.sleep_back),
                tint = cs.onSurface,
                modifier = Modifier.size(21.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = stringResource(R.string.sleep_summary_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = cs.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            DataModeChip(today.availability, today.mode)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(cs.primaryContainer)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onManualEntry
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = stringResource(R.string.sleep_log_sleep),
                    tint = cs.onPrimaryContainer,
                    modifier = Modifier.size(18.dp)
                )
            }

            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (reminder != null) cs.primaryContainer else cs.surfaceVariant)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onClick = onReminderClick
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (reminder != null) Icons.Default.Notifications else Icons.Default.NotificationsNone,
                    contentDescription = stringResource(R.string.sleep_reminder_title),
                    tint = if (reminder != null) cs.onPrimaryContainer else cs.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun ScoreCard(day: SleepDay, onLogClick: () -> Unit) {
    val cs    = MaterialTheme.colorScheme
    val score = day.sleepScore
    val col   = if (score != null) scoreColor(score, cs) else cs.primary

    SleepCard {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment     = Alignment.CenterVertically
        ) {
            if (score != null) {
                CircularScore(score = score, size = 116.dp)
            } else {
                Box(
                    Modifier.size(116.dp).clip(CircleShape).background(cs.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bedtime, null, tint = cs.onSurfaceVariant.copy(0.3f), modifier = Modifier.size(40.dp))
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (score != null) {
                    Box(Modifier.clip(RoundedCornerShape(6.dp)).background(col.copy(0.1f)).padding(horizontal = 8.dp, vertical = 3.dp)) {
                        Text(scoreLabel(score), style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold, color = col))
                    }
                } else {
                    Text(stringResource(R.string.sleep_no_logs_today), style = MaterialTheme.typography.labelMedium.copy(color = cs.onSurfaceVariant))
                }

                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.sleep_total_duration), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant)
                    Text(
                        stringResource(R.string.sleep_duration_preview, day.hours, day.minutes),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = if(day.hasAnyData) cs.onSurface else cs.onSurface.copy(0.3f)
                        )
                    )
                }

                if (!day.hasAnyData) {
                    Button(
                        onClick = onLogClick,
                        modifier = Modifier.height(36.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.sleep_log_sleep), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                    }
                } else {
                    HorizontalDivider(color = cs.outlineVariant.copy(0.5f))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        TimeInfo(Icons.Default.NightsStay, stringResource(R.string.sleep_bed_label), day.bedTimeStr, cs.primary)
                        TimeInfo(Icons.Default.WbSunny, stringResource(R.string.sleep_wake_label), day.wakeTimeStr, Color(0xFFFBBF24))
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, time: String, tint: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(12.dp))
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(time, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.ExtraBold))
    }
}

@Composable
private fun TimelineCard(day: SleepDay) {
    val cs = MaterialTheme.colorScheme
    SleepCard {
        Text(stringResource(R.string.sleep_timeline_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (day.hasAnyData) {
            SleepTimeline(day)
        } else {
            Box(Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(8.dp)).background(cs.surfaceVariant.copy(0.5f)), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.sleep_waiting_for_data), style = MaterialTheme.typography.labelSmall, color = cs.onSurfaceVariant.copy(0.6f))
            }
        }
    }
}

@Composable
private fun TrendCard(days: List<SleepDay>) {
    val cs = MaterialTheme.colorScheme
    SleepCard {
        var tab by remember { mutableIntStateOf(0) }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.sleep_weekly_trend), style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Row(Modifier.clip(RoundedCornerShape(8.dp)).background(cs.surfaceVariant)) {
                listOf(
                    stringResource(R.string.sleep_trend_hours),
                    stringResource(R.string.sleep_trend_score)
                ).forEachIndexed { i, lbl ->
                    val sel = tab == i
                    Box(Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) cs.primary else Color.Transparent).clickable { tab = i }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                        Text(lbl, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, color = if (sel) cs.onPrimary else cs.onSurfaceVariant))
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        if (tab == 0) WeeklyBarChart(days) else ScoreLineChart(days)
    }
}

@Composable
private fun HistoryPreviewCard(days: List<SleepDay>, prevDay: SleepDay?, onOpen: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val historyWithData = days.filter { it.hasAnyData }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.sleep_recent_history),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            )
            TextButton(onClick = onOpen) {
                Text(stringResource(R.string.sleep_see_all), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(14.dp))
            }
        }
        if (historyWithData.isEmpty()) {
            SleepCard { Text(stringResource(R.string.sleep_no_recent_logs), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant) }
        } else {
            historyWithData.takeLast(7).reversed().forEach { day ->
                PremiumHistoryRow(day = day, onClick = onOpen)
            }
        }
    }
}

@Composable
private fun InsightsSection(insights: List<SleepInsight>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.sleep_daily_insights), style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        insights.forEach { InsightCard(it) }
    }
}



