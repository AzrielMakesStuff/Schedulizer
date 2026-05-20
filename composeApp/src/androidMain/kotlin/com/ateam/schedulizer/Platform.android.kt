package com.ateam.schedulizer

// androidMain
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import java.security.SecureRandom

object ContextHolder {
    lateinit var context : Context
    fun init(context : Context) {
    }
}

actual val databaseInstance: AppDatabase by lazy{
    return@lazy (getRoomDatabase(getDatabaseBuilder(ContextHolder.context)));
}