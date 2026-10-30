package com.example.basekmp.data

import ailearn.shared.generated.resources.Res
import ailearn.shared.generated.resources.thumb_answering_a_work_call
import ailearn.shared.generated.resources.thumb_asking_for_a_day_off
import ailearn.shared.generated.resources.thumb_asking_for_directions
import ailearn.shared.generated.resources.thumb_asking_for_help
import ailearn.shared.generated.resources.thumb_asking_for_help_at_the_gym
import ailearn.shared.generated.resources.thumb_asking_questions_in_class
import ailearn.shared.generated.resources.thumb_at_the_pharmacy
import ailearn.shared.generated.resources.thumb_at_the_post_office
import ailearn.shared.generated.resources.thumb_borrowing_a_pen
import ailearn.shared.generated.resources.thumb_borrowing_a_phone_charger
import ailearn.shared.generated.resources.thumb_did_you_get_my_email
import ailearn.shared.generated.resources.thumb_making_everyday_plans
import ailearn.shared.generated.resources.thumb_meeting_a_new_neighbor
import ailearn.shared.generated.resources.thumb_missing_a_connecting_flight
import ailearn.shared.generated.resources.thumb_ordering_breakfast
import ailearn.shared.generated.resources.thumb_picking_up_laundry
import ailearn.shared.generated.resources.thumb_preparing_for_an_exam
import ailearn.shared.generated.resources.thumb_registering_for_an_exam
import ailearn.shared.generated.resources.thumb_reporting_a_lost_item
import ailearn.shared.generated.resources.thumb_returning_an_item
import ailearn.shared.generated.resources.thumb_setting_up_a_meeting
import ailearn.shared.generated.resources.thumb_talking_about_the_weather
import ailearn.shared.generated.resources.thumb_travel_greetings
import ailearn.shared.generated.resources.thumb_understanding_exam_formats
import ailearn.shared.generated.resources.thumb_visiting_a_car_repair_shop
import ailearn.shared.generated.resources.thumb_what_time_is_it
import ailearn.shared.generated.resources.thumb_why_are_you_learning_english
import org.jetbrains.compose.resources.DrawableResource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Data models and catalog management for Video Shadowing.
 * 100% migrated from original app's ShadowingVideos.java.
 * Zero dummy data: loads real catalog and script JSONs directly.
 */
object ShadowingVideos {
    data class Speaker(
        val name: String,
        val emoji: String,
        val male: Boolean
    )

    data class Line(
        val speaker: Int,
        val text: String,
        val translations: Map<String, String> = emptyMap(),
        val startMs: Long,
        val endMs: Long
    ) {
        fun translation(nativeCode: String): String = translations[nativeCode].orEmpty()
        fun textIn(scriptCode: String): String = if (scriptCode == "en") text else translations[scriptCode] ?: text
        fun translationFor(nativeCode: String, scriptCode: String): String =
            if (nativeCode == scriptCode) "" else if (nativeCode == "en") text else translation(nativeCode)
    }

    data class Word(
        val word: String,
        val meaning: String,
        val translations: Map<String, String> = emptyMap()
    ) {
        fun wordIn(scriptCode: String): String = if (scriptCode == "en") word else translations[scriptCode] ?: word
    }

    data class Video(
        val id: String,
        val title: String,
        val level: String,
        val category: String,
        val scene: String,
        val colors: Pair<Long, Long>,
        val asset: String,
        val thumbMs: Long,
        val speakers: List<Speaker>,
        val lines: List<Line>,
        val words: List<Word>,
        val remote: CatalogEntry? = null
    ) {
        val durationMs: Long
            get() = lines.maxOfOrNull { it.endMs }?.takeIf { it > 0 } ?: remote?.durationMs ?: 0L
    }

    data class SavedWord(
        val display: String,
        val meaning: String,
        val videoTitle: String,
        val videoTitles: Map<String, String> = emptyMap()
    )

    private var catalogCache: List<Video>? = null
    private val videoCache = mutableMapOf<String, Video>()

    fun clearCache() {
        catalogCache = null
        videoCache.clear()
    }

    suspend fun all(): List<Video> = withContext(Dispatchers.IO) {
        catalogCache?.let { return@withContext it }
        val catalogEntries = loadCatalogEntries()
        val list = catalogEntries.map { entry ->
            val cached = videoCache[entry.id]
            if (cached != null) cached else {
                Video(
                    id = entry.id,
                    title = entry.title,
                    level = entry.level,
                    category = entry.category,
                    scene = "☕",
                    colors = Pair(0xFFFFE7CC, 0xFFFAD4AC),
                    asset = entry.clipUrl,
                    thumbMs = 500L,
                    speakers = emptyList(),
                    lines = emptyList(),
                    words = emptyList(),
                    remote = entry
                )
            }
        }
        catalogCache = list
        list
    }

    suspend fun load(id: String): Video? = open(id)

    suspend fun open(id: String): Video? = withContext(Dispatchers.IO) {
        videoCache[id]?.let { return@withContext it }
        val catalogEntries = loadCatalogEntries()
        val entry = catalogEntries.firstOrNull { it.id == id }

        val scriptPath = "files/shadowing/scripts/$id.json"
        val bytes = try {
            Res.readBytes(scriptPath)
        } catch (_: Exception) {
            null
        }

        if (bytes == null) {
            return@withContext entry?.let {
                Video(
                    id = it.id,
                    title = it.title,
                    level = it.level,
                    category = it.category,
                    scene = "☕",
                    colors = Pair(0xFFFFE7CC, 0xFFFAD4AC),
                    asset = it.clipUrl,
                    thumbMs = 500L,
                    speakers = emptyList(),
                    lines = emptyList(),
                    words = emptyList(),
                    remote = it
                )
            }
        }

        try {
            val root = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
            val scene = root["scene"]?.jsonPrimitive?.content ?: "☕"
            val colorsArray = root["colors"]?.jsonArray
            val c1 = color(colorsArray?.getOrNull(0)?.jsonPrimitive?.content, 0xFFFFE7CC)
            val c2 = color(colorsArray?.getOrNull(1)?.jsonPrimitive?.content, 0xFFFAD4AC)

            val speakers = root["speakers"]?.jsonArray?.map { sElement ->
                val sObj = sElement.jsonObject
                val voice = sObj["voice"]?.jsonPrimitive?.content ?: "male"
                Speaker(
                    name = sObj["name"]?.jsonPrimitive?.content.orEmpty(),
                    emoji = sObj["emoji"]?.jsonPrimitive?.content.orEmpty(),
                    male = voice.lowercase() == "male"
                )
            } ?: emptyList()

            val lines = root["lines"]?.jsonArray?.map { lElement ->
                val lObj = lElement.jsonObject
                val spIdx = lObj["speaker"]?.jsonPrimitive?.intOrNull ?: 0
                val text = lObj["text"]?.jsonPrimitive?.content.orEmpty()
                val start = lObj["start"]?.jsonPrimitive?.longOrNull ?: 0L
                val end = lObj["end"]?.jsonPrimitive?.longOrNull ?: 0L
                val trObj = lObj["tr"]?.jsonObject
                val trMap = trObj?.mapValues { (_, v) -> v.jsonPrimitive.content } ?: emptyMap()

                Line(
                    speaker = spIdx,
                    text = text,
                    translations = trMap,
                    startMs = start,
                    endMs = end
                )
            } ?: emptyList()

            val words = root["words"]?.jsonArray?.map { wElement ->
                val wObj = wElement.jsonObject
                val w = wObj["word"]?.jsonPrimitive?.content.orEmpty()
                val m = wObj["meaning"]?.jsonPrimitive?.content.orEmpty()
                val trObj = wObj["translations"]?.jsonObject
                val trMap = trObj?.mapValues { (_, v) -> v.jsonPrimitive.content } ?: emptyMap()
                Word(word = w, meaning = m, translations = trMap)
            } ?: emptyList()

            val video = Video(
                id = id,
                title = entry?.title ?: id.replace('_', ' ').replaceFirstChar { it.uppercase() },
                level = entry?.level ?: "A2",
                category = entry?.category ?: "daily",
                scene = scene,
                colors = Pair(c1, c2),
                asset = entry?.clipUrl ?: "",
                thumbMs = 500L,
                speakers = speakers,
                lines = lines,
                words = words,
                remote = entry
            )
            videoCache[id] = video
            video
        } catch (_: Exception) {
            null
        }
    }

    fun title(video: Video, langCode: String = UserPrefs.nativeLanguage): String {
        val titles = video.remote?.titles
        return titles?.get(langCode) ?: video.title
    }

    fun levelName(video: Video): String = when (video.level.uppercase()) {
        "A1", "A2" -> "Beginner"
        "B1", "B2" -> "Intermediate"
        "C1", "C2" -> "Advanced"
        else -> if (video.level.isBlank()) "Beginner" else video.level
    }

    fun thumbnailDrawable(videoId: String): DrawableResource? = when (videoId) {
        "ordering_breakfast" -> Res.drawable.thumb_ordering_breakfast
        "asking_for_help" -> Res.drawable.thumb_asking_for_help
        "asking_for_directions" -> Res.drawable.thumb_asking_for_directions
        "returning_an_item" -> Res.drawable.thumb_returning_an_item
        "at_the_pharmacy" -> Res.drawable.thumb_at_the_pharmacy
        "at_the_post_office" -> Res.drawable.thumb_at_the_post_office
        "meeting_a_new_neighbor" -> Res.drawable.thumb_meeting_a_new_neighbor
        "reporting_a_lost_item" -> Res.drawable.thumb_reporting_a_lost_item
        "asking_for_help_at_the_gym" -> Res.drawable.thumb_asking_for_help_at_the_gym
        "borrowing_a_phone_charger" -> Res.drawable.thumb_borrowing_a_phone_charger
        "picking_up_laundry" -> Res.drawable.thumb_picking_up_laundry
        "asking_for_a_day_off" -> Res.drawable.thumb_asking_for_a_day_off
        "making_everyday_plans" -> Res.drawable.thumb_making_everyday_plans
        "preparing_for_an_exam" -> Res.drawable.thumb_preparing_for_an_exam
        "travel_greetings" -> Res.drawable.thumb_travel_greetings
        "missing_a_connecting_flight" -> Res.drawable.thumb_missing_a_connecting_flight
        "asking_questions_in_class" -> Res.drawable.thumb_asking_questions_in_class
        "understanding_exam_formats" -> Res.drawable.thumb_understanding_exam_formats
        "visiting_a_car_repair_shop" -> Res.drawable.thumb_visiting_a_car_repair_shop
        "did_you_get_my_email" -> Res.drawable.thumb_did_you_get_my_email
        "setting_up_a_meeting" -> Res.drawable.thumb_setting_up_a_meeting
        "answering_a_work_call" -> Res.drawable.thumb_answering_a_work_call
        "why_are_you_learning_english" -> Res.drawable.thumb_why_are_you_learning_english
        "borrowing_a_pen" -> Res.drawable.thumb_borrowing_a_pen
        "talking_about_the_weather" -> Res.drawable.thumb_talking_about_the_weather
        "registering_for_an_exam" -> Res.drawable.thumb_registering_for_an_exam
        "what_time_is_it" -> Res.drawable.thumb_what_time_is_it
        else -> null
    }

    suspend fun savedWordIndex(nativeCode: String = UserPrefs.nativeLanguage): Map<String, SavedWord> = withContext(Dispatchers.IO) {
        val savedWords = UserPrefs.savedWordsState.value.keys
        val allVideos = all()
        val index = mutableMapOf<String, SavedWord>()

        for (v in allVideos) {
            val fullVideo = open(v.id) ?: continue
            for (w in fullVideo.words) {
                val key = w.word.lowercase()
                if (savedWords.contains(key) && !index.containsKey(key)) {
                    val meaning = w.translations[nativeCode] ?: w.meaning
                    index[key] = SavedWord(
                        display = w.word,
                        meaning = meaning,
                        videoTitle = fullVideo.title,
                        videoTitles = fullVideo.remote?.titles ?: emptyMap()
                    )
                }
            }
        }
        // Fallback for saved words not found in loaded video scripts
        for (w in savedWords) {
            if (!index.containsKey(w)) {
                index[w] = SavedWord(
                    display = w.replaceFirstChar { it.uppercase() },
                    meaning = "",
                    videoTitle = "Shadowing Practice",
                    videoTitles = emptyMap()
                )
            }
        }
        index
    }

    private suspend fun loadCatalogEntries(): List<CatalogEntry> {
        val bytes = try {
            Res.readBytes("files/shadowing/catalog.json")
        } catch (e: Exception) {
            println("ShadowingVideos: loadCatalogEntries failed to read bytes: $e")
            return emptyList()
        }

        return try {
            val root = Json.parseToJsonElement(bytes.decodeToString()).jsonObject
            val videos = root["videos"]?.jsonArray ?: return emptyList()
            videos.map { element ->
                val obj = element.jsonObject
                val id = obj["id"]?.jsonPrimitive?.content.orEmpty()
                val title = obj["title"]?.jsonPrimitive?.content.orEmpty()
                val titlesObj = obj["titles"]?.jsonObject
                val titles = titlesObj?.mapValues { (_, v) -> v.jsonPrimitive.content } ?: emptyMap()
                val level = obj["level"]?.jsonPrimitive?.content.orEmpty()
                val category = obj["category"]?.jsonPrimitive?.content.orEmpty()
                val durationMs = obj["duration_ms"]?.jsonPrimitive?.longOrNull ?: 0L
                val clip = obj["clip"]?.jsonPrimitive?.content.orEmpty()
                val thumb = obj["thumb"]?.jsonPrimitive?.content.orEmpty()
                val script = obj["script"]?.jsonPrimitive?.content.orEmpty()
                val version = obj["version"]?.jsonPrimitive?.intOrNull ?: 1
                val minVer = obj["min_version_code"]?.jsonPrimitive?.intOrNull ?: 1

                CatalogEntry(
                    id = id,
                    title = title,
                    titles = titles,
                    level = level,
                    category = category,
                    durationMs = durationMs,
                    clipUrl = clip,
                    thumbUrl = thumb,
                    scriptUrl = script,
                    version = version,
                    minVersionCode = minVer
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun color(hex: String?, fallback: Long): Long {
        val clean = hex?.removePrefix("#") ?: return fallback
        if (clean.length != 6) return fallback
        val parsed = clean.toLongOrNull(16) ?: return fallback
        return 0xFF000000L or parsed
    }
}
