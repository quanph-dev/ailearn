package com.example.basekmp.data

import ailearn.shared.generated.resources.Res
import com.example.basekmp.config.AppConfig
import com.example.basekmp.currentTimeMillis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Loads the plan catalog and reviewed question banks from the original Firebase Storage source. */
object PlanCatalogRepository {
    private const val CATALOG_FILE = "plan.json"
    private const val CATALOG_URL_FILE = "plan.url"
    private const val CATALOG_REFRESH_MS = 10 * 60 * 1000L
    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 12_000

    private val catalogMutex = Mutex()
    private val bankMutex = Mutex()
    private var cachedCatalogUrl: String? = null
    private var cachedCatalog: PlanCatalogData? = null
    private var catalogRefreshAfterMs = 0L
    private val cachedBanks = mutableMapOf<String, PlanBank>()

    suspend fun getCatalog(forceRefresh: Boolean = false): PlanCatalogData? = withContext(Dispatchers.IO) {
        val url = AppConfig.planCatalogUrl.trim()
        if (url.isBlank()) return@withContext bundledCatalog()

        val now = currentTimeMillis()
        if (!forceRefresh && cachedCatalogUrl == url && now < catalogRefreshAfterMs) {
            cachedCatalog?.let { return@withContext it }
        }

        catalogMutex.withLock {
            val lockedNow = currentTimeMillis()
            if (!forceRefresh && cachedCatalogUrl == url && lockedNow < catalogRefreshAfterMs) {
                cachedCatalog?.let { return@withLock it }
            }

            val diskCatalog = readCatalogCache(url)
            val inMemoryCatalog = cachedCatalog?.takeIf { cachedCatalogUrl == url }
            val fallbackCatalog = diskCatalog ?: inMemoryCatalog ?: bundledCatalog()

            val response = platformHttpGet(
                url = url,
                readTimeoutMs = READ_TIMEOUT_MS,
                connectTimeoutMs = CONNECT_TIMEOUT_MS
            )
            val remoteText = response.body.takeIf { response.statusCode in 200..299 && !it.isNullOrBlank() }
            val remoteCatalog = remoteText?.let(::parseCatalog)

            if (remoteCatalog != null && remoteText != null) {
                platformWritePlanCache(CATALOG_FILE, remoteText)
                platformWritePlanCache(CATALOG_URL_FILE, url)
                cachedCatalogUrl = url
                cachedCatalog = remoteCatalog
                catalogRefreshAfterMs = lockedNow + CATALOG_REFRESH_MS
                return@withLock remoteCatalog
            }

            cachedCatalogUrl = url
            cachedCatalog = fallbackCatalog
            // Allow a future screen entry to retry after an offline/invalid response.
            catalogRefreshAfterMs = lockedNow + 30_000L
            fallbackCatalog
        }
    }

    suspend fun bank(stage: PlanStage, learn: String): PlanBank? = withContext(Dispatchers.IO) {
        val languageCode = GoalPlan.learnCode(learn).lowercase()
        if (stage.id.isBlank() || !languageCode.matches(LANGUAGE_CODE)) return@withContext null
        if (stage.languages.isNotEmpty() && stage.languages.none { it.lowercase() == languageCode }) {
            return@withContext null
        }

        val cacheKey = "${stage.id}.v${stage.version}.$languageCode"
        cachedBanks[cacheKey]?.let { return@withContext it }

        bankMutex.withLock {
            cachedBanks[cacheKey]?.let { return@withLock it }
            val allowDrafts = getCatalog()?.showDrafts == true
            val diskPath = "bank/$cacheKey.json"
            val diskBank = platformReadPlanCache(diskPath)?.let(GoalPlanFormat::parseBank)
                ?.takeIf { validBank(it, stage, languageCode, allowDrafts) }
            if (diskBank != null) {
                cachedBanks[cacheKey] = diskBank
                return@withLock diskBank
            }

            val url = bankUrl(stage.id, languageCode) ?: return@withLock null
            val response = platformHttpGet(
                url = url,
                readTimeoutMs = READ_TIMEOUT_MS,
                connectTimeoutMs = CONNECT_TIMEOUT_MS
            )
            val remoteText = response.body.takeIf { response.statusCode in 200..299 && !it.isNullOrBlank() }
            val remoteBank = remoteText?.let(GoalPlanFormat::parseBank)
                ?.takeIf { validBank(it, stage, languageCode, allowDrafts) }
            if (remoteBank != null && remoteText != null) {
                platformWritePlanCache(diskPath, remoteText)
                cachedBanks[cacheKey] = remoteBank
                return@withLock remoteBank
            }

            null
        }
    }

    private suspend fun readCatalogCache(url: String): PlanCatalogData? {
        if (platformReadPlanCache(CATALOG_URL_FILE)?.trim() != url) return null
        return platformReadPlanCache(CATALOG_FILE)?.let(::parseCatalog)
    }

    private suspend fun bundledCatalog(): PlanCatalogData? = try {
        val bytes = Res.readBytes("files/plan/plan.json")
        parseCatalog(bytes.decodeToString())
    } catch (_: Exception) {
        null
    }

    private fun parseCatalog(text: String): PlanCatalogData? = GoalPlanFormat.parse(text)
        ?.takeIf { catalog ->
            catalog.version > 0 && catalog.goals.any { goal ->
                goal.id.isNotBlank() && goal.stages.isNotEmpty() && goal.stages.all { it.id.isNotBlank() }
            }
        }

    private fun validBank(
        bank: PlanBank,
        stage: PlanStage,
        languageCode: String,
        allowDrafts: Boolean
    ): Boolean {
        if (bank.stage != stage.id || bank.learn.lowercase() != languageCode) return false
        if (!bank.reviewed && !allowDrafts) return false
        return bank.levels.isNotEmpty() && bank.levels.values.any { level ->
            level.vocab.isNotEmpty() || level.translation.isNotEmpty() || level.speaking.isNotEmpty()
        }
    }

    private fun bankUrl(stageId: String, languageCode: String): String? {
        if (!stageId.matches(STAGE_ID) || !languageCode.matches(LANGUAGE_CODE)) return null
        val objectPath = "plan/bank/$stageId/$languageCode.json"
        return AppConfig.cdnBucketUrl.trimEnd('/') + "/" + encodePath(objectPath) + "?alt=media"
    }

    private fun encodePath(path: String): String = buildString {
        val hex = "0123456789ABCDEF"
        for (byte in path.encodeToByteArray()) {
            val value = byte.toInt() and 0xFF
            val char = value.toChar()
            if (char in 'a'..'z' || char in 'A'..'Z' || char in '0'..'9' || char in "-_.~") {
                append(char)
            } else {
                append('%')
                append(hex[value ushr 4])
                append(hex[value and 0x0F])
            }
        }
    }

    private val STAGE_ID = Regex("[A-Za-z0-9_-]+")
    private val LANGUAGE_CODE = Regex("[a-z]{2,3}(?:-[a-z0-9]{2,8})?")
}
