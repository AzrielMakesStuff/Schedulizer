package com.ateam.schedulizer

import android.app.Application
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Debug
import android.util.Log
import com.ateam.schedulizer.NotificationSystem.createNotification
import kotlin.random.Random

class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("deez", "receive");
        ContextHolder.context = context.applicationContext as Application
        val notificationManager = context.getSystemService(NotificationManager::class.java)
        val notification = createNotification(intent.getStringExtra("title_key") as String,intent.getStringExtra("body_key") as String)
        notificationManager.notify(Random.nextInt(0,299999),notification)
    }
}