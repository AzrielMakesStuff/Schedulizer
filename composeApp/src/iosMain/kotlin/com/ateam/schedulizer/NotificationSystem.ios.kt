package com.ateam.schedulizer

import kotlinx.datetime.LocalDateTime

actual object NotificationSystem {
    actual fun scheduleNotification(
        title: String,
        body: String,
        time: LocalDateTime
    ) {
    }
}