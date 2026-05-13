package com.ateam.schedulizer

// iosMain
import com.russhwolf.settings.ExperimentalSettingsImplementation
import com.russhwolf.settings.KeychainSettings
import com.russhwolf.settings.Settings
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.arc4random_buf

@OptIn(ExperimentalSettingsImplementation::class)
fun createIosSecureSettings(): Settings = KeychainSettings(service = "SecureKeyTasks")

@OptIn(ExperimentalForeignApi::class)
actual fun generateSecureRandomBytes(size: Int): ByteArray {
    val bytes = ByteArray(size)
    bytes.usePinned { pinned -> arc4random_buf(pinned.addressOf(0), size.toULong()) }
    return bytes
}

