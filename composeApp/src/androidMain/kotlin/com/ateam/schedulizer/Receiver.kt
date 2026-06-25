package com.ateam.schedulizer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.ateam.schedulizer.NotificationSystem.createNotification
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlin.random.Random

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
                // Ensure DB is loaded before reading
                databaseInstance.refresh()
                val task = databaseInstance.tasks.value.find { it.id.toInt() == id } ?: return@launch
                val currentDate = LocalDate.now()
                val nextDate = task.reminderSchedule.nextFireDate(currentDate) ?: return@launch
                NotificationSystem.scheduleNotification(
                    task.title,
                    task.description,
                    nextDate.atTime(12, 0, 0),
                    task.id.toInt()
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}