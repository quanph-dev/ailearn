package com.semavia.ailearn.data

/** App-private cache for the remote shadowing catalog and scripts. */
expect fun platformInitializeShadowingCache(context: Any)

expect suspend fun platformReadShadowingCache(relativePath: String): String?

expect suspend fun platformWriteShadowingCache(relativePath: String, content: String): Boolean
