package com.ateam.schedulizer

import org.kotlincrypto.aes.gcm.AesGcm
import kotlin.io.encoding.Base64
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
        val combined = Base64.decode(encryptedBase64)
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)
        val decryptedBytes = cipher.decrypt(iv, ciphertext, null)
        return decryptedBytes.decodeToString()
    }
}