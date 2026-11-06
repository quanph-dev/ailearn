package com.semavia.ailearn.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

/** Translates only missing language pairs; the source words remain in the language being learned. */
internal object VocabularyTranslations {
    data class Translation(val meaning: String, val translation: String)

    suspend fun translate(
        items: List<Map<String, String>>,
        learnCode: String,
        nativeCode: String
    ): Map<Int, Translation> {
        val sourceLanguage = Languages.learn(learnCode).englishName
        val nativeLanguage = Languages.nativeFor(nativeCode).englishName
        val input = buildJsonArray {
            items.forEachIndexed { index, item ->
                add(buildJsonObject {
                    put("id", index)
                    put("word", item["word"].orEmpty())
                    put("example", item["example"].orEmpty())
                    put("english_meaning", item["meaning"].orEmpty())
                    put("english_example", item["translation"].orEmpty())
                })
            }
        }
        val output = LlmApi.chat(
            system = "You translate educational vocabulary. Translate every meaning and example from $sourceLanguage into $nativeLanguage. Use the English context only as a guide. Return only a JSON array of objects with the original integer id and two non-empty strings named meaning and translation. Do not translate or return the original word.",
            messages = listOf(LlmApi.Msg("user", input.toString())),
            maxTokens = 4096,
            readTimeoutMs = 25000
        ) ?: return emptyMap()
        return parse(output, items.size)
    }

    internal fun parse(output: String, count: Int): Map<Int, Translation> {
        val start = output.indexOf('[')
        val end = output.lastIndexOf(']')
        if (start < 0 || end < start) return emptyMap()
        return try {
            val entries = Json.parseToJsonElement(output.substring(start, end + 1)).jsonArray
            val result = mutableMapOf<Int, Translation>()
            val duplicates = mutableSetOf<Int>()
            for (entry in entries) {
                val obj = entry.jsonObject
                val id = obj["id"]?.jsonPrimitive?.intOrNull ?: continue
                val meaning = obj["meaning"]?.jsonPrimitive?.content?.trim().orEmpty()
                val example = obj["translation"]?.jsonPrimitive?.content?.trim().orEmpty()
                if (id !in 0 until count || meaning.isBlank() || example.isBlank()) continue
                if (id in result) duplicates.add(id) else result[id] = Translation(meaning, example)
            }
            duplicates.forEach(result::remove)
            result
        } catch (_: Exception) {
            emptyMap()
        }
    }
}
