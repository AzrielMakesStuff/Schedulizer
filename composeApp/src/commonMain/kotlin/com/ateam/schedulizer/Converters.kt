package com.ateam.schedulizer

import androidx.room.TypeConverter
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
    fun fromSchedule(s: ReminderSchedule): String = when (s) {
        is ReminderSchedule.None -> "none"
        is ReminderSchedule.EveryNDays -> "every_n_days:${s.days}"
        is ReminderSchedule.Weekly -> "weekly:${s.dayOfWeek}"
        is ReminderSchedule.Fortnightly -> "fortnightly:${s.dayOfWeek}:${s.weekParity}"
        is ReminderSchedule.MonthlyByDate -> "monthly_date:${s.dayOfMonth}"
        is ReminderSchedule.MonthlyByWeekday -> "monthly_weekday:${s.ordinal}:${s.dayOfWeek}"
    }

    @TypeConverter
    fun toSchedule(s: String): ReminderSchedule {
        val parts = s.split(":")

        if (parts.size < 2 && parts[0] != "none") return ReminderSchedule.None

        return try {
            when (parts[0]) {
            "every_n_days" -> ReminderSchedule.EveryNDays(parts[1].toIntOrNull() ?: 1)
            "weekly" -> ReminderSchedule.Weekly(parts[1].toIntOrNull() ?: 1)
            "fortnightly" -> ReminderSchedule.Fortnightly(
                parts.getOrNull(1) ?. toIntOrNull() ?: 1,
                parts.getOrNull(2) ?. toIntOrNull() ?: 0
            )
            "monthly_date" -> ReminderSchedule.MonthlyByDate(parts[1].toIntOrNull() ?: 1)
            "monthly_weekday" -> ReminderSchedule.MonthlyByWeekday(
                parts.getOrNull(1)?.toIntOrNull() ?: 1,
                parts.getOrNull(2)?.toIntOrNull() ?: 1
            )
            else -> ReminderSchedule.None
        }
        } catch (e: Exception)
        {
            ReminderSchedule.None
        }
    }
}