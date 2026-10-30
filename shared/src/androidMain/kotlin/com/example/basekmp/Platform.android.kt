package com.example.basekmp

import android.os.Build
import android.content.Context
import java.util.TimeZone

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private var installEpochDay: Long? = null

fun platformInitializeAppInstallEpochDay(context: Context) {
    installEpochDay = runCatching {
        localEpochDayAt(context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime)
    }.getOrNull()
}

actual fun appInstallEpochDay(): Long? = installEpochDay

actual fun currentLocalEpochDay(): Long = localEpochDayAt(System.currentTimeMillis())

actual fun localEpochDayAt(timestampMillis: Long): Long {
    val localOffsetMs = TimeZone.getDefault().getOffset(timestampMillis).toLong()
    return Math.floorDiv(timestampMillis + localOffsetMs, 86_400_000L)
}
