package com.ateam.schedulizer

import android.app.Application
import android.content.ClipData
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.toClipEntry
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

object ContextHolder {
    lateinit var context : Application
    fun init(context : Application) {
        ContextHolder.context = context
    }
}

actual val databaseInstance: AppDatabase by lazy {
    return@lazy (getRoomDatabase(getDatabaseBuilder(ContextHolder.context)));
}

actual object WidgetManager {
    actual fun updateWidgets() {
        CoroutineScope(Dispatchers.IO).launch {
            StreakWidget().updateAll(ContextHolder.context)
            TaskWidget().updateAll(ContextHolder.context)
        }
    }
}

actual fun clipEntryOf(text: String): ClipEntry {
    val clipData = ClipData.newPlainText("text", text)
    return clipData.toClipEntry()
}

actual suspend fun ClipEntry.readPlainText(): String? {
    val data = clipData
    if (data.itemCount == 0) return null
    return data.getItemAt(0).text?.toString()
}