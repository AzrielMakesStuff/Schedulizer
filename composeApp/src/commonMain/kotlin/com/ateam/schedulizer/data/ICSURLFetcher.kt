package com.ateam.schedulizer.data

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText

object ICSURLFetcher {
    private val client = HttpClient {
        install(HttpTimeout) { requestTimeoutMillis = 15_000 }
        install(HttpRedirect) { checkHttpMethod = false }
    }

    suspend fun fetch(url: String): Result<String> = runCatching {
        client.get(normalize(url)).bodyAsText()
    }

    private fun normalize(url: String): String {
        val trimmed = url.trim()
        return if (trimmed.startsWith("webcal://")) "https://" + trimmed.removePrefix("webcal://") else trimmed
    }
}