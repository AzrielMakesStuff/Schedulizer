package com.ateam.schedulizer

import android.app.Application
import android.widget.Toast
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

actual suspend fun purgeData() {
    databaseInstance.close()
    ContextHolder.context.deleteDatabase("my_room.db")
    privateInstance = getRoomDatabase(getDatabaseBuilder(ContextHolder.context));
    Toast.makeText(ContextHolder.context,"Database has been purged", Toast.LENGTH_SHORT).show()
}