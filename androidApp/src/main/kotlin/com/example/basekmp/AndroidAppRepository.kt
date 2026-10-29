package com.example.basekmp

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
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

    override suspend fun resolveStartupRoute(): String {
        val preferences = dataStore.edit { values ->
            val appOpenCount = (values[APP_OPEN_COUNT] ?: 0) + 1
            values[APP_OPEN_COUNT] = appOpenCount
            val languageSetup = values[IS_LANGUAGE_SETUP] ?: false
            val onboardingCompleted = values[ONBOARDING_COMPLETED] ?: false
            val firstOpenCompleted = values[FIRST_OPEN_COMPLETED] ?: false
            if (!firstOpenCompleted && appOpenCount == 1 && languageSetup && onboardingCompleted) {
                values[FIRST_OPEN_COMPLETED] = true
            }
        }
        return StartupRouteResolver.resolve(
            languageSetup = preferences[IS_LANGUAGE_SETUP] ?: false,
            onboardingCompleted = preferences[ONBOARDING_COMPLETED] ?: false,
            firstOpenCompleted = preferences[FIRST_OPEN_COMPLETED] ?: false
        )
    }

    override suspend fun currentLanguageCode(): String =
        dataStore.data.first()[LANGUAGE_CODE].orEmpty()

    override suspend fun saveLanguage(code: String, completeSetup: Boolean) {
        dataStore.edit { values ->
            values[LANGUAGE_CODE] = code
            if (completeSetup) values[IS_LANGUAGE_SETUP] = true
        }
        LanguagePrefs.set(context, code)
    }

    override suspend fun completeOnboarding() {
        dataStore.edit { it[ONBOARDING_COMPLETED] = true }
    }

    override suspend fun markHomeShown() {
        dataStore.edit { it[FIRST_OPEN_COMPLETED] = true }
    }

    companion object {
        private val LANGUAGE_CODE = stringPreferencesKey("language_code")
        private val IS_LANGUAGE_SETUP = booleanPreferencesKey("is_language_setup")
        private val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val FIRST_OPEN_COMPLETED = booleanPreferencesKey("first_open_completed")
        private val APP_OPEN_COUNT = intPreferencesKey("app_open_count")
    }
}
