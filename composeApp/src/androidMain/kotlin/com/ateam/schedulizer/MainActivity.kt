package com.ateam.schedulizer

import android.app.Application
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
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDateTime
import kotlinx.datetime.toKotlinLocalDateTime
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        ContextHolder.init(applicationContext as Application)

        val notificationChannel = NotificationChannel(Constants.NOTIF_CHANNEL_ID, Constants.NOTIF_CHANNEL_NAME,

            NotificationManager.IMPORTANCE_DEFAULT);
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager;
        notificationManager.createNotificationChannel(notificationChannel);
        NotificationSystem.requestPermission(this);

        val currentTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault());
        val newTime = currentTime.toJavaLocalDateTime().plusSeconds(10,).toKotlinLocalDateTime();
        val newTime2 = currentTime.toJavaLocalDateTime().plusSeconds(15,).toKotlinLocalDateTime();
        val newTime3 = currentTime.toJavaLocalDateTime().plusSeconds(20,).toKotlinLocalDateTime();
        setContent {
            val systemIsDark = isSystemInDarkTheme()
            val isDarkMode = when (currentThemeMode) {
                ThemeMode.SYSTEM -> systemIsDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LaunchedEffect(isDarkMode) {
                NotificationSystem.clearAllNotifications()
                enableEdgeToEdge(
                    statusBarStyle = if (isDarkMode) {
                        SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.TRANSPARENT
                        )
                    }
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

