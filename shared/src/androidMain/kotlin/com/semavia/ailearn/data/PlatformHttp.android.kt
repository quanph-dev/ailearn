package com.semavia.ailearn.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.cancellation.CancellationException

actual suspend fun platformHttpPost(
    url: String,
    headers: Map<String, String>,
    body: String,
    readTimeoutMs: Int,
    connectTimeoutMs: Int
): PlatformHttpResponse = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val u = URL(url)
        val conn = (u.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            doOutput = true
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            headers.forEach { (k, v) -> setRequestProperty(k, v) }
        }
        connection = conn

        conn.outputStream.use { os ->
            os.write(body.toByteArray(Charsets.UTF_8))
            os.flush()
        }

        val code = conn.responseCode
        val isStream = if (code in 200..299) conn.inputStream else conn.errorStream
        val text = isStream?.use { readStream(it) }

        PlatformHttpResponse(code, text, null)
    } catch (ce: CancellationException) {
        connection?.disconnect()
        throw ce
    } catch (t: Throwable) {
        connection?.disconnect()
        PlatformHttpResponse(0, null, t)
    }
}

actual suspend fun platformHttpGet(
    url: String,
    readTimeoutMs: Int,
    connectTimeoutMs: Int
): PlatformHttpResponse = withContext(Dispatchers.IO) {
    var connection: HttpURLConnection? = null
    try {
        val conn = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = connectTimeoutMs
            readTimeout = readTimeoutMs
            useCaches = false
        }
        connection = conn

        val code = conn.responseCode
        val stream = if (code in 200..299) conn.inputStream else conn.errorStream
        PlatformHttpResponse(code, stream?.use { readStream(it) }, null)
    } catch (ce: CancellationException) {
        throw ce
    } catch (t: Throwable) {
        PlatformHttpResponse(0, null, t)
    } finally {
        connection?.disconnect()
    }
}

private fun readStream(stream: InputStream): String {
    return BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).use { it.readText() }
}
