package com.example.basekmp.ui.speaking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechUtterance

class IosPlatformSpeaker(
    private val synthesizer: AVSpeechSynthesizer,
    private val _isSpeaking: MutableState<Boolean>
) : PlatformSpeaker {
    override val isSpeaking: Boolean get() = _isSpeaking.value
    override val isAvailable: Boolean get() = true

    override fun speak(text: String, languageTag: String, voicePreference: String?) {
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)
        synthesizer.speakUtterance(utterance)
        _isSpeaking.value = true
    }

    override fun stop() {
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
        _isSpeaking.value = false
    }
}

@Composable
actual fun rememberPlatformSpeaker(): PlatformSpeaker {
    val synthesizer = remember { AVSpeechSynthesizer() }
    val isSpeaking = remember { mutableStateOf(false) }

    return remember(synthesizer) {
        IosPlatformSpeaker(synthesizer, isSpeaking)
    }
}

@Composable
actual fun PlatformTextToSpeech(
    text: String,
    languageTag: String,
    content: @Composable (Boolean, () -> Unit, Boolean) -> Unit
) {
    val speaker = rememberPlatformSpeaker()
    content(
        speaker.isSpeaking,
        { speaker.speak(text, languageTag) },
        speaker.isAvailable
    )
}
