package com.ateam.schedulizer

import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.LocalDateTime

actual object NotificationSystem {
    actual fun scheduleNotification(title: String, body: String, time: LocalDateTime, task: Task) {}
    actual fun scheduleStreakReminder(slot: StreakReminders.Slot) {}
    actual fun clearStreakReminders() {}
    actual fun clearAllNotifications() {}
    actual fun clearNotification(task: Task) {}
    actual fun clearNotificationById(id: Long, type: String) {}
    actual suspend fun recheckNotifPerm() {}
}
