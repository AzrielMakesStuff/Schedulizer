package com.ateam.schedulizer

object StreakReminders {
    data class Slot(
        val hour: Int,
        val minute: Int,
        val requestCode: Int
    )

    val SLOTS = listOf(
        Slot(hour = 9, minute = 0, requestCode = 9_000_001),
        Slot(hour = 12, minute = 0, requestCode = 9_000_002),
        Slot(hour = 17, minute = 0, requestCode = 9_000_003),
        Slot(hour = 23, minute = 0, requestCode = 9_000_004)
    )
}