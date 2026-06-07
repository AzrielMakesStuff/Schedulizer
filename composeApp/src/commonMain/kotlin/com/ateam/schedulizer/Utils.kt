package com.ateam.schedulizer

import com.kizitonwose.calendar.core.now
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime

object Utils {
    suspend fun init() {
        val tasks = databaseInstance.tasks.value
        tasks.forEach {

            val currentDate = LocalDate.now()
            val nextDate = it.reminderSchedule.nextFireDate(currentDate);
            NotificationSystem.clearNotification(it);
            NotificationSystem.scheduleNotification(it.reminderSchedule.displayName(),it.description,
                nextDate?.atTime(12,0,0) ?: LocalDate.now().atTime(12,0,0)
            )
        }
    }
}