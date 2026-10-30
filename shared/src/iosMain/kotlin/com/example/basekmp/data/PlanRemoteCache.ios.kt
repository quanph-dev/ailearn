package com.example.basekmp.data

actual fun platformInitializePlanCache(context: Any) = Unit

actual fun platformInitializeShadowingCache(context: Any) = Unit

actual suspend fun platformReadPlanCache(relativePath: String): String? = null

actual suspend fun platformWritePlanCache(relativePath: String, content: String): Boolean = false

actual suspend fun platformReadShadowingCache(relativePath: String): String? = null

actual suspend fun platformWriteShadowingCache(relativePath: String, content: String): Boolean = false
