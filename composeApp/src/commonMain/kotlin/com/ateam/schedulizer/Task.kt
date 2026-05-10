package com.ateam.schedulizer

import kotlinx.datetime.*

enum class TaskType { SET, POOL, DAILY }
data class Task(
    val id: Int,
    val type: TaskType,
    val title: String,
    val description: String,
    val start: LocalDateTime,
    val end: LocalDateTime
)

class TaskManager {
    private val tasks = mutableListOf<Task>()


}