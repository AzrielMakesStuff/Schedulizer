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
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.NotificationCompat
import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import java.util.Calendar

actual object NotificationSystem {
    fun createNotification(title: String, body : String): Notification {
    return NotificationCompat.Builder(ContextHolder.context, Constants.NOTIF_CHANNEL_ID).setPriority(
        NotificationManager.IMPORTANCE_DEFAULT).setContentText(body).setContentTitle(title).setSmallIcon(R.drawable.ic_stat_name).build()
    }

    actual fun scheduleNotification(title: String, body: String, time: LocalDateTime, code: Int) {
        val intent = Intent(ContextHolder.context, Receiver::class.java)
        intent.putExtra("title_key", title)
        intent.putExtra("body_key", body)
        intent.putExtra("task_id", code)
        Log.d("NotifDebug", "Scheduling: code=$code component=${intent.component} filterHash=${intent.filterHashCode()}")
        val pendingIntent = PendingIntent.getBroadcast(
            ContextHolder.context, code, intent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d("NotifDebug", "PendingIntent creatorPackage=${pendingIntent.creatorPackage} toString=${pendingIntent}")
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.set(time.year, time.month.number - 1, time.day, time.hour, time.minute, time.second)
        Log.d("NotifDebug", "Setting alarm for: ${calendar.time} millis=${calendar.timeInMillis}")
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent)
        Log.d("NotifDebug", "Alarm set done")
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestPermission(activity: Activity) {
        when {
            ContextHolder.context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED -> {
            }
            else -> {
                requestPermissions(activity,
                     arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2323)
            }
        }
    }


    actual fun clearAllNotifications() {
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        databaseInstance.tasks.value.forEach {
            val intent = Intent(ContextHolder.context, Receiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                ContextHolder.context, it.id.toInt(), intent, FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pendingIntent)
        }
    }

    actual fun clearNotification(task: Task) {
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ContextHolder.context, Receiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            ContextHolder.context, task.id.toInt(), intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }

    actual fun clearNotificationById(id: Int) {
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ContextHolder.context, Receiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            ContextHolder.context, id, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        ) ?: return
        alarmManager.cancel(pendingIntent)
    }
}