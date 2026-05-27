package com.ateam.schedulizer

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.number
import java.util.Calendar

actual class NotificationSystem {
    fun createNotification(title: String, body : String): Notification {
    return NotificationCompat.Builder(ContextHolder.context, Constants.NOTIF_CHANNEL_ID).setPriority(
        NotificationManager.IMPORTANCE_DEFAULT).setContentText(body).setContentTitle(title).build()
    }

    actual fun scheduleNotification(title: String, body: String, time : LocalDateTime) {
        val intent = Intent(ContextHolder.context, Receiver::class.java);
        intent.putExtra("title_key",title)
        intent.putExtra("body_key",body)
        val pendingIntent = PendingIntent.getBroadcast(ContextHolder.context, Constants.REMIND_ID,intent,
            PendingIntent.FLAG_MUTABLE)
        val alarmManager = ContextHolder.context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()
        calendar.set(time.year,time.month.number, time.day,time.hour,time.minute)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,calendar.timeInMillis,pendingIntent)
    }


}