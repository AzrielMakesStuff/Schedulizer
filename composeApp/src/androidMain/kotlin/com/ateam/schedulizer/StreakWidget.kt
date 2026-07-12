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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.text.FontWeight
import com.ateam.schedulizer.entities.Task

class StreakWidget : GlanceAppWidget() {
    override val sizeMode: SizeMode = SizeMode.Exact
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        databaseInstance.refresh()
        val streakStatus = databaseInstance.checkStreak()
        provideContent {
            val tasks by databaseInstance.tasks.collectAsState()
            val streak by databaseInstance.streak.collectAsState()

            val pendingCount = tasks.count { task ->
                when (task) {
                    is Task.Daily -> task.lastCompleted?.date != today()
                    else -> false
                }
            }

            WidgetContent(streak = streak, pendingCount = pendingCount, streakStatus = streakStatus)
        }
    }
}

@Composable
private fun WidgetContent(streak: Int, pendingCount: Int, streakStatus: AppDatabase.StreakStatus) {
    val widgetWidth = LocalSize.current.width
    val widgetHeight = LocalSize.current.height
    val imageColumnWidth = widgetWidth / 3
    val sizeBasis = minOf(widgetHeight.value, widgetWidth.value / 3f)

    val countFontSize = (sizeBasis * 0.30f).coerceIn(15f, 42f).sp
    val subtitleFontSize = (sizeBasis * 0.15f).coerceIn(9f, 18f).sp
    val contentPadding = (sizeBasis * 0.14f).dp.coerceIn(8.dp, 22.dp)
    val iconSize = minOf(imageColumnWidth - 8.dp, widgetHeight - (contentPadding * 2))
        .coerceIn(24.dp, 80.dp)

    Row (
        modifier = GlanceModifier
            .fillMaxSize()
            //.background(ColorProvider(Color(0x1A000000), Color(0x33000000)))
            .background(
                imageProvider = ImageProvider(R.drawable.streak_gradient),
                contentScale = ContentScale.Crop
            )
            .clickable(actionRunCallback<OpenActivityAction>())
            .padding(contentPadding),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = GlanceModifier.width(imageColumnWidth).fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Image(
                provider = ImageProvider(
                    if (streakStatus.completedToday) R.drawable.streak_complete
                    else R.drawable.streak_incomplete
                ),
                contentDescription = "Streak",
                modifier = GlanceModifier.size(iconSize)
            )
        }
        Box (
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${streak + if (pendingCount == 0) 1 else 0}",
                    style = TextStyle(
                        color = ColorProvider(Color.Black, Color.White),
                        fontSize = countFontSize,
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 1
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                Text(
                    text = "$pendingCount tasks remaining",
                    style = TextStyle(
                        color = ColorProvider(Color.Black, Color.White),
                        fontSize = subtitleFontSize
                    ),
                    maxLines = 1
                )
            }
        }
    }

}

class StreakWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = StreakWidget()
}