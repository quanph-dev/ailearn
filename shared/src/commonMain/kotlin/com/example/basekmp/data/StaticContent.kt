package com.example.basekmp.data

import ailearn.shared.generated.resources.Res
import com.example.basekmp.ui.grammar.GrammarQuestion
import com.example.basekmp.ui.speaking.SpeakingCard
import com.example.basekmp.ui.vocab.VocabWord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Loads real bundled practice JSON resources from composeResources/files/practice/
 * 100% migrated from original app's StaticContent.java.
 * Zero dummy data: loads all 155 JSON practice pools directly.
 */
object StaticContent {
    private const val DIR = "files/practice"
    private val rawCache = mutableMapOf<String, Map<String, List<Map<String, String>>>>()
    private val grammarCache = mutableMapOf<String, Map<String, List<GrammarQuestion>>>()

    fun clearCache() {
        rawCache.clear()
        grammarCache.clear()
    }

    private fun norm(code: String): String = if (code == "other" || code.isBlank()) "en" else code.lowercase()

    fun normalizeLevel(level: String): String {
        val l = level.trim().lowercase()
        return when {
            l.startsWith("a") || l.contains("begin") || l.contains("elem") -> "Beginner"
            l.startsWith("b") || l.contains("inter") -> "Intermediate"
            l.startsWith("c") || l.contains("advan") -> "Advanced"
            else -> "Intermediate"
        }
    }

    suspend fun vocab(
        category: String,
        level: String,
        count: Int
    ): List<VocabWord> = vocab(
        learnCode = UserPrefs.learningLanguage,
        nativeCode = UserPrefs.nativeLanguage,
        category = category,
        level = normalizeLevel(level),
        n = count
    )

    suspend fun vocab(
        learnCode: String,
        nativeCode: String,
        category: String,
        level: String,
        n: Int
    ): List<VocabWord> = withContext(Dispatchers.IO) {
        val fileBase = "vocab_${norm(learnCode)}_${norm(nativeCode)}"
        val normLevel = normalizeLevel(level)
        val poolKey = "$category|$normLevel"
        val rawItems = takeRaw(fileBase, fallbackFile = "vocab_en_en", poolKey = poolKey, n = n)
        rawItems.map { map ->
            val word = map["word"].orEmpty()
            val meaning = map["meaning"].orEmpty()
            val example = map["example"].orEmpty()
            val translation = map["translation"].orEmpty()

            val idx = if (word.isNotEmpty()) example.indexOf(word, ignoreCase = true) else -1
            val (before, targetWord, after) = if (idx >= 0) {
                Triple(
                    example.substring(0, idx),
                    example.substring(idx, idx + word.length),
                    example.substring(idx + word.length)
                )
            } else {
                Triple(example, "", "")
            }

            VocabWord(
                word = word,
                meaning = meaning,
                exampleBefore = before,
                exampleWord = targetWord,
                exampleAfter = after,
                translation = translation
            )
        }
    }

    suspend fun grammar(
        type: String,
        difficulty: String,
        count: Int
    ): List<GrammarQuestion> = grammar(
        learnCode = UserPrefs.learningLanguage,
        type = type,
        difficulty = difficulty,
        n = count
    )

    suspend fun grammar(
        learnCode: String,
        type: String,
        difficulty: String,
        n: Int
    ): List<GrammarQuestion> = withContext(Dispatchers.IO) {
        val fileBase = "grammar_${norm(learnCode)}"
        val normDiff = normalizeLevel(difficulty)
        val poolKey = "$type|$normDiff"
        val grammarMap = loadGrammarFile(fileBase, fallbackFile = "grammar_en")
        var questions = grammarMap[poolKey].orEmpty()
        if (questions.isEmpty()) {
            questions = grammarMap.entries.firstOrNull { it.key.startsWith(type) }?.value.orEmpty()
        }
        if (questions.isEmpty() && grammarMap.isNotEmpty()) {
            questions = grammarMap.values.flatten()
        }
        if (questions.isEmpty()) emptyList() else questions.shuffled().take(n.coerceAtLeast(1))
    }

    suspend fun speaking(
        type: String,
        difficulty: String,
        count: Int
    ): List<SpeakingCard> = speaking(
        learnCode = UserPrefs.learningLanguage,
        nativeCode = UserPrefs.nativeLanguage,
        type = type,
        difficulty = normalizeLevel(difficulty),
        n = count
    )

    suspend fun speaking(
        learnCode: String,
        nativeCode: String,
        type: String,
        difficulty: String,
        n: Int
    ): List<SpeakingCard> = withContext(Dispatchers.IO) {
        val fileBase = "speaking_${norm(learnCode)}_${norm(nativeCode)}"
        val normDiff = normalizeLevel(difficulty)
        val poolKey = "$type|$normDiff"
        val rawItems = takeRaw(fileBase, fallbackFile = "speaking_en_en", poolKey = poolKey, n = n)
        rawItems.map { map ->
            SpeakingCard(
                sentence = map["sentence"].orEmpty(),
                meaning = map["meaning"].orEmpty()
            )
        }
    }

    private suspend fun takeRaw(
        fileBase: String,
        fallbackFile: String,
        poolKey: String,
        n: Int
    ): List<Map<String, String>> {
        val map = loadRawFile(fileBase, fallbackFile)
        var pool = map[poolKey].orEmpty()
        if (pool.isEmpty()) {
            val prefix = poolKey.substringBefore("|")
            pool = map.entries.firstOrNull { it.key.startsWith(prefix) }?.value.orEmpty()
        }
        if (pool.isEmpty() && map.isNotEmpty()) {
            pool = map.values.flatten()
        }
        return if (pool.isEmpty()) emptyList() else pool.shuffled().take(n.coerceAtLeast(1))
    }

    private suspend fun loadRawFile(fileBase: String, fallbackFile: String): Map<String, List<Map<String, String>>> {
        rawCache[fileBase]?.let { return it }
        val bytes = readBytesOrFallback("$DIR/$fileBase.json", "$DIR/$fallbackFile.json")
            ?: return emptyMap()

        val jsonStr = bytes.decodeToString()
        val result = mutableMapOf<String, List<Map<String, String>>>()
        try {
            val rootObj = Json.parseToJsonElement(jsonStr).jsonObject
            for ((key, element) in rootObj) {
                val array = element.jsonArray
                val list = array.map { item ->
                    item.jsonObject.mapValues { (_, v) -> v.jsonPrimitive.content }
                }
                result[key] = list
            }
        } catch (_: Exception) {}

        rawCache[fileBase] = result
        return result
    }

    private suspend fun loadGrammarFile(fileBase: String, fallbackFile: String): Map<String, List<GrammarQuestion>> {
        grammarCache[fileBase]?.let { return it }
        val bytes = readBytesOrFallback("$DIR/$fileBase.json", "$DIR/$fallbackFile.json")
            ?: return emptyMap()

        val jsonStr = bytes.decodeToString()
        val result = mutableMapOf<String, List<GrammarQuestion>>()
        try {
            val rootObj = Json.parseToJsonElement(jsonStr).jsonObject
            for ((key, element) in rootObj) {
                val array = element.jsonArray
                val list = array.map { item ->
                    val obj = item.jsonObject
                    GrammarQuestion(
                        text = obj["text"]?.jsonPrimitive?.content.orEmpty(),
                        options = obj["options"]?.jsonArray?.map { it.jsonPrimitive.content }.orEmpty(),
                        answerIndex = obj["answerIndex"]?.jsonPrimitive?.intOrNull ?: 0
                    )
                }
                result[key] = list
            }
        } catch (_: Exception) {}

        grammarCache[fileBase] = result
        return result
    }

    private suspend fun readBytesOrFallback(primaryPath: String, fallbackPath: String): ByteArray? {
        return try {
            Res.readBytes(primaryPath)
        } catch (_: Exception) {
            try {
                Res.readBytes(fallbackPath)
            } catch (_: Exception) {
                null
            }
        }
    }
}
