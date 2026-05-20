package com.ateam.schedulizer

import androidx.room.TypeConverter
import com.ateam.schedulizer.entities.TaskType
import kotlinx.datetime.LocalDateTime

class Converters {
    @TypeConverter fun fromLocalDateTime(value: LocalDateTime?): String? = value?.toString()?.replace("T", " ")
    @TypeConverter fun toLocalDateTime(value: String?): LocalDateTime? = value?.let { LocalDateTime.parse(it.replace(" ", "T")) }

    @TypeConverter fun fromTaskType(value: TaskType?): String? = value?.name
    @TypeConverter fun toTaskType(value: String?): TaskType? = value?.let { TaskType.valueOf(it) }
}