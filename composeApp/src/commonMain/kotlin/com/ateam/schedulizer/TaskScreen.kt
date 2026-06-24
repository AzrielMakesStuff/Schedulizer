package com.ateam.schedulizer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import kotlin.time.Clock

data class PositionedTask(
    val task: Task,
    val laneIndex: Int,
    val totalLanes: Int
)

private fun computeLanes(tasks: List<Task>): Pair<Map<Task, Int>, Map<Task, Int>> {
    val sorted = tasks.sortedBy { task ->
        when (task) {
            is Task.Set -> task.start
            is Task.Pool -> task.expiry
            is Task.Daily -> null
        }
    }
    val lanes = mutableListOf<MutableList<Task>>()
    val laneOf = mutableMapOf<Task, Int>()

    fun Task.tStart(): LocalDateTime? {
        return when (this) {
            is Task.Set -> start
            is Task.Pool -> expiry ?: return null
            is Task.Daily -> return null
        }
    }
    fun Task.tEnd(): LocalDateTime? {
        return when (this) {
            is Task.Set -> end ?: start
            is Task.Pool -> expiry ?: return null
            is Task.Daily -> return null
        }
    }

    sorted.forEach { task ->
        val tStart = task.tStart()
        val tEnd = task.tEnd()
        var placed = false
        for (laneIdx in lanes.indices) {
            val conflict = lanes[laneIdx].any { existing ->
                val eStart = existing.tStart() ?: return@any false
                val eEnd = existing.tEnd() ?: return@any false
                tStart!! < eEnd && tEnd!! > eStart
            }
            if (!conflict) {
                lanes[laneIdx].add(task)
                laneOf[task] = laneIdx
                placed = true
                break
            }
        }
        if (!placed) {
            lanes.add(mutableListOf(task))
            laneOf[task] = lanes.lastIndex
        }
    }

    val totalLanesOf = mutableMapOf<Task, Int>()
    sorted.forEach { task ->
        if (task in totalLanesOf) return@forEach
        val visited = mutableSetOf<Task>()
        val queue = ArrayDeque<Task>()
        queue.add(task)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            val cStart = current.tStart() ?: continue
            val cEnd = current.tEnd() ?: continue
            sorted.forEach { other ->
                if (other !in visited) {
                    val oStart = other.tStart() ?: return@forEach
                    val oEnd = other.tEnd() ?: return@forEach
                    if (cStart < oEnd && cEnd > oStart) queue.add(other)
                }
            }
        }
        val clusterLanes = (visited.maxOfOrNull { laneOf[it] ?: 0 } ?: 0) + 1
        visited.forEach { totalLanesOf[it] = clusterLanes }
    }

    return Pair(laneOf, totalLanesOf)
}

private fun tasksForDate(
    tasks: List<Task>,
    date: LocalDate,
    laneOf: Map<Task, Int>,
    totalLanesOf: Map<Task, Int>
): List<PositionedTask> = tasks
    .filter { task ->
        when (task) {
            is Task.Daily -> false
            is Task.Pool -> task.expiry != null && task.expiry.date == date
            is Task.Set ->
                if (task.end != null) task.start.date <= date && task.end.date >= date
                else task.start.date == date
        }
    }
    .map { PositionedTask(it, laneOf[it] ?: 0, totalLanesOf[it] ?: 1) }

private fun weekDatesFor(
    anchor: LocalDate,
    firstDayOfWeek: kotlinx.datetime.DayOfWeek
): List<LocalDate> {
    val diff = (anchor.dayOfWeek.ordinal - firstDayOfWeek.ordinal + 7) % 7
    val start = anchor.minus(diff, DateTimeUnit.DAY)
    return (0..6).map { start.plus(it, DateTimeUnit.DAY) }
}

fun LocalDateTime.fmt(): String {
    val h = hour.toString().padStart(2, '0')
    val m = minute.toString().padStart(2, '0')
    return "${dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} " +
            "${date.day} ${month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} " +
            "$year $h:$m"
}

fun LocalDate.fmt(): String =
    "${dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} " +
            "$day ${month.name.lowercase().replaceFirstChar { it.uppercase() }.take(3)} $year"

fun now() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

@Composable
fun TaskScreen() {
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    var weekAnchor by remember { mutableStateOf(today()) }
    val visibleMonth by remember(weekAnchor) {
        derivedStateOf { weekDatesFor(weekAnchor, firstDayOfWeek).first().yearMonth }
    }

    var selectedDateOne by remember { mutableStateOf<LocalDateTime?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDateTwo by remember { mutableStateOf<LocalDateTime?>(null) }

    if (showCreateDialog) {
        TaskDialog(
            initialDate = selectedDateOne!!.date,
            initialHour = selectedDateOne!!.hour,
            initialMinute = selectedDateOne!!.minute,
            initialEndDate = pendingDateTwo?.date,
            initialEndHour = pendingDateTwo?.hour,
            initialEndMinute = pendingDateTwo?.minute,
            onDismiss = {
                showCreateDialog = false
                selectedDateOne = null
                pendingDateTwo = null
            }
        )
    }

    fun onCellTapped(date: LocalDate, hour: Int, minute: Int = 0) {
        val tapped = LocalDateTime(date.year, date.month, date.day, hour, minute)
        if (selectedDateOne == null) {
            selectedDateOne = tapped
        } else {
            val dateOne = selectedDateOne!!
            if (tapped >= dateOne) pendingDateTwo = tapped
            else { pendingDateTwo = dateOne; selectedDateOne = tapped }
            showCreateDialog = true
        }
    }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var weekChangedThisDrag by remember { mutableStateOf(false) }

    Box {
        val tasks by databaseInstance.tasks.collectAsState()
        val filteredTasks = remember(tasks) {
            tasks.filter { task ->
                when (task) {
                    is Task.Daily -> true
                    is Task.Set -> {
                        if (task.completed) {
                            task.completionDate?.date?.let { it >= today() } ?: false
                        } else true
                    }
                    is Task.Pool -> {
                        if (task.completed) {
                            task.completionDate?.date?.let { it >= today() } ?: false
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
        }
        val listState = rememberLazyListState()
        val scope = rememberCoroutineScope()

        var editingTask by remember { mutableStateOf<Task?>(null) }

        LaunchedEffect(Unit) { databaseInstance.refresh() }
        LazyColumn(modifier = Modifier.padding(8.dp), listState) {
            item {
                MonthName(
                    monthName = visibleMonth.month.name
                        .lowercase()
                        .replaceFirstChar { it.uppercase() },
                    year = visibleMonth.year
                )

                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .pointerInput(Unit) {
                            detectVerticalDragGestures(
                                onDragEnd = {
                                    dragAccumulator = 0f
                                    weekChangedThisDrag = false
                                },
                                onDragCancel = {
                                    dragAccumulator = 0f
                                    weekChangedThisDrag = false
                                },
                                onVerticalDrag = { _, dragAmount ->
                                    if (weekChangedThisDrag) return@detectVerticalDragGestures
                                    dragAccumulator += dragAmount
                                    if (dragAccumulator < -300f) {
                                        weekAnchor = weekAnchor.plus(7, DateTimeUnit.DAY)
                                        weekChangedThisDrag = true
                                    } else if (dragAccumulator > 300f) {
                                        weekAnchor = weekAnchor.minus(7, DateTimeUnit.DAY)
                                        weekChangedThisDrag = true
                                    }
                                }
                            )
                        }
                ) {
                    AnimatedContent(
                        targetState = weekAnchor,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInVertically { it } + fadeIn()) togetherWith
                                        (slideOutVertically { -it } + fadeOut())
                            } else {
                                (slideInVertically { -it } + fadeIn()) togetherWith
                                        (slideOutVertically { it } + fadeOut())
                            }
                        },
                        label = "weekTransition"
                    ) { anchor ->
                        val dates = remember(anchor) { weekDatesFor(anchor, firstDayOfWeek) }
                        WeeklyScheduleGrid(
                            visibleDates = dates,
                            now = now(),
                            today = today(),
                            selectedDateOne = selectedDateOne,
                            onCellTapped = ::onCellTapped,
                            onTaskTapped = { task ->
                                val date = when (task) {
                                    is Task.Set -> task.start.date
                                    is Task.Pool -> task.expiry?.date ?: today()
                                    is Task.Daily -> today()
                                }
                                weekAnchor = date
                                val index = filteredTasks.indexOf(task)
                                if (index >= 0) scope.launch { listState.animateScrollToItem(index + 1) }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tasks",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
            }
            items(filteredTasks) { task ->
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
                        .clickable {
                            if (task is Task.Set) {
                                weekAnchor = task.start.date
                                scope.launch { listState.animateScrollToItem(0) }
                            }
                        }
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

                                    is Task.Set -> task.copy(
                                        completed = checked,
                                        completionDate = if (checked) now() else null
                                    )

                                    is Task.Pool -> task.copy(
                                        completed = checked,
                                        completionDate = if (checked) now() else null
                                    )
                                }
                                databaseInstance.createTask(updated)
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

                            is Task.Daily -> {
                                Text("Daily task", fontSize = 11.sp, color = subtleColor)
                            }
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

                    IconButton(onClick = { editingTask = task }, enabled = !isChecked) {
                        Icon(
                            Icons.Outlined.Edit, contentDescription = "Edit Task",
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isChecked) 0.3f else 0.8f)
                        )
                    }
                    IconButton(onClick = { scope.launch { databaseInstance.deleteTask(task) } }) {
                        Icon(
                            Icons.Outlined.Delete, contentDescription = "Delete Task",
                            tint = MaterialTheme.colorScheme.error.copy(alpha = alpha)
                        )
                    }
                }
            }
        }

        editingTask?.let { taskToEdit ->
            TaskDialog(task = taskToEdit, onDismiss = { editingTask = null })
        }
    }
}

@Suppress("FrequentlyChangingValue")
@Composable
fun WeeklyScheduleGrid(
    visibleDates: List<LocalDate>,
    now: LocalDateTime,
    today: LocalDate,
    selectedDateOne: LocalDateTime?,
    onCellTapped: (LocalDate, Int, Int) -> Unit,
    onTaskTapped: (task: Task) -> Unit
) {
    val tasks by databaseInstance.tasks.collectAsState(initial = emptyList())
    val hours = remember { (0..23).toList() }
    val dayHeight: Dp = 64.dp
    val headerHeight: Dp = 18.dp
    val labelWidth: Dp = 44.dp
    val visibleHours = 10

    val hScroll = rememberScrollState()
    val density = LocalDensity.current

    val (laneOf, totalLanesOf) = remember(tasks) { computeLanes(tasks) }

    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val hourWidth = (maxWidth - labelWidth) / visibleHours
        val totalWidth = hourWidth * 24

        LaunchedEffect(hourWidth) {
            snapshotFlow { hScroll.maxValue }.first { it > 0 }
            val currentFraction = now.hour + now.minute / 60f
            val density = density
            val targetPx = with(density) { (hourWidth * currentFraction).toPx() }
            val halfVisiblePx = with(density) { (hourWidth * visibleHours / 2f).toPx() }
            hScroll.scrollTo((targetPx - halfVisiblePx).toInt().coerceIn(0, hScroll.maxValue))
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            // Day label column
            Column(modifier = Modifier.width(labelWidth)) {
                Spacer(modifier = Modifier.height(headerHeight))
                visibleDates.forEach { date ->
                    val isToday = date == today
                    Box(
                        modifier = Modifier
                            .width(labelWidth)
                            .height(dayHeight)
                            .background(
                                if (isToday) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                                else Color.Transparent
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = date.dayOfWeek.name.take(3),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = date.day.toString(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isToday) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            // Scrollable grid
            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
                // Hour header row
                Row(modifier = Modifier.width(totalWidth).height(headerHeight)) {
                    hours.forEach { hour ->
                        Box(
                            modifier = Modifier.width(hourWidth).fillMaxHeight(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${hour.toString().padStart(2, '0')}:00",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Light,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Day rows
                visibleDates.forEach { date ->
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val dayTasks = tasksForDate(tasks, date, laneOf, totalLanesOf)

                    Box(
                        modifier = Modifier
                            .width(totalWidth)
                            .height(dayHeight)
                    ) {
                        // Tappable cells — each hour split into two half-hour halves
                        Row(modifier = Modifier.fillMaxSize()) {
                            hours.forEach { hour ->
                                val isHourPending = selectedDateOne?.let {
                                    it.date == date && it.hour == hour && it.minute == 0
                                } ?: false
                                val isHalfPending = selectedDateOne?.let {
                                    it.date == date && it.hour == hour && it.minute == 30
                                } ?: false

                                // :00 half
                                Box(
                                    modifier = Modifier
                                        .width(hourWidth / 2)
                                        .fillMaxHeight()
                                        .clickable { onCellTapped(date, hour, 0) }
                                        .background(
                                            if (isHourPending) primaryColor.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .drawWithContent {
                                            drawContent()
                                            // Bottom border
                                            drawLine(
                                                color = Color.Gray.copy(alpha = 0.15f),
                                                start = Offset(0f, size.height),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 0.5.dp.toPx()
                                            )
                                        }
                                )

                                // :30 half — dotted left edge + solid right edge
                                Box(
                                    modifier = Modifier
                                        .width(hourWidth / 2)
                                        .fillMaxHeight()
                                        .clickable { onCellTapped(date, hour, 30) }
                                        .background(
                                            if (isHalfPending) primaryColor.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .drawWithContent {
                                            drawContent()
                                            // Dotted half-hour divider on left edge
                                            val dotSize = 2.dp.toPx()
                                            val gap = 3.dp.toPx()
                                            var y = 0f
                                            while (y < size.height) {
                                                drawLine(
                                                    color = Color.Gray.copy(alpha = 0.25f),
                                                    start = Offset(0f, y),
                                                    end = Offset(0f, (y + dotSize).coerceAtMost(size.height)),
                                                    strokeWidth = 0.5.dp.toPx()
                                                )
                                                y += dotSize + gap
                                            }
                                            // Bottom border
                                            drawLine(
                                                color = Color.Gray.copy(alpha = 0.15f),
                                                start = Offset(0f, size.height),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 0.5.dp.toPx()
                                            )
                                            // Solid hour border on right
                                            drawLine(
                                                color = Color.Gray.copy(alpha = 0.15f),
                                                start = Offset(size.width, 0f),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 0.5.dp.toPx()
                                            )
                                        }
                                )
                            }
                        }

                        if (date == today) {
                            var now by remember {
                                mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
                            }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                    kotlinx.coroutines.delay(60_000)
                                }
                            }

                            val currentFraction = now.hour + now.minute / 60f
                            val lineX = hourWidth * currentFraction

                            Box(
                                modifier = Modifier
                                    .offset(x = lineX)
                                    .width(2.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.error)
                            )
                        }

                        // Task pills
                        dayTasks.forEach { positioned ->
                            val task = positioned.task

                            val startHour = when (task) {
                                is Task.Pool -> task.expiry!!.hour
                                is Task.Set -> if (task.start.date < date) 0 else task.start.hour
                                is Task.Daily -> 0
                            }
                            val startMinute = when (task) {
                                is Task.Pool -> task.expiry!!.minute
                                is Task.Set -> if (task.start.date < date) 0 else task.start.minute
                                is Task.Daily -> 0
                            }
                            val endHour = when (task) {
                                is Task.Pool -> task.expiry!!.hour + 1
                                is Task.Set -> if (task.end != null && task.end.date > date) 24
                                else (task.end?.hour?.plus(1) ?: (task.start.hour + 1))
                                is Task.Daily -> 0
                            }
                            val endMinute = when (task) {
                                is Task.Pool -> task.expiry!!.minute
                                is Task.Set -> if (task.end != null && task.end.date > date) 0
                                else (task.end?.minute ?: 0)
                                is Task.Daily -> 0
                            }

                            val startFraction = startHour + startMinute / 60f
                            val endFraction = endHour + endMinute / 60f
                            val spanHours = (endFraction - startFraction).coerceAtLeast(0.5f)

                            val pillHeight = 18.dp
                            val pillSpacing = 2.dp
                            val laneTop = (pillHeight + pillSpacing) * positioned.laneIndex

                            val pillPaddingPx = with(density) { 2.dp.toPx() }
                            val pillStartPx = with(density) { (hourWidth * startFraction).toPx() } + pillPaddingPx
                            val pillWidthPx = with(density) { (hourWidth * spanHours).toPx() } - pillPaddingPx * 2
                            val scrollOffset = hScroll.value.toFloat()

                            val visibleLeft = (scrollOffset - pillStartPx).coerceAtLeast(0f)
                            val maxTextOffset = (pillWidthPx - with(density) { 11.dp.toPx() }).coerceAtLeast(0f)
                            val textOffsetPx = visibleLeft.coerceAtMost(maxTextOffset)

                            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

                            val pillColor = when (task) {
                                is Task.Set if task.completed ->
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                                is Task.Set if task.end != null && task.end < now ->
                                    MaterialTheme.colorScheme.error

                                is Task.Pool if task.expiry != null && task.expiry < now ->
                                    MaterialTheme.colorScheme.error

                                else -> primaryColor
                            }

                            Box(
                                modifier = Modifier
                                    .offset(x = hourWidth * startFraction, y = laneTop)
                                    .width(hourWidth * spanHours)
                                    .height(pillHeight)
                                    .padding(horizontal = 2.dp)
                                    .clip(RoundedCornerShape(15))
                                    .background(pillColor.copy(alpha = 0.18f))
                                    .drawWithContent {
                                        drawContent()
                                        drawLine(
                                            color = pillColor,
                                            start = Offset(0f, 4.dp.toPx()),
                                            end = Offset(0f, size.height - 4.dp.toPx()),
                                            strokeWidth = 3.dp.toPx(),
                                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                                        )
                                    }
                                    .clickable { onTaskTapped(task) }
                            ) {
                                Text(
                                    text = task.title,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = pillColor,
                                    modifier = Modifier
                                        .align(Alignment.CenterStart)
                                        .offset(x = with(density) { textOffsetPx.toDp() } + 7.dp)
                                        .padding(end = 4.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}