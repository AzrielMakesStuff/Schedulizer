package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ateam.schedulizer.data.ICSParser
import com.ateam.schedulizer.data.rememberIcsExporter
import com.ateam.schedulizer.data.rememberIcsImporter
import com.ateam.schedulizer.data.rememberIcsSharer
import com.ateam.schedulizer.entities.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun ShareDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()

    var statusMessage by remember { mutableStateOf<String?>(null) }
    var showSubscriptions by remember { mutableStateOf(false) }

    val exportIcs = rememberIcsExporter { success ->
        statusMessage = if (success) "Exported" else "Export failed"
    }
    val shareIcs = rememberIcsSharer()
    val importIcs = rememberIcsImporter { content ->
        if (content == null) {
            statusMessage = "Import cancelled"
        } else {
            scope.launch {
                try {
                    withContext(Dispatchers.Default) { ICSParser.fromICS(content) }
                    statusMessage = "Imported"
                } catch (e: Exception) {
                    e.printStackTrace()
                    statusMessage = "Import failed"
                }
            }
        }
    }

    fun currentIcs(onReady: (String) -> Unit) {
        scope.launch {
            val ics = withContext(Dispatchers.Default) {
                val setTasks = databaseInstance.tasks.value.filterIsInstance<Task.Set>()
                ICSParser.toICS(setTasks, now())
            }
            onReady(ics)
        }
    }

    Popup(onDismissRequest = onDismiss, properties = PopupProperties(focusable = true)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.6f))
                .clickable { onDismiss() },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .padding(24.dp)
                    .widthIn(max = 450.dp)
                    .shadow(12.dp, shape = RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
                    .clickable(enabled = false) { }
            ) {
                Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Share", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        ShareOption(
                            icon = Icons.Filled.Download,
                            label = "Export ICS file",
                            onClick = { currentIcs { ics -> exportIcs("schedulizer.ics", ics) } }
                        )
                        ShareOption(
                            icon = Icons.Filled.Share,
                            label = "Share ICS file",
                            onClick = { currentIcs { ics -> shareIcs("schedulizer.ics", ics) } }
                        )
                        ShareOption(
                            icon = Icons.Filled.Backup,
                            label = "Import ICS file",
                            onClick = { importIcs() }
                        )
                        ShareOption(
                            icon = Icons.Filled.CalendarMonth,
                            label = "Subscriptions",
                            onClick = { showSubscriptions = true }
                        )
                    }
                    statusMessage?.let {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }

                    if (showSubscriptions) CalendarSubscriptionsDialog(onDismiss = { showSubscriptions = false })
                }
            }
        }
    }
}

@Composable
private fun ShareOption(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(84.dp).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector = icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary)
        }
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}