package com.ateam.schedulizer

import kotlinx.datetime.LocalDateTime

expect class NotificationSystem {
    expect fun scheduleNotification(title : String, body: String, time : LocalDateTime);
}