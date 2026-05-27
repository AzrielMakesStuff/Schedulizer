package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import com.ateam.schedulizer.entities.TaskType
import com.ateam.schedulizer.entities.displayName
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(onDismiss : () -> Unit) {
    val timeZone = TimeZone.currentSystemDefault()
    val now = remember { Clock.System.now() }
    val today = remember { now.toLocalDateTime(timeZone) }
    val yesterday = remember { now.minus(1, DateTimeUnit.DAY, timeZone).toLocalDateTime(timeZone) }
    val coroutineScope = rememberCoroutineScope()

    // Input variables
    var taskType by remember { mutableStateOf(TaskType.SET) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateOne by remember { mutableStateOf(today) }
    var dateTwo by remember { mutableStateOf(today) }

    // State tracking
    var expanded by remember { mutableStateOf(false) }
    var allDay by remember { mutableStateOf(false) }
    var ignoreTime by remember { mutableStateOf(false) }
    val startDateState = rememberDatePickerState()
    val endDateState = rememberDatePickerState()

    // Inputs
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
        ) {
            Column {
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
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                        },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        TaskType.entries.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.displayName()) },
                                onClick = {
                                    taskType = type
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
                            Checkbox(
                                checked = allDay,
                                onCheckedChange = { allDay = it }
                            )
                            Text("All Day")
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ignoreTime,
                                onCheckedChange = { ignoreTime = it },
                                enabled = !allDay
                            )
                            Text("Ignore Time")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Start Date")
                        DatePicker(state = startDateState)

                        Spacer(modifier = Modifier.height(8.dp))

                        Text("End Date")
                        DatePicker(state = endDateState)
                    }
                    TaskType.POOL -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ignoreTime,
                                onCheckedChange = { ignoreTime = it }
                            )
                            Text("Ignore Time")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = ignoreTime,
                                onCheckedChange = { ignoreTime = it },
                                enabled = !allDay
                            )
                            Text("Expires?")
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        Text("Expiry Date")
                        DatePicker(state = endDateState)
                    }
                    else -> {}
                }

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
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            fun startOfDay(date: LocalDateTime): LocalDateTime {
                                return LocalDateTime(date.year, date.month.number, date.day, 0, 0)
                            }

                            fun endOfDay(date: LocalDateTime): LocalDateTime {
                                return LocalDateTime(date.year, date.month.number, date.day, 23, 59)
                            }

                            val finalDateOne: LocalDateTime
                            val finalDateTwo: LocalDateTime

                            if (taskType == TaskType.SET || taskType == TaskType.POOL) {

                                if (allDay) {
                                    finalDateOne = startOfDay(dateOne)
                                    finalDateTwo = endOfDay(dateTwo)
                                } else if (ignoreTime) {
                                    finalDateOne = dateOne
                                    finalDateTwo = endOfDay(dateTwo)
                                } else {
                                    finalDateOne = dateOne
                                    finalDateTwo = dateTwo
                                }

                            } else {
                                finalDateOne = dateOne
                                finalDateTwo = dateTwo
                            }

                            val task = Task(
                                type = taskType,
                                title = title,
                                description = description,
                                dateOne = if (taskType == TaskType.SET) finalDateOne else today,
                                dateTwo = if (taskType == TaskType.DAILY) yesterday else finalDateTwo
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
    fun millisToLocalDateTime(millis: Long?): LocalDateTime {
        val instant = kotlin.time.Instant.fromEpochMilliseconds(millis!!)
        return instant.toLocalDateTime(timeZone)
    }
}