package com.example.basekmp.data

/** App-private flat-file cache for the remote 30-day plan catalog and reviewed stage banks. */
expect fun platformInitializePlanCache(context: Any)

expect suspend fun platformReadPlanCache(relativePath: String): String?

expect suspend fun platformWritePlanCache(relativePath: String, content: String): Boolean
