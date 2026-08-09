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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.ateam.schedulizer.data.CalendarSyncManager
import com.ateam.schedulizer.entities.CalendarSubscription
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CalendarSubscriptionsDialog(onDismiss: () -> Unit) {
    val scope = rememberCoroutineScope()
    val subscriptions by databaseInstance.subscriptions.collectAsState()

    var showAddForm by remember { mutableStateOf(false) }
    var nameText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var syncingId by remember { mutableStateOf<Long?>(null) }

    fun addSubscription() {
        val url = urlText.trim()
        val name = nameText.trim().ifBlank { "Calendar" }
        if (url.isBlank()) return
        scope.launch {
            statusMessage = "Adding…"
            val result = withContext(Dispatchers.Default) { CalendarSyncManager.addSubscription(url, name) }
            result.fold(
                onSuccess = {
                    statusMessage = "Added \"$name\""
                    showAddForm = false
                    nameText = ""
                    urlText = ""
                },
                onFailure = { statusMessage = "Couldn't add that calendar" }
            )
        }
    }

    fun syncNow(subscription: CalendarSubscription) {
        scope.launch {
            syncingId = subscription.id
            val result = withContext(Dispatchers.Default) { CalendarSyncManager.sync(subscription) }
            statusMessage = if (result.isSuccess) "Synced \"${subscription.name}\"" else "Sync failed"
            syncingId = null
        }
    }

    fun remove(subscription: CalendarSubscription) {
        scope.launch {
            withContext(Dispatchers.Default) { CalendarSyncManager.removeSubscription(subscription) }
            statusMessage = "Removed \"${subscription.name}\""
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
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calendar subscriptions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showAddForm = !showAddForm }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add subscription")
                        }
                    }

                    if (showAddForm) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = nameText,
                            onValueChange = { nameText = it },
                            label = { Text("Name") },
                            placeholder = { Text("Work calendar") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            label = { Text("Calendar link (ICS URL)") },
                            placeholder = { Text("https://calendar.google.com/.../basic.ics") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            TextButton(onClick = { showAddForm = false }) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { addSubscription() }) { Text("Add") }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (subscriptions.isEmpty() && !showAddForm) {
                        Text(
                            "No subscriptions yet. Add a Google Calendar, Outlook, or Blackboard link to keep tasks in sync automatically.",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        LazyColumn(modifier = Modifier.heightIn(max = 300.dp)) {
                            items(subscriptions, key = { it.id }) { subscription ->
                                SubscriptionRow(
                                    subscription = subscription,
                                    isSyncing = syncingId == subscription.id,
                                    onSyncNow = { syncNow(subscription) },
                                    onRemove = { remove(subscription) }
                                )
                            }
                        }
                    }

                    statusMessage?.let {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(it, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubscriptionRow(
    subscription: CalendarSubscription,
    isSyncing: Boolean,
    onSyncNow: () -> Unit,
    onRemove: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(subscription.name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(
                subscription.lastSyncedAt?.let { "Last synced ${it.date} ${it.time}" } ?: "Never synced",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (isSyncing) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp))
        } else {
            IconButton(onClick = onSyncNow) {
                Icon(Icons.Filled.Refresh, contentDescription = "Sync now")
            }
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
        }
    }
}