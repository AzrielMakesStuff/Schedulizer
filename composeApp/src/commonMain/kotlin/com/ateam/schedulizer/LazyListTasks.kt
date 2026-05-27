package com.ateam.schedulizer

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.core.now
import kotlinx.datetime.LocalDate

@Composable
fun LazyListTasks() {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }

    LaunchedEffect(Unit) {
        databaseInstance.getAllTasks()
        tasks = databaseInstance.getTasksByDate(LocalDate.now())
    }

    LazyColumn {
        items(tasks) { task ->
            Row(Modifier.fillMaxWidth()) {
                Text(task.title)
                Text(task.type.toString())
            }
        }
    }
}