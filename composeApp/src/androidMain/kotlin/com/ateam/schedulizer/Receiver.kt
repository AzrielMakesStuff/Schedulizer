package com.ateam.schedulizer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.ateam.schedulizer.NotificationSystem.createNotification
import com.ateam.schedulizer.NotificationSystem.requestCode
import com.ateam.schedulizer.entities.Task
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class Receiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        val notificationManager = context.getSystemService(NotificationManager::class.java) ?: return
        val id = intent.getLongExtra("task_id", 0)
        val typeTag = intent.getStringExtra("task_type") ?: return
        val title = intent.getStringExtra("title_key") ?: return
        val body = intent.getStringExtra("body_key") ?: return

        NotificationSystem.clearNotificationById(id, typeTag)

        val pendingResult = goAsync()
        GlobalScope.launch {
            try {
                val task = databaseInstance.getTask(id, typeTag) ?: return@launch
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val zone = TimeZone.currentSystemDefault()

                if (!task.isComplete()) {
                    val notification = createNotification(title, body, task.id, task.typeTag())
                    notificationManager.notify(requestCode(task.id, task.typeTag()), notification)
                }

                val shouldReschedule = when (task) {
                    is Task.Daily -> true
                    is Task.Set, is Task.Pool -> !task.isComplete()
                }

                if (!shouldReschedule) return@launch
                val nextDateTime = task.nextNotificationTime(now, zone) ?: return@launch
                NotificationSystem.scheduleNotification(task.title, task.description, nextDateTime, task)
            } catch (e: Exception) {
                Log.e("NotifDebug", "Failed to handle notification for task=$id", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}