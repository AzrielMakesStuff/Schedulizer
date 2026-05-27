package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.rememberLifecycleOwner
import androidx.room.Database
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.compose.WeekCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.weekcalendar.rememberWeekCalendarState
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.OutDateStyle
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import com.kizitonwose.calendar.core.minusMonths
import com.kizitonwose.calendar.core.now
import com.kizitonwose.calendar.core.plusMonths
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.yearMonth

@Composable
fun HomeScreen() {
    val coroutineScope = rememberCoroutineScope()

    val baseColor =
        if (isSystemInDarkTheme()) Color(0xFF303030) else Color.LightGray.copy(alpha = 0.5f)
    val textColoring =
        if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black
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
            outDateStyle = OutDateStyle.EndOfGrid,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillParentMaxHeight()
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

                        val monthName = currentMonth.month.name
                            .lowercase()
                            .replaceFirstChar { it.uppercase() }
                        val year = currentMonth.year
                        MonthName(
                            monthName = visibleMonth.month.name.lowercase()
                                .replaceFirstChar { it.uppercase() }, year = visibleMonth.year
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
                        .height(200.dp)
                        .clip(RoundedCornerShape(25.dp))
                        .background(baseColor)
                        .clickable { coroutineScope.launch { databaseInstance.createTask() } },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "The Game",
                        fontWeight = FontWeight.Bold,
                        color = textColoring,
                        modifier = Modifier.clickable { coroutineScope.launch { databaseInstance.createTask() } })
                }
            }
        }
    }
}

@Composable
fun HomeDay(day: CalendarDay) {
    val isMonthDate = day.position == DayPosition.MonthDate
    val baseColor = if (isSystemInDarkTheme()) Color.Black else Color.White
    val textColor = if (isMonthDate) baseColor else baseColor
    val textColoring = if (isSystemInDarkTheme()) Color.White else Color.Black
    val isToday = day.date == today

    var showOverlay by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .border(
                color = if (isToday) Color.White else Color.White.copy(alpha = 0.6f),
                width = if(isToday) 0.7.dp else 0.5.dp,
            )
            .clickable(enabled = isMonthDate, onClick = { showOverlay = true }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.dayOfMonth.toString(), //Ignore Deprecation here, still works :P
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
