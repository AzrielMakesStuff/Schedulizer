package com.ateam.schedulizer

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.number
import kotlinx.datetime.plus

/**
 * Shared by [ReminderSchedule] and [CalendarRepeatSchedule] so the date math lives in exactly one place.
 */
sealed class RecurrencePattern {

    data class EveryNDays(val days: Int, val anchor: LocalDate) : RecurrencePattern()

    /** [daysOfWeek] each 1 = Monday - 7 = Sunday. Fires on every listed day, every week. */
    data class Weekly(val daysOfWeek: Set<Int>) : RecurrencePattern() {
        init { require(daysOfWeek.isNotEmpty()) { "Weekly needs at least one day" } }
    }

    /** [dayOfWeek] 1-7. [weekParity] 0 or 1. */
    data class Fortnightly(val dayOfWeek: Int, val weekParity: Int) : RecurrencePattern()

    /** Clamped to last day when the month is shorter. */
    data class MonthlyByDate(val dayOfMonth: Int) : RecurrencePattern()

    /** [ordinal] 1-4; -1 = last. [dayOfWeek] 1-7. */
    data class MonthlyByWeekday(val ordinal: Int, val dayOfWeek: Int) : RecurrencePattern() {
        init { require(ordinal in setOf(1, 2, 3, 4, -1)) { "ordinal must be 1-4 or -1, was $ordinal" } }
    }

    /** [month] 1-12, [day] 1-31 (clamped to the month's last day, e.g. Feb 29 in a non-leap year). */
    data class YearlyByDate(val month: Int, val day: Int) : RecurrencePattern()

    /** [month] 1-12. [ordinal] 1-4; -1 = last. [dayOfWeek] 1-7. E.g. "3rd Thursday of November". */
    data class YearlyByWeekday(val month: Int, val ordinal: Int, val dayOfWeek: Int) : RecurrencePattern() {
        init { require(ordinal in setOf(1, 2, 3, 4, -1)) { "ordinal must be 1-4 or -1, was $ordinal" } }
    }
}

/** Short label for this pattern, e.g. "Every 3 days", "Weekly (Mon, Wed)". Does not include a time. */
fun RecurrencePattern.displayName(): String = when (this) {
    is RecurrencePattern.EveryNDays -> if (days == 1) "Daily" else "Every $days days"
    is RecurrencePattern.Weekly -> "Weekly (${daysOfWeek.sorted().joinToString(", ") { isoDayName(it) }})"
    is RecurrencePattern.Fortnightly -> "Fortnightly (${isoDayName(dayOfWeek)})"
    is RecurrencePattern.MonthlyByDate -> "Monthly on the ${ordinalSuffix(dayOfMonth)}"
    is RecurrencePattern.MonthlyByWeekday -> {
        val ord = if (ordinal == -1) "last" else ordinalSuffix(ordinal)
        "Monthly - $ord ${isoDayName(dayOfWeek)}"
    }
    is RecurrencePattern.YearlyByDate -> "Annually on ${monthName(month)} ${ordinalSuffix(day)}"
    is RecurrencePattern.YearlyByWeekday -> {
        val ord = if (ordinal == -1) "last" else ordinalSuffix(ordinal)
        "Annually - $ord ${isoDayName(dayOfWeek)} of ${monthName(month)}"
    }
}

/**
 * The [RecurrencePattern.Fortnightly.weekParity] value whose "on" weeks include the
 * ISO week containing [date].
 */
fun fortnightlyWeekParityFor(date: LocalDate): Int {
    var reference = date
    while (isoWeekNumber(reference) != 1) {
        reference = reference.minus(7, DateTimeUnit.DAY)
    }
    val weeksBetween = (date.toEpochDays() - reference.toEpochDays()) / 7
    return (weeksBetween % 2).toInt()
}

/**
 * Returns the next date on or after [from] that this pattern fires.
 * Every pattern here fires eventually, so this is never null (unlike
 * [ReminderSchedule], which also has the non-recurring [ReminderSchedule.None] case).
 */
fun RecurrencePattern.nextFireDate(from: LocalDate): LocalDate = when (this) {
    is RecurrencePattern.EveryNDays -> {
        val diff = from.toEpochDays() - anchor.toEpochDays()
        val remainder = ((diff % days) + days) % days
        if (remainder == 0L) from else from.plus(days - remainder, DateTimeUnit.DAY)
    }

    is RecurrencePattern.Weekly -> {
        val todayIso = from.dayOfWeek.isoDayNumber
        val minAhead = daysOfWeek.minOf { (it - todayIso + 7) % 7 }
        from.plus(minAhead.toLong(), DateTimeUnit.DAY)
    }

    is RecurrencePattern.Fortnightly -> {
        val todayIso = from.dayOfWeek.isoDayNumber
        val daysAhead = (dayOfWeek - todayIso + 7) % 7
        var candidate = from.plus(daysAhead.toLong(), DateTimeUnit.DAY)

        if (fortnightlyWeekParityFor(candidate) != weekParity) {
            candidate = candidate.plus(7, DateTimeUnit.DAY)
        }
        candidate
    }

    is RecurrencePattern.MonthlyByDate -> {
        val maxDay = daysInMonth(from.year, from.month.number)
        val target = minOf(dayOfMonth, maxDay)
        if (from.day <= target)
            LocalDate(from.year, from.month.number, target)
        else {
            val next = from.plus(1, DateTimeUnit.MONTH)
            val maxNext = daysInMonth(next.year, next.month.number)
            LocalDate(next.year, next.month.number, minOf(dayOfMonth, maxNext))
        }
    }

    is RecurrencePattern.MonthlyByWeekday -> {
        // Every month has at least 4 occurrences of each weekday
        nthWeekdayOfMonth(from.year, from.month.number, ordinal, dayOfWeek)
            ?.takeIf { it >= from }
            ?: run {
                val next = from.plus(1, DateTimeUnit.MONTH)
                nthWeekdayOfMonth(next.year, next.month.number, ordinal, dayOfWeek)!!
            }
    }

    is RecurrencePattern.YearlyByDate -> {
        val maxDay = daysInMonth(from.year, month)
        val target = minOf(day, maxDay)
        val candidate = LocalDate(from.year, month, target)
        if (candidate >= from) candidate
        else {
            val nextYear = from.year + 1
            val maxNextYear = daysInMonth(nextYear, month)
            LocalDate(nextYear, month, minOf(day, maxNextYear))
        }
    }

    is RecurrencePattern.YearlyByWeekday -> {
        // ordinal in {1,2,3,4,-1} always resolves within the target month of this
        // year or (guaranteed) the same month next year.
        nthWeekdayOfMonth(from.year, month, ordinal, dayOfWeek)
            ?.takeIf { it >= from }
            ?: nthWeekdayOfMonth(from.year + 1, month, ordinal, dayOfWeek)!!
    }
}

/** Returns the date this pattern fires strictly after [from] (excludes [from] itself). */
fun RecurrencePattern.nextFireDateAfter(from: LocalDate): LocalDate {
    val candidate = nextFireDate(from)
    return if (candidate > from) candidate else nextFireDate(from.plus(1, DateTimeUnit.DAY))
}

internal fun isoDayName(iso: Int) = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[iso - 1]

internal fun monthName(month: Int) = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
)[month - 1]

internal fun ordinalSuffix(n: Int) = "$n" + when {
    n in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

internal fun daysInMonth(year: Int, month: Int): Int =
    LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day

internal fun nthWeekdayOfMonth(year: Int, month: Int, ordinal: Int, isoDayOfWeek: Int): LocalDate? {
    val first = LocalDate(year, month, 1)
    val firstIso = first.dayOfWeek.isoDayNumber
    val daysToTarget = (isoDayOfWeek - firstIso + 7) % 7
    val firstOccurrence = first.plus(daysToTarget.toLong(), DateTimeUnit.DAY)

    return if (ordinal == -1) {
        var last = firstOccurrence
        var next = last.plus(7, DateTimeUnit.DAY)
        while (next.month.number == month) { last = next; next = next.plus(7, DateTimeUnit.DAY) }
        last
    } else {
        val result = firstOccurrence.plus(((ordinal - 1) * 7).toLong(), DateTimeUnit.DAY)
        if (result.month.number == month) result else null
    }
}

internal fun isoWeekNumber(date: LocalDate): Int {
    val thursday = date.plus((4 - date.dayOfWeek.isoDayNumber).toLong(), DateTimeUnit.DAY)
    val jan1 = LocalDate(thursday.year, 1, 1)
    return ((thursday.toEpochDays() - jan1.toEpochDays()) / 7 + 1).toInt()
}