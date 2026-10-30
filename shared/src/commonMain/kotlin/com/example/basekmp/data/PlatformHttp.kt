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
