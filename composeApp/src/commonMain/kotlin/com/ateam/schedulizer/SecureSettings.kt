package com.ateam.schedulizer

import com.russhwolf.settings.Settings

object SecureSettings {
    var settings: Settings? = null
    var key: String? = null
    fun verifyKey() {
        if (settings != null) {
            val fetchedKey = settings!!.getString("private-key", "")
            if (fetchedKey.isEmpty()) { // If key is empty
                // Generate key and save to storage
                val newKey = generateSecureRandomBytes(256).toString()
                settings!!.putString("private-key", newKey)
                println("Key $newKey")

                // Initialize new database
                // "REINITIALIZE DATABASE"
            } else key = fetchedKey
        }
    }
}