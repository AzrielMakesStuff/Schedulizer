package com.ateam.schedulizer.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.ateam.schedulizer.entities.Expense

@Dao
interface ExpenseDao {
    @Query("select * from Expense")
    suspend fun getAll() : List<Expense>

    @Upsert
    suspend fun addExpense(expense : Expense)

    @Query("DELETE FROM Expense WHERE id = :id")
    suspend fun deleteExpense(id : Long)
}