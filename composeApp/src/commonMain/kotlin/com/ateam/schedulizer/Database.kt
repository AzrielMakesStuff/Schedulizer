package com.ateam.schedulizer

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.ateam.schedulizer.dao.TaskDao
import com.ateam.schedulizer.entities.Settings
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
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@Database(entities = [Task.Set::class, Task.Pool::class, Task.Daily::class, Streak::class, Settings::class], version = 9)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()
    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()
    private val _settings = MutableStateFlow<Settings?>(null)
    val settings: StateFlow<Settings?> = _settings.asStateFlow()

    data class StreakStatus(val lost: Boolean, val completedToday: Boolean)

    suspend fun checkStreak(): StreakStatus {
        val dailies = taskDao().getAllDaily()
        if (dailies.isEmpty()) return StreakStatus(lost = true, completedToday = false)

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
        _settings.value = taskDao().getSettings() ?: Settings()

        WidgetManager.updateWidgets() // Refresh widgets
    }

    suspend fun saveSettings(settings: Settings) {
        taskDao().upsert(settings)
        _settings.value = settings
        rescheduleAllNotifications()
        refresh()
    }

    suspend fun createTask(task: Task) {
        NotificationSystem.clearNotification(task)
        if (!task.isComplete()) {
            val currentDate = LocalDate.now()
            val nextDate = task.reminderSchedule.nextFireDate(currentDate)
            if (nextDate != null) {
                val time = (_settings.value ?: taskDao().getSettings() ?: Settings()).notificationTime
                val nextDateTime = nextDate.atTime(time.hour, time.minute, 0)
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                if (nextDateTime > now) {
                    NotificationSystem.scheduleNotification(
                        task.title,
                        task.description,
                        nextDateTime,
                        task.id.toInt()
                    )
                }
            }
        }
        when (task) {
            is Task.Set -> taskDao().upsert(task)
            is Task.Pool -> taskDao().upsert(task)
            is Task.Daily -> taskDao().upsert(task)
        }
        refresh()
    }

    suspend fun deleteTask(task: Task) {
        NotificationSystem.clearNotification(task)
        when (task) {
            is Task.Set -> taskDao().deleteSet(task.id)
            is Task.Pool -> taskDao().deletePool(task.id)
            is Task.Daily -> taskDao().deleteDaily(task.id)
        }
        refresh()
    }

    suspend fun rescheduleAllNotifications() {
        val tasks = getAll()
        val time = (_settings.value ?: taskDao().getSettings() ?: Settings()).notificationTime
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val currentDate = LocalDate.now()
        tasks.forEach { task ->
            NotificationSystem.clearNotification(task)
            if (task.isComplete()) return@forEach
            val nextDate = task.reminderSchedule.nextFireDate(currentDate) ?: return@forEach
            val nextDateTime = nextDate.atTime(time.hour, time.minute, 0)
            if (nextDateTime <= now) return@forEach
            NotificationSystem.scheduleNotification(
                task.title,
                task.description,
                nextDateTime,
                task.id.toInt()
            )
        }
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