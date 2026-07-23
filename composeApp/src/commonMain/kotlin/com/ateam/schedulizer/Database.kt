package com.ateam.schedulizer

import androidx.room.AutoMigration
import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.ateam.schedulizer.dao.TaskDao
import com.ateam.schedulizer.entities.Settings
import com.ateam.schedulizer.entities.Streak
import com.ateam.schedulizer.entities.Task
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

@DeleteColumn(
    tableName = "Settings",
    columnName = "notificationTime"
)
class Migration10 : AutoMigrationSpec

@Database(
    entities = [
        Task.Set::class,
        Task.Pool::class,
        Task.Daily::class,
        Streak::class,
        Settings::class
    ],
    version = 11,
    autoMigrations = [
        AutoMigration(from = 9, to = 10, spec = Migration10::class)
    ]
)
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
        if (dailies.isEmpty()) {
            taskDao().upsert(Streak(dateInitiated = today(), lostDate = today()))
            return StreakStatus(lost = true, completedToday = false)
        }

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

    suspend fun getTask(id: Long, type: String) = when (type) {
        "Set" -> taskDao().getSet(id)
        "Pool" -> taskDao().getPool(id)
        "Daily" -> taskDao().getDaily(id)
        else -> null
    }

    suspend fun refresh() {
        _tasks.value = getAll()
        _streak.value = (taskDao().getStreak() ?: Streak().also { taskDao().upsert(it) }).streakLength
        _settings.value = taskDao().getSettings() ?: Settings()
        WidgetManager.updateWidgets() // Refresh widgets
        NotificationSystem.recheckNotifPerm()
    }

    suspend fun saveSettings(settings: Settings) {
        taskDao().upsert(settings)
        _settings.value = settings
        rescheduleAllNotifications()
        refresh()
    }

    /**
     * [upsertTask] takes a task, reschedules the notifications
     * and upserts it to the correct dao database.
     *
     * [task] is the new task to upsert.
     */
    suspend fun upsertTask(task: Task) {
        NotificationSystem.clearNotification(task)

        val shouldSchedule = when (task) {
            is Task.Daily -> true
            is Task.Set, is Task.Pool -> !task.isComplete()
        }

        if (shouldSchedule) {
            val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
            val nextDateTime = task.reminderSchedule.nextFireDateTime(now)
            if (nextDateTime != null) {
                NotificationSystem.scheduleNotification(task.title, task.description, nextDateTime, task)
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
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        tasks.forEach { task ->
            NotificationSystem.clearNotification(task)

            val shouldSchedule = when (task) {
                is Task.Daily -> true
                is Task.Set, is Task.Pool -> !task.isComplete()
            }
            if (!shouldSchedule) return@forEach

            val nextDateTime = task.reminderSchedule.nextFireDateTime(now) ?: return@forEach
            NotificationSystem.scheduleNotification(task.title, task.description, nextDateTime, task)
        }
    }
}

val MIGRATION11 = object : Migration(10, 11) {
    override fun migrate(connection: SQLiteConnection) {
        connection.execSQL("ALTER TABLE SetTask ADD COLUMN repeatFrequency TEXT DEFAULT NULL")
        connection.execSQL("ALTER TABLE SetTask ADD COLUMN occurrenceIndex INTEGER NOT NULL DEFAULT 1")
    }
}

fun getRoomDatabase(
    builder: RoomDatabase.Builder<AppDatabase>
): AppDatabase {
    return builder
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION11)
        .fallbackToDestructiveMigrationOnDowngrade(true)
        .build()
}

// The Room compiler generates the actual implementations.
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

