package com.ateam.schedulizer

import androidx.compose.ui.platform.ClipEntry

expect val databaseInstance : AppDatabase

expect object WidgetManager {
    fun updateWidgets()
}

expect fun clipEntryOf(text: String): ClipEntry

expect suspend fun ClipEntry.readPlainText(): String?