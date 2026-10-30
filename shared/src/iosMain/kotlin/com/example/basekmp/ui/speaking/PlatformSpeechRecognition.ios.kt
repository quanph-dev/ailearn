package com.example.basekmp.ui.speaking

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSpeechRecognition(
    languageTag: String,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    content: @Composable (Boolean, () -> Unit, () -> Unit, Boolean) -> Unit
) {
    content(
        false,
        { onError("unavailable") },
        {},
        false
    )
}
