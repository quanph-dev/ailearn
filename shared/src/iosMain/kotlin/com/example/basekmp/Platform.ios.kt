package com.example.basekmp

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSLocale
import platform.Foundation.NSURL
import platform.Foundation.NSTimeZone
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.localeWithLocaleIdentifier
import platform.Foundation.localTimeZone
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIDevice
import platform.posix.time

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}

actual fun getPlatform(): Platform = IOSPlatform()

actual fun playStorePackageName(): String = AppLinks.ANDROID_PACKAGE_NAME

actual fun sharePlainText(text: String) {
    val root = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    var presenter = root
    while (presenter.presentedViewController != null) {
        presenter = presenter.presentedViewController!!
    }
    val activity = UIActivityViewController(
        activityItems = listOf(text),
        applicationActivities = null
    )
    presenter.presentViewController(activity, animated = true, completion = null)
}

actual fun openExternalUrl(url: String) {
    val target = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(target)
}

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

@OptIn(ExperimentalForeignApi::class)
actual fun formatTime(timestampMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis.toDouble() / 1_000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "h:mm a"
        locale = NSLocale.localeWithLocaleIdentifier("en_US")
    }
    return formatter.stringFromDate(date)
}

@OptIn(ExperimentalForeignApi::class)
actual fun formatDateTime(timestampMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis.toDouble() / 1_000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd MMM yyyy, hh:mm a"
        locale = NSLocale.localeWithLocaleIdentifier("en_US")
    }
    return formatter.stringFromDate(date)
}

@OptIn(ExperimentalForeignApi::class)
actual fun formatDate(timestampMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis.toDouble() / 1_000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "dd MMM yyyy"
        locale = NSLocale.localeWithLocaleIdentifier("en_US")
    }
    return formatter.stringFromDate(date)
}

@OptIn(ExperimentalForeignApi::class)
actual fun formatWeekdayShort(timestampMillis: Long): String {
    val date = NSDate.dateWithTimeIntervalSince1970(timestampMillis.toDouble() / 1_000.0)
    val formatter = NSDateFormatter().apply {
        dateFormat = "EEE"
        locale = NSLocale.localeWithLocaleIdentifier("en_US")
    }
    return formatter.stringFromDate(date)
}
