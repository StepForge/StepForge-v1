package com.example.stepforge.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CalendarActivity(
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    minDate: LocalDate = LocalDate.now().minusYears(5),
    maxDate: LocalDate = LocalDate.now(),
    markedDates: Map<LocalDate, Float> = emptyMap()
) {
    val cs = MaterialTheme.colorScheme
    val isDark = cs.background.luminance() < 0.5f
    val accent = Color(0xFF00F5C8)
    val secondAccent = Color(0xFF21E6A3)
    var visibleMonth by remember(selectedDate) { mutableStateOf(YearMonth.from(selectedDate)) }
    var pendingDate by remember(selectedDate) { mutableStateOf(selectedDate) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy", Locale.getDefault()) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(34.dp),
            color = if (isDark) Color(0xFF05070B) else Color(0xFFF8FAF9),
            tonalElevation = 0.dp,
            shadowElevation = 28.dp,
            border = BorderStroke(
                1.dp,
                if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.07f)
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = if (isDark) 0.08f else 0.10f),
                                Color.Transparent
                            )
                        )
                    )
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CalendarHeader(
                    selectedDate = pendingDate,
                    accent = accent,
                    secondAccent = secondAccent
                )

                Card(
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0B1117) else Color.White
                    ),
                    border = BorderStroke(
                        1.dp,
                        if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { visibleMonth = visibleMonth.minusMonths(1) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.10f))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowLeft,
                                    contentDescription = "Previous month",
                                    tint = accent
                                )
                            }

                            Text(
                                text = visibleMonth.atDay(1).format(monthFormatter),
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = cs.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            IconButton(
                                onClick = { visibleMonth = visibleMonth.plusMonths(1) },
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(accent.copy(alpha = 0.10f))
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.KeyboardArrowRight,
                                    contentDescription = "Next month",
                                    tint = accent
                                )
                            }
                        }

                        HorizontalDivider(color = cs.outline.copy(alpha = 0.12f))

                        WeekHeaderRow()

                        val dates = remember(visibleMonth) { visibleMonth.calendarCells() }
                        dates.chunked(7).forEach { week ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                week.forEach { date ->
                                    val isInMonth = YearMonth.from(date) == visibleMonth
                                    val isEnabled = !date.isBefore(minDate) && !date.isAfter(maxDate)
                                    CalendarDayCell(
                                        date = date,
                                        isInMonth = isInMonth,
                                        isEnabled = isEnabled,
                                        isSelected = date == pendingDate,
                                        isToday = date == LocalDate.now(),
                                        markedProgress = markedDates[date]?.coerceIn(0f, 1f),
                                        accent = accent,
                                        modifier = Modifier.weight(1f),
                                        onClick = { if (isEnabled) pendingDate = date }
                                    )
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Cancel",
                            fontWeight = FontWeight.Bold,
                            color = cs.onSurfaceVariant
                        )
                    }

                    Button(
                        onClick = { onDateSelected(pendingDate) },
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp),
                        shape = RoundedCornerShape(999.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accent,
                            contentColor = Color.White
                        )
                    ) {
                        Text(
                            text = "Select",
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalendarHeader(
    selectedDate: LocalDate,
    accent: Color,
    secondAccent: Color
) {
    val cs = MaterialTheme.colorScheme
    val titleFormatter = remember { DateTimeFormatter.ofPattern("EEEE", Locale.getDefault()) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(accent, secondAccent))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = selectedDate.dayOfMonth.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = "Choose date",
                color = cs.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = selectedDate.format(titleFormatter),
                color = cs.onSurface,
                fontSize = 21.sp,
                lineHeight = 23.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = selectedDate.format(dateFormatter),
                color = cs.onSurfaceVariant,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun WeekHeaderRow() {
    val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CalendarDayCell(
    date: LocalDate,
    isInMonth: Boolean,
    isEnabled: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    markedProgress: Float?,
    accent: Color,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.06f else 1f,
        animationSpec = tween(180),
        label = "calendarDayScale"
    )
    val bg = when {
        isSelected -> accent
        isToday -> accent.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val borderColor = when {
        isSelected -> Color.Transparent
        isToday -> accent.copy(alpha = 0.42f)
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> Color.White
        !isEnabled -> cs.onSurfaceVariant.copy(alpha = 0.28f)
        !isInMonth -> cs.onSurfaceVariant.copy(alpha = 0.42f)
        else -> cs.onSurface
    }

    Box(
        modifier = modifier
            .height(42.dp)
            .clip(CircleShape)
            .background(bg)
            .border(1.dp, borderColor, CircleShape)
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        if (markedProgress != null && isEnabled) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = 2.2.dp.toPx()
                val diameter = size.minDimension - stroke
                val topLeft = androidx.compose.ui.geometry.Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)
                drawArc(
                    color = if (isSelected) Color.White.copy(alpha = 0.24f) else accent.copy(alpha = 0.16f),
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
                drawArc(
                    color = if (isSelected) Color.White else accent,
                    startAngle = -90f,
                    sweepAngle = 360f * markedProgress.coerceAtLeast(0.18f),
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round)
                )
            }
        }

        Text(
            text = date.dayOfMonth.toString(),
            color = textColor,
            fontSize = 13.sp,
            fontWeight = if (isSelected || isToday || markedProgress != null) FontWeight.ExtraBold else FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
    }
}

private fun YearMonth.calendarCells(): List<LocalDate> {
    val first = atDay(1)
    val firstIndex = first.dayOfWeek.toCalendarIndex()
    val gridStart = first.minusDays(firstIndex.toLong())
    return (0 until 42).map { gridStart.plusDays(it.toLong()) }
}

private fun DayOfWeek.toCalendarIndex(): Int = when (this) {
    DayOfWeek.MONDAY -> 0
    DayOfWeek.TUESDAY -> 1
    DayOfWeek.WEDNESDAY -> 2
    DayOfWeek.THURSDAY -> 3
    DayOfWeek.FRIDAY -> 4
    DayOfWeek.SATURDAY -> 5
    DayOfWeek.SUNDAY -> 6
}
