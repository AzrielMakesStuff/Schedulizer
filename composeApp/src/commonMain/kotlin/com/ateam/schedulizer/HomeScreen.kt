package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.WeekDay
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth

@Composable
fun HomeScreen() {
    var createTaskDialog by remember { mutableStateOf(false) }
    if (createTaskDialog) CreateTaskDialog { createTaskDialog = false }

    val today = remember { LocalDate.now() }

    val startDate = remember { today.minusMonths(12) }
    val endDate = remember { today.plusMonths(12) }
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

    val state = rememberWeekCalendarState(
        startDate = startDate,
        endDate = endDate,
        firstVisibleWeekDate = today,
        firstDayOfWeek = firstDayOfWeek,
    )

    val visibleMonth = remember(state.firstVisibleWeek) {
        state.firstVisibleWeek.days.first().date.yearMonth
    }

    Box {
        Column(
            modifier = Modifier
                .padding(8.dp)
        ) {
            MonthName(
                monthName = visibleMonth.month.name.lowercase().replaceFirstChar { it.uppercase() },
                year = visibleMonth.year
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(top = 12.dp, bottom = 4.dp, end = 8.dp)
            ) {
                Spacer(modifier = Modifier.width(60.dp))

                Box(modifier = Modifier.weight(1f)) {
                    WeekCalendar(
                        state = state,
                        dayContent = { weekDay ->
                            CompactHomeDayHeader(
                                date = weekDay.date,
                                isToday = weekDay.date == today
                            )
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .heightIn(max = 350.dp)
            ) {
                WeeklyScheduleGrid(visibleDays = state.firstVisibleWeek.days)
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(25.dp))
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth()
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
                    modifier = Modifier.fillMaxSize()
                        .padding(top = 30.dp),
                )
                {
                    LazyListTasks()
                }
            }
        }
    }
}

@Composable
fun CompactHomeDayHeader(date: LocalDate, isToday: Boolean) {
    val dayOfWeekName = date.dayOfWeek.name.take(3)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isToday) MaterialTheme.colorScheme.primaryContainer
                else Color.Transparent
            )
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = dayOfWeekName,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = date.day.toString(),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun WeeklyScheduleGrid(visibleDays: List<WeekDay>) {
    val scrollState = rememberScrollState()
    val hours = remember { (0..23).toList() }
    val hourHeight = 18.dp

    Row(
        modifier = Modifier
            .verticalScroll(scrollState)
            .padding(bottom = 16.dp, start = 8.dp, end = 8.dp)
    ) {
        Column(
            modifier = Modifier.width(60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            hours.forEach { hour ->
                val timeString = "${hour.toString().padStart(2, '0')}:00"
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(hourHeight)
                        .drawWithContent {
                            drawContent()
                            val strokeWidth = 0.5.dp.toPx()
                            if (hour < 23) {
                                drawLine(
                                    color = Color.Gray.copy(alpha = 0.15f),
                                    start = Offset(x = 0f, y = size.height),
                                    end = Offset(x = size.width, y = size.height),
                                    strokeWidth = strokeWidth
                                )
                            }
                            drawLine(
                                color = Color.Gray.copy(alpha = 0.1f),
                                start = Offset(x = size.width, y = 0f),
                                end = Offset(x = size.width, y = size.height),
                                strokeWidth = strokeWidth
                            )
                        },
                    contentAlignment = Alignment.TopCenter
                ) {
                    Text(
                        text = timeString,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            visibleDays.forEach { weekDay ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    hours.forEach { hour ->
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(hourHeight)
                                .drawWithContent {
                                    drawContent()
                                    val strokeWidth = 0.5.dp.toPx()
                                    if (hour < 23) {
                                        drawLine(
                                            color = Color.Gray.copy(alpha = 0.15f),
                                            start = Offset(x = 0f, y = size.height),
                                            end = Offset(x = size.width, y = size.height),
                                            strokeWidth = strokeWidth
                                        )
                                    }
                                    if (weekDay.date.dayOfWeek.ordinal != 5) {
                                        drawLine(
                                            color = Color.Gray.copy(alpha = 0.1f),
                                            start = Offset(x = size.width, y = 0f),
                                            end = Offset(x = size.width, y = size.height),
                                            strokeWidth = strokeWidth
                                        )
                                    }
                                }
                                .padding(2.dp)
                        ) {
                            // Populate individual
                        }
                    }
                }
            }
        }
    }
}