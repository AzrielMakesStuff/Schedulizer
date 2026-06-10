package com.ateam.schedulizer

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Fireplace
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun HomeScreen()
{
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
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Streak System Here",
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .fillMaxHeight(0.3f)
                    .padding(
                        top = 10.dp,
                        bottom = 5.dp,
                        start = 15.dp,
                        end = 5.dp,
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                        text = "Quick Task Create",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.3f)
                    .padding(
                        top = 10.dp,
                        bottom = 5.dp,
                        start = 5.dp,
                        end = 15.dp,
                    )
                    .clip(RoundedCornerShape(30.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
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
                .fillMaxHeight(0.45f)
                .fillMaxWidth()
                .padding(15.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        )
        {
            Row(
                modifier = Modifier
                    .fillMaxSize()
            )
            {
                Column(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                )
                {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Calories",
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "[Num] Calories Burned",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(15.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Top
                )
                {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                        contentDescription = "Steps",
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "[Num] Steps Today",
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}