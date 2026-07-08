package com.ateam.schedulizer

import com.ateam.schedulizer.NotificationSystem.createNotification
import kotlinx.coroutines.DelicateCoroutinesApi
import com.kizitonwose.calendar.core.now
import android.content.BroadcastReceiver
import kotlinx.datetime.toLocalDateTime
import android.app.NotificationManager
import kotlinx.coroutines.GlobalScope
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import android.content.Context
import android.content.Intent
import kotlinx.datetime.plus
import kotlin.random.Random
import kotlin.time.Clock

class Receiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val id = intent.getIntExtra("task_id", 0)
        val title = intent.getStringExtra("title_key") ?: return
        val body = intent.getStringExtra("body_key") ?: return

        val notification = createNotification(title, body)
        notificationManager.notify(Random.nextInt(0, 299999), notification)

        NotificationSystem.clearNotificationById(id)

        val pendingResult = goAsync()
        GlobalScope.launch {
            try {
                databaseInstance.refresh()
                val task = databaseInstance.tasks.value.find { it.id.toInt() == id } ?: return@launch
                if (!task.isComplete()) {
                    val currentDate = LocalDate.now().plus(1, DateTimeUnit.DAY)
                    val nextDate = task.reminderSchedule.nextFireDate(currentDate)
                    if (nextDate != null) {
                        val time = databaseInstance.settings.value?.notificationTime
                        val nextDateTime = nextDate.atTime(time?.hour ?: 12, time?.minute ?: 0, 0)
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        if (nextDateTime > now) {
                            NotificationSystem.scheduleNotification(
                                task.title,
                                task.description,
                                nextDateTime,
                                task.id.toInt()
                            )
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}