package com.ateam.schedulizer.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.ateam.schedulizer.Frequency
import kotlinx.datetime.LocalTime
import kotlin.math.min

@Entity("Settings")
data class Settings(
    @PrimaryKey
    val id: Long = 1,
    val colourMode: Byte = 0, // 0: Auto, 1: Dark Mode, 2: Light Mode (EW)
    @ColumnInfo(defaultValue = "Weekly")
    val paidFrequency: Frequency = Frequency.Weekly, // 0: Auto, 1: Dark Mode, 2: Light Mode (EW)
)