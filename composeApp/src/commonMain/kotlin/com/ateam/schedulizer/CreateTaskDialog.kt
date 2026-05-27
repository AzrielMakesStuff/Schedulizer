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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ateam.schedulizer.entities.Task
import com.ateam.schedulizer.entities.TaskType
import com.ateam.schedulizer.entities.displayName
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateTaskDialog(onDismiss : () -> Unit) {
    val today = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val coroutineScope = rememberCoroutineScope()

    // Input variables
    var taskType by remember { mutableStateOf(TaskType.SET) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dateOne by remember { mutableStateOf(today) }
    var dateTwo by remember { mutableStateOf(today) }

    // State tracking
    var expanded by remember { mutableStateOf(false) }

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
                            val task = Task(
                                type = taskType,
                                title = title,
                                description = description,
                                dateOne = dateOne,
                                dateTwo = dateTwo
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