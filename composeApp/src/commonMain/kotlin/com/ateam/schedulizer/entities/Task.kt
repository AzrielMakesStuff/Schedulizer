package com.ateam.schedulizer.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ateam.schedulizer.CalendarRepeatSchedule
import com.ateam.schedulizer.ReminderSchedule
import com.ateam.schedulizer.nextFireDateTime
import com.ateam.schedulizer.nextOccurrenceStart
import com.ateam.schedulizer.occurrencesAfter
import com.ateam.schedulizer.today
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

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
        val end: LocalDateTime,
        val completed: Boolean? = null, // Null means auto advance - non-completable
        val completionDate: LocalDateTime? = null, // Null means not complete yet
        val repeatFrequency: CalendarRepeatSchedule? = null, // Defaults to null for no repeat
        @ColumnInfo(defaultValue = "1")
        val occurrenceIndex: Int = 1, // An index for recurring tasks for ICS compatibility
        val preReminders: List<Duration> = emptyList(),
        val icsUid: String? = null, // ICS reference
        val subscriptionId: Long? = null // Subscription reference
    ) : Task() {
        val isCompletable: Boolean get() = completed != null

        private fun advanceOnce(): Set? {
            val schedule = repeatFrequency ?: return null
            val newStart = schedule.nextOccurrenceStart(start, occurrenceIndex) ?: return null
            val tz = TimeZone.currentSystemDefault()
            val newEnd = (newStart.toInstant(tz) + (end.toInstant(tz) - start.toInstant(tz))).toLocalDateTime(tz)

            return copy(
                start = newStart,
                end = newEnd,
                completed = completed?.let { false },
                completionDate = null,
                occurrenceIndex = occurrenceIndex + 1
            )
        }

        fun advanceToUpcomingOccurrence(now: LocalDateTime): Set {


            var current = this
            var safety = 0
            while (current.start < now && safety < 10_000) {
                current = current.advanceOnce() ?: break
                safety++
            }
            return current
        }

        fun occurrencesBetween(rangeStart: LocalDate, rangeEnd: LocalDate): List<LocalDateTime> {
            val results = mutableListOf<LocalDateTime>()

            val currentOccurrenceEndDate = end.date
            if (start.date <= rangeEnd && currentOccurrenceEndDate >= rangeStart) {
                results += start
            }

            repeatFrequency?.let { schedule ->
                results += schedule.occurrencesAfter(start, occurrenceIndex, rangeEnd)
                    .filter { it.date >= rangeStart }
            }

            return results
        }
    }

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
        is Set -> completed ?: (end.date > today())
        is Pool -> completed
    }

    fun nextNotificationTime(now: LocalDateTime, zone: TimeZone = TimeZone.currentSystemDefault()): LocalDateTime? {
        return if (this is Set) {
            val nowInstant = now.toInstant(zone)
            val startInstant = start.toInstant(zone)

            preReminders
                .map { duration -> startInstant - duration }
                .filter { it >= nowInstant }
                .minOrNull()
                ?.toLocalDateTime(zone)
        } else {
            reminderSchedule.nextFireDateTime(now)
        }
    }
}