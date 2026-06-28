package com.ateam.schedulizer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val notificationChannel = NotificationChannel(Constants.NOTIF_CHANNEL_ID, Constants.NOTIF_CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(notificationChannel)
        NotificationSystem.requestPermission(this)
        setContent {
            val settings by databaseInstance.settings.collectAsState()
            val systemIsDark = isSystemInDarkTheme()
            val isDarkMode = when (ThemeMode.entries.getOrElse(settings?.colourMode?.toInt() ?: 0) { ThemeMode.AUTO }) {
                ThemeMode.AUTO -> systemIsDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(Unit) {
                databaseInstance.refresh()
                databaseInstance.rescheduleAllNotifications()
            }
            LaunchedEffect(isDarkMode) {
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkMode) SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    else SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                )
            }
            App()
        }
    }

}

@Preview
@Composable
fun AppAndroidPreview() {
    App()
}

