package com.ateam.schedulizer.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.LocalDate

@Dao
interface TaskDao {
    @Upsert
    suspend fun upsert(vararg task : Task)

    @Query("DELETE FROM Task where id = :id")
    suspend fun delete(id : Long)

    @Query("SELECT * FROM Task WHERE date(dateTwo) = :date")
    suspend fun getTasksByDate(date : LocalDate) : List<Task>

    @Query("SELECT * FROM Task")
    suspend fun getAll() : List<Task>

    @Query("Select * FROM Task where id = :id")
    suspend fun get(id : Long) : Task
}
