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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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

    var showForm by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<CalendarSubscription?>(null) }
    var nameText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }
    var linkError by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var syncingId by remember { mutableStateOf<Long?>(null) }

    fun closeForm() {
        showForm = false; editing = null
        nameText = ""; urlText = ""; linkError = null
    }

    fun openAdd() { closeForm(); showForm = true }

    fun openEdit(sub: CalendarSubscription) {
        editing = sub; nameText = sub.name; urlText = sub.url
        linkError = null; showForm = true
    }

    fun save() {
        val url = urlText.trim()
        val name = nameText.trim().ifBlank { "Calendar" }
        if (url.isBlank()) { linkError = "Enter a calendar link"; return }
        val target = editing
        scope.launch {
            saving = true; linkError = null
            val result = withContext(Dispatchers.Default) {
                CalendarSyncManager.saveSubscription(target, name, url)
            }
            saving = false
            result.fold(
                onSuccess = {
                    statusMessage = if (target == null) "Added \"$name\"" else "Updated \"$name\""
                    closeForm()
                },
                onFailure = { linkError = "That link isn't a valid calendar (ICS) feed" }
            )
        }
    }

    fun toggle(sub: CalendarSubscription, enabled: Boolean) {
        scope.launch {
            syncingId = sub.id
            val result = withContext(Dispatchers.Default) { CalendarSyncManager.setEnabled(sub, enabled) }
            statusMessage = when {
                result.isFailure -> "Couldn't ${if (enabled) "enable" else "disable"} \"${sub.name}\""
                enabled -> "Enabled \"${sub.name}\""
                else -> "Disabled \"${sub.name}\" and removed its tasks"
            }
            syncingId = null
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
            if (editing?.id == subscription.id) closeForm()
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
                    .padding(16.dp)
                    .widthIn(max = 560.dp)
                    .fillMaxWidth()
                    .shadow(12.dp, shape = RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
                    .clickable(enabled = false) { }
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Calendar subscriptions", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { if (showForm && editing == null) closeForm() else openAdd() }) {
                            Icon(Icons.Filled.Add, contentDescription = "Add subscription")
                        }
                    }

                    if (showForm) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            if (editing == null) "Add subscription" else "Edit subscription",
                            fontSize = 14.sp, fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
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
                            onValueChange = { urlText = it; linkError = null },
                            label = { Text("Calendar link (ICS URL)") },
                            placeholder = { Text("https://calendar.google.com/.../basic.ics") },
                            isError = linkError != null,
                            supportingText = linkError?.let { { Text(it) } },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (saving) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            TextButton(onClick = { closeForm() }, enabled = !saving) { Text("Cancel") }
                            Spacer(modifier = Modifier.width(8.dp))
                            TextButton(onClick = { save() }, enabled = !saving) {
                                Text(if (editing == null) "Add" else "Save")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (subscriptions.isEmpty() && !showForm) {
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
                                    isEditing = editing?.id == subscription.id,
                                    onToggle = { toggle(subscription, it) },
                                    onEdit = { openEdit(subscription) },
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
    isEditing: Boolean,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onSyncNow: () -> Unit,
    onRemove: () -> Unit
) {
    val alpha = if (subscription.enabled) 1f else 0.5f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .background(
                if (isEditing) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        // Line 1: name + sync status, full width
        Text(
            subscription.name,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha)
        )
        Text(
            if (!subscription.enabled) "Disabled"
            else subscription.lastSyncedAt?.let { "Last synced ${it.date} ${it.time}" } ?: "Never synced",
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        // Line 2: switch on the left, actions on the right
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = subscription.enabled,
                    onCheckedChange = onToggle,
                    enabled = !isSyncing
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    if (subscription.enabled) "Enabled" else "Disabled",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit")
                }
                if (isSyncing) {
                    Box(modifier = Modifier.size(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                } else if (subscription.enabled) {
                    IconButton(onClick = onSyncNow, modifier = Modifier.size(40.dp)) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Sync now")
                    }
                }
                IconButton(onClick = onRemove, modifier = Modifier.size(40.dp)) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Remove",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}