package com.ateam.schedulizer.entities

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key.Companion.Settings
import com.ateam.schedulizer.Settings
import com.russhwolf.settings.Settings
import kotlinx.serialization.Serializable
import com.russhwolf.settings.set
import com.russhwolf.settings.get

@Serializable
data class PublicHoliday(
    val date: String,
    val localName: String,
    val name: String,
    val countryCode: String,
    val fixed: Boolean,
    val global: Boolean,
    val counties: List<String>,
    val launchYear: Int?,
    val types: List<String>
)

//val settings: Settings = Settings()
//
//@Composable
//fun HolidayCache() {
//
//    fun saveHolidays(year: Int, countryCode: String, holidays: List<PublicHoliday>) {
//        val cacheKey = "holidays-${year}-${countryCode}"
//
//        val jsonString = Json.encodeToString(holidays)
//
//        settings[cacheKey] = jsonString
//    }
//
//    fun getHolidays(year: Int, countryCode: String): List<PublicHoliday>? {
//        val cacheKey = "holidays-${year}-${countryCode}"
//
//        val jsonString = String? = settings[cacheKey]
//
//        if (jsonString.isNullOrEmpty()) return null;
//
//        return Json.decodeFromString<List<PublicHoliday>>(jsonString)
//    }
//}
