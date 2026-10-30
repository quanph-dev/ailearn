package com.example.basekmp.data

import ailearn.shared.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Repository for 30-Day Plan curriculum and question banks.
 * 100% migrated from decompiled GoalPlanCatalog.java.
 * Reads actual bundled JSON resources without any mock data.
 */
object PlanCatalogRepository {
    private val mutex = Mutex()
    private var cachedCatalog: PlanCatalogData? = null
    private val cachedBanks = mutableMapOf<String, PlanBank>()

    suspend fun getCatalog(): PlanCatalogData? = withContext(Dispatchers.IO) {
        cachedCatalog?.let { return@withContext it }
        mutex.withLock {
            cachedCatalog?.let { return@withLock it }
            try {
                val bytes = Res.readBytes("files/plan/plan.json")
                val text = bytes.decodeToString()
                val parsed = GoalPlanFormat.parse(text)
                cachedCatalog = parsed
                parsed
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun bank(stage: PlanStage, learn: String): PlanBank? = withContext(Dispatchers.IO) {
        val key = "${stage.id}_$learn"
        cachedBanks[key]?.let { return@withContext it }

        // Attempt to load practice vocab/speaking for the language
        val levelsMap = mutableMapOf<String, LevelBank>()
        val defaultLevels = listOf("a1", "a2", "b1", "b2", "c1")
        for (lvl in defaultLevels) {
            val questions = try {
                PathLessonContent.load(stage.id, lvl, learn)
            } catch (_: Exception) {
                emptyList()
            }
            val vocabItems = questions.filter { it.kind == PathLessonContent.Kind.VOCABULARY || it.kind == PathLessonContent.Kind.TRANSLATION }.map { q ->
                VocabItem(
                    id = q.text.hashCode().toString(),
                    meaning = mapOf("en" to q.start),
                    options = q.options,
                    answer = q.answer,
                    question = mapOf("en" to q.text)
                )
            }
            val speakingItems = questions.filter { it.kind == PathLessonContent.Kind.SPEAKING }.map { q ->
                SpeakingItem(
                    id = q.text.hashCode().toString(),
                    task = mapOf("en" to q.text),
                    say = q.text,
                    start = q.start
                )
            }
            levelsMap[lvl] = LevelBank(vocab = vocabItems, speaking = speakingItems)
        }

        val bank = PlanBank(stage = stage.id, learn = learn, levels = levelsMap)
        cachedBanks[key] = bank
        bank
    }
}
