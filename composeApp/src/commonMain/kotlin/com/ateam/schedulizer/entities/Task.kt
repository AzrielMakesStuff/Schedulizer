package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.*

enum class TaskType { SET, POOL, DAILY }
fun TaskType.displayName(): String = "${name.lowercase().replaceFirstChar { it.uppercase() }} Task"

@Entity
data class Task(
    @PrimaryKey(true) val id: Long = 0,
    val type: TaskType,
    var title: String,
    var description: String,
    val completed: Boolean = false,
    val dateOne: LocalDateTime,
    val dateTwo: LocalDateTime?
)