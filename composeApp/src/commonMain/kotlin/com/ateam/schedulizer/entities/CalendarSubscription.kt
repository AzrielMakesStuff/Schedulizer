package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDateTime

@Entity(tableName = "CalendarSubscription")
data class CalendarSubscription(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val url: String,
    val name: String,
    val lastSyncedAt: LocalDateTime? = null,
    val autoSyncEnabled: Boolean = true
)