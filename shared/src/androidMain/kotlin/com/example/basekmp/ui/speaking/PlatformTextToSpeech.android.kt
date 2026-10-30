package com.example.basekmp.ui.speaking

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale
import kotlin.random.Random

class AndroidPlatformSpeaker(
    private val tts: TextToSpeech,
    private val _isSpeaking: MutableState<Boolean>,
    private val _isReady: MutableState<Boolean>
) : PlatformSpeaker {
    private val randomStyleByLocale = mutableMapOf<String, VoiceStyle>()
    private var pendingSpeech: PendingSpeech? = null
    private var speedMultiplier = 1f
    private var currentStyleRate = 1f

    override val isSpeaking: Boolean get() = _isSpeaking.value
    override val isAvailable: Boolean get() = _isReady.value

    override fun speak(text: String, languageTag: String, voicePreference: String?) {
        if (text.isBlank()) return

        val request = PendingSpeech(text, languageTag, voicePreference)
        if (!_isReady.value) {
            // TextToSpeech initialization is asynchronous. Keep the latest QUEUE_FLUSH request
            // so the first tap is not silently lost while the engine starts.
            pendingSpeech = request
            _isSpeaking.value = true
            return
        }
        speakNow(request)
    }

    internal fun onEngineReady() {
        val request = pendingSpeech ?: return
        pendingSpeech = null
        speakNow(request)
    }

    internal fun onEngineUnavailable() {
        pendingSpeech = null
        _isReady.value = false
        _isSpeaking.value = false
    }

    private fun speakNow(request: PendingSpeech) {
        runCatching {
            val requestedLocale = Locale.forLanguageTag(request.languageTag)
                .takeIf { it.language.isNotBlank() }
                ?: Locale.US
            val locale = resolveLocale(requestedLocale)
            val style = voiceStyleFor(request.voicePreference, locale)
            val selectedVoice = style.gender?.let { pickGenderedVoice(locale, it) }

            val languageResult = tts.setLanguage(locale)
            val voiceApplied = selectedVoice != null &&
                runCatching { tts.setVoice(selectedVoice) == TextToSpeech.SUCCESS }.getOrDefault(false)
            val hasRealGenderVoice = voiceApplied && isRealGenderVoice(selectedVoice, style.gender)

            // A real gendered engine voice already has its own pitch/rate. When Android only
            // offers a generic voice, use the original app's pitch/rate presets as a fallback.
            currentStyleRate = if (hasRealGenderVoice) 1f else style.rate
            val pitch = if (hasRealGenderVoice) 1f else style.pitch
            runCatching { tts.setPitch(pitch) }
            runCatching { tts.setSpeechRate(currentStyleRate * speedMultiplier) }

            // If a requested voice could not be applied, retain the locale selected above.
            // The engine's current locale remains the best available fallback.
            if (languageResult < TextToSpeech.LANG_AVAILABLE && selectedVoice == null) {
                tts.defaultVoice?.let { defaultVoice -> runCatching { tts.voice = defaultVoice } }
            }

            _isSpeaking.value = true
            val result = tts.speak(
                request.text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "engla_${request.text.hashCode()}_${System.currentTimeMillis()}"
            )
            if (result == TextToSpeech.ERROR) _isSpeaking.value = false
        }.onFailure {
            _isSpeaking.value = false
        }
    }

    private fun resolveLocale(requested: Locale): Locale {
        if (supportsLanguage(requested)) return requested
        if (supportsLanguage(Locale.US)) return Locale.US

        // Match the original en-US fallback, then use an installed engine voice only if the
        // device has no en-US data at all.
        val englishVoiceLocale = availableVoices()
            .firstOrNull { it.locale.language.equals(Locale.US.language, ignoreCase = true) }
            ?.locale
        return englishVoiceLocale ?: tts.defaultVoice?.locale ?: Locale.US
    }

    private fun supportsLanguage(locale: Locale): Boolean =
        runCatching { tts.isLanguageAvailable(locale) >= TextToSpeech.LANG_AVAILABLE }.getOrDefault(false)

    private fun voiceStyleFor(preference: String?, locale: Locale): VoiceStyle {
        return when (preference.orEmpty().trim().lowercase(Locale.ROOT)) {
            "female", "emma" -> VoiceStyle.FEMALE
            "male", "james" -> VoiceStyle.MALE
            "robot", "sparky" -> VoiceStyle.ROBOT
            "neutral", "alex" -> VoiceStyle.NEUTRAL
            "random" -> randomStyleByLocale.getOrPut(locale.toLanguageTag()) {
                if (Random.nextBoolean()) VoiceStyle.FEMALE else VoiceStyle.MALE
            }
            else -> VoiceStyle.DEFAULT
        }
    }

    private fun pickGenderedVoice(locale: Locale, gender: String): Voice? {
        val sameLanguage = availableVoices()
            .filter { it.locale.language.equals(locale.language, ignoreCase = true) }
            .sortedBy { it.name.lowercase(Locale.ROOT) }
        val sameCountry = sameLanguage.filter {
            locale.country.isBlank() || it.locale.country.equals(locale.country, ignoreCase = true)
        }
        val candidates = sameCountry.ifEmpty { sameLanguage }
        if (candidates.isEmpty()) return null

        candidates.firstOrNull { knownVoiceGender(it) == gender }?.let { return it }
        candidates.firstOrNull { nameVoiceGender(it) == gender }?.let { return it }

        // The original app falls back to the first/last installed voice when its engine does
        // not expose a recognizable gendered voice, then simulates the style with pitch/rate.
        if (candidates.size < 2) return null
        return if (gender == "female") candidates.first() else candidates.last()
    }

    private fun availableVoices(): List<Voice> = runCatching {
        tts.voices.orEmpty().filter { voice ->
            !voice.isNetworkConnectionRequired && "notInstalled" !in voice.features.orEmpty()
        }
    }.getOrDefault(emptyList())

    private fun isRealGenderVoice(voice: Voice, gender: String?): Boolean =
        gender != null && (knownVoiceGender(voice) == gender || nameVoiceGender(voice) == gender)

    private fun knownVoiceGender(voice: Voice): String? {
        val name = voice.name.lowercase(Locale.ROOT)
        val localeKey = name.substringBefore("-x-")
        val variant = name.substringAfter("-x-", "").substringBefore('-')
        val ids = KNOWN_VOICES[localeKey] ?: return null
        return when {
            variant in ids.male -> "male"
            variant in ids.female -> "female"
            else -> null
        }
    }

    private fun nameVoiceGender(voice: Voice): String? {
        val name = voice.name.lowercase(Locale.ROOT)
        return when {
            "female" in name -> "female"
            "male" in name -> "male"
            else -> null
        }
    }

    override fun stop() {
        pendingSpeech = null
        runCatching { tts.stop() }
        _isSpeaking.value = false
    }

    override fun setSpeed(speed: Float) {
        if (speed <= 0f || !speed.isFinite()) return
        speedMultiplier = speed
        runCatching { tts.setSpeechRate(currentStyleRate * speedMultiplier) }
    }

    private data class PendingSpeech(
        val text: String,
        val languageTag: String,
        val voicePreference: String?
    )

    private enum class VoiceStyle(val gender: String?, val pitch: Float, val rate: Float) {
        DEFAULT(null, 1.0f, 1.0f),
        NEUTRAL(null, 1.0f, 0.95f),
        MALE("male", 0.65f, 0.96f),
        FEMALE("female", 1.25f, 1.02f),
        ROBOT(null, 0.5f, 0.8f)
    }

    private data class GenderVoiceIds(val male: Set<String>, val female: Set<String>)

    private companion object {
        val KNOWN_VOICES = mapOf(
            "en-us" to GenderVoiceIds(
                male = setOf("iol", "iom", "tpd"),
                female = setOf("iob", "iog", "sfg", "tpc", "tpf")
            ),
            "en-gb" to GenderVoiceIds(
                male = setOf("gbb", "gbd", "rjs"),
                female = setOf("gba", "gbc", "gbg", "fis")
            ),
            "en-au" to GenderVoiceIds(
                male = setOf("aub", "aud"),
                female = setOf("afh", "aua", "auc")
            )
        )
    }
}

private class SpeakerHolder {
    var speaker: AndroidPlatformSpeaker? = null
    var engineInitialized = false
    var listenerAttached = false
}

@Composable
actual fun rememberPlatformSpeaker(): PlatformSpeaker {
    val context = LocalContext.current
    val isSpeaking = remember { mutableStateOf(false) }
    val isReady = remember { mutableStateOf(false) }
    val speakerHolder = remember(context) { SpeakerHolder() }

    val engine = remember(context) {
        TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                speakerHolder.engineInitialized = true
                isReady.value = true
                if (speakerHolder.listenerAttached) {
                    speakerHolder.speaker?.onEngineReady()
                }
            } else {
                speakerHolder.engineInitialized = false
                isReady.value = false
                speakerHolder.speaker?.onEngineUnavailable()
            }
        }
    }

    val speaker = remember(engine) {
        AndroidPlatformSpeaker(engine, isSpeaking, isReady).also { instance ->
            speakerHolder.speaker = instance
        }
    }

    DisposableEffect(engine) {
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                isSpeaking.value = false
            }

            @Deprecated("Use onError(String?, Int) instead")
            override fun onError(utteranceId: String?) {
                isSpeaking.value = false
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                isSpeaking.value = false
            }
        })
        speakerHolder.listenerAttached = true
        if (speakerHolder.engineInitialized) speaker.onEngineReady()

        onDispose {
            speakerHolder.listenerAttached = false
            speakerHolder.engineInitialized = false
            speaker.onEngineUnavailable()
            runCatching { engine.stop() }
            runCatching { engine.shutdown() }
        }
    }

    return speaker
}

@Composable
actual fun PlatformTextToSpeech(
    text: String,
    languageTag: String,
    content: @Composable (isSpeaking: Boolean, speak: () -> Unit, isAvailable: Boolean) -> Unit
) {
    val speaker = rememberPlatformSpeaker()
    content(
        speaker.isSpeaking,
        { speaker.speak(text, languageTag) },
        speaker.isAvailable
    )
}
