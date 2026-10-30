package com.example.basekmp.ui.speaking

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

class AndroidPlatformSpeaker(
    private val tts: TextToSpeech,
    private val _isSpeaking: MutableState<Boolean>,
    private val _isReady: MutableState<Boolean>
) : PlatformSpeaker {
    override val isSpeaking: Boolean get() = _isSpeaking.value
    override val isAvailable: Boolean get() = _isReady.value

    override fun speak(text: String, languageTag: String) {
        if (_isReady.value && text.isNotBlank()) {
            runCatching {
                tts.language = Locale.forLanguageTag(languageTag)
                _isSpeaking.value = true
                val result = tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "engla_${text.hashCode()}_${System.currentTimeMillis()}")
                if (result == TextToSpeech.ERROR) {
                    _isSpeaking.value = false
                }
            }.onFailure {
                _isSpeaking.value = false
            }
        }
    }

    override fun stop() {
        runCatching { tts.stop() }
        _isSpeaking.value = false
    }

    override fun setSpeed(speed: Float) {
        runCatching { tts.setSpeechRate(speed) }
    }
}

@Composable
actual fun rememberPlatformSpeaker(): PlatformSpeaker {
    val context = LocalContext.current
    val isSpeaking = remember { mutableStateOf(false) }
    val isReady = remember { mutableStateOf(false) }

    val engine = remember(context) {
        var instance: TextToSpeech? = null
        instance = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady.value = true
            }
        }
        instance
    }

    DisposableEffect(engine) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking.value = false
            }

            @Deprecated("Deprecated in Android")
            override fun onError(utteranceId: String?) {
                isSpeaking.value = false
            }
        })

        onDispose {
            runCatching { engine.stop() }
            runCatching { engine.shutdown() }
        }
    }

    return remember(engine) {
        AndroidPlatformSpeaker(engine, isSpeaking, isReady)
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
