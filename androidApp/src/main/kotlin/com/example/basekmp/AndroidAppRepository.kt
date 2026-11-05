package com.example.basekmp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

private val Context.baseDataStore: DataStore<Preferences> by preferencesDataStore("base_preferences")

@Singleton
class AndroidAppRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : BaseAppRepository {
    private val dataStore = context.baseDataStore
    @Volatile private var lastAppOpenWasReturning = false

    override suspend fun resolveStartupRoute(): String {
        val preferences = dataStore.edit { values ->
            val previousAppOpenCount = values[APP_OPEN_COUNT] ?: 0
            val appOpenCount = previousAppOpenCount + 1
            values[APP_OPEN_COUNT] = appOpenCount
            lastAppOpenWasReturning = values[HAS_OPENED_APP] ?: (previousAppOpenCount > 0)
            values[HAS_OPENED_APP] = true
            val languageSetup = values[IS_LANGUAGE_SETUP] ?: false
            val onboardingCompleted = values[ONBOARDING_COMPLETED] ?: false
            val firstOpenCompleted = values[FIRST_OPEN_COMPLETED] ?: false
            if (values[HOME_VIEW_TRACKING_INITIALIZED] != true) {
                // Existing installs completed onboarding directly into Home before this flag existed.
                if (languageSetup && onboardingCompleted) values[HAS_VIEWED_HOME] = true
                values[HOME_VIEW_TRACKING_INITIALIZED] = true
            }
            // Migrate users who completed the old onboarding before FirstOpen was introduced.
            if (!firstOpenCompleted && languageSetup && onboardingCompleted) {
                values[FIRST_OPEN_COMPLETED] = true
            }
        }
        return StartupRouteResolver.resolve(
            languageSetup = preferences[IS_LANGUAGE_SETUP] ?: false,
            onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
            firstOpenCompleted = preferences[FIRST_OPEN_COMPLETED] ?: false,
            surveyBeforeHome = com.example.basekmp.data.RemoteConfig.surveyBeforeHome
        )
    }

    override suspend fun isReturningAppOpen(): Boolean = lastAppOpenWasReturning

    override suspend fun markHomeViewed() {
        dataStore.edit {
            it[HAS_VIEWED_HOME] = true
            it[HOME_VIEW_TRACKING_INITIALIZED] = true
        }
    }

    override suspend fun currentLanguageCode(): String =
        dataStore.data.first()[LANGUAGE_CODE].orEmpty()

    override suspend fun currentLearningLanguageCode(): String =
        dataStore.data.first()[LEARNING_LANGUAGE_CODE].orEmpty()

    override suspend fun saveLearningLanguageCode(code: String) {
        dataStore.edit { it[LEARNING_LANGUAGE_CODE] = code }
    }

    override suspend fun saveLanguage(code: String, completeSetup: Boolean) {
        dataStore.edit { values ->
            values[LANGUAGE_CODE] = code
            if (completeSetup) values[IS_LANGUAGE_SETUP] = true
        }
        LanguagePrefs.set(context, code)
    }

    override suspend fun completeFirstOpen(surveyLanguageCode: String, initialOnboardingProgress: String) {
        val surveyBeforeHome = com.example.basekmp.data.RemoteConfig.surveyBeforeHome
        dataStore.edit { values ->
            values[IS_LANGUAGE_SETUP] = true
            values[FIRST_OPEN_COMPLETED] = true
            if (!surveyBeforeHome) {
                values[ONBOARDING_COMPLETED] = true
            }
            values[FIRST_OPEN_SURVEY_LANGUAGE] = surveyLanguageCode
            values[LEARNING_LANGUAGE_CODE] = surveyLanguageCode
            values[ONBOARDING_PROGRESS] = initialOnboardingProgress
        }
    }

    override suspend fun firstOpenSurveyLanguage(): String =
        dataStore.data.first()[FIRST_OPEN_SURVEY_LANGUAGE].orEmpty()

    override suspend fun onboardingProgress(): String =
        dataStore.data.first()[ONBOARDING_PROGRESS].orEmpty()

    override suspend fun saveOnboardingProgress(serialized: String) {
        dataStore.edit { it[ONBOARDING_PROGRESS] = serialized }
    }

    override suspend fun completeOnboarding(serialized: String) {
        dataStore.edit { values ->
            values[ONBOARDING_PROGRESS] = serialized
            values[ONBOARDING_COMPLETED] = true
        }
    }

    override suspend fun planProgress(): String =
        dataStore.data.first()[PLAN_PROGRESS].orEmpty()

    override suspend fun savePlanProgress(serialized: String) {
        dataStore.edit { it[PLAN_PROGRESS] = serialized }
    }

    override suspend fun learningProgress(): String =
        dataStore.data.first()[LEARNING_PROGRESS].orEmpty()

    override suspend fun saveLearningProgress(serialized: String) {
        dataStore.edit { it[LEARNING_PROGRESS] = serialized }
    }

    companion object {
        private val LANGUAGE_CODE = stringPreferencesKey("native_language_code")
        private val LEARNING_LANGUAGE_CODE = stringPreferencesKey("learning_language_code")
        private val IS_LANGUAGE_SETUP = booleanPreferencesKey("is_language_setup")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val FIRST_OPEN_COMPLETED = booleanPreferencesKey("first_open_completed")
        private val APP_OPEN_COUNT = androidx.datastore.preferences.core.intPreferencesKey("app_open_count")
        private val HAS_OPENED_APP = booleanPreferencesKey("has_opened_app")
        private val HAS_VIEWED_HOME = booleanPreferencesKey("has_viewed_home")
        private val HOME_VIEW_TRACKING_INITIALIZED = booleanPreferencesKey("home_view_tracking_initialized")
        private val FIRST_OPEN_SURVEY_LANGUAGE = stringPreferencesKey("first_open_survey_language")
        private val ONBOARDING_PROGRESS = stringPreferencesKey("onboarding_progress")
        private val PLAN_PROGRESS = stringPreferencesKey("plan_progress")
        private val LEARNING_PROGRESS = stringPreferencesKey("learning_progress")
    }
}
