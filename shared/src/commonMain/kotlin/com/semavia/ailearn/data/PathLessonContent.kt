package com.semavia.ailearn.data

import ailearn.shared.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Loads real bundled path lesson questions from composeResources/files/path/{lang}/{level}.json
 * 100% migrated from original app's PathLessonContent.java.
 * Zero dummy data: loads actual question trees directly from JSON.
 */
object PathLessonContent {
    enum class Kind(val key: String, val label: String) {
        VOCABULARY("vocabulary", "Vocabulary"),
        GRAMMAR("grammar", "Grammar"),
        SPEAKING("speaking", "Speaking"),
        TRANSLATION("translation", "Translation");

        companion object {
            fun fromKey(key: String): Kind = when (key.lowercase()) {
                "grammar" -> GRAMMAR
                "speaking" -> SPEAKING
                "translation" -> TRANSLATION
                else -> VOCABULARY
            }
        }
    }

    data class Question(
        val kind: Kind,
        val text: String,
        val options: List<String> = emptyList(),
        val answer: Int = -1,
        val start: String = ""
    )

    private val cache = mutableMapOf<String, Map<String, List<Question>>>()

    fun clearCache() {
        cache.clear()
    }

    suspend fun load(
        lessonId: String,
        level: String,
        lang: String = "en"
    ): List<Question> = withContext(Dispatchers.IO) {
        val levelKey = level.lowercase()
        val langKey = if (lang.isBlank() || lang == "other") "en" else lang.lowercase()
        val fileMap = loadLevelFile(langKey, levelKey)
        fileMap[lessonId] ?: fileMap[lessonId.lowercase()].orEmpty()
    }

    private suspend fun loadLevelFile(lang: String, level: String): Map<String, List<Question>> {
        val cacheKey = "$lang/$level"
        cache[cacheKey]?.let { return it }

        val path = "files/path/$lang/$level.json"
        val fallbackPath = "files/path/en/$level.json"

        val bytes = try {
            Res.readBytes(path)
        } catch (_: Exception) {
            try {
                Res.readBytes(fallbackPath)
            } catch (_: Exception) {
                null
            }
        } ?: return emptyMap()

        val jsonStr = bytes.decodeToString()
        val result = mutableMapOf<String, List<Question>>()

        try {
            val root = Json.parseToJsonElement(jsonStr).jsonObject
            for ((lId, lessonElement) in root) {
                val lessonObj = lessonElement.jsonObject
                val questionsArray = lessonObj["questions"]?.jsonArray ?: continue
                val qList = questionsArray.map { qElement ->
                    val qObj = qElement.jsonObject
                    val typeStr = qObj["type"]?.jsonPrimitive?.content ?: "vocabulary"
                    val questionText = qObj["question"]?.jsonPrimitive?.content ?: ""
                    val opts = qObj["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val ans = qObj["answer"]?.jsonPrimitive?.intOrNull ?: -1
                    val startPrompt = qObj["start"]?.jsonPrimitive?.content ?: ""

                    Question(
                        kind = Kind.fromKey(typeStr),
                        text = questionText,
                        options = opts,
                        answer = ans,
                        start = startPrompt
                    )
                }
                result[lId] = qList
            }
        } catch (_: Exception) {}

        cache[cacheKey] = result
        return result
    }
}
