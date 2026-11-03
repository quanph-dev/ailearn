package com.example.basekmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

/** App identity and native actions used by the shared settings screen. */
expect fun playStorePackageName(): String
expect fun sharePlainText(text: String)
expect fun openExternalUrl(url: String)

expect fun currentTimeMillis(): Long

/** Current calendar day in the device's local timezone. */
expect fun currentLocalEpochDay(): Long

/** Calendar day containing [timestampMillis] in the device's local timezone. */
expect fun localEpochDayAt(timestampMillis: Long): Long

/** Install calendar day when the platform can provide it; null on platforms without an app context. */
expect fun appInstallEpochDay(): Long?

expect fun formatTime(timestampMillis: Long): String
expect fun formatDateTime(timestampMillis: Long): String
expect fun formatDate(timestampMillis: Long): String
expect fun formatWeekdayShort(timestampMillis: Long): String
