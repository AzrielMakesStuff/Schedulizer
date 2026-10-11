package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ateam.schedulizer.Frequency

@Entity(tableName = "Expense")
data class Expense(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val cost: Int,
    val frequency: Frequency
)
