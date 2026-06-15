package com.example.stepforge.debug

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.PauseCircle
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.example.stepforge.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebugPanelScreen(
    onBack: () -> Unit
) {
    val ctx = LocalContext.current
    val logs by DebugLogStore.logs.collectAsState()
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f

    var query by remember { mutableStateOf("") }
    var packageFilter by remember { mutableStateOf("com.example.stepforge") }
    var threadFilter by remember { mutableStateOf("") }
    var selectedLevels by remember {
        mutableStateOf(
            DebugLogLevel.entries.associateWith { true }
        )
    }
    var selectedEntry by remember { mutableStateOf<DebugLogEntry?>(null) }
    var paused by remember { mutableStateOf(!DebugLogStore.loggingEnabled) }

    val filtered = remember(logs, query, packageFilter, threadFilter, selectedLevels) {
        logs.filter { entry ->
            val levelOk = selectedLevels[entry.level] == true
            val queryOk = query.isBlank() ||
                    entry.message.contains(query, ignoreCase = true) ||
                    entry.tag.contains(query, ignoreCase = true) ||
                    entry.className.contains(query, ignoreCase = true) ||
                    (entry.stackTrace?.contains(query, ignoreCase = true) == true)

            val pkgOk = packageFilter.isBlank() ||
                    (entry.packageName?.contains(packageFilter, ignoreCase = true) == true)

            val threadOk = threadFilter.isBlank() ||
                    entry.threadName.contains(threadFilter, ignoreCase = true)

            levelOk && queryOk && pkgOk && threadOk
        }.sortedByDescending { it.timestamp }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.debug_console), fontWeight = FontWeight.Bold)
                        Text(stringResource(R.string.debug_logs_count, filtered.size), fontSize = 11.sp, color = cs.onSurface.copy(alpha = 0.65f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            paused = !paused
                            DebugLogStore.setLoggingEnabled(!paused)
                        }
                    ) {
                        Icon(
                            imageVector = if (paused) Icons.Outlined.PlayCircle else Icons.Outlined.PauseCircle,
                            contentDescription = null
                        )
                    }

                    IconButton(
                        onClick = {
                            val file = DebugLogExporter.exportToTextFile(ctx, filtered)
                            if (file != null) {
                                val shared = DebugLogExporter.shareFile(ctx, file)
                                if (!shared) {
                                    Toast.makeText(ctx, ctx.getString(R.string.debug_share_failed), Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(ctx, ctx.getString(R.string.debug_export_failed), Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Outlined.FileUpload, contentDescription = null)
                    }

                    IconButton(
                        onClick = { DebugLogStore.clear() }
                    ) {
                        Icon(Icons.Outlined.Delete, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = cs.background)
            )
        }
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(cs.background)
                .padding(pad)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.debug_search_logs)) },
                colors = OutlinedTextFieldDefaults.colors()
            )

            OutlinedTextField(
                value = packageFilter,
                onValueChange = { packageFilter = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.debug_package_filter)) },
                colors = OutlinedTextFieldDefaults.colors()
            )

            OutlinedTextField(
                value = threadFilter,
                onValueChange = { threadFilter = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text(stringResource(R.string.debug_thread_filter)) },
                colors = OutlinedTextFieldDefaults.colors()
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DebugLogLevel.entries.forEach { level ->
                    FilterChip(
                        selected = selectedLevels[level] == true,
                        onClick = {
                            selectedLevels = selectedLevels.toMutableMap().apply {
                                this[level] = !(this[level] ?: true)
                            }
                        },
                        label = {
                            Text(
                                text = level.name,
                                maxLines = 1
                            )
                        }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                items(filtered, key = { it.id }) { entry ->
                    DebugLogItem(
                        entry = entry,
                        onClick = { selectedEntry = entry }
                    )
                }
            }
        }
    }

    selectedEntry?.let { entry ->
        DebugLogDetailDialog(
            entry = entry,
            onDismiss = { selectedEntry = null }
        )
    }
}

@Composable
private fun DebugLogItem(
    entry: DebugLogEntry,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val sdf = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    val levelColor = when (entry.level) {
        DebugLogLevel.VERBOSE -> Color(0xFF90A4AE)
        DebugLogLevel.DEBUG -> Color(0xFF42A5F5)
        DebugLogLevel.INFO -> Color(0xFF26A69A)
        DebugLogLevel.WARNING -> Color(0xFFFFB300)
        DebugLogLevel.ERROR -> Color(0xFFEF5350)
        DebugLogLevel.CRASH -> Color(0xFFD32F2F)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = cs.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(levelColor, RoundedCornerShape(999.dp))
                )
                Spacer(Modifier.width(8.dp))
                Text(entry.level.name, color = levelColor, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                Spacer(Modifier.width(10.dp))
                Text(sdf.format(Date(entry.timestamp)), color = cs.onSurface.copy(alpha = 0.65f), fontSize = 11.sp)
            }

            Text(
                text = entry.message,
                color = cs.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium
            )

            Text(
                text = "${entry.className.substringAfterLast('.')}#${entry.methodName}:${entry.lineNumber} • ${entry.threadName}",
                color = cs.onSurface.copy(alpha = 0.62f),
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun DebugLogDetailDialog(
    entry: DebugLogEntry,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val analysis = remember(entry, context) { DebugIssueAnalyzer.analyze(context, entry) }
    val sdf = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()) }
    val scrollState = rememberScrollState()

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_close))
            }
        },
        title = {
            Text(stringResource(R.string.debug_log_detail), fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(stringResource(R.string.debug_time_format, sdf.format(Date(entry.timestamp))))
                Text(stringResource(R.string.debug_level_format, entry.level))
                Text(stringResource(R.string.debug_tag_format, entry.tag))
                Text(stringResource(R.string.debug_thread_format, entry.threadName))
                Text(stringResource(R.string.debug_class_format, entry.className))
                Text(stringResource(R.string.debug_method_format, entry.methodName))
                Text(stringResource(R.string.debug_line_format, entry.lineNumber))
                Text(stringResource(R.string.debug_message_format, entry.message))

                if (!entry.stackTrace.isNullOrBlank()) {
                    Text(stringResource(R.string.debug_stacktrace), fontWeight = FontWeight.Bold)
                    Text(entry.stackTrace!!)
                }

                Text(stringResource(R.string.debug_probable_cause), fontWeight = FontWeight.Bold)
                Text(analysis.probableCause)

                Text(stringResource(R.string.debug_suggestion), fontWeight = FontWeight.Bold)
                Text(analysis.suggestion)
            }
        }
    )
}
