package com.ateam.schedulizer

import dev.whyoleg.cryptography.CryptographyProvider
import dev.whyoleg.cryptography.algorithms.AES
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

object Encryption {
    private val provider = CryptographyProvider.Default
    private val aesGcm = provider.get(AES.GCM)

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun encrypt(data: String): String {
        val cipher = deriveKey().cipher()
        val encrypted = cipher.encrypt(data.encodeToByteArray())
        return Base64.UrlSafe.encode(encrypted)
    }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun decrypt(encryptedBase64: String): String {
        val cipher = deriveKey().cipher()
        val decrypted = cipher.decrypt(Base64.UrlSafe.decode(encryptedBase64))
        return decrypted.decodeToString()
    }

    private suspend fun deriveKey(): AES.GCM.Key {
        val keyBytes = requireNotNull(SecureSettings.key) { "Missing key" }
            .hexToByteArray()

        require(keyBytes.size in listOf(16, 24, 32)) {
            "Invalid AES key length: ${keyBytes.size} bytes"
        }

        return aesGcm.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW, keyBytes)
    }
}