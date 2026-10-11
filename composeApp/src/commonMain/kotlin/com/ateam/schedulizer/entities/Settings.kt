package com.ateam.schedulizer.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalTime
import kotlin.math.min

@Entity("Settings")
data class Settings(
    @PrimaryKey
    val id: Long = 1,
    val colourMode: Byte = 0, // 0: Auto, 1: Dark Mode, 2: Light Mode (EW)
)