package com.example.basekmp.ui.speaking

import androidx.compose.runtime.Composable

interface PlatformSpeaker {
    val isSpeaking: Boolean
    val isAvailable: Boolean
    fun speak(text: String, languageTag: String = "en-US")
    fun stop()
    fun setSpeed(speed: Float) {}
}

@Composable
expect fun rememberPlatformSpeaker(): PlatformSpeaker

@Composable
expect fun PlatformTextToSpeech(
    text: String,
    languageTag: String,
    content: @Composable (isSpeaking: Boolean, speak: () -> Unit, isAvailable: Boolean) -> Unit
)

@Composable
fun SpeechPlaybackButton(text: String, languageTag: String) {
    val speaker = rememberPlatformSpeaker()
    com.example.basekmp.components.ListenButton(
        onClick = {
            if (speaker.isSpeaking) {
                speaker.stop()
            } else {
                speaker.speak(text, languageTag)
            }
        },
        isPlaying = speaker.isSpeaking
    )
}
