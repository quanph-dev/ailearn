package com.example.basekmp.ui.shadowing

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

private const val MAX_THUMBNAIL_BYTES = 6 * 1024 * 1024

@Composable
actual fun ShadowingThumbnail(
    remoteUrl: String,
    cacheKey: String,
    fallback: DrawableResource?,
    contentDescription: String?,
    modifier: Modifier
) {
    val context = LocalContext.current.applicationContext
    var image by remember(remoteUrl, cacheKey) { mutableStateOf<ImageBitmap?>(null) }

    LaunchedEffect(remoteUrl, cacheKey) {
        image = withContext(Dispatchers.IO) {
            val cacheFile = thumbnailCacheFile(context.filesDir, cacheKey)
            val cached = runCatching {
                cacheFile?.takeIf { it.isFile }?.readBytes()?.decodeThumbnail()
            }.getOrNull()
            if (cached != null) return@withContext cached

            val bytes = downloadThumbnail(remoteUrl)
            if (bytes != null && cacheFile != null) {
                runCatching {
                    cacheFile.parentFile?.mkdirs()
                    val temporary = File(cacheFile.parentFile, "${cacheFile.name}.tmp")
                    temporary.writeBytes(bytes)
                    if (cacheFile.exists()) cacheFile.delete()
                    if (!temporary.renameTo(cacheFile)) temporary.delete()
                }
            }
            bytes?.decodeThumbnail()
        }
    }

    val loadedImage = image
    if (loadedImage != null) {
        Image(
            bitmap = loadedImage,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    } else if (fallback != null) {
        Image(
            painter = painterResource(fallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = ContentScale.Crop
        )
    }
}

private fun thumbnailCacheFile(filesDir: File, cacheKey: String): File? {
    if (cacheKey.isBlank() || !cacheKey.matches(Regex("[A-Za-z0-9_-]+\\.v[0-9]+"))) return null
    return runCatching {
        val root = File(filesDir, "shadowing_remote/thumbs")
        val file = File(root, "$cacheKey.img")
        val rootPath = root.canonicalPath + File.separator
        file.takeIf { it.canonicalPath.startsWith(rootPath) }
    }.getOrNull()
}

private fun downloadThumbnail(remoteUrl: String): ByteArray? {
    if (!remoteUrl.startsWith("https://")) return null
    val connection = runCatching { URL(remoteUrl).openConnection() as? HttpURLConnection }.getOrNull() ?: return null
    return try {
        connection.connectTimeout = 8_000
        connection.readTimeout = 8_000
        connection.instanceFollowRedirects = true
        connection.setRequestProperty("Accept", "image/*")
        if (connection.responseCode !in 200..299) return null
        val declaredLength = connection.contentLengthLong
        if (declaredLength > MAX_THUMBNAIL_BYTES) return null
        connection.inputStream.use { input ->
            val output = ByteArrayOutputStream()
            val buffer = ByteArray(8 * 1024)
            var total = 0
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_THUMBNAIL_BYTES) return null
                output.write(buffer, 0, count)
            }
            output.toByteArray()
        }
    } catch (_: Exception) {
        null
    } finally {
        connection.disconnect()
    }
}

private fun ByteArray.decodeThumbnail(): ImageBitmap? =
    runCatching { BitmapFactory.decodeByteArray(this, 0, size)?.asImageBitmap() }.getOrNull()
