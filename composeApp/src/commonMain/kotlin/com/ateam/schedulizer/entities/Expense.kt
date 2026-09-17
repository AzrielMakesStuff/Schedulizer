package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.ateam.schedulizer.Frequency
import com.ateam.schedulizer.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

@Entity(tableName = "Expense")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name : String,
    val cost : Int,
    val frequency: Frequency
)