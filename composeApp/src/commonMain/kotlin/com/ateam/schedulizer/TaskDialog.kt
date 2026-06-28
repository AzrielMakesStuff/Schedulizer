package com.ateam.schedulizer

import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
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
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

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

sealed class TaskTypeSelection {
    object Set : TaskTypeSelection()
    object Pool : TaskTypeSelection()
    object Daily : TaskTypeSelection()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDialog(
    initialDate: LocalDate? = null,
    initialHour: Int? = null,
    initialMinute: Int? = null,
    initialEndDate: LocalDate? = null,
    initialEndHour: Int? = null,
    initialEndMinute: Int? = null,
    onDismiss: () -> Unit,
    task: Task? = null
) {
    val isEditing = task != null
    val timeZone = TimeZone.currentSystemDefault()
    val now = remember { Clock.System.now() }
    val today = remember { now.toLocalDateTime(timeZone) }
    val coroutineScope = rememberCoroutineScope()

    var taskTypeSelection by remember {
        mutableStateOf(
            when (task) {
                is Task.Set -> TaskTypeSelection.Set
                is Task.Pool -> TaskTypeSelection.Pool
                is Task.Daily -> TaskTypeSelection.Daily
                null -> TaskTypeSelection.Set
            }
        )
    }

    var title by remember { mutableStateOf(task?.title ?: "") }
    var description by remember { mutableStateOf(task?.description ?: "") }
    var reminderSchedule by remember {
        mutableStateOf(
            task?.reminderSchedule ?: ReminderSchedule.None
        )
    }

    var ignoreTime by remember {
        mutableStateOf(
            when (task) {
                is Task.Set -> task.start.hour == 0 && task.start.minute == 0
                        && task.end?.hour == 23 && task.end.minute == 59

                is Task.Pool -> task.expiry?.hour == 23 && task.expiry.minute == 59
                else -> false
            }
        )
    }
    var oneDay by remember {
        mutableStateOf(
            task is Task.Set
                    && task.end != null
                    && task.start.date == task.end.date
                    && task.start.hour == 0 && task.end.hour == 23
        )
    }
    var hasExpiry by remember {
        mutableStateOf(task is Task.Pool && task.expiry != null)
    }

    val startState = remember {
        DateTimePickerState(
            initialMillis = when (task) {
                is Task.Set -> task.start.date.atStartOfDayIn(timeZone).toEpochMilliseconds()
                else -> initialDate?.atStartOfDayIn(timeZone)?.toEpochMilliseconds()
            },
            initialHour = when (task) {
                is Task.Set -> task.start.hour
                else -> initialHour ?: 0
            },
            initialMinute = when (task) {
                is Task.Set -> task.start.minute
                else -> initialMinute ?: 0
            }
        )
    }
    val endState = remember {
        DateTimePickerState(
            initialMillis = when (task) {
                is Task.Set -> task.end?.date?.atStartOfDayIn(timeZone)?.toEpochMilliseconds()
                is Task.Pool -> task.expiry?.date?.atStartOfDayIn(timeZone)?.toEpochMilliseconds()
                else -> initialEndDate?.atStartOfDayIn(timeZone)?.toEpochMilliseconds()
            },
            initialHour = when (task) {
                is Task.Set -> task.end?.hour ?: 0
                is Task.Pool -> task.expiry?.hour ?: 0
                else -> initialEndHour ?: 0
            },
            initialMinute = when (task) {
                is Task.Set -> task.end?.minute ?: 0
                is Task.Pool -> task.expiry?.minute ?: 0
                else -> initialEndMinute ?: 0
            }
        )
    }

    var expanded by remember { mutableStateOf(false) }
    val typeLabel = when (taskTypeSelection) {
        is TaskTypeSelection.Set -> "Set Task"
        is TaskTypeSelection.Pool -> "Pool Task"
        is TaskTypeSelection.Daily -> "Daily Task"
    }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (isEditing) "Edit Task" else "Create New Task",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(16.dp))

                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = typeLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Task Type") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }) {
                        listOf(
                            TaskTypeSelection.Set to "Set Task",
                            TaskTypeSelection.Pool to "Pool Task",
                            TaskTypeSelection.Daily to "Daily Task"
                        ).forEach { (type, label) ->
                            DropdownMenuItem(
                                text = { Text(label) },
                                onClick = {
                                    taskTypeSelection = type
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

                when (taskTypeSelection) {
                    is TaskTypeSelection.Set -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = oneDay, onCheckedChange = { oneDay = it })
                            Text("One Day")
                        }
                        if (!oneDay) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = ignoreTime,
                                    onCheckedChange = { ignoreTime = it })
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

                    is TaskTypeSelection.Pool -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = hasExpiry, onCheckedChange = { hasExpiry = it })
                            Text("Expires?")
                        }
                        if (hasExpiry) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = ignoreTime,
                                    onCheckedChange = { ignoreTime = it })
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

                    is TaskTypeSelection.Daily -> {
                        Text(
                            "Daily tasks automatically reset each day.",
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
                            fun DateTimePickerState.toStartOfDay(zone: TimeZone): LocalDateTime? {
                                val base = millisToLocalDateTime(millis, zone) ?: return null
                                return LocalDateTime(base.date, LocalTime(0, 0))
                            }

                            fun DateTimePickerState.toEndOfDay(zone: TimeZone): LocalDateTime? {
                                val base = millisToLocalDateTime(millis, zone) ?: return null
                                return LocalDateTime(base.date, LocalTime(23, 59))
                            }

                            val newTask: Task = when (taskTypeSelection) {
                                is TaskTypeSelection.Set -> {
                                    val start = when {
                                        oneDay || ignoreTime -> startState.toStartOfDay(timeZone)
                                            ?: return@Button

                                        else -> startState.toLocalDateTime(timeZone)
                                            ?: return@Button
                                    }
                                    val end = when {
                                        oneDay -> startState.toEndOfDay(timeZone) ?: return@Button
                                        ignoreTime -> endState.toEndOfDay(timeZone) ?: return@Button
                                        else -> endState.toLocalDateTime(timeZone)
                                    }
                                    Task.Set(
                                        id = task?.id ?: 0,
                                        title = title,
                                        description = description,
                                        reminderSchedule = reminderSchedule,
                                        start = start,
                                        end = end,
                                        completed = (task as? Task.Set)?.completed ?: false,
                                        completionDate = (task as? Task.Set)?.completionDate
                                    )
                                }

                                is TaskTypeSelection.Pool -> {
                                    val expiry = when {
                                        !hasExpiry -> null
                                        ignoreTime -> endState.toEndOfDay(timeZone) ?: return@Button
                                        else -> endState.toLocalDateTime(timeZone) ?: return@Button
                                    }
                                    Task.Pool(
                                        id = task?.id ?: 0,
                                        title = title,
                                        description = description,
                                        reminderSchedule = reminderSchedule,
                                        expiry = expiry,
                                        completed = (task as? Task.Pool)?.completed ?: false,
                                        completionDate = (task as? Task.Pool)?.completionDate
                                    )
                                }

                                is TaskTypeSelection.Daily -> Task.Daily(
                                    id = task?.id ?: 0,
                                    title = title,
                                    description = description,
                                    reminderSchedule = reminderSchedule,
                                    lastCompleted = (task as? Task.Daily)?.lastCompleted ?: Clock.System.now()
                                        .toLocalDateTime(TimeZone.currentSystemDefault())
                                )
                            }

                            coroutineScope.launch {
                                databaseInstance.createTask(newTask)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank()
                    ) {
                        Text(if (isEditing) "Save Changes" else "Save")
                    }
                }
            }
        }
    }
}