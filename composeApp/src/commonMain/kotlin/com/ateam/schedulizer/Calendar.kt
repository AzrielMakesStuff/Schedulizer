package com.ateam.schedulizer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
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
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock.System

val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.THURSDAY)
fun today() = System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

private fun formatTime(time: LocalDateTime?) = time?.time?.toString()?.take(5) ?: ""

private data class DatedOccurrence(
    val task: Task,
    val start: LocalDate,
    val end: LocalDate
)

private fun Task.occurrencesInRange(rangeStart: LocalDate, rangeEnd: LocalDate): List<DatedOccurrence> = when (this) {
    is Task.Set -> {
        val spanDays = end.date.toEpochDays() - start.date.toEpochDays()
        occurrencesBetween(rangeStart, rangeEnd).map { occStart ->
            val occStartDate = occStart.date
            val occEndDate = occStartDate.plus(spanDays.toInt(), DateTimeUnit.DAY)
            DatedOccurrence(this, occStartDate, occEndDate)
        }
    }

    is Task.Pool -> {
        val d = expiry?.date
        if (d != null && d in rangeStart..rangeEnd) listOf(DatedOccurrence(this, d, d)) else emptyList()
    }

    is Task.Daily -> emptyList()
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Calendar(onVisibleMonthChanged: (YearMonth) -> Unit = {}) {
    val tasks by databaseInstance.tasks.collectAsState(initial = emptyList())
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    val daysOfWeek = remember(firstDayOfWeek) { daysOfWeek(firstDayOfWeek = firstDayOfWeek)}

    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }

    // Same [startMonth, endMonth] bounds the calendar itself scrolls across, expressed
    // as plain dates so repeating tasks can be projected across the whole scrollable
    // range once (occurrencesAfter bails out as soon as it passes rangeEnd, so this
    // stays bounded even for tasks that repeat forever).
    val rangeStart = remember(startMonth) { LocalDate(startMonth.year, startMonth.month, 1) }
    val rangeEnd = remember(endMonth) {
        LocalDate(endMonth.year, endMonth.month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY)
    }

    val occurrences = remember(tasks, rangeStart, rangeEnd) {
        tasks.flatMap { it.occurrencesInRange(rangeStart, rangeEnd) }
    }

    val laneOf = remember(occurrences) {
        val sorted = occurrences.sortedBy { it.start }
        val lanes = mutableListOf<MutableList<DatedOccurrence>>()
        val laneMap = mutableMapOf<DatedOccurrence, Int>()

        sorted.forEach { occ ->
            var placed = false
            for (laneIdx in lanes.indices) {
                val conflict = lanes[laneIdx].any { existing ->
                    occ.start <= existing.end && occ.end >= existing.start
                }
                if (!conflict) {
                    lanes[laneIdx].add(occ)
                    laneMap[occ] = laneIdx
                    placed = true
                    break
                }
            }
            if (!placed) {
                lanes.add(mutableListOf(occ))
                laneMap[occ] = lanes.lastIndex
            }
        }
        laneMap
    }

    val occurrencesByDate = remember(occurrences, laneOf) {
        buildMap(occurrences.size) {
            occurrences.forEach { occ ->
                val lane = laneOf[occ] ?: return@forEach

                var current = occ.start

                while (current <= occ.end) {
                    val lanes = get(current) ?: arrayOfNulls(3)

                    if (lane < 3) {
                        lanes[lane] = occ
                    }

                    put(current, lanes)

                    current = current.plus(1, DateTimeUnit.DAY)
                }
            }
        }
    }

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first(),
        outDateStyle = OutDateStyle.EndOfGrid
    )

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleMonth }
            .collect { month -> onVisibleMonthChanged(month.yearMonth) }
    }

    HorizontalCalendar(
        modifier = Modifier.fillMaxWidth(),
        state = state,
        dayContent = {
            Day(
                day = it,
                occurrences = occurrencesByDate[it.date] ?: arrayOfNulls(3),
                firstDayOfWeek = daysOfWeek.first()
            )
        },
        monthHeader = {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                MonthHeader(daysOfWeek = daysOfWeek)
                Spacer(Modifier.height(8.dp))
            }
        }
    )
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
fun MonthName(monthName: String, year: Int, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        textAlign = TextAlign.Center,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        text = ("$monthName $year")
    )
}

@Composable
private fun Day(
    day: CalendarDay,
    occurrences: Array<DatedOccurrence?>,
    firstDayOfWeek: DayOfWeek
) {
    val isMonthDate = day.position == DayPosition.MonthDate
    val baseColor = MaterialTheme.colorScheme.onSurface
    val textColor = if (isMonthDate) baseColor else baseColor.copy(alpha = 0.3f)

    val isToday = day.date == today()
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    val textMeasurer = rememberTextMeasurer()

    var showOverlay by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    val now = remember { now() }

    val density = androidx.compose.ui.platform.LocalDensity.current

    val dayNumberTop = 4.dp
    val taskTop = 24.dp
    val laneHeight = 18.dp

    val dayNumberTopPx = with(density) { dayNumberTop.toPx() }
    val taskTopPx = with(density) { taskTop.toPx() }
    val laneHeightPx = with(density) { laneHeight.toPx() }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(0.75f)
            .pointerInput(isMonthDate, occurrences) {
                if (!isMonthDate) return@pointerInput

                detectTapGestures { offset ->
                    val relativeY = offset.y - taskTopPx

                    val lane = if (relativeY >= 0f) {
                        (relativeY / laneHeightPx).toInt()
                    } else {
                        -1
                    }

                    if (lane in 0..2) {
                        val task = occurrences[lane]?.task

                        if (task != null) {
                            selectedTask = task
                            showOverlay = true
                            return@detectTapGestures
                        }
                    }

                    selectedTask = null
                    showOverlay = true
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            if (isToday && isMonthDate) {
                drawRect(
                    color = primaryColor.copy(alpha = 0.15f),
                    size = size
                )
            }

            drawRect(
                color = if (isToday && isMonthDate) {
                    primaryColor.copy(alpha = 0.5f)
                } else {
                    Color.Gray.copy(alpha = 0.1f)
                },
                style = androidx.compose.ui.graphics.drawscope.Stroke(
                    width = 0.5.dp.toPx()
                )
            )

            val dayText = day.date.day.toString()

            val dayTextStyle = TextStyle(
                color = if (isToday) primaryColor else textColor,
                fontSize = 12.sp,
                fontWeight = if (isToday) {
                    FontWeight.Bold
                } else {
                    FontWeight.SemiBold
                }
            )

            val dayLayout = textMeasurer.measure(
                text = dayText,
                style = dayTextStyle
            )

            drawText(
                textMeasurer = textMeasurer,
                text = dayText,
                topLeft = Offset(
                    x = (size.width - dayLayout.size.width) / 2f,
                    y = dayNumberTopPx
                ),
                style = dayTextStyle
            )

            for (laneIndex in 0..2) {
                val occ = occurrences[laneIndex] ?: continue

                val task = occ.task

                val isStart = occ.start == day.date
                val isEnd = occ.end == day.date

                val isCurrentOccurrence = (task as? Task.Set)?.start?.date == occ.start

                val pillColor = when (task) {
                    is Task.Set -> when {
                        task.completed == true && isCurrentOccurrence -> baseColor.copy(alpha = 0.3f)
                        isCurrentOccurrence && task.end < now -> errorColor
                        else -> primaryColor
                    }

                    is Task.Pool -> when {
                        task.completed -> baseColor.copy(alpha = 0.3f)
                        task.expiry != null && task.expiry < now -> errorColor
                        else -> primaryColor
                    }
                    else -> primaryColor
                }

                val y = taskTopPx + laneIndex * laneHeightPx
                val startX = if (isStart) 2.dp.toPx() else 0f
                val endX = if (isEnd) size.width - 2.dp.toPx() else size.width
                val pillTop = y + 1.dp.toPx()
                val pillBottom = y + laneHeightPx - 1.dp.toPx()
                val radius = 4.dp.toPx()
                val path = androidx.compose.ui.graphics.Path().apply {
                    val leftRadius = if (isStart) radius else 0f
                    val rightRadius = if (isEnd) radius else 0f

                    moveTo(startX + leftRadius, pillTop)

                    lineTo(endX - rightRadius, pillTop)

                    if (isEnd) {
                        quadraticTo(
                            endX,
                            pillTop,
                            endX,
                            pillTop + rightRadius
                        )
                    } else {
                        lineTo(endX, pillTop)
                    }

                    lineTo(
                        endX,
                        pillBottom - if (isEnd) radius else 0f
                    )

                    if (isEnd) {
                        quadraticTo(
                            endX,
                            pillBottom,
                            endX - rightRadius,
                            pillBottom
                        )
                    }

                    lineTo(
                        startX + if (isStart) radius else 0f,
                        pillBottom
                    )

                    if (isStart) {
                        quadraticTo(
                            startX,
                            pillBottom,
                            startX,
                            pillBottom - leftRadius
                        )
                    }

                    lineTo(
                        startX,
                        pillTop + if (isStart) radius else 0f
                    )

                    if (isStart) {
                        quadraticTo(
                            startX,
                            pillTop,
                            startX + leftRadius,
                            pillTop
                        )
                    }

                    close()
                }

                drawPath(
                    path = path,
                    color = pillColor.copy(alpha = 0.18f)
                )

                if (isStart) {
                    drawLine(
                        color = pillColor,
                        start = Offset(
                            x = startX,
                            y = pillTop + 4.dp.toPx()
                        ),
                        end = Offset(
                            x = startX,
                            y = pillBottom - 4.dp.toPx()
                        ),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                if (
                    isStart ||
                    day.date.dayOfWeek == firstDayOfWeek ||
                    day.date.day == 1
                ) {
                    val leftPadding = if (isStart) {
                        8.dp.toPx()
                    } else {
                        4.dp.toPx()
                    }

                    val rightPadding = 4.dp.toPx()

                    val availableWidth =
                        (endX - startX - leftPadding - rightPadding)
                            .coerceAtLeast(1f)

                    val titleStyle = TextStyle(
                        color = pillColor,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 8.sp
                    )

                    val layout = textMeasurer.measure(
                        text = task.title,
                        style = titleStyle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis,
                        constraints = androidx.compose.ui.unit.Constraints(
                            maxWidth = availableWidth.toInt()
                        )
                    )

                    drawText(
                        textMeasurer = textMeasurer,
                        text = task.title,
                        topLeft = Offset(
                            x = startX + leftPadding,
                            y = y + (laneHeightPx - layout.size.height) / 2f
                        ),
                        style = titleStyle,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (isMonthDate) {
            DetailsOverlay(
                isVisible = showOverlay,
                onDismiss = {
                    showOverlay = false
                },
                day = day,
                tasks = occurrences
                    .filterNotNull()
                    .map { it.task }
                    .distinct(),
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