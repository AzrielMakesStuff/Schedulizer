package com.ateam.schedulizer

import com.ateam.schedulizer.entities.TaskType
import kotlinx.datetime.LocalDateTime
import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate

class Converters {
    @TypeConverter fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()?.replace("T", " ")
    @TypeConverter fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it.replace(" ", "T")) }

    @TypeConverter fun fromLocalDate(value: LocalDate?): String? = value?.toString()
    @TypeConverter fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter fun fromTaskType(value: TaskType?): String? = value?.name
    @TypeConverter fun toTaskType(value: String?): TaskType? = value?.let { TaskType.valueOf(it) }

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
        return when (parts[0]) {
            "every_n_days" -> ReminderSchedule.EveryNDays(parts[1].toInt())
            "weekly" -> ReminderSchedule.Weekly(parts[1].toInt())
            "fortnightly" -> ReminderSchedule.Fortnightly(parts[1].toInt(), parts[2].toInt())
            "monthly_date" -> ReminderSchedule.MonthlyByDate(parts[1].toInt())
            "monthly_weekday" -> ReminderSchedule.MonthlyByWeekday(parts[1].toInt(), parts[2].toInt())
            else -> ReminderSchedule.None
        }
    }
}