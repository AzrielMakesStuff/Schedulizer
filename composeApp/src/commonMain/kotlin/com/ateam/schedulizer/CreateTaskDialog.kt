package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ateam.schedulizer.entities.Task
import com.ateam.schedulizer.entities.TaskType
import com.ateam.schedulizer.entities.displayName
import kotlinx.coroutines.launch
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number
import kotlin.time.Clock

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

    var selectedYear by remember { mutableStateOf(dateTimeState.millis?.let {
        millisToLocalDateTime(it, TimeZone.currentSystemDefault())?.year
    } ?: today.year) }
    var selectedMonth by remember { mutableStateOf(dateTimeState.millis?.let {
        millisToLocalDateTime(it, TimeZone.currentSystemDefault())?.month?.number
    } ?: today.month.number) }
    var selectedDay by remember { mutableStateOf(dateTimeState.millis?.let {
        millisToLocalDateTime(it, TimeZone.currentSystemDefault())?.day
    } ?: today.day) }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        LocalDate(selectedYear, selectedMonth, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day
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
                    "${dateTimeState.hour.toString().padStart(2, '0')}:${dateTimeState.minute.toString().padStart(2, '0')}",
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
                        // Day
                        NumberScrollPicker(
                            range = 1..daysInMonth,
                            selected = selectedDay,
                            onSelected = { selectedDay = it },
                            modifier = Modifier.weight(1f)
                        )
                        // Month
                        NumberScrollPicker(
                            range = 1..12,
                            selected = selectedMonth,
                            onSelected = { selectedMonth = it },
                            modifier = Modifier.weight(1f)
                        )
                        // Year
                        NumberScrollPicker(
                            range = today.year..today.year + 10,
                            selected = selectedYear,
                            onSelected = { selectedYear = it },
                            modifier = Modifier.weight(2f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showDateDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = {
                            val date = LocalDate(selectedYear, selectedMonth, selectedDay)
                            val instant = date.atStartOfDayIn(TimeZone.currentSystemDefault())
                            dateTimeState.millis = instant.toEpochMilliseconds()
                            showDateDialog = false
                        }) { Text("OK") }
                    }
                }
            }
        }
    }

    if (showTimeDialog) {
        Dialog(onDismissRequest = { showTimeDialog = false }) {
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
                            selected = dateTimeState.hour,
                            onSelected = { dateTimeState.hour = it },
                            modifier = Modifier.weight(1f)
                        )
                        Text(":", style = MaterialTheme.typography.titleLarge)
                        NumberScrollPicker(
                            range = 0..59,
                            selected = dateTimeState.minute,
                            onSelected = { dateTimeState.minute = it },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = { showTimeDialog = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        TextButton(onClick = { showTimeDialog = false }) { Text("OK") }
                    }
                }
            }
        }
    }
}

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

// Holder for a date+time selection
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

@Composable
fun rememberDateTimePickerState() = remember { DateTimePickerState() }

@OptIn(ExperimentalMaterial3Api::class)

@Composable
fun CreateTaskDialog(date : LocalDate? = null, onDismiss: () -> Unit) {
    val timeZone = TimeZone.currentSystemDefault()
    val now = remember { Clock.System.now() }
    val today = remember { now.toLocalDateTime(timeZone) }
    val coroutineScope = rememberCoroutineScope()

    var taskType by remember { mutableStateOf(TaskType.SET) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var ignoreTime by remember { mutableStateOf(false) }
    var hasExpiry by remember { mutableStateOf(date != null) }
    var expanded by remember { mutableStateOf(false) }
    var oneDay by remember { mutableStateOf(false) }

    val startState = rememberDateTimePickerState()
    val endState = remember {
        DateTimePickerState(
            initialMillis = date?.atStartOfDayIn(timeZone)?.toEpochMilliseconds()
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Create New Task", style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = taskType.displayName(),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Task Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        TaskType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName()) },
                                onClick = {
                                    taskType = type
                                    ignoreTime = false
                                    hasExpiry = false
                                    oneDay = false
                                    expanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                when (taskType) {
                    TaskType.SET -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = oneDay, onCheckedChange = { oneDay = it })
                            Text("One Day")
                        }
                        if (!oneDay) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = ignoreTime,
                                    onCheckedChange = { ignoreTime = it }
                                )
                                Text("Ignore Time")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Start", style = MaterialTheme.typography.labelMedium)
                        DateTimePickerField(
                            label = "Pick start date",
                            dateTimeState = startState,
                            showTime = !ignoreTime && !oneDay
                        )
                        if (!oneDay) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("End", style = MaterialTheme.typography.labelMedium)
                            DateTimePickerField(
                                label = "Pick end date",
                                dateTimeState = endState,
                                showTime = !ignoreTime
                            )
                        }
                    }
                    TaskType.POOL -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasExpiry, onCheckedChange = { hasExpiry = it })
                            Text("Expires?")
                        }
                        if (hasExpiry) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = ignoreTime, onCheckedChange = { ignoreTime = it })
                                Text("Ignore Time")
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Expiry", style = MaterialTheme.typography.labelMedium)
                            DateTimePickerField(
                                label = "Pick expiry date",
                                dateTimeState = endState,
                                showTime = !ignoreTime
                            )
                        }
                    }
                    TaskType.DAILY -> {
                        Text(
                            "Daily tasks automatically start today.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            val task = Task(
                                type = taskType,
                                title = title,
                                description = description,
                                completed = false,
                                dateOne = when (taskType) {
                                    TaskType.SET -> startState.toLocalDateTime(timeZone) ?: return@Button
                                    TaskType.POOL, TaskType.DAILY -> today
                                },
                                dateTwo = when (taskType) {
                                    TaskType.SET -> if (!ignoreTime && !oneDay) endState.toLocalDateTime(timeZone) ?: return@Button else null
                                    TaskType.POOL -> if (hasExpiry) endState.toLocalDateTime(timeZone) ?: return@Button else null
                                    TaskType.DAILY -> today
                                }
                            )
                            coroutineScope.launch {
                                databaseInstance.createTask(task)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

fun millisToLocalDateTime(millis: Long?, zone: TimeZone): LocalDateTime? {
    if (millis == null) return null
    return kotlin.time.Instant
        .fromEpochMilliseconds(millis)
        .toLocalDateTime(zone)
}