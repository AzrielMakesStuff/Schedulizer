@file:Suppress("AssignedValueIsNeverRead")
package com.ateam.schedulizer

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import com.ateam.schedulizer.entities.Task
import androidx.compose.runtime.Composable
import com.kizitonwose.calendar.core.now
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun LazyListTasks() {
    var tasks by remember { mutableStateOf<List<Task>>(emptyList()) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        tasks = databaseInstance.getTasksByDate(LocalDate.now())
    }

    LazyColumn {
        items(tasks) { task ->
            Row(Modifier.fillMaxWidth().padding(15.dp)) {
                Column {
                    Text(task.title, style = MaterialTheme.typography.headlineSmall)
                    Text(task.type.toString())
                    Text(task.description, style = MaterialTheme.typography.bodyLarge)
                    Text(task.dateOne.toString())
                    Text(task.dateTwo.toString())
                }
                Button(onClick = {
                    scope.launch {
                        databaseInstance.deleteTask(task)
                        tasks = databaseInstance.getTasksByDate(LocalDate.now())
                    }
                }) {
                    Text("Delete")
                }
            }
        }
    }
}