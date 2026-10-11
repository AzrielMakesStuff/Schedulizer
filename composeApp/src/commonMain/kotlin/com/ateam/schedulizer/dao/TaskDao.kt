package com.ateam.schedulizer.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ateam.schedulizer.entities.CalendarSubscription
import com.ateam.schedulizer.entities.Expense
import com.ateam.schedulizer.entities.Settings
import com.ateam.schedulizer.entities.Streak
import com.ateam.schedulizer.entities.Task

@Dao
interface TaskDao {
    // Set tasks
    @Upsert
    suspend fun upsert(task: Task.Set)
    @Query("DELETE FROM SetTask WHERE id = :id")
    suspend fun deleteSet(id: Long)
    @Query("SELECT * FROM SetTask")
    suspend fun getAllSet(): List<Task.Set>
    @Query("SELECT * FROM SetTask WHERE id = :id")
    suspend fun getSet(id: Long): Task.Set

    // Pool tasks
    @Upsert
    suspend fun upsert(task: Task.Pool)
    @Query("DELETE FROM PoolTask WHERE id = :id")
    suspend fun deletePool(id: Long)
    @Query("SELECT * FROM PoolTask")
    suspend fun getAllPool(): List<Task.Pool>
    @Query("SELECT * FROM PoolTask WHERE id = :id")
    suspend fun getPool(id: Long): Task.Pool

    // Daily tasks
    @Upsert
    suspend fun upsert(task: Task.Daily)
    @Query("DELETE FROM DailyTask WHERE id = :id")
    suspend fun deleteDaily(id: Long)
    @Query("SELECT * FROM DailyTask")
    suspend fun getAllDaily(): List<Task.Daily>
    @Query("SELECT * FROM DailyTask WHERE id = :id")
    suspend fun getDaily(id: Long): Task.Daily

    // Streak
    @Upsert
    suspend fun upsert(streak: Streak)
    @Query("SELECT * FROM Streak WHERE id = 1")
    suspend fun getStreak(): Streak?

    // Settings
    @Upsert
    suspend fun upsert(settings: Settings)
    @Query("SELECT * FROM Settings WHERE id = 1")
    suspend fun getSettings(): Settings?

    // Calendar Subscriptions
    @Upsert
    suspend fun upsert(subscription: CalendarSubscription)
    @Query("DELETE FROM CalendarSubscription WHERE id = :id")
    suspend fun deleteSubscription(id: Long)
    @Query("SELECT * FROM CalendarSubscription")
    suspend fun getAllSubscriptions(): List<CalendarSubscription>
    @Query("SELECT * FROM CalendarSubscription WHERE id = :id")
    suspend fun getSubscription(id: Long): CalendarSubscription?
    @Query("SELECT * FROM SetTask WHERE subscriptionId = :subscriptionId")
    suspend fun getSetTasksForSubscription(subscriptionId: Long): List<Task.Set>

    // Expenses
    @Upsert
    suspend fun upsert(expense: Expense)
    @Query("DELETE FROM Expense WHERE id = :id")
    suspend fun deleteExpense(id: Long)
    @Query("SELECT * FROM Expense")
    suspend fun getAllExpenses(): List<Expense>
}