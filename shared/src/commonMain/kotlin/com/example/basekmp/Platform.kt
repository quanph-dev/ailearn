package com.example.basekmp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform

expect fun currentTimeMillis(): Long

/** Current calendar day in the device's local timezone. */
expect fun currentLocalEpochDay(): Long

/** Calendar day containing [timestampMillis] in the device's local timezone. */
expect fun localEpochDayAt(timestampMillis: Long): Long

/** Install calendar day when the platform can provide it; null on platforms without an app context. */
expect fun appInstallEpochDay(): Long?
