package com.ateam.schedulizer

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import com.kizitonwose.calendar.compose.rememberCalendarState
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kizitonwose.calendar.compose.HorizontalCalendar
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import com.kizitonwose.calendar.core.CalendarMonth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.PopupProperties
import androidx.compose.foundation.layout.Column
import com.kizitonwose.calendar.core.CalendarDay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.runtime.mutableStateOf
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.background
import kotlinx.datetime.format.DayOfWeekNames
import androidx.compose.foundation.clickable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
<<<<<<< Updated upstream
=======
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
>>>>>>> Stashed changes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Popup
import com.kizitonwose.calendar.core.*
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.YearMonth
import kotlinx.datetime.todayIn
import kotlin.time.Clock

val daysOfWeek = daysOfWeek(firstDayOfWeek = DayOfWeek.THURSDAY)
val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

@Composable
<<<<<<< Updated upstream
fun Calendar()
{
    val currentMonth = remember { YearMonth.now() }
    val startMonth = remember { currentMonth.minusMonths(100) }
    val endMonth = remember { currentMonth.plusMonths(100) }
    val state = rememberCalendarState(
        startMonth = startMonth,
        endMonth = endMonth,
        firstVisibleMonth = currentMonth,
        firstDayOfWeek = daysOfWeek.first()
=======
fun Calendar() {
    val baseColor =
        if (isSystemInDarkTheme()) Color(0xFF303030) else Color.LightGray.copy(alpha = 0.5f)
    val textColoring =
        if (isSystemInDarkTheme()) Color.White.copy(alpha = 0.8f) else Color.Black
    val background = if (isSystemInDarkTheme()) Color(0xFF000000) else Color.White

    val scrollState = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
            .windowInsetsPadding(WindowInsets.systemBars)

>>>>>>> Stashed changes
    )
    {
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
<<<<<<< Updated upstream
=======

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

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.15f),
                contentAlignment = Alignment.Center
            )
            {
                Text(
                    text = "Tasks",
                    style = MaterialTheme.typography.headlineSmallEmphasized,
                    textAlign = TextAlign.Center,
                    color = textColoring
                )
            }


            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(10.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .verticalScroll(rememberScrollState())
                    .background(Color.LightGray),
            )
            {
                LazyListTasks()
            }
        }
>>>>>>> Stashed changes
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
<<<<<<< Updated upstream
    val textColor = if(isSystemInDarkTheme()) Color.White else Color.Black
=======
    val isMonthDate = day.position == DayPosition.MonthDate
    val baseColor = if (isSystemInDarkTheme()) Color.White else Color.Black
    val textColor = if (isMonthDate) baseColor else baseColor.copy(alpha = 0.3f)
    val isToday = day.date == today
>>>>>>> Stashed changes

    var showOverlay by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
<<<<<<< Updated upstream
        .aspectRatio(1f)
        .border(
            color = Color.Black,
            width = 0.5.dp,
            //shape = RoundedCornerShape(8.dp)
        )
        .clickable(onClick = {showOverlay = true}),
        contentAlignment = Alignment.Center
    ) {
        Text(text = day.date.day.toString(),
        color = textColor
    )
=======
            .aspectRatio(1f)
            .border(
                color = if (isToday) Color.White else Color.Black.copy(alpha = 0.1f),
                width = 0.5.dp,
            )
            .clickable(enabled = isMonthDate, onClick = { showOverlay = true }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day.date.day.toString(),
            color = if (isToday) textColor.copy(alpha = 0.8f) else textColor
        )
    }
    if (isMonthDate) {
        DetailsOverlay(isVisible = showOverlay, onDismiss = { showOverlay = false }, day = day)
>>>>>>> Stashed changes
    }
    DetailsOverlay(isVisible = showOverlay, onDismiss = { showOverlay = false }, day = day)
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DetailsOverlay(isVisible: Boolean, onDismiss: () -> Unit, day: CalendarDay)
{
    if(isVisible) {
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
                Box (
                    modifier = Modifier
                        .padding(9.dp)
                        .widthIn(max = 450.dp)
                        .heightIn(max = 800.dp)
                        .padding(16.dp)
                        .background(Color(0xFF1E1E1E).copy(alpha = 0.94f), shape = RoundedCornerShape(50.dp))
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
                        )
                        {
                            Column(
                                modifier = Modifier
                                    .padding(24.dp),
//                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            )
                            {
                                val currentDay = day.date.dayOfWeek.name

                                Text(text = currentDay,
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White)

                                Text(text = "Create Task for $currentDay",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White)
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
