package com.ateam.schedulizer

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontWeight
import androidx.room.Delete
import com.ateam.schedulizer.entities.Task

@Composable
fun HomeScreen(tasks: List<Task>, onCreateTask: () -> Unit, onDeleteTask: (Task) -> Unit) {
    Column {
        Button(onClick = onCreateTask) {
            Text("Create Task")
        }
        tasks.forEach { task ->
            Text(text = "${task.title} - ${task.type} - ${task.dateOne} - ${task.dateTwo}")
            Button(onClick = { onDeleteTask(task) }) {
                Text("Delete")
            }
        }
    }
}