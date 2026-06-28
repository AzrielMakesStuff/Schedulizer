package com.ateam.schedulizer

import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.LocalDateTime

class TaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data before rendering
        databaseInstance.refresh()
        val tasks = databaseInstance.tasks.value
        val filteredTasks = tasks.filter { task ->
            when (task) {
                is Task.Daily -> true
                is Task.Set -> {
                    if (task.completed) {
                        val completedYesterday = task.completionDate?.date?.let { it < today() } ?: false
                        val pastEnd = task.end?.let { it < now() } ?: true
                        !(completedYesterday && pastEnd)
                    } else true
                }
                is Task.Pool -> {
                    if (task.completed) {
                        val completedYesterday = task.completionDate?.date?.let { it < today() } ?: false
                        val pastExpiry = task.expiry?.let { it < now() } ?: true
                        !(completedYesterday && pastExpiry)
                    } else true
                }
            }
        }.sortedWith(compareBy(
            { task ->
                when (task) {
                    is Task.Daily if task.lastCompleted?.date == today() -> 1
                    is Task.Set if task.completed -> 1
                    is Task.Pool if task.completed -> 1
                    else -> 0
                }
            },
            { if (it is Task.Daily) 0 else 1 },
            { task ->
                when (task) {
                    is Task.Pool -> task.expiry ?: LocalDateTime(today(), kotlinx.datetime.LocalTime(23, 30))
                    is Task.Set -> task.start
                    is Task.Daily -> task.lastCompleted ?: LocalDateTime(today(), kotlinx.datetime.LocalTime(0, 0))
                }
            }
        ))
        val pendingCount = filteredTasks.count()

        provideContent {
            WidgetContent(filteredTasks, pendingCount = pendingCount)
        }
    }
}

@Composable
private fun WidgetContent(tasks: List<Task>, pendingCount: Int) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0x1A000000), Color(0x33000000))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$pendingCount incomplete tasks:",
            style = TextStyle(
                color = ColorProvider(Color.Black, Color.White)
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "$pendingCount tasks remaining",
            style = TextStyle(
                color = ColorProvider(Color.Black, Color.White)
            )
        )
    }
}

class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}