package com.ateam.schedulizer

import android.Manifest
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.reflect.KParameter
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ContextHolder.init(applicationContext as Application)

        val notificationChannel = NotificationChannel(Constants.NOTIF_CHANNEL_ID, Constants.NOTIF_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT);
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager;
        notificationManager.createNotificationChannel(notificationChannel);
        val timeT = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val actualTime = timeT.toInstant(TimeZone.currentSystemDefault()).plus(10.seconds).toLocalDateTime(
            TimeZone.currentSystemDefault())
        NotificationSystem.scheduleNotification("PENIS","SUCKI MY DICK", actualTime)
        setContent {
            App()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

