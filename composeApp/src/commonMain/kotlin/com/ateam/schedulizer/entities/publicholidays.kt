package com.ateam.schedulizer.entities

import kotlinx.serialization.Serializable

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

//val settings: SettingsScreen = SettingsScreen()
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
