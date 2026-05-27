package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp


@Composable
fun HomeScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
    )
    {
        Column(
             modifier = Modifier.fillMaxSize()
        )
        {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.25f)
                    .padding(15.dp)
                    .clip(RoundedCornerShape(30.dp))
                    .background(Color.LightGray),
                contentAlignment = Alignment.Center
            )
            {
                Text(text = "Hi", fontWeight = FontWeight.Bold)
            }

            Row(
                modifier = Modifier
                    .fillMaxHeight(0.25f)
                    .fillMaxWidth()
            )
            {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight()
                        .padding(15.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                )
                {
                    Text(text = "Hi", fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(15.dp)
                        .clip(RoundedCornerShape(30.dp))
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                )
                {
                    Text(text = "Hi", fontWeight = FontWeight.Bold)
                }
            }

        }

    }
}
//import com.ateam.schedulizer.entities.Task
//
//@Composable
//fun HomeScreen(tasks: List<Task>, onCreateTask: () -> Unit, onDeleteTask: (Task) -> Unit) {
//    Column {
//        Button(onClick = onCreateTask) {
//            Text("Create Task")
//        }
//        tasks.forEach { task ->
//            Text(text = "${task.title} - ${task.type} - ${task.dateOne} - ${task.dateTwo}")
//            Button(onClick = { onDeleteTask(task) }) {
//                Text("Delete")
//            }
//        }
//    }
//}
