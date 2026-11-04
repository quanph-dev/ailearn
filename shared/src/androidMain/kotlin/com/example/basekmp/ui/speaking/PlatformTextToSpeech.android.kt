package com.example.basekmp.ui.speaking

import android.os.Handler
import android.os.Looper
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val MIN_CHUNK_CHARS = 40
private const val MAX_CHUNK_CHARS = 160

private enum class VoiceGender {
    MALE,
    FEMALE
}

internal enum class VoiceStyle(val pitch: Float, val rate: Float) {
    NEUTRAL(1.0f, 0.95f),
    MALE(0.65f, 0.96f),
    FEMALE(1.25f, 1.02f),
    ROBOT(0.5f, 0.8f)
}

private data class GenderVoiceIds(val male: Set<String>, val female: Set<String>)

private val KNOWN_VOICES: Map<String, GenderVoiceIds> = mapOf(
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

private val NON_LATIN: Set<String> = setOf(
    "ar", "ur", "fa", "hi", "bn", "zh", "ja", "ko", "ru", "uk", "th", "he"
)

private val QUOTED = Regex(
    "\"[^\"\\n]+\"|“[^”\\n]+”|«[^»\\n]+»|„[^“”\\n]+[“”]|「[^」\\n]+」|『[^』\\n]+』"
)

private val VIETNAMESE_DIACRITICS = Regex(
    "[\\u0102\\u0103\\u00C2\\u00E2\\u0110\\u0111\\u00CA\\u00EA\\u00D4\\u00F4\\u01A0\\u01A1\\u01AF\\u01B0\\u1EA0-\\u1EF9]"
)
private val SPEECH_DELIMITERS = Regex("${QUOTED.pattern}|[()]")
private val PARENTHESIZED = Regex("\\([^()]*\\)")
private val VIETNAMESE_ACCENTED_WORDS = setOf(
    "chào", "cảm", "ơn", "có", "là", "và", "nhé", "tên", "bạn", "tôi", "đã", "nói", "thế", "này"
)
private val ENGLISH_CUES = setOf(
    "hello", "hi", "good", "morning", "thank", "thanks", "you", "nice", "meet", "please",
    "my", "name", "is", "how", "are", "what", "the", "and", "your", "to", "i", "am"
)

private fun hasNativeLanguageEvidence(text: String, locale: Locale): Boolean {
    if (locale.language.equals("vi", ignoreCase = true)) {
        if (VIETNAMESE_DIACRITICS.containsMatchIn(text)) return true
        return text.lowercase(Locale.ROOT).split(Regex("[^\\p{L}]+")).any { it in VIETNAMESE_ACCENTED_WORDS }
    }
    return text.any { it.code > 127 && it.isLetter() }
}

private fun hasEnglishEvidence(text: String): Boolean =
    text.lowercase(Locale.ROOT).split(Regex("[^\\p{L}]+")).any { it in ENGLISH_CUES }

internal fun voiceStyleFor(pref: String?): VoiceStyle {
    return when (pref?.trim()?.lowercase(Locale.ROOT)) {
        "female", "emma" -> VoiceStyle.FEMALE
        "male", "james" -> VoiceStyle.MALE
        "robot", "sparky" -> VoiceStyle.ROBOT
        "neutral", "alex" -> VoiceStyle.NEUTRAL
        "random" -> if (Random.nextBoolean()) VoiceStyle.MALE else VoiceStyle.FEMALE
        else -> VoiceStyle.FEMALE
    }
}

private fun knownGender(voice: Voice): VoiceGender? {
    val lower = voice.name.lowercase(Locale.ROOT)
    val localeKey = lower.substringBefore("-x-")
    val variant = lower.substringAfter("-x-", "").substringBefore('-')
    val ids = KNOWN_VOICES[localeKey] ?: return null
    return when {
        variant in ids.male -> VoiceGender.MALE
        variant in ids.female -> VoiceGender.FEMALE
        else -> null
    }
}

private fun isRealGenderVoice(voice: Voice?, style: VoiceStyle): Boolean {
    if (voice == null || (style != VoiceStyle.MALE && style != VoiceStyle.FEMALE)) {
        return false
    }
    val wanted = if (style == VoiceStyle.MALE) VoiceGender.MALE else VoiceGender.FEMALE
    val lower = voice.name.lowercase(Locale.ROOT)
    val nameMatches = if (wanted == VoiceGender.FEMALE) {
        "female" in lower
    } else {
        "male" in lower && "female" !in lower
    }
    return knownGender(voice) == wanted || nameMatches
}

private fun pickGenderedVoice(
    tts: TextToSpeech,
    locale: Locale,
    style: VoiceStyle
): Voice? {
    val isMale = style == VoiceStyle.MALE
    val isFemale = style == VoiceStyle.FEMALE
    if (!isMale && !isFemale) return null

    val voices = runCatching { tts.voices }.getOrNull() ?: return null
    val language = locale.language
    val country = locale.country
    val sameLanguage = voices
        .filter { it.locale.language.equals(language, ignoreCase = true) }
        .filter { voice -> runCatching { !voice.isNetworkConnectionRequired }.getOrDefault(true) }
        .filter { voice ->
            runCatching {
                val features = voice.features
                features == null || "notInstalled" !in features
            }.getOrDefault(true)
        }
        .sortedBy { it.name }

    if (sameLanguage.isEmpty()) return null

    val candidates = if (country.isNotBlank()) {
        val exactCountry = sameLanguage.filter {
            it.locale.country.equals(country, ignoreCase = true)
        }
        exactCountry.ifEmpty { sameLanguage }
    } else {
        sameLanguage
    }

    val wantedGender = if (isMale) VoiceGender.MALE else VoiceGender.FEMALE
    candidates.firstOrNull { knownGender(it) == wantedGender }?.let { return it }

    candidates.firstOrNull { voice ->
        val lower = voice.name.lowercase(Locale.ROOT)
        if (isFemale) {
            "female" in lower
        } else {
            "male" in lower && "female" !in lower
        }
    }?.let { return it }

    if (candidates.size < 2) return null
    return if (isFemale) candidates.first() else candidates.last()
}

internal fun splitForSpeech(text: String, maxChars: Int = MAX_CHUNK_CHARS): List<String> {
    if (text.length <= maxChars) return listOf(text)

    val rawChunks = ArrayList<String>()
    val sb = StringBuilder()
    for (i in 0 until text.length) {
        val ch = text[i]
        sb.append(ch)
        val isSentenceEnd = ch in ".!?…。！？؟۔\n" && sb.length >= MIN_CHUNK_CHARS
        if (isSentenceEnd || sb.length >= maxChars) {
            if (sb.length >= maxChars && !isSentenceEnd) {
                val lastSpace = sb.lastIndexOf(" ")
                if (lastSpace > MIN_CHUNK_CHARS) {
                    rawChunks.add(sb.substring(0, lastSpace + 1))
                    val remainder = sb.substring(lastSpace + 1)
                    sb.setLength(0)
                    sb.append(remainder)
                    continue
                }
            }
            rawChunks.add(sb.toString())
            sb.setLength(0)
        }
    }
    if (sb.isNotEmpty()) {
        rawChunks.add(sb.toString())
    }
    val filtered = rawChunks.filter { it.isNotBlank() }
    return filtered.ifEmpty { listOf(text) }
}

internal fun speechSegments(
    text: String,
    primary: Locale,
    alt: Locale?,
    quotedLanguage: Locale? = null
): List<Pair<String, Locale>> {
    if (quotedLanguage != null) {
        if (quotedLanguage.language.equals(primary.language, ignoreCase = true)) {
            return listOf(text to primary)
        }
        val parts = mutableListOf<Pair<String, Locale>>()
        var last = 0
        for (match in QUOTED.findAll(text)) {
            if (match.range.first > last) parts.add(text.substring(last, match.range.first) to primary)
            parts.add(match.value to quotedLanguage)
            last = match.range.last + 1
        }
        if (last < text.length) parts.add(text.substring(last) to primary)
        return parts.ifEmpty { listOf(text to primary) }
    }
    if (alt == null || alt.language.equals(primary.language, ignoreCase = true)) {
        return listOf(text to primary)
    }

    val primaryIsLatin = primary.language !in NON_LATIN
    val altIsNonLatin = alt.language in NON_LATIN
    val altIsLatin = !altIsNonLatin
    val rawSegments = ArrayList<Pair<String, Locale>>()

    if (primaryIsLatin != altIsLatin) {
        val latinLocale = if (!altIsNonLatin) alt else primary
        val nonLatinLocale = if (!altIsNonLatin) primary else alt
        var currentLocale: Locale? = null
        var index = 0
        var segmentStart = 0
        while (index < text.length) {
            val cp = text.codePointAt(index)
            if (Character.isLetter(cp)) {
                val charLocale = if (Character.UnicodeScript.of(cp) == Character.UnicodeScript.LATIN) {
                    latinLocale
                } else {
                    nonLatinLocale
                }
                if (currentLocale != null && currentLocale != charLocale) {
                    rawSegments.add(text.substring(segmentStart, index) to currentLocale)
                    segmentStart = index
                }
                currentLocale = charLocale
            }
            index += Character.charCount(cp)
        }
        rawSegments.add(text.substring(segmentStart) to (currentLocale ?: primary))
    } else {
        val primaryIsEn = primary.language.equals("en", ignoreCase = true)
        val altIsEn = alt.language.equals("en", ignoreCase = true)
        val unquoted = QUOTED.replace(text, " ")
        val nonEnLocale = if (primaryIsEn != altIsEn) {
            if (primaryIsEn) alt else primary
        } else null

        val (outerLocale, quotedLocale) = if (primaryIsEn != altIsEn) {
            val enLocale = if (primaryIsEn) primary else alt
            val native = requireNotNull(nonEnLocale)
            val outsideParentheses = PARENTHESIZED.replace(unquoted, " ")
                .takeIf { candidate -> candidate.any { it.isLetter() } } ?: unquoted
            val nativeOutside = hasNativeLanguageEvidence(outsideParentheses, native)
            val nativeAnywhere = hasNativeLanguageEvidence(unquoted, native) ||
                QUOTED.findAll(text).any { hasNativeLanguageEvidence(it.value, native) }
            if (!nativeOutside && !nativeAnywhere) return listOf(text to enLocale)
            (if (nativeOutside) native else enLocale) to enLocale
        } else {
            primary to alt
        }

        var last = 0
        var parenthesisDepth = 0
        for (match in SPEECH_DELIMITERS.findAll(text)) {
            if (match.range.first > last) {
                val part = text.substring(last, match.range.first)
                val locale = when {
                    parenthesisDepth > 0 && nonEnLocale != null && hasNativeLanguageEvidence(part, nonEnLocale) -> nonEnLocale
                    parenthesisDepth > 0 && nonEnLocale != null && hasEnglishEvidence(part) -> quotedLocale
                    else -> outerLocale
                }
                rawSegments.add(part to locale)
            }
            when (match.value) {
                "(" -> {
                    rawSegments.add(match.value to outerLocale)
                    parenthesisDepth++
                }
                ")" -> {
                    rawSegments.add(match.value to outerLocale)
                    parenthesisDepth = (parenthesisDepth - 1).coerceAtLeast(0)
                }
                else -> {
                    val locale = if (nonEnLocale != null && hasNativeLanguageEvidence(match.value, nonEnLocale)) {
                        nonEnLocale
                    } else quotedLocale
                    rawSegments.add(match.value to locale)
                }
            }
            last = match.range.last + 1
        }
        if (last < text.length) {
            val part = text.substring(last)
            val locale = when {
                parenthesisDepth > 0 && nonEnLocale != null && hasNativeLanguageEvidence(part, nonEnLocale) -> nonEnLocale
                parenthesisDepth > 0 && nonEnLocale != null && hasEnglishEvidence(part) -> quotedLocale
                else -> outerLocale
            }
            rawSegments.add(part to locale)
        }
    }

    val merged = ArrayList<Pair<String, Locale>>()
    for ((segText, segLocale) in rawSegments) {
        val prev = merged.lastOrNull()
        if (prev == null) {
            merged.add(segText to segLocale)
        } else if (prev.second == segLocale || segText.none { it.isLetterOrDigit() }) {
            merged[merged.lastIndex] = (prev.first + segText) to prev.second
        } else if (prev.first.none { it.isLetterOrDigit() }) {
            merged[merged.lastIndex] = (prev.first + segText) to segLocale
        } else {
            merged.add(segText to segLocale)
        }
    }
    return merged.ifEmpty { listOf(text to primary) }
}

internal class EngineSetup {
    val engineLock = Any()
    val localeFor = HashMap<String, Locale>()
    val voiceFor = HashMap<String, Voice?>()
    var appliedVoice: Voice? = null
    var appliedLocale: Locale? = null
    var appliedPitch: Float = -1f
    var appliedRate: Float = -1f
    var chunkEnds: IntArray = IntArray(0)
    @Volatile
    var utteranceTag: Int = 0
}

internal data class SpeechRequest(
    val text: String,
    val locale: Locale,
    val style: VoiceStyle,
    val speed: Float,
    val altLocale: Locale?,
    val quotedLocale: Locale?
)

private fun applyVoice(
    engineSetup: EngineSetup,
    tts: TextToSpeech,
    voiceStyle: VoiceStyle,
    requestedLocale: Locale,
    speed: Float
): Float {
    val localeTag = requestedLocale.toLanguageTag()
    var resolvedLocale = engineSetup.localeFor[localeTag]
    if (resolvedLocale == null) {
        val available = runCatching { tts.isLanguageAvailable(requestedLocale) }.getOrDefault(-1)
        resolvedLocale = if (available < 0) Locale.US else requestedLocale
        engineSetup.localeFor[localeTag] = resolvedLocale
    }

    val voiceKey = "${resolvedLocale.toLanguageTag()}:${voiceStyle.name}"
    val voice = if (engineSetup.voiceFor.containsKey(voiceKey)) {
        engineSetup.voiceFor[voiceKey]
    } else {
        pickGenderedVoice(tts, resolvedLocale, voiceStyle).also { picked ->
            engineSetup.voiceFor[voiceKey] = picked
        }
    }

    if (voice != null) {
        if (engineSetup.appliedVoice != voice || engineSetup.appliedLocale != resolvedLocale) {
            val setOk = runCatching { tts.setVoice(voice) == TextToSpeech.SUCCESS }.getOrDefault(false)
            if (setOk) {
                engineSetup.appliedVoice = voice
                engineSetup.appliedLocale = resolvedLocale
            } else {
                runCatching { tts.setLanguage(resolvedLocale) }
                engineSetup.appliedLocale = resolvedLocale
                engineSetup.appliedVoice = null
            }
        } else {
            engineSetup.appliedVoice = voice
            engineSetup.appliedLocale = resolvedLocale
        }
    } else if (engineSetup.appliedLocale != resolvedLocale || engineSetup.appliedVoice != null) {
        runCatching { tts.setLanguage(resolvedLocale) }
        engineSetup.appliedLocale = resolvedLocale
        engineSetup.appliedVoice = null
    }

    val realGender = isRealGenderVoice(engineSetup.appliedVoice, voiceStyle)
    val pitch = if (realGender) 1.0f else voiceStyle.pitch
    if (engineSetup.appliedPitch != pitch) {
        runCatching { tts.setPitch(pitch) }
        engineSetup.appliedPitch = pitch
    }
    val rate = (if (realGender) 1.0f else voiceStyle.rate) * speed
    if (engineSetup.appliedRate != rate) {
        runCatching { tts.setSpeechRate(rate) }
        engineSetup.appliedRate = rate
    }
    return rate
}

private fun performSpeak(
    engineSetup: EngineSetup,
    mainHandler: Handler,
    clearSpeaking: Runnable,
    spokenCharsState: MutableIntState,
    tts: TextToSpeech,
    request: SpeechRequest
) {
    synchronized(engineSetup.engineLock) {
        val effectiveRate = applyVoice(
            engineSetup = engineSetup,
            tts = tts,
            voiceStyle = request.style,
            requestedLocale = request.locale,
            speed = request.speed
        ).coerceAtLeast(0.25f)

        val timeoutMs = ((request.text.length.toLong() * 125L) / effectiveRate).toLong() + 10_000L
        mainHandler.removeCallbacks(clearSpeaking)
        mainHandler.postDelayed(clearSpeaking, timeoutMs)

        val segments = speechSegments(request.text, request.locale, request.altLocale, request.quotedLocale)
        val chunks = ArrayList<Pair<String, Locale>>()
        for ((segText, segLocale) in segments) {
            for (chunk in splitForSpeech(segText)) {
                chunks.add(chunk to segLocale)
            }
        }

        val ends = IntArray(chunks.size)
        var cumulative = 0
        for (i in chunks.indices) {
            cumulative += chunks[i].first.length
            ends[i] = cumulative
        }
        if (ends.isNotEmpty() && ends[ends.lastIndex] < request.text.length) {
            ends[ends.lastIndex] = request.text.length
        }

        engineSetup.chunkEnds = ends
        // Use a request id so late callbacks from an earlier identical utterance are ignored.
        engineSetup.utteranceTag += 1
        spokenCharsState.intValue = 0

        for ((index, pair) in chunks.withIndex()) {
            val (chunkText, chunkLocale) = pair
            applyVoice(
                engineSetup = engineSetup,
                tts = tts,
                voiceStyle = request.style,
                requestedLocale = chunkLocale,
                speed = request.speed
            )
            val queueMode = if (index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
            val utteranceId = "utt-${engineSetup.utteranceTag}-$index"
            runCatching {
                // A non-empty bundle makes TextToSpeech copy its current voice parameters
                // for this utterance before the next segment changes the voice.
                val params = Bundle().apply {
                    putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId)
                }
                tts.speak(chunkText, queueMode, params, utteranceId)
            }
        }
    }
}

class AndroidPlatformSpeaker internal constructor(
    private val tts: TextToSpeech,
    private val engineSetup: EngineSetup,
    private val mainHandler: Handler,
    private val clearSpeakingRunnable: Runnable,
    private val _isSpeaking: MutableState<Boolean>,
    private val _audioStarted: MutableState<Boolean>,
    private val _spokenChars: MutableIntState,
    private val _isReady: MutableState<Boolean>,
    private val _pending: MutableState<SpeechRequest?>,
    private val _warmLocales: MutableState<List<Locale>>,
    private val _isAppResumed: MutableState<Boolean>,
    private val defaultVoiceStyle: VoiceStyle
) : PlatformSpeaker {
    private var speedMultiplier: Float = 1.0f

    override val isSpeaking: Boolean get() = _isSpeaking.value
    override val isLoading: Boolean get() = _isSpeaking.value && !_audioStarted.value
    override val isPlaying: Boolean get() = _isSpeaking.value && _audioStarted.value
    override val spokenChars: Int get() = _spokenChars.intValue
    override val hasStartedSpeaking: Boolean get() = _spokenChars.intValue > 0
    override val isAvailable: Boolean get() = _isReady.value

    override fun warmUp(vararg languageTags: String) {
        if (!_isAppResumed.value) return
        val locales = languageTags.mapNotNull { tag ->
            Locale.forLanguageTag(speechLocaleTagFor(tag)).takeIf { it.language.isNotBlank() }
        }
        _warmLocales.value = locales
    }

    override fun speak(
        text: String,
        languageTag: String,
        voicePreference: String?,
        speed: Float,
        altLanguageTag: String?,
        quotedLanguageTag: String?
    ) {
        if (text.isBlank() || !_isAppResumed.value) return

        val effectiveSpeed = if (speed != 1.0f && speed > 0f && speed.isFinite()) {
            speedMultiplier = speed
            speed
        } else {
            speedMultiplier
        }

        val primaryLocale = Locale.forLanguageTag(speechLocaleTagFor(languageTag))
            .takeIf { it.language.isNotBlank() }
            ?: Locale.US
        val altLocale = altLanguageTag?.takeIf { it.isNotBlank() }?.let { tag ->
            Locale.forLanguageTag(speechLocaleTagFor(tag)).takeIf { it.language.isNotBlank() }
        }
        val quotedLocale = quotedLanguageTag?.takeIf { it.isNotBlank() }?.let { tag ->
            Locale.forLanguageTag(speechLocaleTagFor(tag)).takeIf { it.language.isNotBlank() }
        }
        val style = if (voicePreference != null) voiceStyleFor(voicePreference) else defaultVoiceStyle
        val request = SpeechRequest(
            text = text,
            locale = primaryLocale,
            style = style,
            speed = effectiveSpeed,
            altLocale = altLocale,
            quotedLocale = quotedLocale
        )

        _spokenChars.intValue = 0
        _audioStarted.value = false
        _isSpeaking.value = true
        mainHandler.removeCallbacks(clearSpeakingRunnable)
        mainHandler.postDelayed(clearSpeakingRunnable, 12_000L)

        if (_isReady.value) {
            _pending.value = null
            performSpeak(
                engineSetup = engineSetup,
                mainHandler = mainHandler,
                clearSpeaking = clearSpeakingRunnable,
                spokenCharsState = _spokenChars,
                tts = tts,
                request = request
            )
        } else {
            _pending.value = request
        }
    }

    override fun stop() {
        mainHandler.removeCallbacks(clearSpeakingRunnable)
        _pending.value = null
        engineSetup.utteranceTag += 1
        runCatching { tts.stop() }
        _isSpeaking.value = false
        _spokenChars.intValue = 0
        _audioStarted.value = false
    }

    override fun setSpeed(speed: Float) {
        if (speed <= 0f || !speed.isFinite()) return
        speedMultiplier = speed
    }
}

@Composable
actual fun rememberPlatformSpeaker(voicePreference: String?): PlatformSpeaker {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val isAppResumed = remember(lifecycle) {
        mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    val style = voiceStyleFor(voicePreference)
    val isSpeaking = remember { mutableStateOf(false) }
    val audioStarted = remember { mutableStateOf(false) }
    val spokenChars = remember { mutableIntStateOf(0) }
    val isReady = remember { mutableStateOf(false) }
    val pending = remember { mutableStateOf<SpeechRequest?>(null) }
    val warmLocales = remember { mutableStateOf<List<Locale>>(emptyList()) }
    val engineSetup = remember { EngineSetup() }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val clearSpeaking = remember {
        Runnable {
            isSpeaking.value = false
            audioStarted.value = false
        }
    }

    val tts = remember(context) {
        TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isReady.value = true
            } else {
                isReady.value = false
                pending.value = null
                isSpeaking.value = false
                audioStarted.value = false
            }
        }
    }

    DisposableEffect(tts) {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            private fun indexOf(utteranceId: String?): Int? {
                if (utteranceId == null) return null
                val prefix = "utt-${engineSetup.utteranceTag}-"
                if (!utteranceId.startsWith(prefix)) return null
                return utteranceId.substringAfterLast('-').toIntOrNull()
            }

            override fun onStart(utteranceId: String?) {
                val idx = indexOf(utteranceId) ?: return
                mainHandler.post {
                    audioStarted.value = true
                    val baseChars = if (idx == 0) {
                        0
                    } else {
                        engineSetup.chunkEnds.getOrNull(idx - 1) ?: return@post
                    }
                    if (baseChars > spokenChars.intValue) {
                        spokenChars.intValue = baseChars
                    }
                }
            }

            override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
                val idx = indexOf(utteranceId) ?: return
                mainHandler.post {
                    val baseChars = if (idx == 0) {
                        0
                    } else {
                        engineSetup.chunkEnds.getOrNull(idx - 1) ?: return@post
                    }
                    val total = baseChars + end
                    if (total > spokenChars.intValue) {
                        spokenChars.intValue = total
                    }
                }
            }

            override fun onDone(utteranceId: String?) {
                val idx = indexOf(utteranceId) ?: return
                mainHandler.post {
                    val chunkEnd = engineSetup.chunkEnds.getOrNull(idx)
                    if (chunkEnd != null && chunkEnd > spokenChars.intValue) {
                        spokenChars.intValue = chunkEnd
                    }
                    if (idx >= engineSetup.chunkEnds.size - 1) {
                        mainHandler.removeCallbacks(clearSpeaking)
                        isSpeaking.value = false
                        audioStarted.value = false
                    }
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                if (indexOf(utteranceId) == null) return
                mainHandler.post {
                    mainHandler.removeCallbacks(clearSpeaking)
                    isSpeaking.value = false
                    audioStarted.value = false
                }
            }

            override fun onError(utteranceId: String?, errorCode: Int) {
                if (indexOf(utteranceId) == null) return
                mainHandler.post {
                    mainHandler.removeCallbacks(clearSpeaking)
                    isSpeaking.value = false
                    audioStarted.value = false
                }
            }
        })

        onDispose {
            mainHandler.removeCallbacks(clearSpeaking)
            pending.value = null
            runCatching { tts.stop() }
            runCatching { tts.shutdown() }
        }
    }

    LaunchedEffect(isReady.value, pending.value, isAppResumed.value) {
        if (!isAppResumed.value) return@LaunchedEffect
        val request = pending.value
        if (isReady.value && request != null) {
            pending.value = null
            performSpeak(
                engineSetup = engineSetup,
                mainHandler = mainHandler,
                clearSpeaking = clearSpeaking,
                spokenCharsState = spokenChars,
                tts = tts,
                request = request
            )
        }
    }

    LaunchedEffect(isReady.value, warmLocales.value, style, isAppResumed.value) {
        if (!isAppResumed.value || !isReady.value) return@LaunchedEffect
        val requested = warmLocales.value
        if (requested.isEmpty() && (style == VoiceStyle.NEUTRAL || style == VoiceStyle.ROBOT)) {
            return@LaunchedEffect
        }
        val wanted = (requested + Locale.getDefault()).distinctBy { it.toLanguageTag() }
        withContext(Dispatchers.IO) {
            for (loc in wanted) {
                runCatching {
                    synchronized(engineSetup.engineLock) {
                        val tag = loc.toLanguageTag()
                        var resolved = engineSetup.localeFor[tag]
                        if (resolved == null) {
                            resolved = if (tts.isLanguageAvailable(loc) < 0) Locale.US else loc
                            engineSetup.localeFor[tag] = resolved
                        }
                        val voiceKey = "${resolved.toLanguageTag()}:${style.name}"
                        if (!engineSetup.voiceFor.containsKey(voiceKey)) {
                            engineSetup.voiceFor[voiceKey] = pickGenderedVoice(tts, resolved, style)
                        }
                    }
                }
            }
        }
    }

    val speaker = remember(tts, style, isAppResumed) {
        AndroidPlatformSpeaker(
            tts = tts,
            engineSetup = engineSetup,
            mainHandler = mainHandler,
            clearSpeakingRunnable = clearSpeaking,
            _isSpeaking = isSpeaking,
            _audioStarted = audioStarted,
            _spokenChars = spokenChars,
            _isReady = isReady,
            _pending = pending,
            _warmLocales = warmLocales,
            _isAppResumed = isAppResumed,
            defaultVoiceStyle = style
        )
    }

    DisposableEffect(lifecycle, speaker) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> isAppResumed.value = true
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP,
                Lifecycle.Event.ON_DESTROY -> {
                    isAppResumed.value = false
                    speaker.stop()
                }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
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
