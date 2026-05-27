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
import com.ateam.schedulizer.entities.TaskType
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Database(entities = [Task::class], version = 1)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    private var _tasks : List<Task> = emptyList()
    val tasks : List<Task> get() = _tasks
    abstract fun getDao(): TaskDao

    suspend fun getAllTasks() {
        _tasks = getDao().getAll()
    }

    suspend fun createTask(task : Task) {
        getAllTasks()
        getDao().upsert(task)
        getAllTasks()
    }

    suspend fun deleteTask(task: Task) {
        getAllTasks()
        getDao().delete(task.id)
        getAllTasks()
    }

    suspend fun getTasksByDate(date : LocalDate) : List<Task> = getDao().getTasksByDate(date)
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