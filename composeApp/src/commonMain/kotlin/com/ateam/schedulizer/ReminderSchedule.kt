package com.ateam.schedulizer

import kotlinx.datetime.*

/**
 * How often a reminder fires.
 */
sealed class ReminderSchedule {
    data class EveryNDays(val days: Int) : ReminderSchedule()

    /**
     * Weekly - fires on a specific day of the week.
     * [dayOfWeek] 1 = Monday - 7 = Sunday
     */
    data class Weekly(val dayOfWeek: Int) : ReminderSchedule()

    /**
     * Fortnightly - same as weekly but skips alternate weeks.
     * [dayOfWeek] 1-7.
     * [weekParity] 0 or 1 - which of the two alternating weeks to fire on.
     */
    data class Fortnightly(val dayOfWeek: Int, val weekParity: Int) : ReminderSchedule()

    /**
     * Monthly by day of month, e.g. every 15th.
     * Clamped to last day when the month is shorter.
     */
    data class MonthlyByDate(val dayOfMonth: Int) : ReminderSchedule()

    /**
     * Monthly by ordinal weekday, e.g.: "2nd Tuesday".
     * [ordinal] 1-4; -1 = last.
     * [dayOfWeek] 1-7.
     */
    data class MonthlyByWeekday(val ordinal: Int, val dayOfWeek: Int) : ReminderSchedule()

    /** No reminders. */
    data object None : ReminderSchedule()
}

fun ReminderSchedule.displayName(): String = when (this) {
    is ReminderSchedule.EveryNDays -> if (days == 1) "Daily" else "Every $days days"
    is ReminderSchedule.Weekly -> "Weekly (${isoDayName(dayOfWeek)})"
    is ReminderSchedule.Fortnightly -> "Fortnightly (${isoDayName(dayOfWeek)})"
    is ReminderSchedule.MonthlyByDate -> "Monthly on the ${ordinalSuffix(dayOfMonth)}"
    is ReminderSchedule.MonthlyByWeekday -> {
        val ord = if (ordinal == -1) "last" else ordinalSuffix(ordinal)
        "Monthly — $ord ${isoDayName(dayOfWeek)}"
    }
    is ReminderSchedule.None -> "No reminders"
}

private fun isoDayName(iso: Int) = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[iso - 1]

private fun ordinalSuffix(n: Int) = "$n" + when {
    n in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

/**
 * Returns the next date on or after [from] that this schedule fires.
 * Returns null for [ReminderSchedule.None].
 */
fun ReminderSchedule.nextFireDate(from: LocalDate): LocalDate? = when (this) {
    is ReminderSchedule.None -> null
    is ReminderSchedule.EveryNDays -> from
    is ReminderSchedule.Weekly -> {
        val todayIso = from.dayOfWeek.isoDayNumber // Mon=1 - Sun=7
        val daysAhead = (dayOfWeek - todayIso + 7) % 7
        from.plus(daysAhead.toLong(), DateTimeUnit.DAY)
    }

    is ReminderSchedule.Fortnightly -> {
        val todayIso = from.dayOfWeek.isoDayNumber
        val daysAhead = (dayOfWeek - todayIso + 7) % 7
        var candidate = from.plus(daysAhead.toLong(), DateTimeUnit.DAY)
        while (candidate.dayOfWeek.isoDayNumber.let { true } && isoWeekNumber(candidate) % 2 != weekParity) {
            candidate = candidate.plus(7, DateTimeUnit.DAY)
        }
        candidate
    }

    is ReminderSchedule.MonthlyByDate -> {
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

    is ReminderSchedule.MonthlyByWeekday -> {
        nthWeekdayOfMonth(from.year, from.month.number, ordinal, dayOfWeek)
            ?.takeIf { it >= from }
            ?: run {
                val next = from.plus(1, DateTimeUnit.MONTH)
                nthWeekdayOfMonth(next.year, next.month.number, ordinal, dayOfWeek)
            }
    }
}

private fun daysInMonth(year: Int, month: Int): Int =
    LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day

/**
 * Returns the [ordinal]-th [isoDayOfWeek] in the given month,
 * or the *last* occurrence when [ordinal] == -1.
 */
private fun nthWeekdayOfMonth(year: Int, month: Int, ordinal: Int, isoDayOfWeek: Int): LocalDate? {
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

/** ISO week number */
private fun isoWeekNumber(date: LocalDate): Int {
    val thursday = date.plus((4 - date.dayOfWeek.isoDayNumber).toLong(), DateTimeUnit.DAY)
    val jan1 = LocalDate(thursday.year, 1, 1)
    return ((thursday.toEpochDays() - jan1.toEpochDays()) / 7 + 1).toInt()
}