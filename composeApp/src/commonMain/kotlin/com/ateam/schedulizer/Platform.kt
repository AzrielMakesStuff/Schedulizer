package com.ateam.schedulizer

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

import androidx.compose.ui.platform.ClipEntry

expect val databaseInstance : AppDatabase

expect object WidgetManager {
    fun updateWidgets()
}

expect suspend fun purgeData();

@Composable
fun alertDialogBuilder(title : String, description: String, confirmationRequest : () -> Unit, onDismissRequest : () -> Unit) {
    AlertDialog(
        title = {
            Text(text = title)
        },
        text = {
            Text(text = description)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    confirmationRequest()
                }
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(
                onClick = {
                    onDismissRequest()
                }
            ) {
                Text("Dismiss")
            }
        }
    )
}

expect fun clipEntryOf(text: String): ClipEntry

expect suspend fun ClipEntry.readPlainText(): String?