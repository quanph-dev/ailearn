package com.example.basekmp

import android.os.Build
import android.os.LocaleList
import java.util.Locale

actual fun getSystemLanguageCode(): String =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val locales = android.content.res.Resources.getSystem().configuration.locales
        if (!locales.isEmpty) locales[0].toLanguageTag() else Locale.getDefault().toLanguageTag()
    } else {
        @Suppress("DEPRECATION")
        android.content.res.Resources.getSystem().configuration.locale.toLanguageTag()
    }

actual fun applyPlatformLanguage(code: String) {
    if (code.isBlank()) return
    val tag = code.trim().replace('_', '-')
    val locale = Locale.forLanguageTag(tag)
    if (Locale.getDefault() != locale) {
        Locale.setDefault(locale)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        val list = LocaleList(locale)
        if (LocaleList.getDefault() != list) {
            LocaleList.setDefault(list)
        }
    }
}

actual fun ensureResourceLanguage(code: String) {
    applyPlatformLanguage(code)
}

