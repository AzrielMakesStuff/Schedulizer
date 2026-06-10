package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.daysOfWeek
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.todayIn
import kotlin.time.Clock.System
import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus

val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.THURSDAY)
val today = System.todayIn(TimeZone.currentSystemDefault())

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Calendar() {
    val tasks by databaseInstance.tasks.collectAsState(initial = emptyList())

    val laneOf = remember(tasks) {
        val sorted = tasks.sortedBy { it.dateOne }
        val lanes = mutableListOf<MutableList<Task>>()
        val laneMap = mutableMapOf<Task, Int>()

        sorted.forEach { task ->
            val tStart = task.dateOne.date
            val tEnd = task.dateTwo?.date ?: tStart
            var placed = false
            for (laneIdx in lanes.indices) {
                val conflict = lanes[laneIdx].any { existing ->
                    val eStart = existing.dateOne.date
                    val eEnd = existing.dateTwo?.date ?: eStart
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
            val start = task.dateOne.date
            val end = task.dateTwo?.date ?: start
            var current = start
            while (current <= end) {
                map.getOrPut(current) {
                    mutableListOf()
                }.add(task)
                current = current.plus(1, DateTimeUnit.DAY)
            }
        }
        map
    }

    val background = MaterialTheme.colorScheme.background

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
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

        Column {
            val backgroundColor = MaterialTheme.colorScheme.surface

            HorizontalCalendar(
                monthBody = { _, content ->
                    Box(modifier = Modifier.background(backgroundColor)) {
                        content()
                    }
                },
                state = state,
                dayContent = { Day(it, taskByDate[it.date] ?: emptyList(), laneOf) },
                monthHeader = { month: CalendarMonth ->
                    val daysOfWeek = remember(month) {
                        month.weekDays.first().map { it.date.dayOfWeek }
                    }
                    val monthName = month.yearMonth.month.name
                        .lowercase()
                        .replaceFirstChar { it.uppercase() }
                    val year = month.yearMonth.year
                    MonthName(monthName = monthName, year = year)
                    MonthHeader(daysOfWeek = daysOfWeek)
                }
            )
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
    val textColoring = MaterialTheme.colorScheme.onBackground
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        textAlign = TextAlign.Center,
        color = textColoring,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        text = ("$monthName $year")
    )
}

@Composable
fun Day(day: CalendarDay, tasks: List<Task>, laneOf: Map<Task, Int>) {
    val isMonthDate = day.position == DayPosition.MonthDate
    val baseColor = MaterialTheme.colorScheme.onSurface
    val textColor = if (isMonthDate) baseColor else baseColor.copy(alpha = 0.3f)
    val isToday = day.date == today

    var showOverlay by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .aspectRatio(0.55f)
            .border(
                color = if ((isToday) && (isMonthDate)) textColor.copy(alpha = 0.8f) else Color.Transparent.copy(
                    alpha = 0.2f
                ),
                width = 1.5.dp,
            )
            .clickable(enabled = isMonthDate, onClick = { showOverlay = true })
            .background(
                color = if ((isToday) && (isMonthDate)) MaterialTheme.colorScheme.primaryContainer.copy(
                    alpha = 0.3f
                ) else Color.Transparent
            ),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = day.date.day.toString(),
                color = if (isToday) textColor.copy(alpha = 0.8f) else textColor,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )

            for (laneIndex in 0..2) {
                val task = tasks.find { laneOf[it] == laneIndex }
                if (task != null) {
                    val isStart = task.dateOne.date == day.date
                    val isEnd = (task.dateTwo?.date ?: task.dateOne.date) == day.date

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(14.dp)
                            .padding(vertical = 1.dp)
                            .padding(
                                start = if (isStart) 2.dp else 0.dp,
                                end = if (isEnd) 2.dp else 0.dp
                            )
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(
                                    topStart = if (isStart) 2.dp else 0.dp,
                                    bottomStart = if (isStart) 2.dp else 0.dp,
                                    topEnd = if (isEnd) 2.dp else 0.dp,
                                    bottomEnd = if (isEnd) 2.dp else 0.dp
                                )
                            ),
                            contentAlignment = Alignment.CenterStart
                    ) {
                        if (isStart || day.date.dayOfWeek == daysOfWeek.first() || day.date.day == 1) {
                            Text(
                                text = task.title,
                                fontSize = 7.sp,
                                lineHeight = 8.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 4.dp)
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(14.dp))
                }
            }
        }
        if (isMonthDate) {
            DetailsOverlay(
                isVisible = showOverlay,
                onDismiss = { showOverlay = false },
                day = day,
                tasks = tasks
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
        tasks: List<Task>
    ) {
        var createTaskDialog by remember { mutableStateOf(false) }

        if (createTaskDialog) {
            TaskDialog(
                dateTwo = day.date,
                onDismiss = { createTaskDialog = false }
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
                            .padding(9.dp)
                            .widthIn(max = 450.dp)
                            .heightIn(max = 800.dp)
                            .padding(8.dp)
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.99f),
                                shape = RoundedCornerShape(25.dp)
                            )
                            .clickable(enabled = false) { },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                                    val currentDay = day.date.dayOfWeek.name

                                    Text(
                                        text = "$currentDay - ${day.date}",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )

                                    if (tasks.isNotEmpty()) {
                                        Text(
                                            text = "Tasks for Today",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                        tasks.forEach { task ->
                                            Text(
                                                text = "${task.title} - ${task.type}",
                                                color = MaterialTheme.colorScheme.onSurface,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    } else {
                                        Text(
                                            text = "Add Task?",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )
                                    }
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
    }

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
    val textColoring = MaterialTheme.colorScheme.onSurface
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.name.take(3),
                color = textColoring,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

//The Game