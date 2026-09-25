package com.ateam.schedulizer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import com.ateam.schedulizer.NotificationSystem.createNotification
import com.ateam.schedulizer.NotificationSystem.requestCode
import com.ateam.schedulizer.entities.Task
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class Receiver : BroadcastReceiver() {
    @OptIn(DelicateCoroutinesApi::class)
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.getBooleanExtra("is_streak_reminder", false)) {
            handleStreakReminder(context, intent)
            return
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val id = intent.getLongExtra("task_id", 0)
        val typeTag = intent.getStringExtra("task_type") ?: return
        val title = intent.getStringExtra("title_key") ?: return
        val body = intent.getStringExtra("body_key") ?: return

        NotificationSystem.clearNotificationById(id, typeTag)

        val pendingResult = goAsync()
        GlobalScope.launch {
            try {
                val rawTask = databaseInstance.getTask(id, typeTag) ?: return@launch
                val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
                val task = databaseInstance.advanceIfStale(rawTask, now)
                val zone = TimeZone.currentSystemDefault()

                if (NotificationManagerCompat.from(ContextHolder.context).areNotificationsEnabled() && !task.isComplete()) {
                    val notification = createNotification(title, body, task.id, task.typeTag())
                    notificationManager.notify(
                        requestCode(task.id, task.typeTag()),
                        notification
                    )
                }

                val shouldReschedule = when (task) {
                    is Task.Daily -> true
                    is Task.Set, is Task.Pool -> !task.isComplete()
                }

                if (NotificationManagerCompat.from(ContextHolder.context).areNotificationsEnabled() && !shouldReschedule) return@launch
                val nextDateTime = task.nextNotificationTime(now, zone) ?: return@launch
                NotificationSystem.scheduleNotification(task.title, task.description, nextDateTime, task)
            } catch (e: Exception) {
                Log.e("NotifDebug", "Failed to handle notification for task=$id", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleStreakReminder(context: Context, intent: Intent) {
        val slot = StreakReminders.Slot(
            hour = intent.getIntExtra("streak_hour", 0),
            minute = intent.getIntExtra("streak_minute", 0),
            requestCode = intent.getIntExtra("streak_code", 0)
        )
        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val pendingResult = goAsync()
        GlobalScope.launch {
            try {
                val dailies = databaseInstance.taskDao().getAllDaily()
                val today = today()
                val stillAtRisk = dailies.isNotEmpty() && dailies.any { it.lastCompleted?.date != today }

                if (stillAtRisk && NotificationManagerCompat.from(ContextHolder.context).areNotificationsEnabled()) {
                    val notification = createNotification(
                        "Don't lose your streak!",
                        "You still have daily tasks to complete for today."
                    )
                    notificationManager.notify(slot.requestCode, notification)
                }

                if (dailies.isNotEmpty()) NotificationSystem.scheduleStreakReminder(slot)
            } catch (e: Exception) {
                Log.e("NotifDebug", "Failed to handle streak reminder", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}