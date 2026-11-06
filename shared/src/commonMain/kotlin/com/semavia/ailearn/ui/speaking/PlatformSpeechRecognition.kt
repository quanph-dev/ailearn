package com.semavia.ailearn.ui.speaking

import androidx.compose.runtime.Composable

@Composable
expect fun PlatformSpeechRecognition(
    languageTag: String,
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onPartialResult: (String) -> Unit = {},
    onInteractionStateChange: (Boolean) -> Unit = {},
    content: @Composable (
        isListening: Boolean,
        start: () -> Unit,
        stop: () -> Unit,
        isAvailable: Boolean
    ) -> Unit
)
