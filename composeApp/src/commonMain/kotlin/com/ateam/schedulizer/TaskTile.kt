package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateam.schedulizer.entities.Task
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.minus

@Composable
fun TaskTile(task: Task, onScrollToTop: (LocalDate) -> Unit, editTask: () -> Unit, deleteTask: () -> Unit) {

    val scope = rememberCoroutineScope()

    val isCompletedToday = task is Task.Daily && task.lastCompleted?.date == today()

    val isExpired = when (task) {
        is Task.Set -> !task.completed && task.end != null && task.end < now()
        is Task.Pool -> !task.completed && task.expiry != null && task.expiry < now()
        is Task.Daily -> false
    }

    val isChecked = when (task) {
        is Task.Daily -> isCompletedToday
        is Task.Set -> task.completed
        is Task.Pool -> task.completed
    }

    val alpha = if (isChecked) 0.4f else 1f
    val textColor = when {
        isChecked -> MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        isExpired -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurface
    }

    val subtleColor = when {
        isChecked -> MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.6f)
        isExpired -> MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    }

    val cardBackground = when {
        isChecked -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        isExpired -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(cardBackground)
            .clickable { if (task is Task.Set) onScrollToTop(task.start.date) }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = { checked ->
                scope.launch {
                    val updated = when (task) {
                        is Task.Daily -> {
                            val updated = task.copy(lastCompleted = if (checked) now() else LocalDateTime(now().date.minus(1, DateTimeUnit.DAY), now().time))
                            println("DAILY STREAK RESET TO: ${updated.lastCompleted}")
                            updated
                        }

                        is Task.Set -> task.copy(completed = checked, completionDate = if (checked) now() else null)

                        is Task.Pool -> task.copy(
                            completed = checked,
                            completionDate = if (checked) now() else null
                        )
                    }
                    databaseInstance.upsertTask(updated)
                }
            }
        )

        Spacer(Modifier.width(8.dp))

        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = task.title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = textColor,
                    textDecoration = if (isChecked) androidx.compose.ui.text.style.TextDecoration.LineThrough
                    else androidx.compose.ui.text.style.TextDecoration.None
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = task.displayName(),
                    fontSize = 10.sp,
                    color = when {
                        isExpired -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.primary.copy(alpha = alpha)
                    },
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            when {
                                isExpired -> MaterialTheme.colorScheme.errorContainer.copy(
                                    alpha = 0.5f
                                )

                                else -> MaterialTheme.colorScheme.primaryContainer.copy(
                                    alpha = alpha * 0.5f
                                )
                            }
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            if (task.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    task.description, fontSize = 12.sp, color = subtleColor,
                    maxLines = 2, overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(Modifier.height(4.dp))

            when (task) {
                is Task.Set -> {
                    if (task.end != null) {
                        Text(
                            "${task.start.fmt()} to ${task.end.fmt()}",
                            fontSize = 11.sp, color = subtleColor
                        )
                    } else {
                        Text(task.start.fmt(), fontSize = 11.sp, color = subtleColor)
                    }
                }

                is Task.Pool -> {
                    if (task.expiry != null) {
                        Text(
                            "Expires ${task.expiry.fmt()}",
                            fontSize = 11.sp,
                            color = subtleColor
                        )
                    } else {
                        Text("No expiry", fontSize = 11.sp, color = subtleColor)
                    }
                }

                is Task.Daily -> Text("Daily task", fontSize = 11.sp, color = subtleColor)
            }

            val completionDate = when (task) {
                is Task.Set -> task.completionDate
                is Task.Pool -> task.completionDate
                is Task.Daily -> null
            }
            if (isChecked && completionDate != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Completed ${completionDate.fmt()}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            if (isChecked && task is Task.Daily) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Completed today", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
            }
            if (isExpired) {
                Spacer(Modifier.height(2.dp))
                Text(
                    "Expired", fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }

        IconButton(onClick = { editTask() }, enabled = !isChecked) {
            Icon(
                Icons.Outlined.Edit, contentDescription = "Edit Task",
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isChecked) 0.3f else 0.8f)
            )
        }
        IconButton(onClick = { deleteTask() }) {
            Icon(
                Icons.Outlined.Delete, contentDescription = "Delete Task",
                tint = MaterialTheme.colorScheme.error.copy(alpha = alpha)
            )
        }
    }
}