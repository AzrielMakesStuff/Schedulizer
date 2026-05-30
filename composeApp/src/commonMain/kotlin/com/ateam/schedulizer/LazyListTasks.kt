@file:Suppress("AssignedValueIsNeverRead")
package com.ateam.schedulizer

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun LazyListTasks() {
    val tasks by databaseInstance.tasks.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) { databaseInstance.refresh() }

    LazyColumn {
        items(tasks) { task ->
            Row(Modifier.fillMaxWidth().padding(15.dp)) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "${task.title} - ${task.type}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(task.description,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text("${task.dateOne} - ${task.dateTwo.toString()}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                IconButton(onClick = { scope.launch {databaseInstance.deleteTask(task)}}) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Item"
                    )
                }
            }
        }
    }
}