package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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

val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.THURSDAY)
val today = System.todayIn(TimeZone.currentSystemDefault())

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Calendar() {
    val tasks by databaseInstance.tasks.collectAsState()
    val tasksByEndDate = remember(tasks) {
        tasks.filter { it.dateTwo != null }
            .groupBy { it.dateTwo!!.date }
    }
    val baseColor = if (isSystemInDarkTheme()) Color(0xFF303030) else Color.LightGray.copy(alpha = 0.5f)
    val textColoring = if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black
    val background = if (isSystemInDarkTheme()) Color(0xFF000000) else Color.White

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
            val light = Color(0xFFF5F5F5)
            val dark = Color(0xFF1E1E24)

            val backgroundColor = if (isSystemInDarkTheme()) dark else light

            state.firstVisibleMonth.toString()
            HorizontalCalendar(
                monthBody = { _, content ->
                    Box(modifier = Modifier.background(backgroundColor)) {
                        content()
                    }
                },
                state = state,
                dayContent = { Day(it, tasksByEndDate[it.date] ?: emptyList()) },
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
    val textColoring =
        if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black
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
    val textColoring =
        if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black
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
fun Day(day: CalendarDay, tasks: List<Task>) {
    val isMonthDate = day.position == DayPosition.MonthDate
    val baseColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    val textColor = if (isMonthDate) baseColor else baseColor.copy(alpha = 0.3f)
    val isToday = day.date == today

    var showOverlay by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .aspectRatio(0.55f)
            .border(
                color = if (isToday) Color.White else Color.Black,
                width = 1.5.dp,
            )
            .clickable(enabled = isMonthDate, onClick = { showOverlay = true }),
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

        tasks.take(3).forEach { task ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 1.dp, horizontal = 2.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                        shape = RoundedCornerShape(2.dp)
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 7.sp,
                    lineHeight = 8.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = textColor,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
            }
    }
    if (isMonthDate) {
        DetailsOverlay(isVisible = showOverlay, onDismiss = { showOverlay = false }, day = day, tasks = tasks)
    }
}

@Composable
fun DetailsOverlay(isVisible: Boolean, onDismiss: () -> Unit, day: CalendarDay, tasks: List<Task>) {
    var createTaskDialog by remember { mutableStateOf(false) }

    if (createTaskDialog) {
        CreateTaskDialog(
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
                        .padding(16.dp)
                        .background(
                            Color(0xFF1E1E1E).copy(alpha = 0.94f),
                            shape = RoundedCornerShape(50.dp)
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
                                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(16.dp))
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
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                Text(
                                    text = "Tasks for Today",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = Color.White,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )

                                if (tasks.isNotEmpty()) {
                                    tasks.forEach { task ->
                                        Text(
                                            text = "${task.title} - ${task.type}",
                                            color = Color.White,
                                            modifier = Modifier.padding(vertical = 4.dp)
                                        )
                                    }
                                }
                                Button(onClick = {
                                    onDismiss()
                                    createTaskDialog = true
                                }) {
                                    Text(text = "Create Task for $currentDay")
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
        val textColoring =
            if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black
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