package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import kotlinx.datetime.*
import kotlin.time.Clock

// State

class DateTimePickerState(
    initialMillis: Long? = null,
    initialHour: Int = 0,
    initialMinute: Int = 0
) {
    var millis by mutableStateOf(initialMillis)
    var hour by mutableStateOf(initialHour)
    var minute by mutableStateOf(initialMinute)

    fun toLocalDateTime(zone: TimeZone): LocalDateTime? {
        val base = millisToLocalDateTime(millis, zone) ?: return null
        return LocalDateTime(base.date, LocalTime(hour, minute))
    }
}

// Scrollable number picker

@Suppress("FrequentlyChangingValue")
@Composable
fun NumberScrollPicker(
    range: IntRange,
    selected: Int,
    onSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val itemHeight = 48.dp
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selected - range.first)

    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            onSelected(range.first + listState.firstVisibleItemIndex)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.height(itemHeight * 3),
        flingBehavior = rememberSnapFlingBehavior(listState),
        contentPadding = PaddingValues(vertical = itemHeight)
    ) {
        items(range.count()) { index ->
            val value = range.first + index
            val isSelected = listState.firstVisibleItemIndex == index
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.height(itemHeight).fillMaxWidth()
            ) {
                Text(
                    value.toString().padStart(2, '0'),
                    style = if (isSelected) MaterialTheme.typography.titleLarge
                    else MaterialTheme.typography.bodyMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

@Composable
fun TimePickerDialog(
    initialHour: Int = 0,
    initialMinute: Int = 0,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var tempHour by remember { mutableStateOf(initialHour) }
    var tempMinute by remember { mutableStateOf(initialMinute) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Select Time", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    NumberScrollPicker(
                        range = 0..23,
                        selected = tempHour,
                        onSelected = { tempHour = it },
                        modifier = Modifier.weight(1f)
                    )
                    Text(":", style = MaterialTheme.typography.titleLarge)
                    NumberScrollPicker(
                        range = 0..59,
                        selected = tempMinute,
                        onSelected = { tempMinute = it },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = { onConfirm(tempHour, tempMinute); onDismiss() }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimePickerField(
    label: String,
    dateTimeState: DateTimePickerState,
    showTime: Boolean = true
) {
    var showDateDialog by remember { mutableStateOf(false) }
    var showTimeDialog by remember { mutableStateOf(false) }

    val today = remember {
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    }

    var selectedYear by remember { mutableStateOf(
        dateTimeState.millis?.let { millisToLocalDateTime(it, TimeZone.currentSystemDefault())?.year }
            ?: today.year) }
    var selectedMonth by remember { mutableStateOf(
        dateTimeState.millis?.let { millisToLocalDateTime(it, TimeZone.currentSystemDefault())?.month?.number }
            ?: today.month.number) }
    var selectedDay by remember { mutableStateOf(
        dateTimeState.millis?.let { millisToLocalDateTime(it, TimeZone.currentSystemDefault())?.day }
            ?: today.day) }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        LocalDate(selectedYear, selectedMonth, 1)
            .plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
    }
    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) selectedDay = daysInMonth
    }

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        OutlinedButton(
            onClick = { showDateDialog = true },
            modifier = if (showTime) Modifier.weight(1f) else Modifier.fillMaxWidth()
        ) {
            Text(
                dateTimeState.millis?.let {
                    millisToLocalDateTime(it, TimeZone.currentSystemDefault())?.date.toString()
                } ?: label,
                maxLines = 1
            )
        }
        if (showTime) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(
                onClick = { showTimeDialog = true },
                modifier = Modifier.weight(1f),
                enabled = dateTimeState.millis != null
            ) {
                Text(
                    "${dateTimeState.hour.toString().padStart(2, '0')}:" + dateTimeState.minute.toString().padStart(2, '0'),
                    maxLines = 1
                )
            }
        }
    }

    if (showDateDialog) {
        Dialog(onDismissRequest = { showDateDialog = false }) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(24.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Select Date", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberScrollPicker(1..daysInMonth, selectedDay, { selectedDay = it }, Modifier.weight(1f))
                        NumberScrollPicker(1..12, selectedMonth, { selectedMonth = it }, Modifier.weight(1f))
                        NumberScrollPicker(today.year..today.year + 10, selectedYear, { selectedYear = it }, Modifier.weight(2f))
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDateDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            val date = LocalDate(selectedYear, selectedMonth, selectedDay)
                            dateTimeState.millis = date.atStartOfDayIn(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                            showDateDialog = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }

    if (showTimeDialog) {
        TimePickerDialog(
            initialHour = dateTimeState.hour,
            initialMinute = dateTimeState.minute,
            onDismiss = { showTimeDialog = false },
            onConfirm = { h, m -> dateTimeState.hour = h; dateTimeState.minute = m }
        )
    }
}

// Shared utility

fun millisToLocalDateTime(millis: Long?, zone: TimeZone): LocalDateTime? {
    if (millis == null) return null
    return kotlin.time.Instant.fromEpochMilliseconds(millis).toLocalDateTime(zone)
}