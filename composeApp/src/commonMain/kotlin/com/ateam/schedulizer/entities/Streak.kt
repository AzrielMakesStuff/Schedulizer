package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.ateam.schedulizer.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

@Entity(tableName = "Streak")
data class Streak(
    @PrimaryKey
    val id: Long = 1,
    val dateInitiated: LocalDate = today,
    val lostDate: LocalDate? = null
) {
    @Ignore
    val streakLength = dateInitiated.daysUntil(today)
}