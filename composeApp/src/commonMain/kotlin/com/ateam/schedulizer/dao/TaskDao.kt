package com.ateam.schedulizer.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ateam.schedulizer.entities.Task

@Dao
interface SetTaskDao {
    @Upsert
    suspend fun upsert(task: Task.Set)

    @Query("DELETE FROM SetTask WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM SetTask")
    suspend fun getAll(): List<Task.Set>

    @Query("SELECT * FROM SetTask WHERE id = :id")
    suspend fun get(id: Long): Task.Set}

@Dao
interface PoolTaskDao {
    @Upsert
    suspend fun upsert(task: Task.Pool)

    @Query("DELETE FROM PoolTask WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM PoolTask")
    suspend fun getAll(): List<Task.Pool>

    @Query("SELECT * FROM PoolTask WHERE id = :id")
    suspend fun get(id: Long): Task.Pool
}

@Dao
interface DailyTaskDao {
    @Upsert
    suspend fun upsert(task: Task.Daily)

    @Query("DELETE FROM DailyTask WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM DailyTask")
    suspend fun getAll(): List<Task.Daily>

    @Query("SELECT * FROM DailyTask WHERE id = :id")
    suspend fun get(id: Long): Task.Daily
}
