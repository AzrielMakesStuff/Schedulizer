package com.ateam.schedulizer

import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.core.now
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.atTime

expect object NotificationSystem {
    fun scheduleNotification(title : String, body: String, time : LocalDateTime);
    fun clearAllNotifications()
    fun clearNotification(task : Task)

}