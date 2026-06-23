package com.ateam.schedulizer

import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.LocalDateTime

expect object NotificationSystem {
    fun scheduleNotification(title : String, body: String, time : LocalDateTime,code : Int);
    fun clearAllNotifications()
    fun clearNotification(task : Task)
    fun clearNotificationById(id : Int)

}