package com.ateam.schedulizer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ateam.schedulizer.entities.Task
import kotlinx.coroutines.launch

@Composable
fun DeleteDialog(task: Task, onDismiss: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { scope.launch { onDismiss() } },
        title = { Text("Delete task") },
        text = { Text("Delete \"${task.title}\"? This can't be undone.") },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    databaseInstance.deleteTask(task)
                    onDismiss()
                }
            }) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = { scope.launch { onDismiss() } }) { Text("Cancel") }
        }
    )
}