package com.semavia.ailearn.analytics

import androidx.compose.runtime.staticCompositionLocalOf

/** Platform bridge for product analytics emitted from shared Compose UI. */
fun interface AnalyticsEventLogger {
    fun logEvent(eventName: String, parameters: Map<String, Any>)
}

object NoOpAnalyticsEventLogger : AnalyticsEventLogger {
    override fun logEvent(eventName: String, parameters: Map<String, Any>) = Unit
}

val LocalAnalyticsEventLogger = staticCompositionLocalOf<AnalyticsEventLogger> {
    NoOpAnalyticsEventLogger
}

fun AnalyticsEventLogger.track(eventName: String, vararg parameters: Pair<String, Any>) {
    logEvent(eventName, mapOf(*parameters))
}
