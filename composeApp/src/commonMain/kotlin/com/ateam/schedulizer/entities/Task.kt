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
    val title: String,
    val description: String,
    val dateOne: LocalDateTime,
    val dateTwo: LocalDateTime?
)

class TaskManager {
    private val tasks = mutableListOf<Task>()

    fun addTask(task: Task) {
        
    }
}