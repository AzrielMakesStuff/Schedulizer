package com.ateam.schedulizer

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import dev.whyoleg.cryptography.algorithms.AES
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory

suspend fun getDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val passPhrase = Encryption.deriveKey().encodeToByteArray(AES.Key.Format.RAW);
    val factory = SupportOpenHelperFactory(passPhrase);
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath("my_room.db")
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    ).openHelperFactory(factory);
}