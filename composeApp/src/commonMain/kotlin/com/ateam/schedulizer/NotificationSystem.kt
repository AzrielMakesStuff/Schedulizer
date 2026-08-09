package com.ateam.schedulizer

import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.LocalDateTime

expect object NotificationSystem {
    fun scheduleNotification(title : String, body: String, time: LocalDateTime, task: Task)
    fun scheduleStreakReminder(slot: StreakReminders.Slot)
    fun clearStreakReminders()
    fun clearAllNotifications()
    fun clearNotification(task : Task)
    fun clearNotificationById(id : Long, type: String)
    suspend fun recheckNotifPerm()
}