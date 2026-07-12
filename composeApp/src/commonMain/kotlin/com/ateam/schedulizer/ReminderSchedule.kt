package com.ateam.schedulizer

import kotlinx.datetime.*

/**
 * How often a reminder fires, and at what time of day.
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

fun ReminderSchedule.displayName(): String {
    val t = timeSuffix()
    return when (this) {
        is ReminderSchedule.EveryNDays -> (if (days == 1) "Daily" else "Every $days days") + t
        is ReminderSchedule.Weekly -> "Weekly (${isoDayName(dayOfWeek)})$t"
        is ReminderSchedule.Fortnightly -> "Fortnightly (${isoDayName(dayOfWeek)})$t"
        is ReminderSchedule.MonthlyByDate -> "Monthly on the ${ordinalSuffix(dayOfMonth)}$t"
        is ReminderSchedule.MonthlyByWeekday -> {
            val ord = if (ordinal == -1) "last" else ordinalSuffix(ordinal)
            "Monthly — $ord ${isoDayName(dayOfWeek)}$t"
        }
        is ReminderSchedule.None -> "No reminders"
    }
}

private fun ReminderSchedule.timeSuffix(): String =
    if (this is ReminderSchedule.None) "" else " at ${time.hour.toString().padStart(2, '0')}:${time.minute.toString().padStart(2, '0')}"

private fun isoDayName(iso: Int) = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")[iso - 1]

private fun ordinalSuffix(n: Int) = "$n" + when {
    n in 11..13 -> "th"
    n % 10 == 1 -> "st"
    n % 10 == 2 -> "nd"
    n % 10 == 3 -> "rd"
    else -> "th"
}

/**
 * Returns the next date on or after [from] that this schedule's day-pattern fires.
 * Does NOT consider time-of-day or "now" — use [nextFireDateTime] for that.
 * Returns null for [ReminderSchedule.None].
 */
fun ReminderSchedule.nextFireDate(from: LocalDate): LocalDate? = when (this) {
    is ReminderSchedule.None -> null

    is ReminderSchedule.EveryNDays -> {
        val diff = from.toEpochDays() - anchor.toEpochDays()
        val remainder = ((diff % days) + days) % days
        if (remainder == 0L) from else from.plus(days - remainder, DateTimeUnit.DAY)
    }

    is ReminderSchedule.Weekly -> {
        val todayIso = from.dayOfWeek.isoDayNumber
        val daysAhead = (dayOfWeek - todayIso + 7) % 7
        from.plus(daysAhead.toLong(), DateTimeUnit.DAY)
    }

    is ReminderSchedule.Fortnightly -> {
        val todayIso = from.dayOfWeek.isoDayNumber
        val daysAhead = (dayOfWeek - todayIso + 7) % 7
        var candidate = from.plus(daysAhead.toLong(), DateTimeUnit.DAY)
        while (isoWeekNumber(candidate) % 2 != weekParity) {
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

/**
 * Returns the next full [LocalDateTime] this schedule fires, strictly after [now].
 * Combines the day-pattern with the schedule's own [ReminderSchedule.time].
 * Returns null for [ReminderSchedule.None].
 */
fun ReminderSchedule.nextFireDateTime(now: LocalDateTime): LocalDateTime? {
    if (this is ReminderSchedule.None) return null

    var candidateDate = nextFireDate(now.date) ?: return null
    var candidateDateTime = candidateDate.atTime(time)

    if (candidateDateTime <= now) {
        candidateDate = nextFireDate(now.date.plus(1, DateTimeUnit.DAY)) ?: return null
        candidateDateTime = candidateDate.atTime(time)
    }

    return candidateDateTime
}

private fun daysInMonth(year: Int, month: Int): Int =
    LocalDate(year, month, 1).plus(1, DateTimeUnit.MONTH).minus(1, DateTimeUnit.DAY).day

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

private fun isoWeekNumber(date: LocalDate): Int {
    val thursday = date.plus((4 - date.dayOfWeek.isoDayNumber).toLong(), DateTimeUnit.DAY)
    val jan1 = LocalDate(thursday.year, 1, 1)
    return ((thursday.toEpochDays() - jan1.toEpochDays()) / 7 + 1).toInt()
}