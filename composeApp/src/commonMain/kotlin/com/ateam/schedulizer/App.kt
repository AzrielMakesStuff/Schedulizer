package com.ateam.schedulizer

import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.background
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.runtime.*

@Composable
@Preview
fun App() {
    SecureSettings.verifyKey()

    var plaintext by remember { mutableStateOf("Hello Cruel World") }
    var encrypted by remember { mutableStateOf("") }
    var decrypted by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        encrypted = Encryption.encrypt(plaintext)
        decrypted = Encryption.decrypt(encrypted)
    }

    MaterialTheme {
        Column(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.primaryContainer)
                .safeContentPadding()
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(plaintext)
            Text(encrypted)
            Text(decrypted)
        }
    }
}