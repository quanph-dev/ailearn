package com.example.basekmp.data

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import com.example.basekmp.currentTimeMillis
import com.example.basekmp.config.AppConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class LearningProgressSnapshot(
    val version: Int = 1,
    val streakCount: Int = 0,
    val learnedMinutes: Int = 0,
    val wordsMastered: Int = 0,
    val lastActiveDay: Long = 0L,
    val homeTourDone: Boolean = false,
    val history: List<LearningItem> = emptyList(),
    val pathLessons: Map<String, Int> = emptyMap(),
    val pathTrailSeen: Set<String> = emptySet(),
    val pathLevel: String? = null,
    val englishLevelChosen: Boolean = false,
    val shadowingDone: Map<String, Int> = emptyMap(),
    val savedWords: Map<String, Long> = emptyMap(),
    val shadowingSpeed: Float = 1.0f,
    val speakingDone: Map<String, Long> = emptyMap(),
    val completedLessons: Map<String, Long> = emptyMap(),
    val openedLessons: Set<String> = emptySet(),
    val nativeLanguage: String = "vi",
    val targetLanguage: String = "en",
    val englishLevel: String = "A1",
    val dailyGoalMinutes: Int = 10,
    val tutorVoice: String = "Female",
    val learnLanguageFromSurvey: Boolean = false
)

/**
 * In-memory reactive projection of persisted learner state.
 * Learning data starts empty and is hydrated by the app repository.
 */
object UserPrefs {
    // Feature Constants
    const val FEATURE_AI_TUTOR = "ai_tutor"
    const val FEATURE_LESSON = "ai_lesson"
    const val FEATURE_SPEAKING = "speaking"
    const val FEATURE_SHADOWING = "shadowing"
    const val FEATURE_LEARNING_PATH = "learning_path"

    // Reactive Metric States
    val streakCountState: MutableIntState = mutableIntStateOf(0)
    val learnedMinutesState: MutableIntState = mutableIntStateOf(0)
    val wordsMasteredState: MutableIntState = mutableIntStateOf(0)
    val lastActiveDayState: MutableLongState = mutableLongStateOf(0L)
    val homeTourDoneState: MutableState<Boolean> = mutableStateOf(false)

    // Reactive Learning History
    val learningHistoryState: SnapshotStateList<LearningItem> = mutableStateListOf()

    // Progress States
    val pathLessonsState: MutableState<Map<String, Int>> = mutableStateOf(emptyMap())
    val pathTrailSeenState: MutableState<Set<String>> = mutableStateOf(emptySet())
    private var _pathLevel: String? = null
    var pathLevel: String?
        get() = _pathLevel
        set(value) {
            if (_pathLevel != value) {
                _pathLevel = value
                changed()
            }
        }
    private var _englishLevelChosen: Boolean = false
    var englishLevelChosen: Boolean
        get() = _englishLevelChosen
        set(value) {
            if (_englishLevelChosen != value) {
                _englishLevelChosen = value
                changed()
            }
        }

    val shadowingDoneState: MutableState<Map<String, Int>> = mutableStateOf(emptyMap())
    val savedWordsState: MutableState<Map<String, Long>> = mutableStateOf(emptyMap())
    private var _shadowingSpeed: Float = 1.0f
    var shadowingSpeed: Float
        get() = _shadowingSpeed
        set(value) {
            val normalized = value.coerceIn(0.25f, 2f)
            if (_shadowingSpeed != normalized) {
                _shadowingSpeed = normalized
                changed()
            }
        }

    val speakingDoneState: MutableState<Map<String, Long>> = mutableStateOf(emptyMap())
    val completedLessonsState: MutableState<Map<String, Long>> = mutableStateOf(emptyMap())
    val openedLessonsState: MutableState<Set<String>> = mutableStateOf(emptySet())

    // User Profile
    private var _nativeLanguage: String = "vi"
    var nativeLanguage: String
        get() = _nativeLanguage
        set(value) {
            if (_nativeLanguage != value) {
                _nativeLanguage = value
                changed()
            }
        }
    private var _targetLanguage: String = "en"
    var targetLanguage: String
        get() = _targetLanguage
        set(value) {
            if (_targetLanguage != value) {
                _targetLanguage = value
                changed()
            }
        }
    var learningLanguage: String
        get() = targetLanguage
        set(value) { targetLanguage = value }
    private var _englishLevel: String = "A1"
    var englishLevel: String
        get() = _englishLevel
        set(value) {
            if (_englishLevel != value) {
                _englishLevel = value
                changed()
            }
        }
    private var _dailyGoalMinutes: Int = 10
    var dailyGoalMinutes: Int
        get() = _dailyGoalMinutes
        set(value) {
            val normalized = value.coerceIn(1, 1440)
            if (_dailyGoalMinutes != normalized) {
                _dailyGoalMinutes = normalized
                changed()
            }
        }
    private var _tutorVoice: String = "Female"
    var tutorVoice: String
        get() = _tutorVoice
        set(value) {
            if (_tutorVoice != value) {
                _tutorVoice = value
                changed()
            }
        }
    val onboardingDoneState: MutableState<Boolean> = mutableStateOf(false)
    var onboardingDone: Boolean
        get() = onboardingDoneState.value
        set(value) { onboardingDoneState.value = value }
    val firstOpenDoneState: MutableState<Boolean> = mutableStateOf(false)
    var firstOpenDone: Boolean
        get() = firstOpenDoneState.value
        set(value) { firstOpenDoneState.value = value }
    private var _learnLanguageFromSurvey: Boolean = false
    var learnLanguageFromSurvey: Boolean
        get() = _learnLanguageFromSurvey
        set(value) {
            if (_learnLanguageFromSurvey != value) {
                _learnLanguageFromSurvey = value
                changed()
            }
        }

    private val progressJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var persistenceSink: ((String) -> Unit)? = null
    private var isRestoringProgress = false

    fun setProgressPersistenceSink(sink: ((String) -> Unit)?) {
        persistenceSink = sink
    }

    private fun changed() {
        if (!isRestoringProgress) runCatching { persistenceSink?.invoke(serializeLearningProgress()) }
    }

    fun serializeLearningProgress(): String = progressJson.encodeToString(
        LearningProgressSnapshot(
            streakCount = streakCountState.intValue,
            learnedMinutes = learnedMinutesState.intValue,
            wordsMastered = wordsMasteredState.intValue,
            lastActiveDay = lastActiveDayState.longValue,
            homeTourDone = homeTourDoneState.value,
            history = learningHistoryState.takeLast(MAX_HISTORY_ITEMS),
            pathLessons = pathLessonsState.value,
            pathTrailSeen = pathTrailSeenState.value,
            pathLevel = pathLevel,
            englishLevelChosen = englishLevelChosen,
            shadowingDone = shadowingDoneState.value,
            savedWords = savedWordsState.value,
            shadowingSpeed = shadowingSpeed,
            speakingDone = speakingDoneState.value,
            completedLessons = completedLessonsState.value,
            openedLessons = openedLessonsState.value,
            nativeLanguage = nativeLanguage,
            targetLanguage = targetLanguage,
            englishLevel = englishLevel,
            dailyGoalMinutes = dailyGoalMinutes,
            tutorVoice = tutorVoice,
            learnLanguageFromSurvey = learnLanguageFromSurvey
        )
    )

    fun restoreLearningProgress(serialized: String): Boolean {
        val snapshot = runCatching { progressJson.decodeFromString<LearningProgressSnapshot>(serialized) }.getOrNull()
            ?: return false
        isRestoringProgress = true
        try {
            streakCountState.intValue = snapshot.streakCount.coerceAtLeast(0)
            learnedMinutesState.intValue = snapshot.learnedMinutes.coerceAtLeast(0)
            wordsMasteredState.intValue = snapshot.wordsMastered.coerceAtLeast(0)
            lastActiveDayState.longValue = snapshot.lastActiveDay.coerceAtLeast(0L)
            homeTourDoneState.value = snapshot.homeTourDone
            learningHistoryState.clear()
            learningHistoryState.addAll(snapshot.history.takeLast(MAX_HISTORY_ITEMS))
            pathLessonsState.value = snapshot.pathLessons.mapValues { (_, score) -> score.coerceIn(0, 100) }
            pathTrailSeenState.value = snapshot.pathTrailSeen
            pathLevel = snapshot.pathLevel
            englishLevelChosen = snapshot.englishLevelChosen
            shadowingDoneState.value = snapshot.shadowingDone.mapValues { (_, score) -> score.coerceIn(0, 100) }
            savedWordsState.value = snapshot.savedWords
            shadowingSpeed = snapshot.shadowingSpeed
            speakingDoneState.value = snapshot.speakingDone
            completedLessonsState.value = snapshot.completedLessons
            openedLessonsState.value = snapshot.openedLessons
            nativeLanguage = snapshot.nativeLanguage
            targetLanguage = snapshot.targetLanguage
            englishLevel = snapshot.englishLevel
            dailyGoalMinutes = snapshot.dailyGoalMinutes
            tutorVoice = snapshot.tutorVoice
            learnLanguageFromSurvey = snapshot.learnLanguageFromSurvey
        } finally {
            isRestoringProgress = false
        }
        return true
    }

    var wordsMastered: Int
        get() = wordsMasteredState.intValue
        set(value) {
            wordsMasteredState.intValue = value.coerceAtLeast(0)
            changed()
        }

    var learnedMinutes: Int
        get() = learnedMinutesState.intValue
        set(value) {
            learnedMinutesState.intValue = value.coerceAtLeast(0)
            changed()
        }

    fun recordPathLesson(lessonId: String, score: Int) = markPathLessonDone(lessonId, score)

    fun recordPracticeDone(feature: String, score: Int, total: Int) {
        val now = currentTimeMillis()
        learningHistoryState.add(0, LearningItem(feature.replaceFirstChar { it.uppercase() }, now, score, "$feature Practice", total, englishLevel))
        recordStreakDay()
        changed()
    }

    // Subscription state is supplied by the billing layer; no debug grant is assumed.
    var isPro: Boolean = AppConfig.IS_PRO_DEFAULT
    var demoPremium: Boolean = false
    var proFromDebugGrant: Boolean = false
    var hasPlaySubscription: Boolean = false

    fun getGenerationQuotaApplies(): Boolean {
        if (AppConfig.BYPASS_GENERATION_QUOTA) return false
        return !isPro && !demoPremium && !proFromDebugGrant && !hasPlaySubscription
    }

    fun freeLessonChatsLeft(): Int = AppConfig.UNLIMITED_AI_SLOTS

    // Activity & Progress tracking
    fun addLearnedMinutes(minutes: Int) {
        if (minutes > 0) {
            learnedMinutesState.intValue += minutes
            changed()
        }
    }

    fun incWordsMastered(count: Int = 1) {
        if (count > 0) {
            wordsMasteredState.intValue += count
            changed()
        }
    }

    fun todayEpochDay(): Long = currentTimeMillis() / MILLIS_PER_DAY

    fun getLearnLanguage(): String = targetLanguage
    fun setLearnLanguage(it: String) { targetLanguage = it }
    fun getDailyGoal(): String = "$dailyGoalMinutes min / day"
    fun setDailyGoal(it: String) {
        val mins = it.substringBefore(" ").toIntOrNull() ?: 10
        dailyGoalMinutes = mins
    }
    fun clearAllSessions() { /* preserve core data */ }
    fun clearTimedSessions() { /* preserve core data */ }

    fun recordStreakDay() {
        val today = currentTimeMillis() / MILLIS_PER_DAY
        val lastDay = lastActiveDayState.longValue
        if (lastDay == today) return
        streakCountState.intValue = if (lastDay == today - 1L) {
            streakCountState.intValue + 1
        } else {
            1
        }
        lastActiveDayState.longValue = today
        changed()
    }

    fun addLearning(title: String, score: Int = 0, detail: String = "", total: Int = 100, level: String = "") {
        val item = LearningItem(
            title = title,
            timestamp = currentTimeMillis(),
            score = score,
            detail = detail,
            total = total,
            level = level
        )
        learningHistoryState.add(0, item)
        changed()
    }

    fun removeLearning(item: LearningItem) {
        if (learningHistoryState.remove(item)) changed()
    }

    fun clearAllHistory() {
        learningHistoryState.clear()
        changed()
    }

    fun markPathLessonDone(id: String, score: Int) {
        val current = pathLessonsState.value.toMutableMap()
        val prev = current[id] ?: 0
        if (score >= prev) {
            current[id] = score.coerceIn(0, 100)
            pathLessonsState.value = current
            changed()
        }
    }

    fun pathTrailSeen(): Set<String> = pathTrailSeenState.value

    fun markPathTrailSeen(ids: Collection<String>) {
        pathTrailSeenState.value = pathTrailSeenState.value + ids
        changed()
    }

    fun markShadowingDone(id: String, score: Int = 100) {
        val current = shadowingDoneState.value.toMutableMap()
        val prev = current[id] ?: 0
        if (score >= prev) {
            current[id] = score.coerceIn(0, 100)
            shadowingDoneState.value = current
            changed()
        }
    }

    fun saveWords(words: Collection<String>): Int {
        val current = savedWordsState.value.toMutableMap()
        val now = currentTimeMillis()
        var added = 0
        for (w in words) {
            val key = w.lowercase().trim()
            if (key.isNotEmpty() && !current.containsKey(key)) {
                current[key] = now
                added++
            }
        }
        if (added > 0) {
            savedWordsState.value = current
            incWordsMastered(added)
        }
        return added
    }

    fun removeSavedWord(word: String) {
        val key = word.lowercase().trim()
        val current = savedWordsState.value.toMutableMap()
        if (current.remove(key) != null) {
            savedWordsState.value = current
            changed()
        }
    }

    fun speakingDone(): Map<String, Long> = speakingDoneState.value

    fun markSpeakingDone(difficulty: String, type: String) {
        val current = speakingDoneState.value.toMutableMap()
        current["$difficulty:$type"] = currentTimeMillis()
        speakingDoneState.value = current
        changed()
    }

    fun markSpeakingDone(sentence: String) {
        markSpeakingDone(englishLevel, sentence)
    }

    fun completedLessons(): Map<String, Long> = completedLessonsState.value

    fun markLessonCompleted(key: String) {
        val current = completedLessonsState.value.toMutableMap()
        current[key] = currentTimeMillis()
        completedLessonsState.value = current
        changed()
    }

    fun openedLessons(): Set<String> = openedLessonsState.value

    fun markLessonOpened(key: String) {
        openedLessonsState.value = openedLessonsState.value + key
        changed()
    }

    fun lessonStarted(key: String): Boolean {
        return openedLessonsState.value.contains(key) || completedLessonsState.value.containsKey(key)
    }

    private const val MILLIS_PER_DAY = 86_400_000L
    private const val MAX_HISTORY_ITEMS = 500
}
