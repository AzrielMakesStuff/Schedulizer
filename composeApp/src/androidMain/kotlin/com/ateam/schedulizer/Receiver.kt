package com.ateam.schedulizer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ateam.schedulizer.NotificationSystem.createNotification
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
        Log.d("NotifDebug", "RECEIVER FIRED id=${intent.getIntExtra("task_id", -1)} at ${Clock.System.now()}")
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
                Log.d("NotifDebug", "Coroutine started for id=$id")
                databaseInstance.refresh()
                Log.d("NotifDebug", "Refresh done, tasks=${databaseInstance.tasks.value.size}")
                val task = databaseInstance.tasks.value.find { it.id.toInt() == id }
                if (task == null) {
                    Log.e("NotifDebug", "Task $id not found post-refresh")
                    return@launch
                }
                Log.d("NotifDebug", "Task=${task.title} isComplete=${task.isComplete()}")
                if (!task.isComplete()) {
                    val nextDate = task.reminderSchedule.nextFireDate(LocalDate.now().plus(1, DateTimeUnit.DAY))
                    Log.d("NotifDebug", "nextDate=$nextDate")
                    if (nextDate != null) {
                        val time = databaseInstance.settings.value?.notificationTime
                        val nextDateTime = nextDate.atTime(time?.hour ?: 12, time?.minute ?: 0, 0)
                        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                        Log.d("NotifDebug", "nextDateTime=$nextDateTime now=$now")
                        if (nextDateTime > now) {
                            NotificationSystem.scheduleNotification(task.title, task.description, nextDateTime, task.id.toInt())
                            Log.d("NotifDebug", "Rescheduled OK")
                        } else {
                            Log.e("NotifDebug", "nextDateTime <= now, skipped scheduling")
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e("NotifDebug", "FAILED for id=$id", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}