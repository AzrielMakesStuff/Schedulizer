package com.ateam.schedulizer.data

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberIcsExporter(onResult: (Boolean) -> Unit): (filename: String, content: String) -> Unit {
    val context = LocalContext.current
    var pendingContent by remember { mutableStateOf<String?>(null) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/calendar")) { uri ->
        val content = pendingContent
        pendingContent = null
        if (uri == null || content == null) {
            onResult(false); return@rememberLauncherForActivityResult
        }
        try {
            context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
            onResult(true)
        } catch (e: Exception) {
            e.printStackTrace(); onResult(false)
        }
    }

    return { fileName, content ->
        pendingContent = content
        launcher.launch(fileName)
    }
}

@Composable
actual fun rememberIcsImporter(onResult: (content: String?) -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) { onResult(null); return@rememberLauncherForActivityResult }
        try {
            val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            onResult(text)
        } catch (e: Exception) {
            e.printStackTrace(); onResult(null)
        }
    }
    return { launcher.launch(arrayOf("text/calendar", "text/plain", "*/*")) }
}

@Composable
actual fun rememberIcsSharer(): (filename: String, content: String) -> Unit {
    val context = LocalContext.current
    return { fileName, content ->
        try {
            val dir = File(context.cacheDir, "shared").apply { mkdirs() }
            val file = File(dir, fileName).apply { writeText(content) }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/calendar"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Share ICS file"))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}