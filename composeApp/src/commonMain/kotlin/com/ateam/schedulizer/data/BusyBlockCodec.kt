package com.ateam.schedulizer.data

import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

data class BusyBlock(val start: LocalDateTime, val durationMinutes: Int)

fun BusyBlock.endDateTime(): LocalDateTime {
    val zone = TimeZone.currentSystemDefault()
    return start.toInstant(zone).plus(durationMinutes, DateTimeUnit.MINUTE, zone).toLocalDateTime(zone)
}

object BusyBlockCodec {
    private const val PREFIX = "SCHZB1|"
    private val EPOCH_REF: Instant = LocalDate(2020, 1, 1).atStartOfDayIn(TimeZone.UTC)

    fun encode(blocks: List<BusyBlock>): String {
        val zone = TimeZone.currentSystemDefault()
        val body = blocks.joinToString(",") { block ->
            val startInstant = block.start.toInstant(zone)
            val startMinutes = (startInstant - EPOCH_REF).inWholeMinutes
            "${startMinutes.toString(36)}-${block.durationMinutes.toString(36)}"
        }
        return PREFIX + body
    }

    fun decode(text: String): List<BusyBlock>? {
        val trimmed = text.trim()
        if (!trimmed.startsWith(PREFIX)) return null
        val body = trimmed.removePrefix(PREFIX)
        if (body.isBlank()) return emptyList()

        val zone = TimeZone.currentSystemDefault()
        return try {
            body.split(",").map { entry ->
                val (startPart, durationPart) = entry.split("-")
                val startMinutes = startPart.toLong(36)
                val duration = durationPart.toInt(36)
                require(duration > 0)
                val start = EPOCH_REF.plus(startMinutes, DateTimeUnit.MINUTE, zone).toLocalDateTime(zone)
                BusyBlock(start, duration)
            }
        } catch (_: Exception) {
            null
        }
    }

    fun fromTasks(tasks: List<Task.Set>): List<BusyBlock> {
        val zone = TimeZone.currentSystemDefault()
        return tasks.mapNotNull { task ->
            val end = task.end
            val minutes = (end.toInstant(zone) - task.start.toInstant(zone)).inWholeMinutes.toInt()
            if (minutes <= 0) null else BusyBlock(task.start, minutes)
        }
    }
}