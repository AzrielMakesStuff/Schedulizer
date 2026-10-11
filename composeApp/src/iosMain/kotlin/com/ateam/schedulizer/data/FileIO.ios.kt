package com.ateam.schedulizer.data

import androidx.compose.runtime.Composable

@Composable
actual fun rememberIcsExporter(onResult: (Boolean) -> Unit): (fileName: String, content: String) -> Unit {
    return { _, _ ->
        onResult(false)
    }
}

@Composable
actual fun rememberIcsImporter(onResult: (content: String?) -> Unit): () -> Unit {
    return {
        onResult(null)
    }
}

@Composable
actual fun rememberIcsSharer(): (fileName: String, content: String) -> Unit {
    return { _, _ ->
        // No-op sharer stub for iOS
    }
}
