package com.ateam.schedulizer

import androidx.room.TypeConverter
import com.kizitonwose.calendar.core.now
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime

class Converters {
    @TypeConverter fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()?.replace("T", " ")
    @TypeConverter fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it.replace(" ", "T")) }

    @TypeConverter fun fromLocalDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter fun fromLocalTime(value: LocalTime?): String? = value?.toString()
    @TypeConverter fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromSchedule(s: ReminderSchedule): String {
        fun t(time: LocalTime) = "${time.hour}:${time.minute}"
        return when (s) {
            is ReminderSchedule.None -> "none"
            is ReminderSchedule.EveryNDays -> "every_n_days|${s.days}|${s.anchor.toEpochDays()}|${t(s.time)}"
            is ReminderSchedule.Weekly -> "weekly|${s.dayOfWeek}|${t(s.time)}"
            is ReminderSchedule.Fortnightly -> "fortnightly|${s.dayOfWeek}|${s.weekParity}|${t(s.time)}"
            is ReminderSchedule.MonthlyByDate -> "monthly_date|${s.dayOfMonth}|${t(s.time)}"
            is ReminderSchedule.MonthlyByWeekday -> "monthly_weekday|${s.ordinal}|${s.dayOfWeek}|${t(s.time)}"
        }
    }

    @TypeConverter
    fun toSchedule(s: String): ReminderSchedule {
        val parts = s.split("|")
        if (parts[0] == "none") return ReminderSchedule.None

        fun parseTime(str: String?): LocalTime {
            val hm = str?.split(":")
            val h = hm?.getOrNull(0)?.toIntOrNull() ?: 12
            val m = hm?.getOrNull(1)?.toIntOrNull() ?: 0
            return LocalTime(h, m)
        }

        return try {
            when (parts[0]) {
                "every_n_days" -> ReminderSchedule.EveryNDays(
                    days = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                    anchor = parts.getOrNull(2)?.toLongOrNull()?.let { LocalDate.fromEpochDays(it.toInt()) }
                        ?: LocalDate.now(),
                    time = parseTime(parts.getOrNull(3))
                )
                "weekly" -> ReminderSchedule.Weekly(
                    dayOfWeek = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                    time = parseTime(parts.getOrNull(2))
                )
                "fortnightly" -> ReminderSchedule.Fortnightly(
                    dayOfWeek = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                    weekParity = parts.getOrNull(2)?.toIntOrNull() ?: 0,
                    time = parseTime(parts.getOrNull(3))
                )
                "monthly_date" -> ReminderSchedule.MonthlyByDate(
                    dayOfMonth = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                    time = parseTime(parts.getOrNull(2))
                )
                "monthly_weekday" -> ReminderSchedule.MonthlyByWeekday(
                    ordinal = parts.getOrNull(1)?.toIntOrNull() ?: 1,
                    dayOfWeek = parts.getOrNull(2)?.toIntOrNull() ?: 1,
                    time = parseTime(parts.getOrNull(3))
                )
                else -> ReminderSchedule.None
            }
        } catch (e: Exception) {
            ReminderSchedule.None
        }
    }

    @TypeConverter
    fun fromRepeatSchedule(s: CalendarRepeatSchedule?): String? {
        s ?: return null

        val patternPart = when (val p = s.pattern) {
            is RecurrencePattern.EveryNDays -> "every_n_days|${p.days}|${p.anchor.toEpochDays()}"
            is RecurrencePattern.Weekly -> "weekly|${p.daysOfWeek.sorted().joinToString(",")}"
            is RecurrencePattern.Fortnightly -> "fortnightly|${p.dayOfWeek}|${p.weekParity}"
            is RecurrencePattern.MonthlyByDate -> "monthly_date|${p.dayOfMonth}"
            is RecurrencePattern.MonthlyByWeekday -> "monthly_weekday|${p.ordinal}|${p.dayOfWeek}"
            is RecurrencePattern.YearlyByDate -> "yearly_date|${p.month}|${p.day}"
            is RecurrencePattern.YearlyByWeekday -> "yearly_weekday|${p.month}|${p.ordinal}|${p.dayOfWeek}"
        }

        val endPart = when (val e = s.end) {
            is RecurrenceEnd.Never -> "never"
            is RecurrenceEnd.UntilDate -> "until|${e.date.toEpochDays()}"
            is RecurrenceEnd.AfterOccurrences -> "count|${e.count}"
        }

        return "$patternPart~$endPart"
    }

    @TypeConverter
    fun toRepeatSchedule(s: String?): CalendarRepeatSchedule? {
        s ?: return null

        return try {
            val segments = s.split("~", limit = 2)
            val patternParts = segments[0].split("|")
            val endParts = segments.getOrNull(1)?.split("|") ?: listOf("never")

            val pattern: RecurrencePattern = when (patternParts[0]) {
                "every_n_days" -> RecurrencePattern.EveryNDays(
                    days = patternParts.getOrNull(1)?.toIntOrNull() ?: 1,
                    anchor = patternParts.getOrNull(2)?.toLongOrNull()?.let { LocalDate.fromEpochDays(it.toInt()) }
                        ?: LocalDate.now()
                )
                "weekly" -> RecurrencePattern.Weekly(
                    daysOfWeek = patternParts.getOrNull(1)
                        ?.split(",")
                        ?.mapNotNull { it.toIntOrNull() }
                        ?.toSet()
                        ?.takeIf { it.isNotEmpty() }
                        ?: setOf(1)
                )
                "fortnightly" -> RecurrencePattern.Fortnightly(
                    dayOfWeek = patternParts.getOrNull(1)?.toIntOrNull() ?: 1,
                    weekParity = patternParts.getOrNull(2)?.toIntOrNull() ?: 0
                )
                "monthly_date" -> RecurrencePattern.MonthlyByDate(
                    dayOfMonth = patternParts.getOrNull(1)?.toIntOrNull() ?: 1
                )
                "monthly_weekday" -> RecurrencePattern.MonthlyByWeekday(
                    ordinal = patternParts.getOrNull(1)?.toIntOrNull() ?: 1,
                    dayOfWeek = patternParts.getOrNull(2)?.toIntOrNull() ?: 1
                )
                "yearly_date" -> RecurrencePattern.YearlyByDate(
                    month = patternParts.getOrNull(1)?.toIntOrNull() ?: 1,
                    day = patternParts.getOrNull(2)?.toIntOrNull() ?: 1
                )
                "yearly_weekday" -> RecurrencePattern.YearlyByWeekday(
                    month = patternParts.getOrNull(1)?.toIntOrNull() ?: 1,
                    ordinal = patternParts.getOrNull(2)?.toIntOrNull() ?: 1,
                    dayOfWeek = patternParts.getOrNull(3)?.toIntOrNull() ?: 1
                )
                else -> return null // unrecognized pattern tag — don't guess, drop the schedule
            }

            val end: RecurrenceEnd = when (endParts[0]) {
                "never" -> RecurrenceEnd.Never
                "until" -> RecurrenceEnd.UntilDate(
                    date = endParts.getOrNull(1)?.toLongOrNull()?.let { LocalDate.fromEpochDays(it.toInt()) }
                        ?: LocalDate.now()
                )
                "count" -> RecurrenceEnd.AfterOccurrences(
                    count = endParts.getOrNull(1)?.toIntOrNull() ?: 1
                )
                else -> RecurrenceEnd.Never
            }

            CalendarRepeatSchedule(pattern, end)
        } catch (e: Exception) {
            null
        }
    }
}