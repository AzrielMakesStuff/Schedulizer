package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class TaskEntity(
    @PrimaryKey(true) val id: Long = 0,
    val type: String,
    val title: String,
    val description: String,
    val dateOne: String,
    val dateTwo: String
)
