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
import com.example.basekmp.config.AppConfig
import com.example.basekmp.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val catalogMutex = Mutex()
    private val scriptMutex = Mutex()
    private var catalogUrlCache: String? = null
    private var catalogRefreshedAt = 0L

    private const val CATALOG_FILE = "catalog.json"
    private const val CATALOG_URL_FILE = "catalog.url"
    private const val CATALOG_REFRESH_MS = 10 * 60 * 1000L

    fun clearCache() {
        catalogCache = null
        videoCache.clear()
        catalogUrlCache = null
        catalogRefreshedAt = 0L
    }

    suspend fun all(): List<Video> = withContext(Dispatchers.IO) {
        val url = AppConfig.shadowingCatalogUrl.trim()
        val now = currentTimeMillis()
        if (catalogUrlCache == url && now - catalogRefreshedAt < CATALOG_REFRESH_MS) {
            catalogCache?.let { return@withContext it }
        }
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
                    asset = playbackUrl(entry),
                    thumbMs = 500L,
                    speakers = emptyList(),
                    lines = emptyList(),
                    words = emptyList(),
                    remote = entry
                )
            }
        }
        val orderedList = ordered(list, AppConfig.shadowingFirstVideo)
        catalogCache = orderedList
        orderedList
    }

    suspend fun load(id: String): Video? = open(id)

    suspend fun open(id: String): Video? = withContext(Dispatchers.IO) {
        videoCache[id]?.let { return@withContext it }
        val catalogEntries = loadCatalogEntries()
        val entry = catalogEntries.firstOrNull { it.id == id }

        val scriptText = entry?.let { loadScript(it) } ?: bundledScript(id)
        if (scriptText == null) return@withContext entry?.toVideoWithoutScript()

        try {
            val root = Json.parseToJsonElement(scriptText).jsonObject
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
                val trObj = (wObj["translations"] ?: wObj["tr"])?.jsonObject
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
                asset = entry?.let(::playbackUrl).orEmpty(),
                thumbMs = 500L,
                speakers = speakers,
                lines = lines,
                words = words,
                remote = entry
            )
            if (video.lines.isEmpty()) return@withContext null
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

    fun thumbnailUrl(video: Video): String {
        val path = video.remote?.thumbUrl?.takeIf { it.isNotBlank() } ?: return ""
        if (path.startsWith("https://")) return path
        val objectPath = if (path.startsWith("shadowing/")) path else "shadowing/$path"
        return storageUrl(objectPath)
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
        val normalizedNativeCode = nativeCode.lowercase()

        for (v in allVideos) {
            val fullVideo = open(v.id) ?: continue
            for (w in fullVideo.words) {
                val scriptCodes = (listOf("en") + w.translations.keys).distinct()
                for (scriptCode in scriptCodes) {
                    val displayWord = w.wordIn(scriptCode)
                    val key = displayWord.lowercase()
                    if (savedWords.contains(key) && !index.containsKey(key)) {
                        val meaning = when {
                            scriptCode == "en" && normalizedNativeCode == "en" -> w.meaning
                            scriptCode == "en" -> w.translations[normalizedNativeCode] ?: w.meaning
                            normalizedNativeCode == "en" || normalizedNativeCode == scriptCode -> w.word
                            else -> w.translations[normalizedNativeCode] ?: w.word
                        }
                        index[key] = SavedWord(
                            display = displayWord,
                            meaning = meaning,
                            videoTitle = fullVideo.title,
                            videoTitles = fullVideo.remote?.titles ?: emptyMap()
                        )
                    }
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
        val url = AppConfig.shadowingCatalogUrl.trim()
        val now = currentTimeMillis()
        if (catalogUrlCache == url && now - catalogRefreshedAt < CATALOG_REFRESH_MS) {
            catalogEntriesFromVideos()?.let { return it }
        }

        return catalogMutex.withLock {
            val lockedNow = currentTimeMillis()
            if (catalogUrlCache == url && lockedNow - catalogRefreshedAt < CATALOG_REFRESH_MS) {
                catalogEntriesFromVideos()?.let { return@withLock it }
            }

            val cachedText = if (platformReadShadowingCache(CATALOG_URL_FILE)?.trim() == url) {
                platformReadShadowingCache(CATALOG_FILE)
            } else null
            val cachedEntries = cachedText?.let(::parseCatalogEntries)
            val response = if (url.isNotBlank()) {
                platformHttpGet(url, readTimeoutMs = 10_000, connectTimeoutMs = 10_000)
            } else null
            val remoteText = response?.body?.takeIf { response.statusCode in 200..299 && it.isNotBlank() }
            val remoteEntries = remoteText?.let(::parseCatalogEntries)

            if (!remoteText.isNullOrBlank() && !remoteEntries.isNullOrEmpty()) {
                if (catalogUrlCache != url || catalogRefreshedAt > 0L) {
                    catalogCache = null
                    videoCache.clear()
                }
                platformWriteShadowingCache(CATALOG_FILE, remoteText)
                platformWriteShadowingCache(CATALOG_URL_FILE, url)
                catalogUrlCache = url
                catalogRefreshedAt = lockedNow
                return@withLock remoteEntries
            }

            val bundledEntries = if (cachedEntries.isNullOrEmpty()) bundledCatalogEntries() else null
            val fallback = cachedEntries?.takeIf { it.isNotEmpty() } ?: bundledEntries.orEmpty()
            catalogUrlCache = url
            catalogRefreshedAt = lockedNow - CATALOG_REFRESH_MS + 30_000L
            fallback
        }
    }

    /** Keeps the same Remote Config pin behavior as the original app. */
    private fun ordered(videos: List<Video>, pinnedIds: String): List<Video> {
        val preferredIds = pinnedIds.split(',').map(String::trim).filter(String::isNotEmpty).distinct()
        if (preferredIds.isEmpty()) return videos

        val pinnedVideos = preferredIds.mapNotNull { id -> videos.firstOrNull { it.id == id } }
        return pinnedVideos + videos.filterNot { it in pinnedVideos }
    }

    private fun catalogEntriesFromVideos(): List<CatalogEntry>? = catalogCache?.map { video ->
        video.remote ?: CatalogEntry(
            id = video.id,
            title = video.title,
            level = video.level,
            category = video.category,
            clipUrl = video.asset
        )
    }

    private suspend fun bundledCatalogEntries(): List<CatalogEntry> = try {
        parseCatalogEntries(Res.readBytes("files/shadowing/catalog.json").decodeToString())
    } catch (_: Exception) {
        emptyList()
    }

    private fun parseCatalogEntries(text: String): List<CatalogEntry> = try {
        val root = Json.parseToJsonElement(text).jsonObject
        val videos = root["videos"]?.jsonArray ?: return emptyList()
        videos.mapNotNull { element ->
            runCatching {
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
            }.getOrNull()?.takeIf {
                it.id.matches(Regex("[A-Za-z0-9_-]+")) &&
                    it.version > 0 &&
                    isSafeAssetReference(it.clipUrl) &&
                    (it.thumbUrl.isBlank() || isSafeAssetReference(it.thumbUrl)) &&
                    (it.scriptUrl.isBlank() || isSafeAssetReference(it.scriptUrl))
            }
        }
    } catch (_: Exception) {
        emptyList()
    }

    private suspend fun loadScript(entry: CatalogEntry): String? = scriptMutex.withLock {
        val scriptPath = entry.scriptUrl.takeIf { it.isNotBlank() } ?: "scripts/${entry.id}.json"
        val cacheName = "scripts/${entry.id}.v${entry.version}.json"
        val sourceName = "$cacheName.url"
        val sourceUrl = when {
            scriptPath.startsWith("https://") -> scriptPath
            else -> {
                val objectPath = if (scriptPath.startsWith("shadowing/")) scriptPath else "shadowing/$scriptPath"
                storageUrl(objectPath)
            }
        }
        val cachedText = if (platformReadShadowingCache(sourceName)?.trim() == sourceUrl) {
            platformReadShadowingCache(cacheName)
        } else null
        val validCachedText = cachedText?.takeIf(::hasUsableScriptLines)
        if (!validCachedText.isNullOrBlank()) return@withLock validCachedText

        val remoteResponse = if (sourceUrl.isNotBlank()) {
            platformHttpGet(sourceUrl, readTimeoutMs = 10_000, connectTimeoutMs = 10_000)
        } else null
        val remoteText = remoteResponse?.body?.takeIf {
            remoteResponse.statusCode in 200..299 && hasUsableScriptLines(it)
        }
        if (!remoteText.isNullOrBlank()) {
            platformWriteShadowingCache(cacheName, remoteText)
            platformWriteShadowingCache(sourceName, sourceUrl)
            return@withLock remoteText
        }
        bundledScript(entry.id)?.takeIf(::hasUsableScriptLines)
    }

    private fun hasUsableScriptLines(text: String): Boolean = runCatching {
        Json.parseToJsonElement(text).jsonObject["lines"]?.jsonArray?.isNotEmpty() == true
    }.getOrDefault(false)

    private suspend fun bundledScript(id: String): String? = try {
        Res.readBytes("files/shadowing/scripts/$id.json").decodeToString()
    } catch (_: Exception) {
        null
    }

    fun playbackUrl(video: Video): String {
        val clipPath = video.remote?.clipUrl?.takeIf { it.isNotBlank() } ?: video.asset
        if (clipPath.startsWith("http://") || clipPath.startsWith("https://") ||
            clipPath.startsWith("asset://") || clipPath.startsWith("file://") ||
            clipPath.startsWith("content://") || clipPath.startsWith("/")) return clipPath
        val objectPath = if (clipPath.startsWith("shadowing/")) clipPath else "shadowing/$clipPath"
        return storageUrl(objectPath)
    }

    private fun CatalogEntry.toVideoWithoutScript() = Video(
        id = id,
        title = title,
        level = level,
        category = category,
        scene = "☕",
        colors = Pair(0xFFFFE7CC, 0xFFFAD4AC),
        asset = playbackUrl(this),
        thumbMs = 500L,
        speakers = emptyList(),
        lines = emptyList(),
        words = emptyList(),
        remote = this
    )

    private fun playbackUrl(entry: CatalogEntry): String {
        val clip = entry.clipUrl
        if (clip.startsWith("http://") || clip.startsWith("https://")) return clip
        val objectPath = if (clip.startsWith("shadowing/")) clip else "shadowing/$clip"
        return storageUrl(objectPath)
    }

    private fun storageUrl(objectPath: String): String {
        val base = AppConfig.cdnBucketUrl.trim()
        if (base.isBlank()) return ""
        val encodedPath = buildString {
            val hex = "0123456789ABCDEF"
            for (byte in objectPath.encodeToByteArray()) {
                val value = byte.toInt() and 0xFF
                val char = value.toChar()
                if (char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in "-_.~/") {
                    append(char)
                } else {
                    append('%')
                    append(hex[value ushr 4])
                    append(hex[value and 0x0F])
                }
            }
        }
        return "${base.trimEnd('/')}/${encodedPath.replace("/", "%2F")}?alt=media"
    }

    private fun isSafeObjectPath(path: String): Boolean =
        path.isNotBlank() && !path.startsWith('/') && '\\' !in path &&
            path.split('/').all { it.isNotBlank() && it != "." && it != ".." }

    private fun isSafeAssetReference(path: String): Boolean =
        path.startsWith("https://") || isSafeObjectPath(path)

    private fun color(hex: String?, fallback: Long): Long {
        val clean = hex?.removePrefix("#") ?: return fallback
        if (clean.length != 6) return fallback
        val parsed = clean.toLongOrNull(16) ?: return fallback
        return 0xFF000000L or parsed
    }
}
