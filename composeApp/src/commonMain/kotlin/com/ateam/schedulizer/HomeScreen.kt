package com.ateam.schedulizer

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.ateam.schedulizer.dao.TaskDao

@Composable
fun HomeScreen() {
    var dao by remember { mutableStateOf<TaskDao?>(null) }
    LaunchedEffect(Unit) {
        val database = getDatabaseInstance();
        dao = database.getDao();
    }
    Text(text = "Hi", fontWeight = FontWeight.Bold)
}