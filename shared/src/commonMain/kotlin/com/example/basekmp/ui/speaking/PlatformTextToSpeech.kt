package com.example.basekmp.ui.speaking

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.basekmp.data.UserPrefs
import com.example.basekmp.ui.lifecycle.rememberIsAppResumed
import kotlinx.coroutines.delay

interface PlatformSpeaker {
    val isSpeaking: Boolean
    val isLoading: Boolean get() = false
    val isPlaying: Boolean get() = isSpeaking
    val spokenChars: Int get() = 0
    val hasStartedSpeaking: Boolean get() = spokenChars > 0
    val isAvailable: Boolean

    fun warmUp(vararg languageTags: String) {}

    fun speak(
        text: String,
        languageTag: String = "en-US",
        voicePreference: String? = UserPrefs.tutorVoice,
        speed: Float = 1.0f,
        altLanguageTag: String? = null,
        quotedLanguageTag: String? = null
    )

    fun stop()
    fun setSpeed(speed: Float) {}
}

// Replace internal mood markers with spaces so speech progress still maps to the displayed text.
internal fun speechTextWithoutControlTags(text: String): String {
    val prefix = "(?:TD|TB|B)"
    val tag = "(?:great|ok|oops|hi)"
    val pattern = Regex("\\[\\s*$prefix\\s*$tag\\s*]", RegexOption.IGNORE_CASE)
    val leading = Regex("^\\s*\\[\\s*$tag\\s*]", RegexOption.IGNORE_CASE)
    fun blankMatches(input: String, regex: Regex) = regex.replace(input) { " ".repeat(it.value.length) }
    return blankMatches(blankMatches(text, pattern), leading)
}

internal fun hasSpeakableContent(text: String): Boolean = text.any { it.isLetterOrDigit() }

fun speechLocaleTagFor(codeOrName: String?): String {
    return when (codeOrName.orEmpty().trim().lowercase()) {
        "en", "english", "en-us" -> "en-US"
        "en-gb", "british" -> "en-GB"
        "en-au", "australian" -> "en-AU"
        "es", "spanish", "es-es" -> "es-ES"
        "fr", "french", "fr-fr" -> "fr-FR"
        "ar", "arabic" -> "ar"
        "ur", "urdu" -> "ur"
        "hi", "hindi", "hi-in" -> "hi-IN"
        "zh", "chinese", "chinese (mandarin)", "zh-cn" -> "zh-CN"
        "ja", "japanese", "ja-jp" -> "ja-JP"
        "ko", "korean", "ko-kr" -> "ko-KR"
        "it", "italian", "it-it" -> "it-IT"
        "de", "german", "de-de" -> "de-DE"
        "pt", "portuguese", "portuguese (brazilian)", "pt-br" -> "pt-BR"
        "tr", "turkish", "tr-tr" -> "tr-TR"
        "ru", "russian", "ru-ru" -> "ru-RU"
        "vi", "vietnamese", "vi-vn" -> "vi-VN"
        "th", "thai", "th-th" -> "th-TH"
        else -> "en-US"
    }
}

private const val SPEECH_GRACE_MS = 1200L
const val TYPEWRITER_CHARS_PER_SECOND = 55

@Composable
fun rememberTypewriter(
    text: String,
    enabled: Boolean,
    charsPerSecond: Int = TYPEWRITER_CHARS_PER_SECOND,
    spokenChars: Int? = null
): String {
    if (!enabled) return text

    var count by rememberSaveable(text) { mutableIntStateOf(0) }
    var timedAllowed by rememberSaveable(text) { mutableStateOf(spokenChars == null) }
    val liveSpoken by rememberUpdatedState(spokenChars ?: 0)
    val isAppResumed = rememberIsAppResumed()

    if (spokenChars != null && spokenChars > count) {
        count = spokenChars.coerceAtMost(text.length)
    }

    LaunchedEffect(text, spokenChars == null, isAppResumed) {
        if (!isAppResumed) return@LaunchedEffect
        if (spokenChars == null) {
            timedAllowed = true
        } else {
            delay(SPEECH_GRACE_MS)
            if (liveSpoken <= 0) {
                timedAllowed = true
            }
        }
    }

    LaunchedEffect(text, timedAllowed, isAppResumed) {
        if (!isAppResumed || !timedAllowed || count >= text.length) return@LaunchedEffect
        val stepMs = (1000L / charsPerSecond.coerceAtLeast(1)).coerceAtLeast(8L)
        while (count < text.length) {
            delay(stepMs)
            count++
        }
    }

    val revealed = maxOf(count, spokenChars ?: 0).coerceIn(0, text.length)
    return text.take(revealed)
}

@Composable
expect fun rememberPlatformSpeaker(
    voicePreference: String? = UserPrefs.tutorVoice
): PlatformSpeaker

@Composable
expect fun PlatformTextToSpeech(
    text: String,
    languageTag: String,
    content: @Composable (isSpeaking: Boolean, speak: () -> Unit, isAvailable: Boolean) -> Unit
)

@Composable
fun SpeechPlaybackButton(text: String, languageTag: String) {
    val speaker = rememberPlatformSpeaker(UserPrefs.tutorVoice)
    com.example.basekmp.components.ListenButton(
        onClick = {
            if (speaker.isSpeaking) {
                speaker.stop()
            } else {
                speaker.speak(text, languageTag, UserPrefs.tutorVoice)
            }
        },
        isPlaying = speaker.isSpeaking
    )
}
