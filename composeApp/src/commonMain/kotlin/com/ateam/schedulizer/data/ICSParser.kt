package com.ateam.schedulizer.data

import com.ateam.schedulizer.CalendarRepeatSchedule
import com.ateam.schedulizer.RecurrenceEnd
import com.ateam.schedulizer.RecurrencePattern
import com.ateam.schedulizer.ReminderSchedule
import com.ateam.schedulizer.databaseInstance
import com.ateam.schedulizer.entities.Task
import com.ateam.schedulizer.isoWeekNumber
import kotlinx.datetime.*
import kotlinx.datetime.LocalDate
import kotlinx.datetime.number

object ICSParser {
    fun toICS(tasks: List<Task.Set>, dtStamp: LocalDateTime): String =
        buildIcsCalendar(tasks.map { it.toIcsEvent(dtStamp) })

    suspend fun fromICS(
        icsText: String,
        defaultReminderSchedule: ReminderSchedule = ReminderSchedule.None
    ): List<Task.Set> {
        val imported = mutableListOf<Task.Set>()

        for (block in extractVEvents(icsText)) {
            val props = parseProperties(block)
            val task = propsToTask(props, defaultReminderSchedule) ?: continue
            databaseInstance.upsertTask(task)
            imported += task
        }

        return imported
    }

    // ---- VCALENDAR/VEVENT splitting ----

    private fun extractVEvents(icsText: String): List<String> {
        val unfolded = unfold(icsText)
        val blocks = mutableListOf<String>()
        var current: MutableList<String>? = null

        for (line in unfolded.lines()) {
            when {
                line == "BEGIN:VEVENT" -> current = mutableListOf()
                line == "END:VEVENT" -> {
                    current?.let { blocks += it.joinToString("\n") }
                    current = null
                }
                current != null -> current.add(line)
            }
        }
        return blocks
    }

    /** RFC 5545 line unfolding: a line starting with a space/tab continues the previous line. */
    private fun unfold(text: String): String {
        val normalized = text.replace("\r\n", "\n").replace("\r", "\n")
        val result = StringBuilder()
        for (line in normalized.split("\n")) {
            if (line.startsWith(" ") || line.startsWith("\t")) {
                result.append(line.substring(1))
            } else {
                if (result.isNotEmpty()) result.append("\n")
                result.append(line)
            }
        }
        return result.toString()
    }

    // ---- property parsing ----

    private data class IcsProperty(val name: String, val params: Map<String, String>, val value: String)

    private fun parseProperties(block: String): Map<String, IcsProperty> {
        val props = mutableMapOf<String, IcsProperty>()
        for (line in block.lines()) {
            if (line.isBlank()) continue
            val colonIndex = line.indexOf(':')
            if (colonIndex == -1) continue

            val head = line.take(colonIndex)
            val value = unescape(line.substring(colonIndex + 1))

            val parts = head.split(";")
            val name = parts.first()
            val params = parts.drop(1).mapNotNull { p ->
                val eq = p.indexOf('=')
                if (eq == -1) null else p.take(eq) to p.substring(eq + 1)
            }.toMap()

            props[name] = IcsProperty(name, params, value)
        }
        return props
    }

    private fun unescape(text: String): String =
        text.replace("\\n", "\n").replace("\\N", "\n")
            .replace("\\,", ",").replace("\\;", ";").replace("\\\\", "\\")

    // ---- date/time parsing ----
    private fun parseIcsDateTime(prop: IcsProperty): LocalDateTime {
        val v = prop.value
        val isUtc = v.endsWith("Z")
        val clean = v.removeSuffix("Z")

        val local = if (clean.contains("T")) {
            val (datePart, timePart) = clean.split("T")
            val year = datePart.substring(0, 4).toInt()
            val month = datePart.substring(4, 6).toInt()
            val day = datePart.substring(6, 8).toInt()
            val hour = timePart.substring(0, 2).toInt()
            val minute = timePart.substring(2, 4).toInt()
            val second = if (timePart.length >= 6) timePart.substring(4, 6).toInt() else 0
            LocalDateTime(year, month, day, hour, minute, second)
        } else {
            // DATE-only value (e.g. an all-day event) — treat as midnight.
            LocalDateTime(clean.take(4).toInt(), clean.substring(4, 6).toInt(), clean.substring(6, 8).toInt(), 0, 0, 0)
        }

        return if (isUtc) local.toInstant(TimeZone.UTC).toLocalDateTime(TimeZone.currentSystemDefault()) else local
    }

    private fun parseIcsDate(value: String): LocalDate {
        val clean = value.removeSuffix("Z").substringBefore("T")
        return LocalDate(clean.take(4).toInt(), clean.substring(4, 6).toInt(), clean.substring(6, 8).toInt())
    }

    // ---- RRULE parsing ----

    private val isoDayFromIcs = mapOf(
        "MO" to 1, "TU" to 2, "WE" to 3, "TH" to 4, "FR" to 5, "SA" to 6, "SU" to 7
    )

    /** Parses a BYDAY entry like "MO", "2TU", or "-1FR" into (ordinal, isoDayOfWeek). Ordinal is 0 when no numeric prefix is present. */
    private fun parseByDayEntry(entry: String): Pair<Int, Int> {
        val dayCode = entry.takeLast(2)
        val ordinal = entry.dropLast(2).toIntOrNull() ?: 0
        return ordinal to (isoDayFromIcs[dayCode] ?: 1)
    }

    /**
     * Parses an RRULE value into a [RecurrencePattern] + [RecurrenceEnd], or null if
     * it's a shape this app doesn't model — see [fromICS] doc for how that's handled.
     */
    private fun parseRRule(rrule: String, dtStart: LocalDateTime): Pair<RecurrencePattern, RecurrenceEnd>? {
        val fields = rrule.split(";").mapNotNull { f ->
            val eq = f.indexOf('=')
            if (eq == -1) null else f.take(eq) to f.substring(eq + 1)
        }.toMap()

        val freq = fields["FREQ"] ?: return null
        val interval = fields["INTERVAL"]?.toIntOrNull() ?: 1
        val byDay = fields["BYDAY"]?.split(",")?.map { parseByDayEntry(it) }.orEmpty()
        val byMonthDay = fields["BYMONTHDAY"]?.toIntOrNull()
        val byMonth = fields["BYMONTH"]?.toIntOrNull()

        val end = when {
            fields["COUNT"] != null -> RecurrenceEnd.AfterOccurrences(fields.getValue("COUNT").toInt())
            fields["UNTIL"] != null -> RecurrenceEnd.UntilDate(parseIcsDate(fields.getValue("UNTIL")))
            else -> RecurrenceEnd.Never
        }

        val pattern: RecurrencePattern? = when (freq) {
            "DAILY" -> RecurrencePattern.EveryNDays(interval, dtStart.date)

            "WEEKLY" -> {
                val days = if (byDay.isNotEmpty()) byDay.map { it.second }.toSet()
                else setOf(dtStart.date.dayOfWeek.isoDayNumber)
                when (interval) {
                    1 -> RecurrencePattern.Weekly(days)
                    // Fortnightly only holds one weekday; multi-day biweekly rules keep the first.
                    2 -> RecurrencePattern.Fortnightly(days.first(), isoWeekNumber(dtStart.date) % 2)
                    else -> null // every-3-weeks and beyond aren't modeled
                }
            }

            "MONTHLY" -> when {
                byMonthDay != null -> RecurrencePattern.MonthlyByDate(byMonthDay)
                byDay.isNotEmpty() && byDay.first().first != 0 ->
                    RecurrencePattern.MonthlyByWeekday(byDay.first().first, byDay.first().second)
                else -> null
            }

            "YEARLY" -> {
                val month = byMonth ?: dtStart.date.month.number
                when {
                    byMonthDay != null -> RecurrencePattern.YearlyByDate(month, byMonthDay)
                    byDay.isNotEmpty() && byDay.first().first != 0 ->
                        RecurrencePattern.YearlyByWeekday(month, byDay.first().first, byDay.first().second)
                    byMonth != null -> RecurrencePattern.YearlyByDate(month, dtStart.date.day)
                    else -> null
                }
            }

            else -> null // HOURLY, MINUTELY, SECONDLY aren't modeled
        }

        return pattern?.let { it to end }
    }

    // ---- assembling a Task.Set ----

    private fun propsToTask(props: Map<String, IcsProperty>, defaultReminderSchedule: ReminderSchedule): Task.Set? {
        val dtStartProp = props["DTSTART"] ?: return null
        val dtStart = parseIcsDateTime(dtStartProp)
        val dtEnd = props["DTEND"]?.let { parseIcsDateTime(it) }
        val title = props["SUMMARY"]?.value ?: "Imported event"
        val description = props["DESCRIPTION"]?.value ?: ""

        // A present-but-unmappable RRULE falls through to null here, which just means
        val repeatFrequency = props["RRULE"]?.value
            ?.let { parseRRule(it, dtStart) }
            ?.let { (pattern, end) -> CalendarRepeatSchedule(pattern, end) }

        return Task.Set(
            title = title,
            description = description,
            reminderSchedule = defaultReminderSchedule,
            start = dtStart,
            end = dtEnd,
            repeatFrequency = repeatFrequency,
            occurrenceIndex = 1
        )
    }
}