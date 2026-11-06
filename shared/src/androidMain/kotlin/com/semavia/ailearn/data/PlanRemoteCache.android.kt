package com.semavia.ailearn.data

import android.content.Context
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private object AndroidPlanRemoteCache {
    @Volatile
    var context: Context? = null
}

private object AndroidShadowingRemoteCache {
    @Volatile
    var context: Context? = null
}

actual fun platformInitializePlanCache(context: Any) {
    AndroidPlanRemoteCache.context = (context as? Context)?.applicationContext
}

actual fun platformInitializeShadowingCache(context: Any) {
    AndroidShadowingRemoteCache.context = (context as? Context)?.applicationContext
}

actual suspend fun platformReadPlanCache(relativePath: String): String? = withContext(Dispatchers.IO) {
    cacheFile(relativePath)?.takeIf { it.isFile }?.readText(Charsets.UTF_8)
}

actual suspend fun platformWritePlanCache(relativePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
    val destination = cacheFile(relativePath) ?: return@withContext false
    runCatching {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "${destination.name}.tmp")
        temporary.writeText(content, Charsets.UTF_8)
        if (destination.exists() && !destination.delete()) {
            temporary.delete()
            return@runCatching false
        }
        val renamed = temporary.renameTo(destination)
        if (!renamed) temporary.delete()
        renamed
    }.getOrDefault(false)
}

actual suspend fun platformReadShadowingCache(relativePath: String): String? = withContext(Dispatchers.IO) {
    shadowingCacheFile(relativePath)?.takeIf { it.isFile }?.readText(Charsets.UTF_8)
}

actual suspend fun platformWriteShadowingCache(relativePath: String, content: String): Boolean = withContext(Dispatchers.IO) {
    val destination = shadowingCacheFile(relativePath) ?: return@withContext false
    runCatching {
        destination.parentFile?.mkdirs()
        val temporary = File(destination.parentFile, "${destination.name}.tmp")
        temporary.writeText(content, Charsets.UTF_8)
        if (destination.exists() && !destination.delete()) {
            temporary.delete()
            return@runCatching false
        }
        val renamed = temporary.renameTo(destination)
        if (!renamed) temporary.delete()
        renamed
    }.getOrDefault(false)
}

private fun cacheFile(relativePath: String): File? {
    if (relativePath.isBlank() || relativePath.startsWith('/') || '\\' in relativePath) return null
    if (relativePath.split('/').any { it.isBlank() || it == "." || it == ".." }) return null
    val context = AndroidPlanRemoteCache.context ?: return null
    val root = File(context.filesDir, "plan_remote")
    val file = File(root, relativePath)
    val rootPath = root.canonicalPath + File.separator
    return file.takeIf { it.canonicalPath.startsWith(rootPath) }
}

private fun shadowingCacheFile(relativePath: String): File? {
    if (relativePath.isBlank() || relativePath.startsWith('/') || '\\' in relativePath) return null
    if (relativePath.split('/').any { it.isBlank() || it == "." || it == ".." }) return null
    val context = AndroidShadowingRemoteCache.context ?: return null
    val root = File(context.filesDir, "shadowing_remote")
    val file = File(root, relativePath)
    val rootPath = root.canonicalPath + File.separator
    return file.takeIf { it.canonicalPath.startsWith(rootPath) }
}
