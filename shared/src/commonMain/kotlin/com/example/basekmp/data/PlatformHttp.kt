package com.example.basekmp.data

data class PlatformHttpResponse(
    val statusCode: Int,
    val body: String?,
    val error: Throwable? = null
)

expect suspend fun platformHttpPost(
    url: String,
    headers: Map<String, String>,
    body: String,
    readTimeoutMs: Int,
    connectTimeoutMs: Int = 15000
): PlatformHttpResponse

/** Fetches a public resource such as the Firebase-hosted plan catalog or a stage bank. */
expect suspend fun platformHttpGet(
    url: String,
    readTimeoutMs: Int = 10000,
    connectTimeoutMs: Int = 10000
): PlatformHttpResponse
