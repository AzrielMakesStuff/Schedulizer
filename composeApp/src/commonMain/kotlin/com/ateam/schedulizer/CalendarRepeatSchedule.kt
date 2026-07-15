package com.ateam.schedulizer

import kotlinx.datetime.*

/** When a repeating calendar task's occurrences stop. */
sealed class RecurrenceEnd {
    data object Never : RecurrenceEnd()
    data class UntilDate(val date: LocalDate) : RecurrenceEnd()
    data class AfterOccurrences(val count: Int) : RecurrenceEnd()
}

/**
 * A repeat schedule for calendar tasks: a day-[pattern] (shared with [ReminderSchedule])
 * plus an [end] condition.
 *
 * Deliberately does NOT store its own start date or time — the owning task (e.g.
 * [com.ateam.schedulizer.entities.Task.Set]) is the single source of truth for "when
 * is the currently-active occurrence", via its own start/end fields plus an
 * `occurrenceIndex` tracking how many times the series has fired. This schedule is
 * just the recurrence rule, projected forward from wherever the caller currently is.
 */
data class CalendarRepeatSchedule(
    val pattern: RecurrencePattern,
    val end: RecurrenceEnd = RecurrenceEnd.Never
)

fun CalendarRepeatSchedule.displayName(): String {
    val endSuffix = when (val e = end) {
        is RecurrenceEnd.Never -> ""
        is RecurrenceEnd.UntilDate -> ", until ${e.date}"
        is RecurrenceEnd.AfterOccurrences -> ", ${e.count} time${if (e.count == 1) "" else "s"}"
    }
    return pattern.displayName() + endSuffix
}

/**
 * Returns the start [LocalDateTime] of the occurrence immediately following
 * [currentStart], or null if [currentStart] was the series' final occurrence per [end].
 *
 * [currentOccurrenceIndex] is [currentStart]'s 1-indexed position in the series
 * (the first-ever occurrence is index 1). The result keeps the same time-of-day as
 * [currentStart].
 */
fun CalendarRepeatSchedule.nextOccurrenceStart(
    currentStart: LocalDateTime,
    currentOccurrenceIndex: Int
): LocalDateTime? {
    if (end is RecurrenceEnd.AfterOccurrences && currentOccurrenceIndex >= end.count) return null

    val nextDate = pattern.nextFireDateAfter(currentStart.date)
    if (end is RecurrenceEnd.UntilDate && nextDate > end.date) return null

    return nextDate.atTime(currentStart.time)
}

/**
 * Returns the projected start [LocalDateTime] of the occurrence [n] positions after
 * [currentStart] (n=1 is the very next one), or null if the schedule ends before then.
 * Useful for previewing further-out occurrences without actually advancing anything.
 */
fun CalendarRepeatSchedule.occurrenceAfter(
    currentStart: LocalDateTime,
    currentOccurrenceIndex: Int,
    n: Int
): LocalDateTime? {
    require(n >= 1) { "n must be >= 1" }

    var date = currentStart.date
    var index = currentOccurrenceIndex
    repeat(n) {
        if (end is RecurrenceEnd.AfterOccurrences && index >= end.count) return null
        date = pattern.nextFireDateAfter(date)
        index++
        if (end is RecurrenceEnd.UntilDate && date > end.date) return null
    }
    return date.atTime(currentStart.time)
}

/**
 * Returns projected occurrence start date-times strictly after [currentStart], up to
 * and including [rangeEnd], respecting [end]. Useful for previewing a series' upcoming
 * occurrences on a calendar view without mutating anything. Bounded defensively at
 * 10,000 iterations to avoid a runaway loop on a malformed schedule.
 */
fun CalendarRepeatSchedule.occurrencesAfter(
    currentStart: LocalDateTime,
    currentOccurrenceIndex: Int,
    rangeEnd: LocalDate
): List<LocalDateTime> {
    val results = mutableListOf<LocalDateTime>()
    var date = currentStart.date
    var index = currentOccurrenceIndex
    var guard = 0

    while (guard < 10_000) {
        guard++
        if (end is RecurrenceEnd.AfterOccurrences && index >= end.count) break

        date = pattern.nextFireDateAfter(date)
        index++
        if (end is RecurrenceEnd.UntilDate && date > end.date) break
        if (date > rangeEnd) break

        results += date.atTime(currentStart.time)
    }

    return results
}