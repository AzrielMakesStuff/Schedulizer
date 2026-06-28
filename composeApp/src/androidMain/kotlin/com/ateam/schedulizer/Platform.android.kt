package com.ateam.schedulizer

import android.app.Application
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

actual val databaseInstance: AppDatabase by lazy{
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