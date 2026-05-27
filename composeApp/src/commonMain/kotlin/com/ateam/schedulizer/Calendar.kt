package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.core.*
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.YearMonth
import kotlinx.datetime.format.DayOfWeekNames
import com.kizitonwose.calendar.core.CalendarDay
import com.kizitonwose.calendar.core.CalendarMonth
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.THURSDAY)

@Preview
@Composable
fun Calendar()
{
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) } // Adjust as needed
    val endMonth = remember { currentMonth.plusMonths(100) } // Adjust as needed
    val firstDayOfWeek = remember { daysOfWeek() } // Available from the library

    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
    )

    Column {

        val light = Color(0xFFF5F5F5)
        val dark = Color(0xFF1E1E24)

        val backgroundColor = if (isSystemInDarkTheme()) {
            dark
        } else{
            light
        }

        state.firstVisibleMonth.toString()
        HorizontalCalendar(
            monthBody = { _, content ->
                Box(
                    modifier = Modifier.background(backgroundColor)
                ) {
                    content()
                }
            },
            state = state,
            dayContent = { Day(it) },
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

@Composable
fun DaysOfWeekTitle(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = DayOfWeekNames.ENGLISH_ABBREVIATED.names[dayOfWeek.ordinal],
            )
        }
    }
}

@Composable
fun MonthName(monthName: String, year: Int) {
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center,
        fontSize = 22.sp,
        fontWeight = FontWeight.Bold,
        text = ("$monthName $year")
    )
}

@Composable
fun Day(day: CalendarDay) {
    val textColor = if(isSystemInDarkTheme()) {
        Color.White
    } else {
        Color.Black
    }

    Box(
        modifier = Modifier
        .aspectRatio(1f)
        .border(
            color = Color.Black,
            width = 0.5.dp,
            //shape = RoundedCornerShape(8.dp)
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.date.day.toString(),
        color = textColor
    )
    }
}

@Composable
fun MonthHeader(daysOfWeek: List<DayOfWeek>) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (dayOfWeek in daysOfWeek) {
            Text(
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                text = dayOfWeek.name.take(3),
                fontWeight = FontWeight.Bold,
            )
        }
    }
}