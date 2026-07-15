package com.ateam.schedulizer.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ateam.schedulizer.CalendarRepeatSchedule
import com.ateam.schedulizer.ReminderSchedule
import com.ateam.schedulizer.RecurrenceEnd
import com.ateam.schedulizer.nextOccurrenceStart
import com.ateam.schedulizer.occurrencesAfter
import com.ateam.schedulizer.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

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
        val repeatFrequency: CalendarRepeatSchedule? = null,
        @ColumnInfo(defaultValue = "1")
        val occurrenceIndex: Int = 1
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

    fun notificationCode(): Int {
        val tag = when (this) {
            is Set -> 1
            is Pool -> 2
            is Daily -> 3
        }
        return (id.toInt() * 4) + tag
    }
}

fun Task.Set.completeAndAdvance(completedAt: LocalDateTime): Task.Set {
    val schedule = repeatFrequency
    val newStart = schedule?.nextOccurrenceStart(start, occurrenceIndex)

    if (schedule == null || newStart == null) {
        return copy(completed = true, completionDate = completedAt)
    }

    val newEnd = end?.let { oldEnd ->
        val tz = TimeZone.currentSystemDefault()
        val duration = oldEnd.toInstant(tz) - start.toInstant(tz)
        (newStart.toInstant(tz) + duration).toLocalDateTime(tz)
    }

    return copy(
        start = newStart,
        end = newEnd,
        completed = false,
        completionDate = null,
        occurrenceIndex = occurrenceIndex + 1
    )
}

fun Task.Set.occurrencesBetween(rangeStart: LocalDate, rangeEnd: LocalDate): List<LocalDateTime> {
    val results = mutableListOf<LocalDateTime>()

    val currentOccurrenceEndDate = end?.date ?: start.date
    if (start.date <= rangeEnd && currentOccurrenceEndDate >= rangeStart) {
        results += start
    }

    repeatFrequency?.let { schedule ->
        results += schedule.occurrencesAfter(start, occurrenceIndex, rangeEnd)
            .filter { it.date >= rangeStart }
    }

    return results
}