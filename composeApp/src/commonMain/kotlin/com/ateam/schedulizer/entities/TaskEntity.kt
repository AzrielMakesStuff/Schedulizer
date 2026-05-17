package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ateam.schedulizer.TaskType
import kotlinx.datetime.LocalDateTime

@Entity
data class TaskEntity(
    @PrimaryKey(true) val id: Long = 0,
    val type: TaskType,
    val title: String,
    val description: String,
    val dateOne: LocalDateTime,
    val dateTwo: LocalDateTime
)
