package com.example.basekmp

import platform.Foundation.*
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

internal class IosAppRepository : BaseAppRepository {
    private val defaults = NSUserDefaults.standardUserDefaults

    override suspend fun resolveStartupRoute(): String {
        val count = defaults.integerForKey(APP_OPEN_COUNT) + 1
        defaults.setInteger(count, APP_OPEN_COUNT)
        val languageSetup = defaults.boolForKey(IS_LANGUAGE_SETUP)
        val onboardingCompleted = defaults.boolForKey(ONBOARDING_COMPLETED)
        var firstOpenCompleted = defaults.boolForKey(FIRST_OPEN_COMPLETED)
        if (!firstOpenCompleted && count == 1L && languageSetup && onboardingCompleted) {
            defaults.setBool(true, FIRST_OPEN_COMPLETED)
            firstOpenCompleted = true
        }
        return StartupRouteResolver.resolve(languageSetup, onboardingCompleted, firstOpenCompleted)
    }

    override suspend fun currentLanguageCode(): String = defaults.stringForKey(LANGUAGE_CODE).orEmpty()

    override suspend fun saveLanguage(code: String, completeSetup: Boolean) {
        defaults.setObject(code, LANGUAGE_CODE)
        if (completeSetup) defaults.setBool(true, IS_LANGUAGE_SETUP)
    }

    override suspend fun completeOnboarding() {
        defaults.setBool(true, ONBOARDING_COMPLETED)
    }

    override suspend fun markHomeShown() {
        defaults.setBool(true, FIRST_OPEN_COMPLETED)
    }

    private companion object {
        const val LANGUAGE_CODE = "language_code"
        const val IS_LANGUAGE_SETUP = "is_language_setup"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val FIRST_OPEN_COMPLETED = "first_open_completed"
        const val APP_OPEN_COUNT = "app_open_count"
    }
}

actual fun getSystemLanguageCode(): String =
    NSLocale.preferredLanguages.firstOrNull()?.let { it as? String } ?: "en"

actual fun applyPlatformLanguage(code: String) {
    val normalizedCode = code.replace("pt-br", "pt-BR")
    NSUserDefaults.standardUserDefaults.setObject(arrayListOf(normalizedCode), "AppleLanguages")
    NSUserDefaults.standardUserDefaults.synchronize()
}

actual object LocalAppLocale {
    private val appLocale = staticCompositionLocalOf {
        NSLocale.preferredLanguages.firstOrNull()?.let { it as? String } ?: "en"
    }

    actual val current: String
        @Composable get() = appLocale.current

    @Composable
    actual infix fun provides(value: String?): ProvidedValue<*> =
        appLocale provides (value ?: appLocale.current)
}

@Composable
actual fun PlatformLocaleProvider(code: String, content: @Composable () -> Unit) {
    val normalizedCode = code.replace("pt-br", "pt-BR")
    val direction = if (code.startsWith("ar", ignoreCase = true)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalAppLocale provides normalizedCode,
        LocalLayoutDirection provides direction
    ) {
        key(normalizedCode) { content() }
    }
}
