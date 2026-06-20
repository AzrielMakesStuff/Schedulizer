package com.ateam.schedulizer

import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Debug
import android.provider.ContactsContract
import android.util.Log
import androidx.room.Database
import com.ateam.schedulizer.NotificationSystem.createNotification
import com.kizitonwose.calendar.core.now
import kotlinx.datetime.LocalDate
import kotlinx.datetime.atTime
import kotlin.random.Random

class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        ContextHolder.context = context.applicationContext as Application
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val id = intent.getIntExtra("task_id",0)

        val notification = createNotification(intent.getStringExtra("title_key") as String,intent.getStringExtra("body_key") as String)
        notificationManager.notify(Random.nextInt(0,299999),notification)

        NotificationSystem.clearNotificationById(id)
        val currentDate = LocalDate.now()
        val tasks = databaseInstance.tasks
    }
}