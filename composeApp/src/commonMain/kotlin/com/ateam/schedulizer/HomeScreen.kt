package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.yearMonth

@Composable
fun HomeScreen() {
    var createTaskDialog by remember { mutableStateOf(false) }

    val baseColor =
        if (isSystemInDarkTheme()) Color(0xFF303030) else Color.LightGray.copy(alpha = 0.5f)
    val textColoring =
        if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black
    val background = if (isSystemInDarkTheme()) Color(0xFF000000) else Color.White

    if (createTaskDialog) CreateTaskDialog { createTaskDialog = false }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .padding(8.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(baseColor),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val currentDate = remember { LocalDate.now() }
                    val startDate = remember { currentDate.minusMonths(12) }
                    val endDate = remember { currentDate.plusMonths(12) }
                    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }

                    val state = rememberWeekCalendarState(
                        startDate = startDate,
                        endDate = endDate,
                        firstVisibleWeekDate = currentDate,
                        firstDayOfWeek = firstDayOfWeek,
                    )

                    val visibleMonth = remember(state.firstVisibleWeek) {
                        state.firstVisibleWeek.days.first().date.yearMonth
                    }

                    MonthName(
                        monthName = visibleMonth.month.name.lowercase()
                            .replaceFirstChar { it.uppercase() },
                        year = visibleMonth.year
                    )

                    WeekCalendar(
                        state = state,
                        dayContent = { day ->
                            HomeDay(CalendarDay(day.date, DayPosition.MonthDate))
                        },
                        weekHeader = { week ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                for (day in week.days) {
                                    Text(
                                        modifier = Modifier
                                            .weight(1f),
                                        textAlign = TextAlign.Center,
                                        text = day.date.dayOfWeek.name.take(3),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = textColoring
                                    )
                                }
                            }
                        }
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .height(100.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(baseColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "The Game",
                    fontWeight = FontWeight.Bold,
                    color = textColoring,
                    modifier = Modifier.clickable { createTaskDialog = true }
                )
            }


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(8.dp)
                    .clip(RoundedCornerShape(25.dp))
                    .background(baseColor)
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
fun HomeDay(day: CalendarDay) {
    val isMonthDate = day.position == DayPosition.MonthDate
    val textColoring = if (isSystemInDarkTheme()) Color.White else Color.Black
    val isToday = day.date == today

    var showOverlay by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .border(
                color = if (isToday) Color.White else Color.White.copy(alpha = 0.6f),
                width = if (isToday) 0.7.dp else 0.5.dp,
            )
            .clickable(enabled = isMonthDate, onClick = { showOverlay = true }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.day.toString(),
            color = textColoring,
            modifier = Modifier
                .padding(10.dp)
                .align(Alignment.TopCenter)
        )
    }
    if (isMonthDate) {
        DetailsOverlay(isVisible = showOverlay, onDismiss = { showOverlay = false }, day = day)
    }
}
