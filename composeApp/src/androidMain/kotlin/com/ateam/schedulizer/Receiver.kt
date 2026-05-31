package com.ateam.schedulizer

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Debug
import com.ateam.schedulizer.NotificationSystem.createNotification

class Receiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val notificationManager = context.getSystemService(NotificationManager::class.java)

        val notification = createNotification(intent.getStringExtra("title_key") as String,intent.getStringExtra("body_key") as String)
        notificationManager.notify(Constants.REMIND_ID,notification)
    }
}