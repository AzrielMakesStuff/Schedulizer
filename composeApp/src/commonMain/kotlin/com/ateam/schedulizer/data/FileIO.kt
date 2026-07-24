package com.ateam.schedulizer.data

import androidx.compose.runtime.Composable

@Composable
expect fun rememberIcsExporter(onResult: (Boolean) -> Unit): (fileName: String, content: String) -> Unit

@Composable
expect fun rememberIcsImporter(onResult: (content: String?) -> Unit): () -> Unit

@Composable
expect fun rememberIcsSharer(): (fileName: String, content: String) -> Unit