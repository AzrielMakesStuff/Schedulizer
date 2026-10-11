package com.ateam.schedulizer

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ateam.schedulizer.entities.Settings
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

enum class ThemeMode {
    AUTO,
    DARK,
    LIGHT
}

@Composable
fun SettingsScreen() {
    val settings by databaseInstance.settings.collectAsState()
    val currentMode = ThemeMode.entries.getOrElse(settings?.colourMode?.toInt() ?: 0) { ThemeMode.AUTO }
    val coroutineScope = rememberCoroutineScope()
    val openAlertDialog = remember { mutableStateOf(false) }

    if (openAlertDialog.value) {
        alertDialogBuilder(
            "Purge Data",
            "Are you sure you want to purge the app database?",
            confirmationRequest = {
                coroutineScope.launch { purgeData() }
                openAlertDialog.value = false
            },
            onDismissRequest = { openAlertDialog.value = false }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(15.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            SettingsCard(title = "Theme") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ThemeButton(
                        selected = currentMode == ThemeMode.AUTO,
                        onClick = { setTheme(ThemeMode.AUTO) },
                        color = MaterialTheme.colorScheme.outline,
                        label = "A"
                    )
                    ThemeButton(
                        selected = currentMode == ThemeMode.DARK,
                        onClick = { setTheme(ThemeMode.DARK) },
                        color = Color.Black,
                        border = Color.Gray
                    )
                    ThemeButton(
                        selected = currentMode == ThemeMode.LIGHT,
                        onClick = { setTheme(ThemeMode.LIGHT) },
                        color = Color.White,
                        border = Color.LightGray
                    )
                }
            }

            SettingsCard(title = "Data") {
                Button(onClick = { openAlertDialog.value = true }) {
                    Text("Purge Database Data")
                }
            }
        }
    }
}

/** A rounded card that sizes to its content instead of a fraction of the screen. */
@Composable
private fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .padding(horizontal = 16.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

@Composable
fun ThemeButton(
    selected: Boolean,
    onClick: () -> Unit,
    color: Color,
    border: Color = MaterialTheme.colorScheme.primary,
    label: String? = null
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(color)
            .border(
                width = if (selected) 4.dp else 1.dp,
                color = if (selected) MaterialTheme.colorScheme.primary else border,
                shape = CircleShape
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (label != null) {
            Text(
                text = label,
                fontSize = 24.sp,
                color = if (color == Color.Black) Color.White else Color.Black,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

fun setTheme(theme: ThemeMode) {
    databaseInstance.settings.value?.let { current ->
        saveSettings(current.copy(colourMode = theme.ordinal.toByte()))
    }
}

fun saveSettings(settings: Settings) {
    CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
        databaseInstance.saveSettings(settings)
    }
}