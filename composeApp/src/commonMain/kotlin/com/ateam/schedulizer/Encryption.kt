package com.ateam.schedulizer

import dev.whyoleg.cryptography.CryptographyProvider
import kotlin.io.encoding.ExperimentalEncodingApi
import dev.whyoleg.cryptography.algorithms.AES
import kotlin.io.encoding.Base64

object Encryption {

    private val provider = CryptographyProvider.Default
    private val aes = provider.get(AES.GCM)

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun encrypt(data: String): String {
        val keyBytes = requireNotNull(SecureSettings.key) { "Missing key" }
            .hexToByteArray()

        val key = aes.keyDecoder().decodeFromByteArray(AES.Key.Format.RAW ,keyBytes)

        val iv = generateSecureRandomBytes(12)

        val encrypted = key.encrypt(
            iv = iv,
            plaintext = data.encodeToByteArray(),
            associatedData = null
        )

        val combined = iv + encrypted
        return Base64.encode(combined)
    }

    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(encryptedBase64: String, secretKeyHex: String): String {
        val keyBytes = secretKeyHex.hexToByteArray()
        val key = aes.keyDecoder().decodeFromByteArray(keyBytes)

        val combined = Base64.decode(encryptedBase64)

        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)

        val decrypted = key.decrypt(
            iv = iv,
            ciphertext = ciphertext,
            associatedData = null
        )

        return decrypted.decodeToString()
    }
}