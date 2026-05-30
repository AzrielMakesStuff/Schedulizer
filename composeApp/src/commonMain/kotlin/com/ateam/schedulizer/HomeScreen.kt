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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.setValue
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
import com.kizitonwose.calendar.core.now
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
    val sorted = tasks.sortedBy { it.dateOne }
    val lanes = mutableListOf<MutableList<Task>>()
    val laneOf = mutableMapOf<Task, Int>()

    sorted.forEach { task ->
        val tStart = task.dateOne
        val tEnd = task.dateTwo ?: task.dateOne
        var placed = false
        for (laneIdx in lanes.indices) {
            val conflict = lanes[laneIdx].any { existing ->
                val eStart = existing.dateOne
                val eEnd = existing.dateTwo ?: existing.dateOne
                tStart < eEnd && tEnd > eStart
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
            val cStart = current.dateOne
            val cEnd = current.dateTwo ?: current.dateOne
            sorted.forEach { other ->
                if (other !in visited) {
                    val oStart = other.dateOne
                    val oEnd = other.dateTwo ?: other.dateOne
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
        if (task.dateTwo != null) task.dateOne.date <= date && task.dateTwo.date >= date
        else task.dateOne.date == date
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

@Composable
fun HomeScreen() {
    val today = remember { LocalDate.now() }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    var weekAnchor by remember { mutableStateOf(today) }
    val visibleMonth by remember(weekAnchor) {
        derivedStateOf { weekDatesFor(weekAnchor, firstDayOfWeek).first().yearMonth }
    }

    var selectedDateOne by remember { mutableStateOf<LocalDateTime?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var pendingDateTwo by remember { mutableStateOf<LocalDateTime?>(null) }

    if (showCreateDialog) {
        CreateTaskDialog(
            dateOne = selectedDateOne!!.date,
            hourOne = selectedDateOne!!.hour,
            dateTwo = pendingDateTwo?.date,
            hourTwo = pendingDateTwo?.hour
        ) {
            showCreateDialog = false
            selectedDateOne = null
            pendingDateTwo = null
        }
    }

    fun onCellTapped(date: LocalDate, hour: Int) {
        val tapped = LocalDateTime(date.year, date.month, date.day, hour, 0)
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
        Column(modifier = Modifier.padding(8.dp)) {

            Column(
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth()
            ) {
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
                            today = today,
                            selectedDateOne = selectedDateOne,
                            onCellTapped = ::onCellTapped
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(25.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Tasks",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 30.dp)
                ) {
                    LazyListTasks()
                }
            }
        }
    }
}

@Composable
fun WeeklyScheduleGrid(
    visibleDates: List<LocalDate>,
    today: LocalDate,
    selectedDateOne: LocalDateTime?,
    onCellTapped: (LocalDate, Int) -> Unit
) {
    val tasks by databaseInstance.tasks.collectAsState(initial = emptyList())
    val hours = remember { (0..23).toList() }
    val dayHeight: Dp = 52.dp
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
            val currentHour = Clock.System.now()
                .toLocalDateTime(TimeZone.currentSystemDefault()).hour
            val targetPx = with(density) {
                (hourWidth * maxOf(0, currentHour - 1)).toPx().toInt()
            }
            hScroll.scrollTo(targetPx)
        }

        Row(modifier = Modifier.fillMaxWidth()) {
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

            Column(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(hScroll)
            ) {
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

                visibleDates.forEach { date ->
                    val primaryColor = MaterialTheme.colorScheme.primary
                    val dayTasks = tasksForDate(tasks, date, laneOf, totalLanesOf)

                    Box(
                        modifier = Modifier
                            .width(totalWidth)
                            .height(dayHeight)
                    ) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            hours.forEach { hour ->
                                val isPendingStart = selectedDateOne?.let {
                                    it.date == date && it.hour == hour
                                } ?: false

                                Box(
                                    modifier = Modifier
                                        .width(hourWidth)
                                        .fillMaxHeight()
                                        .clickable { onCellTapped(date, hour) }
                                        .background(
                                            if (isPendingStart)
                                                primaryColor.copy(alpha = 0.12f)
                                            else Color.Transparent
                                        )
                                        .drawWithContent {
                                            drawContent()
                                            drawLine(
                                                color = Color.Gray.copy(alpha = 0.15f),
                                                start = Offset(size.width, 0f),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 0.5.dp.toPx()
                                            )
                                            drawLine(
                                                color = Color.Gray.copy(alpha = 0.15f),
                                                start = Offset(0f, size.height),
                                                end = Offset(size.width, size.height),
                                                strokeWidth = 0.5.dp.toPx()
                                            )
                                        }
                                )
                            }
                        }

                        dayTasks.forEach { positioned ->
                            val task = positioned.task

                            val startHour = if (task.dateOne.date < date) 0
                            else task.dateOne.hour
                            val endHour = if (task.dateTwo != null && task.dateTwo.date > date) 23
                            else (task.dateTwo?.hour ?: task.dateOne.hour)
                            val spanHours = (endHour - startHour + 1).coerceAtLeast(1)

                            val laneH = dayHeight / positioned.totalLanes
                            val laneTop = laneH * positioned.laneIndex

                            Box(
                                modifier = Modifier
                                    .offset(
                                        x = hourWidth * startHour,
                                        y = laneTop
                                    )
                                    .width(hourWidth * spanHours)
                                    .height(laneH)
                                    .padding(horizontal = 2.dp, vertical = 2.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(primaryColor.copy(alpha = 0.15f))
                                    .drawWithContent {
                                        drawContent()
                                        drawLine(
                                            color = primaryColor,
                                            start = Offset(0f, 0f),
                                            end = Offset(0f, size.height),
                                            strokeWidth = 3.dp.toPx()
                                        )
                                    }
                                    .padding(start = 6.dp, top = 2.dp, end = 4.dp)
                            ) {
                                Text(
                                    text = task.title,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    lineHeight = 11.sp,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    color = primaryColor,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}