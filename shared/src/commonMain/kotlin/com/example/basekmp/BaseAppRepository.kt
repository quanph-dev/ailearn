package com.example.basekmp

interface BaseAppRepository {
    suspend fun resolveStartupRoute(): String
    suspend fun currentLanguageCode(): String
    suspend fun saveLanguage(code: String, completeSetup: Boolean)
    suspend fun completeOnboarding()
    suspend fun markHomeShown()
}

expect fun getSystemLanguageCode(): String
expect fun applyPlatformLanguage(code: String)

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
    const val Onboarding = "onboarding"
    const val Home = "browser_home"
    const val LanguageSettings = "language_settings"
}

object StartupRouteResolver {
    fun resolve(
        languageSetup: Boolean,
        onboardingCompleted: Boolean,
        firstOpenCompleted: Boolean
    ): String = if (firstOpenCompleted) {
        BaseRoutes.Home
    } else {
        BaseRoutes.StartupLanguage
    }
}

data class BaseLanguage(
    val code: String,
    val name: String,
    val nativeName: String,
    val flagName: String
)

val BaseLanguages = listOf(
    BaseLanguage("en", "English", "English", "flag_english"),
    BaseLanguage("fr", "French", "Français", "flag_france"),
    BaseLanguage("ar", "Arabic (Saudi Arabia)", "العربية", "flag_saudi"),
    BaseLanguage("id", "Indonesian", "Bahasa Indonesia", "flag_indonesia"),
    BaseLanguage("es", "Spanish", "Español", "flag_spain"),
    BaseLanguage("de", "German", "Deutsch", "flag_germany"),
    BaseLanguage("vi", "Vietnamese", "Tiếng Việt", "flag_vietnam"),
    BaseLanguage("hi", "Hindi", "हिन्दी", "flag_india"),
    BaseLanguage("pt", "Portuguese", "Português", "flag_portuguese"),
    BaseLanguage("pt-br", "Portuguese (Brazil)", "Português (Brasil)", "flag_brazil")
)

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
