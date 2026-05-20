package com.ateam.schedulizer

expect fun generateSecureRandomBytes(size: Int): ByteArray
expect suspend fun getDatabaseInstance() : AppDatabase