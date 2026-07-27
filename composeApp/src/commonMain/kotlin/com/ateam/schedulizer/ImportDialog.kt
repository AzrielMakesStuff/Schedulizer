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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ateam.schedulizer.data.BusyBlock
import com.ateam.schedulizer.data.BusyBlockCodec
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

private fun weekDatesFor(
    anchor: LocalDate,
    firstDayOfWeek: kotlinx.datetime.DayOfWeek
): List<LocalDate> {
    val diff = (anchor.dayOfWeek.ordinal - firstDayOfWeek.ordinal + 7) % 7
    val start = anchor.minus(diff, DateTimeUnit.DAY)
    return (0..6).map { start.plus(it, DateTimeUnit.DAY) }
}

@Composable
fun ImportDialog(
    onDismiss: () -> Unit
) {
    val firstDayOfWeek = remember { firstDayOfWeekFromLocale() }
    var weekAnchor by remember { mutableStateOf(today()) }

    var dragAccumulator by remember { mutableFloatStateOf(0f) }
    var weekChangedThisDrag by remember { mutableStateOf(false) }

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Friend's busy blocks, parsed from a pasted string. Preview-only — never turned
    // into Task entries, since there's no title/content to give them meaning as tasks.
    var friendBlocks by remember { mutableStateOf<List<BusyBlock>?>(null) }
    var statusMessage by remember { mutableStateOf<String?>(null) }

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
            Column(
                modifier = Modifier
                    .fillMaxWidth(0.95f)
                    .shadow(12.dp, shape = RoundedCornerShape(24.dp))
                    .background(
                        MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(24.dp)
                    )
                    .clickable(enabled = false) { }
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Compare Schedules",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

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
                            selectedDateOne = null,
                            onCellTapped = { _, _, _ -> },
                            onTaskTapped = { _, _ -> },
                            overlayBusyBlocks = friendBlocks.orEmpty()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when {
                        friendBlocks == null ->
                            "Paste a schedule string from a friend to overlay their busy times, " +
                                    "or export yours to share with them."
                        friendBlocks!!.isEmpty() -> "No busy times in that data."
                        else -> "Showing ${friendBlocks!!.size} busy block(s) overlaid above (hatched)."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp),
                    textAlign = TextAlign.Center,
                )

                statusMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                val clip = clipboard.getClipEntry()
                                val text = clip?.readPlainText()
                                val decoded = text?.let { BusyBlockCodec.decode(it) }
                                if (decoded == null) {
                                    statusMessage = "Couldn't read a schedule from the clipboard."
                                } else {
                                    friendBlocks = decoded
                                    statusMessage = "Loaded ${decoded.size} busy block(s)."
                                }
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Paste & Preview")
                    }
                    Button(
                        onClick = {
                            scope.launch {
                                val weekDates = weekDatesFor(weekAnchor, firstDayOfWeek)
                                val myTasks = databaseInstance.tasks.value
                                    .filterIsInstance<Task.Set>()
                                    .filter { it.start.date in weekDates }
                                val blocks = BusyBlockCodec.fromTasks(myTasks)
                                val encoded = BusyBlockCodec.encode(blocks)
                                clipboard.setClipEntry(clipEntryOf(encoded))
                                statusMessage = "Copied this week's busy times to clipboard."
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Export This Week")
                    }
                }
            }
        }
    }
}