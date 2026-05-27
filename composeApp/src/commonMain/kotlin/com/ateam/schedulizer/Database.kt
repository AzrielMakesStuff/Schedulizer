package com.ateam.schedulizer

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ateam.schedulizer.dao.TaskDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import com.ateam.schedulizer.entities.Task
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.LocalDate

@Database(entities = [Task::class], version = 3)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    abstract fun getDao(): TaskDao

    suspend fun refresh() {
        _tasks.value = getDao().getAll()
    }

    suspend fun getTasksByDate(date: LocalDate): List<Task> =
        getDao().getTasksByDate(date)

    suspend fun createTask(task: Task) {
        getDao().upsert(task)
        refresh()
    }

    suspend fun deleteTask(task: Task) {
        getDao().delete(task.id)
        refresh()
    }
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}