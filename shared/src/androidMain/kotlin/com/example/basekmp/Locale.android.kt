package com.example.basekmp

import java.util.Locale

actual fun getSystemLanguageCode(): String = Locale.getDefault().toLanguageTag()

actual fun applyPlatformLanguage(code: String) {
    Locale.setDefault(Locale.forLanguageTag(code))
}
