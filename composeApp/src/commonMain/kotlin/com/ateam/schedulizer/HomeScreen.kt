package com.ateam.schedulizer

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Task
import androidx.compose.material.icons.filled.Today
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateam.schedulizer.entities.Task
import org.jetbrains.compose.resources.painterResource
import schedulizer.composeapp.generated.resources.Res
import schedulizer.composeapp.generated.resources.streak_complete
import schedulizer.composeapp.generated.resources.streak_incomplete
import schedulizer.composeapp.generated.resources.streakbackground

@Composable
fun HomeScreen()
{
    val tasks by databaseInstance.tasks.collectAsState(initial = emptyList())

    val streakStatus = remember { mutableStateOf(AppDatabase.StreakStatus(
        lost = false,
        completedToday = true
    )) }

    LaunchedEffect(Unit) {
        databaseInstance.refresh()
        streakStatus.value = databaseInstance.checkStreak()
    }

    val streak by databaseInstance.streak.collectAsState()

    var createTaskDialog by remember { mutableStateOf(false) }

    if (createTaskDialog) {
        TaskDialog(
            onDismiss = { createTaskDialog = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    )
    {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.3f)
                .padding(15.dp)
                .clip(RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(Res.drawable.streakbackground),
                alpha = 0.5f,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                contentScale = ContentScale.Crop,
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(15.dp),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.Top
            )
            {
                Image(
                    painter = painterResource(if (streakStatus.value.completedToday) Res.drawable.streak_complete else Res.drawable.streak_incomplete),
                    contentDescription = "Streak",
                )
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(
                        start = 15.dp,
                        end = 60.dp,
                        top = 15.dp,
                        bottom = 15.dp,
                    ),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            )
            {
                val streakNow = streak + if (streakStatus.value.completedToday) 1 else 0
                Text(
                    //The streak number value is here
                    text = "$streakNow Day${if (streakNow != 1) "s" else ""}",
                    autoSize = TextAutoSize.StepBased(
                        minFontSize = 10.sp,
                        maxFontSize = 20.sp,
                    ),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
                contentAlignment = Alignment.TopCenter
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxHeight()
                        .padding(top = 10.dp),
                    contentAlignment = Alignment.TopCenter
                )
                {
                    Text(
                        text = "Daily Task",
                        fontWeight = FontWeight.Bold,
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                )
                {
                    Text(
                        text = "Put a random task here or something",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }

            }
        }
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth()
                .padding(15.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)),
        )
        {
            Column(modifier = Modifier.fillMaxSize())
            {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                )
                {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    )
                    {
                        Icon(
                            imageVector = Icons.Default.Task,
                            contentDescription = "Tasks",
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Total Tasks",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "${
                                tasks.count { task ->
                                    when (task) {
                                        is Task.Daily -> task.lastCompleted?.date != today()
                                        is Task.Set -> !task.completed
                                        is Task.Pool -> !task.completed
                                    }
                                }
                            }",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(15.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Top
                    )
                    {
                        Icon(
                            imageVector = Icons.Default.Today,
                            contentDescription = "tasksToday",
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "Tasks Today",
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(modifier = Modifier.height(5.dp))
                        Text(
                            text = "${
                                tasks.count { task ->
                                    when (task) {
                                        is Task.Daily -> task.lastCompleted?.date != today()
                                        is Task.Set -> !task.completed && task.start.date <= today() && (task.end == null || task.end.date >= today())
                                        is Task.Pool -> !task.completed && (task.expiry == null || task.expiry.date <= today())
                                    }
                                }
                            }",
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
                {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    )
                    {
                        Text(
                            text = "Tasks",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.headlineMedium
                        )
                        IconButton(onClick = {
                            createTaskDialog = true
                        }) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Create Task",
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}