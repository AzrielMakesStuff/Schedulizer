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
                val generatedKey = generateSecureRandomBytes(32).toHexString()
                settings!!.putString("private-key", generatedKey)
                key = generatedKey

                // Initialize new database

            } else key = fetchedKey
        }
        println("Key $key")
    }
}