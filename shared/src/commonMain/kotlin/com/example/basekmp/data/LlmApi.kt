package com.example.basekmp.data

import com.example.basekmp.config.AppConfig
import kotlinx.coroutines.delay
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

/**
 * Connects to the server-side LLM proxy using its legacy API key header and exponential retry backoff.
 */
object LlmApi {
    private const val APP_NAME = "english-learning-app"
    private const val APP_VERSION = "1"
    private const val PLATFORM = "mobile"
    private const val LLM_URL = AppConfig.LLM_API_URL
    private const val LLM_KEY = AppConfig.LLM_API_KEY_VALUE

    const val MAX_CHAT_TOKENS = 600
    const val MAX_UTILITY_TOKENS = 500
    const val MAX_JSON_TOKENS = 8192
    const val REPLY_BUDGET_MS = 25000L
    private const val CONNECT_TIMEOUT_MS = 15000
    private const val MAX_ATTEMPTS = 3
    private const val RETRY_JITTER_MS = 300L
    private val RETRY_BACKOFF_MS = longArrayOf(700L, 1500L)

    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    @Serializable
    data class Msg(
        val role: String,
        val content: String
    )

    data class Failure(
        val kind: String,
        val http: Int,
        val cause: String
    )

    class FailureSlot(
        var failure: Failure? = null
    ) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<FailureSlot>
    }

    data class Attempt(
        val output: String?,
        val retryable: Boolean = false,
        val failure: Failure? = null
    )

    suspend fun chat(
        messages: List<Msg>,
        system: String? = null,
        maxTokens: Int = MAX_CHAT_TOKENS,
        readTimeoutMs: Int = 15000
    ): String? {
        val messagesArray = buildJsonArray {
            if (!system.isNullOrBlank()) {
                add(buildJsonObject {
                    put("role", "system")
                    put("content", system)
                })
            }
            messages.forEach { msg ->
                add(buildJsonObject {
                    put("role", msg.role)
                    put("content", msg.content)
                })
            }
        }

        val requestBody = buildJsonObject {
            put("app-name", APP_NAME)
            put("version", APP_VERSION)
            put("platform", PLATFORM)
            put("max_tokens", maxTokens)
            put("messages", messagesArray)
        }.toString()

        return post(requestBody, readTimeoutMs)
    }

    suspend fun complete(
        prompt: String,
        maxTokens: Int = MAX_UTILITY_TOKENS,
        readTimeoutMs: Int = 15000
    ): String? {
        val requestBody = buildJsonObject {
            put("app-name", APP_NAME)
            put("version", APP_VERSION)
            put("platform", PLATFORM)
            put("max_tokens", maxTokens)
            put("input", prompt)
        }.toString()

        return post(requestBody, readTimeoutMs)
    }

    suspend fun post(body: String, readTimeoutMs: Int): String? {
        if (LLM_KEY.isBlank()) {
            return null
        }

        val headers = mapOf(
            "Content-Type" to "application/json",
            AppConfig.LLM_API_KEY_HEADER to LLM_KEY
        )

        var attempt: Attempt? = null
        for (i in 0 until MAX_ATTEMPTS) {
            if (i > 0) {
                val base = RETRY_BACKOFF_MS.getOrElse(i - 1) { 1500L }
                val jitter = Random.nextLong(RETRY_JITTER_MS)
                delay(base + jitter)
            }

            attempt = attemptPost(body, readTimeoutMs, headers)
            if (!attempt.retryable) {
                break
            }
        }

        return attempt?.output
    }

    private suspend fun attemptPost(
        body: String,
        readTimeoutMs: Int,
        headers: Map<String, String>
    ): Attempt {
        val response = try {
            platformHttpPost(
                url = LLM_URL,
                headers = headers,
                body = body,
                readTimeoutMs = readTimeoutMs,
                connectTimeoutMs = CONNECT_TIMEOUT_MS
            )
        } catch (t: Throwable) {
            return Attempt(null, retryable = false, failure = failureOf(t))
        }

        if (response.error != null) {
            return Attempt(null, retryable = false, failure = failureOf(response.error))
        }

        val code = response.statusCode
        val responseText = response.body ?: ""

        if (code != 200) {
            val retryable = code == 429 || code >= 500
            return Attempt(
                null,
                retryable = retryable,
                failure = Failure("api_error", code, "http_$code")
            )
        }

        return try {
            val parsed = json.parseToJsonElement(responseText) as? JsonObject
            val success = parsed?.get("success")?.jsonPrimitive?.booleanOrNull ?: false
            if (success) {
                val out = parsed?.get("output")?.jsonPrimitive?.content?.trim() ?: ""
                if (out.isEmpty()) {
                    Attempt(null, retryable = false, failure = Failure("api_error", code, "empty"))
                } else {
                    Attempt(out, retryable = false, failure = null)
                }
            } else {
                val errorMsg = parsed?.get("error")?.jsonPrimitive?.content ?: "unknown"
                Attempt(null, retryable = false, failure = Failure("api_error", code, "success_false: $errorMsg"))
            }
        } catch (e: Exception) {
            Attempt(null, retryable = false, failure = Failure("api_error", code, "bad_json"))
        }
    }

    fun failureOf(t: Throwable): Failure {
        val msg = t.message ?: ""
        return when {
            msg.contains("timeout", ignoreCase = true) -> Failure("timeout", 0, "read_timeout")
            msg.contains("connect", ignoreCase = true) -> Failure("unreachable", 0, "connect")
            msg.contains("host", ignoreCase = true) || msg.contains("dns", ignoreCase = true) -> Failure("unreachable", 0, "dns")
            msg.contains("ssl", ignoreCase = true) -> Failure("unreachable", 0, "ssl")
            else -> Failure("api_error", 0, t::class.simpleName?.take(40) ?: "io")
        }
    }
}
