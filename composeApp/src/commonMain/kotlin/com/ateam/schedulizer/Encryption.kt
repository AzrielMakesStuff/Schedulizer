package com.ateam.schedulizer

import org.kotlincrypto.aead.AesGcm
import kotlin.io.encoding.Base64 // Built-in experimental decoder/encoder in modern Kotlin
import kotlin.io.encoding.ExperimentalEncodingApi

object Encryption {

    @OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
    fun encrypt(data: String): String {
        val key = SecureSettings.key?.hexToByteArray()
        val cipher = AesGcm(key)
        val iv = generateSecureRandomBytes(12)
        val ciphertext = cipher.encrypt(iv, data.encodeToByteArray(), null)
        val combined = iv + ciphertext
        return Base64.encode(combined)
    }

    @OptIn(ExperimentalStdlibApi::class, ExperimentalEncodingApi::class)
    fun decrypt(encryptedBase64: String, secretKeyHex: String): String {
        val key = secretKeyHex.hexToByteArray()
        val cipher = AesGcm(key)

        // Decode the combined Base64 data back into bytes
        val combined = Base64.decode(encryptedBase64)

        // Extract the original 12-byte IV from the front
        val iv = combined.copyOfRange(0, 12)

        // Extract the actual ciphertext from the remaining bytes
        val ciphertext = combined.copyOfRange(12, combined.size)

        // Decrypt the payload
        val decryptedBytes = cipher.decrypt(iv, ciphertext, null)

        return decryptedBytes.decodeToString()
    }
}