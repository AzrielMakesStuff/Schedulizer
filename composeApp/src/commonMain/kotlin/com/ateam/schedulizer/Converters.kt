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
}