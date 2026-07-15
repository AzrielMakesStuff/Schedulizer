package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.number
import kotlin.time.Clock

/** Small caps section header used throughout the task dialog for visual grouping. */
@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(bottom = 6.dp)
    )
}

/** Consistent divider + spacing used to separate the dialog's sections. */
@Composable
private fun DialogSectionDivider() {
    Spacer(Modifier.height(14.dp))
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    Spacer(Modifier.height(14.dp))
}

/** Default time-of-day used when a schedule needs one but none was picked yet. */
private val DEFAULT_REMINDER_TIME = LocalTime(9, 0)

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

    // Time-of-day, shared across all schedule types
    var hour by remember { mutableStateOf(if (schedule is ReminderSchedule.None) 5 else schedule.time.hour) }
    var minute by remember { mutableStateOf(if (schedule is ReminderSchedule.None) 0 else schedule.time.minute) }

    // Anchor only matters for EveryNDays — fixed once, at whatever date the
    // task already had, or today for a brand new task. Never silently recomputed.
    val anchor = remember {
        (schedule as? ReminderSchedule.EveryNDays)?.anchor ?: LocalDate.now()
    }

    fun emit() {
        val time = LocalTime(hour, minute)
        val s = when (selectedType) {
            "Daily" -> ReminderSchedule.EveryNDays(1, anchor, time)
            "Every N Days" -> ReminderSchedule.EveryNDays(
                nDays.toIntOrNull()?.coerceAtLeast(2) ?: 2, anchor, time
            )
            "Weekly" -> ReminderSchedule.Weekly(weeklyDay, time)
            "Fortnightly" -> ReminderSchedule.Fortnightly(weeklyDay, 0, time)
            "Monthly by Date" -> ReminderSchedule.MonthlyByDate(monthDay, time)
            "Monthly by Weekday" -> ReminderSchedule.MonthlyByWeekday(ordinal, ordinalDay, time)
            else -> ReminderSchedule.None
        }
        onScheduleChange(s)
    }

    val ordinalLabels = listOf("1st","2nd","3rd","4th","Last")
    val ordinalValues = listOf(1, 2, 3, 4, -1)

    Column(modifier = modifier) {
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
                OrdinalPicker(
                    labels = ordinalLabels,
                    values = ordinalValues,
                    selected = ordinal,
                    onSelected = { ordinal = it; emit() }
                )
                Spacer(Modifier.height(8.dp))
                DayOfWeekPicker(selected = ordinalDay) { ordinalDay = it; emit() }
            }
        }

        if (selectedType != "None") {
            Spacer(Modifier.height(8.dp))
            Text("Reminder Time", style = MaterialTheme.typography.labelMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    NumberScrollPicker(
                        range = 0..23,
                        selected = hour,
                        onSelected = { hour = it; emit() }
                    )
                }
                Text(":", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 4.dp))
                Box(modifier = Modifier.weight(1f)) {
                    NumberScrollPicker(
                        range = 0..59,
                        selected = minute,
                        onSelected = { minute = it; emit() }
                    )
                }
            }
        }
    }
}

/** Segmented-style row for day of week (Mon–Sun, 1–7). Single selection. */
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
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active)
                        MaterialTheme.colorScheme.primaryContainer else
                        MaterialTheme.colorScheme.surface
                )
            ) { Text(label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** Segmented-style row for day of week (Mon–Sun, 1–7). Multi selection, always keeps at least one day chosen. */
@Composable
private fun MultiDayOfWeekPicker(selected: Set<Int>, onSelected: (Set<Int>) -> Unit) {
    val days = listOf("M","T","W","T","F","S","S")
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        days.forEachIndexed { i, label ->
            val iso = i + 1
            val active = iso in selected
            OutlinedButton(
                onClick = {
                    val updated = if (active) selected - iso else selected + iso
                    if (updated.isNotEmpty()) onSelected(updated)
                },
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(0.dp),
                colors = ButtonDefaults.outlinedButtonColors(
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
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active)
                        MaterialTheme.colorScheme.primaryContainer else
                        MaterialTheme.colorScheme.surface
                )
            ) { Text(label, style = MaterialTheme.typography.labelSmall) }
        }
    }
}

/** Horizontally scrollable row of month chips (Jan–Dec), used by the yearly repeat options. */
@Composable
private fun MonthChipRow(selected: Int, onSelected: (Int) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        (1..12).forEach { m ->
            val active = selected == m
            OutlinedButton(
                onClick = { onSelected(m) },
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = if (active)
                        MaterialTheme.colorScheme.primaryContainer else
                        MaterialTheme.colorScheme.surface
                )
            ) { Text(monthName(m), style = MaterialTheme.typography.labelSmall) }
        }
    }
}

private enum class RepeatEndType { NEVER, UNTIL, AFTER }

/**
 * Configures an optional [CalendarRepeatSchedule] for a [Task.Set] — the recurrence
 * pattern plus its end condition. [anchorDate] is the task's current start date and is
 * used both as the reference point for day-of-week/day-of-month defaults and as the
 * `anchor` for an "Every N Days" pattern.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarRepeatPicker(
    schedule: CalendarRepeatSchedule?,
    anchorDate: LocalDate,
    onScheduleChange: (CalendarRepeatSchedule?) -> Unit,
    modifier: Modifier = Modifier
) {
    val types = listOf(
        "Does not repeat", "Daily", "Every N Days", "Weekly", "Fortnightly",
        "Monthly by Date", "Monthly by Weekday", "Yearly by Date", "Yearly by Weekday"
    )

    fun patternToType(p: RecurrencePattern?): String = when (p) {
        null -> "Does not repeat"
        is RecurrencePattern.EveryNDays -> if (p.days == 1) "Daily" else "Every N Days"
        is RecurrencePattern.Weekly -> "Weekly"
        is RecurrencePattern.Fortnightly -> "Fortnightly"
        is RecurrencePattern.MonthlyByDate -> "Monthly by Date"
        is RecurrencePattern.MonthlyByWeekday -> "Monthly by Weekday"
        is RecurrencePattern.YearlyByDate -> "Yearly by Date"
        is RecurrencePattern.YearlyByWeekday -> "Yearly by Weekday"
    }

    val existingPattern = schedule?.pattern
    var expanded by remember { mutableStateOf(false) }
    var selectedType by remember { mutableStateOf(patternToType(existingPattern)) }

    var nDays by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.EveryNDays)?.days?.toString() ?: "2") }
    var weeklyDays by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.Weekly)?.daysOfWeek
            ?: setOf(anchorDate.dayOfWeek.isoDayNumber)) }
    var fortnightlyDay by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.Fortnightly)?.dayOfWeek ?: anchorDate.dayOfWeek.isoDayNumber) }
    var monthDay by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.MonthlyByDate)?.dayOfMonth ?: anchorDate.day) }
    var monthlyOrdinal by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.MonthlyByWeekday)?.ordinal ?: 1) }
    var monthlyOrdinalDay by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.MonthlyByWeekday)?.dayOfWeek ?: anchorDate.dayOfWeek.isoDayNumber) }
    var yearlyMonth by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.YearlyByDate)?.month ?: anchorDate.month.number) }
    var yearlyDay by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.YearlyByDate)?.day ?: anchorDate.day) }
    var yearlyWkMonth by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.YearlyByWeekday)?.month ?: anchorDate.month.number) }
    var yearlyWkOrdinal by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.YearlyByWeekday)?.ordinal ?: 1) }
    var yearlyWkDay by remember { mutableStateOf(
        (existingPattern as? RecurrencePattern.YearlyByWeekday)?.dayOfWeek ?: anchorDate.dayOfWeek.isoDayNumber) }

    var endType by remember { mutableStateOf(
        when (schedule?.end) {
            is RecurrenceEnd.UntilDate -> RepeatEndType.UNTIL
            is RecurrenceEnd.AfterOccurrences -> RepeatEndType.AFTER
            else -> RepeatEndType.NEVER
        }
    ) }
    val untilDateState = remember {
        DateTimePickerState(
            initialMillis = (schedule?.end as? RecurrenceEnd.UntilDate)?.date
                ?.atStartOfDayIn(TimeZone.currentSystemDefault())?.toEpochMilliseconds(),
            initialHour = 0,
            initialMinute = 0
        )
    }
    var afterCount by remember { mutableStateOf(
        (schedule?.end as? RecurrenceEnd.AfterOccurrences)?.count ?: 5) }

    fun buildPattern(): RecurrencePattern? = when (selectedType) {
        "Does not repeat" -> null
        "Daily" -> RecurrencePattern.EveryNDays(1, anchorDate)
        "Every N Days" -> RecurrencePattern.EveryNDays(nDays.toIntOrNull()?.coerceAtLeast(2) ?: 2, anchorDate)
        "Weekly" -> RecurrencePattern.Weekly(weeklyDays)
        "Fortnightly" -> RecurrencePattern.Fortnightly(fortnightlyDay, isoWeekNumber(anchorDate) % 2)
        "Monthly by Date" -> RecurrencePattern.MonthlyByDate(monthDay)
        "Monthly by Weekday" -> RecurrencePattern.MonthlyByWeekday(monthlyOrdinal, monthlyOrdinalDay)
        "Yearly by Date" -> RecurrencePattern.YearlyByDate(yearlyMonth, yearlyDay)
        "Yearly by Weekday" -> RecurrencePattern.YearlyByWeekday(yearlyWkMonth, yearlyWkOrdinal, yearlyWkDay)
        else -> null
    }

    fun buildEnd(): RecurrenceEnd = when (endType) {
        RepeatEndType.NEVER -> RecurrenceEnd.Never
        RepeatEndType.UNTIL -> untilDateState.toLocalDateTime(TimeZone.currentSystemDefault())?.date
            ?.let { RecurrenceEnd.UntilDate(it) } ?: RecurrenceEnd.Never
        RepeatEndType.AFTER -> RecurrenceEnd.AfterOccurrences(afterCount.coerceAtLeast(1))
    }

    fun emit() {
        val pattern = buildPattern()
        onScheduleChange(if (pattern == null) null else CalendarRepeatSchedule(pattern, buildEnd()))
    }

    val ordinalLabels = listOf("1st","2nd","3rd","4th","Last")
    val ordinalValues = listOf(1, 2, 3, 4, -1)

    Column(modifier = modifier) {
        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
            OutlinedTextField(
                value = selectedType,
                onValueChange = {},
                readOnly = true,
                label = { Text("Repeat") },
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

        if (selectedType == "Does not repeat") return@Column

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
            "Weekly" -> MultiDayOfWeekPicker(selected = weeklyDays) { weeklyDays = it; emit() }
            "Fortnightly" -> DayOfWeekPicker(selected = fortnightlyDay) { fortnightlyDay = it; emit() }
            "Monthly by Date" -> {
                NumberScrollPicker(range = 1..31, selected = monthDay, onSelected = { monthDay = it; emit() })
                Text(
                    "Clamped to last day in shorter months.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            "Monthly by Weekday" -> {
                OrdinalPicker(ordinalLabels, ordinalValues, monthlyOrdinal) { monthlyOrdinal = it; emit() }
                Spacer(Modifier.height(8.dp))
                DayOfWeekPicker(selected = monthlyOrdinalDay) { monthlyOrdinalDay = it; emit() }
            }
            "Yearly by Date" -> {
                MonthChipRow(selected = yearlyMonth) { yearlyMonth = it; emit() }
                Spacer(Modifier.height(8.dp))
                NumberScrollPicker(range = 1..31, selected = yearlyDay, onSelected = { yearlyDay = it; emit() })
            }
            "Yearly by Weekday" -> {
                MonthChipRow(selected = yearlyWkMonth) { yearlyWkMonth = it; emit() }
                Spacer(Modifier.height(8.dp))
                OrdinalPicker(ordinalLabels, ordinalValues, yearlyWkOrdinal) { yearlyWkOrdinal = it; emit() }
                Spacer(Modifier.height(8.dp))
                DayOfWeekPicker(selected = yearlyWkDay) { yearlyWkDay = it; emit() }
            }
        }

        Spacer(Modifier.height(12.dp))
        Text("Ends", style = MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(RepeatEndType.NEVER to "Never", RepeatEndType.UNTIL to "On date", RepeatEndType.AFTER to "After N")
                .forEach { (type, label) ->
                    val active = endType == type
                    OutlinedButton(
                        onClick = { endType = type; emit() },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(0.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (active)
                                MaterialTheme.colorScheme.primaryContainer else
                                MaterialTheme.colorScheme.surface
                        )
                    ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                }
        }

        when (endType) {
            RepeatEndType.UNTIL -> {
                Spacer(Modifier.height(8.dp))
                DateTimePickerField(label = "Until", dateTimeState = untilDateState, showTime = false)
                LaunchedEffect(untilDateState.millis) { emit() }
            }
            RepeatEndType.AFTER -> {
                Spacer(Modifier.height(8.dp))
                NumberScrollPicker(range = 1..52, selected = afterCount, onSelected = { afterCount = it; emit() })
            }
            RepeatEndType.NEVER -> {}
        }

        buildPattern()?.let { pattern ->
            Spacer(Modifier.height(8.dp))
            Text(
                "Repeats " + CalendarRepeatSchedule(pattern, buildEnd()).displayName(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
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
    var repeatSchedule by remember {
        mutableStateOf((task as? Task.Set)?.repeatFrequency)
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

    // Daily tasks always send a same-time-every-day reminder — there's no meaningful
    // choice of frequency, so we lock it in as soon as this becomes a Daily task
    // rather than exposing the frequency picker at all.
    LaunchedEffect(taskTypeSelection) {
        if (taskTypeSelection is TaskTypeSelection.Daily) {
            val current = reminderSchedule
            if (current !is ReminderSchedule.EveryNDays || current.days != 1) {
                reminderSchedule = ReminderSchedule.EveryNDays(1, LocalDate.now(), DEFAULT_REMINDER_TIME)
            }
        }
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
                .widthIn(max = 440.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    if (isEditing) "Edit Task" else "Create New Task",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                DialogSectionDivider()

                SectionLabel("Type")
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
                                    if (type != TaskTypeSelection.Set) repeatSchedule = null
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                DialogSectionDivider()

                SectionLabel("Schedule")
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

                        Spacer(modifier = Modifier.height(12.dp))
                        SectionLabel("Repeat")
                        CalendarRepeatPicker(
                            schedule = repeatSchedule,
                            anchorDate = startState.toLocalDateTime(timeZone)?.date ?: LocalDate.now(),
                            onScheduleChange = { repeatSchedule = it }
                        )
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

                DialogSectionDivider()

                SectionLabel("Details")
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

                DialogSectionDivider()

                SectionLabel("Reminder")
                if (taskTypeSelection is TaskTypeSelection.Daily) {
                    // No frequency choice for Daily tasks — always fires once a day.
                    // Only the time of day is configurable.
                    Text(
                        "A reminder is sent every day at the same time.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val dailySchedule = reminderSchedule as? ReminderSchedule.EveryNDays
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            NumberScrollPicker(
                                range = 0..23,
                                selected = dailySchedule?.time?.hour ?: DEFAULT_REMINDER_TIME.hour,
                                onSelected = { h ->
                                    val anchor = dailySchedule?.anchor ?: LocalDate.now()
                                    val minute = dailySchedule?.time?.minute ?: DEFAULT_REMINDER_TIME.minute
                                    reminderSchedule = ReminderSchedule.EveryNDays(1, anchor, LocalTime(h, minute))
                                }
                            )
                        }
                        Text(":", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 4.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            NumberScrollPicker(
                                range = 0..59,
                                selected = dailySchedule?.time?.minute ?: DEFAULT_REMINDER_TIME.minute,
                                onSelected = { m ->
                                    val anchor = dailySchedule?.anchor ?: LocalDate.now()
                                    val hour = dailySchedule?.time?.hour ?: DEFAULT_REMINDER_TIME.hour
                                    reminderSchedule = ReminderSchedule.EveryNDays(1, anchor, LocalTime(hour, m))
                                }
                            )
                        }
                    }
                } else {
                    ReminderSchedulePicker(
                        schedule = reminderSchedule,
                        onScheduleChange = { reminderSchedule = it }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

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
                                        completionDate = (task as? Task.Set)?.completionDate,
                                        repeatFrequency = repeatSchedule,
                                        occurrenceIndex = (task as? Task.Set)?.occurrenceIndex ?: 1
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
                                databaseInstance.upsertTask(newTask)
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