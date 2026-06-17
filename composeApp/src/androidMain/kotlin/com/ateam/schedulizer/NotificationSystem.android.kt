package com.ateam.schedulizer

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Debug
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.app.NotificationCompat
import com.ateam.schedulizer.entities.Task
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import java.util.Calendar
import kotlin.random.Random

actual object NotificationSystem {
    fun createNotification(title: String, body : String): Notification {
    return NotificationCompat.Builder(ContextHolder.context, Constants.NOTIF_CHANNEL_ID).setPriority(
        NotificationManager.IMPORTANCE_DEFAULT).setContentText(body).setContentTitle(title).setSmallIcon(R.drawable.ic_launcher_background).build()
    }

    actual fun scheduleNotification(title: String, body: String, time : LocalDateTime, code : Int) {
        val intent = Intent(ContextHolder.context, Receiver::class.java);
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        intent.putExtra("title_key",title)
        intent.putExtra("body_key",body)
        val pendingIntent = PendingIntent.getBroadcast(ContextHolder.context, code,intent,
            PendingIntent.FLAG_MUTABLE)
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.set(time.year,time.month.number - 1, time.day,time.hour,time.minute,time.second)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,pendingIntent)
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
        val alarmManager =
            ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val tasks = databaseInstance.tasks;
        tasks.value.forEach {
            val intent = Intent(ContextHolder.context, Receiver::class.java);
            intent.putExtra("title_key",it.reminderSchedule.displayName())
            intent.putExtra("body_key",it.description)
            val pendingIntent = PendingIntent.getBroadcast(ContextHolder.context, Constants.REMIND_ID,intent,
                PendingIntent.FLAG_MUTABLE)
            alarmManager.cancel(pendingIntent)
        }
        }



    actual fun clearNotification(task : Task) {
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ContextHolder.context, Receiver::class.java);
        intent.putExtra("title_key",task.reminderSchedule.displayName())
        intent.putExtra("body_key",task.description)
        val pendingIntent = PendingIntent.getBroadcast(ContextHolder.context, task.id.toInt(),intent,
            PendingIntent.FLAG_MUTABLE)
        alarmManager.cancel(pendingIntent)
    }

    actual fun clearNotificationById(id: Int) {
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(ContextHolder.context, Receiver::class.java);
        val pendingIntent = PendingIntent.getBroadcast(ContextHolder.context, id,intent,
            PendingIntent.FLAG_MUTABLE)
        alarmManager.cancel(pendingIntent)
    }
}


