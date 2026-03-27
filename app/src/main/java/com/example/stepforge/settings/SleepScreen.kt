package com.example.stepforge.settings

import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.CloudSync
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.stepforge.StepCounterService
import com.example.stepforge.data.AppDatabase
import com.example.stepforge.data.SleepSession
import com.example.stepforge.data.stepforgeStore
import com.example.stepforge.ui.components.SleepSyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/* ===========================================================
 * SLEEP – ANA EKRAN (Auto Sync + Manual date auto-calc fix)
 * ===========================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepScreen(
    onBack: () -> Unit,
    onLaunchHealthConnectPermissions: () -> Unit
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bg = cs.background
    val card = if (isDark) cs.surface else Color.White
    val panel = if (isDark) Color(0xFF101218) else cs.surfaceVariant
    val statPanel = if (isDark) Color(0xFF0F1116) else cs.surfaceVariant

    val textMain = cs.onSurface
    val textSub = cs.onSurface.copy(alpha = 0.7f)

    val neonA = if (isDark) Color(0xFF00FFA3) else Color(0xFF4FD1C5)
    val neonB = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)
    val heroQuick = Brush.horizontalGradient(listOf(neonA, neonB))

    val purple = if (isDark) Color(0xFFB388FF) else Color(0xFF7C3AED)

    val db = remember { AppDatabase.getDatabase(ctx) }
    val dao = remember { db.sleepSessionDao() }
    val stageDao = remember { db.sleepStageDao() }
    val hc = remember { SleepSyncManager(ctx) }

    // ✅ Auto sync DataStore keys
    val KEY_SLEEP_LAST_SYNC_OK = longPreferencesKey("sleep_last_sync_ok_ms")
    val KEY_SLEEP_LAST_SYNC_ATTEMPT = longPreferencesKey("sleep_last_sync_attempt_ms")
    val KEY_SLEEP_SKIP_DELETE_CONFIRM = booleanPreferencesKey("sleep_skip_delete_confirm")


    var sessions by remember { mutableStateOf<List<SleepSession>>(emptyList()) }
    var hcConnected by remember { mutableStateOf(false) }
    var syncing by remember { mutableStateOf(false) }
    var skipDeleteConfirm by remember { mutableStateOf(false) }


    // ✅ Debug/status info to show that auto-sync works
    var autoSyncStatus by remember { mutableStateOf<String?>(null) }
    var lastSyncOkMs by remember { mutableStateOf(0L) }

    var pendingDelete by remember { mutableStateOf<SleepSession?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var dontAskAgain by remember { mutableStateOf(false) }


    val sdfYmd = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
    val sdfLastSync = remember { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }

    fun todayDate(): String =
        sdfYmd.format(Date())

    fun dateDaysAgo(days: Int): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -days) }
        return sdfYmd.format(cal.time)
    }

    fun ymdFromMillis(ms: Long): String = sdfYmd.format(Date(ms))

    suspend fun reload() {
        sessions = withContext(Dispatchers.IO) { dao.getRecentSessions(120) }
        hcConnected = withContext(Dispatchers.IO) { !hc.hasMissingPermissions() }
    }

    var lastSyncAttemptMs by remember { mutableStateOf(0L) }

    suspend fun readAutoSyncPrefs() {
        val prefs = ctx.stepforgeStore.data.first()
        lastSyncOkMs = prefs[KEY_SLEEP_LAST_SYNC_OK] ?: 0L
        lastSyncAttemptMs = prefs[KEY_SLEEP_LAST_SYNC_ATTEMPT] ?: 0L
        skipDeleteConfirm = prefs[KEY_SLEEP_SKIP_DELETE_CONFIRM] ?: false
    }


    /**
     * ✅ Auto sync: ekrana girince / resume olunca çalışır.
     * - force = true ise 10 dakikalık bekleme süresini deler (Buton için).
     */
    suspend fun maybeAutoSync(trigger: String, force: Boolean = false) {
        if (syncing && !force) return

        val now = System.currentTimeMillis()
        val prefs = ctx.stepforgeStore.data.first()
        val lastOk = prefs[KEY_SLEEP_LAST_SYNC_OK] ?: 0L
        val lastAttempt = prefs[KEY_SLEEP_LAST_SYNC_ATTEMPT] ?: 0L

        lastSyncOkMs = lastOk

        // Debounce: Çok sık (1 dk içinde) denemesin (force değilse)
        val minAttemptGapMs = 60_000L
        if (!force && lastAttempt > 0L && (now - lastAttempt) < minAttemptGapMs) {
            return
        }

        // Eğer yakın zamanda başarılı sync olduysa tekrar denemesin (force değilse)
        val minOkGapMs = 10 * 60_000L
        if (!force && lastOk > 0L && (now - lastOk) < minOkGapMs) {
            Log.d("SleepAutoSync", "Sync skipped: recently updated.")
            return
        }

        // ✅ İZİN KONTROLÜ (S10 ve Android 14+ uyumlu):
        val missingPermissions = withContext(Dispatchers.IO) {
            val granted = hc.getGrantedPermissions() // Manager'a eklediğimiz yeni fonksiyon
            val required = hc.getPermissionStrings()
            required.filter { it !in granted }
        }

        // Eğer kritik OKUMA izni yoksa dur, varsa devam et
        if (missingPermissions.any { it.contains("READ_SLEEP") }) {
            hcConnected = false
            autoSyncStatus = "Permissions missing."
            Log.w("SleepAutoSync", "Missing permissions: $missingPermissions")
            return
        }

        // Buraya geldiyse izinler tam demektir
        hcConnected = true
        syncing = true
        autoSyncStatus = "Successfully Connected ✅"
        Log.d("SleepAutoSync", "start($trigger)")

        ctx.stepforgeStore.edit { p -> p[KEY_SLEEP_LAST_SYNC_ATTEMPT] = now }

        try {
            val ok = withContext(Dispatchers.IO) { hc.syncLast7Days() }
            reload()

            if (ok) {
                val okNow = System.currentTimeMillis()
                ctx.stepforgeStore.edit { p -> p[KEY_SLEEP_LAST_SYNC_OK] = okNow }
                lastSyncOkMs = okNow
                autoSyncStatus = "Sync successful ✅"
            } else {
                autoSyncStatus = "No new data found."
            }
        } catch (e: Exception) {
            autoSyncStatus = "Error: ${e.message}"
            Log.e("SleepAutoSync", "error", e)
        } finally {
            syncing = false
        }
    }




    LaunchedEffect(Unit) {
        reload()
        readAutoSyncPrefs()
        // ✅ ekrana girince otomatik sync dene
        maybeAutoSync(trigger = "open")
    }

    // ✅ Ekrana geri dönünce de otomatik sync dene (butona basmadan)
    DisposableEffect(Unit) {
        val activity = ctx as? ComponentActivity
        if (activity == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    scope.launch {
                        readAutoSyncPrefs()
                        maybeAutoSync(trigger = "resume")
                    }
                }
            }
            activity.lifecycle.addObserver(observer)
            onDispose {
                activity.lifecycle.removeObserver(observer)
            }
        }
    }

    val last30Dates = remember { (0..29).map { dateDaysAgo(it) }.reversed() }
    val sessionsByDate = remember(sessions) { sessions.groupBy { it.date } }

    var selectedDate by remember { mutableStateOf(todayDate()) }
    val selectedDaySessions = remember(sessionsByDate, selectedDate) {
        sessionsByDate[selectedDate].orEmpty().sortedByDescending { it.startTime }
    }
    val selectedSession = selectedDaySessions.firstOrNull()

    val scroll = rememberScrollState()
    LaunchedEffect(Unit) { scroll.scrollTo(scroll.maxValue) }

    val last7WithData = remember(sessions) {
        sessions.sortedByDescending { it.startTime }.take(7)
    }
    val avgMin = remember(last7WithData) {
        if (last7WithData.isEmpty()) 0
        else last7WithData.sumOf { it.totalMinutes } / last7WithData.size
    }
    val consistency = remember(last7WithData) {
        if (last7WithData.size < 2) 0 else {
            val mean = last7WithData.map { it.totalMinutes }.average()
            val dev = last7WithData.map { abs(it.totalMinutes - mean) }.average()
            (100 - (dev / 120.0 * 100)).roundToInt().coerceIn(0, 100)
        }
    }

    // Quick Log state
    var showQuickLog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    var editDate by remember { mutableStateOf(todayDate()) }

    var startHour by remember { mutableStateOf(23) }
    var startMinute by remember { mutableStateOf(0) }
    var endHour by remember { mutableStateOf(7) }
    var endMinute by remember { mutableStateOf(0) }
    var quality by remember { mutableStateOf(80) }

    // SleepScreen.kt içindeki fonksiyonun tam ve zeki hali:
    fun setEditorDefaultsForDate(date: String) {
        scope.launch {

            // 1️⃣ Eğer DB'de kayıt varsa → HER ŞEYİ BIRAK, ONU GÖSTER
            val sessionsForDate = sessionsByDate[date].orEmpty()
            val s = sessionsForDate.maxByOrNull { it.startTime }
            if (s != null) {
                val calS = Calendar.getInstance().apply { timeInMillis = s.startTime }
                val calE = Calendar.getInstance().apply { timeInMillis = s.endTime }

                startHour = calS.get(Calendar.HOUR_OF_DAY)
                startMinute = calS.get(Calendar.MINUTE)

                endHour = calE.get(Calendar.HOUR_OF_DAY)
                endMinute = calE.get(Calendar.MINUTE)

                quality = s.qualityScore.coerceIn(0, 100)
                editDate = date
                return@launch
            }

            // 2️⃣ DB yoksa → PROBABLE_SLEEP var mı bak
            val prefs = ctx.stepforgeStore.data.first()

            val ready = prefs[StepCounterService.PROBABLE_SLEEP_READY] ?: false
            val startMs = prefs[StepCounterService.PROBABLE_SLEEP_START] ?: 0L
            val endMs = prefs[StepCounterService.PROBABLE_SLEEP_END] ?: 0L

            if (ready && startMs > 0L && endMs > startMs) {

                val calStart = Calendar.getInstance().apply { timeInMillis = startMs }
                val calEnd = Calendar.getInstance().apply { timeInMillis = endMs }

                startHour = calStart.get(Calendar.HOUR_OF_DAY)
                startMinute = calStart.get(Calendar.MINUTE)

                endHour = calEnd.get(Calendar.HOUR_OF_DAY)
                endMinute = calEnd.get(Calendar.MINUTE)

                quality = 80
                editDate = date

                return@launch
            }

            // 3️⃣ Hiçbir şey yoksa → SADECE MANUEL DEFAULT
            startHour = 23
            startMinute = 0
            endHour = 7
            endMinute = 0
            quality = 60
            editDate = date
        }
    }







    // SleepScreen.kt içinde bu fonksiyonu şu şekilde güncelleyin:

    fun computeMillis(date: String, sh: Int, sm: Int, eh: Int, em: Int): Pair<Long, Long> {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val base = sdf.parse(date) ?: Date()

        val calStart = Calendar.getInstance().apply {
            time = base
            set(Calendar.HOUR_OF_DAY, sh)
            set(Calendar.MINUTE, sm)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calEnd = Calendar.getInstance().apply {
            time = base
            set(Calendar.HOUR_OF_DAY, eh)
            set(Calendar.MINUTE, em)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // ✅ Eğer end, start'tan küçükse end ertesi güne kaymalı
        if (calEnd.timeInMillis <= calStart.timeInMillis) {
            calEnd.add(Calendar.DAY_OF_YEAR, 1)
        }

        return calStart.timeInMillis to calEnd.timeInMillis
    }



    val (previewStartMs, previewEndMs) =
        remember(editDate, startHour, startMinute, endHour, endMinute) {
            computeMillis(editDate, startHour, startMinute, endHour, endMinute)
        }
    ((previewEndMs - previewStartMs) / 60_000L).toInt().coerceAtLeast(0)

    val sessionMap = remember(sessionsByDate) { sessionsByDate }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "Sleep",
                            color = cs.onBackground,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (hcConnected) "Connected to Health Connect" else "Manual tracking",
                            color = cs.onBackground.copy(alpha = 0.65f),
                            fontSize = 12.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
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
                .padding(horizontal = 18.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // HERO – QUICK LOG
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(if (isDark) 14.dp else 6.dp, RoundedCornerShape(26.dp)),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) card else Color.White
                ),
                shape = RoundedCornerShape(26.dp)
            ) {
                Column(
                    Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Bedtime,
                            contentDescription = null,
                            tint = if (isDark) neonB else purple
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Sleep log",
                            color = textMain,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (hcConnected)
                                        (if (isDark) neonA.copy(alpha = 0.15f) else Color(0xFFD1FAE5))
                                    else
                                        (if (isDark) Color(0xFF3C2A15) else Color(0xFFFFF7E6)),
                                    RoundedCornerShape(999.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (hcConnected) "Health Connect" else "Manual",
                                color = if (hcConnected) neonA else Color(0xFFFFA726),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Text(
                        text = "Pick a day, then log or edit your sleep.",
                        color = textSub,
                        fontSize = 12.sp
                    )

                    Button(
                        onClick = {// ✅ Her zaman o anki gerçek tarihi al
                            val realToday = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                            setEditorDefaultsForDate(realToday)
                            showQuickLog = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues()
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(heroQuick, RoundedCornerShape(999.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = null,
                                    tint = if (isDark) Color.Black else Color.White
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Quick log",
                                    color = if (isDark) Color.Black else Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // HEALTH CONNECT
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = panel),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.CloudSync,
                            contentDescription = null,
                            tint = if (hcConnected) (if (isDark) neonB else Color(0xFF0EA5E9))
                            else (if (isDark) Color(0xFF90A4AE) else Color(0xFF64748B))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Health Connect",
                            color = textMain,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = if (hcConnected) "Connected" else "Not connected",
                            color = if (hcConnected) neonA else textSub,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // ✅ “Çalıştığını gösteren” satır (Auto Sync)
                    val lastOkLabel = if (lastSyncOkMs > 0L) {
                        "Last sync: ${sdfLastSync.format(Date(lastSyncOkMs))}"
                    } else {
                        "Last sync: Never"
                    }
                    Text(
                        text = lastOkLabel,
                        color = textSub,
                        fontSize = 11.sp
                    )
                    if (autoSyncStatus != null) {
                        Text(
                            text = autoSyncStatus!!,
                            color = cs.onSurface.copy(alpha = 0.85f),
                            fontSize = 11.sp
                        )
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                syncing = true
                                try {
                                    val missing = withContext(Dispatchers.IO) { hc.hasMissingPermissions() }
                                    if (missing) {
                                        onLaunchHealthConnectPermissions()
                                        Toast.makeText(
                                            ctx,
                                            "Grant Sleep permission, then sync.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        autoSyncStatus = "Manual sync: missing permission"
                                        return@launch
                                    }

                                    // ✅ Manual sync = force
                                    maybeAutoSync(trigger = "button", force = true)
                                } finally {
                                    syncing = false
                                }
                            }
                        },
                        enabled = !syncing,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isDark) Color(0xFF151821) else Color.White
                        )
                    ) {
                        Text(
                            if (syncing) "Syncing..." else "Sync last 7 days",
                            color = textMain,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // AVG / CONS
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MiniStatCard(
                    title = "Avg",
                    value = formatDuration(avgMin),
                    description = "Average sleep duration over your last 7 logs.",
                    bg = statPanel,
                    textMain = textMain,
                    textSub = textSub,
                    accent = if (isDark) neonB else Color(0xFF22C1C3),
                    modifier = Modifier.weight(1f)
                )
                MiniStatCard(
                    title = "Cons.",
                    value = "$consistency/100",
                    description = "Consistency of your sleep duration (higher is steadier).",
                    bg = statPanel,
                    textMain = textMain,
                    textSub = textSub,
                    accent = if (isDark) neonA else Color(0xFF22C55E),
                    modifier = Modifier.weight(1f)
                )
            }

            // BROWSE BY DAY
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = panel),
                shape = RoundedCornerShape(22.dp)
            ) {
                Column(
                    Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        "Browse by day",
                        color = textMain,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(scroll),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        var lastMonth: String? = null
                        last30Dates.forEach { date ->
                            val m = monthLabel(date)
                            if (m != lastMonth) {
                                MonthPill(text = m, isDark = isDark)
                                lastMonth = m
                            }

                            val s = sessionsByDate[date]
                            DayTile(
                                day = shortDay(date),
                                week = shortWeekday(date),
                                hasData = !s.isNullOrEmpty(),
                                minutes = s?.sumOf { it.totalMinutes } ?: 0,
                                selected = date == selectedDate,
                                isDark = isDark,
                                accent = if (s?.any { it.source == "health_connect" } == true) purple else neonB,
                                onClick = { selectedDate = date }
                            )
                        }
                    }

                    if (selectedSession != null) {
                        SleepDetailCardForList(
                            session = selectedSession!!,
                            bg = card,
                            textMain = textMain,
                            textSub = textSub,
                            neon = Brush.horizontalGradient(listOf(neonA, neonB)),
                            accent = if (selectedSession!!.source == "health_connect") purple else neonB,
                            onDelete = { s ->
                                if (skipDeleteConfirm) {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            dao.deleteById(s.id)
                                            stageDao.deleteBySessionId(s.id)
                                        }
                                        reload()
                                        selectedDate = s.date

                                        Toast.makeText(
                                            ctx,
                                            "Sleep deleted",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                } else {
                                    pendingDelete = s
                                    dontAskAgain = false
                                    showDeleteDialog = true
                                }
                            }
                        )

                        if (selectedDaySessions.size > 1) {
                            SleepSessionsListCard(
                                date = selectedDate,
                                sessions = selectedDaySessions,
                                bg = card,
                                textMain = textMain,
                                textSub = textSub,
                                neon = Brush.horizontalGradient(listOf(neonA, neonB)),
                                accent = if (selectedSession!!.source == "health_connect") purple else neonB,
                                onDelete = { s ->
                                    if (skipDeleteConfirm) {
                                        scope.launch {
                                            withContext(Dispatchers.IO) {
                                                dao.deleteById(s.id)
                                                stageDao.deleteBySessionId(s.id)
                                            }
                                            reload()
                                            selectedDate = s.date
                                            Toast.makeText(
                                                ctx,
                                                "Sleep deleted",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    } else {
                                        pendingDelete = s
                                        dontAskAgain = false
                                        showDeleteDialog = true
                                    }
                                }
                            )
                        }

                    } else {
                        Text(
                            text = "No entry for $selectedDate. Tap Quick log to add it.",
                            color = textSub,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }

    // --- Quick Log Dialog + Pickers ---

    if (showDatePicker) {
        SleepDatePickerDialog(
            initialDate = editDate,
            sessionMap = sessionMap,
            onDismiss = { showDatePicker = false },
            onSelect = { d ->
                editDate = d
                showDatePicker = false
                setEditorDefaultsForDate(d)
            }
        )
    }

    if (showStartPicker) {
        CanvasClockTimePickerDialog(
            title = "Start time",
            initialHour = startHour,
            initialMinute = startMinute,
            accent = if (isDark) neonB else Color(0xFF22C1C3),
            onDismiss = { showStartPicker = false },
            onConfirm = { h, m ->
                startHour = h
                startMinute = m
                showStartPicker = false
            }
        )
    }

    if (showEndPicker) {
        CanvasClockTimePickerDialog(
            title = "End time",
            initialHour = endHour,
            initialMinute = endMinute,
            accent = if (isDark) neonA else Color(0xFF22C55E),
            onDismiss = { showEndPicker = false },
            onConfirm = { h, m ->
                endHour = h
                endMinute = m
                showEndPicker = false
            }
        )
    }

    if (showQuickLog) {
        val dialogBg = if (isDark) Color(0xFF111318) else cs.surface
        val innerCard = if (isDark) Color(0xFF0F1116) else cs.surfaceVariant

        // 1. Canlı süre hesaplama (Asleep/Awake arası fark)
        val (previewStartMs, previewEndMs) = remember(editDate, startHour, startMinute, endHour, endMinute) {
            computeMillis(editDate, startHour, startMinute, endHour, endMinute)
        }
        val diffMs = previewEndMs - previewStartMs
        val hours = diffMs / (1000 * 60 * 60)
        val minutes = (diffMs / (1000 * 60)) % 60

        AlertDialog(
            onDismissRequest = { showQuickLog = false },
            title = {
                Text("Sleep Log", color = cs.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp)
            },
            containerColor = dialogBg,
            shape = RoundedCornerShape(28.dp),
            confirmButton = {
                // ✅ KAYDETME İŞLEMİ
                TextButton(
                    onClick = {
                        scope.launch {
                            val totalMinutes = (diffMs / 60_000L).toInt().coerceAtLeast(0)
                            val saveDate = editDate

                            withContext(Dispatchers.IO) {
                                dao.insert(
                                    SleepSession(
                                        date = saveDate,
                                        startTime = previewStartMs,
                                        endTime = previewEndMs,
                                        totalMinutes = totalMinutes,
                                        qualityScore = quality.coerceIn(0, 100),
                                        source = "manual"
                                    )
                                )
                            }

                            reload()
                            selectedDate = saveDate
                            Toast.makeText(ctx, "Sleep saved.", Toast.LENGTH_SHORT).show()
                            showQuickLog = false

                        }
                    }
                ) {
                    Text("Record", color = neonB, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQuickLog = false }) {
                    Text("Cancel", color = cs.onSurface.copy(alpha = 0.6f))
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // SÜRE GÖSTERİMİ (Büyük ve profesyonel)
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "${hours}h ${minutes}m",
                            color = if(hours in 7..9) neonA else Color(0xFFFFB74D),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = "Estimated from activity",
                            color = cs.onSurface.copy(alpha = 0.5f),
                            fontSize = 11.sp
                        )
                    }

                    PickerButton(
                        icon = Icons.Outlined.Schedule,
                        title = "Date",
                        value = prettyDate(editDate),
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        background = innerCard,
                        isDark = isDark
                    )


                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        // ASLEEP (Yatış)
                        PickerButton(
                            icon = Icons.Outlined.Bedtime,
                            title = "Asleep",
                            value = "%02d:%02d".format(startHour, startMinute),
                            onClick = { showStartPicker = true },
                            modifier = Modifier.weight(1f),
                            background = innerCard,
                            isDark = isDark
                        )
                        // AWAKE (Uyanış)
                        PickerButton(
                            icon = Icons.Outlined.Schedule,
                            title = "Awake",
                            value = "%02d:%02d".format(endHour, endMinute),
                            onClick = { showEndPicker = true },
                            modifier = Modifier.weight(1f),
                            background = innerCard,
                            isDark = isDark
                        )
                    }

                    // Tarih Bilgilendirmesi
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(innerCard, RoundedCornerShape(14.dp))
                            .padding(8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Logging for ${prettyDate(editDate)}",
                            color = cs.onSurface.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Kalite Kaydı
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Quality", color = cs.onSurface, fontSize = 14.sp)
                            Spacer(Modifier.weight(1f))
                            Text("$quality%", color = neonB, fontWeight = FontWeight.Bold)
                        }
                        Slider(
                            value = quality.toFloat(),
                            onValueChange = { quality = it.roundToInt() },
                            valueRange = 0f..100f,
                            colors = SliderDefaults.colors(thumbColor = neonB, activeTrackColor = neonB)
                        )
                    }
                }
            }
        )
    }


    if (showDeleteDialog && pendingDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    "Delete sleep record",
                    fontWeight = FontWeight.SemiBold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This sleep record will be permanently removed.",
                        fontSize = 13.sp
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(8.dp)
                    ) {
                        Checkbox(
                            checked = dontAskAgain,
                            onCheckedChange = { dontAskAgain = it }
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Don't ask again",
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val s = pendingDelete!!
                        scope.launch {
                            if (dontAskAgain) {
                                ctx.stepforgeStore.edit {
                                    it[KEY_SLEEP_SKIP_DELETE_CONFIRM] = true
                                }
                                skipDeleteConfirm = true
                            }

                            withContext(Dispatchers.IO) {
                                dao.deleteById(s.id)
                                stageDao.deleteBySessionId(s.id)
                            }

                            reload()
                            selectedDate = s.date

                            Toast.makeText(
                                ctx,
                                "Sleep deleted",
                                Toast.LENGTH_SHORT
                            ).show()

                            pendingDelete = null
                            showDeleteDialog = false
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFE53935), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    pendingDelete = null
                    showDeleteDialog = false
                }) {
                    Text("Cancel")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }
}


@Composable
private fun SleepSessionsListCard(
    date: String,
    sessions: List<SleepSession>,
    bg: Color,
    textMain: Color,
    textSub: Color,
    neon: Brush,
    accent: Color,
    onDelete: (SleepSession) -> Unit
) {
    val totalMinutes = sessions.sumOf { it.totalMinutes }
    val latest = sessions.maxByOrNull { it.startTime }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.CloudSync, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Sessions on $date",
                        color = textMain,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${sessions.size} entries • ${formatDuration(totalMinutes)} total",
                        color = textSub,
                        fontSize = 11.sp
                    )
                }
                if (latest != null) {
                    Box(
                        modifier = Modifier
                            .background(accent.copy(alpha = 0.14f), RoundedCornerShape(999.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = when (latest.source) {
                                "health_connect" -> "HC"
                                "auto" -> "AUTO"
                                else -> "Manual"
                            },
                            color = accent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            sessions.forEachIndexed { index, s ->
                val rowAccent = when (s.source) {
                    "health_connect" -> accent
                    "auto" -> accent.copy(alpha = 0.92f)
                    else -> accent.copy(alpha = 0.85f)
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                            RoundedCornerShape(16.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .width(4.dp)
                            .height(42.dp)
                            .background(rowAccent, RoundedCornerShape(999.dp))
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "${formatClock(s.startTime)} - ${formatClock(s.endTime)}",
                            color = textMain,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "${formatDuration(s.totalMinutes)} • ${s.qualityScore}/100 quality",
                            color = textSub,
                            fontSize = 11.sp
                        )
                    }
                    IconButton(onClick = { onDelete(s) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "Delete sleep",
                            tint = Color(0xFFE53935),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (index != sessions.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}



/* ===========================================================
 * DATE PICKER – SLEEP
 * ===========================================================
 */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SleepDatePickerDialog(
    initialDate: String,
    sessionMap: Map<String, List<SleepSession>>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bg = if (isDark) Color(0xFF111318) else cs.surface
    val panel = if (isDark) Color(0xFF0F1116) else cs.surfaceVariant
    val neonA = if (isDark) Color(0xFF00FFA3) else Color(0xFF4FD1C5)
    val neonB = if (isDark) Color(0xFF00F5FF) else Color(0xFF2CB6AE)

    fun parseYmd(date: String): Calendar? {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val d = sdf.parse(date) ?: return null
            Calendar.getInstance().apply {
                time = d
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun formatYmd(cal: Calendar): String =
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

    val initCal = remember(initialDate) { parseYmd(initialDate) ?: Calendar.getInstance() }
    var shownMonth by remember {
        mutableStateOf(
            Calendar.getInstance().apply {
                timeInMillis = initCal.timeInMillis
                set(Calendar.DAY_OF_MONTH, 1)
            }
        )
    }

    fun monthTitle(cal: Calendar): String =
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(cal.time)

    fun daysGridForMonth(cal: Calendar): List<Calendar?> {
        val temp = Calendar.getInstance().apply {
            timeInMillis = cal.timeInMillis
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val firstDow = temp.get(Calendar.DAY_OF_WEEK)
        val offset = if (firstDow == Calendar.SUNDAY) 6 else firstDow - 2

        val daysInMonth = temp.getActualMaximum(Calendar.DAY_OF_MONTH)

        val out = mutableListOf<Calendar?>()
        repeat(offset) { out.add(null) }
        for (d in 1..daysInMonth) {
            val c = Calendar.getInstance().apply {
                timeInMillis = temp.timeInMillis
                set(Calendar.DAY_OF_MONTH, d)
            }
            out.add(c)
        }
        while (out.size % 7 != 0) out.add(null)
        return out
    }

    val grid = remember(shownMonth.timeInMillis) { daysGridForMonth(shownMonth) }

    val density = LocalDensity.current
    val swipeThresholdPx = with(density) { 50.dp.toPx() }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row {
                TextButton(onClick = {
                    val t = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                    onSelect(t)
                }) {
                    Text("Today", color = if (isDark) neonB else Color(0xFF22C1C3))
                }
                TextButton(onClick = onDismiss) {
                    Text("Close", color = cs.onSurface.copy(alpha = 0.85f))
                }
            }
        },
        title = {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    shownMonth = Calendar.getInstance().apply {
                        timeInMillis = shownMonth.timeInMillis
                        add(Calendar.MONTH, -1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                }) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "Prev", tint = cs.onSurface)
                }

                Text(
                    text = monthTitle(shownMonth),
                    color = cs.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                    fontSize = 16.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Visible,
                    textAlign = TextAlign.Center
                )

                IconButton(onClick = {
                    shownMonth = Calendar.getInstance().apply {
                        timeInMillis = shownMonth.timeInMillis
                        add(Calendar.MONTH, 1)
                        set(Calendar.DAY_OF_MONTH, 1)
                    }
                }) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "Next", tint = cs.onSurface)
                }
            }
        },
        containerColor = bg,
        shape = RoundedCornerShape(20.dp),
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("M", "T", "W", "T", "F", "S", "S").forEach { dayLabel ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = dayLabel,
                                color = cs.onSurface.copy(alpha = 0.55f),
                                fontSize = 12.sp,
                                maxLines = 1,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.pointerInput(shownMonth.timeInMillis) {
                        var downX = 0f
                        var tracking = false
                        awaitPointerEventScope {
                            while (true) {
                                val ev = awaitPointerEvent()
                                val ch = ev.changes.firstOrNull() ?: continue

                                if (ch.pressed && !tracking) {
                                    downX = ch.position.x
                                    tracking = true
                                }

                                if (!ch.pressed && tracking) {
                                    val dx = ch.position.x - downX
                                    if (dx > swipeThresholdPx) {
                                        shownMonth = Calendar.getInstance().apply {
                                            timeInMillis = shownMonth.timeInMillis
                                            add(Calendar.MONTH, -1)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }
                                    } else if (dx < -swipeThresholdPx) {
                                        shownMonth = Calendar.getInstance().apply {
                                            timeInMillis = shownMonth.timeInMillis
                                            add(Calendar.MONTH, 1)
                                            set(Calendar.DAY_OF_MONTH, 1)
                                        }
                                    }
                                    tracking = false
                                }
                            }
                        }
                    }
                ) {
                    grid.chunked(7).forEach { week ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            val filledWeek = if (week.size < 7) week + List(7 - week.size) { null } else week

                            filledWeek.forEach { dayCal ->
                                if (dayCal == null) {
                                    Box(Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    val ymd = formatYmd(dayCal)
                                    val s = sessionMap[ymd]
                                    val hasData = !s.isNullOrEmpty()
                                    val selected = (ymd == initialDate)

                                    val shape = RoundedCornerShape(14.dp)
                                    val baseBg = when {
                                        selected -> if (isDark) Color(0xFF171A26) else cs.surfaceVariant
                                        else -> panel
                                    }

                                    val borderBrush = when {
                                        selected -> Brush.linearGradient(listOf(neonB, neonA, neonB))
                                        hasData -> Brush.linearGradient(listOf(neonA, neonB))
                                        else -> null
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .aspectRatio(1f)
                                            .shadow(if (selected || hasData) 10.dp else 0.dp, shape, clip = false)
                                            .background(baseBg, shape)
                                            .then(
                                                if (borderBrush != null) Modifier.border(2.dp, borderBrush, shape)
                                                else Modifier
                                            )
                                            .clickable { onSelect(ymd) }
                                            .padding(2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = dayCal.get(Calendar.DAY_OF_MONTH).toString(),
                                            color = if (selected) neonB else cs.onSurface,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            fontSize = 13.sp,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Text(
                    text = "Border = saved sleep • Swipe horizontally to change month",
                    color = cs.onSurface.copy(alpha = 0.6f),
                    fontSize = 11.sp
                )
            }
        }
    )
}

/* ===========================================================
 * HELPER COMPOSABLES – PickerButton, DayTile, vb.
 * ===========================================================
 */

@Composable
private fun PickerButton(
    icon: ImageVector,
    title: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    background: Color = Color(0xFF0F1116),
    textMain: Color = Color.White,
    textSub: Color = Color.White.copy(alpha = 0.65f),
    isDark: Boolean = true
) {
    Row(
        modifier = modifier
            .background(background, RoundedCornerShape(14.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (isDark) Color(0xFF00F5FF) else Color(0xFF22C1C3)
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = textSub, fontSize = 11.sp)
            Text(
                value,
                color = textMain,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = textMain.copy(alpha = 0.5f)
        )
    }
}

@Composable
private fun MiniStatCard(
    title: String,
    value: String,
    description: String,
    bg: Color,
    textMain: Color,
    textSub: Color,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    title,
                    color = textSub,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    tint = textSub.copy(alpha = 0.7f),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                description,
                color = textSub,
                fontSize = 11.sp,
                lineHeight = 14.sp
            )
        }
    }
}

@Composable
private fun MonthPill(text: String, isDark: Boolean) {
    Box(
        modifier = Modifier
            .background(
                color = if (isDark) Color(0xFF1A1C23) else Color(0xFFE5E9F2),
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text,
            color = if (isDark) Color.White.copy(alpha = 0.75f) else Color(0xFF1F2933),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun DayTile(
    day: String,
    week: String,
    hasData: Boolean,
    minutes: Int,
    selected: Boolean,
    isDark: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val bg = when {
        selected && isDark -> Color(0xFF151821)
        selected && !isDark -> Color(0xFFE0F2FE)
        !selected && isDark -> Color(0xFF111318)
        else -> Color(0xFFF8FAFC)
    }
    val border = when {
        selected -> accent
        isDark -> Color(0xFF232631)
        else -> Color(0xFFE2E8F0)
    }
    val labelColor = if (isDark) Color.White else Color(0xFF0F172A)
    val sub = if (hasData) formatDuration(minutes) else "—"
    val subColor = if (hasData) accent else if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF94A3B8)

    Column(
        modifier = Modifier
            .width(90.dp)
            .shadow(if (selected) 10.dp else 0.dp, RoundedCornerShape(18.dp))
            .background(bg, RoundedCornerShape(18.dp))
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            week,
            color = if (isDark) Color.White.copy(alpha = 0.65f) else Color(0xFF6B7280),
            fontSize = 10.sp
        )
        Text(day, color = labelColor, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            sub,
            color = subColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(2.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(border, RoundedCornerShape(999.dp))
        )
    }
}

@Composable
private fun SleepDetailCardForList(
    session: SleepSession,
    bg: Color,
    textMain: Color,
    textSub: Color,
    neon: Brush,
    accent: Color,
    onDelete: (SleepSession) -> Unit
)
{
    val q = session.qualityScore.coerceIn(0, 100)
    val start = formatClock(session.startTime)
    val end = formatClock(session.endTime)
    val duration = formatDuration(session.totalMinutes)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(22.dp)),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Bedtime, contentDescription = null, tint = accent)
                Spacer(Modifier.width(8.dp))
                Text("Selected night", color = textMain, fontWeight = FontWeight.SemiBold)

                Spacer(Modifier.width(8.dp))
                IconButton(
                    onClick = { onDelete(session) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete sleep",
                        tint = Color(0xFFE53935),
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .background(accent.copy(alpha = 0.18f), RoundedCornerShape(999.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = when (session.source) {
                            "health_connect" -> "HC"
                            "auto" -> "AUTO"
                            else -> "Manual"
                        },
                        color = accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }


            Text(duration, color = textMain, fontSize = 26.sp, fontWeight = FontWeight.Bold)

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                InfoChip(label = "Start", value = start, accent = accent, modifier = Modifier.weight(1f))
                InfoChip(label = "End", value = end, accent = accent, modifier = Modifier.weight(1f))
                InfoChip(label = "Quality", value = "$q", accent = Color(0xFF00F5FF), modifier = Modifier.weight(1f))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .background(
                        if (MaterialTheme.colorScheme.background.luminance() < 0.5f)
                            Color(0xFF202430)
                        else
                            Color(0xFFE2E8F0),
                        RoundedCornerShape(999.dp)
                    ),
                contentAlignment = Alignment.CenterStart
            ) {
                val progress = (session.totalMinutes / (8f * 60f)).coerceIn(0f, 1f)
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(10.dp)
                        .background(neon, RoundedCornerShape(999.dp))
                )
            }
        }
    }
}

@Composable
private fun InfoChip(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Column(
        modifier = modifier
            .background(
                if (isDark) Color(0xFF0F1116) else Color(0xFFF8FAFC),
                RoundedCornerShape(14.dp)
            )
            .padding(horizontal = 10.dp, vertical = 10.dp)
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f), fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

/* ===========================================================
 * Canvas clock picker
 * ===========================================================
 */

private enum class Mode { HOUR, MINUTE }

@Composable
private fun CanvasClockTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    accent: Color,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    val bg = if (isDark) Color(0xFF111318) else cs.surface
    val panel = if (isDark) Color(0xFF0F1116) else cs.surfaceVariant
    val track = if (isDark) Color(0xFF202430) else Color(0xFFE0E5EC)
    val tickColorLong = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF0F172A).copy(alpha = 0.45f)
    val tickColorShort = if (isDark) Color.White.copy(alpha = 0.22f) else Color(0xFF0F172A).copy(alpha = 0.20f)
    val textColor = cs.onSurface

    var hour by remember { mutableStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember { mutableStateOf(initialMinute.coerceIn(0, 59)) }
    var mode by remember { mutableStateOf(Mode.HOUR) }

    fun angleToValue(angle: Float, max: Int): Int {
        val a = angle - (-PI.toFloat() / 2f)
        val norm = ((a % (2f * PI.toFloat())) + (2f * PI.toFloat())) % (2f * PI.toFloat())
        val ratio = norm / (2f * PI.toFloat())
        return (ratio * max).roundToInt().coerceIn(0, max - 1)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { onConfirm(hour, minute) }) {
                Text("Set", color = accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = textColor.copy(alpha = 0.85f))
            }
        },
        containerColor = bg,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(title, color = textColor, fontWeight = FontWeight.SemiBold)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SegButton("Hour", mode == Mode.HOUR, accent) { mode = Mode.HOUR }
                    SegButton("Min", mode == Mode.MINUTE, accent) { mode = Mode.MINUTE }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "%02d:%02d".format(hour, minute),
                        color = textColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        },
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(panel, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .size(240.dp)
                        .pointerInput(mode) {
                            awaitPointerEventScope {
                                while (true) {
                                    val event = awaitPointerEvent()
                                    val p = event.changes.firstOrNull() ?: continue
                                    if (!p.pressed) continue

                                    val pos = p.position
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val dx = pos.x - center.x
                                    val dy = pos.y - center.y
                                    val ang = atan2(dy, dx)
                                    if (mode == Mode.HOUR) hour = angleToValue(ang, 24)
                                    else minute = angleToValue(ang, 60)
                                    p.consume()
                                }
                            }
                        }
                ) {
                    val r = size.minDimension / 2f
                    val stroke = r * 0.10f
                    val center = Offset(size.width / 2f, size.height / 2f)

                    drawCircle(
                        color = track,
                        radius = r - stroke / 2f,
                        center = center,
                        style = Stroke(width = stroke)
                    )

                    val tickCount = if (mode == Mode.HOUR) 24 else 60
                    val longEvery = if (mode == Mode.HOUR) 6 else 5
                    for (i in 0 until tickCount) {
                        val ang =
                            (i.toFloat() / tickCount) * (2f * PI.toFloat()) - (PI.toFloat() / 2f)
                        val isLong = (i % longEvery == 0)
                        val inner = r - stroke - if (isLong) 18f else 10f
                        val outer = r - stroke - 2f
                        drawLine(
                            color = if (isLong) tickColorLong else tickColorShort,
                            start = Offset(center.x + cos(ang) * inner, center.y + sin(ang) * inner),
                            end = Offset(center.x + cos(ang) * outer, center.y + sin(ang) * outer),
                            strokeWidth = if (isLong) 3f else 2f,
                            cap = StrokeCap.Round
                        )
                    }

                    val value = if (mode == Mode.HOUR) hour else minute
                    val max = if (mode == Mode.HOUR) 24 else 60
                    val sweep = (value.toFloat() / max) * 360f
                    val startAngle = -90f

                    drawArc(
                        color = accent,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                        size = Size((r - stroke / 2f) * 2f, (r - stroke / 2f) * 2f),
                        topLeft = Offset(
                            center.x - (r - stroke / 2f),
                            center.y - (r - stroke / 2f)
                        )
                    )

                    val angRad =
                        (value.toFloat() / max) * (2f * PI.toFloat()) - (PI.toFloat() / 2f)
                    val handLen = r - stroke - 26f
                    val hx = center.x + cos(angRad) * handLen
                    val hy = center.y + sin(angRad) * handLen
                    drawLine(
                        color = accent,
                        start = center,
                        end = Offset(hx, hy),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(color = accent, radius = 10f, center = Offset(hx, hy))
                    drawCircle(
                        color = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f),
                        radius = 16f,
                        center = Offset(hx, hy)
                    )
                    drawCircle(color = accent, radius = 10f, center = Offset(hx, hy))
                    drawCircle(
                        color = if (isDark) Color.White.copy(alpha = 0.25f) else Color(0xFFCBD5F5),
                        radius = 8f,
                        center = center
                    )
                }
            }
        }
    )
}

// SleepScreen fonksiyonunun bittiği süslü parantezin DIŞINA, en alta yapıştırın:
fun isSameDay(t1: Long, t2: Long): Boolean {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return sdf.format(Date(t1)) == sdf.format(Date(t2))
}

@Composable
private fun SegButton(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    val bg = if (selected) accent.copy(alpha = 0.18f) else Color.Transparent
    val fg = if (selected) accent else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(999.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(text, color = fg, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/* ===========================================================
 * UTIL
 * ===========================================================
 */

private fun prettyDate(date: String): String = try {
    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfOut = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
    sdfOut.format(sdfIn.parse(date)!!)
} catch (_: Exception) {
    date
}

private fun formatDuration(min: Int): String {
    val m = min.coerceAtLeast(0)
    val h = m / 60
    val mm = m % 60
    return if (h > 0) "${h}h ${mm}m" else "${mm}m"
}

private fun formatClock(ms: Long): String = try {
    val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
    sdf.format(Date(ms))
} catch (_: Exception) {
    "--:--"
}

private fun monthLabel(date: String): String = try {
    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfOut = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    sdfOut.format(sdfIn.parse(date)!!)
} catch (_: Exception) { "" }

private fun shortDay(date: String): String = try {
    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfOut = SimpleDateFormat("dd", Locale.getDefault())
    sdfOut.format(sdfIn.parse(date)!!)
} catch (_: Exception) { "--" }

private fun shortWeekday(date: String): String = try {
    val sdfIn = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val sdfOut = SimpleDateFormat("EEE", Locale.getDefault())
    sdfOut.format(sdfIn.parse(date)!!)
} catch (_: Exception) { "" }