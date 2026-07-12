package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock.System

val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.THURSDAY)
fun today() = System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
//fun now() = System.now().toLocalDateTime(TimeZone.currentSystemDefault())

private fun formatTime(time: LocalDateTime?) = time?.time?.toString()?.take(5) ?: ""

private fun formatDate(date: LocalDate) =
    "${date.dayOfWeek.name.take(3)}, ${date.month.name.take(3)} ${date.day}"
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Calendar() {
    val tasks by databaseInstance.tasks.collectAsState(initial = emptyList())
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeek = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek = firstDayOfWeek)}

    val laneOf = remember(tasks) {
        val sorted = tasks.sortedBy { task ->
            when (task) {
                is Task.Set -> task.start
                is Task.Pool -> task.expiry
                is Task.Daily -> null
            }
        }
        val lanes = mutableListOf<MutableList<Task>>()
        val laneMap = mutableMapOf<Task, Int>()

        sorted.forEach { task ->
            val tStart = when (task) {
                is Task.Set -> task.start.date
                is Task.Pool -> task.expiry?.date ?: today()
                is Task.Daily -> return@forEach  // dailies don't appear on calendar
            }
            val tEnd = when (task) {
                is Task.Set -> task.end?.date ?: tStart
                is Task.Pool -> task.expiry?.date ?: tStart
                is Task.Daily -> return@forEach
            }
            var placed = false
            for (laneIdx in lanes.indices) {
                val conflict = lanes[laneIdx].any { existing ->
                    val eStart = when (existing) {
                        is Task.Set -> existing.start.date
                        is Task.Pool -> existing.expiry?.date ?: today()
                        is Task.Daily -> return@any false
                    }
                    val eEnd = when (existing) {
                        is Task.Set -> existing.end?.date ?: eStart
                        is Task.Pool -> existing.expiry?.date ?: eStart
                        is Task.Daily -> return@any false
                    }
                    tStart <= eEnd && tEnd >= eStart
                }
                if (!conflict) {
                    lanes[laneIdx].add(task)
                    laneMap[task] = laneIdx
                    placed = true
                    break
                }
            }
            if (!placed) {
                lanes.add(mutableListOf(task))
                laneMap[task] = lanes.lastIndex
            }
        }
        laneMap
    }

    val taskByDate = remember(tasks) {
        val map = mutableMapOf<LocalDate, MutableList<Task>>()
        tasks.forEach { task ->
            val (start, end) = when (task) {
                is Task.Set -> task.start.date to (task.end?.date ?: task.start.date)
                is Task.Pool -> task.expiry?.date?.let { it to it } ?: return@forEach
                is Task.Daily -> return@forEach
            }
            var current = start
            while (current <= end) {
                map.getOrPut(current) { mutableListOf() }.add(task)
                current = current.plus(1, DateTimeUnit.DAY)
            }
        }
        map
    }

    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            //.background(background)
            .background(MaterialTheme.colorScheme.background)
    ) {
        val currentMonth = remember { YearMonth.now() }
        val startMonth = remember { currentMonth.minusMonths(100) }
        val endMonth = remember { currentMonth.plusMonths(100) }
        val state = rememberCalendarState(
            startMonth = startMonth,
            endMonth = endMonth,
            firstVisibleMonth = currentMonth,
            firstDayOfWeek = daysOfWeek.first(),
            outDateStyle = OutDateStyle.EndOfGrid)

        Column(modifier = Modifier
            .fillMaxSize()
            .padding(8.dp)
        ) {
//            val backgroundColor = MaterialTheme.colorScheme.surface
//
//            HorizontalCalendar(
//                modifier = Modifier.weight(1f),
//                monthBody = { _, content ->
//                    Box(modifier = Modifier.background(backgroundColor).wrapContentHeight()) {
//                        content()
//                    }
//                },
//                state = state,
//                dayContent = { Day(it, taskByDate[it.date] ?: emptyList(), laneOf) },
//                monthHeader = { month: CalendarMonth ->
//                    val daysOfWeek = remember(month) {
//                        month.weekDays.first().map { it.date.dayOfWeek }
//                    }
//                    val monthName = month.yearMonth.month.name
//                        .lowercase()
//                        .replaceFirstChar { it.uppercase() }
//                    val year = month.yearMonth.year
//                    Column {
//                        MonthName(monthName = monthName, year = year)
//                        MonthHeader(daysOfWeek = daysOfWeek)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                HorizontalCalendar(
                    modifier = Modifier.fillMaxSize(),
                    state = state,
                    dayContent = { Day(it, taskByDate[it.date] ?: emptyList(), laneOf, daysOfWeek.first())},
                    monthHeader = { month: CalendarMonth ->
                        val monthName = month.yearMonth.month.name
                            .lowercase()
                            .replaceFirstChar {it.uppercase() }
                        val year = month.yearMonth.year
                        Column(modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)) {
                            MonthName(monthName = monthName, year = year)
                            Spacer(Modifier.height(8.dp))
                            MonthHeader(daysOfWeek = daysOfWeek)
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>) {
    val textColoring = MaterialTheme.colorScheme.onBackground
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = DayOfWeekNames.ENGLISH_ABBREVIATED.names[dayOfWeek.ordinal],
                color = textColoring
            )
        }
    }
}

@Composable
fun MonthName(monthName: String, year: Int) {
    //val textColoring = MaterialTheme.colorScheme.onBackground
    Text(
//        modifier = Modifier
//            .fillMaxWidth()
//            .padding(vertical = 4.dp),
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.Center,
//        color = textColoring,
//        fontSize = 22.sp,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        text = ("$monthName $year")
    )
}

@Composable
fun Day(day: CalendarDay, tasks: List<Task>, laneOf: Map<Task, Int>, firstDayOfWeek: DayOfWeek) {
    val isMonthDate = day.position == DayPosition.MonthDate
    val baseColor = MaterialTheme.colorScheme.onSurface
    val textColor = if (isMonthDate) baseColor else baseColor.copy(alpha = 0.3f)
    val isToday = day.date == today()
    val primaryColor = MaterialTheme.colorScheme.primary

    var showOverlay by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }
    val now = remember { now() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .border(
//                color = if ((isToday) && (isMonthDate)) textColor.copy(alpha = 0.8f) else Color.Transparent.copy(
//                    alpha = 0.2f
//                ),
//                width = 1.5.dp,
                color = if (isToday && isMonthDate) primaryColor.copy(alpha = 0.5f) else Color.Gray.copy(alpha = 0.1f),
                width = 0.5.dp,
            )
            .clickable(enabled = isMonthDate, onClick = {
                selectedTask = null
                showOverlay = true
            })
            .background(
//                color = if ((isToday) && (isMonthDate)) MaterialTheme.colorScheme.primaryContainer.copy(
//                    alpha = 0.3f
//                ) else Color.Transparent
                color = if (isToday && isMonthDate) primaryColor.copy(alpha = 0.15f) else Color.Transparent
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
//                modifier = Modifier.padding(top = 10.dp),
                modifier = Modifier.padding(top = 4.dp),
                text = day.date.day.toString(),
//                color = if (isToday) textColor.copy(alpha = 0.8f) else textColor,
////                fontWeight = FontWeight.Bold,
                color = if (isToday) primaryColor else textColor,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.SemiBold,
                fontSize = 12.sp
            )

            Spacer (Modifier.height(4.dp))

            for (laneIndex in 0..2) {
                val task = tasks.find { laneOf[it] == laneIndex }
                if (task != null) {
                    val isStart = when (task) {
                        is Task.Set -> task.start.date == day.date
                        is Task.Pool -> task.expiry?.date == day.date
                        is Task.Daily -> false
                    }

                    val pillColor = when (task) {
                        is Task.Set -> when {
                            task.completed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            task.end != null && task.end < now -> MaterialTheme.colorScheme.error
                            else -> primaryColor
                        }
                        is Task.Pool -> when {
                            task.completed -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            task.expiry != null && task.expiry < now -> MaterialTheme.colorScheme.error
                            else -> primaryColor
                        }
                        else -> primaryColor
                    }

                    val isEnd = when (task) {
                        is Task.Set -> (task.end?.date ?: task.start.date) == day.date
                        is Task.Pool -> task.expiry?.date == day.date
                        is Task.Daily -> false
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .padding(vertical = 1.dp)
                            .padding(
                                start = if (isStart) 2.dp else 0.dp,
                                end = if (isEnd) 2.dp else 0.dp
                            )
//                            .background(
//                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
//                                shape = RoundedCornerShape(
//                                    topStart = if (isStart) 2.dp else 0.dp,
//                                    bottomStart = if (isStart) 2.dp else 0.dp,
//                                    topEnd = if (isEnd) 2.dp else 0.dp,
//                                    bottomEnd = if (isEnd) 2.dp else 0.dp
                            .clip(
                                RoundedCornerShape(
                                    topStart = if (isStart) 4.dp else 0.dp,
                                    bottomStart = if (isStart) 4.dp else 0.dp,
                                    topEnd = if (isEnd) 4.dp else 0.dp,
                                    bottomEnd = if (isEnd) 4.dp else 0.dp
                                )
                            )
                            .background(pillColor.copy(alpha = 0.18f))
                            .drawWithContent {
                                drawContent()
                                if (isStart) {
                                    drawLine(
                                        color = pillColor,
                                        start = Offset(0f, 4.dp.toPx()),
                                        end = Offset(0f, size.height - 4.dp.toPx()),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )
                                }
                            }
                            .clickable {
                                selectedTask = task
                                showOverlay = true
                            },
                            contentAlignment = Alignment.CenterStart
                    ) {
                        if (isStart || day.date.dayOfWeek == firstDayOfWeek || day.date.day == 1) {
                            Text(
                                text = task.title,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.SemiBold,
                                lineHeight = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = pillColor,
                                modifier = Modifier.padding(start = if (isStart) 8.dp else 4.dp, end = 4.dp)
                            )
                        }
                    }
                } else {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                            .clickable(enabled = isMonthDate) {
                                selectedTask = null
                                showOverlay = true
                            }
                    )
                }
            }
        }
        if (isMonthDate) {
            DetailsOverlay(
                isVisible = showOverlay,
                onDismiss = { showOverlay = false },
                day = day,
                tasks = tasks,
                selectedTask = selectedTask
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailsOverlay(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    day: CalendarDay,
    tasks: List<Task>,
    selectedTask: Task? = null
) {
    val scope = rememberCoroutineScope()
    var createTaskDialog by remember { mutableStateOf(false) }
    var editingTask by remember { mutableStateOf<Task?>(null) }

    if (createTaskDialog) {
        TaskDialog(
            initialDate = day.date,
            onDismiss = { createTaskDialog = false }
        )
    }

    editingTask?.let { task ->
        TaskDialog(
            task = task,
            onDismiss = { editingTask = null }
        )
    }

    if (isVisible) {
        Popup(
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.6f))
                    .clickable { onDismiss() },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .padding(24.dp)
                        .widthIn(max = 450.dp)
                        .shadow(12.dp, shape = RoundedCornerShape(16.dp))
                        .background(
                            MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .clickable(enabled = false) { },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val currentDayStr = day.date.dayOfWeek.name.lowercase()
                            .replaceFirstChar { it.uppercase() }
                        Text(
                            text = "$currentDayStr, ${day.date.month.name.take(3)} ${day.date.day}",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        if (selectedTask != null) {
                            Text(
                                text = selectedTask.title,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                            )
                            Text(
                                text = selectedTask.displayName(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.secondary,
                            )
                            val timeInfo = when (selectedTask) {
                                is Task.Set -> "${formatTime(selectedTask.start)} - ${
                                    formatTime(selectedTask.end)
                                }"

                                is Task.Pool -> selectedTask.expiry?.let {
                                    "Due by ${formatTime(it)}"
                                } ?: "No deadline"

                                else -> "Daily"
                            }
                            Text(
                                timeInfo,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                            if (selectedTask.description.isNotEmpty()) {
                                Text(
                                    selectedTask.description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(top = 12.dp)
                                )
                            }

                            Row(modifier = Modifier.padding(top = 16.dp)) {
                                IconButton(onClick = {
                                    onDismiss()
                                    editingTask = selectedTask
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit Task",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                IconButton(onClick = {
                                    scope.launch {
                                        databaseInstance.deleteTask(selectedTask)
                                    }
                                    onDismiss()
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete Task",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        } else {
                            Text(
                                "Scheduled Tasks",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            tasks.forEach { task ->
                                Text(
                                    "• ${task.title}",
                                    modifier = Modifier.padding(vertical = 2.dp)
                                )
                            }
                            if (tasks.isEmpty()) Text(
                                "No tasks scheduled",
                                color = Color.Gray
                            )
                            Spacer(Modifier.height(24.dp))
                            IconButton(onClick = {
                                onDismiss()
                                createTaskDialog = true
                            }) {
                                Icon(
                                    imageVector = Icons.Filled.Add,
                                    contentDescription = "Create Task",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
    //val textColoring = MaterialTheme.colorScheme.onSurface
    val textColoring = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.name.take(3),
                color = textColoring,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}


//The Game