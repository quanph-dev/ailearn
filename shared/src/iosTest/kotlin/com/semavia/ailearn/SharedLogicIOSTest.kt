package com.semavia.ailearn

import platform.Foundation.NSUserDefaults
import kotlin.test.Test
import kotlin.test.assertEquals

class SharedLogicIOSTest {

    @Test
    fun applyingLanguagePersistsSupportedAppleLanguageArray() {
        val defaults = NSUserDefaults.standardUserDefaults
        val previousLanguages = defaults.arrayForKey("AppleLanguages")
        try {
            applyPlatformLanguage("pt-br")
            val savedLanguages = defaults.arrayForKey("AppleLanguages")
            assertEquals("pt-BR", savedLanguages?.firstOrNull() as? String)
        } finally {
            if (previousLanguages == null) {
                defaults.removeObjectForKey("AppleLanguages")
            } else {
                defaults.setObject(previousLanguages, "AppleLanguages")
            }
        }
    }
}
