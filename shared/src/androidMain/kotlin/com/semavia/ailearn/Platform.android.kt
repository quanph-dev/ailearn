package com.semavia.ailearn

import android.os.Build
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun currentTimeMillis(): Long = System.currentTimeMillis()

private var installEpochDay: Long? = null
private var applicationContext: Context? = null

fun platformInitializeAppInstallEpochDay(context: Context) {
    applicationContext = context.applicationContext
    installEpochDay = runCatching {
        localEpochDayAt(context.packageManager.getPackageInfo(context.packageName, 0).firstInstallTime)
    }.getOrNull()
}

actual fun appInstallEpochDay(): Long? = installEpochDay

actual fun playStorePackageName(): String =
    applicationContext?.packageName ?: AppLinks.ANDROID_PACKAGE_NAME

actual fun sharePlainText(text: String) {
    val context = applicationContext ?: return
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    context.startActivity(
        Intent.createChooser(sendIntent, "Share Semavia")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )
}

actual fun openExternalUrl(url: String) {
    val context = applicationContext ?: return
    val uri = Uri.parse(url)
    val intent = when (uri.scheme?.lowercase()) {
        "http", "https" -> Intent(Intent.ACTION_VIEW, uri)
        "mailto" -> Intent(Intent.ACTION_SENDTO, uri)
        else -> return
    }.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

actual fun currentLocalEpochDay(): Long = localEpochDayAt(System.currentTimeMillis())

actual fun localEpochDayAt(timestampMillis: Long): Long {
    val localOffsetMs = TimeZone.getDefault().getOffset(timestampMillis).toLong()
    return Math.floorDiv(timestampMillis + localOffsetMs, 86_400_000L)
}

actual fun formatTime(timestampMillis: Long): String =
    SimpleDateFormat("h:mm a", Locale.US).format(Date(timestampMillis))

actual fun formatDateTime(timestampMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.US).format(Date(timestampMillis))

actual fun formatDate(timestampMillis: Long): String =
    SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(timestampMillis))

actual fun formatWeekdayShort(timestampMillis: Long): String =
    SimpleDateFormat("EEE", Locale.US).format(Date(timestampMillis))
