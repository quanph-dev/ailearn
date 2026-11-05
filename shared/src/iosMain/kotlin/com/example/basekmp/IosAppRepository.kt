package com.example.basekmp

import platform.Foundation.*
import androidx.compose.runtime.ProvidedValue
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.basekmp.data.Strings

internal class IosAppRepository : BaseAppRepository {
    private val defaults = NSUserDefaults.standardUserDefaults
    private var lastAppOpenWasReturning = false

    override suspend fun resolveStartupRoute(): String {
        val previousAppOpenCount = defaults.integerForKey(APP_OPEN_COUNT)
        val count = previousAppOpenCount + 1
        defaults.setInteger(count, APP_OPEN_COUNT)
        lastAppOpenWasReturning = if (defaults.objectForKey(HAS_OPENED_APP) != null) {
            defaults.boolForKey(HAS_OPENED_APP)
        } else {
            previousAppOpenCount > 0
        }
        defaults.setBool(true, HAS_OPENED_APP)
        val languageSetup = defaults.boolForKey(IS_LANGUAGE_SETUP)
        val onboardingCompleted = defaults.boolForKey(ONBOARDING_COMPLETED)
        val firstOpenCompleted = defaults.boolForKey(FIRST_OPEN_COMPLETED)
        if (!defaults.boolForKey(HOME_VIEW_TRACKING_INITIALIZED)) {
            // Existing installs completed onboarding directly into Home before this flag existed.
            if (languageSetup && onboardingCompleted) defaults.setBool(true, HAS_VIEWED_HOME)
            defaults.setBool(true, HOME_VIEW_TRACKING_INITIALIZED)
        }
        // Migrate users who completed the old onboarding before FirstOpen was introduced.
        if (!firstOpenCompleted && languageSetup && onboardingCompleted) {
            defaults.setBool(true, FIRST_OPEN_COMPLETED)
        }
        return StartupRouteResolver.resolve(
            languageSetup = languageSetup,
            onboardingCompleted = onboardingCompleted,
            firstOpenCompleted = firstOpenCompleted,
            surveyBeforeHome = com.example.basekmp.data.RemoteConfig.surveyBeforeHome
        )
    }

    override suspend fun isReturningAppOpen(): Boolean = lastAppOpenWasReturning

    override suspend fun markHomeViewed() {
        defaults.setBool(true, HAS_VIEWED_HOME)
        defaults.setBool(true, HOME_VIEW_TRACKING_INITIALIZED)
    }

    override suspend fun currentLanguageCode(): String = defaults.stringForKey(LANGUAGE_CODE).orEmpty()

    override suspend fun currentLearningLanguageCode(): String = defaults.stringForKey(LEARNING_LANGUAGE_CODE).orEmpty()

    override suspend fun saveLearningLanguageCode(code: String) {
        defaults.setObject(code, LEARNING_LANGUAGE_CODE)
    }

    override suspend fun saveLanguage(code: String, completeSetup: Boolean) {
        defaults.setObject(code, LANGUAGE_CODE)
        if (completeSetup) defaults.setBool(true, IS_LANGUAGE_SETUP)
    }

    override suspend fun completeFirstOpen(surveyLanguageCode: String, initialOnboardingProgress: String) {
        val surveyBeforeHome = com.example.basekmp.data.RemoteConfig.surveyBeforeHome
        defaults.setBool(true, IS_LANGUAGE_SETUP)
        defaults.setBool(true, FIRST_OPEN_COMPLETED)
        if (!surveyBeforeHome) {
            defaults.setBool(true, ONBOARDING_COMPLETED)
        }
        defaults.setObject(surveyLanguageCode, FIRST_OPEN_SURVEY_LANGUAGE)
        defaults.setObject(surveyLanguageCode, LEARNING_LANGUAGE_CODE)
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
        const val LANGUAGE_CODE = "native_language_code"
        const val LEARNING_LANGUAGE_CODE = "learning_language_code"
        const val IS_LANGUAGE_SETUP = "is_language_setup"
        const val ONBOARDING_COMPLETED = "onboarding_completed"
        const val FIRST_OPEN_COMPLETED = "first_open_completed"
        const val APP_OPEN_COUNT = "app_open_count"
        const val HAS_OPENED_APP = "has_opened_app"
        const val HAS_VIEWED_HOME = "has_viewed_home"
        const val HOME_VIEW_TRACKING_INITIALIZED = "home_view_tracking_initialized"
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

actual fun ensureResourceLanguage(code: String) = Unit

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
    val direction = if (Strings.isRtl(code)) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }
    CompositionLocalProvider(
        LocalAppLocale provides normalizedCode,
        LocalLayoutDirection provides direction
    ) {
        content()
    }
}
