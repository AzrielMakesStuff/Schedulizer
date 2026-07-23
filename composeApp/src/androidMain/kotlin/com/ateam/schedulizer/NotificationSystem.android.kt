package com.ateam.schedulizer

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleCoroutineScope
import com.ateam.schedulizer.entities.Task
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import java.util.Calendar

var lastNotifPerm = NotificationManagerCompat.from(ContextHolder.context).areNotificationsEnabled()
actual object NotificationSystem {
    fun createNotification(title: String, body: String, taskId: Long? = null, taskType: String? = null): Notification {
        val launchIntent = ContextHolder.context.packageManager
            .getLaunchIntentForPackage(ContextHolder.context.packageName)
            ?.apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                if (taskId != null && taskType != null) {
                    putExtra("task_id", taskId)
                    putExtra("task_type", taskType)
                }
            }

        val contentPendingIntent = PendingIntent.getActivity(
            ContextHolder.context,
            requestCode(taskId ?: 0, taskType ?: ""),
            launchIntent,
            FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(ContextHolder.context, Constants.NOTIF_CHANNEL_ID)
            .setPriority(NotificationManager.IMPORTANCE_DEFAULT)
            .setContentText(body)
            .setContentTitle(title)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)
            .build()
    }

    fun requestCode(id: Long, type: String): Int {
        val tag = when (type) {
            "Set" -> 1
            "Pool" -> 2
            "Daily" -> 3
            else -> 0
        }
        return (id.toInt() * 4) + tag
    }

    actual fun scheduleNotification(title: String, body: String, time: LocalDateTime, task: Task) {

       if (NotificationManagerCompat.from(ContextHolder.context).areNotificationsEnabled()) {
            val intent = Intent(ContextHolder.context, Receiver::class.java)
            intent.putExtra("title_key", title)
            intent.putExtra("body_key", body)
            intent.putExtra("task_id", task.id)
            intent.putExtra("task_type", task.typeTag())
            val code = requestCode(task.id, task.typeTag())
            val pendingIntent = PendingIntent.getBroadcast(
                ContextHolder.context, code, intent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val alarmManager =
                ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val calendar = Calendar.getInstance()
            calendar.set(
                time.year,
                time.month.number - 1,
                time.day,
                time.hour,
                time.minute,
                time.second
            )
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestPermission(activity: Activity) {
        when {
            ContextHolder.context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {}
            else -> requestPermissions(activity, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2323)
        }
    }

    actual fun clearAllNotifications() {
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        databaseInstance.tasks.value.forEach {
            val intent = Intent(ContextHolder.context, Receiver::class.java)
            val code = requestCode(it.id, it.typeTag())
            val pendingIntent = PendingIntent.getBroadcast(
                ContextHolder.context, code, intent, FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    actual fun clearNotification(task: Task) {
        clearNotificationById(task.id, task.typeTag())
    }

    actual fun clearNotificationById(id: Long, type: String) {
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ContextHolder.context, Receiver::class.java)
        val code = requestCode(id, type)
        val pendingIntent = PendingIntent.getBroadcast(
            ContextHolder.context, code, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }

    actual suspend fun recheckNotifPerm() {
        val notifsEnabled = NotificationManagerCompat.from(ContextHolder.context).areNotificationsEnabled()
        if (!lastNotifPerm && notifsEnabled) {
            databaseInstance.rescheduleAllNotifications()
            lastNotifPerm = true
        }
        else if (lastNotifPerm && !notifsEnabled) clearAllNotifications()
        lastNotifPerm = notifsEnabled
    }
}