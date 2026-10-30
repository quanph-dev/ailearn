package com.example.basekmp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSTimeZone
import platform.UIKit.UIDevice
import platform.posix.time

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long = time(null).toLong() * 1_000L

actual fun currentLocalEpochDay(): Long = localEpochDayAt(currentTimeMillis())

actual fun appInstallEpochDay(): Long? = null

@OptIn(ExperimentalForeignApi::class)
actual fun localEpochDayAt(timestampMillis: Long): Long {
    val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis.toDouble() / 1_000.0)
    val offsetMillis = NSTimeZone.localTimeZone.secondsFromGMTForDate(date).toLong() * 1_000L
    return kotlin.math.floor((timestampMillis + offsetMillis).toDouble() / 86_400_000.0).toLong()
}
