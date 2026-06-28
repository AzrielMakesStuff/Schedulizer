package com.ateam.schedulizer

expect val databaseInstance : AppDatabase

expect object WidgetManager {
    fun updateWidgets()
}