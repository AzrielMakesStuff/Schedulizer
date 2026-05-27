package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import com.ateam.schedulizer.entities.Task
import com.ateam.schedulizer.entities.TaskType
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.exp
import kotlin.time.Clock

@Composable
fun CreateTaskDialog(onDismiss : () -> Unit) {
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

    // Input variables
    var taskType = remember { TaskType.SET }
    var title = remember { "" }
    var description = remember { "" }
    var dateOne = remember { today }
    var dateTwo = remember { today }

    // State tracking
    var expanded = remember { false }

    // Inputs
    Dialog(onDismissRequest = onDismiss) {
        Box(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            Column {
                Text("Task Type")
                DropdownMenu(expanded, { expanded = false }) {
                    TaskType.entries.toTypedArray().forEach { type ->
                        DropdownMenuItem({
                            Text(type.toString())
                        }, {
                            taskType = type
                            expanded = false
                        })
                    }
                }
            }
        }

    }

    val task = Task(
        type = taskType,
        title = "",
        description = "",
        dateOne = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
        dateTwo = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    )
    rememberCoroutineScope().launch { databaseInstance.createTask(task) }
}

@Composable
fun dropdownItem() {

}