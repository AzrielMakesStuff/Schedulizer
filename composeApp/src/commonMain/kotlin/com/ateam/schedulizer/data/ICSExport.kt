package com.ateam.schedulizer.data

import com.ateam.schedulizer.CalendarRepeatSchedule
import com.ateam.schedulizer.RecurrenceEnd
import com.ateam.schedulizer.RecurrencePattern
import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.*

fun CalendarRepeatSchedule.toRRule(occurrenceIndex: Int): String? {
    if (end is RecurrenceEnd.AfterOccurrences) {
        val remaining = end.count - (occurrenceIndex - 1)
        if (remaining <= 0) return null
    }

    val freqPart = when (val p = pattern) {
        is RecurrencePattern.EveryNDays -> "FREQ=DAILY;INTERVAL=${p.days}"
        is RecurrencePattern.Weekly -> "FREQ=WEEKLY;BYDAY=${p.daysOfWeek.sorted().joinToString(",") { icsDay(it) }}"
        is RecurrencePattern.Fortnightly -> "FREQ=WEEKLY;INTERVAL=2;BYDAY=${icsDay(p.dayOfWeek)}"
        is RecurrencePattern.MonthlyByDate -> "FREQ=MONTHLY;BYMONTHDAY=${p.dayOfMonth}"
        is RecurrencePattern.MonthlyByWeekday -> "FREQ=MONTHLY;BYDAY=${p.ordinal}${icsDay(p.dayOfWeek)}"
        is RecurrencePattern.YearlyByDate -> "FREQ=YEARLY;BYMONTH=${p.month};BYMONTHDAY=${p.day}"
        is RecurrencePattern.YearlyByWeekday -> "FREQ=YEARLY;BYMONTH=${p.month};BYDAY=${p.ordinal}${icsDay(p.dayOfWeek)}"
    }

    val endPart = when (val e = end) {
        is RecurrenceEnd.Never -> ""
        is RecurrenceEnd.UntilDate -> ";UNTIL=${e.date.toIcsUntil()}"
        is RecurrenceEnd.AfterOccurrences -> ";COUNT=${e.count - (occurrenceIndex - 1)}"
    }

    return freqPart + endPart
}

private fun icsDay(isoDayOfWeek: Int) =
    listOf("MO", "TU", "WE", "TH", "FR", "SA", "SU")[isoDayOfWeek - 1]

/** RRULE UNTIL as end-of-day UTC on the given date. */
private fun LocalDate.toIcsUntil(): String {
    val y = year.toString().padStart(4, '0')
    val m = monthNumber.toString().padStart(2, '0')
    val d = dayOfMonth.toString().padStart(2, '0')
    return "${y}${m}${d}T235959Z"
}

private fun LocalDateTime.toIcsDateTimeUtc(zone: TimeZone = TimeZone.currentSystemDefault()): String {
    val utc = this.toInstant(zone).toLocalDateTime(TimeZone.UTC)
    val y = utc.year.toString().padStart(4, '0')
    val mo = utc.monthNumber.toString().padStart(2, '0')
    val d = utc.dayOfMonth.toString().padStart(2, '0')
    val h = utc.hour.toString().padStart(2, '0')
    val mi = utc.minute.toString().padStart(2, '0')
    val s = utc.second.toString().padStart(2, '0')
    return "${y}${mo}${d}T${h}${mi}${s}Z"
}

private fun icsEscape(text: String): String =
    text.replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\n", "\\n")

fun Task.Set.toIcsEvent(dtStamp: LocalDateTime = start): String {
    val uid = "schedulizer-set-$id@ateam"
    val dtStart = start.toIcsDateTimeUtc()
    val dtEnd = end?.toIcsDateTimeUtc()
    val rrule = repeatFrequency?.toRRule(occurrenceIndex)

    val lines = mutableListOf(
        "BEGIN:VEVENT",
        "UID:$uid",
        "DTSTAMP:${dtStamp.toIcsDateTimeUtc()}",
        "DTSTART:$dtStart"
    )
    if (dtEnd != null) lines += "DTEND:$dtEnd"
    if (rrule != null) lines += "RRULE:$rrule"
    lines += "SUMMARY:${icsEscape(title)}"
    if (description.isNotBlank()) lines += "DESCRIPTION:${icsEscape(description)}"
    lines += "END:VEVENT"

    return lines.joinToString("\r\n")
}

fun buildIcsCalendar(events: List<String>, prodId: String = "-//ATeam//Schedulizer//EN"): String {
    val lines = mutableListOf(
        "BEGIN:VCALENDAR",
        "VERSION:2.0",
        "PRODID:$prodId",
        "CALSCALE:GREGORIAN"
    )
    lines += events
    lines += "END:VCALENDAR"
    return lines.joinToString("\r\n")
}