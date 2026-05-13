package com.ateam.schedulizer

import androidx.compose.ui.window.ComposeUIViewController

fun MainViewController() = ComposeUIViewController {
    SecureSettings.settings = createIosSecureSettings()
    App()
}