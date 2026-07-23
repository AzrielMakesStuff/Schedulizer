package com.ateam.schedulizer

import android.content.Context
import android.widget.Toast
import androidx.room.Room
import androidx.room.RoomDatabase
import com.ateam.schedulizer.databaseInstance

fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("my_room.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

