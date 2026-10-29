package com.example.basekmp

import kotlin.test.Test
import kotlin.test.assertEquals

class SharedCommonTest {

    @Test
    fun freshInstallStartsAtLanguageSelection() {
        assertEquals(
            BaseRoutes.StartupLanguage,
            StartupRouteResolver.resolve(
                languageSetup = false,
                onboardingCompleted = false,
                firstOpenCompleted = false
            )
        )
    }

    @Test
    fun incompleteFirstOpenReturnsToLanguageEvenWhenSetupFlagsAreSet() {
        assertEquals(
            BaseRoutes.StartupLanguage,
            StartupRouteResolver.resolve(
                languageSetup = true,
                onboardingCompleted = true,
                firstOpenCompleted = false
            )
        )
    }

    @Test
    fun completedFirstOpenReturnsHome() {
        assertEquals(
            BaseRoutes.Home,
            StartupRouteResolver.resolve(
                languageSetup = true,
                onboardingCompleted = true,
                firstOpenCompleted = true
            )
        )
    }

    @Test
    fun deviceLanguageIsPrioritizedAfterEnglishAndFrench() {
        val languages = sortedBaseLanguages("vi").map { it.code }

        assertEquals(listOf("en", "fr", "vi", "ar"), languages.take(4))
        assertEquals(10, languages.distinct().size)
    }

    @Test
    fun brazilianPortugueseDeviceLocaleMatchesRegionSpecificOption() {
        val languages = sortedBaseLanguages("pt-BR").map { it.code }

        assertEquals(listOf("en", "fr", "pt-br", "ar"), languages.take(4))
        assertEquals("pt-br", supportedLanguageCode("pt-BR"))
    }

    @Test
    fun unsupportedDeviceLanguageFallsBackToEnglish() {
        assertEquals("en", supportedLanguageCode("ja-JP"))
    }
}
