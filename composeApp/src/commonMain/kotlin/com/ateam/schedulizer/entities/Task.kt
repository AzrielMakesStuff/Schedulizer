package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ateam.schedulizer.ReminderSchedule
import com.ateam.schedulizer.today
import kotlinx.datetime.LocalDateTime

sealed class Task {
    abstract val id: Long
    abstract val title: String
    abstract val description: String
    abstract val reminderSchedule: ReminderSchedule

    fun displayName(): String = when (this) {
        is Set -> "Set Task"
        is Pool -> "Pool Task"
        is Daily -> "Daily Task"
    }

    @Entity(tableName = "SetTask")
    data class Set(
        @PrimaryKey(autoGenerate = true) override val id: Long = 0,
        override val title: String,
        override val description: String,
        override val reminderSchedule: ReminderSchedule,
        val start: LocalDateTime,
        val end: LocalDateTime?,
        val completed: Boolean = false,
        val completionDate: LocalDateTime? = null,
    ) : Task()

    @Entity(tableName = "PoolTask")
    data class Pool(
        @PrimaryKey(autoGenerate = true) override val id: Long = 0,
        override val title: String,
        override val description: String,
        override val reminderSchedule: ReminderSchedule,
        val expiry: LocalDateTime?,
        val completed: Boolean = false,
        val completionDate: LocalDateTime? = null,
    ) : Task()

    @Entity(tableName = "DailyTask")
    data class Daily(
        @PrimaryKey(autoGenerate = true) override val id: Long = 0,
        override val title: String,
        override val description: String,
        override val reminderSchedule: ReminderSchedule,
        val lastCompleted: LocalDateTime?,
    ) : Task()

    fun typeTag(): String = when (this) {
        is Daily -> "Daily"
        is Set -> "Set"
        is Pool -> "Pool"
    }

    fun isComplete(): Boolean = when (this) {
        is Daily -> lastCompleted?.date == today()
        is Set -> completed
        is Pool -> completed
    }
}