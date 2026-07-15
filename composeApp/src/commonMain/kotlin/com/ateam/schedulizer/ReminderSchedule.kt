package com.ateam.schedulizer

import kotlinx.datetime.*

/**
 * How often a reminder fires, and at what time of day.
 *
 * Day-pattern math (which dates this fires on) lives in [RecurrencePattern] and is
 * shared with [CalendarRepeatSchedule]; this class adds the time-of-day and the
 * non-recurring [None] case on top.
 */
sealed class ReminderSchedule {
    abstract val time: LocalTime

    data class EveryNDays(
        val days: Int,
        val anchor: LocalDate,
        override val time: LocalTime
    ) : ReminderSchedule()

    /** [dayOfWeek] 1 = Monday - 7 = Sunday */
    data class Weekly(val dayOfWeek: Int, override val time: LocalTime) : ReminderSchedule()

    /** [dayOfWeek] 1-7. [weekParity] 0 or 1. */
    data class Fortnightly(
        val dayOfWeek: Int,
        val weekParity: Int,
        override val time: LocalTime
    ) : ReminderSchedule()

    /** Clamped to last day when the month is shorter. */
    data class MonthlyByDate(val dayOfMonth: Int, override val time: LocalTime) : ReminderSchedule()

    /** [ordinal] 1-4; -1 = last. [dayOfWeek] 1-7. */
    data class MonthlyByWeekday(
        val ordinal: Int,
        val dayOfWeek: Int,
        override val time: LocalTime
    ) : ReminderSchedule()

    /** No reminders. */
    data object None : ReminderSchedule() {
        override val time: LocalTime = LocalTime(5, 0) // unused for scheduling; kept as a sane default
    }
}

/** Maps to the shared day-pattern representation; null for [ReminderSchedule.None]. */
fun ReminderSchedule.toPattern(): RecurrencePattern? = when (this) {
    is ReminderSchedule.EveryNDays -> RecurrencePattern.EveryNDays(days, anchor)
    is ReminderSchedule.Weekly -> RecurrencePattern.Weekly(setOf(dayOfWeek))
    is ReminderSchedule.Fortnightly -> RecurrencePattern.Fortnightly(dayOfWeek, weekParity)
    is ReminderSchedule.MonthlyByDate -> RecurrencePattern.MonthlyByDate(dayOfMonth)
    is ReminderSchedule.MonthlyByWeekday -> RecurrencePattern.MonthlyByWeekday(ordinal, dayOfWeek)
    is ReminderSchedule.None -> null
}

fun ReminderSchedule.displayName(): String {
    val pattern = toPattern() ?: return "No reminders"
    val t = " at ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"
    return pattern.displayName() + t
}

/**
 * Returns the next date on or after [from] that this schedule's day-pattern fires.
 * Does NOT consider time-of-day or "now" — use [nextFireDateTime] for that.
 * Returns null for [ReminderSchedule.None].
 */
fun ReminderSchedule.nextFireDate(from: LocalDate): LocalDate? = toPattern()?.nextFireDate(from)

/**
 * Returns the next full [LocalDateTime] this schedule fires, strictly after [now].
 * Combines the day-pattern with the schedule's own [ReminderSchedule.time].
 * Returns null for [ReminderSchedule.None].
 */
fun ReminderSchedule.nextFireDateTime(now: LocalDateTime): LocalDateTime? {
    val pattern = toPattern() ?: return null

    var candidateDate = pattern.nextFireDate(now.date)
    var candidateDateTime = candidateDate.atTime(time)

    if (candidateDateTime <= now) {
        candidateDate = pattern.nextFireDate(now.date.plus(1, DateTimeUnit.DAY))
        candidateDateTime = candidateDate.atTime(time)
    }

    return candidateDateTime
}