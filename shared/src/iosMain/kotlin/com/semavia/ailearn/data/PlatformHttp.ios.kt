package com.semavia.ailearn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

actual suspend fun platformHttpPost(
    url: String,
    headers: Map<String, String>,
    body: String,
    readTimeoutMs: Int,
    connectTimeoutMs: Int
): PlatformHttpResponse = withContext(Dispatchers.Default) {
    // Pure KMP fallback / iOS stub implementation
    PlatformHttpResponse(503, null, UnsupportedOperationException("iOS native HTTP not configured"))
}

actual suspend fun platformHttpGet(
    url: String,
    readTimeoutMs: Int,
    connectTimeoutMs: Int
): PlatformHttpResponse = withContext(Dispatchers.Default) {
    PlatformHttpResponse(503, null, UnsupportedOperationException("iOS native HTTP not configured"))
}
