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
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(dateOne : LocalDate? = null, dateTwo : LocalDate? = null, hourOne : Int? = null, hourTwo : Int? = null, onDismiss: () -> Unit, task: Task) {
    val timeZone = TimeZone.currentSystemDefault()
    val now = remember { Clock.System.now() }
    val today = remember { now.toLocalDateTime(timeZone) }
    val coroutineScope = rememberCoroutineScope()

    var expanded by remember { mutableStateOf(false) }

    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var taskType by remember { mutableStateOf(task.type) }

    val startState = remember { DateTimePickerState(initialMillis = task.dateOne.date.atStartOfDayIn(timeZone).toEpochMilliseconds(), initialHour = task.dateOne.hour, initialMinute = task.dateOne.minute)
    }
    val endState = remember { DateTimePickerState(initialMillis = task.dateTwo?.date?.atStartOfDayIn(timeZone)?.toEpochMilliseconds(), initialHour = task.dateTwo?.hour ?: 0, initialMinute = task.dateTwo?.minute ?: 0) }

    var ignoreTime by remember { mutableStateOf(false) }
    var hasExpiry by remember { mutableStateOf(dateTwo != null) }
    var oneDay by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text("Edit Task", style = MaterialTheme.typography.titleMedium)
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
                                text = { Text(task.type.displayName()) },
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

                when (task.type) {
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
                    value = task.description,
                    onValueChange = { task.description = it },
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
                                type = task.type,
                                title = title,
                                description = description,
                                completed = false,
                                dateOne = when (task.type) {
                                    TaskType.SET -> startState.toLocalDateTime(timeZone) ?: return@Button
                                    TaskType.POOL, TaskType.DAILY -> today
                                },
                                dateTwo = when (task.type) {
                                    TaskType.SET -> if (!ignoreTime && !oneDay) endState.toLocalDateTime(timeZone) ?: return@Button else null
                                    TaskType.POOL -> if (hasExpiry) endState.toLocalDateTime(timeZone) ?: return@Button else null
                                    TaskType.DAILY -> today
                                }
                            )
                            val updatedTask = task.copy(
                                type = task.type,
                                title = title,
                                description = description,
                            )

                            coroutineScope.launch {
                                databaseInstance.createTask(updatedTask)
                                onDismiss()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = title.isNotBlank()
                    ) {
                        Text("Save Changes")
                    }
                }
            }
        }
    }
}