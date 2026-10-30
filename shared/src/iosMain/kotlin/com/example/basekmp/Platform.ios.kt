package com.example.basekmp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIDevice
import platform.posix.time

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

@OptIn(ExperimentalForeignApi::class)
actual fun currentTimeMillis(): Long = time(null).toLong() * 1_000L
