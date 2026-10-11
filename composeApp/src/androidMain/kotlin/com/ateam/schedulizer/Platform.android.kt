package com.ateam.schedulizer

import android.app.Application
import android.content.ClipData
import android.widget.Toast
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
lateinit var privateInstance : AppDatabase
actual val databaseInstance: AppDatabase get() {
    if (!::privateInstance.isInitialized) {
            privateInstance = getRoomDatabase(getDatabaseBuilder(ContextHolder.context))
    }
   return privateInstance
}

actual object WidgetManager {
    actual fun updateWidgets() {
        CoroutineScope(Dispatchers.IO).launch {
            StreakWidget().updateAll(ContextHolder.context)
            TaskWidget().updateAll(ContextHolder.context)
        }
    }
}

actual fun clipEntryOf(text: String): Any {
    val clipData = ClipData.newPlainText("text", text)
    return clipData.toClipEntry()
}

actual suspend fun Any.readPlainText(): String? {
    val entry = this as? ClipEntry ?: return null
    val data = entry.clipData
    if (data.itemCount == 0) return null
    return data.getItemAt(0).text?.toString()
}

actual suspend fun readClipboardText(): String? = null

actual suspend fun purgeData() {
    databaseInstance.close()
    ContextHolder.context.deleteDatabase("my_room.db")
    privateInstance = getRoomDatabase(getDatabaseBuilder(ContextHolder.context));
    Toast.makeText(ContextHolder.context,"Database has been purged", Toast.LENGTH_SHORT).show()
}

actual fun supportsDynamicColor(): Boolean = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S

@androidx.compose.runtime.Composable
actual fun getDynamicColorScheme(isDark: Boolean): androidx.compose.material3.ColorScheme? {
    val context = ContextHolder.context
    return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        if (isDark) androidx.compose.material3.dynamicDarkColorScheme(context) else androidx.compose.material3.dynamicLightColorScheme(context)
    } else {
        null
    }
}