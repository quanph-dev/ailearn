package com.example.basekmp.data

import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.mutableIntStateOf
import com.example.basekmp.config.AppConfig
import com.example.basekmp.currentTimeMillis
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
private data class PlanProgressSnapshot(
    val version: Int = 1,
    val goalId: String? = null,
    val name: String = "",
    val goalNote: String = "",
    val xp: Int = 0,
    val diamonds: Int = 0,
    val celebratedDay: Int = 0,
    val startedEpochDay: Long? = null,
    val completedDays: List<Int> = emptyList(),
    val doneActivities: Map<String, List<PlanProgress.Activity>> = emptyMap(),
    val wordsPerDay: Map<String, Int> = emptyMap(),
    val seenIds: Map<String, List<String>> = emptyMap(),
    val seenContent: Map<String, List<String>> = emptyMap()
)

/** Local 30-day plan state, persisted by the platform app repository. */
class PlanProgress private constructor() {

    @Serializable
    enum class Activity(val key: String) {
        VIDEO("video"),
        QUIZ("quiz"),
        SPEAKING(UserPrefs.FEATURE_SPEAKING),
        TALK("talk")
    }

    companion object {
        const val DAY_XP: Int = 50
        const val DAY_DIAMONDS: Int = 10
        const val LEVEL_XP: Int = 100
        private const val MILLIS_PER_DAY: Long = 86_400_000L

        val INSTANCE: PlanProgress by lazy { PlanProgress() }

        fun get(context: Any? = null): PlanProgress = INSTANCE
    }

    val revision: MutableIntState = mutableIntStateOf(0)

    private val _completedDays = mutableSetOf<Int>()
    private val _doneActivities = mutableMapOf<Int, MutableSet<Activity>>()
    private val _wordsPerDay = mutableMapOf<Int, Int>()
    private val _seenIds = mutableMapOf<String, MutableSet<String>>()
    private val _seenContent = mutableMapOf<String, MutableSet<String>>()
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private var persistenceSink: ((String) -> Unit)? = null
    private var startedEpochDay: Long? = null

    var goalId: String? = null
        private set
    var name: String = ""
        private set
    var goalNote: String = ""
        private set
    var xp: Int = 0
        private set
    var diamonds: Int = 0
        private set
    var celebratedDay: Int = 0
        private set

    val level: Int
        get() = (xp / LEVEL_XP) + 1

    val xpToNextLevel: Int
        get() = LEVEL_XP - (xp % LEVEL_XP)

    private fun changed() {
        revision.intValue += 1
        runCatching { persistenceSink?.invoke(serialize()) }
    }

    fun setPersistenceSink(sink: ((String) -> Unit)?) {
        persistenceSink = sink
    }

    fun serialize(): String = json.encodeToString(
        PlanProgressSnapshot(
            goalId = goalId,
            name = name,
            goalNote = goalNote,
            xp = xp,
            diamonds = diamonds,
            celebratedDay = celebratedDay,
            startedEpochDay = startedEpochDay,
            completedDays = _completedDays.sorted(),
            doneActivities = _doneActivities.mapKeys { it.key.toString() }.mapValues { (_, value) -> value.toList() },
            wordsPerDay = _wordsPerDay.mapKeys { it.key.toString() },
            seenIds = _seenIds.mapValues { (_, value) -> value.toList() },
            seenContent = _seenContent.mapValues { (_, value) -> value.toList() }
        )
    )

    fun restore(serialized: String): Boolean {
        val snapshot = runCatching { json.decodeFromString<PlanProgressSnapshot>(serialized) }.getOrNull() ?: return false
        goalId = snapshot.goalId?.takeIf { it.isNotBlank() }
        name = snapshot.name
        goalNote = snapshot.goalNote
        xp = snapshot.xp.coerceAtLeast(0)
        diamonds = snapshot.diamonds.coerceAtLeast(0)
        celebratedDay = snapshot.celebratedDay.coerceAtLeast(0)
        startedEpochDay = snapshot.startedEpochDay
        _completedDays.clear()
        _completedDays.addAll(snapshot.completedDays.filter { it in 1..GoalPlan.DAYS })
        _doneActivities.clear()
        snapshot.doneActivities.forEach { (key, values) ->
            key.toIntOrNull()?.takeIf { it in 1..GoalPlan.DAYS }?.let { _doneActivities[it] = values.toMutableSet() }
        }
        _wordsPerDay.clear()
        snapshot.wordsPerDay.forEach { (key, count) ->
            key.toIntOrNull()?.takeIf { it in 1..GoalPlan.DAYS }?.let { _wordsPerDay[it] = count.coerceAtLeast(0) }
        }
        _seenIds.clear()
        snapshot.seenIds.forEach { (key, values) -> _seenIds[key] = values.toMutableSet() }
        _seenContent.clear()
        snapshot.seenContent.forEach { (key, values) -> _seenContent[key] = values.toMutableSet() }
        revision.intValue += 1
        return true
    }

    fun start(goal: String, name: String, goalNote: String = "") {
        goalId = goal.takeIf { it.isNotBlank() }
        this.name = name.trim()
        this.goalNote = goalNote.trim()
        xp = 0
        diamonds = 0
        startedEpochDay = currentTimeMillis() / MILLIS_PER_DAY
        _completedDays.clear()
        _doneActivities.clear()
        _wordsPerDay.clear()
        _seenIds.clear()
        _seenContent.clear()
        celebratedDay = 0
        changed()
    }

    fun completedDays(): Set<Int> = _completedDays.toSet()

    fun currentDay(): Int {
        for (day in 1..GoalPlan.DAYS) {
            if (!_completedDays.contains(day)) return day
        }
        return GoalPlan.DAYS + 1
    }

    fun done(day: Int): Set<Activity> = _doneActivities[day]?.toSet() ?: emptySet()

    fun markDone(day: Int, activity: Activity) {
        if (day !in 1..GoalPlan.DAYS || !isUnlocked(day)) return
        val set = _doneActivities.getOrPut(day) { mutableSetOf() }
        if (set.add(activity)) changed()
    }

    fun isUnlocked(day: Int): Boolean {
        if (goalId == null || day !in 1..GoalPlan.DAYS) return false
        if (AppConfig.BYPASS_PAYWALL) return true
        return daysUntilUnlocked(day) == 0
    }

    fun todayDay(): Int {
        val startDay = startedEpochDay ?: return 1
        val currentEpochDay = currentTimeMillis() / MILLIS_PER_DAY
        return (currentEpochDay - startDay + 1L).coerceIn(1L, GoalPlan.DAYS.toLong()).toInt()
    }

    fun daysUntilUnlocked(day: Int): Int {
        if (goalId == null || day !in 1..GoalPlan.DAYS) return 0
        if (_completedDays.contains(day) || AppConfig.BYPASS_PAYWALL) return 0
        return maxOf(day - todayDay(), 0)
    }

    fun completeDay(day: Int): Boolean {
        if (day != currentDay() || !_doneActivities[day].orEmpty().containsAll(Activity.entries)) return false
        if (!_completedDays.add(day)) return false
        xp += DAY_XP
        diamonds += DAY_DIAMONDS
        UserPrefs.recordStreakDay()
        changed()
        return true
    }

    fun words(day: Int): Int = _wordsPerDay[day] ?: 0

    fun setWords(day: Int, count: Int) {
        if (day !in 1..GoalPlan.DAYS) return
        val safeCount = count.coerceAtLeast(0)
        val previous = _wordsPerDay[day] ?: 0
        if (safeCount != previous) {
            _wordsPerDay[day] = safeCount
            UserPrefs.incWordsMastered((safeCount - previous).coerceAtLeast(0))
            changed()
        }
    }

    fun spendDiamonds(amount: Int): Boolean {
        if (amount <= 0 || diamonds < amount) return false
        diamonds -= amount
        changed()
        return true
    }

    fun seen(stage: String, learn: String, level: String): Set<String> =
        _seenIds["${stage}_${learn}_$level"]?.toSet() ?: emptySet()

    fun addSeen(stage: String, learn: String, level: String, ids: Collection<String>) {
        val set = _seenIds.getOrPut("${stage}_${learn}_$level") { mutableSetOf() }
        if (set.addAll(ids)) changed()
    }

    fun seenContent(learn: String, level: String): Set<String> =
        _seenContent["${learn}_$level"]?.toSet() ?: emptySet()

    fun addSeenContent(learn: String, level: String, content: Collection<String>) {
        val set = _seenContent.getOrPut("${learn}_$level") { mutableSetOf() }
        if (set.addAll(content.filter { it.isNotEmpty() })) changed()
    }
}
