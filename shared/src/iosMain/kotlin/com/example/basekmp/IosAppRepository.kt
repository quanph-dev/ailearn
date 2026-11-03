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
        // Migrate users who completed the old onboarding before FirstOpen was introduced.
        if (!firstOpenCompleted && languageSetup && onboardingCompleted) {
            defaults.setBool(true, FIRST_OPEN_COMPLETED)
            firstOpenCompleted = true
        }
        return StartupRouteResolver.resolve(
            languageSetup,
            onboardingCompleted,
            firstOpenCompleted,
            com.example.basekmp.data.RemoteConfig.surveyBeforeHome
        )
    }

    override suspend fun currentLanguageCode(): String = defaults.stringForKey(LANGUAGE_CODE).orEmpty()

    override suspend fun saveLanguage(code: String, completeSetup: Boolean) {
        defaults.setObject(code, LANGUAGE_CODE)
        if (completeSetup) defaults.setBool(true, IS_LANGUAGE_SETUP)
    }

    override suspend fun completeFirstOpen(surveyLanguageCode: String?, initialOnboardingProgress: String) {
        val surveyBeforeHome = com.example.basekmp.data.RemoteConfig.surveyBeforeHome
        defaults.setBool(true, IS_LANGUAGE_SETUP)
        defaults.setBool(true, FIRST_OPEN_COMPLETED)
        if (!surveyBeforeHome) {
            defaults.setBool(true, ONBOARDING_COMPLETED)
        }
        defaults.setObject(surveyLanguageCode.orEmpty(), FIRST_OPEN_SURVEY_LANGUAGE)
        defaults.setObject(initialOnboardingProgress, ONBOARDING_PROGRESS)
    }

    override suspend fun firstOpenSurveyLanguage(): String =
        defaults.stringForKey(FIRST_OPEN_SURVEY_LANGUAGE).orEmpty()

    override suspend fun onboardingProgress(): String =
        defaults.stringForKey(ONBOARDING_PROGRESS).orEmpty()

    override suspend fun saveOnboardingProgress(serialized: String) {
        defaults.setObject(serialized, ONBOARDING_PROGRESS)
    }

    override suspend fun completeOnboarding(serialized: String) {
        defaults.setObject(serialized, ONBOARDING_PROGRESS)
        defaults.setBool(true, ONBOARDING_COMPLETED)
    }

    override suspend fun planProgress(): String = defaults.stringForKey(PLAN_PROGRESS).orEmpty()

    override suspend fun savePlanProgress(serialized: String) {
        defaults.setObject(serialized, PLAN_PROGRESS)
    }

    override suspend fun learningProgress(): String = defaults.stringForKey(LEARNING_PROGRESS).orEmpty()

    override suspend fun saveLearningProgress(serialized: String) {
        defaults.setObject(serialized, LEARNING_PROGRESS)
    }

    private companion object {
        const val LANGUAGE_CODE = "language_code"
        const val IS_LANGUAGE_SETUP = "is_language_setup"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val FIRST_OPEN_COMPLETED = "first_open_completed"
        const val APP_OPEN_COUNT = "app_open_count"
        const val FIRST_OPEN_SURVEY_LANGUAGE = "first_open_survey_language"
        const val ONBOARDING_PROGRESS = "onboarding_progress"
        const val PLAN_PROGRESS = "plan_progress"
        const val LEARNING_PROGRESS = "learning_progress"
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
