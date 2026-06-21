package com.ateam.schedulizer

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ateam.schedulizer.dao.DailyTaskDao
import com.ateam.schedulizer.dao.PoolTaskDao
import com.ateam.schedulizer.dao.SetTaskDao
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime



@Database(entities = [Task.Set::class, Task.Pool::class, Task.Daily::class], version = 6)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    abstract fun setDao(): SetTaskDao
    abstract fun poolDao(): PoolTaskDao
    abstract fun dailyDao(): DailyTaskDao

    suspend fun getAll(): List<Task> = setDao().getAll() + poolDao().getAll() + dailyDao().getAll()

    suspend fun refresh() {
        _tasks.value = getAll()
    }

    suspend fun createTask(task: Task) {

        NotificationSystem.clearNotification(task)
        val currentDate = LocalDate.now()
        val nextDate = task.reminderSchedule.nextFireDate(currentDate);
        NotificationSystem.scheduleNotification(
            task.title,
            task.description,
            nextDate?.atTime(12,0,0) ?: LocalDate.now().atTime(12,0,0),
            task.id.toInt()
        )


        when (task) {
            is Task.Set -> setDao().upsert(task)
            is Task.Pool -> poolDao().upsert(task)
            is Task.Daily -> dailyDao().upsert(task)
        }

        refresh()
    }

    suspend fun deleteTask(task: Task) {
        NotificationSystem.clearNotification(task)
        when (task) {
            is Task.Set -> setDao().delete(task.id)
            is Task.Pool -> poolDao().delete(task.id)
            is Task.Daily -> dailyDao().delete(task.id)
        }

        refresh()
    }
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(true)
        .build()
}

// The Room compiler generates the `actual` implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}
