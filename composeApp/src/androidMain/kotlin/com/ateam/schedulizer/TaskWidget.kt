package com.ateam.schedulizer

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.color.ColorProvider
import androidx.glance.layout.*
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextDecoration
import androidx.glance.text.TextStyle
import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.minus

// Action parameter keys
private val taskIdKey = ActionParameters.Key<Int>("task_id")

// Toggle
class ToggleTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context, glanceId: GlanceId, parameters: ActionParameters
    ) {
        val id = parameters[taskIdKey] ?: return
        val allTasks = databaseInstance.getAll()
        val task = allTasks.find { it.id.toInt() == id } ?: return
        val updated = when (task) {
            is Task.Daily -> {
                val alreadyDone = task.lastCompleted?.date == today()
                val yesterday = LocalDateTime(today().minus(1, DateTimeUnit.DAY), LocalTime(0, 0))
                task.copy(lastCompleted = if (alreadyDone) yesterday else now())
            }
            is Task.Set -> task.copy(
                completed = !task.completed,
                completionDate = if (!task.completed) now() else null
            )
            is Task.Pool -> task.copy(
                completed = !task.completed,
                completionDate = if (!task.completed) now() else null
            )
        }
        databaseInstance.createTask(updated)
    }
}

// Create
class CreateTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context, glanceId: GlanceId, parameters: ActionParameters
    ) {
        context.startActivity(
            Intent(context, WidgetTaskActivity::class.java)
                .putExtra(WidgetTaskActivity.EXTRA_ACTION, WidgetTaskActivity.ACTION_CREATE)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }
}

// Edit
class EditTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context, glanceId: GlanceId, parameters: ActionParameters
    ) {
        val id = parameters[taskIdKey] ?: return
        context.startActivity(
            Intent(context, WidgetTaskActivity::class.java)
                .putExtra(WidgetTaskActivity.EXTRA_ACTION, WidgetTaskActivity.ACTION_EDIT)
                .putExtra(WidgetTaskActivity.EXTRA_TASK_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }
}

// Delete
class DeleteTaskAction : ActionCallback {
    override suspend fun onAction(
        context: Context, glanceId: GlanceId, parameters: ActionParameters
    ) {
        val id = parameters[taskIdKey] ?: return
        context.startActivity(
            Intent(context, WidgetTaskActivity::class.java)
                .putExtra(WidgetTaskActivity.EXTRA_ACTION, WidgetTaskActivity.ACTION_DELETE)
                .putExtra(WidgetTaskActivity.EXTRA_TASK_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }
}

class OpenActivityAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        context.startActivity(
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        )
    }
}

// Widget
class TaskWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val tasks by databaseInstance.tasks.collectAsState()
            val filtered = remember(tasks) {
                tasks
                    .filter { task ->
                        when (task) {
                            is Task.Daily -> true
                            is Task.Set -> {
                                if (task.completed) {
                                    val doneYesterday = task.completionDate?.date?.let { it < today() } ?: false
                                    val pastEnd = task.end?.let { it < now() } ?: true
                                    !(doneYesterday && pastEnd)
                                } else true
                            }
                            is Task.Pool -> {
                                if (task.completed) {
                                    val doneYesterday = task.completionDate?.date?.let { it < today() } ?: false
                                    val pastExpiry = task.expiry?.let { it < now() } ?: true
                                    !(doneYesterday && pastExpiry)
                                } else true
                            }
                        }
                    }
                    .sortedWith(compareBy(
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
                                is Task.Pool -> task.expiry ?: LocalDateTime(today(), LocalTime(23, 30))
                                is Task.Set -> task.start
                                is Task.Daily -> task.lastCompleted ?: LocalDateTime(today(), LocalTime(0, 0))
                            }
                        }
                    ))
            }
            WidgetContent(tasks = filtered)
        }
    }
}

// UI
@Composable
private fun WidgetContent(tasks: List<Task>) {
    val surface = ColorProvider(Color(0xFFFFFFFF), Color(0xFF1C1C1E))
    val onSurface = ColorProvider(Color(0xFF1C1C1E), Color(0xFFFFFFFF))
    val muted = ColorProvider(Color(0xFF8E8E93), Color(0xFF8E8E93))
    val accent = ColorProvider(Color(0xFF007AFF), Color(0xFF0A84FF))
    val danger = ColorProvider(Color(0xFFFF3B30), Color(0xFFFF453A))
    val green = ColorProvider(Color(0xFF1A7A33), Color(0xFF4CD964))
    val errorBg = ColorProvider(Color(0xFFFFF0F0), Color(0xFF2A1515))
    val checkedBg = ColorProvider(Color(0xFFF5F5F5), Color(0xFF141414))
    val divider = ColorProvider(Color(0x1A000000), Color(0x1AFFFFFF))
    val headerDivider = ColorProvider(Color(0x33000000), Color(0x33FFFFFF))

    Column(modifier = GlanceModifier.fillMaxSize().background(surface).clickable(actionRunCallback<OpenActivityAction>())) {

        // Header
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Tasks",
                style = TextStyle(color = onSurface, fontSize = 17.sp, fontWeight = FontWeight.Bold),
                modifier = GlanceModifier.defaultWeight()
            )
            Image(
                provider = ImageProvider(R.drawable.ic_widget_add),
                contentDescription = "Create task",
                colorFilter = androidx.glance.ColorFilter.tint(accent),
                modifier = GlanceModifier
                    .size(32.dp)
                    .clickable(actionRunCallback<CreateTaskAction>())
                    .padding(4.dp)
            )
        }

        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(0.5.dp)
                .background(headerDivider)
        ) {}

        // Empty state
        if (tasks.isEmpty()) {
            Box(
                modifier = GlanceModifier.fillMaxSize().clickable(actionRunCallback<OpenActivityAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text("No tasks", style = TextStyle(color = muted, fontSize = 13.sp))
            }
            return@Column
        }

        // Task rows
        LazyColumn(modifier = GlanceModifier.fillMaxSize()) {
            items(tasks) { task ->
                val isChecked = when (task) {
                    is Task.Daily -> task.lastCompleted?.date == today()
                    is Task.Set -> task.completed
                    is Task.Pool  -> task.completed
                }
                val isExpired = when (task) {
                    is Task.Set -> !task.completed && task.end != null && task.end < now()
                    is Task.Pool -> !task.completed && task.expiry != null && task.expiry < now()
                    is Task.Daily -> false
                }

                val rowBg = if (isExpired) errorBg else if (isChecked) checkedBg else surface
                val titleColor = if (isChecked) muted else if (isExpired) danger else onSurface
                val metaColor = if (isExpired) danger else muted
                val badgeColor = if (task is Task.Daily) green else accent
                val badgeLabel = when (task) {
                    is Task.Set -> "Set"
                    is Task.Pool -> "Pool"
                    is Task.Daily -> "Daily"
                }
                val metaLine = when (task) {
                    is Task.Set -> if (task.end != null) "${task.start.fmt()} -> ${task.end.fmt()}" else task.start.fmt()
                    is Task.Pool -> if (task.expiry != null) "Expires ${task.expiry.fmt()}" else "No expiry"
                    is Task.Daily -> "Daily task"
                }
                val id = task.id.toInt()

                Column(modifier = GlanceModifier.fillMaxWidth()) {
                    Row(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .background(rowBg)
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Checkbox
                        Image(
                            provider = ImageProvider(
                                if (isChecked) R.drawable.ic_widget_check_on
                                else R.drawable.ic_widget_check_off
                            ),
                            contentDescription = if (isChecked) "Mark incomplete" else "Mark complete",
                            colorFilter = androidx.glance.ColorFilter.tint(
                                if (isChecked) accent else muted
                            ),
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(
                                    actionRunCallback<ToggleTaskAction>(
                                        actionParametersOf(taskIdKey to id)
                                    )
                                )
                                .padding(end = 6.dp)
                        )

                        // Task body
                        Column(modifier = GlanceModifier.defaultWeight()) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = task.title,
                                    style = TextStyle(
                                        color = titleColor,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        textDecoration = if (isChecked) TextDecoration.LineThrough
                                        else TextDecoration.None
                                    ),
                                    maxLines = 1,
                                    modifier = GlanceModifier.defaultWeight()
                                )
                                Spacer(GlanceModifier.width(4.dp))
                                Text(
                                    text = badgeLabel,
                                    style = TextStyle(color = badgeColor, fontSize = 9.sp)
                                )
                                if (isExpired) {
                                    Spacer(GlanceModifier.width(3.dp))
                                    Text(
                                        text = "Expired",
                                        style = TextStyle(color = danger, fontSize = 9.sp)
                                    )
                                }
                            }
                            if (task.description.isNotBlank()) {
                                Text(
                                    text = task.description,
                                    style = TextStyle(color = muted, fontSize = 11.sp),
                                    maxLines = 1
                                )
                            }
                            Text(
                                text = metaLine,
                                style = TextStyle(color = metaColor, fontSize = 10.sp),
                                maxLines = 1
                            )
                        }

                        // Edit (hidden when checked)
                        if (!isChecked) {
                            Image(
                                provider = ImageProvider(R.drawable.ic_widget_edit),
                                contentDescription = "Edit task",
                                colorFilter = androidx.glance.ColorFilter.tint(muted),
                                modifier = GlanceModifier
                                    .size(28.dp)
                                    .clickable(
                                        actionRunCallback<EditTaskAction>(
                                            actionParametersOf(taskIdKey to id)
                                        )
                                    )
                                    .padding(horizontal = 2.dp)
                            )
                        } else {
                            Spacer(GlanceModifier.width(28.dp))
                        }

                        // Delete
                        Image(
                            provider = ImageProvider(R.drawable.ic_widget_delete),
                            contentDescription = "Delete task",
                            colorFilter = androidx.glance.ColorFilter.tint(danger),
                            modifier = GlanceModifier
                                .size(28.dp)
                                .clickable(
                                    actionRunCallback<DeleteTaskAction>(
                                        actionParametersOf(taskIdKey to id)
                                    )
                                )
                                .padding(start = 2.dp)
                        )
                    }

                    Box(
                        modifier = GlanceModifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(divider)
                    ) {}
                }
            }

            item {
                Box(
                    modifier = GlanceModifier
                        .fillMaxSize()
                        .height(1500.dp)
                        .clickable(actionRunCallback<OpenActivityAction>()),
                ) {}
            }
        }
    }
}

// Receiver
class TaskWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = TaskWidget()
}