package com.ateam.schedulizer

expect fun generateSecureRandomBytes(size: Int): ByteArray
expect val databaseInstance : AppDatabase