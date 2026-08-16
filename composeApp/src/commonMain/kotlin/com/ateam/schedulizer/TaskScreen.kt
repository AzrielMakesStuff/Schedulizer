package com.ateam.schedulizer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarViewWeek
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Share
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.annotation.InternalVoyagerApi
import cafe.adriel.voyager.navigator.internal.BackHandler
import com.ateam.schedulizer.data.BusyBlock
import com.ateam.schedulizer.data.endDateTime
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
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearMonth
import kotlin.time.Clock
import kotlin.time.Duration.Companion.milliseconds

data class TaskOccurrence(
    val task: Task,
    val occurrenceStart: LocalDateTime,
    val occurrenceEnd: LocalDateTime?
)

data class PositionedTask(
    val task: Task,
    val occurrenceStart: LocalDateTime,
    val occurrenceEnd: LocalDateTime?,
    val laneIndex: Int,
    val totalLanes: Int
)

private fun Task.occurrencesInRange(rangeStart: LocalDate, rangeEnd: LocalDate): List<TaskOccurrence> = when (this) {
    is Task.Set -> occurrencesBetween(rangeStart, rangeEnd).map { occStart ->
        // Preserve the original start->end duration for each projected occurrence.
        val occEnd = end.let { oldEnd ->
            val tz = TimeZone.currentSystemDefault()
            val duration = oldEnd.toInstant(tz) - start.toInstant(tz)
            (occStart.toInstant(tz) + duration).toLocalDateTime(tz)
        }
        TaskOccurrence(this, occStart, occEnd)
    }

    is Task.Pool ->
        if (expiry != null && expiry.date in rangeStart..rangeEnd) listOf(TaskOccurrence(this, expiry, expiry))
        else emptyList()

    is Task.Daily -> emptyList()
}

private const val NEAR_INSTANT_MINUTES = 1

private fun TaskOccurrence.isNearInstant(): Boolean {
    val tz = TimeZone.currentSystemDefault()
    val end = occurrenceEnd ?: occurrenceStart
    return (end.toInstant(tz) - occurrenceStart.toInstant(tz)) <= NEAR_INSTANT_MINUTES.let {
        kotlin.time.Duration.parse("${it}m")
    }
}

private fun computeLanes(occurrences: List<TaskOccurrence>): Pair<Map<TaskOccurrence, Int>, Map<TaskOccurrence, Int>> {
    val sorted = occurrences.sortedBy { it.occurrenceStart }
    val lanes = mutableListOf<MutableList<TaskOccurrence>>()
    val laneOf = mutableMapOf<TaskOccurrence, Int>()

    fun TaskOccurrence.spanStart(): LocalDateTime =
        if (isNearInstant()) LocalDateTime(occurrenceStart.date, kotlinx.datetime.LocalTime(0, 0))
        else occurrenceStart

    fun TaskOccurrence.spanEnd(): LocalDateTime =
        if (isNearInstant()) {
            val nextDay = occurrenceStart.date.plus(1, DateTimeUnit.DAY)
            LocalDateTime(nextDay, kotlinx.datetime.LocalTime(0, 0))
        } else occurrenceEnd ?: occurrenceStart

    sorted.forEach { occ ->
        var placed = false
        for (laneIdx in lanes.indices) {
            val conflict = lanes[laneIdx].any { existing ->
                occ.spanStart() < existing.spanEnd() && occ.spanEnd() > existing.spanStart()
            }
            if (!conflict) {
                lanes[laneIdx].add(occ)
                laneOf[occ] = laneIdx
                placed = true
                break
            }
        }
        if (!placed) {
            lanes.add(mutableListOf(occ))
            laneOf[occ] = lanes.lastIndex
        }
    }

    val totalLanesOf = mutableMapOf<TaskOccurrence, Int>()
    sorted.forEach { occ ->
        if (occ in totalLanesOf) return@forEach
        val visited = mutableSetOf<TaskOccurrence>()
        val queue = ArrayDeque<TaskOccurrence>()
        queue.add(occ)
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (!visited.add(current)) continue
            sorted.forEach { other ->
                if (other !in visited &&
                    current.spanStart() < other.spanEnd() && current.spanEnd() > other.spanStart()
                ) {
                    queue.add(other)
                }
            }
        }
        val clusterLanes = (visited.maxOfOrNull { laneOf[it] ?: 0 } ?: 0) + 1
        visited.forEach { totalLanesOf[it] = clusterLanes }
    }

    return Pair(laneOf, totalLanesOf)
}

/** Filters [occurrences] to those visible on [date] (spanning multi-day occurrences included), positioned via [laneOf]/[totalLanesOf]. */
private fun occurrencesForDate(
    occurrences: List<TaskOccurrence>,
    date: LocalDate,
    laneOf: Map<TaskOccurrence, Int>,
    totalLanesOf: Map<TaskOccurrence, Int>
): List<PositionedTask> = occurrences
    .filter { occ ->
        if (occ.occurrenceEnd != null) occ.occurrenceStart.date <= date && occ.occurrenceEnd.date >= date
        else occ.occurrenceStart.date == date
    }
    .map {
        PositionedTask(
            it.task,
            it.occurrenceStart,
            it.occurrenceEnd,
            laneOf[it] ?: 0,
            totalLanesOf[it] ?: 1
        )
    }

private fun busyBlocksForDate(blocks: List<BusyBlock>, date: LocalDate): List<BusyBlock> =
    blocks.filter { it.start.date <= date && it.endDateTime().date >= date }

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

fun now() = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())

@OptIn(InternalVoyagerApi::class)
@Composable
fun TaskScreen() {
    var createTaskDialog by remember { mutableStateOf(false) }
    var shareCalendarDialog by remember { mutableStateOf(false) }
    var importCalendarDialog by remember { mutableStateOf(false) }
    var isCalendarView by remember { mutableStateOf(false) }

    if (createTaskDialog) TaskDialog(onDismiss = { createTaskDialog = false })
    if (shareCalendarDialog) ShareDialog(onDismiss = { shareCalendarDialog = false })
    if (importCalendarDialog) ImportDialog(onDismiss = { importCalendarDialog = false })

    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    var weekAnchor by remember { mutableStateOf(today()) }
    var calendarVisibleMonth by remember { mutableStateOf(today().yearMonth) }

    val visibleMonth by remember(weekAnchor, isCalendarView, calendarVisibleMonth) {
        derivedStateOf {
            if (isCalendarView) calendarVisibleMonth
            else weekDatesFor(weekAnchor, firstDayOfWeek).first().yearMonth
        }
    }

    var selectedDateOne by remember { mutableStateOf<LocalDateTime?>(null) }
    var selectedDateTwo by remember { mutableStateOf<LocalDateTime?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }

    var editingTask by remember { mutableStateOf<Task?>(null) }
    var deletingTask by remember { mutableStateOf<Task?>(null) }

    BackHandler(enabled = editingTask != null || deletingTask != null || createTaskDialog || showCreateDialog || selectedDateOne != null) {
        when {
            editingTask != null -> editingTask = null
            deletingTask != null -> deletingTask = null
            showCreateDialog -> {
                showCreateDialog = false
                selectedDateOne = null
                selectedDateTwo = null
            }
            createTaskDialog -> createTaskDialog = false
            selectedDateOne != null -> selectedDateOne = null
        }
    }

    if (showCreateDialog) {
        TaskDialog(
            initialDate = selectedDateOne!!.date,
            initialHour = selectedDateOne!!.hour,
            initialMinute = selectedDateOne!!.minute,
            initialEndDate = selectedDateTwo?.date,
            initialEndHour = selectedDateTwo?.hour,
            initialEndMinute = selectedDateTwo?.minute,
            onDismiss = {
                showCreateDialog = false
                selectedDateOne = null
                selectedDateTwo = null
            }
        )
    }

    fun onCellTapped(date: LocalDate, hour: Int, minute: Int = 0) {
        val tapped = LocalDateTime(date.year, date.month, date.day, hour, minute)
        if (selectedDateOne == null) selectedDateOne = tapped
        else {
            val dateOne = selectedDateOne!!
            if (tapped >= dateOne) selectedDateTwo = tapped
            else { selectedDateTwo = dateOne; selectedDateOne = tapped }
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
                    is Task.Set -> task.start.date <= today() && (task.end.date) >= today()
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
                        is Task.Set if (task.completed == true || (!task.isCompletable && task.end < now())) -> 1
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

        LaunchedEffect(Unit) { databaseInstance.refresh() }
        LazyColumn(modifier = Modifier.padding(8.dp), listState) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                )
                {
                    MonthName(
                        monthName = visibleMonth.month.name
                            .lowercase()
                            .replaceFirstChar { it.uppercase() },
                        year = visibleMonth.year
                    )
                    IconButton(onClick = { shareCalendarDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.Share,
                            contentDescription = "Share Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { importCalendarDialog = true }) {
                        Icon(
                            imageVector = Icons.Filled.ImportExport,
                            contentDescription = "Import Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(onClick = { isCalendarView = !isCalendarView }) {
                        if(!isCalendarView) Icon(
                            imageVector = Icons.Filled.CalendarMonth,
                            contentDescription = "Switch to Calendar",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        else Icon(
                            imageVector = Icons.Filled.CalendarViewWeek,
                            contentDescription = "Switch to Week View",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }


                Spacer(modifier = Modifier.height(8.dp))
                if (!isCalendarView) {
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
                                onTaskTapped = { task, occurrenceStart ->
                                    weekAnchor = occurrenceStart.date
                                    val index = filteredTasks.indexOf(task)
                                    if (index >= 0) scope.launch {
                                        listState.animateScrollToItem(
                                            index + 1
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
                else
                {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    {
                        Calendar(
                            onVisibleMonthChanged = { calendarVisibleMonth = it }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(5.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tasks",
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = { createTaskDialog = true }
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = "Create Task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            items(filteredTasks) { task ->
                TaskTile(task,
                    onScrollToTop = {
                        weekAnchor = it
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    editTask = {
                        editingTask = task
                    },
                    deleteTask = {
                        deletingTask = task
                    }
                )
            }
        }

        editingTask?.let { taskToEdit ->
            TaskDialog(task = taskToEdit, onDismiss = { editingTask = null })
        }

        deletingTask?.let { taskToDelete ->
            DeleteDialog(taskToDelete) {
                deletingTask = null
            }
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
    onTaskTapped: (task: Task, occurrenceStart: LocalDateTime) -> Unit,
    overlayBusyBlocks: List<BusyBlock> = emptyList()
) {
    val tasks by databaseInstance.tasks.collectAsState(initial = emptyList())
    val hours = remember { (0..23).toList() }
    val dayHeight: Dp = 64.dp
    val headerHeight: Dp = 18.dp
    val labelWidth: Dp = 44.dp
    val visibleHours = 10

    val hScroll = rememberScrollState()
    val density = LocalDensity.current

    val rangeStart = visibleDates.first()
    val rangeEnd = visibleDates.last()

    // Project every task (repeating Set tasks included) into concrete occurrences
    // within the visible week, then lay THOSE out — not the raw tasks — so a
    // repeating task gets an independently-positioned pill per occurrence.
    val occurrences = remember(tasks, rangeStart, rangeEnd) {
        tasks.flatMap { it.occurrencesInRange(rangeStart, rangeEnd) }
    }
    val (laneOf, totalLanesOf) = remember(occurrences) { computeLanes(occurrences) }

    // Precompute the per-date occurrence lists once per (occurrences, dates) change
    // instead of re-filtering + re-mapping the whole occurrence list on every
    // recomposition of every day row (this was previously called inline per day).
    val dayTasksByDate = remember(occurrences, visibleDates, laneOf, totalLanesOf) {
        visibleDates.associateWith { d -> occurrencesForDate(occurrences, d, laneOf, totalLanesOf) }
    }

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
                    val dayTasks = dayTasksByDate[date] ?: emptyList()

                    Box(
                        modifier = Modifier
                            .width(totalWidth)
                            .height(dayHeight)
                    ) {
                        // Tappable cells — previously 48 Box composables per day (336 for
                        // the whole week), each carrying its own clickable/background/
                        // drawWithContent. That's what made scrolling this row back into
                        // view expensive. A single Canvas draws every hour/half-hour line
                        // and selection highlight in one pass, and one pointerInput resolves
                        // taps by math instead of 48 separate click targets.
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .pointerInput(date, hourWidth) {
                                    detectTapGestures { offset ->
                                        val hourWidthPx = hourWidth.toPx()
                                        val hour = (offset.x / hourWidthPx).toInt().coerceIn(0, 23)
                                        val withinHour = offset.x - hour * hourWidthPx
                                        val minute = if (withinHour < hourWidthPx / 2) 0 else 30
                                        onCellTapped(date, hour, minute)
                                    }
                                }
                        ) {
                            val hourWidthPx = hourWidth.toPx()
                            val borderColor = Color.Gray.copy(alpha = 0.15f)
                            val dotColor = Color.Gray.copy(alpha = 0.25f)
                            val strokeThin = 0.5.dp.toPx()
                            val dotSize = 2.dp.toPx()
                            val gap = 3.dp.toPx()

                            for (hour in 0 until 24) {
                                val xHour = hourWidthPx * hour
                                val xHalf = xHour + hourWidthPx / 2

                                // Selection highlight for whichever half is pending
                                if (selectedDateOne != null && selectedDateOne.date == date && selectedDateOne.hour == hour) {
                                    val left = if (selectedDateOne.minute == 0) xHour else xHalf
                                    drawRect(
                                        color = primaryColor.copy(alpha = 0.12f),
                                        topLeft = Offset(left, 0f),
                                        size = Size(hourWidthPx / 2, size.height)
                                    )
                                }

                                // Dotted half-hour divider
                                var y = 0f
                                while (y < size.height) {
                                    drawLine(
                                        color = dotColor,
                                        start = Offset(xHalf, y),
                                        end = Offset(xHalf, (y + dotSize).coerceAtMost(size.height)),
                                        strokeWidth = strokeThin
                                    )
                                    y += dotSize + gap
                                }

                                // Solid hour border on the right edge of this hour
                                drawLine(
                                    color = borderColor,
                                    start = Offset(xHour + hourWidthPx, 0f),
                                    end = Offset(xHour + hourWidthPx, size.height),
                                    strokeWidth = strokeThin
                                )
                            }

                            // Bottom border across the full row
                            drawLine(
                                color = borderColor,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = strokeThin
                            )
                        }

                        if (date == today) {
                            var now by remember {
                                mutableStateOf(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()))
                            }
                            LaunchedEffect(Unit) {
                                while (true) {
                                    now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                                    kotlinx.coroutines.delay(60_000.milliseconds)
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

                        // Task pills — one per occurrence, not one per task
                        dayTasks.forEach { positioned ->
                            val task = positioned.task
                            val occStart = positioned.occurrenceStart
                            val occEnd = positioned.occurrenceEnd
                            val startHour = if (occStart.date < date) 0 else occStart.hour
                            val startMinute = if (occStart.date < date) 0 else occStart.minute
                            val endHour = when {
                                (occEnd != null) && (occEnd.date > date) -> 24
                                occEnd != null -> occEnd.hour
                                else -> startHour + 1
                            }
                            val endMinute = when {
                                (occEnd != null) && (occEnd.date > date) -> 0
                                occEnd != null -> occEnd.minute
                                else -> 0
                            }
                            val startFraction = startHour + startMinute / 60f
                            val endFraction = endHour + endMinute / 60f
                            val isNearInstant = positioned.let {
                                val tz = TimeZone.currentSystemDefault()
                                val end = it.occurrenceEnd ?: it.occurrenceStart
                                (end.toInstant(tz) - it.occurrenceStart.toInstant(tz)) <= kotlin.time.Duration.parse("1m")
                            }
                            val pillStartFraction: Float
                            val pillEndFraction: Float
                            if (isNearInstant) {
                                pillStartFraction = 0f
                                pillEndFraction = 24f
                            } else {
                                val spanHours = (endFraction - startFraction).coerceAtLeast(0.5f)
                                pillStartFraction = startFraction
                                pillEndFraction = (startFraction + spanHours).coerceAtMost(24f)
                            }
                            val pillHeight = 18.dp
                            val pillSpacing = 2.dp
                            val laneTop = (pillHeight + pillSpacing) * positioned.laneIndex
                            val pillPaddingPx = with(density) { 2.dp.toPx() }
                            val pillStartPx = with(density) { (hourWidth * pillStartFraction).toPx() } + pillPaddingPx
                            val pillWidthPx = with(density) { (hourWidth * (pillEndFraction - pillStartFraction)).toPx() } - pillPaddingPx * 2
                            val scrollOffset = hScroll.value.toFloat()
                            val visibleLeft = (scrollOffset - pillStartPx).coerceAtLeast(0f)
                            val maxTextOffset = (pillWidthPx - with(density) { 11.dp.toPx() }).coerceAtLeast(0f)
                            val textOffsetPx = visibleLeft.coerceAtMost(maxTextOffset)
                            val nowMoment = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                            val isCurrentOccurrence = (task as? Task.Set)?.start == occStart
                            val pillColor = when {
                                task is Task.Set && (task.completed == true || (!task.isCompletable && task.end < now())) && isCurrentOccurrence ->
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                                task is Task.Pool && task.completed ->
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)

                                occEnd != null && occEnd < nowMoment ->
                                    MaterialTheme.colorScheme.error

                                else -> primaryColor
                            }

                            Box(
                                modifier = Modifier
                                    .offset(x = hourWidth * pillStartFraction, y = laneTop)
                                    .width(hourWidth * (pillEndFraction - pillStartFraction))
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
                                    .clickable { onTaskTapped(task, occStart) }
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

                        val overlayColor = MaterialTheme.colorScheme.tertiary
                        busyBlocksForDate(overlayBusyBlocks, date).forEach { block ->
                            val blockEnd = block.endDateTime()

                            val startHour = if (block.start.date < date) 0 else block.start.hour
                            val startMinute = if (block.start.date < date) 0 else block.start.minute
                            val endHour = if (blockEnd.date > date) 24 else blockEnd.hour + (if (blockEnd.minute > 0) 1 else 0)
                            val endMinute = if (blockEnd.date > date) 0 else blockEnd.minute

                            val startFraction = startHour + startMinute / 60f
                            val endFraction = (endHour + endMinute / 60f).coerceAtLeast(startFraction + 0.25f)
                            val spanHours = (endFraction - startFraction).coerceAtLeast(0.5f)

                            Box(
                                modifier = Modifier
                                    .offset(x = hourWidth * startFraction, y = 0.dp)
                                    .width(hourWidth * spanHours)
                                    .fillMaxHeight()
                                    .padding(vertical = 2.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(overlayColor.copy(alpha = 0.14f))
                                    .drawWithContent {
                                        drawContent()
                                        val step = 6.dp.toPx()
                                        var x = -size.height
                                        while (x < size.width) {
                                            drawLine(
                                                color = overlayColor.copy(alpha = 0.35f),
                                                start = Offset(x, size.height),
                                                end = Offset(x + size.height, 0f),
                                                strokeWidth = 1.dp.toPx()
                                            )
                                            x += step
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        }
    }
}