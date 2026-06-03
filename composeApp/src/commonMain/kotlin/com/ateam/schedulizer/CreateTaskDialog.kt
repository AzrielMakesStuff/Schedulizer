package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ateam.schedulizer.entities.Task
import com.ateam.schedulizer.entities.TaskType
import com.ateam.schedulizer.entities.displayName
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderSchedulePicker(
    schedule: ReminderSchedule,
    onScheduleChange: (ReminderSchedule) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = listOf("None", "Daily", "Every N Days", "Weekly",
        "Fortnightly", "Monthly by Date", "Monthly by Weekday")

    fun scheduleToType(s: ReminderSchedule) = when (s) {
        is ReminderSchedule.None -> "None"
        is ReminderSchedule.EveryNDays -> if (s.days == 1) "Daily" else "Every N Days"
        is ReminderSchedule.Weekly -> "Weekly"
        is ReminderSchedule.Fortnightly -> "Fortnightly"
        is ReminderSchedule.MonthlyByDate -> "Monthly by Date"
        is ReminderSchedule.MonthlyByWeekday -> "Monthly by Weekday"
    }

    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(scheduleToType(schedule)) }

    // Sub-fields
    var nDays by remember { mutableStateOf(
        (schedule as? ReminderSchedule.EveryNDays)?.days?.toString() ?: "2") }
    var weeklyDay by remember { mutableStateOf(
        (schedule as? ReminderSchedule.Weekly)?.dayOfWeek
            ?: (schedule as? ReminderSchedule.Fortnightly)?.dayOfWeek ?: 1) }
    var monthDay by remember { mutableStateOf(
        (schedule as? ReminderSchedule.MonthlyByDate)?.dayOfMonth ?: 1) }
    var ordinal by remember { mutableStateOf(
        (schedule as? ReminderSchedule.MonthlyByWeekday)?.ordinal ?: 1) }
    var ordinalDay by remember { mutableStateOf(
        (schedule as? ReminderSchedule.MonthlyByWeekday)?.dayOfWeek ?: 1) }

    // Emit updated schedule whenever sub fields change
    fun emit() {
        val s = when (selectedType) {
            "Daily" -> ReminderSchedule.EveryNDays(1)
            "Every N Days" -> ReminderSchedule.EveryNDays(nDays.toIntOrNull()?.coerceAtLeast(2) ?: 2)
            "Weekly" -> ReminderSchedule.Weekly(weeklyDay)
            "Fortnightly" -> ReminderSchedule.Fortnightly(weeklyDay, 0)
            "Monthly by Date" -> ReminderSchedule.MonthlyByDate(monthDay)
            "Monthly by Weekday" -> ReminderSchedule.MonthlyByWeekday(ordinal, ordinalDay)
            else -> ReminderSchedule.None
        }
        onScheduleChange(s)
    }

    val dayNames = listOf("Mon","Tue","Wed","Thu","Fri","Sat","Sun")
    val ordinalLabels = listOf("1st","2nd","3rd","4th","Last")
    val ordinalValues = listOf(1, 2, 3, 4, -1)

    Column(modifier = modifier) {
        // Type selector
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Reminder Frequency") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                modifier = Modifier
                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                types.forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type) },
                        onClick = { selectedType = type; expanded = false; emit() }
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))
        when (selectedType) {
            "Every N Days" -> {
                OutlinedTextField(
                    value = nDays,
                    onValueChange = { nDays = it; emit() },
                    label = { Text("Every N days") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "Weekly", "Fortnightly" -> {
                DayOfWeekPicker(selected = weeklyDay) { weeklyDay = it; emit() }
            }
            "Monthly by Date" -> {
                NumberScrollPicker(
                    range = 1..31,
                    selected = monthDay,
                    onSelected = { monthDay = it; emit() }
                )
                Text(
                    "Clamped to last day in shorter months.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            "Monthly by Weekday" -> {
                // Ordinal picker (1st–4th, Last)
                OrdinalPicker(
                    labels  = ordinalLabels,
                    values  = ordinalValues,
                    selected = ordinal,
                    onSelected = { ordinal = it; emit() }
                )
                Spacer(Modifier.height(8.dp))
                DayOfWeekPicker(selected = ordinalDay) { ordinalDay = it; emit() }
            }
        }
    }
}

/** Segmented-style row for day of week (Mon–Sun, 1–7). */
@Composable
private fun DayOfWeekPicker(selected: Int, onSelected: (Int) -> Unit) {
    val days = listOf("M","T","W","T","F","S","S")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEachIndexed { i, label ->
            val iso = i + 1
            val active = selected == iso
            OutlinedButton(
                onClick = { onSelected(iso) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active)
                        MaterialTheme.colorScheme.primaryContainer else
                        MaterialTheme.colorScheme.surface
                )
            ) { Text(label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** Row of ordinal buttons: 1st, 2nd, 3rd, 4th, Last. */
@Composable
private fun OrdinalPicker(
    labels: List<String>, values: List<Int>,
    selected: Int, onSelected: (Int) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        labels.zip(values).forEach { (label, value) ->
            val active = selected == value
            OutlinedButton(
                onClick = { onSelected(value) },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp),
                colors = androidx.compose.material3.ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active)
                        MaterialTheme.colorScheme.primaryContainer else
                        MaterialTheme.colorScheme.surface
                )
            ) { Text(label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(dateOne : LocalDate? = null, dateTwo : LocalDate? = null, hourOne : Int? = null, hourTwo : Int? = null, onDismiss: () -> Unit) {
    val timeZone = TimeZone.currentSystemDefault()
    val now = remember { Clock.System.now() }
    val today = remember { now.toLocalDateTime(timeZone) }
    val coroutineScope = rememberCoroutineScope()

    var taskType by remember { mutableStateOf(TaskType.SET) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var ignoreTime by remember { mutableStateOf(false) }
    var hasExpiry by remember { mutableStateOf(dateTwo != null) }
    var expanded by remember { mutableStateOf(false) }
    var reminderSchedule by remember { mutableStateOf<ReminderSchedule>(ReminderSchedule.None) }
    var oneDay by remember { mutableStateOf(false) }


    val startState = remember {
        DateTimePickerState(
            initialMillis = dateOne?.atStartOfDayIn(timeZone)?.toEpochMilliseconds(),
            initialHour = hourOne ?: 0
        )
    }
    val endState = remember {
        DateTimePickerState(
            initialMillis = dateTwo?.atStartOfDayIn(timeZone)?.toEpochMilliseconds(),
            initialHour = hourTwo ?: 0
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

                ReminderSchedulePicker(
                    schedule = reminderSchedule,
                    onScheduleChange = { reminderSchedule = it }
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
                                reminderSchedule = reminderSchedule,
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