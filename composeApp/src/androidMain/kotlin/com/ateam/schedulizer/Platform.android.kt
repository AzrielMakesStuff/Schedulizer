package com.ateam.schedulizer

import android.app.Application

object ContextHolder {
    lateinit var context : Application
    fun init(context : Application) {
        ContextHolder.context = context
    }
}

actual val databaseInstance: AppDatabase by lazy{
    return@lazy (getRoomDatabase(getDatabaseBuilder(ContextHolder.context)));
}