package com.semavia.ailearn.ui.speaking

import androidx.compose.runtime.Composable

@Composable
actual fun PlatformSpeechRecognition(
    languageTag: String,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onPartialResult: (String) -> Unit,
    onInteractionStateChange: (Boolean) -> Unit,
    content: @Composable (Boolean, () -> Unit, () -> Unit, Boolean) -> Unit
) {
    content(
        false,
        { onError("unavailable") },
        {},
        false
    )
}
