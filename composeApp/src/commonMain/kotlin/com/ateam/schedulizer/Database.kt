package com.ateam.schedulizer

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ateam.schedulizer.dao.TaskDao
import com.ateam.schedulizer.entities.Streak
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlinx.datetime.minus

@Database(entities = [Task.Set::class, Task.Pool::class, Task.Daily::class, Streak::class], version = 7)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()

    data class StreakStatus(val lost: Boolean, val completedToday: Boolean)

    suspend fun checkStreak(): StreakStatus {
        val dailies = taskDao().getAllDaily()
        if (dailies.isEmpty()) return StreakStatus(lost = false, completedToday = false)

        val lost = dailies.any { task ->
            task.lastCompleted == null || task.lastCompleted.date < today().minus(1, DateTimeUnit.DAY)
        }

        val completedToday = dailies.all { task ->
            task.lastCompleted?.date == today()
        }

        if (lost) {
            taskDao().upsert(Streak(dateInitiated = today(), lostDate = today()))
            println("STREAK LOST")
        }
        refresh()
        return StreakStatus(lost = lost, completedToday = completedToday)
    }

    abstract fun taskDao(): TaskDao

    suspend fun getAll(): List<Task> =
        taskDao().run { getAllSet() + getAllPool() + getAllDaily() }

    suspend fun refresh() {
        _tasks.value = getAll()
        _streak.value = (taskDao().getStreak() ?: Streak().also { taskDao().upsert(it) }).streakLength
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
            is Task.Set -> taskDao().upsert(task)
            is Task.Pool -> taskDao().upsert(task)
            is Task.Daily -> taskDao().upsert(task)
        }
        refresh()
    }

    suspend fun deleteTask(task: Task) {
        when (task) {
            is Task.Set -> taskDao().deleteSet(task.id)
            is Task.Pool -> taskDao().deletePool(task.id)
            is Task.Daily -> taskDao().deleteDaily(task.id)
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

// The Room compiler generates the actual implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}