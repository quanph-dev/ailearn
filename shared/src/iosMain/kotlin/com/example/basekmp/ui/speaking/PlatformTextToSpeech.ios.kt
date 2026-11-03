package com.example.basekmp.ui.speaking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.DisposableEffect
import com.example.basekmp.ui.lifecycle.rememberIsAppResumed
import platform.Foundation.NSNotificationCenter
import platform.darwin.NSObject
import platform.AVFAudio.AVSpeechBoundary
import platform.AVFAudio.AVSpeechSynthesisVoice
import platform.AVFAudio.AVSpeechSynthesizer
import platform.AVFAudio.AVSpeechSynthesizerDelegateProtocol
import platform.AVFAudio.AVSpeechUtterance
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationWillResignActiveNotification

class IosPlatformSpeaker(
    private val synthesizer: AVSpeechSynthesizer,
    private val _isSpeaking: MutableState<Boolean>,
    private val isAppResumed: MutableState<Boolean>
) : PlatformSpeaker {
    private val _spokenChars = mutableIntStateOf(0)
    private var activeUtterance: AVSpeechUtterance? = null
    private val delegate = object : NSObject(), AVSpeechSynthesizerDelegateProtocol {
        override fun speechSynthesizer(
            synthesizer: AVSpeechSynthesizer,
            didFinishSpeechUtterance: AVSpeechUtterance
        ) {
            finishIfCurrent(didFinishSpeechUtterance)
        }
    }

    init {
        synthesizer.delegate = delegate
    }

    override val isSpeaking: Boolean get() = _isSpeaking.value
    override val isLoading: Boolean get() = false
    override val isPlaying: Boolean get() = _isSpeaking.value
    override val spokenChars: Int get() = _spokenChars.intValue
    override val hasStartedSpeaking: Boolean get() = _spokenChars.intValue > 0
    override val isAvailable: Boolean get() = true

    override fun warmUp(vararg languageTags: String) {}

    override fun speak(
        text: String,
        languageTag: String,
        voicePreference: String?,
        speed: Float,
        altLanguageTag: String?
    ) {
        if (text.isBlank() || !isAppResumed.value) return
        if (synthesizer.isSpeaking()) {
            activeUtterance = null
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
        val utterance = AVSpeechUtterance.speechUtteranceWithString(text)
        utterance.voice = AVSpeechSynthesisVoice.voiceWithLanguage(languageTag)
        activeUtterance = utterance
        _spokenChars.intValue = 0
        _isSpeaking.value = true
        synthesizer.speakUtterance(utterance)
    }

    override fun stop() {
        activeUtterance = null
        if (synthesizer.isSpeaking()) {
            synthesizer.stopSpeakingAtBoundary(AVSpeechBoundary.AVSpeechBoundaryImmediate)
        }
        _isSpeaking.value = false
        _spokenChars.intValue = 0
    }

    private fun finishIfCurrent(utterance: AVSpeechUtterance) {
        if (activeUtterance !== utterance) return
        _spokenChars.intValue = utterance.speechString.length
        activeUtterance = null
        _isSpeaking.value = false
    }
}

@Composable
actual fun rememberPlatformSpeaker(voicePreference: String?): PlatformSpeaker {
    val synthesizer = remember { AVSpeechSynthesizer() }
    val isSpeaking = remember { mutableStateOf(false) }
    val lifecycleResumed = rememberIsAppResumed()
    val isAppResumed = remember {
        mutableStateOf(lifecycleResumed)
    }

    val speaker = remember(synthesizer, isAppResumed) {
        IosPlatformSpeaker(synthesizer, isSpeaking, isAppResumed)
    }

    DisposableEffect(synthesizer, speaker) {
        val center = NSNotificationCenter.defaultCenter
        val resignObserver = center.addObserverForName(
            UIApplicationWillResignActiveNotification,
            null,
            null
        ) {
            isAppResumed.value = false
            speaker.stop()
        }
        val activeObserver = center.addObserverForName(
            UIApplicationDidBecomeActiveNotification,
            null,
            null
        ) {
            isAppResumed.value = true
        }

        onDispose {
            center.removeObserver(resignObserver)
            center.removeObserver(activeObserver)
            speaker.stop()
            synthesizer.delegate = null
        }
    }

    return speaker
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
