package com.ateam.schedulizer

import kotlinx.datetime.*

enum class TaskType { SET, POOL, DAILY }
data class Task(
    val id: Int,
    val type: TaskType,
    val title: String,
    val description: String,
    val dateOne: LocalDateTime,
    val dateTwo: LocalDateTime
)

class TaskManager {
    private val tasks = mutableListOf<Task>()

    fun addTask(task: Task) {
        
    }
}