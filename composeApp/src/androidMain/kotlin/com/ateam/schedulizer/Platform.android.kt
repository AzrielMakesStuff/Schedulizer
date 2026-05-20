package com.ateam.schedulizer

// androidMain
import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import java.security.SecureRandom

fun createAndroidSecureSettings(context: Context): Settings {
    val masterKey = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()

    val delegate = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    return SharedPreferencesSettings(delegate)
}

actual fun generateSecureRandomBytes(size: Int): ByteArray {
    val random = SecureRandom()
    val bytes = ByteArray(size)
    random.nextBytes(bytes)
    return bytes
}

object ContextHolder {
    lateinit var context : Context
    fun init(context : Context) {
    }
}

actual val databaseInstance: AppDatabase by lazy{
    return@lazy (getRoomDatabase(getDatabaseBuilder(ContextHolder.context)));
}