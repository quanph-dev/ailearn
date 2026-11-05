package com.example.basekmp

import android.os.Build
import android.os.LocaleList
import com.example.basekmp.data.Strings
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicAndroidHostTest {

    @Test
    fun testApplyPlatformLanguageUpdatesLocaleAndLocaleList() {
        val originalLocale = Locale.getDefault()
        try {
            applyPlatformLanguage("vi")
            assertEquals("vi", Locale.getDefault().language)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                assertEquals("vi", LocaleList.getDefault()[0].language)
            }

            applyPlatformLanguage("es")
            assertEquals("es", Locale.getDefault().language)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                assertEquals("es", LocaleList.getDefault()[0].language)
            }

            ensureResourceLanguage("vi")
            assertEquals("vi", Locale.getDefault().language)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                assertEquals("vi", LocaleList.getDefault()[0].language)
            }
        } finally {
            applyPlatformLanguage(originalLocale.toLanguageTag())
        }
    }
}
