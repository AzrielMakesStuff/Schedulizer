package com.ateam.schedulizer

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

@Composable
fun AppTheme(content: @Composable () -> Unit) {
    val settings by databaseInstance.settings.collectAsState()
    val systemIsDark = isSystemInDarkTheme()
    val isDarkMode = when (ThemeMode.entries.getOrElse(settings?.colourMode?.toInt() ?: 0) { ThemeMode.AUTO }) {
        ThemeMode.AUTO -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val dynamicColor = supportsDynamicColor()
    val colorScheme = if (dynamicColor) {
        getDynamicColorScheme(isDarkMode) ?: if (isDarkMode) darkColorScheme() else lightColorScheme()
    } else {
        if (isDarkMode) darkColorScheme() else lightColorScheme()
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}