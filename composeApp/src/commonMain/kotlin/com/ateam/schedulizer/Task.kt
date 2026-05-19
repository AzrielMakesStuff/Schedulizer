package com.ateam.schedulizer

import kotlinx.datetime.*

enum class TaskType { SET, POOL, DAILY }
data class Task(
    val id: Long,
    val type: TaskType,
     val title: String,
    val description: String,
    val dateOne: LocalDateTime,
    val dateTwo: LocalDateTime
) {
    init {

    }
}

class TaskManager {
    private val tasks = mutableListOf<Task>()


}