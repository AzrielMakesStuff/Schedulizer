package com.ateam.schedulizer.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ateam.schedulizer.entities.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Insert
    suspend fun storeTasks(vararg task : TaskEntity);

    @Query("DELETE FROM TaskEntity where id = :id")

    suspend fun delete(id : Long);

    @Query("SELECT * FROM TaskEntity")
    suspend fun getAll() : List<TaskEntity>

    @Query("Select * FROM TaskEntity where id = :id")
    suspend fun get(id : Long) : TaskEntity
}
