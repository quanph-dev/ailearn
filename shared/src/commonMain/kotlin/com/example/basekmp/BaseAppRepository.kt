package com.example.basekmp

import com.example.basekmp.data.Strings

interface BaseAppRepository {
    suspend fun resolveStartupRoute(): String
    suspend fun isReturningAppOpen(): Boolean
    suspend fun markHomeViewed()
    suspend fun currentLanguageCode(): String
    suspend fun saveLanguage(code: String, completeSetup: Boolean)
    suspend fun currentLearningLanguageCode(): String
    suspend fun saveLearningLanguageCode(code: String)
    suspend fun completeFirstOpen(surveyLanguageCode: String, initialOnboardingProgress: String)
    suspend fun firstOpenSurveyLanguage(): String
    suspend fun onboardingProgress(): String
    suspend fun saveOnboardingProgress(serialized: String)
    suspend fun completeOnboarding(serialized: String)
    suspend fun planProgress(): String
    suspend fun savePlanProgress(serialized: String)
    suspend fun learningProgress(): String
    suspend fun saveLearningProgress(serialized: String)
}

expect fun getSystemLanguageCode(): String
expect fun applyPlatformLanguage(code: String)
expect fun ensureResourceLanguage(code: String)

@androidx.compose.runtime.Composable
expect fun PlatformLocaleProvider(code: String, content: @androidx.compose.runtime.Composable () -> Unit)

expect object LocalAppLocale {
    val current: String
        @androidx.compose.runtime.Composable get

    @androidx.compose.runtime.Composable
    infix fun provides(value: String?): androidx.compose.runtime.ProvidedValue<*>
}

@androidx.compose.runtime.Composable
expect fun BackActionHandler(enabled: Boolean, onBack: () -> Unit)

object BaseRoutes {
    const val Splash = "splash"
    const val StartupLanguage = "startup_language"
    const val ApplyLanguage = "apply_language"
    const val FirstOpenIntro = "first_open_intro"
    const val FirstOpenSurvey = "first_open_survey"
    const val FirstOpenSurveyAd = "first_open_survey_ad"
    const val Onboarding = "onboarding"
    const val Starting = "starting"
    const val Home = "browser_home"
    const val LanguageSettings = "language_settings"
}

object StartupRouteResolver {
    fun resolve(
        languageSetup: Boolean,
        onboardingCompleted: Boolean,
        firstOpenCompleted: Boolean,
        surveyBeforeHome: Boolean = true
    ): String = when {
        !firstOpenCompleted -> BaseRoutes.FirstOpenIntro
        surveyBeforeHome && !onboardingCompleted -> BaseRoutes.Onboarding
        else -> BaseRoutes.Home
    }
}

data class BaseLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)

private val appLanguageNames = mapOf(
    "ar" to "Arabic",
    "de" to "German",
    "en" to "English",
    "es" to "Spanish",
    "fr" to "French",
    "hi" to "Hindi",
    "it" to "Italian",
    "ja" to "Japanese",
    "ko" to "Korean",
    "pt" to "Portuguese",
    "ru" to "Russian",
    "th" to "Thai",
    "tr" to "Turkish",
    "ur" to "Urdu",
    "vi" to "Vietnamese",
    "zh" to "Chinese"
)

val BaseLanguages = Strings.languages.map { language ->
    BaseLanguage(
        code = language.code,
        name = appLanguageNames[language.code] ?: language.name,
        nativeName = language.name,
        flag = language.flag
    )
}

fun sortedBaseLanguages(systemLanguage: String): List<BaseLanguage> {
    val english = BaseLanguages.first { it.code == "en" }
    val french = BaseLanguages.first { it.code == "fr" }
    val arabic = BaseLanguages.first { it.code == "ar" }
    val normalizedLanguage = systemLanguage.lowercase().replace('_', '-')
    val systemLanguageBase = normalizedLanguage.substringBefore('-')
    val priority = when (systemLanguageBase) {
        "en" -> listOf(french, arabic, english)
        "fr" -> listOf(english, arabic, french)
        else -> {
            val deviceLanguage = BaseLanguages.firstOrNull { it.code == normalizedLanguage }
                ?: BaseLanguages.firstOrNull { it.code.substringBefore('-') == systemLanguageBase }
            listOfNotNull(english, french, deviceLanguage?.takeIf { it.code != "ar" }, arabic)
        }
    }
    val priorityCodes = priority.mapTo(mutableSetOf()) { it.code }
    return priority + BaseLanguages.filterNot { it.code in priorityCodes }
}

fun supportedLanguageCode(systemLanguage: String): String {
    val normalizedLanguage = systemLanguage.lowercase().replace('_', '-')
    return BaseLanguages.firstOrNull { it.code == normalizedLanguage }?.code
        ?: BaseLanguages.firstOrNull { it.code == normalizedLanguage.substringBefore('-') }?.code
        ?: "en"
}
