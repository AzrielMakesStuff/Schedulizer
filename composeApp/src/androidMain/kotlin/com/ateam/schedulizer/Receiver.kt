package com.ateam.schedulizer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ateam.schedulizer.NotificationSystem.createNotification
import com.ateam.schedulizer.entities.Task
import com.kizitonwose.calendar.core.now
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Clock

class Receiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val id = intent.getLongExtra("task_id", 0)
        val typeTag = intent.getStringExtra("task_type") ?: return
        val title = intent.getStringExtra("title_key") ?: return
        val body = intent.getStringExtra("body_key") ?: return

        NotificationSystem.clearNotificationById(id, typeTag)

        val pendingResult = goAsync()
        GlobalScope.launch {
            try {
                val task = databaseInstance.getTask(id, typeTag) ?: return@launch
                Log.d("NotifDebug", "Task=${task.title} isComplete=${task.isComplete()}")

                if (!task.isComplete()) {
                    val notification = createNotification(title, body)
                    notificationManager.notify(Random.nextInt(0, 299999), notification)
                }

                val shouldReschedule = when (task) {
                    is Task.Daily -> true
                    is Task.Set, is Task.Pool -> !task.isComplete()
                }

                if (shouldReschedule) {
                    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                    val nextDateTime = task.reminderSchedule.nextFireDateTime(now)
                    if (nextDateTime != null) {
                        NotificationSystem.scheduleNotification(task.title, task.description, nextDateTime, task)
                        Log.d("NotifDebug", "Rescheduled OK for $nextDateTime")
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}