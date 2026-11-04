package com.example.basekmp.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.math.abs

/**
 * 100% faithful port of GoalPlanFormat.java.
 * Parses curriculum bundle (plan.json) and level banks.
 */
object GoalPlanFormat {
    const val QUIZ_CAP = 15
    private val ID_REGEX = Regex("[a-z0-9_]+")

    val DEFAULT_QUIZ = listOf(
        listOf(5, 5, 8),
        listOf(10, 8, 10),
        listOf(15, 10, 12),
        listOf(20, 12, 14),
        listOf(30, 15, 15)
    )

    val DEFAULT_SPEAKING = listOf(
        listOf(5, 3, 4),
        listOf(10, 4, 5),
        listOf(15, 5, 6),
        listOf(20, 6, 7),
        listOf(30, 7, 8)
    )

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parse(jsonStr: String): PlanCatalogData? {
        return try {
            val root = json.parseToJsonElement(jsonStr).jsonObject
            val version = root["version"]?.jsonPrimitive?.intOrNull ?: 1
            val showDrafts = root["show_drafts"]?.jsonPrimitive?.booleanOrNull ?: false

            val quizCounts = parseRows(root["quiz_counts"]?.jsonArray, DEFAULT_QUIZ)
            val speakingCounts = parseRows(root["speaking_counts"]?.jsonArray, DEFAULT_SPEAKING)

            val goalsList = mutableListOf<PlanGoal>()
            val goalsArray = root["goals"]?.jsonArray ?: JsonArray(emptyList())

            for (elem in goalsArray) {
                val goalObj = elem.jsonObject
                val rawId = goalObj["id"]?.jsonPrimitive?.content?.trim() ?: ""
                if (!ID_REGEX.matches(rawId)) continue

                val goalTitle = goalObj["title"]?.jsonPrimitive?.content ?: rawId
                val emoji = goalObj["emoji"]?.jsonPrimitive?.content ?: ""
                val titles = parseStringsMap(goalObj["titles"]?.jsonObject)
                val learnTitles = parseLearnTitlesMap(goalObj["learn_titles"]?.jsonObject)

                val stagesList = mutableListOf<PlanStage>()
                val stagesArray = goalObj["stages"]?.jsonArray ?: JsonArray(emptyList())

                for (sElem in stagesArray) {
                    val sObj = sElem.jsonObject
                    val sId = sObj["id"]?.jsonPrimitive?.content?.trim() ?: ""
                    if (!ID_REGEX.matches(sId)) continue

                    val sTitle = sObj["title"]?.jsonPrimitive?.content ?: sId
                    val sTitles = parseStringsMap(sObj["titles"]?.jsonObject)
                    val sVideos = sObj["videos"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content.trim().takeIf { it.isNotEmpty() } } ?: emptyList()
                    val sLangs = sObj["languages"]?.jsonArray?.mapNotNull { it.jsonPrimitive.content.trim().takeIf { it.isNotEmpty() } }?.toSet() ?: emptySet()
                    val sVersion = sObj["version"]?.jsonPrimitive?.intOrNull ?: 1
                    val sLearnTitles = parseLearnTitlesMap(sObj["learn_titles"]?.jsonObject)

                    stagesList.add(
                        PlanStage(
                            id = sId,
                            title = sTitle,
                            titles = sTitles,
                            videos = sVideos,
                            languages = sLangs,
                            version = sVersion,
                            learnTitles = sLearnTitles
                        )
                    )
                }

                goalsList.add(
                    PlanGoal(
                        id = rawId,
                        title = goalTitle,
                        titles = titles,
                        emoji = emoji,
                        stages = stagesList,
                        learnTitles = learnTitles
                    )
                )
            }

            PlanCatalogData(
                version = version,
                showDrafts = showDrafts,
                quizCounts = quizCounts,
                speakingCounts = speakingCounts,
                goals = goalsList
            )
        } catch (e: Exception) {
            null
        }
    }

    fun parseBank(jsonStr: String): PlanBank? {
        return try {
            val root = json.parseToJsonElement(jsonStr).jsonObject
            val stage = root["stage"]?.jsonPrimitive?.content ?: ""
            val learn = root["learn"]?.jsonPrimitive?.content ?: ""
            val reviewed = root["status"]?.jsonPrimitive?.content == "reviewed"

            val levelsObj = root["levels"]?.jsonObject ?: JsonObject(emptyMap())
            val levelsMap = mutableMapOf<String, LevelBank>()

            for ((levelKey, levelElem) in levelsObj) {
                val lObj = levelElem.jsonObject

                // Vocab
                val vocabList = mutableListOf<VocabItem>()
                val vocabArray = lObj["vocab"]?.jsonArray ?: JsonArray(emptyList())
                for (vElem in vocabArray) {
                    val vObj = vElem.jsonObject
                    val options = vObj["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val answer = vObj["answer"]?.jsonPrimitive?.intOrNull ?: -1
                    if (options.size >= 2 && answer in options.indices) {
                        vocabList.add(
                            VocabItem(
                                id = vObj["id"]?.jsonPrimitive?.content ?: "",
                                meaning = parseStringsMap(vObj["meaning"]?.jsonObject),
                                options = options,
                                answer = answer,
                                question = parseStringsMap(vObj["question"]?.jsonObject)
                            )
                        )
                    }
                }

                // Translation
                val transList = mutableListOf<TranslationItem>()
                val transArray = lObj["translation"]?.jsonArray ?: JsonArray(emptyList())
                for (tElem in transArray) {
                    val tObj = tElem.jsonObject
                    val options = tObj["options"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                    val answer = tObj["answer"]?.jsonPrimitive?.intOrNull ?: -1
                    if (options.size >= 2 && answer in options.indices) {
                        transList.add(
                            TranslationItem(
                                id = tObj["id"]?.jsonPrimitive?.content ?: "",
                                source = parseStringsMap(tObj["source"]?.jsonObject),
                                options = options,
                                answer = answer
                            )
                        )
                    }
                }

                // Speaking
                val speakList = mutableListOf<SpeakingItem>()
                val speakArray = lObj["speaking"]?.jsonArray ?: JsonArray(emptyList())
                for (sElem in speakArray) {
                    val sObj = sElem.jsonObject
                    val say = sObj["say"]?.jsonPrimitive?.content?.trim() ?: ""
                    if (say.isNotEmpty()) {
                        speakList.add(
                            SpeakingItem(
                                id = sObj["id"]?.jsonPrimitive?.content ?: "",
                                task = parseStringsMap(sObj["task"]?.jsonObject),
                                say = say,
                                start = sObj["start"]?.jsonPrimitive?.content?.trim() ?: "",
                                alt = sObj["alt"]?.jsonArray?.map { it.jsonPrimitive.content } ?: emptyList()
                            )
                        )
                    }
                }

                levelsMap[levelKey] = LevelBank(vocab = vocabList, translation = transList, speaking = speakList)
            }

            PlanBank(stage = stage, learn = learn, reviewed = reviewed, levels = levelsMap)
        } catch (e: Exception) {
            null
        }
    }

    fun count(rows: List<List<Int>>, minutes: Int, cap: Int = Int.MAX_VALUE): Int {
        val best = rows.minWithOrNull(
            compareBy<List<Int>> { abs(it[0] - minutes) }
                .thenByDescending { it[0] }
        ) ?: return 0
        return best[2].coerceAtMost(cap)
    }

    fun title(title: String, titles: Map<String, String>, learnTitles: Map<String, String>?): String {
        return titleAndLanguage(title, titles, learnTitles).first
    }

    fun titleAndLanguage(
        title: String,
        titles: Map<String, String>,
        learnTitles: Map<String, String>?
    ): Pair<String, String> {
        val language = Strings.getLanguage()
        val choices = listOf(
            language to learnTitles?.get(language),
            language to titles[language],
            "en" to learnTitles?.get("en"),
            "en" to titles["en"]
        )
        val choice = choices.firstOrNull { !it.second.isNullOrBlank() }
        return if (choice != null) choice.second!! to choice.first else title to "en"
    }

    private fun parseStringsMap(obj: JsonObject?): Map<String, String> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, String>()
        for ((k, v) in obj) {
            val text = v.jsonPrimitive.content.trim()
            if (text.isNotEmpty()) {
                result[k] = text
            }
        }
        return result
    }

    private fun parseLearnTitlesMap(obj: JsonObject?): Map<String, Map<String, String>> {
        if (obj == null) return emptyMap()
        val result = mutableMapOf<String, Map<String, String>>()
        for ((k, v) in obj) {
            val inner = parseStringsMap(v as? JsonObject)
            if (inner.isNotEmpty()) {
                result[k] = inner
            }
        }
        return result
    }

    private fun parseRows(arr: JsonArray?, default: List<List<Int>>): List<List<Int>> {
        if (arr == null) return default
        val parsed = mutableListOf<List<Int>>()
        for (item in arr) {
            val sub = item.jsonArray
            if (sub.size == 3) {
                val r0 = sub[0].jsonPrimitive.intOrNull ?: 0
                val r1 = sub[1].jsonPrimitive.intOrNull ?: 0
                val r2 = sub[2].jsonPrimitive.intOrNull ?: 0
                if (r0 > 0 && r1 in 0..r2) {
                    parsed.add(listOf(r0, r1, r2))
                }
            }
        }
        return if (parsed.isNotEmpty()) parsed else default
    }
}
