package com.ateam.schedulizer

// iosMain
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings

@OptIn(ExperimentalSettingsImplementation::class)
fun createIosSecureSettings(): Settings = KeychainSettings(service = "SecureKeyTasks")

actual val databaseInstance: AppDatabase by lazy {
    return@lazy getRoomDatabase(getDatabaseBuilder());
}

actual object WidgetManager {
    actual fun updateWidgets() { /* no-op */ }
}

actual fun supportsDynamicColor(): Boolean = false

@androidx.compose.runtime.Composable
actual fun getDynamicColorScheme(isDark: Boolean): androidx.compose.material3.ColorScheme? = null