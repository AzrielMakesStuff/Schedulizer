package com.ateam.schedulizer

import androidx.compose.runtime.Composable
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.color.ColorProvider
import androidx.compose.ui.graphics.Color
import android.content.Context
import androidx.compose.ui.unit.dp
import com.ateam.schedulizer.entities.Task

class StreakWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        // Load data before rendering
        databaseInstance.refresh()
        val tasks = databaseInstance.tasks.value
        val streak = databaseInstance.streak.value
        val pendingCount = tasks.count { task ->
            when (task) {
                is Task.Daily -> task.lastCompleted?.date != today()
                else -> false
            }
        }

        provideContent {
            WidgetContent(streak = streak, pendingCount = pendingCount)
        }
    }
}

@Composable
private fun WidgetContent(streak: Int, pendingCount: Int) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(Color(0x1A000000), Color(0x33000000))),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "$streak 🔥",
            style = TextStyle(
                color = ColorProvider(Color.Black, Color.White)
            )
        )
        Spacer(modifier = GlanceModifier.height(8.dp))
        Text(
            text = "$pendingCount tasks remaining",
            style = TextStyle(
                color = ColorProvider(Color.Black, Color.White)
            )
        )
    }
}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}